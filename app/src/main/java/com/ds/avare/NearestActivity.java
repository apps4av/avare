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
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.ds.avare.adapters.NearestAdapter;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.AirportInfo;
import com.ds.avare.utils.DestinationAlertDialog;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import java.util.Observable;
import java.util.Observer;

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
    private Button mButton2000;
    private Button mButton4000;
    private Button mButton6000;
    private Button mButtonFuel;

    private DestinationAlertDialog mDestinationAlertDialog;
    private AirportInfo mClosestTask;

    private String mSelected;

    private boolean mIsWaypoint;

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null && mService != null) {
                prepareAdapter(location);
            }
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
        MainActivity m = (MainActivity)this.getParent();
        if(m != null) {
            m.showMapTab();
        }
    }

    /**
     *
     * @param dst
     */
    private void goTo(String dst) {
        mIsWaypoint = false;
        mDestination = DestinationFactory.build(mService, dst, Destination.BASE);
        mDestination.addObserver(NearestActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
    }

    /**
     *
     * @param dst
     */
    private void planTo(String dst) {
        mIsWaypoint = true;
        mDestination = DestinationFactory.build(mService, dst, Destination.BASE);
        mDestination.addObserver(NearestActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
    }


    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mIsWaypoint = false;

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.nearest, null);
        setContentView(view);

        /*
         * Dest button
         */
        mButtonFuel = (Button)view.findViewById(R.id.nearest_button_fuel);
        mButtonFuel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                if(mNearestAdapter == null) {
                    return;
                }
                
                int id = mNearestAdapter.getClosestWith100LL();
                if(id < 0) {
                    return;
                }
                Airport a = mService.getArea().getAirport(id);

                goTo(a.getId());
            }
            
        });

        /*
         * Dest button
         */
        mButton2000 = (Button)view.findViewById(R.id.nearest_button_2000);
        mButton2000.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                if(mNearestAdapter == null) {
                    return;
                }
                
                int id = mNearestAdapter.getClosestRunwayWithMinLength(2000);
                if(id < 0) {
                    return;
                }
                Airport a = mService.getArea().getAirport(id);

                goTo(a.getId());
            }
            
        });
        /*
         * Dest button
         */
        mButton4000 = (Button)view.findViewById(R.id.nearest_button_4000);
        mButton4000.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                if(mNearestAdapter == null) {
                    return;
                }
                
                int id = mNearestAdapter.getClosestRunwayWithMinLength(4000);
                if(id < 0) {
                    return;
                }
                Airport a = mService.getArea().getAirport(id);

                goTo(a.getId());

            }
            
        });
        /*
         * Dest button
         */
        mButton6000 = (Button)view.findViewById(R.id.nearest_button_6000);
        mButton6000.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                if(mNearestAdapter == null) {
                    return;
                }
                
                int id = mNearestAdapter.getClosestRunwayWithMinLength(6000);
                if(id < 0) {
                    return;
                }
                Airport a = mService.getArea().getAirport(id);
                goTo(a.getId());

            }
            
        });

        mNearest = (ListView)view.findViewById(R.id.nearest_list);

        mPref = new Preferences(getApplicationContext());
        mService = null;


        mDestinationAlertDialog = new DestinationAlertDialog(NearestActivity.this);
        mDestinationAlertDialog.setCallback(
                new GenericCallback() {
                    @Override
                    public Object callback(Object o, Object o1) {
                        String param = (String) o;
                        try {
                            mDestinationAlertDialog.dismiss();
                        } catch (Exception e) {
                        }

                        if (null == mSelected) {
                            return null;
                        }
                        if (mService == null) {
                            return null;
                        }

                        if (param.equals("CSup")) {
                            if (null != mSelected) {
                                if (mService != null) {
                                    mService.setLastAfdAirport(mSelected);
                                    ((MainActivity) NearestActivity.this.getParent()).showAfdTab();
                                }
                            }
                        } else if (param.equals("Plate")) {
                            if (null != mSelected) {
                                if (PlatesActivity.doesAirportHavePlates(mPref.getServerDataFolder(), mSelected)) {
                                    if (mService != null) {
                                        mService.setLastPlateAirport(mSelected);
                                        mService.setLastPlateIndex(0);
                                        ((MainActivity) NearestActivity.this.getParent()).showPlatesTab();
                                    }
                                }
                            }
                        } else if (param.equals("+Plan")) {
                            if (null != mSelected) {
                                planTo(mSelected);
                            }
                        } else if (param.equals("->D")) {
                            if (mSelected == null) {
                                return null;
                            }
                            // It's ok if dbType is null
                            goTo(mSelected);
                        }
                        return null;
                    }
                });
    }

    /**
     * 
     */
    private boolean prepareAdapter(Location location) {
        int airportnum = mService.getArea().getAirportsNumber();
        GpsParams params = new GpsParams(location);
        if(0 == airportnum) {
            return false;
        }
        
        final String [] airport = new String[Preferences.MAX_AREA_AIRPORTS];
        final String [] airportname = new String[Preferences.MAX_AREA_AIRPORTS];
        final String [] dist = new String[Preferences.MAX_AREA_AIRPORTS];
        final String [] bearing = new String[Preferences.MAX_AREA_AIRPORTS];
        final String [] fuel = new String[Preferences.MAX_AREA_AIRPORTS];
        final String [] runlen = new String[Preferences.MAX_AREA_AIRPORTS];
        final String[] elevation = new String[Preferences.MAX_AREA_AIRPORTS];
        final boolean[] glide = new boolean[Preferences.MAX_AREA_AIRPORTS];

        for(int id = 0; id < airportnum; id++) {
            Airport a = mService.getArea().getAirport(id);
            airport[id] = a.getId();
            airportname[id] = a.getName() + "(" + a.getId() + ")";
            fuel[id] = a.getFuel();
            dist[id] = "" + ((float)(Math.round(a.getDistance() * 10.f)) / 10.f) + " " + Preferences.distanceConversionUnit;
            double heading = Helper.getMagneticHeading(a.getBearing(), params.getDeclinition());
            bearing[id] = Helper.correctConvertHeading(Math.round(heading)) + '\u00B0';
            elevation[id] = a.getElevation();
            runlen[id] = a.getLongestRunway();
            glide[id] = a.canGlide();
        }

        for(int id = airportnum; id < Preferences.MAX_AREA_AIRPORTS; id++) {
            // fill up the adapter
            airport[id] = "";
            airportname[id] = "";
            fuel[id] = "";
            dist[id] = "";
            bearing[id] = "";
            elevation[id] = "";
            runlen[id] = "";
            glide[id] = false;
        }


        if(null == mNearestAdapter) {
            mNearestAdapter = new NearestAdapter(NearestActivity.this, dist, airportname, bearing, fuel, elevation, runlen, glide);
        }
        else {
            mNearestAdapter.updateList(dist, airportname, bearing, fuel, elevation, runlen, glide);
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
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);

            if (mPref.isSimulationMode() || (!prepareAdapter(null))) {
                mToast.setText(getString(R.string.AreaNF));
                mToast.show();
                return;
            }
            mNearest.setAdapter(mNearestAdapter);

            mNearest.setClickable(true);
            mNearest.setDividerHeight(10);
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
                    if (mDestination != null) {
                        if (mDestination.isLooking()) {
                            /*
                             * Already looking for dest.
                             */
                            return;
                        }
                    }

                    Airport a = mService.getArea().getAirport(position);
                    if (null == a) {
                        return;
                    }
                    mSelected = a.getId();
                    if (null != mSelected) {
                        goTo(mSelected);
                        mSelected = null;
                    }

                    if (!a.canGlide()) {
                        mToast.setText(R.string.NotGlideRange);
                        mToast.show();
                    }
                }
            });

            mNearest.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                               int index, long arg3) {

                    Airport a = mService.getArea().getAirport(index);
                    if (null == a) {
                        return true;
                    }
                    mSelected = a.getId();

                    if (mSelected == null || null == mService) {
                        return true;
                    }

                    // stop previous lookup
                    if (null != mClosestTask) {
                        mClosestTask.cancel(true);
                    }


                    //airport
                    mClosestTask = new AirportInfo();

                    mClosestTask.execute(null, null, a.getId(),
                            NearestActivity.this, mService, mPref, null, false,
                            new GenericCallback() {
                                @Override
                                public Object callback(Object o, Object o1) {
                                    LongTouchDestination ltd = (LongTouchDestination) o1;
                                    ltd.setMoreButtons(false);
                                    mDestinationAlertDialog.show();
                                    mDestinationAlertDialog.setData(ltd);

                                    // If the long press event has already occurred, we need to do the gesture callback here
                                    return null;
                                }
                            });
                    return true;
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }

        @Override
        public void onBindingDied(ComponentName name) {

        }

        @Override
        public void onNullBinding(ComponentName name) {

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

        if(null != mDestinationAlertDialog) {
            try {
                mDestinationAlertDialog.dismiss();
            }
            catch (Exception e) {
            }
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
        getApplicationContext().bindService(intent, mConnection, 0);
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
        /*
         * Destination found?
         */
        if(arg0 instanceof Destination) {
            Boolean result = (Boolean)arg1;
            if(result) {

                /*
                 * Temporarily move to destination by giving false GPS signal.
                 */
                if(null == mDestination) {
                    mToast.setText(getString(R.string.DestinationNF));
                    mToast.show();
                    return;
                }
                if((Destination)arg0 != mDestination) {
                    /*
                     * If user presses a selection repeatedly, reject previous
                     */
                    return;
                }
                StringPreference s = new StringPreference(mDestination.getType(), mDestination.getDbType(), mDestination.getFacilityName(), mDestination.getID());
                mService.getDBResource().setUserRecent(s);

                if(!mIsWaypoint) {
                    if(mService != null) {
                        mService.setDestination((Destination)arg0);
                    }
                    mToast.setText(getString(R.string.DestinationSet) + ((Destination)arg0).getID());
                    mToast.show();
                    MainActivity m = (MainActivity)this.getParent();
                    if(m != null) {
                        m.showMapTab();
                    }
                }
                else {
                    if(mService != null) {
                        if(mService.getPlan().appendDestination((Destination)arg0)) {
                            mToast.setText(((Destination)arg0).getID() + getString(R.string.PlanSet));
                        }
                        else {
                            mToast.setText(((Destination)arg0).getID() + getString(R.string.PlanNoset));
                        }
                        mToast.show();
                    }
                }
            }
            else {
                mToast.setText(getString(R.string.DestinationNF));
                mToast.show();
            }
        }
    }
}
