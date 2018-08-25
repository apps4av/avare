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


import android.util.SparseArray;

import com.ds.avare.StorageService;

/**
 * 
 * @author zkhan
 *
 */
public class TrafficCache {
    private static final int MAX_ENTRIES = 100;
    private SparseArray<Traffic> mTraffic;
    private int mOwnAltitude;
    
    public TrafficCache() { 
        mTraffic = new SparseArray<Traffic>();
        mOwnAltitude = StorageService.MIN_ALTITUDE;
    }
    
    /**
     * 
     * @param
     */
    public void putTraffic(String callsign, int address, float lat, float lon, int altitude, 
            float heading, int speed, long time) {

        /*
         * For any new entries, check max traffic objects.
         */
        if(mTraffic.get(address) == null) {
            if(mTraffic.size() >= MAX_ENTRIES) {
                return;
            }            
        }
        
        mTraffic.put(address, new Traffic(callsign, address, lat, lon, altitude, 
                heading, speed, time));
    }

    public void setOwnAltitude(int altitude) {
        mOwnAltitude = altitude;
    }

    public int getOwnAltitude() {
        return mOwnAltitude;
    }
    
    /**
     * 
     * @return
     */
    public SparseArray<Traffic> getTraffic() {
        return mTraffic;
    }    
}
