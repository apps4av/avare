/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.position;

/**
 * Simple WGS coordinates
 * @author zkhan
 *
 */
public class Coordinate {

    private double mLat;
    private double mLon;
   
    private boolean mSeparate;
    private int mLeg;
    
    
    public static void setLeg(Coordinate[] coord, int leg) {
    	for (Coordinate c : coord) {
    		c.mLeg = leg;
    	}
    }
    
    public int getLeg() {
    	return mLeg;
    }
    
    public void setSegment(int segment) {
    	mLeg = segment;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public Coordinate(double lon, double lat) {
        mLat = lat;
        mLon = lon;
        mSeparate = false;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public double getLongitude() {
        return mLon;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public double getLatitude() {
        return mLat;
    }

    /**
     * 
     */
    public void makeSeparate()  {
       mSeparate = true; 
    }
    
    /**
     * 
     * @return
     */
    public boolean isSeparate() {
        return mSeparate;
    }
}
