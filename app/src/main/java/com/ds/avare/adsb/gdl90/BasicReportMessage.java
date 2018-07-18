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
public class BasicReportMessage extends Message {

    public int mHour;
    public int mMin;
    public int mSec;
    public boolean mAdsbTarget;
    public boolean mTisbTarget;
    public boolean mSurfaceVehicle;
    public boolean mAdsbBeacon;
    public boolean mAdsrTarget;
    public boolean mIcaoAddressIsSelfAssigned;
    public int mAddressQualifier;
    public int mIcaoAddress;
    public double mLon;
    public double mLat;
    public int mAltitude;
    public int mNortherlyVelocity;
    public int mEasterlyVelocity;
    public int mVertVelocity;
    public double mHeading;
    public int mNic;
    public int mSpeed;
    public boolean mTrueTrackAngle;
    public boolean mMagneticHeading;
    public boolean mTrueHeading;
    public String mCallSign;


    public BasicReportMessage() {
        super(MessageType.BASIC_REPORT);
    }

    @Override
    protected void parse(byte[] msg) {

        mCallSign = "";

        long timeOfReception = 0;
        timeOfReception = ((long) (msg[2]) & 0xFF) << 16;
        timeOfReception += ((long) (msg[1]) & 0xFF) << 8;
        timeOfReception += ((long) (msg[0]) & 0xFF);

        double hours = (double) timeOfReception * 0.00008 / 3600;
        double minutes = (hours - Math.floor(hours)) * 60;
        double seconds = (minutes - Math.floor(minutes)) * 60;

        mHour = (int) hours;
        mMin = (int) minutes;
        mSec = (int) seconds;

        int payloadTypeCode = 0;
        payloadTypeCode = ((int) msg[3] & 0xF8) >> 3;

        switch (payloadTypeCode) {
            case 0:
                mAdsbTarget = true;
                mTisbTarget = false;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = false;
                break;

            case 1:
                mAdsbTarget = true;
                mTisbTarget = false;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = true;
                break;

            case 2:
                mAdsbTarget = false;
                mTisbTarget = true;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = true;
                mIcaoAddressIsSelfAssigned = false;
                break;

            case 3:
                mAdsbTarget = false;
                mTisbTarget = true;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = false;
                break;

            case 4:
                mAdsbTarget = false;
                mTisbTarget = false;
                mSurfaceVehicle = true;
                mAdsbBeacon = false;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = false;
                break;

            case 5:
                mAdsbTarget = false;
                mTisbTarget = false;
                mSurfaceVehicle = false;
                mAdsbBeacon = true;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = false;
                break;

            case 6:
                mAdsbTarget = false;
                mTisbTarget = false;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = true;
                mIcaoAddressIsSelfAssigned = true;
                break;

            case 7:
                mAdsbTarget = false;
                mTisbTarget = false;
                mSurfaceVehicle = false;
                mAdsbBeacon = false;
                mAdsrTarget = false;
                mIcaoAddressIsSelfAssigned = false;
                break;

            default:
                break;
        }

        mAddressQualifier = (int) msg[3] & 0x07;

        mIcaoAddress = ((int) msg[4] & 0xFF) << 16;
        mIcaoAddress += ((int) msg[5] & 0xFF) << 8;
        mIcaoAddress += ((int) msg[6] & 0xFF);


        // bytes [7-9]: 23 bits of latitude info (does not include bit 8 of byte 9)
        int tmp = 0;
        tmp = 0;
        tmp = (int) msg[7] & 0xFF;
        tmp <<= 8;
        tmp += (int) msg[8] & 0xFF;
        tmp <<= 8;
        tmp += (int) msg[9] & 0xFE;
        tmp >>= 1;

        boolean isSouth = (tmp & 0x800000) > 0;
        mLat = ((double) tmp) * Constants.LON_LAT_RESOLUTION;
        if (isSouth) {
            mLat *= -1;
        }

        // bytes [9-12]: 24 bits of longitude info (starts with bit 8 of byte 9)
        tmp = 0;
        tmp = (int) msg[10] & 0xFF;
        tmp <<= 8;
        tmp |= (int) msg[11] & 0xFF;
        tmp <<= 8;
        tmp |= (int) msg[12] & 0xFE;
        tmp >>= 1;

        if (((int) msg[9] & 1) == 1) {
            tmp += 0x800000;
        }

        boolean isWest = (tmp & 0x800000) > 0;
        mLon = ((double) (tmp & 0x7fffff)) * Constants.LON_LAT_RESOLUTION;
        if (isWest) {
            mLon = -1 * (180.0 - mLon);
        }

        // byte [12], bit 8: altitude type 
        //boolean altitudeType = ((int)msg[12] & 1) > 0;

        int codedAltitude = 0;
        codedAltitude = ((int) msg[13] & 0xFF) << 4;
        codedAltitude += ((int) msg[14] & 0xF0) >> 4;
        mAltitude = (codedAltitude * 25) - 1025;

        mNic = (int) msg[14] & 0x04;

        boolean airborne = !((msg[15] & 0x80) > 0);
        boolean supersonic = (msg[15] & 0x40) > 0;

        boolean horizVelocityIsSoutherly;
        boolean horizVelocityIsWesterly;
        int vel = 0;
        int tracking = 0;
        int northVelocityMagnitude = 0;
        int eastVelocityMagnitude = 0;
        int multiplier = 1;
        int tahType = 0;

        if (airborne) {   // data is horizontal velocity
            if (supersonic) {
                multiplier = 4;
            }

            vel = ((int) msg[15] & 0x0f) << 6;
            vel += ((int) msg[16] & 0xfd) >> 2;
            horizVelocityIsSoutherly = ((int) msg[15] & 0x10) > 0;
            northVelocityMagnitude = (vel * multiplier) - multiplier;

            if (horizVelocityIsSoutherly) {
                northVelocityMagnitude *= -1;
            }
            mNortherlyVelocity = northVelocityMagnitude;

            vel = 0;
            if (((int) msg[16] & 1) > 0) {
                vel = 0x200;
            }
            vel |= ((int) msg[17] & 0xFF) << 1;
            if (((int) msg[18] & 0x80) > 0) {
                vel |= 0x01;
            }
            horizVelocityIsWesterly = ((int) msg[16] & 0x02) > 0;

            eastVelocityMagnitude = (vel * multiplier) - multiplier;

            if (horizVelocityIsWesterly) {
                eastVelocityMagnitude *= -1;
            }
            mEasterlyVelocity = eastVelocityMagnitude;

            mSpeed = 0;
            mTrueTrackAngle = false;
            mMagneticHeading = false;
            mTrueHeading = false;
            mHeading = (360 + Math.toDegrees(-Math.atan2(-eastVelocityMagnitude, northVelocityMagnitude))) % 360;

            // vertical velocity
            //boolean vertVelocitySourceisBarometric = ((int)msg[18] & 0x40) > 0;
            //boolean vertVelocityIsDownward = ((int)msg[18] & 0x20) > 0;
            int verticalRate = 0;

            verticalRate = ((int) msg[18] & 0x1f) << 4;
            verticalRate += ((int) msg[19] & 0xf0) >> 4;
            mVertVelocity = (verticalRate * 64) - 64;

        } else {    // object is on the ground
            vel = ((int) msg[15] & 0x0f) << 6;
            vel += ((int) msg[16] & 0xfd) >> 2;
            mSpeed = (vel * multiplier) - multiplier;

            tracking = ((int) msg[17] & 0xFF) << 1;
            if (((int) msg[18] & 0x80) > 0) {
                tracking += 1;
            }

            mHeading = tracking * 0.703125;

            tahType = (int) msg[16] & 0x02;

            switch (tahType) {
                case 1:
                    mTrueTrackAngle = true;
                    mMagneticHeading = false;
                    mTrueHeading = false;
                    break;

                case 2:
                    mTrueTrackAngle = false;
                    mMagneticHeading = true;
                    mTrueHeading = false;
                    break;

                case 3:
                    mTrueTrackAngle = false;
                    mMagneticHeading = false;
                    mTrueHeading = true;
                    break;

                default:
                    mTrueTrackAngle = false;
                    mMagneticHeading = false;
                    mTrueHeading = false;
                    break;
            }
            mNortherlyVelocity = 0;
            mEasterlyVelocity = 0;

            mVertVelocity = 0;
        }

        if (payloadTypeCode == 0) {
        } else if ((payloadTypeCode == 1) || (payloadTypeCode == 2) || (payloadTypeCode == 3)) {
        }

        Logger.Logit("Basic traffic report " + " icao addr " + mIcaoAddress +
                " lat/lon " + mLat + "/" + mLon + " mAltitude " + mAltitude +
                " heading " + mHeading);

    }

}
