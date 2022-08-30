package com.ds.avare.adsb;

import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;
import android.util.SparseArray;

import com.ds.avare.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class AudibleTrafficAlerts implements Runnable {
    final private MediaPlayer mpTraffic;
    final private MediaPlayer mpBogey;
    final private MediaPlayer mpClosingIn;

    final private MediaPlayer mpOver;
    final private MediaPlayer mpLow, mpHigh, mpLevel;
    final private MediaPlayer[] arrMpClockHours;
    final private MediaPlayer[] arrMpTrafficAliases;
    final protected MediaPlayer[] arrMpClosingInSeconds;
    final private SequentialMediaPlayer sequentialMediaPlayer;
    private static volatile Thread runnerThread;
    final private LinkedList<AlertItem> alertQueue;
    final private LinkedList<String> phoneticAlphaIcaoSequenceQueue;
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
            MediaPlayer.create(ctx, R.raw.tr_traffic),
            MediaPlayer.create(ctx, R.raw.tr_bogey),
            new MediaPlayer[] {
                MediaPlayer.create(ctx, R.raw.tr_one), MediaPlayer.create(ctx, R.raw.tr_two), MediaPlayer.create(ctx, R.raw.tr_three),
                MediaPlayer.create(ctx, R.raw.tr_four), MediaPlayer.create(ctx, R.raw.tr_five), MediaPlayer.create(ctx, R.raw.tr_six),
                MediaPlayer.create(ctx, R.raw.tr_seven), MediaPlayer.create(ctx, R.raw.tr_eight), MediaPlayer.create(ctx, R.raw.tr_nine),
                MediaPlayer.create(ctx, R.raw.tr_ten), MediaPlayer.create(ctx, R.raw.tr_eleven), MediaPlayer.create(ctx, R.raw.tr_twelve)
            },
            new MediaPlayer[] {
                MediaPlayer.create(ctx, R.raw.tr_alpha), MediaPlayer.create(ctx, R.raw.tr_bravo), MediaPlayer.create(ctx, R.raw.tr_charlie),
                MediaPlayer.create(ctx, R.raw.tr_delta), MediaPlayer.create(ctx, R.raw.tr_echo), MediaPlayer.create(ctx, R.raw.tr_foxtrot),
                MediaPlayer.create(ctx, R.raw.tr_golf), MediaPlayer.create(ctx, R.raw.tr_hotel), MediaPlayer.create(ctx, R.raw.tr_india),
                MediaPlayer.create(ctx, R.raw.tr_juliet), MediaPlayer.create(ctx, R.raw.tr_kilo), MediaPlayer.create(ctx, R.raw.tr_lima),
                MediaPlayer.create(ctx, R.raw.tr_mike), MediaPlayer.create(ctx, R.raw.tr_november), MediaPlayer.create(ctx, R.raw.tr_oscar),
                MediaPlayer.create(ctx, R.raw.tr_papa), MediaPlayer.create(ctx, R.raw.tr_quebec), MediaPlayer.create(ctx, R.raw.tr_romeo),
                MediaPlayer.create(ctx, R.raw.tr_sierra), MediaPlayer.create(ctx, R.raw.tr_tango), MediaPlayer.create(ctx, R.raw.tr_uniform),
                MediaPlayer.create(ctx, R.raw.tr_victor), MediaPlayer.create(ctx, R.raw.tr_whiskey), MediaPlayer.create(ctx, R.raw.tr_xray),
                MediaPlayer.create(ctx, R.raw.tr_yankee), MediaPlayer.create(ctx, R.raw.tr_zulu)
            },
            MediaPlayer.create(ctx, R.raw.tr_high),
            MediaPlayer.create(ctx, R.raw.tr_low),
            MediaPlayer.create(ctx, R.raw.tr_level),
            MediaPlayer.create(ctx, R.raw.tr_cl_closingin),
            new MediaPlayer[] {
                MediaPlayer.create(ctx, R.raw.tr_cl_01), MediaPlayer.create(ctx, R.raw.tr_cl_02),
                MediaPlayer.create(ctx, R.raw.tr_cl_03), MediaPlayer.create(ctx, R.raw.tr_cl_04),
                MediaPlayer.create(ctx, R.raw.tr_cl_05), MediaPlayer.create(ctx, R.raw.tr_cl_06),
                MediaPlayer.create(ctx, R.raw.tr_cl_07), MediaPlayer.create(ctx, R.raw.tr_cl_08),
                MediaPlayer.create(ctx, R.raw.tr_cl_09), MediaPlayer.create(ctx, R.raw.tr_cl_10)
            },
                MediaPlayer.create(ctx, R.raw.tr_cl_over)
        );

    }

    protected AudibleTrafficAlerts(MediaPlayer mpTraffic, MediaPlayer mpBogey, MediaPlayer[] arrMpClockHours,
                                   MediaPlayer[] arrMpTrafficAliases, MediaPlayer mpHigh, MediaPlayer mpLow,
                                   MediaPlayer mpLevel, MediaPlayer mpClosingIn, MediaPlayer[] arrMpClosingInSeconds,
                                   MediaPlayer mpOver)

    {
        alertQueue = new LinkedList<>();
        phoneticAlphaIcaoSequenceQueue = new LinkedList<>();
        sequentialMediaPlayer = new SequentialMediaPlayer(alertQueue);

        this.mpTraffic = mpTraffic;
        this.mpBogey = mpBogey;
        this.arrMpClockHours = arrMpClockHours;
        this.arrMpTrafficAliases = arrMpTrafficAliases;
        this.mpHigh = mpHigh;
        this.mpLow = mpLow;
        this.mpLevel = mpLevel;
        this.mpClosingIn = mpClosingIn;
        this.arrMpClosingInSeconds = arrMpClosingInSeconds;
        this.mpOver = mpOver;
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

    public void setUseTrafficAliases(boolean useTrafficAliases) {
        this.useTrafficAliases = useTrafficAliases;
    }

    public void setTopGunDorkMode(boolean topGunDorkMode) {
        this.topGunDorkMode = topGunDorkMode;
    }
    public void setClosingTimeEnabled(boolean closingTimeEnabled) { this.closingTimeEnabled = closingTimeEnabled;  }
    public void setClosingTimeThreasholdSeconds(int closingTimeThreasholdSeconds) {  this.closingTimeThreasholdSeconds = closingTimeThreasholdSeconds;  }
    public void setClosestApproachThreasholdNmi(float closestApproachThreasholdNmi) {  this.closestApproachThreasholdNmi = closestApproachThreasholdNmi;  }


    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (alertQueue) {
                if (this.alertQueue.size() > 0 && !sequentialMediaPlayer.isPlaying) {
                    if (sequentialMediaPlayer.setMedia(buildAudioMessage(alertQueue.removeFirst())))
                        sequentialMediaPlayer.play();
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

    protected List<MediaPlayer> buildAudioMessage(AlertItem alertItem) {
        final List<MediaPlayer> alertAudio = new ArrayList<>();
        final double altitudeDiff = alertItem.ownAltitude - alertItem.traffic.mAltitude;
        final int clockHour = (int) nearestClockHourFromHeadingAndLocations(
                alertItem.ownLocation.getLatitude(), alertItem.ownLocation.getLongitude(),
                alertItem.traffic.mLat, alertItem.traffic.mLon, alertItem.ownLocation.getBearing());
        alertAudio.add(topGunDorkMode ? mpBogey : mpTraffic);
        if (useTrafficAliases) {
            int icaoIndex = phoneticAlphaIcaoSequenceQueue.indexOf(alertItem.traffic.mCallSign);
            if (icaoIndex == -1) {
                phoneticAlphaIcaoSequenceQueue.add(alertItem.traffic.mCallSign);
                icaoIndex = phoneticAlphaIcaoSequenceQueue.size()-1;
            }
            // TODO: double/triple/etc. id if you get to end, rather than starting over...worth it?
            alertAudio.add(arrMpTrafficAliases[icaoIndex % arrMpTrafficAliases.length]);
        }
        if (alertItem.closingEvent != null) {
            alertAudio.add(mpClosingIn);
            if ((int)alertItem.closingEvent.closingSeconds() > arrMpClosingInSeconds.length)
                alertAudio.add(mpOver);
            alertAudio.add(arrMpClosingInSeconds[
                    Math.min(arrMpClosingInSeconds.length-1, Math.max(0,
                            (int)alertItem.closingEvent.closingSeconds()-1))]);
        }
        alertAudio.add(arrMpClockHours[clockHour - 1]);
        alertAudio.add(Math.abs(altitudeDiff) < 100 ? mpLevel
                : (altitudeDiff > 0 ? mpLow : mpHigh));
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
                if (this.closingTimeEnabled) { //TODO: CE pref enabled
                    final double closingEventTimeSec = closestApproachTime(
                            t.mLat, t.mLon, ownLocation.getLatitude(), ownLocation.getLongitude(),
                            t.mHeading, ownLocation.getBearing(), t.mHorizVelocity, (int) ownLocation.getSpeed()
                    )*60.00*60.00;
                    if (closingEventTimeSec > 0 && closingEventTimeSec < this.closingTimeThreasholdSeconds) {
                        final double[] myCaLoc = locationAfterTime(ownLocation.getLatitude(), ownLocation.getLongitude(),
                                ownLocation.getBearing(), ownLocation.getSpeed(), closingEventTimeSec/3600.00);
                        final double[] theirCaLoc = locationAfterTime(t.mLat, t.mLon, t.mHeading,
                                t.mHorizVelocity, closingEventTimeSec/3600.00);
                        final double caDistance = greatCircleDistance(myCaLoc[0], myCaLoc[1], theirCaLoc[0], theirCaLoc[1]);
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
            if (alertItem.closingEvent != null)
                System.out.println("Adding one with closing event");
            final int alertIndex = alertQueue.indexOf(alertItem);
            if (alertIndex == -1) {
                this.alertQueue.add(alertItem);
            } else {    // if already in queue, update with the most recent data prior to speaking
                this.alertQueue.set(alertIndex, alertItem);
            }
            alertQueue.notifyAll();
        }
    }

    /**
     * Helper class that uses media event handling to ensure strictly sequential play of a list
     * of media resources
     */
    private static class SequentialMediaPlayer implements MediaPlayer.OnCompletionListener {

        private List<MediaPlayer> media;
        private boolean isPlaying = false;
        private int mediaIndex = 0;
        final private Object playStatusMonitorObject;

        SequentialMediaPlayer(Object playStatusMonitorObject) {
            if (playStatusMonitorObject == null)
                throw new IllegalArgumentException("Play status monitor object must not be null");
            this.playStatusMonitorObject = playStatusMonitorObject;
        }

        /**
         * @param media Media item sequence to queue in player
         */
        public synchronized boolean setMedia(List<MediaPlayer> media) {
            if (!isPlaying) {
                this.media = media;
                this.mediaIndex = 0;
                for (MediaPlayer mp : media)
                    mp.setOnCompletionListener(this);
                return true;
            } else
                return false;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (++mediaIndex <= media.size()-1)
                play();
            else {
                this.isPlaying = false;
                synchronized(playStatusMonitorObject) {
                    playStatusMonitorObject.notifyAll();
                }
            }
        }

        public synchronized void play() {
            if (media == null || mediaIndex > media.size()-1)
                throw new IllegalStateException("No more media to play; finished sequence or no media set");
            isPlaying = true;
            media.get(mediaIndex).start();
        }
    }

    protected static double angleFromCoordinate(double lat1, double long1, double lat2,
                                              double long2) {

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
        final double a = (lon2 - lon1) * (60.0 * Math.cos(Math.toRadians((lat1+lat2)/2.0000))) ;
        final double b = velocity2*Math.cos(Math.toDegrees(heading2)) - velocity1*Math.cos(Math.toDegrees(heading1));
        final double c = (lat2 - lat1) * 60.0;
        final double d = velocity2*Math.sin(Math.toDegrees(heading2)) - velocity1*Math.sin(Math.toDegrees(heading1));

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
}