/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;

import com.ds.avare.adsb.TfrCache;
import com.ds.avare.adsb.TrafficCache;
import com.ds.avare.cap.DrawCapLines;
import com.ds.avare.externalFlightPlan.ExternalPlanMgr;
import com.ds.avare.flight.FlightStatus;
import com.ds.avare.flightLog.KMLRecorder;
import com.ds.avare.gps.ExtendedGpsParams;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.AutoPilot;
import com.ds.avare.instruments.CDI;
import com.ds.avare.instruments.DistanceRings;
import com.ds.avare.instruments.EdgeDistanceTape;
import com.ds.avare.instruments.FlightTimer;
import com.ds.avare.instruments.FuelTimer;
import com.ds.avare.instruments.GlideProfile;
import com.ds.avare.instruments.Odometer;
import com.ds.avare.instruments.UpTimer;
import com.ds.avare.instruments.VNAV;
import com.ds.avare.instruments.VSI;
import com.ds.avare.network.ShapeFetcher;
import com.ds.avare.network.TFRFetcher;
import com.ds.avare.orientation.OrientationInterface;
import com.ds.avare.place.Area;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Favorites;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Plan;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Pan;
import com.ds.avare.shapes.Draw;
import com.ds.avare.shapes.MetarLayer;
import com.ds.avare.shapes.PixelDraw;
import com.ds.avare.shapes.RadarLayer;
import com.ds.avare.shapes.ShapeFileShape;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.content.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.userDefinedWaypoints.UDWMgr;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines;
import com.ds.avare.utils.Mutex;
import com.ds.avare.utils.NavComments;
import com.ds.avare.utils.ShadowedText;
import com.ds.avare.utils.TimeConstants;
import com.ds.avare.weather.AdsbWeatherCache;
import com.ds.avare.weather.InternetWeatherCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

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
public class StorageService  {

    /**
     * The Sqlite database
     */
    private DataSource mDataSource;
    /**
     * Store this
     */
    private Destination mDestination;
    /**
     * Store this
     */
    private GpsParams mGpsParams;
    private ExtendedGpsParams mGpsParamsExtended;

    /**
     * Store this
     */
    private Movement mMovement;

    private JSONObject mGeoAltitude = null;

    public static final int MIN_ALTITUDE = -1000;

    // Draw for Map
    private Draw mDraw;
    
    // Write for plates
    private PixelDraw mPixelDraw;
    
    private InternetWeatherCache mInternetWeatherCache;
    
    private AdsbWeatherCache mAdsbWeatherCache;

    private TfrCache mAdsbTfrCache;
    
    private TrafficCache mTrafficCache;
    
    private RadarLayer mRadarLayer;
    
    private String mLastPlateAirport;
    private int mLastPlateIndex;
    private float[] mMatrix;

	/*
     * Last location and its sem for sending NMEA to the world
     */
    private Mutex mLocationSem;
    private Location mLocation;
    
    private boolean mDownloading;

    String mOverrideListName;

    private MetarLayer mMetarLayer;


    /**
     * GPS
     */
    private Gps mGps;

    /**
     * Store this
     */
    private Pan mPan;
    
    /*
     * Chart Supplement showing
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

    private ShapeFetcher mShapeFetcher;

    /*
     * A list of GPS listeners
     */
    private LinkedList<GpsInterface> mGpsCallbacks;

    /*
 * A list of GPS listeners
 */
    private LinkedList<OrientationInterface> mOrientationCallbacks;

    /*
     * A diagram bitmap
     */
    private BitmapHolder mAfdDiagramBitmap;

    /*
     * A
     */
    private BitmapHolder mPlateDiagramBitmap;

    private int mCounter;

    private TileMap mTiles;
    
    // Handler for the top two lines of status information
    private InfoLines mInfoLines;

    // Navigation comments from flight plans
    private NavComments mNavComments;
    
