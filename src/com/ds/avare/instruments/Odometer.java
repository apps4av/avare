/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.instruments;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;

public class Odometer {
	private double		mValue;		// current value
	private double 		mValueSave;	// value last time we did a save
	private GpsParams	mGpsParams;	// last used gps parameters
	private Preferences mPref;		// How we access permanent storage
	
	public Odometer(){
	}
	
	public void setPref(Preferences pref) {
		mPref = pref;
	}
	
	/***
	 * Return the current value of the odometer
	 * @return
	 */
	public double getValue() {
		return mValue;
	}
	
	/***
	 * Reset the value of the odometer to zero.
	 * @param pref The preference object so as to clear the saved value
	 */
	public void reset() {
		if(mPref != null) {
			mValue = 0;
			mValueSave = 0;
			mGpsParams = null;
			mPref.setOdometer(mValue);
		}
	}

	/***
	 * Update the value of the odometer based upon the gpsParams passed in
	 * @param pref preferences object soas to save the current odometer value
	 * @param gpsParams current gps locations
	 */
	public void updateValue(GpsParams gpsParams) {
		if(mPref == null) {
			return;
		}
		
		if(gpsParams == null || mPref.isSimulationMode()) {
			return;
		}
		
		// Our first time in means we need to read the current setting from
		// the preferences
		if(mGpsParams == null) {
			mValue = mPref.getOdometer();
			mGpsParams = gpsParams;
			mValueSave = mValue;
		} else {
			
			// Not the first time, calculate how much distance between current and previous
			double distance = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), 
								gpsParams.getLongitude(), gpsParams.getLatitude()).getDistance();

			// if the distance is less than 1/10 mile, then ignore it for now
			if(distance < .1)
				return;
			
			// Adjust our odometer by the calculated distance
			// for display reasons, roll it over at 100,000 miles
			mValue += distance;
    		if (mValue > 100000) {
    			mValue -= 100000;
    		}
    		
			// If we traveled more than a half mile, then write it to the preferences for safe keeping
	    	if((mValue - mValueSave) > .5) {
	    		mPref.setOdometer(mValue);
	    		mValueSave = mValue;
	    	}
	    	
	    	// Set our last used GPS params
    		mGpsParams = gpsParams;
		}
		return;
	}
}
