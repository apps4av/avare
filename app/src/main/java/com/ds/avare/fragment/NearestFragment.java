/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.adapters.NearestAdapter;
import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import java.util.Observable;
import java.util.Observer;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class NearestFragment extends Fragment implements Observer {
    public static final String TAG = "NearestFragment";

    private StorageService mService;
    private ListView mNearest;
    private NearestAdapter mNearestAdapter;
    private Preferences mPref;
    private Destination mDestination;
    private AnimateButton mAnimateDest;
    private AnimateButton mAnimatePlates;
    private Button mPlatesButton;
    private Button mButton2000;
    private Button mButton4000;
    private Button mButton6000;
    private Button mButtonFuel;
    private CoordinatorLayout mCoordinatorLayout;


    private Button mDestButton;
    private String mSelectedAirportId;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(getActivity());
        super.onCreate(savedInstanceState);

        mPref = new Preferences(getContext());
        mService = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nearest, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        /*
         * Dest button
         */
        mDestButton = (Button) view.findViewById(R.id.nearest_button_dest);
        mDestButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * On click, find destination that was pressed on in view
                 */
                Button b = (Button)v;
                mDestination = new Destination(b.getText().toString(), Destination.BASE, mPref, mService);
                mDestination.addObserver(NearestFragment.this);
                Snackbar.make(
                        mCoordinatorLayout,
                        getString(R.string.Searching) + " " + b.getText().toString(),
                        Snackbar.LENGTH_SHORT
                ).show();
                mDestination.find();
            }

        });


        /*
         * Plates button
         */
        mPlatesButton = (Button) view.findViewById(R.id.nearest_button_plates);
        mPlatesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * On click, find destination that was pressed on in view
                 */
                if(mService != null) {
                    mService.setLastPlateAirport(mSelectedAirportId);
                    mService.setLastPlateIndex(0);
                }
                ((MainActivity) getContext()).showPlatesView();
            }
        });


        /*
         * Dest button
         */
        mButtonFuel = (Button) view.findViewById(R.id.nearest_button_fuel);
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

                mDestination = new Destination(a.getId(), Destination.BASE, mPref, mService);
                mDestination.addObserver(NearestFragment.this);
                Snackbar.make(mCoordinatorLayout, getString(R.string.Searching), Snackbar.LENGTH_SHORT).show();
                mDestination.find();
            }

        });

        /*
         * Dest button
         */
        mButton2000 = (Button) view.findViewById(R.id.nearest_button_2000);
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

                mDestination = new Destination(a.getId(), Destination.BASE, mPref, mService);
                mDestination.addObserver(NearestFragment.this);
                Snackbar.make(mCoordinatorLayout, getString(R.string.Searching), Snackbar.LENGTH_SHORT).show();
                mDestination.find();
            }

        });
        /*
         * Dest button
         */
        mButton4000 = (Button) view.findViewById(R.id.nearest_button_4000);
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

                mDestination = new Destination(a.getId(), Destination.BASE, mPref, mService);
                mDestination.addObserver(NearestFragment.this);
                Snackbar.make(mCoordinatorLayout, getString(R.string.Searching), Snackbar.LENGTH_SHORT).show();
                mDestination.find();
            }

        });
        /*
         * Dest button
         */
        mButton6000 = (Button) view.findViewById(R.id.nearest_button_6000);
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

                mDestination = new Destination(a.getId(), Destination.BASE, mPref, mService);
                mDestination.addObserver(NearestFragment.this);
                Snackbar.make(mCoordinatorLayout, getString(R.string.Searching), Snackbar.LENGTH_SHORT).show();
                mDestination.find();
            }
        });

        mNearest = (ListView) view.findViewById(R.id.nearest_list);

        mAnimatePlates = new AnimateButton(getContext(), mPlatesButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateDest = new AnimateButton(getContext(), mDestButton, AnimateButton.DIRECTION_L_R, (View[])null);

        mCoordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);
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

        final String [] airport = new String[airportnum];
        final String [] airportname = new String[airportnum];
        final String [] dist = new String[airportnum];
        final String [] bearing = new String[airportnum];
        final String [] fuel = new String[airportnum];
        final String [] runlen = new String[airportnum];
        final String[] elevation = new String[airportnum];
        final boolean[] glide = new boolean[airportnum];

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
            glide[id] = a.canGlide(mPref);
        }
        if(null == mNearestAdapter) {
            mNearestAdapter = new NearestAdapter(getContext(), dist, airportname, bearing, fuel, elevation, runlen, glide);
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
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);

            if(mPref.isSimulationMode() || (!prepareAdapter(null))) {
                Snackbar.make(mCoordinatorLayout, getString(R.string.AreaNF), Snackbar.LENGTH_SHORT).show();
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
                    if(mDestination != null) {
                        if(mDestination.isLooking()) {
                            /*
                             * Already looking for dest.
                             */
                            return;
                        }
                    }

                    Airport a = mService.getArea().getAirport(position);
                    mSelectedAirportId = a.getId();
                    mDestButton.setText(a.getId());

                    if(PlatesFragment.doesAirportHavePlates(mPref.mapsFolder(), a.getId())) {
                        mAnimatePlates.animate(true);
                    }
                    else {
                        mAnimatePlates.stopAndHide();
                    }

                    mAnimateDest.animate(true);
                    if(!a.canGlide(mPref)) {
                        Snackbar.make(mCoordinatorLayout, getString(R.string.NotGlideRange), Snackbar.LENGTH_SHORT).show();
                    }
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
    public void onPause() {
        super.onPause();

        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getContext().unbindService(mConnection);
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(getActivity());

        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(getContext(), StorageService.class);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
                mPref.addToRecent(((Destination)arg0).getStorageName());
                Snackbar.make(
                        mCoordinatorLayout,
                        getString(R.string.DestinationSet) + ((Destination)arg0).getID(),
                        Snackbar.LENGTH_SHORT
                ).show();
                ((MainActivity) getContext()).showMapView();
            }
            else {
                Snackbar.make(mCoordinatorLayout, getString(R.string.DestinationNF), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

}
