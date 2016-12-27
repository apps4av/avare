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
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.BasicOnScaleGestureListener;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DisplayIcon;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.ViewParams;

/**
 * 
 * @author zkhan
 * @author plinel
 *
 */
public class PlatesView extends View implements OnTouchListener {
	
	private Paint                        mPaint;
    private GestureDetector              mGestureDetector;
    private BitmapHolder                 mBitmap;
    private GpsParams                    mGpsParams;
    private String                       mErrorStatus;
    private Preferences                  mPref;
    private BitmapHolder                 mAirplaneBitmap;
    private float[]                      mMatrix;
    private boolean                      mShowingAD;
    private StorageService               mService;
    private double                       mAirportLon;
    private double                       mAirportLat;
    private ViewParams                   mViewParams;

    private Context                   mContext;
    /*
     * Is it drawing?
     */
    private boolean                   mDraw;

    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    private static final int SHADOW = 4;

    private ScaleGestureDetector mScaleDetector;

    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(getResources().getDimension(R.dimen.TextSize));

        mViewParams = new ViewParams();
        mContext = context;
        mViewParams.setPan(new Pan());
        mMatrix = null;
        mShowingAD = false;
        mGpsParams = new GpsParams(null);
        mAirportLon = 0;
        mAirportLat = 0;
        mPref = new Preferences(context);
        mViewParams.setScale(new Scale(mViewParams.getMaxScale()));
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        setBackgroundColor(Color.BLACK);
        mAirplaneBitmap = DisplayIcon.getDisplayIcon(context, mPref);
        mDipToPix = Helper.getDpiToPix(context);
        BasicOnScaleGestureListener gestureListener = new BasicOnScaleGestureListener(mViewParams, this);
        mScaleDetector = new ScaleGestureDetector(context, gestureListener);
    }

    // Condition for rotation, only rotate when track up and either airport diagram or geo tagged plate is showing
    private boolean shouldRotate() {
        // XXX: Fix rotation
        return mPref.isTrackUpPlates() && (mShowingAD || null != mMatrix);
    }

    /**
     * 
     * @param context
     */
	public PlatesView(Context context) {
		this(context, null, 0);
	}

    /**
     * 
     * @param context
     */
    public PlatesView(Context context, AttributeSet set) {
        this(context, set, 0);
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

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        boolean retVal = mGestureDetector.onTouchEvent(e);
        retVal = mScaleDetector.onTouchEvent(e) || retVal;
        return retVal;
    }

    /**
     * @param holder
     */
    public void setBitmap(BitmapHolder holder) {
        boolean init = (mBitmap == null);
        mBitmap = holder;
        postInvalidate();
        if (init) center(); // center the first time a bitmap is loaded
    }
    /**
     * 
     * @param canvas
     */
    private void drawDrawing(Canvas canvas) {
        if(null == mService) {
            return;
        }


        Paint.Cap oldCaps = mPaint.getStrokeCap();
        mPaint.setStrokeCap(Paint.Cap.ROUND); // We use a wide line. Without ROUND the line looks broken.
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(6 * mDipToPix);
        mService.getPixelDraw().drawShape(canvas, mPaint);

        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(2 * mDipToPix);
        mService.getPixelDraw().drawShape(canvas, mPaint);
        mPaint.setStrokeCap(oldCaps); // Restore the Cap we had before drawing

    }

    /**
     * Set params to show lon/lat 
     */
    public void setParams(float[] params, boolean ad) {
        mMatrix = params;
        mShowingAD = ad;
        postInvalidate();
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {

        float angle = 0;
        boolean bRotated = false;
        if(mBitmap != null && mBitmap.getBitmap() != null) {
    	
            
            float scale = mViewParams.getScale().getScaleFactorRaw();

            float lon = (float) mGpsParams.getLongitude();
            float lat = (float) mGpsParams.getLatitude();
            float pixx = 0;
            float pixy = 0;
            float pixAirportx = 0;
            float pixAirporty = 0;

                
            if(null != mGpsParams && null != mMatrix) {

                /*
                 * Calculate offsets of our location
                 */


                if (mShowingAD) {
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
                    pixAirportx = (wftA * (float) mAirportLon + wftC * (float) mAirportLat + wftE) / 2.f;
                    pixAirporty = (wftB * (float) mAirportLon + wftD * (float) mAirportLat + wftF) / 2.f;
                    
                    /*
                     * Now find angle.
                     * Add 0.1 to lat that gives us north
                     * Y increase down so give -180
                     */
                    float pixxn = (wftA * lon + wftC * (lat + 0.1f) + wftE) / 2.f;
                    float pixyn = (wftB * lon + wftD * (lat + 0.1f) + wftF) / 2.f;
                    float diffx = pixxn - pixx;
                    float diffy = pixyn - pixy;
                    angle = (float) Math.toDegrees(Math.atan2(diffx, -diffy));
                } else {
                    /*
                     * User's database
                     */
                    float dx = mMatrix[0];
                    float dy = mMatrix[1];
                    float lonTopLeft = mMatrix[2];
                    float latTopLeft = mMatrix[3];
                    pixx = (lon - lonTopLeft) * dx;
                    pixy = (lat - latTopLeft) * dy;
                    pixAirportx = ((float) mAirportLon - lonTopLeft) * dx;
                    pixAirporty = ((float) mAirportLat - latTopLeft) * dy;
                    angle = 0;
                }

            }

            // rotate only when showing AD, or showing geo tagged approach plate.
            if(shouldRotate()) {
                canvas.save();
                bRotated = true;
                canvas.rotate(-(int) mGpsParams.getBearing(),getWidth() / 2,getHeight() / 2);
            }

            /*
        	 * Plate
        	 */
            float x = mViewParams.getPan().getMoveX() * scale
                    + getWidth() / 2
                    - mBitmap.getWidth() / 2 * scale;
            float y = mViewParams.getPan().getMoveY() * scale
                    + getHeight() / 2
                    - mBitmap.getHeight() / 2 * scale;
            mBitmap.getTransform().setScale(scale, scale);
            mBitmap.getTransform().postTranslate(x, y);

            // Add plates tag PG's website
            mPaint.setColor(0x007F00);
            mPaint.setAlpha(255);
            canvas.drawText(mContext.getString(R.string.VerifyPlates), x, mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top + getWidth() / 4, mPaint);

            if(mPref.isNightMode()) {
                Helper.invertCanvasColors(mPaint);
            }
            canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), mPaint);
            Helper.restoreCanvasColors(mPaint);

            if(null != mGpsParams && null != mMatrix) {
                
                /*
                 * Draw a circle at center of airport if tagged
                 */
                mPaint.setColor(Color.GREEN);
                mPaint.setAlpha(127);
                canvas.drawCircle(
                        pixAirportx * scale
                        + getWidth() / 2
                        + mViewParams.getPan().getMoveX() * scale
                        - mBitmap.getWidth() / 2 * scale,
                        pixAirporty * scale
                        + getHeight() / 2
                        + mViewParams.getPan().getMoveY() * scale
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
	                        + mViewParams.getPan().getMoveX() * scale
	                        - mBitmap.getWidth() / 2 * scale,
	                        pixy * scale
	                        + getHeight() / 2
	                        - mAirplaneBitmap.getHeight() / 2
	                        + mViewParams.getPan().getMoveY() * scale
	                        - mBitmap.getHeight() / 2 * scale);
	                canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
                }
            }

        }
    	/*
    	 * Draw drawing
    	 */
    	this.drawDrawing(canvas);

        /*
         * restore
         */
        if(bRotated) {
            canvas.restore();
        }

        // do not rotate info lines
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
         * On short press, move to center
         */
        mViewParams.setPan(new Pan());

        if (mBitmap == null || mBitmap.getBitmap() == null) return;

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

    /**
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {

            // Don't pan/draw if multi-touch scaling is under way
            if( mViewParams.isScaling() ) {
                return false;
            }

            // If user is drawing
            if(mDraw && mService != null) {
                float x = e2.getX() ;
                float y = e2.getY() ;

                /*
                 * Threshold the drawing so we do not generate too many points
                 */
                if (shouldRotate()) {
                    double thetab = mGpsParams.getBearing();
                    double p[] = new double[2];
                    p = Helper.rotateCoord(getWidth() / 2,getHeight() / 2 , thetab, x, y);
                    mService.getPixelDraw().addPoint((float)p[0],(float)p[1]);
                }
                else {
                    mService.getPixelDraw().addPoint(x, y);
                }

            }

            // If user is panning
            if( !mDraw ) {

                // In the case of ship-up, rotate the scrolling
                if (shouldRotate()) {
                    double thetab = mGpsParams.getBearing();
                    double p[];
                    p = Helper.rotateCoord(0, 0, thetab, distanceX, distanceY);

                    distanceX = (float)p[0];
                    distanceY = (float)p[1];
                }

                float moveX = mViewParams.getPan().getMoveX() - (distanceX) / mViewParams.getScale().getScaleFactor();
                float moveY = mViewParams.getPan().getMoveY() - (distanceY) / mViewParams.getScale().getScaleFactor();

                mViewParams.getPan().setMove(moveX, moveY);
            }

            invalidate();
            return true;
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
     */
    public void setAirport(String name, double lon, double lat) {
        mAirportLon = lon;
        mAirportLat = lat;
        postInvalidate();
    }

}
