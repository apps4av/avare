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

import com.ds.avare.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audible ADSB nearby traffic alerts, with optional "closest approach" closing time alerts
 */
public class AudibleTrafficAlerts implements Runnable {

    // Audible alert sound pool player utility
    private final SequentialSoundPoolPlayer soundPlayer;

    // Sound pool soundId's
    private final int trafficSoundId;
    private final int bogeySoundId;
    private final int closingInSoundId;
    private final int overSoundId;
    private final int lowSoundId, highSoundId, levelSoundId;
    private final int[] clockHoursSoundIds;
    private final int[] trafficAliasesSoundIds;
    protected final int[] numberSoundIds;
    private final int secondsSoundId;
    private final int milesSoundId;
    private final int climbingSoundId;
    private final int descendingSoundId;
    private final int criticallyCloseChirp;
    private final int withinSoundId;
    protected final int decimalSoundId;

    // Trackers for traffic callsigns, update freshness, and alert frequenncy
    private final LinkedList<String> phoneticAlphaIcaoSequenceQueue;
    private final Map<String,Long> lastAlertTime;
    private final Map<String,String> lastDistanceUpdate;

    // Configuration settings
    private boolean useTrafficAliases = true;
    private boolean topGunDorkMode = false;
    private boolean closingTimeEnabled = true;
    private boolean groundAlertsEnabled = false;
    private int closingTimeThresholdSeconds = 15;
    private float closestApproachThresholdNmi = 3.0f;
    private float criticalClosingAlertRatio = .4f;
    private float maxAlertFrequencySeconds = 15f;
    private float minSpeed = 0.0f;
    private boolean useDecimalPrecision = false;

    // Core alert tracking data structures, threading objects, and feature instances
    private static volatile Thread alertQueueProcessingConsumerThread;
    protected final ExecutorService trafficAlertProducerExecutor = Executors.newSingleThreadExecutor();
    // This object's monitor is used for inter-thread communication and synchronization
    private static final LinkedList<Alert> alertQueue = new LinkedList<>();
    private static AudibleTrafficAlerts singleton;

    // Constants
    private static final float MPS_TO_KNOTS_CONV = 1.0f/0.514444f;

    protected static class Alert {
        private final String trafficCallsign;
        private final double distanceNmi;
        private final ClosingEvent closingEvent;
        private final int clockHour;
        private final double altitudeDiff;
        private final int vspeed;

        protected Alert(String trafficCallsign, int clockHour, double altitudeDiff, ClosingEvent closingEvent, double distnaceNmi, int vspeed) {
            this.trafficCallsign = trafficCallsign;
            this.clockHour = clockHour;
            this.altitudeDiff = altitudeDiff;
            this.closingEvent = closingEvent;
            this.distanceNmi = distnaceNmi;
            this.vspeed = vspeed;
        }

