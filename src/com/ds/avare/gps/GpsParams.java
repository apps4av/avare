/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com), 
Jesse McGraw (jlmcgraw@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.gps;

import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;

import android.hardware.GeomagneticField;
import android.location.Location;

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
    private float mDeclination;
    
    
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
            mScale.setScaleAt(mLatitude);
            mDeclination = 0;
            return;
        }
        
        mSpeed = location.getSpeed() * Preferences.distanceConversion; // ms / sec to knot / hr;
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
        mScale.setScaleAt(mLatitude);
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
}
