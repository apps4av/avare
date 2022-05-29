/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.connections;

import android.content.Context;

import com.ds.avare.place.Destination;
import com.ds.avare.utils.GenericCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author rasii, zkhan
 *
 */
public class GPSSimulatorConnection extends Connection {
    private static double KNOTS_TO_MS = 0.514444f;
    private static double FEET_TO_METERS = 0.3048f;

    private static GPSSimulatorConnection mConnection;

    private double mBearing = 45;
    private double mSpeed = 150f * KNOTS_TO_MS;
    private double mAltitude = 5000 * FEET_TO_METERS;
    private double mLon = -94.7376f;
    private double mLat = 38.8476f;
    private double mLonInit = -94.7376f;
    private double mLatInit = 38.8476f;

    private boolean mLandAtDest = false;
    private boolean mFlyToDest = false;
    private boolean mDestValid = false;
    private double mDestDistance = -1;
    private double mDestBearing = -1;
    private double mDestElevation = 0;

    /**
     *
     *
     */
    private GPSSimulatorConnection() {
        super("GPS Simulator Input");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {

                double bearing = mBearing;

                /*
                 * Start the GPS Simulator
                 */
                while (isRunning()) {

                    if(mService != null) {

                        Destination d = mService.getDestination();
                        if(d != null) {
                            mDestBearing = d.getBearing();
                            mDestDistance = d.getDistance();
                            mDestElevation = d.getElevation() * FEET_TO_METERS;
                            mDestValid = true;
                        }
                        else {
                            mDestValid = false;
                        }

                    }
                    double time = 1; // in seconds
                    if (isStopped()) {
                        break;
                    }

                    try {
                        Thread.sleep((long) (time * 1000));
                    } catch (Exception e) {

                    }

                    if (mFlyToDest && mDestValid) {
                        // Just keep last bearing if we're getting really close, we don't want
                        // crazy swings at destination passage
                        if (mDestDistance > 0.1) {
                            bearing = mDestBearing;
                        }
                    } else {
                        bearing = mBearing;
                    }

                    double speed = mSpeed;
                    double altitude = mAltitude;

                    // See if we're supposed to land, only do it at the final destination
                    // This is pretty simple logic, but it's something...
                    if (mLandAtDest && mDestValid) {
                        // Get close then stop
                        if (mDestDistance < 10) {
                            // This is somewhat random, but will simulate a descent
                            altitude = mDestElevation + ((mAltitude - mDestElevation) * (mDestDistance / 10.0));

                            // Slow down as we get close, stop when we're really close
                            if (mDestDistance < 0.1) {
                                speed = 0;
                                altitude = mDestElevation;
                            } else if (mDestDistance < 1) {
                                speed = Math.max(mSpeed * mDestDistance / 2.0, 20 * KNOTS_TO_MS);
                            } else if (mDestDistance < 5) {
                                speed = mSpeed * ((mDestDistance + 5) / 10.0);
                            }
                        }
                    }

                    double earthRadius = 6371000f; // Earth Radius in meters
                    double distance = speed * time;
                    double degToRad = Math.PI / 180.0;
                    double radToDeg = 180.0 / Math.PI;
                    double lat2 = Math.asin(Math.sin(degToRad * mLat) *
                            Math.cos(distance / earthRadius) +
                            Math.cos(degToRad * mLat) *
                                    Math.sin(distance / earthRadius) *
                                    Math.cos(degToRad * bearing));
                    double lon2 = degToRad * mLon + Math.atan2(Math.sin(degToRad * bearing) *
                                    Math.sin(distance / earthRadius) *
                                    Math.cos(degToRad * mLat),
                            Math.cos(distance / earthRadius) -
                                    Math.sin(degToRad * mLat) * Math.sin(lat2));

                    // Now convert radians to degrees
                    mLat = lat2 * radToDeg;
                    mLon = lon2 * radToDeg;

                      /*
                       * Make a GPS location message
                       */
                    JSONObject object = new JSONObject();
                    try {
                        object.put("type", "ownship");
                        object.put("longitude", mLon);
                        object.put("latitude", mLat);
                        object.put("speed", speed);
                        object.put("bearing", bearing);
                        object.put("altitude", altitude);
                        object.put("time", System.currentTimeMillis());
                    } catch (JSONException e1) {
                        continue;
                    }

                    sendDataToHelper(object.toString());
                }

                return null;
            }
        });
    }

    /**
     *
     * @return
     */
    public static GPSSimulatorConnection getInstance(Context ctx) {
        if(null == mConnection) {
            mConnection = new GPSSimulatorConnection();
        }
        return mConnection;
    }

    @Override
    public List<String> getDevices() {
        return new ArrayList<String>();
    }

    @Override
    public String getConnDevice() {
        return "";
    }

    @Override
    public void disconnect() {
        disconnectConnection();
    }

    @Override
    public boolean connect(String to, boolean securely) {
        String data[] = to.split(",");
        try {
            mLatInit = Double.parseDouble(data[0]);
            mLonInit = Double.parseDouble(data[1]);
            mLat = mLatInit;
            mLon = mLonInit;
            mBearing = Double.parseDouble(data[2]);
            mSpeed = Double.parseDouble(data[3]) * KNOTS_TO_MS;
            mAltitude = Double.parseDouble(data[4]) * FEET_TO_METERS;
            mFlyToDest = Boolean.parseBoolean(data[5]);
            mLandAtDest = Boolean.parseBoolean(data[6]);
        }
        catch (Exception e) {
            return false;
        }
        return connectConnection();
    }

    @Override
    public void write(byte[] aData) {

    }
}
