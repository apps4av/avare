/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

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
import com.ds.avare.touch.MultiTouchController;
import com.ds.avare.touch.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.touch.MultiTouchController.PointInfo;
import com.ds.avare.touch.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;

/**
 * 
 * @author zkhan
 *
 */
public class PlatesTagView extends View implements MultiTouchObjectCanvas<Object>, OnTouchListener {
	

    private Pan                          mPan;
	private Paint                        mPaint;
    private MultiTouchController<Object> mMultiTouchC;
    private PointInfo                    mCurrTouchPoint;
    private BitmapHolder                 mBitmap;
    private Scale                        mScale;

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
        mScale = new Scale();
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        setBackgroundColor(Color.BLACK);
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
        return mMultiTouchC.onTouchEvent(e);
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
                mScale.getScaleFactor(), false, 0, 0, false, 0);
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
    	
        float min = Math.min(getWidth(), getHeight()) - 8;
        mPaint.setTextSize(min / 20);
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
        
        float scale = mScale.getScaleFactor();
        
        /*
         * Plate
         */
        mBitmap.getTransform().setScale(scale, scale);
        mBitmap.getTransform().postTranslate(
                mPan.getMoveX() * scale,
                mPan.getMoveY() * scale);
        
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
    	
    	/*
    	 * The cross in the middle
    	 */
    	mPaint.setColor(Color.RED);
    	mPaint.setStyle(Style.STROKE);
        canvas.drawLine(0, getHeight() / 2, getWidth() , getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 2, mPaint);
        
    }
    
    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mPan = new Pan();
        mScale = new Scale();
        
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

}
