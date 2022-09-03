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
import com.ds.avare.utils.WindsAloftHelper;

/**
 * 
 * @author zkhan
 *
 */
public class WindsAloft {

    private String time;
    private String station;
    private String w0k;
    private String w3k;
    private String w6k;
    private String w9k;
    private String w12k;
    private String w18k;
    private String w24k;
    private String w30k;
    private String w34k;
    private String w39k;
    private float lon;
    private float lat;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getW0k() {
        return w0k;
    }

    public void setW0k(String w0k) {
        this.w0k = w0k;
    }

    public String getW3k() {
        return w3k;
    }

    public void setW3k(String w3k) {
        this.w3k = w3k;
    }

    public String getW6k() {
        return w6k;
    }

    public void setW6k(String w6k) {
        this.w6k = w6k;
    }

    public String getW9k() {
        return w9k;
    }

    public void setW9k(String w9k) {
        this.w9k = w9k;
    }

    public String getW12k() {
        return w12k;
    }

    public void setW12k(String w12k) {
        this.w12k = w12k;
    }

    public String getW18k() {
        return w18k;
    }

    public void setW18k(String w18k) {
        this.w18k = w18k;
    }

    public String getW24k() {
        return w24k;
    }

    public void setW24k(String w24k) {
        this.w24k = w24k;
    }

    public String getW30k() {
        return w30k;
    }

    public void setW30k(String w30k) {
        this.w30k = w30k;
    }

    public String getW34k() {
        return w34k;
    }

    public void setW34k(String w34k) {
        this.w34k = w34k;
    }

    public String getW39k() {
        return w39k;
    }

    public void setW39k(String w39k) {
        this.w39k = w39k;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long timestamp;
    
    /**
     * 
     */
    public WindsAloft() {
        w0k = w3k = w6k = w9k = w12k = w18k = w24k = w30k = w34k = w39k = station = time = "";
    }
 
    /**
     * 
     * @param copy
     */
    public WindsAloft(WindsAloft copy) {
        w0k = copy.w0k;
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
    public double[] getWindAtAltitude(double altitude, double[] metarWind) {


        String wstring1;
        String wstring2;
        double wind[] = new double[2]; // speed, direction
        double higherAltitude;
        double lowerAltitude;

        // use metar wind for interpolation to ground if available
        if (null != metarWind) {

            double mdir = metarWind[0] / 10;
            double mspeed = metarWind[1];
            if(mspeed >= 100) {
                mspeed -= 100;
                mdir += 50;
            }

            w0k = String.format("%02d%02d", (int)mdir, (int)mspeed);
            if (w3k.equals("")) {
                w3k = w0k;
            }
            if (w6k.equals("")) {
                w6k = w0k;
            }
            if (w9k.equals("")) {
                w9k = w0k;
            }
            if (w12k.equals("")) {
                w12k = w0k;
            }
            if (w18k.equals("")) {
                w18k = w0k;
            }
            if (w24k.equals("")) {
                w24k = w0k;
            }
            if (w30k.equals("")) {
                w30k = w0k;
            }
            if (w34k.equals("")) {
                w34k = w0k;
            }
            if (w39k.equals("")) {
                w39k = w0k;
            }
        }

        // slope of line, wind at y and altitude at x, y = mx + b
        // slope = (wind_at_higher_altitude - wind_at_lower_altitude) / (higher_altitude - lower_altitude)
        // wind =  slope * altitude + wind_intercept
        // wind_intercept = wind_at_lower_altitude - slope * lower_altitude

        // fill missing wind from higher altitude
        if(w34k.equals("")) {
            w34k = w39k;
        }
        if(w30k.equals("")) {
            w30k = w34k;
        }
        if(w24k.equals("")) {
            w24k = w30k;
        }
        if(w18k.equals("")) {
            w18k = w24k;
        }
        if(w12k.equals("")) {
            w12k = w18k;
        }
        if(w9k.equals("")) {
            w9k = w12k;
        }
        if(w6k.equals("")) {
            w6k = w9k;
        }
        if(w3k.equals("")) {
            w3k = w6k;
        }
        if(w0k.equals("")) {
            w0k = w3k;
        }
        wind[0] = 0;
        wind[1] = 0;
        if (altitude < 0) {
            return wind;
        }
        else if(altitude >= 0 && altitude < 3000) {
            higherAltitude = 3000;
            lowerAltitude = 0;
            wstring1 = w3k;
            wstring2 = w0k;
        }
        else if(altitude >= 3000 && altitude < 6000) {
            higherAltitude = 6000;
            lowerAltitude = 3000;
            wstring1 = w6k;
            wstring2 = w3k;
        }
        else if(altitude >= 6000 && altitude < 9000) {
            higherAltitude = 9000;
            lowerAltitude = 6000;
            wstring1 = w9k;
            wstring2 = w6k;
        }
        else if(altitude >= 9000 && altitude < 12000) {
            higherAltitude = 12000;
            lowerAltitude = 9000;
            wstring1 = w12k;
            wstring2 = w9k;
        }
        else if(altitude >= 12000 && altitude < 18000) {
            higherAltitude = 18000;
            lowerAltitude = 12000;
            wstring1 = w18k;
            wstring2 = w12k;
        }
        else if(altitude >= 18000 && altitude < 24000) {
            higherAltitude = 24000;
            lowerAltitude = 18000;
            wstring1 = w24k;
            wstring2 = w18k;
        }
        else if(altitude >= 24000 && altitude < 30000) {
            higherAltitude = 30000;
            lowerAltitude = 24000;
            wstring1 = w30k;
            wstring2 = w24k;
        }
        else if(altitude >= 30000 && altitude < 34000) {
            higherAltitude = 34000;
            lowerAltitude = 30000;
            wstring1 = w34k;
            wstring2 = w30k;
        }
        else {
            higherAltitude = 39000;
            lowerAltitude = 34000;
            wstring1 = w39k;
            wstring2 = w34k;
        }

        try {
            WindsAloftHelper.DirSpeed higherWind, lowerWind;

            higherWind = WindsAloftHelper.DirSpeed.parseFrom(wstring1);
            lowerWind = WindsAloftHelper.DirSpeed.parseFrom(wstring2);
            double slope = ((higherWind.Speed - lowerWind.Speed) / (higherAltitude - lowerAltitude));
            double intercept = lowerWind.Speed - slope * lowerAltitude;
            wind[0] =  slope * altitude + intercept;

            slope = ((higherWind.Dir - lowerWind.Dir) / (higherAltitude - lowerAltitude));
            intercept = lowerWind.Dir - slope * lowerAltitude;
            wind[1] =  slope * altitude + intercept;
        } catch (Exception e) {
            wind[0] = 0;
            wind[1] = 0;
        }

        return wind;
    }

}
