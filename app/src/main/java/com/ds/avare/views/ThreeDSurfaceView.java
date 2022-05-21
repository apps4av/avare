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
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ds.avare.position.Scale;

/**
 * Created by zkhan on 5/16/16.
 */
public class ThreeDSurfaceView extends PanZoomView implements View.OnTouchListener {

    private float mAngle;

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

    public float getAngle() {
        return -(float)Math.toDegrees(mAngle);
    }

    public float getDisplacementY() {
        return -mPan.getMoveY() * MAX_SCALE / getHeight();
    }

    public float getDisplacementX() {
        return  mPan.getMoveX() * MAX_SCALE / getWidth();
    }

    public float getScale() {
        return mScale.getScaleFactor();
    }

    public void init() {
        mAngle = 0;
        mScale = new Scale(MAX_SCALE, MIN_SCALE);
        setOnTouchListener(this);
    }

    void setAngle(Point p0, Point p1) {
        mAngle = (float) Math.atan2(p1.y - p0.y, p1.x - p0.x);
    }

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        if(e.getPointerCount() != 2) { // not multi
            mAngle = 0;
        }
        else {
            Point p0 = getFirstPoint(e);
            Point p1 = getSecondPoint(e);
            setAngle(p0, p1);
        }
        return false;
    }
}
