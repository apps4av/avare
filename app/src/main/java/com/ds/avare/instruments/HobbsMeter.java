/*
Copyright (c) 2013, Avare software (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.instruments;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * author : rwalker
 * Implementation of a simple HOBBS meter.
 * Maintains elapsed time between start/stop calls in 1/100 of a second increments discretely.
 * @author Ron
 *
 */
public class HobbsMeter {
	int		mHundredths = 0;
	int		mTenths = 0;
	int		mHours = 0;
	Timer	mTimer = null;
	
    /** 
     * Fires every second, and after 36 seconds, will adjust the hobbs counter up by 1/100 of an hour.
     * We need the one second resolution in order to "blink" the middle period character when the
     * timer is active.
     */
    private class HobbsTask extends TimerTask {

        public void run() {
        	synchronized(this) {
	        	mHundredths++;
	        	if(mHundredths > 9) {
	        		mHundredths = 0;
	        		mTenths++;
	        		if(mTenths > 9) {
	        			mTenths = 0;
	        			mHours++;
	        		}
	        	}
        	}
        }
    }

    /**
     * Setup this meter to the default condition
     */
    private void setup(){
    	stop();
    	reset();
	}

    /**
     * Default constructor.
     */
	public HobbsMeter() {
		setup();
	}

	/**
	 * Is this meter currently running
	 * @return true if it is keeping time, false otherwise
	 */
	public boolean isRunning() {
		return mTimer == null ? false : true;
	}
	
	/**
	 * Get the value of this HOBSS meter and return it in a formatted string
	 * @return "X:YY" where X is hours, and YY is hundredths of an hour. Always
	 * return a string that is 5 characters in length. The center period '.' will
	 * blink every second when the clock is running.
	 */
	public String getValue() {
		return String.format(Locale.getDefault(), "%02d.%d%d", mHours, mTenths, mHundredths);
	}
	
	/**
	 * Reset this HOBBS meter to empty
	 */
	public void reset() {
		synchronized (this) {	// Don't want the timer event to fire and
			                    // adjust any of these values until we have
	    	mHundredths = 0;	// them all cleared
	    	mTenths = 0;
	    	mHours = 0;
		}
	}
	
	/**
	 * Start this meter running. 
	 */
	public void start() {
        mTimer = new Timer();					// Create a timer for the hobbs meter
        TimerTask taskHobbs = new HobbsTask();	// The task thread that does the work
        mTimer.scheduleAtFixedRate(taskHobbs, 1, 36 * 1000);	// Set to run every 1/100 of an hour
	}
	
	/**
	 * Stop this meter
	 */
	public void stop() {
		if(mTimer != null) {
			mTimer.cancel();
		}
		mTimer = null;
	}
}
