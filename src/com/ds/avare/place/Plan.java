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



import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.location.Location;

import com.ds.avare.StorageService;
import com.ds.avare.externalFlightPlan.ExternalFlightPlan;
import com.ds.avare.externalFlightPlan.ExternalPlanMgr;
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
public class Plan implements Observer {

    private Destination[] mDestination;
    private boolean[] mPassed;
    
    
    private static final int MAX_DESTINATIONS = 15;
    
    private static final int MILES_PER_SEGMENT = 50;
    
    private static final double MIN_REACHED_DISTANCE = 5;
    
    private boolean mActive;

    private TrackShape mTrackShape;
    
    private String mEte;
    private double mDistance;
    private double mBearing;
    private GpsParams mLastLocation;
    private Passage mPassage;
    private boolean mDestChanged;
    private double mDeclination;
    private int mReplaceId;
    private Preferences mPref;
    private StorageService mService;
    private double mCurrentDistance;
    private boolean mReached;
    private String mName;
    private ExternalPlanMgr mExtPlanMgr;
    
    /**
     * 
     * @param dataSource
     */
    public Plan(Context ctx) {
        mPref = new Preferences(ctx);
        clear();
    }

    /**
     * 
     */
    public void clear() {
        mActive = false;
        mTrackShape = new TrackShape();
        mDistance = 0;
        mLastLocation = null;
        mBearing = 0;
        mDeclination = 0;
        mDestChanged = false;
        mReached = false;
        mDestination = new Destination[MAX_DESTINATIONS];
        mPassed = new boolean[MAX_DESTINATIONS];
        for(int i = 0; i < MAX_DESTINATIONS; i++) {
            mPassed[i] = false;
        }
        mEte = "--:--";
        mPassage = new Passage();
        mName = null;
    }
    
    public void setName(String name) {
    	mName = name;
    }
    
    public String getName() {
    	return mName;
    }
    
