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
public class HeartbeatMessage extends Message {

    int mHour;
    int mMinute;
    int mSecond;

    Boolean mGpsPositionValid;
    Boolean mBatteryLow;
    Boolean mDeviceRunning;

    public HeartbeatMessage() {
        super(MessageType.HEARTBEAT);
    }

    /**
     * @param msg
     */
    public void parse(byte msg[]) {
        /*
         * Some useful fields
         */
        int d = msg[0] & 0xFF;
        mGpsPositionValid = (d & 0x80L) != 0;
        mBatteryLow = (d & 0x40L) != 0;
        mDeviceRunning = (d & 0x01L) != 0;

        /*
         * Get time
         */
        int d1 = msg[1] & 0xFF;
        int d2 = msg[2] & 0xFF;
        int d3 = msg[3] & 0xFF;

        long timeStamp = ((d1 & 0x80L) << 9) | (d3 << 8) | d2;
        double mHourFrac = (float) timeStamp / 3600.0f;
        mHour = (int) Math.floor((double) mHourFrac);
        double mMinuteFrac = (double) mHourFrac - (double) mHour;
        mMinute = (int) Math.floor((double) mMinuteFrac * 60.0f);
        double mSecondsFrac = (mMinuteFrac * 60) - mMinute;
        mSecond = (int) Math.round(mSecondsFrac * 60.0f);
        if (mSecond == 60) {
            mSecond = 0;
            mMinute++;
        }
        if (mMinute == 60) {
            mMinute = 0;
            mHour++;
        }
        Logger.Logit(" mHour " + mHour + " mMinute " + mMinute + " mSecond " + mSecond + " isBatteryLow " + mBatteryLow);
    }

}
