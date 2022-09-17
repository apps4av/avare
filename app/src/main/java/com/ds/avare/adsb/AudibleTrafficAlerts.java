/*
        Copyright (c) 2022, Shane Lenagh
        All rights reserved.

        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright notice, this
        list of conditions and the following disclaimer.

        * Redistributions in binary form must reproduce the above copyright notice,
        this list of conditions and the following disclaimer in the documentation
        and/or other materials provided with the distribution.

        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
        AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
        IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
        FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
        DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
        SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
        CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
        OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        EXPECT THIS SOFTWARE TO FAIL WHEN LIFE, HEALTH, AND PROPERTY ARE AT STAKE.
*/
package com.ds.avare.adsb;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import com.ds.avare.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Audible ADSB nearby traffic alerts, with optional "closest approach" closing time alerts
 */
public class AudibleTrafficAlerts implements Runnable {

    private final SequentialSoundPoolPlayer soundPlayer;
    private final int trafficSoundId;
    private final int bogeySoundId;
    private final int closingInSoundId;
    private final int overSoundId;
    private final int lowSoundId, highSoundId, levelSoundId;
    private final int[] clockHoursSoundIds;
    private final int[] trafficAliasesSoundIds;
    protected final int[] closingInSecondsSoundIds;
    private final int criticallyCloseChirp;
    private final List<Integer> soundIdsToLoad;

    private final LinkedList<String> phoneticAlphaIcaoSequenceQueue;
    private final Map<String,Long> lastAlertTime;
    private final Map<String,String> lastDistanceUpdate;

    private boolean useTrafficAliases = true;
    private boolean topGunDorkMode = false;
    private boolean closingTimeEnabled = true;
    private int closingTimeThreasholdSeconds = 15;
    private float closestApproachThreasholdNmi = 3.0f;
    private float criticalClosingAlertRatio = .4f;

    private static volatile Thread runnerThread;
    // This object's monitor is used for inter-thread communication and synchronization
    private static final LinkedList<AlertItem> alertQueue = new LinkedList<>();
    private static AudibleTrafficAlerts singleton;

    private final static float MAX_ALERT_FREQUENCY_SECONDS = 5f;

    protected static class ClosingEvent {
        private final double closingTimeSec;
        private final double closestApproachDistanceNmi;
        private final long eventTimeMillis;
        private final boolean isCriticallyClose;

        public ClosingEvent(double closingTimeSec, double closestApproachDistanceNmi, boolean isCriticallyClose) {
            this.closingTimeSec = closingTimeSec;
            this.closestApproachDistanceNmi = closestApproachDistanceNmi;
            this.eventTimeMillis = System.currentTimeMillis();
            this.isCriticallyClose = isCriticallyClose;
        }

        public double closingSeconds() {
            return closingTimeSec-(System.currentTimeMillis()-eventTimeMillis)/1000.000;
        }
    }

    protected static class AlertItem {
        final private Traffic traffic;
        final private Location ownLocation;
        final private double distanceNmi;
        final private int ownAltitude;
        final private ClosingEvent closingEvent;

