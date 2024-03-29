/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
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
package com.ds.avare.orientation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.MovingAverage;

import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by zkhan on 12/19/16.
 */

public class Orientation implements SensorEventListener {

    private OrientationInterface mOrientationCallback;
    private SensorManager mManager;
    private float[] mOrientationTmps;
    private double[] mGravityTemps;
    private boolean mIsAvailable;
    private final double ALPHA = 0.8;
    private MovingAverage mMovingAverageA;

    /**
     * Calls back with orientation
     * @param callback
     */
    public Orientation(OrientationInterface callback) {
        mOrientationCallback = callback;
        mOrientationTmps = new float[3];
        mGravityTemps = new double[3];
        mIsAvailable = false;
        mMovingAverageA = new MovingAverage(20);
    }

    /**
     * Start getting orientation
     */
    public void start() {


        mManager = (SensorManager) StorageService.getInstance().getApplicationContext().getSystemService(SENSOR_SERVICE);
        List<Sensor> typedSensors = mManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
        if ((typedSensors == null) || (typedSensors.size() <= 0)) {
            mIsAvailable = false;
        }
        else {
            mManager.registerListener(this, typedSensors.get(0),
                    SensorManager.SENSOR_DELAY_GAME);
            mIsAvailable = true;
        }
        typedSensors = mManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if ((typedSensors == null) || (typedSensors.size() <= 0)) {
            mIsAvailable = false;
        }
        else {
            mManager.registerListener(this, typedSensors.get(0),
                    SensorManager.SENSOR_DELAY_GAME);
            mIsAvailable = true;
        }
    }

    /**
     * Stop getting orientation
     */
    public void stop() {
        if(null != mManager) {
            mManager.unregisterListener(this);
        }
        mIsAvailable = false;
    }


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            float q[] = new float[16];

            if(0 == msg.what) {
                SensorEvent event = (SensorEvent)msg.obj;

                if (Sensor.TYPE_ACCELEROMETER == event.sensor.getType()) {

                    // Isolate the force of gravity with the low-pass filter.
                    mGravityTemps[0] = ALPHA * mGravityTemps[0] + (1 - ALPHA) * event.values[0];
                    mGravityTemps[1] = ALPHA * mGravityTemps[1] + (1 - ALPHA) * event.values[1];
                    mGravityTemps[2] = ALPHA * mGravityTemps[2] + (1 - ALPHA) * event.values[2];

                    // Remove the gravity contribution with the high-pass filter, return acceleration in X
                    mMovingAverageA.add(event.values[0] - mGravityTemps[0]);
                }

                if (Sensor.TYPE_ROTATION_VECTOR == event.sensor.getType()) {
                    SensorManager.getRotationMatrixFromVector(q, event.values);

                    SensorManager.getOrientation(q, mOrientationTmps);
                    if (mOrientationCallback != null) {
                        mOrientationCallback.onSensorChanged(
                                Math.toDegrees(mOrientationTmps[0]),
                                Math.toDegrees(mOrientationTmps[1]),
                                Math.toDegrees(mOrientationTmps[2]),
                                0,
                                mMovingAverageA.get(), 0, 0, 0, 0, 0);
                    }
                }
            }
        }
    };

    public boolean isSensorAvailable() {
        return mIsAvailable;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Message msg = mHandler.obtainMessage();
        msg.what = 0;
        msg.obj = event;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
