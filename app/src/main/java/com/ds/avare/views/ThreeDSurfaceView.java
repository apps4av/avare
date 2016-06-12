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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.touch.BasicOnScaleGestureListener;
import com.ds.avare.utils.ViewParams;

/**
 * Created by zkhan on 5/16/16.
 */
public class ThreeDSurfaceView extends GLSurfaceView {

    private float mAngle;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private ViewParams mViewParams;
    private double mStartRadians;
    private float mStartAngle;

    private static final float MAX_SCALE = 12f;
    private static final float MIN_SCALE = 1.0f; // change this range in relationship to MAX_VIEW_ANGLE

    public ThreeDSurfaceView(Context context) {
        this(context, null);
    }

    public ThreeDSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        mScaleDetector.onTouchEvent(e);

        // Do rotation if this is multi-touch
        if( e.getPointerCount() == 2) {
            float deltaX = e.getX(0) - e.getX(1);
            float deltaY = e.getY(0) - e.getY(1);

            double radians = Math.atan2(deltaY, deltaX);

            // This is the first touch - save the starting delta
            if( !mViewParams.isScaling() ) {
                mStartRadians = radians;
                mStartAngle = mAngle;
            }

            mAngle = mStartAngle + (float)(radians - mStartRadians);
        }

        requestRender();
        return true;
    }

    public float getAngle() {
        return -(float)Math.toDegrees(mAngle);
    }

    public float getDisplacementY() {
        return -mViewParams.getPan().getMoveY() * MAX_SCALE / getHeight() / mViewParams.getScaleFactor();
    }

    public float getDisplacementX() {
        return  mViewParams.getPan().getMoveX() * MAX_SCALE / getWidth() / mViewParams.getScaleFactor();
    }

    public float getScale() {
        return mViewParams.getScaleFactor();
    }

    public void init() {
        mAngle = 0;
        mViewParams = new ViewParams();
        mViewParams.setPan(new Pan());
        mViewParams.setScale(new Scale(MAX_SCALE));
        mViewParams.setMaxScale(MAX_SCALE);
        mViewParams.setMinScale(MIN_SCALE);

        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
        BasicOnScaleGestureListener gestureListener = new BasicOnScaleGestureListener(mViewParams, this);
        mScaleDetector = new ScaleGestureDetector(getContext(), gestureListener);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {

            if (mViewParams.isScaling()) {
                return false;
            }

            float moveX = mViewParams.getPan().getMoveX() - (distanceX) / mViewParams.getScale().getScaleFactor();
            float moveY = mViewParams.getPan().getMoveY() - (distanceY) / mViewParams.getScale().getScaleFactor();

            mViewParams.getPan().setMove(moveX, moveY);
            invalidate();
            return true;
        }
    }
}
