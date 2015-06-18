/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.shapes;

import android.graphics.Color;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;


/**
 * @author zkhan
 *
 */
public class TrackShape extends Shape {

    private static final int MILES_PER_SEGMENT = 50;
    
    private static final int LEG_PREV = Color.GRAY;
    private static final int LEG_CURRENT = Color.GREEN;
    private static final int LEG_NEXT = Color.MAGENTA;

    public static int getLegColor(int dstNxt, int segNum) {
    	if(dstNxt <= segNum) {
    		return LEG_NEXT;
    	} else if(dstNxt - 1 == segNum) {
    		return LEG_CURRENT;
    	} else { 
    		return LEG_PREV;
    	}
    }
    
    /**
     * Set the destination for this track 
     */
    public TrackShape() {
        
        /*
         * No label for track line
         */
        super("");
    }

    /**
     * Update track as the aircraft moves 
     */
    public void updateShape(GpsParams loc, Destination destination) {
    
        /*
         * Where the aircraft is
         */
        double lastLon = loc.getLongitude();
        double lastLat = loc.getLatitude();
        double destLon = 0;
        double destLat = 0;
        

        if(null != destination) {
            destLon = destination.getLocation().getLongitude();
            destLat = destination.getLocation().getLatitude();
        }

        Projection p = new Projection(lastLon, lastLat, destLon, destLat);
        int segments = (int)p.getDistance() / MILES_PER_SEGMENT + 3; // Min 3 points
        Coordinate coord[] = p.findPoints(segments);
        super.mCoords.clear();
        
        /*
         * Now make shape from coordinates with segments
         */
        coord[0].makeSeparate();
        coord[segments - 1].makeSeparate();
        for(int i = 0; i < segments; i++) {
            super.add(coord[i].getLongitude(), coord[i].getLatitude(), coord[i].isSeparate());
        }
    }
    
    /**
     * Update track as the aircraft moves 
     */
    public void updateShapeFromPlan(Coordinate[] coord) {
    
        super.mCoords.clear();
        
        if(null == coord) {
            return;
        }
        /*
         * Now make shape from coordinates with segments
         */
        for(Coordinate c: coord) {
            super.add(c.getLongitude(), c.getLatitude(), c.isSeparate(), c.getLeg());
        }
    }
    
}
