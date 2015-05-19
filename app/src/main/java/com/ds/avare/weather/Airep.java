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

/**
 * 
 * @author zkhan
 *
 */
public class Airep {

    public static final int RADIUS = 5; // degrees;
    
    public String time;
    public String reportType;
    public String rawText;
    public float lon;
    public float lat;
    
    public long timestamp;
    
    /**
     * 
     */
    public Airep() {
        
    }
    
    /**
     * 
     * @param copy
     */
    public Airep(Airep copy) {
        time = copy.time;
        reportType = copy.reportType;
        lon = copy.lon;
        lat = copy.lat;
        rawText = copy.rawText;
        timestamp = copy.timestamp;
    }
    
    /**
     * 
     * @param lon0
     * @param lat0
     * @param variation
     */
    public void updateTextWithLocation(double lon0, double lat0, double variation) {
        Projection p = new Projection(lon, lat, lon0, lat0);
        reportType += "(" +
                Math.round(p.getDistance()) + Preferences.distanceConversionUnit + " " + 
                p.getGeneralDirectionFrom(variation)+ ")";

    }

}


