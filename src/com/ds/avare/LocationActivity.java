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
import java.net.URI;
import java.util.Observable;
import java.util.Observer;
import com.ds.avare.R;
import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;

import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

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
    
    /**
     * Shows warning message about Avare
     */
    private AlertDialog mAlertDialogWarn;

    /**
     * Shows exit dialog
     */
    private AlertDialog mAlertDialogExit;

    /**
     * Shows warning about GPS
     */
    private AlertDialog mGpsWarnDialog;
    
    private Button mDestButton;
    private Button mCenterButton;
    private Button mDrawClearButton;
    private Button mTracksButton;
    private Button mHelpButton;
    private Button mCrossButton;
    private Button mPrefButton;
    private Button mPlanButton;
    private Button mDownloadButton;
    private Button mMenuButton;
    private RelativeLayout mDestLayout;
    private ToggleButton mSimButton;
    private Button mDrawButton;
    private ToggleButton mTrackButton;
    private Spinner mChartSpinner;
    private Bundle mExtras;
    private ZoomControls mZoom;
    private VerticalSeekBar mBar;
    private boolean mIsWaypoint;
    private boolean mSpinner;
    private TextView mInfoText;
    private TextView mChartText;

    private ExpandableListView mListPopout;

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
                
                /*
                 * For terrain update threshold.
                 */
                float threshold = Helper.calculateThreshold(params.getAltitude());
                mBar.setProgress(Math.round(threshold));
                mLocationView.updateThreshold(threshold);
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
            else if(!(new File(mPref.mapsFolder() + "/tiles")).exists()) {
                mLocationView.updateErrorStatus(getString(R.string.MissingMaps));
                if(null != mLocationView.getChart()) {
                    Intent i = new Intent(LocationActivity.this, ChartsDownloadActivity.class);
                    i.putExtra(getString(R.string.download), mLocationView.getChart());
                    i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(i);
                }
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
        mDestination = new Destination(dst, type, mPref, mService);
        mDestination.addObserver(LocationActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
        mDestLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * 
     * @param dst
     */
    private void planTo(String dst, String type) {
        mIsWaypoint = true;
        mDestination = new Destination(dst, type, mPref, mService);
        mDestination.addObserver(LocationActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
        mService.getPlan().makeInactive();
        mDestLayout.setVisibility(View.INVISIBLE);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        
        /*
         * Back button hides some controls
         */
        if(mDestLayout.getVisibility() == View.VISIBLE) {
            mDestLayout.setVisibility(View.INVISIBLE);
            return;
        }

        /*
         * And may exit
         */
        mAlertDialogExit = new AlertDialog.Builder(LocationActivity.this).create();
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

        mAlertDialogExit.show();

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
        mSpinner = false;

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.location, null);
        setContentView(view);
        mLocationView = (LocationView)view.findViewById(R.id.location);
        
        

        /*
         * To be notified of some action in the view
         */
        mLocationView.setGestureCallback(new GestureInterface() {

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GestureInterface#gestureCallBack(int, java.lang.String)
             */
            @Override
            public void gestureCallBack(int event, LongTouchDestination data) {
                if(GestureInterface.LONG_PRESS == event) {
                    if(mLocationView.getDraw()) {
                        /*
                         * Show animation button for draw clear
                         */
                        AnimateButton e = new AnimateButton(getApplicationContext(), mDrawClearButton, AnimateButton.DIRECTION_L_R, (View[])null);
                        e.animate(true);
                    }
                    else {
                        /*
                         * Show the animation button for dest
                         */
                        mInfoText.setText(data.info);
                        mChartText.setText(data.chart);
                        if(isSameDest(data.airport)) {
                            mDestButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.remove, 0, 0, 0);
                            mDestButton.setText(getString(R.string.Delete));
                        }
                        else {
                            mDestButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.direct, 0, 0, 0);
                            mDestButton.setText(getString(R.string.Destination));
                        }
                        mCrossButton.setText(data.airport);
                        mDestLayout.setVisibility(View.VISIBLE);
                        
                        /*
                         * Now populate the pop out weather etc.
                         */
                        PopoutAdapter p = new PopoutAdapter(getApplicationContext(), data);
                        mListPopout.setAdapter(p);
                    }
                }
            }
            
        });

        mInfoText = (TextView)view.findViewById(R.id.location_text_info);
        mChartText = (TextView)view.findViewById(R.id.location_text_chart);

        mListPopout = (ExpandableListView)view.findViewById(R.id.location_list_popout);

        mChartSpinner = (Spinner)view.findViewById(R.id.location_spinner_chart);
        mChartSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
                if(mSpinner == false) {
                    /*
                     * Shitty spinner calls this when inflated. Send it back on inflation.
                     */
                    mSpinner = true;
                    mChartSpinner.setSelection(Integer.parseInt(mPref.getChartType()));
                    return;
                }
                mPref.setChartType("" + id);
                /*
                 * Contrast bars only in terrain view
                 */
                if(mPref.getChartType().equals("5")) {
                    mBar.setVisibility(View.VISIBLE);
                }
                else {
                    mBar.setVisibility(View.INVISIBLE);
                }
                mLocationView.forceReload();                
            }
           
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        
        mBar = (VerticalSeekBar)view.findViewById(R.id.location_seekbar_threshold);
        mBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       

            @Override       
            public void onStopTrackingTouch(SeekBar seekBar) {      
            }       

            @Override       
            public void onStartTrackingTouch(SeekBar seekBar) {     
            }       

            @Override       
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLocationView.updateThreshold(progress);
            }       
        });
        
        mZoom = (ZoomControls)view.findViewById(R.id.location_zoom_controls);
        mZoom.setVisibility(View.INVISIBLE);
        mZoom.setOnZoomInClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(!mLocationView.zoomIn(true)) {
                    mToast.setText(getString(R.string.noZoomIn));
                    mToast.show();
                }
            }
        });
        mZoom.setOnZoomOutClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(!mLocationView.zoomIn(false)) {
                    mToast.setText(getString(R.string.noZoomOut));
                    mToast.show();
                }
            }
        });
        
        mCenterButton = (Button)view.findViewById(R.id.location_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mLocationView.center();
            }
            
        });

        mCrossButton = (Button)view.findViewById(R.id.location_button_cross);
        mCrossButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mDestLayout.setVisibility(View.INVISIBLE);
            }
            
        });

        mDestLayout = (RelativeLayout)view.findViewById(R.id.location_popout_layout);

        mMenuButton = (Button)view.findViewById(R.id.location_button_menu);
        mMenuButton.getBackground().setAlpha(255);
        mMenuButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AnimateButton k = new AnimateButton(getApplicationContext(), mTracksButton, AnimateButton.DIRECTION_R_L);
                AnimateButton s = new AnimateButton(getApplicationContext(), mSimButton, AnimateButton.DIRECTION_R_L);
                AnimateButton z = new AnimateButton(getApplicationContext(), mZoom, AnimateButton.DIRECTION_R_L);
                AnimateButton t = new AnimateButton(getApplicationContext(), mTrackButton, AnimateButton.DIRECTION_R_L);
                AnimateButton c = new AnimateButton(getApplicationContext(), mChartSpinner, AnimateButton.DIRECTION_R_L, (View[])null);
                AnimateButton b = new AnimateButton(getApplicationContext(), mHelpButton, AnimateButton.DIRECTION_L_R, mCenterButton, mMenuButton, mDrawButton);
                AnimateButton d = new AnimateButton(getApplicationContext(), mDownloadButton, AnimateButton.DIRECTION_L_R, (View[])null);
                AnimateButton f = new AnimateButton(getApplicationContext(), mPrefButton, AnimateButton.DIRECTION_L_R, (View[])null);
                b.animate(true);
                d.animate(true);
                c.animate(true);
                s.animate(true);
                t.animate(true);
                f.animate(true);
                z.animate(true);
                k.animate(true);
            }
            
        });

        mHelpButton = (Button)view.findViewById(R.id.location_button_help);
        mHelpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationActivity.this, WebActivity.class);
                intent.putExtra("url", NetworkHelper.getHelpUrl());
                startActivity(intent);
            }
            
        });

        mPlanButton = (Button)view.findViewById(R.id.location_button_plan);
        mPlanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Button b = mCrossButton;
                
                String type = Destination.BASE;
                if(b.getText().toString().contains("&")) {
                    type = Destination.GPS;
                }
                planTo(b.getText().toString(), type);
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

        /*
         * Dest button
         */
        mDestButton = (Button)view.findViewById(R.id.location_button_dest);
        mDestButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                Button b = mCrossButton;
                /*
                 * If button pressed was a destination go there, otherwise if none, then delete current dest
                 */
                String dest = b.getText().toString();
                if(mDestButton.getText().toString().equals(getString(R.string.Delete))) {
                    mService.setDestination(null);
                    mDestLayout.setVisibility(View.INVISIBLE);
                    mLocationView.invalidate();
                    return;
                }
                String type = Destination.BASE;
                if(dest.contains("&")) {
                    type = Destination.GPS;
                }
                goTo(dest, type);
            }
        });
        
        
        mSimButton = (ToggleButton)view.findViewById(R.id.location_button_sim);
        if(mPref.isSimulationMode()) {
            mSimButton.setText(getString(R.string.SimulationMode));
            mSimButton.setChecked(true);
        }
        else {
            mSimButton.setText(getString(R.string.Navigate));            
            mSimButton.setChecked(false);
        }
        mSimButton.setOnClickListener(new OnClickListener() {

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

        mTrackButton = (ToggleButton)view.findViewById(R.id.location_button_track);
        mTrackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Bring up preferences
                 */
                if(mTrackButton.getText().equals(getString(R.string.TrackUp))) {
                    mLocationView.setTrackUp(true);
                }
                else {
                    mLocationView.setTrackUp(false);
                }
            }
            
        });

        /*
         * Draw
         */
        mDrawButton = (Button)view.findViewById(R.id.location_button_draw);
        mDrawButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Bring up preferences
                 */
                if(mDrawButton.getText().equals(getString(R.string.Draw))) {
                    mLocationView.setDraw(true);
                }
                else {
                    mLocationView.setDraw(false);                    
                }
            }
            
        });

        /*
         * The tracking button handler. Enable/Disable the saving of track points
         * to a KML file
         */
        mTracksButton = (Button)view.findViewById(R.id.location_button_tracks);
        mTracksButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(null != mService) {
                	URI fileURI = mService.setTracks(mPref, !mService.getTracks());
                	/* The fileURI is returned when the tracks are closed off.
                	 */
                	if(fileURI != null) {
	                	String fileName = fileURI.getPath().substring((fileURI.getPath().lastIndexOf('/') + 1));
	        			switch(mPref.autoPostTracks()) {
		    				case 0:
		    	    			/* Just display a toast message to the user that the file was saved
		    	    			 */
		    	    			Toast.makeText(getApplicationContext(), 
		    	    							String.format(getString(R.string.AutoPostTracksDialogText), fileName), 
		    	    							Toast.LENGTH_LONG).show();
		    	    			break;
		
		    				case 1:
		    	    			/* Send this file out as an email attachment
		    	    			 */
		    	        	    try {
		    	            	    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		    	            	    emailIntent.setType("application/kml");
		    	                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.AutoPostTracksSubject) + " " + fileName); 
		    	            	    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(fileURI.getPath())));
		    	            	    startActivity(emailIntent);
		    	        	    } catch (Exception e) { 
		    	        	        
		    	        	    }
		    	        	    break;
		
		    				case 2:
		    	    			/* Send it somewhere as KML. Let the user choose where.
		    	    			 */
		    	        	    try {
		    	            	    Intent viewIntent = new Intent(android.content.Intent.ACTION_VIEW);
		    	            	    viewIntent.setDataAndType(Uri.fromFile(new File(fileURI.getPath())), "application/vnd.google-earth.kml+xml");
		    	            	    startActivity(Intent.createChooser(viewIntent, getString(R.string.AutoPostTracksTitle)));
		    	        	    } catch (Exception e) { 
		    	        	        
		    	        	    }
		    	        	    break;
		    			}
                	}
                }
            }        
        });

        /*
         * Throw this in case GPS is disabled.
         */
        if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
            mGpsWarnDialog = new AlertDialog.Builder(LocationActivity.this).create();
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
            mGpsWarnDialog.show();        
        }
        
        /*
         * Check if this was sent from Google Maps
         */
        mExtras = getIntent().getExtras();
 
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

            mService.getTiles().setOrientation();
            
            /*
             * Check if database needs upgrade
             */
            if(mPref.isNewerVersion(LocationActivity.this)) {
                Intent i = new Intent(LocationActivity.this, ChartsDownloadActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                return;
            }
            if(!mService.getDBResource().isPresent()) {
                mToast.setText(R.string.DownloadDB);
                mToast.show();
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
            if(mPref.isSimulationMode() && (null != mDestination)) {
                l = mDestination.getLocation();
            }
            if(null != l) {
                mService.setGpsParams(new GpsParams(l));
            }
            
            if(null != mService.getGpsParams()) {
                mLocationView.initParams(mService.getGpsParams(), mService); 
                mLocationView.updateParams(mService.getGpsParams());
            }

            mLocationView.updateDestination();

            /*
             * Show avare warning when service says so 
             */
            if(mService.shouldWarn()) {
             
                mAlertDialogWarn = new AlertDialog.Builder(LocationActivity.this).create();
                mAlertDialogWarn.setTitle(getString(R.string.WarningMsg));
                mAlertDialogWarn.setMessage(getString(R.string.Warning));
                mAlertDialogWarn.setCanceledOnTouchOutside(false);
                mAlertDialogWarn.setCancelable(false);
                mAlertDialogWarn.setIcon(R.drawable.important_red);
                mAlertDialogWarn.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Understand), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    
                mAlertDialogWarn.show();
            }    
            
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
                    
                    mDestination = new Destination(addr, Destination.MAPS, mPref, mService);
                    mDestination.addObserver(LocationActivity.this);
                    mToast.setText(getString(R.string.Searching) + " " + addr);
                    mToast.show();
                    mDestination.find();
                }
                mExtras = null;
            }

            /*
             * Force reload
             */
            mLocationView.forceReload();
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
        /*
         * Contrast bars only in terrain view
         */
        if(mPref.getChartType().equals("5")) {
            mBar.setVisibility(View.VISIBLE);
        }
        else {
            mBar.setVisibility(View.INVISIBLE);
        }

        mDestLayout.setVisibility(View.INVISIBLE);

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
        
        /*
         * Kill dialogs
         */
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
        
        if(null != mAlertDialogExit) {
            try {
                mAlertDialogExit.dismiss();
            }
            catch (Exception e) {
            }
        }
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
        mLocationView.cleanup();
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
                mLocationView.updateDestination();
                if(!mIsWaypoint) {
                    if(mService != null) {
                        mService.setDestination((Destination)arg0);
                    }
                    mToast.setText(getString(R.string.DestinationSet) + ((Destination)arg0).getID());
                    mToast.show();
                    ((MainActivity)this.getParent()).switchTab(0);
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
}
