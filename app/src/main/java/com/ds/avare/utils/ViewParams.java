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
    public float mScaleFactor = 1.f;
    public Scale mScale;
    public Pan mPan;
    public static final float MAX_SCALE = 8;
    public static final float MIN_SCALE = 0.03125f;
    public boolean mScaling;
}
