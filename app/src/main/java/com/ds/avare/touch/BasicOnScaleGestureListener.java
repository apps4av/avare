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
    protected float lastFocusX;
    protected float lastFocusY;

    // Passed in from the caller to get/set attributes
    protected ViewParams viewParams;
    protected View view;

    public BasicOnScaleGestureListener(ViewParams viewParams, View view) {
        this.viewParams = viewParams;
        this.view = view;
    }

    // Detects that new pointers are going down.
    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        viewParams.mScaling = true;

        lastFocusX = scaleGestureDetector.getFocusX();
        lastFocusY = scaleGestureDetector.getFocusY();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        viewParams.mScaling = false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        float scaleFactor = detector.getScaleFactor();
        viewParams.mScaleFactor *= scaleFactor;
        viewParams.mScaleFactor = Math.max(ViewParams.MIN_SCALE, Math.min(viewParams.mScaleFactor, ViewParams.MAX_SCALE));
        viewParams.mScale.setScaleFactor(viewParams.mScaleFactor);

        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();

        float moveX = viewParams.mPan.getMoveX() + ((focusX - lastFocusX) / viewParams.mScaleFactor);
        float moveY = viewParams.mPan.getMoveY() + ((focusY - lastFocusY) / viewParams.mScaleFactor);
        lastFocusX = focusX;
        lastFocusY = focusY;

        viewParams.mPan.setMove(moveX, moveY);

        view.invalidate();

        return true;
    }
}
