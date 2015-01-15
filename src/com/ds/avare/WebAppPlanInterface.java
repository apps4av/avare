/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare;


import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.plan.LmfsInterface;
import com.ds.avare.plan.LmfsPlan;
import com.ds.avare.plan.LmfsPlanList;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppPlanInterface implements Observer {
    private StorageService mService; 
    private Preferences mPref;
    private WebView mWebView;
    private SearchTask mSearchTask;
    private CreateTask mCreateTask;
    private Context mContext;
    private LinkedHashMap<String, String> mSavedPlans;
    private GenericCallback mCallback;
	private LmfsPlanList mFaaPlans;
	
    private static final int MSG_CLEAR_PLAN = 2;
    private static final int MSG_ADD_PLAN = 3;
    private static final int MSG_ADD_SEARCH = 4;
    private static final int MSG_TIMER = 5;
    private static final int MSG_CLEAR_PLAN_SAVE = 7;
    private static final int MSG_ADD_PLAN_SAVE = 8;
    private static final int MSG_NOTBUSY = 9;
    private static final int MSG_BUSY = 10;
    private static final int MSG_ACTIVE = 11;
    private static final int MSG_INACTIVE = 12;
    private static final int MSG_FILL_FORM = 13;
    private static final int MSG_SAVE_HIDE = 14;
    private static final int MSG_ERROR = 15;
    private static final int MSG_FAA_PLANS = 16;
    
    /** 
     * Instantiate the interface and set the context
     */
    WebAppPlanInterface(Context c, WebView ww, GenericCallback cb) {
        mPref = new Preferences(c);
        mWebView = ww;
        mContext = c;
        mCallback = cb;
    }

    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
        mSavedPlans = Plan.getAllPlans(mService, mPref.getPlans());
    }

    /**
     * 
     */
    public void timer() {
    	
    	Plan plan = mService.getPlan();

    	// If we are in sim mode, then send a message
	    if(mPref.isSimulationMode()) {
	    	mHandler.sendEmptyMessage(MSG_TIMER);
	    }
	    
	    // Also update active state
	    if(plan.isActive()) {
	    	mHandler.sendEmptyMessage(MSG_ACTIVE);	    	
	    	// Set destination next if plan active only
			if(plan.getDestination(plan.findNextNotPassed()) != null) {
				mService.setDestinationPlanNoChange(plan.getDestination(plan.findNextNotPassed()));
			}
	    }
	    else {
	    	mHandler.sendEmptyMessage(MSG_INACTIVE);  	
	    }
    }
    
    /**
     * 
     */
    public void clearPlan() {
    	mHandler.sendEmptyMessage(MSG_CLEAR_PLAN);
    }

    /**
     * 
     */
    public void clearPlanSave() {
    	mHandler.sendEmptyMessage(MSG_CLEAR_PLAN_SAVE);
    }

    /**
     * 
     * @param id
     * @param type
     */
    public void addWaypointToPlan(String id, String type, String subtype) {
    	// Add using javascript to show on page, strings require '' around them
    	Message m = mHandler.obtainMessage(MSG_ADD_PLAN, (Object)("'" + id + "','" + type + "','" + subtype + "'"));
    	mHandler.sendMessage(m);
    }

    /**
     * New saved plan list when the plan save list changes.
     */
    public void newSavePlan() {
    	clearPlanSave();
    	Iterator<String> it = (Iterator<String>) mSavedPlans.keySet().iterator();
    	while(it.hasNext()) {
        	Message m = mHandler.obtainMessage(MSG_ADD_PLAN_SAVE, (Object)("'" + it.next() + "'"));
        	mHandler.sendMessage(m);
        }
    }

    /**
     * New plan when the plan changes.
     */
    public void newPlan() {
        clearPlan();
        Plan plan = mService.getPlan();
        int num = plan.getDestinationNumber();
        for(int dest = 0; dest < num; dest++) {
        	Destination d = plan.getDestination(dest);
        	addWaypointToPlan(d.getID(), d.getType(), d.getFacilityName());
        }
    }

    /**
     * New dest
     * @param arg0
     * @param arg1
     */
	@Override
	public void update(Observable arg0, Object arg1) {
		/*
		 * Add to Plan that we found from add action
		 */
		Destination d = (Destination)arg0;
		mService.getPlan().appendDestination(d);
		addWaypointToPlan(d.getID(), d.getType(), d.getFacilityName());
	}
	
    /**
     * Move an entry in the plan
     */
    @JavascriptInterface
    public void moveUp() {
    	// surround JS each call with busy indication / not busy 

    	Plan plan = mService.getPlan();
    	// move active point up
    	int next = plan.findNextNotPassed();
    	if(next == 0) {
    		// cannot do already at top
    		return;
    	}
    	plan.move(next, next - 1);
    	plan.regress(); // move with the point
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	newPlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * Move an entry in the plan
     */
    @JavascriptInterface
    public void moveDown() {
    	// surround JS each call with busy indication / not busy 
    	
    	// move active point down
    	Plan plan = mService.getPlan();
    	
    	int next = plan.findNextNotPassed();
    	if(next == (plan.getDestinationNumber() - 1)) {
    		// cannot do already at bottom
    		return;
    	}

    	plan.move(next, next + 1);
    	plan.advance();
    	
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	newPlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    @JavascriptInterface
    public void discardPlan() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mService.newPlan();
    	mService.getPlan().setName("");
    	newPlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }


    /**
     * Activate/Deactivate the plan	
     */
    @JavascriptInterface
    public void activateToggle() {

    	Plan plan = mService.getPlan();
    	if(plan.isActive()) {
    		plan.makeInactive();
	    	mHandler.sendEmptyMessage(MSG_INACTIVE);
    		mService.setDestination(null);
    	}
    	else {
    		plan.makeActive(mService.getGpsParams());    		
	    	mHandler.sendEmptyMessage(MSG_ACTIVE);
    		if(plan.getDestination(plan.findNextNotPassed()) != null) {
    			mService.setDestinationPlanNoChange(plan.getDestination(plan.findNextNotPassed()));
    		}
    	}
    }

    /**
     * 
     * @param num
     */
    @JavascriptInterface
    public void deleteWaypoint() {
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	// Delete the one that has a mark on it, or the active waypoint
    	mService.getPlan().remove(mService.getPlan().findNextNotPassed());
    	newPlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    @JavascriptInterface
    public void moveBack() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mService.getPlan().regress();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    @JavascriptInterface
    public void moveForward() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mService.getPlan().advance();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param id
     * @param type
     */	
    @JavascriptInterface
    public void addToPlan(String id, String type, String subtype) {
    	/*
    	 * Add from JS search query
    	 */
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	Destination d = new Destination(id, type, mPref, mService);
    	d.addObserver(this);
    	d.find(subtype);
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void savePlan(String name) {

    	Plan plan = mService.getPlan();
    	if(plan.getDestinationNumber() < 2) {
    		// Anything less than 2 is not a plan
    		return;
    	}
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	plan.setName(name);
    	String format = plan.putPlanToStorageFormat();
    	mSavedPlans.put(name, format);
    	mPref.putPlans(Plan.putAllPlans(mService, mSavedPlans));
    	newSavePlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param index
     */
    @JavascriptInterface
    public void loadPlan(String name) {
    	// surround JS each call with busy indication / not busy 
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// If we have an active plan, we need to turn it off now since we are
    	// loading a new one.
    	if(null != mService.getPlan()) {
    		mService.getPlan().makeInactive();
    	}

    	// Clear out any destination that may have been set.
		mService.setDestination(null);

    	mService.newPlanFromStorage(mSavedPlans.get(name), false);
    	mService.getPlan().setName(name);
    	newPlan();
    	
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param index
     */
    @JavascriptInterface
    public void loadPlanReverse(String name) {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mService.newPlanFromStorage(mSavedPlans.get(name), true);
    	mService.getPlan().setName(name);
    	newPlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * Called from the javascript page when we need to build a new
     * list of flight plans that are screened by the indicated filter
     * 
     * @param planFilter flight plan must contain this string
     */
    @JavascriptInterface
    public void planFilter(String planFilter) {
    	
    	// Turn on the spinny wheel to indicate we are thinking
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// Clear out the list of plans
    	clearPlanSave();
    	
    	// Make the filter string uppercase
    	planFilter = planFilter.toUpperCase();
    	
    	// Get the starting iterator of our collection of plans
    	Iterator<String> it = (Iterator<String>) mSavedPlans.keySet().iterator();
    	
    	// While we have a 'next' plan
    	while(it.hasNext()) {
    		
    		// Get the name of the plan
    		String planName = it.next();
    		
    		//Make an upper case copy
    		String planNameUC = planName.toUpperCase();
    		
    		// If the plan name contains the plan filter string
    		if(true == planNameUC.contains(planFilter)) {
    			
    			// Tell the web page to add this plan name to its list
	        	Message m = mHandler.obtainMessage(MSG_ADD_PLAN_SAVE, (Object)("'" + planName + "'"));
	        	mHandler.sendMessage(m);
    		}
        }

    	// Tell the plan list table to show the plans
    	Message m = mHandler.obtainMessage(MSG_SAVE_HIDE, (Object)("true"));
    	mHandler.sendMessage(m);
    	
    	// Turn off the spinny wheel thingy
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param num
     */
    @JavascriptInterface
    public void saveDelete(String name) {
    	
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mSavedPlans.remove(name);
    	mPref.putPlans(Plan.putAllPlans(mService, mSavedPlans));
    	newSavePlan();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /** 
     * Search for some place
     */
    @JavascriptInterface
    public void search(String value) {
        
    	/*
         * If text is 0 length or too long, then do not search, show last list
         */
        if(0 == value.length()) {
            return;
        }
        
        if(null != mSearchTask) {
            if (!mSearchTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                /*
                 * Cancel the last query
                 */
                mSearchTask.cancel(true);
            }
        }

    	mHandler.sendEmptyMessage(MSG_BUSY);
        mSearchTask = new SearchTask();
        mSearchTask.execute(value);
    }

    /** 
     * JS polls every second to get all plan data.
     */
    @JavascriptInterface
    public String getPlanData() {
    	Plan plan = mService.getPlan();
        /*
         * Now update HTML with latest plan stuff, do this every time we start the Plan screen as 
         * things might have changed.
         */
    	int passed = plan.findNextNotPassed();
    	int numDest = plan.getDestinationNumber();
    	double declination = 0;
    	// add plan name upfront
    	String plans = plan.getName() + "::::";
    	if(mService.getGpsParams() != null) {
    		declination = mService.getGpsParams().getDeclinition();
    	}
    	
    	// make a :: separated plan list, then add totals to it
    	for(int num = 0; num < numDest; num++) {
    		plans += 
    				(passed == num ? 1 : 0) + "," +
    				Math.round(Helper.getMagneticHeading(plan.getDestination(num).getBearing(), declination)) + "," + 
    				Math.round(plan.getDestination(num).getDistance()) + "," +
    				plan.getDestination(num).getEte() + "::::";
    	}
    	// add total
    	plans += plan.toString();
    	
    	return plans;
    }


    /** 
     * Create a plan, guessing the types
     */
    @JavascriptInterface
    public void createPlan(String value) {
        
    	/*
         * If text is 0 length or too long, then do not search, show last list
         */
        if(0 == value.length()) {
            return;
        }

        if(null != mCreateTask) {
            if (!mCreateTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                /*
                 * Cancel the last query
                 */
                mCreateTask.cancel(true);
            }
        }

    	mHandler.sendEmptyMessage(MSG_BUSY);
        mCreateTask = new CreateTask();
        mCreateTask.execute(value);
    }

    /**
     * Fill plan form with data stored
     */
    @JavascriptInterface
    public void fillPlan() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// Fill in from storage, this is going to be mostly reflecting the user's most 
    	// used settings in the form
    	LmfsPlan pl = new LmfsPlan(mPref.getLMFSPlan());
    	
    	// If plan has valid BASE origin and destinations, fill them in
    	if(mService != null) {
    		pl.setFromPlan(mService.getPlan());
    	}
    	
    	// Fill form
    	Message m = mHandler.obtainMessage(MSG_FILL_FORM, (Object)(
    	    	"'" +  pl.flightRules  + "'," +
    			"'" +  pl.aircraftIdentifier + "'," +
    			"'" +  pl.departure + "'," +
    			"'" +  pl.destination + "'," +
    			"'" +  pl.departureInstant + "'," + 
    			"'" +  LmfsPlan.durationToTime(pl.flightDuration) + "'," +
    			"'" +  pl.altDestination1 + "'," + 
    			"'" +  pl.altDestination2 + "'," + 
    			"'" +  pl.aircraftType + "'," +
    			"'" +  pl.numberOfAircraft + "'," +
    			"'" +  pl.heavyWakeTurbulence + "'," +
    			"'" +  pl.aircraftEquipment + "'," +
    			"'" +  pl.speedKnots + "'," + 
    			"'" +  pl.altitudeFL + "'," +
    			"'" +  LmfsPlan.durationToTime(pl.fuelOnBoard) + "'," + 
    			"'" +  pl.pilotData + "'," +
    			"'" +  pl.peopleOnBoard + "'," + 
    			"'" +  pl.aircraftColor + "'," +
    			"'" +  pl.route + "'," +
    			"'" +  pl.type + "'," +
    			"'" +  pl.remarks + "'"
    			));
    	mHandler.sendMessage(m);
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }
    
    
    /** 
     * File an FAA plan and save it
     */
    @JavascriptInterface
    public void filePlan(
    	String flightRules,
    	String aircraftIdentifier,
    	String departure,
    	String destination,
    	String departureInstant, 
    	String flightDuration,
    	String altDestination1, 
    	String altDestination2, 
    	String aircraftType,
    	String numberOfAircraft,
    	String heavyWakeTurbulence,
    	String aircraftEquipment,
    	String speedKnots, 
    	String altitudeFL,
    	String fuelOnBoard, 
    	String pilotData,
    	String peopleOnBoard, 
    	String aircraftColor,
    	String route,
    	String type,
    	String remarks) {
        
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	LmfsPlan pl = new LmfsPlan();
    	pl.flightRules = flightRules;
    	pl.aircraftIdentifier = aircraftIdentifier;
    	pl.departure = departure;
    	pl.destination = destination;
    	pl.departureInstant = LmfsPlan.getTimeFromInput(departureInstant);
    	pl.flightDuration = LmfsPlan.getDurationFromInput(flightDuration);
    	pl.altDestination1 = altDestination1; 
    	pl.altDestination2 = altDestination2; 
    	pl.aircraftType = aircraftType;
    	pl.numberOfAircraft = numberOfAircraft;
    	pl.heavyWakeTurbulence = heavyWakeTurbulence;
    	pl.aircraftEquipment = aircraftEquipment;
    	pl.speedKnots = speedKnots; 
    	pl.altitudeFL = altitudeFL;
    	pl.fuelOnBoard = LmfsPlan.getDurationFromInput(fuelOnBoard); 
    	pl.pilotData = pilotData;
    	pl.peopleOnBoard = peopleOnBoard; 
    	pl.aircraftColor = aircraftColor;
    	pl.route = route;
    	pl.type = type;
    	pl.remarks = remarks;
 
    	// Save user input for auto fill
    	mPref.saveLMFSPlan(pl.makeJSON());
    	
    	// Now file and show error messages
    	LmfsInterface infc = new LmfsInterface(mContext);
    	infc.fileFlightPlan(pl);
    	String err = infc.getError();
    	if(null == err) {
    		// success filing
    		getPlans();
    		return;
    	}
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);
    	
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    
    /**
     * Close, open plan at FAA
     */
    @JavascriptInterface
    public void planChangeState(int row, String action) {
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || row >= mFaaPlans.getPlans().size()) {
    		return;
    	}
    	
    	/*
    	 * Do the action of the plan
    	 */
    	LmfsInterface infc = new LmfsInterface(mContext);

    	String err = null;
    	String id = mFaaPlans.getPlans().get(row).getId();
    	String ver = mFaaPlans.getPlans().get(row).versionStamp;
    	if(id == null) {
    		return;
    	}
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	if(action.equals("Activate")) {
    		// Activate plan with given ID
    		infc.activateFlightPlan(id, ver);
    	}
    	else if(action.equals("Close")) {
    		// Activate plan with given ID
    		infc.closeFlightPlan(id);
    	}
    	else if(action.equals("Cancel")) {
    		// Activate plan with given ID
    		infc.cancelFlightPlan(id);
    	}
    	err = infc.getError();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    	if(null == err) {
    		// success changing, update state
    		getPlans();
    		return;
    	}
    	
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);    	
    }


    /** 
     * Get a list of FAA plans
     */
    @JavascriptInterface
    public void getPlans() {      
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	LmfsInterface infc = new LmfsInterface(mContext);

    	mFaaPlans = infc.getFlightPlans();
    	String err = infc.getError();
    	if(null == err) {
    		// success filing
    		err = mContext.getString(R.string.Success);
    	}
    	
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);
    	
    	mHandler.sendEmptyMessage(MSG_FAA_PLANS);

    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * @author zkhan
     *
     */
    private class CreateTask extends AsyncTask<Object, Void, Boolean> {

    	String selection[] = null;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            Thread.currentThread().setName("Create");

            if(null == mService) {
                return false;
            }

            String srch[] = ((String)vals[0]).toUpperCase(Locale.US).split(" ");
            
            /*
             * Here we guess types since we do not have user select
             */
            selection = new String[srch.length];

            for(int num = 0; num < srch.length; num++) {
	            /*
	             * This is a geo coordinate with &?
	             */
	            if(srch[num].contains("&")) {
	            	selection[num] = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS, srch[num])).getHashedName();
	            	continue;
	            }
	            
	            /*
	             * Search from database. Make this a simple one off search
	             */
	            StringPreference s = mService.getDBResource().searchOne(srch[num]);
	            if(s != null) {
	                selection[num] = s.getHashedName();
	            }
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            /*
             * Set new search adapter
             */

            if(null == selection || false == result) {
            	mHandler.sendEmptyMessage(MSG_NOTBUSY);
                return;
            }
            
            /*
             * Add each to the plan search
             */
            for (int num = 0; num < selection.length; num++) {
            	String val = selection[num];
            	if(null == val) {
            		continue;
            	}
	            String id = StringPreference.parseHashedNameId(val);
	            String type = StringPreference.parseHashedNameDestType(val);
	            String dbtype = StringPreference.parseHashedNameDbType(val);

	        	/*
	        	 * Add each
	        	 */
	        	Destination d = new Destination(id, type, mPref, mService);
	        	d.addObserver(WebAppPlanInterface.this);
	        	d.find(dbtype);
            }
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
        }
    }


    /**
     * @author zkhan
     *
     */
    private class SearchTask extends AsyncTask<Object, Void, Boolean> {

    	String selection[] = null;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            Thread.currentThread().setName("Search");

            String srch = ((String)vals[0]).toUpperCase(Locale.US);
            if(null == mService) {
                return false;
            }

            /*
             * This is a geo coordinate with &?
             */
            if(srch.contains("&")) {

            	selection = new String[1];
            	selection[0] = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS, srch)).getHashedName();
                return true;
            }
            
            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            /*
             * Search from database. Make this a simple search
             */
            mService.getDBResource().search(srch, params, true);
            if(params.size() > 0) {
                selection = new String[params.size()];
                int iterator = 0;
                for(String key : params.keySet()){
                    selection[iterator] = StringPreference.getHashedName(params.get(key), key);
                    iterator++;
                }
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            /*
             * Set new search adapter
             */

            if(null == selection || false == result) {
            	mHandler.sendEmptyMessage(MSG_NOTBUSY);
                return;
            }
            
            /*
             * Add each to the plan search
             */
            for (int num = 0; num < selection.length; num++) {
            	String val = selection[num];
            	
	            String id = StringPreference.parseHashedNameId(val);
	            String name = StringPreference.parseHashedNameFacilityName(val);
	            String type = StringPreference.parseHashedNameDestType(val);
	            String dbtype = StringPreference.parseHashedNameDbType(val);

	        	Message m = mHandler.obtainMessage(MSG_ADD_SEARCH, (Object)("'" + id + "','" + name + "','" + type + "','" + dbtype + "'"));
	        	mHandler.sendMessage(m);
            }
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
        }
    }


    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     * Must use handler for functions called from JS, but for uniformity, call all JS from this handler
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Plan plan = mService.getPlan();
        	if(MSG_CLEAR_PLAN == msg.what) {
        		mWebView.loadUrl("javascript:plan_clear()");
        	}
        	else if(MSG_ADD_PLAN == msg.what) {
            	String func = "javascript:plan_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ADD_SEARCH == msg.what) {
            	String func = "javascript:search_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if (MSG_TIMER == msg.what) {
        		plan.simulate();
        	}
        	else if(MSG_CLEAR_PLAN_SAVE == msg.what) {
            	String func = "javascript:save_clear()";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ADD_PLAN_SAVE == msg.what) {
            	String func = "javascript:save_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_NOTBUSY == msg.what) {
        		mCallback.callback((Object)PlanActivity.UNSHOW_BUSY, null);
        	}
        	else if(MSG_BUSY == msg.what) {
        		mCallback.callback((Object)PlanActivity.SHOW_BUSY, null);
        	}
        	else if(MSG_ACTIVE == msg.what) {
        		mCallback.callback((Object)PlanActivity.ACTIVE, null);
        	}
        	else if(MSG_INACTIVE == msg.what) {
        		mCallback.callback((Object)PlanActivity.INACTIVE, null);
        	}
           	else if(MSG_FILL_FORM == msg.what) {	
            	String func = "javascript:plan_fill(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
           	else if(MSG_SAVE_HIDE == msg.what) {	
            	String func = "javascript:save_hide(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ERROR == msg.what) {	
        		mCallback.callback((Object)PlanActivity.MESSAGE, msg.obj);
        	}
        	else if(MSG_FAA_PLANS == msg.what) {
        		/*
        		 * Fill the table of plans
        		 */
        		if(mFaaPlans.getPlans() == null) {
        			return;
        		}
        		String p = "";
        		for (LmfsPlan pl : mFaaPlans.getPlans()) {
        			p += pl.departure + "-" + pl.destination + "-" + pl.aircraftIdentifier + "," + pl.currentState + ",";
        		}
        		String func = "javascript:set_faa_plans('" + p + "')";
            	mWebView.loadUrl(func);
        	}
        }
    };

}