package com.ds.avare.adsb;

import android.graphics.Color;

import com.ds.avare.utils.Helper;

public class Traffic {

    public int mIcaoAddress;
    public float mLat;
    public float mLon;
    public int mAltitude;
    public int mHorizVelocity;
    public float mHeading;
    public String mCallSign;
    private long mLastUpdate;
    

    public static final double TRAFFIC_ALTITUDE_DIFF_DANGEROUS = 1000; //ft 300m required minimum
    

    
    // ms
    private static final long EXPIRES = 1000 * 60 * 1;

    /**
     * 
     * @param callsign
     * @param address
     * @param lat
     * @param lon
     * @param altitude
     * @param heading
     */
    public Traffic(String callsign, int address, float lat, float lon, int altitude, 
            float heading, int speed, long time)
    {
        mIcaoAddress = address;
        mCallSign = callsign;
        mLon = lon;
        mLat = lat;
        mAltitude = altitude;
        mHeading = heading;
        mHorizVelocity = speed;
        mLastUpdate = time;
        
        /*
         * Limit
         */
        if(mHorizVelocity >= 0xFFF) {
            mHorizVelocity = 0;
        }
    }
    
    /**
     * 
     * @return
     */
    public boolean isOld() {

        long diff = Helper.getMillisGMT();
        diff -= mLastUpdate; 
        if(diff > EXPIRES) {
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @return
     */
    public static int getColorFromAltitude(double myAlt, double theirAlt) {
        int color;
        double diff = myAlt - theirAlt;
        if(diff > TRAFFIC_ALTITUDE_DIFF_DANGEROUS) {
            /*
             * Much below us
             */
            color = Color.GREEN;
        }
        else if (diff < TRAFFIC_ALTITUDE_DIFF_DANGEROUS && diff > 0) {
            /*
             * Dangerously below us
             */
            color = Color.RED;
        }
        else if (diff < -TRAFFIC_ALTITUDE_DIFF_DANGEROUS) {
            /*
             * Much above us
             */
            color = Color.BLUE;
        }
        else {
            /*
             * Dangerously above us
             */
            color = Color.MAGENTA;
        }
 
        return color;
    }
    
}
