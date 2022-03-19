/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
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
import android.location.GpsStatus;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.Logger;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class IOActivity extends Activity {
    
    private TextView mTextLog;
    private Spinner mIO;
    private StorageService mService;
    private Location mCurrentLocation;
    private GpsStatus mCurrentGpsStatus;


    private Preferences mPref;

    private Fragment[] mFragments = new Fragment[10];

    private WifiManager.MulticastLock mMulticastLock;

    @Override
    public void onBackPressed() {
        MainActivity m = (MainActivity)this.getParent();
        if(m != null) {
            m.showMapTab();
        }
    }

    /*
     * Start GPS
     */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
            mCurrentGpsStatus = gpsStatus;
        }

        @Override
        public void locationCallback(Location location) {
            mCurrentLocation = location;
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            if(timeout) {
                mCurrentLocation = null;
                mCurrentGpsStatus = null;
            }
        }

        @Override
        public void enabledCallback(boolean enabled) {

        }
    };

    public StorageService getService() {
        return mService;
    }
    public Location getLocation() {
        return mCurrentLocation;
    }
    public GpsStatus getGpsStatus() {
        return mCurrentGpsStatus;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : mFragments) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

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
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.activity_io, null);
        mTextLog = (TextView)view.findViewById(R.id.main_text_log);
        mIO = (Spinner)view.findViewById(R.id.main_spinner_ios);
        Logger.setTextView(mTextLog);
        Logger.setContext(this);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(view);
        mPref = new Preferences(getApplicationContext());

        Bundle args = new Bundle();
        int pos = 0;
        mFragments[pos++] = new BlueToothInFragment();
        mFragments[pos++] = new WiFiInFragment();
        mFragments[pos++] = new XplaneFragment();
        mFragments[pos++] = new MsfsFragment();
        mFragments[pos++] = new BlueToothOutFragment();
        mFragments[pos++] = new FileFragment();
        mFragments[pos++] = new GPSSimulatorFragment();
        mFragments[pos++] = new USBInFragment();
        mFragments[pos++] = new Dump1090Fragment();
        mFragments[pos++] = new ToolsFragment();

        for(int i = 0; i < pos; i++) {
            mFragments[i].setArguments(args);
        }

        mIO.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPref.setFragmentIndex(position);
                changeFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter adapter = new ArrayAdapter<CharSequence>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new String[] {
                getString(R.string.Bluetooth), 
                getString(R.string.WIFI), 
                getString(R.string.XPlane), 
                getString(R.string.MSFS), 
                getString(R.string.AP), 
                getString(R.string.Play), 
                getString(R.string.GPSSIM), 
                getString(R.string.USBIN),
                "Dump1090",
                getString(R.string.Tools)
                });
        mIO.setAdapter(adapter);
        mIO.setSelection(mPref.getFragmentIndex());

        // Acquire Multicast Lock to receive multicast packets over Wifi.
        WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wm.createMulticastLock("avarehelper");
        mMulticastLock.acquire();
    }

    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*
         * Clean up stuff on exit
         */
        getApplicationContext().unbindService(mConnection);        

        // Release multicast lock.
        mMulticastLock.release();
    }

    /**
     * Select fragment
     * @param id
     */
    private void changeFragment(int id) {
        if(id >= 0) {

            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Store fragment we are showing now
            mPref.setFragmentIndex(id);

            switch(id) {

                case 0:
                    BlueToothInFragment btin = (BlueToothInFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, btin);
                    break;

                case 1:
                    WiFiInFragment wfin = (WiFiInFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, wfin);
                    break;

                case 2:
                    XplaneFragment xp = (XplaneFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, xp);
                    break;
                case 3:
                    MsfsFragment msfs = (MsfsFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, msfs);
                    break;
                case 4:
                    BlueToothOutFragment btout  = (BlueToothOutFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, btout);
                    break;
                case 5:
                    FileFragment file = (FileFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, file);
                    break;
                case 6:
                    GPSSimulatorFragment gpsSim = (GPSSimulatorFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, gpsSim);
                    break;
                case 7:
                    USBInFragment usbin = (USBInFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, usbin);
                    break;
                case 8:
                    Dump1090Fragment d1090 = (Dump1090Fragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, d1090);
                    break;
                case 9:
                    ToolsFragment tools = (ToolsFragment) mFragments[id];
                    fragmentTransaction.replace(R.id.detailFragment, tools);
                    break;
            }

            fragmentTransaction.commit();

        }
        else {

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(this);
        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, 0);

        changeFragment(mPref.getFragmentIndex());

    }

}
