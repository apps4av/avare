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

    public BasicOnScaleGestureListener(ViewParams mViewParams, View view) {
        this.mViewParams = mViewParams;
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
}
