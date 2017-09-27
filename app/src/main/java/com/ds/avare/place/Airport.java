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

import java.util.LinkedHashMap;

import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;


/**
 * 
 * @author zkhan
 *
 */
public class Airport {

    private String mId;
    private double mLon;
    private double mLat;
    private double mVariation;
    private Projection mProj;
    private String mName;
    private String mFuel;
    private String mElevation;
    private String mLongestRunway;
    private double mHeight;
   
    /**
     * 
     * @param id
     * @param lon
     * @param lat
     * @param cLon
     * @param cLat
     */
    public Airport(LinkedHashMap<String, String> params, double cLon, double cLat) {
        mLon = Double.parseDouble(params.get(LocationContentProviderHelper.LONGITUDE));
        mLat = Double.parseDouble(params.get(LocationContentProviderHelper.LATITUDE));;
        mId = params.get(LocationContentProviderHelper.LOCATION_ID);
        mName = params.get(LocationContentProviderHelper.FACILITY_NAME);
        mFuel = params.get(LocationContentProviderHelper.FUEL_TYPES);
        mElevation = params.get("Elevation");
        mVariation = Helper.parseVariation(params.get(LocationContentProviderHelper.MAGNETIC_VARIATION));
        mLongestRunway = "";
        mHeight = 0;
        
        mProj = new Projection(cLon, cLat, mLon, mLat);
    }

    /**
     * 
     * @param cLon
     * @param cLat
     */
    public void updateLocation(double cLon, double cLat) {
        mProj = new Projection(cLon, cLat, mLon, mLat);
    }
    
    /**
     * 
     * @return
     */
    public String getId() {
        return mId;
    }

    /**
     * 
     * @return
     */
    public double getDistance() {
        return mProj.getDistance();
    }

    /**
     * 
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * 
     * @return
     */
    public double getBearing() {
        return mProj.getBearing();
    }

    /**
     * 
     * @return
     */
    public double getVariation() {
        return mVariation;
    }

    /**
     * 
     * @return
     */
    public String getFuel() {
        return mFuel;
    }

    /**
     * 
     * @return
     */
    public String getElevation() {
        return mElevation;
    }

    /**
     * 
     * @return
     */
    public String getLongestRunway() {
        return mLongestRunway;
    }

    /**
     * 
     * @return
     */
    public void setLongestRunway(String runway) {
        mLongestRunway = runway;
    }
    
    /**
     * Set the height for required glide ratio to this airport in feet(altitude) / km,nm,mi
     * @param altitude
     */
    public void setHeight(double altitude) {
        try {
            mHeight = altitude - Double.parseDouble(getElevation().replace("ft", ""));
        }
        catch(Exception e) {
        }        
    }

    /**
     * @param altitude
     */
    public boolean canGlide(Preferences mPref) {
        /*
         * Height * glide ratio (distance feet / height feet) = distance
         */
        double radius = mHeight * mPref.getGlideRatio() / Preferences.feetConversion;
        if(radius > getDistance()) {
            /*
             * This is in glide distance
             */
            return true;
        }
        
        return false;
    }

}
