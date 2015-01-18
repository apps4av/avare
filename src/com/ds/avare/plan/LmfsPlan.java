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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONObject;

import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;

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
	public String remarks;
	public String currentState;
	public String versionStamp;

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
		remarks = "";
		versionStamp = "";
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
			flightRules = nas.getString("flightRules");
			aircraftIdentifier = nas.getString("aircraftIdentifier");
			departure = nas.getJSONObject("departure").getString("locationIdentifier");
			destination = nas.getJSONObject("destination").getString("locationIdentifier");
			departureInstant = nas.getString("departureInstant"); 
			flightDuration = nas.getString("flightDuration");
			altDestination1 = nas.getString("altDestination1");
			altDestination1 = altDestination1.equals("null") ? null : altDestination1; 
			altDestination2 = nas.getString("altDestination2"); 
			altDestination2 = altDestination2.equals("null") ? null : altDestination2; 
			aircraftType = nas.getString("aircraftType");
			numberOfAircraft = nas.getString("numberOfAircraft");
			heavyWakeTurbulence = nas.getString("heavyWakeTurbulence");
			heavyWakeTurbulence = heavyWakeTurbulence.equals("null") ? null : heavyWakeTurbulence; 
			aircraftEquipment = nas.getString("aircraftEquipment");
			speedKnots = nas.getJSONObject("speed").getString("speedKnots");
			altitudeFL = nas.getJSONObject("altitude").getString("altitudeFL");
			fuelOnBoard = nas.getString("fuelOnBoard");
			pilotData = nas.getString("pilotData");
			peopleOnBoard = nas.getString("peopleOnBoard"); 
			aircraftColor = nas.getString("aircraftColor");
			route = nas.getString("route");
			route = route.equals("null") ? null : route;
			remarks = nas.getString("remarks");
			remarks = remarks.equals("null") ? null : remarks;
	    	currentState = json.getString("currentState");
			mValid = true;
		}
		catch(Exception e) {
			mValid = false;			
		}
		
	}

	// Hashmap safety from null
	private void put(Map<String, String> params, String name, String val) {
		if(null != name && null != val) {
			if(val.length() != 0) {
				params.put(name, val);
			}
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
		put(params, "remarks" , remarks);
		return params;
	}
	
	/**
	 * Make a JSON of the plan. This is for storage only so people's preferred settings are filled in on the File form.
	 * @return
	 */
	public String makeJSON() {
		String ret = "";
		// Parse JSON
		try {
			/*
			 * Get all plans from summaries (do not get plan details till user needs)".
			 */
			JSONObject json = new JSONObject();
			// Only support NAS
			JSONObject nas = new JSONObject();
			JSONObject dep = new JSONObject();
			JSONObject des = new JSONObject();
			JSONObject alt = new JSONObject();
			JSONObject spd = new JSONObject();
			nas.put("flightRules", flightRules);
			nas.put("aircraftIdentifier", aircraftIdentifier);
			dep.put("locationIdentifier", departure);
			des.put("locationIdentifier", destination);
			nas.put("departure", dep);
			nas.put("destination", des);
			nas.put("departureInstant", departureInstant); 
			nas.put("flightDuration", flightDuration);
			nas.put("altDestination1", altDestination1); 
			nas.put("altDestination2", altDestination2); 
			nas.put("aircraftType", aircraftType); 
			nas.put("numberOfAircraft", numberOfAircraft); 
			nas.put("heavyWakeTurbulence", heavyWakeTurbulence); 
			nas.put("aircraftEquipment", aircraftEquipment); 
			spd.put("speedKnots", speedKnots);
			nas.put("speed", spd);
			alt.put("altitudeFL", altitudeFL);
			nas.put("altitude", alt);
			nas.put("fuelOnBoard", fuelOnBoard); 
			nas.put("pilotData", pilotData); 
			nas.put("peopleOnBoard", peopleOnBoard);
			nas.put("aircraftColor", aircraftColor);
			nas.put("route", route);
			nas.put("remarks", remarks);
			json.put("currentState", currentState);
			json.put("nasFlightPlan", nas);
			ret = json.toString();
			mValid = true;
		}
		catch(Exception e) {
			mValid = false;
		}
		return ret;
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getTime(String future) {
    	// fill time to now()
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        now.add(Calendar.MINUTE, Integer.parseInt(future));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(now.getTime());
	}

	/**
	 * 
	 * @return
	 */
	public static String getTimeFromInstance(String instance) {
    	// fill time to now()
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		try {
			long time = Long.parseLong(instance);
	        now.setTimeInMillis(time);
		}
		catch (Exception e) {
			return "";
		}
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(now.getTime());
	}

	/**
	 * Get time from user input to time acceptable by LMFS
	 * @return
	 */
	public static String getTimeFromInput(String time) {
		String data = time.replace(" ", "T") + ":00";
		return data;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	public static String getDurationFromInput(String input) {
		String ret = "PT" + input;
		return ret;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public static String durationToTime(String input) {
		String ret[] = input.split("PT");
		if(ret.length < 2) {
			return input;
		}
		return ret[1];
	}

	/**
	 * 
	 * @param time
	 * @return
	 */
	public static String timeToDuration(double time) {
		// time is in hours, convert to LMFS format of PTXXHYYM
		int hours = (int)time;
		int min = (int)((time - hours) * 60.0);
		return "PT" + hours + "H" + min + "M";
	}


	/**
	 * Fill this LMFS form based on plan
	 * @param p
	 */
	public void setFromPlan(Plan p) {
		int num = p.getDestinationNumber();
		if(num >= 2) {
			if(p.getDestination(num - 1).getType().equals(Destination.BASE)) {
				destination = p.getDestination(num - 1).getID();
			}
			if(p.getDestination(0).getType().equals(Destination.BASE)) {
				departure = p.getDestination(0).getID();
			}
		}
    	// find time remaining time based on true AS
		double time = 0;
		try {
			time = p.getDistance() / Double.parseDouble(speedKnots);
		}
		catch (Exception e) {
		}
		flightDuration = LmfsPlan.timeToDuration(time);
		fuelOnBoard = LmfsPlan.timeToDuration(time + 0.75); // 45 min reserve
		
    	// fill time to now()
        departureInstant = getTime("0");

        if(num > 2) {
        	route = "";
	        // Fill route
	        for(int dest = 1; dest < (num - 1); dest++) {
	        	String type = p.getDestination(dest).getType();
	        	// Only add fixes and navaids
	        	if(type.equals(Destination.FIX) || type.equals(Destination.NAVAID)) {
	        		route += p.getDestination(dest).getID();
	        	}
	        }
        }
        if(route.equals("")) {
        	route = DIRECT;
        }
	}
}
