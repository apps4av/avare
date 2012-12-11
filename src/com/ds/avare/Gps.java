/**
 * 
 */
package com.ds.avare;

import java.util.List;
import java.util.Observer;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * @author zkhan
 *
 */
public class Gps {

    private Context mContext;
    
    /**
     * App preferences
     */
    private Preferences mPreferences;

    private Location mLastLocation;
    
    private int mGpsPeriod;
        
    private static final int GPS_PERIOD_LONG_MS = 8000;

    /**
     * 
     */
    public Gps(Context ctx) {
        mPreferences = new Preferences(ctx);
        mContext = ctx;
    }

    /**
     * 
     * @return
     */
    public boolean isGpsAvailable() {
        LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);  
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
    public Location getLastLocation() {
        LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);  
        List<String> providers = lm.getProviders(true);

        Location l = null;
        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) {
                break;
            }
        }
        if(null != l) {
            mLastLocation = l;
        }
        return l;
    }
    
    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    public boolean isBetterLocation(Location location, Location currentBestLocation) {
       
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

   /** Checks whether two providers are the same */
   private boolean isSameProvider(String provider1, String provider2) {
       if (provider1 == null) {
         return provider2 == null;
       }
       return provider1.equals(provider2);
   }
}
