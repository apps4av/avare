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

import android.location.Location;

import com.ds.avare.storage.Preferences;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


/**
 * 
 * @author zkhan
 *
 */
public class LmfsPlan {


    public static final String PROPOSED = "PROPOSED";
    public static final String TYPE = "ICAO";
	public static final String DIRECT = "DCT";
	public static final String RULE_VFR = "VFR";
	public static final String ROUTE_WIDTH = "50"; // nm for briefing

	private boolean mValid;
	private String mId;

	public String aircraftId;
	public String flightRule;
	public String flightType;
	public String noOfAircraft;
	public String aircraftType;
	public String wakeTurbulence;
	public String aircraftEquipment;
	public String departure;
	public String departureDate;
	public String cruisingSpeed;
	public String level;
	public String surveillanceEquipment;
	public String route;
	public String otherInfo;
	public String destination;
	public String totalElapsedTime;
	public String alternate1;
	public String alternate2;
	public String fuelEndurance;
	public String peopleOnBoard;
	public String aircraftColor;
	public String supplementalRemarks;
	public String pilotInCommand;
	public String pilotInfo;
	public String versionStamp;
	public String currentState;

	/**
	 * 
	 */
	private void init() {
		mValid = false;
		mId = null;

		aircraftId = "";
		flightRule = "";
		flightType = "";
		noOfAircraft = "";
		aircraftType = "";
		wakeTurbulence = "";
		aircraftEquipment = "";
		departure = "";
		departureDate = "";
		cruisingSpeed = "";
		level = "";
		surveillanceEquipment = "";
		route = "";
		otherInfo = "";
		destination = "";
		totalElapsedTime = "";
		alternate1 = "";
		alternate2 = "";
		fuelEndurance = "";
		peopleOnBoard = "";
		aircraftColor = "";
		supplementalRemarks = "";
		pilotInCommand = "";
		pilotInfo = "";
		currentState = PROPOSED;
		route = DIRECT;
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
	 * LMFS plan from Preferences
	 */
	public LmfsPlan(Preferences pref) {
		init();

        flightRule = RULE_VFR;
        aircraftId = pref.getAircraftTailNumber();
		aircraftType = pref.getAircraftType();
		flightType = "G"; // GA
        noOfAircraft = "1";
        wakeTurbulence = "LIGHT";
        route = DIRECT;
		surveillanceEquipment = pref.getAircraftSurveillanceEquipment();
        aircraftEquipment = pref.getAircraftEquipment();
        cruisingSpeed = String.valueOf(pref.getAircraftTAS());
		pilotInfo = pref.getPilotContact();
		pilotInCommand = pref.getPilotContact();
        peopleOnBoard = "1";
        aircraftColor = pref.getAircraftColorPrimary() + "/" + pref.getAircraftColorSecondary();

        if(surveillanceEquipment.equals("")) {
            surveillanceEquipment = "N";
        }
        if(aircraftEquipment.equals("")) {
            aircraftEquipment = "N";
        }
		mValid = true;
	}


	/**
	 * LMFS plan from JSON
	 * @param data will be like:
	 * {"versionStamp":"20150112162243160","actualDepartureInstant":null,"beaconCode":null,"currentState":"PROPOSED","sarTracking":true,"nasFlightPlan":{},"returnCodedMessage":[],"returnMessage":[],"artccInfo":null,"icaoFlightPlan":{...},"alertSubscription":true,"notificationsSubscription":true,"returnStatus":true,"artccState":null}
	 */
	public LmfsPlan(String data) {
		init();
		
		// Parse JSON
		try {
			/*
			 * Get plan from data which can be internet or storage.
			 */
			JSONObject json = new JSONObject(data);
			// Only support ICAO
			JSONObject icao = json.getJSONObject("icaoFlightPlan");
			if(icao == null) {
				return;
			}
			flightRule = icao.getString("flightRules");
			aircraftId = icao.getString("aircraftIdentifier");
			departure = icao.getJSONObject("departure").getString("locationIdentifier");
			destination = icao.getJSONObject("destination").getString("locationIdentifier");
			departureDate = icao.getString("departureInstant");
			totalElapsedTime = icao.getString("flightDuration");

			try {
				route = icao.getString("route");
				if(route.equals("null")) {
					route = null;
				}
			}
			catch(Exception e2) {
				route = "DCT";
			}
			if(route != null) {
				if(route.equals("null")) {
					route = null;
				}
			}

			try {
				alternate1 = icao.getJSONObject("altDestination1").getString("locationIdentifier");
			}
			catch (Exception e2) {
				alternate1 = null;
			}
			if(alternate1 != null) {
				if(alternate1.equals("null")) {
					alternate1 = null;
				}
			}

			try {
				alternate2 = icao.getJSONObject("altDestination2").getString("locationIdentifier");
			}
			catch (Exception e2) {
				alternate2 = null;
			}
			if(alternate2 != null) {
				if(alternate2.equals("null")) {
					alternate2 = null;
				}
			}

            try {
                otherInfo = icao.getString("otherInfo");
            }
            catch (Exception e) {
                otherInfo = null;
            }
            if(otherInfo != null) {
                if(otherInfo.equals("null")) {
                    otherInfo = null;
                }
            }
			aircraftType = icao.getString("aircraftType");
			aircraftEquipment = icao.getString("aircraftEquipment");
			noOfAircraft = icao.getString("numberOfAircraft");
			wakeTurbulence = icao.getString("wakeTurbulence");
			cruisingSpeed = icao.getJSONObject("speed").getString("speedKnots");
			level = icao.getJSONObject("altitude").getString("altitudeTypeA");
			fuelEndurance = icao.getString("fuelOnBoard");
			peopleOnBoard = icao.getString("peopleOnBoard");
			if(peopleOnBoard.equals("null")) {
				peopleOnBoard = "";
			}
			aircraftColor = icao.getString("aircraftColor");
			pilotInfo = icao.getString("pilotData");
			flightType = icao.getString("typeOfFlight");
            surveillanceEquipment = icao.getString("surveillanceEquipment");
			supplementalRemarks = icao.getString("suppRemarks");
			if(supplementalRemarks.equals("null")) {
				supplementalRemarks = "";
			}
			pilotInCommand = icao.getString("pilotInCommand");

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
				if(!val.equals("null")) {
					params.put(name, val);					
				}
			}
		}
	}
	
