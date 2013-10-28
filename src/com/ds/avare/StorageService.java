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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import com.ds.avare.flightLog.KMLRecorder;
import com.ds.avare.gps.*;
import com.ds.avare.hobbsMeter.FlightTimer;
import com.ds.avare.network.TFRFetcher;
import com.ds.avare.place.Area;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Pan;
import com.ds.avare.shapes.Draw;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.weather.InternetWeatherCache;

import android.app.Service;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import java.io.File;
import java.net.URI;

/**
 * @author zkhan
 * Main storage service. It stores all states so when activity dies,
 * we dont start from no state.
 * This is especially important for start up functions that take time,
 * one of which is databse un-zipping.
 * 
 * Also sends intent to display warning, since its too intrusive to show a 
 * warning every time activity starts.
 */
public class StorageService extends Service {

    /**
     * The Sqlite database
     */
    private DataSource mImageDataSource;
    /**
     * Store this
     */
    private Destination mDestination;
    /**
     * Store this
     */
    private GpsParams mGpsParams;
    /**
     * Store this
     */
    private Movement mMovement;
    
    private Draw mDraw;
    
    private InternetWeatherCache mInternetWeatherCache;
    
    /**
     * GPS
     */
    private Gps mGps;

    /**
     * Store this
     */
    private Pan mPan;
    
    /*
     * Plate showing
     */
    private int mPlateIndex;
    
    /*
     * A/FD showing
     */
    private int mAfdIndex;

    /**
     * Area around us
     */
    private Area mArea;

    /**
     * Flight plan
     */
    private Plan mPlan;

    /**
     * TFR list
     */
    private TFRFetcher mTFRFetcher;

    /**
     * For performing periodic activities.
     */
    private Timer mTimer;
    
    /*
     * A list of GPS listeners
     */
    private LinkedList<GpsInterface> mGpsCallbacks;

    /*
     * A diagram bitmap
     */
    private BitmapHolder mDiagramBitmap;                            
        
    /**
     * Local binding as this runs in same thread
     */
    private final IBinder binder = new LocalBinder();

    /*
     * When to show warning
     */
    private Boolean mShouldWarn = true;
    
    private boolean mIsGpsOn;
    
    private int mCounter;
    
    private TileMap mTiles;
    
    /*
     * Hobbs time
     */
    private FlightTimer  mFlightTimer;

    /*
     * Declare our KML position tracker
     * For writing plots to a KML file
     */
    private KMLRecorder mKMLRecorder;

    /**
     * @author zkhan
     *
     */
    public class LocalBinder extends Binder {
        /**
         * @return
         */
        public StorageService getService() {
            return StorageService.this;
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }
    
    /* (non-Javadoc)
     * @see android.app.Service#onUnbind(android.content.Intent)
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
          
        super.onCreate();

        mImageDataSource = new DataSource(getApplicationContext());
        
        mArea = new Area(mImageDataSource);
        mPlan = new Plan();
        
        /*
         * All tiles
         */
        mTiles = new TileMap(getApplicationContext());
          
        mInternetWeatherCache = new InternetWeatherCache();
        mInternetWeatherCache.parse(this);
        mTFRFetcher = new TFRFetcher(getApplicationContext());
        mTFRFetcher.parse();
        mTimer = new Timer();
        TimerTask gpsTime = new UpdateTask();
        mIsGpsOn = false;
        mGpsCallbacks = new LinkedList<GpsInterface>();
        mDiagramBitmap = null;
        mPlateIndex = 0;
        mAfdIndex = 0;
        
        mDraw = new Draw();
        
        /*
         * Allocate a flight timer object
         */
        mFlightTimer = new FlightTimer();

        /*
         * Start up the KML recorder feature
         */
        mKMLRecorder = new KMLRecorder();

        /*
         * Monitor TFR every hour.
         */
        mTimer.scheduleAtFixedRate(gpsTime, 0, 60 * 1000);
        
