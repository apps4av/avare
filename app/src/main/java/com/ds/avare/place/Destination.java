/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Projection;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.DataBaseHelper;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.userDefinedWaypoints.UDWMgr;
import com.ds.avare.userDefinedWaypoints.Waypoint;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.TwilightCalculator;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

/**
 * @author zkhan, jlmcgraw
 * Stores everything about destination, its name (ICAO code)
 * Does databse query to find the destination as well.
 */
public class Destination extends Observable {

    /**
     * 
     */
    private String mName;
    /**
     * Cache it for database query from async task
     */
    private DataSource mDataSource;
    /**
     * 
     */
    private double mDistance;
    /**
     * 
     */
    private double mBearing;
    /**
     * If a destination is found?
     */
    private boolean mFound;
    /**
     * ETE to destination
     * ETA at destination
     */
    private String mEte;
    private String mEta;

    /*
     * Track to dest.
     */
    TrackShape mTrackShape;
        
    /*
     * Its lon/lat
     */
    private double mLond;
    private double mLatd;

    private String mAfdFound[];
    
    private Preferences mPref;
    
    private StorageService mService;
    
    private boolean mLooking;
    private boolean mInited;
    
    private double mDeclination;

    /*
     * This is where destination was set.
     */
    private double mLonInit;
    private double mLatInit;
    
    private String mDestType;
    private String mDbType;
    private String mCmt;
    private LinkedList<Runway> mRunways;
    private LinkedHashMap <String, String>mFreq;
    private LinkedList<Awos> mAwos;
    
    public static final String GPS = "GPS";
    public static final String MAPS = "Maps";
    public static final String BASE = "Base";
    public static final String FIX = "Fix";
    public static final String NAVAID = "Navaid";
    public static final String AD = "AIRPORT-DIAGRAM";
    public static final String UDW = "UDW";
    
    /**
     * Contains all info in a hash map for the destination
     * Dozens of parameters in a linked map because simple map would rearrange the importance
     */
    private LinkedHashMap <String, String>mParams;
    
    public String getCmt() {
    	return mCmt;
    }
    
	/**
	 * @param name
	 * @param DataSource
	 */
	public Destination(String name, String type, Preferences pref, StorageService service) {
	    GpsParams params = service.getGpsParams();
	    mInited = false;
	    if(null != params) {
    	    mLonInit = params.getLongitude();
            mLatInit = params.getLatitude();
            mInited = true;
	    }
        mDbType = "";
        mFound = mLooking = false;
        mRunways = new LinkedList<Runway>();
        mService = service;
        mDataSource = mService.getDBResource(); 
        mTrackShape = new TrackShape();
        mPref = pref;
        mEte = new String("--:--");
        mEta = new String("--:--");
        mParams = new LinkedHashMap<String, String>();
        mFreq = new LinkedHashMap<String, String>();
        mAwos = new LinkedList<Awos> ();
        mAfdFound = null;
	    mName = name.toUpperCase(Locale.getDefault());
	    mDestType = type;
    	mLond = mLatd = 0;
	}

    /**
     * Simple GPS destination. No db query required
     */
    public Destination(StorageService service, double lon, double lat) {
        GpsParams params = service.getGpsParams();
        mPref = new Preferences(service.getApplicationContext());
        if(null != params) {
            mLonInit = params.getLongitude();
            mLatInit = params.getLatitude();
        }
        else {
            mLonInit = lon;
            mLatInit = lat;            
        }
        mInited = true;
        mService = service;
        mDbType = GPS;
        mFound = true;
        mLooking = false;
        mRunways = new LinkedList<Runway>();
        mTrackShape = new TrackShape();
        mEte = new String("--:--");
        mEta = new String("--:--");
        mLond = lon;
        mLatd = lat;
        mParams = new LinkedHashMap<String, String>();
        mFreq = new LinkedHashMap<String, String>();
        mAwos = new LinkedList<Awos> ();
        mParams.put(DataBaseHelper.LONGITUDE, "" + mLond);
        mParams.put(DataBaseHelper.LATITUDE, "" + mLatd);
        mParams.put(DataBaseHelper.FACILITY_NAME, GPS);
        addTime();
        mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
        mAfdFound = null;
        mName = Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
        mDestType = GPS;
    }


