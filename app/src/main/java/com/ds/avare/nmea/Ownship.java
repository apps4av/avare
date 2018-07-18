/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.nmea;

/**
 * 
 * @author zkhan
 * A class that combines GGA and RMC message to make an ownship report.
 */
public class Ownship {

    public float mLat;
    public float mLon;
    
    public int     mAltitude;

    public int     mHorizontalVelocity;
    public int     mDirection;
    
    long mTime;

    /**
     * 
     */
    public Ownship() {
        mTime = 0;
        mLat = 0;
        mLon = 0;
        
        mAltitude = 0;

        mHorizontalVelocity = 0;
        mDirection = 0;
    }
    
    /**
     * 
     * @param m
     * @return If a new report is ready.
     */
    public boolean addMessage(Message m) {
        
        if (m instanceof GGAMessage) {
            mLat = ((GGAMessage) m).mLat;
            mLon = ((GGAMessage) m).mLon;
            mAltitude = ((GGAMessage) m).mAltitude;
            mTime = m.getTime();
            
            return true;
        }
        else if(m instanceof RMCMessage) {
            mLat = ((RMCMessage) m).mLat;
            mLon = ((RMCMessage) m).mLon;
            mDirection = ((RMCMessage) m).mDirection;
            mHorizontalVelocity = ((RMCMessage) m).mHorizontalVelocity;
            mTime = m.getTime();

            return true;
        }
        
        
        return false;
    }
    
    /**
     * 
     * @return
     */
    public long getTime() {
       return mTime; 
    }
}
