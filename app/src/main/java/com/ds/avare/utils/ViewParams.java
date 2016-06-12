package com.ds.avare.utils;

import android.graphics.Paint;
import android.view.GestureDetector;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;

/**
 * Created by roleary on 5/31/2016.
 */
public class ViewParams {
    private float mScaleFactor = 1.f;
    private Scale mScale;
    private Pan mPan;
    private float mMaxScale = 8;
    private float mMinScale = 0.03125f;
    private boolean mScaling;

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float mScaleFactor) {
        this.mScaleFactor = mScaleFactor;
    }

    public Scale getScale() {
        return mScale;
    }

    public void setScale(Scale mScale) {
        this.mScale = mScale;
    }

    public Pan getPan() {
        return mPan;
    }

    public void setPan(Pan mPan) {
        this.mPan = mPan;
    }

    public float getMaxScale() {
        return mMaxScale;
    }

    public void setMaxScale(float mMaxScale) {
        this.mMaxScale = mMaxScale;
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float mMinScale) {
        this.mMinScale = mMinScale;
    }

    public boolean isScaling() {
        return mScaling;
    }

    public void setScaling(boolean mScaling) {
        this.mScaling = mScaling;
    }

}
