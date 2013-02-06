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
import java.util.Observable;
import java.util.Observer;
import com.ds.avare.R;
import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;

import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

/**
 * @author zkhan
 * Main activity
 */
public class LocationActivity extends Activity implements Observer {

    /**
     * This view display location on the map.
     */
    private LocationView mLocationView;
    /**
     * Shows warning message about Avare
     */
    private AlertDialog mAlertDialogWarn;
    /**
     * Shows warning about GPS
     */
    private AlertDialog mGpsWarnDialog;
    /**
     * Current destination info
     */
    private Destination mDestination;
    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;

    /**
     * App preferences
     */
    private Preferences mPref;
    
    private Toast mToast;
    
    private Button mDestButton;
    private Button mHelpButton;
    private Button mPrefButton;
    private Button mGpsButton;
    private Button mDownloadButton;
    private Button mMenuButton;
    private Bundle mExtras;

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null && mService != null) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
                GpsParams params = new GpsParams(location);
                
                /*
                 * Store GPS last location in case activity dies, we want to start from same loc
                 */
                mLocationView.updateParams(params); 
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            /*
             *  No GPS signal
             *  Tell location view to show GPS status
             */
            if(null == mService) {
                mLocationView.updateErrorStatus(getString(R.string.Init));
            }
            else if(!(new File(mPref.mapsFolder() + "/tiles")).exists()) {
                mLocationView.updateErrorStatus(getString(R.string.MissingMaps));
            }
            else if(mPref.isSimulationMode()) {
                mLocationView.updateErrorStatus(getString(R.string.SimulationMode));                
            }
            else if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
                /*
                 * Prompt user to enable GPS.
                 */
                mLocationView.updateErrorStatus(getString(R.string.GPSEnable)); 
            }
            else if(timeout) {
                mLocationView.updateErrorStatus(getString(R.string.GPSLost));
            }
            else {
                /*
                 *  GPS kicking.
                 */
                mLocationView.updateErrorStatus(null);
            }           
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }          
    };
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mPref = new Preferences(this);

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.location, null);
        setContentView(view);
        mLocationView = (LocationView)view.findViewById(R.id.location);
        
        /*
         * To be notified of some action in the view
         */
        mLocationView.setGestureCallback(new GestureInterface() {

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GestureInterface#gestureCallBack(int, java.lang.String)
             */
            @Override
            public void gestureCallBack(int event, String airport) {
                if(GestureInterface.LONG_PRESS == event) {
                    /*
                     * Show the animation button for dest
                     */
                    mDestButton.setText(airport);
                    AnimateButton a = new AnimateButton(getApplicationContext(), mDestButton);
                    a.animate(true);
                }
            }
            
        });

        mMenuButton = (Button)view.findViewById(R.id.location_button_menu);
        mMenuButton.getBackground().setAlpha(127);
        mMenuButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mDestButton.setText(getString(R.string.Destination));
                AnimateButton a = new AnimateButton(getApplicationContext(), mDestButton);
                AnimateButton b = new AnimateButton(getApplicationContext(), mHelpButton);
                AnimateButton d = new AnimateButton(getApplicationContext(), mDownloadButton);
                AnimateButton e = new AnimateButton(getApplicationContext(), mGpsButton);
                AnimateButton f = new AnimateButton(getApplicationContext(), mPrefButton);
                a.animate(true);
                b.animate(true);
                d.animate(true);
                e.animate(true);
                f.animate(true);
            }
            
        });

        mHelpButton = (Button)view.findViewById(R.id.location_button_help);
        mHelpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationActivity.this, WebActivity.class);
                intent.putExtra("url", NetworkHelper.getHelpUrl());
                startActivity(intent);
            }
            
        });

        mGpsButton = (Button)view.findViewById(R.id.location_button_gps);
        mGpsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(LocationActivity.this, SatelliteActivity.class));
            }
            
        });

        mDownloadButton = (Button)view.findViewById(R.id.location_button_dl);
        mDownloadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {    
                startActivity(new Intent(LocationActivity.this, ChartsDownloadActivity.class));
            }
        });

        mPrefButton = (Button)view.findViewById(R.id.location_button_pref);
        mPrefButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Bring up preferences
                 */
                startActivity(new Intent(LocationActivity.this, PrefActivity.class));

            }
            
        });

        /*
         * Dest button
         */
        mDestButton = (Button)view.findViewById(R.id.location_button_dest);
        mDestButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */

                Button b = (Button)v;
                /*
                 * If button pressed was a destination go there, otherwise ask for dest
                 */
                if(!b.getText().toString().equals(getString(R.string.Destination))) {
                    
                    String type = Destination.BASE;
                    if(b.getText().toString().contains("&")) {
                        type = Destination.GPS;
                    }
                    mDestination = new Destination(b.getText().toString(), type, mPref, mService);
                    mDestination.addObserver(LocationActivity.this);
                    mToast.setText(getString(R.string.Searching) + " " + b.getText().toString());
                    mToast.show();
                    mDestination.find();
                    return;
                }

                Intent i = new Intent(LocationActivity.this, PlanActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        });
        

        /*
         * Throw this in case GPS is disabled.
         */
        if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
            mGpsWarnDialog = new AlertDialog.Builder(LocationActivity.this).create();
            mGpsWarnDialog.setTitle(getString(R.string.GPSEnable));
            mGpsWarnDialog.setButton(getString(R.string.Yes),  new DialogInterface.OnClickListener() {
                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            });
            mGpsWarnDialog.setButton2(getString(R.string.No),  new DialogInterface.OnClickListener() {
                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            mGpsWarnDialog.show();
        }
        
        /*
         * Check if this was sent from Google Maps
         */
        mExtras = getIntent().getExtras();
 
        mService = null;
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

            /*
             * Must do this here since bitmap needs to be reclaimed
             */
            mLocationView.setTiles(mService.getTiles());
            
            if(!mService.getDBResource().isPresent()) {
                /*
                 * If could not open database then bring up download activity.
                 */
                startActivity(new Intent(LocationActivity.this, ChartsDownloadActivity.class));
                return;
            }

            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();

            /*
             * Now set location.
             */
            Location l = Gps.getLastLocation(getApplicationContext());
            if(mPref.isSimulationMode() && (null != mDestination)) {
                l = mDestination.getLocation();
            }
            if(null != l) {
                mService.setGpsParams(new GpsParams(l));
            }
            
            if(null != mService.getGpsParams()) {
                mLocationView.initParams(mService.getGpsParams(), mService); 
                mLocationView.updateParams(mService.getGpsParams());
            }

            mLocationView.updateDestination(mDestination);

            /*
             * Show avare warning when service says so 
             */
            if(mService.shouldWarn()) {
             
                mAlertDialogWarn = new AlertDialog.Builder(LocationActivity.this).create();
                mAlertDialogWarn.setTitle(getString(R.string.WarningMsg));
                mAlertDialogWarn.setMessage(getString(R.string.Warning));
                mAlertDialogWarn.setCanceledOnTouchOutside(true);
                mAlertDialogWarn.setCancelable(true);
                mAlertDialogWarn.setButton(getString(R.string.OK),  new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    
                mAlertDialogWarn.show();
            }    
            
            /*
             * See if we got an intent to search for address as dest
             */
            if(null != mExtras) {
                String addr = mExtras.getString(Intent.EXTRA_TEXT);
                if(addr != null) {
                    
                    /*
                     * , cannot be saved in prefs
                     */
                    addr = StringPreference.formatAddressName(addr);
                    
                    mDestination = new Destination(addr, Destination.MAPS, mPref, mService);
                    mDestination.addObserver(LocationActivity.this);
                    mToast.setText(getString(R.string.Searching) + " " + addr);
                    mToast.show();
                    mDestination.find();
                }
                mExtras = null;
            }

        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
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
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);

        if(null != mAlertDialogWarn) {
            try {
                mAlertDialogWarn.dismiss();
            }
            catch (Exception e) {
            }
        }

        if(null != mGpsWarnDialog) {
            try {
                mGpsWarnDialog.dismiss();
            }
            catch (Exception e) {
            }
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        if(null != mGpsWarnDialog) {
            try {
                mGpsWarnDialog.dismiss();
            }                
            catch (Exception e) {   
            }
        }
        
        /*
         * Clear tiles memory
         */
        mLocationView.setTiles(null);

        super.onDestroy();
    }

    /**
     * 
     */
    @Override
    public void update(Observable arg0, Object arg1) {
        /*
         * Destination found?
         */
        if(arg0 instanceof Destination) {
            Boolean result = (Boolean)arg1;
            if(result) {
            
                /*
                 * Temporarily move to destination by giving false GPS signal.
                 */
                if(null == mLocationView || null == mDestination) {
                    mToast.setText(getString(R.string.DestinationNF));
                    mToast.show();
                    return;
                }
                mLocationView.updateParams(new GpsParams(mDestination.getLocation()));
                if(mService != null) {
                    mService.setDestination((Destination)arg0);
                }
                mLocationView.updateDestination(mDestination);
                mPref.addToRecent(mDestination.getStorageName());
                
                mToast.setText(getString(R.string.DestinationSet) + ((Destination)arg0).getID());
                mToast.show();
            }
            else {
                mToast.setText(getString(R.string.DestinationNF));
                mToast.show();
            }
        }
    }
}
