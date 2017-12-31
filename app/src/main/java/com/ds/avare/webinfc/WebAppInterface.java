/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.webinfc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.WeatherActivity;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.plan.LmfsInterface;
import com.ds.avare.plan.LmfsPlan;
import com.ds.avare.plan.LmfsPlanList;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import java.util.Locale;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppInterface {
    private Context mContext;
    private StorageService mService; 
    private WebView mWebView;
    private Preferences mPref;
	private LmfsPlanList mFaaPlans;
    private GenericCallback mCallback;

    private static final int MSG_NOTBUSY = 9;
    private static final int MSG_BUSY = 10;
    private static final int MSG_FILL_FORM = 13;
    private static final int MSG_ERROR = 15;
    private static final int MSG_FAA_PLANS = 16;
    private static final int MSG_SET_EMAIL = 17;

    /** 
     * Instantiate the interface and set the context
     */
    public WebAppInterface(Context c, WebView v, GenericCallback cb) {
        mWebView = v;
        mContext = c;
        mPref = new Preferences(c);
        mCallback = cb;
    }
    
    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
    }

    private String checkNull(String input) {
    	if(input == null) {
    		return "";
    	}
    	return input;
    }
    
    /**
     * Fill plan form with data stored
     */
    @JavascriptInterface
    public void fillPlan() {
    	if(mService == null) {
    		return;
    	}
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// Fill in from storage, this is going to be mostly reflecting the user's most 
    	// used settings in the form
    	LmfsPlan pl = new LmfsPlan(mPref);
    	Plan p = mService.getPlan();
    	String destination = "";
    	String departure = "";
    	String flightDuration = "";
    	String fuelOnBoard = "";
    	String departureInstant = "";
    	String route = "";
    	
    	// If plan has valid BASE origin and destinations, fill them in
    	// Fill this LMFS form based on plan
		int num = p.getDestinationNumber();
		if(num >= 2) {
			if(p.getDestination(num - 1).getType().equals(Destination.BASE)) {
				destination = "K" + p.getDestination(num - 1).getID(); // USA only
			}
			if(p.getDestination(0).getType().equals(Destination.BASE)) {
				departure = "K" + p.getDestination(0).getID(); // USA only
			}
		}
			
    	// find time remaining time based on true AS
		double time = 0;
		try {
			time = p.getDistance() / Double.parseDouble(pl.cruisingSpeed);
		}
		catch (Exception e) {
		}
		flightDuration = LmfsPlan.timeToDuration(time);
		fuelOnBoard = LmfsPlan.timeToDuration(time + 0.75); // 45 min reserve
			
    	// fill time to now()
        departureInstant = System.currentTimeMillis() + "";

        if(num > 2) {
        	route = "";
	        // Fill route
	        for(int dest = 1; dest < (num - 1); dest++) {
	        	String type = p.getDestination(dest).getType();
	        	if(type.equals(Destination.GPS) || type.equals(Destination.MAPS) || type.equals(Destination.UDW)) {
	        		route += LmfsPlan.convertLocationToGpsCoords(p.getDestination(dest).getLocation()) + " ";
	        	}
				else if(type.equals(Destination.BASE)) {
					route += "K" + p.getDestination(dest).getID() + " "; // USA
				}
	        	else {
	        		route += p.getDestination(dest).getID() + " ";
	        	}
	        }
        }
        if(route.equals("")) {
        	route = LmfsPlan.DIRECT;
        }
    	
    	// Fill form
    	Message m = mHandler.obtainMessage(MSG_FILL_FORM, (Object)(
				"'" +  checkNull(pl.aircraftId) + "'," +
    	    	"'" +  checkNull(pl.flightRule)  + "'," +
				"'" +  checkNull(pl.flightType)  + "'," +
				"'" +  checkNull(pl.noOfAircraft)  + "'," +
				"'" +  checkNull(pl.aircraftType) + "'," +
				"'" +  checkNull(pl.wakeTurbulence) + "'," +
				"'" +  checkNull(pl.aircraftEquipment) + "'," +
    			"'" +  checkNull(departure) + "'," +
				"'" +  checkNull(LmfsPlan.getTimeFromInstance(departureInstant)) + "'," +
				"'" +  checkNull(pl.cruisingSpeed) + "'," +
				"'" +  checkNull(String.format(Locale.US, "%03d", p.getAltitude() / 100)) + "'," +
				"'" +  checkNull(pl.surveillanceEquipment) + "'," +
				"'" +  checkNull(route) + "'," +
				"'" +  checkNull(pl.otherInfo) + "'," +
				"'" +  checkNull(destination) + "'," +
				"'" +  checkNull(LmfsPlan.durationToTime(flightDuration)) + "'," +
				"'" +  checkNull(pl.alternate1) + "'," +
				"'" +  checkNull(pl.alternate2) + "'," +
				"'" +  checkNull(LmfsPlan.durationToTime(fuelOnBoard)) + "'," +
				"'" +  checkNull(pl.peopleOnBoard) + "'," +
				"'" +  checkNull(pl.aircraftColor) + "'," +
				"'" +  checkNull(pl.supplementalRemarks) + "'," +
    			"'" +  checkNull(pl.pilotInCommand) + "'," +
				"'" +  checkNull(pl.pilotInfo) + "'"
    			));
    	mHandler.sendMessage(m);
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /** 
     * Select the plan on the FAA list
     */
    @JavascriptInterface
    public void moveTo(int index) {
    	if(null == mFaaPlans) {
    		return;
    	}
    	
    	// refresh
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mFaaPlans.mSelectedIndex = index;
    	mHandler.sendEmptyMessage(MSG_FAA_PLANS);

    	mHandler.sendEmptyMessage(MSG_NOTBUSY);

    }

    
    /**
     * Get briefing.
     */
    @JavascriptInterface
    public void getWeather() {
    	// refresh
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	LmfsInterface infc = new LmfsInterface(mContext);
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || mFaaPlans.mSelectedIndex >= mFaaPlans.getPlans().size()) {
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    		return;
    	}
    	
		// Get plan, then request its briefing
		LmfsPlan pl = infc.getFlightPlan(mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).getId());
    	String err = infc.getError();
    	if(null != err) {
    		// failed to get plan
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
        	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
        	mHandler.sendMessage(m);
        	return;
    	}

    	infc.getBriefing(pl, mPref.isWeatherTranslated(), LmfsPlan.ROUTE_WIDTH);
    	err = infc.getError();
    	if(null == err) {
    		// success
    		err = mContext.getString(R.string.Success);
    	}
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);

    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }
    
    /** 
     * File an FAA plan and save it
     */
    @JavascriptInterface
    public void filePlan(
		String aircraftId,
		String flightRule,
		String flightType,
		String noOfAircraft,
		String aircraftType,
		String wakeTurbulence,
		String aircraftEquipment,
		String departure,
		String departureDate,
		String cruisingSpeed,
		String level,
		String surveillanceEquipment,
		String route,
		String otherInfo,
		String destination,
		String totalElapsedTime,
		String alternate1,
		String alternate2,
		String fuelEndurance,
		String peopleOnBoard,
		String aircraftColor,
		String supplementalRemarks,
		String pilotInCommand,
		String pilotInfo) {

    	mHandler.sendEmptyMessage(MSG_BUSY);
    	LmfsPlan pl = new LmfsPlan();
		pl.aircraftId = aircraftId.toUpperCase(Locale.getDefault());
    	pl.flightRule = flightRule.toUpperCase(Locale.getDefault());
		pl.flightType = flightType.toUpperCase(Locale.getDefault());
		pl.noOfAircraft = noOfAircraft.toUpperCase(Locale.getDefault());
		pl.aircraftType = aircraftType.toUpperCase(Locale.getDefault());
		pl.wakeTurbulence = wakeTurbulence.toUpperCase(Locale.getDefault());
		pl.aircraftEquipment = aircraftEquipment.toUpperCase(Locale.getDefault());
    	pl.departure = departure.toUpperCase(Locale.getDefault());
		pl.departureDate = LmfsPlan.getInstanceFromTime(departureDate.toUpperCase(Locale.getDefault()));
		pl.cruisingSpeed = cruisingSpeed.toUpperCase(Locale.getDefault());
		pl.level = level.toUpperCase(Locale.getDefault());
		pl.surveillanceEquipment = surveillanceEquipment.toUpperCase(Locale.getDefault());
		pl.route = route.toUpperCase(Locale.getDefault());
		pl.otherInfo = otherInfo.toUpperCase(Locale.getDefault());
		pl.destination = destination.toUpperCase(Locale.getDefault());
    	pl.totalElapsedTime = LmfsPlan.getDurationFromInput(totalElapsedTime.toUpperCase(Locale.getDefault()));
    	pl.alternate1 = alternate1.toUpperCase(Locale.getDefault());
    	pl.alternate2 = alternate2.toUpperCase(Locale.getDefault());
    	pl.fuelEndurance = LmfsPlan.getDurationFromInput(fuelEndurance.toUpperCase(Locale.getDefault()));
		pl.peopleOnBoard = peopleOnBoard.toUpperCase(Locale.getDefault());
		pl.aircraftColor = aircraftColor.toUpperCase(Locale.getDefault());
		pl.supplementalRemarks = supplementalRemarks.toUpperCase(Locale.getDefault());
		pl.pilotInCommand = pilotInCommand.toUpperCase(Locale.getDefault());
		pl.pilotInfo = pilotInfo.toUpperCase(Locale.getDefault());

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
     * Amend an FAA plan and save it
     */
    @JavascriptInterface
    public void amendPlan(
			String aircraftId,
			String flightRule,
			String flightType,
			String noOfAircraft,
			String aircraftType,
			String wakeTurbulence,
			String aircraftEquipment,
			String departure,
			String departureDate,
			String cruisingSpeed,
			String level,
			String surveillanceEquipment,
			String route,
			String otherInfo,
			String destination,
			String totalElapsedTime,
			String alternate1,
			String alternate2,
			String fuelEndurance,
			String peopleOnBoard,
			String aircraftColor,
			String supplementalRemarks,
			String pilotInCommand,
			String pilotInfo) {
        
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || mFaaPlans.mSelectedIndex >= mFaaPlans.getPlans().size()) {
    		return;
    	}
    	
    	LmfsPlan pl = mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex);
    	if(pl.getId() == null) {
    		return;
    	}
    	
    	mHandler.sendEmptyMessage(MSG_BUSY);
		pl.aircraftId = aircraftId.toUpperCase(Locale.getDefault());
		pl.flightRule = flightRule.toUpperCase(Locale.getDefault());
		pl.flightType = flightType.toUpperCase(Locale.getDefault());
		pl.noOfAircraft = noOfAircraft.toUpperCase(Locale.getDefault());
		pl.aircraftType = aircraftType.toUpperCase(Locale.getDefault());
		pl.wakeTurbulence = wakeTurbulence.toUpperCase(Locale.getDefault());
		pl.aircraftEquipment = aircraftEquipment.toUpperCase(Locale.getDefault());
		pl.departure = departure.toUpperCase(Locale.getDefault());
		pl.departureDate = LmfsPlan.getInstanceFromTime(departureDate.toUpperCase(Locale.getDefault()));
		pl.cruisingSpeed = cruisingSpeed.toUpperCase(Locale.getDefault());
		pl.level = level.toUpperCase(Locale.getDefault());
		pl.surveillanceEquipment = surveillanceEquipment.toUpperCase(Locale.getDefault());
		pl.route = route.toUpperCase(Locale.getDefault());
		pl.otherInfo = otherInfo.toUpperCase(Locale.getDefault());
		pl.destination = destination.toUpperCase(Locale.getDefault());
		pl.totalElapsedTime = LmfsPlan.getDurationFromInput(totalElapsedTime.toUpperCase(Locale.getDefault()));
		pl.alternate1 = alternate1.toUpperCase(Locale.getDefault());
		pl.alternate2 = alternate2.toUpperCase(Locale.getDefault());
		pl.fuelEndurance = LmfsPlan.getDurationFromInput(fuelEndurance.toUpperCase(Locale.getDefault()));
		pl.peopleOnBoard = peopleOnBoard.toUpperCase(Locale.getDefault());
		pl.aircraftColor = aircraftColor.toUpperCase(Locale.getDefault());
		pl.supplementalRemarks = supplementalRemarks.toUpperCase(Locale.getDefault());
		pl.pilotInCommand = pilotInCommand.toUpperCase(Locale.getDefault());
		pl.pilotInfo = pilotInfo.toUpperCase(Locale.getDefault());

    	// Now file and show error messages
    	LmfsInterface infc = new LmfsInterface(mContext);
    	infc.amendFlightPlan(pl);
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
    public void planChangeState(String action, String arg) {
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || mFaaPlans.mSelectedIndex >= mFaaPlans.getPlans().size()) {
    		return;
    	}
    	
    	/*
    	 * Do the action of the plan
    	 */
    	LmfsInterface infc = new LmfsInterface(mContext);

    	String err = null;
    	String id = mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).getId();
    	String ver = mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).versionStamp;
    	if(id == null) {
    		return;
    	}
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	if(action.equals("Activate")) {
    		// Activate plan with given ID
    		infc.activateFlightPlan(id, ver, arg);
    	}
    	else if(action.equals("Close")) {
    		// Activate plan with given ID
    		infc.closeFlightPlan(id, arg);
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
     * Get an FAA plan, and fill the form
     */
    @JavascriptInterface
    public void loadPlan() {      
    	
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || mFaaPlans.mSelectedIndex >= mFaaPlans.getPlans().size()) {
    		return;
    	}

    	mHandler.sendEmptyMessage(MSG_BUSY);

    	LmfsInterface infc = new LmfsInterface(mContext);

    	// Get plan in fill in the form
		LmfsPlan pl = infc.getFlightPlan(mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).getId());
    	String err = infc.getError();
    	if(null != err) {
    		// failed to get plan
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
        	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
        	mHandler.sendMessage(m);
        	return;
    	}

    	// Fill form
    	Message m = mHandler.obtainMessage(MSG_FILL_FORM, (Object)(
						"'" +  checkNull(pl.aircraftId) + "'," +
						"'" +  checkNull(pl.flightRule)  + "'," +
						"'" +  checkNull(pl.flightType)  + "'," +
						"'" +  checkNull(pl.noOfAircraft)  + "'," +
						"'" +  checkNull(pl.aircraftType) + "'," +
						"'" +  checkNull(pl.wakeTurbulence) + "'," +
						"'" +  checkNull(pl.aircraftEquipment) + "'," +
						"'" +  checkNull(pl.departure) + "'," +
						"'" +  checkNull(LmfsPlan.getTimeFromInstance(pl.departureDate)) + "'," +
						"'" +  checkNull(pl.cruisingSpeed) + "'," +
						"'" +  checkNull(pl.level) + "'," +
						"'" +  checkNull(pl.surveillanceEquipment) + "'," +
						"'" +  checkNull(pl.route) + "'," +
						"'" +  Helper.formatJsArgs(checkNull(pl.otherInfo)) + "'," +
						"'" +  checkNull(pl.destination) + "'," +
						"'" +  checkNull(LmfsPlan.durationToTime(pl.totalElapsedTime)) + "'," +
						"'" +  checkNull(pl.alternate1) + "'," +
						"'" +  checkNull(pl.alternate2) + "'," +
						"'" +  checkNull(LmfsPlan.durationToTime(pl.fuelEndurance)) + "'," +
						"'" +  checkNull(pl.peopleOnBoard) + "'," +
						"'" +  checkNull(pl.aircraftColor) + "'," +
						"'" +  Helper.formatJsArgs(checkNull(pl.supplementalRemarks)) + "'," +
						"'" +  Helper.formatJsArgs(checkNull(pl.pilotInCommand)) + "'," +
						"'" +  Helper.formatJsArgs(checkNull(pl.pilotInfo)) + "'"
    			));
    	mHandler.sendMessage(m);
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    public void setEmail() {
        // Set email in page for user to know where to register with
        mHandler.sendEmptyMessage(MSG_SET_EMAIL);
    }

    /**
     * 
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            
        	if(MSG_NOTBUSY == msg.what) {
        		mCallback.callback((Object)WeatherActivity.UNSHOW_BUSY, null);
        	}
        	else if(MSG_BUSY == msg.what) {
        		mCallback.callback((Object)WeatherActivity.SHOW_BUSY, null);
        	}
           	else if(MSG_FILL_FORM == msg.what) {	
            	String func = "javascript:plan_fill(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ERROR == msg.what) {	
        		mCallback.callback((Object)WeatherActivity.MESSAGE, msg.obj);
        	}
        	else if(MSG_FAA_PLANS == msg.what) {
        		/*
        		 * Fill the table of plans
        		 */
        		if(mFaaPlans.getPlans() == null) {
        			return;
        		}
        		String p = "";
        		int i = 0;
        		// Sent out plans as text separated by commas like selected,name,state
        		for (LmfsPlan pl : mFaaPlans.getPlans()) {
        			p += ((i == mFaaPlans.mSelectedIndex) ? "1" : "0") + "," + pl.departure + "-" + pl.destination + "-" + pl.aircraftId + "," + pl.currentState + ",";
        			i++;
        		}
        		String func = "javascript:set_faa_plans('" + p + "')";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_SET_EMAIL == msg.what) {
	    		String func = "javascript:set_email('" + mPref.getRegisteredEmail() + "')";
	        	mWebView.loadUrl(func);
        	}
        }
    };
}