    public void setExtPlanMgr(ExternalPlanMgr extPlanMgr) {
    	mExtPlanMgr = extPlanMgr;
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
        mPassed[rmId] = false;
        for(int id = rmId; id < num; id++) {
            mDestination[id] = mDestination[id + 1];
            mDestination[id + 1] = null;
            mPassed[id] = mPassed[id + 1];
            mPassed[id + 1] = false;
        }
        if(getDestinationNumber() > 0) {
            if(mLastLocation != null) {
                mTrackShape.updateShapeFromPlan(getCoordinates());
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
                mTrackShape.updateShapeFromPlan(getCoordinates());
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
        mTrackShape.updateShapeFromPlan(getCoordinates());

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
        if(mPassed[0] == false) {
            mPassed[0] = true;
        }
        
        /*
         * Depends if it is active or plan
         */
        if(mActive) {
        
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
            for(int id = (np + 1); id < num; id++) {
                mDestination[id].updateTo(new GpsParams(mDestination[id - 1].getLocation()));
                mDistance += mDestination[id].getDistance();
            }

        }
        else {
            mDestination[0].updateTo(params);
            for(int id = 1; id < num; id++) {
                mDestination[id].updateTo(new GpsParams(mDestination[id - 1].getLocation()));
            }
            
            mDistance = 0;
            for(int id = 0; id < num; id++) {
                /*
                 * For all upcoming, add distance. Distance is from way point to way point
                 */
                if(!mPassed[id]) {
                    mDistance += mDestination[id].getDistance();
                }
            }
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
        mEte = Helper.calculateEte(mPref.useBearingForETEA(), mDistance, params.getSpeed(), mBearing, params.getBearing());
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
        return Helper.makeLine(mDistance, Preferences.distanceConversionUnit, mEte, mBearing, mDeclination); 
    }

    /**
     * Activate flight plan
     */
    public void makeActive(GpsParams params) {
        mLastLocation = params;
        if(null != params) {
            mTrackShape.updateShapeFromPlan(getCoordinates());
        }
        mActive = true;
    }
    
    /**
     * Inativate flight plan
     */
    public void makeInactive() {
    	// If this is an externally loaded plan, then it specifically
    	// needs to be turned off
    	if(null != mExtPlanMgr) {
	    	ExternalFlightPlan plan = mExtPlanMgr.get(mName);
	    	if(null != plan) {
	    		plan.setActive(false);
	    	}
    	}
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
    public Coordinate[] getCoordinates() {
        int num = getDestinationNumber();
        
        Coordinate[] c = null;

        /*
         * Form a general path on the great circle of this plan
         */
        for(int id = 1; id < num; id++) {
            double lon0 = getDestination(id - 1).getLocation().getLongitude();
            double lat0 = getDestination(id - 1).getLocation().getLatitude();
            Projection p = new Projection(
                    getDestination(id).getLocation().getLongitude(), 
                    getDestination(id).getLocation().getLatitude(),
                    lon0, lat0);
            int segments = (int)p.getDistance() / MILES_PER_SEGMENT + 3; // Min 3 points
            Coordinate coord[] = p.findPoints(segments);

            coord[0].makeSeparate();
            if(null == c) {
                c = coord;
            }
            else {
                c = concat(c, coord);
            }
        }
        /*
         * Last circle
         */
        if(c != null) {
            c[c.length - 1].makeSeparate();
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
     * 
     */
    public void simulate() {
        int num = getDestinationNumber(); 
        if(num > 0) {
            updateLocation(new GpsParams(mDestination[num - 1].getLocation()));
        }
    }
    
    /**
     * Find a point withing close range of this
     * @param lon
     * @param lat
     */
    public int findClosePointId(double lon, double lat) {
        if(mActive) {
            int num = getDestinationNumber();
            for(int id = 0; id < num; id++) {
                Location l = mDestination[id].getLocation();
                double lon1 = l.getLongitude();
                double lat1 = l.getLatitude();
                double dist = (lon - lon1) * (lon -lon1) + (lat - lat1) * (lat - lat1);
                if(dist < Preferences.MIN_TOUCH_MOVEMENT_SQ_DISTANCE) {
                    return id;
                }
            }
        }
        return -1;
    }    
    
    /**
     * Used for rubberbanding only
     * Replace destination
     */
    public void replaceDestination(StorageService service, Preferences pref, int id, double lon, double lat, boolean finish) {
        boolean active = mActive;
        String airport = null;
        if(finish) {
            airport = service.getDBResource().findClosestAirportID(lon, lat);
        
            mReplaceId = id;

            mService = service;
            Destination d;
            if(null != airport) {
                d = new Destination(airport, Destination.BASE, pref, service);
            }
            else {
                d = new Destination(service, lon, lat);                
            }
            d.addObserver(this);
            d.find();
        }
        else {

            // replace
            mDestination[id] = new Destination(service, lon, lat);

            mTrackShape.updateShapeFromPlan(getCoordinates());
        }

        // keep active state
        mActive = active;
    }

    /**
     * insert destination in a plan at closests distance
     */
    public boolean insertDestination(Destination dest) {
        int n = getDestinationNumber();
        int index = -1;
        if(n >= MAX_DESTINATIONS) {
            return false;
        }

        if(n < 2) {
            /*
             * If none exist already, add it to the end, otherwise insert in between
             */
            mDestination[n] = dest;
        }
        else {
        
            /*
             * Find closest point
             */
            double dist1 = Double.MAX_VALUE;
            int indexc = 0;
            
            Coordinate[] coord = getCoordinates();
            
            for(int id = 0; id < coord.length; id++) {
                double lon = coord[id].getLongitude();
                double lat = coord[id].getLatitude();
                double lon1 = dest.getLocation().getLongitude();
                double lat1 = dest.getLocation().getLatitude();
                double dist = (lon - lon1) * (lon - lon1) + (lat - lat1) * (lat - lat1);
                
                if(coord[id].isSeparate()) {
                    indexc++;
                }
                 
                // first point
                if(dist < dist1) {
                    dist1 = dist;
                    index = indexc;
                }
            }
            
            if(index < 0 || index >= MAX_DESTINATIONS) {
                return false;
            }
            
            // add with passed flag intact
            boolean passed = mPassed[index]; 
            mDestination[n] = dest;
            mPassed[n] = passed;
            
            // insert
            move(n, index);
            
        }
        
        return(true);
    }

    /**
     * 
     */
    @Override
    public void update(Observable observable, Object data) {
        if(mReplaceId >= getDestinationNumber() || mReplaceId < 0) {
            return;
        }
        
        Destination d = (Destination)observable;
        mDestination[mReplaceId] = d;
        
        // If same as the one being dragged, update the dest on map
        if(mReplaceId == findNextNotPassed()) {
           mService.setDestinationPlanNoChange(d);
        }

        mTrackShape.updateShapeFromPlan(getCoordinates());
    }

    /**
     * A class that finds a station passage
     * @author zkhan
     *
     */
    private class Passage {

        double mInitBearing;
        double mCurrentBearing;
        
        public Passage() {
            mInitBearing = Double.MIN_VALUE;
        }

        /*
         * Algorithm to calculate passage.
         */
        private boolean hasPassed() {

            /*
             * no passing in sim mode
             */
            if(mPref.isSimulationMode()) {
                return false;
            }
            
            /*
             * quadrant change
             */
            if(mInitBearing >= 0 && mInitBearing <= 90) {
                if(mCurrentBearing <= 270 && mCurrentBearing >= 180) {
                    return true;
                }
            }
            if(mInitBearing >= 90 && mInitBearing <= 180) {
                if(mCurrentBearing <= 360 && mCurrentBearing >= 270) {
                    return true;
                }
            }
            if(mInitBearing >= 180 && mInitBearing <= 270) {
                if(mCurrentBearing <= 90 && mCurrentBearing >= 0) {
                    return true;
                }
            }
            if(mInitBearing >= 270 && mInitBearing <= 360) {
                if(mCurrentBearing <= 180 && mCurrentBearing >= 90) {
                    return true;
                }
            }
            
            
            /*
             * If previously we got close to a point, and now getting away, we reached.
             */
            if((mCurrentDistance > (MIN_REACHED_DISTANCE + 1)) && mReached) {
                mReached = false;
                return true;
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
            if(mInitBearing == Double.MIN_VALUE) {
                mInitBearing = (p.getBearing() + 360) % 360;
                return false;
            }
            
            mCurrentBearing = (p.getBearing() + 360) % 360;
            mCurrentDistance = p.getDistance();

            /*
             * Are we close enough?
             */
            if(mReached == false) {
                if(mCurrentDistance < MIN_REACHED_DISTANCE) {
                    mReached = true;
                }
            }
            
            return hasPassed();
        }
        
    }
    

}


