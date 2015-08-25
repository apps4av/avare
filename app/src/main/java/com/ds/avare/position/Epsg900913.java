/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.position;

import com.ds.avare.utils.BitmapHolder;

/**
 * A class that finds tile at a give location and zooms
 * @author zkhan
 *
 */
public class Epsg900913 {

    /**
     * To get tile info.
     */
    private static final double SIZE = BitmapHolder.HEIGHT;
	private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137.0 / 2.0;
    private static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137.0 / SIZE;

    private int mTx;
    private int mTy;
    private double mLonL;
    private double mLatU;
    private double mLonR;
    private double mLatD;

    private void findBounds(double zoom) {


        mLonL = metersToLon(xPixelsToMeters(zoom, mTx * SIZE));
        mLonR = metersToLon(xPixelsToMeters(zoom, (mTx + 1) * SIZE));

        mLatD = metersToLat(yPixelsToMeters(zoom, mTy * SIZE));
        mLatU = metersToLat(yPixelsToMeters(zoom, (mTy + 1) * SIZE));

    }

    /**
     * Find tile
     * @param lat
     * @param lon
     * @param zoom
     */
    public Epsg900913(double lat, double lon, double zoom) {
        
        double mx = lonToMeters(lon);
        double my = latToMeters(lat);
        
        double px = xMetersToPixels(zoom, mx);
        double py = yMetersToPixels(zoom, my);

        //tile number
        mTx = xPixelsToTile(px);
        mTy = yPixelsToTile(py);

        findBounds(zoom);
    }

    /**
     * Find tile
     * @param tx
     * @param ty
     * @param zoom
     */
    public Epsg900913(int tx, int ty, double zoom) {
        
        //tile number is knows. Find its bounds
        mTx = tx;
        mTy = ty;
        
        findBounds(getResolution(zoom));
    }


    /*
     * Misc. calls
     */
    public static double getResolution(double zoom) {
        return INITIAL_RESOLUTION / Math.pow(2, zoom);
    }

    public static double latToMeters(double lat) {
        double my = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0 )) / (Math.PI / 180.0);
        return my * ORIGIN_SHIFT / 180.0;
    }

    public static double lonToMeters(double lon) {
        return lon * ORIGIN_SHIFT / 180.0;
    }

    public static double metersToLat(double my) {
        double lat = (my / ORIGIN_SHIFT) * 180.0;
        lat = 180.0 / Math.PI * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return lat;
    }

    public static double metersToLon(double mx) {
        return (mx / ORIGIN_SHIFT) * 180.0;
    }

    public static double xPixelsToMeters(double zoom, double px) {
        return px * getResolution(zoom) - ORIGIN_SHIFT;
    }

    public static double yPixelsToMeters(double zoom, double py) {
        return py * getResolution(zoom) - ORIGIN_SHIFT;
    }

    public static double xMetersToPixels(double zoom, double mx) {
        return (mx + ORIGIN_SHIFT) / getResolution(zoom);
    }

    public static double yMetersToPixels(double zoom, double my) {
        return (my + ORIGIN_SHIFT) / getResolution(zoom);
    }

    public static int xPixelsToTile(double px) {
        return (int)(Math.ceil(px / SIZE) - 1);
    }

    public static int yPixelsToTile(double py) {
        return (int)(Math.ceil(py / SIZE) - 1);
    }

    public static int xMetersToTile(double zoom, double mx) {
        double px = xMetersToPixels(zoom, mx);
        return xPixelsToTile(px);
    }

    public static int yMetersToTile(double zoom, double my) {
        double py = yMetersToPixels(zoom, my);
        return yPixelsToTile(py);
    }

    /*
     * Tile col/rows
     */
    public int getTilex() {
        return mTx;
    }

    public int getTiley() {
        return mTy;
    }

    public double getLonUpperLeft() {
        return mLonL;
    }

    public double getLonLowerLeft() {
        return mLonL;
    }

    public double getLonLowerRight() {
        return mLonR;
    }

    public double getLonUpperRight() {
        return mLonR;
    }

    public double getLonCenter() {
    	return (mLonR + mLonL) / 2.0;
    }

    public double getLatUpperLeft() {
        return mLatU;
    }

    public double getLatUpperRight() {
        return mLatU;
    }

    public double getLatLowerRight() {
        return mLatD;
    }

    public double getLatLowerLeft() {
        return mLatD;
    }

    public double getLatCenter() {
    	return (mLatU + mLatD) / 2.0;
    }

    /*
     * Find longitude of offset from this tile projection
     */
    public static double getLongitudeOf(double ofs, double lon, double zoom) {
        double px = xMetersToPixels(zoom, lonToMeters(lon));
        px += ofs;
        double mx = xPixelsToMeters(zoom, px);
        return metersToLon(mx);
    }

    /*
     * Find longitude of offset from this tile projection
     */
    public static double getLatitudeOf(double ofs, double lat, double zoom) {
        double py = yMetersToPixels(zoom, latToMeters(lat));
        py -= ofs;
        double my = yPixelsToMeters(zoom, py);
        return metersToLat(my);
    }

    /*
     * Find offset X of this given longitude
     */
    public static double getOffsetX(double lon, double lon2, double zoom) {
        double px0 = xMetersToPixels(zoom, lonToMeters(lon2));
        double px1 = xMetersToPixels(zoom, lonToMeters(lon));
        return px0 - px1;
    }

    /*
     * Find offset Y of this given longitude
     */
    public static double getOffsetY(double lat, double lat2, double zoom) {
        double py0 = yMetersToPixels(zoom, latToMeters(lat2));
        double py1 = yMetersToPixels(zoom, latToMeters(lat));
        return py1 - py0;
    }

}
