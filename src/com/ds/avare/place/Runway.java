/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
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
    
    public static final float INVALID = -1000;
   
    /**
     * 
     */
    public Runway(String number, String variation, String heading, String lon, String lat) {
        mNumber = number;
        mLon = INVALID;
        mLat = INVALID;
        try {
            mLon = Double.parseDouble(lon);
        }
        catch (Exception e) {
        }
        try {
            mLat = Double.parseDouble(lat);
        }
        catch (Exception e) {
        }
        mHeading = heading;
        mVariation = Helper.parseVariation(variation);
    }

    /**
     * 
     * @return
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
                 * This is an approxmation.
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
}
