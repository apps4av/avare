/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * @author zkhan
 *
 */
public class Gps implements LocationListener, android.location.GpsStatus.Listener {

    private Context mContext;
    
    /**
     * App preferences
     */
    private Preferences mPreferences;

    private Location mLastLocation;
    
    private int mGpsPeriod;
    
    private GpsInterface mGpsCallback;
    
    /**
     * Time of last GPS message
     */
    private long mGpsLastUpdate;

    /**
     * A timer that clicks to check GPS status
     */
    private Timer mTimer;

    /*
     * GPS manager
     */
    private LocationManager mLocationManager;
        
    private static final int GPS_PERIOD_LONG_MS = 8000;

    /**
     * 
     */
    public Gps(Context ctx, GpsInterface callback) {
        mPreferences = new Preferences(ctx);
        mContext = ctx;
        mLocationManager = null;
        mGpsCallback = callback;
        if(mPreferences.isGpsUpdatePeriodShort()) {
            mGpsPeriod = 0;
        }
        else {
            mGpsPeriod = GPS_PERIOD_LONG_MS;
        }
    }

    /**
     * 
     * @return
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
     * @return
     */
    public static Location getLastLocation(Context ctx) {
        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);

        if(null == lm) {
            return null;
        }

        List<String> providers = lm.getProviders(true);

        Location l = null;
        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) {
                break;
            }
        }
        return l;
    }
    
    /** 
     * Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
       
       final int TWO_MINUTES = 1000 * 60 * 2;
       
       if (currentBestLocation == null) {
           // A new location is always better than no location
           return true;
       }

       // Check whether the new location fix is newer or older
       long timeDelta = location.getTime() - currentBestLocation.getTime();
       boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
       boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
       boolean isNewer = timeDelta > 0;

       // If it's been more than two minutes since the current location, use the new location
       // because the user has likely moved
       if (isSignificantlyNewer) {
           // If the new location is more than two minutes older, it must be worse
           return true;
       } 
       else if (isSignificantlyOlder) {
           return false;
       }

       // Check whether the new location fix is more or less accurate
       int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
       boolean isLessAccurate = accuracyDelta > 0;
       boolean isMoreAccurate = accuracyDelta < 0;
       boolean isSignificantlyLessAccurate = accuracyDelta > 200;

       // Check if the old and new location are from the same provider
       boolean isFromSameProvider = isSameProvider(location.getProvider(),
               currentBestLocation.getProvider());

       // Determine location quality using a combination of timeliness and accuracy
       if (isMoreAccurate) {
           return true;
       } 
       else if (isNewer && !isLessAccurate) {
           return true;
       } 
       else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
           return true;
       }
       return false;
   }

   /** 
    * Checks whether two providers are the same
    */
   private boolean isSameProvider(String provider1, String provider2) {
       if (provider1 == null) {
           return provider2 == null;
       }
       return provider1.equals(provider2);
   }

    /**
     * Must be called to use GPS
     */
    public void start() {
        if(mPreferences.isGpsUpdatePeriodShort()) {
            mGpsPeriod = 0;
        }
        else {
            mGpsPeriod = GPS_PERIOD_LONG_MS;
        }
        
        /*
         * Start GPS
         */
        if(null == mLocationManager) {
                
            mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);

            if(isGpsAvailable(mContext)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        mGpsPeriod / 4, 0, this);
                mLocationManager.addGpsStatusListener(this);
            }
            
            /*
             * Also obtain GSM based locations
             */
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
                    0, 0, this);
            
            mGpsLastUpdate = SystemClock.uptimeMillis();
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
    public void stop() {
        
        if(null == mLocationManager) {
            return;
        }

        if(null != mTimer) {
            mTimer.cancel();
        }

        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
        mLocationManager = null;
    }

    /**
     * 
     * @return
     */
    public static boolean isGpsDisabled(Context ctx, Preferences pref) {
        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);

        if(null == lm) {
            return true;
        }
        return(pref.shouldGpsWarn() && 
                (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)));
    }

    /**
     * 
     */
    @Override
    public void onGpsStatusChanged(int event) {
        if(null == mLocationManager) {
            return;
        }
        GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
        if(GpsStatus.GPS_EVENT_STOPPED == event) {
            mGpsCallback.statusCallback(null);           
        }
        else {
            mGpsCallback.statusCallback(gpsStatus);           
        }
    }

    /**
     * 
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null && (!mPreferences.isSimulationMode())) {


            mGpsLastUpdate = SystemClock.uptimeMillis();

            /*
             * Called by GPS. Update everything driven by GPS.
             */
            if(isBetterLocation(location, mLastLocation)) {
                mLastLocation = location;
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
            if((SystemClock.uptimeMillis() - mGpsLastUpdate) > 
                (GPS_PERIOD_LONG_MS * 2)) {
                mGpsCallback.timeoutCallback(true);                
            }
            else {
                mGpsCallback.timeoutCallback(false);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER)) {
            mGpsCallback.enabledCallback(false);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER)) {
            mGpsCallback.enabledCallback(true);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle arg2) {
        if(provider.equals(LocationManager.GPS_PROVIDER) &&
                (status != LocationProvider.AVAILABLE)) {
            mGpsCallback.statusCallback(null);
        }
    }
}
