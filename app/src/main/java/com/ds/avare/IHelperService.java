/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.ds.avare.instruments.CDI;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class exposes the remote service to the client.
 * The client will be the Avare Helper, sending data to Avare
 * author zkhan
 */
public class IHelperService extends Service {

    private StorageService mService;
    private JSONObject mGeoAltitude;

    public static final int MIN_ALTITUDE = -1000;

    /**
     * We need to bind to storage service to do anything useful 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mGeoAltitude = null;
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate() {       
        mService = null;
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        getApplicationContext().unbindService(mConnection);
        mService = null;
    }

    /**
     * 
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    /**
     * 
     */
    private final IHelper.Stub mBinder = new IHelper.Stub() {
        @Override
        public void sendDataText(String text) {
            
            /*
             * This is where we are all messages
             * All messages are comma separated
             */
            Message msg = mHandler.obtainMessage();
            msg.obj = text;
            mHandler.sendMessage(msg);
        }

        @Override
        /**
         * 
         */
        public String recvDataText() {
            Location l = mService.getLocationBlocking();
            JSONObject object = new JSONObject();
            try {
                object.put("type", "ownship");
                object.put("longitude", (double)l.getLongitude());
                object.put("latitude", (double)l.getLatitude());
                object.put("speed", (double)l.getSpeed());
                object.put("bearing", (double)l.getBearing());
                object.put("altitude", (double)l.getAltitude());
                object.put("time", l.getTime());
                
                Destination d = mService.getDestination();
                Plan p = mService.getPlan();
                CDI c = mService.getCDI();
                double distance = 0;
                double bearing = 0;
                double lon = 0;
                double lat = 0;
                double elev = 0;
                double idNext = -1;
                double idOrig = -1;
                double deviation = 0;
                double bearingTrue = 0;
                double bearingMagnetic = 0;

                // If destination set, send how to get there (for autopilots).
                if(d != null) {
                    distance = d.getDistance();
                    bearing = d.getBearing();
                    lon = d.getLocation().getLongitude();
                    lat = d.getLocation().getLatitude();
                    elev = d.getElevation();
                    if(p != null) {
                        idNext = p.findNextNotPassed();
                        idOrig = idNext - 1;
                        bearingTrue = p.getBearing((int)idOrig, (int)idNext);
                        bearingMagnetic = Helper.getMagneticHeading(bearingTrue, d.getDeclination());
                    }
                    if(c != null) {
                        deviation = c.getDeviation();
                        if(!c.isLeft()) {
                            deviation = -deviation;
                        }
                    }
                }
                object.put("destDistance", distance);
                object.put("destBearing", bearing);
                object.put("destLongitude", lon);
                object.put("destLatitude", lat);
                object.put("destId", idNext);
                object.put("destOriginId", idOrig);
                object.put("destDeviation", deviation);
                object.put("destElev", elev);
                object.put("bearingTrue", bearingTrue);
                object.put("bearingMagnetic", bearingMagnetic);
            } catch (JSONException e1) {
                return null;
            }
            return object.toString();
        }
    };
    
