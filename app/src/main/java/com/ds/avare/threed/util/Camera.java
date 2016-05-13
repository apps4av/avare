/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.threed.util;

import android.opengl.Matrix;

import com.ds.avare.threed.data.Vector3d;

/**
 * Created by zkhan on 5/9/16.
 */
public class Camera {
    private Vector3d mCameraPos;
    private Vector3d mCameraLook;
    private Vector3d mCameraUp;

    /**
     * Camera always up +ve z axis
     */
    public Camera() {
        mCameraPos = new Vector3d(0, 0, 0);
        mCameraLook = new Vector3d(0, 0, 0);
        mCameraUp = new Vector3d(0, 0, 0);
    }

    /**
     * Set camera position and target
     * @param x
     * @param y
     * @param z
     * @param x0
     * @param y0
     * @param z0
     */
    public void set(Vector3d pos, Vector3d look) {

        Vector3d up = new Vector3d(look.getX(), look.getY(), 1000f);
        mCameraPos.set(pos);
        mCameraLook.set(look);
        mCameraUp.set(up); // do not rotate up vector, assume no bank
    }

    /**
     * Get view matrix from camera position and target
     * @param viewMatrix
     */
    public void setViewMatrix(float[] viewMatrix) {
        Matrix.setLookAtM(viewMatrix, 0,
                mCameraPos.getX(), mCameraPos.getY(), mCameraPos.getZ(),
                mCameraLook.getX(), mCameraLook.getY(), mCameraLook.getZ(),
                mCameraUp.getX(), mCameraUp.getY(), mCameraUp.getZ());

    }

}
