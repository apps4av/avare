package com.ds.avare.test;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;

import com.ds.avare.MainActivity;
import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.google.common.io.Files;

import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static android.view.ViewConfiguration.getLongPressTimeout;

/**
 * Created by pasniak on 5/5/2017.
 */

abstract class InterfaceTest {
    private final static String SLASH = File.separator;
    private  MainActivity mMain;
    private final Context mCtx = RuntimeEnvironment.application;
    protected StorageService mStorageService;
    protected WebView mWebView;

    private static String getCycle() throws IOException {
        final String cycleUrl = "http://www.apps4av.org/new/version.php";
        final String currentCycle = new Scanner(new URL(cycleUrl).openStream(), "UTF-8").useDelimiter("\\A").next();
        if (currentCycle.isEmpty()) { throw new IOException("Unable to get cycle from "+cycleUrl); }        
        return currentCycle;
    }
    
    private static String downloadDatabaseZip(Context ctx, String currentCycle, String fileName) throws IOException {
        Preferences mPref = new Preferences(ctx);
        final URL website = new URL(mPref.getRoot() + currentCycle + "/" + fileName);
        final String avareAppDir = System.getProperty("user.dir", "./"); // sth like C:\Users\Michal\StudioProjects\avare\app\build\tmp\1705\databases.zip
        final String cachedBuildFilePath = new File(avareAppDir).getAbsolutePath()
            + SLASH + "build" + SLASH + "tmp" + SLASH + currentCycle + SLASH + fileName;
        if (!org.codehaus.plexus.util.FileUtils.fileExists(cachedBuildFilePath)) {
            System.out.println ("Creating dir " + cachedBuildFilePath);
            Files.createParentDirs(new File(cachedBuildFilePath));
            System.out.println ("Downloading " + website);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(cachedBuildFilePath);
            long n = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println ("Downloaded " + n + " bytes to " + cachedBuildFilePath);
        } else {
            System.out.println ("Using " + cachedBuildFilePath);
        }
        return cachedBuildFilePath;
    }

    private void unzipDb(String cachedBuildFilePath, String unzipFileNameRegex, String subDir) throws IOException {
        final String roboTestDir = mMain.getFilesDir().getPath();
        final File activityFilesDir = new File(roboTestDir + (subDir=="" ? "" : SLASH + subDir));
        activityFilesDir.mkdirs();
        final ZipFile zip = new ZipFile(cachedBuildFilePath);
        final Enumeration zipFileEntries = zip.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) zipFileEntries.nextElement();
            if (zipEntry.getName().matches(unzipFileNameRegex)) {
                unzipFile(zip, zipEntry, zipEntry.getName(), activityFilesDir);
            }
        }
    }

    private static void unzipFile(ZipFile zip, ZipEntry e, String unzipFileName, File toDirectory) throws IOException {
        System.out.print ("Unzip " + unzipFileName + " to " + toDirectory);
        BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(e));
        FileOutputStream fos = new FileOutputStream(toDirectory + SLASH + unzipFileName);
        int got, all = 0;
        final int BUFFER_SIZE = 1024 * 8;
        byte buffer[] = new byte[BUFFER_SIZE];
        while ((got = bis.read(buffer)) != -1) {
            fos.write(buffer, 0, got);
            all += got;
        }
        System.out.println (" (" + all + " bytes)");
    }

    private void deleteDb() {
        final String roboTestDir = mMain.getFilesDir().getPath();
        final File activityFilesDir = new File(roboTestDir);
        System.out.println ("Del " + roboTestDir);
        deleteDir(activityFilesDir);
    }

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    protected void downloadDatabases () throws IOException {
        String cachedBuildFilePath = downloadDatabaseZip(mCtx, getCycle(), "databases.zip"); // download database to the build cache
        unzipDb(cachedBuildFilePath, "main.db", ""); // unzip to the test directory
    }
    
    protected void downloadWeather ()  throws IOException {
        String weatherCachedBuildFilePath = downloadDatabaseZip(mCtx, "", "weather.zip"); // download weather to the build cache
        unzipDb(weatherCachedBuildFilePath, ".*", ""); // unzip all to the test directory
    }
        
    @Before
    public void setUp() throws IOException {
        mMain = Robolectric.setupActivity(MainActivity.class);
        
        downloadDatabases(); // all tests require FAA database    
        downloadMore(); // some test require more data

        prepStorageService();
        setupWebView();
        setupInterface(mCtx); // setup test-specific GUIs
    }

    @After
    public void tearDown() {
        deleteDb();
        System.out.println();
    }

    private void setupWebView() {
        mWebView = new WebView(mCtx);
    }

    abstract void setupInterface(Context ctx);

    private void prepStorageService() {
        mStorageService = Robolectric.setupService(StorageService.class);
        mStorageService.onCreate();
    }

    // override to download data beyond base FAA data
    void downloadMore () throws IOException {}
    
    // test helpers
    protected static class MyGenericCallback extends GenericCallback {
        @Override
        public Object callback(Object o1, Object o2) {
            return null;
        }
    }

    protected static MotionEvent getLongPressEvent(float x, float y) {
        // simulate long press
        long longPressMillis = getLongPressTimeout();
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + longPressMillis;
        int metaState = 0;
        return MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );
    }

}
