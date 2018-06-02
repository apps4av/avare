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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ds.avare.animation.AnimateButton;
import com.ds.avare.animation.TwoButton;
import com.ds.avare.animation.TwoButton.TwoClickListener;
import com.ds.avare.flight.FlightStatusInterface;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.FuelTimer;
import com.ds.avare.instruments.UpTimer;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Boundaries;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.place.Plan;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Emergency;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines.InfoLineFieldLoc;
import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.OptionButton;
import com.ds.avare.utils.Tips;
import com.ds.avare.views.LocationView;
import com.ds.avare.webinfc.WebAppMapInterface;

import java.io.File;
import java.net.URI;
import java.util.Observable;
import java.util.Observer;

/**
 * @author zkhan, jlmcgraw
 * Main activity
 */
public class LocationActivity extends Activity implements Observer {

    /**
     * This view display location on the map.
     */
    private LocationView mLocationView;
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

    private Location mInitLocation;
    
    /**
     * Shows warning message about Avare
     */
    private AlertDialog mAlertDialogDatabase;

    /**
     * Shows exit dialog
     */
    private AlertDialog mAlertDialogExit;

    /**
     * Shows warning about GPS
     */
    private AlertDialog mGpsWarnDialog;
    /**
     * To go to emergency mode
     */
    private AlertDialog mSosDialog;

    /**
     * Version related warnings
     */
    private AlertDialog mWarnDialog;

    private ImageButton mCenterButton;
    private Button mDrawClearButton;
    private TwoButton mTracksButton;
    private Button mHelpButton;
    private Button mPrefButton;
    private Button mDownloadButton;
    private Button mMenuButton;
    private TwoButton mSimButton;
    private TwoButton mDrawButton;
    private Button mWebButton;
    private OptionButton mChartOption;
    private OptionButton mLayerOption;
    private Bundle mExtras;
    private boolean mIsWaypoint;
    private AnimateButton mAnimateTracks;
    private AnimateButton mAnimateSim;
    private AnimateButton mAnimateWeb;
    private AnimateButton mAnimateLayer;
    private AnimateButton mAnimateChart;
    private AnimateButton mAnimateHelp;
    private AnimateButton mAnimateDownload;
    private AnimateButton mAnimatePref;
    private String mAirportPressed;
    private AlertDialog mAlertDialogDestination;
    private WebAppMapInterface mInfc;

    private Button mPlanPrev;
    private ImageButton mPlanPause;
    private Button mPlanNext;
    private boolean mMenuOut;


    private TankObserver mTankObserver;
    private TimerObserver mTimerObserver;

    private FlightStatusInterface mFSInfc = new FlightStatusInterface() {
        @Override
        public void rollout() {
            if(mPref != null && mService != null) {
                if(mPref.isAutoDisplayAirportDiagram()) {
                    int nearestNum = mService.getArea().getAirportsNumber();
                    if(nearestNum > 0) {
                        /*
                         * Find the nearest airport and load its plate on rollout
                         */
                        Airport nearest = mService.getArea().getAirport(0);
                        if(nearest != null && PlatesActivity.doesAirportHaveAirportDiagram(mPref.mapsFolder(),
                                nearest.getId()) && nearest.getDistance() < Preferences.DISTANCE_TO_AUTO_LOAD_PLATE) {
                            mService.setLastPlateAirport(nearest.getId());
                            mService.setLastPlateIndex(0);
                            ((MainActivity) LocationActivity.this.getParent()).showPlatesTab();
                        }
                    }
                }
            }
        }
    };

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

