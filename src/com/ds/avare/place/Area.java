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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.storage.DataSource;

import android.os.AsyncTask;
import android.os.SystemClock;

/**
 * 
 * @author zkhan
 *
 */
public class Area {

    private DataSource mDataSource;
    private Airport[] mAirports = new Airport[MAX_AIRPORTS];
    private DataBaseAreaTask mDt;
    private double mLon;
    private double mLat;
    private double mVariation;
    private boolean mFound;
    private long mLastTime;
    
    private static final int MAX_AIRPORTS = 20;
    
    private static final int UPDATE_TIME = 10000;
    
    /**
     * 
     * @param dataSource
     */
    public Area(DataSource dataSource) {
        mDataSource = dataSource;
        mLon = mLat = 0;
        mVariation = 0;
        mLastTime = SystemClock.elapsedRealtime();
        mFound = false;
    }

    /**
     * 
     */
    public Airport getAirport(int index) {
        /*
         * Check for null.
         */
        if(index >= MAX_AIRPORTS) {
            return null;
        }
        return(mAirports[index]);
    }

    /**
     *
     */
    public double getVariation() {
        return(mVariation);
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
        for(id = 0; id < MAX_AIRPORTS; id++) {
            if(getAirport(id) == null) {
                break;
            }
        }
        return(id);
    }

    /**
     * 
     * @param lon
     * @param lat
     */
    public void updateLocation(GpsParams params) {
        
        double lon = params.getLongitude();
        double lat = params.getLatitude();

        if(mFound) {
            long ctime = SystemClock.elapsedRealtime();
            ctime -= mLastTime;
            if(Math.abs(ctime) < UPDATE_TIME) {
                /*
                 * Slow down on creating async tasks.
                 */
                return;
            }
        }
        mLastTime = SystemClock.elapsedRealtime();
        
        mLon = lon;
        mLat = lat;
        
        if(mDt != null) {
            /*
             * Do not overwhelm
             */
            if(mDt.getStatus() != AsyncTask.Status.FINISHED) {
                return;
            }
        }
        mDt = new DataBaseAreaTask();
        mDt.execute();
    }

    /**
     * @author zkhan
     * Query for closest airports task
     */
    private class DataBaseAreaTask extends AsyncTask<Object, Void, Void> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Object... vals) {

            Thread.currentThread().setName("Area");
            
            if(null == mDataSource) {
                return null;
            }
            
            mDataSource.findClosestAirports(mLon, mLat, mAirports);
            /*
             * Sort on distance because distance found from sqlite is less than perfect
             */
            if(getAirportsNumber() > 1) {
                List<Airport> list = Arrays.asList(mAirports);
                Collections.sort(list);
                mAirports = (Airport[]) list.toArray();
                mFound = true;
            }
            
            /*
             * Variation in this area is equal to the variation at nearest airport
             */
            for(int id = 0; id < getAirportsNumber(); id++) {
                mVariation = mAirports[id].getVariation(); 
                if(mVariation != 0) {
                    break;
                }
            }
            
            for(int id = 0; id < getAirportsNumber(); id++) {
                mVariation = mAirports[id].getVariation(); 
            }

            return null;
        }
    }
}
