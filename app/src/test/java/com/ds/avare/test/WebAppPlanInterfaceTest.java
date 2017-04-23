package com.ds.avare.test;

import android.content.Context;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.ds.avare.AvareApplication;
import com.ds.avare.BuildConfig;
import com.ds.avare.MainActivity;
import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.webinfc.WebAppPlanInterface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
public class WebAppPlanInterfaceTest {

    private final static String SLASH = File.separator;
    private final Context mCtx = RuntimeEnvironment.application;
    private StorageService mStorageService;
    private WebAppPlanInterface mWebAppPlanInterface;
    private WebView mWebView;

    @Before
    public void setUp() throws IOException {
        String cachedBuildFilePath = downloadDatabaseZip(); // download database to the build cache
        unzipDb(cachedBuildFilePath, "main.db"); // unzip main db to the test directory
        prepStorageService();
        setupWebView();
    }

    @Test
    public void airportSearch() throws Exception {
        mWebAppPlanInterface.search("KCDW");
        assertEquals("Airport not found", "javascript:search_add('CDW','ESSEX COUNTY','Base','AIRPORT')",
                getLastLoadedUrl());
    }
    @Test
    public void airportAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("CDW","Base","AIRPORT");
        assertEquals(mStorageService.getPlan().getDestinationNumber(),1);
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
        assertEquals(mStorageService.getPlan().getDestinationNumber(),1);
    }
    @Test
    public void userWaypointSearch() throws Exception {
        mWebAppPlanInterface.search("40.4747&-74.1844");
        assertEquals("User waypoint not found", "javascript:search_add('40.4747&-74.1844','GPS','GPS','GPS')",
                getLastLoadedUrl());
    }
    @Test
    public void userWaypointAdd() throws Exception {
        mWebAppPlanInterface.addToPlan("40.4747&-74.1844","GPS","GPS");
        assertEquals(1, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void createAndLoadPlans() throws Exception {
        String data;

        // create the simplest plan with 2 points
        mWebAppPlanInterface.createPlan("KCDW SBJ");
        assertEquals(2, mStorageService.getPlan().getDestinationNumber());
        data = mWebAppPlanInterface.getPlanData();
        final String PLAN1_DATA = "::::1,0,0,0,0,--:--,CDW,Base,-.-,-::::0,0,0,0,0,--:--,SBJ,Navaid,-.-,-::::  0nm --:-- 360° -.-";
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

        // refresh
        mWebAppPlanInterface.refreshPlanList();
        ArrayList<String> plans = mWebAppPlanInterface.getPlanNames(10);
        assertEquals("TEST",  plans.get(0));
        assertEquals("TEST2", plans.get(1));

        //now retrieve plan 1 to N51
        mWebAppPlanInterface.loadPlan("TEST");
        data = mWebAppPlanInterface.getPlanData();
        assertEquals("TEST" + PLAN1_DATA, data);
        assertUrl("javascript:plan_add('SBJ','Navaid','SOLBERG 112.90')");

        // refresh
        mWebAppPlanInterface.refreshPlanList();
        assertUrl("javascript:set_plan_count('1 - 2 of 2')");

        // filter out the second plan
        mWebAppPlanInterface.planFilter("2");
        assertUrl("javascript:set_plan_count('1 - 1 of 1')");
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
        mWebAppPlanInterface.createPlan("KCDW SBJ 40.4747&-74.1844");
        assertEquals(3, mStorageService.getPlan().getDestinationNumber());
    }
    @Test
    public void createPlanWithAirway() throws Exception {
        mWebAppPlanInterface.createPlan("N51 V6 EMPYR V6 LGA");
        assertEquals(10, mStorageService.getPlan().getDestinationNumber()); // there is a kink in the route, so 10 points
    }

    // test helpers
    private static class MyGenericCallback extends GenericCallback {
        @Override
        public Object callback(Object o1, Object o2) {
            return null;
        }
    }
    @NonNull
    private String downloadDatabaseZip() throws IOException {
        Preferences mPref = new Preferences(mCtx);
        final URL website = new URL(mPref.getRoot() + "/1704/databases.zip");
        final String fileName = "databases.zip";
        final String avareAppDir = System.getProperty("user.dir", "./"); // sth like S:\Projects\avare\app
        final String cachedBuildFilePath = new File(avareAppDir).getAbsolutePath() + SLASH + "build" + SLASH + "tmp" + SLASH + fileName;
        if (!org.codehaus.plexus.util.FileUtils.fileExists(cachedBuildFilePath)) {
            System.out.println ("Download " + website);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(cachedBuildFilePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return cachedBuildFilePath;
    }
    private static void unzipDb(String cachedBuildFilePath, String unzipFileName) throws IOException {
        final String roboTestDir = Robolectric.setupActivity(MainActivity.class).getFilesDir().getPath();
        final File activityFilesDir = new File(roboTestDir);
        final ZipFile zip = new ZipFile(cachedBuildFilePath);
        final Enumeration zipFileEntries = zip.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) zipFileEntries.nextElement();
            if (zipEntry.getName().equals(unzipFileName)) {
                unzipFile(zip, zipEntry, unzipFileName, activityFilesDir);
            }
        }
    }
    private static void unzipFile(ZipFile zip, ZipEntry e, String unzipFileName, File toDirectory) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(e));
        FileOutputStream fos = new FileOutputStream(toDirectory + SLASH + unzipFileName);
        int got;
        final int BUFFER_SIZE = 1024 * 8;
        byte buffer[] = new byte[BUFFER_SIZE];
        while ((got = bis.read(buffer)) != -1) {
            fos.write(buffer, 0, got);
        }
    }
    private void setupWebView() {
        mWebView = new WebView(mCtx);
        mWebAppPlanInterface = new WebAppPlanInterface(mCtx, mWebView, new MyGenericCallback());
        mWebAppPlanInterface.connect(mStorageService);
    }
    private void prepStorageService() {
        mStorageService = Robolectric.setupService(StorageService.class);
        mStorageService.onCreate();
    }
}
