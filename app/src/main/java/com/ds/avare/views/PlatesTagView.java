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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;

/**
 * 
 * @author zkhan
 * 
 * User tags a plate through this view
 *
 */
public class PlatesTagView extends View implements MultiTouchObjectCanvas<Object>, OnTouchListener {
	

    private Pan                          mPan;
	private Paint                        mPaint;
    private MultiTouchController<Object> mMultiTouchC;
    private PointInfo                    mCurrTouchPoint;
    private BitmapHolder                 mBitmap;
    private Scale                        mScale;
    private int                          mX;
    private int                          mY;
    private float                        mAirportX;
    private float                        mAirportY;
    private String                        mAirportName;

    private static final double MAX_PLATE_SCALE = 8;
    
    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mPan = new Pan();
        setOnTouchListener(this);
        mScale = new Scale(MAX_PLATE_SCALE);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        setBackgroundColor(Color.BLACK);
        mX = mY = 0;
        mAirportName = "";
        mAirportX = mAirportY = -1;
    }
    
    /**
     * 
     * @param context
     */
	public PlatesTagView(Context context) {
		super(context);
		setup(context);
	}

    /**
     * 
     * @param context
     */
    public PlatesTagView(Context context, AttributeSet set) {
        super(context, set);
        setup(context);
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
        return mMultiTouchC.onTouchEvent(e, mScale.getMaxScale(), mScale.getMinScale(), 1);
    }

    /**
     * @param name
     */
    public void setBitmap(BitmapHolder holder) {
        mBitmap = holder;
        postInvalidate();
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getDraggableObjectAtPoint(com.ds.avare.MultiTouchController.PointInfo)
     */
    public Object getDraggableObjectAtPoint(PointInfo pt) {
        return mBitmap;
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getPositionAndScale(java.lang.Object, com.ds.avare.MultiTouchController.PositionAndScale)
     */
    public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut) {
        objPosAndScaleOut.set(mPan.getMoveX(), mPan.getMoveY(), true,
                mScale.getScaleFactorRaw(), false, 0, 0, false, 0);
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#selectObject(java.lang.Object, com.ds.avare.MultiTouchController.PointInfo)
     */
    public void selectObject(Object obj, PointInfo touchPoint) {
        touchPointChanged(touchPoint);
    }

    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#setPositionAndScale(java.lang.Object, com.ds.avare.MultiTouchController.PositionAndScale, com.ds.avare.MultiTouchController.PointInfo)
     */
    public boolean setPositionAndScale(Object obj,PositionAndScale newObjPosAndScale, PointInfo touchPoint) {
        touchPointChanged(touchPoint);
        if(false == mCurrTouchPoint.isMultiTouch()) {
            /*
             * Multi-touch is zoom, single touch is pan
             */
            mPan.setMove(newObjPosAndScale.getXOff(), newObjPosAndScale.getYOff());
        }
        else {
            /*
             * Clamp scaling.
             */
            mScale.setScaleFactor(newObjPosAndScale.getScale());
        }

        /*
         * Store location
         */
        mX = Math.round((-mPan.getMoveX() + mBitmap.getWidth() / 2));
        mY = Math.round((-mPan.getMoveY() + mBitmap.getHeight() / 2));

        invalidate();
        return true;
    }

    /**
     * @param touchPoint
     */
    private void touchPointChanged(PointInfo touchPoint) {
        mCurrTouchPoint.set(touchPoint);
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
    	
        mPaint.setStrokeWidth(1);
        float min = Math.min(getWidth(), getHeight()) - 8;
        mPaint.setTextSize(min / 20);
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
        
        float scale = mScale.getScaleFactorRaw();
        
        /*
         * Plate
         */
        mBitmap.getTransform().setScale(scale, scale);
        mBitmap.getTransform().postTranslate(
                mPan.getMoveX() * scale
                + getWidth() / 2
                - mBitmap.getWidth() / 2 * scale ,
                mPan.getMoveY() * scale
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
                    + mPan.getMoveX() * scale
                    - mBitmap.getWidth() / 2 * scale);
            float y =
                    (mAirportY * scale
                    + getHeight() / 2
                    + mPan.getMoveY() * scale
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
        mPan.setMove(
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
        mPan = new Pan();
        mScale = new Scale(MAX_PLATE_SCALE);
        
        /*
         * Fit plate to screen
         */
        if(mBitmap != null) {
            float h = getHeight();
            float ih = mBitmap.getHeight();
            float fac = h / ih;
            mScale.setScaleFactor(fac);
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
}

