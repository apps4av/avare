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
import com.ds.avare.storage.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Audible ADSB nearby traffic alerts, with optional "time to closest point of approach" (TCPA) alerts
 */
public class AudibleTrafficAlerts implements Runnable {

    // Audible alert sound pool player utility
    private final SequentialSoundPoolPlayer soundPlayer;

    // Sound pool soundId's
    private final int trafficSoundId;
    private final int bogeySoundId;
    private final int closingInSoundId;
    private final int overSoundId;
    private final int lowSoundId, highSoundId, sameAltitudeSoundId;
    private final int oClockSoundId;
    private final int[] twentiesToNinetiesSoundIds;
    private final int hundredSoundId, thousandSoundId;
    private final int atSoundId;
    private final int[] alphabetSoundIds;
    protected final int[] numberSoundIds;
    private final int secondsSoundId;
    private final int milesSoundId;
    private final int climbingSoundId, descendingSoundId, levelSoundId;
    private final int criticallyCloseChirpSoundId;
    private final int withinSoundId;
    protected final int decimalSoundId;

    // Trackers for traffic callsigns, update freshness, and alert frequenncy
    private final List<String> phoneticAlphaIcaoSequenceQueue;
    private final Map<String,Long> lastCallsignAlertTime;
    private final Map<String,String> lastDistanceUpdate;
    private volatile long nextAvailableAlertTime = 0;

    // Configuration settings
    private boolean topGunDorkMode = false;
    private float maxAlertFrequencySeconds = 15f;
    protected DistanceCalloutOption distanceCalloutOption = DistanceCalloutOption.NONE;
    protected TrafficIdCalloutOption trafficIdCalloutOption = TrafficIdCalloutOption.FULL_CALLSIGN;
    private boolean verticalAttitudeCallout = false;

    // Core alert tracking data structures, threading objects, and feature instances
    private static Thread alertQueueProcessingConsumerThread;
    protected final ExecutorService trafficAlertProducerExecutor = Executors.newSingleThreadExecutor();
    private static AudibleTrafficAlerts singleton;
    // This object's monitor is used for inter-thread communication and synchronization
    private static final List<Alert> alertQueue = new ArrayList<>();

    // Constants
    private static final float MPS_TO_KNOTS_CONV = 1.0f/0.514444f;
    private static final long MIN_ALERT_SEPARATION_MS = 750;

    protected enum DistanceCalloutOption {
        NONE, INDIVIDUAL_ROUNDED, INDIVIDUAL_DECIMAL, COLLOQUIAL_ROUNDED, COLLOQUIAL_DECIMAL;
    }
    protected enum TrafficIdCalloutOption {
        NONE, PHONETIC_ALPHA_ID, FULL_CALLSIGN
    }

    protected static final class Alert {
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

        protected static final class ClosingEvent {
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
    }

    protected AudibleTrafficAlerts(Context ctx) {
        this(new SequentialSoundPoolPlayer(), ctx);
    }

