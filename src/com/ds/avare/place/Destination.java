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
import java.util.Comparator;
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
     * ETA to destination
     */
    private String mEta;
    
    /*
     * Track to dest.
     */
    TrackShape mTrackShape;

    /*
     * For GPS taxi
     */
    private float[] mMatrix;
        
    /*
     * Its lon/lat
     */
    private double mLond;
    private double mLatd;

    private String mPlateFound[];
    private String mAfdFound[];
    
    private Preferences mPref;
    
    private StorageService mService;
    
    private boolean mLooking;
    private boolean mInited;
    
    private String mAfdName;
    
    private double mDeclination;

    /*
     * This is where destination was set.
     */
    private double mLonInit;
    private double mLatInit;
    
    private String mDestType;
    private String mDbType;
    private LinkedList<Runway> mRunways;
    private LinkedHashMap <String, String>mFreq;
    private LinkedList<Awos> mAwos;
    
    public static final String GPS = "GPS";
    public static final String MAPS = "Maps";
    public static final String BASE = "Base";
    public static final String FIX = "Fix";
    public static final String NAVAID = "Navaid";
    public static final String AD = "AIRPORT-DIAGRAM";
    
    /**
     * Contains all info in a hash map for the destination
     * Dozens of parameters in a linked map because simple map would rearrange the importance
     */
    private LinkedHashMap <String, String>mParams;
    
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
        mEta = new String("--:--");
        mParams = new LinkedHashMap<String, String>();
        mFreq = new LinkedHashMap<String, String>();
        mAwos = new LinkedList<Awos> ();
        mPlateFound = null;
        mAfdFound = null;
        
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
	        return;
	    }
	    mName = name.toUpperCase(Locale.getDefault());
	    mDestType = type;
    	mLond = mLatd = 0;
	}

	/**
	 * 
	 * @return
	 */
	public String getStorageName() {
	    StringPreference s = new StringPreference(mDestType, mDbType, getFacilityName(), getID());
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
    	mEta = Helper.calculateEta(mDistance, speed);
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
			return Helper.makeLine(mDistance, Preferences.distanceConversionUnit, mEta, mBearing, mDeclination); 
		}
	}
	
	/**
	 * Database  query to find destination
	 */
	public void find() {
	    /*
	     * Do in background as database queries are disruptive
	     */
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
            mPlateFound = null;
            mAfdFound = null;
            mFound = true;
            mLooking = false;
            mDbType = GPS;
            mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
            if(!mName.contains("&")) {
                /*
                 * This comes from MAPS to GPS for user edited
                 */
                mName += "@" + mLatd + "&" + mLond;
            }
            setChanged();
            notifyObservers(true);
	    }
	    else { 
            mLooking = true;
            DataBaseLocationTask locmDataBaseTask = new DataBaseLocationTask();
            locmDataBaseTask.execute();
	    }
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
                mPlateFound = null;
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
	        
	        mDataSource.findDestination(mName, mDestType, mParams, mRunways, mFreq, mAwos);

	        if(mDestType.equals(BASE)) {
	            
                mPlateFound = null;
                mAfdFound = null;
                mAfdName = mDataSource.findAFD(mName);
                
	            /*
	             * Found destination extract its airport plates
	             */
                String tmp0[] = null;
                int len0 = 0;
	            if(null != mName) {
    	            FilenameFilter filter = new FilenameFilter() {
    	                public boolean accept(File directory, String fileName) {
    	                    return fileName.endsWith(Preferences.IMAGE_EXTENSION);
    	                }
    	            };
                    String plates[] = null;
    	            plates = new File(mPref.mapsFolder() + "/plates/" + mName).list(filter);
                    if(null != plates) {
                        java.util.Arrays.sort(plates, new PlatesComparable());
                        len0 = plates.length;
                        tmp0 = new String[len0];
                        for(int plate = 0; plate < len0; plate++) {
                            /*
                             * Add plates/AD
                             */
                            String tokens[] = plates[plate].split(Preferences.IMAGE_EXTENSION);
                            tmp0[plate] = mPref.mapsFolder() + "/plates/" + mName + "/" +
                                    tokens[0];
                        }
                    }
	            }
	            
                /*
                 * Take off and alternate minimums
                 */
                String tmp2[] = mDataSource.findMinimums(mName);
                int len2 = 0;
                if(null != tmp2) {
                    len2 = tmp2.length;
                    for(int min = 0; min < len2; min++) {
                        /*
                         * Add minimums with path
                         */
                        String folder = tmp2[min].substring(0, 1) + "/";
                        tmp2[min] = mPref.mapsFolder() + "/minimums/" + folder + tmp2[min];
                    }
                }
                
                /*
                 * Now combine to/alt with plates
                 */
                if(0 == len0 && 0 != len2) {
                    mPlateFound = tmp2;
                }
                else if(0 != len0 && 0 == len2) {
                    mPlateFound = tmp0;
                }
                else if(0 != len0 && 0 != len2) {
                    mPlateFound = new String[len0 + len2];
                    System.arraycopy(tmp0, 0, mPlateFound, 0, len0);
                    System.arraycopy(tmp2, 0, mPlateFound, len0, len2);
                }
                
                /*
                 * Find A/FD
                 */
                if(null != mAfdName) {
                    FilenameFilter filter = new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return (fileName.matches(mAfdName + Preferences.IMAGE_EXTENSION) || 
                                    fileName.matches(mAfdName + "-[0-9]+" + Preferences.IMAGE_EXTENSION));
                        }
                    };
                    String afd[] = null;
                    afd = new File(mPref.mapsFolder() + "/afd/").list(filter);
                    if(null != afd) {
                        java.util.Arrays.sort(afd);
                        int len1 = afd.length;
                        String tmp1[] = new String[len1];
                        for(int plate = 0; plate < len1; plate++) {
                            /*
                             * Add A/FD
                             */
                            String tokens[] = afd[plate].split(Preferences.IMAGE_EXTENSION);
                            tmp1[plate] = mPref.mapsFolder() + "/afd/" +
                                    tokens[0];             
                        }
                        if(len1 > 0) {
                            mAfdFound = tmp1;
                        }
                    }
                }
	        }

            /*
             * GPS taxi for this airport?
             */
            mMatrix = mDataSource.findDiagramMatrix(mName);

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
    public String[] getPlates() {
        return(mPlateFound);
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
    public float[] getMatrix() {
        return(mMatrix);
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
    
    /**
     * 
     * @author zkhan
     *
     */
    private class PlatesComparable implements Comparator<String>{
        
        @Override
        public int compare(String o1, String o2) {
            /*
             * Airport diagram must be  first
             */
            if(o1.startsWith("AIRPORT-DIAGRAM")) {
                return -1;
            }
            if(o2.startsWith("AIRPORT-DIAGRAM")) {
                return 1;
            }
            
            /*
             * Continued must follow main
             */
            if(o1.contains(",-CONT.") && o1.startsWith(o2.replace(Preferences.IMAGE_EXTENSION, ""))) {
                return 1;
            }
            if(o2.contains(",-CONT.") && o2.startsWith(o1.replace(Preferences.IMAGE_EXTENSION, ""))) {
                return -1;
            }
            
            return o1.compareTo(o2);
        }
    }

	public LinkedList<Awos> getAwos() {
		return(mAwos);
		
	} 

}
