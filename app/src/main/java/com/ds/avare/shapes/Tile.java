/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;


/**
 * @author zkhan
 * The class that holds all info about a tile
 */
public class Tile {
    /**
     * 
     * Center tile is most important aspect of this database.
     * Everything is relative to this tile, so we store center tiles aspects like
     */
    private String mName;
    private double mLonUL;
    private double mLonLL;
    private double mLonUR;
    private double mLonLR;
    private double mLatUL;
    private double mLatLL;
    private double mLatUR;
    private double mLatLR;
    private double mLonC;
    private double mLatC;
    private double mWidth;
    private double mHeight;
    private String mChart;

    /**
     * 
     */
    public Tile() {
        mName = "";
        mLonUL = 0;
        mLonLL = 0;
        mLonUR = 0;
        mLonLR = 0;
        mLatUL = 0;
        mLatLL = 0;
        mLatUR = 0;
        mLatLR = 0;
        mLonC = 0;
        mLatC = 0;
        mWidth = BitmapHolder.WIDTH;
        mHeight = BitmapHolder.HEIGHT;
    }

    public Tile(
               Preferences pref,
               String name, 
               double lonul, double latul, double lonll, double latll,
               double lonur, double latur, double lonlr, double latlr,
               double lonc, double latc,
               String chart) {
        mName = name;
        mLonUL = lonul;
        mLonLL = lonll;
        mLonUR = lonur;
        mLonLR = lonlr;
        mLatUL = latul;
        mLatLL = latll;
        mLatUR = latur;
        mLatLR = latlr;
        mLonC = lonc;
        mLatC = latc;
        mChart = chart;
        int opts[] = new int[2];
        BitmapHolder.getTileOptions(name, pref, opts);
        mWidth = opts[0];
        mHeight = opts[1];
    }

    /**
     * Find if give location is within this tile
     * @param lon
     * @param lat
     * @return
     */
    public boolean within(double lon, double lat) {
        return (
                (mLonUL <= lon) && (mLonLL <= lon) && (mLonUR >= lon) && (mLonLR >= lon) &&
                (mLatUL >= lat) && (mLatUR >= lat) && (mLatLL <= lat) && (mLatLR <= lat)
               );          
    }

    /**
     * @return
     */
    public double getPx() {
        return(-((mLonUL - mLonUR)  + (mLonLL - mLonLR)) / (mWidth * 2));
    }
    
    /**
     * 
     * @return
     */
    public String getChart() {
        return mChart;
    }
    
    /**
     * @return
     */
    public double getPy() {
        return(-((mLatUL - mLatLL)  + (mLatUR - mLatLR)) / (mHeight * 2));
    }

    /**
     * Find offsetTopX from top of tile
     * @param lon
     * @return
     */
    public double getOffsetTopX(double lon) {
        double px = getPx();
        
        if(px != 0) {
            return(lon - mLonUL) / px;
        }
        else {
            return(0);
        }        
    }

    /**
     * Find offsetTopY from top of tile
     * @param lon
     * @return
     */
    public double getOffsetTopY(double lat) {
        double py = getPy();
        
        if(py != 0) {
            return(lat - mLatUL) / py;
        }
        else {
            return(0);
        }        
    }

    /**
     * Find offsetX from center of tile
     * @param lon
     * @return
     */
    public double getOffsetX(double lon) {

        double px = getPx();
        
        if(px != 0) {
            return(lon - mLonC) / px - (BitmapHolder.WIDTH / 2 - mWidth / 2);
        }
        else {
            return(0);
        }
    }
    
    /**
     * Find offsetY from center of tile
     * @param lon
     * @param lat
     * @return
     */
    public double getOffsetY(double lat) {

        double py = getPy();
        
        if(py != 0) {
            return (lat - mLatC) / py - (BitmapHolder.HEIGHT / 2 - mHeight / 2);
        }
        else {
            return(0);
        }
    }

    /**
     * @return Name of this tile
     */
    public String getName() {
        return(mName);
    }
    
    /**
     * @param rowm
     * @param colm
     * @return Neighboring tile based on its row, col
     */
    public String getNeighbor(int rowm, int colm) {
        
        String ret = Helper.incTileName(mName, rowm, colm);
        if(null == ret) {
            return("error.jpeg");
        }
        return ret;
    }
    
    public double getLatitude() {
        return mLatC;
    }
    
    public double getLongitude() {
        return mLonC;
    }
    
}
