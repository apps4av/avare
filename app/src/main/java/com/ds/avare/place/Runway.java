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

import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 */
public class Runway {

    private String mNumber;
    private String mHeading;
    private double mVariation;
    private double mLon;
    private double mLat;
    private String mSurface;
    private String mElevation;
    private String mLights;
    private String mThreshold;
    private String mWidth;
    private String mLength;
    private String mPattern;
    private String mILS;
    private String mVGSI;
    
    public static final float INVALID = -1000;
   
    /**
     * 
     */
    public Runway(String number) {
        mNumber = number;
        mLon = INVALID;
        mLat = INVALID;

    }

    /**
     * 
     * @return
     */
    public String getSurface() {
        return mSurface;
    }

    /**
     * 
     * @return
     */
    public String getThreshold() {
        return mThreshold;
    }

    /**
     * 
     * @return
     */
    public String getWidth() {
        return mWidth;
    }

    /**
     * 
     * @return
     */
    public String getLength() {
        return mLength;
    }

    /**
     * 
     * @return
     */
    public String getPattern() {
        return mPattern;
    }

    /**
     * 
     * @return
     */
    public String getLights() {
        return mLights;
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
     * string Runway Number
     */
    public String getNumber() {
        return mNumber;
    }

    /**
     * 
     * @return
     */
    public float getTrue() {
        float ret = INVALID;

        /*
         * Get true heading of runway if given
         */
        try {
            ret = Integer.parseInt(mHeading);
        }
        catch (Exception e) {
            
        }
        
        
        /*
         * Nothing found in True, now parse number of runway and add variation.
         */
        if(INVALID == ret) {
            try {
                /*
                 * This is an approximation.
                 */
                ret = (float)Integer.parseInt(mNumber.replace("L", "").replace("R", "").replace("C", "")) * 10.f - (float)mVariation;
            }
            catch (Exception e) {
                
            }
    
        }
        return ret;
    }

    /**
     * 
     * @return
     */
    public double getLongitude() {
        return mLon;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitude() {
        return mLat;
    }
    
    /**
     * 
     * @return
     */
    public String getILS() {
        return mILS;
    }

    /**
     * 
     * @return
     */
    public String getVGSI() {
        return mVGSI;
    }

    /**
     * 
     * @return
     */
    public void setSurface(String surface) {
        mSurface = surface;
    }

    /**
     * 
     * @return
     */
    public void setThreshold(String threshold) {
        mThreshold = threshold;
    }

    /**
     * 
     * @return
     */
    public void setWidth(String width) {
        mWidth = width;
    }

    /**
     * 
     * @return
     */
    public void setLength(String length) {
        mLength = length;
    }

    /**
     * 
     * @return
     */
    public void setPattern(String pattern) {
        mPattern = pattern;
    }

    /**
     * 
     * @return
     */
    public void setLights(String lights) {
        mLights = lights;
    }

    /**
     * 
     * @return
     */
    public void setElevation(String elevation) {
        mElevation = elevation;
    }

    /**
     * 
     * @return
     */
    public void setHeading(String heading) {
        mHeading = heading;
    }

    /**
     * 
     * @return
     */
    public void setLongitude(String lon) {
        try {
            mLon = Double.parseDouble(lon);
        }
        catch (Exception e) {
        }
    }

    /**
     * 
     * @return
     */
    public void setLatitude(String lat) {
        try {
            mLat = Double.parseDouble(lat);
        }
        catch (Exception e) {
        }
    }
    
    /**
     * 
     * @return
     */
    public void setVariation(String variation) {
        mVariation = Helper.parseVariation(variation);
    }
    
    /**
     * 
     * @return
     */
    public void setILS(String ils) {
        mILS = ils;
    }

    /**
     * 
     * @return
     */
    public void setVGSI(String vgsi) {
        mVGSI = vgsi;
    }

}
