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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Flight timer class. Extension of the HobbsMeter. It starts/stops the hobbs meter when
 * the detected ground speed is greater than a specified value. Yes, flying IN to a strong
 * headwind at stall speed might fool it ... 
 * @author Ron
 *
 */
public class FlightTimer extends HobbsMeter {
	private Timer		 mTimer;
	private double 		 mMinFlightSpeed = 20;	// 20 mph/kph/kts is fast enough to say we intend to fly
	private double		 mSpeed = 0;
	
	/**
	 * 
	 */
	public FlightTimer() {
        mTimer = new Timer();							// Create a timer for a thread to monitor the GPS speed
        TimerTask timerTask = new FlightTimerTask();	// The task thread that does the work
        mTimer.scheduleAtFixedRate(timerTask, 0, 1000);	// Set to run once per second
	}

	/**
     * Fires once per second to see if we need to turn the hobbs on or off
     */
    private class FlightTimerTask extends TimerTask {
        public void run() {
    		if(mSpeed >= mMinFlightSpeed) {	// Are we flying ?
        		if(isRunning() == true)		// If hobbs already running... 
        			return;					// ...then nothing to do
        		start();					// Otherwise start the hobbs
        	} 
    		else {						// We are no longer flying
        		if(isRunning() == false)	// Do we need to stop the hobbs ? 
        			return;					// no, just return
        		stop();						// yes - stop it here
        	}
        }
    }
    
    public void setSpeed(double speed) {
    	mSpeed = speed;
    }
    
    public void setMinFlightSpeed(double minFlightSpeed) {
    	mMinFlightSpeed = minFlightSpeed;
    }
}