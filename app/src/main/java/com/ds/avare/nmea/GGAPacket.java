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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 
 * @author zkhan
 *
 */
public class GGAPacket extends Packet {

    public GGAPacket(long time, double latitude, double longitude, double altitude) {
        mPacket = "$GPGGA,";
        
        
        /*
         * Convert to UTC system time, and format to hhmmss as in NMEA
         */
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mPacket += sdf.format(date) + ",";
        
        /*
         * Put latitude
         */
        if(latitude > 0) {
            int lat;
            double deg;
            
            lat = (int)latitude;
            deg = (latitude - (double)lat) * 60.0;
            
            mPacket += String.format("%02d", lat);
            mPacket += String.format("%06.3f", deg);
            mPacket += ",N,";
        }
        else {
            int lat;
            double deg;
            latitude = -latitude;
            lat = (int)latitude;
            deg = (latitude - (double)lat) * 60.0;
            
            mPacket += String.format("%02d", lat);
            mPacket += String.format("%06.3f", deg);
            mPacket += ",S,";            
        }

        /*
         * Put longitude
         */
        if(longitude > 0) {
            int lon;
            double deg;
            
            lon = (int)longitude;
            deg = (longitude - (double)lon) * 60.0;
            
            mPacket += String.format("%03d", lon);
            mPacket += String.format("%06.3f", deg);
            mPacket += ",E,";
        }
        else {
            int lon;
            double deg;
            longitude = -longitude;
            lon = (int)longitude;
            deg = (longitude - (double)lon) * 60.0;
            
            mPacket += String.format("%03d", lon);
            mPacket += String.format("%06.3f", deg);
            mPacket += ",W,";            
        }

        /*
         * Sim mode
         */
        mPacket += "8,";
        
        /*
         * Do not have satellite info
         */
        mPacket += "00,";

        /*
         * Horizontal dilution 
         */
        mPacket += "1.0,";
        
        /*
         * Put altitude
         */
        mPacket += String.format("%.1f", altitude);
        mPacket += ",M,";

        /*
         * XXX:
         * Calculate geoid
         * 
         * Add couple of empty fields
         */
        mPacket += "0.0,M,,";

        assemble();
    }

}
