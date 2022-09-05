package com.ds.avare.adsb;


import android.content.Context;
import android.location.Location;
import android.media.MediaPlayer;

import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

public class AudibleTrafficAlertsTest {

    @Test
    public void angleFromCoordinate_strightUp() {
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
    public void angleFromCoordinate_diaganal() {
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
        final int velo1 = 60, velo2 = 60;
        final float heading1 = 270, heading2 = 90;
        final double caTime = AudibleTrafficAlerts.closestApproachTime(lat1, lon1, lat2, lon2, heading1, heading2, velo1, velo2);
        Assert.assertEquals("Closest approach seconds", .35, Math.abs(caTime), .1);
    }

    @Test
    public void closestApproachTime_northSouthHeadOn() {
        final double lat1 = 45, lat2 = 46, lon1 = -95, lon2 = -95;
        final int velo1 = 60, velo2 = 60;
        final float heading1 = 180, heading2 = 0;
        final double caTime = AudibleTrafficAlerts.closestApproachTime(lat1, lon1, lat2, lon2, heading1, heading2, velo1, velo2);
        Assert.assertEquals("Closest approach seconds", .5, Math.abs(caTime), .1);
    }

    @Test
    public void locationAfterTime_northAt60ktsIsOneDegreeLat() {
        final double lat = 41, lon = -95, time = 1 /* hour */;
        final float velo = 60; /* knots */
        final float heading = 0; /* North */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading,velo, time);
        Assert.assertEquals("new lat", 42, latLonOverTime[0], 0.1);
        Assert.assertEquals("new lon the same", lon, latLonOverTime[1], 0.1);
    }

    @Test
    public void locationAfterTime_eastAt60ktsNearEquatorIsAboutOneDegreeLon() {
        final double lat = 10, lon = -95, time = 1 /* hour */;
        final float velo = 60; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading,velo, time);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertEquals("new lon", -94, latLonOverTime[1], 0.1);
    }

    @Test
    public void locationAfterTime_eastForAShortTimeSlowlyShowsSomeMovement() {
        final double lat = 41.184505, lon = -95.948730, time = .008952 /* hour */;
        final float velo = 37; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading,velo, time);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertNotEquals("new lon", lon, latLonOverTime[1], 0.0000001);
    }

    @Test
    public void locationAfterTime_eastAt60ktsNearEquatorIsAboutOneDegreeLonHalfHour() {
        final double lat = 10, lon = -95, time = .5 /* hour */;
        final float velo = 60; /* knots */
        final float heading = 90; /* East */
        double[] latLonOverTime = AudibleTrafficAlerts.locationAfterTime(lat, lon, heading,velo, time);
        Assert.assertEquals("new lat the same", lat, latLonOverTime[0], 0.0);
        Assert.assertEquals("new lon", -94.5, latLonOverTime[1], 0.1);
    }

    @Test
    public void buildAlertSoundIdSequence_closingEventBeyondAlertRange_PicksLastMedia() {
        AudibleTrafficAlerts ata = getMockedAudibleTrafficAlerts(10);
        Location mockLoc = getMockLocation(41.3,-95.4, 200.0f);
        AudibleTrafficAlerts.AlertItem alert = new AudibleTrafficAlerts.AlertItem(
                new Traffic("abc123", 1234, 41.23f, -95.32f, 2300, 315, 300, System.currentTimeMillis()),
                mockLoc, 2225,
                new AudibleTrafficAlerts.ClosingEvent(67, 1.0)
        );
        List<Integer> media = ata.buildAlertSoundIdSequence(alert);
        Assert.assertTrue("Last media used", media.contains(ata.closingInSecondsSoundIds[ata.closingInSecondsSoundIds.length-1]));
    }

    @Test
    public void buildAlertSoundIdSequence_closingEventLessThanHalfSecond_PicksFirstMedia() {
        AudibleTrafficAlerts ata = getMockedAudibleTrafficAlerts(10);
        Location mockLoc = getMockLocation(41.3,-95.4, 200.0f);
        AudibleTrafficAlerts.AlertItem alert = new AudibleTrafficAlerts.AlertItem(
                new Traffic("abc123", 1234, 41.23f, -95.32f, 2300, 315, 300, System.currentTimeMillis()),
                mockLoc, 2225,
                new AudibleTrafficAlerts.ClosingEvent(.25, 1.0)
        );
        List<Integer> media = ata.buildAlertSoundIdSequence(alert);
        Assert.assertTrue("First media used", media.contains(ata.closingInSecondsSoundIds[0]));
    }

    private AudibleTrafficAlerts getMockedAudibleTrafficAlerts(int secondsCount) {
        int[] seconds = new int[secondsCount];
        for (int i = 0; i < secondsCount; i++)
            seconds[i] = 2000 + i;
        return new AudibleTrafficAlerts(getMockSoundPlayer(), -1, -2,
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, new int[] { 13, 14 }, -3, -4,
                -5, -6, seconds, -7);
    }

    private Location getMockLocation(double latitude, double longitude, float bearing) {
        final Location mockLoc = mock(Location.class);
        when(mockLoc.getLatitude()).thenReturn(latitude);
        when(mockLoc.getLongitude()).thenReturn(longitude);
        when(mockLoc.getBearing()).thenReturn(bearing);
        return mockLoc;
    }

    private AudibleTrafficAlerts.SequentialSoundPoolPlayer getMockSoundPlayer() {
        AudibleTrafficAlerts.SequentialSoundPoolPlayer sp = mock(AudibleTrafficAlerts.SequentialSoundPoolPlayer.class);
        when(sp.load(anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
        return sp;
    }
}