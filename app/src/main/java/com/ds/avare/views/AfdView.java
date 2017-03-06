/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 */
public class AfdView extends View implements OnTouchListener {
	

    private Scale                        mScale;
    private Pan                          mPan;
	private Paint                        mPaint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private Preferences                  mPref;
    
    private static final float MAX_AFD_SCALE = 8;
    private float mScaleFactor          = 1.f;
    private boolean                     mScaling;

    private ScaleGestureDetector mScaleDetector;

    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPan = new Pan();
        mScale = new Scale(MAX_AFD_SCALE);
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mPref = new Preferences(context);
    }
    
    /**
     * 
     * @param context
     */
	public AfdView(Context context) {
        this(context, null, 0);
	}

    /**
     * 
     * @param context
     */
    public AfdView(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    /**
     * 
     * @param context
     */
    public AfdView(Context context, AttributeSet set, int arg) {
        super(context, set, arg);
        setup(context);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        boolean retVal = mGestureDetector.onTouchEvent(e);
        retVal = mScaleDetector.onTouchEvent(e) || retVal;
        return retVal;
    }

    /**
     * @param
     */
    public void setBitmap(BitmapHolder holder) {
        mBitmap = holder;
        postInvalidate();
    }

    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mPan = new Pan();

        invalidate();
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {
    	if(mBitmap == null) {
    		return;
    	}
    	if(mBitmap.getBitmap() == null) {
    		return;
    	}
    	
        float min = Math.min(getWidth(), getHeight()) - 8;
        mPaint.setTextSize(min / 20);
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
        
        float scale = mScale.getScaleFactorRaw();

    	/*
    	 * A/FD
    	 */
        mBitmap.getTransform().setScale(scale, scale);
        mBitmap.getTransform().postTranslate(
                mPan.getMoveX() * scale
                + getWidth() / 2
                - mBitmap.getWidth() / 2 * scale ,
                mPan.getMoveY() * scale
                + getHeight() / 2
                - mBitmap.getHeight() / 2 * scale);
        
        if(mPref.isNightMode()) {
            Helper.invertCanvasColors(mPaint);
        }
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
        Helper.restoreCanvasColors(mPaint);
    }

    // Class to handle multi-touch scale gestures
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        /**
         * This is the active focal point in terms of the viewport. Could be a local
         * variable but kept here to minimize per-frame allocations.
         */
        private float lastFocusX;
        private float lastFocusY;

        // Detects that new pointers are going down.
        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            mScaling = true;
            lastFocusX = scaleGestureDetector.getFocusX();
            lastFocusY = scaleGestureDetector.getFocusY();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mScaling = false;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float scaleFactor = detector.getScaleFactor();
            mScaleFactor *= scaleFactor;
            mScaleFactor = Math.min(mScaleFactor, MAX_AFD_SCALE);
            mScale.setScaleFactor(mScaleFactor);

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float moveX = mPan.getMoveX() + ((focusX - lastFocusX) / mScaleFactor);
            float moveY = mPan.getMoveY() + ((focusY - lastFocusY) / mScaleFactor);
            lastFocusX = focusX;
            lastFocusY = focusY;

            mPan.setMove(moveX, moveY);

            invalidate();

            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            // Don't pan/draw if multi-touch scaling is under way
            if( mScaling ) return false;

            float moveX = mPan.getMoveX() - (distanceX) / mScale.getScaleFactor();
            float moveY = mPan.getMoveY() - (distanceY) / mScale.getScaleFactor();
            mPan.setMove(moveX, moveY);

            invalidate();
            return true;
        }

    }

}
