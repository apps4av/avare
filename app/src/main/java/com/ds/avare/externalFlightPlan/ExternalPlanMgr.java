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
	Preferences		mPref;
	
	/***
	 * public constructor for user defined waypoints collection
	 * @param service the storage service
	 * @param application context
	 */
	public ExternalPlanMgr(StorageService service, Context context) {
		mService = service;
		mContext = context;
		mPref = new Preferences(mContext);

		// Time to load all the points in
		forceReload();
	}

	/***
	 * Return the configured directory where we find our plans
	 * @return
	 */
	private String getDir() {
		return mPref.getUDWLocation();
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
	 * Return a collection of plan names that contain the desired string
	 * @param likeThis Plan name contains this string, if null or zero length, add it
	 * @return the collection of strings that qualify
	 */
	public ArrayList<String> getPlanFileNames(String likeThis) {
		
		// The collection to hold our results
		ArrayList<String> planNames = new ArrayList<String>();
		
		// fileList will be used to hold the collection of files in this directory
		File dirFile = new File(getDir());
		
		// Enumerate all the files that are in here
		File[] fileList = dirFile.listFiles();
		
		// For each file we found here
		if(null != fileList) {
			for(File file : fileList) {
				String planName = file.getName();
				if(null != likeThis && likeThis.length() > 0) { 
					if(true == planName.contains(likeThis)) {
						planNames.add(planName);
					}
				} else {
					planNames.add(planName);
				}
			}
		}
		return planNames;
	}

	/***
	 * Return a collection of plan names that contain the desired string
	 * @param likeThis Plan name contains this string, if null or zero length, add it
	 * @return the collection of strings that qualify
	 */
	public ArrayList<String> getPlanNames(String likeThis) {
		
		// The collection to hold our results
		ArrayList<String> planNames = new ArrayList<String>();
		
		for(ExternalFlightPlan plan : mPlans) {
			String planName = plan.getName();
			if(null != likeThis && likeThis.length() > 0) { 
				if(true == planName.contains(likeThis)) {
					planNames.add(planName);
				}
			} else {
				planNames.add(planName);
			}
		}
		return planNames;
	}

	/***
	 * Return a specifically named plan
	 * @param name what to get
	 * @return the plan or null if not found
	 */
	public ExternalFlightPlan get(String name) {
		if(null != name) {
			for(ExternalFlightPlan plan : mPlans) {
				if(true == plan.getName().equalsIgnoreCase(name)) {
					return plan;
				}
			}
		}
		return null;
	}

	/***
	 * Is the indicated name saved as an external plan ?
	 * @param name
	 * @return true/false
	 */
	public boolean isExternal(String name) {
		return null == get(name) ? false : true; 
	}
	
	/***
	 * Delete the named plan from our collection
	 * @param name - what plan to delete
	 */
	public boolean delete(String name) {
		// Find the plan based upon its name
		ExternalFlightPlan plan = get(name);
		if (null != plan) {
			// We have the plan, now get the name of the file it was saved as
			File file = new File(plan.getFileName());
			
			// Do the delete, if successful, then remove it from our plan collection
			if(true == file.delete()) {
				mPlans.remove(plan);
				return true;
			}
		}
		return false;
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
		// Load them all in - use the UserDefinedWaypoints config location
		populate(getDir());
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
					ExternalFlightPlan plan = factory.parse(file.getPath());
					if(null != plan) {
						// Only add this new one if we do NOT have one with the same name
						if (null == get(plan.mName)) {
							// Set the full file path in this plan
							plan.setFileName(file.getPath());
							mPlans.add(plan);
						}
					}
				}
			}
		}
	}
}
