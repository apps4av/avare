/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.flight;

/**
 * Created by zkhan on 12/19/16.
 */

import com.ds.avare.utils.MovingAverage;

/**
 * Find rate information not available in standard GPS location interface.
 */
public class PitotStaticRates {

    private static final double TREND_SECONDS = 6.0; // standard way

    private double mLastAltitude;
    private double mLastSpeed;
    private long   mLastTime;

    private double mDiffAltitude;
    private double mDiffSpeed;

    private MovingAverage mMovingAverageAltitudeChange;
    private MovingAverage mMovingAverageSpeedChange;

    public PitotStaticRates() {
        mLastAltitude = Double.MAX_VALUE;
        mLastSpeed = Double.MAX_VALUE;
        mLastTime = Long.MAX_VALUE;
        mDiffAltitude = 0;
        mDiffSpeed = 0;
        mMovingAverageAltitudeChange = new MovingAverage(30);
        mMovingAverageSpeedChange = new MovingAverage(30);
    }

    public void setParams(double altitude, double airspeed) {
        boolean init = true;
        if(mLastAltitude == Double.MAX_VALUE) {
            mLastAltitude = altitude;
            init = false;
        }
        if(mLastSpeed == Double.MAX_VALUE) {
            mLastSpeed = airspeed;
            init = false;
        }
        if(mLastTime == Long.MAX_VALUE) {
            mLastTime = System.currentTimeMillis();
            init = false;
        }

        /**
         * Init with proper values before calculations
         */
        if(!init) {
            return;
        }

        /*
         * Do calculations
         */
        long TimeDiff = (System.currentTimeMillis() - mLastTime);
        if(TimeDiff == 0) {
            return;
        }
        mDiffAltitude = ((altitude - mLastAltitude) * 1000) / TimeDiff;
        mDiffSpeed = ((airspeed - mLastSpeed) * 1000) / TimeDiff;

        mLastAltitude = altitude;
        mLastSpeed = airspeed;
        mLastTime = System.currentTimeMillis();

        mMovingAverageAltitudeChange.add(mDiffAltitude);
        mMovingAverageSpeedChange.add(mDiffSpeed);
    }

    public double getAltitudeRateOfChange() {
        return mMovingAverageAltitudeChange.get();
    }
    public double getAltitudeTrend() {
        return mLastAltitude + mMovingAverageAltitudeChange.get() * TREND_SECONDS;
    }
    public double getDiffAltitudeTrend() {
        return mMovingAverageAltitudeChange.get() * TREND_SECONDS;
    }
    public double getSpeedTrend() {
        // speed cannot be below 0
        double out = mLastSpeed + mMovingAverageSpeedChange.get() * TREND_SECONDS;
        if(out < 0) {
            out = 0;
        }
        return out;
    }
    public double getDiffSpeedTrend() {
        return mMovingAverageSpeedChange.get() * TREND_SECONDS;
    }
}
