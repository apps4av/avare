package com.ds.avare.adsb;

import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.util.SparseArray;

import com.ds.avare.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class AudibleTrafficAlerts implements Runnable {
    private int trafficSoundId;
    private int bogeySoundId;
    private int closingInSoundId;

    private  SequentialSoundPoolPlayer soundPlayer;
    private int overSoundId;
    private int lowSoundId, highSoundId, levelSoundId;
    private int[] clockHoursSoundIds;
    private int[] trafficAliasesSoundIds;
    protected final int[] closingInSecondsSoundIds;

    private static volatile Thread runnerThread;
    private LinkedList<AlertItem> alertQueue;
    private LinkedList<String> phoneticAlphaIcaoSequenceQueue;
    private static AudibleTrafficAlerts singleton;
    private boolean useTrafficAliases = true;
    private boolean topGunDorkMode = false;
    private boolean closingTimeEnabled = true;
    private int closingTimeThreasholdSeconds = 15;
    private float closestApproachThreasholdNmi = 3.0f;

    protected static class ClosingEvent {
        private final double closingTimeSec;
        private final double closestApproachDistanceNmi;
        private final long eventTimeMillis;

        public ClosingEvent(double closingTimeSec, double closestApproachDistanceNmi) {
            this.closingTimeSec = closingTimeSec;
            this.closestApproachDistanceNmi = closestApproachDistanceNmi;
            this.eventTimeMillis = System.currentTimeMillis();
        }

        public double closingSeconds() {
            return closingTimeSec-(System.currentTimeMillis()-eventTimeMillis)/1000.000;
        }
    }

    protected static class AlertItem {
        final private Traffic traffic;
        final private Location ownLocation;
        final private int ownAltitude;
        final private ClosingEvent closingEvent;

        protected AlertItem(Traffic traffic, Location ownLocation, int ownAltitude, ClosingEvent closingEvent) {
            this.ownAltitude = ownAltitude;
            this.traffic = traffic;
            this.ownLocation = ownLocation;
            this.closingEvent = closingEvent;
        }

        @Override
        public final int hashCode() {
            return traffic.mCallSign.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof AlertItem))
                return false;
            return ((AlertItem)o).traffic.mCallSign.equals(this.traffic.mCallSign);
        }
    }

    protected AudibleTrafficAlerts(Context ctx) {
        this(
            new SequentialSoundPoolPlayer(ctx),
            R.raw.tr_traffic, R.raw.tr_bogey,
            new int[] { R.raw.tr_one, R.raw.tr_two, R.raw.tr_three, R.raw.tr_four, R.raw.tr_five,
                R.raw.tr_six, R.raw.tr_seven, R.raw.tr_eight, R.raw.tr_nine, R.raw.tr_ten,
                R.raw.tr_eleven, R.raw.tr_twelve },
            new int[] { R.raw.tr_alpha, R.raw.tr_bravo, R.raw.tr_charlie, R.raw.tr_delta, R.raw.tr_echo,
                R.raw.tr_foxtrot, R.raw.tr_golf, R.raw.tr_hotel, R.raw.tr_india, R.raw.tr_juliet,
                R.raw.tr_kilo, R.raw.tr_lima, R.raw.tr_mike, R.raw.tr_november, R.raw.tr_oscar,
                R.raw.tr_papa, R.raw.tr_quebec, R.raw.tr_romeo, R.raw.tr_sierra, R.raw.tr_tango,
                R.raw.tr_uniform, R.raw.tr_victor, R.raw.tr_whiskey, R.raw.tr_xray, R.raw.tr_yankee,
                R.raw.tr_zulu },
            R.raw.tr_high, R.raw.tr_low, R.raw.tr_level, R.raw.tr_cl_closingin,
            new int[] { R.raw.tr_cl_01, R.raw.tr_cl_02, R.raw.tr_cl_03, R.raw.tr_cl_04, R.raw.tr_cl_05,
                R.raw.tr_cl_06, R.raw.tr_cl_07, R.raw.tr_cl_08, R.raw.tr_cl_09, R.raw.tr_cl_10,
                R.raw.tr_cl_11, R.raw.tr_cl_12, R.raw.tr_cl_13, R.raw.tr_cl_14, R.raw.tr_cl_15,
                R.raw.tr_cl_16, R.raw.tr_cl_17, R.raw.tr_cl_18, R.raw.tr_cl_19, R.raw.tr_cl_20 },
            R.raw.tr_cl_over
        );
    }

    protected AudibleTrafficAlerts(SequentialSoundPoolPlayer sp, int trafficResId, int bogeyResId, int[] clockHoursResId,
                                   int[] trafficAliasesResIds, int highResId, int lowResId,
                                   int levelResId, int closingInResId, int[] closingInSecondsResIds,
                                   int overResId)

    {
        alertQueue = new LinkedList<>();
        phoneticAlphaIcaoSequenceQueue = new LinkedList<>();
        this.soundPlayer = sp;
        this.trafficSoundId = sp.load(trafficResId);
        this.bogeySoundId = sp.load(bogeyResId);
        this.clockHoursSoundIds = loadSoundArray(sp, clockHoursResId);
        this.trafficAliasesSoundIds = loadSoundArray(sp, trafficAliasesResIds);
        this.highSoundId = sp.load(highResId);
        this.lowSoundId = sp.load(lowResId);
        this.levelSoundId = sp.load(levelResId);
        this.closingInSoundId = sp.load(closingInResId);
        this.closingInSecondsSoundIds = loadSoundArray(sp, closingInSecondsResIds);
        this.overSoundId = sp.load(overResId);
    }

    private int[] loadSoundArray(SequentialSoundPoolPlayer sp, int... resourceIds) {
        int[] soundIds = new int[resourceIds.length];
        for (int i = 0; i < resourceIds.length; i++)
            soundIds[i] = sp.load(resourceIds[i]);
        return soundIds;
    }


    public synchronized static AudibleTrafficAlerts getAndStartAudibleTrafficAlerts(Context ctx) {
        if (singleton == null)
            singleton = new AudibleTrafficAlerts(ctx);
        if (runnerThread == null || runnerThread.isInterrupted()) {
            runnerThread = new Thread(singleton, "AudibleAlerts");
            runnerThread.start();
        }
        return singleton;
    }

    public static synchronized void stopAudibleTrafficAlerts() {
        if (runnerThread != null) {
            runnerThread.interrupt();
            runnerThread = null;
        }
    }

    public void setUseTrafficAliases(boolean useTrafficAliases) { this.useTrafficAliases = useTrafficAliases;     }
    public void setTopGunDorkMode(boolean topGunDorkMode) { this.topGunDorkMode = topGunDorkMode; }
    public void setClosingTimeEnabled(boolean closingTimeEnabled) { this.closingTimeEnabled = closingTimeEnabled;  }
    public void setClosingTimeThreasholdSeconds(int closingTimeThreasholdSeconds) {  this.closingTimeThreasholdSeconds = closingTimeThreasholdSeconds;  }
    public void setClosestApproachThreasholdNmi(float closestApproachThreasholdNmi) {  this.closestApproachThreasholdNmi = closestApproachThreasholdNmi;  }


    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (alertQueue) {
                if (this.alertQueue.size() > 0) {
                    List<Integer> alertMessage = buildAlertSoundIdSequence(alertQueue.removeFirst());
                    soundPlayer.playSequence(alertMessage);
                } else {
                    try {
                        alertQueue.wait();
                    } catch (InterruptedException e) {
                        /* Proceed top top and check if thread is meant to exit */
                    }
                }
            }
        }
    }

    protected List<Integer> buildAlertSoundIdSequence(AlertItem alertItem) {
        final List<Integer> alertAudio = new ArrayList<>();
        final double altitudeDiff = alertItem.ownAltitude - alertItem.traffic.mAltitude;
        final int clockHour = (int) nearestClockHourFromHeadingAndLocations(
                alertItem.ownLocation.getLatitude(), alertItem.ownLocation.getLongitude(),
                alertItem.traffic.mLat, alertItem.traffic.mLon, alertItem.ownLocation.getBearing());
        alertAudio.add(topGunDorkMode ? bogeySoundId : trafficSoundId);
        if (useTrafficAliases) {
            int icaoIndex = phoneticAlphaIcaoSequenceQueue.indexOf(alertItem.traffic.mCallSign);
            if (icaoIndex == -1) {
                phoneticAlphaIcaoSequenceQueue.add(alertItem.traffic.mCallSign);
                icaoIndex = phoneticAlphaIcaoSequenceQueue.size()-1;
            }
            // TODO: double/triple/etc. id if you get to end, rather than starting over...worth it?
            alertAudio.add(trafficAliasesSoundIds[icaoIndex % trafficAliasesSoundIds.length]);
        }
        if (alertItem.closingEvent != null) {
            alertAudio.add(closingInSoundId);
            if ((int)alertItem.closingEvent.closingSeconds() > closingInSecondsSoundIds.length)
                alertAudio.add(overSoundId);
            alertAudio.add(closingInSecondsSoundIds[
                    Math.min(closingInSecondsSoundIds.length-1, Math.max(0,
                            (int)alertItem.closingEvent.closingSeconds()-1))]);
        }
        alertAudio.add(clockHoursSoundIds[clockHour - 1]);
        alertAudio.add(Math.abs(altitudeDiff) < 100 ? levelSoundId
                : (altitudeDiff > 0 ? lowSoundId : highSoundId));
        return alertAudio;
    }

    public void handleAudibleAlerts(Location ownLocation, SparseArray<Traffic> allTraffic,
                                           float alertDistance, int ownAltitude,
                                           int altitudeProximityDangerMinimum)
    {
        for (int i = 0; i < allTraffic.size(); i++) {
            Traffic t = allTraffic.get(allTraffic.keyAt(i));
            double altDiff = ownAltitude - t.mAltitude;
            if (greatCircleDistance(
                    ownLocation.getLatitude(), ownLocation.getLongitude(),  t.mLat,  t.mLon
            ) < alertDistance
                    && Math.abs(altDiff) < altitudeProximityDangerMinimum
            ) {
                ClosingEvent ce = null;
                if (this.closingTimeEnabled) {
                    final double closingEventTimeSec = closestApproachTime(
                            t.mLat, t.mLon, ownLocation.getLatitude(), ownLocation.getLongitude(),
                            t.mHeading, ownLocation.getBearing(), t.mHorizVelocity, (int) ownLocation.getSpeed()
                    )*60.00*60.00;
                    //System.out.println("Closing time for "+t.mCallSign+" is "+closingEventTimeSec);
                    if (closingEventTimeSec > 0 && closingEventTimeSec < this.closingTimeThreasholdSeconds) {
                        final double[] myCaLoc = locationAfterTime(ownLocation.getLatitude(), ownLocation.getLongitude(),
                                ownLocation.getBearing(), ownLocation.getSpeed(), closingEventTimeSec/3600.00);
                        final double[] theirCaLoc = locationAfterTime(t.mLat, t.mLon, t.mHeading,
                                t.mHorizVelocity, closingEventTimeSec/3600.00);
                        final double caDistance = greatCircleDistance(myCaLoc[0], myCaLoc[1], theirCaLoc[0], theirCaLoc[1]);
                        //System.out.println("Closest approach for "+t.mCallSign+" is "+caDistance);
                        if (caDistance < this.closestApproachThreasholdNmi) {
                            ce = new ClosingEvent(closingEventTimeSec, caDistance);
                        }
                    }
                }
                alertTrafficPosition(new AlertItem(t, ownLocation, ownAltitude, ce));
            }
        }

    }

    private void  alertTrafficPosition(AlertItem alertItem) {
        synchronized (alertQueue) {
            final int alertIndex = alertQueue.indexOf(alertItem);
            if (alertIndex == -1) {
                this.alertQueue.add(alertItem);
            } else {    // if already in queue, update with the most recent data prior to speaking
                this.alertQueue.set(alertIndex, alertItem);
            }
            alertQueue.notifyAll();
        }
    }

    protected static double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {
        final double lat1Rad = Math.toRadians(lat1);
        final double long1Rad = Math.toRadians(long1);
        final double lat2Rad = Math.toRadians(lat2);
        final double long2Rad = Math.toRadians(long2);

        final double dLon = (long2Rad - long1Rad);

        final double y = Math.sin(dLon) * Math.cos(lat2Rad);
        final double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad)
                * Math.cos(lat2Rad) * Math.cos(dLon);

        final double bearingRad = Math.atan2(y, x);

        return  (Math.toDegrees(bearingRad) + 360) % 360;
    }

    protected static long nearestClockHourFromHeadingAndLocations(
            double lat1, double long1, double lat2, double long2,  double myBearing) {
        final long nearestClockHour = Math.round(relativeBearingFromHeadingAndLocations(lat1, long1, lat2, long2, myBearing)/30.0);
        return nearestClockHour != 0 ? nearestClockHour : 12;
    }

    protected static double relativeBearingFromHeadingAndLocations(double lat1, double long1, double lat2, double long2,  double myBearing) {
        return (angleFromCoordinate(lat1, long1, lat2, long2) - myBearing + 360) % 360;
    }

    /**
     * Great circle distance between two lat/lon's via Haversine formula, Java impl courtesy of https://introcs.cs.princeton.edu/java/12types/GreatCircle.java.html
     * @param lat1 Latitude 1
     * @param lon1 Longitude 1
     * @param lat2 Latitude 2
     * @param lon2 Longitude 2
     * @return Great circle distance between two points
     */
    private static double greatCircleDistance(double lat1, double lon1, double lat2, double lon2) {

        final double x1 = Math.toRadians(lat1);
        final double y1 = Math.toRadians(lon1);
        final double x2 = Math.toRadians(lat2);
        final double y2 = Math.toRadians(lon2);

        /*
         * Compute using Haversine formula
         */
        final double a = Math.pow(Math.sin((x2-x1)/2), 2)
                + Math.cos(x1) * Math.cos(x2) * Math.pow(Math.sin((y2-y1)/2), 2);

        // great circle distance in radians
        final double angle2 = 2 * Math.asin(Math.min(1, Math.sqrt(a)));

        // convert back to degrees, and each degree on a great circle of Earth is 60 nautical miles
        return 60 * Math.toDegrees(angle2);
    }

    protected static double closestApproachTime(double lat1, double lon1, double lat2, double lon2,
                                           float heading1, float heading2, int velocity1, int velocity2) {
        final double a = (lon2 - lon1) * (60.0000 * Math.cos(Math.toRadians((lat1+lat2)/2.0000))) ;
        final double b = velocity2*Math.sin(Math.toRadians(heading2)) - velocity1*Math.sin(Math.toRadians(heading1));
        final double c = (lat2 - lat1) * 60.0000;
        final double d = velocity2*Math.cos(Math.toRadians(heading2)) - velocity1*Math.cos(Math.toRadians(heading1));

        return - ((a*b + c*d) / (b*b + d*d));
    }

    protected static double[] locationAfterTime(double lat, double lon, float heading, float velocityInKt, double timeInHrs) {
        final double newLat =  lat + Math.cos(Math.toRadians(heading)) * (velocityInKt/60.00000) * timeInHrs;
        return new double[]  {
                newLat,
                lon + Math.sin(Math.toRadians(heading))
                        * (velocityInKt / (60.00000*Math.cos(Math.toRadians((newLat+lat)/2.0000))))
                        * timeInHrs
        };
    }

    /**
     * Helper class for playing synchronous, sequential sound samples
     */
    protected static class SequentialSoundPoolPlayer implements SoundPool.OnLoadCompleteListener {
        private final SoundPool sp;
        private final HashMap<Integer, Long> soundDurationMap;
        private final List<Integer> loadedSounds;
        private Handler handler;
        private boolean isPlaying = false;
        private Context ctx;


        SequentialSoundPoolPlayer(Context ctx) {
            sp = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
            this.ctx = ctx;
            soundDurationMap = new HashMap<>();
            loadedSounds = new ArrayList<>();
            handler = new Handler();
            sp.setOnLoadCompleteListener(this);
        }

        public synchronized int load(int resId) {
            final int soundId = sp.load(ctx, resId, 1);
            soundDurationMap.put(soundId, getSoundDuration(ctx, resId));
            return soundId;
        }

        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if(status == 0)
                loadedSounds.add(sampleId);
        }

        public synchronized void playSequence(List<Integer> soundIds) {
                for (Integer soundId : soundIds)
                    sequentialPlay(soundId);
        }

        private synchronized void sequentialPlay(int soundId) {
            if (!loadedSounds.contains(soundId))
                throw new IllegalStateException("This soundId is not yet loaded: "+soundId);
            sp.play(soundId, 1f, 1f, 1, 0, 1);
            try {
                // Give sound time to finish before returning, based on known duration
                Thread.sleep(soundDurationMap.get(soundId));
            } catch (InterruptedException e) {
                /* Expected */
            }
        }

        private long getSoundDuration(Context context, int rawId){
            final MediaPlayer player = MediaPlayer.create(context, rawId);
            long duration = player.getDuration();
            player.release();
            return duration;
        }
    }
}