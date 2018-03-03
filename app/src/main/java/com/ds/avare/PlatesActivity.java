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
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ds.avare.animation.TwoButton;
import com.ds.avare.animation.TwoButton.TwoClickListener;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.FuelTimer;
import com.ds.avare.instruments.UpTimer;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.place.Plan;
import com.ds.avare.plan.Cifp;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.PngCommentReader;
import com.ds.avare.views.PlatesView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

/**
 * @author zkhan,rasii
 * An activity that deals with plates
 */
public class PlatesActivity extends Activity implements Observer, Chronometer.OnChronometerTickListener  {
    private Preferences mPref;
    private PlatesView mPlatesView;
    private StorageService mService;
    private Destination mDestination;
    private ImageButton mCenterButton;
    private Button mAirportButton;
    private Button mPlatesButton;
    private Button mApproachButton;
    private Button mPlatesTagButton;
    private Button mPlatesTimerButton;
    private Chronometer mChronometer;
    private AlertDialog mPlatesPopup;
    private AlertDialog mApproachPopup;
    private AlertDialog mAirportPopup;
    private Button mDrawClearButton;
    private TwoButton mDrawButton;
    private Destination mDest;
    private Toast mToast;
    private ArrayList<String> mListPlates;
    private ArrayList<String> mListApproaches;
    private ArrayList<String> mListAirports;
    private String mPlateFound[];
    private String mDestString;
    private String nearString;
    private boolean mCounting;
    private LinkedList<Cifp> mCifp;
    private TankObserver mTankObserver;
    private TimerObserver mTimerObserver;

    public static final String AD = "AIRPORT-DIAGRAM";
    public static final String AREA = "AREA";

    /*
     * For GPS taxi
     */
    private float[] mMatrix;


    /**
     *
     */
    public static String getNameFromPath(String name) {
        if(null == name) {
            return null;
        }
        String parts[] = name.split("/");
        if(parts.length < 2) {
            return null;
        }
        String aname = parts[parts.length - 2] + "/" + parts[parts.length - 1];
        return aname;
    }


    /**
     * @return
     */
    public float[] getMatrix(String name) {
        if(name.equals(AD)) {
            return(mMatrix);            
        }

        if(mService != null && mService.getPlateDiagram() != null && mService.getPlateDiagram().getName() != null) {


            String aname = getNameFromPath(mService.getPlateDiagram().getName());
            if(aname != null) {
                float ret[];

                // find in user's own tags first
                LinkedList<String> tags = PlatesTagActivity.getTagsStorageFromat(mPref.getGeotags());
                for(String t : tags) {
                    String toks[] = t.split(",");
                    if(toks[0].equals(aname)) {
                       /*
                        * Found
                        */
                        float matrix[] = new float[4];
                        matrix[0] = (float)Double.parseDouble(toks[1]);
                        matrix[1] = (float)Double.parseDouble(toks[2]);
                        matrix[2] = (float)Double.parseDouble(toks[3]);
                        matrix[3] = (float)Double.parseDouble(toks[4]);
                        return matrix;
                    }
                }

                /*
                 * EXIF
                 */
                if(name.startsWith(AREA)) {
                    ret = PngCommentReader.readPlate(mPref.mapsFolder() + "/area/" + aname);
                }
                else {
                    ret = PngCommentReader.readPlate(mPref.mapsFolder() + "/plates/" + aname);
                }
                if(null != ret) {
                    return ret;
                }

            }
        }
        return null;
    }    
       