	/**
	 * 
	 * @param name
	 * @param type
	 */
	private void parseGps(String name, String type) {
        /*
         * GPS
         * GPS coordinates are either x&y (user), or addr@x&y (google maps)
         * get the x&y part, then parse them to lon=y lat=x
         */
        if(name.contains("&")) {
            String token[] = new String[2];
            token[1] = token[0] = name;
            if(name.contains("@")) {
                /*
                 * This could be the geo point from maps
                 */
                token = name.split("@");
            }
            /*
             * This is lon/lat destination
             */
            String tokens[] = token[1].split("&");
            
            try {
                mLond = Double.parseDouble(tokens[1]);
                mLatd = Double.parseDouble(tokens[0]);
            }
            catch (Exception e) {
                /*
                 * Bad input from user on GPS
                 */
                mName = "";
                mDestType = "";
                return;
            }
            
            /*
             * Sane input
             */
            if((!Helper.isLatitudeSane(mLatd)) || (!Helper.isLongitudeSane(mLond))) {
                mName = "";
                mDestType = "";
                return;             
            }
            mName = token[0];
            mDestType = type;
        }
	}
	
	/**
	 * 
	 * @return
	 */
	public String getStorageName() {
	    StringPreference s = new StringPreference(mDestType, mDbType, getFacilityName(), getID());
	    return s.getHashedName();
	}

	// Build up a storage name using the values passed in
	public static String getStorageName(String destType, String dbType, String facilityName, String id) {
	    StringPreference s = new StringPreference(destType, dbType, facilityName, id);
	    return s.getHashedName();
		
	}
	
	/**
     * Update the current speed, lat, lon, that will update
     * ETA, distance and bearing to the destination
	 * @param params
	 */
	public void updateTo(GpsParams params) {
	    
	    /*
	     */
        double mLon = params.getLongitude();
        double mLat = params.getLatitude();
        double speed = params.getSpeed();
        mDeclination = params.getDeclinition();

		if(!mFound) {
			return;
		}

        if(!mInited) {
            mLonInit = mLon;
            mLatInit = mLat;
            mInited = true;
        }
        
		/*
		 * Project and find distance
		 */
		Projection p = new Projection(mLon, mLat, mLond, mLatd);
		
    	mDistance = p.getDistance();

    	mBearing = p.getBearing();
    	
    	/*
    	 * ETA when speed != 0
    	 */
    	mEte = Helper.calculateEte(mPref.useBearingForETEA() && (!mService.getPlan().isActive()), mDistance, speed, mBearing, params.getBearing());

    	// Calculate the time of arrival at our destination. We SHOULD be taking in to account
    	// the timezone at that location
    	mEta = Helper.calculateEta(mPref.useBearingForETEA() && (!mService.getPlan().isActive()), Calendar.getInstance().getTimeZone(), mDistance, speed, mBearing, params.getBearing());
	}

	/**
	 * 
	 * @return
	 */
	public String getEte() {
		return mEte;
	}

	/**
	 * 
	 * @return
	 */
	public String getEta() {
		return mEta;
	}

	/**
	 * 
	 */
	private void addTime() {
        TwilightCalculator calc = new TwilightCalculator();
        calc.calculateTwilight(mLatd, mLond);
        mParams.put("Sunrise", calc.getSunrise());
        mParams.put("Sunset", calc.getSunset());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    /*
	     * For display purpose
	     */
		if(!mFound) {
			return(mName + "? ");
		}
		else {
			return Helper.makeLine(mDistance, Preferences.distanceConversionUnit, mEte, mBearing, mDeclination); 
		}
	}

