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
import com.ds.avare.threed.data.Vector3d;

/**
 * Created by zkhan on 5/11/16.
 */
public class AreaMapper {

    private GpsParams mGpsParams;
    private GpsParams mGpsParamsLast;

    private Tile mElevationTile;
    private Tile mMapTile;


    private boolean mNewMapTile;
    private boolean mNewElevationTile;

    private float count = 0;

    public AreaMapper() {
        mGpsParams = new GpsParams(null);
        mGpsParamsLast = new GpsParams(null);
        mNewMapTile = false;
        mNewElevationTile = false;
        mElevationTile = null;
        mMapTile = null;
        count = 0;
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
            mGpsParamsLast = mGpsParams;
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

    public boolean isMapTileNew() {
        return mNewMapTile;
    }

    public boolean isElevationTileNew() {
        return mNewElevationTile;
    }

    public Vector3d getCameraVectorLookAt() {
        Vector3d cameraVectorLookAt  = new Vector3d(0.0f, -2.0f + count / 100.f, 1.0f);
        count += 1;
        if(count == 200) {
            count = 0;
        }
        return cameraVectorLookAt;
    }

    public Vector3d getCameraVectorPosition() {
        Vector3d cameraVectorPosition = new Vector3d(0.0f, -2.1f + count / 100.f, 1.0f);
        return cameraVectorPosition;
    }

}
