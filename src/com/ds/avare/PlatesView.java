/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.MultiTouchController.PointInfo;
import com.ds.avare.MultiTouchController.PositionAndScale;

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
    private PixelCoordinates             mPixels;
    private double                      mPx;
    private double                      mPy;
    private double                      mOLon;
    private double                      mOLat;
    private boolean                     mDrawLonLat;
    private double                      mRotated;
    private DecimalFormat 				  mFormat;
    private GpsParams                    mGpsParams;
    private String                       mErrorStatus;
    private float                       mTextDiv;
    private Preferences                  mPref;
    private BitmapHolder                 mAirplaneBitmap;
    
    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mPan = new Pan();
        mPixels = new PixelCoordinates();
        mDrawLonLat = false;
        mRotated = 0;
        mGpsParams = new GpsParams(null);
        mPref = new Preferences(context);
        mTextDiv = mPref.isPortrait() ? 24.f : 12.f;
        mScale = new Scale();
        setOnTouchListener(this);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mFormat = new DecimalFormat("00.00");
        mAirplaneBitmap = new BitmapHolder(context, R.drawable.planegreen);
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
        if(e.getAction() == MotionEvent.ACTION_UP) {
        }
        mGestureDetector.onTouchEvent(e);
        return mMultiTouchC.onTouchEvent(e);
    }

    /**
     * @param name
     */
    public void setBitmap(BitmapHolder holder) {
        mBitmap = holder;
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
     * Get points on the chart
     */
    public PixelCoordinates getPoints() {
    	return mPixels;
    }

    /**
     * Set params to show lon/lat 
     */
    public void setParams(double[] params) {
    	mDrawLonLat = true;
    	
    	mOLon = params[0];
    	mOLat = params[1];
    	mPx = params[2];
    	mPy = params[3];
    	mRotated = params[4];
    	postInvalidate();
    }
    

    /**
     * 
     * @return
     */
    public double getXn() {
        return(mPan.getMoveX());
    }

    /**
     * 
     * @return
     */
    public double getYn() {
        return(mPan.getMoveY());
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
        FontMetrics fm = mPaint.getFontMetrics();
        float fh =  fm.bottom - fm.top;
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
        
    	/*
    	 * Plate
    	 */
    	mBitmap.getTransform().setScale(mScale.getScaleFactor(), mScale.getScaleFactor());
    	mBitmap.getTransform().postTranslate(
    			mPan.getMoveX() * mScale.getScaleFactor() + getWidth() / 2 - mBitmap.getWidth() / 2 * mScale.getScaleFactor(),
    			mPan.getMoveY() * mScale.getScaleFactor() + getHeight() / 2 - mBitmap.getHeight() / 2 * mScale.getScaleFactor());
    	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
    	mPaint.setStrokeWidth(4);
    	/*
    	 * Drag line for points
    	 */
    	if(mPixels.firstPointAcquired()) {
    	    if(
    	            (Math.abs(mPixels.getX0() - mPan.getMoveX()) < PixelCoordinates.POINTS_MIN_PIXELS) ||
    	            (Math.abs(mPixels.getY0() - mPan.getMoveY()) < PixelCoordinates.POINTS_MIN_PIXELS)
    	            ) {
    	        mPaint.setColor(Color.RED);    	        
    	    }
    	    else {
                mPaint.setColor(Color.GREEN);    	        
    	    }
        	canvas.drawLine(
        			getWidth() / 2,
        			getHeight() / 2,
        			getWidth() / 2 + mPan.getMoveX() * mScale.getScaleFactor() - (float)mPixels.getX0() * mScale.getScaleFactor(),
        			getHeight() / 2 + mPan.getMoveY() * mScale.getScaleFactor() - (float)mPixels.getY0() * mScale.getScaleCorrected(),
        			mPaint);    		
    	}
    	
    	/*
    	 * Cross hair
    	 */
        mPaint.setColor(Color.RED);
    	canvas.drawLine(getWidth() / 2, getHeight() / 2 - 16, getWidth() / 2, getHeight() / 2 + 16, mPaint);
    	canvas.drawLine(getWidth() / 2 - 16, getHeight() / 2, getWidth() / 2 + 16, getHeight() / 2, mPaint);
    	
    	/*
    	 * This will show lon/lat under current cross hair
    	 */
    	if(mDrawLonLat) {
    	    double lonms;
    	    double latms;
    	    if(mRotated == 0) {
                lonms = mOLon - mPx * mPan.getMoveX();
                latms = mOLat - mPy * mPan.getMoveY();              
    	    }
    	    else {
    	        /*
    	         * Rotated plate
    	         */
                lonms = mOLon - mPy * mPan.getMoveY();
                latms = mOLat - mPx * mPan.getMoveX();
    	    }
    		int lon = (int)lonms;
    		int lat = (int)latms;
    		double lonl = Math.abs((lonms - (double)lon) * 60);
    		if(60 == lonl) {
    		    lon--;
    		    lonl = 0;
    		}
    		double latl = Math.abs((latms - (double)lat) * 60);
            if(60 == latl) {
                lat++;
                latl = 0;
            }
            
            mPaint.setTextAlign(Align.LEFT);
    		canvas.drawText("" + lon + '\u00B0' + mFormat.format(lonl) + "'" + ", "
    		        + lat + '\u00B0' + mFormat.format(latl) + "'", 0, getHeight() - fh, mPaint);
    	}
    	
        if(mErrorStatus != null) {
            mPaint.setTextAlign(Align.RIGHT);

            mPaint.setColor(Color.RED);
            canvas.drawText(mErrorStatus,
                    getWidth(), getHeight() / mTextDiv, mPaint);
        }
        else {
            
            if(null == mGpsParams) {
                return;
            }
            
            /*
             * Calculate offsets of our location
             */
            double x, y;
            if(mRotated == 0) {
                x = (mOLon - mGpsParams.getLongitude()) / mPx;
                y = (mOLat - mGpsParams.getLatitude()) / mPy;              
            }
            else {
                /*
                 * Rotated plate
                 */
                y = (mOLon - mGpsParams.getLongitude()) / mPy;
                x = (mOLat - mGpsParams.getLatitude()) / mPx;
            }
           
            /*
             * Draw airplane at that location
             */
            mAirplaneBitmap.getTransform().setRotate((float)mGpsParams.getBearing() - (float)mRotated,
                    mAirplaneBitmap.getWidth() / 2.f,
                    mAirplaneBitmap.getHeight() / 2.f);
            
            mAirplaneBitmap.getTransform().postTranslate(
                    getWidth() / 2.f
                    - (float)x * mScale.getScaleFactor()
                    - mAirplaneBitmap.getWidth()  / 2.f
                    + mPan.getMoveX() * mScale.getScaleFactor(),
                    getHeight() / 2.f
                    - (float)y * mScale.getScaleFactor()
                    - mAirplaneBitmap.getHeight()  / 2.f
                    + mPan.getMoveY() * mScale.getScaleFactor());
            canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
        }
        

    }
    
    /**
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            /*
             * On double tap, move to center
             */
            mPan = new Pan();
            return true;
        }

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
        	
        }
    }
}
