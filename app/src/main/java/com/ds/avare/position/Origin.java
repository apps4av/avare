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

import com.ds.avare.gps.GpsParams;

/**
 * A class that keeps lon/lat pair of what is shown.
 * @author zkhan
 *
 */
public class Origin {

    private double mLonC;
    private double mLatC;
    private double mLonL;
    private double mLonR;
    private double mLatU;
    private double mLatL;
    private double mScaleX;
    private double mScaleY;
    
    /**
     * 
     */
    public Origin() {
        
    }
    
    /**
     * 
     * @param params
     * @param scale
     * @param pan
     * @param px
     * @param py
     * @param width
     * @param height
     */
    public void update(GpsParams params, Scale scale, Pan pan, double px, double py,
            int width, int height) {

        /*
         * Calculate all coordinates of the chart under view
         */
        mScaleX = px / scale.getScaleFactor();
        mScaleY = py / scale.getScaleCorrected();
                
        double loc0x =
            pan.getMoveX() * scale.getScaleFactor();
        
        double loc0y = 
            pan.getMoveY() * scale.getScaleCorrected();
        
        mLonC = params.getLongitude() - loc0x * mScaleX;
        mLatC = params.getLatitude() - loc0y * mScaleY; 
        
        mLonL = mLonC - (width / 2) * mScaleX;
        mLonR = mLonC + (width / 2) * mScaleX;

        mLatU = mLatC - (height / 2) * mScaleY;
        mLatL = mLatC + (height / 2) * mScaleY;
    }

    /**
     * 
     * @return
     */
    public double getLongitudeOf(double of) {
        return mLonL + of * mScaleX;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitudeOf(double of) {
        return mLatU + of * mScaleY;
    }

    /**
     * 
     * @return
     */
    public double getLongitudeCenter() {
        return mLonC;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitudeCenter() {
        return mLatC;
    }

    /**
     * 
     * @return
     */
    public double getLongitudeLeft() {
        return mLonL;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitudeUpper() {
        return mLatU;
    }

    /**
     * 
     * @return
     */
    public double getLongitudeRight() {
        return mLonR;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitudeLower() {
        return mLatL;
    }
    
    /**
     * @param
     * double longitude
     * @return
     * double The X offset on the screen of the given longitude
     */
    public double getOffsetX(double lon) {
        double diff = lon - mLonL;
        return diff / mScaleX;
    }

    /**
     * @param
     * double latitude
     * @return
     * double The Y offset on the screen of the given latitude
     */
    public double getOffsetY(double lat) {
        double diff = lat - mLatU;
        return diff / mScaleY;
    }

    /** Is the position passed to the function within the range of the 
    * current display panel
    *
    * @param c The coordinate to check against the current display frame
    * @return true if the value is on the display, false if not
    */
    public boolean isInDisplayRange(Coordinate c){
    	if(c.getLongitude() < mLonL) {
    		return false;
        }
    	
    	if(c.getLongitude() > mLonR) {
    		return false;
        }
    	
    	if(c.getLatitude() > mLatU) {
    		return false;
        }
    	
    	if(c.getLatitude() < mLatL) {
    		return false;
        }

    	return true;
    }
}