    protected AudibleTrafficAlerts(SequentialSoundPoolPlayer sp, Context ctx)
    {
        this.phoneticAlphaIcaoSequenceQueue = new ArrayList<>();
        this.lastCallsignAlertTime = new HashMap<>();
        this.lastDistanceUpdate = new HashMap<>();
        this.soundPlayer = sp;
        this.trafficSoundId = sp.load(ctx, R.raw.tr_traffic)[0];
        this.bogeySoundId = sp.load(ctx, R.raw.tr_bogey)[0];
        this.alphabetSoundIds = sp.load(ctx, R.raw.tr_alpha, R.raw.tr_bravo, R.raw.tr_charlie, R.raw.tr_delta, R.raw.tr_echo,
                R.raw.tr_foxtrot, R.raw.tr_golf, R.raw.tr_hotel, R.raw.tr_india, R.raw.tr_juliet,
                R.raw.tr_kilo, R.raw.tr_lima, R.raw.tr_mike, R.raw.tr_november, R.raw.tr_oscar,
                R.raw.tr_papa, R.raw.tr_quebec, R.raw.tr_romeo, R.raw.tr_sierra, R.raw.tr_tango,
                R.raw.tr_uniform, R.raw.tr_victor, R.raw.tr_whiskey, R.raw.tr_xray, R.raw.tr_yankee,
                R.raw.tr_zulu);
        this.highSoundId = sp.load(ctx, R.raw.tr_high)[0];
        this.lowSoundId = sp.load(ctx, R.raw.tr_low)[0];
        this.sameAltitudeSoundId = sp.load(ctx, R.raw.tr_same_altitude)[0];
        this.levelSoundId = sp.load(ctx, R.raw.tr_level)[0];
        this.closingInSoundId = sp.load(ctx, R.raw.tr_cl_closingin)[0];
        this.numberSoundIds = sp.load(ctx, R.raw.tr_00, R.raw.tr_01, R.raw.tr_02, R.raw.tr_03, R.raw.tr_04, R.raw.tr_05,
                R.raw.tr_06, R.raw.tr_07, R.raw.tr_08, R.raw.tr_09, R.raw.tr_10, R.raw.tr_11,
                R.raw.tr_12, R.raw.tr_13, R.raw.tr_14, R.raw.tr_15, R.raw.tr_16, R.raw.tr_17, R.raw.tr_18,
                R.raw.tr_19);
        this.overSoundId = sp.load(ctx, R.raw.tr_cl_over)[0];
        this.criticallyCloseChirpSoundId = sp.load(ctx, R.raw.tr_cl_chirp)[0];
        this.secondsSoundId = sp.load(ctx, R.raw.tr_seconds)[0];
        this.milesSoundId = sp.load(ctx, R.raw.tr_miles)[0];
        this.climbingSoundId = sp.load(ctx, R.raw.tr_climbing)[0];
        this.descendingSoundId = sp.load(ctx, R.raw.tr_descending)[0];
        this.withinSoundId = sp.load(ctx, R.raw.tr_within)[0];
        this.decimalSoundId = sp.load(ctx, R.raw.tr_decimal)[0];
        this.oClockSoundId = sp.load(ctx, R.raw.tr_oclock)[0];
        this.twentiesToNinetiesSoundIds = sp.load(ctx, R.raw.tr_20, R.raw.tr_30, R.raw.tr_40, R.raw.tr_50,
            R.raw.tr_60, R.raw.tr_70, R.raw.tr_80, R.raw.tr_90);
        this.hundredSoundId = sp.load(ctx, R.raw.tr_100)[0];
        this.thousandSoundId = sp.load(ctx, R.raw.tr_1000)[0];
        this.atSoundId = sp.load(ctx, R.raw.tr_at)[0];
        sp.setSoundSequenceCompletionListener(new SoundSequenceOnCompletionListener() {
            @Override
            public void onSoundSequenceCompletion(List<Integer> soundIdSequence) {
                nextAvailableAlertTime = System.currentTimeMillis() + MIN_ALERT_SEPARATION_MS;
                synchronized (alertQueue) {
                    alertQueue.notifyAll();
                }
            }
        });
    }

    public void setTopGunDorkMode(final boolean topGunDorkMode) { this.topGunDorkMode = topGunDorkMode; }
    public void setAlertMaxFrequencySec(final float maxAlertFrequencySeconds) {  this.maxAlertFrequencySeconds = maxAlertFrequencySeconds;  }
    public void setDistanceCalloutOption(final String distanceCalloutOption) { this.distanceCalloutOption = DistanceCalloutOption.valueOf(distanceCalloutOption); }
    public void setVerticalAttitudeCallout (final boolean verticalAttitudeCallout) { this.verticalAttitudeCallout = verticalAttitudeCallout; }
    public void setTrafficIdCalloutOption(final String trafficIdCalloutOption) { this.trafficIdCalloutOption = TrafficIdCalloutOption.valueOf(trafficIdCalloutOption); }


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

