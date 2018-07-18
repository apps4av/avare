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
public class TrafficReportMessage extends Message {

    public int mStatus;
    public int mAddressType;
    public int mIcaoAddress;
    public float mLat;
    public float mLon;
    public int mAltitude;
    public int mMiscInd;
    public int mNic;
    public int mNacp;
    public int mHorizVelocity;
    public int mVertVelocity;
    public float mHeading;
    public int mEmitCategory;
    public int emergencyPriorityCode;
    public String mCallSign;

    public TrafficReportMessage() {
        super(MessageType.TRAFFIC_REPORT);
    }

    /**
     * @param highByte
     * @param midByte
     * @param lowByte
     * @return
     */
    private float calculateDegrees(int highByte, int midByte, int lowByte) {
        int position = 0;

        float xx;

        position = highByte;
        position <<= 8;
        position |= midByte;
        position <<= 8;
        position |= lowByte;
        position &= 0xFFFFFFFF;

        if ((position & 0x800000) != 0) {
            int yy;

            position |= 0xFF000000;

            yy = (int) position;
            xx = (float) (yy);
        } else {
            xx = (position & 0x7FFFFF);
        }

        xx *= Constants.LON_LAT_RESOLUTION;

        return xx;
    }


    @Override
    protected void parse(byte[] msg) {

        /*
         * upper nibble of first byte is the traffic alert mStatus
         */
        mStatus = (msg[0] & 0xF0) >> 4;

        /*
         * lower nibble of first byte is the address type:
         * 0 = ADS-B with ICAO address
         * 1 = ADS-B with self-assigned address
         * 2 = TIS-B with ICAO address
         * 3 = TIS-B with track file ID
         * 4 = surface vehicle
         * 5 = ground station beacon
         * 6-15 = reserved
         */
        mAddressType = msg[0] & 0x0F;

        /*
         * next three bytes are the traffic's ICAO address
         */
        mIcaoAddress = 0;
        mIcaoAddress = ((((int) msg[1]) & 0xFF) << 16) + ((((int) (msg[2]) & 0xFF) << 8)) + ((((int) msg[3]) & 0xFF));

        /*
         * next 3 bytes are lat value with resolution = 180 / (2^23) degrees
         */
        mLat = this.calculateDegrees((int) (msg[4] & 0xFF), (int) (msg[5] & 0xFF), (int) (msg[6] & 0xFF));

        /*
         * next 3 bytes are lon value with resolution = 180 / (2^23) degrees
         */
        mLon = this.calculateDegrees((int) (msg[7] & 0xFF), (int) (msg[8] & 0xFF), (int) (msg[9] & 0xFF));

        /*
         * next 12 bits are altitude
         */
        int upper = (((int) msg[10]) & 0xFF) << 4;
        int lower = (((int) msg[11]) & 0xF0) >> 4;
        mAltitude = upper + lower;
        mAltitude *= 25;
        mAltitude -= 1000;

        /*
         * next nibble is miscellaneous indicators:
         * bit 3   bit 2    bit 1    bit 0
         *   x       x        0        0    = mHeading not valid
         *   x       x        0        1    = mHeading is true track angle
         *   x       x        1        0    = mHeading is magnetic heading
         *   x       x        1        1    = mHeading is true heading
         *   x       0        x        x    = report is updated
         *   x       1        x        x    = report is extrapolated
         *   0       x        x        x    = on ground
         *   1       x        x        x    = airborne
         */
        mMiscInd = ((int) msg[11] & 0x0F);

        /*
         * Next nibble is the navigation integrity category (nic). (See GDL-90 spec pg. 21.)
         */
        mNic = ((int) msg[12] & 0xF0) >> 4;

        /*
         * Next nibble is navigation accuracy category for position (nacP)
         */
        mNacp = (int) msg[12] & 0x0F;

        /*
         * next 12 bits are horizontal velocity in knots. (0xFFF = unknown)
         */
        upper = ((int) msg[13] & 0xFF) << 4;
        lower = ((int) msg[14] & 0xF0) >> 4;
        mHorizVelocity = upper | lower;

        /*
         * next 12 bits are vertical velocity in units of 64 fpm. (0xFFF = unknown)
         */
        upper = ((int) msg[14] & 0x0F) << 8;
        lower = (int) msg[15] & 0xFF;
        mVertVelocity = (upper | lower) * 64;

        /*
         * next nibble is the track heading with resolution = 360/256
         */
        mHeading = ((float) ((int) msg[16] & 0xFF)) * 360.f / 256.f;

        /*
         * next byte is emitter category. see GDL-90 spec page 23
         */
        mEmitCategory = msg[17] & 0xFF;

        /*
         * next 8 bytes are callsign
         */
        byte callsign[] = new byte[8];
        callsign[0] = msg[18];
        callsign[1] = msg[19];
        callsign[2] = msg[20];
        callsign[3] = msg[21];
        callsign[4] = msg[22];
        callsign[5] = msg[23];
        callsign[6] = msg[24];
        callsign[7] = msg[25];
        mCallSign = new String(callsign);

        /*
         * next 4 bits are emergency/priority code
         */
        emergencyPriorityCode = ((int) msg[26] & 0xF0) >> 4;

        Logger.Logit("Traffic report callsign " + mCallSign + " icao addr " + mIcaoAddress +
                " lat/lon " + mLat + "/" + mLon + " mAltitude " + mAltitude +
                " heading " + mHeading);
    }

}
