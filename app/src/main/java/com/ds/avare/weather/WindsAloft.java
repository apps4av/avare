/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.weather;

import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.WeatherHelper;

/**
 * 
 * @author zkhan
 *
 */
public class WindsAloft {

    public String time;
    public String station;
    public String w3k; 
    public String w6k; 
    public String w9k; 
    public String w12k; 
    public String w18k; 
    public String w24k; 
    public String w30k; 
    public String w34k; 
    public String w39k; 
    public float lon;
    public float lat;
    
    public long timestamp;
    
    /**
     * 
     */
    public WindsAloft() {
        w3k = w6k = w9k = w12k = w18k = w24k = w30k = w34k = w39k = station = time = "";
    }
 
    /**
     * 
     * @param copy
     */
    public WindsAloft(WindsAloft copy) {
        w3k = copy.w3k;
        w6k = copy.w6k;
        w9k = copy.w9k;
        w12k = copy.w12k;
        w18k = copy.w18k;
        w24k = copy.w24k;
        w30k = copy.w30k;
        w34k = copy.w34k;
        w39k = copy.w39k;
        station = copy.station;
        time = copy.time;
        lon = copy.lon;
        lat = copy.lat;
        timestamp = copy.timestamp;
    }
    
    /**
     * 
     * @param lon0
     * @param lat0
     * @param variation
     */
    public void updateStationWithLocation(double lon0, double lat0, double variation) {
        Projection p = new Projection(lon, lat, lon0, lat0);
        station += "(" +
                Math.round(p.getDistance()) + Preferences.distanceConversionUnit + " " + 
                p.getGeneralDirectionFrom(variation)+ ")";

    }

    /**
     * Find winds aloft at given altitude. Use interpolation
     * @return
     */
    public double[] getWindAtAltitude(double altitude) {

        String wstring1 = "";
        String wstring2 = "";
        double wind[] = new double[2]; // speed, direction
        double fac = 0;
        if(altitude < 3000) {
            wstring1 = w3k;
            wstring2 = w3k;
            fac = 0;
        }
        else if(altitude >= 3000 && altitude < 6000) {
            wstring1 = w3k;
            wstring2 = w6k;
            fac = ((double)altitude - 3000) / altitude;
        }
        else if(altitude >= 6000 && altitude < 9000) {
            wstring1 = w6k;
            wstring2 = w9k;
            fac = ((double)altitude - 6000) / altitude;
        }
        else if(altitude >= 9000 && altitude < 12000) {
            wstring1 = w9k;
            wstring2 = w12k;
            fac = ((double)altitude - 9000) / altitude;
        }
        else if(altitude >= 12000 && altitude < 18000) {
            wstring1 = w12k;
            wstring2 = w18k;
            fac = ((double)altitude - 12000) / altitude;
        }
        else if(altitude >= 18000 && altitude < 24000) {
            wstring1 = w18k;
            wstring2 = w24k;
            fac = ((double)altitude - 18000) / altitude;
        }
        else if(altitude >= 24000 && altitude < 30000) {
            wstring1 = w24k;
            wstring2 = w30k;
            fac = ((double)altitude - 24000) / altitude;
        }
        else if(altitude >= 30000 && altitude < 34000) {
            wstring1 = w30k;
            wstring2 = w34k;
            fac = ((double)altitude - 30000) / altitude;
        }
        else if(altitude >= 34000 && altitude <= 39000) {
            wstring1 = w34k;
            wstring2 = w39k;
            fac = ((double)altitude - 34000) / altitude;
        }
        else {
            wstring1 = w39k;
            wstring2 = w39k;
            fac = 0;
        }

        // interpolate wind
        int d1 = WeatherHelper.decodeWindDir(wstring1);
        int s1 = WeatherHelper.decodeWindSpeed(wstring1);
        int d2 = WeatherHelper.decodeWindDir(wstring2);
        int s2 = WeatherHelper.decodeWindSpeed(wstring2);

        wind[0] = ((double)s2 - (double)s1) * fac + s1;
        wind[1] = (((double)d2 - (double)d1) * fac + d1) % 360;

        return wind;
    }
}