    /**
     * Process alert queue in a separate thread
     */
    @Override
    public void run() {
        // Wait for all sounds to load before starting alert queue processing
        soundPlayer.waitUntilAllSoundsAreLoaded();
        // Alert queue processing loop
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (alertQueue) {
                try {
                    final int alertQueueSize = alertQueue.size();
                    if (alertQueueSize > 0 && !soundPlayer.isPlaying) {
                        final Alert alert = alertQueue.get(0);
                        long timeToWaitForThisCallsign = 0;
                        final long timeToWaitForAny;
                        if ((timeToWaitForAny = nextAvailableAlertTime - System.currentTimeMillis()) <= 0 // separate all alerts for clarity
                                && ((alert.closingEvent != null && alert.closingEvent.isCriticallyClose)  // critical closing events can repeat...
                                    || (!lastCallsignAlertTime.containsKey(alert.trafficCallsign)
                                    || (timeToWaitForThisCallsign = (long) (this.maxAlertFrequencySeconds * 1000.0)
                                            - (System.currentTimeMillis() - lastCallsignAlertTime.get(alert.trafficCallsign))) <= 0)))    // ...otherwise, respect config for delay between same callsign
                        {
                            lastCallsignAlertTime.put(alert.trafficCallsign, System.currentTimeMillis());
                            final long soundDuration = soundPlayer.playSequence(buildAlertSoundIdSequence(alertQueue.get(0)));
                            alertQueue.remove(0);
                            nextAvailableAlertTime = System.currentTimeMillis() + soundDuration + MIN_ALERT_SEPARATION_MS;
                            alertQueue.wait(soundDuration + MIN_ALERT_SEPARATION_MS);   // wait thread until alert finished playing
                        } else {    // need to wait, or let someone else go for now
                            if (timeToWaitForAny > 0 || (timeToWaitForThisCallsign > 0 && alertQueueSize == 1)) {
                                // Don't rattle off multiple alerts too fast, even if there are distinct callsigns, and honor desired separation between alerts from same callsign
                                final long timeToWait = Math.max(timeToWaitForAny, timeToWaitForThisCallsign);
                                nextAvailableAlertTime = System.currentTimeMillis() + timeToWait;
                                alertQueue.wait(timeToWait);
                            } else if (timeToWaitForAny <= 0 && alertQueueSize > 1) { // This one can't go, but let next in line try
                                alertQueue.remove(0);
                                alertQueue.add(Math.min(1, alertQueueSize), alert); // Put it to second in line to wait
                            }
                        }
                    } else {
                        // No-one to process now, so wait for notification of queue update from producer
                        alertQueue.wait();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Ensure status is persisted
                    // Proceed top top and let thread exit
                }
            }
        }
    }

    /**
     * Construct soundId sequence based on alert properties and preference configuration
     * @param alert Alert item to build soundId sequence for
     * @return Sequence of soundId's for the soundplayer that represents the assembled alert
     */
    protected final List<Integer> buildAlertSoundIdSequence(Alert alert) {
        final List<Integer> alertAudio = new ArrayList<>();
        if (alert.closingEvent != null && alert.closingEvent.isCriticallyClose)
            alertAudio.add(criticallyCloseChirpSoundId);
        alertAudio.add(this.topGunDorkMode ? bogeySoundId : trafficSoundId);
        switch (this.trafficIdCalloutOption) {
            case PHONETIC_ALPHA_ID:
                addPhoneticAlphaTrafficIdAudio(alertAudio, alert.trafficCallsign);
                break;
            case FULL_CALLSIGN:
                addFullCallsignTrafficIdAudio(alertAudio, alert.trafficCallsign);
        }
        if (alert.closingEvent != null) {
            addTimeToClosestPointOfApproachAudio(alertAudio, alert.closingEvent);
        }
        addPositionAudio(alertAudio, alert.clockHour, alert.altitudeDiff);
        if (this.distanceCalloutOption != DistanceCalloutOption.NONE) {
            addDistanceAudio(alertAudio, alert.distanceNmi);
        }
        if (this.verticalAttitudeCallout && alert.vspeed != Integer.MAX_VALUE /* Indeterminate value */) {
            addVerticalAttitudeAudio(alertAudio, alert.vspeed);
        }
        return alertAudio;
    }

    private void addTimeToClosestPointOfApproachAudio(final List<Integer> alertAudio, final Alert.ClosingEvent closingEvent) {
        if (addClosingSecondsAudio(alertAudio, closingEvent.closingSeconds())) {
            if (this.distanceCalloutOption != DistanceCalloutOption.NONE) {
                alertAudio.add(withinSoundId);
                addDistanceAudio(alertAudio, closingEvent.closestApproachDistanceNmi);
            }
        }
    }

