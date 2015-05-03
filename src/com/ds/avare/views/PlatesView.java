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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.MultiTouchController;
import com.ds.avare.touch.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.touch.MultiTouchController.PointInfo;
import com.ds.avare.touch.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DisplayIcon;
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
    private Preferences                  mPref;
    private BitmapHolder                 mAirplaneBitmap;
    private float[]                     mMatrix;
    private boolean                    mShowingAD;
    private StorageService              mService;
    private double                     mAirportLon;
    private double                     mAirportLat;

    /*
     * Is it drawing?
     */
    private boolean                   mDraw;

    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

    private static final double MAX_PLATE_SCALE = 8;
    
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    private static final int SHADOW = 4;

    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(getResources().getDimension(R.dimen.TextSize));

        mPan = new Pan();
        mMatrix = null;
        mShowingAD = false;
        mGpsParams = new GpsParams(null);
        mAirportLon = 0;
        mAirportLat = 0;
        mPref = new Preferences(context);
        mScale = new Scale(MAX_PLATE_SCALE);
        setOnTouchListener(this);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mAirplaneBitmap = DisplayIcon.getDisplayIcon(context, mPref);
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
             * Do not move on multitouch
             */
            if(mDraw && mService != null) {
                float x = mCurrTouchPoint.getX() * mScale.getScaleFactor();
                float y = mCurrTouchPoint.getY() * mScale.getScaleFactor();
                /*
                 * Threshold the drawing so we do not generate too many points
                 */
                mService.getPixelDraw().addPoint(x, y);
                return true;
            }

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
     * 
     * @param canvas
     */
    private void drawDrawing(Canvas canvas) {
        if(null == mService) {
            return;
        }

        /*
         * Get draw points.
         */
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(4 * mDipToPix);
        mService.getPixelDraw().drawShape(canvas, mPaint);
        
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
        
    	if(mBitmap != null && mBitmap.getBitmap() != null) {
    	
            
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
            
        	if(mPref.isNightMode()) {
                Helper.invertCanvasColors(mPaint);
            }
        	canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
            Helper.restoreCanvasColors(mPaint);
            
                
            if(null != mGpsParams && null != mMatrix) {
            
                /*
                 * Calculate offsets of our location
                 */
                
                float lon = (float)mGpsParams.getLongitude();
                float lat = (float)mGpsParams.getLatitude();
                float pixx = 0;
                float pixy = 0;
                float pixAirportx = 0;
                float pixAirporty = 0;
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
                    pixAirportx = (wftA * (float)mAirportLon + wftC * (float)mAirportLat + wftE) / 2.f;
                    pixAirporty = (wftB * (float)mAirportLon + wftD * (float)mAirportLat + wftF) / 2.f;
                    
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
                     * User's database
                     */
                    float dx = mMatrix[0];
                    float dy = mMatrix[1];
                    float lonTopLeft = mMatrix[2];
                    float latTopLeft = mMatrix[3];
                    pixx = (lon - lonTopLeft) * dx;
                    pixy = (lat - latTopLeft) * dy;
                    pixAirportx = ((float)mAirportLon - lonTopLeft) * dx;
                    pixAirporty = ((float)mAirportLat - latTopLeft) * dy;
                    angle = 0;
                }
                
                /*
                 * Draw a circle at center of airport if tagged
                 */
                mPaint.setColor(Color.GREEN);
                mPaint.setAlpha(127);
                canvas.drawCircle(
                        pixAirportx * scale
                        + getWidth() / 2
                        + mPan.getMoveX() * scale
                        - mBitmap.getWidth() / 2 * scale,
                        pixAirporty * scale
                        + getHeight() / 2
                        + mPan.getMoveY() * scale 
                        - mBitmap.getHeight() / 2 * scale,
                        16, mPaint);
                mPaint.setAlpha(255);
                
                
                /*
                 * Draw airplane at that location
                 */
                if(null != mAirplaneBitmap) {
	                mAirplaneBitmap.getTransform().setRotate((float)mGpsParams.getBearing() + angle,
	                        mAirplaneBitmap.getWidth() / 2,
	                        mAirplaneBitmap.getHeight() / 2);
	                
	                mAirplaneBitmap.getTransform().postTranslate(
	                        pixx * scale
	                        + getWidth() / 2
	                        - mAirplaneBitmap.getWidth() / 2
	                        + mPan.getMoveX() * scale
	                        - mBitmap.getWidth() / 2 * scale,
	                        pixy * scale
	                        + getHeight() / 2
	                        - mAirplaneBitmap.getHeight() / 2
	                        + mPan.getMoveY() * scale 
	                        - mBitmap.getHeight() / 2 * scale);
	                canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
                }
            }
        }

    	/*
    	 * Draw drawing
    	 */
    	this.drawDrawing(canvas);

        if(mService != null && mPref.showPlateInfoLines()) {
            mService.getInfoLines().drawCornerTextsDynamic(canvas, mPaint, TEXT_COLOR, TEXT_COLOR_OPPOSITE, SHADOW,
                    getWidth(), getHeight(), mErrorStatus, null);
        }

    }
    
    /**
     * 
     * @param s
     */
    public void setService(StorageService s) {
        mService = s;
    }
    
    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mScale = new Scale(MAX_PLATE_SCALE);
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
        
        @Override
        public boolean onDown(MotionEvent e) {
            if(null != mService) {
                /*
                 * Add separation between chars
                 */
                mService.getPixelDraw().addSeparation();
            }
            return true;
        }

    }
    
    
    /**
     * 
     * @param b
     */
    public void setDraw(boolean b) {
        mDraw = b;
        invalidate();
    }

    /**
     *
     */
    public boolean getDraw() {
        return mDraw;
    }

    /**
     * 
     * @param x
     * @param y
     */
    public void setAirport(String name, double lon, double lat) {
        mAirportLon = lon;
        mAirportLat = lat;
        postInvalidate();
    }

    /**
     * 
     * @param factor
     */
    public void adjustZoom(double factor) {
    	/**
    	 * XXX: Disabled till fixed.
    	mScale.adjustZoom(factor);
    	postInvalidate();
    	 */
    }
}
