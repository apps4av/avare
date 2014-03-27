package com.ds.avare.adsb;

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
    
    private static final long EXPIRES = 1000 * 60 * 10;

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
}