    private void addDistanceAudio(final List<Integer> alertAudio, final double distance) {
        switch (this.distanceCalloutOption) {
            case COLLOQUIAL_DECIMAL:
            case INDIVIDUAL_DECIMAL:
                addNumericalAlertAudio(alertAudio, distance, true);
                break;
            default:
                addNumericalAlertAudio(alertAudio, distance, false);
        }
        alertAudio.add(milesSoundId);
    }

    private void addVerticalAttitudeAudio(final List<Integer> alertAudio, final int vspeed) {
        if (Math.abs(vspeed) < 100)
            alertAudio.add(levelSoundId);
        else if (vspeed >= 100)
            alertAudio.add(climbingSoundId);
        else if (vspeed <= -100)
            alertAudio.add(descendingSoundId);
    }

    private void addPositionAudio(final List<Integer> alertAudio, final int clockHour, final double altitudeDiff) {
        alertAudio.add(atSoundId);
        alertAudio.add(numberSoundIds[clockHour]);
        alertAudio.add(oClockSoundId);
        alertAudio.add(Math.abs(altitudeDiff) < 100 ? sameAltitudeSoundId
                : (altitudeDiff > 0 ? lowSoundId : highSoundId));
    }

    private boolean addClosingSecondsAudio(final List<Integer> alertAudio, final double closingSeconds) {
        // Subtract speaking time of audio clips, and computation thereof, prior to # of seconds in this alert
        final double adjustedClosingSeconds = closingSeconds - (soundPlayer.getPartialSoundSequenceDuration(alertAudio)+100)/1000.00;
        if (adjustedClosingSeconds > 0) {
            alertAudio.add(closingInSoundId);
            addNumericalAlertAudio(alertAudio, adjustedClosingSeconds, false);
            alertAudio.add(secondsSoundId);
            return true;
        }
        return false;
    }

    private void addPhoneticAlphaTrafficIdAudio(final List<Integer> alertAudio, final String callsign) {
        int icaoIndex = phoneticAlphaIcaoSequenceQueue.indexOf(callsign);
        if (icaoIndex == -1) {
            phoneticAlphaIcaoSequenceQueue.add(callsign);
            icaoIndex = phoneticAlphaIcaoSequenceQueue.size()-1;
        }
        alertAudio.add(alphabetSoundIds[icaoIndex % alphabetSoundIds.length]);
    }

    private void addFullCallsignTrafficIdAudio(final List<Integer> alertAudio, final String callsign) {
        final String normalizedCallsign = callsign.toUpperCase(Locale.ROOT);
        for (int i = 0; i < normalizedCallsign.length(); i++) {
            final char c = normalizedCallsign.charAt(i);
            if (c <= '9' && c >= '0')
                alertAudio.add(this.numberSoundIds[c-'0']);
            else if (c >= 'A' && c <= 'Z')
                alertAudio.add(this.alphabetSoundIds[c-'A']);
        }
    }

    /**
     * Inject an individual digit audio alert sound sequence (1,032 ==> "one-zero-three-two")
     * @param alertAudio Existing audio list to add numeric value to
     * @param numeric Numeric value to speak into alert audio
     * @param doDecimal Whether to speak 1st decimal into alert (false ==> rounded to whole #)
     */
    protected final void addNumericalAlertAudio(final List<Integer> alertAudio, final double numeric, final boolean doDecimal) {
        switch (this.distanceCalloutOption) {
            case COLLOQUIAL_DECIMAL:
            case COLLOQUIAL_ROUNDED:
                addColloquialNumericBaseAlertAudio(alertAudio, doDecimal ? numeric : Math.round(numeric));
                break;
            default:
                addNumberSequenceNumericBaseAlertAudio(alertAudio, doDecimal ? numeric : Math.round(numeric));
        }
        if (doDecimal) {
            addFirstDecimalAlertAudioSequence(alertAudio, numeric);
        }
    }

    /**
     * Speak a number in digit-by-digit format (1962 ==> "one nine six two")
     * @param alertAudio List of soundId to append to
     * @param numeric Numeric value to speak into alertAudio
     */
    private final void addNumberSequenceNumericBaseAlertAudio(final List<Integer> alertAudio, final double numeric) {
        double curNumeric = numeric;    // iteration variable for digit processing
        for (int i = (int) Math.max(Math.log10(numeric), 0); i >= 0; i--) {
            if (i == 0)
                alertAudio.add(numberSoundIds[(int) Math.min(curNumeric % 10, 9)]);
            else {
                final double pow10 = Math.pow(10, i);
                alertAudio.add(numberSoundIds[(int) Math.min(curNumeric / pow10, 9)]);
                curNumeric = curNumeric % pow10;
            }
        }
    }

