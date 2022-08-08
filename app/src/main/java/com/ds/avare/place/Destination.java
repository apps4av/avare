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

import android.location.Location;

import com.ds.avare.StorageService;
import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Projection;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.CalendarHelper;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.TwilightCalculator;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.utils.WindTriagle;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.WindsAloft;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Observable;

/**
 * @author zkhan, jlmcgraw
 * Stores everything about destination, its name (ICAO code)
 * Does databse query to find the destination as well.
 */
public class Destination extends Observable {

    /**
     * 
     */
    protected String mName;

    /**
     * 
     */
    private double mDistance;
    private double mGroundSpeed;
    /**
     * 
     */
    private double mBearing;
    private double mWca;
    private double mCrs;

    /**
     * If a destination is found?
     */
    protected boolean mFound;
    /**
     * ETE to destination
     * ETA at destination
     */
    private String mEte;
    private Long mEteSec;
    private String mFuel;
    private float mFuelGallons;
    private String mEta;

    private WindsAloft mWinds;
    private int mAltitude;

    /*
     * Track to dest.
     */
    protected TrackShape mTrackShape;
        
    /*
     * Its lon/lat
     */
    protected double mLond;
    protected double mLatd;
    protected double mEle;

    private String mWindString;
    
    protected Preferences mPref;
    
    protected StorageService mService;

    protected boolean mLooking;
    protected boolean mInited;
    
    private double mDeclination;

    /*
     * This is where destination was set.
     */
    protected double mLonInit;
    protected double mLatInit;

    protected String mDestType;
    protected String mDbType;

    public static final String GPS = "GPS";
    public static final String MAPS = "Maps";
    public static final String BASE = "Base";
    public static final String FIX = "Fix";
    public static final String NAVAID = "Navaid";
    public static final String UDW = "UDW";

    /**
     * Contains all info in a hash map for the destination
     * Dozens of parameters in a linked map because simple map would rearrange the importance
     */
    protected LinkedHashMap <String, String>mParams;
    private double mWindMetar[] = null;

