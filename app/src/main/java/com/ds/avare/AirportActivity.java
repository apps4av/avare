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
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.ds.avare.adapters.TypeValueAdapter;
import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.place.Plan;
import com.ds.avare.place.Runway;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.AfdView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

/**
 * @author zkhan,rasii
 * An activity that deals with A/FD information
 */
public class AirportActivity extends Activity implements Observer {   
    private StorageService mService;
    private Preferences mPref;
    private Destination mDestination;
    private ListView mAirportView;
    private Toast mToast;
    private AfdView mAfdView;
    private Button mAirportButton;
    private Button mViewButton;
    private AlertDialog mViewPopup;
    private AlertDialog mAirportPopup;    
    private ArrayList<String> mListViews;
    private ArrayList<String> mListAirports;
    private ImageButton mCenterButton;
    private String mDestString;
    private String mNearString;

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
         * Create toast beforehand so multiple clicks don't throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        mDestString = "<" + getString(R.string.Destination) + ">";
        mNearString = "<" + getString(R.string.Nearest) + ">";
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.airport, null);
        setContentView(view);
        mAirportView = (ListView)view.findViewById(R.id.airport_list);
        mAfdView = (AfdView)view.findViewById(R.id.airport_afd);
        
        mViewButton = (Button)view.findViewById(R.id.airport_button_views);
        mViewButton.getBackground().setAlpha(255);
        mViewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListViews.size() == 0 || arePopupsShowing()) {
                    return;
                }
                
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setViewFromPos(which);
                    }
                };
                
                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(AirportActivity.this);
                int index = mService.getAfdIndex();
                if(index >= mListViews.size()) {
                    index = 0;
                }
                mViewPopup = builder.setSingleChoiceItems(mListViews.toArray(new String[mListViews.size()]), index, onClickListener).create();
                if(!isFinishing()) {
                    mViewPopup.show();
                }
            }
        });         
        
        mAirportButton = (Button)view.findViewById(R.id.airport_button_airports);
        mAirportButton.getBackground().setAlpha(255);
        mAirportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListAirports.size() == 0 || arePopupsShowing()) {
                    return;
                }
                
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setNewDestinationFromPos(which);
                    }
                };
                
                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(AirportActivity.this);
                int index = mListAirports.indexOf(mService.getLastAfdAirport());
                mAirportPopup = builder.setSingleChoiceItems(mListAirports.toArray(new String[mListAirports.size()]), index, onClickListener).create();
                if(!isFinishing()) {
                    mAirportPopup.show();
                }

            }
        });              

        mCenterButton = (ImageButton)view.findViewById(R.id.airport_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
        mCenterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAfdView.center();
            }
        });
        
        mService = null;
    }
    
    private boolean arePopupsShowing() {
        return (null != mViewPopup && mViewPopup.isShowing()) || 
                (null != mAirportPopup && mAirportPopup.isShowing());
    }
    
    private void setupViewInfo() {
        mListViews.clear();
        mListViews.add(getString(R.string.AFD));
        
        /*
         * Get Text A/FD
         */
        LinkedHashMap <String, String>map = mDestination.getParams();
        LinkedList<Awos> awos = mDestination.getAwos();
        LinkedHashMap <String, String>freq = mDestination.getFrequencies();
        LinkedList<Runway> runways = mDestination.getRunways();
        String[] views = new String[map.size() + freq.size() + awos.size() + runways.size()];
        String[] values = new String[map.size() + freq.size() + awos.size() + runways.size()];
        int[] categories = new int[map.size() + freq.size() + awos.size() + runways.size()];
        int iterator = 0;
        /*
         * Add header. Check below if this is not added twice
         */
        String s = map.get(LocationContentProviderHelper.LOCATION_ID);
        if(s != null) {
            views[iterator] = LocationContentProviderHelper.LOCATION_ID;
            values[iterator] = s;
            categories[iterator] = TypeValueAdapter.CATEGORY_LABEL;
            iterator++;
        }
        s = map.get(LocationContentProviderHelper.FACILITY_NAME);
        if(s != null) {
            views[iterator] = LocationContentProviderHelper.FACILITY_NAME;
            categories[iterator] = TypeValueAdapter.CATEGORY_LABEL;
            values[iterator] = s;
            iterator++;
        }
        s = map.get(LocationContentProviderHelper.FUEL_TYPES);
        if(s != null) {
            views[iterator] = LocationContentProviderHelper.FUEL_TYPES;
            categories[iterator] = TypeValueAdapter.CATEGORY_FUEL;
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
            categories[iterator] = TypeValueAdapter.CATEGORY_FREQUENCY;

            iterator++;
        }
        /*
         * Add frequencies (unicom, atis, tower etc)  
         */
        for(String key : freq.keySet()){
            views[iterator] = key;
            values[iterator] = freq.get(key);
            categories[iterator] = TypeValueAdapter.CATEGORY_FREQUENCY;
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
            categories[iterator] = TypeValueAdapter.CATEGORY_RUNWAYS;

            iterator++;
        }

        /*
         * Add the rest
         */
        for(String key : map.keySet()){
            if(key.equals(LocationContentProviderHelper.LOCATION_ID) || key.equals(LocationContentProviderHelper.FACILITY_NAME) ||
                    key.equals(LocationContentProviderHelper.FUEL_TYPES)) {
                continue;
            }
            views[iterator] = key;
            categories[iterator] = TypeValueAdapter.CATEGORY_ANY;
            values[iterator] = map.get(key);
            iterator++;
        }

        mAirportView.setClickable(false);
        mAirportView.setDividerHeight(10);
        TypeValueAdapter tvAdapter = new TypeValueAdapter(AirportActivity.this, views, values, categories, mPref.isNightMode());
        mAirportView.setAdapter(tvAdapter);

        mAirportView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                    int index, long arg3) {
                return true;
            }
        });
        
        /*
         * Not found
         */
        if((!mDestination.isFound()) || (mDestination.getAfd() == null)) {
            mAfdView.setBitmap(null);
            mAirportView.setVisibility(View.VISIBLE);
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
            mListViews.add(tokens[tokens.length - 1]);
        }     
    }
    
    /*
     * Add an airport to the airports list if it doesn't already exist
     */
    private void addAirport(String name) {  
        if(mListAirports.indexOf(name) < 0) {
            mListAirports.add(name);
        }
    }
    
    private void setViewFromPos(int pos) {
        if(mDestination != null && mService != null && mListViews != null) {
            String[] afd = mDestination.getAfd();
            if(afd != null) {
                if(pos > afd.length) {
                    pos = 0;
                }
                mViewButton.setText(mListViews.get(pos));
                if(pos > 0) {
                    if(mService.getAfdDiagram() == null ||
                            mService.getAfdDiagram().getName() == null ||
                            (!mService.getAfdDiagram().getName().equals(afd[pos - 1] + Preferences.IMAGE_EXTENSION))) {
                        mService.loadAfdDiagram(afd[pos - 1]);
                    }

                    mAfdView.setBitmap(mService.getAfdDiagram());
                    /*
                     * Show graphics
                     */
                    mAirportView.setVisibility(View.INVISIBLE);
                    mAfdView.setVisibility(View.VISIBLE);
                    mCenterButton.setVisibility(View.VISIBLE);
                }
                else {
                    mAirportView.setVisibility(View.VISIBLE);                            
                    mAfdView.setVisibility(View.INVISIBLE);
                    mCenterButton.setVisibility(View.INVISIBLE);
                    mService.loadAfdDiagram(null);
                    mAfdView.setBitmap(null);
                }
                mService.setAfdIndex(pos);
            }
            //if afd is null, avare is going to fall back to Avare A/FD data
            //we need to set the view button to A/F
            else {
                mViewButton.setText(mListViews.get(0));
            }
        }
    }

    private void setNewDestinationFromPos(int pos) {
        if(mService != null && mListAirports != null) {
            Destination oldDest = mService.getLastAfdDestination();
            mDestination = null;
            String airport = mListAirports.get(pos);
            Destination curDest = mService.getDestination();
            if(curDest != null && !curDest.getType().equals(Destination.BASE)) {
                curDest = null;
            }
            if(airport.equals(mDestString)) {
                if(null != curDest) {
                    airport = curDest.getID();
                }
                else {
                    airport = null;
                }
                mService.setLastAfdAirport(mDestString);
            }
            else if(airport.equals(mNearString)) {
                int nearestNum = mService.getArea().getAirportsNumber();
                Airport nearest = null;
                if(nearestNum > 0) {
                    nearest = mService.getArea().getAirport(0);
                    airport = nearest.getId();
                }
                else {
                    airport = null;
                }
                mService.setLastAfdAirport(mNearString);
            }
            else {
                mService.setLastAfdAirport(airport);
            }
            
            if(airport == null && mListAirports.size() > 2) {
                airport = mListAirports.get(2);
            }
            
            // If data is still null, there are no valid airports
            if(airport == null) {
                mAirportButton.setText(mService.getLastAfdAirport());
                mViewButton.setText("");
                
                mAfdView.setBitmap(null);
                mToast.setText(getString(R.string.ValidDest));
                mAirportView.setVisibility(View.VISIBLE);
                mAfdView.setVisibility(View.INVISIBLE);
                mCenterButton.setVisibility(View.INVISIBLE);
                mToast.show();
                return;
            }
            
            // If we don't have to do a Destination.find() skip it.
            int viewPos = mService.getAfdIndex();
            if(null != oldDest && oldDest.getID().equals(airport) && oldDest.getType().equals(Destination.BASE)) {
                mDestination = oldDest;
                setupViewInfo();
            }
            else if(curDest != null && curDest.getID().equals(airport) && curDest.getType().equals(Destination.BASE)) {
                mDestination = curDest;
                setupViewInfo();
                viewPos = mPref.isDefaultAFDImage() ? 1 : 0;
            }
            else {
                viewPos = 0;
                mDestination = DestinationFactory.build(mService, airport, Destination.BASE);
                mService.setLastAfdDestination(mDestination);
                mDestination.addObserver(AirportActivity.this);
                mToast.setText(getString(R.string.Searching) + " " + mDestination.getID());
                mToast.show();
                mDestination.find();
            }
            
            mService.setLastAfdDestination(mDestination);
            mAirportButton.setText(airport);

            setViewFromPos(viewPos);
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

            /*
             * Initialize the lists
             */
            mListViews = new ArrayList<String>();
            mListViews.add(AirportActivity.this.getString(R.string.AFD));

            mListAirports = new ArrayList<String>();
            mListAirports.add(mDestString);
            mListAirports.add(mNearString);
            
            /*
             * Are we being told to load an airport?
             */
            String lastDest = mService.getLastAfdAirport();
            if(null != lastDest) {
                addAirport(lastDest);
            }

            /*
             * Load the current destination
             */
            Destination curDestination = mService.getDestination();
            if(null != curDestination && curDestination.getType().equals(Destination.BASE)) {
                addAirport(curDestination.getID());
            }
            
            /*
             *  Load the nearest airport
             */
            int nearestNum = mService.getArea().getAirportsNumber();
            Airport nearest = null;
            if(nearestNum > 0) {
                nearest = mService.getArea().getAirport(0);
                addAirport(nearest.getId());
            }
            
            /*
             * Add anything in the plan
             */
            Plan plan = mService.getPlan();
            if(null != plan) {
                int nDest = plan.getDestinationNumber();
                for(int i=0; i < nDest; i++) {
                    Destination planDest = plan.getDestination(i);
                    if(null != planDest && planDest.getType().equals(Destination.BASE)) {
                        addAirport(planDest.getID());
                    }
                }
            }
            
            /*
             * Now add anything in the recently found list
             */
            String [] vals = mPref.getRecent(); 
            for(int pos=0; pos < vals.length; pos++) {
                String destType = StringPreference.parseHashedNameDestType(vals[pos]);
                if(destType != null && destType.equals(Destination.BASE)) {
                    String id = StringPreference.parseHashedNameId(vals[pos]);

                    addAirport(id);                
                }
            }

            int lastIndex = Math.max(mListAirports.indexOf(mService.getLastAfdAirport()), 0);
            setNewDestinationFromPos(lastIndex);            
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
        
        try {
            mViewPopup.dismiss();
        }
        catch(Exception e) {}
        
        try {
            mAirportPopup.dismiss();
        }
        catch(Exception e) {} 

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

        mDestination = null;
        /*
         * Registering our receiver
         * Bind now.
         */        
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Helper.setOrientationAndOn(this);
    }

    @Override
    public void update(Observable observable, Object data) {
        /*
         * Destination found?
         */
        if(observable instanceof Destination) {
            Boolean result = (Boolean)data;
            if(result) {
                if(null == mDestination) {
                    mToast.setText(getString(R.string.DestinationNF));
                    mToast.show();
                    return;
                }
                if((Destination)observable != mDestination) {
                    /*
                     * If user presses a selection repeatedly, reject previous
                     */
                    return;                    
                }

                setupViewInfo();
                setViewFromPos(mPref.isDefaultAFDImage() ? 1 : 0);
            }
            else {
                mToast.setText(getString(R.string.DestinationNF));
                mToast.show();
            }
        }
    }
}
