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
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.connections.ConnectionFactory;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DisplayIcon;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.vision.digitalink.RecognitionResult;


/**
 * 
 * @author zkhan
 * @author plinel
 *
 */
public class PlatesView extends PanZoomView implements View.OnTouchListener {

	private Paint                        mPaint;
    private BitmapHolder                 mBitmap;
    private BitmapHolder               mLineHeadingBitmap;
    private GpsParams                    mGpsParams;
    private String                       mErrorStatus;
    private Preferences                  mPref;
    private BitmapHolder                 mAirplaneBitmap;
    private float[]                     mMatrix;
    private boolean                    mShowingAD;
    private StorageService              mService;
    private double                     mAirportLon;
    private double                     mAirportLat;
    private GestureDetector            mGestureDetector;
    private Context                     mContext;
    private boolean                     mDrawing;
    private Ink.Stroke.Builder          mStrokeBuilder;
    private Ink.Builder                 mInkBuilder;
    DigitalInkRecognizer                mRecognizer;
    private boolean                    mWriting;
    private GenericCallback            mCallback;


    /*
     * Is it drawing?
     */
    private boolean                   mDraw;

    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

    DigitalInkRecognitionModel mModel = null;
    private static final double MAX_PLATE_SCALE = 8;
    
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    private static final int SHADOW = 4;


    /**
     * 
     * @param context
     */
    private void  setup(Context context) {
        mContext = context;

        mPaint = new Paint();
        mPaint.setTypeface(Helper.getTypeFace(context));
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(Helper.adjustTextSize(mContext, R.dimen.TextSize));

        mCallback = null;
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mMatrix = null;
        mShowingAD = false;
        mGpsParams = new GpsParams(null);
        mAirportLon = 0;
        mAirportLat = 0;
        mDrawing = false;
        mWriting = false;
        mPref = new Preferences(context);
        setOnTouchListener(this);
        setBackgroundColor(Color.BLACK);
        mAirplaneBitmap = DisplayIcon.getDisplayIcon(context, mPref);
        mLineHeadingBitmap = new BitmapHolder(context, R.drawable.line_heading);
        mDipToPix = Helper.getDpiToPix(context);

        mStrokeBuilder = Ink.Stroke.builder();
        mInkBuilder = Ink.builder();

        // Pick a recognition model.
        mModel = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.EN_US).build();
        RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();

