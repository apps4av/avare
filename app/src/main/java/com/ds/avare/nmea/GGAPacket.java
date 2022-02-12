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

    /*
     * $GPGGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh
     * 1    = UTC of Position
     * 2    = Latitude
     * 3    = N or S
     * 4    = Longitude
     * 5    = E or W
     * 6    = GPS quality indicator (0=invalid; 1=GPS fix; 2=Diff. GPS fix)
     * 7    = Number of satellites in use [not those in view]
     * 8    = Horizontal dilution of position
     * 9    = Antenna altitude above/below mean sea level (geoid)
     * 10   = M-Meters  (Antenna height unit)
     * 11   = Geoidal separation (Diff. between WGS-84 earth ellipsoid and
     *        mean sea level.  -=geoid is below WGS-84 ellipsoid)
     * 12   = M-Meters  (Units of geoidal separation)
     * 13   = Age in seconds since last update from diff. reference station (NOT USED)
     * 14   = Diff. reference station ID# (NOT USED)
     * 15   = Checksum
     */

    public static final String TAG = "$GPGGA";
    public static final String TAGN = "$GNGGA";
    public static final int HD     = 8;
    public static final int ALT    = 9;
    public static final int GEOID  = 11;

    public GGAPacket(long time, double latitude, double longitude, double altitude, int satCount, double geoid, double horDil) {
        mPacket = TAG + ",";

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

            mPacket += String.format(Locale.getDefault(), "%02d", lat);
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

            mPacket += String.format(Locale.getDefault(),"%03d", lon);
            mPacket += String.format(Locale.getDefault(),"%06.3f", deg);
            mPacket += ",W,";
        }

        /*
         * A true GPS fix
         */
        mPacket += "1,";
        
        /*
         * How many satellites used in this fix.
         */
        mPacket += String.format(Locale.getDefault(),"%02d,", satCount);

        /*
         * Horizontal dilution 
         */
        mPacket += String.format(Locale.getDefault(),"%1.1f,", horDil);
        
        /*
         * Put altitude in METERS
         */
        mPacket += String.format(Locale.getDefault(),"%.1f,M,", altitude);

        /*
         * GEOID and a couple of empty fields
         */
        mPacket += String.format(Locale.getDefault(),"%.1f,M,,", geoid);

        assemble();
    }
}
