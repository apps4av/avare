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
public class AhrsReportMessage extends Message {


    public float mYaw;
    public float mYawRate;
    public float mPitch;
    public float mRoll;
    public float mSlip;
    public float mAccl;
    public boolean mValid;

    public AhrsReportMessage() {
        super(MessageType.AHRS_REPORT);
    }


    private float combineBytesForFloat(byte hi, byte lo) {

        int ih = ((int)hi) & 0xFF;
        int il = ((int)lo) & 0xFF;
        int sum = (ih << 8) + il;

        if(sum > 32767) {
            // negative number
            return (-(float)(65536 - sum)) / 10f;
        }
        else {
            return ((float)sum) / 10f;
        }

    }

    /**
     * @param msg
     */
    public void parse(byte msg[]) {

        mValid = false;
        if (0x45 == msg[0]) {// iLevil

            if(0x1 == msg[1]) { // AHRS

                int id = 3;
                byte m0  = msg[id++];
                byte m1  = msg[id++];
                byte m2  = msg[id++];
                byte m3  = msg[id++];
                byte m4  = msg[id++];
                byte m5  = msg[id++];
                byte m6  = msg[id++];
                byte m7  = msg[id++];
                byte m8  = msg[id++];
                byte m9  = msg[id++];
                byte m10 = msg[id++];
                byte m11 = msg[id++];


                mRoll     = combineBytesForFloat(m0, m1);
                mPitch    = combineBytesForFloat(m2, m3);
                mYaw      = combineBytesForFloat(m4, m5);
                mSlip     = combineBytesForFloat(m6, m7);
                mYawRate  = combineBytesForFloat(m8, m9);
                mAccl     = combineBytesForFloat(m10, m11);
                mValid = true;

                // PFD takes in reverse pitch
                // Accl on iLevil is G-force, not inclinometer displacement
                mPitch = -mPitch;
                mAccl = 0;
            }
        }
    }

}
