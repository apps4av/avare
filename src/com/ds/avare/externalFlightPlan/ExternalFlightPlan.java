package com.ds.avare.externalFlightPlan;

import java.util.List;

import org.json.JSONArray;

import com.ds.avare.place.Destination;
import com.ds.avare.userDefinedWaypoints.Waypoint;

public class ExternalFlightPlan {
	String mName;
	String mDesc;
	String mType;
	String mCreator;
	String mExt;
	List<Waypoint> mWaypoints;

	public List<Waypoint> getWaypoints() { return mWaypoints; }
	public String getName() { return mName; }
	public String getDesc() { return mDesc; }
	public String getType() { return mType; }
	
	void setCreator(String creator) { mCreator = creator; } 
	
	public ExternalFlightPlan(String name, String desc, String type, List<Waypoint> waypoints) {
		mName  = name;
		mDesc  = desc;
		mType  = type;
		mWaypoints = waypoints;
	}
	
	/***
	 * Return a string representation of this flight plan
	 */
	public String toString() {
		String plan = mName + "::";
		for(int idx = 0; idx < mWaypoints.size(); ) {
			Waypoint wp = mWaypoints.get(idx);
			plan += wp.getName() + "(" + Destination.UDW + ")";
			if(++idx < mWaypoints.size()) {
				plan += ">";
			}
		}
		return plan;
	}
	
	/***
	 * Return a JSON formatted string that contains the way points of this plan
	 * @return
	 */
	public String toJSONString() {
        JSONArray jsonArr = new JSONArray();
		for(int idx = 0; idx < mWaypoints.size(); idx++) {
			Waypoint wp = mWaypoints.get(idx);
			jsonArr.put(Destination.getStorageName(Destination.UDW, null, null, wp.getName()));
        }
        return jsonArr.toString();
	}
	
	/***
	 * Set this plan as active/inactive. That means turning on/off all of the waypoints so they
	 * will be displayed or not
	 * 
	 * @param active 
	 */
	public void setActive(boolean active) {
		for(int idx = 0; idx < mWaypoints.size(); idx++) {
			mWaypoints.get(idx).setVisible(active);
		}
	}
}
