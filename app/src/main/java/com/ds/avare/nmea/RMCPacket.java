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

import android.hardware.GeomagneticField;

/**
 * 
 * @author zkhan
 *
 */
public class RMCPacket extends Packet {
    
    public RMCPacket(long time, double latitude, double longitude, double speed, double bearing) {
        mPacket = "$GPRMC,";
        
        
        /*
         * Variation
         */
        GeomagneticField gmf = new GeomagneticField((float)latitude, 
                (float)longitude, 0, time);
        double dec = -gmf.getDeclination();

        /*
         * Convert to UTC system time, and format to hhmmss as in NMEA
         */
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mPacket += sdf.format(date) + ",";
        
        mPacket += "A,";

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
         * Put speed in m/s, convert to knots (this is because NMEA speed is in knots)
         */
        mPacket += String.format("%05.1f", speed / 0.514444);
        mPacket += ",";

        /*
         * Put bearing
         */
        mPacket += String.format("%05.1f", bearing);
        mPacket += ",";

        /*
         * Put date
         */
        sdf = new SimpleDateFormat("ddMMyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mPacket += sdf.format(date) + ",";
        

        /*
         * Put variation
         */
        if(dec < 0) {
            dec = -dec;
            mPacket += String.format("%05.1f", dec);
            mPacket += ",E";
        }
        else {
            mPacket += String.format("%05.1f", dec);
            mPacket += ",W";            
        }
      
        assemble();
        
    }

}