	   /**
     * Database  query to find destination
     */
    public void findGuessType() {
        /*
         * Do in background as database queries are disruptive
         */
        mLooking = true;
        DataBaseLocationTask locmDataBaseTask = new DataBaseLocationTask();
        locmDataBaseTask.execute(true, "");
    }
    
    /**
     * Database  query to find destination
     */
    public void find() {
        /*
         * Do in background as database queries are disruptive
         */
        mLooking = true;
        DataBaseLocationTask locmDataBaseTask = new DataBaseLocationTask();
        locmDataBaseTask.execute(false, "");
    }

	/**
	 * Database  query to find destination
	 * @param dbType
	 */
	public void find(String dbType) {
	    /*
	     * Do in background as database queries are disruptive
	     */
        mLooking = true;
        DataBaseLocationTask locmDataBaseTask = new DataBaseLocationTask();
        locmDataBaseTask.execute(false, dbType);
	}
	
    /**
     * @author zkhan
     * Query for destination task
     */
    private class DataBaseLocationTask extends AsyncTask<Object, Void, Boolean> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {        	
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            Thread.currentThread().setName("Destination");

            Boolean guess = (Boolean)vals[0];
            String dbType = (String)vals[1];
            
            /*
             * If we dont know type, find with a guess.
             */
            if(guess) {
                StringPreference s = mService.getDBResource().searchOne(mName);
                if(null == s) {
                    return false;
                }
                mDestType = s.getType();
                mName = s.getId();
            }

            /*
             * If GPS/Maps, parse
             */
            if(mName.contains("&")) {
                parseGps(mName, mDestType);
            }

	        if(mDestType.equals(UDW)){
	        	Waypoint p = mService.getUDWMgr().get(mName);
	        	if(null != p) {
	        		mLatd = p.getLat();
	        		mLond = p.getLon();
	        		mCmt  = p.getCmt();
		            mParams.put(DataBaseHelper.LONGITUDE, "" + mLond);
		            mParams.put(DataBaseHelper.LATITUDE, "" + mLatd);
		            mParams.put(DataBaseHelper.FACILITY_NAME, UDWMgr.UDWDESCRIPTION);
		            addTime();
		            mAfdFound = null;
		            mFound = true;
		            mLooking = false;
		            mDbType = UDW;
		            mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
		        	return true;
	        	}
	        	return false;
	        }

	        if(mDestType.equals(GPS)) {
	            /*
	             * For GPS coordinates, simply put parsed lon/lat in params
	             * No need to query database
	             */
	            mParams = new LinkedHashMap<String, String>();
	            mFreq = new LinkedHashMap<String, String>();
	            mAwos = new LinkedList<Awos> ();
	            mParams.put(DataBaseHelper.LONGITUDE, "" + mLond);
	            mParams.put(DataBaseHelper.LATITUDE, "" + mLatd);
	            mParams.put(DataBaseHelper.FACILITY_NAME, GPS);
	            addTime();
	            mAfdFound = null;
	            mFound = true;
	            mLooking = false;
	            mDbType = GPS;
	            mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
	            if(!isGPSValid(mName)) {
	                mFound = false;
	            }
	            if(!mName.contains("&")) {
	                /*
	                 * This comes from MAPS to GPS for user edited
	                 */
	                mName += "@" + mLatd + "&" + mLond;
	            }
	            return true;
	        }

            if(null == mDataSource) {
                return false;
            }
	            

