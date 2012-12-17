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


import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class NearestActivity extends Activity  implements Observer {
    
    private StorageService mService;
    private ListView mNearest;
    private NearestAdapter mNearestAdapter;
    private Toast mToast;
    private Preferences mPref;
    private Destination mDestination;
    
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
                 * Update distances/bearing to all airports in the area
                 */
                mService.getArea().updateLocation(params);
                prepareAdapter();
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }          
    };


    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.nearestact, null);
        setContentView(view);
        mNearest = (ListView)view.findViewById(R.id.nearestlist);

        mPref = new Preferences(getApplicationContext());
        mService = null;
    }

    /**
     * 
     */
    private boolean prepareAdapter() {
        int airportnum = mService.getArea().getAirportsNumber();
        if(0 == airportnum) {
            return false;
        }
        
        final String [] airport = new String[airportnum];
        final String [] airportname = new String[airportnum];
        final String [] dist = new String[airportnum];
        final String [] bearing = new String[airportnum];
        final String [] fuel = new String[airportnum];
        final Integer[] color = new Integer[airportnum];

        for(int id = 0; id < airportnum; id++) {
            airport[id] = mService.getArea().getAirport(id).getId();
            airportname[id] = mService.getArea().getAirport(id).getName() + "(" + 
                    mService.getArea().getAirport(id).getId() + ")";
            fuel[id] = mService.getArea().getAirport(id).getFuel();
            dist[id] = "" + (float)(Math.round(mService.getArea().getAirport(id).getDistance() * 10) / 10) + " nm";
            bearing[id] = "" + Math.round(mService.getArea().getAirport(id).getBearing()) + '\u00B0';
            color[id] = WeatherHelper.metarSquare(mService.getArea().getAirport(id).getWeather());
        }
        if(null == mNearestAdapter) {
            mNearestAdapter = new NearestAdapter(NearestActivity.this, dist, airportname, bearing, fuel, color);
        }
        else {
            mNearestAdapter.updateList(dist, airportname, bearing, fuel, color);
        }
        return true;
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

            if(false == prepareAdapter()) {
                mToast.setText(getString(R.string.AreaNF));
                mToast.show();
                return;
            }
            mNearest.setAdapter(mNearestAdapter);
            
            mNearest.setClickable(true);
            mNearest.setDividerHeight(10);
            mNearest.setCacheColorHint(Color.WHITE);
            mNearest.setBackgroundColor(Color.WHITE);
            mNearest.setOnItemClickListener(new OnItemClickListener() {

                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                        int position, long id) {
                    /*
                     * Set destination to this airport if clicked on it
                     */
                    mDestination = new Destination(mService.getArea().getAirport(position).getId(),
                            mPref, mService.getDBResource());
                    mDestination.addObserver(NearestActivity.this);
                    mDestination.find();
                }
            });
            
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
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    /**
     * 
     */
    @Override
    public void update(Observable arg0, Object arg1) {
        if(arg0 instanceof Destination) {
            Boolean result = (Boolean)arg1;
            if(result) {
                if(null != mService) {
                    mService.setDestination((Destination)arg0);
                }
                mPref.addToRecent(((Destination)arg0).getID());
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
