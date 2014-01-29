/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.Helper;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;

/**
 * 
 * @author zkhan
 * Modified by damccull
 */
public class PrefActivity extends PreferenceActivity {
    //Constants for xml file specification
    public static final String PREF_UI = "pref_ui";
    public static final String PREF_DISPLAY = "pref_display";
    public static final String PREF_GPS = "pref_gps";
    public static final String PREF_WEATHER = "pref_weather";
    public static final String PREF_STORAGE = "pref_storage";

    private StorageService mService;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState); 

//        addPreferencesFromResource(R.xml.preferences);
        //setContentView(R.layout.preferences);
        mService = null;


        //Legacy Headers support
        String action = getIntent().getAction();
        if (action != null) {
            //If we have an intent with action, check which preferences
            //screen to add to this activity
            if (action.equals(PREF_UI)) {
                addPreferencesFromResource(R.xml.pref_ui);
            } else if (action.equals(PREF_GPS)) {
                addPreferencesFromResource(R.xml.pref_gps);
            } else if (action.equals(PREF_DISPLAY)) {
                addPreferencesFromResource(R.xml.pref_display);
            } else if (action.equals(PREF_WEATHER)) {
                addPreferencesFromResource(R.xml.pref_weather);
            } else if (action.equals(PREF_STORAGE)) {
                addPreferencesFromResource(R.xml.pref_general);
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            //No intent, and we're not Honeycomb+ so add the legacy headers instead
            addPreferencesFromResource(R.xml.pref_headers_legacy);
        }
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();

        Helper.setOrientationAndOn(this);
        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    /**
     *
     */
    @Override
    public void onPause() {
        super.onPause();


        getApplicationContext().unbindService(mConnection);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Returns true if the fragment is valid or if on a version of android prior to kitkat.
     *
     * @param fragmentName Fragment.class.getName()
     * @return True or False
     */
    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected boolean isValidFragment(String fragmentName) {
        if (fragmentName.equals(SettingsFragment.class.getName())) {
            return true;
        }
        return super.isValidFragment(fragmentName);
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     * <p/>
     * A fragment that can be used for all settings screens. Pass an extra with the
     * name "settings" and a value to be matched in this method.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");

            if (settings.equals(PrefActivity.PREF_UI)) {
                addPreferencesFromResource(R.xml.pref_ui);
            } else if (settings.equals(PrefActivity.PREF_GPS)) {
                addPreferencesFromResource(R.xml.pref_gps);
            } else if (settings.equals(PrefActivity.PREF_WEATHER)) {
                addPreferencesFromResource(R.xml.pref_weather);
            } else if (settings.equals(PrefActivity.PREF_DISPLAY)) {
                addPreferencesFromResource(R.xml.pref_display);
            } else if (settings.equals(PrefActivity.PREF_STORAGE)) {
                addPreferencesFromResource(R.xml.pref_general);
            }

        }
    }
    
    
    /*
 * Start GPS
 */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }
    };
    /** Defines callbacks for service binding, passed to bindService() */
    /**
     * 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


}
