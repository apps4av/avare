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

import java.util.Locale;

/**
 * 
 * @author zkhan
 *
 */
public class RMBPacket extends Packet {
    
    public RMBPacket(double distance, double bearing, double longitude, double latitude, String idNext, String idOrig, double deviation, double speed, boolean planComplete) {
        mPacket = "$GPRMB,";
        
        //valid
        mPacket += "A,";

        // deviation
        String dir = "L";
        if(deviation < 0) {
            dir = "R";
            deviation = -deviation;
        }
        if(deviation > 9.99) {
            deviation = 9.99;
        }
        mPacket += String.format(Locale.getDefault(),"%04.2f", deviation);
        mPacket += ",";
        mPacket += dir;
        mPacket += ",";

        mPacket += idOrig;
        mPacket += ",";

        mPacket += idNext;
        mPacket += ",";
        
        /*
         * Put latitude
         */
        if(latitude > 0) {
            int lat;
            double deg;
            
            lat = (int)latitude;
            deg = (latitude - (double)lat) * 60.0;

            mPacket += String.format(Locale.getDefault(),"%02d", lat);
            mPacket += String.format(Locale.getDefault(),"%06.3f", deg);
            mPacket += ",N,";
        }
        else {
            int lat;
            double deg;
            latitude = -latitude;
            lat = (int)latitude;
            deg = (latitude - (double)lat) * 60.0;

            mPacket += String.format(Locale.getDefault(),"%02d", lat);
            mPacket += String.format(Locale.getDefault(),"%06.3f", deg);
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

            mPacket += String.format(Locale.getDefault(),"%03d", lon);
            mPacket += String.format(Locale.getDefault(),"%06.3f", deg);
            mPacket += ",E,";
        }
        else {
            int lon;
            double deg;
            longitude = -longitude;
            lon = (int)longitude;
            deg = (longitude - (double)lon) * 60.0;

            mPacket +=  String.format(Locale.getDefault(),"%03d", lon);
            mPacket += String.format(Locale.getDefault(),"%06.3f", deg);
            mPacket += ",W,";
        }

        /*
         * Put range
         */
        if(distance >= 1000) {
            distance = 999.9;
        }
        mPacket += String.format(Locale.getDefault(),"%05.1f", distance);
        mPacket += ",";

        /*
         * Put bearing
         */
        mPacket += String.format(Locale.getDefault(),"%05.1f", bearing);
        mPacket += ",";

        /*
         * Put speed
         */
        mPacket += String.format(Locale.getDefault(),"%05.1f,", speed);

        /*
         * Final item is whether or not we have arrived at our final destination
         */
        mPacket += planComplete ? "A" : "V";
      
        assemble();
    }
}