        /*
         * Start GPS, and call all activities registered to listen to GPS
         */
        GpsInterface intf = new GpsInterface() {

            /**
             * 
             * @return
             */
            private LinkedList<GpsInterface> extracted() {
                return (LinkedList<GpsInterface>)mGpsCallbacks.clone();
            }

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GpsInterface#statusCallback(android.location.GpsStatus)
             */            
            @Override
            public void statusCallback(GpsStatus gpsStatus) {
                LinkedList<GpsInterface> list = extracted();
                Iterator<GpsInterface> it = list.iterator();
                while (it.hasNext()) {
                    GpsInterface infc = it.next();
                    infc.statusCallback(gpsStatus);
                }
            }

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GpsInterface#locationCallback(android.location.Location)
             */
            @Override
            public void locationCallback(Location location) {
                LinkedList<GpsInterface> list = extracted();
                Iterator<GpsInterface> it = list.iterator();
                while (it.hasNext()) {
                    GpsInterface infc = it.next();
                    infc.locationCallback(location);
                    if(null != location) {
                        if(!location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                            /*
                             * Getting location from somewhere other than built in GPS.
                             * Update timeout so we do not timeout on GPS timer.
                             */
                            mGps.updateTimeout();
                        }
                        setGpsParams(new GpsParams(location));
                        getArea().updateLocation(getGpsParams());
                        getPlan().updateLocation(getGpsParams());
                        if(getPlan().hasDestinationChanged()) {
                            /*
                             * If plan active then set destination to next not passed way point
                             */
                            setDestinationPlanNoChange(getPlan().getDestination(getPlan().findNextNotPassed()));
                        }

                        if(mDestination != null) {
                            mDestination.updateTo(getGpsParams());
                        }
                    }
                }
            }

            /*
             * (non-Javadoc)
             * @see com.ds.avare.GpsInterface#timeoutCallback(boolean)
             */
            @Override
            public void timeoutCallback(boolean timeout) {
                LinkedList<GpsInterface> list = extracted();
                Iterator<GpsInterface> it = list.iterator();
                while (it.hasNext()) {
                    GpsInterface infc = it.next();
                    infc.timeoutCallback(timeout);
                }                
            }

            @Override
            public void enabledCallback(boolean enabled) {
                LinkedList<GpsInterface> list = extracted();
                Iterator<GpsInterface> it = list.iterator();
                while (it.hasNext()) {
                    GpsInterface infc = it.next();
                    infc.enabledCallback(enabled);
                }
                if(enabled) {
                    if(!mGpsCallbacks.isEmpty()) {
                        mGps.start();
                    }
                }
            }
        };
        mGps = new Gps(this, intf);
    }
        
    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        /*
         * If we ever exit, reclaim memory
         */
        mTiles.recycleBitmaps();
        
        if(null != mDiagramBitmap) {
            mDiagramBitmap.recycle();
            mDiagramBitmap = null;
        }
        mTiles = null;
        
        System.gc();
        
        if(mTimer != null) {
            mTimer.cancel();
        }
        if(mGps != null) {
            mGps.stop();
        }
        super.onDestroy();
        
