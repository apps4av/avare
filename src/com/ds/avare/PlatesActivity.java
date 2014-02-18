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


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
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
import android.view.MotionEvent;
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
    private Spinner mSpinnerPlates;
    private Spinner mSpinnerAirports;
    private List<String> mListPlates;
    private List<String> mListAirports;
    private String mPlateFound[];
    private double mDeclination;

    public static final String AD = "AIRPORT-DIAGRAM";

	
    /*
     * For GPS taxi
     */
    private float[] mMatrix;
    private HashMap<String, float[]> mMatrixPlates;
       
    /**
     * @return
     */
    public float[] getMatrix(String name) {
        if(name.equals(AD)) {
            return(mMatrix);            
        }
        if(null != mMatrixPlates) {
            
            /*
             * Convert from points on plate to draw matrix
             * 
             */
            float matrix[] = mMatrixPlates.get(name);            
            if(null != matrix) {
                
                /*
                 * Plates are in magnetic north orientation
                 */
                matrix[4] = (float)mDeclination;
                return matrix;
            }
        }
        return null;
    }    
       
    /*
     * Add an airport to the airports list if it doesn't already exist
     */
    private void addAirport(String mName) {
    	if(!mListAirports.contains(mName) && doesAirportHavePlates(mPref.mapsFolder(), mName)) {
    		mListAirports.add(mName);
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
            if(location != null) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
                GpsParams params = new GpsParams(location);
                
                mDeclination = params.getDeclinition();
                
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
    };

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).showMapTab();
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
        mSpinnerPlates = (Spinner)view.findViewById(R.id.plates_spinner_plates);
        mSpinnerPlates.setOnItemSelectedListener(new OnItemSelectedListener() {
              	
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            	
                if(mService != null && mPlateFound != null) {
                    if(pos >= mPlateFound.length) {
                        pos = 0;
                    }
                    mSpinnerPlates.setSelection(pos);
                    mService.loadDiagram(mPlateFound[pos] + Preferences.IMAGE_EXTENSION);
                    mPlatesView.setBitmap(mService.getDiagram());
                    String name = mListPlates.get(pos);

                    mPlatesView.setParams(null, true);
                    if(name.startsWith(Destination.AD)) {
                        mPlatesView.setParams(getMatrix(name), true);
                    }
                    else if(name.startsWith("RNAV-GPS")) {
                        mPlatesView.setParams(getMatrix(name), false);                            
                    }
                    else {
                        mPlatesView.setParams(null, true);
                    }  
                    mService.setLastPlateIndex(pos);
                }
                else {
                    mToast.setText(getString(R.string.PlatesNF));
                    mToast.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        /*
         * Spinner to select an airport
         */
        mSpinnerAirports = (Spinner)view.findViewById(R.id.plates_spinner_airports);
        mSpinnerAirports.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(null != mService) {
                    mService.setLastPlateIndex(0);
                }
                return false;
            }
        });
        mSpinnerAirports.setOnItemSelectedListener(new OnItemSelectedListener() {
        	
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

                if(mService != null && mListAirports != null) {
                    String airport = mListAirports.get(pos);
                    String mapFolder = mPref.mapsFolder();
                    mPlateFound = null;

                    /*
                     * Start with the plates in the /plates/ directory
                     */
                    String tmp0[] = null;
                    int len0 = 0;
                    if(null != airport) {
                        FilenameFilter filter = new FilenameFilter() {
                            public boolean accept(File directory, String fileName) {
                                return fileName.endsWith(Preferences.IMAGE_EXTENSION);
                            }
                        };
                        String plates[] = null;
                        
                        plates = new File(mapFolder + "/plates/" + airport).list(filter);
                        if(null != plates) {
                            java.util.Arrays.sort(plates, new PlatesComparable());
                            len0 = plates.length;
                            tmp0 = new String[len0];
                            for(int plate = 0; plate < len0; plate++) {
                                /*
                                 * Add plates/AD
                                 */
                                String tokens[] = plates[plate].split(Preferences.IMAGE_EXTENSION);
                                tmp0[plate] = mapFolder + "/plates/" + airport + "/" +
                                        tokens[0];
                            }
                        }
                    }
                    
                    /*
                     * Take off and alternate minimums
                     */
                    /*
                     * TODO: RAS make this an async request (maybe just move all 
                     * the loading to the background - especially if we get the last used 
                     * chart loaded immediately)
                     */
                    String tmp2[] = mService.getDBResource().findMinimums(airport);
                    int len2 = 0;
                    if(null != tmp2) {
                        len2 = tmp2.length;
                        for(int min = 0; min < len2; min++) {
                            /*
                             * Add minimums with path
                             */
                            String folder = tmp2[min].substring(0, 1) + "/";
                            tmp2[min] = mapFolder + "/minimums/" + folder + tmp2[min];
                        }
                    }
                    
                    /*
                     * Now combine takeoff and alternate minimums with plates
                     */
                    if(0 == len0 && 0 != len2) {
                        mPlateFound = tmp2;
                    }
                    else if(0 != len0 && 0 == len2) {
                        mPlateFound = tmp0;
                    }
                    else if(0 != len0 && 0 != len2) {
                        mPlateFound = new String[len0 + len2];
                        System.arraycopy(tmp0, 0, mPlateFound, 0, len0);
                        System.arraycopy(tmp2, 0, mPlateFound, len0, len2);
                    }    	
                    
                    mListPlates = new ArrayList<String>();
                    
                    if(null != mPlateFound) {
                    	
                        /*
                         * GPS taxi for this airport?
                         */
                        mMatrix = mService.getDBResource().findDiagramMatrix(airport);
                        mMatrixPlates = mService.getDBResource().findPlatesMatrix(airport);
                        
                        for(int plate = 0; plate < mPlateFound.length; plate++) {
                            String tokens[] = mPlateFound[plate].split("/");
                            mListPlates.add(tokens[tokens.length - 1]);
                        }   
                        
                        /*
                         * A list of plates available for this airport
                         */
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(PlatesActivity.this,
                                android.R.layout.simple_spinner_item, mListPlates);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mSpinnerPlates.setAdapter(adapter);

                        mService.setLastPlateAirport(airport);
                        mSpinnerPlates.setSelection(mService.getLastPlateIndex());
                    }
                    else {
                        /*
                         * Reset to the last one that worked
                         */
                        ArrayAdapter<String> myAdap = (ArrayAdapter<String>)mSpinnerAirports.getAdapter();  
                        mSpinnerAirports.setSelection(myAdap.getPosition(mService.getLastPlateAirport()));
                        mToast.setText(getString(R.string.PlatesNF));
                        mToast.show();
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
            
            mListAirports = new ArrayList<String>();
            int nearestNum = mService.getArea().getAirportsNumber();
            
            /*
             * Are we being told to load an airport?
             */
            if(null != mService.getLastPlateAirport()) {
                addAirport(mService.getLastPlateAirport());
            }
            
            /*
             *  Load the nearest airport
             */
            if(nearestNum > 0) {
                Airport nearest = mService.getArea().getAirport(0);
                addAirport(nearest.getId());
            }

            /*
             * See if we have a destination, if we do - add it to the airports list
             */
            mDestination = mService.getDestination();
            String dest = null;
            if(null != mDestination && mDestination.getType().equals(Destination.BASE)) {
                dest = mDestination.getID();
                addAirport(dest);
            }
            
            /*
             * Add airports in the plan
             */
            Plan plan = mService.getPlan();
            if(null != plan) {
                int nDest = plan.getDestinationNumber();
                for(int i=0; i < nDest; i++) {
                    Destination planDest = plan.getDestination(i);
                    if(planDest.getType().equals(Destination.BASE)) {
                        addAirport(planDest.getID());
                    }
                }
            }
            
            /*
             * Now add all the airports that are in the recently found list
             */
            String [] vals = mPref.getRecent(); 
            for(int pos=0; pos < vals.length; pos++) {
                String destType = StringPreference.parseHashedNameDestType(vals[pos]);
                if(destType != null && destType.equals("Base")) {
                    String id = StringPreference.parseHashedNameId(vals[pos]);

                    addAirport(id);
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(PlatesActivity.this,
                    android.R.layout.simple_spinner_item, mListAirports);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinnerAirports.setAdapter(adapter);  

            if(mListAirports.size() > 0) {
                mSpinnerAirports.setSelection(0);
            }
            else {
                mToast.setText(getString(R.string.PlatesNF));
                mToast.show();  
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
     * @author zkhan
     *
     */
    private class PlatesComparable implements Comparator<String>{
        
        @Override
        public int compare(String o1, String o2) {
            /*
             * Airport diagram must be  first
             */
            if(o1.startsWith(AD)) {
                return -1;
            }
            if(o2.startsWith(AD)) {
                return 1;
            }
            
            /*
             * Continued must follow main
             */
            if(o1.contains(",-CONT.") && o1.startsWith(o2.replace(Preferences.IMAGE_EXTENSION, ""))) {
                return 1;
            }
            if(o2.contains(",-CONT.") && o2.startsWith(o1.replace(Preferences.IMAGE_EXTENSION, ""))) {
                return -1;
            }
            
            return o1.compareTo(o2);
        }
    }    

    /**
     * 
     * @param mapFolder
     * @param id
     * @return
     */
    public static boolean doesAirportHavePlates(String mapFolder, String id) {
        return new File(mapFolder + "/plates/" + id).exists();
    }
    
    /**
     * 
     * @param mapFolder
     * @param id
     * @return
     */
    public static boolean doesAirportHaveAirportDiagram(String mapFolder, String id) {
        return new File(mapFolder + "/plates/" + id + "/" + AD + Preferences.IMAGE_EXTENSION).exists();
    }
}