    /**
     * Speak a number in colloquial format (1962 ==> "one thousand nine hundred sixty-two")
     * @param alertAudio List of soundId to append to
     * @param numeric Numeric value to speak into alertAudio
     */
    private final void addColloquialNumericBaseAlertAudio(final List<Integer> alertAudio, final double numeric) {
        double curNumeric = numeric;    // iteration variable for digit processing
        for (int i = (int) Math.max(Math.log10(numeric), 0); i >= 0; i--) {
            if (i == 0
                // Only speak "zero" if it is only zero (not part of tens/hundreds/thousands)
                && ((int)(curNumeric % 10) != 0 || ((int) Math.max(Math.log10(numeric), 0)) == 0))
            {
                alertAudio.add(numberSoundIds[(int) Math.min(curNumeric % 10, 9)]);
            } else {
                if (i > 3) {
                    alertAudio.add(overSoundId);
                    alertAudio.addAll(Arrays.asList(
                            numberSoundIds[9], thousandSoundId, numberSoundIds[9], hundredSoundId, twentiesToNinetiesSoundIds[9 - 2], numberSoundIds[9]));
                    return;
                } else {
                    final double pow10 = Math.pow(10, i);
                    final int digit = (int) Math.min(curNumeric / pow10, 9);
                    if (i == 1 && digit == 1) {             // tens/teens
                        alertAudio.add(numberSoundIds[10 + (int) curNumeric % 10]);
                        return;
                    } else {
                        if (i == 1 && digit != 0) {         // twenties/thirties/etc.
                            alertAudio.add(twentiesToNinetiesSoundIds[digit-2]);
                        } else if (i == 2 && digit != 0) {  // hundreds
                            alertAudio.add(numberSoundIds[digit]);
                            alertAudio.add(hundredSoundId);
                        } else if (i == 3 && digit != 0) {  // thousands
                            alertAudio.add(numberSoundIds[digit]);
                            alertAudio.add(thousandSoundId);
                        }
                        curNumeric = curNumeric % pow10;
                    }
                }
            }
        }
    }

    private void addFirstDecimalAlertAudioSequence(final List<Integer> alertAudio, final double numeric) {
        final int firstDecimal = (int) Math.min(Math.round((numeric-Math.floor(numeric))*10), 9);
        if (firstDecimal != 0) {
            alertAudio.add(decimalSoundId);
            alertAudio.add(numberSoundIds[firstDecimal]);
        }
    }

