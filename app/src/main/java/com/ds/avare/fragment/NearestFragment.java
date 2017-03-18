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

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import com.ds.avare.adapters.NearestAdapter;
import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import java.util.Observable;
import java.util.Observer;

/**
 * @author zkhan
 */
public class NearestFragment extends StorageServiceGpsListenerFragment implements Observer {

    public static final String TAG = "NearestFragment";

    private ListView mNearest;
    private NearestAdapter mNearestAdapter;
    private Destination mDestination;
    private AnimateButton mAnimateDest;
    private AnimateButton mAnimatePlates;
    private Button mPlatesButton;
    private Button mButton2000;
    private Button mButton4000;
    private Button mButton6000;
    private Button mButtonFuel;

    private Button mDestButton;
    private String mSelectedAirportId;

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
                mDestination = DestinationFactory.build(mService, b.getText().toString(), Destination.BASE);
                mDestination.addObserver(NearestFragment.this);
                showSnackbar(getString(R.string.Searching) + " " + b.getText().toString(), Snackbar.LENGTH_SHORT);
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
                ((MainActivity) getContext()).showPlatesViewAndCenter();
            }
        });

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

                mDestination = DestinationFactory.build(mService, a.getId(), Destination.BASE);
                mDestination.addObserver(NearestFragment.this);
                showSnackbar(getString(R.string.Searching), Snackbar.LENGTH_SHORT);
                mDestination.find();
            }

        });

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

                mDestination = DestinationFactory.build(mService, a.getId(), Destination.BASE);
                mDestination.addObserver(NearestFragment.this);
                showSnackbar(getString(R.string.Searching), Snackbar.LENGTH_SHORT);
                mDestination.find();
            }

        });

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

                mDestination = DestinationFactory.build(mService, a.getId(), Destination.BASE);
                mDestination.addObserver(NearestFragment.this);
                showSnackbar(getString(R.string.Searching), Snackbar.LENGTH_SHORT);
                mDestination.find();
            }

        });

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

                mDestination = DestinationFactory.build(mService, a.getId(), Destination.BASE);
                mDestination.addObserver(NearestFragment.this);
                showSnackbar(getString(R.string.Searching), Snackbar.LENGTH_SHORT);
                mDestination.find();
            }
        });

        mNearest = (ListView) view.findViewById(R.id.nearest_list);

        mAnimatePlates = new AnimateButton(getContext(), mPlatesButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateDest = new AnimateButton(getContext(), mDestButton, AnimateButton.DIRECTION_L_R, (View[])null);
    }

    @Override
    protected void postServiceConnected() {
        if (mPref.isSimulationMode() || (!prepareAdapter(null))) {
            showSnackbar(getString(R.string.AreaNF), Snackbar.LENGTH_SHORT);
            return;
        }
        mNearest.setAdapter(mNearestAdapter);

        mNearest.setClickable(true);
        mNearest.setDividerHeight(10);
        mNearest.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
                // Set destination to this airport if clicked on it
                if (mDestination != null && mDestination.isLooking()) {
                    // Already looking for dest.
                    return;
                }

                Airport a = mService.getArea().getAirport(position);
                mSelectedAirportId = a.getId();
                mDestButton.setText(a.getId());

                if(PlatesFragment.doesAirportHavePlates(mPref.mapsFolder(), a.getId())) {
                    mAnimatePlates.animate(true);
                } else {
                    mAnimatePlates.stopAndHide();
                }

                mAnimateDest.animate(true);
                if(!a.canGlide(mPref)) {
                    showSnackbar(getString(R.string.NotGlideRange), Snackbar.LENGTH_SHORT);
                }
            }
        });
    }

    @Override
    protected void onGpsLocation(Location location) {
        if (location != null && mService != null) {
            prepareAdapter(location);
        }
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        if(arg0 instanceof Destination) {
            Boolean result = (Boolean)arg1;
            if(result) {
                if(null != mService) {
                    mService.setDestination((Destination)arg0);
                }
                mPref.addToRecent(((Destination)arg0).getStorageName());
                showSnackbar(getString(R.string.DestinationSet) + ((Destination)arg0).getID(), Snackbar.LENGTH_SHORT);
                ((MainActivity) getContext()).showMapView();
            }
            else {
                showSnackbar(getString(R.string.DestinationNF), Snackbar.LENGTH_SHORT);
            }
        }
    }

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

}
