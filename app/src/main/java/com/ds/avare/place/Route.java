/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.place;

import java.util.LinkedList;

import com.ds.avare.position.Coordinate;

/**
 * @author zkhan
 *
 */
public class Route {

    LinkedList<Destination> mDestinations;
    
    /**
     * 
     */
    public Route() {
        mDestinations = new LinkedList<Destination>();
    }

    /**
     * 
     * @return
     */
    public Destination getDestination() {
        return mDestinations.getFirst();
    }

    /**
     * 
     * @param d
     */
    public void add(Destination d) {
        mDestinations.add(d);
    }
    
    /**
     * 
     * @param d
     */
    public void clear() {
        mDestinations.clear();
    }

    /**
     * 
     * @return
     */
    public Coordinate[] getCoordinates() {
        
        /*
         * Get coordinates of the entire route
         */
        int len = mDestinations.size();
        int loop;
        if(0 == len) {
            return null;
        }
        Coordinate coords[] = new Coordinate[len];
        loop = 0;
        for (Destination d : mDestinations) {
            coords[loop] = new Coordinate(d.getLocation().getLongitude(), d.getLocation().getLatitude());;
            loop++;
        }
        
        return coords;
    }
}
