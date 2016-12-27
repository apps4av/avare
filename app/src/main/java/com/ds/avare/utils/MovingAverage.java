/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

/**
 * Created by zkhan on 12/27/16.
 */

public class MovingAverage {

    private double mNumbers[];
    private int mSize;

    public MovingAverage(int size) {
        mSize = size;
        if(mSize <= 0) {
            return;
        }
        mNumbers = new double[mSize];
        for(int count = 0; count < mSize; count++) {
            mNumbers[count] = 0;
        }
    }


    public void add(double num) {
        //move
        if(mSize <= 0) {
            return;
        }
        for(int count = (mSize - 1); count > 0; count--) {
            mNumbers[count] = mNumbers[count - 1];
        }
        // put
        mNumbers[0] = num;
    }


    public double get() {
        if(mSize <= 0) {
            return 0;
        }
        double sum = 0;
        for(int count = 0; count < mSize; count++) {
            sum += mNumbers[count];
        }
        return sum / mSize;
    }

}
