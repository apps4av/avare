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
    private Passage mPassage;
    private boolean mDestChanged;
    private double mDeclination;
    
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
        mDeclination = 0;
        mDestChanged = false;
        mEta = "--:--";
        mPassage = new Passage();
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
            if(mLastLocation != null) {
                mTrackShape.updateShapeFromPlan(getCoordinates(
                        mLastLocation.getLongitude(), mLastLocation.getLatitude()));
            }
        }
        else {
            mTrackShape = new TrackShape();
        }        
    }

    /**
     * 
     * @return
     */
    public void move(int from, int to) {
        int num = getDestinationNumber();
        if(from >= num || to >= num) {
            return;
        }
        if(from > to) {
            /*
             * Move everything down
             */
            Destination tmp = mDestination[from];
            for(int i = from; i > to; i--) {
                mDestination[i] = mDestination[i - 1];
            }
            mDestination[to] = tmp;
        }
        else if (from < to) {
            /*
             * Move everything up
             */
            Destination tmp = mDestination[from];
            for(int i = from; i < to; i++) {
                mDestination[i] = mDestination[i + 1];
            }
            mDestination[to] = tmp;
        }
        
        if(num > 0) {
            if(mLastLocation != null) {
                mTrackShape.updateShapeFromPlan(getCoordinates(
                        mLastLocation.getLongitude(), mLastLocation.getLatitude()));
            }
        }
        else {
            mTrackShape = new TrackShape();
        }        
    }

    /**
     * 
     * @return
     */
    public boolean hasDestinationChanged() {
        /*
         * Auto change to next dest.
         */
        boolean ret = mDestChanged;
        mDestChanged = false;
        return ret;
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
        /*
         * 
         */
        for(int id = 0; id < getDestinationNumber(); id++) {
            if(!mPassed[id]) {
                return id;
            }
        }
        return 0;
    }

    /*
     * If passed
     */
    public boolean isPassed(int id) {
        return mPassed[id];
    }

    /*
     * If passed
     */
    public void setPassed(int id) {
        mPassed[id] = true;
    }

    /*
     * If not passed
     */
    public void setNotPassed(int id) {
        mPassed[id] = false;
    }

    /**
     * 
     * @param lon
     * @param lat
     */
    public void updateLocation(GpsParams params) {
        mDistance = 0;
        mBearing = 0;
        mDeclination = params.getDeclinition();
        int num = getDestinationNumber();
        int np = findNextNotPassed();
        if(0 == num) {
            mPassage = new Passage();
            return;
        }
        
        /*
         * For all passed way points set distance to current
         */
        for(int id = 0; id <= np; id++) {
            mDestination[id].updateTo(params);
        }
        mDistance = mDestination[np].getDistance();
        
        /*
         * For all upcoming, add distance. Distance is from way point to way point
         */
        for(int id = np; id < (num - 1); id++) {
            mDestination[id + 1].updateTo(new GpsParams(mDestination[id].getLocation()));
            mDistance += mDestination[id + 1].getDistance();
        }
        if(num > 0) {
            mBearing = mDestination[findNextNotPassed()].getBearing();
            if(mPassage.updateLocation(params, mDestination[findNextNotPassed()])) {
                /*
                 * Passed. Go to next. Only when active
                 */
                if(mActive) {
                    mPassed[findNextNotPassed()] = true;
                    mDestChanged = true;
                }
            }   
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
        return Helper.makeLine(mDistance, Preferences.distanceConversionUnit, mEta, mBearing, mDeclination); 
    }

    /**
     * Activate flight plan
     */
    public void makeActive(GpsParams params) {
        mLastLocation = params;
        if(null != params) {
            mTrackShape.updateShapeFromPlan(getCoordinates(
                    mLastLocation.getLongitude(), mLastLocation.getLatitude()));
        }
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
    
    /**
     * A class that finds a station passage
     * @author zkhan
     *
     */
    private class Passage {

        double mLastDistance;
        double mLastBearing;
        double mCurrentDistance;
        double mCurrentBearing;
        
        private static final double PASSAGE_DISTANCE_MIN = 2;

        public Passage() {
            mLastDistance = -1;
            mLastBearing = -1;
        }

        /*
         * Algorithm to calculate passage.
         */
        private boolean hasPassed() {
            
            /*
             * Distance increases
             */
            if(mCurrentDistance > mLastDistance) {
                
                /*
                 * We are in passage zone
                 */
                if(mCurrentDistance < PASSAGE_DISTANCE_MIN) {
                    return true;
                }
                
            }
            
            return false;
        }
        
        /**
         * 
         * @param params
         */
        public boolean updateLocation(GpsParams params, Destination nextDest) {
            Projection p = new Projection(params.getLongitude(), params.getLatitude(),
                    nextDest.getLocation().getLongitude(), nextDest.getLocation().getLatitude());
            if(mLastBearing < 0) {
                /*
                 * Init on first input on location
                 */
                mLastDistance = p.getDistance();
                mLastBearing = p.getBearing();
                return false;
            }
          
            mCurrentDistance = p.getDistance();
            mCurrentBearing = p.getBearing();
            
            boolean ret = hasPassed();
            
            mLastDistance = mCurrentDistance;
            mLastBearing = mCurrentBearing;
            return ret;
        }
        
    }
    
}


