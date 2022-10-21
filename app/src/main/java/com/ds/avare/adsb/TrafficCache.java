/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adsb;


import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import android.location.Location;

/**
 * 
 * @author zkhan
 *
 */
public class TrafficCache {
    private static final int MAX_ENTRIES = 20;
    private Traffic[] mTraffic;
    private int mOwnAltitude;
    private boolean mOwnIsAirborne;
    private Location mOwnLocation;
    private int mOwnVertVelocity;
    Preferences mPref;

    public TrafficCache() {
        mTraffic = new Traffic[MAX_ENTRIES + 1];
        mOwnAltitude = StorageService.MIN_ALTITUDE;
        mPref = StorageService.getInstance().getPreferences();
    }

    private class TrafficComparator implements Comparator<Traffic>
    {
        public int compare(Traffic left, Traffic right) {
            if(null == left && null != right) {
                return 1;
            }
            if(null != left && null == right) {
                return -1;
            }
            if(null == left && null == right) {
                return 0;
            }
            double l = findDistance(left.mLon, left.mLat);
            double r = findDistance(right.mLon, right.mLat);
            if(l > r) {
                return 1;
            }
            if(l < r) {
                return -1;
            }
            return 0;
        }
    }

    private double findDistance(double lon, double lat) {
        // find 3d distance between current position and airplane
        // treat 1 NM of horz separation as 500 feet of altitude (C182 120kts, 1000 fpm)
        GpsParams p = StorageService.getInstance().getGpsParams();
        double horDist = Projection.getStaticDistance(p.getLongitude(), p.getLatitude(), lon, lat) * Preferences.feetConversion;
        double altDist = Math.abs(p.getAltitude() - mOwnAltitude) * Preferences.feetConversion / 500;
        double fac = horDist + altDist;
        return fac;
    }

    private void handleAudibleAlerts() {
        if (mPref.isAudibleTrafficAlerts()) {
            final StorageService storageService = StorageService.getInstance();
            AudibleTrafficAlerts audibleTrafficAlerts = AudibleTrafficAlerts.getAndStartAudibleTrafficAlerts(storageService.getApplicationContext());
            audibleTrafficAlerts.setUseTrafficAliases(mPref.isAudibleAlertTrafficId());
            audibleTrafficAlerts.setTopGunDorkMode(mPref.isAudibleTrafficAlertsTopGunMode());
            audibleTrafficAlerts.setClosingTimeEnabled(mPref.isAudibleClosingInAlerts());
            audibleTrafficAlerts.setClosingTimeThresholdSeconds(mPref.getAudibleClosingInAlertSeconds());
            audibleTrafficAlerts.setClosestApproachThresholdNmi(mPref.getAudibleClosingInAlertDistanceNmi());
            audibleTrafficAlerts.setCriticalClosingAlertRatio(mPref.getAudibleClosingInCriticalAlertRatio());
            audibleTrafficAlerts.setAlertMaxFrequencySec(mPref.getAudibleTrafficAlertsMaxFrequency());
            audibleTrafficAlerts.setGroundAlertsEnabled(mPref.isAudibleGroundAlertsEnabled());
            audibleTrafficAlerts.setMinSpeed(mPref.getAudibleTrafficAlertsMinSpeed());
            audibleTrafficAlerts.handleAudibleAlerts(getOwnLocation(), getTraffic(),
                    mPref.getAudibleTrafficAlertsDistanceMinimum(), mOwnAltitude, mOwnIsAirborne,
                    mOwnVertVelocity);
        } else {
            AudibleTrafficAlerts.stopAudibleTrafficAlerts();
        }
    }

    /**
     * 
     * @param
     */
    public void putTraffic(String callsign, int address, boolean isAirborne, float lat, float lon, int altitude,
            float heading, int speed, int vspeed, long time) {

        int filterAltitude = StorageService.getInstance().getPreferences().showAdsbTrafficWithin();
        if(address == mPref.getAircraftICAOCode() || address == StorageService.getInstance().getIcaoAddress()) {
            // do not show own traffic
            return;
        }

        for(int i = 0; i < MAX_ENTRIES; i++) {
            if(mTraffic[i] == null) {
                continue;
            }
            if(mTraffic[i].isOld()) {
                // purge old
                mTraffic[i] = null;
                continue;
            }
            // filter traffic too high
            int altDiff = Math.abs(mOwnAltitude - mTraffic[i].mAltitude);
            if(Math.abs(altDiff) > filterAltitude) {
                mTraffic[i] = null;
                continue;
            }
            // update
            if(mTraffic[i].mIcaoAddress == address) {
                // callsign not available. use last one
                if(callsign.equals("")) {
                    callsign = mTraffic[i].mCallSign;
                }
                mTraffic[i] = new Traffic(callsign, address, isAirborne, lat, lon, altitude, heading, speed, vspeed, time);

                handleAudibleAlerts();
                return;
            }
        }

        // filter traffic too high
        int altDiff = Math.abs(mOwnAltitude - altitude);
        if(Math.abs(altDiff) > filterAltitude) {
            return;
        }
        // put it in the end
        mTraffic[MAX_ENTRIES] = new Traffic(callsign, address, isAirborne, lat, lon, altitude, heading, speed, vspeed, time);

        // sort
        Arrays.sort(mTraffic, new TrafficComparator());

        handleAudibleAlerts();

    }

    public void setOwnAltitude(int altitude) {
        mOwnAltitude = altitude;
    }
    public void setOwnIsAirborne(boolean isAirborne) {
        mOwnIsAirborne = isAirborne;
    }
    public void setOwnVertVelocity(int vspeed) {
        mOwnVertVelocity = vspeed;
    }


    public int getOwnAltitude() {
        return mOwnAltitude;
    }
    public boolean getOwnIsAirborne() { return mOwnIsAirborne; }
    public int getOwnVertVelocity() {
        return mOwnVertVelocity;
    }

    public void setOwnLocation(Location loc) {
        this.mOwnLocation = loc;
    }
    public Location getOwnLocation() { return this.mOwnLocation; }

    /**
     * 
     * @return
     */
    public LinkedList<Traffic> getTraffic() {
        LinkedList<Traffic> t = new LinkedList<>();

        for(int i = 0; i < MAX_ENTRIES; i++) {
            if(null != mTraffic[i]) {
                t.add(mTraffic[i]);
            }
        }
        return t;
    }

}
