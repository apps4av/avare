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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ds.avare.instruments.CDI;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.utils.Helper;

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

/**
 * This class exposes the remote service to the client.
 * The client will be the Avare Helper, sending data to Avare
 * author zkhan
 */
public class IHelperService extends Service {

    private StorageService mService;
    
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
                
                // If destination set, send how to get there (for autopilots).
                if(d != null) {
                    distance = d.getDistance();
                    bearing = d.getBearing();
                    lon = d.getLocation().getLongitude();
                    lat = d.getLocation().getLatitude();
                    elev = d.getElevation();
                    if(p != null) {
                        idNext = p.findNextNotPassed() + 1;
                        idOrig = idNext - 1;
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
                else if(type.equals("ownship")) {
                    Location l = new Location(LocationManager.GPS_PROVIDER);
                    l.setLongitude(object.getDouble("longitude"));
                    l.setLatitude(object.getDouble("latitude"));
                    l.setSpeed((float)object.getDouble("speed"));
                    l.setBearing((float)object.getDouble("bearing"));
                    l.setAltitude(object.getDouble("altitude"));
                    l.setTime(object.getLong("time"));
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
                else if(type.equals("METAR") || type.equals("SPECI")) {
                    /*
                     * Put METAR
                     */
                    mService.getAdsbWeather().putMetar(object.getLong("time"), 
                            object.getString("location"), object.getString("data"));
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

            } catch (JSONException e) {
                return;
            }
        }
    };
}