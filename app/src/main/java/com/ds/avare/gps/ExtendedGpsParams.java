/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.gps;

/**
 * Created by zkhan on 12/19/16.
 */

/**
 * Find rate information not available in standard GPS location interface.
 */
public class ExtendedGpsParams {

    private static final double TREND_SECONDS = 6.0; // standard way

    private double mLastAltitude;
    private double mLastBearing;
    private double mLastSpeed;
    private long   mLastTime;

    private double mDiffAltitude;
    private double mDiffBearing;
    private double mDiffSpeed;

    public ExtendedGpsParams() {
        mLastAltitude = Double.MAX_VALUE;
        mLastBearing = Double.MAX_VALUE;
        mLastSpeed = Double.MAX_VALUE;
        mLastTime = Long.MAX_VALUE;
        mDiffAltitude = 0;
        mDiffBearing = 0;
        mDiffSpeed = 0;
    }

    /**
     * Warning... do to call faster than 1 second per call.
     * @param p
     */
    public void setParams(GpsParams p) {
        boolean init = true;
        if(mLastAltitude == Double.MAX_VALUE) {
            mLastAltitude = p.getAltitude();
            init = false;
        }
        if(mLastBearing == Double.MAX_VALUE) {
            mLastBearing = p.getBearing();
            init = false;
        }
        if(mLastSpeed == Double.MAX_VALUE) {
            mLastSpeed = p.getSpeed();
            init = false;
        }
        if(mLastTime == Long.MAX_VALUE) {
            mLastTime = p.getTime();
            init = false;
        }

        /**
         * Init with proper values before calculations
         */
        if(!init) {
            return;
        }

        if(null == p ) {
            return;
        }

        /*
         * Do calculations
         */
        long TimeDiff = p.getTime() - mLastTime;
        if(0 == TimeDiff) {
            return;
        }
        mDiffAltitude = ((p.getAltitude() - mLastAltitude) * 1000) / TimeDiff;
        mDiffBearing = ((p.getBearing() - mLastBearing) * 1000) / TimeDiff;
        mDiffSpeed = ((p.getSpeed() - mLastSpeed) * 1000) / TimeDiff;

        mLastAltitude = p.getAltitude();
        mLastSpeed = p.getSpeed();
        mLastBearing = p.getBearing();
        mLastTime = p.getTime();
    }

    public double getBearingRateOfChange() {
        return mDiffBearing;
    }
    public double getAltitudeRateOfChange() {
        return mDiffAltitude;
    }
    public double getSpeedRateOfChange() {
        return mDiffSpeed;
    }
    public double getBearingTrend() {
        if(mLastBearing == Double.MAX_VALUE) {
            return 0;
        }
        return mLastBearing + mDiffBearing * TREND_SECONDS;
    }
    public double getAltitudeTrend() {
        if(mLastAltitude == Double.MAX_VALUE) {
            return 0;
        }
        return mLastAltitude + mDiffAltitude * TREND_SECONDS;
    }
    public double getSpeedTrend() {
        if(mLastSpeed == Double.MAX_VALUE) {
            return 0;
        }
        return mLastSpeed + mDiffSpeed * TREND_SECONDS;
    }
}