    /**
     * Process ADSB traffic list and decide which traffic needs to be added to the alert queue
     * @param ownLocation Ownship location
     * @param allTraffic Traffic array
     * @param pref App preferences object
     * @param ownAltitude Ownship altitude
     * @param ownVspeed Ownship vertical speed (climb/descent fpm)
     */
    public void handleAudibleAlerts(final Location ownLocation, final LinkedList<Traffic> allTraffic,
            final Preferences pref, final int ownAltitude, final boolean ownIsAirborne, final int ownVspeed)
    {
        if (ownLocation == null) {
            return; // need own location for alerts
        }
        // Don't alert for traffic when we are taxiing on the ground, unless desired (e.g., runway incursions?)
        final boolean isAudibleGroundAlertsEnabled = pref.isAudibleGroundAlertsEnabled();
        if (!(ownIsAirborne || isAudibleGroundAlertsEnabled)) {
            return;
        }
        // Don't alert if under config min speed, to prevent audible pollution during high-workload low speed activities
        if (ownLocation.getSpeed()*MPS_TO_KNOTS_CONV < pref.getAudibleTrafficAlertsMinSpeed()) {
            return;
        }
        // Pull all preferences needed by executor lambda into final vars, to allow fast GC of lambda
        final float trafficAlertsDistanceMinimum = pref.getAudibleTrafficAlertsDistanceMinimum();
        final float trafficAlertsAltitude = pref.getAudibleTrafficAlertsAltitude();
        final boolean isAudibleClosingAlerts = pref.isAudibleClosingInAlerts();
        final String ownTailNumber = ifNullElse(pref.getAircraftTailNumber(), "ownship12349").trim();
        final float closingAlertsDistanceMinimum, closingAlertsCriticalAlertRatio, closingAlertsAltitude;
        final int closingAlertsSeconds;
        if (isAudibleClosingAlerts) {
            closingAlertsDistanceMinimum = pref.getAudibleClosingInAlertDistanceNmi();
            closingAlertsCriticalAlertRatio = pref.getAudibleClosingInCriticalAlertRatio();
            closingAlertsAltitude = pref.getAudibleClosingAlertsAltitude();
            closingAlertsSeconds = pref.getAudibleClosingInAlertSeconds();
        } else {
            closingAlertsDistanceMinimum = 0;
            closingAlertsCriticalAlertRatio = 0;
            closingAlertsAltitude = 0;
            closingAlertsSeconds = 0;
        }
        // Make traffic handling loop async producer thread, to not delay caller handling loop
        getTrafficAlertProducerExecutor().execute(() -> {
			synchronized(lastDistanceUpdate) {	// in case calls get overlaid
				for (Traffic traffic : allTraffic) {
					if (null == traffic) {
						continue;
					}
					// Don't alert for traffic that is taxiing on the ground, unless desired (e.g., runway incursions?)
					if (!(traffic.mIsAirborne || isAudibleGroundAlertsEnabled)) {
						continue;
					}
                    // Make a good-faith effort to filter ownship "ghost" audible alerts by comparing ownship and traffic callsigns/n-numbers
                    if (traffic.mCallSign != null && traffic.mCallSign.trim().equalsIgnoreCase(ownTailNumber)) {
                        continue;
                    }
					final double altDiff = ownAltitude - traffic.mAltitude;
					final String distanceCalcUpdateKey = traffic.mCallSign + "_" + traffic.getLastUpdate() + "_" + ownLocation.getTime();
					final String lastDistanceUpdateKey = lastDistanceUpdate.get(traffic.mCallSign);
					final double currentDistance;
					if (
						(lastDistanceUpdateKey == null || !lastDistanceUpdateKey.equals(distanceCalcUpdateKey))
						// traffic is within configured "cylinder" of audible alert (radius & height/alt)
						&& Math.abs(altDiff) < trafficAlertsAltitude
						&& (currentDistance = greatCircleDistance(
							ownLocation.getLatitude(), ownLocation.getLongitude(), traffic.mLat, traffic.mLon
						)) < trafficAlertsDistanceMinimum
					) {
						upsertTrafficAlertQueue(new Alert(traffic.mCallSign,
								nearestClockHourFromHeadingAndLocations(ownLocation.getLatitude(),
										ownLocation.getLongitude(), traffic.mLat, traffic.mLon, ownLocation.getBearing()),
								altDiff,
								isAudibleClosingAlerts
										? determineClosingEvent(ownLocation, traffic, currentDistance,
											ownAltitude, ownVspeed, closingAlertsSeconds, closingAlertsDistanceMinimum,
											closingAlertsCriticalAlertRatio, closingAlertsAltitude)
										: null,
								currentDistance, traffic.mVertVelocity
						));
						lastDistanceUpdate.put(traffic.mCallSign, distanceCalcUpdateKey);
					}
				}
			}
        });
    }

    private static <T> T ifNullElse(final T val, final T defaultVal) {
        return val == null ? defaultVal : val;
    }

