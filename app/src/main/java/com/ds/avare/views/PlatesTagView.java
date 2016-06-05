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
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;

import com.ds.avare.touch.BasicOnScaleGestureListener;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.ViewParams;

/**
 * 
 * @author zkhan
 * 
 * User tags a plate through this view
 *
 */
public class PlatesTagView extends View implements OnTouchListener {
	

	private Paint                        mPaint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private int                          mX;
    private int                          mY;
    private float                        mAirportX;
    private float                        mAirportY;
    private String                       mAirportName;

    private ViewParams mViewParams;
    private ScaleGestureDetector mScaleDetector;

    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mViewParams = new ViewParams();
        mViewParams.mPan = new Pan();
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mViewParams.mScale = new Scale(mViewParams.MAX_SCALE);
        setBackgroundColor(Color.BLACK);
        mX = mY = 0;
        mAirportName = "";
        mAirportX = mAirportY = -1;
        BasicOnScaleGestureListener gestureListener = new BasicOnScaleGestureListener(mViewParams, this);
        mScaleDetector = new ScaleGestureDetector(context, gestureListener);
    }
    
    /**
     * 
     * @param context
     */
	public PlatesTagView(Context context) {
        this(context, null, 0);
	}

    /**
     * 
     * @param context
     */
    public PlatesTagView(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    /**
     * 
     * @param context
     */
    public PlatesTagView(Context context, AttributeSet set, int arg) {
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
        postInvalidate();
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
    	
        mPaint.setStrokeWidth(1);
        float min = Math.min(getWidth(), getHeight()) - 8;
        mPaint.setTextSize(min / 20);
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
        
        float scale = mViewParams.mScale.getScaleFactorRaw();
        
        /*
         * Plate
         */
        mBitmap.getTransform().setScale(scale, scale);
        mBitmap.getTransform().postTranslate(
                mViewParams.mPan.getMoveX() * scale
                + getWidth() / 2
                - mBitmap.getWidth() / 2 * scale ,
                mViewParams.mPan.getMoveY() * scale
                + getHeight() / 2
                - mBitmap.getHeight() / 2 * scale);
        
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
    	
    	/*
    	 * The cross in the middle
    	 */
    	mPaint.setColor(Color.RED);
    	mPaint.setStyle(Style.STROKE);
        canvas.drawLine(0, getHeight() / 2, getWidth() , getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 4, mPaint);
        
        /*
         * Draw Airport circle
         */
        if(mAirportX > 0 && mAirportY > 0 && mAirportName != null) {
            mPaint.setStrokeWidth(4);
            mPaint.setColor(Color.GREEN);
            float x =
                    (mAirportX * scale
                    + getWidth() / 2
                    + mViewParams.mPan.getMoveX() * scale
                    - mBitmap.getWidth() / 2 * scale);
            float y =
                    (mAirportY * scale
                    + getHeight() / 2
                    + mViewParams.mPan.getMoveY() * scale
                    - mBitmap.getHeight() / 2 * scale);
            
            mPaint.setAlpha(127);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, 16, mPaint);
            mPaint.setShadowLayer(4, 4, 4, Color.BLACK);
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(1);
            canvas.drawText(mAirportName, x + 16, y + 16, mPaint);
            mPaint.setAlpha(255);
        }
    }
    
    /**
     * Verify a point at x, y
     * @param x
     * @param y
     */
    public void verify(double x, double y) {
        mViewParams.mPan.setMove(
                (float)-x + mBitmap.getWidth() / 2,
                (float)-y + mBitmap.getHeight() / 2
                );
        invalidate();
    }
    
    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mViewParams.mPan = new Pan();
        mViewParams.mScale = new Scale(mViewParams.MAX_SCALE);
        
        /*
         * Fit plate to screen
         */
        if(mBitmap != null) {
            float h = getHeight();
            float ih = mBitmap.getHeight();
            float fac = h / ih;
            mViewParams.mScale.setScaleFactor(fac);
        }

        postInvalidate();
    }
    
    
    /**
     * Current X with scale adjusted
     */
    public int getx() {
        return mX;
    }
    
    /**
     * Current Y with scale adjusted
     */
    public int gety() {
        return mY;
    }

    /**
     * 
     * @param x
     * @param y
     */
    public void setAirport(String name, float x, float y) {
        mAirportX = x;
        mAirportY = y;
        mAirportName = name;
        postInvalidate();
    }
    
    /**
     * 
     */
    public void unsetAirport() {
        mAirportX = -1;
        mAirportY = -1;
        mAirportName = "";        
        postInvalidate();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {

            if (mViewParams.mScaling) return false;

            float moveX = mViewParams.mPan.getMoveX() - (distanceX) / mViewParams.mScale.getScaleFactor();
            float moveY = mViewParams.mPan.getMoveY() - (distanceY) / mViewParams.mScale.getScaleFactor();

            mViewParams.mPan.setMove(moveX, moveY);
            invalidate();
            return true;
        }
    }
}

