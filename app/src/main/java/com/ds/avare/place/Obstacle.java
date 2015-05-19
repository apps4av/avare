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

/**
 * @author zkhan
 *
 */
public class Obstacle {

    private double mLon;
    private double mLat;
    private int mHeight; 

    /*
     * params that make this obstacle dangerous
     * Lon/lat degrees radius
     */
    public static final float RADIUS = 0.2f;	// .1 degree = ~5nm
    public static final float HEIGHT_BELOW = 200;

    /**
     * 
     * @param longitude
     * @param latitude
     * @param height
     */
    public Obstacle(double longitude, double latitude, int height) {
        
        mLon = longitude;
        mLat = latitude;
        mHeight = height; 
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
    public double getLongitude() {
        return mLon;
    }

    /**
     * 
     * @return
     */
    public int getHeight() {
        return mHeight;
    }
}