	        /*
	         * For Google maps address, if we have already geo decoded it using internet,
	         * then no need to do again because internet may not be available on flight.
	         * It could be coming from storage and not google maps.
	         */
	        if(mDestType.equals(MAPS)) {

	            if(mLond == 0 && mLatd == 0) {
	                /*
	                 * We have already decomposed it?
	                 * No.
	                 */
	                String strAddress = mName;
	                
	                Geocoder coder = new Geocoder(mService);
	                Address location = null;

	                /*
	                 * Decompose
	                 */
	                try {
	                    List<Address> address = coder.getFromLocationName(strAddress, 1);
	                    if (address != null) {
	                        location = address.get(0);
	                    }
	                }
	                catch (Exception e) {
	                    return false;
	                }
	                
	                if(null == location) {
	                    return false;
	                }
	                                        
	                /*
	                 * Decomposed it
	                 * 
	                 */
	                try {
    	                mLond = Helper.truncGeo(location.getLongitude());
    	                mLatd = Helper.truncGeo(location.getLatitude());
	                }
	                catch (Exception e) {
	                    
	                }
	                if((!Helper.isLatitudeSane(mLatd)) || (!Helper.isLongitudeSane(mLond))) {
	                    return false;  
	                }

	            }
                /*
                 * Common stuff
                 */
                mParams = new LinkedHashMap<String, String>();
                mFreq = new LinkedHashMap<String, String>();
                mAwos = new LinkedList<Awos> ();
                mAfdFound = null;
                mDbType = mDestType;
                mParams.put(DataBaseHelper.TYPE, mDestType);
                mParams.put(DataBaseHelper.FACILITY_NAME, mName);
                mParams.put(DataBaseHelper.LONGITUDE, "" + mLond);
                mParams.put(DataBaseHelper.LATITUDE, "" + mLatd);
                addTime();
                mName += "@" + mLatd + "&" + mLond;
                return true;                    
	        }
	        
	        /*
	         * For all others, find in DB
	         */
	        mDataSource.findDestination(mName, mDestType, dbType, mParams, mRunways, mFreq, mAwos);

