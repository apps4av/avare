/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
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
package com.ds.avare.gps;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ds.avare.nmea.GGAPacket;
import com.ds.avare.storage.Preferences;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

import static android.os.Build.VERSION.*;

/**
 * @author zkhan
 *
 */
public class Gps implements LocationListener, android.location.GpsStatus.Listener {

    private Context mContext;

    Object mNmeaMessageListener;

    /**
     * App preferences
     */
    private Preferences mPref;

    private int mGpsPeriod;
    
    private GpsInterface mGpsCallback;
    
    /**
     * Time of last GPS message
     */
    private long mGpsLastUpdate;
    
    /**
     * Altitude from NMEA as it can be wrong from Android's API
     */
    private double mAltitude;

    /**
     * A timer that clicks to check GPS status
     */
    private Timer mTimer;

    /*
     * GPS manager
     */
    private LocationManager mLocationManager;
        
    private static final int GPS_PERIOD_LONG_MS = 8000;

    // Some global properties of the GPS itself that are required
    // when driving an autopilot
    private static int mSatCount;
    private static double mGeoid;
    private static double mHorDil;

    static int getSatCount() {
        return mSatCount;
    }
    static double getGeoid() {
        return mGeoid;
    }
    static double getHorDil() {
        return mHorDil;
    }

    /**
     * 
     */
    public Gps(Context ctx, GpsInterface callback) {
        mPref = new Preferences(ctx);
        mContext = ctx;
        mLocationManager = null;
        mTimer = null;
        mAltitude = 0;
        mGpsCallback = callback;
        if(mPref.isGpsUpdatePeriodShort()) {
            mGpsPeriod = 0;
        }
        else {
            mGpsPeriod = GPS_PERIOD_LONG_MS;
        }
    }

    /**
     * 
     * @return void
     */
    public static boolean isGpsAvailable(Context ctx) {
        
        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);

        if(null == lm) {
            return false;
        }
        
