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
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.view.Window;

/**
 * 
 * @author zkhan
 *
 */
public class PrefActivity extends PreferenceActivity {
    private StorageService mService;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Helper.setTheme(this);

        // apply this for android v35 or above, opt out of edge to edge enforcement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            this.getTheme().applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false);
        }

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        setContentView(R.layout.preferences);
        mService = StorageService.getInstance();
    }

    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();
        
        Helper.setOrientationAndOn(this);
        mService.registerGpsListener(mGpsInfc);
    }

    /**
     * 
     */
    @Override
    public void onPause() {
        super.onPause();

        /*
         * This should update preferences in static memory
         */
        Preferences pref = new Preferences(this);

        mService.unregisterGpsListener(mGpsInfc);

        /*
         * This will will sure we update tiles when someone changes storage folder
         */
        mService.getTiles().forceReload();
    }
}
