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
import com.ds.avare.shapes.Tile;
import com.ds.avare.storage.Preferences;

/**
 * A class that keeps lon/lat pair of what is shown.
 * @author zkhan
 * @author plinel
 *
 */
public class Origin {

    // latitude and longitude of center of screen
    private double mLonScreenCenter;
    private double mLatScreenCenter;
    // latitude and longitude of upper left of screen
    private double mLonScreenLeft;
    private double mLonScreenRight;
    private double mLatScreenTop;
    private double mLatScreenBot;
    private double mZoom;
    private double mScale;

    /**
     * 
     */
    public Origin() {
        
    }
    
    /**
     * 
     * @param params
     * @param pan
     */
    public void update(Tile currentTile, int width, int height, GpsParams params, Pan pan, Scale scale) {
        if(currentTile == null) {
            return;
        }
        mScale = scale.getScaleFactor();
        mZoom = currentTile.getZoom();
        // Get top and center of scrren lat/lon using projection
        mLatScreenCenter = Epsg900913.getLatitudeOf(-pan.getMoveY(), params.getLatitude(), mZoom);
        mLonScreenCenter = Epsg900913.getLongitudeOf(-pan.getMoveX(), params.getLongitude(), mZoom);
        mLatScreenTop = Epsg900913.getLatitudeOf(-pan.getMoveY() - height / 2 / mScale, params.getLatitude(), mZoom);
        mLonScreenLeft = Epsg900913.getLongitudeOf(-pan.getMoveX() - width / 2 / mScale, params.getLongitude(), mZoom);
        mLonScreenRight= mLonScreenLeft - (mLonScreenLeft - mLonScreenCenter) * 2;
        mLatScreenBot = mLatScreenTop - (mLatScreenTop - mLatScreenCenter) * 2;
    }

    public double getLonScreenLeft(){
        return mLonScreenLeft;
    }

    public double getLatScreenTop(){
        return mLatScreenTop;
    }
    /*
     * Return bottom screen latitude
     */
    public double getLatScreenBot() {
        return mLatScreenBot;
    }
    /*
     * Return Right screen longitude
     */
    public double getLonScreenRight() {
        return mLonScreenRight;
    }
    /**
     * 
     * @return
     */
    public double getLongitudeOf(double of) {
        return Epsg900913.getLongitudeOf(of / mScale, mLonScreenLeft, mZoom);
    }
    
    /**
     * 
     * @return
     */
    public double getLatitudeOf(double of) {
        return Epsg900913.getLatitudeOf(of / mScale, mLatScreenTop, mZoom);
    }

    /**
     * double The X offset on the screen of the given longitude
     */
    public double getOffsetX(double lon) {
        return Epsg900913.getOffsetX(mLonScreenLeft, lon, mZoom) * mScale;
    }

    /**
     * double The Y offset on the screen of the given latitude
     */
    public double getOffsetY(double lat) {
        return Epsg900913.getOffsetY(mLatScreenTop, lat, mZoom) * mScale;
    }

    /**
     *
     * @return
     */
    public double getLongitudeCenter() {
        return mLonScreenCenter;
    }

    /**
     *
     * @return
     */
    public double getLatitudeCenter() {
        return mLatScreenCenter;
    }


    /**
     * Find number of pixels in given NM at given latitude
     * @return
     */
    public int getPixelsInNmAtLatitude(double nm, double lat) {

        // 60 miles per degree latitude, half up, half down
        double latl = lat - nm / 60.0 / 2.0;
        double latu = lat + nm / 60.0 / 2.0;

        // return absolute distance
        return (int)Math.round(getOffsetY(latl) - getOffsetY(latu));
    }
}