    public Destination(StorageService service, String name) {
        mPref = new Preferences(service.getApplicationContext());
        mService = service;
        mTrackShape = new TrackShape();
        mEte = "--:--";
        mEta = "--:--";
        mFuel = "-.-";
        mAltitude = 0;
        mParams = new LinkedHashMap<String, String>();

        mEteSec = Long.MAX_VALUE;
        mFuelGallons = Float.MAX_VALUE;

        mWindString = "-";

        mFound = false;
        mLooking = false;

        GpsParams params = service.getGpsParams();

        mName = name.toUpperCase(Locale.getDefault());

        mInited = false;

        mLond = 0;
        mLatd = 0;

        if(null != params) {
            mLonInit = params.getLongitude();
            mLatInit = params.getLatitude();
            mInited = true;
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
        mDeclination = params.getDeclinition();
        mAltitude = (int)params.getAltitude();

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


        mWca = 0;
        mCrs = mBearing;
        mWindString = "-";
        double hd = mBearing;
        double wm[] = {0, 0};
        if(mWindMetar != null) {
            // if low altitude flight use correction with metar
            wm[1] = mWindMetar[1];
            wm[0] = (mWindMetar[0] - mDeclination + 360) % 360;
        }
        double ws = 0;
        double wd = 0;
        double tas = params.getSpeed();
        if(mWinds != null) {
            // wind calculation
            double winds[] = mWinds.getWindAtAltitude(mAltitude, wm);
            ws = winds[0];
            wd = winds[1];
            mWindString = String.format(Locale.getDefault(),
                    ws >= 100 ? "%03d@%03d" : "%03d@%02d", Math.round(wd), Math.round(ws));
        }
        else {
            mWindString = "-";
        }

        if(!mPref.isSimulationMode()) {
            double t[] = WindTriagle.getTrueFromGroundAndWind(params.getSpeed(), params.getBearing(), ws, wd);
            tas = t[0];
            hd = t[1];
        }

        // from wind triangle
        mGroundSpeed = Math.sqrt(ws * ws + tas * tas - 2 * ws * tas * Math.cos((hd - wd) * Math.PI / 180.0));
        mWca = -Math.toDegrees(Math.atan2(ws * Math.sin((hd - wd) * Math.PI / 180.0), tas - ws * Math.cos((hd - wd) * Math.PI / 180.0)));
        mCrs = (hd + mWca + 360) % 360;

        if(mPref.useBearingForETEA() && (!mService.getPlan().isActive())) {
            // This is just when we have a destination set and no plan is active
            // We can't assume that we are heading DIRECTLY for the destination, so
            // we need to figure out the multiply factor by taking the COS of the difference
            // between the bearing and the heading.
            double angDif = Helper.angularDifference(params.getBearing(), mBearing);
            double xFactor = 1;

            // If the difference is 90 or greater, then ETE means nothing as we are not
            // closing on the target
            if(angDif < 90) {
                // Calculate the actual relative speed closing on the target
                xFactor = Math.cos(angDif * Math.PI / 180);
            }
            mGroundSpeed *= xFactor;
        }

    	/*
    	 * ETA when speed != 0
    	 */
    	mEte = Helper.calculateEte(mDistance, mGroundSpeed, 0, true);
        if(mGroundSpeed < 1) { // practically stationary
            mEteSec = Long.MAX_VALUE;
            mFuelGallons = Float.MAX_VALUE;
            mFuel = "-.-";
        }
        else {
            mEteSec = (long)(mDistance / mGroundSpeed * 3600);
            mFuelGallons = (float)mEteSec / 3600 * mPref.getFuelBurn();
            mFuel = String.valueOf((float)Math.round(mFuelGallons * 10.f) / 10.f);
        }

    	// Calculate the time of arrival at our destination based on the system time
        // We SHOULD be taking in to account the timezone at that location
    	mEta = Helper.calculateEta(CalendarHelper.getInstance(System.currentTimeMillis()), mDistance, mGroundSpeed);
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


    protected void found() {
        TwilightCalculator calc = new TwilightCalculator();
        calc.calculateTwilight(mLatd, mLond);
        mParams.put("Sunrise", calc.getSunrise());
        mParams.put("Sunset", calc.getSunset());
        /*
         * Anyone watching if destination found?
         */
        mTrackShape.updateShape(new GpsParams(getLocationInit()), Destination.this);
        // Save last known good location
        mPref.setLastLocation(getLocation().getLongitude(), getLocation().getLatitude());

        mLooking = false;
        Destination.this.setChanged();
        Destination.this.notifyObservers(Boolean.valueOf(mFound));
    }


    /**
     *
     * @return
     */
    public String getEte() {
        return mEte;
    }

    public float getFuelGallons() {
        return mFuelGallons;
    }

    public String getCourse() {
        return String.valueOf(mCrs);
    }

    public double getWCA() {
        return mWca;
    }

    public double getGroundSpeed() {
        return mGroundSpeed;
    }

    /**
     *
     * @return
     */
    public Long getEteSec() {
        return mEteSec;
    }

    /**
     *
     * @return
     */
    public String getEta() {
        return mEta;
    }

    public String getFuel() {
        return mFuel;
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
        return(null);
    }

    /**
     * @return
     */
    public String getFacilityName() {
    	return(mParams.get(LocationContentProviderHelper.FACILITY_NAME));
    }

    public void setFacilityName(String facilityName) {
        mName = facilityName;
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
        return(null);
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
        return(null);
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

    public double getDistanceInNM() {
        if (Preferences.isKnots())  return mDistance;              // Already in nautical miles
        if (Preferences.isMPH())    return mDistance * Preferences.MI_TO_NM;   // miles to nautical
        return mDistance * Preferences.KM_TO_NM;                               // kilometers to nautical
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
    public String getDbType() {
        return mDbType;
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
		return(null);
	} 

	/***
	 * Fetch the destination elevation 
	 * @return Elevation in feet. <-200 is an error
	 */
    public static final float INVALID_ELEVATION = -200;
	public double getElevation(){
        try {
            return Double.parseDouble(mParams.get(LocationContentProviderHelper.ELEVATION));
        }
        catch (Exception ignore) { }
		return INVALID_ELEVATION;
	}

    public void setElevation(Float ele) {
        mParams.put(LocationContentProviderHelper.ELEVATION, ele.toString());
    }

    public boolean hasValidElevation() {
        return getElevation() > INVALID_ELEVATION;
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
	 * Get declination
	 * @return
	 */
	public double getDeclination() {
		return mDeclination;
	}


    public String getWinds() {
        return mWindString;
    }


    public void find() {}

    public void findGuessType() {

    }

    public void find(String dbType) {

    }

    public String getCmt() {
        return null;
    }

    protected void updateWinds() {

	    // call in async task from sub classes to populate winds

        try {
            // Find winds
            Metar m = null;
            WindsAloft w = null;
            if (mPref.useAdsbWeather()) {
                w = mService.getAdsbWeather().getWindsAloft(mLond, mLatd);
                if (null != w) {
                    if (null != w.station) {
                        m = mService.getAdsbWeather().getMETAR(w.station);
                    }
                }
            } else {
                w = mService.getDBResource().getWindsAloft(mLond, mLatd);
                if (null != w) {
                    if (null != w.station) {
                        m = mService.getDBResource().getMetar(w.station);
                    }
                }
            }
            if (m != null) {
                mWindMetar = WeatherHelper.getWindFromMetar(m.rawText);
            }
            mWinds = w;
        } catch (Exception e) {
            mWindMetar = null;
            mWinds = null;
        }

    }

    public int getAltitude() {
        return mAltitude;
    }

    // Simple flag to indicate our intent to land at this destination
    private boolean mLanding = false;
    public void setLanding(boolean landing) { mLanding = landing; }
    public boolean getLanding() { return mLanding; }
}
