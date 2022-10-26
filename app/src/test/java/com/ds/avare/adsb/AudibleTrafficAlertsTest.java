package com.ds.avare.adsb;


import android.content.Context;
import android.location.Location;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

import com.ds.avare.adsb.gdl90.Message;
import com.ds.avare.storage.Preferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudibleTrafficAlertsTest {

    @Test
    public void angleFromCoordinate_straightUp() {
        Assert.assertEquals(0, AudibleTrafficAlerts.angleFromCoordinate(0, 0, 90, 0), 0);
    }

    @Test
    public void angleFromCoordinate_east() {
        Assert.assertEquals(90, AudibleTrafficAlerts.angleFromCoordinate(0, 0, 0, 45), 0);
    }

    @Test
    public void angleFromCoordinate_west() {
        Assert.assertEquals(270, AudibleTrafficAlerts.angleFromCoordinate(0, 90, 0, 45), 0);
    }

    @Test
    public void angleFromCoordinate_diagonal() {
        Assert.assertEquals(225, AudibleTrafficAlerts.angleFromCoordinate(43.5439, -96.730, 42.57, -98.0421), 0.5);
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_east() {
        Assert.assertEquals(3,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        0, 0, 0, 45, 0));
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_nearEnoughToEastRounded() {
        Assert.assertEquals(3,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        0, 0, 0, 45, 5));
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_straightBehindFromAnAngle() {
        Assert.assertEquals(6,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        43.5439, -96.730, 42.57, -98.0421, 44.999));
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_oneMinuteToMidnightRounded() {
        Assert.assertEquals(12,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        43.5439, -96.730, 44.8402, -96.7621, 0));
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_oneMinutePastMidnightRounded() {
        Assert.assertEquals(12,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        43.5439, -96.730, 48.034, -96.654, 0));
    }

    @Test
    public void nearestClockHourFromHeadingAndLocations_nearlySouthAndIFacingSouth() {
        Assert.assertEquals(12,
                AudibleTrafficAlerts.nearestClockHourFromHeadingAndLocations(
                        43.5439, -96.730, 39.5718, -96.735, 180));
    }

    @Test
    public void closestApproachTime_eastWestHeadOn() {
        final double lat1 = 45, lat2 = 45, lon1 = -95, lon2 = -94;
        final int velocity1 = 60, velocity2 = 60;
        final float heading1 = 270, heading2 = 90;
        final double caTime = AudibleTrafficAlerts.closestApproachTime(lat1, lon1, lat2, lon2, heading1, heading2, velocity1, velocity2);
        Assert.assertEquals("Closest approach seconds", .35, Math.abs(caTime), .1);
    }

    @Test
    public void closestApproachTime_northSouthHeadOn() {
        final double lat1 = 45, lat2 = 46, lon1 = -95, lon2 = -95;
        final int velocity1 = 60, velocity2 = 60;
        final float heading1 = 180, heading2 = 0;
        final double caTime = AudibleTrafficAlerts.closestApproachTime(lat1, lon1, lat2, lon2, heading1, heading2, velocity1, velocity2);
        Assert.assertEquals("Closest approach seconds", .5, Math.abs(caTime), .1);
    }

    @Test
    public void locationAfterTime_northAt60ktsIsOneDegreeLat() {
        final double lat = 41, lon = -95, time = 1 /* hour */;
        final float velocity = 60; /* knots */
        final float heading = 0; /* North */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading, velocity, time,200, 20);
        Assert.assertEquals("new lat", 42, latLonOverTime[0], 0.1);
        Assert.assertEquals("new lon the same", lon, latLonOverTime[1], 0.1);
    }

    @Test
    public void locationAfterTime_eastAt60ktsNearEquatorIsAboutOneDegreeLon() {
        final double lat = 10, lon = -95, time = 1 /* hour */;
        final float velocity = 60; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading, velocity, time, 200, 20);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertEquals("new lon", -94, latLonOverTime[1], 0.1);
    }

    @Test
    public void locationAfterTime_eastForAShortTimeSlowlyShowsSomeMovement() {
        final double lat = 41.184505, lon = -95.948730, time = .008952 /* hour */;
        final float velocity = 37; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading, velocity, time, 200, 20);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertNotEquals("new lon", lon, latLonOverTime[1], 0.0000001);
    }

    @Test
    public void locationAfterTime_eastAt60ktsNearEquatorIsAboutOneDegreeLonHalfHour() {
        final double lat = 10, lon = -95, time = .5 /* hour */;
        final float velocity = 60; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading, velocity, time, 20, 20);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertEquals("new lon", -94.5, latLonOverTime[1], 0.1);
    }

    @Test
    public void buildAlertSoundIdSequence_numericListPrefSet_closingEventLessThanHalfSecond_PicksFirstMedia() {
        AudibleTrafficAlerts ata = getTestAudibleTrafficAlerts(10);
        ata.distanceCalloutOption = AudibleTrafficAlerts.DistanceCalloutOption.INDIVIDUAL_DECIMAL;
        Location mockLoc = getMockLocation(41.3,-95.4, 200.0f);
        AudibleTrafficAlerts.Alert alert = new AudibleTrafficAlerts.Alert(
                "abc123", 2, 75,
                new AudibleTrafficAlerts.Alert.ClosingEvent(.25, 1.0, false), 99.9f, 10
        );
        List<Integer> media = ata.buildAlertSoundIdSequence(alert);
        final int firstMedia = ata.numberSoundIds[0];
        Assert.assertTrue("First media ["+firstMedia+"] used: "+media, media.contains(firstMedia));
    }

    @Test
    public void handleAudibleAlerts_handleTrafficRunnableIsGarbageCollected() {
        AudibleTrafficAlerts spyAta = spy(getTestAudibleTrafficAlerts(10));
        CapturingSingleThreadExecutor capEx = new CapturingSingleThreadExecutor();
        doReturn(capEx).when(spyAta).getTrafficAlertProducerExecutor();
        spyAta.handleAudibleAlerts(
                getMockLocation(45, 46, 270), new LinkedList<Traffic>(), mock(Preferences.class), 2200, true, 20);
        WeakReference<Runnable> runnableRef = new WeakReference<>(capEx.runnables.get(0));
        capEx.runnables.clear();
        forceGc();
        Assert.assertNull("Reference to runnable after GC", runnableRef.get());
    }

    @Test
    public void handleAudibleAlerts_nullLocationDoesNotCauseRunnableExecutionOrError() {
        AudibleTrafficAlerts spyAta = spy(getTestAudibleTrafficAlerts(10));
        CapturingSingleThreadExecutor capEx = new CapturingSingleThreadExecutor();
        doReturn(capEx).when(spyAta).getTrafficAlertProducerExecutor();
        LinkedList<Traffic> someTraffic = new LinkedList<>();
        Traffic t = new Traffic();
        t.mIsAirborne = true;
        someTraffic.add(t);
        spyAta.handleAudibleAlerts(
                null, someTraffic, mock(Preferences.class), 2200, true, 20);
        Assert.assertEquals("Executed runnables", 0, capEx.runnables.size());
    }

    @Test
    public void addNumericalAlertAudioSequence_numericListPrefSet_largeNumberWithDecimal() {
        final AudibleTrafficAlerts ata = getTestAudibleTrafficAlerts(10);
        ata.distanceCalloutOption = AudibleTrafficAlerts.DistanceCalloutOption.INDIVIDUAL_DECIMAL;
        final ArrayList<Integer> soundIds = new ArrayList<>();
        ata.addNumericalAlertAudioSequence(soundIds, 1049.99, true);
        Assert.assertEquals("SoundIds from number",
                Arrays.asList(ata.numberSoundIds[1], ata.numberSoundIds[0], ata.numberSoundIds[4], ata.numberSoundIds[9], ata.decimalSoundId, ata.numberSoundIds[9]),
                soundIds);
    }

    private AudibleTrafficAlerts getTestAudibleTrafficAlerts(int secondsCount) {
        final int[] seconds = new int[secondsCount];
        for (int i = 0; i < secondsCount; i++)
            seconds[i] = 2000 + i;
        return new AudibleTrafficAlerts(getMockSoundPlayer(), mock(Context.class));
    }


    private Location getMockLocation(double latitude, double longitude, float bearing) {
        final Location mockLoc = mock(Location.class);
        when(mockLoc.getLatitude()).thenReturn(latitude);
        when(mockLoc.getLongitude()).thenReturn(longitude);
        when(mockLoc.getBearing()).thenReturn(bearing);
        return mockLoc;
    }

    private void forceGc() {
        //allocate quite some memory to make sure that the GC runs
        byte[] bigChunkOfMemory = new byte[4000000];
        System.gc();
    }

    private AudibleTrafficAlerts.SequentialSoundPoolPlayer getMockSoundPlayer() {
        AudibleTrafficAlerts.SequentialSoundPoolPlayer sp = mock(AudibleTrafficAlerts.SequentialSoundPoolPlayer.class);
        when(sp.load(any(), any()))
                .thenAnswer(invocation -> {
                    int[] regurg = new int[invocation.getArguments().length-1];
                    for (int i = 1; i < invocation.getArguments().length; i++)
                        regurg[i-1] = (int) invocation.getArgument(i);
                    return regurg;
                });
        return sp;
    }

    private static class CapturingSingleThreadExecutor implements Executor {
        private ArrayList<Runnable> runnables = new ArrayList<>();
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        @Override
        public void execute(Runnable r) {
            runnables.add(r);
            executor.execute(r);
        }
    }
}