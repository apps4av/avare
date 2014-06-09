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
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.MultiTouchController;
import com.ds.avare.touch.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.touch.MultiTouchController.PointInfo;
import com.ds.avare.touch.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 */
public class PlatesView extends View implements MultiTouchObjectCanvas<Object>, OnTouchListener {
	

    private Scale                        mScale;
    private Pan                          mPan;
	private Paint                        mPaint;
    private MultiTouchController<Object> mMultiTouchC;
    private PointInfo                    mCurrTouchPoint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private GpsParams                    mGpsParams;
    private String                       mErrorStatus;
    private float                       mTextDiv;
    private Preferences                  mPref;
    private BitmapHolder                 mAirplaneBitmap;
    private float[]                     mMatrix;
    private float			             mDipToPix;
    private boolean                    mShowingAD;
    
    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mPan = new Pan();
        mMatrix = null;
        mShowingAD = false;
        mGpsParams = new GpsParams(null);
        mPref = new Preferences(context);
        mTextDiv = mPref.getOrientation().contains("Portrait") ? 24.f : 12.f;
        mScale = new Scale();
        setOnTouchListener(this);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mAirplaneBitmap = new BitmapHolder(context, mPref.isHelicopter() ? R.drawable.heli : R.drawable.plane);
	/*
	 *  Converts 1 dip (device independent pixel) into its equivalent physical pixels
	 */
        mDipToPix = Helper.getDpiToPix(context);
    }
    
    /**
     * 
     * @param context
     */
	public PlatesView(Context context) {
		super(context);
		setup(context);
	}

    /**
     * 
     * @param context
     */
    public PlatesView(Context context, AttributeSet set) {
        super(context, set);
        setup(context);
    }

    /**
     * 
     * @param context
     */
    public PlatesView(Context context, AttributeSet set, int arg) {
        super(context, set, arg);
        setup(context);
    }

    
    /**
     * @param params
     */
    public void updateParams(GpsParams params) {
        /*
         * Comes from location manager
         */
        mGpsParams = params;
        postInvalidate();
    }

    
    /**
     * @param status
     */
    public void updateErrorStatus(String status) {
        /*
         * Comes from timer of activity
         */
        mErrorStatus = status;
        postInvalidate();
    }

    
    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
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
     * Set params to show lon/lat 
     */
    public void setParams(float[] params, boolean ad) {
    	mMatrix = params;
    	mShowingAD = ad;
    	postInvalidate();
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
        
    	if(mPref.isNightMode()) {
            Helper.invertCanvasColors(mPaint);
        }
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
        Helper.restoreCanvasColors(mPaint);
        
    	mPaint.setStrokeWidth(4 * mDipToPix);
    	
    	
        if(mErrorStatus != null) {
            mPaint.setTextAlign(Align.RIGHT);

            mPaint.setColor(Color.RED);
            canvas.drawText(mErrorStatus,
                    getWidth(), getHeight() / mTextDiv, mPaint);
        }
        else {
            
            if(null == mGpsParams || null == mMatrix) {
                return;
            }
            
            /*
             * Calculate offsets of our location
             */
            
            float lon = (float)mGpsParams.getLongitude();
            float lat = (float)mGpsParams.getLatitude();
            float pixx = 0;
            float pixy = 0;
            float angle = 0;
            
            if(mShowingAD) {
                /*
                 * Mike's matrix
                 */
                float wftA = mMatrix[6];
                float wftB = mMatrix[7];
                float wftC = mMatrix[8];
                float wftD = mMatrix[9];
                float wftE = mMatrix[10];
                float wftF = mMatrix[11];
                
                pixx = (wftA * lon + wftC * lat + wftE) / 2.f;
                pixy = (wftB * lon + wftD * lat + wftF) / 2.f;
                
                /*
                 * Now find angle.
                 * Add 0.1 to lat that gives us north
                 * Y increase down so give -180
                 */
                float pixxn = (wftA * lon + wftC * (lat + 0.1f) + wftE) / 2.f;
                float pixyn = (wftB * lon + wftD * (lat + 0.1f) + wftF) / 2.f;
                float diffx = pixxn - pixx;
                float diffy = pixyn - pixy;
                angle = (float)Math.toDegrees(Math.atan2(diffx, -diffy));
            }
            else {
                /*
                 * Faisal's database
                 */
                pixx = (lon - mMatrix[0]) * mMatrix[1] / 2.f;
                pixy = (lat - mMatrix[2]) * mMatrix[3] / 2.f;
                angle = -mMatrix[4];
            }
            
            /*
             * Draw airplane at that location
             */
            mAirplaneBitmap.getTransform().setRotate((float)mGpsParams.getBearing() + angle,
                    mAirplaneBitmap.getWidth() / 2.f,
                    mAirplaneBitmap.getHeight() / 2.f);
            
            mAirplaneBitmap.getTransform().postTranslate(
                    pixx * scale
                    - mAirplaneBitmap.getWidth()  / 2.f
                    + mPan.getMoveX() * scale,
                    pixy * scale
                    - mAirplaneBitmap.getHeight()  / 2.f
                    + mPan.getMoveY() * scale);
            canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
        }
    }
    
    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mScale = new Scale();
        mPan = new Pan();
        
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
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
        	
        }
    }
}