        @Override
        public final int hashCode() {
            return trafficCallsign.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof Alert))
                return false;
            return ((Alert)o).trafficCallsign.equals(this.trafficCallsign);
        }

        protected static class ClosingEvent {
            private final double closingTimeSec;
            private final double closestApproachDistanceNmi;    // TODO: Give option to speak this too?
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
            new int[] { R.raw.tr_00, R.raw.tr_01, R.raw.tr_02, R.raw.tr_03, R.raw.tr_04, R.raw.tr_05,
                R.raw.tr_06, R.raw.tr_07, R.raw.tr_08, R.raw.tr_09 },
            R.raw.tr_cl_over, R.raw.tr_cl_chirp
        );
    }

    protected AudibleTrafficAlerts(SequentialSoundPoolPlayer sp, Context ctx, int trafficResId, int bogeyResId, int[] clockHoursResIds,
                                   int[] trafficAliasesResIds, int highResId, int lowResId,
                                   int levelResId, int closingInResId, int[] closingInSecondsResIds,
                                   int overResId, int criticallyCloseChirp)

    {
        this.phoneticAlphaIcaoSequenceQueue = new LinkedList<>();
        this.lastAlertTime = new HashMap<>();
        this.lastDistanceUpdate = new HashMap<>();
        this.soundPlayer = sp;
        this.trafficSoundId = sp.load(ctx, trafficResId)[0];
        this.bogeySoundId = sp.load(ctx, bogeyResId)[0];
        this.clockHoursSoundIds = sp.load(ctx, clockHoursResIds);
        this.trafficAliasesSoundIds = sp.load(ctx, trafficAliasesResIds);
        this.highSoundId = sp.load(ctx, highResId)[0];
        this.lowSoundId = sp.load(ctx, lowResId)[0];
        this.levelSoundId = sp.load(ctx, levelResId)[0];
        this.closingInSoundId = sp.load(ctx, closingInResId)[0];
        this.numberSoundIds = sp.load(ctx, closingInSecondsResIds);
        this.overSoundId = sp.load(ctx, overResId)[0];
        this.criticallyCloseChirp = sp.load(ctx, criticallyCloseChirp)[0];
        this.secondsSoundId = sp.load(ctx, R.raw.tr_seconds)[0];
        this.milesSoundId = sp.load(ctx, R.raw.tr_miles)[0];
        this.climbingSoundId = sp.load(ctx, R.raw.tr_climbing)[0];
        this.descendingSoundId = sp.load(ctx, R.raw.tr_descending)[0];
        this.withinSoundId = sp.load(ctx, R.raw.tr_within)[0];
        this.decimalSoundId = sp.load(ctx, R.raw.tr_decimal)[0];
    }


    /**
     * Factory to get feature instance, and start alert queue processing thread
     * @param ctx Android context
     * @return Single audible alerts instance
     */
    public synchronized static AudibleTrafficAlerts getAndStartAudibleTrafficAlerts(Context ctx) {
        if (singleton == null) {
            singleton = new AudibleTrafficAlerts(ctx);
            synchronized (alertQueue) {
                alertQueue.clear(); // start with a clean slate
            }
        }
        if (alertQueueProcessingConsumerThread == null || alertQueueProcessingConsumerThread.isInterrupted()) {
            alertQueueProcessingConsumerThread = new Thread(singleton, "AudibleAlerts");
            alertQueueProcessingConsumerThread.start();
        }

        return singleton;
    }

    public static synchronized void stopAudibleTrafficAlerts() {
        synchronized (alertQueue) {
            if (alertQueueProcessingConsumerThread != null) {
                if (!alertQueueProcessingConsumerThread.isInterrupted()) {
                    alertQueueProcessingConsumerThread.interrupt();
                }
                alertQueueProcessingConsumerThread = null;
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
    public void setClosingTimeThresholdSeconds(int closingTimeThresholdSeconds) {  this.closingTimeThresholdSeconds = closingTimeThresholdSeconds;  }
    public void setClosestApproachThresholdNmi(float closestApproachThresholdNmi) {  this.closestApproachThresholdNmi = closestApproachThresholdNmi;  }
    public void setCriticalClosingAlertRatio(float criticalClosingAlertRatio) {  this.criticalClosingAlertRatio = criticalClosingAlertRatio;  }
    public void setAlertMaxFrequencySec(float maxAlertFrequencySeconds) {  this.maxAlertFrequencySeconds = maxAlertFrequencySeconds;  }
    public void setGroundAlertsEnabled(boolean groundAlertsEnabled) {  this.groundAlertsEnabled = groundAlertsEnabled;  }
    public void setMinSpeed(float minSpeed) { this.minSpeed = minSpeed; }

    /**
     * Process alert queue in a separate thread
     */
    @Override
    public void run() {
        // Wait for all sounds to load before starting alert queue processing
        final long start = System.currentTimeMillis();
        soundPlayer.waitUntilAllSoundsAreLoaded();
        System.out.println("Time to load sounds (ms): "+(System.currentTimeMillis()-start));
        // Alert queue processing loop
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (alertQueue) {
                if (alertQueue.size() > 0 && !soundPlayer.isPlaying()) {
                    final Alert alert = alertQueue.getFirst();
                    if (!lastAlertTime.containsKey(alert.trafficCallsign)
                        || (System.currentTimeMillis()-lastAlertTime.get(alert.trafficCallsign))/1000.0
                            > this.maxAlertFrequencySeconds)
                    {
                        lastAlertTime.put(alert.trafficCallsign, System.currentTimeMillis());
                        soundPlayer.playSequence(buildAlertSoundIdSequence(alertQueue.removeFirst()));
                    } else {
                        alertQueue.removeFirst();
                        alertQueue.add(Math.min(1, alertQueue.size()), alert); // Put it to second in line to wait
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
     * @param alert Alert item to build soundId sequence for
     * @return Sequence of soundId's for the soundplayer that represents the assembled alert
     */
    protected List<Integer> buildAlertSoundIdSequence(Alert alert) {
        final List<Integer> alertAudio = new ArrayList<>();
        if (alert.closingEvent != null && alert.closingEvent.isCriticallyClose)
            alertAudio.add(criticallyCloseChirp);
        alertAudio.add(topGunDorkMode ? bogeySoundId : trafficSoundId);
        if (useTrafficAliases) {
            int icaoIndex = phoneticAlphaIcaoSequenceQueue.indexOf(alert.trafficCallsign);
            if (icaoIndex == -1) {
                phoneticAlphaIcaoSequenceQueue.add(alert.trafficCallsign);
                icaoIndex = phoneticAlphaIcaoSequenceQueue.size()-1;
            }
            // TODO: double/triple/etc. id if you get to end, rather than starting over...worth it?
            alertAudio.add(trafficAliasesSoundIds[icaoIndex % trafficAliasesSoundIds.length]);
        }
        if (alert.closingEvent != null) {
            // Subtract speaking time of audio clips prior to # of seconds in this alert
            final double adjustedClosingSeconds = alert.closingEvent.closingSeconds()
                    - soundPlayer.getSoundSequenceDuration(alertAudio)/1000.00;
            if (adjustedClosingSeconds > 0) {
                alertAudio.add(closingInSoundId);
                addNumericalAlertAudioSequence(alertAudio, adjustedClosingSeconds, false);
                alertAudio.add(secondsSoundId);
            }
            alertAudio.add(withinSoundId);
            addNumericalAlertAudioSequence(alertAudio, alert.closingEvent.closestApproachDistanceNmi, this.useDecimalPrecision);
            alertAudio.add(milesSoundId);
        }
        alertAudio.add(clockHoursSoundIds[alert.clockHour - 1]);
        alertAudio.add(Math.abs(alert.altitudeDiff) < 100 ? levelSoundId
                : (alert.altitudeDiff > 0 ? lowSoundId : highSoundId));
        addNumericalAlertAudioSequence(alertAudio, alert.distanceNmi, this.useDecimalPrecision);
        alertAudio.add(milesSoundId);
        if (alert.vspeed > 100)
            alertAudio.add(climbingSoundId);
        else if (alert.vspeed < 100)
            alertAudio.add(descendingSoundId);
        else
            alertAudio.add(levelSoundId);
        return alertAudio;
    }

    /**
     * Inject an individual digit audio alert sound sequence (1,032 ==> "one-zero-three-two")
     * @param alertAudio Existing audio list to add numeric value to
     * @param numeric Numeric value to speak into alert audio
     * @param doDecimal Whether to speak 1st decimal into alert (false ==> rounded to whole #)
     */
    protected final void addNumericalAlertAudioSequence(final List<Integer> alertAudio, final double numeric, final boolean doDecimal) {
        double processedNumeric = doDecimal ? numeric : Math.round(numeric);
        for (int i = (int) Math.max(Math.log10(processedNumeric), 0); i >= 0; i--) {
            if (i == 0)
                alertAudio.add(numberSoundIds[(int) Math.min(processedNumeric % 10, 9)]);
            else {
                final double pow10 = Math.pow(10, i);
                alertAudio.add(numberSoundIds[(int) Math.min(processedNumeric / pow10, 9)]);
                processedNumeric = processedNumeric % pow10;
            }
        }
        if (doDecimal) {
            final int firstDecimal = (int) Math.min(Math.round((numeric-Math.floor(numeric))*10), 9);
            if (firstDecimal != 0) {
                alertAudio.add(decimalSoundId);
                alertAudio.add(numberSoundIds[firstDecimal]);
            }
        }
    }

    /**
     * Process ADSB traffic list and decide which traffic needs to be added to the alert queue
     * @param ownLocation Ownship location
     * @param allTraffic Traffic array
     * @param alertDistance Horizontal istance in which alert is triggered
     * @param ownAltitude Ownship altitude
     */
    public void handleAudibleAlerts(Location ownLocation, LinkedList<Traffic> allTraffic,
                                    float alertDistance, int ownAltitude, boolean ownIsAirborne)
    {
        if (ownLocation == null) {
            return; // need own location for alerts
        }
        // Don't alert for traffic taxiing on the ground, unless desired (e.g., runway incursions?)
        if (!(groundAlertsEnabled || ownIsAirborne)) {
            return;
        }
        // Don't alert if under config min speed, to prevent audible pollution during high-workload low speed activities
        if (ownLocation.getSpeed()*MPS_TO_KNOTS_CONV < minSpeed) {
            return;
        }
        // Make traffic handling loop async producer thread, to not delay caller handling loop
        getTrafficAlertProducerExecutor().execute(() -> {
            for (Traffic traffic : allTraffic) {
                if(null == traffic) {
                    continue;
                }
                // Don't alert for traffic taxiing on the ground, unless desired (e.g., runway incursions?)
                if (!(groundAlertsEnabled || traffic.mIsAirborne)) {
                    continue;
                }
                synchronized(lastDistanceUpdate) {
                    final double altDiff = ownAltitude - traffic.mAltitude;
                    final String distanceCalcUpdateKey = traffic.mCallSign.trim()+"_"+traffic.getLastUpdate() + "_" + ownLocation.getTime();
                    final String lastDistanceUpdateKey = lastDistanceUpdate.get(traffic.mCallSign);
                    double currentDistance;
                    if (
                            (lastDistanceUpdateKey == null || !lastDistanceUpdateKey.equals(distanceCalcUpdateKey))
                                    && Math.abs(altDiff) < Traffic.TRAFFIC_ALTITUDE_DIFF_DANGEROUS
                                    && (currentDistance = greatCircleDistance(
                                    ownLocation.getLatitude(), ownLocation.getLongitude(), traffic.mLat, traffic.mLon
                            )) < alertDistance
                    ) {
                        trafficAlertQueueUpsert(new Alert(traffic.mCallSign,
                                nearestClockHourFromHeadingAndLocations(ownLocation.getLatitude(),
                                        ownLocation.getLongitude(), traffic.mLat, traffic.mLon, ownLocation.getBearing()),
                                altDiff,
                                closingTimeEnabled ? determineClosingEvent(ownLocation, traffic, currentDistance) : null,
                                currentDistance, traffic.mVertVelocity
                        ));
                        lastDistanceUpdate.put(traffic.mCallSign, distanceCalcUpdateKey);
                    }
                }

            }
        });
    }

    protected Alert.ClosingEvent determineClosingEvent(final Location ownLocation, final Traffic traffic, final double currentDistance) {
        final int ownSpeedInKts = (int)Math.round(MPS_TO_KNOTS_CONV * ownLocation.getSpeed());
        final double closingEventTimeSec = Math.abs(closestApproachTime(
                traffic.mLat, traffic.mLon, ownLocation.getLatitude(), ownLocation.getLongitude(),
                traffic.mHeading, ownLocation.getBearing(), traffic.mHorizVelocity, ownSpeedInKts
        ))*60.00*60.00;
        if (closingEventTimeSec < this.closingTimeThresholdSeconds) {
            final double[] myCaLoc = locationAfterTime(ownLocation.getLatitude(), ownLocation.getLongitude(),
                    ownLocation.getBearing(), ownSpeedInKts, closingEventTimeSec/3600.000);
            final double[] theirCaLoc = locationAfterTime(traffic.mLat, traffic.mLon, traffic.mHeading,
                    traffic.mHorizVelocity, closingEventTimeSec/3600.000);
            final double caDistance = greatCircleDistance(myCaLoc[0], myCaLoc[1], theirCaLoc[0], theirCaLoc[1]);
            if (caDistance < this.closestApproachThresholdNmi && currentDistance > caDistance) {
                final boolean criticallyClose = this.criticalClosingAlertRatio > 0
                        &&(closingEventTimeSec / this.closingTimeThresholdSeconds) <= criticalClosingAlertRatio
                        && (caDistance / this.closestApproachThresholdNmi) <= criticalClosingAlertRatio;
                return new Alert.ClosingEvent(closingEventTimeSec, caDistance, criticallyClose);
            }
        }
        return null;
    }

    protected Executor getTrafficAlertProducerExecutor() {    // DI point for mocking in tests
        return trafficAlertProducerExecutor;
    }

    private void trafficAlertQueueUpsert(Alert alert) {
        synchronized (alertQueue) {
            final int alertIndex = alertQueue.indexOf(alert);
            if (alertIndex == -1) {
                // If this is a "critically close" alert, put it ahead of the first non-critically close alert
                if (alert.closingEvent != null && alert.closingEvent.isCriticallyClose) {
                    for (int i = 0; i < alertQueue.size(); i++) {
                        final Alert curAlert = alertQueue.get(i);
                        if (curAlert.closingEvent == null || !curAlert.closingEvent.isCriticallyClose) {
                            alertQueue.add(i, alert);
                            break;
                        }
                    }
                } else {
                    alertQueue.add(alert);
                }
            } else {    // if already in queue, update with the most recent data prior to speaking
                alertQueue.set(alertIndex, alert);
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

    protected static int nearestClockHourFromHeadingAndLocations(
            double lat1, double long1, double lat2, double long2,  double myBearing)
    {
        final int nearestClockHour = (int) Math.round(relativeBearingFromHeadingAndLocations(lat1, long1, lat2, long2, myBearing)/30.0);
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

    /**
     * Time to closest approach between two 2-d kinematic vectors; credit to: https://math.stackexchange.com/questions/1775476/shortest-distance-between-two-objects-moving-along-two-lines
     * @param lat1 Latitude 1
     * @param lon1 Longitude 2
     * @param lat2 Latitude 2
     * @param lon2 Longitude 2
     * @param heading1 Heading 1
     * @param heading2 Heading 2
     * @param velocity1 Velocity 1
     * @param velocity2 Velocity 2
     * @return Time (in units of velocity) of closest point of approach
     */
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
                        // Again, use cos of average lat to give some weighting based on shorter intra-lon distance changes at higher latitudes
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
        private final MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        private final List<Integer> soundIdsToLoad;
        private boolean isWaitingForSoundsToLoad = false;

        private static final float SOUND_PLAY_RATE = 1f;

        private SequentialSoundPoolPlayer(Object synchNotificationMonitor) {
            // Setting concurrent streams to 2 to allow some edge overlap for looper post-to-execution delay
            this.soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC,0);
            this.soundPool.setOnLoadCompleteListener(this);
            this.loadedSounds = new ArrayList<>();
            this.soundDurationMap = new HashMap<>();
            this.soundIdsToLoad = new ArrayList<>();
            this.synchNotificationMonitor = synchNotificationMonitor;
            this.handler = new Handler(Looper.getMainLooper());
        }

        public synchronized boolean isPlaying() {
            return isPlaying;
        }

        public synchronized int[] load(Context ctx, int... resIds) {
            final int[] soundIds = new int[resIds.length];
            for (int i = 0; i < resIds.length; i++) {
                soundIds[i] = soundPool.load(ctx, resIds[i], 1);
                soundIdsToLoad.add(soundIds[i]);
                soundDurationMap.put(soundIds[i], getSoundDuration(ctx, resIds[i]));
            }
            return soundIds;
        }

        public long getSoundSequenceDuration(List<Integer> soundIds) {
            long soundSequenceDurationMs = 0;
            for (int soundId : soundIds)
                soundSequenceDurationMs += soundDurationMap.get(soundId);
            return (long) (soundSequenceDurationMs / SOUND_PLAY_RATE);
        }

        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            synchronized (loadedSounds) {
                if (status == 0) {
                    loadedSounds.add(sampleId);
                    if (isWaitingForSoundsToLoad && loadedSounds.size() == soundIdsToLoad.size()) {
                        loadedSounds.notifyAll();
                    }
                }
            }
        }

        public synchronized void  waitUntilAllSoundsAreLoaded() {
            synchronized (loadedSounds) {
                this.isWaitingForSoundsToLoad = true;
                while (!loadedSounds.containsAll(soundIdsToLoad))
                    try {
                        loadedSounds.wait();
                    } catch (InterruptedException ie) {
                        /* Expected */
                    }
                this.isWaitingForSoundsToLoad = false;
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
         * Runnable to put on Android looper for playing each alert (sound sequence)
         */
        private static class SequentialSoundPlayRunnable implements Runnable {
            private final List<Integer> soundIds;
            private int curSoundIndex = 0;
            private final Handler handler;
            private final SoundPool soundPool;
            private final Map<Integer,Long> soundDurationMap;
            private final SoundSequenceOnCompletionListener listener;

            public SequentialSoundPlayRunnable(List<Integer> soundIds, SoundSequenceOnCompletionListener listener,
                                               Handler handler, SoundPool soundPool, Map<Integer,Long> soundDurationMap)
            {
                this.soundIds = soundIds;
                this.handler = handler;
                this.soundPool = soundPool;
                this.soundDurationMap = soundDurationMap;
                this.listener = listener;
            }

            @Override
            public void run() {
                if (curSoundIndex < soundIds.size()) {
                    final int soundId = this.soundIds.get(curSoundIndex++);
                    soundPool.play(soundId, 1, 1, 1, 0, SOUND_PLAY_RATE);
                    handler.postDelayed(this, (long) Math.ceil(soundDurationMap.get(soundId) / SOUND_PLAY_RATE));
                } else {
                    if (listener != null)
                        listener.onSoundSequenceCompletion(soundIds);
                }
            }
        }
    }
}