    /*
     * Add an airport to the airports list if it doesn't already exist
     */
    private void addAirport(String name) {
        if(!mListAirports.contains(name) && doesAirportHavePlates(mPref.mapsFolder(), name)) {
            mListAirports.add(name);
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
    
    private boolean arePopupsShowing() {
        return (null != mPlatesPopup && mPlatesPopup.isShowing()) || 
                (null != mAirportPopup && mAirportPopup.isShowing()) ||
                (null != mApproachPopup && mApproachPopup.isShowing());
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mPref = new Preferences(getApplicationContext());
        
        mDestString = "<" + getString(R.string.Destination) + ">";
        nearString = "<" + getString(R.string.Nearest) + ">";
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plates, null);
        setContentView(view);
        mPlatesView = (PlatesView)view.findViewById(R.id.plates);
        
        mPlatesButton = (Button)view.findViewById(R.id.plates_button_plates);
        mPlatesButton.getBackground().setAlpha(255);
        mPlatesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListPlates.size() == 0 || arePopupsShowing()) {
                    return;
                }

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setPlateFromPos(which);
                    }
                };

                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(PlatesActivity.this);
                int index = mService.getLastPlateIndex();
                if (index >= mListPlates.size()) {
                    index = 0;
                }
                builder.setTitle(getString(R.string.SelectPlateToShow));
                mPlatesPopup = builder.setSingleChoiceItems(mListPlates.toArray(new String[mListPlates.size()]), index, onClickListener).create();
                if(!isFinishing()) {
                    mPlatesPopup.show();
                }
            }
        });

        mApproachButton = (Button)view.findViewById(R.id.plates_button_approach);
        mApproachButton.getBackground().setAlpha(255);
        mApproachButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arePopupsShowing()) {
                    return;
                }

                if (mListApproaches.size() == 0) {
                    mToast.setText(getString(R.string.NoApproachToShow));
                    mToast.show();
                    return;
                }

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (mCifp == null || which >= mCifp.size()) {
                            return;
                        }
                        Cifp cifp = mCifp.get(which);
                        cifp.setApproach(mService, mPref);
                        ((MainActivity) PlatesActivity.this.getParent()).showPlanTab();
                    }
                };

                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(PlatesActivity.this);
                builder.setTitle(getString(R.string.SelectApproachToShow));
                mApproachPopup = builder.setSingleChoiceItems(mListApproaches.toArray(new String[mListApproaches.size()]), 0, onClickListener).create();
                if(!isFinishing()) {
                    mApproachPopup.show();
                }
            }
        });


        /*
         * Timer, make chronometer invisible. Just use button to show time
         */
        mChronometer = (Chronometer)view.findViewById(R.id.plates_chronometer);
        mCounting = false;
        mPlatesTimerButton = (Button)view.findViewById(R.id.plates_button_timer);
        mPlatesTimerButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!mCounting) {
                    /*
                     * Show when counting, dont show when stopped
                     */
                    mCounting = true;
                    mPlatesTimerButton.setText("");
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mChronometer.setOnChronometerTickListener(PlatesActivity.this);
                }
                else {
                    mCounting = false;
                    mChronometer.setOnChronometerTickListener(null);
                    mPlatesTimerButton.setText(getString(R.string.Timer));
                }
            }
        });
        if(mPref.removeB2Plate()) {
            mPlatesTimerButton.setVisibility(View.INVISIBLE);
        }


        /*
         * Draw
         */
        mDrawButton = (TwoButton)view.findViewById(R.id.plate_button_draw);
        mDrawButton.setTwoClickListener(new TwoClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Bring up preferences
                 */
                if(mDrawButton.getText().equals(getString(R.string.Draw))) {
                    mPlatesView.setDraw(true);
                    mDrawClearButton.setVisibility(View.VISIBLE);
                }
                else {
                    mPlatesView.setDraw(false);                    
                    mDrawClearButton.setVisibility(View.INVISIBLE);
                }
            }
            
        });
        if(mPref.removeB1Plate()) {
            mDrawButton.setVisibility(View.INVISIBLE);
        }

        mDrawClearButton = (Button)view.findViewById(R.id.plate_button_draw_clear);
        mDrawClearButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mService != null) {
                    if(mPlatesView.getDraw()) {
                        mService.getPixelDraw().clear();
                    }
                }
            }            
        });
        
        mAirportButton = (Button)view.findViewById(R.id.plates_button_airports);
        mAirportButton.getBackground().setAlpha(255);
        mAirportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListAirports == null || mListAirports.size() == 0 || arePopupsShowing()) {
                    return;
                }

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setAirportFromPos(which);
                    }
                };

                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(PlatesActivity.this);
                int index = mListAirports.indexOf(mService.getLastPlateAirport());
                builder.setTitle(getString(R.string.SelectAirportToShow));
                mAirportPopup = builder.setSingleChoiceItems(mListAirports.toArray(new String[mListAirports.size()]), index, onClickListener).create();
                if(!isFinishing()) {
                    mAirportPopup.show();
                }
            }
        });      
               
        mCenterButton = (ImageButton)view.findViewById(R.id.plates_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlatesView.center();
            }
        });
        mCenterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // long press on center button sets track toggle
                mPref.setTrackUpPlates(!mPref.isTrackUpPlates());
                if (mPref.isTrackUpPlates()) {
                    mCenterButton.getBackground().setColorFilter(0xFF71BC78, PorterDuff.Mode.MULTIPLY);
                    mToast.setText(getString(R.string.TrackUp));
                } else {
                    mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
                    mToast.setText(getString(R.string.NorthUp));
                }
                mToast.show();
                mPlatesView.invalidate();
                return true;
            }
        });


        mPlatesTagButton = (Button)view.findViewById(R.id.plates_button_tag);
        mPlatesTagButton.getBackground().setAlpha(255);
        mPlatesTagButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null && mService.getPlateDiagram() != null) {
                    String name = mService.getPlateDiagram().getName();
                    if (name != null) {
                        Intent intent = new Intent(PlatesActivity.this, PlatesTagActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });

        if(mPref.removeB3Plate()) {
            mPlatesTagButton.setVisibility(View.INVISIBLE);
        }


        /*
         * Create toast beforehand so multiple clicks don't throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        // Allocate the watch object for fuel tanks
        mTankObserver = new TankObserver();
        mTimerObserver = new TimerObserver();

        mService = null;
    }
    
    private void setPlateFromPos(int pos) {
        if(mService != null && mPlateFound != null) {
            if(pos >= mPlateFound.length) {
                pos = 0;
            }

            if(mService.getPlateDiagram() == null
                    || mService.getPlateDiagram().getName() == null
                    || (!mService.getPlateDiagram().getName().equals(mPlateFound[pos] + Preferences.IMAGE_EXTENSION))) {
                mService.loadPlateDiagram(mPlateFound[pos]);
            }
            mPlatesView.setBitmap(mService.getPlateDiagram());
            String name = mListPlates.get(pos);

            mPlatesView.setParams(null, true);
            float m[] = getMatrix(name);
            mService.setMatrix(null); // to small to show on map
            if(name.startsWith(AD)) {
                mPlatesView.setParams(m, true);
            }
            else if(name.startsWith(AREA)) {
                mPlatesView.setParams(m, false);
            }
            else {
                mPlatesView.setParams(m, false);
                mService.setMatrix(m); // save for other activities that want to show plate
            }
            mPlatesButton.setText(name);
            mService.setLastPlateIndex(pos);
        }
        else {
            mPlatesButton.setText("");
            mToast.setText(getString(R.string.PlatesNF));
            mToast.show();
        }

        // Get flight procedures set up for this plate
        // Note: Move to BG task
        mListApproaches = new ArrayList<>();
        mCifp = mService.getDBResource().findProcedure(mAirportButton.getText().toString(), mPlatesButton.getText().toString());
        if(mCifp.size() != 0) {
            // Show which plates have approaches
            mApproachButton.setTextColor(0xFF007F00);
        }
        else {
            mApproachButton.setTextColor(0xFFFF0000);
        }
        for(Cifp cifp : mCifp) {
            mListApproaches.add(cifp.getInitialCourse());
        }
    } 
    
    private String getLastIfAirport() {
        String airport = null;
        if(null != mService) {
            airport = mService.getLastPlateAirport();
            if(null != airport && (airport.equals(mDestString) || airport.equals(nearString))) {
                airport = null;
            }
        }
        
        return airport;
    }
    
    private void setAirportFromPos(int pos) {
        if(mService != null && mListAirports != null && mListAirports.size() > pos) {
            String airport = mListAirports.get(pos);
            
            Destination curDest = mService.getDestination();
            if(curDest != null && !curDest.getType().equals(Destination.BASE)) {
                curDest = null;
            }
            if(airport.equals(mDestString)) {   
                if(null != curDest && doesAirportHavePlates(mPref.mapsFolder(), curDest.getID())) {
                    airport = curDest.getID();
                }
                else {
                    airport = getLastIfAirport();
                }
                mService.setLastPlateAirport(mDestString);                
            }
            else if(airport.equals(nearString)) {
                int nearestNum = mService.getArea().getAirportsNumber();
                Airport nearest = null;
                if(nearestNum > 0) {
                    nearest = mService.getArea().getAirport(0);
                    if(doesAirportHavePlates(mPref.mapsFolder(), nearest.getId())) {
                        airport = nearest.getId();
                    }
                    else {
                        airport = getLastIfAirport();
                    }
                }
                else {
                    airport = getLastIfAirport();
                }
                mService.setLastPlateAirport(nearString);
            }
            else {
                mService.setLastPlateAirport(airport);
            }
            
            if(null == airport && mListAirports.size() > 2) {
                airport = mListAirports.get(2);
            }
            
            mPlateFound = null;
            if(null != airport) {
                
                mDest = DestinationFactory.build(mService, airport, Destination.BASE);
                mDest.addObserver(PlatesActivity.this);
                mDest.find();

                String mapFolder = mPref.mapsFolder();

                /*
                 * Start with the plates in the /plates/ directory
                 */
                if(null != airport) {
                    FilenameFilter filter = new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.endsWith(Preferences.IMAGE_EXTENSION);
                        }
                    };
                    
                    /*
                     * TODO: RAS make this an async request (maybe just move all 
                     * the loading to the background - especially if we get the last used 
                     * chart loaded immediately)
                     */

                    String dplates[] = new File(mapFolder + "/plates/" + airport).list(filter);
                    String aplates[] = new File(mapFolder + "/area/" + airport).list(filter);
                    String mins[] = mService.getDBResource().findMinimums(airport);

                    TreeMap<String, String> plates = new TreeMap<String, String>(new PlatesComparable());
                    if (dplates != null) {
                    	for(String plate : dplates) {
	                        String tokens[] = plate.split(Preferences.IMAGE_EXTENSION);
	                    	plates.put(tokens[0], mapFolder + "/plates/" + airport + "/" + tokens[0]);
                    	}
                    }
                    if (aplates != null) {
	                    for(String plate : aplates) {
	                        String tokens[] = plate.split(Preferences.IMAGE_EXTENSION);
	                    	plates.put(tokens[0], mapFolder + "/area/" + airport + "/" + tokens[0]);
	                    }
                    }
                    if(mins != null) {
	                    for(String plate : mins) {
	                        String folder = plate.substring(0, 1) + "/";
	                    	plates.put("Min. " + plate, mapFolder + "/minimums/" + folder + plate);
	                    }
                    }
                    if(plates.size() > 0) {
                    	mPlateFound = Arrays.asList(plates.values().toArray()).toArray(new String[plates.values().toArray().length]);
                        mListPlates = new ArrayList<String>(plates.keySet());                        
                    }
                }
            }

            if(null != mPlateFound) {
                /*
                 * GPS taxi for this airport?
                 */
                mMatrix = mService.getDBResource().findDiagramMatrix(airport);
                mService.setMatrix(null);
                
                String oldAirport = mAirportButton.getText().toString();
                mAirportButton.setText(airport);
                
                /*
                 * A list of plates available for this airport
                 */
                setPlateFromPos(airport.equals(oldAirport) ? mService.getLastPlateIndex() : 0);
            }
            else {
                mAirportButton.setText(mService.getLastPlateAirport());
                /*
                 * Reset to the last one that worked
                 */ 
                mToast.setText(getString(R.string.PlatesNF));
                mToast.show();
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
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);         
            mPlatesView.setService(mService);

            mListPlates = new ArrayList<String>();
            mListApproaches = new ArrayList<String>();

            mListAirports = new ArrayList<String>();
            mListAirports.add(mDestString);
            mListAirports.add(nearString);

            /*
             * Are we being told to load an airport?
             */
            if(null != mService.getLastPlateAirport()) {
                addAirport(mService.getLastPlateAirport());
            }
            
            /*
             *  Load the nearest airport
             */
            int nearestNum = mService.getArea().getAirportsNumber();
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

            int lastIndex = Math.max(mListAirports.indexOf(mService.getLastPlateAirport()), 0);
            setAirportFromPos(lastIndex);
            
            // Tell the fuel tank timer we need to know when it runs out
            mService.getFuelTimer().addObserver(mTankObserver);
            mService.getUpTimer().addObserver(mTimerObserver);
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
					mPlatesView.postInvalidate();
					break;

				case FuelTimer.SWITCH_TANK:
					AlertDialog alertDialog = new DecoratedAlertDialogBuilder(PlatesActivity.this).create();
					alertDialog.setTitle(PlatesActivity.this.getString(R.string.switchTanks));
					alertDialog.setCancelable(false);
					alertDialog.setCanceledOnTouchOutside(false);
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, PlatesActivity.this.getString(R.string.OK), new DialogInterface.OnClickListener() {
		
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
                    mPlatesView.postInvalidate();
                    break;

            }
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
            mService.getFuelTimer().removeObserver(mTankObserver);
            mService.getUpTimer().removeObserver(mTimerObserver);
        }

        try {
            mPlatesPopup.dismiss();
        }
        catch(Exception e) {
            
        }
        try {
            mApproachPopup.dismiss();
        }
        catch(Exception e) {

        }
        try {
            mAirportPopup.dismiss();
        }
        catch(Exception e) {
            
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
        
        if(null != mService) {
            // Tell the fuel tank timer we need to know when it runs out
            mService.getFuelTimer().addObserver(mTankObserver);
            mService.getUpTimer().addObserver(mTimerObserver);
        }

        // Button colors to be synced across activities
        if(mPref.isTrackUpPlates()) {
            mCenterButton.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
        }
        else {
            mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
        }
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
             * Airport diagram must be first
             */
            String[] type = {AD, "AREA", "ILS-", "HI-ILS-", "LOC-", "HI-LOC-", "LDA-", "SDA-", "GPS-", "RNAV-GPS-", "RNAV-RNP-", "VOR-", "HI-VOR-", "TACAN-", "HI-TACAN-", "NDB-", "COPTER-", "CUSTOM-", "LAHSO", "HOT-SPOT", "Min."};
            
            for(int i = 0; i < type.length; i++) {
                if(o1.startsWith(type[i]) && (!o2.startsWith(type[i]))) {
                    return -1;
                }
                if(o2.startsWith(type[i]) && (!o1.startsWith(type[i]))) {
                    return 1;
                }
            }

            /*
             * Continued must follow main
             */
            String comp1 = o2.replace(Preferences.IMAGE_EXTENSION, "");
            if(o1.contains("-CONT.") && (!o2.contains("-CONT."))) {
                if(o1.startsWith(comp1)) {
                    return 1;
                }
            }
            String comp2 = o1.replace(Preferences.IMAGE_EXTENSION, "");
            if(o2.contains("-CONT.") && (!o1.contains("-CONT."))) {
                if(o2.startsWith(comp2)) {
                    return -1;
                }
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
        return new File(mapFolder + "/plates/" + id).exists() || new File(mapFolder + "/area/" + id).exists();
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
    
    
    @Override
    public void update(Observable observable, Object data) {
        if(mDest.isFound()) {
            mPlatesView.setAirport(mDest.getID(), mDest.getLocation().getLongitude(), mDest.getLocation().getLatitude());
        }
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        /*
         * Set the label of timer button to time
         */
        mPlatesTimerButton.setText(chronometer.getText());
    }

}