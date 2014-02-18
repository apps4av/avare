/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.flight;

import java.util.Iterator;
import java.util.LinkedList;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.utils.Helper;

public class FlightStatus {
    /*
     * TODO: RAS - need better algorithm for detecting flight and landing
     *             could include height agl if it's accurate. 
     */
    private static final double SPEED_FOR_ROLLOUT = 40;
    private static final double SPEED_FOR_FLIGHT = 50;
    
    private boolean mFlying;
    private LinkedList<FlightStatusInterface> mCallbacks;
    
    public FlightStatus(GpsParams params) {
        mFlying = false;
        mCallbacks = new LinkedList<FlightStatusInterface>();
        
        if(null != params) {
            updateLocation(params);
        }
    }

    public void registerListener(FlightStatusInterface fsi) {
        synchronized(mCallbacks) {
            mCallbacks.add(fsi);
        }
    }
    public void unregisterListener(FlightStatusInterface fsi) {
        synchronized(mCallbacks) {
            mCallbacks.remove(fsi);
        }
    }    

    public void updateLocation(GpsParams params) {
        double currentSpeed = Helper.getSpeedInKnots(params.getSpeed());
        if(mFlying) {
            if(currentSpeed < SPEED_FOR_ROLLOUT) {
                mFlying = false;
                
                LinkedList<FlightStatusInterface> callbacks = (LinkedList<FlightStatusInterface>) mCallbacks.clone();
                Iterator<FlightStatusInterface> it = callbacks.iterator();
                while (it.hasNext()) {
                    FlightStatusInterface fsi = it.next();
                    fsi.rollout();
                }
            }
        }
        else if(currentSpeed > SPEED_FOR_FLIGHT) {
            mFlying = true;
        }        
    }
}
