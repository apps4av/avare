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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Runway;
import com.ds.avare.storage.DataBaseHelper;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class AirportActivity extends Activity {
    
    private StorageService mService;
    private Destination mDestination;
    private ListView mAirport;
    private Toast mToast;
    private AfdView mAfdView;
    private Spinner mSpinner;
    private List<String> mList;
    private boolean mIgnoreFocus;
    private Button mCenterButton;


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

        /*
         * Create toast beforehand so multiple clicks don't throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.airport, null);
        setContentView(view);
        mAirport = (ListView)view.findViewById(R.id.airport_list);
        mAfdView = (AfdView)view.findViewById(R.id.airport_afd);
        mSpinner = (Spinner)view.findViewById(R.id.airport_spinner);

        mCenterButton = (Button)view.findViewById(R.id.airport_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mAfdView.center();
            }
            
        });

        mIgnoreFocus = true;

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                if(mDestination != null && mService != null && mList != null) {
                    /*
                     * mIgnoreFocus will get rid of spinner position 0 call on onResume()
                     */
                    if(mIgnoreFocus) {
                        pos = mService.getAfdIndex();
                        mIgnoreFocus = false;
                    }
                    String[] afd = mDestination.getAfd();
                    if(afd != null) {
                        if(pos > afd.length) {
                            pos = 0;
                        }
                        mSpinner.setSelection(pos);
                        if(pos > 0) {
                            mService.loadDiagram(afd[pos - 1] + Preferences.IMAGE_EXTENSION);
                            mAfdView.setBitmap(mService.getDiagram());
                            /*
                             * Show graphics
                             */
                            mAirport.setVisibility(View.INVISIBLE);
                            mAfdView.setVisibility(View.VISIBLE);
                            mCenterButton.setVisibility(View.VISIBLE);
                        }
                        else {
                            mAirport.setVisibility(View.VISIBLE);                            
                            mAfdView.setVisibility(View.INVISIBLE);
                            mCenterButton.setVisibility(View.INVISIBLE);
                            mService.loadDiagram(null);
                            mAfdView.setBitmap(null);
                        }
                        mService.setAfdIndex(pos);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

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

            mList = new ArrayList<String>();
            mList.add(getApplicationContext().getString(R.string.AFD));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(AirportActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);

            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();
            if(null == mDestination) {
                mAfdView.setBitmap(null);
                mToast.setText(getString(R.string.ValidDest));
                mAirport.setVisibility(View.VISIBLE);
                mAfdView.setVisibility(View.INVISIBLE);
                mCenterButton.setVisibility(View.INVISIBLE);
                mToast.show();
                return;
            }

            /*
             * Get Text A/FD
             */
            LinkedHashMap <String, String>map = mDestination.getParams();
            LinkedList<Awos> awos = mDestination.getAwos();
            LinkedHashMap <String, String>freq = mDestination.getFrequencies();
            LinkedList<Runway> runways = mDestination.getRunways();
            String[] views = new String[map.size() + freq.size() + awos.size() + runways.size()];
            String[] values = new String[map.size() + freq.size() + awos.size() + runways.size()];
            int iterator = 0;
            /*
             * Add header. Check below if this is not added twice
             */
            String s = map.get(DataBaseHelper.LOCATION_ID);
            if(s != null) {
                views[iterator] = DataBaseHelper.LOCATION_ID;
                values[iterator] = s;
                iterator++;
            }
            s = map.get(DataBaseHelper.FACILITY_NAME);
            if(s != null) {
                views[iterator] = DataBaseHelper.FACILITY_NAME;
                values[iterator] = s;
                iterator++;
            }
            s = map.get(DataBaseHelper.FUEL_TYPES);
            if(s != null) {
                views[iterator] = DataBaseHelper.FUEL_TYPES;
                values[iterator] = s;
                iterator++;
            }
            
			/*
			 * Add AWOS
			 */
			for (Awos awos1 : awos) {
				// We should hide/display UHF frequencies as a preference.
				// Military pilots may want to use Avare too!
				String separator = new String("");
				String f1p1 = new String("");
				String f2p2 = new String("");

				views[iterator] = awos1.getType();
				// Create the string for the first frequency/phone pair
				String f1 = awos1.getFreq1();
				String p1 = awos1.getPhone1();
				separator = (f1.equals("") || p1.equals("")) ? "" : " / ";
				if (!f1.equals("") || !p1.equals("")) {
					f1p1 = f1 + separator + p1;
				}
				// Create the string for the second frequency/phone pair
				String f2 = awos1.getFreq2();
				String p2 = awos1.getPhone2();
				separator = (f2.equals("") || p2.equals("")) ? "" : " / ";
				if (!f2.equals("") || !p2.equals("")) {
					f2p2 = "\n" + f2 + separator + p2;
				}
				// Create the string for the remarks
				String rem = awos1.getRemarks();
				if (!rem.equals("") && (!f1p1.equals("") || !f2p2.equals(""))) {
					rem = "\n\n" + rem;
				}

				// Add them all to our array
				values[iterator] = f1p1 + f2p2 + rem;
				iterator++;
			}
            /*
             * Add frequencies (unicom, atis, tower etc)  
             */
            for(String key : freq.keySet()){
                views[iterator] = key;
                values[iterator] = freq.get(key);
                iterator++;
            }
            /*
             * Add runways
             */
            for(Runway run : runways){
				String mRunwayName = "Runway-";
				if (run.getNumber().startsWith("H")) {
					mRunwayName = "Helipad-";
				} else {
					if (run.getNumber().endsWith("W")) {
						mRunwayName = "Waterway-";
					}
				}
            	mRunwayName = mRunwayName+run.getNumber();
            	views[iterator] = mRunwayName + " (" + run.getLength() + "'x" + run.getWidth() + "')";
                values[iterator] = 
                        "DT: " + run.getThreshold() + ",\n" +
                        "Elev: " + run.getElevation() + ",\n" +
                        "Surf: " + run.getSurface() + ",\n" +
                        "Ptrn: " + run.getPattern() + ",\n" +
                        "ALS: " + run.getLights() + ",\n" +
                        "ILS: " + run.getILS() + ",\n" +
                        "VGSI: " + run.getVGSI()
                        ;
                iterator++;
            }

            /*
             * Add the rest
             */
            for(String key : map.keySet()){
                if(key.equals(DataBaseHelper.LOCATION_ID) || key.equals(DataBaseHelper.FACILITY_NAME) ||
                        key.equals(DataBaseHelper.FUEL_TYPES)) {
                    continue;
                }
                views[iterator] = key;
                values[iterator] = map.get(key);
                iterator++;
            }

            mAirport.setClickable(false);
            mAirport.setDividerHeight(10);
            TypeValueAdapter mAdapter = new TypeValueAdapter(AirportActivity.this, views, values);
            
            mAirport.setAdapter(mAdapter);

            mAirport.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
                    return true;
                }
            });

            /*
             * Start adding graphical A/FD
             */
            
            /*
             * Not found
             */
            if((!mDestination.isFound()) || (mDestination.getAfd() == null)) {
                mAfdView.setBitmap(null);
                mAirport.setVisibility(View.VISIBLE);
                mAfdView.setVisibility(View.INVISIBLE);
                mCenterButton.setVisibility(View.INVISIBLE);
                return;                
            }
            
            /*
             * Now add all A/FD pages to the list
             */
            String[] afd = mDestination.getAfd();            
            for(int plate = 0; plate < afd.length; plate++) {
                String tokens[] = afd[plate].split("/");
                mList.add(tokens[tokens.length - 1]);
            }
            
            /*
             * A list of A/FD available for this airport
             */
            adapter = new ArrayAdapter<String>(AirportActivity.this,
                    android.R.layout.simple_spinner_item, mList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);            
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
        
        /*
         * Registering our receiver
         * Bind now.
         */
        mIgnoreFocus = true;
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Helper.setOrientationAndOn(this);
    }

    /**
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
