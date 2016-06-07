/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.metalev.multitouch.controller.MultiTouchController;

/**
 * Created by zkhan on 5/16/16.
 */
public class ThreeDSurfaceView extends GLSurfaceView implements MultiTouchController.MultiTouchObjectCanvas<Object> {

    private float mX;
    private float mY;
    private float mScale;
    private float mAngle;
    private MultiTouchController<Object> mMultiTouchC;
    private MultiTouchController.PointInfo mCurrTouchPoint;

    private static final float MAX_SCALE = 12f;
    private static final float MIN_SCALE = 1.0f; // change this range in relationship to MAX_VIEW_ANGLE

    public ThreeDSurfaceView(Context context) {
        super(context);
        init();
    }

    public ThreeDSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return mMultiTouchC.onTouchEvent(e, MAX_SCALE, MIN_SCALE, 1);
    }

    public float getAngle() {
        return -(float)Math.toDegrees(mAngle);
    }

    public float getDisplacementY() {
        return -mY * MAX_SCALE / getHeight();
    }

    public float getDisplacementX() {
        return  mX * MAX_SCALE / getWidth();
    }

    public float getScale() {
        return mScale;
    }

    public void init() {
        mX = 0;
        mY = 0;
        mAngle = 0;
        mScale = 1.0f;
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new MultiTouchController.PointInfo();
    }

    @Override
    public Object getDraggableObjectAtPoint(MultiTouchController.PointInfo touchPoint) {
        return this;
    }

    @Override
    public void getPositionAndScale(Object obj, MultiTouchController.PositionAndScale objPosAndScaleOut) {
        objPosAndScaleOut.set(mX, mY, true, mScale, true, mScale, mScale, true, mAngle);
    }

    @Override
    public boolean setPositionAndScale(Object obj, MultiTouchController.PositionAndScale newObjPosAndScale, MultiTouchController.PointInfo touchPoint) {
        mCurrTouchPoint.set(touchPoint);
        if(false == mCurrTouchPoint.isMultiTouch()) {
            /*
             * Multi-touch is zoom, single touch is pan
             */
            mX = newObjPosAndScale.getXOff();
            mY = newObjPosAndScale.getYOff();
        }
        else {
            /*
             * Clamp scaling.
             */
            mScale = newObjPosAndScale.getScale();
            if(mScale > MAX_SCALE) {
                mScale = MAX_SCALE;
            }
            if(mScale < MIN_SCALE) {
                mScale = MIN_SCALE;
            }
            mAngle = newObjPosAndScale.getAngle();
        }

        return true;
    }

    @Override
    public void selectObject(Object obj, MultiTouchController.PointInfo touchPoint) {
        mCurrTouchPoint.set(touchPoint);
    }
}
