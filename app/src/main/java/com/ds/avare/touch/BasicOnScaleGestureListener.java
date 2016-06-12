/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.touch;

import android.view.ScaleGestureDetector;
import android.view.View;

import com.ds.avare.utils.ViewParams;

// Class to handle multi-touch scale gestures for simple views, such as
// AFD and Plate views.
public class BasicOnScaleGestureListener
        extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    /**
     * This is the active focal point in terms of the viewport. Could be a local
     * variable but kept here to minimize per-frame allocations.
     */
    protected float mLastFocusX;
    protected float mLastFocusY;

    // Passed in from the caller to get/set attributes
    protected ViewParams mViewParams;
    protected View mView;

    public BasicOnScaleGestureListener(ViewParams viewParams, View view) {
        this.mViewParams = viewParams;
        this.mView = view;
    }

    // Detects that new pointers are going down.
    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        mViewParams.setScaling(true);

        mLastFocusX = scaleGestureDetector.getFocusX();
        mLastFocusY = scaleGestureDetector.getFocusY();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mViewParams.setScaling(false);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        mViewParams.setScaleFactor(mViewParams.getScaleFactor() * scaleFactor);
        mViewParams.setScaleFactor(Math.max(mViewParams.getMinScale(), Math.min(mViewParams.getScaleFactor(), mViewParams.getMaxScale())));
        mViewParams.getScale().setScaleFactor(mViewParams.getScaleFactor());

        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();

        float moveX = mViewParams.getPan().getMoveX() + ((focusX - mLastFocusX) / mViewParams.getScaleFactor());
        float moveY = mViewParams.getPan().getMoveY() + ((focusY - mLastFocusY) / mViewParams.getScaleFactor());
        mLastFocusX = focusX;
        mLastFocusY = focusY;

        mViewParams.getPan().setMove(moveX, moveY);
        mView.invalidate();

        return true;
    }

    public static enum TouchMode {
        DRAW_MODE, PAN_MODE
    }
}
