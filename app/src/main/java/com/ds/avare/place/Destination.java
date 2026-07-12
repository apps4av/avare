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
import com.ds.avare.position.Coordinate;
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

    // Length of a calculation segment. A leg is broken into segments this long so
    // that winds are sampled and applied as they change along the route (like avarex).
    private static final double SEGMENT_LENGTH = 100;
    // Cap on the number of segments per leg to bound wind lookups on very long legs
    private static final int MAX_SEGMENTS = 50;

    /**
     * Contains all info in a hash map for the destination
     * Dozens of parameters in a linked map because simple map would rearrange the importance
     */
    protected LinkedHashMap <String, String>mParams;
    private double mWindMetar[] = null;

    public Destination(String name) {
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

        GpsParams params = StorageService.getInstance().getGpsParams();

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
		 * Project and find distance and initial bearing to the destination
		 */
		Projection p = new Projection(mLon, mLat, mLond, mLatd);
		
    	mDistance = p.getDistance();

    	mBearing = p.getBearing();


        mWca = 0;
        mCrs = mBearing;
        mWindString = "-";
        mGroundSpeed = params.getSpeed();

        // METAR wind used to interpolate winds down to the ground on low altitude flight
        double metarWind[] = null;
        if(mWindMetar != null) {
            metarWind = new double[2];
            metarWind[1] = mWindMetar[1];
            metarWind[0] = (mWindMetar[0] - mDeclination + 360) % 360;
        }

        Preferences pref = StorageService.getInstance().getPreferences();

        /*
         * True airspeed is constant along the leg. In simulation the given speed is the
         * aircraft cruise TAS. In live flight derive the TAS from the GPS ground speed and
         * the wind at the current position.
         */
        double tas = params.getSpeed();
        WindsAloft startWinds = getWindsAloftAt(mLon, mLat);
        if(!pref.isSimulationMode()) {
            double ws0 = 0, wd0 = 0;
            if(startWinds != null) {
                double winds[] = startWinds.getWindAtAltitude(mAltitude, metarWind);
                ws0 = winds[0];
                wd0 = winds[1];
            }
            double t[] = WindTriagle.getTrueFromGroundAndWind(params.getSpeed(), params.getBearing(), ws0, wd0);
            tas = t[0];
        }

        /*
         * Break the leg into segments along the great circle so that winds are applied as
         * they change along the route. This matches avarex DestinationCalculations. Time and
         * fuel accumulate over the segments; the values shown are from the first (start)
         * segment since averaging them makes no sense.
         */
        int num = (int) Math.round(mDistance / SEGMENT_LENGTH);
        if(num < 2) {
            num = 2;
        }
        if(num > MAX_SEGMENTS) {
            num = MAX_SEGMENTS;
        }
        // findPoints() returns points ordered from destination to origin, so walk it backwards
        Coordinate points[] = p.findPoints(num);

        double totalTimeSec = 0;
        boolean stationary = false;
        boolean first = true;

        for(int i = num - 1; i > 0; i--) {
            Coordinate segStart = points[i];     // closer to the origin
            Coordinate segEnd = points[i - 1];   // closer to the destination

            double d = Projection.getStaticDistance(segStart.getLongitude(), segStart.getLatitude(),
                    segEnd.getLongitude(), segEnd.getLatitude());
            double tc = Projection.getStaticBearing(segStart.getLongitude(), segStart.getLatitude(),
                    segEnd.getLongitude(), segEnd.getLatitude());

            double ws = 0, wd = 0;
            WindsAloft w = first ? startWinds : getWindsAloftAt(segStart.getLongitude(), segStart.getLatitude());
            if(w != null) {
                double winds[] = w.getWindAtAltitude(mAltitude, metarWind);
                ws = winds[0];
                wd = winds[1];
            }

            double sol[] = WindTriagle.solveWindTriangle(ws, wd, tc, tas);
            double wca = sol[0];
            double gs = sol[2];

            if(first) {
                first = false;
                mGroundSpeed = gs;
                mWca = wca;
                mCrs = (tc + wca + 360) % 360;
                if(w != null) {
                    mWindString = String.format(Locale.getDefault(),
                            ws >= 100 ? "%03d@%03d" : "%03d@%02d", Math.round(wd), Math.round(ws));
                }
                else {
                    mWindString = "-";
                }
            }

            if(gs < 1) { // practically stationary or the headwind exceeds the true airspeed
                stationary = true;
                break;
            }
            totalTimeSec += 3600.0 * d / gs;
        }

        double xFactor = 1;
        if(pref.useBearingForETEA() && (!StorageService.getInstance().getPlan().isActive())) {
            // This is just when we have a destination set and no plan is active
            // We can't assume that we are heading DIRECTLY for the destination, so
            // we need to figure out the multiply factor by taking the COS of the difference
            // between the bearing and the heading.
            double angDif = Helper.angularDifference(params.getBearing(), mBearing);

            // If the difference is 90 or greater, then ETE means nothing as we are not
            // closing on the target
            if(angDif < 90) {
                // Calculate the actual relative speed closing on the target
                xFactor = Math.cos(angDif * Math.PI / 180);
            }
            mGroundSpeed *= xFactor;
        }

        if(stationary || totalTimeSec <= 0 || xFactor <= 0) { // practically stationary
            mEteSec = Long.MAX_VALUE;
            mFuelGallons = Float.MAX_VALUE;
            mFuel = "-.-";
            mEte = "--:--";
            mEta = "--:--";
        }
        else {
            mEteSec = (long)(totalTimeSec / xFactor);
            mFuelGallons = (float)mEteSec / 3600 * StorageService.getInstance().getAircraft().getFuelBurnRate();
            mFuel = String.valueOf((float)Math.round(mFuelGallons * 10.f) / 10.f);
            mEte = Helper.calculateEte(0, 0, mEteSec, false);

            // Calculate the time of arrival at our destination based on the system time
            // We SHOULD be taking in to account the timezone at that location
            double effSpeed = mDistance * 3600.0 / (double) mEteSec;
            mEta = Helper.calculateEta(CalendarHelper.getInstance(System.currentTimeMillis()), mDistance, effSpeed);
        }
	}

    /**
     * Find winds aloft nearest to a given point, using the same source (ADSB or database)
     * that updateWinds() uses. Returns null if none are available.
     */
    private WindsAloft getWindsAloftAt(double lon, double lat) {
        try {
            StorageService s = StorageService.getInstance();
            if (s.getPreferences().useAdsbWeather()) {
                return s.getAdsbWeather().getWindsAloft(lon, lat);
            }
            return s.getDBResource().getWindsAloft(lon, lat);
        } catch (Exception e) {
            return null;
        }
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
        StorageService.getInstance().getPreferences().setLastLocation(getLocation().getLongitude(), getLocation().getLatitude());

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
            StorageService s = StorageService.getInstance();
            if (s.getPreferences().useAdsbWeather()) {
                w = s.getAdsbWeather().getWindsAloft(mLond, mLatd);
                if (null != w) {
                    if (null != w.getStation()) {
                        m = s.getAdsbWeather().getMETAR(w.getStation());
                    }
                }
            } else {
                w = s.getDBResource().getWindsAloft(mLond, mLatd);
                if (null != w) {
                    if (null != w.getStation()) {
                        m = s.getDBResource().getMetar(w.getStation());
                    }
                }
            }
            if (m != null) {
                mWindMetar = WeatherHelper.getWindFromMetar(m.getRawText());
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
