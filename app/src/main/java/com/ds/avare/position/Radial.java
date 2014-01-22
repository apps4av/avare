/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.position;

import com.ds.avare.storage.Preferences;

/**
 * 
 * @author zkhan
 * A class that deals with radial distances
 */
public class Radial {

    /**
     * 
     * @param lon starting lon
     * @param lat starting lat
     * @param distance
     * @param bearing
     * @return
     */
    public static Coordinate findCoordinate(double lon, double lat, double distance, double bearing) {
        
        //http://www.movable-type.co.uk/scripts/latlong.html

        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double tc = Math.toRadians(bearing);
        double d = distance / Preferences.earthRadiusConversion;
        
        double mlat = Math.asin(Math.sin(lat1) * Math.cos(d) + 
                Math.cos(lat1) * Math.sin(d) * Math.cos(tc) );
        double mlon = lon1 + Math.atan2(Math.sin(tc) * Math.sin(d) * Math.cos(lat1), 
                       Math.cos(d) - Math.sin(lat1) * Math.sin(mlat));
        
        
        return new Coordinate(Math.toDegrees(mlon), Math.toDegrees(mlat));
    }
    
}