        remoteModelManager.isModelDownloaded(mModel).addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                if(success) {
                    mWriting = true;
                }
                else {
                    mWriting = false;
                    remoteModelManager.download(mModel, new DownloadConditions.Builder().build());
                }
            }
        });

        mRecognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(mModel).build());
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

    

    /**
     * @param holder
     */
    public void setBitmap(BitmapHolder holder) {
        mBitmap = holder;
        center();
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
    	
            
            float scale = mScale.getScaleFactor();

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
            float x = mPan.getMoveX() * scale
                    + getWidth() / 2
                    - mBitmap.getWidth() / 2 * scale;
            float y = mPan.getMoveY() * scale
                    + getHeight() / 2
                    - mBitmap.getHeight() / 2 * scale;
            mBitmap.getTransform().setScale(scale, scale);
            mBitmap.getTransform().postTranslate(x, y);

            float endX = mBitmap.getWidth() * scale;
            float endY = mBitmap.getHeight()* scale;

            if(null != mService){
                mService.getPixelDraw().setMapPoints(x,y,endX+x,endY + y);
            }

            // Add plates tag PG's website
            mPaint.setColor(0x007F00);
            mPaint.setAlpha(255);

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
                        + mPan.getMoveX() * scale
                        - mBitmap.getWidth() / 2 * scale,
                        pixAirporty * scale
                        + getHeight() / 2
                        + mPan.getMoveY() * scale 
                        - mBitmap.getHeight() / 2 * scale,
                        16, mPaint);
                mPaint.setAlpha(255);
                
                
                /*
                 * Draw airplane at that location plus a track line
                 */
                if(null != mAirplaneBitmap) {


                    mLineHeadingBitmap.getTransform().setRotate((float)mGpsParams.getBearing() + angle - 180,
                            mLineHeadingBitmap.getWidth() / 2,
                            0);

                    mLineHeadingBitmap.getTransform().postTranslate(
                            pixx * scale
                                    + getWidth() / 2
                                    - mLineHeadingBitmap.getWidth() / 2
                                    + mPan.getMoveX() * scale
                                    - mBitmap.getWidth() / 2 * scale,
                            pixy * scale
                                    + getHeight() / 2
                                    + 0
                                    + mPan.getMoveY() * scale
                                    - mBitmap.getHeight() / 2 * scale);
                    canvas.drawBitmap(mLineHeadingBitmap.getBitmap(), mLineHeadingBitmap.getTransform(), mPaint);


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

        /*
         * restore
         */
        if(bRotated) {
            canvas.restore();
        }

        // do not rotate info lines
        if(mService != null) {
            if(mPref.showPlateInfoLines()) {
                mService.getInfoLines().drawCornerTextsDynamic(canvas, mPaint, TEXT_COLOR,
                ConnectionFactory.getConnection(ConnectionFactory.CF_BlueToothConnectionOut, mContext).isConnected() ? Color.BLUE : TEXT_COLOR_OPPOSITE,
                SHADOW, getWidth(), getHeight(), mErrorStatus, null);
            }

            if(mPref.getShowCDI()) {
                Destination dest = mService.getDestination();
                if (dest != null) {
                    mService.getVNAV().drawVNAV(canvas, getWidth(), getHeight(), dest);
                }
            }
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
        resetPan();
        resetZoom(MAX_PLATE_SCALE);

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

    final Handler mHandler = new Handler();

    @Override
    public boolean onTouch(View view, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        float x= e.getX();
        float y= e.getY();
        long t = e.getEventTime();

        // writing
        if(mWriting && mCallback != null) {
            switch (e.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    mHandler.removeCallbacksAndMessages(null); // down so word continues
                    mStrokeBuilder = Ink.Stroke.builder();
                    mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    break;
                case MotionEvent.ACTION_MOVE:
                    mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    break;
                case MotionEvent.ACTION_UP:
                    mStrokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    mInkBuilder.addStroke(mStrokeBuilder.build());
                    mStrokeBuilder = null;
                    // only if callback is not null do the detection
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // This is what to send to the recognizer.
                            Ink ink = mInkBuilder.build();

                            mRecognizer.recognize(ink)
                                    .addOnSuccessListener(
                                            new OnSuccessListener<RecognitionResult>() {
                                                @Override
                                                public void onSuccess(RecognitionResult recognitionResult) {
                                                    if(mCallback != null) {
                                                        mCallback.callback(recognitionResult.getCandidates().get(0).getText(), null);
                                                    }
                                                }
                                            }
                                    );
                            // new recognition
                            mInkBuilder = Ink.builder();
                        }
                    }, 500); // lifting up for 500 ms is a new word
                default:
                    break;

            }
            // writing so do not pan
            return true;
        }

        if (shouldRotate()) {
            // rotate pan
            double thetab = mGpsParams.getBearing();
            double p[] = new double[2];
            p = Helper.rotateCoord(getWidth() / 2, getHeight() / 2, thetab, e.getX(), e.getY());
            e.setLocation((float)p[0], (float)p[1]);
        }

        // drawing stuff
        if(e.getPointerCount() == 1 && mDraw && mService != null) { // only draw with 1 pointer
            switch (e.getAction() & MotionEvent.ACTION_MASK) { // draw when moving with 1 pointer and there was a pointer down before
                case MotionEvent.ACTION_DOWN:
                    mDrawing = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mDrawing = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mDrawing) {
                        Point pt = getFirstPoint(e);
                        /*
                         * Threshold the drawing so we do not generate too many points
                         */
                        if (mPref.isTrackUp()) {
                            double thetab = mGpsParams.getBearing();
                            double p[] = new double[2];
                            p = Helper.rotateCoord(getWidth() / 2, getHeight() / 2, thetab, pt.x, pt.y);
                            mService.getPixelDraw().addPoint((float) p[0], (float) p[1]);
                        } else {
                            mService.getPixelDraw().addPoint(pt.x, pt.y);
                        }
                        invalidate();
                        return true; // do not PTZ
                    }
                    break;
            }
        }
        else {
            mDrawing = false;
        }

        return false;
    }

    /**
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

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

    // writing recognition callback
    public void setWriteCallback(GenericCallback cb) {
        mCallback = cb;
    }
}
