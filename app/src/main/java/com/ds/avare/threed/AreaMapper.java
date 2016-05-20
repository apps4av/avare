/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.threed;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.shapes.Tile;
import com.ds.avare.storage.Preferences;
import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.utils.BitmapHolder;

/**
 * Created by zkhan on 5/11/16.
 */
public class AreaMapper {

    private GpsParams mGpsParams;

    private Tile mElevationTile;
    private Tile mMapTile;

    private boolean mNewMapTile;
    private boolean mNewElevationTile;

    public AreaMapper() {
        mGpsParams = new GpsParams(null);
        mNewMapTile = false;
        mNewElevationTile = false;
        mElevationTile = null;
        mMapTile = null;
    }

    /**
     * Convert a location to a vector3d
     * @return
     */
    public Vector4d gpsToAxis(double longitude, double latitude, double altitude, double angle) {
        if(mMapTile == null || mElevationTile == null) {
            return new Vector4d(-10, -10, -10, 0); // off screen
        }

        double latc = mMapTile.getLatitude();
        double lonc = mMapTile.getLongitude();
        double lat = latitude;
        double lon = longitude;
        double px = mMapTile.getPx(); //lon per pixel
        double py = mMapTile.getPy(); //lat per pixel
        double dlat = latc - lat;
        double dlon = -(lonc - lon);
        double y = (dlat / py); // pixels from center
        double x = (dlon / px); // pixels from center
        float ynorm = (float)y / BitmapHolder.HEIGHT * 2;
        float xnorm = (float)x / BitmapHolder.WIDTH * 2;

        double alt = altitude / (255 * 25 * Preferences.heightConversion); // altitude in feet 25 meters per pixel

        Vector4d ret = new Vector4d(xnorm, ynorm, (float)alt, (float)angle);
        return ret;
    }

    /**
         * Got from GPS, set
         * @param gpsParams
         */
    public void setGpsParams(GpsParams gpsParams) {
        if(gpsParams.getLatitude() != mGpsParams.getLatitude() ||
                gpsParams.getLongitude() != mGpsParams.getLongitude() ||
                gpsParams.getAltitude() != mGpsParams.getAltitude() ||
                gpsParams.getBearing() != mGpsParams.getBearing()
                ) {
            mGpsParams = gpsParams;
        }
    }


    public void setMapTile(Tile tile) {
        // Location & OpenGL sync
        synchronized (this) {
            if (mMapTile == null || (!tile.getName().equals(mMapTile.getName()))) {
                mMapTile = tile;
                mNewMapTile = true;
            }
        }
    }

    public Tile getMapTile() {
        Tile t;
        // Location & OpenGL sync
        synchronized (this) {
            mNewMapTile = false;
            t = mMapTile;
        }
        return t;
    }

    public void setElevationTile(Tile tile) {
        // Location & OpenGL sync
        synchronized (this) {
            if (mElevationTile == null || (!tile.getName().equals(mElevationTile.getName()))) {
                mElevationTile = tile;
                mNewElevationTile = true;
            }
        }
    }

    public  Tile getElevationTile() {
        Tile t;
        // Location & OpenGL sync
        synchronized (this) {
            mNewElevationTile = false;
            t = mElevationTile;
        }
        return t;
    }

    public  Vector4d getSelfLocation() {
        return gpsToAxis(mGpsParams.getLongitude(), mGpsParams.getLatitude(), mGpsParams.getAltitude(), mGpsParams.getBearing());
    }

    public boolean isMapTileNew() {
        return mNewMapTile;
    }

    public boolean isElevationTileNew() {
        return mNewElevationTile;
    }

    public GpsParams getmGpsParams() {
        return mGpsParams;
    }

}