    protected final Alert.ClosingEvent determineClosingEvent(final Location ownLocation, final Traffic traffic, final double currentDistance,
        final int ownAltitude, final int ownVspeed, final int closingTimeThresholdSeconds, final float closestApproachThresholdNmi,
        final float criticalClosingAlertRatio, final float closingAlertAltitude)
    {
        final int ownSpeedInKts = Math.round(MPS_TO_KNOTS_CONV * ownLocation.getSpeed());
        final double closingEventTimeSec = Math.abs(closestApproachTime(
                traffic.mLat, traffic.mLon, ownLocation.getLatitude(), ownLocation.getLongitude(),
                traffic.mHeading, ownLocation.getBearing(), traffic.mHorizVelocity, ownSpeedInKts
        ))*60.00*60.00;
        if (closingEventTimeSec < closingTimeThresholdSeconds) {
            final double[] myCaLoc = locationAfterTime(ownLocation.getLatitude(), ownLocation.getLongitude(),
                    ownLocation.getBearing(), ownSpeedInKts, closingEventTimeSec/3600.000, ownAltitude, ownVspeed);
            final double[] theirCaLoc = locationAfterTime(traffic.mLat, traffic.mLon, traffic.mHeading,
                    traffic.mHorizVelocity, closingEventTimeSec/3600.000, traffic.mAltitude, traffic.mVertVelocity);
            final double caDistance;
            final double altDiff = myCaLoc[2] - theirCaLoc[2];
            // If traffic will be within configured "cylinder" of closing/TCPA alerts, create a closing event
            if (Math.abs(altDiff) < closingAlertAltitude
                    && (caDistance = greatCircleDistance(myCaLoc[0], myCaLoc[1], theirCaLoc[0], theirCaLoc[1])) < closestApproachThresholdNmi
                    && currentDistance > caDistance)    // catches cases when moving away
            {
                final boolean criticallyClose = criticalClosingAlertRatio > 0
                        &&(closingEventTimeSec / closingTimeThresholdSeconds) <= criticalClosingAlertRatio
                        && (caDistance / closestApproachThresholdNmi) <= criticalClosingAlertRatio;
                return new Alert.ClosingEvent(closingEventTimeSec, caDistance, criticallyClose);
            }
        }
        return null;
    }

    protected Executor getTrafficAlertProducerExecutor() {    // DI point for mocking in tests
        return trafficAlertProducerExecutor;
    }

