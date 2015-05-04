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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONArray;
import org.json.JSONObject;

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
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 */
public class Plan implements Observer {

    private Destination[] mDestination;
    private boolean[] mPassed;

    private static final int MAX_DESTINATIONS = 100;

    private static final int MILES_PER_SEGMENT = 50;

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
    private String mName;
    private boolean mEarlyPass;
    private boolean mEarlyPassEvent;
    private boolean mSuspend = false;

    /**
     * 
     * @param dataSource
     */
    public Plan(Context ctx, StorageService service) {
        mPref = new Preferences(ctx);
        mService = service;
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
        mDestination = new Destination[MAX_DESTINATIONS];
        mPassed = new boolean[MAX_DESTINATIONS];
        for (int i = 0; i < MAX_DESTINATIONS; i++) {
            mPassed[i] = false;
        }
        mEte = "--:--";
        mPassage = new Passage();
        mEarlyPass = false;
        mEarlyPassEvent = false;
        mName = null;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    /**
     * 
     */
    public Destination getDestination(int index) {
        /*
         * Check for null.
         */
        if (index >= MAX_DESTINATIONS) {
            return null;
        }
        return (mDestination[index]);
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
        for (id = 0; id < MAX_DESTINATIONS; id++) {
            if (getDestination(id) == null) {
                break;
            }
        }
        return (id);
    }

    /**
     * 
     * @return
     */
    public void remove(int rmId) {
        int num = getDestinationNumber() - 1;
        if (rmId > num || rmId < 0) {
            return;
        }
        mDestination[rmId] = null;
        mPassed[rmId] = false;
        for (int id = rmId; id < num; id++) {
            mDestination[id] = mDestination[id + 1];
            mDestination[id + 1] = null;
            mPassed[id] = mPassed[id + 1];
            mPassed[id + 1] = false;
        }
        if (getDestinationNumber() > 0) {
            if (mLastLocation != null) {
                mTrackShape.updateShapeFromPlan(getCoordinates());
            }
        } else {
            mTrackShape = new TrackShape();
        }
    }

    /**
     * 
     * @return
     */
    public void move(int from, int to) {
        int num = getDestinationNumber();
        if (from >= num || to >= num) {
            return;
        }
        if (from > to) {
            /*
             * Move everything down
             */
            Destination tmp = mDestination[from];
            for (int i = from; i > to; i--) {
                mDestination[i] = mDestination[i - 1];
            }
            mDestination[to] = tmp;
        } else if (from < to) {
            /*
             * Move everything up
             */
            Destination tmp = mDestination[from];
            for (int i = from; i < to; i++) {
                mDestination[i] = mDestination[i + 1];
            }
            mDestination[to] = tmp;
        }

        if (num > 0) {
            if (mLastLocation != null) {
                mTrackShape.updateShapeFromPlan(getCoordinates());
            }
        } else {
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
        if (n >= MAX_DESTINATIONS) {
            return false;
        }

        if (n > 0) {
            /*
             * Check if last set was set again, it makes no sense to have same
             * dest twice in sequence
             */
            if (mDestination[n - 1].getStorageName().equals(
                    dest.getStorageName())) {
                return false;
            }
        }
        mDestination[n] = dest;
        mPassed[n] = false;
        if (null == mLastLocation) {
            mLastLocation = new GpsParams(mDestination[n].getLocationInit());
        }
        mTrackShape.updateShapeFromPlan(getCoordinates());

        return (true);
    }

    /*
     * Find the next not passed destination
     */
    public int findNextNotPassed() {
        /*
         * 
         */
        for (int id = 0; id < getDestinationNumber(); id++) {
            if (!mPassed[id]) {
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
        if (0 == num) {
            mPassage = new Passage();
            return;
        }

        /*
         * Depends if it is active or plan
         */
        if (mActive) {

            /*
             * For all passed way points set distance to current
             */
            for (int id = 0; id <= np; id++) {
                mDestination[id].updateTo(params);
            }
            mDistance = mDestination[np].getDistance();

            /*
             * For all upcoming, add distance. Distance is from way point to way
             * point
             */
            for (int id = (np + 1); id < num; id++) {
                Location l = mDestination[id - 1].getLocation();
                l.setSpeed((float)GpsParams.speedConvert(params.getSpeed()));
                l.setBearing((float)params.getBearing());
                GpsParams p = new GpsParams(l);
                mDestination[id].updateTo(p);
                mDistance += mDestination[id].getDistance();
            }

        } else {
            mDestination[0].updateTo(params);
            for (int id = 1; id < num; id++) {
                mDestination[id].updateTo(new GpsParams(mDestination[id - 1]
                        .getLocation()));
            }

            mDistance = 0;
            for (int id = 0; id < num; id++) {
                /*
                 * For all upcoming, add distance. Distance is from way point to
                 * way point
                 */
                if (!mPassed[id]) {
                    mDistance += mDestination[id].getDistance();
                }
            }
        }

        if(false == mSuspend) {
	        if (num > 0) {
	            mBearing = mDestination[findNextNotPassed()].getBearing();
	            if (mPassage.updateLocation(params,
	                    mDestination[findNextNotPassed()])) {
	                /*
	                 * Passed. Go to next. Only when active
	                 */
	                if (mActive) {
	                    mPassed[findNextNotPassed()] = true;
	                    mDestChanged = true;
	                }
	            }
	        }
        }
        mEte = Helper.calculateEte(mPref.useBearingForETEA() && (!isActive()), mDistance,
                params.getSpeed(), mBearing, params.getBearing());
        mLastLocation = params;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        /*
         * For display purpose
         */
        return Helper.makeLine(mDistance, Preferences.distanceConversionUnit,
                mEte, mBearing, mDeclination);
    }

    /**
     * Activate flight plan
     */
    public void makeActive(GpsParams params) {
        mLastLocation = params;
        if (null != params) {
            mTrackShape.updateShapeFromPlan(getCoordinates());
        }

        // If this is an externally defined plan, then it specifically
        // needs to be turned on
        if (null != mService) {
            ExternalFlightPlan efp = mService.getExternalPlanMgr().get(mName);
            if (null != efp) {
                efp.setActive(true);
                mService.getNavComments().setLeft(efp.getCmt());
            }
        }
        mActive = true;
    }

    /**
     * Inactivate flight plan
     */
    public void makeInactive() {
        // If this is an externally defined plan, then it specifically
        // needs to be turned off
        if (null != mService) {
            ExternalFlightPlan efp = mService.getExternalPlanMgr().get(mName);
            if (null != efp) {
                efp.setActive(false); // Turn off the plan
                mService.getNavComments().clear();
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
        for (int id = 1; id < num; id++) {
            double lon0 = getDestination(id - 1).getLocation().getLongitude();
            double lat0 = getDestination(id - 1).getLocation().getLatitude();
            Projection p = new Projection(getDestination(id).getLocation()
                    .getLongitude(), getDestination(id).getLocation()
                    .getLatitude(), lon0, lat0);
            int segments = (int) p.getDistance() / MILES_PER_SEGMENT + 3; // Min
                                                                            // 3
                                                                            // points
            Coordinate coord[] = p.findPoints(segments);

            coord[0].makeSeparate();
            Coordinate.setLeg(coord, id - 1);
            if (null == c) {
                c = coord;
            } else {
                c = concat(c, coord);
            }
        }
        /*
         * Last circle
         */
        if (c != null) {
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
        if (getDestinationNumber() > 0) {
            // Now if we have at least one destination, set GPS coords
            // to the next not passed to simulate we are there.
            // This gives accurate plan total from start of plan
            updateLocation(new GpsParams(
                    mDestination[findNextNotPassed()].getLocation()));
        }
    }

    /**
     * Find a point withing close range of this
     * 
     * @param lon
     * @param lat
     */
    public int findClosePointId(double lon, double lat, double factor) {
        if (mActive) {
            int num = getDestinationNumber();
            for (int id = 0; id < num; id++) {
                Location l = mDestination[id].getLocation();
                double lon1 = l.getLongitude();
                double lat1 = l.getLatitude();
                double dist = (lon - lon1) * (lon - lon1) + (lat - lat1)
                        * (lat - lat1);
                if (dist < (Preferences.MIN_TOUCH_MOVEMENT_SQ_DISTANCE / factor)) {
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Used for rubberbanding only Replace destination
     */
    public void replaceDestination(Preferences pref, int id, double lon,
            double lat, boolean finish) {
        boolean active = mActive;
        String airport = null;
        if (finish) {
            airport = mService.getDBResource().findClosestAirportID(lon, lat);

            mReplaceId = id;

            Destination d;
            if (null != airport) {
                d = new Destination(airport, Destination.BASE, pref, mService);
            } else {
                d = new Destination(mService, lon, lat);
            }
            d.addObserver(this);
            d.find();
        } else {

            // replace
            mDestination[id] = new Destination(mService, lon, lat);

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
        if (n >= MAX_DESTINATIONS) {
            return false;
        }

        if (n < 2) {
            /*
             * If none exist already, add it to the end, otherwise insert in
             * between
             */
            mDestination[n] = dest;
        } else {

            /*
             * Find closest point
             */
            double dist1 = Double.MAX_VALUE;
            int indexc = 0;

            Coordinate[] coord = getCoordinates();

            for (int id = 0; id < coord.length; id++) {
                double lon = coord[id].getLongitude();
                double lat = coord[id].getLatitude();
                double lon1 = dest.getLocation().getLongitude();
                double lat1 = dest.getLocation().getLatitude();
                double dist = (lon - lon1) * (lon - lon1) + (lat - lat1)
                        * (lat - lat1);

                if (coord[id].isSeparate()) {
                    indexc++;
                }

                // first point
                if (dist < dist1) {
                    dist1 = dist;
                    index = indexc;
                }
            }

            if (index < 0 || index >= MAX_DESTINATIONS) {
                return false;
            }

            // add with passed flag intact
            boolean passed = mPassed[index];
            mDestination[n] = dest;
            mPassed[n] = passed;

            // insert
            move(n, index);

        }

        return (true);
    }

    /**
     * 
     */
    @Override
    public void update(Observable observable, Object data) {
        if (mReplaceId >= getDestinationNumber() || mReplaceId < 0) {
            return;
        }

        Destination d = (Destination) observable;
        mDestination[mReplaceId] = d;

        // If same as the one being dragged, update the dest on map
        if (mReplaceId == findNextNotPassed()) {
            mService.setDestinationPlanNoChange(d);
        }

        mTrackShape.updateShapeFromPlan(getCoordinates());
    }

    /**
     * A class that finds a station passage
     * 
     * @author zkhan
     *
     */
    private class Passage {

        double mLastDistance;
        double mLastBearing;
        double mCurrentDistance;
        double mCurrentBearing;
        double mSpeed;
        
        // Use this to set early pass flag, meaning we are close to our dest.
        private static final double EARLY_PASS_THRESHOLD = 23; // seconds

        // The idea is to adjust the passage distance to lower value near the
        // airport to
        // have the approaches work properly.
        // For enroute 8+ NM, your pass zone is 2 NM
        private static final double PASSAGE_ENROUTE_DISTANCE_MIN = 2;

        // For approach (<8NM) your pass zone is 0.4 miles
        private static final double PASSAGE_APPROACH_MIN = 0.4;
        private static final double PASSAGE_APPROACH_DISTANCE = 8;

        public Passage() {
            mLastDistance = -1;
            mLastBearing = -1;
            mSpeed = -1;
        }

        /*
         * Algorithm to calculate passage.
         */
        private boolean hasPassed(double distanceFromLanding) {

            double max;
            if (distanceFromLanding < PASSAGE_APPROACH_DISTANCE) {
                max = PASSAGE_APPROACH_MIN;
            } else {
                max = PASSAGE_ENROUTE_DISTANCE_MIN;
            }

            /*
             * no passing in sim mode
             */
            if (mPref.isSimulationMode()) {
                return false;
            }

            /*
             * Find early pass
             */
            double timerem = (mCurrentDistance / mSpeed) * 3600;
            if (timerem < EARLY_PASS_THRESHOLD) {
                if (!mEarlyPass) {
                    mEarlyPass = true;
                    mEarlyPassEvent = true;
                }
            }

            /*
             * Distance increases
             */
            if (mCurrentDistance > mLastDistance) {
                /*
                 * We are in passage zone
                 */
                if (mCurrentDistance < max) {
                    mEarlyPass = false;
                    mEarlyPassEvent = false;
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
        	
            Projection p = new Projection(params.getLongitude(),
                    params.getLatitude(),
                    nextDest.getLocation().getLongitude(), nextDest
                            .getLocation().getLatitude());

            if (mLastBearing < 0) {
                /*
                 * Init on first input on location
                 */
                mLastDistance = p.getDistance();
                mLastBearing = (p.getBearing() + 360) % 360;
                return false;
            }

            mCurrentDistance = p.getDistance();
            mSpeed = params.getSpeed();
            mCurrentBearing = (p.getBearing() + 360) % 360;

            Destination lastDest = mDestination[getDestinationNumber() - 1];
            Projection plast = new Projection(params.getLongitude(),
                    params.getLatitude(),
                    lastDest.getLocation().getLongitude(), lastDest
                            .getLocation().getLatitude());

            boolean ret = hasPassed(plast.getDistance());

            mLastDistance = mCurrentDistance;
            mLastBearing = mCurrentBearing;
            return ret;
        }
    }

    // Regress to the PREVIOUS waypoint in the plan
    // We do this by searching from the end of the plan
    // to find the first waypoint that is marked as passed and changing
    // it to NOT passed, then setting a new destination
    public void regress() {
        int passed = findNextNotPassed() - 1;
        if (passed >= 0) {
            setNotPassed(passed);
            Destination dest = getDestination(passed);
            mService.setDestinationPlanNoChange(dest);
        }
    }

    // Advance to the next waypoint in the plan
    // Search each one to find the first point we have
    // not yet passed. Mark that as passed and set destination to the one
    // after.
    public void advance() {
        int notpassed = findNextNotPassed();
        if (notpassed == (getDestinationNumber() - 1)) {
            return;
        }
        setPassed(notpassed);
        Destination dest = getDestination(notpassed + 1);
        mService.setDestinationPlanNoChange(dest);
    }
    
    // Suspend the algorithm that determines waypoint passage
    public boolean suspendResume() {
    	mSuspend = !mSuspend;
    	return mSuspend;
    }

    /**
     * Put this plan in JSON array
     * 
     * @param cls
     * @return
     */
    public String putPlanToStorageFormat() {
        /*
         * Put in JSON array all destinations in storage types.
         */
        JSONArray jsonArr = new JSONArray();
        for (int pln = 0; pln < getDestinationNumber(); pln++) {

            Destination d = mDestination[pln];
            jsonArr.put(d.getStorageName());
        }

        return jsonArr.toString();
    }

    /**
     * Get plan made from JSON object
     * 
     * @param cls
     * @return
     */
    public Plan(Context ctx, StorageService service, String json,
            boolean reverse) {
        /*
         * Do as the other constructor does, but make a plan from json
         */

        mPref = new Preferences(ctx);
        mService = service;
        clear();

        JSONArray jsonArr = null;
        try {
            jsonArr = new JSONArray(json);
        } catch (Exception e) {
        }

        if (null == jsonArr) {
            return;
        }

        int num;
        for (int i = 0; i < jsonArr.length(); i++) {
            try {
                String dest = jsonArr.getString(i);
                String id = StringPreference.parseHashedNameId(dest);
                String type = StringPreference.parseHashedNameDestType(dest);
                String dbtype = StringPreference.parseHashedNameDbType(dest);

                /*
                 * Reverse load if asked
                 */
                if (reverse) {
                    num = jsonArr.length() - i - 1;
                } else {
                    num = i;
                }
                mDestination[num] = new Destination(id, type, mPref, mService);
                mDestination[num].find(dbtype);
            } catch (Exception e) {
                continue;
            }
        }
    }

    /**
     * Hashmap is used for storing ALL plans
     * 
     * @param plans
     * @return
     */
    public static LinkedHashMap<String, String> getAllPlans(
            StorageService service, String plans) {

        // Linked hashmap as we want to keep the order of plans
        // hashmap because that deals with updating plans
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

        // parse JSON from storage
        try {
            JSONObject json = new JSONObject(plans);
            Iterator<?> keys = json.keys();
            while (keys.hasNext()) {
                String name = (String) keys.next();
                String destinations = json.getString(name);
                map.put(name, destinations);
            }
        } catch (Exception e) {
        }

        // Now fetch the external plans
        if (null != service) {
            ExternalPlanMgr epm = service.getExternalPlanMgr();
            ArrayList<String> planNames = epm.getPlanNames(null);
            for (String planName : planNames) {
                ExternalFlightPlan extPlan = epm.get(planName);
                map.put(planName, extPlan.toJSONString());
            }
        }

        // Return the map
        return map;
    }

    /**
     * Build and return a string for storage that represents all internally
     * managed plans
     * 
     * @param service
     *            - the Storage service
     * @param map
     *            collection of known flight plans
     * @return json string to save
     */
    @SuppressWarnings("unchecked")
    public static String putAllPlans(StorageService service,
            LinkedHashMap<String, String> map) {

        // We need to make a copy here to work on. "map" as passed in may
        // contain externally saved
        // flight plans.
        LinkedHashMap<String, String> localMap = (LinkedHashMap<String, String>) map
                .clone();

        // For all of the known external flight plans, we need to remove that
        // name from the cloned
        // map so that it will not be saved to memory
        // TODO: Abstract all plans to a single base class and this step can be
        // removed
        if (null != service) {
            ExternalPlanMgr epm = service.getExternalPlanMgr();
            ArrayList<String> planNames = epm.getPlanNames(null);
            for (String planName : planNames) {
                if (true == localMap.containsKey(planName)) {
                    localMap.remove(planName);
                }
            }
        }

        // Put a collection of plans in storage format
        JSONObject json = new JSONObject(localMap);
        return json.toString();
    }

    /**
     * Get total distance remaining
     * 
     * @return
     */
    public double getDistance() {
        return mDistance;
    }

    /**
     * Move index forward to a given place
     * 
     * @param index
     */
    public void moveTo(int index) {
        int num = getDestinationNumber();
        if ((index < 0) || (index >= num)) {
            return;
        }

        for (int i = 0; i < num; i++) {
            mPassed[i] = false;
        }
        for (int i = 0; i < index; i++) {
            mPassed[i] = true;
        }
    }

    /**
     * See if early pass is set
     * 
     * @return
     */
    public boolean isEarlyPass() {
        boolean pass = mEarlyPassEvent;
        mEarlyPassEvent = false;
        return pass;
    }
}
