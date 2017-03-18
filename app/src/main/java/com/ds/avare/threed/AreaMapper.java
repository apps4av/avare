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
import com.ds.avare.position.Epsg900913;
import com.ds.avare.shapes.SubTile;
import com.ds.avare.storage.Preferences;
import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.utils.Helper;

/**
 * Created by zkhan on 5/11/16.
 */
public class AreaMapper {

    private GpsParams mGpsParams;

    private SubTile mElevationTile;
    private SubTile mMapTile;

    private float mRatio;

    private boolean mNewMapTile;
    private boolean mNewElevationTile;

    public AreaMapper() {
        mGpsParams = new GpsParams(null);
        mNewMapTile = false;
        mNewElevationTile = false;
        mElevationTile = null;
        mMapTile = null;
        mRatio = 0;
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
        double ynorm = y / ((double)SubTile.DIM) * 2;
        double xnorm = x / ((double)SubTile.DIM) * 2;

        double alt = Helper.findPixelFromElevationNormalized(altitude) * mRatio;

        Vector4d ret = new Vector4d((float)xnorm, (float)ynorm, (float)alt, (float)angle);
        return ret;
    }


    /**
     * X for given lon starting from 0
     * @param lon
     * @return
     */
    public double getXForLon(double lon) {
        if(mMapTile != null) {
            return mMapTile.getOffsetX(lon) + SubTile.DIM / 2;
        }
        return -1;
    }

    /**
     * Y for given lat starting from 0
     * @param lat
     * @return
     */
    public double getYForLat(double lat) {
        if(mMapTile != null) {
            return mMapTile.getOffsetY(lat) + SubTile.DIM / 2;
        }
        return -1;
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


    public void setMapTile(SubTile tile) {
        // Location & OpenGL sync
        synchronized (this) {
            if (mMapTile == null || (!tile.getName().equals(mMapTile.getName()))) {
                mMapTile = tile;
                mNewMapTile = true;
            }
        }
    }

    public SubTile getMapTile() {
        SubTile t;
        // Location & OpenGL sync
        synchronized (this) {
            mNewMapTile = false;
            t = mMapTile;
        }
        return t;
    }

    public void setElevationTile(SubTile tile) {
        // Location & OpenGL sync
        synchronized (this) {
            if (mElevationTile == null || (!tile.getName().equals(mElevationTile.getName()))) {
                mElevationTile = tile;
                /**
                 * Meters per pixel of altitude divided by Meters per pixel on ground for particular zoom.
                 * This will give height to terrain in proportion with ground
                 */
                mRatio = 2.0f * (float)(Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE / Preferences.heightConversion / Epsg900913.getResolution(tile.getZoom())); //2.0 because +z for height while -x to +x for ground
                mNewElevationTile = true;
            }
        }
    }

    public  SubTile getElevationTile() {
        SubTile t;
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

    public GpsParams getGpsParams() {
        return mGpsParams;
    }

    public float getTerrainRatio() {
        return mRatio;
    }
}
