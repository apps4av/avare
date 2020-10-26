/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.gps;

import android.hardware.GeomagneticField;
import android.location.Location;

import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;

import java.util.Locale;

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
    private int    mSatCount;
    private double mGeoid;
    private double mHorDil;


    /**
     * @param location a location object
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
            mSatCount = 0;
            mGeoid = 0;
            mHorDil = 1;
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

        // These items come from the base GPS object since they are not part
        // of the position information, but rather properties of the gps receiver itself
        mSatCount = Gps.getSatCount();
        mHorDil   = Gps.getHorDil();
        mGeoid    = Gps.getGeoid();
    }

    /***
     * Make an exact copy from the RightHandSide into a newly allocated LeftHandSide object
     * @param rhs source of the object to deep copy
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
        lhs.mSatCount    = rhs.mSatCount;
        lhs.mGeoid       = rhs.mGeoid;
        lhs.mHorDil      = rhs.mHorDil;
        return lhs;
    }

    /**
     * Speed in location to speed in params
     * @param locationSpeed speed
     * @return double
     */
    public static double speedConvert(double locationSpeed) {
    	return locationSpeed / Preferences.speedConversion;
    }

    /**
     * Altitude in location to altitude in params
     * @param locationAltitude altitude
     * @return double
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

    public double getSpeedInKnots() {
        if (Preferences.isKnots())  return mSpeed;              // Already in KNOTS
        if (Preferences.isMPH())    return mSpeed * Preferences.MI_TO_NM;   // MPH to KNOTS
        return mSpeed * Preferences.KM_TO_NM;                               // KPH to KNOTS
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
     * double Current altitude in feet
     */
    public double getAltitude() {
        return mAltitude;
    }

    public double getAltitudeInMeters() {
        return mAltitude / Preferences.heightConversion;
    }

    /**
     * @return double
     */
    public double getBearing() {
        return mBearing;
    }

    /**
     * @return Scale
     */
    public Scale getScale() {
        return mScale;
    }

    /**
     * 
     * @return double
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
     * @return String
     */
    public String getLatStringDMS() {
    	return (mLatitude >= 0 ? "N" : "S") + getDMS(Math.abs(mLatitude));
    }
    
    /***
     * Convert the longitude into string format of Deg Min Sec
     * @return String
     */
    public String getLonStringDMS() {
    	return (mLongitude >= 0 ? "E" : "W") + getDMS(Math.abs(mLongitude));
    }

    /***
     * Convert the indicated double value into a deg/min/sec string representation 
     * @param frac fractional position value
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
		return String.format(Locale.getDefault(),"%02.0f\u00B0 %02.0f\' %02.2f\"", deg, min, sec);
    }

    /**
     *
     * @param speed how fast
     */
    public void setSpeed(int speed) {
         mSpeed = speed;
    }

    /**
     *
     * @param altitude how high
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

    public int getSatCount() { return mSatCount; }

    public double getHorDil() { return mHorDil; }

    public double getGeoid() { return mGeoid; }
}
