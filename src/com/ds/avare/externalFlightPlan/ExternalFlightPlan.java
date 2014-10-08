package com.ds.avare.externalFlightPlan;

import java.util.List;

import com.ds.avare.place.Destination;
import com.ds.avare.userDefinedWaypoints.Waypoint;

public class ExternalFlightPlan {
	String mName;
	String mDesc;
	String mType;
	String mCreator;
	List<Waypoint> mWaypoints;

	public List<Waypoint> getWaypoints() { return mWaypoints; }
	public String getName() { return mName; }
	public String getDesc() { return mDesc; }

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
	 * Set this plan as active. That means turning on all of the waypoints so they
	 * will be displayed
	 * 
	 * @param active 
	 */
	public void setActive(boolean active) {
		for(int idx = 0; idx < mWaypoints.size(); idx++) {
			mWaypoints.get(idx).setVisible(active);
		}
	}
}
