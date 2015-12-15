/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.instruments;

import android.content.Context;

import com.ds.avare.storage.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Fuel tank timer instrument. Implemented as a display field for the
 * top two status lines. It is initialized with a number of minutes to
 * count down - when zero is reached, a dialog is displayed telling the user
 * to switch tanks. The user must acknowledge the dialog, the count is then
 * reset.
 *
 * Gesture processing:
 * Single press - start/stop the running timer.
 * Long press   - reset the timer to the max value.
 */
public class FuelTimer extends Observable {
	private int 			mInterval;
	private int 			mCurrentValue;
	private Timer 			mTimer;
	private boolean 		mCounting;
	private List<Observer> mObservers;

	public static final int REFRESH = 0;
	public static final int SWITCH_TANK = 1;
	
	/**
	 * ctor 
	 */
	public FuelTimer(Context ctx) {
		mInterval = new Preferences(ctx).getFuelTimerInterval();
		mCounting = false;
		mObservers = new ArrayList<Observer>();
		reset();
	}

	/**
	 * Add this observer to our collection
	 * @param observer
	 */
	public void addObserver(Observer observer) {
		synchronized(mObservers) {
			if(false == mObservers.contains(observer)) {
				mObservers.add(observer);
			}
		}
	}
	
	/**
	 * Someone is no longer interested in what we have to say
	 * @param observer
	 */
	public void removeObserver(Observer observer) {
		synchronized(mObservers) {
			mObservers.remove(observer);
		}
	}
	
	/**
	 * Notify all of our observers of an event
	 * @param event
	 */
	public void notifyObservers(int event) {
		synchronized(mObservers) {
			for(Observer o : mObservers) {
				o.update(this, event);
			}
		}
	}
	
	/**
	 * Start the countdown clock. Create a timer that counts 
	 * once per second.
	 */
	private void start() {
        mTimer = new Timer();
        TimerTask taskHobbs = new FuelTimerTask();
        mTimer.scheduleAtFixedRate(taskHobbs, 1000, 1000);
        mCounting = true;
	}

	/**
	 * Stop counting down. Cancel the timer.
	 */
	private void stop() {
		mCounting = false;
		mTimer.cancel();
	}

	/**
	 * Get the value MM.SS to display. If we detect we are at zero, then send out
	 * a message to all observers that we should switch tanks
	 * 
	 * @return How much time left before switching tanks
	 */
	public String  getDisplay() {
		if(0 == mCurrentValue) {
			mCurrentValue--;
			notifyObservers(SWITCH_TANK);
		}
		
		// Account for the value being negative, just display zero
		if (0 > mCurrentValue) {
			return "00.00";
		}
		
		return String.format(Locale.getDefault(), "%02d.%02d", 
						mCurrentValue / 60, mCurrentValue % 60);
	}

	/***
	 * Set our current time to the max allowable
	 */
	public void reset() {
		mCurrentValue = mInterval * 60;
	}
	
	/**
	 * Task that runs once per second. Decrement our counter and
	 * tell the view to re-draw.
	 * @author Ron
	 *
	 */
	private class FuelTimerTask extends TimerTask {
		public void run() {
			if(0 < mCurrentValue) {
				mCurrentValue--;
				notifyObservers(REFRESH);
			}
		}
	}

	/**
	 * Start/stop the countdown timer.
	 */
	public void toggleState() {
		if (true == mCounting) {
			stop();
		} else {
			start();
		}
	}
}