	        if(mDestType.equals(BASE)) {

                /*
                 * Find A/FD
                 */
                mAfdFound = null;
                final LinkedList<String> afdName = mDataSource.findAFD(mName);
                if(afdName.size() > 0) {
                    FilenameFilter filter = new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            boolean match = false;
                            for(final String name : afdName) {
                                match |= fileName.matches(name + Preferences.IMAGE_EXTENSION) ||
                                        fileName.matches(name + "-[0-9]+" + Preferences.IMAGE_EXTENSION);
                            }
                            return match;
                        }
                    };
                    String afd[] = null;
                    afd = new File(mPref.mapsFolder() + "/afd/").list(filter);
                    if(null != afd) {
                        java.util.Arrays.sort(afd);
                        int len1 = afd.length;
                        String tmp1[] = new String[len1];
                        for(int count = 0; count < len1; count++) {
                            /*
                             * Add A/FD
                             */
                            String tokens[] = afd[count].split(Preferences.IMAGE_EXTENSION);
                            tmp1[count] = mPref.mapsFolder() + "/afd/" +
                                    tokens[0];             
                        }
                        if(len1 > 0) {
                            mAfdFound = tmp1;
                        }
                    }
                }
	        }

            return(!mParams.isEmpty());
        }
        

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
        	/*
        	 * This runs on UI
        	 */
            mFound = result;
            if(mDbType.equals(GPS) || mDbType.equals(UDW) || mDbType.equals(MAPS)) {
                /*
                 * These dont come from db so dont assign from params.
                 */
            }
            else {
    			if(mFound) {
                    mDbType = mParams.get(DataBaseHelper.TYPE);
                    try {
            		    mLond = Double.parseDouble(mParams.get(DataBaseHelper.LONGITUDE));
            		    mLatd = Double.parseDouble(mParams.get(DataBaseHelper.LATITUDE));
                    }
                    catch(Exception e) {
                        mFound = false;
                    }
    			}
            }
            /**
             * 
             */
            addTime();

			/*
			 * Anyone watching if destination found?
			 */
            mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
			Destination.this.setChanged();
            Destination.this.notifyObservers(Boolean.valueOf(mFound));
            mLooking = false;
	    }
    }
    
    /**
     * @return
     */
    public boolean isFound() {
    	return(mFound);
    }

    /**
     * @return
     */
    public boolean isLooking() {
        return(mLooking);
    }

    /**
     * @return
     */
    public String[] getAfd() {
        return(mAfdFound);
    }

    /**
     * @return
     */
    public String getFacilityName() {
    	return(mParams.get(DataBaseHelper.FACILITY_NAME));
    }

    /**
     * @return
     */
    public String getID() {
        return(mName);
    }

    /**
     * @return
     */
    public LinkedList<Runway> getRunways() {
        return(mRunways);
    }

    /**
     * @return
     */
    public BitmapHolder getBitmap() {
        return(mService.getDiagram());
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getParams() {
    	return(mParams);
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getFrequencies() {
        return(mFreq);
    }

    /**
     * @return
     */
    public double getBearing() {
        return mBearing;
    }

    /**
     * @return
     */
    public double getDistance() {
        return mDistance;
    }

    /**
     * 
     * @return
     */
    public Location getLocation() {
        Location l = new Location("");
        l.setLatitude(mLatd);
        l.setLongitude(mLond);
        return l;
    }    

    /**
     * 
     * @return
     */
    public String getType() {
        return mDestType;
    }
    
    /**
     * 
     * @return
     */
    public Location getLocationInit() {
        Location l = new Location("");
        l.setLatitude(mLatInit);
        l.setLongitude(mLonInit);
        return l;
    }    

    /**
     * 
     * @return
     */
    public TrackShape getTrackShape() {
        return mTrackShape;
    }

	public LinkedList<Awos> getAwos() {
		return(mAwos);
		
	} 

	/***
	 * Fetch the destination elevation 
	 * @return Elevation in feet. <-200 is an error
	 */
	public double getElevation(){
        try {
            double elev = (Double.parseDouble(mParams.get(DataBaseHelper.ELEVATION)));
            return elev;
        }
        catch (Exception e) { }
		return -200;
	}
	
	/**
	 * Find vertical speed to this dest in feet/m per minute
	 * Limit to +/- 9999
	 */
	public String getVerticalSpeedTo(GpsParams params) {
	    long vs = Math.min(getVerticalSpeedToNoFmt(params), 9999);
	    vs = Math.max(vs, -9999);
	    String retVS = String.format(Locale.getDefault(), "%+05d", vs);

	    return retVS;
	}
	
	/**
     * Find flight path required to this dest in degrees
     */
    public String getFlightPathRequired(GpsParams params) {
        double fpr = 0;
        if(mDistance > 0) {
            fpr = Math.atan2(getAltitudeAboveDest(params), mDistance * Preferences.feetConversion) * 180.0 / Math.PI;
        }
        
        return String.format(Locale.getDefault(), "%+06.2f", -fpr);
    }
    
    public double getAltitudeAboveDest(GpsParams gpsParams) {
        double height = gpsParams.getAltitude();
        if(mDestType.equals(BASE)) {
            try {
                /*
                 * For bases, go to pattern altitude
                 */
                String pa = mParams.get("Pattern Altitude");
                height -= Double.parseDouble(pa);
            }
            catch(Exception e) {
                
            }
        }
        else {
            /*
             * Only for airport
             */
            return 0;
        }
        
        return height;
    }
    
	public long getVerticalSpeedToNoFmt(GpsParams gpsParams)
	{
	    double altAbove = getAltitudeAboveDest(gpsParams);
	    double time = (mDistance / gpsParams.getSpeed()) * 60;
	    if(altAbove == 0 || time == 0) {
	        return 0;
	    }

	    return -Math.round(altAbove / time);
	}
	
	/**
	 * Find if a GPS dst is valid
	 * @return
	 */
	public static boolean isGPSValid(String dst) {
        if(dst.contains("&")) {
            String tokens[] = dst.split("&");
            
            try {
                double lon = Double.parseDouble(tokens[1]);
                double lat = Double.parseDouble(tokens[0]);
                if((Helper.isLatitudeSane(lat)) && (Helper.isLongitudeSane(lon))) {
                    return true;
                }
            }
            catch (Exception e) {
            }
        }
	    return false;
	}

	/**
	 * Get declination
	 * @return
	 */
	public double getDeclination() {
		return mDeclination;
	}
}
