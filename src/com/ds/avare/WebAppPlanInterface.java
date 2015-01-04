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
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;

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
    private Context mContext;
    private LinkedHashMap<String, String> mSavedPlans;
    
    private static final int MSG_UPDATE_PLAN = 1;
    private static final int MSG_CLEAR_PLAN = 2;
    private static final int MSG_ADD_PLAN = 3;
    private static final int MSG_ADD_SEARCH = 4;
    private static final int MSG_TIMER = 5;
    private static final int MSG_CLEAR_PLAN_SAVE = 7;
    private static final int MSG_ADD_PLAN_SAVE = 8;
    
    /** 
     * Instantiate the interface and set the context
     */
    WebAppPlanInterface(Context c, WebView ww) {
        mPref = new Preferences(c);
        mWebView = ww;
        mContext = c;
        mSavedPlans = Plan.getAllPlans(mPref.getPlans());
    }

    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
    }

    /**
     * 
     */
    public void timer() {
	    // If we are in sim mode, then send a message
	    if(mPref.isSimulationMode()) {
	    	mHandler.sendEmptyMessage(MSG_TIMER);
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
        updatePlan();
    }

    /**
     * Update the passed point on the Plan page
     * @param passed
     */
    public void updatePlan() {
        mHandler.sendEmptyMessage(MSG_UPDATE_PLAN);
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
		updatePlan();
	}
	
    /**
     * Move an entry in the plan
     */
    @JavascriptInterface
    public void move(int from, int to) {
    	mService.getPlan().move(from, to);
    	newPlan();
    	updatePlan();
    }

    /**
     * 
     */
    @JavascriptInterface
    public void discardPlan() {
    	mService.newPlan();
    	mService.getPlan().setName("");
    	newPlan();
    	updatePlan();
    }


    /**
     * Activate/Deactivate the plan	
     */
    @JavascriptInterface
    public void activateToggle() {
    	Plan plan = mService.getPlan();
    	if(plan.isActive()) {
    		plan.makeInactive();
    		mService.setDestination(null);
    	}
    	else {
    		plan.makeActive(mService.getGpsParams());    		
    		if(plan.getDestination(plan.findNextNotPassed()) != null) {
    			mService.setDestinationPlanNoChange(plan.getDestination(plan.findNextNotPassed()));
    		}
    	}
 
    	// Must use handler from functions called from JS
		updatePlan();
    }

    /**
     * 
     * @param num
     */
    @JavascriptInterface
    public void deleteWaypoint(int num) {
    	mService.getPlan().remove(num);
    	newPlan();
		updatePlan();
    }

    /**
     * 
     */
    @JavascriptInterface
    public void moveBack() {
    	mService.getPlan().regress();
		updatePlan();
    }

    /**
     * 
     */
    @JavascriptInterface
    public void moveForward() {
    	mService.getPlan().advance();
		updatePlan();
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
    	Destination d = new Destination(id, type, mPref, mService);
    	d.addObserver(this);
    	d.find(subtype);
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
    	plan.setName(name);
    	String format = plan.putPlanToStorageFormat();
    	mSavedPlans.put(name, format);
    	mPref.putPlans(Plan.putAllPlans(mSavedPlans));
    	newSavePlan();
    }

    /**
     * 
     * @param index
     */
    @JavascriptInterface
    public void loadPlan(String name) {
    	mService.newPlanFromStorage(mSavedPlans.get(name), false);
    	mService.getPlan().setName(name);
    	newPlan();
    	updatePlan();
    }

    /**
     * 
     * @param index
     */
    @JavascriptInterface
    public void loadPlanReverse(String name) {
    	mService.newPlanFromStorage(mSavedPlans.get(name), true);
    	mService.getPlan().setName(name);
    	newPlan();
    	updatePlan();
    }

    /**
     * 
     * @param num
     */
    @JavascriptInterface
    public void saveDelete(String name) {
    	mSavedPlans.remove(name);
    	mPref.putPlans(Plan.putAllPlans(mSavedPlans));
    	newSavePlan();
    }

    /** 
     * Get weather data async
     */
    @JavascriptInterface
    public void search(String value) {
        
    	/*
         * If text is 0 length or too long, then do not search, show last list
         */
        if(0 == value.length()) {
            return;
        }
        
        /*
         * This is a geo coordinate with &?
         */
        if(value.contains("&")) {
        	Message m = mHandler.obtainMessage(MSG_ADD_SEARCH, (Object)("'" + value + 
        			"','" + Destination.GPS + "','" 
        			+ Destination.GPS + "','" + Destination.GPS + "'"));
        	mHandler.sendMessage(m);
            return;
        }
        // This is address search?
        else if(value.startsWith("address,")) {
            String addr = value.substring(8); // 8 = length of "address,"
            if(addr.length() > 1) {
            	Message m = mHandler.obtainMessage(MSG_ADD_SEARCH, (Object)("'" + addr + 
            			"','" + Destination.MAPS + "','" 
            			+ Destination.MAPS + "','" + Destination.MAPS + "'"));
            	mHandler.sendMessage(m);            	
            }
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
        
        mSearchTask = new SearchTask();
        mSearchTask.execute(value.toString());
    }

    /**
     * @author zkhan
     *
     */
    private class SearchTask extends AsyncTask<Object, Void, Boolean> {

        private String[] selection;

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

            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            synchronized (SearchActivity.class) {
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

            if(null == selection) {
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
        	if(MSG_UPDATE_PLAN == msg.what) {
                /*
                 * Now update HTML with latest plan stuff, do this every time we start the Plan screen as 
                 * things might have changed.
                 */
            	int passed = plan.findNextNotPassed();
            	for(int num = 0; num < plan.getDestinationNumber(); num++) {
            		String url = "javascript:set_plan_line(" + 
            				num + "," +
            				(passed == num ? 1 : 0) + ",'" +
            				Math.round(plan.getDestination(num).getBearing()) + "','" + 
            				Math.round(plan.getDestination(num).getDistance()) + "','" +
            				plan.getDestination(num).getEte() +
            				"')";
            		mWebView.loadUrl(url);
            	}
            	
            	// Set destination next
        		if(plan.getDestination(plan.findNextNotPassed()) != null) {
        			mService.setDestinationPlanNoChange(plan.getDestination(plan.findNextNotPassed()));
        		}
        		// Then change state of activate button
            	mWebView.loadUrl("javascript:set_active(" + plan.isActive() + ")");
            	mWebView.loadUrl("javascript:plan_set_total('" + mContext.getString(R.string.Total) + " " + plan.toString() + "')");
            	if(null != plan.getName()) {
            		mWebView.loadUrl("javascript:plan_setname('" + plan.getName() + "')");
            	}
        	}
        	else if(MSG_CLEAR_PLAN == msg.what) {
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
        }
    };

}