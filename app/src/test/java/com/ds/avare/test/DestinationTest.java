package com.ds.avare.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.preference.PreferenceManager;
import android.test.mock.MockContext;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.CalendarHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test Destination calculations
 * Created by pasniak on 3/26/2017.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageService.class, PreferenceManager.class,
        GeomagneticField.class, Destination.class, CalendarHelper.class}) //classes which create the instance
public class DestinationTest {

    @Before
    public void setUp() throws Exception {

        // mock the preferences used by Destination calculations
        final Preferences prefs = mock(Preferences.class);
        when(prefs.isSimulationMode()).thenReturn(true);
        when(prefs.useBearingForETEA()).thenReturn(true);
        when(prefs.getFuelBurn()).thenReturn(10.0f);
        when(prefs.getDistanceUnit()).thenReturn("kt");
        when(prefs.getAircraftTAS()).thenReturn(60);
        mockStatic(PreferenceManager.class);
        whenNew(Preferences.class).withAnyArguments().thenReturn(prefs);

        /// mock shared preferences
        final SharedPreferences sharedPrefs = mock(SharedPreferences.class);
        when(sharedPrefs.getBoolean("SimulationMode", false)).thenReturn(true);
        when(sharedPrefs.getString(anyString(), anyString())).thenReturn("kt");

        final android.test.mock.MockContext ctx = mock(MockContext.class);
        when(ctx.getString(R.string.DistKnot)).thenReturn("nm");
        when(PreferenceManager.getDefaultSharedPreferences((Context)isNull()))
                .thenReturn(sharedPrefs);
        when(PreferenceManager.getDefaultSharedPreferences((Context)any()))
                .thenReturn(sharedPrefs);

        final GeomagneticField geoField = mock(GeomagneticField.class);
        when(geoField.getDeclination()).thenReturn(0f);
        whenNew(GeomagneticField.class).withAnyArguments().thenReturn(geoField);
    }

    @Test
    public void testUpdateTo() throws Exception {

        // mock storage service used to access preferences and GPS location
        // in the Destination constructor
        final StorageService storageService = mock(StorageService.class);
        when(storageService.getApplicationContext()).thenReturn(null);
        /// given the above returns null, the destination is at (0N, 0W)
        Destination d = new Destination(storageService, "TEST");
        setField(d, "mFound", true); // mock that destination is found (?)

        // mock calendar set to 12:00 for ETA time calculations
        when(mockCalendar.getHour()).thenReturn(12);
        when(mockCalendar.getMinute()).thenReturn(0);
        mockStatic(CalendarHelper.class);
        when(CalendarHelper.getInstance(any(long.class))).thenReturn(mockCalendar);

        // mock GPS location
        final Location loc1 = mock(Location.class);
        when(loc1.getSpeed()).thenReturn(60f);
        final GpsParams g = new GpsParams(loc1);

        // update destination with a location
        d.updateTo(g);

        assertEquals("Wrong 0kt ETE", "00.00", d.getEte());
        assertEquals("Wrong 0kt ETA", "12:00", d.getEta());

        // mock another GPS location, 1 degree off...
        final Location loc2 = mock(Location.class);
        when(loc2.getSpeed()).thenReturn(60f);
        when(loc2.getLongitude()).thenReturn(1d);
        when(loc2.getLatitude()).thenReturn(0d);
        when(loc2.getAltitude()).thenReturn(0d);
        final GpsParams g2 = new GpsParams(loc2);

        d.updateTo(g2);

        assertEquals("Wrong 60kt ETE", "01:00", d.getEte()); //... at 60kt we should be there in 1h
        assertEquals("Wrong 60kt ETA", "13:00", d.getEta());
    }

    // sets a private field using reflection
    private static void setField(Destination d,
                                String name,
                                Object value) throws Exception {
        Field foundField = Destination.class.getDeclaredField(name);
        foundField.setAccessible(true);
        foundField.set(d, value);

    }

    @Mock
    CalendarHelper mockCalendar;
}