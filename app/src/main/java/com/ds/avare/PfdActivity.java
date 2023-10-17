/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.ds.avare.flight.PitotStaticRates;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.orientation.OrientationInterface;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.PfdView;

/**
 * @author zkhan
 */
public class PfdActivity extends BaseActivity {

    private PfdView mPfdView;

    private boolean mRollReverse;

    private PitotStaticRates mPitotStaticRates;


    /**
     * GPS calls
     */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {

            if(mService.getGpsParams() == null || mService.getExtendedGpsParams() == null) {
                return;
            }
            double bearing = 0;
            double cdi = 0;
            double vdi = 0;

            if(mService.getVNAV() != null) {
                vdi = mService.getVNAV().getGlideSlope();
            }
            if(mService.getCDI() != null) {
                cdi = mService.getCDI().getDeviation();
                if (!mService.getCDI().isLeft()) {
                    cdi = -cdi;
                }
            }
            if(mService.getDestination() != null) {
                bearing = mService.getDestination().getBearing();
            }
            mPfdView.setParams(mService.getGpsParams(), bearing, cdi, vdi);
            mPfdView.postInvalidate();
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }

    };

    private OrientationInterface mOrientationInfc = new OrientationInterface() {

        @Override
        public void onSensorChanged(double yaw, double pitch, double roll, double slip, double acceleration, double yawrate, double aoa, double airspeed, double altitude, double vsi) {
            mPitotStaticRates.setParams(altitude, airspeed);
            mPfdView.setYaw((float)yaw);
            mPfdView.setPitch((float)pitch);
            mPfdView.setRoll(mRollReverse ? -(float)roll : (float)roll);
            mPfdView.setSlip((float)slip);
            mPfdView.setAcceleration((float)acceleration);
            mPfdView.setYawRate((float)yawrate);
            mPfdView.setAoa((float)aoa);
            mPfdView.setAirspeed((float)airspeed);
            mPfdView.setAltitude((float)altitude);
            mPfdView.setVsi((float)vsi);
            mPfdView.setSpeedTrend((float)mPitotStaticRates.getDiffSpeedTrend());
            mPfdView.setAltitudeChange((float)mPitotStaticRates.getDiffAltitudeTrend());
            mPfdView.postInvalidate();

        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.pfd, null);
        setContentView(view);

        mPitotStaticRates = new PitotStaticRates();
        mPfdView = (PfdView) view.findViewById(R.id.pfd_view);


    }
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        mService.registerGpsListener(mGpsInfc);
        mService.registerOrientationListener(mOrientationInfc);
        mRollReverse = mPref.reverseRollInAhrs();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        mService.unregisterGpsListener(mGpsInfc);
        mService.unregisterOrientationListener(mOrientationInfc);
    }

}