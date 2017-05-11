package com.ds.avare.test;

import android.content.Context;

import com.ds.avare.AvareApplication;
import com.ds.avare.BuildConfig;
import com.ds.avare.webinfc.WebAppPlanInterface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
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
    public void userWaypointSearch() throws Exception {
        mWebAppPlanInterface.search("40.4747&-74.1844");
        assertEquals("User waypoint not found", "javascript:search_add('40.4747&-74.1844','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void userWaypointIcaoSearch() throws Exception {
        mWebAppPlanInterface.search("4028N07411W");
        assertEquals("User waypoint not found", "javascript:search_add('4028N07411W','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void userWaypointIcaoDecSecsSearch() throws Exception {
        mWebAppPlanInterface.search("4028305N07411305W");
        assertEquals("User waypoint not found", "javascript:search_add('4028305N07411305W','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void userWaypointAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("40.4747&-74.1844","GPS","GPS");
        assertEquals(1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void userWaypointIcaoAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("4028N07411W","GPS","GPS");
        assertEquals(1, mStorageService.getPlan().getDestinationNumber());
    }

    final String PLAN1_DATA = "::::1,0,0,0,0,--:--,CDW,AIRPORT,-.-,-::::0,0,0,0,0,--:--,SBJ,VOR/DME,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN2_DATA = "::::1,0,0,0,0,--:--,TTN,AIRPORT,-.-,-::::0,0,0,0,0,--:--,N51,AIRPORT,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN3_DATA = "::::1,0,0,0,0,--:--,4000N07400W,GPS,-.-,-::::0,0,0,0,0,--:--,4100N07400W,GPS,-.-,-::::  0nm --:-- 360° -.-";
    final String PLAN4_DATA = "::::1,0,0,0,0,--:--,40.00&-74.00,GPS,-.-,-::::0,0,0,0,0,--:--,41.00&-74.00,GPS,-.-,-::::  0nm --:-- 360° -.-";
    
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
        mWebAppPlanInterface.savePlan("TEST"); //to preferences
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
        mWebAppPlanInterface.createPlan("4000N07400W 4100N07400W");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        assertUrl("javascript:plan_add('4100N07400W','GPS','GPS')");

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
        mWebAppPlanInterface.refreshPlanList();
        ArrayList<String> plans = mWebAppPlanInterface.getPlanNames(10);
        assertEquals("TEST",  plans.get(0));
        assertEquals("TEST2", plans.get(1));
        assertEquals("TEST3", plans.get(2));
        assertEquals("TEST4", plans.get(3));

        //now retrieve plan 1 to N51
        mWebAppPlanInterface.loadPlan("TEST");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST" + PLAN1_DATA, data);
        assertUrl("javascript:plan_add('SBJ','Navaid','SOLBERG 112.90')");

        // refresh
        mWebAppPlanInterface.refreshPlanList();
        assertUrl("javascript:set_plan_count('1 - 4 of 4')");

        // filter out the second plan
        mWebAppPlanInterface.planFilter("2");
        assertUrl("javascript:set_plan_count('1 - 1 of 1')");

        
        //now retrieve plan 2 to N51
        mWebAppPlanInterface.loadPlan("TEST2");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST2" + PLAN2_DATA, data);
        assertUrl("javascript:plan_add('N51','Base','SOLBERG-HUNTERDON')");


        //now retrieve plan 3 to 4100N07400W
        mWebAppPlanInterface.loadPlan("TEST3");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST3" + PLAN3_DATA, data);
        assertUrl("javascript:plan_add('4100N07400W','GPS','GPS')");
        
        
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
        mWebAppPlanInterface.createPlan("KCDW SBJ 40.4747&-74.1844 4100N07400W");
        assertEquals(4, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void createPlanWithAirway() throws Exception {
        mWebAppPlanInterface.createPlan("N51 V6 EMPYR V6 LGA");
        assertEquals(10, mStorageService.getPlan().getDestinationNumber()); // there is a kink in the route, so 10 points
    }

    public void setupInterface(Context ctx) {
        mWebAppPlanInterface = new WebAppPlanInterface(ctx, mWebView, new MyGenericCallback());
        mWebAppPlanInterface.connect(mStorageService);
    }
}