	/**
	 * Put the plan in hashmap
	 * @return
	 */
	public Map<String, String> makeHashMap() {
		Map<String, String> params = new HashMap<String, String>();
        put(params, "type", TYPE);
		put(params, "flightRules", flightRule);
		put(params, "aircraftIdentifier", aircraftId);
		put(params, "departure", departure);
		put(params, "destination", destination); 
		put(params, "departureInstant", LmfsPlan.getTimeFromInput(LmfsPlan.getTimeFromInstance(departureDate)));
		put(params, "flightDuration", totalElapsedTime);
        put(params, "route", route);
		put(params, "altDestination1", alternate1);
		put(params, "altDestination2", alternate2);
        put(params, "aircraftType", aircraftType);
        put(params, "otherInfo", otherInfo);
        put(params, "aircraftType", aircraftType);
        put(params, "aircraftEquipment", aircraftEquipment);
        put(params, "numberOfAircraft", noOfAircraft);
		put(params, "wakeTurbulence", wakeTurbulence);
		put(params, "speedKnots", cruisingSpeed);
		put(params, "altitudeTypeA", level);
		put(params, "fuelOnBoard", fuelEndurance);
        put(params, "peopleOnBoard", peopleOnBoard);
        put(params, "aircraftColor", aircraftColor);
        put(params, "pilotData", pilotInfo);
        put(params, "typeOfFlight", flightType);
        put(params, "surveillanceEquipment", surveillanceEquipment);
		put(params, "suppRemarks", supplementalRemarks);
        put(params, "pilotInCommand", pilotInfo);
		return params;
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
	 * 
	 * @return
	 */
	public static String getInstanceFromTime(String time) {
    	// fill time to now()
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dt;
        try {
        	dt = sdf.parse(time);
        }
        catch(Exception e) {
        	return "";
        }
        return "" + dt.getTime();
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
	 * Convert Gps coordinates to something liked by LMFS
	 * @param p
	 * @return
	 */
	public static String convertLocationToGpsCoords(Location p) {
		//2548N08017W
		double lat = Math.abs(p.getLatitude());
		double lon = Math.abs(p.getLongitude());
		int latd = (int)lat;
		int latm = (int) ((lat - (double)latd) * 60.0);
		int lond = (int)lon;
		int lonm = (int) ((lon - (double)lond) * 60.0);
		String latgeo;
		String longeo;
		
		if(p.getLatitude() < 0) {
			latgeo = "S";
		}
		else {
			latgeo = "N";
		}

		if(p.getLongitude() < 0) {
			longeo = "W";
		}
		else {
			longeo = "E";
		}
		
		String ret = String.format(Locale.getDefault(), "%02d%02d%s%03d%02d%s", latd, latm, latgeo, lond, lonm, longeo);
		
		return ret;
	}
}
