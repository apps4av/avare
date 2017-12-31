/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.ds.avare.content.DataSource;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.storage.Preferences;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 
 * @author zkhan
 *
 */
public class Area {

    private DataSource mDataSource;
    private Airport[] mAirports = new Airport[Preferences.MAX_AREA_AIRPORTS];
    private HashMap<String, Airport> mAirportCache;
    private DataBaseAreaTask mDt;
    private double mLon;
    private double mLat;
    private long mLastTime;
    private double mAltitude;
    private Preferences mPref;
    
    private static final int UPDATE_TIME = 10000;
    
    /**
     * 
     * @param dataSource
     */
    public Area(DataSource dataSource, Context ctx) {
        mDataSource = dataSource;
        mLon = mLat = 0;
        mAltitude = 0;
        mLastTime = SystemClock.elapsedRealtime() - UPDATE_TIME;
        mPref = new Preferences(ctx);
        mAirportCache = new LinkedHashMap<String, Airport>();
    }

    /**
     * 
     */
    public Airport getAirport(int index) {
        /*
         * Check for null.
         */
        if(index >= mAirports.length) {
            return null;
        }
        return(mAirports[index]);
    }

    /**
     * 
     * @return
     */
    public int getAirportsNumber() {

        /*
         * Get all airports in a string array in this area.
         */
        int id;
        for(id = 0; id < mAirports.length; id++) {
            if(getAirport(id) == null) {
                break;
            }
        }
        return(id);
    }

    /**
     * 
     */
    public void updateLocation(GpsParams params) {
        
        double lon = params.getLongitude();
        double lat = params.getLatitude();

        long ctime = SystemClock.elapsedRealtime();
        ctime -= mLastTime;
        if(ctime < UPDATE_TIME) {
            /*
             * Slow down on creating async tasks.
             */
            return;
        }
        mLastTime = SystemClock.elapsedRealtime();
        
        mLon = lon;
        mLat = lat;
        mAltitude = params.getAltitude();
        
        if(mDt != null) {
            /*
             * Do not overwhelm
             */
            if(mDt.getStatus() == AsyncTask.Status.RUNNING) {
                mDt.cancel(true);
            }
        }
        mDt = new DataBaseAreaTask();
        mDt.execute();
    }

    /**
     * @author zkhan
     * Query for closest airports task
     */
    private class DataBaseAreaTask extends AsyncTask<Object, Void, Object> {

        Airport[] airports = null;
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Object... vals) {

            Thread.currentThread().setName("Area");
            
            if(null == mDataSource) {
                return null;
            }
            
            mAirportCache = mDataSource.findClosestAirports(mLon, mLat, mAirportCache, mPref.getLongestRunway());
            airports = mAirportCache.values().toArray(new Airport[0]);


            return null;
        }
        
        @Override
        protected void onPostExecute(Object res) {
            if(airports == null) {
                return;
            }

            /*
             * Sort on distance because distance found from sqlite is less than perfect
             */
            int n = airports.length;

            for(int i = 0; i < n; i++) {
                if(airports[i] != null) {
                    airports[i].updateLocation(mLon, mLat);
                }
            }

            // Bubble sort
            // Removed Collections.sort() because this function has issues with some OS versions
            for (int c = 0; c < (n - 1); c++) {
                for (int d = 0; d < (n - c - 1); d++) {
                    if(airports[d + 1] == null || airports[d] == null) {
                        break;
                    }
                    if (airports[d].getDistance() > airports[d + 1].getDistance()) { /* For ascending order use < */
                        Airport swap = airports[d];
                        airports[d] = airports[d + 1];
                        airports[d + 1] = swap;
                    }
                }
            }

            /*
             * Now test if all airports are at glide-able distance.
             */
            for(int i = 0; i < n; i++) {
                if(airports[i] != null) {
                    airports[i].setHeight(mAltitude);
                }
            }

            mAirports = airports;
        }

    }
    
}
