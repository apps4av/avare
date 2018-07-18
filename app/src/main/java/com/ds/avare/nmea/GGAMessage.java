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
public class GGAMessage extends Message {

    float mLat;
    float mLon;
    
    int     mAltitude;

    
    public GGAMessage() {
        super(MessageType.EssentialFix);
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
        /*
         * 3 = lat
         * 4 = lon
         * 5 = speed
         * 6 = bearing
         */
        double tmp;
        double tmp1;
        try {
            tmp = Double.parseDouble(tokens[2]);
            tmp1 = (double)((int)tmp / (int)100);
            mLat = (float)(tmp - (tmp1 * 100.0)) / 60.f + (float)tmp1;
            if(tokens[3].equals("S")) {
                mLat = -mLat;
            }
            tmp = Double.parseDouble(tokens[4]);
            tmp1 = (double)((int)tmp / (int)100);
            mLon = (float)(tmp - (tmp1 * 100.0)) / 60.f + (float)tmp1;
            if(tokens[5].equals("W")) {
                mLon = -mLon;
            }
            mAltitude = (int)Double.parseDouble(tokens[9]);
            
            Logger.Logit("lat " + mLat + " lon " + mLon + " mAltitude "  + mAltitude);
        }
        catch (Exception e) {
            
        }
    }
}
