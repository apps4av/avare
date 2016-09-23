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

import java.util.Locale;


/**
 * Get Nav log of the plan
 * @author zkhan
 *
 */
public class LmfsPlanLog {

	String mLog;
	
	/*
	 * Parse something like:
	 * 
	 * {
	 * "returnCodedMessage":[],
	 * "returnMessage":[],
	 * "navLogResponseType":{
	 * 		"navLogFix":[{
	 * 			"cruisingAltitude":null,
	 * 			"isadeviation":-19.0,
	 * 			"legDistance":{
	 * 				"distanceUnit":"NAUTICAL_MILE","distanceValue":0.0
	 * 			},
	 * 			"magneticCourse":0.0,
	 * 			"magneticHeading":0.0,
	 * 			"outsideAirTemperature":-4.0,
	 * 			"parentAirWayName":null,
	 * 			"remainingDistance":{
	 * 				"distanceUnit":"NAUTICAL_MILE","distanceValue":37.0
	 * 			},
	 * 			"windDirection":276.0,
	 * 			"windSpeed":17.0,
	 * 			"fixName":"KBOS",
	 * 			"estimatedTimeEnroute":null,
	 * 			"fuelConsumed":{
	 * 				"fuelValue":2.0,
	 * 				"fuelUnit":"GALLONS_FUEL_UNITS_TYPE"
	 * 			},
	 * 			"countryCode":"K6",
	 * 			"estimatedInstantAtFix":1423007700000,
	 * 			"geodeticCoordinate":{
	 * 				"latitude":42.362,
	 * 				"longitude":-71.005
	 * 			},
	 * 			"altitudeAtFix":{
	 * 			"icaoFiledLevel":null,
	 * 			"nasFiledLevel":{
	 * 				"nasLevelValue":0,
	 * 				"nasLevelUnits":"NAS_FLIGHT_LEVEL",
	 * 				"nasLevelValue2":null
	 * 			}
	 * 		},
	 * 		"groundSpeedAtFix":{
	 * 			"tasunits":"TAS_KNOTS",
	 * 			"tasvalue":0.0
	 * 		},
	 * 		"typeOfWaypoint":"AIRPORT_FIXTYPE"
	 * 	},
	 *  ...
	 * "returnStatus":true
	 * }
	 */
	public LmfsPlanLog(String data) {
		
		double totalFuel = 0;
		long totalTime = 0;
		
		if(null == data) {
			mLog = "";
			return;
		}
    	mLog = 
    			"<table class=\"table\"><th>WP</th><th>Wind/Temp</th><th>Leg</th><th>Remain</th><th>MC</th><th>MH</th><th>Time</th><th>Fuel</th>";
		try {
			/*
			 * Get all item logs".
			 */
			JSONObject json = new JSONObject(data);
			JSONArray array = json.getJSONObject("navLogResponseType").getJSONArray("navLogFix");
		    for(int fix = 0 ; fix < array.length(); fix++) {
		    	/*
		    	 * Now make HTML
		    	 */
		    	JSONObject obj = array.getJSONObject(fix);
		    	mLog += "<tr>";
		    	
		    	mLog += "<td>";
		    	String wp = obj.getString("fixName");
		    	if(wp.equals("null")) {
		    		wp = "Fix";
		    	}
		    	mLog += wp;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	long windd = Math.round(obj.getDouble("windDirection"));
		    	long winds = Math.round(obj.getDouble("windSpeed"));
		    	long oat = Math.round(obj.getDouble("outsideAirTemperature"));
		    	mLog += windd + "@" + winds + "/" + oat;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	long legDist = Math.round(obj.getJSONObject("legDistance").getDouble("distanceValue"));
		    	mLog += legDist;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	long remDist = Math.round(obj.getJSONObject("remainingDistance").getDouble("distanceValue"));
		    	mLog += remDist;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	long mc = Math.round(obj.getDouble("magneticCourse"));
		    	mLog += mc;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	long mh = Math.round(obj.getDouble("magneticHeading"));
		    	mLog += mh;
		    	mLog += "</td>";
		    	
		    	mLog += "<td>";
		    	String ete = obj.getString("estimatedTimeEnroute");
		    	if(ete.equals("null")) {
		    		ete = "PT0H0M0S";
		    	}
		    	long time = getElapsedTime(ete);
		    	mLog += getTimeFromLong(time);
		    	mLog += "</td>";

		    	totalTime += time;

		    	mLog += "<td>";
		    	double fuel = obj.getJSONObject("fuelConsumed").getDouble("fuelValue");
		    	totalFuel += fuel;
		    	mLog += fuel;
		    	mLog += "</td>";
		    	
		    	mLog += "</tr>";
		    }
		    mLog += "</table>";
		    mLog += "Total: Fuel " + totalFuel;
		    mLog += " Time " + getTimeFromLong(totalTime) + "<br>";
		}
		catch(Exception e) {
			
		}
	}

	/**
	 * Get the log in HTML
	 * @return
	 */
	public String getLogInHTML() {
		return mLog;
	}
	
	/**
	 * Get human time from long time
	 * @param secs
	 * @return
	 */
	private String getTimeFromLong(long secs) {
		
		int seconds = (int) (secs) % 60 ;
		int minutes = (int) ((secs / (60)) % 60);
		int hours   = (int) ((secs / (60 * 60)) % 24);
		
		return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
	}
	
	/**
	 * 
	 * @param time
	 * @return
	 */
	private long getElapsedTime(String time) {
		time = time.replace("PT", "");
		long timeLong = 0;
		try {
			String tokens[] = time.split("[HMS]");
			if(tokens.length == 3) {
				timeLong = Integer.parseInt(tokens[0]) + Integer.parseInt(tokens[1]) * 60 + Integer.parseInt(tokens[2]) * 3600;
			}
			else if(tokens.length == 2) {
				timeLong = Integer.parseInt(tokens[0]) * 60 + Integer.parseInt(tokens[1]);
			}
			else if(tokens.length == 1) {
				timeLong = Integer.parseInt(tokens[0]);
			}
		}
		catch(Exception e) {
			
		}
		return timeLong;
	}
}