    // Handler for drawing text with an oval shadow
    private ShadowedText mShadowedText;
    OrientationInterface mOrientationInterface;

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
    private GlideProfile mGlideProfile;

    private DrawCapLines mCap;

    private ExternalPlanMgr mExternalPlanMgr;

    /*
     * Watches GPS to notify of phases of flight
     */
    private FlightStatus mFlightStatus;
   

    // The edge distance tape instrument
    private EdgeDistanceTape mEdgeDistanceTape;

    // Timer for switching fuel tanks
    private FuelTimer mFuelTimer;

    // Timer for count up
    private UpTimer mUpTimer;

    Favorites mFavorites;

    LinkedList<LabelCoordinate> mGameTfrLabels;

    private int mIcaoAddress;

    // Last time location was updated
    private long mLastLocationUpdate;

    public String getOverrideListName() {
        return mOverrideListName;
    }

    public void setOverrideListName(String overrideListName) {
        mOverrideListName = overrideListName;
    }



    /* (non-Javadoc)
     */
    public void create() {

        mDataSource = new DataSource();
        
        mArea = new Area();
        mPlan = new Plan();
        mDownloading = false;
        
        /*
         * All tiles
         */
        mTiles = new TileMap();

        mInternetWeatherCache = new InternetWeatherCache();
        mInternetWeatherCache.parse();
        mTFRFetcher = new TFRFetcher();
        mTFRFetcher.parse();
        mShapeFetcher = new ShapeFetcher();
        mShapeFetcher.parse();
        mGpsParamsExtended = new ExtendedGpsParams();

        mGpsCallbacks = new LinkedList<GpsInterface>();
        mOrientationCallbacks = new LinkedList<OrientationInterface>();
        mAfdDiagramBitmap = null;
        mPlateDiagramBitmap = null;
        mIcaoAddress = 0;
        mAfdIndex = mPref.isDefaultAFDImage() ? 1 : 0;
        mOverrideListName = null;
        mTrafficCache = new TrafficCache();
        mLocationSem = new Mutex();
        mAdsbWeatherCache = new AdsbWeatherCache();
        mAdsbTfrCache = new TfrCache();
        mLastPlateAirport = null;
        mLastPlateIndex = 0;
        mLastLocationUpdate = 0;
        mGpsParams = new GpsParams(null);

        mCap = new DrawCapLines(Helper.adjustTextSize(mContext, R.dimen.distanceRingNumberTextSize));
        
        mInfoLines = new InfoLines();

        mShadowedText = null;

        mDraw = new Draw();
        mPixelDraw = new PixelDraw();

        /*
         * Allocate a flight timer object
         */
        mFlightTimer = new FlightTimer();

        /*
         * Start up the KML recorder feature
         */
        mKMLRecorder = new KMLRecorder();
        
        /*
         * Internet nexrad
         */
        mRadarLayer = new RadarLayer();

        /*
         * Internet metar
         */
        mMetarLayer = new MetarLayer();

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
        mUDWMgr = new UDWMgr();
      
        // Allocate a new DistanceRing instrument
        mDistanceRings = new DistanceRings(Helper.adjustTextSize(mContext, R.dimen.distanceRingNumberTextSize));

        mGlideProfile = new GlideProfile(Helper.adjustTextSize(mContext, R.dimen.distanceRingNumberTextSize));
        
        mFlightStatus = new FlightStatus(mGpsParams);
        
        // For handling external flight plans
        mExternalPlanMgr = new ExternalPlanMgr();

        // Allocate the nav comments object
        mNavComments = new NavComments();
        
        mEdgeDistanceTape = new EdgeDistanceTape();
        
        // Declare a fuel tank switching timer. Default to 30
        // minutes per tank
        mFuelTimer = new FuelTimer();
        mUpTimer = new UpTimer();


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

                long diff = System.currentTimeMillis() - mLastLocationUpdate;
                // Do not overwhelm as GPS can send a lot of position updates per second
                if(diff < TimeConstants.ONE_SECOND) {
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
                    mGpsParamsExtended.setParams(mGpsParams);

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

                    getGlideProfile().updateGlide(mGpsParams);
                    
                    // Vertical descent rate calculation
                    getVNAV().calcGlideSlope(mGpsParams, mDestination);
                    
                    // Tell the VSI where we are.
                    getVSI().updateValue(mGpsParams);
                    
                    getFlightStatus().updateLocation(mGpsParams);

                    mFavorites.update(StorageService.this);
                    
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
                    getCDI().calcDeviation(mDestination, mPlan);

                    mLastLocationUpdate = System.currentTimeMillis();
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
        mGps = new Gps(intf);

        /*
         * Start orientation
         */
        mOrientationInterface = new OrientationInterface() {

            /**
             *
             * @return
             */
            private LinkedList<OrientationInterface> extracted() {
                return (LinkedList<OrientationInterface>)mOrientationCallbacks.clone();
            }

            @Override
            public void onSensorChanged(double yaw, double pitch, double roll, double slip, double acceleration, double yawrate, double aoa, double airspeed, double altitude, double vsi) {
                LinkedList<OrientationInterface> list = extracted();
                Iterator<OrientationInterface> it = list.iterator();
                while (it.hasNext()) {
                    OrientationInterface infc = it.next();
                    infc.onSensorChanged(yaw, pitch, roll, slip, acceleration, yawrate, aoa, airspeed, altitude, vsi);
                }
            }
        };

        mFavorites = new Favorites(this);

    }

    private static StorageService mService = null;
    private Context mContext = null;
    private static Preferences mPref;

    private StorageService() {
    }

    /**
     * Call once only
     * @param c
     */
    public void setContext(Context c) {
        mContext = c;
        mPref = new Preferences(c);
        mService.create();
    }

    public static StorageService getInstance() {
        if(null == mService) {
            mService = new StorageService();
            return mService;
        }
        return mService;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    public void destroy() {
        mPref.setLastLocation(mGpsParams.getLongitude(), mGpsParams.getLatitude());

        /*
         * If we ever exit, reclaim memory
         */
        mTiles.recycleBitmaps();

        if(null != mAfdDiagramBitmap) {
            mAfdDiagramBitmap.recycle();
            mAfdDiagramBitmap = null;
        }
        if(null != mPlateDiagramBitmap) {
            mPlateDiagramBitmap.recycle();
            mPlateDiagramBitmap = null;
        }
        mTiles = null;
        
        System.gc();
        
        if(mGps != null) {
            mGps.stop();
        }

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


    public Context getApplicationContext() {
        return mContext;
    }

    /**
     *
     * @return
     */
    public TfrCache getAdsbTfrCache() {
        return mAdsbTfrCache;
    }

    /**
     * @return
     */
    public ArrayList<ShapeFileShape> getShapeShapes() {
        return mShapeFetcher.getShapes();
    }

    /**
     * @return
     */
    public LinkedList<TFRShape> getTFRShapes() {
        return mTFRFetcher.getShapes();
    }

    public LinkedList<TFRShape> getAdsbTFRShapes() {
        return mAdsbTfrCache.getShapes();
    }

    public LinkedList<LabelCoordinate> getGameTfrLabels() {
        return mGameTfrLabels;
    }

    /**
     * @return
     */
    public DataSource getDBResource() {
        return mDataSource;
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
        mAfdIndex = mPref.isDefaultAFDImage() ? 1 : 0;

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
        mAfdIndex = mPref.isDefaultAFDImage() ? 1 : 0;
        
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

    public Favorites getFavorites() {
        return mFavorites;
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
     * @return
     */
    public ExtendedGpsParams getExtendedGpsParams() {
        return mGpsParamsExtended;
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
     * @param p
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
     * @return
     */
    public void newPlanFromStorage(String storage, boolean reverse) {
        mPlan = new Plan(storage, reverse);
    }

    /**
     * 
     * @param name
     */
    public void loadAfdDiagram(String name) {
        if(mAfdDiagramBitmap != null) {
            /*
             * Clean old one first
             */
            mAfdDiagramBitmap.recycle();
            mAfdDiagramBitmap = null;
            System.gc();
        }
        if(null != name) {
            mAfdDiagramBitmap = new BitmapHolder(name);
        }
    }

    /**
     *
     * @param name
     */
    public void loadPlateDiagram(String name) {
        if(mPlateDiagramBitmap != null) {
            /*
             * Clean old one first
             */
            mPlateDiagramBitmap.recycle();
            mPlateDiagramBitmap = null;
            System.gc();
        }
        if(null != name) {
            mPlateDiagramBitmap = new BitmapHolder(name);
        }
    }


    /**
     *
     * @return
     */
    public MetarLayer getMetarLayer() {
        return mMetarLayer;
    }

    /**
     * 
     * @return
     */
    public BitmapHolder getPlateDiagram() {
       return mPlateDiagramBitmap;
    }

    /**
     *
     * @return
     */
    public BitmapHolder getAfdDiagram() {
        return mAfdDiagramBitmap;
    }

    /**
     *
     * @return
     */
    public float[] getMatrix() {
        return mMatrix;
    }

    /**
     *
     * @return
     */
    public void setMatrix(float[] matrix) {
        mMatrix = matrix;
    }

    public Preferences getPreferences() {
        return mPref;
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
        synchronized(mGpsCallbacks) {
            mGpsCallbacks.add(gps);
        }
    }

    /**
     * 
     * @param gps
     */
    public void unregisterGpsListener(GpsInterface gps) {

        synchronized(mGpsCallbacks) {
            mGpsCallbacks.remove(gps);
        }
        
    }

    /**
     *
     * @param o
     */
    public boolean registerOrientationListener(OrientationInterface o) {
        /*
         * If first listener, start orientation
         */
        synchronized(mOrientationCallbacks) {
            mOrientationCallbacks.add(o);
        }
        return true;
    }

    /**
     *
     * @param o
     */
    public void unregisterOrientationListener(OrientationInterface o) {

        synchronized(mOrientationCallbacks) {
            mOrientationCallbacks.remove(o);
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
    public RadarLayer getRadarLayer() {
       return mRadarLayer;
    }

    public int getIcaoAddress() {
        return mIcaoAddress;
    }
    
    /**
     * 
     */
    public void deleteTFRFetcher() {
        mTFRFetcher = new TFRFetcher();
    }

    /**
     *
     */
    public void deleteShapeFetcher() {
        mShapeFetcher = new ShapeFetcher();
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
        mRadarLayer.flush();
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
     */
    public void setDownloading(boolean state) {
       mDownloading = state; 
    }
    
    public ShadowedText getShadowedText() {
        if (mShadowedText==null) {
            mShadowedText = new ShadowedText();
            mShadowedText = new ShadowedText();
        }
    	return mShadowedText;
    }
    
    public UDWMgr getUDWMgr() {
    	return mUDWMgr;
    }
    
    public DistanceRings getDistanceRings() {
    	return mDistanceRings;
    }

    public GlideProfile getGlideProfile() {
        return mGlideProfile;
    }

    public ExternalPlanMgr getExternalPlanMgr() {
    	return mExternalPlanMgr;
    }
    
    public NavComments getNavComments() {
    	return mNavComments;
    }

    public EdgeDistanceTape getEdgeTape() {
    	return mEdgeDistanceTape;
    }

    public FuelTimer getFuelTimer() {
        return mFuelTimer;
    }
    public UpTimer getUpTimer() {
        return mUpTimer;
    }

	public DrawCapLines getCap() {
		return mCap;
	}


    /**
     * Receive data for weather / traffic etc
     * @return
     */
    public void getDataFromIO(String text) {

        if(text == null) {
            return;
        }

            /*
             * Get JSON
             */
        try {
            JSONObject object = new JSONObject(text);

            String type = object.getString("type");
            if(type == null) {
                return;
            }
            else if(type.equals("traffic")) {
                getTrafficCache().putTraffic(
                        object.getString("callsign"),
                        object.getInt("address"),
                        object.optBoolean("isairborne", true),
                        (float)object.getDouble("latitude"),
                        (float)object.getDouble("longitude"),
                        object.getInt("altitude"),
                        (float)object.getDouble("bearing"),
                        (int)object.getInt("speed"),
                        Helper.getMillisGMT()
                            /*XXX:object.getLong("time")*/);
            }
            else if(type.equals("geoaltitude")) {
                mGeoAltitude = object;
            }
            else if(type.equals("ahrs")) {
                double yaw = object.getDouble("yaw");
                double pitch = object.getDouble("pitch");
                double roll = object.getDouble("roll");
                double slip = object.getDouble("slip");
                double yawrate = object.getDouble("yawrate");
                double acceleration = object.getDouble("acceleration");
                double aoa = object.getDouble("aoa");
                double airspeed = object.getDouble("airspeed");
                double altitude = object.getDouble("altitude");
                double vsi = object.getDouble("vsi");
                mOrientationInterface.onSensorChanged(yaw, pitch, roll, slip, acceleration, yawrate, aoa, airspeed, altitude, vsi);
            }
            else if(type.equals("ownship")) {
                Location l = new Location(LocationManager.GPS_PROVIDER);
                l.setLongitude(object.getDouble("longitude"));
                l.setLatitude(object.getDouble("latitude"));
                l.setSpeed((float) object.getDouble("speed"));
                l.setBearing((float) object.getDouble("bearing"));
                l.setTime(object.getLong("time"));
                try {
                    mIcaoAddress = object.getInt("address");
                }
                catch (Exception e) {
                    mIcaoAddress = mPref.getAircraftICAOCode(); // sometimes ownship message will be from external GPS, and will not have ICAO, use the user set one
                }
                // Choose most appropriate altitude. This is because people fly all sorts
                // of equipment with or without altitudes
                // convert all altitudes in feet
                double pressureAltitude = object.getDouble("altitude") * Preferences.heightConversion;
                double deviceAltitude = MIN_ALTITUDE;
                double geoAltitude = MIN_ALTITUDE;
                // If geo altitude from adsb available, use it if not too old
                if(mGeoAltitude != null) {
                    long t1 = object.getLong("time");
                    long t2 = mGeoAltitude.getLong("time");
                    if((t1 - t2) < 10000) { // 10 seconds
                        geoAltitude = mGeoAltitude.getDouble("altitude") * Preferences.heightConversion;
                        if(geoAltitude < MIN_ALTITUDE) {
                            geoAltitude = MIN_ALTITUDE;
                        }
                    }
                }
                // If geo altitude from device available, use it if not too old
                if(getGpsParams() != null) {
                    long t1 = System.currentTimeMillis();
                    long t2 = getGpsParams().getTime();
                    if ((t1 - t2) < 10000) { // 10 seconds
                        deviceAltitude = getGpsParams().getAltitude();
                        if(deviceAltitude < MIN_ALTITUDE) {
                            deviceAltitude = MIN_ALTITUDE;
                        }
                    }
                }

                // choose best altitude. give preference to pressure altitude because that is
                // the most correct for traffic purpose.
                double alt = pressureAltitude;
                if(alt <= MIN_ALTITUDE) {
                    alt = geoAltitude;
                }
                if(alt <= MIN_ALTITUDE) {
                    alt = deviceAltitude;
                }
                if(alt <= MIN_ALTITUDE) {
                    alt = MIN_ALTITUDE;
                }

                // set pressure altitude for traffic alerts
                getTrafficCache().setOwnAltitude((int) alt);
                // set own location for proximity traffic alerts
                getTrafficCache().setOwnLocation(l);
                // set own airborne status
                getTrafficCache().setOwnIsAirborne(object.optBoolean("isairborne", true));

                // For own height prefer geo altitude, do not use deviceAltitude here because
                // we could get into rising altitude condition through feedback
                alt = geoAltitude;
                if(alt <= MIN_ALTITUDE) {
                    alt = pressureAltitude;
                }
                if(alt <= MIN_ALTITUDE) {
                    alt = MIN_ALTITUDE;
                }
                l.setAltitude(alt / Preferences.heightConversion);
                getGps().onLocationChanged(l, type);
            }
            else if(type.equals("nexrad")) {

                    /*
                     * XXX: If we are getting this from station, it must be current, fix this.
                     */
                long time = Helper.getMillisGMT();//object.getLong("time");
                int cols = object.getInt("x");
                int rows = object.getInt("y");
                int block = object.getInt("blocknumber");
                boolean conus = object.getBoolean("conus");
                JSONArray emptyArray = object.getJSONArray("empty");
                JSONArray dataArray = object.getJSONArray("data");

                if(emptyArray == null || dataArray == null) {
                    return;
                }
                int empty[] = new int[emptyArray.length()];
                for(int i = 0; i < empty.length; i++) {
                    empty[i] = emptyArray.getInt(i);
                }
                int data[] = new int[dataArray.length()];
                for(int i = 0; i < data.length; i++) {
                    data[i] = dataArray.getInt(i);
                }

                    /*
                     * Put in nexrad.
                     */
                getAdsbWeather().putImg(
                        time, block, empty, conus, data, cols, rows);
            }
            else if(type.equals("sua")) {
                getAdsbWeather().putSua(
                        Helper.getMillisGMT(),
                        object.getString("text"));
            }
            else if(type.equals("airmet") || type.equals("sigmet")) {
                getAdsbWeather().putAirSigMet(
                        Helper.getMillisGMT(),
                        object.getString("number"),
                        object.getString("shape"),
                        object.getString("data"),
                        object.getString("text"),
                        object.getString("startTime"),
                        object.getString("endTime")
                );
            }
            else if(type.equals("notam")) {
                getAdsbTfrCache().putTfr(
                        Helper.getMillisGMT(),
                        object.getString("number"),
                        object.getString("shape"),
                        object.getString("data"),
                        object.getString("text"),
                        object.getString("startTime"),
                        object.getString("endTime"));
            }
            else if(type.equals("METAR") || type.equals("SPECI")) {
                    /*
                     * Put METAR
                     */
                getAdsbWeather().putMetar(object.getLong("time"),
                        object.getString("location"), object.getString("data"), object.getString("flight_category"));
            }
            else if(type.equals("TAF") || type.equals("TAF.AMD")) {
                getAdsbWeather().putTaf(object.getLong("time"),
                        object.getString("location"), object.getString("data"));
            }
            else if(type.equals("WINDS")) {
                getAdsbWeather().putWinds(object.getLong("time"),
                        object.getString("location"), object.getString("data"));
            }
            else if(type.equals("PIREP")) {
                getAdsbWeather().putAirep(object.getLong("time"),
                        object.getString("location"), object.getString("data"),
                        getDBResource());
            }

        } catch (JSONException e) {
            return;
        }

    }

    /**
     * data for autopilot
     * @return String - the nmea sentences for the autopliot in a serialized JSON string
     */
    public String makeDataForIO() {
        GpsParams gpsParams = new GpsParams(getLocationBlocking()); // Where we currently are
        Destination dest    = getDestination();                     // Where we are going
        Plan plan           = getPlan();                            // The flight plan
        JSONObject object   = new JSONObject();                     // An object to build

        try {
            object.put("type", "nmeanav");
            object.put("apsentences", AutoPilot.apCreateSentences(gpsParams, plan, dest));
        } catch (JSONException ignore) {
            return null;
        }
        return object.toString();
    }

}
