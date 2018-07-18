/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adsb.gdl90;

import com.ds.avare.utils.Logger;

/**
 * @author zkhan
 */
public class UplinkMessage extends Message {

    private FisBuffer mFis;

    public UplinkMessage() {
        super(MessageType.UPLINK);
    }

    /**
     * @param msg
     */
    public void parse(byte msg[]) {

        /*
         * First 3 bytes are Zulu time,
         * Next 8 is UAT header
         * Rest of 424 is payload
         *
         */
        int skip = 3;
        int lat = 0;
        lat += ((int) msg[skip + 0]) & 0xFF;
        lat <<= 8;
        lat += ((int) msg[skip + 1]) & 0xFF;
        lat <<= 8;
        lat += ((int) msg[skip + 2]) & 0xFE;
        lat >>= 1;

        boolean isSouth = (lat & 0x800000) != 0;
        float degLat = (float) lat * (float) Constants.LON_LAT_RESOLUTION;
        if (isSouth) {
            degLat *= -1;
        }

        int lon = 0;
        lon += ((int) msg[skip + 3]) & 0xFF;
        lon <<= 8;
        lon += ((int) msg[skip + 4]) & 0xFF;
        lon <<= 8;
        lon += ((int) msg[skip + 5]) & 0xFE;
        lon >>= 1;
        if ((msg[skip + 2] & 0x01) != 0) {
            lon += 0x800000;
        }

        boolean isWest = (lon & 0x800000) != 0;
        float degLon = (lon & 0x7fffff) * (float) Constants.LON_LAT_RESOLUTION;
        if (isWest) {
            degLon = -1.f * (180.f - degLon);
        }

        boolean positionValid = (msg[skip + 5] & 0x01) != 0;

        boolean applicationDataValid = (msg[skip + 6] & 0x20) != 0;
        if (false == applicationDataValid) {
            return;
        }

        // byte 6, bits 4-8: slot ID
        int slotID = msg[skip + 6] & 0x1f;

        // byte 7, bit 1-4: TIS-B site ID. If zero, the broadcasting station is not broadcasting TIS-B data
        int tisbSiteID = (msg[skip + 7] & 0xf0) >> 4;

        // byte 9-432: application data (multiple iFrames).
        skip = 3 + 8;
        mFis = new FisBuffer(msg, skip, slotID, tisbSiteID, positionValid, degLat, degLon);

        /*
         * Now decode all.
         */
        mFis.makeProducts();

        Logger.Logit("Uplink message");
    }

    /**
     * @return
     */
    public FisBuffer getFis() {
        return mFis;
    }
}
