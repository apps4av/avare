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
import com.ds.avare.touch.BasicOnScaleGestureListener;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.ViewParams;

/**
 * 
 * @author zkhan
 *
 */
public class AfdView extends View implements OnTouchListener {
	
	private Paint                        mPaint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private Preferences                  mPref;
    
    private ViewParams  mViewParams;

    private ScaleGestureDetector mScaleDetector;

    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mViewParams = new ViewParams();
        mViewParams.setPan(new Pan());
        mViewParams.setScale(new Scale(mViewParams.getMaxScale()));
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mPref = new Preferences(context);
        BasicOnScaleGestureListener gestureListener = new BasicOnScaleGestureListener(mViewParams, this);
        mScaleDetector = new ScaleGestureDetector(context, gestureListener);
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
        if( mBitmap != null ) center();
        postInvalidate();
    }

    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mViewParams.setPan(new Pan());

        // Figure out the scale that will fit to window
        float heightScale = (float)this.getHeight() / (float)mBitmap.getBitmap().getHeight();
        float widthScale = (float)this.getWidth() / (float)mBitmap.getBitmap().getWidth();
        float toFitScaleFactor = Math.min(heightScale, widthScale);

        // Scale to "fit", and set that as minimum scale
        mViewParams.getScale().setScaleFactor(toFitScaleFactor);
        mViewParams.setScaleFactor(toFitScaleFactor);
        mViewParams.setMinScale(toFitScaleFactor);

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
        
        float scale = mViewParams.getScale().getScaleFactorRaw();

    	/*
    	 * A/FD
    	 */
        mBitmap.getTransform().setScale(scale, scale);
        mBitmap.getTransform().postTranslate(
                mViewParams.getPan().getMoveX() * scale
                + getWidth() / 2
                - mBitmap.getWidth() / 2 * scale ,
                mViewParams.getPan().getMoveY() * scale
                + getHeight() / 2
                - mBitmap.getHeight() / 2 * scale);
        
        if(mPref.isNightMode()) {
            Helper.invertCanvasColors(mPaint);
        }
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
        Helper.restoreCanvasColors(mPaint);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            // Don't pan/draw if multi-touch scaling is under way
            if( mViewParams.isScaling()) {
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
