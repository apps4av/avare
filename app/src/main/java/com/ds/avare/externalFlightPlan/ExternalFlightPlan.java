/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.externalFlightPlan;

import java.util.List;

import org.json.JSONArray;

import com.ds.avare.StorageService;
import com.ds.avare.place.Destination;
import com.ds.avare.userDefinedWaypoints.UDWMgr;
import com.ds.avare.userDefinedWaypoints.Waypoint;

/***
 * Encapsulation of an externally stored flight plan
 * 
 * @author Ron
 *
 */
public class ExternalFlightPlan {
	String mFileName;
	String mName;
	String mCmt;
	String mType;
	List<Waypoint> mWaypoints;
	
	// Get 'ers
	public List<Waypoint> getWaypoints() { return mWaypoints; }
	public String getName() { return mName; }
	public String getType() { return mType; }
	public String getCmt()  { return mCmt; }
	public String getFileName() { return mFileName; }

	// Set 'ers
	void setFileName(String fileName) { mFileName = fileName;
	
	}
	/***
	 * Construct a complete flight plan with the given parameters
	 * @param name Name of the plan, must be unique
	 * @param cmt Comment about the flight plan
	 * @param type GPX/CSV/KML etc
	 * @param waypoints Collection of waypoints that make up the plan
	 */
	public ExternalFlightPlan(String name, String cmt, String type, List<Waypoint> waypoints) {
		mName  = name;
		mCmt   = cmt;
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
			plan += wp.getName() + "(" + wp.getType() + ")";
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
		for(Waypoint wp : mWaypoints) {
			jsonArr.put(Destination.getStorageName(wp.getType(), null, null, wp.getName()));
        }
        return jsonArr.toString();
	}
	
	/***
	 * Set this plan as active/inactive. That means turning on/off all of the waypoints so they
	 * will be displayed or not
	 * 
	 * @param active true to show the waypoint, false to hide it
	 */
	public void setActive(boolean active) {
		for(Waypoint wp : mWaypoints) {
			wp.setVisible(active);
		}
	}

	/***
	 * We are being told to load our plan into memory. This involves telling the
	 * waypoint manager what points are in our path
	 * @param mService
	 */
	public void load(StorageService mService) {
		UDWMgr UdwMgr = mService.getUDWMgr();
		for(Waypoint wp : mWaypoints) {
			UdwMgr.add(wp);
		}
	}

	/***
	 * Time to unload the plan. Remove our waypoints from the manager
	 * @param mService
	 */
	public void unload(StorageService mService) {
		UDWMgr UdwMgr = mService.getUDWMgr();
		for(Waypoint wp : mWaypoints) {
			UdwMgr.remove(wp);
		}
	}
}
