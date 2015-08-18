/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import android.content.Context;

import com.ds.avare.R;
import com.ds.avare.position.Epsg900913;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.NetworkHelper;


/**
 * @author zkhan
 * The class that holds all info about a tile
 */
public class Tile {
	
	// This is from arrays.xml. This should not be retrieved "cleverly" from arrays.xml 
	// because translation will mess it up
	public static final String ELEVATION_INDEX = "6";
	
    /**
     * 
     * Center tile is most important aspect of this database.
     * Everything is relative to this tile, so we store center tiles aspects like
     */
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
    private int mRow;
    private int mCol;
    private double mZoom;
    private String mExtension;
    private Epsg900913 mProj;
    private String mChartIndex;

    /**
     * Common function for all tile constructors.
     */
    private void setup(Preferences pref) {
        mLonUL = mProj.getLonUpperLeft();
        mLatUL = mProj.getLatUpperLeft();
        mLonLR = mProj.getLonLowerRight();
        mLatLR = mProj.getLatLowerRight();
        mLonLL = mProj.getLonLowerLeft();
        mLatLL = mProj.getLatLowerLeft();
        mLonUR = mProj.getLonUpperRight();
        mLatUR = mProj.getLatUpperRight();
        mLonC = mProj.getLonCenter();
        mLatC = mProj.getLatCenter();
        mRow = mProj.getTiley();
        mCol = mProj.getTilex();
        mWidth = BitmapHolder.WIDTH;
        mHeight = BitmapHolder.HEIGHT;
    }
    
    /**
     * 
     * @param t
     * @param row
     * @param col
     */
    public Tile(Context ctx, Preferences pref, Tile t, int col, int row) {
    	mChartIndex = t.mChartIndex;
        mZoom = t.getZoom();
        mExtension = t.getExtension();
    	// Make a new tile from a given center tile, at an offset of row/col
    	Epsg900913 proj = t.getProjection();
    	int tx = proj.getTilex() + col;
    	int ty = proj.getTiley() - row; // row increase up
    	mProj = new Epsg900913(tx, ty, mZoom);
    	setup(pref);
    }

    /**
     * Get a tile for a particular position
     * @param pref
     * @param lon
     * @param lat
     */
    public Tile(Context ctx, Preferences pref, double lon, double lat, double zoom) {
    	mChartIndex = pref.getChartType();
    	/*
    	 * Zoom appropriate to the given chart type.
    	 * Max zoom is specified in arrays.xml, from where we find the 
    	 * max zoom for this tile of this chart type.
    	 * Zoom will go from max to max - zoom of scale
    	 */
        mZoom = Integer.valueOf(ctx.getResources().getStringArray(R.array.ChartMaxZooms)
        		[Integer.valueOf(mChartIndex)]) - zoom;
        /*
         * Extension varies for chart types because some chart have better compression with 
         * one or other type of standard
         */
        mExtension = ctx.getResources().getStringArray(R.array.ChartFileExtesion)
        		[Integer.valueOf(mChartIndex)];
    	mProj = new Epsg900913(lat, lon, mZoom);
    	setup(pref);
    }
    
    /**
     * Get a tile for a particular position for elevation. Use this function for elevation only for AGL
     * @param pref
     * @param lon
     * @param lat
     */
    public Tile(Context ctx, Preferences pref, double lon, double lat) {
    	mChartIndex = ELEVATION_INDEX;
    	/*
    	 * Zoom appropriate to the given chart type.
    	 * Max zoom is specified in arrays.xml, from where we find the 
    	 * max zoom for this tile of this chart type.
    	 */
        mZoom = Integer.valueOf(ctx.getResources().getStringArray(R.array.ChartMaxZooms)
        		[Integer.valueOf(mChartIndex)]); // use max zoom for elevation tiles used for AGL
        /*
         * Extension varies for chart types because some chart have better compression with 
         * one or other type of standard
         */
        mExtension = ctx.getResources().getStringArray(R.array.ChartFileExtesion)
        		[Integer.valueOf(mChartIndex)];
    	mProj = new Epsg900913(lat, lon, mZoom);
    	setup(pref);
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
     * longitude per pixels for this tile
     */
    public double getPx() {
        return(-((mLonUL - mLonUR)  + (mLonLL - mLonLR)) / (mWidth * 2));
    }
    
    /**
     * @return
     * latitude per pixels for this tile
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
     * @param rowm
     * @return Neighboring tile based on its row
     */
    private int getNeighborRow(int rowm) {
    	return mRow + rowm;
    }
    
    /**
     * @param colm
     * @return Neighboring tile based on its col
     */
    private int getNeighborCol(int colm) {
        return  mCol + colm;
    }
    
    /**
     * 
     * @return
     */
    public double getLatitude() {
        return mLatC;
    }
    
    /**
     * 
     * @return
     */
    public double getLongitude() {
        return mLonC;
    }
    
    /**
     * 
     * @return
     */
    public Epsg900913 getProjection() {
    	return mProj;
    }
    
    /**
     * 
     * @return
     */
    public double getZoom() {
    	return mZoom;
    }

    /**
     * 
     * @return
     */
    private String getExtension() {
    	return mExtension;
    }

    /**
     * @return Name of the tile relative to this tile (col, row)
     */
    public String getTileNeighbor(int col, int row) {
    	int coll = getNeighborCol(col);
    	int rowl = getNeighborRow(row);
    	// form /tiles/type/zoom/col/row.jpg
    	String name = "tiles/" + "/" + mChartIndex
    			+ "/" + (int)mZoom +  "/" + coll + "/" + rowl + mExtension; 
        return(name);
    }

    /**
     * @return Name of the tile for db zip name
     */
    public String getTileDbName() {
        // form type/zoom/col/row
        String name = mChartIndex + "/" + (int)mZoom +  "/" + getNeighborCol(0) + "/" + getNeighborRow(0);
        return(name);
    }

    /**
     *
     * @return
     */
    public String getChartIndex() {
        return mChartIndex;
    }

    /**
     * @return Name of the tile
     */
    public String getName() {
    	return getTileNeighbor(0, 0);
    }

}