        protected AlertItem(Traffic traffic, Location ownLocation, int ownAltitude, ClosingEvent closingEvent, double distnaceNmi) {
            this.ownAltitude = ownAltitude;
            this.traffic = traffic;
            this.ownLocation = ownLocation;
            this.closingEvent = closingEvent;
            this.distanceNmi = distnaceNmi;
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
            new SequentialSoundPoolPlayer(alertQueue), ctx,
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
                R.raw.tr_cl_16, R.raw.tr_cl_17, R.raw.tr_cl_18, R.raw.tr_cl_19, R.raw.tr_cl_20,
                R.raw.tr_cl_21, R.raw.tr_cl_22, R.raw.tr_cl_23, R.raw.tr_cl_24, R.raw.tr_cl_25,
                R.raw.tr_cl_26, R.raw.tr_cl_27, R.raw.tr_cl_28, R.raw.tr_cl_29, R.raw.tr_cl_30 },
            R.raw.tr_cl_over, R.raw.tr_cl_chirp
        );
    }

    protected AudibleTrafficAlerts(SequentialSoundPoolPlayer sp, Context ctx, int trafficResId, int bogeyResId, int[] clockHoursResId,
                                   int[] trafficAliasesResIds, int highResId, int lowResId,
                                   int levelResId, int closingInResId, int[] closingInSecondsResIds,
                                   int overResId, int criticallyCloseChirp)

    {
        this.phoneticAlphaIcaoSequenceQueue = new LinkedList<>();
        this.soundIdsToLoad = new ArrayList<>();
        this.lastAlertTime = new HashMap<>();
        this.lastDistanceUpdate = new HashMap<>();
        this.soundPlayer = sp;
        this.trafficSoundId = loadSound(trafficResId, ctx);
        this.bogeySoundId = loadSound(bogeyResId, ctx);
        this.clockHoursSoundIds = loadSoundArray(ctx, clockHoursResId);
        this.trafficAliasesSoundIds = loadSoundArray(ctx, trafficAliasesResIds);
        this.highSoundId = loadSound(highResId, ctx);
        this.lowSoundId = loadSound(lowResId, ctx);
        this.levelSoundId = loadSound(levelResId, ctx);
        this.closingInSoundId = loadSound(closingInResId, ctx);
        this.closingInSecondsSoundIds = loadSoundArray(ctx, closingInSecondsResIds);
        this.overSoundId = loadSound(overResId, ctx);
        this.criticallyCloseChirp = loadSound(criticallyCloseChirp, ctx);
    }

    private int loadSound(int resId, Context ctx) {
        final int soundId = this.soundPlayer.load(resId, ctx);
        soundIdsToLoad.add(soundId);
        return soundId;
    }

    private int[] loadSoundArray(Context ctx, int... resourceIds) {
        int[] soundIds = new int[resourceIds.length];
        for (int i = 0; i < resourceIds.length; i++) {
            soundIds[i] = this.soundPlayer.load(resourceIds[i], ctx);
            soundIdsToLoad.add(soundIds[i]);
        }
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
        synchronized (alertQueue) {
            if (runnerThread != null) {
                if (!runnerThread.isInterrupted()) {
                    runnerThread.interrupt();
                }
                runnerThread = null;
            }
            alertQueue.clear();
            if (singleton != null) {
                try {
                    singleton.soundPlayer.close();
                } catch (Exception e) { /* At least we tried to close resources */ }
                singleton = null;
                System.gc();    // Good-faith effort to reclaim feature memory, if possible
            }
        }
    }

    public void setUseTrafficAliases(boolean useTrafficAliases) { this.useTrafficAliases = useTrafficAliases; }
    public void setTopGunDorkMode(boolean topGunDorkMode) { this.topGunDorkMode = topGunDorkMode; }
    public void setClosingTimeEnabled(boolean closingTimeEnabled) { this.closingTimeEnabled = closingTimeEnabled;  }
    public void setClosingTimeThreasholdSeconds(int closingTimeThreasholdSeconds) {  this.closingTimeThreasholdSeconds = closingTimeThreasholdSeconds;  }
    public void setClosestApproachThreasholdNmi(float closestApproachThreasholdNmi) {  this.closestApproachThreasholdNmi = closestApproachThreasholdNmi;  }
    public void setCriticalClosingAlertRatio(float criticalClosingAlertRatio) {  this.criticalClosingAlertRatio = criticalClosingAlertRatio;  }

    /**
     * Process alert queue in a separate thread
     */
    @Override
    public void run() {
        // Wait for all sounds to load before starting alert queue processing
        soundPlayer.waitUntilAllSoundsAreLoaded(soundIdsToLoad);
        // Alert queue processing loop
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (alertQueue) {
                if (alertQueue.size() > 0 && !soundPlayer.isPlaying()) {
                    final AlertItem alert = alertQueue.getFirst();
                    if (!lastAlertTime.containsKey(alert.traffic.mCallSign)
                        || (System.currentTimeMillis()-lastAlertTime.get(alert.traffic.mCallSign))/1000.0
                            > MAX_ALERT_FREQUENCY_SECONDS)
                    {
                            lastAlertTime.put(alert.traffic.mCallSign, System.currentTimeMillis());
                            final List<Integer> alertMessage = buildAlertSoundIdSequence(alertQueue.removeFirst());
                            soundPlayer.playSequence(alertMessage);
                    }
                } else {
                    try {
                        alertQueue.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Ensure status is persisted
                        /* Proceed top top and let thread exit */
                    }
                }
            }
        }
    }

    /**
     * Construct soundId sequence based on alert properties
     * @param alertItem Alert item to build soundId sequence for
     * @return Sequence of soundId's for the soundplayer that represents the assembled alert
     */
    protected List<Integer> buildAlertSoundIdSequence(AlertItem alertItem) {
        final List<Integer> alertAudio = new ArrayList<>();
        final double altitudeDiff = alertItem.ownAltitude - alertItem.traffic.mAltitude;
        final int clockHour = (int) nearestClockHourFromHeadingAndLocations(
                alertItem.ownLocation.getLatitude(), alertItem.ownLocation.getLongitude(),
                alertItem.traffic.mLat, alertItem.traffic.mLon, alertItem.ownLocation.getBearing());
        if (alertItem.closingEvent != null && alertItem.closingEvent.isCriticallyClose)
            alertAudio.add(criticallyCloseChirp);
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
        if (alertItem.closingEvent != null && alertItem.closingEvent.closingSeconds() > 0) {
            alertAudio.add(closingInSoundId);
            if ((int)alertItem.closingEvent.closingSeconds() > closingInSecondsSoundIds.length)
                alertAudio.add(overSoundId);
            alertAudio.add(closingInSecondsSoundIds[
                    Math.min(closingInSecondsSoundIds.length-1, Math.max(0,
                            ((int)Math.round(alertItem.closingEvent.closingSeconds()))-1))]);
        }
        alertAudio.add(clockHoursSoundIds[clockHour - 1]);
        alertAudio.add(Math.abs(altitudeDiff) < 100 ? levelSoundId
                : (altitudeDiff > 0 ? lowSoundId : highSoundId));
        return alertAudio;
    }

    /**
     * Process ADSB traffic list and decide which traffic needs to be added to the alert queue
     * @param ownLocation Ownship location
     * @param allTraffic Traffic array
     * @param alertDistance Horizontal istance in which alert is triggered
     * @param ownAltitude Ownship altitude
     * @param altitudeProximityDangerMinimum Altitude difference in which alert is triggered
     */
    public void handleAudibleAlerts(Location ownLocation, SparseArray<Traffic> allTraffic,
                                    float alertDistance, int ownAltitude,
                                    int altitudeProximityDangerMinimum)
    {
        for (int i = 0; i < allTraffic.size(); i++) {
            final Traffic t = allTraffic.get(allTraffic.keyAt(i));
            final double altDiff = ownAltitude - t.mAltitude;
            final String distanceCalcUpdateKey = t.getLastUpdate()+"_"+ownLocation.getTime();
            final String lastDistanceUpdateKey = lastDistanceUpdate.get(t.mCallSign);
            double currentDistance;
            if (
                (lastDistanceUpdateKey == null || !lastDistanceUpdateKey.equals(distanceCalcUpdateKey))
                && Math.abs(altDiff) < altitudeProximityDangerMinimum
                && (currentDistance = greatCircleDistance(
                    ownLocation.getLatitude(), ownLocation.getLongitude(),  t.mLat,  t.mLon
                )) < alertDistance
            ) {
                ClosingEvent ce = null;
                if (this.closingTimeEnabled) {
                    final int ownSpeedInKts = (int)Math.round(1.0/0.514444f * ownLocation.getSpeed());
                    final double closingEventTimeSec = Math.abs(closestApproachTime(
                            t.mLat, t.mLon, ownLocation.getLatitude(), ownLocation.getLongitude(),
                            t.mHeading, ownLocation.getBearing(), t.mHorizVelocity, ownSpeedInKts
                    ))*60.00*60.00;
                    if (closingEventTimeSec > 0 && closingEventTimeSec < this.closingTimeThreasholdSeconds) {
                        final double[] myCaLoc = locationAfterTime(ownLocation.getLatitude(), ownLocation.getLongitude(),
                                ownLocation.getBearing(), ownSpeedInKts, closingEventTimeSec/3600.000);
                        final double[] theirCaLoc = locationAfterTime(t.mLat, t.mLon, t.mHeading,
                                t.mHorizVelocity, closingEventTimeSec/3600.000);
                        final double caDistance = greatCircleDistance(myCaLoc[0], myCaLoc[1], theirCaLoc[0], theirCaLoc[1]);
                        if (caDistance < this.closestApproachThreasholdNmi && currentDistance > caDistance) {
                                final boolean criticallyClose = this.criticalClosingAlertRatio > 0
                                    &&(closingEventTimeSec / this.closingTimeThreasholdSeconds) <= criticalClosingAlertRatio
                                    && (caDistance / this.closestApproachThreasholdNmi*1.0) <= criticalClosingAlertRatio;
                                ce = new ClosingEvent(closingEventTimeSec, caDistance, criticallyClose);
                        }
                    }
                }
                alertTrafficPosition(new AlertItem(t, ownLocation, ownAltitude, ce, currentDistance));
            }
            lastDistanceUpdate.put(t.mCallSign, distanceCalcUpdateKey);
        }
    }

    private void  alertTrafficPosition(AlertItem alertItem) {
        synchronized (alertQueue) {
            final int alertIndex = alertQueue.indexOf(alertItem);
            if (alertIndex == -1) {
                    alertQueue.add(alertItem);
            } else {    // if already in queue, update with the most recent data prior to speaking
                alertQueue.set(alertIndex, alertItem);
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
            double lat1, double long1, double lat2, double long2,  double myBearing)
    {
        final long nearestClockHour = Math.round(relativeBearingFromHeadingAndLocations(lat1, long1, lat2, long2, myBearing)/30.0);
        return nearestClockHour != 0 ? nearestClockHour : 12;
    }

    protected static double relativeBearingFromHeadingAndLocations(double lat1, double long1, double lat2, double long2,  double myBearing)
    {
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
                                           float heading1, float heading2, int velocity1, int velocity2)
    {
        // Use cosine of average of two latitudes, to give some weighting for lesser intra-lon distance at higher latitudes
        final double a = (lon2 - lon1) * (60.0000 * Math.cos(Math.toRadians((lat1+lat2)/2.0000)));
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

    private interface SoundSequenceOnCompletionListener {
        void onSoundSequenceCompletion(List<Integer> soundIdSequence);
    }

    /**
     * Plays sequential sound samples (e.g., an audio alert) via in-memory SoundPool
     */
    protected static class SequentialSoundPoolPlayer
            implements SoundPool.OnLoadCompleteListener, SoundSequenceOnCompletionListener, AutoCloseable
    {
        private final SoundPool soundPool;
        private final Map<Integer, Long> soundDurationMap;
        private final List<Integer> loadedSounds;
        private final Handler handler;
        private boolean isPlaying = false;
        private final Object synchNotificationMonitor;
        private int waitingForLoadSoundCount = -1;
        private final MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();

        private SequentialSoundPoolPlayer(Object synchNotificationMonitor) {
            // Setting concurrent streams to 2 to allow some edge overlap for looper post-to-execution delay
            this.soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC,0);
            this.soundPool.setOnLoadCompleteListener(this);
            this.loadedSounds = new ArrayList<>();
            this.soundDurationMap = new HashMap<>();
            this.synchNotificationMonitor = synchNotificationMonitor;
            this.handler = new Handler(Looper.getMainLooper());
        }

        public synchronized boolean isPlaying() {
            return isPlaying;
        }

        public synchronized int load(int resId, Context ctx) {
            final int soundId = soundPool.load(ctx, resId, 1);
            soundDurationMap.put(soundId, getSoundDuration(ctx, resId));
            return soundId;
        }

        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized (loadedSounds) {
                if (status == 0) {
                    loadedSounds.add(sampleId);
                    if (this.waitingForLoadSoundCount != -1 && loadedSounds.size() >= waitingForLoadSoundCount) {
                        loadedSounds.notifyAll();
                    }
                }
            }
        }

        public synchronized void  waitUntilAllSoundsAreLoaded(List<Integer> soundIds) {
            synchronized (loadedSounds) {
                this.waitingForLoadSoundCount = soundIds.size();
                while (!loadedSounds.containsAll(soundIds))
                    try {
                        loadedSounds.wait();
                    } catch (InterruptedException ie) {
                        /* Expected */
                    }
                this.waitingForLoadSoundCount = -1;
            }
        }

        @Override
        public void onSoundSequenceCompletion(List<Integer> soundIdSequence) {
            synchronized (synchNotificationMonitor) {
                isPlaying = false;
                synchNotificationMonitor.notifyAll();
            }
        }

        public synchronized void playSequence(List<Integer> soundIds) {
            synchronized (synchNotificationMonitor) {
                if (isPlaying) {
                    return;
                }
                isPlaying = true;
                handler.post(new SequentialSoundPlayRunnable(soundIds, this, handler, soundPool, soundDurationMap));
            }
        }

        private long getSoundDuration(Context context, int rawId) {
            final AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawId);
            metaRetriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            final String durStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Long.parseLong(durStr);
        }

        @Override
        public void close() throws Exception {
            soundPool.release();
            metaRetriever.release();
        }

        /**
         * Runnable to put on Android looper for each alert (sound sequence)
         */
        private static class SequentialSoundPlayRunnable implements Runnable {
            private final List<Integer> soundIds;
            private int curSoundIndex = 0;
            private final Runnable soundPlayCompletionRunnable;
            private final Handler handler;
            private final SoundPool soundPool;
            private final Map<Integer,Long> soundDurationMap;

            private static final float SOUND_PLAY_RATE = 1f;

            public SequentialSoundPlayRunnable(List<Integer> soundIds, SoundSequenceOnCompletionListener listener,
                                               Handler handler, SoundPool soundPool, Map<Integer,Long> soundDurationMap)
            {
                this.soundIds = soundIds;
                this.handler = handler;
                this.soundPool = soundPool;
                this.soundDurationMap = soundDurationMap;
                this.soundPlayCompletionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (curSoundIndex < soundIds.size()) {
                            SequentialSoundPlayRunnable.this.handler.post(SequentialSoundPlayRunnable.this);
                        } else {
                            if (listener != null)
                                listener.onSoundSequenceCompletion(soundIds);
                        }
                    }
                };
            }

            @Override
            public void run() {
                final int soundId = this.soundIds.get(curSoundIndex++);
                soundPool.play(soundId, 1, 1, 1, 0, SOUND_PLAY_RATE);
                handler.postDelayed(this.soundPlayCompletionRunnable, (long) Math.ceil(soundDurationMap.get(soundId)/SOUND_PLAY_RATE));
            }
        }
    }
}