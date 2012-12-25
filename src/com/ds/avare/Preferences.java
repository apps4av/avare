/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Preferences for main activity
 */
public class Preferences {


    /*
     * BOS is default
     */
    public static final String BASE = "BOS"; 
    
    /*
     * MAX number of elements
     */
    public static final int MAX_RECENT = 15; 
    
    /*
     * TFR update period for slow and fast updates
     */
    public static final long TFR_UPDATE_PERIOD_MS = 60 * 1000;
    public static final long TFR_GET_PERIOD_MIN = 60;
    
    /*
     * Max memory and max screen size it will support
     */
    public static final long MEM_128 = 128 * 1024 * 1024;
    public static final long MEM_64 = 64 * 1024 * 1024;
    public static final long MEM_32 = 32 * 1024 * 1024;
    public static final long MEM_16 = 16 * 1024 * 1024;
    
    public static final int MEM_128_X = 9;
    public static final int MEM_128_Y = 7;
    public static final int MEM_64_X = 7;
    public static final int MEM_64_Y = 5;
    public static final int MEM_32_X = 5;
    public static final int MEM_32_Y = 3;
    public static final int MEM_16_X = 3;
    public static final int MEM_16_Y = 3;
    
    /**
     * Preferences
     */
    private SharedPreferences mPref;
    private Context mContext;

    /**
     * 
     * @param ctx
     */
    public Preferences(Context ctx) {
        /*
         * Load preferences.
         */
        mContext = ctx;
        /*
         * Get/set path for maps:
         * If path not already set, then set in this order:
         * 1. External
         * 2. Internal
         * 3. External Cache
         * 4. Internal Cache
         */
        mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String path = mPref.getString(mContext.getString(R.string.Maps), null);
        if(null == path) {
            File dir = mContext.getExternalFilesDir(null);
            if(dir == null) {
                dir = mContext.getFilesDir();
            }
            if(dir == null) {
                dir = mContext.getExternalCacheDir();
            }
            if(dir == null) {
                dir = mContext.getCacheDir();
            }
            path = dir.getAbsolutePath();
            SharedPreferences.Editor editor = mPref.edit();
            editor.putString(mContext.getString(R.string.Maps), path);
            editor.commit();
        }
    }

    /**
     * 
     * @return
     */
    public String[] getRecent() {
        String recent = mPref.getString(mContext.getString(R.string.Recent),
                getBase() + ",");
        String[] tokens = recent.split(",");
        return tokens;
    }

    /**
     * 
     * @return
     */
    public void addToRecent(String name) {
        String[] tokens = getRecent();
        List<String> l = new LinkedList<String>(Arrays.asList(tokens));
        for(int id = 0; id < l.size(); id++) {
            if(l.get(id).equals(name)) {
                l.remove(id);
            }
        }
        l.add(0, name);
        if(l.size() > MAX_RECENT) {
            l = l.subList(0, MAX_RECENT - 1);
        }
        
        String recent = "";
        for(int id = 0; id < l.size(); id++) {
            recent = recent + l.get(id) + ",";
        }
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(mContext.getString(R.string.Recent), recent);
        editor.commit();
    }

    /**
     * 
     * @return
     */
    public String getBase() {
        return(mPref.getString(mContext.getString(R.string.Base), BASE));
    }

    /**
     * 
     * @return
     */
    public int[] getTilesNumber() {
        int[] ret = new int[2];
        
        /*
         * Find max tiles this system can support.
         */
        long mem = Runtime.getRuntime().maxMemory();

        if(mem >= MEM_128) {
            ret[0] = MEM_128_X;
            ret[1] = MEM_128_Y;
        }
        else if(mem >= MEM_64) {
            ret[0] = MEM_64_X;
            ret[1] = MEM_64_Y;
        }
        else if(mem >= MEM_32) {
            ret[0] = MEM_32_X;
            ret[1] = MEM_32_Y;
        }
        else if(mem >= MEM_16) {
            ret[0] = MEM_16_X;
            ret[1] = MEM_16_Y;
        }
                
        return ret;  
    }

    /**
     * 
     * @return
     */
    public boolean isTrackEnabled() {
        return(mPref.getBoolean(mContext.getString(R.string.ShowTrack), true));
    }
    
    /**
     * 
     * @return
     */
    public boolean isSimulationMode() {
        return(mPref.getBoolean(mContext.getString(R.string.SimulationMode), false));
    }

    /**
     * 
     * @return
     */
    public boolean shouldGpsWarn() {
        return(mPref.getBoolean(mContext.getString(R.string.GpsOffWarn), true));
    }

    /**
     * 
     * @return
     */
    public boolean isPortrait() {
        return(mPref.getBoolean(mContext.getString(R.string.OrientationP), false));
    }

    /**
     * 
     * @return
     */
    public boolean shouldTFRAndMETARShow() {
        return(mPref.getBoolean(mContext.getString(R.string.TFRAndMETAR), false));
    }

    /**
     * 
     * @return
     */
    public boolean shouldScreenStayOn() {
        return(mPref.getBoolean(mContext.getString(R.string.ScreenOn), true));
    }

    /**
     * 
     * @return
     */
    public boolean isGpsUpdatePeriodShort() {
        return(mPref.getBoolean(mContext.getString(R.string.GpsTime), false));
    }

    /**
     * 
     * @return
     */
    public String getChartType() {
        return(mPref.getString(mContext.getString(R.string.ChartType), "0"));
    }

    /**
     * 
     * @return
     */
    public String mapsFolder() {
        String path = mPref.getString(mContext.getString(R.string.Maps), null);
        return(path);
    }

    /**
     * 
     * @return
     */
    public void saveString(String name, String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(name, value);
        editor.commit();
    }

    /**
     * 
     * @return
     */
    public String loadString(String name) {
        return(mPref.getString(name, null));
    }

}