    private void upsertTrafficAlertQueue(final Alert alert) {
        synchronized (alertQueue) {
            final int alertIndex = alertQueue.indexOf(alert);
            if (alertIndex == -1) {
                // If this is a "critically close" alert, put it ahead of the first non-critically close alert
                final int alertQueueSize;
                if (alert.closingEvent != null && alert.closingEvent.isCriticallyClose && (alertQueueSize = alertQueue.size()) > 0) {
                    for (int i = 0; i < alertQueueSize; i++) {
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
            if (!soundPlayer.isPlaying && System.currentTimeMillis() > nextAvailableAlertTime) // Don't wake up consumer if he can't work now anyway
                alertQueue.notifyAll();
        }
    }

    protected static double angleFromCoordinate(final double lat1, final double long1, final double lat2, final double long2) {
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
            final double lat1, final double long1, final double lat2, final double long2, final double myBearing)
    {
        final int nearestClockHour = (int) Math.round(relativeBearingFromHeadingAndLocations(lat1, long1, lat2, long2, myBearing)/30.0);
        return nearestClockHour != 0 ? nearestClockHour : 12;
    }

    protected static double relativeBearingFromHeadingAndLocations(final double lat1, final double long1,
                               final double lat2, final double long2,  final double myBearing)
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
    private static double greatCircleDistance(final double lat1, final double lon1, final double lat2, final double lon2) {
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
        final double angle2 = 2.0 * Math.asin(Math.min(1, Math.sqrt(a)));

        // convert back to degrees, and each degree on a great circle of Earth is 60 nautical miles
        return 60.0 * Math.toDegrees(angle2);
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
    protected static double closestApproachTime(final double lat1, final double lon1, final double lat2, final double lon2,
                                           final float heading1, final float heading2, final int velocity1, final int velocity2)
    {
        // Use cosine of average of two latitudes, to give some weighting for lesser intra-lon distance at higher latitudes
        final double a = (lon2 - lon1) * (60.0000 * Math.cos(Math.toRadians((lat1+lat2)/2.0000)));
        final double b = velocity2*Math.sin(Math.toRadians(heading2)) - velocity1*Math.sin(Math.toRadians(heading1));
        final double c = (lat2 - lat1) * 60.0000;
        final double d = velocity2*Math.cos(Math.toRadians(heading2)) - velocity1*Math.cos(Math.toRadians(heading1));

        return - ((a*b + c*d) / (b*b + d*d));
    }

    protected static double[] locationAfterTime(final double lat, final double lon, final float heading, final float velocityInKt, final double timeInHrs, final float altInFeet, final float vspeedInFpm) {
        final double newLat =  lat + Math.cos(Math.toRadians(heading)) * (velocityInKt/60.00000) * timeInHrs;
        return new double[]  {
                newLat,
                lon + Math.sin(Math.toRadians(heading))
                        // Again, use cos of average lat to give some weighting based on shorter intra-lon distance changes at higher latitudes
                        * (velocityInKt / (60.00000*Math.cos(Math.toRadians((newLat+lat)/2.0000))))
                        * timeInHrs,
                altInFeet + (vspeedInFpm * (60.0 * timeInHrs))
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
        private volatile boolean isPlaying = false;
        private final MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        private final List<Integer> soundIdsToLoad;
        private SoundSequenceOnCompletionListener listener;
        private boolean isWaitingForSoundsToLoad = false;


        private static final float SOUND_PLAY_RATE = 1f;
        private static final double OVERLAP_RATIO = 0.94;  // allows more natural flow of phrases

        private SequentialSoundPoolPlayer() {
            // Setting concurrent streams to 2 to allow for overlap ratio and looper post-to-execution delay
            this.soundPool = new SoundPool(2, AudioManager.STREAM_NOTIFICATION,0);
            this.soundPool.setOnLoadCompleteListener(this);
            this.loadedSounds = new ArrayList<>();
            this.soundDurationMap = new HashMap<>();
            this.soundIdsToLoad = new ArrayList<>();
            this.handler = new Handler(Looper.getMainLooper());
        }

        public void setSoundSequenceCompletionListener(SoundSequenceOnCompletionListener listener) {
            this.listener = listener;
        }

        protected int[] load(Context ctx, int... resIds) {
            final int[] soundIds = new int[resIds.length];
            for (int i = 0; i < resIds.length; i++) {
                soundIds[i] = soundPool.load(ctx, resIds[i], 1);
                soundIdsToLoad.add(soundIds[i]);
                soundDurationMap.put(soundIds[i], getSoundDuration(ctx, resIds[i]));
            }
            return soundIds;
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

        public final void waitUntilAllSoundsAreLoaded() {
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
            isPlaying = false;
            if (this.listener != null)
                listener.onSoundSequenceCompletion(soundIdSequence);
        }

        public final long playSequence(List<Integer> soundIds) {
            if (isPlaying) {
                return 0;
            }
            isPlaying = true;
            final SequentialSoundPlayRunnable spRunnable = new SequentialSoundPlayRunnable(soundIds, this, handler, soundPool, soundDurationMap);
            handler.post(spRunnable);
            return spRunnable.totalDuration;
        }

        protected long getPartialSoundSequenceDuration(List<Integer> soundIds) {
            long soundSequenceDurationMs = 0;
            for (int soundId : soundIds)
                soundSequenceDurationMs += soundDurationMap.get(soundId);
            return (long) ((soundSequenceDurationMs / SOUND_PLAY_RATE) * OVERLAP_RATIO);
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
            private final SoundSequenceOnCompletionListener listener;
            private final long[] delayDurations;
            private long totalDuration;

            public SequentialSoundPlayRunnable(List<Integer> soundIds, SoundSequenceOnCompletionListener listener,
                                               Handler handler, SoundPool soundPool, Map<Integer,Long> soundDurationMap)
            {
                this.soundIds = soundIds;
                this.handler = handler;
                this.soundPool = soundPool;
                this.listener = listener;
                // Pre-load durations to prevent map-access time and delay math from causing audio delays
                final int soundCount = soundIds.size();
                this.delayDurations = new long[soundCount];
                for (int i = 0; i < soundCount; i++) {
                    this.delayDurations[i] = (long) (i == soundCount-1 // Nothing to overlap for last sound in sequence
                            ? Math.ceil(soundDurationMap.get(soundIds.get(i)) / SOUND_PLAY_RATE)
                            : Math.ceil(soundDurationMap.get(soundIds.get(i)) / SOUND_PLAY_RATE * OVERLAP_RATIO));
                    this.totalDuration += this.delayDurations[i];
                }
            }

            @Override
            public void run() {
                if (curSoundIndex < soundIds.size()) {
                    soundPool.play(this.soundIds.get(curSoundIndex), 1, 1, 1, 0, SOUND_PLAY_RATE);
                    handler.postDelayed(this, delayDurations[curSoundIndex++]);
                } else {
                    if (listener != null)
                        listener.onSoundSequenceCompletion(soundIds);
                }
            }
        }
    }
}