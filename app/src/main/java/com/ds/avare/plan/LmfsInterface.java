/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.plan;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.ds.avare.R;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.storage.Preferences;

import android.content.Context;

/**
 * LMFS 1-800-WX-BRIEF.COM interface 
 * @author zkhan
 *
 */
public class LmfsInterface {

	private static final String AVARE_LMFS_URL = "https://apps4av.net/new/lmfs.php";
	
	private Context mContext;
    private Preferences mPref;
	private String mError; // null means no error (success), value means error message
	
	/**
	 * Call apps4av servers to use REST calls to 1-800-WX-Brief. 
	 * The API interface is basically all POST, and requires three parameters
	 * - avareMethod This forms the URL for the REST call
	 * - httpMethod This decides the POST/GET HTTP method of REST calls
	 * - webUserName This is the username registered on LMFS and is same as the username registered when Avare was installed 
	 * 
	 * All replies are JSON
	 */
	public LmfsInterface(Context ctx) {
		mContext = ctx;
        mPref = new Preferences(ctx);
	}

	/**
	 * Parse error from the server 
	 */
	private String parseError(String ret) {
		try {
			// Something like {"returnCodedMessage":[],"returnMessage":[],"flightPlanSummary":[],"returnStatus":true}
			JSONObject json = new JSONObject(ret);
			boolean status = json.getBoolean("returnStatus");
			if(!status) {
				String val = json.getString("returnMessage");
				val = val.replace("\\", ""); // do not escape
				return val;
			}
		} catch (JSONException e) {
			return(mContext.getString(R.string.Failed));
		}
		return null;
	}

	/**
	 * Get all flight plans
	 */
	public LmfsPlanList getFlightPlans() {

		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + webUserName + "/retrieveFlightPlanSummaries";
		String httpMethod = "GET";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}

		return new LmfsPlanList(ret);
	}
			

	/**
	 * Get a flight plans
	 */
	public LmfsPlan getFlightPlan(String id) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + id + "/retrieve";
		String httpMethod = "GET";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}

		return new LmfsPlan(ret);
	}
			

	/**
	 * Close a flight plan
	 */
	public String closeFlightPlan(String id, String loc) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + id + "/close";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		if(!loc.equals("")) {
			// Only when overdue
			params.put("closeDestinationInfo", loc);
		}

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}

		return ret;
	}

	
	/**
	 * Activate a flight plan
	 */
	public String activateFlightPlan(String id, String version, String future) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + id + "/activate";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		// Always activate NOW. Depart within 30 mins
		params.put("actualDepartureInstant", LmfsPlan.getTimeFromInput(LmfsPlan.getTime(future)));
		params.put("versionStamp", version);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}

		return ret;
	}

	/**
	 * Cancel a flight plans
	 */
	public String cancelFlightPlan(String id) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + id + "/cancel";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}

		return ret;
	}
	
	/**
	 * File a flight plan
	 */
	public String fileFlightPlan(LmfsPlan plan) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/file";
		String httpMethod = "POST";
		
		Map<String, String> params = plan.makeHashMap();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		
		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}
		
		return ret;
	}

	
	/**
	 * Amend a flight plan
	 */
	public String amendFlightPlan(LmfsPlan plan) {
		
		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/" + plan.getId() + "/amend";
		String httpMethod = "POST";
		
		Map<String, String> params = plan.makeHashMap();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		params.put("versionStamp", plan.versionStamp);
		
		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}
		
		return ret;
	}

	/**
	 * Gte briefing
	 * @param id
	 */
	public void getBriefing(LmfsPlan pl, boolean translated, String routeWidth) {

		String webUserName = mPref.getRegisteredEmail();
		String avareMethod = "FP/emailBriefing";
		String httpMethod = "POST";
		
		Map<String, String> params = pl.makeHashMap();

		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		params.put("briefingType", "EMAIL");
		params.put("briefingEmailAddresses", mPref.getRegisteredEmail());
		params.put("recipientEmailAddresses", mPref.getRegisteredEmail());
		params.put("routeCorridorWidth", routeWidth);
		if(translated) {
			params.put("briefingPreferences", "{\"plainText\":true}");			
		}
		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			mError = parseError(ret);
		} catch (Exception e) {
			mError = "Network error";
		}
	}


	
	/**
	 * Get error for last xaction
	 * @return
	 */
	public String getError() {
		return mError;
	}

}