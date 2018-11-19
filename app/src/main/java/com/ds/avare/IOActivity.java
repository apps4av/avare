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


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import com.ds.avare.connections.Connection;
import com.ds.avare.connections.WifiConnection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.Logger;

/**
 * @author zkhan
 * An activity that deals with external devices providing data
 */
public class IOActivity extends Activity {

    private Connection mWifi;
    private Preferences mPref;

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).showMapTab();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mPref = new Preferences(getApplicationContext());
        mWifi = WifiConnection.getInstance(this);

        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.io, null);
        setContentView(view);
        Logger.setContext(this);
        Logger.setTextView((TextView)view.findViewById(R.id.io_textview_logger));

        final CheckBox cbwifi = view.findViewById(R.id.io_checkbox_connect_wifi);
        cbwifi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // connect on check box checked
                TextView text = findViewById(R.id.io_textview_logger);
                if (((CheckBox)view).isChecked()) {
                    // connect
                    if (mWifi.connect(mPref.getWiFiPort(), false)) {
                        mPref.setIOenabled(true);
                        text.setTextColor(Color.WHITE);
                        mWifi.start(mPref);
                    } else {
                        cbwifi.setChecked(false);
                        text.setTextColor(Color.GRAY);
                    }
                } else {
                    mPref.setIOenabled(false);
                    text.setTextColor(Color.GRAY);
                    mWifi.stop();
                    mWifi.disconnect();
                }
            }
        });

        // Send data on avare open aidl interface to keep compatibility with external IO and with HIZ app.
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // remember last state
        if (mPref.getIOenabled()) {
            cbwifi.setChecked(true);
            if (!mWifi.isConnected()) {
                if (mWifi.connect(mPref.getWiFiPort(), false)) {
                    mWifi.start(mPref);
                }
            }
            TextView text = findViewById(R.id.io_textview_logger);
            text.setTextColor(Color.WHITE);
            text.setText(getString(R.string.WIFIListening));
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mWifi.setHelper(binder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();
        Helper.setOrientationAndOn(this);
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
