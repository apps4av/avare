/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.gdl90;

/**
 * 
 * @author zkhan
 *
 */
public class OwnshipMessage extends Message {

    float mLat;
    float mLon;
    
    
    boolean mIsAirborne;
    boolean mIsExtrapolated;
    int      mTrackType;
    
    int mNIC;
    int mNACP;
    int mHorizontalVelocity;
    int mVerticalVelocity;
    
    int mAltitude;
    
    boolean mIsTrackHeadingValid;
    boolean mIsTrackHeadingTrueTrackAngle;
    boolean mIsTrackHeadingHeading;
    boolean mIsTrackHeadingTrueHeading;
    float   mDirection;

    
    public OwnshipMessage() {
        super(MessageType.OWNSHIP);
    }

    /**
     * 
     * @param msg
     */
    public void parse(byte msg[]) {

        /*
         * Lon/lat
         */
        mLat = this.calculateDegrees((int)(msg[4] & 0xFF), (int)(msg[5] & 0xFF), (int)(msg[6] & 0xFF));
        mLon = this.calculateDegrees((int)(msg[7] & 0xFF), (int)(msg[8] & 0xFF), (int)(msg[9] & 0xFF));

        /*
         * Altitude
         * XXX: Correct for -ve value;
         */
        int upper = ((int)(msg[10] & 0xFF)) << 4;
        int lower = ((int)(msg[11] & 0xF0)) >> 4;
        int alt = upper + lower;
        alt *= 25;
        alt -= 1000;
        mAltitude = alt;
        
        /*
         * Misc.
         */
        mIsAirborne = (msg[11] & 0x08) != 0;
        mIsExtrapolated = (msg[11] & 0x04) != 0;
        mTrackType = msg[11] & 0x03;
        
        /*
         * Quality
         */
        mNIC = ((msg[12] & 0xF0) >> 4) & 0x0F;
        mNACP = msg[12] & 0x0F;

        /*
         * Velocity
         */
        upper = ((int)(msg[13] & 0xFF)) << 4;
        lower = ((int)(msg[14] & 0xF0)) >> 4;
        mHorizontalVelocity = upper + lower;

        /*
         * VS
         * XXX: Correct for -ve value
         */
        mVerticalVelocity = (((int)msg[14] & 0x0F) << 4) + (int)(msg[15] & 0xFF);
        mVerticalVelocity *= 64;
        
        /*
         * Track/heading
         */
        mIsTrackHeadingValid = (mTrackType & 0xF) != 0;
        mIsTrackHeadingTrueTrackAngle = ((mTrackType & 0x1) & (mTrackType ^ 0x02)) != 0;
        mIsTrackHeadingHeading = (mTrackType & 0x2) != 0;
        mIsTrackHeadingTrueHeading = (mTrackType & 0x3) != 0;
        mDirection = ((int)msg[16] & 0xFF) * (float)Constants.HEADING_RESOLUTION;

        Logger.Logit("lat " + mLat + " lon " + mLon + " horzVel " + mHorizontalVelocity + " mVerticalVelocity" + mVerticalVelocity
                + " mAltitude "  + mAltitude + " direction " + mDirection + " trueheading " + mIsTrackHeadingTrueHeading);
    }

    /**
     * 
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
            xx = (float)(yy);
        }
        else {
            xx = (position & 0x7FFFFF);
        }    

        xx *= Constants.LON_LAT_RESOLUTION;
        
        return xx;
    }


}
