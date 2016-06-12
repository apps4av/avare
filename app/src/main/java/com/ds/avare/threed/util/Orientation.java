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

import com.ds.avare.views.ThreeDSurfaceView;

/**
 * Created by zkhan on 5/19/16.
 */
public class Orientation {

    ThreeDSurfaceView mView;

    private static final float MAX_VIEW_ANGLE = 90;

    private float mAngle;
    private float mViewAngle;
    private float mDisplacementX;
    private float mDisplacementY;

    /**
     * Set orientation
     * @param view
     */
    public void set(ThreeDSurfaceView view) {

        // pinch zoom changes view angle, move left right changes x, and move up down changes y, and rotate changes angle
        mView = view;
        mAngle = view.getAngle(); //rotates map in satellite camera
        mDisplacementX = view.getDisplacementX(); // rotation for fp
        mDisplacementY = view.getDisplacementY(); // titls up and down for fp, pans map in satellite
        mViewAngle = MAX_VIEW_ANGLE / view.getScale(); // changes view angle
    }

    public Orientation() {
        mAngle = 0;
        mViewAngle = MAX_VIEW_ANGLE;
        mDisplacementX = 0;
        mDisplacementY = 0;
    }

    public float getViewAngle() {
        return mViewAngle;
    }

    public float getDisplacementY(boolean fp) {
        if(fp) {
            return 0;
        }
        return mDisplacementY;
    }
    public float getDisplacementX(boolean fp) {
        if(fp) {
            return 0;
        }
        return mDisplacementX;
    }

    public float getMapRotation(boolean fp) {
        if(fp) {
            return 0;
        }
        return mAngle;
    }

    public float getRotationZ(boolean fp) {
        if(!fp) {
            return 0;
        }
        float degrees = (mDisplacementX * 15);

        return degrees;
    }

    public float getRotationX(boolean fp) {
        if(!fp) {
            return 0;
        }
        // convert displacement y to angle
        float degrees = (-mDisplacementY * 15);

        return degrees;
    }

}
