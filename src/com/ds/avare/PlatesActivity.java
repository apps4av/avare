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


import java.util.ArrayList;
import java.util.List;

import com.ds.avare.gdl90.AdsbStatus;
import com.ds.avare.gdl90.Id6364Product;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlatesActivity extends Activity {
    
    private Preferences mPref;
    private PlatesView mPlatesView;
    private StorageService mService;
    private Destination mDestination;
    private Button mCenterButton;
    private Toast mToast;
    private Spinner mSpinner;
    private List<String> mList;
    private float[] mMatrix;
    private boolean mIgnoreFocus;

    /*
     * Start GPS
     */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
                GpsParams params = new GpsParams(location); 
                
                /*
                 * Store GPS last location in case activity dies, we want to start from same loc
                 */
                mPlatesView.updateParams(params); 
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            /*
             *  No GPS signal
             *  Tell location view to show GPS status
             */
            if(mPref.isSimulationMode()) {
                mPlatesView.updateErrorStatus(getString(R.string.SimulationMode));                
            }
            else if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
                /*
                 * Prompt user to enable GPS.
                 */
                mPlatesView.updateErrorStatus(getString(R.string.GPSEnable)); 
            }
            else if(timeout) {
                mPlatesView.updateErrorStatus(getString(R.string.GPSLost));
            }
            else {
                /*
                 *  GPS kicking.
                 */
                mPlatesView.updateErrorStatus(null);
            }           
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }

        @Override
        public void adbsMessageCallbackNexrad(Id6364Product pn) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void adbsStatusCallback(AdsbStatus adsbStatus) {
            // TODO Auto-generated method stub
            
        }
    };

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).switchTab(0);
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mPref = new Preferences(getApplicationContext());
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plates, null);
        setContentView(view);
        mPlatesView = (PlatesView)view.findViewById(R.id.plates);
        mIgnoreFocus = true;
        
        mCenterButton = (Button)view.findViewById(R.id.plates_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mPlatesView.center();
            }
            
        });

        /*
         * Spinner to select a plate
         */
        mSpinner = (Spinner)view.findViewById(R.id.plates_spinner);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
        
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                if(mDestination != null && mService != null && mList != null) {
                    /*
                     * If moving to invalid position (this happens with spinner automatically,
                     * then if last plate was not 0, then use it.
                     * Persistence of plates.
                     */
                    /*
                     * mIgnoreFocus will get rid of spinner position 0 call on onResume()
                     */
                    if(mIgnoreFocus) {
                        pos = mService.getPlateIndex();
                        mIgnoreFocus = false;
                    }
                    String[] plates = mDestination.getPlates();
                    if(plates != null) {
                        if(pos >= plates.length) {
                            pos = 0;
                        }
                        mSpinner.setSelection(pos);
                        mService.loadDiagram(plates[pos] + Preferences.IMAGE_EXTENSION);
                        mPlatesView.setBitmap(mService.getDiagram());
                        String name = mList.get(pos);
                        if(name.equals(Destination.AD)) {
                            mPlatesView.setParams(mMatrix);
                        }
                        else {
                            mPlatesView.setParams(null);
                        }
                        mService.setPlateIndex(pos);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
            
        /*
         * Create toast beforehand so multiple clicks don't throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

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

            mList = new ArrayList<String>();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(PlatesActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);            

            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();
            if(null == mDestination) {
                mPlatesView.setBitmap(null);
                mToast.setText(getString(R.string.ValidDest));
                mToast.show();
                return;
            }
            
            /*
             * Start adding plates
             */
            /*
             * Not found
             */
            if((!mDestination.isFound()) || (mDestination.getPlates() == null)) {
                mPlatesView.setBitmap(null);
                mToast.setText(getString(R.string.PlatesNF));
                mToast.show();
                return;                
            }
            
            /*
             * Airport diagram found
             */

            /*
             * Now add all plates to the list
             */
            String[] plates = mDestination.getPlates();
            for(int plate = 0; plate < plates.length; plate++) {
                String tokens[] = plates[plate].split("/");
                mList.add(tokens[tokens.length - 1]);
            }

            /*
             * A list of plates available for this airport
             */
            adapter = new ArrayAdapter<String>(PlatesActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);            

            /*
             * Find lon/lat px from database
             */
            mMatrix = mDestination.getMatrix();
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

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
    }

    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();
        mIgnoreFocus = true;
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
    public void onDestroy() {
        super.onDestroy();
    }
}
