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
 * A class that deals with projected distances and bearings between two points
 * Uses rhumb line
 */
public class Projection {

    /**
     *
     */
    private double mBearing;
    /**
     * 
     */
    private double mDistance;

    private static final boolean rhumb = true;
    
    /**
     * @param lon1 Longitude of point 1
     * @param lat1 Latitude of point 1
     * @param lon2 Longitude of point 2
     * @param lat2 Latitude of point 2
     */
    public Projection(double lon1, double lat1, double lon2, double lat2) {
        
        
        //http://www.movable-type.co.uk/scripts/latlong.html
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        
        if(rhumb) {
            // distance rhumb line 
            double dLat = lat2 - lat1;
            double dLon = Math.abs(lon2 - lon1);
            
            double dPhi = Math.log(Math.tan(lat2 / 2 + Math.PI / 4) / Math.tan(lat1 / 2 + Math.PI / 4));
            double q = (dPhi != 0) ? dLat / dPhi : Math.cos(lat1);
            
            if (Math.abs(dLon) > Math.PI) {
                dLon = dLon > 0 ? - (2 * Math.PI - dLon) : (2 * Math.PI + dLon);
            }
            
            mDistance = Math.sqrt(dLat * dLat + q * q * dLon * dLon) * Preferences.earthRadiusConversion;
    
            // bearing rhumb line
            dLon = lon2 - lon1;
              
            if (Math.abs(dLon) > Math.PI) dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
            mBearing = (Math.toDegrees(Math.atan2(dLon, dPhi)) + 360) % 360;
        }
        else {
            
            // Haversine
            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;
            
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) * 
                    Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            mDistance = Preferences.earthRadiusConversion * c;
                       
            double y = Math.sin(dLon) * Math.cos(lat2);
            double x = Math.cos(lat1) * Math.sin(lat2) -
                    Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
            
            mBearing = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        }
    }

    /**
     * @return
     */
    public double getBearing() {
        return(mBearing);
    }

    /**
     * @return
     */
    public double getDistance() {
        return(mDistance);
    }

    /**
     * @return
     */
    public String getGeneralDirectionFrom() {
        
        String dir;
        final double DIR_DEGREES = 15;
        
        /*
         * These are important to show for traffic controller communications.
         */
        if(mBearing > (DIR_DEGREES) && mBearing <= (90 - DIR_DEGREES)) {
            dir = "SW";
        }
        else if(mBearing > (90 - DIR_DEGREES) && mBearing <= (90 + DIR_DEGREES)) {
            dir = "W";
        }
        else if(mBearing > (90 + DIR_DEGREES) && mBearing <= (180 - DIR_DEGREES)) {
            dir = "NW";
        }
        else if(mBearing > (180 - DIR_DEGREES) && mBearing <= (180 + DIR_DEGREES)) {
            dir = "N";
        }
        else if(mBearing > (180 + DIR_DEGREES) && mBearing <= (270 - DIR_DEGREES)) {
            dir = "NE";
        }
        else if(mBearing > (270 - DIR_DEGREES) && mBearing <= (270 + DIR_DEGREES)) {
            dir = "E";
        }
        else if(mBearing > (270 + DIR_DEGREES) && mBearing <= (360 - DIR_DEGREES)) {
            dir = "SE";
        }
        else {
            dir = "S";
        }

        return(dir);
    }
}
