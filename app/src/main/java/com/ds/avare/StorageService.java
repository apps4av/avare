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

import com.ds.avare.adsb.TrafficCache;
import com.ds.avare.cap.DrawCapLines;
import com.ds.avare.externalFlightPlan.ExternalPlanMgr;
import com.ds.avare.flight.Checklist;
import com.ds.avare.flight.FlightStatus;
import com.ds.avare.flightLog.KMLRecorder;
import com.ds.avare.gps.*;
import com.ds.avare.instruments.CDI;
import com.ds.avare.instruments.DistanceRings;
import com.ds.avare.instruments.EdgeDistanceTape;
import com.ds.avare.instruments.FlightTimer;
import com.ds.avare.instruments.FuelTimer;
import com.ds.avare.instruments.Odometer;
import com.ds.avare.instruments.VNAV;
import com.ds.avare.instruments.VSI;
import com.ds.avare.network.TFRFetcher;
import com.ds.avare.place.Area;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Pan;
import com.ds.avare.shapes.Draw;
import com.ds.avare.shapes.ElevationTile;
import com.ds.avare.shapes.PixelDraw;
import com.ds.avare.shapes.Radar;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.Tile;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.storage.DataSource;
import com.ds.avare.userDefinedWaypoints.UDWMgr;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.InfoLines;
import com.ds.avare.utils.Mutex;
import com.ds.avare.utils.NavComments;
import com.ds.avare.utils.ShadowedText;
import com.ds.avare.weather.AdsbWeatherCache;
import com.ds.avare.weather.InternetWeatherCache;

import android.app.Service;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.IBinder;

import java.net.URI;

