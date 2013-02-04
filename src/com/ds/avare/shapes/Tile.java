/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
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
               double lonc, double latc) {
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
        int opts[] = new int[2];
        BitmapHolder.getBitmapOptions(pref, name, opts);
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
     * @return
     */
    public double getPy() {
        return(-((mLatUL - mLatLL)  + (mLatUR - mLatLR)) / (mHeight * 2));
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
        
        /*
         * This is all magic. Check databse specificaiton.
         * Tiles are stored row/col as:
         * 0/row/master_row_col where row, col have leading zeros
         */
        String [] tokens = mName.split("[/_.]");
        
        try {
            /*
             * Do not want to crash if weird situation arises.
             */
            int row = (Integer.parseInt(tokens[tokens.length - 3]) + rowm);
            int col = (Integer.parseInt(tokens[tokens.length - 2]) + colm);
            int lenr = tokens[tokens.length - 3].length();
            int lenc = tokens[tokens.length - 2].length();
            
            String rformatted = String.format("%0" + lenr + "d", row);
            String cformatted = String.format("%0" + lenc + "d", col);
            return(tokens[0] + "/" + tokens[1] + "/" + row + "/" + tokens[3] 
                    + (tokens[4].equals("c") ? "_c" : "") + "_" + rformatted + "_" + cformatted + ".jpeg");       
        }
        catch (Exception e) {
            
        }
        return("error.jpeg");
    }
    
    public double getLatitude() {
        return mLatC;
    }
    
    public double getLongitude() {
        return mLonC;
    }
    
}
