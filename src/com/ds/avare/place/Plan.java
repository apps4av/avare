/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;



import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 */
public class Plan {

    private Destination[] mDestination = new Destination[MAX_DESTINATIONS];
    private Boolean[] mPassed = new Boolean[MAX_DESTINATIONS];
    
    private static final int MAX_DESTINATIONS = 10;
    
    private static final int MILES_PER_SEGMENT = 50;
    
    private boolean mActive;

    private TrackShape mTrackShape;
    
    private String mEta;
    private double mDistance;
    private double mBearing;
    private GpsParams mLastLocation;
    
    
    /**
     * 
     * @param dataSource
     */
    public Plan() {
        mActive = false;
        mTrackShape = new TrackShape();
        mDistance = 0;
        mLastLocation = null;
        mBearing = 0;
        mEta = "--:--";
    }

    /**
     * 
     */
    public Destination getDestination(int index) {
        /*
         * Check for null.
         */
        if(index >= MAX_DESTINATIONS) {
            return null;
        }
        return(mDestination[index]);
    }
    
    /**
     * 
     */
    public Boolean isPassed(int index) {
        /*
         * Check for null.
         */
        if(index >= MAX_DESTINATIONS) {
            return true;
        }
        return(mPassed[index]);
    }

    /**
     * 
     * @return
     */
    public int getDestinationNumber() {

        /*
         * Get all airports in a string array in this Plan.
         */
        int id;
        for(id = 0; id < MAX_DESTINATIONS; id++) {
            if(getDestination(id) == null) {
                break;
            }
        }
        return(id);
    }

    /**
     * 
     * @return
     */
    public void remove(int rmId) {
        int num = getDestinationNumber() - 1;
        if(rmId > num || rmId < 0) {
            return;
        }
        mDestination[rmId] = null;
        mPassed[rmId] = null;
        for(int id = rmId; id < num; id++) {
            mDestination[id] = mDestination[id + 1];
            mDestination[id + 1] = null;
            mPassed[id] = mPassed[id + 1];
            mPassed[id + 1] = null;
        }
        if(getDestinationNumber() > 0) {
            mTrackShape.updateShapeFromPlan(getCoordinates(
                    mLastLocation.getLongitude(), mLastLocation.getLatitude()));
        }
        else {
            mTrackShape = new TrackShape();
        }
    }

    /**
     * 
     * @return
     */
    public boolean appendDestination(Destination dest) {

        int n = getDestinationNumber();
        if(n >= MAX_DESTINATIONS) {
            return false;
        }
        
        if(n > 0) {
            /*
             * Check if last set was set again, it makes no sense to have same dest twice in sequence
             */
            if(mDestination[n - 1].getStorageName().equals(dest.getStorageName())) {
                return false;
            }
        }
        mDestination[n] = dest;
        mPassed[n] = false;
        if(null == mLastLocation) {
            mLastLocation = new GpsParams(mDestination[n].getLocationInit());
        }
        mTrackShape.updateShapeFromPlan(getCoordinates(
                mLastLocation.getLongitude(), mLastLocation.getLatitude()));

        return(true);
    }

    /*
     * Find the next not passed destination
     */
    public int findNextNotPassed() {
        for(int id = 0; id < getDestinationNumber(); id++) {
            if(!mPassed[id]) {
                return id;
            }
        }
        return 0;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public void updateLocation(GpsParams params) {
        mDistance = 0;
        int num = getDestinationNumber();
        for(int id = 0; id < num; id++) {
            mDestination[id].updateTo(params);
            mDistance += mDestination[id].getDistance();
        }
        mBearing = 0;
        if(num > 0) {
            mBearing = mDestination[0].getBearing();
        }
        mEta = Helper.calculateEta(mDistance, params.getSpeed());
        mLastLocation = params;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        /*
         * For display purpose
         */
        return Helper.makeLine(mDistance, Preferences.distanceConversionUnit, mEta, mBearing, ""); 
    }

    /**
     * Activate flight plan
     */
    public void makeActive() {
        mActive = true;
    }
    
    /**
     * Inativate flight plan
     */
    public void makeInactive() {
        mActive = false;
    }
    
    /**
     * flight plan
     */
    public boolean isActive() {
        return mActive;
    }
    
    
    /*
     * Used to concat coordinates.
     */
    private Coordinate[] concat(Coordinate[] A, Coordinate[] B) {
        int aLen = A.length;
        int bLen = B.length;
        Coordinate[] C = new Coordinate[aLen + bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }
    
    /*
     * Get a list of coordinates forming this route on great circle
     */
    public Coordinate[] getCoordinates(double initLon, double initLat) {
        int num = getDestinationNumber();
        
        double lon0 = initLon;
        double lat0 = initLat;
        Coordinate[] c = null;

        /*
         * Form a general path on the great circle of this plan
         */
        for(int id = 0; id < num; id++) {
            Projection p = new Projection(
                    getDestination(id).getLocation().getLongitude(), 
                    getDestination(id).getLocation().getLatitude(),
                    lon0, lat0);
            int segments = (int)p.getDistance() / MILES_PER_SEGMENT + 3; // Min 3 points
            Coordinate coord[] = p.findPoints(segments);

            if(null == c) {
                c = coord;
            }
            else {
                c = concat(c, coord);
            }
            lon0 = getDestination(id).getLocation().getLongitude();
            lat0 = getDestination(id).getLocation().getLatitude();            
        }
        return c;
    }

    /**
     * 
     * @return
     */
    public TrackShape getTrackShape() {
        return mTrackShape;
    }
}


