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
 * This class is a demo class that shows how to send NMEA packets to Avare from other apps.
 * In this case traffic message from ADSB with a made up NMEA message.
 *
 */
public class RTMPacket extends Packet {
    
    public RTMPacket(
        long time,
        int icaoAddress,
        float latitude,
        float longitude,
        int altitude,
        int horizVelocity,
        int vertVelocity,
        float heading,
        String callSign) {
        
        mPacket = "$GPRTM,";
        
        /*
         * Convert to UTC system time, and format to hhmmss as in NMEA
         */
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mPacket += sdf.format(date) + ",";
        
        //icao
        mPacket += Integer.toString(icaoAddress) + ",";

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
         * Put altitude
         */
        mPacket += String.format("%.1f", (float)altitude);
        mPacket += ",";

        /*
         * Put heading
         */
        mPacket += String.format("%05.1f", (float)heading);
        mPacket += ",";

        /*
         * Put speed in knots
         */
        mPacket += String.format("%05.1f", (float)horizVelocity);
        mPacket += ",";

        /*
         * Put vert velocity in ft/min
         */
        mPacket += String.format("%05.1f", (float)vertVelocity);
        mPacket += ",";

        /*
         * Put callsign
         */
        mPacket += callSign;
        mPacket += ",";

        assemble();
        
    }

}