        List<String> providers = lm.getProviders(true);
        for (int i = providers.size() - 1; i >= 0; i--) {
            if(providers.get(i).equals(LocationManager.GPS_PROVIDER)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 
     * @return void
     */
    public static Location getLastLocation(Context ctx) {
        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);

        if(null == lm) {
            return null;
        }

        List<String> providers = lm.getProviders(false);

        Location l = null;

        try {
            for (int i = providers.size() - 1; i >= 0; i--) {
                l = lm.getLastKnownLocation(providers.get(i));
                if (l != null) {
                    break;
                }
            }
        }
        catch (SecurityException e) {
            return null;
        }
        return l;
    }

    /**
     * Must be called to use GPS
     */
    public synchronized void start() {
        
        if(mPref.isGpsUpdatePeriodShort()) {
            mGpsPeriod = 0;
        }
        else {
            mGpsPeriod = GPS_PERIOD_LONG_MS;
        }
        
        /*
         * Start GPS but dont start if already started
         */
        if(null == mLocationManager) {
            
            mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);

            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        mGpsPeriod / 4, 0, this);
                mLocationManager.addGpsStatusListener(this);
                if(SDK_INT >= 24) {
                    mNmeaMessageListener = new android.location.OnNmeaMessageListener() {
                        @Override
                        public void onNmeaMessage(String nmea, long timestamp) {
                            processNmea(nmea, timestamp);
                        }
                    };
                    mLocationManager.addNmeaListener((android.location.OnNmeaMessageListener) mNmeaMessageListener);
                }
            }
            catch (SecurityException e) {
                mLocationManager = null;
            }
            catch (Exception e) {
                mLocationManager = null;
            }
        }


        /*
         * Start timer
         */
        if(null == mTimer) {
            updateTimeout();
            mTimer = new Timer();
            TimerTask gpsTime = new UpdateGps();
            /*
             * Give some delay for check start
             */
            mTimer.scheduleAtFixedRate(gpsTime, (GPS_PERIOD_LONG_MS * 2),
                    GPS_PERIOD_LONG_MS / 4);
        }
    }
    
    /**
     * 
     */
    public synchronized void stop() {
        
        /*
         * Stop but dont stop if already stopped
         */
        if(null != mLocationManager) {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
            if(SDK_INT >= 24) {
                mLocationManager.removeNmeaListener((android.location.OnNmeaMessageListener)mNmeaMessageListener);
            }
            mLocationManager = null;
            return;
        }

        if(null != mTimer) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * 
     * @return boolean
     */
    public static boolean isGpsDisabled(Context ctx, Preferences pref) {
        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);

        if(null == lm) {
            return true;
        }
        return(pref.isGpsWarn() &&
                (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)));
    }

    /**
     * 
     */
    @Override
    public void onGpsStatusChanged(int event) {
        mSatCount = 0;
        if(null == mLocationManager) {
            return;
        }
        GpsStatus gpsStatus;
        try {
            gpsStatus = mLocationManager.getGpsStatus(null);
        }
        catch (SecurityException e) {
            return;
        }
        if (null == gpsStatus) {
            return;
        }
        mGpsCallback.statusCallback(gpsStatus);
        if (null == gpsStatus.getSatellites()) {
            return;
        }
        for (GpsSatellite sat : gpsStatus.getSatellites()) {
            if(sat.usedInFix()) {
                mSatCount++;
            }
        }
    }

    /**
     * 
     */
    @Override
    public void onLocationChanged(Location location) {
        
        if(mPref.getExternalGpsSource().equals("2")) {
            return;
        }
        
        if ((location != null)
                && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            
            updateTimeout();

            // MSL from NMEA is more correct as its not corrected by LocationManager
            if(mPref.useNmeaAltitude()) {
                location.setAltitude(mAltitude);
            }
            
            /*
             * Called by GPS. Update everything driven by GPS.
             */
            if(!mPref.isSimulationMode()) {
                mGpsCallback.locationCallback(location);
            }
        }
    }

    /**
     * From IO module
     */
    public void onLocationChanged(Location location, String from) {
        if(mPref.getExternalGpsSource().equals("1")) {
            return;
        }
        
        if ((location != null)
                && location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            
            updateTimeout();

            /*
             * Called by GPS. Update everything driven by GPS.
             */
            if(!mPref.isSimulationMode()) {
                mGpsCallback.locationCallback(location);
            }
        }
    }

    /**
     * @author zkhan
     *
     */ 
    private class UpdateGps extends TimerTask {
        
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            
            /*
             * Check when last event was received
             */
            long now = SystemClock.elapsedRealtime();
            long last;
            synchronized(this) {
                last = mGpsLastUpdate;
            }
            if((now - last) > 
                (GPS_PERIOD_LONG_MS * 4)) {
                mGpsCallback.timeoutCallback(true);                
            }
            else {
                mGpsCallback.timeoutCallback(false);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER) && (!mPref.isSimulationMode())) {
            mGpsCallback.enabledCallback(false);
        }
    }
    
    @Override
    public void onProviderEnabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER) && (!mPref.isSimulationMode())) {
            mGpsCallback.enabledCallback(true);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle arg2) {
        if(provider.equals(LocationManager.GPS_PROVIDER) &&
                (status == LocationProvider.OUT_OF_SERVICE)
                && (!mPref.isSimulationMode())) {
            mGpsCallback.statusCallback(null);
        }
    }
    
    /**
     * This is to not let the timer expire 
     */
    public void updateTimeout() {
        synchronized(this) {
            mGpsLastUpdate = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Process nmea
     * @param nmea
     * @param timestamp
     */
    private void processNmea(String nmea, long timestamp) {
        /*
         * Use this for altitude and some GPS status values
         */
        if(nmea.startsWith(GGAPacket.TAG) || nmea.startsWith(GGAPacket.TAGN)) {
            // Horozontal dilution
            String val[] = nmea.split(",");
            if(val.length > GGAPacket.HD) {
                try {
                    mHorDil = Double.parseDouble(val[GGAPacket.HD]);
                }
                catch (Exception e) {
                    mHorDil = 0;
                }
            }

            // Altitude
            if(val.length > GGAPacket.ALT) {
                try {
                    mAltitude = Double.parseDouble(val[GGAPacket.ALT]);
                }
                catch (Exception e) {
                    mAltitude = 0;
                }
            }

            //Height above the WGS-84 Ellipsoid
            if(val.length > GGAPacket.GEOID) {
                try {
                    mGeoid = Double.parseDouble(val[GGAPacket.GEOID]);
                }
                catch (Exception e) {
                    mGeoid = 0;
                }
            }
        }
    }
}