        System.runFinalizersOnExit(true);
        System.exit(0);
    }
    
    public TileMap getTiles() {
        return mTiles;
    }
    
    /*
     * Get/Set (state), Get (resources, state) functions for activity
     */

    /**
     * @return
     */
    public Boolean shouldWarn() {
        if(mShouldWarn) {
            mShouldWarn = false;
            return true;
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public TFRFetcher getTFRFetcher() {
        return mTFRFetcher;
    }
    
    /**
     * @return
     */
    public LinkedList<TFRShape> getTFRShapes() {
        return mTFRFetcher.getShapes();
    }

    /**
     * @return
     */
    public DataSource getDBResource() {
        return mImageDataSource;
    }
    
    /**
     * @return
     */
    public Destination getDestination() {
        return mDestination;
    }

    /**
     * @param destination
     */
    public void setDestination(Destination destination) {
        mDestination = destination;
        mPlateIndex = 0;
        mAfdIndex = 0;
        getPlan().makeInactive();
    }

    /**
     * @param destination from plan
     */
    public void setDestinationPlanNoChange(Destination destination) {
        mDestination = destination;
        mPlateIndex = 0;
        mAfdIndex = 0;
    }

    /**
     * @param destination from plan
     */
    public void setDestinationPlan(Destination destination) {
        mDestination = destination;
        mPlateIndex = 0;
        mAfdIndex = 0;
        getPlan().makeActive(mGpsParams);
    }

    /**
     * 
     * @param index
     */
    public void setPlateIndex(int index) {
        mPlateIndex = index;
    }

    /**
     * 
     * @return
     */
    public int getPlateIndex() {
        return mPlateIndex;
    }

    /**
     * 
     * @param index
     */
    public void setAfdIndex(int index) {
        mAfdIndex = index;
    }

    /**
     * 
     * @return
     */
    public int getAfdIndex() {
        return mAfdIndex;
    }

    /**
     * @return
     */
    public GpsParams getGpsParams() {
        return mGpsParams;
    }

    /**
     * @param params
     */
    public void setGpsParams(GpsParams params) {
        mGpsParams = params;
    }

    /**
     * @param m
     */
    public void setMovement(Movement m) {
        mMovement = m;
    }

    /**
     * @return
     */
    public Movement getMovement() {
        return mMovement;
    }

    
    /**
     * @param m
     */
    public void setPan(Pan p) {
        mPan = p;
    }

    /**
     * @return
     */
    public Pan getPan() {
        return mPan;
    }

    /**
     * @return
     */
    public Area getArea() {
        return mArea;
    }

    /**
     * @return
     */
    public Plan getPlan() {
        return mPlan;
    }

    /**
     * @return
     */
    public void newPlan() {
        mPlan = new Plan();
    }

    /**
     * 
     * @param name
     */
    public void loadDiagram(String name) {
        if(mDiagramBitmap != null) {
            /*
             * Clean old one first
             */
            mDiagramBitmap.recycle();
            mDiagramBitmap = null;
            System.gc();
        }
        if(null != name) {
            mDiagramBitmap = new BitmapHolder(name);            
        }
    }
    
    /**
     * 
     * @return
     */
    public BitmapHolder getDiagram() {
       return mDiagramBitmap; 
    }
    
    /**
     * @author zkhan
     *
     */
    private class UpdateTask extends TimerTask {
        
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        public void run() {

            /*
             * Stop the GPS delayed by 1 to 2 minutes if no other activity is registered 
             * to it for 1 to 2 minutes.
             */
            synchronized(this) {
                mCounter++;
                if((!mIsGpsOn) && (mGps != null) && (mCounter >= 2)) {
                    mGps.stop();
                }
            }

        }
    }
    
    /**
     * 
     * @param gps
     */
    public void registerGpsListener(GpsInterface gps) {
        /*
         * If first listener, start GPS
         */
        mGps.start();
        synchronized(this) {
            mIsGpsOn = true;
        }
        synchronized(mGpsCallbacks) {
            mGpsCallbacks.add(gps);
        }
    }

    /**
     * 
     * @param gps
     */
    public void unregisterGpsListener(GpsInterface gps) {
        
        boolean isempty = false;
        
        synchronized(mGpsCallbacks) {
            mGpsCallbacks.remove(gps);
            isempty = mGpsCallbacks.isEmpty();
        }
        
        /*
         * If no listener, relinquish GPS control
         */
        if(isempty) {
            synchronized(this) {
                mCounter = 0;
                mIsGpsOn = false;                
            }            
        }
    }

    /**
     * Get points to draw
     * @return
     */
    public Draw getDraw() {
        return mDraw;
    }
    
    /**
     * 
     * @return
     */
    public InternetWeatherCache getInternetWeatherCache() {
        return mInternetWeatherCache;
    }
    
    /**
     * 
     * @return
     */
    public FlightTimer getFlightTimer() {
        return mFlightTimer;
    }
    
    /**
     * Called when the user presses the "tracks" button on the locationview screen to
     * toggle the state of the saving of GPS positions.
     * @param b enable/disable tracking
     * @return URI of the file that was just closed, or null if it was just opened
     */
    public URI setTracks(Preferences pref, boolean shouldTrack) {
        if(shouldTrack) {
            KMLRecorder.Config config = mKMLRecorder.new Config(
                true,	/* always remove tracks from display on start */
                10,		/* 10 seconds between position updates */
                true,	/* use verbose details */
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar + "com.ds.avare" + File.separatorChar + "Tracks",
                20);	/* How fast we are going before starting to track, 20 knots */
            mKMLRecorder.start(config);
            return null;
        }
        else {
            return mKMLRecorder.stop();
        }
    }

    /**
     * Are we currently saving the location information
     * @return Boolean to indicate whether we are actively writing tracks
     */
    public boolean getTracks() {
        return mKMLRecorder.isRecording();
    }

    /**
     * 
     * @return
     */
    public KMLRecorder getKMLRecorder() {
        return mKMLRecorder;
    }
    
    /**
     * 
     * @return
     */
    public Gps getGps() {
        return mGps;
    }
}