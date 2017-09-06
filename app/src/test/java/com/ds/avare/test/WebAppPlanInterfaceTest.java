package com.ds.avare.test;

import android.content.Context;
import android.location.Location;

import com.ds.avare.AvareApplication;
import com.ds.avare.BuildConfig;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.webinfc.WebAppPlanInterface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by pasniak on 4/1/2017.
 *
 * Note: if "Font not found at" error occurs this is due to a bug
 * see https://github.com/robolectric/robolectric/issues/2647
 * and https://issuetracker.google.com/issues/37347564
 * A temporary workaround is to add task dependency on mergeDebugAssets
 * in Run/Debug Configurations (see https://i.imgur.com/u6KSxQq.png)
 */


@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, application = AvareApplication.class)
@PowerMockIgnore({"org.mockito.", "org.robolectric."})
public class WebAppPlanInterfaceTest extends InterfaceTest {

    private WebAppPlanInterface mWebAppPlanInterface;
    
    @Test
    public void airportSearch() throws Exception {
        mWebAppPlanInterface.search("KCDW");
        assertEquals("Airport not found", "javascript:search_add('CDW','ESSEX COUNTY','Base','AIRPORT')",
                getLastLoadedUrl());
    }
    @Test
    public void airportAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("CDW","Base","AIRPORT");
        assertEquals("Airport not added", 1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void navaidSearch() throws Exception {
        mWebAppPlanInterface.search("SBJ");
        assertEquals("Navaid not found", "javascript:search_add('SBJ','SOLBERG 112.90','Navaid','VOR/DME')",
                getLastLoadedUrl());
    }
    @Test
    public void navaidAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("SBJ","Navaid","VOR/DME");
        assertEquals("Navaid not added", 1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void wptSearch() throws Exception {
        mWebAppPlanInterface.search("40.4747&-74.1844");
        assertEquals("User waypoint not found", "javascript:search_add('40.4747&-74.1844','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void wptIcaoSearch() throws Exception {
        mWebAppPlanInterface.search("402800N0741100W");
        assertEquals("User waypoint not found", "javascript:search_add('402800N0741100W','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void wptAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("40.4747&-74.1844","GPS","GPS");
        assertEquals(1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void wptIcaoAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("402800N0741100W","GPS","GPS");
        assertEquals(1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void wptIcaoBadLatAdd() throws Exception {
        // "9" cannot be in minutes/seconds  
        mWebAppPlanInterface.addToPlan("900000N0741000W","GPS","GPS");
        assertEquals("Able to add bad latitude degrees", 0, mStorageService.getPlan().getDestinationNumber());
        mWebAppPlanInterface.addToPlan("409000N0741000W","GPS","GPS");
        assertEquals("Able to add bad latitude minutes", 0, mStorageService.getPlan().getDestinationNumber());
        mWebAppPlanInterface.addToPlan("400090N0741000W","GPS","GPS");
        assertEquals("Able to add bad latitude seconds", 0, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void wptIcaoBadLonAdd() throws Exception {
        // "9" cannot be in minutes/seconds  
        mWebAppPlanInterface.addToPlan("402000N1974100W","GPS","GPS");
        assertEquals("Able to add bad longitude degrees", 0, mStorageService.getPlan().getDestinationNumber());
        mWebAppPlanInterface.addToPlan("402000N0749000W","GPS","GPS");
        assertEquals("Able to add bad longitude minutes", 0, mStorageService.getPlan().getDestinationNumber());
        mWebAppPlanInterface.addToPlan("402000N0741090W","GPS","GPS");
        assertEquals("Able to add bad longitude seconds", 0, mStorageService.getPlan().getDestinationNumber());
    }
    
    final String PLAN1_DATA = "::::1,0,0,0,0,--:--,CDW,AIRPORT,-.-,-::::0,0,0,0,0,--:--,SBJ,VOR/DME,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN2_DATA = "::::1,0,0,0,0,--:--,TTN,AIRPORT,-.-,-::::0,0,0,0,0,--:--,N51,AIRPORT,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN3_DATA = "::::1,0,0,0,0,--:--,400000N0740000W,GPS,-.-,-::::0,0,0,0,0,--:--,410000N0740000W,GPS,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN4_DATA = "::::1,0,0,0,0,--:--,40.00&-74.00,GPS,-.-,-::::0,0,0,0,0,--:--,41.00&-74.00,GPS,-.-,-::::  0nm --:-- 360° -.-";
    
    @Test
    public void createLongCoast2CoastPlanFromSkyvector() throws Exception
    {
        // this is a copy-paste of a long plan from SkyVector, each point separated by is 1-3 spaces, 40 points total
        mWebAppPlanInterface.createPlan(" 404411N0742217W  N05  TIKLE  8PA0 405409N0771129W  405747N0774906W 405908N0790430W  DISHE  ACO  KC66S 405658N0822541W  405710N0825236W 405242N0833633W  405000N0841346W 404755N0844847W  404808N0853707W 404833N0861950W  404706N0871226W 404603N0875932W  404352N0883612W 404352N0892237W  404321N0902506W 404308N0913949W  403858N0925716W 403808N0943536W  403507N0964408W 403327N0975223W  403339N0994109W 403231N1010935W  403134N1025146W 402512N1041434W  402646N1054358W 402204N1074645W  AWLIJ  401901N1104801W 401032N1130326W  401110N1152501W 401110N1175116W  400948N1203157W 400858N1224827W   ");
        assertEquals("Long SkyVector plan creation failed", 40, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void createLongMEtoFLPlanFromSkyVector() throws Exception
    {
        // this is a copy-paste of a long plan along the E coast from SkyVector, each point separated by is 1-3 spaces, 24 points total
        // https://skyvector.com/?ll=36.279707198143875,-87.7340698184927&chart=301&zoom=11&fpl=%20KFSO%204448N07307W%20BJA%204413N07315W%204351N07320W%204328N07343W%204244N07357W%204215N07403W%204139N07420W%204043N07433W%203943N07510W%203847N07549W%203724N07643W%203623N07642W%203544N07704W%203426N07848W%203322N07951W%203229N08119W%203054N08150W%202956N08148W%20KORL%202730N08110W%202638N08058W%202600N08046W%202516N08055W
        mWebAppPlanInterface.createPlan(" 444758N0730707W  BJA  441254N0731432W 435111N0732001W  432808N0734322W 424401N0735633W  421451N0740309W 413906N0741954W  404305N0743232W 394319N0750953W  384710N0754910W 372410N0764300W  362253N0764227W 354429N0770352W  342549N0784741W 332146N0795125W  322844N0811845W 305405N0814931W  295549N0814752W  KORL 272937N0810958W  263815N0805809W 260021N0804531W  251546N0805524W  ");
        assertEquals("Long SkyVector plan creation failed", 24, mStorageService.getPlan().getDestinationNumber());
        assertMinMaxCoords(mStorageService.getPlan(), 45, 25, -82, -73);
    }

    @Test
    public void createAndLoadPlans() throws Exception {
        String data;

        // create plan 1 with 2 points
        mWebAppPlanInterface.createPlan("KCDW SBJ");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        data = mWebAppPlanInterface.getPlanData();
        final String PLAN1_DATA = "::::1,0,0,0,0,--:--,CDW,AIRPORT,-.-,-::::0,0,0,0,0,--:--,SBJ,VOR/DME,-.-,-::::  0nm --:-- 360° -.-";
        assertEquals(PLAN1_DATA, data);

        //save it
        mWebAppPlanInterface.savePlan("TEST1"); //to preferences
        assertUrl("javascript:set_plan_count('1 - 1 of 1')");

        // refresh
        mWebAppPlanInterface.refreshPlanList();
        assertUrl("javascript:set_plan_count('1 - 1 of 1')");

        // clean up the current plan
        mWebAppPlanInterface.discardPlan();

        
        // create plan 2
        mWebAppPlanInterface.createPlan("KTTN N51");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        assertUrl("javascript:plan_add('N51','Base','SOLBERG-HUNTERDON')");

        //save it to preferences
        mWebAppPlanInterface.savePlan("TEST2");
        assertUrl("javascript:set_plan_count('1 - 2 of 2')");

        // clean up the current plan
        mWebAppPlanInterface.discardPlan();

        
        // create plan 3 with ICAO style coordinates
        mWebAppPlanInterface.createPlan("400000N0740000W 410000N0740000W");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        assertUrl("javascript:plan_add('410000N0740000W','GPS','GPS')");

        //save it to preferences
        mWebAppPlanInterface.savePlan("TEST3");
        assertUrl("javascript:set_plan_count('1 - 3 of 3')");

        // clean up the current plan
        mWebAppPlanInterface.discardPlan();


        // create plan 4 with Google style coordinates
        mWebAppPlanInterface.createPlan("40.00&-74.00 41.00&-74.00");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        assertUrl("javascript:plan_add('41.00&-74.00','GPS','GPS')");

        //save it to preferences
        mWebAppPlanInterface.savePlan("TEST4");
        assertUrl("javascript:set_plan_count('1 - 4 of 4')");

        // clean up the current plan
        mWebAppPlanInterface.discardPlan();


        // refresh
        ArrayList<String> plans = mWebAppPlanInterface.getPlanNames(10);
        assertEquals("TEST1", plans.get(0));
        assertEquals("TEST2", plans.get(1));
        assertEquals("TEST3", plans.get(2));
        assertEquals("TEST4", plans.get(3));

        //now retrieve plan 1 to N51
        mWebAppPlanInterface.loadPlan("TEST1");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST1" + PLAN1_DATA, data);
        assertUrl("javascript:plan_add('SBJ','Navaid','SOLBERG 112.90')");

        // filter out the second plan
        mWebAppPlanInterface.planFilter("2");
        assertUrl("javascript:set_plan_count('1 - 1 of 1')");

        //now retrieve plan 2 to N51
        mWebAppPlanInterface.loadPlan("TEST2");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST2" + PLAN2_DATA, data);
        assertUrl("javascript:plan_add('N51','Base','SOLBERG-HUNTERDON')");


        //now retrieve plan 3 to 410000N0740000W
        mWebAppPlanInterface.loadPlan("TEST3");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST3" + PLAN3_DATA, data);
        assertUrl("javascript:plan_add('410000N0740000W','GPS','GPS')");
        
        
        //now retrieve plan 4 to 41.00&-74.00
        mWebAppPlanInterface.loadPlan("TEST4");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST4" + PLAN4_DATA, data);
        assertUrl("javascript:plan_add('41.00&-74.00','GPS','GPS')");
    }

    private void assertUrl(String expected) {
        String lastLoadedUrl = shadowOf(mWebView).getLastLoadedUrl(); // get state of the JS engine
        assertEquals(expected, lastLoadedUrl);
    }

    private String getLastLoadedUrl() {
        return shadowOf(mWebView).getLastLoadedUrl();
    }

    @Test
    public void createPlanWithUserWpt() throws Exception {
        mWebAppPlanInterface.createPlan("KCDW SBJ 40.4747&-74.1844 410000N0740000W");
        assertEquals(4, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void createPlanWithAirway() throws Exception {
        mWebAppPlanInterface.createPlan("SLATT V6 EMPYR V6 LGA");
        assertEquals(13, mStorageService.getPlan().getDestinationNumber());
    }

    @Test
    public void planOperations() throws Exception {
        // create plan with points spaced 1 degree lattitude = 60mn/log
        final String PLAN = "40.00&-74.00 41.00&-74.00 42.00&-74.00 43.00&-74.00 44.00&-74.00 45.00&-74.00";
        final int nbWaypoints = 6;

        mWebAppPlanInterface.createPlan(PLAN);
        assertEquals(nbWaypoints, mStorageService.getPlan().getDestinationNumber());

        final Plan p = mStorageService.getPlan();

        assertEquals("Plan track shape", (nbWaypoints-1)*4, p.getTrackShape().getNumCoords());

        for (int i = 0; i < nbWaypoints ; i++) {
            assertFalse("Check for not passed destinations", p.isPassed(i));
            assertEquals("Bearing calcs", 0, p.getBearing(i, i+1), 0.1);
        }
        assertNull("Access beyond last destination", p.getDestination(100));

        p.simulate();
        assertEquals(300, p.getDistance(), 1); // so roughly 300nm

        mWebAppPlanInterface.activateToggle();
        p.simulate();

        int lastLeg = nbWaypoints-1;
        assertEquals("Last leg fuel calculation", "6.0", p.getDestination(lastLeg).getFuel());  // at default 10 gal/hour

        // add and remove waypoint
        mWebAppPlanInterface.addToPlan("46.00&-74.00","GPS","GPS");
        assertEquals("Waypoint add", nbWaypoints+1, mStorageService.getPlan().getDestinationNumber());
        assertEquals("Plan track shape", (nbWaypoints-1)*4 + 4, p.getTrackShape().getNumCoords());

        // check geosearch 
        int found = p.findClosePointId(-74, 46, 2);
        assertEquals("Finding by position", lastLeg+1, found);

        p.remove(lastLeg+1);
        p.simulate();
        assertEquals("Leg add and remove", 300, p.getDistance(), 1);

        p.advance();
        assertTrue("Check for passed destinations", p.isPassed(0));
        for (int i = 1; i < nbWaypoints ; i++) {
            assertFalse("Check for not passed destinations failed", p.isPassed(i));
        }

        p.makeInactive();
        assertFalse("Deactivation failed", p.isActive());
    }    
    
    public void setupInterface(Context ctx) {
        mWebAppPlanInterface = new WebAppPlanInterface(ctx, mWebView, new MyGenericCallback());
        mWebAppPlanInterface.connect(mStorageService);
    }

    private static void assertMinMaxCoords(Plan p, int latMax, int latMin, int lonMin, int lonMax) {
        for (int i = 0; i < p.getDestinationNumber(); i++) {
            Destination d = p.getDestination(i);
            Location l = d.getLocation();
            double lat = l.getLatitude(), lon = l.getLongitude();
            assertTrue("Lat conversion "+lat+" failed at " + d.getStorageName(), latMax > lat && lat > latMin);
            assertTrue("Lon conversion "+lon+" failed at "+ d.getStorageName(),  lonMin < lon && lon < lonMax);
        }
    }

}
