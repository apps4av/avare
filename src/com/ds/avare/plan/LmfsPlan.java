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

import org.json.JSONObject;

/**
 * 
 * @author zkhan
 *
 */
public class LmfsPlan {

	
	private static final String DOMESTIC = "DOMESTIC";
	private static final String PROPOSED = "PROPOSED";
	private static final String DIRECT = "DCT";

	private boolean mValid;
	private String mId;
	
	public String flightRules;
	public String aircraftIdentifier;
	public String departure;
	public String destination;
	public String departureInstant; 
	public String flightDuration;
	public String altDestination1; 
	public String altDestination2; 
	public String aircraftType;
	public String numberOfAircraft;
	public String heavyWakeTurbulence;
	public String aircraftEquipment; 
	public String speedKnots; 
	public String altitudeFL;
	public String fuelOnBoard; 
	public String pilotData;
	public String peopleOnBoard; 
	public String aircraftColor;
	public String route;
	public String type;
	public String currentState;

	/**
	 * 
	 */
	private void init() {
		mValid = false;
		mId = null;
		
		flightRules = "";
		aircraftIdentifier = "";
		departure = "";
		destination = "";
		departureInstant = ""; 
		flightDuration = "";
		altDestination1 = ""; 
		altDestination2 = ""; 
		aircraftType = "";
		numberOfAircraft = "";
		heavyWakeTurbulence = "";
		aircraftEquipment = "";
		speedKnots = ""; 
		altitudeFL = "";
		fuelOnBoard = ""; 
		pilotData = "";
		peopleOnBoard = ""; 
		aircraftColor = "";
		currentState = PROPOSED;
		route = DIRECT;

		type = DOMESTIC;		
	}
	
	/**
	 * 
	 */
	public LmfsPlan() {
		init();
	}

	/**
	 * Set invalid when not parsed properly
	 * @return
	 */
	public boolean isValid() {
		return mValid;
	}

	/**
	 * Returns ID of plan 
	 * @return
	 */
	public String getId() {
		return mId;
	}

	/**
	 * Set ID. Used when we get plan summary
	 * @param id
	 */
	public void setId(String id) {
		mId = id;
	}

	/**
	 * LMFS plan from JSON
	 * @param data will be like:
	 * {"versionStamp":"20150112162243160","actualDepartureInstant":null,"beaconCode":null,"currentState":"PROPOSED","sarTracking":true,"nasFlightPlan":{"destination":{"latLong":"2548N08017W","locationIdentifier":"MIA"},"departure":{"latLong":"2939N09517W","locationIdentifier":"HOU"},"route":"DCT","aircraftColor":"BLUE","aircraftEquipment":"G","aircraftIdentifier":"N172EF","aircraftType":"P28A","flightRules":"VFR","fuelOnBoard":"PT20H0M","numberOfAircraft":3,"altDestination1":{"latLong":"3219N09005W","locationIdentifier":"JAN"},"altDestination2":null,"altitude":{"altitudeBlock":null,"altitudeVFR":null,"altitudeVFRFL":null,"altitudeABVFL":null,"altitudeFL":35,"altitudeOTP":null,"altitudeOTPFL":null},"departureInstant":1421274180000,"flightDuration":"PT1H30M","remarks":null,"peopleOnBoard":1,"speed":{"speedKnots":100,"speedMach":null,"speedClassified":null},"heavyWakeTurbulence":true,"pilotData":"PII restricted"},"returnCodedMessage":[],"returnMessage":[],"artccInfo":null,"icaoFlightPlan":null,"alertSubscription":true,"notificationsSubscription":true,"returnStatus":true,"artccState":null}
	 */
	public LmfsPlan(String data) {
		init();
		
		// Parse JSON
		try {
			/*
			 * Get all plans from summaries (do not get plan details till user needs)".
			 */
			JSONObject json = new JSONObject(data);
			// Only support NAS
			JSONObject nas = json.getJSONObject("nasFlightPlan");
			if(nas == null) {
				return;
			}
			flightRules = json.getString("flightRules");
			aircraftIdentifier = json.getString("aircraftIdentifier");
			departure = nas.getJSONObject("departure").getString("locationIdentifier");
			destination = nas.getJSONObject("destination").getString("locationIdentifier");
			departureInstant = json.getString("departureInstant"); 
			flightDuration = json.getString("flightDuration");
			altDestination1 = json.getString("altDestination1"); 
			altDestination2 = json.getString("altDestination12"); 
			aircraftType = json.getString("aircraftType");
			numberOfAircraft = json.getString("numberOfAircraft");
			heavyWakeTurbulence = json.getString("heavyWakeTurbulence");
			aircraftEquipment = json.getString("aircraftEquipment");
			speedKnots = json.getString("speedKnots");
			altitudeFL = json.getJSONObject("altitude").getString("altitudeFL");
			fuelOnBoard = json.getString("fuelOnBoard");
			pilotData = json.getString("pilotData");
			peopleOnBoard = json.getString("peopleOnBoard"); 
			aircraftColor = json.getString("aircraftColor");
			route = json.getString("route");
	    	currentState = json.getString("currentState");
		}
		catch(Exception e) {
			
		}
		
	}

	// Hashmap safety from null
	private void put(Map<String, String> params, String name, String val) {
		if(null != name && null != val) {
			params.put(name, val);
		}
	}
	
	/**
	 * Put the plan in hashmap
	 * @return
	 */
	public Map<String, String> makeHashMap() {
		Map<String, String> params = new HashMap<String, String>();
		put(params, "type", type);
		put(params, "flightRules" , flightRules); 
		put(params, "aircraftIdentifier" , aircraftIdentifier); 
		put(params, "departure" , departure);
		put(params, "destination" , destination); 
		put(params, "departureInstant" , departureInstant); 
		put(params, "flightDuration" , flightDuration);
		put(params, "altDestination1" , altDestination1); 
		put(params, "altDestination2" , altDestination2); 
		put(params, "aircraftType" , aircraftType); 
		put(params, "numberOfAircraft" , numberOfAircraft);
		put(params, "heavyWakeTurbulence" , heavyWakeTurbulence);
		put(params, "aircraftEquipment" , aircraftEquipment); 
		put(params, "speedKnots" , speedKnots);
		put(params, "altitudeFL" , altitudeFL);
		put(params, "fuelOnBoard" , fuelOnBoard);
		put(params, "pilotData" , pilotData);
		put(params, "peopleOnBoard" , peopleOnBoard); 
		put(params, "aircraftColor" , aircraftColor);
		put(params, "route" , route);
		return params;
	}
	
}
