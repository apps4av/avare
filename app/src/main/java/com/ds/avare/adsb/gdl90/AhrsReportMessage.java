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

/**
 * @author zkhan
 */
public class AhrsReportMessage extends Message {


    // all static because this info comes from various messages and it is important to keep old values for untouched variables
    static public float mYaw;
    static public float mYawRate;
    static public float mPitch;
    static public float mRoll;
    static public float mSlip;
    static public float mAccl;
    static public float mAoa;
    static public float mAirspeed;
    static public float mAltitude;
    static public float mVsi;
    public boolean mValid = false;

    public AhrsReportMessage() {
        super(MessageType.AHRS_REPORT);
    }


    private float combineBytesForFloat(byte hi, byte lo) {

        int ih = ((int)hi) & 0xFF;
        int il = ((int)lo) & 0xFF;
        int sum = (ih << 8) + il;

        if(sum > 32767) {
            // negative number
            return (-(float)(65536 - sum));
        }
        else {
            return ((float)sum);
        }

    }

    private float combineBytesForFloatUnsigned(byte hi, byte lo) {

        int ih = ((int)hi) & 0xFF;
        int il = ((int)lo) & 0xFF;
        int sum = (ih << 8) + il;

        return ((float)sum);


    }

    /**
     * @param msg
     */
    public void parse(byte msg[]) {

        mValid = false;
        if (0x45 == msg[0]) {// iLevil

            if(0x2 == msg[1]) { // Aoa
                // 1.0 is critical aoa
                // 0.68 is start of red range
                // 0.0 is neutral Aoa
                int id = 3;
                byte m0  = msg[id++];
                mValid = true;
                if(m0 == 0xFF) {
                    mAoa = -1;
                }
                else {
                    mAoa = ((float)m0) / 100.0f;
                }
            }
            else if(0x1 == msg[1]) { // AHRS

                byte m0 = 0, m1 = 0, m2 = 0, m3 = 0, m4 = 0, m5 = 0, m6 = 0, m7 = 0, m8 = 0, m9 = 0, m10 = 0, m11 = 0, m12 = 0, m13 = 0, m14 = 0, m15 = 0, m16 = 0, m17 = 0;
                try {
                    int id = 3;
                    m0 = msg[id++];
                    m1 = msg[id++];
                    m2 = msg[id++];
                    m3 = msg[id++];
                    m4 = msg[id++];
                    m5 = msg[id++];
                    m6 = msg[id++];
                    m7 = msg[id++];
                    m8 = msg[id++];
                    m9 = msg[id++];
                    m10 = msg[id++];
                    m11 = msg[id++];
                    m12 = msg[id++];
                    m13 = msg[id++];
                    m14 = msg[id++];
                    m15 = msg[id++];
                    m16 = msg[id++];
                    m17 = msg[id++];
                }
                catch (Exception e){
                    // to accomodate various ADSB devices as this is not well defined array.
                }

                float num;
                num = combineBytesForFloat(m0, m1);
                if(num != 0x7FFF) {
                    mRoll = num / 10f;
                }
                num = combineBytesForFloat(m2, m3);
                if(num != 0x7FFF) {
                    mPitch = num / 10f;
                }
                num = combineBytesForFloat(m4, m5);
                if(num != 0x7FFF) {
                    mYaw = num / 10f;
                }
                num = combineBytesForFloat(m6, m7);
                if(num != 0x7FFF) {
                    mSlip = num / 10f;
                }
                num = combineBytesForFloat(m8, m9);
                if(num != 0x7FFF) {
                    mYawRate = num / 10f; // degrees per second
                }
                num = combineBytesForFloat(m10, m11);
                if(num != 0x7FFF) {
                    mAccl = num / 10f;
                }
                num = combineBytesForFloat(m12, m13);
                if(num != 0x7FFF) {
                    mAirspeed = num / 10f;
                }
                num = combineBytesForFloatUnsigned(m14, m15);
                if(num != 0xFFFF) {
                    mAltitude = num - 5000; // 5000 is SL
                }
                num = combineBytesForFloat(m16, m17);
                if(num != 0x7FFF) {
                    mVsi = num;
                }

                mValid    = true;
            }
        }
    }

}
