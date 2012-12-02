/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


/**
 * @author zkhan
 *
 */
public class TrackShape extends Shape {

    double mDestLon;
    double mDestLat;
    
    private static final int SEGMENTS = 10;
    
    /**
     * Set the destination for this track 
     */
    public TrackShape(Destination destination) {
        
        /*
         * No label for track line
         */
        super("");
        mDestLon = destination.getLocation().getLongitude();
        mDestLat = destination.getLocation().getLatitude();
    }

    /**
     * Update track as the aircraft moves 
     */
    public void updateShape(GpsParams loc) {
        
        double lastLon = loc.getLongitude();
        double lastLat = loc.getLatitude();
        
        double lonstep = (mDestLon - lastLon) / SEGMENTS; 
        double latstep = (mDestLat - lastLat) / SEGMENTS;
        
        super.mCoords.clear();
        /*
         * Now make shape from coordinates with segments
         */
        for(int i = 0; i <= SEGMENTS; i++) {
            super.add(lastLon + i * lonstep, lastLat + i * latstep);
        }
    }
}
