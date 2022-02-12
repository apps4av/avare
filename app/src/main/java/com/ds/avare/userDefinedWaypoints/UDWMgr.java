/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.userDefinedWaypoints;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/***
 * User Defined Waypoint Manager.
 * 
 * The UDW is a user defined collection of locations with a name. It is passed the underlying service, context
 * and a directory at creation. This directory is scanned and all files found are parsed for waypoints by
 * the factory.
 * @author Ron
 *
 */
public class UDWMgr {
	static final int MAXUDW = 100;
	Paint 			mPaint;		// Paint object used to do the display work
	List<Waypoint>  mPoints;	// Collection of points of interest
	StorageService	mService;
	Context			mContext;
	float			mPix;
	float 			m2Pix;
	float 			m15Pix;
    public static String UDWDESCRIPTION;

	/***
	 * public constructor for user defined waypoints collection
	 * @param service the storage service
	 * @param context context
	 */
	public UDWMgr(StorageService service, Context context) {
		mService = service;
		mContext = context;
		
		// Time to load all the points in
		forceReload();
		
		// Allocate and initialize the paint object
		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        
        setDipToPix(Helper.getDpiToPix(context));

    	UDWDESCRIPTION = mContext.getString(R.string.UDWDescription);
	}

	public int getCount() {
		return mPoints.size();
	}
	
	/***
	 * Empty out the collection of points that we have.
	 */
	void clear() {
		// If we already have a collection, then clear it
		if(null != mPoints) {
			mPoints.clear();
		}
		mPoints = null;
	}

	/***
	 * Reload the datapoints from the configured directory
	 */
	public void forceReload() {
		// Find out where to look for the files
		Preferences pref = new Preferences(mContext);
		
		// Load them all in
		populate(pref.getUserDataFolder());
	}
	
	/***
	 * Populate our collection of points based upon the files found
	 * in this directory
	 * @param directory where to look for the user defined waypoint files
	 */
	void populate(String directory)
	{
		clear();
		
		// Start off with an empty collection
		mPoints = new ArrayList<Waypoint>();

		// Ensure that the directory we are given is semi-reasonable
		if(null != directory && directory.length() > 0) {
			// Create the factory to parse the input files
			UDWFactory factory = new UDWFactory();
	
			// fileList will be used to hold the collection of files in this directory
			File dirFile = new File(directory);
			
			// Enumerate all the files that are in here
			File[] fileList = dirFile.listFiles();

			if(null != fileList) {
				// For each file we found here
				for(File file : fileList) {
					
					// Tell the factory to parse the file and get the collection of entries
					List<Waypoint> waypoints = factory.parse(file.getPath());
	
					// If we found some entries here ...
					if(null != waypoints) {
						for(Waypoint p : waypoints) {
							add(p);
						}
					}
				}
			}
		}
	}

	/***
	 * Add the specific waypoint to our collection
	 * Duplicates not allowed
	 * Max of MAXUDW in our collection
	 * @param waypoint
	 */
	public void add(Waypoint waypoint) {
		if(null != waypoint) {
			if(mPoints.size() < MAXUDW) {
				mPoints.add(waypoint);
			}
		}
	}
	
	/***
	 * Remove the specified waypoint from our collection
	 * @param waypoint what to forget
	 */
	public void remove(Waypoint waypoint) {
		if(false == waypoint.getLocked()) {
			mPoints.remove(waypoint);
		}
	}
	
	/***
	 * Calculate the "device independent pixel" to "display pixel" conversion factor
	 * @param dipToPix
	 */
	void setDipToPix(float dipToPix) {
        mPix = dipToPix;
        m2Pix = 2 * mPix;
        m15Pix = 15 * mPix;
	}
	
	/***
	 * Time to draw all of our points on the display
	 * 
	 * @param canvas Where to draw
	 * @param face Typeface to use
	 * @param origin Top/Left origin of the logical display
	 */
	public void draw(Canvas canvas, boolean trackUp, GpsParams gpsParams, Typeface face, Origin origin) {
		
		// If there are no points to display, then just get out of here
		if(null == mPoints) {
			return;
		}

		// Set some paint specs up here
        mPaint.setTypeface(face);
        mPaint.setTextSize(m15Pix);
        mPaint.setShadowLayer(2, 3, 3, Color.BLACK );

		// Loop through every point that we have and draw them if its set visible
		for (Waypoint p : mPoints) {
			if(true == p.getVisible()) {
				p.draw(canvas, origin, trackUp, gpsParams, mPaint, mService, whereAndHowFar(p), m2Pix);
			}
		}
	}

    // Calculate the distance and bearing to the point from our current location
    //
    String whereAndHowFar(Waypoint p) {
    	GpsParams gpsParams = mService.getGpsParams();
    	if(null == gpsParams) {
    		return "";
    	}
    	
    	// Heading from current location
    	double hdg = Projection.getStaticBearing(gpsParams.getLongitude(), gpsParams.getLatitude(), p.mLon, p.mLat);

    	// Adjust heading for declination
    	hdg = Helper.getMagneticHeading(hdg, gpsParams.getDeclinition());
    	
    	// distance from current location
    	double dst = Projection.getStaticDistance(gpsParams.getLongitude(), gpsParams.getLatitude(), p.mLon, p.mLat);
    	
    	// return  a formatted string
    	return String.format("%03d %03d", (int) dst, (int) hdg);
    }
    
    // Search our list for a name that closely matches what is passed in.
    //
    public void search(String name, LinkedHashMap<String, String> params) {
    	if(null != mPoints) {
    		String uName = name.toUpperCase();
    		for(Waypoint p : mPoints) {
    			String mName = p.mName.toUpperCase();
    			if (mName.startsWith(uName)) {
    		        StringPreference s = new StringPreference(Destination.UDW, Destination.UDW, UDWDESCRIPTION, p.mName);
    		        s.putInHash(params);
    			}
    		}
    	}
    }

    /***
     * Return the named waypoint object
     * @param name
     * @return
     */
    public Waypoint get(String name){
    	if(null != mPoints) {
    		for(Waypoint p : mPoints) {
    			if(true == p.mName.equalsIgnoreCase(name)) {
    				return p;
    			}
    		}
    	}
    	return null;
    }
}