/**
 * @author zkhan
 * Main storage service. It stores all states so when activity dies,
 * we dont start from no state.
 * This is especially important for start up functions that take time,
 * one of which is database un-zipping.
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

    // Draw for Map
    private Draw mDraw;
    
    // Write for plates
    private PixelDraw mPixelDraw;
    
    private InternetWeatherCache mInternetWeatherCache;
    
    private AdsbWeatherCache mAdsbWeatherCache;
    
    private TrafficCache mTrafficCache;
    
    private Radar mRadar;
    
    private String mLastPlateAirport;
    private int mLastPlateIndex;
    
	/*
     * Last location and its sem for sending NMEA to the world
     */
    private Mutex mLocationSem;
    private Location mLocation;
    
    private boolean mDownloading;
    
    private LinkedList<Checklist> mCheckLists;
    
    /**
     * GPS
     */
    private Gps mGps;

    /**
     * Store this
     */
    private Pan mPan;
    
    /*
     * A/FD showing
     */
    private String mLastAfdAirport;
    private Destination mLastAfdDestination;
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

    private boolean mIsGpsOn;
    
    private int mCounter;
    
    private TileMap mTiles;
    
    private ElevationTile mElevTile;
    
    // Handler for the top two lines of status information
    private InfoLines mInfoLines;

    // Navigation comments from flight plans
    private NavComments mNavComments;
    
    // Handler for drawing text with an oval shadow
    private ShadowedText mShadowedText;
    
    /*
     * Curret ground elevation
     */
    private double mElev;
    private double mThreshold;
    
    /*
     * Hobbs time
     */
    private FlightTimer  mFlightTimer;

    /*
     * Declare our KML position tracker
     * For writing plots to a KML file
     */
    private KMLRecorder mKMLRecorder;

    private Odometer mOdometer;
    
    // The Course Deviation Indicator
    private CDI mCDI;
    
    // The vertical approach slope indicator
    private VNAV mVNAV;
    
    // Vertical speed indicator
    private VSI mVSI;
    
    // User defined points of interest
    private UDWMgr mUDWMgr;

    // Distance ring instrument
    private DistanceRings mDistanceRings;
    
    private DrawCapLines mCap;
    

    private ExternalPlanMgr mExternalPlanMgr;
    
    /*
     * Watches GPS to notify of phases of flight
     */
    private FlightStatus mFlightStatus;
   
    /*
     * Current checklist
     */
    private Checklist mChecklist;
    
    // The edge distance tape instrument
    private EdgeDistanceTape mEdgeDistanceTape;
    
    // Timer for switching fuel tanks
    private FuelTimer mFuelTimer;
    
    
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
        
        mArea = new Area(mImageDataSource, this);
        mPlan = new Plan(this, this);
        mDownloading = false;
        
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
        mAfdIndex = 0;
        mTrafficCache = new TrafficCache();
        mLocationSem = new Mutex();
        mAdsbWeatherCache = new AdsbWeatherCache(getApplicationContext());
        mLastPlateAirport = null;
        mLastPlateIndex = 0;
        mElevTile = new ElevationTile(getApplicationContext());
        mCheckLists = null;
        
        mCap = new DrawCapLines(this, getApplicationContext(), getResources().getDimension(R.dimen.distanceRingNumberTextSize));
        
        mInfoLines = new InfoLines(this);

        mShadowedText = new ShadowedText(getApplicationContext());
        
        mDraw = new Draw();
        mPixelDraw = new PixelDraw();
        
        mElev = -1;
        mThreshold = 0;
        
        mChecklist = new Checklist("");
        
        /*
         * Allocate a flight timer object
         */
        mFlightTimer = new FlightTimer();

        /*
         * Start up the KML recorder feature
         */
        mKMLRecorder = new KMLRecorder();
        
        /*
         * Internet radar
         */
        mRadar = new Radar(getApplicationContext());

        mRadar.parse();

        /*
         * Start the odometer now
         */
        mOdometer = new Odometer();

        // Allocate the Course Deviation Indicator
        mCDI = new CDI();

        // Allocate the VNAV
        mVNAV = new VNAV();
        
        // Allocate the VSI
        mVSI = new VSI();
        
        // Allocate a handler for PointsOfInterest
        mUDWMgr = new UDWMgr(this, getApplicationContext()); 
      
        // Allocate a new DistanceRing instrument
        mDistanceRings = new DistanceRings(this, getApplicationContext(), getResources().getDimension(R.dimen.distanceRingNumberTextSize));
        
        mFlightStatus = new FlightStatus(mGpsParams);
        
        // For handling external flight plans
        mExternalPlanMgr = new ExternalPlanMgr(this, getApplicationContext());

        // Allocate the nav comments object
        mNavComments = new NavComments();
        
        mEdgeDistanceTape = new EdgeDistanceTape();
        
        // Declare a fuel tank switching timer. Default to 30
        // minutes per tank
        mFuelTimer = new FuelTimer(30);	
        
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
                
                if(mDownloading) {
                    /**
                     * Download runs the tasks, so dont do it since we are
                     * updating files. This flag is set by Download activity.
                     */
                    return;
                }
                
                LinkedList<GpsInterface> list = extracted();
                Iterator<GpsInterface> it = list.iterator();
                while (it.hasNext()) {
                    GpsInterface infc = it.next();
                    infc.locationCallback(location);
                }
                
                /*
                 * Update the service objects with location
                 */
                if(null != location) {
                    if(!location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                        /*
                         * Getting location from somewhere other than built in GPS.
                         * Update timeout so we do not timeout on GPS timer.
                         */
                        mGps.updateTimeout();
                    }
                    setGpsParams(new GpsParams(location));
                    mLocation = location;
                    mLocationSem.unlock();
                    mArea.updateLocation(getGpsParams());
                    mPlan.updateLocation(getGpsParams());

                    // Adjust the flight timer
                    getFlightTimer().setSpeed(mGpsParams.getSpeed());
                    
                    // Tell the KML recorder a new point to potentially plot
                    getKMLRecorder().setGpsParams(mGpsParams);
                    
                    // Let the odometer know how far we traveled
                    getOdometer().updateValue(mGpsParams);
                    
                    // Vertical descent rate calculation
                    getVNAV().calcGlideSlope(mGpsParams, mDestination);
                    
                    // Tell the VSI where we are.
                    getVSI().updateValue(mGpsParams);
                    
                    getFlightStatus().updateLocation(mGpsParams);
                    
                    if(mPlan.hasDestinationChanged()) {
                        /*
                         * If plan active then set destination to next not passed way point
                         */
                        setDestinationPlanNoChange(mPlan.getDestination(mPlan.findNextNotPassed()));
                    }

                    if(mDestination != null) {
                        mDestination.updateTo(getGpsParams());
                    }
                    
                    // Calculate course line deviation - this must be AFTER the destination update
                    // since the CDI uses the destination in its calculations
                    getCDI().calcDeviation(mDestination, getPlan());
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
        mElevTile.recycleBitmaps();
        
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
    
    /**
     * 
     */
    public InfoLines getInfoLines() {
       return mInfoLines; 
    }
    
    /**
     * 
     * @return
     */
    public TileMap getTiles() {
        return mTiles;
    }
    
    /*
     * Get/Set (state), Get (resources, state) functions for activity
     */

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
        mAfdIndex = 0;

        // A direct destination implies a new plan. Ensure to turn off
        // the plan
        if(null != mPlan){
        	mPlan.makeInactive();
        }
    }

    /**
     * @param destination from plan
     */
    public void setDestinationPlanNoChange(Destination destination) {
        mDestination = destination;
        mAfdIndex = 0;
        
        // Update the right side of the nav comments from the destination
        // TODO: I don't like this here, it should be pushed into the PLAN itself
        if(null != destination) {
        	mNavComments.setRight(destination.getCmt());
        }
    }

    /**
     * 
     * @return
     */
    public Destination getLastAfdDestination() {
        return mLastAfdDestination;
    }
    
    /**
     * 
     * @param destination
     */
    public void setLastAfdDestination(Destination destination) {
        mLastAfdDestination = destination;
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


    public String getLastAfdAirport() {
        return mLastAfdAirport;
    }
    public void setLastAfdAirport(String airport) {
        mLastAfdAirport = airport;
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
        mPlan = new Plan(this, this);
    }

    /**
     * @return
     */
    public void newPlanFromStorage(String storage, boolean reverse) {
        mPlan = new Plan(this, this, storage, reverse);
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
     * Get points to draw
     * @return
     */
    public PixelDraw getPixelDraw() {
        return mPixelDraw;
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
     * @param shouldTrack enable/disable tracking
     * @return URI of the file that was just closed, or null if it was just opened
     */
    public URI setTracks(boolean shouldTrack) {
        if(shouldTrack) {
            mKMLRecorder.start();
            return null;
        }
        else {
            URI fileURI = mKMLRecorder.stop();

            // If a file was created, then tell the media scanner about it
            if(null != fileURI) {
            	MediaScannerConnection.scanFile(getApplicationContext(), new String[] { fileURI.getPath() }, null, null);
            }
			return fileURI;
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

    public Odometer getOdometer() {
    	return mOdometer;
    }
    
    public CDI getCDI() {
    	return mCDI;
    }

    public VNAV getVNAV() {
    	return mVNAV;
    }
    
    public VSI getVSI() {
    	return mVSI;
    }
    
    public FlightStatus getFlightStatus() {
        return mFlightStatus;
    }
    
    /**
     * 
     * @return
     */
    public Gps getGps() {
        return mGps;
    }
    
    /**
     * 
     */
    public Location getLocationBlocking() {
        try {
            mLocationSem.lock();
        } catch (Exception e) {
        }
        return mLocation;
    }

    /**
     * 
     * @return
     */
    public AdsbWeatherCache getAdsbWeather() {
       return mAdsbWeatherCache; 
    }
    
    /**
     * 
     * @return
     */
    public TrafficCache getTrafficCache() {
       return mTrafficCache; 
    }

    
    /**
     * 
     * @return
     */
    public Radar getRadar() {
       return mRadar; 
    }
    
    /**
     * 
     */
    public void deleteTFRFetcher() {
        mTFRFetcher = new TFRFetcher(getApplicationContext());

    }
 
    /**
     * 
     */
    public void deleteInternetWeatherCache() {
        mInternetWeatherCache = new InternetWeatherCache();        
    }
    
    /**
     * 
     */
    public void deleteRadar() {
        mRadar.flush();        
    }
    
    /**
     * 
     */
    public String getLastPlateAirport() {
        return mLastPlateAirport;
    }
    
    /*
     * 
     */
    public void setLastPlateAirport(String airport) {
        mLastPlateAirport = airport;
    }
    
    /**
     * 
     * @param index
     */
    public void setLastPlateIndex(int index) {
        mLastPlateIndex = index;
    }
    
    /**
     * 
     * @return
     */
    public int getLastPlateIndex() {
        return mLastPlateIndex;
    }
 
    /**
     * 
     * @param t
     */
    public void setElevationTile(Tile t) {
        mElevTile.setElevationTile(t);
    }

    /**
     * 
     * @return
     */
    public BitmapHolder getElevationBitmap() {
        return mElevTile.getElevationBitmap();
    }
    
    /**
     * 
     */
    public void setDownloading(boolean state) {
       mDownloading = state; 
    }
    
    /**
     * 
     * @return
     */
    public LinkedList<Checklist> getCheckLists() {
        return mCheckLists;
    }
    
    /**
     * 
     * @param list
     */
    public void setCheckLists(LinkedList<Checklist> list) {
        mCheckLists = list;
    }
    
    /**
     * 
     * @return
     */
    public double getElevation() {
       return mElev; 
    }
    
    /**
     * 
     * @return
     */
    public void setElevation(double elev) {
       mElev = elev; 
    }
    
    /**
     * 
     * @return
     */
    public double getThreshold() {
       return mThreshold; 
    }
    
    /**
     * 
     * @return
     */
    public void setThreshold(double thr) {
       mThreshold = thr; 
    }

    public ShadowedText getShadowedText() {
    	return mShadowedText;
    }
    
    public UDWMgr getUDWMgr() {
    	return mUDWMgr;
    }
    
    public DistanceRings getDistanceRings() {
    	return mDistanceRings;
    }
    
    public ExternalPlanMgr getExternalPlanMgr() {
    	return mExternalPlanMgr;
    }
    
    public NavComments getNavComments() {
    	return mNavComments;
    }
    
    public Checklist getChecklist() {
    	return mChecklist;
    }
    
    public void setChecklist(Checklist cl) {
    	mChecklist = cl;
    }

    public EdgeDistanceTape getEdgeTape() {
    	return mEdgeDistanceTape;
    }
    
    public FuelTimer getFuelTimer() {
    	return mFuelTimer;
    }

	public DrawCapLines getCap() {
		return mCap;
	}
}
