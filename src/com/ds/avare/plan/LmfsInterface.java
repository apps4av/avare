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
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.ds.avare.message.NetworkHelper;
import com.ds.avare.utils.PossibleEmail;

import android.content.Context;

/**
 * LMFS 1-800-WX-BRIEF.COM interface 
 * @author zkhan
 *
 */
public class LmfsInterface {

	private static final String FILE_DOMESTIC = "DOMESTIC";
	
	private static final String AVARE_LMFS_URL = "https://apps4av.net/new/lmfs.php";
	
	private Context mContext;
	
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
	}

	/**
	 * Parse error from the server 
	 */
	private String parseError(String ret) {
		try {
			JSONObject json = new JSONObject(ret);
			boolean status = json.getBoolean("returnStatus");
			if(!status) {
				return json.getString("returnMessage");
			}
		} catch (JSONException e) {
		}
		return null;
	}
	
	/**
	 * Get all flight plans
	 */
	public LinkedList<String> getFlightPlans() {
		LinkedList<String> plans = new LinkedList<String>();
		
		String webUserName = PossibleEmail.get(mContext);
		String avareMethod = "FP/" + webUserName + "/retrieveFlightPlanSummaries";
		String httpMethod = "GET";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}

		return plans;
	}
			

	/**
	 * Get a flight plans
	 */
	public String getFlightPlan(String id) {
		
		String webUserName = PossibleEmail.get(mContext);
		String avareMethod = "FP/" + id + "/retrieve";
		String httpMethod = "GET";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}

		return ret;
	}
			

	/**
	 * Close a flight plan
	 */
	public String closeFlightPlan(String id) {
		
		String webUserName = PossibleEmail.get(mContext);
		String avareMethod = "FP/" + id + "/close";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}

		return ret;
	}

	
	/**
	 * Activate a flight plan
	 */
	public String activateFlightPlan(String id) {
		
		String webUserName = PossibleEmail.get(mContext);
		String avareMethod = "FP/" + id + "/activate";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}

		return ret;
	}

	/**
	 * Cancel a flight plans
	 */
	public String cancelFlightPlan(String id) {
		
		String webUserName = PossibleEmail.get(mContext);
		String avareMethod = "FP/" + id + "/retrieve";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);

		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}

		return ret;
	}
	
	/**
	 * File a flight plan
	 */
	public String fileFlightPlan(
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
			String aircraftColor) {
		
		String webUserName = PossibleEmail.get(mContext);
		String type = FILE_DOMESTIC;
		String avareMethod = "FP/file";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		params.put("type", type);
		params.put("flightRules" , flightRules); 
		params.put("aircraftIdentifier" , aircraftIdentifier); 
		params.put("departure" , departure);
		params.put("destination" , destination); 
		params.put("departureInstant" , departureInstant); 
		params.put("flightDuration" , flightDuration);
		params.put("altDestination1" , altDestination1); 
		params.put("altDestination2" , altDestination2); 
		params.put("aircraftType" , aircraftType); 
		params.put("numberOfAircraft" , numberOfAircraft);
		params.put("heavyWakeTurbulence" , heavyWakeTurbulence);
		params.put("aircraftEquipment" , aircraftEquipment); 
		params.put("speedKnots" , speedKnots);
		params.put("altitudeFL" , altitudeFL);
		params.put("fuelOnBoard" , fuelOnBoard);
		params.put("pilotData" , pilotData);
		params.put("peopleOnBoard" , peopleOnBoard); 
		params.put("aircraftColor" , aircraftColor);
		
		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}
		
		return ret;
	}

	
	/**
	 * Amend a flight plan
	 */
	public String amendFlightPlan(
			String id,
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
			String aircraftColor) {
		
		String webUserName = PossibleEmail.get(mContext);
		String type = FILE_DOMESTIC;
		String avareMethod = "FP/" + id + "/amend";
		String httpMethod = "POST";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("webUserName", webUserName);
		params.put("avareMethod", avareMethod);
		params.put("httpMethod", httpMethod);
		params.put("type", type);
		params.put("flightRules" , flightRules); 
		params.put("aircraftIdentifier" , aircraftIdentifier); 
		params.put("departure" , departure);
		params.put("destination" , destination); 
		params.put("departureInstant" , departureInstant); 
		params.put("flightDuration" , flightDuration);
		params.put("altDestination1" , altDestination1); 
		params.put("altDestination2" , altDestination2); 
		params.put("aircraftType" , aircraftType); 
		params.put("numberOfAircraft" , numberOfAircraft);
		params.put("heavyWakeTurbulence" , heavyWakeTurbulence);
		params.put("aircraftEquipment" , aircraftEquipment); 
		params.put("speedKnots" , speedKnots);
		params.put("altitudeFL" , altitudeFL);
		params.put("fuelOnBoard" , fuelOnBoard);
		params.put("pilotData" , pilotData);
		params.put("peopleOnBoard" , peopleOnBoard); 
		params.put("aircraftColor" , aircraftColor);
		
		String ret = null;
		try {
			ret = NetworkHelper.post(AVARE_LMFS_URL, params);
			ret = parseError(ret);
		} catch (Exception e) {
		}
		
		return ret;
	}
}