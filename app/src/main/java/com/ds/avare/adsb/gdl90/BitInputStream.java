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
 * A class that reads bits from a data stream.
 *
 * @author zkhan
 */
public class BitInputStream {

    private byte mBuffer[];
    private int mLocation;

    private int mIBuffer;


    private int mBitsLeft;

    /**
     * @param buffer
     */
    public BitInputStream(byte buffer[]) {
        mBuffer = buffer;
        mLocation = 0;
        mBitsLeft = 8;
        mIBuffer = ((int) buffer[0]) & 0xFF;
    }

    /**
     * @param aNumberOfBits
     * @return
     */
    public int getBits(final int aNumberOfBits) {
        int value = 0;
        int num = aNumberOfBits;
        while (num-- > 0) {
            value <<= 1;
            value |= readBit();
        }
        return value;
    }

    /**
     * @return
     */
    public int readBit() {
        if (mBitsLeft == 0) {
            mIBuffer = ((int) mBuffer[++mLocation]) & 0xFF;
            mBitsLeft = 8;
        }

        mBitsLeft--;
        int bit = (mIBuffer >> mBitsLeft) & 0x1;

        bit = (bit == 0) ? 0 : 1;

        return bit;
    }

    /**
     * @return
     */
    public int totalRead() {
        return mLocation + 1;
    }
}
