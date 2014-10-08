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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;

public class ExternalPlanMgr {
	List<ExternalFlightPlan> 	mPlans;	// Collection of external flight plans
	StorageService	mService;
	Context			mContext;
	
	/***
	 * public constructor for user defined waypoints collection
	 * @param service the storage service
	 * @param application context
	 */
	public ExternalPlanMgr(StorageService service, Context context) {
		mService = service;
		mContext = context;
		
		// Time to load all the points in
		forceReload();
	}

	/***
	 * Build up a string[] that represents all plans we know about
	 * @return the plans
	 */
	public String[] getPlans() {
		if(0 == mPlans.size()) {
			return null;
		}
		
		String[] plans = new String[mPlans.size()];

		int idx = 0;
		for(ExternalFlightPlan plan : mPlans) {
			plans[idx++] = plan.toString();
		}
		return plans;
	}

	/***
	 * Return a specifically named plan
	 * @param name what to get
	 * @return the plan or null if not found
	 */
	public ExternalFlightPlan get(String name) {
		for(ExternalFlightPlan plan : mPlans) {
			if(plan.getName().equals(name)) {
				return plan;
			}
		}
		return null;
	}
	
	/***
	 * Set the named external flight plan to the indicated state
	 * @param name plan to find
	 * @param active turn it on or off
	 */
	public void setActive(String name, boolean active) {
		if(null != name) {
			for(ExternalFlightPlan plan : mPlans) {
				if(plan.getName().equals(name)) {
					plan.setActive(active);
					return;
				}
			}
		}
	}
	
	/***
	 * Clear out all of our defined plans
	 */
	void clear() {
		// If we already have a collection, then clear it
		if(null != mPlans) {
			mPlans.clear();
		}
		mPlans = null;
	}

	/***
	 * Reload the flight plans from disk
	 */
	public void forceReload() {
		// Find out where to look for the files
		Preferences pref = new Preferences(mContext);
		
		// Load them all in - use the UserDefinedWaypoints config location
		populate(pref.getUDWLocation());
	}
	
	/***
	 * Populate our collection of external flight plans
	 * in this directory
	 * @param directory where to look for the user defined external plan files
	 */
	void populate(String directory)
	{
		clear();
		
		// Start off with an empty collection
		mPlans = new ArrayList<ExternalFlightPlan>();

		// Ensure that the directory we are given is semi-reasonable
		if(null != directory && directory.length() > 0) {
			// Create the factory to parse the input files
			PlanFactory factory = new PlanFactory();
	
			// fileList will be used to hold the collection of files in this directory
			File dirFile = new File(directory);
			
			// Enumerate all the files that are in here
			File[] fileList = dirFile.listFiles();
			
			// For each file we found here
			if(null != fileList) {
				for(File file : fileList) {
					// Tell the factory to parse the file 
					ExternalFlightPlan plan = factory.parse(mService, file.getPath());
					if(null != plan) {
						mPlans.add(plan);
					}
				}
			}
		}
	}
}
