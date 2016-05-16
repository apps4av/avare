/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com), 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.gps;

import android.hardware.GeomagneticField;
import android.location.Location;

import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;

/**
 * @author zkhan
 * A class to hold GPS params in the format we need
 */
public class GpsParams {

    private double mSpeed;
    private double mLongitude;
    private double mLatitude;
    private double mAltitude;
    private double mBearing;
    private Scale  mScale;
    private float  mDeclination;
    private long   mTime;
    


    /**
     * @param location
     */
    public GpsParams(Location location) {
        
        if(location == null) {
            mSpeed = 0;
            mLongitude = 0;
            mLatitude = 0;
            mAltitude = 0;
            mBearing = 0;
            mScale = new Scale();
            mDeclination = 0;
            mTime = 0;
            return;
        }
        
        mSpeed = location.getSpeed() * Preferences.speedConversion; // ms / sec to knot / hr;
        mLongitude = location.getLongitude();
        mLatitude = location.getLatitude();
        mAltitude = location.getAltitude() * Preferences.heightConversion; // meters to feet;
        
        /*
         * Find declination
         */
        GeomagneticField gmf = new GeomagneticField((float)location.getLatitude(), 
                (float)location.getLongitude(), 0, System.currentTimeMillis());
        mDeclination = -gmf.getDeclination();
        gmf = null;
        
        mBearing = (location.getBearing() + 360) % 360;
        mScale = new Scale();
        
        mTime = location.getTime();
    }

    /***
     * Make an exact copy from the RightHandSide into a newly allocated LeftHandSide object
     * @param rhs
     * @return Duplicate copy of the right hand side
     */
    public static GpsParams copy(GpsParams rhs) {
    	GpsParams lhs = new GpsParams(null);
        lhs.mSpeed       = rhs.mSpeed;
        lhs.mLongitude   = rhs.mLongitude;
        lhs.mLatitude    = rhs.mLatitude;
        lhs.mAltitude    = rhs.mAltitude;
        lhs.mBearing     = rhs.mBearing;
        lhs.mScale       = rhs.mScale;
        lhs.mDeclination = rhs.mDeclination;
        lhs.mTime        = rhs.mTime;
        return lhs;
    }

    /**
     * Speed in location to speed in params
     * @param locationSpeed
     * @return
     */
    public static double speedConvert(double locationSpeed) {
    	return locationSpeed / Preferences.speedConversion;
    }

    /**
     * Altitude in location to altitude in params
     * @param locationAltitude
     * @return
     */
    public static double altitudeConvert(double locationAltitude) {
        return locationAltitude / Preferences.heightConversion;
    }

    /**
     * @return
     * double Speed in (miles or knots) per hour depending on preference settings
     */
    public double getSpeed() {
        return mSpeed;
    }
    /**
     * @return 
     * double Current longitude
     */
    public double getLongitude() {
        return mLongitude;
    }
    /**
     * @return 
     * double Current latitude
     */
    public double getLatitude() {
        return mLatitude;
    }
    /**
     * @return
     * double Current altitude preferred distance unit (e.g. feet or meters)
     */
    public double getAltitude() {
        return mAltitude;
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
    public Scale getScale() {
        return mScale;
    }

    /**
     * 
     * @return
     */
    public double getDeclinition() {
        return mDeclination;
    }

    /**
     * Return  the UTS time of this fix, in milliseconds since Jan 1, 1970.
     * @return long mTime
     */
    public long getTime() {
    	return mTime;
    }
    
    /***
     * Convert the latitude into string format of Deg Min Sec
     * @return
     */
    public String getLatStringDMS() {
    	return (mLatitude >= 0 ? "N" : "S") + getDMS(Math.abs(mLatitude));
    }
    
    /***
     * Convert the longitude into string format of Deg Min Sec
     * @return
     */
    public String getLonStringDMS() {
    	return (mLongitude >= 0 ? "E" : "W") + getDMS(Math.abs(mLongitude));
    }

    /***
     * Convert the indicated double value into a deg/min/sec string representation 
     * @param frac
     * @return DD MM SS.SS format
     */
	private String getDMS(double frac) {
    	
    	// Degress is the integer part of the number
		double deg = (int)(frac);

		// Minutes is the decimal part of the number multiplied by 60
		frac -= deg;
		frac *= 60;
		double min = (int)(frac);

		// Seconds is the reminder after the minutes calc multipled by 60
		frac -= min;
		double sec = frac * 60;

		// Place all those values into a string and return
		return String.format("%02.0f\u00B0 %02.0f\' %02.2f\"", deg, min, sec);
    }

    /**
     *
     * @param speed
     */
    public void setSpeed(int speed) {
         mSpeed = speed;
    }

    /**
     *
     * @param altitude
     */
    public void setAltitude(int altitude) {
        mAltitude = altitude;
    }

    public void setLongitude(double lon) {
        mLongitude = lon;
    }

    public void setLatitude(double lat) {
        mLatitude = lat;
    }
}
