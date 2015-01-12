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

/**
 * 
 * @author zkhan
 *
 */
public class LmfsPlan {

	
	private static final String DOMESTIC = "DOMESTIC";

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
	public String type;

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
	 * LMFS plan from JSON
	 * @param json
	 */
	public LmfsPlan(String json) {
		init();
	}
	
	/**
	 * Put the plan in hashmap
	 * @return
	 */
	public Map<String, String> makeHashMap() {
		Map<String, String> params = new HashMap<String, String>();
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
		
		return params;
	}
	
}
