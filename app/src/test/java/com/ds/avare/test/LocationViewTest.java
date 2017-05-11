package com.ds.avare.test;

import android.content.Context;
import android.location.Location;
import android.view.MotionEvent;

import com.ds.avare.AvareApplication;
import com.ds.avare.BuildConfig;
import com.ds.avare.LocationActivity;
import com.ds.avare.R;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.views.LocationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Michal on 5/5/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, application = AvareApplication.class)
@PowerMockIgnore({"org.mockito.", "org.robolectric."})
public class LocationViewTest extends InterfaceTest {

    private LocationView mLocationView;
    
    @Test
    public void smallAptPressed() throws Exception {
        // set location
        Location current = new Location("N51 location");
        current.setLatitude(40.582744444);
        current.setLongitude(-74.736716667);
        GpsParams params = new GpsParams(current);
        mLocationView.initParams(params, mStorageService);

        // long press
        MotionEvent motionEvent = getLongPressEvent(0,0);
        mLocationView.dispatchTouchEvent(motionEvent);

        // assert destination pressed
        LongTouchDestination d =  mLocationView.getLongTouchDestination();
        assertEquals("N51 Airport not found", "N51", d.airport);
        assertEquals("N51 Info not found", "0nm(S  of) 013°", d.info);
        assertNotNull("N51 SUA not found", d.sua);
        assertNull("N51 TAF found", d.taf);  // Solberg has no ATIS
        assertNull("N51 METAR found", d.metar);
        assertNull("N51 Performance found", d.performance);
        HtmlAsserts.assertRowCount(d.navaids, 4);
    }
    
    @Test
    public void aptWithAtisAndTafPressed() throws Exception {
        // set location
        Location current = new Location("TTN location"); // TTN has ATIS and TAF
        current.setLatitude(40.27617299393192);
        current.setLongitude(-74.8124999934585);
        GpsParams params = new GpsParams(current);
        mLocationView.initParams(params, mStorageService);

        // long press
        MotionEvent motionEvent = getLongPressEvent(0,0);
        mLocationView.dispatchTouchEvent(motionEvent);

        // assert destination pressed
        LongTouchDestination d =  mLocationView.getLongTouchDestination();
        assertEquals("TTN Airport not found", "TTN", d.airport);
        assertEquals("TTN Info not found", "0nm(SE of) 327°", d.info);
        assertNotNull("TTN SUA not found", d.sua);
        assertNotNull("TTN TAF not found", d.taf); // Trenton has TAF and ATIS
        assertNotNull("TTN METAR not found", d.metar);
        assertNotNull("TTN Performance not found", d.performance);
        String[] perf = d.performance.split("\\n");
        assertTrue("Performance has time "+perf[0],      perf[0].matches("\\d{6}Z"));
        assertTrue("Performance has DA "+perf[1],        perf[1].matches("Density Altitude -?\\d* ft"));
        assertTrue("Performance has runway "+perf[2],    perf[2].matches("Best Wind Runway \\d{2}"));
        assertTrue("Performance has wind "+perf[3],      perf[3].matches(" \\d{1,2}(G\\d{1,2})?KT (Head|Tail)"));
        assertTrue("Performance has crosswind "+perf[4], perf[4].matches(" \\d{1,2}(G\\d{1,2})?KT (Left|Right) X"));
        
        HtmlAsserts.assertRowCount(d.navaids, 4);
    }
    
    public void downloadMore () throws IOException {
        downloadWeather();
    }
    
    public void setupInterface(Context ctx) {
        final LocationActivity locationActivity = Robolectric.buildActivity(LocationActivity.class).create().get();
        mLocationView = (LocationView) locationActivity.findViewById(R.id.location);
    }
}
