/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.storage;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.utils.BitmapHolder;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Preferences for main activity
 */
public class Preferences {

    /*
     * These are set when inited
     */
    public static double speedConversion = 1.944;
    public static double heightConversion = 3.28084;
    public static double feetConversion = 6076.12;
    public static double earthRadiusConversion = 3440.069;
    public static String distanceConversionUnit = "nm";
    public static String speedConversionUnit = "kt";
    public static String vsConversionUnit = "fpm";

    /*
     * plate auto load distance
     */
    public static final double DISTANCE_TO_AUTO_LOAD_PLATE = 3.0;

    public static final String IMAGE_EXTENSION = ".png";

    /*
     * MAX number of elements
     */
    public static final int MAX_RECENT = 50;

    public static final int MAX_PLANS = 20;
    public static final int MAX_LISTS = 20;

    public static final int MAX_AREA_AIRPORTS = 20;

    public static final double MIN_TOUCH_MOVEMENT_SQ_DISTANCE = 0.001;

    /*
     * Max memory and max screen size it will support
     */
    public static final long MEM_256 = 256 * 1024 * 1024;
    public static final long MEM_192 = 192 * 1024 * 1024;
    public static final long MEM_128 = 128 * 1024 * 1024;
    public static final long MEM_64 = 64 * 1024 * 1024;
    public static final long MEM_32 = 32 * 1024 * 1024;


    public static final int MEM_192_X = 9;
    public static final int MEM_192_Y = 7;
    public static final int MEM_192_OH = 13;
    public static final int MEM_128_X = 7;
    public static final int MEM_128_Y = 5;
    public static final int MEM_128_OH = 7;
    public static final int MEM_64_X = 5;
    public static final int MEM_64_Y = 3;
    public static final int MEM_64_OH = 5;
    public static final int MEM_32_X = 3;
    public static final int MEM_32_Y = 3;
    public static final int MEM_32_OH = 3;
    public static final int MEM_16_X = 3;
    public static final int MEM_16_Y = 3;
    public static final int MEM_16_OH = 3;

    /**
     * Preferences
     */
    private SharedPreferences mPref;
    private Context mContext;

    public static double NM_TO_MI = 1.15078;
    public static double NM_TO_KM = 1.852;
    public static double MS_TO_KT = 1.94384;

    /**
     * @param ctx
     */
    public Preferences(Context ctx) {
        /*
         * Load preferences.
         */
        mContext = ctx;
        /*
         * Set default prefs.
         */
        mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (getDistanceUnit().equals(mContext.getString(R.string.UnitKnot))) {
            speedConversion = 1.944; // m/s to kt/hr
            heightConversion = 3.28084;
            feetConversion = 6076.12;
            earthRadiusConversion = 3440.069;
            distanceConversionUnit = mContext.getString(R.string.DistKnot);
            speedConversionUnit = mContext.getString(R.string.SpeedKnot);
            vsConversionUnit = mContext.getString(R.string.VsFpm);
        } else if (getDistanceUnit().equals(mContext.getString(R.string.UnitMile))) {
            speedConversion = 2.2396; // m/s to mi/hr
            heightConversion = 3.28084;
            feetConversion = 5280;
            earthRadiusConversion = 3963.1676;
            distanceConversionUnit = mContext.getString(R.string.DistMile);
            speedConversionUnit = mContext.getString(R.string.SpeedMile);
            vsConversionUnit = mContext.getString(R.string.VsFpm);
        } else if (getDistanceUnit().equals(mContext.getString(R.string.UnitKilometer))) {
            speedConversion = 3.6; // m/s to kph
            heightConversion = 3.28084;
            feetConversion = 3280.84;
            earthRadiusConversion = 6378.09999805;
            distanceConversionUnit = mContext.getString(R.string.DistKilometer);
            speedConversionUnit = mContext.getString(R.string.SpeedKilometer);
            vsConversionUnit = mContext.getString(R.string.VsFpm);
        }
    }