                if(mService != null && mService.getPlan().isEarlyPass() && mPref.isBlinkScreen()) {
                	/*
                	 * Check that if we are close to passing a plan passage, blink
                	 */
                	blink();
                }
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
            else if(!(new File(mPref.mapsFolder() + "/" + getResources().getStringArray(R.array.resFilesDatabase)[0]).exists())) {
                mLocationView.updateErrorStatus(getString(R.string.DownloadDBShort));
            }
            else if(!(new File(mPref.mapsFolder() + "/tiles")).exists()) {
                mLocationView.updateErrorStatus(getString(R.string.MissingMaps));
            }
            else if(mPref.isSimulationMode()) {
                mLocationView.updateErrorStatus(getString(R.string.SimulationMode));
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

    /**
     *
     * @param dest
     * @return
     */
    private boolean isSameDest(String dest) {
        if(mService != null) {
            Destination cdest = mService.getDestination();
            if(cdest != null) {
                if(dest.contains("&")) {
                    /*
                     * GPS dest needs comparison with closeness.
                     */
                    String tokens[] = dest.split("&");
                    double lon;
                    double lat;
                    try {
                        lon = Double.parseDouble(tokens[1]);
                        lat = Double.parseDouble(tokens[0]);
                    }
                    catch(Exception e) {
                        return false;
                    }
                    if(Helper.isSameGPSLocation(cdest.getLocation().getLongitude(),
                            cdest.getLocation().getLatitude(), lon, lat)) {
                        return true;
                    }
                }
                else if(dest.equals(cdest.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param dst
     */
    private void goTo(String dst, String type) {
        mIsWaypoint = false;
        mDestination = DestinationFactory.build(mService, dst, type);
        mDestination.addObserver(LocationActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
    }

    /**
     *
     * @param dst
     */
    private void planTo(String dst, String type) {
        mIsWaypoint = true;
        mDestination = DestinationFactory.build(mService, dst, type);
        mDestination.addObserver(LocationActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {

        if(mMenuOut) {
            hideMenu(); // hide menu on back first
            return;
        }

        /*
         * And may exit
         */
        mAlertDialogExit = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
        mAlertDialogExit.setTitle(getString(R.string.Exit));
        mAlertDialogExit.setCanceledOnTouchOutside(true);
        mAlertDialogExit.setCancelable(true);
        mAlertDialogExit.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * Go to background
                 */
                setTrackState(false);   // ensure tracks are turned off
                LocationActivity.super.onBackPressed();
                dialog.dismiss();
            }
        });
        mAlertDialogExit.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * Go to background
                 */
                dialog.dismiss();
            }
        });

        if(!isFinishing()) {
            mAlertDialogExit.show();
        }

    }

    /**
     *
     */
    private void hideMenu() {
        mAnimateTracks.animateBack();
        mAnimateWeb.animateBack();
        mAnimateSim.animateBack();
        mAnimateLayer.animateBack();
        mAnimateChart.animateBack();
        mAnimateHelp.animateBack();
        mAnimateDownload.animateBack();
        mAnimatePref.animateBack();
        mMenuOut = false;
    }

    /**
     *
     */
    private void showMenu() {
        mAnimateTracks.animate();
        mAnimateWeb.animate();
        mAnimateSim.animate();
        mAnimateLayer.animate();
        mAnimateChart.animate();
        mAnimateHelp.animate();
        mAnimateDownload.animate();
        mAnimatePref.animate();
        mMenuOut = true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Helper.setTheme(this);
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

        // Need GPS permission right away
        if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
        }

        mMenuOut = false;

        /*
         * To be notified of some action in the view
         */
        mLocationView.setGestureCallback(new GestureInterface() {
            InfoLineFieldLoc _InfoLineFieldLoc;
            int _nNewSelection = 0;

            // This is the doubletap gesture that is called when the user desires
            // to change the "instrument" text display. We are passed the row and field index
            // of what is requested to change.
            @Override
            public void gestureCallBack(int nEvent, InfoLineFieldLoc infoLineFieldLoc) {
                if (infoLineFieldLoc == null) {
                    return;
                }

                _InfoLineFieldLoc = infoLineFieldLoc;

                if (GestureInterface.LONG_PRESS == nEvent) {
                    if (mService != null) {
                        mService.getInfoLines().longPress(_InfoLineFieldLoc);
                        return;
                    }
                }

                if (GestureInterface.TOUCH == nEvent) {
                    if (mService != null) {
                        mService.getInfoLines().touch(_InfoLineFieldLoc);
                        return;
                    }
                }

                if (GestureInterface.DOUBLE_TAP == nEvent) {

                    // Create the alert dialog and add the title.
                    DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(LocationActivity.this);
                    builder.setTitle(R.string.SelectTextFieldTitle);

                    // The list of items to chose from. When a selection is made, save it off locally
                    builder.setSingleChoiceItems(_InfoLineFieldLoc.getOptions(),
                            _InfoLineFieldLoc.getSelected(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    _nNewSelection = which;
                                }
                            });

                    // OK button, copy the new selection to the true array so it will be displayed
                    builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            if (mService != null) {
                                mService.getInfoLines().setFieldType(_InfoLineFieldLoc, _nNewSelection);
                            }
                        }
                    });

                    // Cancel, nothing to do here, let the dialog self-destruct
                    builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

                    // Create and show the dialog now
                    AlertDialog dialog = builder.create();
                    if(!isFinishing()) {
                        dialog.show();
                    }
                }
            }

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GestureInterface#gestureCallBack(int, java.lang.String)
             */
            @Override
            public void gestureCallBack(int event, LongTouchDestination data) {

                if (GestureInterface.TOUCH == event) {
                    hideMenu();
                }

                if (GestureInterface.LONG_PRESS == event) {

                    if(mInfc != null) {
                        mInfc.setData(data);
                    }
                    if(!isFinishing()) {
                        mAlertDialogDestination.show();
                    }

                    /*
                     * Show the popout
                     * Now populate the pop out weather etc.
                     */
                    mAirportPressed = data.airport;

                }
            }

        });

        mChartOption = (OptionButton)view.findViewById(R.id.location_spinner_chart);
        mChartOption.setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                mPref.setChartType("" + (int) o1);
                mLocationView.forceReload();
                return null;
            }
        });
        mChartOption.setOptions(Boundaries.getChartTypes());
        mChartOption.setCurrentSelectionIndex(Integer.parseInt(mPref.getChartType()));
        mLocationView.forceReload();

        final MainActivity mainActivity = (MainActivity) getParent();
        if (mainActivity != null) {
            View tabView = mainActivity.getTabWidget().getChildTabViewAt(MainActivity.tabMain);
            tabView.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (mainActivity.getTabHost().getCurrentTab() == MainActivity.tabMain) {
                                mChartOption.performClick();
                            }
                            return false;
                        }
                    }
            );
        }

        mLayerOption = (OptionButton)view.findViewById(R.id.location_spinner_layer);
        mLayerOption.setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                mPref.setLayerType(mLayerOption.getCurrentValue());
                mLocationView.setLayerType(mPref.getLayerType());
                return null;
            };
        });

        mCenterButton = (ImageButton)view.findViewById(R.id.location_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mLocationView.center();
            }

        });
        mCenterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // long press on center button sets track toggle
                mPref.setTrackUp(!mPref.isTrackUp());
                if(mPref.isTrackUp()) {
                    mCenterButton.getBackground().setColorFilter(0xFF71BC78, PorterDuff.Mode.MULTIPLY);
                    mToast.setText(getString(R.string.TrackUp));
                }
                else {
                    mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
                    mToast.setText(getString(R.string.NorthUp));
                }
                mToast.show();
                mLocationView.invalidate();
                return true;
            }
        });


        mMenuButton = (Button)view.findViewById(R.id.location_button_menu);
        mMenuButton.getBackground().setAlpha(255);
        mMenuButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showMenu();
            }

        });

        mHelpButton = (Button)view.findViewById(R.id.location_button_help);
        mHelpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationActivity.this, WebActivity.class);
                intent.putExtra("url", NetworkHelper.getHelpUrl(LocationActivity.this));
                startActivity(intent);
            }

        });


        mDrawClearButton = (Button)view.findViewById(R.id.location_button_draw_clear);
        mDrawClearButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mService != null) {
                    if(mLocationView.getDraw()) {
                        mService.getDraw().clear();
                    }
                }
            }
        });

        mDownloadButton = (Button)view.findViewById(R.id.location_button_dl);
        mDownloadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(LocationActivity.this, ChartsDownloadActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
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

        mWebButton = (Button)view.findViewById(R.id.location_button_sos);
        mWebButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mSosDialog = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
                mSosDialog.setTitle(getString(R.string.DeclareEmergency));
                mSosDialog.setMessage(getString(R.string.DeclareEmergencyDetails));
                mSosDialog.setCancelable(false);
                mSosDialog.setCanceledOnTouchOutside(false);
                mSosDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String ret = Emergency.declare(getApplicationContext(), mPref, mService);
                        hideMenu();
                        Toast.makeText(LocationActivity.this, ret, Toast.LENGTH_LONG).show();

                    }
                });
                mSosDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        hideMenu();
                    }
                });
                if(!isFinishing()) {
                    mSosDialog.show();
                }
                if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.SEND_SMS)) {
                    ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.SEND_SMS}, 103); // tabhost needs parent for permissions
                }
            }

        });


        mSimButton = (TwoButton)view.findViewById(R.id.location_button_sim);
        if(mPref.isSimulationMode()) {
            mSimButton.setText(getString(R.string.SimulationMode));
            mSimButton.setChecked(true);
        }
        else {
            mSimButton.setText(getString(R.string.Navigate));
            mSimButton.setChecked(false);
        }
        mSimButton.setTwoClickListener(new TwoClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * Bring up preferences
                 */
                if(mSimButton.getText().equals(getString(R.string.SimulationMode))) {
                    mPref.setSimMode(true);
                    if(null != mService) {
                        Destination dest = mService.getDestination();
                        if(null != dest) {
                            Location l = dest.getLocation();
                            mLocationView.updateParams(new GpsParams(l));
                        }
                        mLocationView.forceReload();
                    }
                }
                else {
                    mPref.setSimMode(false);
                }

            }

        });

        /*
         * Draw
         */
        mDrawButton = (TwoButton)view.findViewById(R.id.location_button_draw);
        mDrawButton.setTwoClickListener(new TwoClickListener() {

            @Override
            public void onClick(View v) {
                /*
                 * Bring up preferences
                 */
                if(mDrawButton.getText().equals(getString(R.string.Draw))) {
                    mLocationView.setDraw(true);
                    mDrawClearButton.setVisibility(View.VISIBLE);
                }
                else {
                    mLocationView.setDraw(false);
                    mDrawClearButton.setVisibility(View.INVISIBLE);
                }
            }

        });
        if(mPref.removeB1Map()) {
            mDrawButton.setVisibility(View.INVISIBLE);
        }

        /*
         * The tracking button handler. Enable/Disable the saving of track points
         * to a KML file
         */
        mTracksButton = (TwoButton)view.findViewById(R.id.location_button_tracks);
        mTracksButton.setTwoClickListener(new TwoClickListener() {

            @Override
            public void onClick(View v) {
	            if(null != mService) {
                    boolean state = mService.getTracks();
                    setTrackState(!state);
                    mPref.setTrackingState(!state);
	            }
            }
        });

        /*
         * Throw this in case GPS is disabled.
         */
        if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
            mGpsWarnDialog = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
            mGpsWarnDialog.setTitle(getString(R.string.GPSEnable));
            mGpsWarnDialog.setCancelable(false);
            mGpsWarnDialog.setCanceledOnTouchOutside(false);
            mGpsWarnDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            });
            mGpsWarnDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            if(!isFinishing()) {
                mGpsWarnDialog.show();
            }
        }

                /*
         * Throw this in case GPS is disabled.
         */
        if(mPref.showTips()) {
            mWarnDialog = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
            mWarnDialog.setTitle(getString(R.string.Tip));
            mWarnDialog.setMessage(Tips.getTip(LocationActivity.this, mPref));
            mWarnDialog.setCancelable(false);
            mWarnDialog.setCanceledOnTouchOutside(false);
            mWarnDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                /* (non-Javadoc)
                 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                 */
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            if(!isFinishing()) {
                mWarnDialog.show();
            }
        }

        /*
         * Check if this was sent from Google Maps
         */
        mExtras = getIntent().getExtras();

        // The Flight Plan Prev button collection. There are 3, Previous, Pause,
        // and next. They are only visible when a plan has been loaded and
        // activated.

        // Previous - set next destination to the previous waypoint
        mPlanPrev = (Button)view.findViewById(R.id.plan_prev);
        mPlanPrev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(null != mService) {
					Plan activePlan = mService.getPlan();
					if(true == activePlan.isActive()) {
						activePlan.regress();
					}
				}
			}

        });

        // Pause - Do no process any waypoint passage logic
        mPlanPause = (ImageButton)view.findViewById(R.id.plan_pause);
        mPlanPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(null != mService) {
					Plan activePlan = mService.getPlan();
					if(null != activePlan) {
						if(true == activePlan.suspendResume()) {
							mPlanPause.setImageResource(android.R.drawable.ic_media_pause);
						} else {
							mPlanPause.setImageResource(android.R.drawable.ic_media_play);
						}
					}
				}
			}

        });

        // Next - advance the destination to the next waypoint
        mPlanNext = (Button)view.findViewById(R.id.plan_next);
        mPlanNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(null != mService) {
					Plan activePlan = mService.getPlan();
					if(true == activePlan.isActive()) {
						activePlan.advance();
					}
				}
			}

        });

        mService = null;
        mAnimateTracks = new AnimateButton(LocationActivity.this, mTracksButton, AnimateButton.DIRECTION_R_L, mPlanPrev);
        mAnimateWeb = new AnimateButton(LocationActivity.this, mWebButton, AnimateButton.DIRECTION_L_R);
        mAnimateSim = new AnimateButton(LocationActivity.this, mSimButton, AnimateButton.DIRECTION_R_L, mPlanNext);
        mAnimateLayer = new AnimateButton(LocationActivity.this, mLayerOption, AnimateButton.DIRECTION_R_L, mPlanPause);
        mAnimateChart = new AnimateButton(LocationActivity.this, mChartOption, AnimateButton.DIRECTION_R_L, (View[])null);
        mAnimateHelp = new AnimateButton(LocationActivity.this, mHelpButton, AnimateButton.DIRECTION_L_R, mCenterButton, mDrawButton, mMenuButton);
        mAnimateDownload = new AnimateButton(LocationActivity.this, mDownloadButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimatePref = new AnimateButton(LocationActivity.this, mPrefButton, AnimateButton.DIRECTION_L_R, (View[])null);

        // Allocate the object that will get told about the status of the
        // fuel tank
        mTankObserver = new TankObserver();
        mTimerObserver = new TimerObserver();


        mInitLocation = Gps.getLastLocation(getApplicationContext());
        if (null == mInitLocation) {
            mInitLocation = mPref.getLastLocation();
        }

        DecoratedAlertDialogBuilder alert = new DecoratedAlertDialogBuilder(LocationActivity.this);

        try {
            /*
             * Make a dialog to show destination info, when long pressed on it
             */
            WebView wv = new WebView(LocationActivity.this);
            wv.loadUrl("file:///android_asset/map.html");

            mInfc = new WebAppMapInterface(LocationActivity.this, wv, new GenericCallback() {
                /*
                 * (non-Javadoc)
                 * @see com.ds.avare.utils.GenericCallback#callback(java.lang.Object)
                 */
                @Override
                public Object callback(Object o, Object o1) {

                    String param = (String) o;
                    String airport = (String) o;

                    try {
                        mAlertDialogDestination.dismiss();
                    }
                    catch (Exception e){}


                    if (null == mAirportPressed) {
                        return null;
                    }
                    if (mService == null) {
                        return null;
                    }

                    if (param.equals("A/FD")) {
                        /*
                         * A/FD
                         */
                        if (!mAirportPressed.contains("&")) {
                            mService.setLastAfdAirport(mAirportPressed);
                            ((MainActivity) LocationActivity.this.getParent()).showAfdTab();
                        }
                        mAirportPressed = null;
                    } else if (param.equals("Plate")) {
                        /*
                         * Plate
                         */
                        if (!mAirportPressed.contains("&")) {
                            mService.setLastPlateAirport(mAirportPressed);
                            mService.setLastPlateIndex(0);
                            ((MainActivity) LocationActivity.this.getParent()).showPlatesTab();
                        }
                        mAirportPressed = null;
                    } else if (param.equals("+Plan")) {
                        String type = Destination.BASE;
                        if (mAirportPressed.contains("&")) {
                            type = Destination.GPS;
                        }
                        planTo(mAirportPressed, type);
                        mAirportPressed = null;
                    } else if (param.equals("->D")) {

                        /*
                         * On click, find destination that was pressed on in view
                         * If button pressed was a destination go there, otherwise if none, then delete current dest
                         */
                        String dest = mAirportPressed;
                        mAirportPressed = null;
                        String type = Destination.BASE;
                        if (dest.contains("&")) {
                            type = Destination.GPS;
                        }
                        goTo(dest, type);
                    }
                    return null;
                }
            });
            wv.addJavascriptInterface(mInfc, "AndroidMap");

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);

                    return true;
                }
            });

            wv.getSettings().setJavaScriptEnabled(true);
            wv.getSettings().setBuiltInZoomControls(false);
            // This is need on some old phones to get focus back to webview.
            wv.setFocusable(true);
            wv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    arg0.performClick();
                    arg0.requestFocus();
                    return false;
                }
            });
            // Do not let selecting text
            wv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
            wv.setLongClickable(false);


            alert.setView(wv);
        }
        catch (RuntimeException e) {
            // This is when webkit is updating, we get an exception that webview is not defined.
        }
        mAlertDialogDestination = alert.create();
        mAlertDialogDestination.requestWindowFeature(Window.FEATURE_NO_TITLE);

    }

    private void setTrackState(boolean bState)
    {
        if(mService == null) {
            return;
        }
        URI fileURI = mService.setTracks(bState);
        /* The fileURI is returned when the tracks are closed off.
         */
        if(bState) {
            Toast.makeText(LocationActivity.this,
                    getString(R.string.TracksOn),
                    Toast.LENGTH_LONG).show();

        }
        if(fileURI != null) {
            String fileName = fileURI.getPath().substring((fileURI.getPath().lastIndexOf('/') + 1));
            switch(mPref.autoPostTracks()) {
                case 0:
		    	    			/* Just display a toast message to the user that the file was saved
		    	    			 */
                    Toast.makeText(LocationActivity.this,
                            String.format(getString(R.string.AutoPostTracksDialogText), fileName),
                            Toast.LENGTH_LONG).show();
                    break;

                case 1:
		    	    			/* Send this file out as an email attachment
		    	    			 */
                    try {
                        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("application/kml");
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                getString(R.string.AutoPostTracksSubject) + " " + fileName);
                        emailIntent.putExtra(Intent.EXTRA_STREAM,
                                Uri.fromFile(new File(fileURI.getPath())));
                        startActivity(emailIntent);
                    } catch (Exception e) {
                    }
                    break;

                case 2:
		    	    			/* Send it somewhere as KML. Let the user choose where.
		    	    			 */
                    try {
                        Intent viewIntent = new Intent(android.content.Intent.ACTION_VIEW);
                        viewIntent.setDataAndType(Uri.fromFile(new File(fileURI.getPath())),
                                "application/vnd.google-earth.kml+xml");
                        startActivity(Intent.createChooser(viewIntent,
                                getString(R.string.AutoPostTracksTitle)));
                    } catch (Exception e) {

                    }
                    break;
            }
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

            if(!mPref.isRegistered()) {
                Intent i = new Intent(LocationActivity.this, RegisterActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }

            /*
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
            mService.getFlightStatus().registerListener(mFSInfc);

            mService.getTiles().setOrientation();
            
            /*
             * Check if database needs upgrade
             */
            if(!mService.getDBResource().isPresent()) {

                mAlertDialogDatabase = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
                mAlertDialogDatabase.setTitle(getString(R.string.download));
                mAlertDialogDatabase.setCancelable(false);
                mAlertDialogDatabase.setCanceledOnTouchOutside(false);
                mAlertDialogDatabase.setMessage(getString(R.string.DownloadDB));
                mAlertDialogDatabase.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.download), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(LocationActivity.this, ChartsDownloadActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        mLocationView.zoomOut();
                        startActivity(i);
                    }
                });
                mAlertDialogDatabase.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                if(!isFinishing()) {
                    mAlertDialogDatabase.show();
                }
                return;
            }

            /*
             * Now set location if not obtained from service
             */
            mDestination = mService.getDestination();
            if(mPref.isSimulationMode()) {
                // In sim mode, set location to destination or last known location
                if(mDestination != null && mDestination.getLocation() != null) {
                    mService.setGpsParams(new GpsParams(mDestination.getLocation()));
                }
                else if (mInitLocation != null) {
                    mService.setGpsParams(new GpsParams(mInitLocation));
                }
            }
            else {
                // In navigate mode, leave location to GPS location, or last known location
                if(mService.getGpsParams() == null && mInitLocation != null) {
                    mService.setGpsParams(new GpsParams(mInitLocation));
                }
            }

            mLocationView.initParams(mService.getGpsParams(), mService);
            mLocationView.updateParams(mService.getGpsParams());

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

                    mDestination = DestinationFactory.build(mService, addr, Destination.MAPS);
                    mDestination.addObserver(LocationActivity.this);
                    mToast.setText(getString(R.string.Searching) + " " + addr);
                    mToast.show();
                    mDestination.find();
                }
                mExtras = null;
            }

            // mService is now valid, set the plan button vis
            setPlanButtonVis();

            // Tell the fuel tank timer we need to know when it runs out
            mService.getFuelTimer().addObserver(mTankObserver);
            mService.getUpTimer().addObserver(mTimerObserver);

            mLayerOption.setSelectedValue(mPref.getLayerType());
            mLocationView.setLayerType(mPref.getLayerType());


            boolean enabled = mPref.isTrackingEnabled();
            boolean on = mService.getTracks();
            mTracksButton.setChecked(enabled);
            // tracks start stop state machine
            if((!on) && enabled) {
                // not tracking and enabled
                setTrackState(true);
            }
            else if(on && (!enabled)) {
                // tracking and disabled
                setTrackState(false);
            }
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /**
     * We are interested in events from the fuel tank timer
     * @author Ron
     *
     */
    private class TankObserver implements Observer {

		@Override
		public void update(Observable observable, Object data) {
			final FuelTimer fuelTimer = (FuelTimer) observable;
			switch ((Integer)data) {
				case FuelTimer.REFRESH:
					mLocationView.postInvalidate();
					break;

				case FuelTimer.SWITCH_TANK:
					AlertDialog alertDialog = new DecoratedAlertDialogBuilder(LocationActivity.this).create();
					alertDialog.setTitle(LocationActivity.this.getString(R.string.switchTanks));
					alertDialog.setCancelable(false);
					alertDialog.setCanceledOnTouchOutside(false);
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, LocationActivity.this.getString(R.string.OK), new DialogInterface.OnClickListener() {

		                public void onClick(DialogInterface dialog, int which) {
		                    fuelTimer.reset();
		                    dialog.dismiss();
		                }
		            });
                    if(!isFinishing()) {
                        alertDialog.show();
                    }
					break;
			}
		}
    }

    /**
     * We are interested in events from the timer
     * @author Ron
     *
     */
    private class TimerObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            final UpTimer upTimer = (UpTimer) observable;
            switch ((Integer)data) {
                case UpTimer.REFRESH:
                    mLocationView.postInvalidate();
                    break;
            }
        }
    }

    /**
     * Set the flight plan buttons visibility
     */
    private void setPlanButtonVis() {
	    int planButtons = View.INVISIBLE;
		if (true == mPref.getPlanControl()) {
	        if (null != mService) {
		        Plan activePlan = mService.getPlan();
		        if (null != activePlan) {
		        	if (true == activePlan.isActive()) {
	        			planButtons = View.VISIBLE;
	        		}
	        	}
	        }
	    }

	    // Set the flight plan button visibility
	    mPlanPrev.setVisibility(planButtons);
	    mPlanPause.setVisibility(planButtons);
	    mPlanNext.setVisibility(planButtons);
	}

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

        // Set visibility of the plan buttons
        setPlanButtonVis();

        if(null != mService) {
            // Tell the fuel tank timer we need to know when it runs out
            mService.getFuelTimer().addObserver(mTankObserver);
            mService.getUpTimer().addObserver(mTimerObserver);
        }

        // Button colors to be synced across activities
        if(mPref.isTrackUp()) {
            mCenterButton.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
        }
        else {
            mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
        }

    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
            mService.getFlightStatus().unregisterListener(mFSInfc);
            mService.getFuelTimer().removeObserver(mTankObserver);
            mService.getUpTimer().removeObserver(mTimerObserver);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);
        
        /*
         * Kill dialogs
         */
        if(null != mAlertDialogDatabase) {
            try {
                mAlertDialogDatabase.dismiss();
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

        if(null != mSosDialog) {
            try {
                mSosDialog.dismiss();
            }
            catch (Exception e) {
            }
        }

        if(null != mWarnDialog) {
            try {
                mWarnDialog.dismiss();
            }
            catch (Exception e) {
            }
        }

        if(null != mAlertDialogExit) {
            try {
                mAlertDialogExit.dismiss();
            }
            catch (Exception e) {
            }
        }

        if(null != mAlertDialogDestination) {
            try {
                mAlertDialogDestination.dismiss();
            }
            catch (Exception e) {
            }
        }

        /*
         * Do this as switching from screen needs to hide its menu
         */
        hideMenu();
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
                mPref.addToRecent(mDestination.getStorageName());
                if(!mIsWaypoint) {
                    mLocationView.updateDestination();
                    if(mService != null) {
                        mService.setDestination((Destination)arg0);
                    }
                    mToast.setText(getString(R.string.DestinationSet) + ((Destination)arg0).getID());
                    mToast.show();
                    ((MainActivity)this.getParent()).showMapTab();
                }
                else {
                    if(mService != null) {
                        if(mService.getPlan().insertDestination((Destination)arg0)) {
                            mToast.setText(((Destination)arg0).getID() + getString(R.string.PlanSet));
                        }
                        else {
                            mToast.setText(((Destination)arg0).getID() + getString(R.string.PlanNoset));
                        }
                        mToast.show();                            
                    }
                }
                
                /*
                 * Move to new dest temporarily if sim mode.
                 */
                if(mPref.isSimulationMode()) {
                    Location l = mDestination.getLocation();
                    mLocationView.updateParams(new GpsParams(l));
                }
                mLocationView.forceReload();

            }
            else {
                mToast.setText(getString(R.string.DestinationNF));
                mToast.show();
            }
        }
    }
    
    /**
     * Blink screen for an alert
     */
    private void blink() {
        Runnable r = new Runnable() {
            public void run() {
            	/*
            	 * By making the view invisible, background shows
            	 */
                if(mLocationView.getVisibility() == View.VISIBLE) {
                	mLocationView.setVisibility(View.INVISIBLE);
                }
                else {
                	mLocationView.setVisibility(View.VISIBLE);                        	
                }
            }
        };
        
        /*
         * Schedule 10 times
         */
        Handler h = new Handler();
        for(int ms = 500; ms <= 5000; ms+=500) {
        	h.postDelayed(r, ms);
        }
    }

}
