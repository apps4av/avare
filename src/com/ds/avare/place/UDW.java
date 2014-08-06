/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.KmlUDWParser;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.UDWFactory;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

/***
 * User Defined Waypoint.
 * 
 * The UDW is a user defined collection of locations with a name. It is passed the underlying service, context
 * and a directory at creation. This directory is scanned and all files found are parsed for waypoints by
 * the factory.
 * @author Ron
 *
 */
public class UDW {
	Paint 			mPaint;		// Paint object used to do the display work
	List<UDWFactory.Placemark> mPoints;	// Collection of points of interest
	StorageService	mService;
	float			mPix;
	float 			m2Pix;
	float 			m5Pix;
	float 			m8Pix;
	float 			m15Pix;
	float 			m25Pix;
	float 			m30Pix;
	
	/***
	 * public constructor for user defined waypoints collection
	 * @param service the storage service
	 * @param application context
	 * @param directory where all of the POI files are located
	 */
	public UDW(StorageService service, Context context) {

		// Find out where to look for the files
		Preferences pref = new Preferences(context);
		String directory = pref.getUDWLocation();
		
		// Start off with an empty collection
		mPoints = new ArrayList<UDWFactory.Placemark>();

		// Ensure that the directory we are given is semi-reasonable
		if(null != directory && directory.length() > 0) {
			// Create the factory to parse the input files
			UDWFactory factory = new UDWFactory();
	
			// fileList will be used to hold the collection of files in this directory
			File dirFile = new File(directory);
			
			// Enumerate all the files that are in here
			File[] fileList = dirFile.listFiles();
			
			// For each file we found here
			for(File file : fileList) {
				
				// Tell the factory to parse the file and get the collection of entries
				List<UDWFactory.Placemark> entries = factory.parse(file.getPath());
				
				// If we found any, then add them to our stash
				if(null != entries) {
					mPoints.addAll(entries);
				}
			}
		}
		
		// Allocate and initialize the paint object
		mPaint = new Paint();
        mPaint.setAntiAlias(true);
        
        setDipToPix(Helper.getDpiToPix(context));
        
        mService = service;
	}

	// Calculate some of the display size constants
	void setDipToPix(float dipToPix) {
        mPix = dipToPix;
        m2Pix = 2 * mPix;
        m5Pix = 5 * mPix;
        m8Pix = 8 * mPix;
        m15Pix = 15 * mPix;
        m25Pix = 25 * mPix;
        m30Pix = 30 * mPix;
	}
	
	/***
	 * Time to draw all of our points on the display
	 * 
	 * @param canvas Where to draw
	 * @param face Typeface to use
	 * @param origin Top/Left origin of the logical display
	 */
	public void draw(Canvas canvas, Typeface face, Origin origin) {
		
		// If there are no points to display, then just get out of here
		if(null == mPoints) {
			return;
		}

		// Set some paint specs up here
        mPaint.setTypeface(face);
        mPaint.setTextSize(m15Pix);
        mPaint.setShadowLayer(2, 3, 3, Color.BLACK );

		// Loop through every point that we have
		//
		for (UDWFactory.Placemark p : mPoints) {

			// Map the lat/lon to the x/y of the current canvas
			float x = (float) origin.getOffsetX(p.mLon);
			float y = (float) origin.getOffsetY(p.mLat);

			switch(p.mMarkerType){
				case UDWFactory.Placemark.CYANDOT: {
					// Draw the filled circle, centered on the point
			        mPaint.setStyle(Style.FILL);
			        mPaint.setColor(Color.CYAN);
			        mPaint.setAlpha(0x9F);
			        canvas.drawCircle(x, y, (float) m8Pix, mPaint);
		
			        // A black ring around it to highlight it a bit
			        mPaint.setStyle(Style.STROKE);
			        mPaint.setColor(Color.BLACK);
			        mPaint.setStrokeWidth(m2Pix);
			        canvas.drawCircle(x, y, (float) m8Pix, mPaint);
			        break;
				}
			}
			
	        // Set the display text properties
	        mPaint.setStyle(Style.FILL);
	        mPaint.setColor(Color.WHITE);

	        // Draw the name above
	        mService.getShadowedText().draw(canvas, mPaint, p.mName, Color.BLACK, x, y - m25Pix);
	        
	        // and the distance/brg below IF that piece of metadata is true
	        if(true == p.mShowDist) {
				String dstBrg = whereAndHowFar(p);
		        mService.getShadowedText().draw(canvas, mPaint, dstBrg, Color.BLACK, x, y + m25Pix);
	        }
		}
	}

    // Calculate the distance and bearing to the point from our current location
    //
    String whereAndHowFar(UDWFactory.Placemark p) {
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
    		final String uName = name.toUpperCase();
    		for(int idx = 0; idx < mPoints.size(); idx++) {
    			UDWFactory.Placemark p = mPoints.get(idx);
    			final String mName = p.mName.toUpperCase();
    			if (mName.startsWith(uName)) {
    		        StringPreference s = new StringPreference(Destination.UDW, Destination.UDW, p.mDescription, p.mName);
    		        s.putInHash(params);
    			}
    		}
    	}
    }
    
    // Return the placemark for the given name. Uppercase compare for everything
    //
    public UDWFactory.Placemark getPlacemark(String name){
    	if(null != mPoints) {
    		final String uName = name.toUpperCase();
    		for(int idx = 0; idx < mPoints.size(); idx++) {
    			UDWFactory.Placemark p = mPoints.get(idx);
    			final String mName = p.mName.toUpperCase();
    			if (mName.equals(uName)) {
    				return p;
    			}
    		}
    	}
    	return null;
    }
}