    /**
     * Posting a location hence do from UI thread
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {            

            String text = (String)msg.obj;
            
            if(text == null || mService == null) {
                return;
            }
            
            /*
             * Get JSON
             */
            try {
                JSONObject object = new JSONObject(text);

                String type = object.getString("type");
                if(type == null) {
                    return;
                }
                else if(type.equals("traffic")) {
                    mService.getTrafficCache().putTraffic(
                            object.getString("callsign"),
                            object.getInt("address"),
                            (float)object.getDouble("latitude"),
                            (float)object.getDouble("longitude"),
                            object.getInt("altitude"),
                            (float)object.getDouble("bearing"),
                            (int)object.getInt("speed"),
                            Helper.getMillisGMT()
                            /*XXX:object.getLong("time")*/);
                }
                else if(type.equals("geoaltitude")) {
                    mGeoAltitude = object;
                }
                else if(type.equals("ownship")) {
                    Location l = new Location(LocationManager.GPS_PROVIDER);
                    l.setLongitude(object.getDouble("longitude"));
                    l.setLatitude(object.getDouble("latitude"));
                    l.setSpeed((float) object.getDouble("speed"));
                    l.setBearing((float) object.getDouble("bearing"));
                    l.setTime(object.getLong("time"));

                    // Choose most appropriate altitude. This is because people fly all sorts
                    // of equipment with or without altitudes
                    // convert all altitudes in feet
                    double pressureAltitude = object.getDouble("altitude") * Preferences.heightConversion;
                    double deviceAltitude = MIN_ALTITUDE;
                    double geoAltitude = MIN_ALTITUDE;
                    // If geo altitude from adsb available, use it if not too old
                    if(mGeoAltitude != null) {
                        long t1 = object.getLong("time");
                        long t2 = mGeoAltitude.getLong("time");
                        if((t1 - t2) < 10000) { // 10 seconds
                            geoAltitude = mGeoAltitude.getDouble("altitude") * Preferences.heightConversion;
                            if(geoAltitude < MIN_ALTITUDE) {
                                geoAltitude = MIN_ALTITUDE;
                            }
                        }
                    }
                    // If geo altitude from device available, use it if not too old
                    if(mService.getGpsParams() != null) {
                        long t1 = System.currentTimeMillis();
                        long t2 = mService.getGpsParams().getTime();
                        if ((t1 - t2) < 10000) { // 10 seconds
                            deviceAltitude = mService.getGpsParams().getAltitude();
                            if(deviceAltitude < MIN_ALTITUDE) {
                                deviceAltitude = MIN_ALTITUDE;
                            }
                        }
                    }

                    // choose best altitude. give preference to pressure altitude because that is
                    // the most correct for traffic purpose.
                    double alt = pressureAltitude;
                    if(alt <= MIN_ALTITUDE) {
                        alt = geoAltitude;
                    }
                    if(alt <= MIN_ALTITUDE) {
                        alt = deviceAltitude;
                    }
                    if(alt <= MIN_ALTITUDE) {
                        alt = MIN_ALTITUDE;
                    }

                    // set pressure altitude for traffic alerts
                    mService.getTrafficCache().setOwnAltitude((int) alt);

                    // For own height prefer geo altitude, do not use deviceAltitude here because
                    // we could get into rising altitude condition through feedback
                    alt = geoAltitude;
                    if(alt <= MIN_ALTITUDE) {
                        alt = pressureAltitude;
                    }
                    if(alt <= MIN_ALTITUDE) {
                        alt = MIN_ALTITUDE;
                    }
                    l.setAltitude(alt / Preferences.heightConversion);
                    mService.getGps().onLocationChanged(l, type);
                }
                else if(type.equals("nexrad")) {
                    
                    /*
                     * XXX: If we are getting this from station, it must be current, fix this.
                     */
                    long time = Helper.getMillisGMT();//object.getLong("time");
                    int cols = object.getInt("x");
                    int rows = object.getInt("y");
                    int block = object.getInt("blocknumber");
                    boolean conus = object.getBoolean("conus");
                    JSONArray emptyArray = object.getJSONArray("empty");
                    JSONArray dataArray = object.getJSONArray("data");
                    
                    if(emptyArray == null || dataArray == null) {
                        return;
                    }
                    int empty[] = new int[emptyArray.length()];
                    for(int i = 0; i < empty.length; i++) {
                        empty[i] = emptyArray.getInt(i);
                    }
                    int data[] = new int[dataArray.length()];
                    for(int i = 0; i < data.length; i++) {
                        data[i] = dataArray.getInt(i);
                    }
                    
                    /*
                     * Put in nexrad.
                     */
                    mService.getAdsbWeather().putImg(
                            time, block, empty, conus, data, cols, rows);
                }
                else if(type.equals("sua")) {
                    mService.getAdsbWeather().putSua(
                            Helper.getMillisGMT(),
                            object.getString("text"));
                }
                else if(type.equals("airmet") || type.equals("sigmet")) {
                    mService.getAdsbWeather().putAirSigMet(
                            Helper.getMillisGMT(),
                            object.getString("number"),
                            object.getString("shape"),
                            object.getString("data"),
                            object.getString("text"),
                            object.getString("startTime"),
                            object.getString("endTime")
                            );
                }
                else if(type.equals("notam")) {
                    mService.getAdsbTfrCache().putTfr(
                            Helper.getMillisGMT(),
                            object.getString("number"),
                            object.getString("shape"),
                            object.getString("data"),
                            object.getString("text"),
                            object.getString("startTime"),
                            object.getString("endTime"));
                }
                else if(type.equals("METAR") || type.equals("SPECI")) {
                    /*
                     * Put METAR
                     */
                    mService.getAdsbWeather().putMetar(object.getLong("time"), 
                            object.getString("location"), object.getString("data"), object.getString("flight_category"));
                }
                else if(type.equals("TAF") || type.equals("TAF.AMD")) {
                    mService.getAdsbWeather().putTaf(object.getLong("time"), 
                            object.getString("location"), object.getString("data"));
                }
                else if(type.equals("WINDS")) {
                    mService.getAdsbWeather().putWinds(object.getLong("time"), 
                            object.getString("location"), object.getString("data"));
                }
                else if(type.equals("PIREP")) {
                    mService.getAdsbWeather().putAirep(object.getLong("time"), 
                            object.getString("location"), object.getString("data"),
                            mService.getDBResource());
                }
                if ((type.equals("PIREP")) || (type.equals("WINDS")) || (type.equals("TAF")) ||
                        (type.equals("TAF.AMD")) || (type.equals("METAR")) || (type.equals("SPECI") ||
                        (type.equals("nexrad")))) {
                    // Check here to see if it is an uplink type, and if so, extract the UAT tower location
                    double lon, lat;
                    int tisid;
                    /*
                     * Put METAR
                     */
                    lon = object.getDouble("towerlon");
                    lat = object.getDouble("towerlat");
                    tisid = object.getInt("tisid");
                    mService.getAdsbWeather().putUatTower(object.getLong("time"), lon, lat, tisid);
                }

            } catch (JSONException e) {
                return;
            }
        }
    };
}