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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
    private Preferences mPreferences;
    
    private AlertDialog mDestDialog;
    
    private View mDestView;
    
    private Toast mToast;
    
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
            else if(!mService.getDBResource().isOpen()) {
                mLocationView.updateErrorStatus(getString(R.string.LoadingMaps));
            }
            else if(!(new File(mPreferences.mapsFolder() + "/tiles")).exists()) {
                mLocationView.updateErrorStatus(getString(R.string.MissingMaps));
            }
            else if(mPreferences.isSimulationMode()) {
                mLocationView.updateErrorStatus(getString(R.string.SimulationMode));                
            }
            else if(Gps.isGpsDisabled(getApplicationContext(), mPreferences)) {
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
        mPreferences = new Preferences(this);

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.location, null);
        setContentView(view);
        mLocationView = (LocationView)view.findViewById(R.id.location);

        mDestDialog = new AlertDialog.Builder(this).create();
        mDestDialog.setTitle(getString(R.string.DestinationPrompt));
        
        mDestView = layoutInflater.inflate(R.layout.destination, null);
        mDestDialog.setView(mDestView);

        /*
         * Throw this in case GPS is disabled.
         */
        if(Gps.isGpsDisabled(getApplicationContext(), mPreferences)) {
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

            if(mService.getDBResource().isOpen()) {
            	mService.getDBResource().close();
            }
            mService.getDBResource().open(mPreferences.mapsFolder() + "/" + getString(R.string.DatabaseName));
            if(!mService.getDBResource().isOpen()) {
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
            if(mPreferences.isSimulationMode() && (null != mDestination)) {
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

        if(null != mDestDialog) {
            try {
                mDestDialog.dismiss();
            }
            catch (Exception e) {
            }
        }

        mLocationView.freeTiles();
        
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

        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.location, menu);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()) {
        
            case R.id.pref:
                /*
                 * Bring up preferences
                 */
                startActivity(new Intent(this, PrefActivity.class));
                break;
                
            case R.id.help:
                Intent intent = new Intent(this, WebActivity.class);
                intent.putExtra("url", NetworkHelper.getHelpUrl());
                startActivity(intent);
                break;
                
            case R.id.donate:
                Intent donint = new Intent(this, WebActivity.class);
                donint.putExtra("url", NetworkHelper.getDonationURL());
                startActivity(donint);
                break;

            case R.id.download:
                startActivity(new Intent(LocationActivity.this, ChartsDownloadActivity.class));
                break;

            case R.id.gps:
                startActivity(new Intent(LocationActivity.this, SatelliteActivity.class));
                break;

            case R.id.newdestination:
                if(null == mService) {
                    return false;                    
                }
                if(null == mService.getDBResource()) {
                    return false;
                }
                if(!mService.getDBResource().isOpen()) {
                    return false;
                }
                
                /*
                 * Query new destination
                 * Present an alert dialog with a text field
                 */


                /*
                 *  limit FAA/ICAO code length to 4
                 */
                final EditText tv = (EditText)mDestView.findViewById(R.id.DestText);
                tv.setImeOptions(EditorInfo.IME_ACTION_DONE);
                tv.setText(mPreferences.getBase());
                tv.selectAll();

                /*
                 * Spinner calls repeatedly location 0 even when not selected.
                 * Make location 0 non valid.
                 */
                String [] vals = mPreferences.getRecent();
                String[] valsPos0 = new String[vals.length + 1];
                valsPos0[0] = getString(R.string.Recent);
                for(int i = 0; i < vals.length; i++) {
                    valsPos0[i + 1] = vals[i];
                }
                
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(LocationActivity.this,
                        R.layout.input, valsPos0);

                final Spinner lv = (Spinner)mDestView.findViewById(R.id.destList);
               
                lv.setAdapter(adapter);

                /*
                 * Listen to list selection, do it last spinner calls it immediately
                 */
                lv.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        if(0 == position) {
                            return;
                        }
                        String dst = adapter.getItem(position);
                        mDestination = new Destination(dst, mPreferences, mService.getDBResource());
                        mDestination.addObserver(LocationActivity.this);
                        mDestination.find();
                        mDestDialog.dismiss();                        
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });

                tv.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        /*
                         * Clear hint when user touches the edit box
                         */
                        tv.setText("");
                        return false;
                    }
                });


                Button ok = (Button)mDestView.findViewById(R.id.destbuttonOK);
                ok.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        String dst = tv.getText().toString();
                        mDestination = new Destination(dst, mPreferences, mService.getDBResource());
                        mDestination.addObserver(LocationActivity.this);
                        mDestination.find();
                        mDestDialog.dismiss();
                        
                    }
                });

                Button cancel = (Button)mDestView.findViewById(R.id.destbuttonCancel);
                cancel.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        mDestDialog.dismiss();
                    }
                });

                mDestDialog.show();


                break;
        }
        return true;
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
                mLocationView.updateParams(new GpsParams(mDestination.getLocation()));
                if(mService != null) {
                    mService.setDestination((Destination)arg0);
                }
                mLocationView.updateDestination(mDestination);
                mPreferences.addToRecent(mDestination.getID());
                
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
