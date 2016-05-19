/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.threed.data;

/**
 * Created by zkhan on 5/9/16.
 */
public class Vector3d {
    protected float mX;
    protected float mY;
    protected float mZ;
    private float[] mV;
    private float[] mS;

    public Vector3d(float x, float y, float z) {
        mX = x;
        mY = y;
        mZ = z;
        mV = new float[4];
        mS = new float[4];
        mV[0] = x;
        mV[1] = y;
        mV[2] = z;
        mV[3] = 1.0f;
    }

    public void set(Vector3d in) {
        mX = in.getX();
        mY = in.getY();
        mZ = in.getZ();
    }

    public float getX() {
        return mX;
    }
    public float getY() {
        return mY;
    }
    public float getZ() {
        return mZ;
    }
    public float[] getVectorArray() {
        return mV;
    }
    // Mostly used for rotation
    public float[] getVectorArrayScratch() {
        return mS;
    }


}
