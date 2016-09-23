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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;


/**
 * Get a list of plans
 * @author zkhan
 *
 */
public class LmfsPlanList {

	private LinkedList<LmfsPlan> mPlans;
	public int mSelectedIndex;
	
	/*
	 * Parse something like:
	 * 
	 * {
	 *  "returnStatus":true,"returnCodedMessage":[],"returnMessage":[],"flightPlanSummary":
	 *  [
	 * 	 {"route":"DCT","versionStamp":"20150112162243160","aircraftIdentifier":"N172EF","alertCount":0,"departureInstant":1421274180000,"flightDuration":"PT1H30M","actualDepartureInstant":null,"currentState":"PROPOSED","flightId":"66052270_527360_258","icaoSummaryFields":null,"nasSummaryFields":{"destination":{"latLong":"2548N08017W","locationIdentifier":"MIA"},"departure":{"latLong":"2939N09517W","locationIdentifier":"HOU"},"flightRules":"VFR"}},
	 *   {"route":"DCT","versionStamp":"20150112163040050","aircraftIdentifier":"N172EF","alertCount":0,"departureInstant":1421360580000,"flightDuration":"PT1H30M","actualDepartureInstant":null,"currentState":"PROPOSED","flightId":"66052270_527360_259","icaoSummaryFields":null,"nasSummaryFields":{"destination":{"latLong":"2548N08017W","locationIdentifier":"MIA"},"departure":{"latLong":"2939N09517W","locationIdentifier":"HOU"},"flightRules":"VFR"}}
	 *  ]
	 * }
	 */
	public LmfsPlanList(String data) {
		mSelectedIndex = 0;
		try {
			/*
			 * Get all plans from summaries (do not get plan details till user needs)".
			 */
			JSONObject json = new JSONObject(data);
			JSONArray array = json.getJSONArray("flightPlanSummary");
			mPlans = new LinkedList<LmfsPlan>();
		    for(int plan = 0 ; plan < array.length(); plan++) {
		    	LmfsPlan pl = new LmfsPlan();
		    	JSONObject obj = array.getJSONObject(plan);
		    	// Fill in all data needed to show user and identifiable plan
		    	pl.setId(obj.getString("flightId"));
		    	pl.currentState = obj.getString("currentState");
		    	pl.versionStamp = obj.getString("versionStamp");
		    	pl.aircraftId = obj.getString("aircraftIdentifier");
		    	pl.destination = obj.getJSONObject("icaoSummaryFields").getJSONObject("destination").getString("locationIdentifier");
		    	pl.departure = obj.getJSONObject("icaoSummaryFields").getJSONObject("departure").getString("locationIdentifier");
		    	mPlans.add(pl);
		    }
		}
		catch(Exception e) {
			
		}
	}

	/**
	 * List of all plans (may not be filled in except the ID)
	 * @return
	 */
	public LinkedList<LmfsPlan> getPlans() {
		return mPlans;
	}
}
