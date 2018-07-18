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

import com.ds.avare.utils.Logger;

/**
 * 
 * @author zkhan
 *
 */
public class RTMMessage extends Message {

    public float mLat;
    public float mLon;
    
    public int   mSpeed;
    public int   mAltitude;
    public int     mDirection;
    public int     mIcaoAddress;

    
    public RTMMessage() {
        super(MessageType.Traffic);
    }

    /**
     * 
     * @param msg
     */
    public void parse(String msg) {
        String tokens[] = msg.split(",");
        
        if(tokens.length < 10) {
            return;
        }
        /* 1 = time
         * 2 = icao address
         * 3,4 = lat
         * 5,6 = lon
         * 7 = altitude
         * 8 = bearing
         * 9 = speed
         */
        double tmp;
        double tmp1;
        
        try {
            mIcaoAddress = Integer.parseInt(tokens[2]);           
        }
        catch (Exception e) {
            
        }
        
        try {
            tmp = Double.parseDouble(tokens[3]);
            tmp1 = (double)((int)tmp / (int)100);
            mLat = (float)(tmp - (tmp1 * 100.0)) / 60.f + (float)tmp1;
            if(tokens[4].equals("S")) {
                mLat = -mLat;
            }
            tmp = Double.parseDouble(tokens[5]);
            tmp1 = (double)((int)tmp / (int)100);
            mLon = (float)(tmp - (tmp1 * 100.0)) / 60.f + (float)tmp1;
            if(tokens[6].equals("W")) {
                mLon = -mLon;
            }

            mAltitude = (int)Double.parseDouble(tokens[7]);
            mDirection = (int)Double.parseDouble(tokens[8]);
            
            /*
             * Kt to m/s
             */
            mSpeed = (int)(Double.parseDouble(tokens[9]) * 0.514444);
            
            Logger.Logit("Traffic icao addr " + mIcaoAddress + " lat " + mLat + " lon " + mLon + " speed " + mSpeed +
                    " direction " + mDirection);

        }
        catch (Exception e) {
            
        }
    }
}