    /**
     * @return
     */
    public String getRoot() {

        String val = mPref.getString(mContext.getString(R.string.Root), null);
        if (null == val) {
            SharedPreferences.Editor editor = mPref.edit();
            editor.putString(mContext.getString(R.string.Root), "0");
            editor.commit();
            val = "0";
        }
        if (val.equals("0")) {
            return "http://www.apps4av.org/new/";
        }
        else if (val.equals("1")) {
            return "http://avare.kitepilot.net/new/";
        }
        else if (val.equals("2")) {
	    val = mPref.getString(mContext.getString(R.string.PrivateServer), "http://127.0.0.1/");
	    if (!val.substring(val.length() - 1).equals("/")) {
		val = val + "/";   
	    }
            return val;
        }
        return ("");
    }

    /**
     * @return
     */
    public String[] getRecent() {
        String recent = mPref.getString(mContext.getString(R.string.Recent), "");
        String[] tokens = recent.split(",");
        return tokens;
    }

    /**
     * @return
     */
    public void modifyARecent(String name, String description) {
        String[] tokens = getRecent();
        description = description.replaceAll("[^A-Za-z0-9 ]", "");
        description = description.replaceAll(";", " ");
        List<String> l = new LinkedList<String>(Arrays.asList(tokens));
        for (int id = 0; id < l.size(); id++) {
            if (l.get(id).equals(name)) {
                String oldName = name;
                String newName = null;
                int desc = oldName.lastIndexOf("@");
                if (desc < 0) {
                    newName = description + "@" + oldName;
                } else {
                    newName = description + oldName.substring(desc, oldName.length());
                }
                l.set(id, newName);
                break;
            }
        }

        String recent = "";
        for (int id = 0; id < l.size(); id++) {
            recent = recent + l.get(id) + ",";
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.Recent), recent);
        editor.commit();
    }

    /**
     * @return
     */
    public void deleteARecent(String name) {
        String[] tokens = getRecent();
        List<String> l = new LinkedList<String>(Arrays.asList(tokens));
        for (int id = 0; id < l.size(); id++) {
            if (l.get(id).equals(name)) {
                l.remove(id);
            }
        }

        String recent = "";
        for (int id = 0; id < l.size(); id++) {
            recent = recent + l.get(id) + ",";
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.Recent), recent);
        editor.commit();
    }

    /**
     * @return
     */
    public void addToRecent(String name) {
        String[] tokens = getRecent();
        List<String> l = new LinkedList<String>(Arrays.asList(tokens));
        for (int id = 0; id < l.size(); id++) {
            if (l.get(id).equals(name)) {
                l.remove(id);
            }
        }
        l.add(0, name);
        if (l.size() > MAX_RECENT) {
            l = l.subList(0, MAX_RECENT - 1);
        }

        String recent = "";
        for (int id = 0; id < l.size(); id++) {
            recent = recent + l.get(id) + ",";
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.Recent), recent);
        editor.commit();
    }

    /**
     * @return
     */
    public String getPlans() {
        return mPref.getString(mContext.getString(R.string.Plan) + "v10", "");
    }


    /**
     * @return
     */
    public void putPlans(String name) {
        mPref.edit().putString(mContext.getString(R.string.Plan) + "v10", name).commit();
    }


    /**
     * @return
     */
    public static int[] getTilesNumber(Context ctx) {
        int[] ret = new int[3];
        
        /*
         * Find max tiles this system can support.
         */
        long mem = Runtime.getRuntime().maxMemory();

        if (mem >= MEM_192) {
            ret[0] = MEM_192_X;
            ret[1] = MEM_192_Y;
            ret[2] = MEM_192_OH;
        } else if (mem >= MEM_128) {
            ret[0] = MEM_128_X;
            ret[1] = MEM_128_Y;
            ret[2] = MEM_128_OH;
        } else if (mem >= MEM_64) {
            ret[0] = MEM_64_X;
            ret[1] = MEM_64_Y;
            ret[2] = MEM_64_OH;
        } else if (mem >= MEM_32) {
            ret[0] = MEM_32_X;
            ret[1] = MEM_32_Y;
            ret[2] = MEM_32_OH;
        } else {
            ret[0] = MEM_16_X;
            ret[1] = MEM_16_Y;
            ret[2] = MEM_16_OH;
        }


        // find screen size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();
        defaultDisplay.getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int tilesx = (width / BitmapHolder.WIDTH) + 2; // add 1 for round up, and 1 for zoom
        int tilesy = (height / BitmapHolder.HEIGHT) + 2;

        // odd tiles only
        if(tilesx % 2 == 0) {
            tilesx++;
        }
        if(tilesy % 2 == 0) {
            tilesy++;
        }

        // no going above memory limit
        if(tilesx > ret[0]) {
            tilesx = ret[0];
        }
        if(tilesy > ret[1]) {
            tilesy = ret[1];
        }
        ret[0] = tilesx;
        ret[1] = tilesy;

        if(ret[0] <= 0) {
            ret[0] = MEM_16_X;
        }
        if(ret[1] <= 0) {
            ret[1] = MEM_16_Y;
        }

        return ret;
    }

    public boolean isLeaveRunning() {
        return (mPref.getBoolean(mContext.getString(R.string.LeaveRunning), true));
    }

    /**
     * @return
     */
    public boolean isShowBackground() {
        return (mPref.getBoolean(mContext.getString(R.string.Background), true));
    }

    public boolean isShowAllFacilities() {
        return (mPref.getBoolean(mContext.getString(R.string.AllFacilities), false));
    }

    /**
     * @return
     */
    public boolean isTrackEnabled() {
        return (mPref.getBoolean(mContext.getString(R.string.ShowTrack), true));
    }

    /**
     * @return
     */
    public boolean isTrackingEnabled() {
        return (mPref.getBoolean(mContext.getString(R.string.TracksOn), false));
    }

    /**
     * @return
     */
    public void setTrackingState(boolean state) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(mContext.getString(R.string.TracksOn), state);
        editor.commit();
    }


    /**
     * @return
     */
    public boolean isWeatherTranslated() {
        return (mPref.getBoolean(mContext.getString(R.string.XlateWeather), false));
    }

    public boolean isExtendRunways() {
        return (mPref.getBoolean(mContext.getString(R.string.Runways), true));
    }

    public boolean isAutoDisplayAirportDiagram() {
        return (mPref.getBoolean(mContext.getString(R.string.AutoShowAirportDiagram), false));
    }
    public boolean isShowLabelMETARS() {
        return (mPref.getBoolean(mContext.getString(R.string.ShowLabelMETARS), false));
    }

    /**
     *
     * @return
     */
    public boolean showObstacles() {
        return(mPref.getBoolean(mContext.getString(R.string.Obstacles), false));
    }

    /**
     * @return
     */
    public boolean isSimulationMode() {
        return (mPref.getBoolean(mContext.getString(R.string.SimulationMode), false));
    }

    // An int value that represents what ICON is used to show current location on
    // the charts/diagrams. Airplane, Helicopter, Canard ... etc
    public int getDisplayIcon() {
        String val = mPref.getString(mContext.getString(R.string.DisplayIcon), "0");
        try {
            return (Integer.parseInt(val));
        } catch (Exception e) {
        }
        return 0;
    }

    // An int value that represents the seconds before the waypoint to advance
    // to the next leg
    public int getEarlyPassTimer() {
        String val = mPref.getString(mContext.getString(R.string.EarlyPassTimer), "10");
        try {
            return (Integer.parseInt(val));
        } catch (Exception e) {
        }
        return 10;
    }

    /**
     * Get chart cycle previous, next, current
     *
     * @return
     */
    public int getCycleAdjust() {
        String val = mPref.getString(mContext.getString(R.string.Cycle), "0");
        try {
            return (Integer.parseInt(val));
        } catch (Exception e) {
        }
        return 0;
    }

    public boolean isGpsWarn() {
        return (mPref.getBoolean(mContext.getString(R.string.GpsOffWarn), true));
    }

    /**
     * @return
     */
    public String getOrientation() {
        return (mPref.getString(mContext.getString(R.string.OrientationP), "Portrait"));
    }

    public boolean isKeepScreenOn() {
        return (mPref.getBoolean(mContext.getString(R.string.KeepScreenOn), true));
    }

    /**
     * @return
     */
    public boolean isGpsUpdatePeriodShort() {
        return (mPref.getBoolean(mContext.getString(R.string.GpsTime), false));
    }

    /**
     * @return
     */
    public boolean isNightMode() {
        return (mPref.getBoolean(mContext.getString(R.string.NightMode), false));
    }

    /**
     * @param activity
     * @return
     */
    public boolean isNewerVersion(Activity activity) {
        PackageInfo packageInfo = null;
        
        /*
         * Get current version code.
         */
        try {
            packageInfo = activity.getPackageManager()
                    .getPackageInfo("com.ds.avare", 0);
        } catch (Exception e) {
            packageInfo = null;
        }
        
        /*
         * Found.
         */
        if (null != packageInfo) {
            int newCode = packageInfo.versionCode;
            int oldCode = mPref.getInt(mContext.getString(R.string.app_name), 0);
            if (oldCode != newCode) {
                /*
                 * Updated or new
                 */
                SharedPreferences.Editor editor = mPref.edit();
                editor.putInt(mContext.getString(R.string.app_name), newCode);
                editor.commit();
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    public String getChartType() {
        return (mPref.getString(mContext.getString(R.string.ChartType), "0"));
    }

    /**
     * @return
     */
    public String getAirSigMetType() {
        return (mPref.getString(mContext.getString(R.string.AirSigType), "ALL"));
    }

    /**
     * @return
     */
    public void setChartType(String type) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.ChartType), type);
        editor.commit();
    }


    /**
     * @return
     */
    public String getChartType3D() {
        return (mPref.getString(mContext.getString(R.string.ChartType3D), "0"));
    }

    /**
     * @return
     */
    public void setChartType3D(String type) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.ChartType3D), type);
        editor.commit();
    }


    /**
     * @return
     */
    public void setSimMode(boolean sim) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(mContext.getString(R.string.SimulationMode), sim);
        editor.commit();
    }

    /**
     * @return
     */
    public String getDistanceUnit() {
        String val = mPref.getString(mContext.getString(R.string.Units), "0");
        if (val.equals("0")) {
            return (mContext.getString(R.string.UnitKnot));
        } else if (val.equals("1")) {
            return (mContext.getString(R.string.UnitMile));
        } else {
            return (mContext.getString(R.string.UnitKilometer));
        }
    }

    /**
     * @return
     */
    public String mapsFolder() {
        File path = mContext.getFilesDir();
        /*
         * Make it fail safe?
         */
        if (path == null) {
            path = mContext.getCacheDir();
            if (path == null) {
                path = mContext.getExternalCacheDir();
                if (path == null) {
                    path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (path == null) {
                        path = Environment.getExternalStorageDirectory();
                        if (null == path) {
                            path = new File("/mnt/sdcard/avare");
                        }
                    }
                }
            }
        }
        /*
         * If no path, use internal folder.
         * If cannot get internal folder, return / at least
         */
        String loc = mPref.getString(mContext.getString(R.string.Maps), path.getAbsolutePath());
        
        /*
         * XXX: Legacy for 5.1.0 and 5.1.1.
         */
        if (loc.equals("Internal")) {
            loc = mContext.getFilesDir().getAbsolutePath() + "/data";
        } else if (loc.equals("External")) {
            loc = mContext.getExternalFilesDir(null) + "/data";
        }

        return (loc);
    }

    /**
     * @return
     */
    public void setMapsFolder(String folder) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.Maps), folder);
        editor.commit();
    }

    /**
     * @return
     */
    public String loadString(String name) {
        return (mPref.getString(name, null));
    }

    public boolean isDrawTracks() {
        return (mPref.getBoolean(mContext.getString(R.string.TrkUpdShowHistory), false));
    }

    /**
     * @return
     */
    public boolean useAdsbWeather() {
        return (mPref.getBoolean(mContext.getString(R.string.ADSBWeather), false));
    }

    /**
     * @return
     */
    public boolean useNmeaAltitude() {
        return (mPref.getBoolean(mContext.getString(R.string.UseNmeaAltitude), false));
    }

    /**
     * @return
     */
    public int showLayer() {
        String val = mPref.getString(mContext.getString(R.string.LayerTransparency), "255");
        try {
            return (Integer.parseInt(val));
        } catch (Exception e) {
        }
        return 255;
    }

    /**
     * @return
     */
    public int showAdsbTrafficWithin() {
        try {
            return (Integer.parseInt(mPref.getString(mContext.getString(R.string.ADSBTrafficFilter), "100000")));
        } catch (Exception x) {
            return 100000;
        }
    }

    /**
     * @return
     */
    public boolean showAdsbCallSign() {
        return mPref.getBoolean(mContext.getString(R.string.prefShowAdsbCallSign), true);
    }

    /**
     * @return
     */
    public int getTimerRingSize() {
        try {
            return (Integer.parseInt(mPref.getString(mContext.getString(R.string.prefTimerRingSize), "5")));
        } catch (Exception x) {
            return 5;
        }
    }

    /**
     * What type of distance rings are we supposed to show, 0=none, 1=dynamic, 2=static at 2/5/10
     *
     * @return
     */
    public int getDistanceRingType() {
        try {
            return (Integer.parseInt(mPref.getString(mContext.getString(R.string.prefDistanceRingType), "0")));
        } catch (Exception x) {
            return 0;
        }
    }

    /**
     *
     *
     * @return
     */
    public void showDistanceRingStatic() {
        mPref.edit().putString(mContext.getString(R.string.prefDistanceRingType), "2").commit();
    }

    /**
     * @return
     */
    public int getDistanceRingColor() {
        // default color
        int color = Color.rgb(102, 0, 51);

        // configured color
        String prefColor = mPref.getString(mContext.getString(R.string.prefDistanceRingColors), "Default");
        if (prefColor.equals("Red")) {
            color = Color.RED;
        }
        if (prefColor.equals("Blue")) {
            color = Color.BLUE;
        }
        if (prefColor.equals("Black")) {
            color = Color.BLACK;
        }

        return color;
    }

    /**
     * @return 0-Do not auto post, 1-send in an email, 2-user selects an app to handle KML
     */
    public int autoPostTracks() {
        try {
            return (Integer.parseInt(mPref.getString(mContext.getString(R.string.prefAutoPostTracks), "1")));
        } catch (Exception x) {
            return 1;
        }
    }

    /**
     * @return
     */
    public boolean useDynamicFields() {
        return mPref.getBoolean(mContext.getString(R.string.prefUseDynamicFields), true);
    }

    public boolean isBlinkScreen() {
        return mPref.getBoolean(mContext.getString(R.string.blinkScreen), false);
    }

    /**
     * @return
     */
    public String getRowFormats() {
        return mPref.getString(mContext.getString(R.string.prefGetRowFormats), "6,0,0,0,7,8,5 10,0,0,0,3,12,4 6,7,8,5 10,3,12,4");
    }

    /**
     * @param rowFormats
     */
    public void setRowFormats(String rowFormats) {
        mPref.edit().putString(mContext.getString(R.string.prefGetRowFormats), rowFormats).commit();
    }

    /**
     * @return
     */
    public double getOdometer() {
        try {
            return (Double.parseDouble(mPref.getString(mContext.getString(R.string.prefOdometer), "0")));
        } catch (Exception x) {
            return 0;
        }
    }

    /**
     * @param registered
     */
    public void setRegistered(boolean registered) {
        mPref.edit().putBoolean(mContext.getString(R.string.register), registered).commit();
    }

    /**
     * @return
     */
    public boolean isRegistered() {
        return  mPref.getBoolean(mContext.getString(R.string.register), false);
    }

    /**
     * @return
         */
    public String getExternalGpsSource() {
        return mPref.getString(mContext.getString(R.string.externalGps), "0");
    }

    /**
     * @param value
     */
    public void setOdometer(double value) {
        mPref.edit().putString(mContext.getString(R.string.prefOdometer), String.format("%f", value)).commit();
    }

    /**
     * @return
     */
    public boolean getShowCDI() {
        return mPref.getBoolean(mContext.getString(R.string.prefShowCDI), false);
    }

    /**
     * @return
     */
    public boolean isShowEdgeTape() {
        return mPref.getBoolean(mContext.getString(R.string.EdgeTape), false);
    }

    /**
     * 7 is the  glide ratio of most common aircraft like C172 and C182
     *
     * @return
     */
    public float getGlideRatio() {
        String def = "7.0";
        float mratio = Float.parseFloat(def);
        String ratio = mPref.getString(mContext.getString(R.string.GlideRatio), def);
        try {
            mratio = Float.parseFloat(ratio);
        } catch (Exception e) {
            /*
             * Save default
             */
            mPref.edit().putString(mContext.getString(R.string.GlideRatio), def).commit();
        }
        return (mratio);
    }

    /**
     * @return
     */
    public String getLists() {
        return mPref.getString(mContext.getString(R.string.List), "");
    }

    /**
     * @return
     */
    public void putLists(String name) {
        mPref.edit().putString(mContext.getString(R.string.List), name).commit();
    }


    /**
     * @return
     */
    public boolean useBearingForETEA() {
        return mPref.getBoolean(mContext.getString(R.string.ETABearing), true);
    }

    /**
     * @return
     */
    public boolean allowRubberBanding() {
        return mPref.getBoolean(mContext.getString(R.string.rubberBand), true);
    }

    /**
     * @return
     */
    public boolean showCAPGrids() {
        return mPref.getBoolean(mContext.getString(R.string.CAPGrid), false);
    }

    /**
     * @return
     */
    public boolean showTips() {
        return mPref.getBoolean(mContext.getString(R.string.prefTip), false);
    }

    /**
     * @return
     */
    public boolean showPlateInfoLines() {
        return mPref.getBoolean(mContext.getString(R.string.ExtendInfoLines), false);
    }

    /**
     * @return
     */
    public boolean show3DInfoLines() {
        return mPref.getBoolean(mContext.getString(R.string.ExtendInfoLines3D), true);
    }

    /**
     * @return
     */
    public String getUDWLocation() {
        try {
            return mPref.getString(mContext.getString(R.string.UDWLocation), "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * @param udwLocation
     */
    public void setUDWLocation(String udwLocation) {
        mPref.edit().putString(mContext.getString(R.string.UDWLocation), udwLocation).commit();
    }



    // Read all the tab preference selections and return them in a single bitmapped long value
    public long getTabs() {
        long mTabs = 1;

        if (mPref.getBoolean(mContext.getString(R.string.prefTabPlates), true)) {
            mTabs |= 1 << MainActivity.tabPlates;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabAFD), true)) {
            mTabs |= 1 << MainActivity.tabAFD;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabFind), true)) {
            mTabs |= 1 << MainActivity.tabFind;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabThreeD), true)) {
            mTabs |= 1 << MainActivity.tabThreeD;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabPlan), true)) {
            mTabs |= 1 << MainActivity.tabPlan;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabWX), true)) {
            mTabs |= 1 << MainActivity.tabWXB;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabNear), true)) {
            mTabs |= 1 << MainActivity.tabNear;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabChecklist), true)) {
            mTabs |= 1 << MainActivity.tabChecklist;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabTools), true)) {
            mTabs |= 1 << MainActivity.tabTools;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabPfd), true)) {
            mTabs |= 1 << MainActivity.tabPfd;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabWnb), true)) {
            mTabs |= 1 << MainActivity.tabWnb;
        }

        if (mPref.getBoolean(mContext.getString(R.string.prefTabIO), true)) {
            mTabs |= 1 << MainActivity.tabIO;
        }

        return mTabs;
    }

    public void setTipIndex(int index) {
        mPref.edit().putInt(mContext.getString(R.string.tipIndex), index).commit();
    }
    public int getTipIndex() {
        return mPref.getInt(mContext.getString(R.string.tipIndex), 0);
    }

    /**
     * @return
     */
    public String getLongestRunway() {
        return mPref.getString(mContext.getString(R.string.runwayLengths), "0");
    }

    /**
     * @return
     */
    public String getRegisteredEmail() {
        return mPref.getString(mContext.getString(R.string.Email), null);
    }

    /**
     * @param email
     */
    public void setRegisteredEmail(String email) {
        mPref.edit().putString(mContext.getString(R.string.Email), email).commit();
    }

    public boolean getPlanControl() {
        return mPref.getBoolean(mContext.getString(R.string.prefPlanControl), false);
    }

    public boolean getPlanPassage() {
        return mPref.getBoolean(mContext.getString(R.string.prefPlanPassage), false);
    }

    // Get last location known
    public Location getLastLocation() {
        // Default is middle of USA, Kansas City
        float lon = mPref.getFloat(mContext.getString(R.string.GPS) + "lon", -94.5f);
        float lat = mPref.getFloat(mContext.getString(R.string.GPS) + "lat", 39.5f);
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLongitude(lon);
        l.setLatitude(lat);
        return l;
    }

    // Set last location we got
    public void setLastLocation(double lon, double lat) {
        mPref.edit()
                .putFloat(mContext.getString(R.string.GPS) + "lon", (float) lon)
                .putFloat(mContext.getString(R.string.GPS) + "lat", (float) lat)
                .commit();
    }


    /**
     * @return
     */
    public int getExpiryTime() {
        String exp = mPref.getString(mContext.getString(R.string.Expires), "360");
        return Integer.parseInt(exp);
    }

    /**
     * @return
     */
    public String getLayerType() {
        return mPref.getString(mContext.getString(R.string.LayerType), "No Layer");
    }

    public boolean isTrackUp() {
        return mPref.getBoolean(mContext.getString(R.string.TrackUp), false);
    }

    public boolean setTrackUp(boolean trackUp) {
        return mPref.edit().putBoolean(mContext.getString(R.string.TrackUp), trackUp).commit();
    }

    public boolean isTrackUpPlates() {
        return mPref.getBoolean(mContext.getString(R.string.TrackUpPlates), false);
    }

    public boolean setTrackUpPlates(boolean trackUp) {
        return mPref.edit().putBoolean(mContext.getString(R.string.TrackUpPlates), trackUp).commit();
    }

    public boolean isFirstPerson() {
        return mPref.getBoolean(mContext.getString(R.string.FirstPerson), false);
    }

    public boolean setFirstPerson(boolean fp) {
        return mPref.edit().putBoolean(mContext.getString(R.string.FirstPerson), fp).commit();
    }

    public void setLayerType(String layerType) {
        mPref.edit()
                .putString(mContext.getString(R.string.LayerType), layerType)
                .commit();
    }

    public int getAircraftTAS() {
        String speed = mPref.getString(mContext.getString(R.string.AircraftTAS), "");
        int aircraftTAS = 100;
        try {
            aircraftTAS = Integer.parseInt(speed);
        } catch (Exception e) {

        }
        return aircraftTAS;
    }

    public String getAircraftHomeBase() {
        return mPref.getString(mContext.getString(R.string.AircraftHomeBase), "KSBA");
    }

    public String getAircraftEquipment() {
        return mPref.getString(mContext.getString(R.string.AircraftEquipment), "N");
    }

    public String getAircraftSurveillanceEquipment() {
        return mPref.getString(mContext.getString(R.string.AircraftSurveillance), "N");
    }

    public String getAircraftColorPrimary() {
        return mPref.getString(mContext.getString(R.string.AircraftColorPrimary), "W");
    }

    public String getAircraftColorSecondary() {
        return mPref.getString(mContext.getString(R.string.AircraftColorSecondary), "B");
    }

    public String getAircraftType() {
        return mPref.getString(mContext.getString(R.string.AircraftType), "TEST");
    }

    public String getEmergencyNumber() {
        return mPref.getString(mContext.getString(R.string.EmergencyContact), "");
    }

    public String getEmergencyChecklist() {
        return mPref.getString(mContext.getString(R.string.EmergencyChecklist), "");
    }

    public int getAircraftICAOCode() {
        int code = 0;
        try {
            code = Integer.parseInt(mPref.getString(mContext.getString(R.string.AircraftICAOCode), ""));
        }
        catch (Exception e) {

        }
        return code;
    }

    public String getAircraftTailNumber() {
        return mPref.getString(mContext.getString(R.string.AircraftTailNumber), "N1TEST");
    }

    public String getPilotContact() {
        return mPref.getString(mContext.getString(R.string.PilotContact), "TEST PILOT 1-800-WX-BRIEF");
    }

    public String getShapeFileName() {
        return mPref.getString(mContext.getString(R.string.ShapeFile), "custom.shp");
    }

    public int getFuelTimerInterval() {
        String interval = mPref.getString(mContext.getString(R.string.FuelTimerInterval), "30");
        int intervalInt = 30;
        try {
            intervalInt = Integer.parseInt(interval);
        } catch (Exception e) {

        }
        return intervalInt;
    }

    public float getFuelBurn() {
        try {
            return (Float.parseFloat(mPref.getString(mContext.getString(R.string.FuelRateLabel), "10")));
        } catch (Exception x) {
            return 10;
        }
    }

    public boolean removeB1Plate() {
        return mPref.getBoolean(mContext.getString(R.string.b1plate), false);
    }
    public boolean removeB2Plate() {
        return mPref.getBoolean(mContext.getString(R.string.b2plate), false);
    }
    public boolean removeB1Map() {
        return mPref.getBoolean(mContext.getString(R.string.b1map), false);
    }

    public boolean removeB3Plate() {
        return mPref.getBoolean(mContext.getString(R.string.b3plate), false);
    }

    public int getRateAskCount() {
        return mPref.getInt("rateAskLastCount", 0);
    }

    public void setRateAskCount(int set) {
        mPref.edit().putInt("rateAskLastCount", set).commit();
    }

    public boolean isVerticalPfd() {
        return mPref.getBoolean(mContext.getString(R.string.VerticalPfd), false);
    }

    public int getWindsAloftCeiling() {
        try {
           return Integer.parseInt(mPref.getString(mContext.getString(R.string.WindsAloftCeiling), "39"));
        } catch (Exception x) {
            return 39;
        }
    }


    /**
      * @return
      */
     public String getGeotags() {
        return mPref.getString(mContext.getString(R.string.Geotag), "");
     }


    /**
      * @param tags
      */
    public void setGeotags(String tags) {
        mPref.edit().putString(mContext.getString(R.string.Geotag), tags).commit();
    }

    /**
     *
     * @return
     */
    public boolean showGameTFRs() {
        return mPref.getBoolean(mContext.getString(R.string.GameTFR), false);
    }

    public void enableGameTFRs() {
        mPref.edit().putBoolean(mContext.getString(R.string.GameTFR), true).commit();
    }

    /**
     * @return
     */
    public String getWnbs() {
        return mPref.getString(mContext.getString(R.string.Wnb), "");
    }

    /**
     * @return
     */
    public void putWnbs(String name) {
        mPref.edit().putString(mContext.getString(R.string.Wnb), name).commit();
    }

    public boolean isDefaultAFDImage() {
        return mPref.getBoolean(mContext.getString(R.string.DefaultAFD), false);
    }

    public String getWiFiPort() {
        return mPref.getString(mContext.getString(R.string.WIFIPort), "4000");
    }
}


