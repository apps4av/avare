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


import java.util.LinkedList;
import java.util.List;

import com.ds.avare.adsb.NexradBitmap;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.instruments.EdgeDistanceTape;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Scale;
import com.ds.avare.shapes.DistanceRings;
import com.ds.avare.shapes.MetShape;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.Tile;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.touch.MultiTouchController;
import com.ds.avare.touch.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.touch.MultiTouchController.PointInfo;
import com.ds.avare.touch.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines;
import com.ds.avare.utils.InfoLines.InfoLineFieldLoc;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;
import com.ds.avare.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

/**
 * @author zkhan
 * 
 * This is a view that user sees 99% of the time. Has moving map on it.
 */
public class LocationView extends View implements MultiTouchObjectCanvas<Object>, OnTouchListener {
    /**
     * paint for onDraw
     */
    private Paint                      mPaint;
    /**
     * Current GPS location
     */
    private GpsParams                  mGpsParams;
    private GpsParams				    mVSIParams;
    
    /**
     * The plane on screen
     */
    private BitmapHolder               mAirplaneBitmap;
    private BitmapHolder               mRunwayBitmap;
    private BitmapHolder               mLineBitmap;
    private BitmapHolder               mObstacleBitmap;
    private BitmapHolder               mLineHeadingBitmap;
    private BitmapHolder               mAirplaneOtherBitmap;
    
    /**
     * The magic of multi touch
     */
    private MultiTouchController<Object> mMultiTouchC;
    /**
     * The magic of multi touch
     */
    private PointInfo                   mCurrTouchPoint;
    /**
     * Gesture like long press, double touch outside of multi-touch
     */
    private GestureDetector             mGestureDetector;
    /**
     * Cache
     */
    private Context                     mContext;
    /**
     * Current movement from center
     */
    private Movement                    mMovement;
    /**
     * GPS status string if it fails, set by activity
     */
    private String                      mErrorStatus;
   
    /**
     * Task that would draw tiles on bitmap.
     */
    private TileDrawTask                mTileDrawTask; 
    private Thread                      mTileDrawThread;

    
    /**
     * Task that would draw obstacles
     */
    private ObstacleTask                mObstacleTask; 

    /**
     * Task that finds closets airport.
     */
    private ClosestAirportTask          mClosestTask; 

    /**
     * Storage service that contains all the state
     */
    private StorageService              mService;

    /**
     * Translation of current pan 
     */
    private Pan                         mPan;
    
    private DataSource                  mImageDataSource;
    
    /**
     * To tell activity to do something on a gesture or touch
     */
    private GestureInterface            mGestureCallBack; 

    /**
     * Scale factor based on pinch zoom
     */
    private Scale                       mScale;
    
    /*
     * A hashmap to load only required tiles.
     */
    
    private Preferences                 mPref;
    private TextPaint                   mTextPaint;
    
    private Typeface                    mFace;
    
    private String                      mOnChart;
    
    /**
     * These are longitude and latitude at top left (0,0)
     */
    private Origin                      mOrigin;
    
    /*
     * Projection of a touch point
     */
    private Projection                  mPointProjection;
    
    /*
     * Obstacles
     */
    private LinkedList<Obstacle>        mObstacles;
    
    /*
     * Is it drawing?
     */
    private boolean                   mDraw;

    /*
     * Threshold for terrain
     */
    private float                      mThreshold;
    

    private boolean                    mTrackUp;
    
    /*
     * Current ground elevation
     */
    private double                      mElev;
    
    /*
     * Macro of zoom
     */
    private int                         mMacro;

    private float                       mPx;
    private float                       mPy;

    private int                mDragPlanPoint;
    private float                mDragStartedX;
    private float                mDragStartedY;
    
    /*
     * Shadow length 
     */
    private static final int SHADOW = 4;
    
    /*
     *  Copy the existing paint to a new paint so we don't mess it up
     */
    private Paint mRunwayPaint;
    private Paint mTextPaintShadow;
    private Paint mShadowPaint;
    private Paint mDistanceRingPaint;
    private Rect mTextSize;
    private RectF mShadowBox;

    /*
     * Text on screen color
     */
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    
    private static final float MOVEMENT_THRESHOLD = 32.f;
    
    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

    // Instantaneous vertical speed in feet per minute
    double mVSI;

    // Handler for the top two lines of status information
    InfoLines mInfoLines;
    
    Point mDownFocusPoint;
    int mTouchSlopSquare;
    boolean mDoCallbackWhenDone;
    LongTouchDestination mLongTouchDestination;

    /**
     * @param context
     */
    private void setup(Context context) {
        
        /*
         * Set up all graphics.
         */
        mContext = context;
        mPan = new Pan();
        mScale = new Scale();
        mOrigin = new Origin();
        mMovement = new Movement();
        mErrorStatus = null;
        mThreshold = 0;
        mOnChart = null;
        mTrackUp = false;
        mElev = -1;
        mMacro = 1;
        mDragPlanPoint = -1;
        mImageDataSource = null;
        mGpsParams = new GpsParams(null);
        mVSIParams = new GpsParams(null);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPointProjection = null;
        mDraw = false;
        
        mPref = new Preferences(context);
        
        mFace = Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf");
        mPaint.setTypeface(mFace);
        mPaint.setTextSize(getResources().getDimension(R.dimen.TextSize));
        
        
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(mFace);
        mTextPaint.setTextSize(R.dimen.TextSize);
        
        mPx = 1;
        mPy = 1;
        
        /*
         * Set up the paint for the distance rings as much as possible here
         */
        mDistanceRingPaint = new Paint();
        mDistanceRingPaint.setAntiAlias(true);
        mDistanceRingPaint.setTextSize(getResources().getDimension(R.dimen.distanceRingNumberTextSize));
        
        /*
         * Set up the paint for the runways as much as possible here
         */
        mRunwayPaint = new Paint(mPaint);
        mRunwayPaint.setTextSize(getResources().getDimension(R.dimen.runwayNumberTextSize));

        mTextPaintShadow = new Paint();
        mTextPaintShadow.setTypeface(mFace);
        mTextPaintShadow.setAntiAlias(true);
        mTextPaintShadow.setColor(TEXT_COLOR);
        mTextPaintShadow.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
        mTextPaintShadow.setStyle(Paint.Style.FILL);
        
        mShadowPaint = new Paint(mTextPaintShadow);
        mShadowPaint.setShadowLayer(0, 0, 0, 0);
        mShadowPaint.setAlpha(0x7f);
        mShadowPaint.setStyle(Style.FILL);
        
        mTileDrawTask = new TileDrawTask();
        mTileDrawThread = new Thread(mTileDrawTask);
        mTileDrawThread.start();

        mTextSize = new Rect();
        mShadowBox = new RectF(mTextSize);

        setOnTouchListener(this);
        mAirplaneBitmap = new BitmapHolder(context, mPref.isHelicopter() ? R.drawable.heli : R.drawable.plane);
        mAirplaneOtherBitmap = new BitmapHolder(context, R.drawable.planeother);
        mLineBitmap = new BitmapHolder(context, R.drawable.line);
        mLineHeadingBitmap = new BitmapHolder(context, R.drawable.line_heading);
        mRunwayBitmap = new BitmapHolder(context, R.drawable.runway_extension);
        mObstacleBitmap = new BitmapHolder(context, R.drawable.obstacle);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        // We're going to give the user twice the slop as normal
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop() * 2;
        mTouchSlopSquare = touchSlop * touchSlop;
        mDoCallbackWhenDone = false;
             
        mDipToPix = Helper.getDpiToPix(context);
        
        mInfoLines = new InfoLines(this);
    }
    
    /**
     * 
     */
    private void updateCoordinates() {
        mOrigin.update(mGpsParams, mScale, mPan,
                mMovement.getLongitudePerPixel(), mMovement.getLatitudePerPixel(),
                getWidth(), getHeight()); 
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public LocationView(Context context) {
        super(context);
        setup(context);
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public LocationView(Context context, AttributeSet aset) {
        super(context, aset);
        setup(context);
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public LocationView(Context context, AttributeSet aset, int arg) {
        super(context, aset, arg);
        setup(context);
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {
        drawMap(canvas);
    }
       
    /**
     * 
     * @param x
     * @param y
     * @param finish
     */
    private void rubberBand(float x, float y, boolean finish) {
        
        String airport = null;
        
        // Rubberbanding
        if(mDragPlanPoint >= 0 && mService.getPlan() != null) {
            
            // Threshold for movement
            float movementx = Math.abs(x - mDragStartedX);
            float movementy = Math.abs(y - mDragStartedY);
            if((movementx > MOVEMENT_THRESHOLD * mDipToPix) 
                    || (movementy > MOVEMENT_THRESHOLD * mDipToPix)) {
                /*
                 * Do something to plan
                 * This is the new location
                 */
                double lon = mOrigin.getLongitudeOf(x);
                double lat = mOrigin.getLatitudeOf(y);
                
                mService.getPlan().replaceDestination(mService, mPref, mDragPlanPoint, lon, lat, finish);
               
                // This will not snap again
                mDragStartedX = -1000;
                mDragStartedY = -1000; 
            }
        }
    }
    
    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        boolean bPassToGestureDetector = true;
        if(e.getAction() == MotionEvent.ACTION_UP) {

            /**
             * Rubberbanding
             */
            rubberBand(e.getX(), e.getY(), true);
            
            /*
             * Drag stops for rubber band
             */
            mDragPlanPoint = -1;
            
            /*
             * Do not draw point. Only when long press and down.
             */
             mPointProjection = null;

            /*
             * Now that we have moved passed the macro level, re-query for new tiles.
             * Do not query repeatedly hence check for mFactor = 1
             */
            if(mMacro != mScale.getMacroFactor()) {
                dbquery(true);
            }
        }
        else if (e.getAction() == MotionEvent.ACTION_DOWN) {

            if(mService != null) {
                /*
                 * Find if this is close to a plan point. Do rubber banding if true
                 */
                if(mService.getPlan() != null && mDragPlanPoint < 0) {
                    double lon = mOrigin.getLongitudeOf(e.getX());
                    double lat = mOrigin.getLatitudeOf(e.getY());
                    mDragPlanPoint = mService.getPlan().findClosePointId(lon, lat);
                    mDragStartedX = e.getX();
                    mDragStartedY = e.getY();
                }
                
            }

            mGestureCallBack.gestureCallBack(GestureInterface.TOUCH, (LongTouchDestination)null);
            
            // Remember this point so we can make sure we move far enough before losing the long press
            mDoCallbackWhenDone = false;
            mDownFocusPoint = getFocusPoint(e);
            startClosestAirportTask(e.getX(), e.getY());
        }
        else if(e.getAction() == MotionEvent.ACTION_MOVE) {
            if(mDownFocusPoint != null) {
        
                Point fp = getFocusPoint(e);
                final int deltaX = fp.x - mDownFocusPoint.x;
                final int deltaY = fp.y - mDownFocusPoint.y;
                int distanceSquare = (deltaX * deltaX) + (deltaY * deltaY);
                bPassToGestureDetector = distanceSquare > mTouchSlopSquare;
                
            }
            // Rubberbanding, intermediate
            rubberBand(e.getX(), e.getY(), false);
        }
        
        if(bPassToGestureDetector) {
            // Once we break out of the square or stop the long press, keep sending
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_UP) {
                mDownFocusPoint = null;
                mPointProjection = null;
                if(mClosestTask != null) {
                    mClosestTask.cancel(true);
                }
            }
            mGestureDetector.onTouchEvent(e);
        }
        return mMultiTouchC.onTouchEvent(e);
    }
    
    /**
     * 
     * @param e
     * @return
     */
    Point getFocusPoint(MotionEvent e) {
        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = e.getPointerCount();
        for (int i = 0; i < count; i++) {
            sumX += e.getX(i);
            sumY += e.getY(i);
        }
        final int div = count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;
        
        Point p = new Point();
        p.set((int)focusX, (int)focusY);
        return p;
    }


    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getDraggableObjectAtPoint(com.ds.avare.MultiTouchController.PointInfo)
     */
    public Object getDraggableObjectAtPoint(PointInfo pt) {
        return this;
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
             * Do not move on drag
             */
            if(mDragPlanPoint >= 0) {
                return true;                
            }
            
            /*
             * Do not move on multitouch
             */
            if(mDraw && (!mTrackUp) && mService != null) {
                float x = mCurrTouchPoint.getX();
                float y = mCurrTouchPoint.getY();
                /*
                 * Threshold the drawing so we do not generate too many points
                 */
                mService.getDraw().addPoint(x, y, mOrigin);
                return true;
            }

            /*
             * TODO: track up pan is problematic
             * 
             */
            if(mPan.setMove(
                            newObjPosAndScale.getXOff(),
                            newObjPosAndScale.getYOff())) {
                /*
                 * Query when we have moved one tile. This will happen in background.
                 */
                dbquery(true);
            }
        }
        else {
            
            // Zooming does not change drag
            mDragPlanPoint = -1;
            
            /*
             * on double touch find distance and bearing between two points.
             */
            if(!mTrackUp) {
                if(mPointProjection == null) {
                    double x0 = mCurrTouchPoint.getXs()[0];
                    double y0 = mCurrTouchPoint.getYs()[0];
                    double x1 = mCurrTouchPoint.getXs()[1];
                    double y1 = mCurrTouchPoint.getYs()[1];
    
                    double lon0 = mOrigin.getLongitudeOf(x0);
                    double lat0 = mOrigin.getLatitudeOf(y0);
                    double lon1 = mOrigin.getLongitudeOf(x1);
                    double lat1 = mOrigin.getLatitudeOf(y1);
                    mPointProjection = new Projection(lon0, lat0, lon1, lat1);
                }
            }

            /*
             * Clamp scaling.
             */
            
            mScale.setScaleFactor(newObjPosAndScale.getScale());
        }
        updateCoordinates();
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

    /**
     * 
     * @param force
     */
    private void dbquery(boolean force) {

        if(mService == null) {
            return;
        }
        
        if(mImageDataSource == null) {
            return;
        }
        
        if(null == mService) {
            return;                
        }

        if(!force) {
            double offsets[] = new double[2];
            double p[] = new double[2];
                                        
            if(mImageDataSource.isWithin(mGpsParams.getLongitude(), 
                    mGpsParams.getLatitude(), offsets, p)) {
                /*
                 * We are within same tile no need for query.
                 */
                mMovement = new Movement(offsets, p);
                postInvalidate();
                return;
            }
        }

        /*
         * Find
         */
        mTileDrawTask.lat = mGpsParams.getLatitude();
        mTileDrawTask.lon = mGpsParams.getLongitude();
        mTileDrawThread.interrupt();
    }

    /**
     * This function will rotate and move a bitmap to a given lon/lat on screen
     * @param b
     * @param angle
     * @param lon
     * @param lat
     * @param div Shift the image half way up so it could be centered on y axis
     */
    private void rotateBitmapIntoPlace(BitmapHolder b, float angle, double lon, double lat, boolean div) {
        float x = (float)mOrigin.getOffsetX(lon);
        float y = (float)mOrigin.getOffsetY(lat);                        
                            
        b.getTransform().setTranslate(
                x - b.getWidth() / 2,
                y - (div ? b.getHeight() / 2 : b.getHeight()));
        
        b.getTransform().postRotate(angle, x, y);   
    }
    

    /**
     *
     * @param canvas
     */
    private void drawTiles(Canvas canvas) {
        mPaint.setShadowLayer(0, 0, 0, 0);
  
        if(null != mService) {
            int empty = 0;
            int tn = mService.getTiles().getTilesNum();
            
            for(int tilen = 0; tilen < tn; tilen++) {
                
                BitmapHolder tile = mService.getTiles().getTile(tilen);
                /*
                 * Scale, then move under the plane which is at center
                 */
                boolean nochart = false;
                if(null == tile) {
                    nochart = true;
                }
                else if(null == tile.getBitmap()) {
                    nochart = true;
                }
                
                if(nochart) {
                    continue;
                }

                /*
                 * Find how many empty tiles
                 */
                if(!tile.getFound()) {
                    empty++;
                }

                if(mPref.isNightMode() && (mPref.getChartType().equals("3") || mPref.getChartType().equals("4"))) {
                    /*
                     * IFR charts invert color at night
                     */
                    Helper.invertCanvasColors(mPaint);
                }
                else if(mPref.getChartType().equals("5")) {
                    /*
                     * Terrain
                     */
                    Helper.setThreshold(mPaint, mThreshold);
                }
                
                /*
                 * Pretty straightforward. Pan and draw individual tiles.
                 */
                tile.getTransform().setScale(mScale.getScaleFactor(), mScale.getScaleCorrected());
                tile.getTransform().postTranslate(
                        getWidth()  / 2.f
                        - BitmapHolder.WIDTH  / 2.f * mScale.getScaleFactor() 
                        + ((tilen % mService.getTiles().getXTilesNum()) * BitmapHolder.WIDTH - BitmapHolder.WIDTH * (int)(mService.getTiles().getXTilesNum() / 2)) * mScale.getScaleFactor()
                        + mPan.getMoveX() * mScale.getScaleFactor()
                        + mPan.getTileMoveX() * BitmapHolder.WIDTH * mScale.getScaleFactor()
                        - (float)mMovement.getOffsetLongitude() * mScale.getScaleFactor(),
                        
                        getHeight() / 2.f 
                        - BitmapHolder.HEIGHT / 2.f * mScale.getScaleCorrected()  
                        + mPan.getMoveY() * mScale.getScaleCorrected()
                        + ((tilen / mService.getTiles().getXTilesNum()) * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT * (int)(mService.getTiles().getYTilesNum() / 2)) * mScale.getScaleCorrected() 
                        + mPan.getTileMoveY() * BitmapHolder.HEIGHT * mScale.getScaleCorrected()
                        - (float)mMovement.getOffsetLatitude() * mScale.getScaleCorrected());
                
                Bitmap b = tile.getBitmap();
                if(null != b) {
                    canvas.drawBitmap(b, tile.getTransform(), mPaint);
                }
                
                Helper.restoreCanvasColors(mPaint);
            }
            
            /*
             * If nothing on screen, write a not found message
             */
            if(empty >= tn) {
                mDistanceRingPaint.setColor(Color.WHITE);
                drawShadowedText(canvas, mDistanceRingPaint,
                        mContext.getString(R.string.MissingMaps), 
                        Color.RED, getWidth() / 2, getHeight() / 2);
            }
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawTFR(Canvas canvas) {
        mPaint.setColor(Color.RED);
        mPaint.setShadowLayer(0, 0, 0, 0);
        
        /*
         * Draw TFRs, TFR
         */            
        LinkedList<TFRShape> shapes = null;
        if(null != mService) {
            shapes = mService.getTFRShapes();
        }
        if(null != shapes && null == mPointProjection) {
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(3 * mDipToPix);
            mPaint.setShadowLayer(0, 0, 0, 0);
            for(int shape = 0; shape < shapes.size(); shape++) {
                shapes.get(shape).drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace, mPref.isNightMode());
            }
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawAirSigMet(Canvas canvas) {
        mPaint.setShadowLayer(0, 0, 0, 0);
        
        /*
         * Draw TFRs, TFR
         */            
        List<AirSigMet> mets = null;
        if((null != mService) && (!mPref.useAdsbWeather())) {
            mets = mService.getInternetWeatherCache().getAirSigMet();
        }
        
        if(null != mets && null == mPointProjection) {
            mPaint.setStrokeWidth(2 * mDipToPix); 
            mPaint.setShadowLayer(0, 0, 0, 0);
            String typeArray[] = mContext.getResources().getStringArray(R.array.AirSig);
            int colorArray[] = mContext.getResources().getIntArray(R.array.AirSigColor);
            String storeType = mPref.getAirSigMetType();
            for(int i = 0; i < mets.size(); i++) {
                AirSigMet met = mets.get(i);
                int color = 0;
                
                String type = met.hazard + " " + met.reportType;
                if(storeType.equals("ALL")) {
                    /*
                     * All draw all shapes
                     */
                }
                else if(!storeType.equals(type)) {
                    /*
                     * This should not be drawn.
                     */
                    continue;
                }
                
                for(int j = 0; j < typeArray.length; j++) {
                    if(typeArray[j].equals(type)) {
                        color = colorArray[j];
                        break;
                    }
                }
                
                /*
                 * Now draw
                 */
                if(met.shape != null && color != 0) {
                    mPaint.setColor(color);
                    met.shape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace, mPref.isNightMode());
                }
            }
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawRadar(Canvas canvas) {
        if(mService == null || (0 == mPref.showRadar()) || null != mPointProjection) {
            return;
        }
        
        /*
         * Radar is way too old.
         */
        if(mService.getRadar().isOld()) {
            return;
        }
        
        /*
         * If using ADSB, then dont show
         */
        if(mPref.useAdsbWeather()) {
            return;
        }

        mPaint.setAlpha(mPref.showRadar());
        mService.getRadar().draw(canvas, mPaint, mOrigin, mScale, mPx, mPy);
        mPaint.setAlpha(255);

    }

    /**
     * 
     * @param canvas
     */
    private void drawNexrad(Canvas canvas) {
        if(mService == null || 0 == mPref.showRadar()) {
            return;
        }
        
        /*
         * Get nexrad bitmaps to draw.
         */
        SparseArray<NexradBitmap> bitmaps = null;
        if(mScale.getMacroFactor() > 4) {
            if(!mService.getAdsbWeather().getNexradConus().isOld()) {
                /*
                 * CONUS for larger scales.
                 */
                bitmaps = mService.getAdsbWeather().getNexradConus().getImages();                
            }
        }
        else {
            if(!mService.getAdsbWeather().getNexrad().isOld()) {
                bitmaps = mService.getAdsbWeather().getNexrad().getImages();
            }
        }

        if(null == bitmaps || null != mPointProjection || (!mPref.useAdsbWeather())) {
            return;
        }

        for(int i = 0; i < bitmaps.size(); i++) {
            int key = bitmaps.keyAt(i);
            NexradBitmap b = bitmaps.get(key);
            BitmapHolder bitmap = b.getBitmap();
            if(null != bitmap) {          
                /*
                 * draw them scaled.
                 */
                float scalex = (float)(b.getScaleX() / mPx);
                float scaley = (float)(b.getScaleY() / mPy);
                float x = (float)mOrigin.getOffsetX(b.getLonTopLeft());
                float y = (float)mOrigin.getOffsetY(b.getLatTopLeft());
                bitmap.getTransform().setScale(scalex * mScale.getScaleFactor(), 
                        scaley * mScale.getScaleCorrected());
                bitmap.getTransform().postTranslate(x, y);
                if(bitmap.getBitmap() != null) {
                    mPaint.setAlpha(mPref.showRadar());
                    canvas.drawBitmap(bitmap.getBitmap(), bitmap.getTransform(), mPaint);
                    mPaint.setAlpha(255);
                }
            }
        }
    }


    /**
     * 
     * @param canvas
     */
    private void drawTraffic(Canvas canvas) {
        if(mService == null) {
            return;
        }
        
        /*
         * Get nexrad bitmaps to draw.
         */
        SparseArray<Traffic> traffic = mService.getTrafficCache().getTraffic();

        if((!mPref.showAdsbTraffic()) || (null == traffic) || (null != mPointProjection)) {
            return;
        }

        for(int i = 0; i < traffic.size(); i++) {
            int key = traffic.keyAt(i);
            Traffic t = traffic.get(key);
            if(t.isOld()) {
                traffic.delete(key);
                continue;
            }
            
            if(null != mAirplaneOtherBitmap) {
                rotateBitmapIntoPlace(mAirplaneOtherBitmap, t.mHeading,
                        t.mLon, t.mLat, true);
                mDistanceRingPaint.setColor(Color.WHITE);
                canvas.drawBitmap(mAirplaneOtherBitmap.getBitmap(), mAirplaneOtherBitmap.getTransform(), mPaint);
                /*
                 * Make traffic line and info
                 */
                float x = (float)mOrigin.getOffsetX(t.mLon);
                float y = (float)mOrigin.getOffsetY(t.mLat);
                drawShadowedText(canvas, mDistanceRingPaint,
                        t.mAltitude + "'", Color.DKGRAY, x, y);

            }
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawTrack(Canvas canvas) {
        if(null == mService) {
            return;
        }

        if(mService.getDestination() != null && null == mPointProjection) {
            if(mPref.isTrackEnabled()) {
                mPaint.setColor(Color.MAGENTA);
                mPaint.setStrokeWidth(5 * mDipToPix);
                mPaint.setAlpha(162);
                if(mService.getDestination().isFound() && !mService.getPlan().isActive()  && (!mPref.isSimulationMode())) {
                    mService.getDestination().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace, mPref.isNightMode());
                }
                else if (mService.getPlan().isActive()) {
                    mService.getPlan().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace, mPref.isNightMode());                    
                }
            }
            if(!mPref.isSimulationMode()) {
                /*
                 * Draw actual track
                 */
                if(null != mLineBitmap && mGpsParams != null) {
                    rotateBitmapIntoPlace(mLineBitmap, (float)mService.getDestination().getBearing(),
                            mGpsParams.getLongitude(), mGpsParams.getLatitude(), false);
                    canvas.drawBitmap(mLineBitmap.getBitmap(), mLineBitmap.getTransform(), mPaint);
                }
                /*
                 * Draw actual heading
                 */
                if(null != mLineHeadingBitmap && mGpsParams != null) {
                    rotateBitmapIntoPlace(mLineHeadingBitmap, (float)mGpsParams.getBearing(),
                            mGpsParams.getLongitude(), mGpsParams.getLatitude(), false);
                    canvas.drawBitmap(mLineHeadingBitmap.getBitmap(), mLineHeadingBitmap.getTransform(), mPaint);
                }
            }
        }
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
        mService.getDraw().drawShape(canvas, mPaint, mOrigin);
        
    }


    /**
     * 
     * @param canvas
     */
    private void drawObstacles(Canvas canvas) {
        if(mPref.shouldShowObstacles()) {
            if((mObstacles != null) && (null == mPointProjection)) {
                mPaint.setShadowLayer(0, 0, 0, 0);
                for (Obstacle o : mObstacles) {
                    rotateBitmapIntoPlace(mObstacleBitmap, 0, o.getLongitude(), o.getLatitude(), false);
                    canvas.drawBitmap(mObstacleBitmap.getBitmap(), mObstacleBitmap.getTransform(), mPaint);
                }
            }
        }
    }
    
    /**
     * 
     * @param canvas
     */
    private void drawAircraft(Canvas canvas) {
        mPaint.setShadowLayer(0, 0, 0, 0);
        mPaint.setColor(Color.WHITE);

        if(null != mAirplaneBitmap && null == mPointProjection) {
            
            /*
             * Rotate and move to a panned location
             */
            rotateBitmapIntoPlace(mAirplaneBitmap, (float)mGpsParams.getBearing(),
                    mGpsParams.getLongitude(), mGpsParams.getLatitude(), true);
            canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawRunways(Canvas canvas) {
        if (!mPref.shouldExtendRunways()) {
            return;
        }
        if (null == mService) {
            return;
        }
        if (null != mRunwayBitmap && null != mService.getDestination()
                && null == mPointProjection) {
            LinkedList<Runway> runways = mService.getDestination().getRunways();
            if (runways != null) {
                int xfactor;
                int yfactor;

                /*
                 * For all runways
                 */
                for (Runway r : runways) {
                    float heading = r.getTrue();
                    if (Runway.INVALID == heading) {
                        continue;
                    }
                    /*
                     * Get lat/lon of the runway. If either one is invalid, use
                     * airport lon/lat
                     */
                    double lon = r.getLongitude();
                    double lat = r.getLatitude();
                    if (Runway.INVALID == lon || Runway.INVALID == lat) {
                        lon = mService.getDestination().getLocation()
                            .getLongitude();
                        lat = mService.getDestination().getLocation()
                            .getLatitude();
                    }
                    /*
                     * Rotate and position the runway bitmap
                     */
                    rotateBitmapIntoPlace(mRunwayBitmap, heading, lon, lat,
                            false);
                    /*
                     * Draw it.
                     */
                    canvas.drawBitmap(mRunwayBitmap.getBitmap(),
                            mRunwayBitmap.getTransform(), mRunwayPaint);
                    /*
                     * Get the canvas x/y coordinates of the runway itself
                     */
                    float x = (float)mOrigin.getOffsetX(lon);
                    float y = (float)mOrigin.getOffsetY(lat);
                    /*
                     * The runway number, i.e. What's painted on the runway
                     */
                    String num = r.getNumber();
                    /*
                     * If there are parallel runways, offset their text so it
                     * does not overlap
                     */
                    xfactor = yfactor = (int)(mRunwayBitmap.getHeight() + mRunwayPaint.getTextSize()/2);
                    
                    if (num.contains("C")) {
                        xfactor = yfactor = xfactor * 3 / 4;
                    }
                    else if (num.contains("L")) {
                        xfactor = yfactor = xfactor / 2;
                    }
                    /*
                     * Determine canvas coordinates of where to draw the runway
                     * numbers with simple rotation math.
                     */
                    float runwayNumberCoordinatesX = x + xfactor
                            * (float) Math.sin(Math.toRadians(heading - 180));
                    float runwayNumberCoordinatesY = y - yfactor
                            * (float) Math.cos(Math.toRadians(heading - 180));
                    mRunwayPaint.setStyle(Style.FILL);
                    mRunwayPaint.setColor(Color.BLUE);
                    mRunwayPaint.setAlpha(162);
                    mRunwayPaint.setShadowLayer(0, 0, 0, 0);
                    mRunwayPaint.setStrokeWidth(4 * mDipToPix);
                    /*
                     * Get a vector perpendicular to the vector of the runway
                     * heading bitmap
                     */
                    float vXP = -(runwayNumberCoordinatesY - y);
                    float vYP = (runwayNumberCoordinatesX - x);
                    /*
                     * Reverse the vector of the pattern line if right traffic
                     * is indicated for this runway
                     */
                    if (r.getPattern().equalsIgnoreCase("Right")) {
                        vXP = -(vXP);
                        vYP = -(vYP);
                    }
                    /*
                     * Draw the base leg of the pattern
                     */
                    canvas.drawLine(runwayNumberCoordinatesX,
                            runwayNumberCoordinatesY, runwayNumberCoordinatesX
                            + vXP / 3, runwayNumberCoordinatesY + vYP
                            / 3, mRunwayPaint);
                    /*
                     * If in track-up mode, rotate canvas around screen x/y of
                     * where we want to draw runway numbers in opposite
                     * direction to bearing so they appear upright
                     */
                    if (mTrackUp && (mGpsParams != null)) {
                        canvas.save();
                        canvas.rotate((int) mGpsParams.getBearing(),
                            runwayNumberCoordinatesX,
                            runwayNumberCoordinatesY);
                    }
                    /*
                     * Draw the text so it's centered within the shadow
                     * rectangle, which is itself centered at the end of the
                     * extended runway centerline
                     */
                    
                    mRunwayPaint.setStyle(Style.FILL);
                    mRunwayPaint.setColor(Color.WHITE);
                    mRunwayPaint.setAlpha(255);
                    mRunwayPaint.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
                    drawShadowedText(canvas, mRunwayPaint, num, Color.DKGRAY,
                            runwayNumberCoordinatesX, runwayNumberCoordinatesY);
                    if (mTrackUp) {
                        canvas.restore();
                    }
                }
            }
        }
    }

    /**
     * Draws concentric circles around the current aircraft position showing distance.
     * author: rwalker
     * 
     * @param canvas upon which to draw the circles
     */
    private void drawDistanceRings(Canvas canvas) {
        /*
         * Some pre-conditions that would prevent us from drawing anything
         */
        if (mService == null) {
            return;
        }
        /*
         * Calculate the size of distance and speed rings
         */
        double currentSpeed = mGpsParams.getSpeed();
        DistanceRings.calculateRings(mContext, mPref, mScale, mMovement,
                currentSpeed);
        float ringR[] = DistanceRings.getRings();
        /*
         * Get our current position. That will be the center of all the rings
         */
        float x = (float) (mOrigin.getOffsetX(mGpsParams.getLongitude()));
        float y = (float) (mOrigin.getOffsetY(mGpsParams.getLatitude()));
        double bearing = mGpsParams.getBearing();	/* What direction are we headed */
        if(mTrackUp) {		// If our direction is always to the top, then set
        	bearing = 0;	// our bearing to due up as well
        }

        /*
         * If the user wants the distance rings display, now is the time
         */
        if ((mPref.getDistanceRingType() != 0) && (null == mPointProjection)) {
            /*
             * Set the paint accordingly
             */
            mDistanceRingPaint.setStrokeWidth(3 * mDipToPix);
            mDistanceRingPaint.setShadowLayer(0, 0, 0, 0);
            mDistanceRingPaint.setColor(DistanceRings.COLOR_DISTANCE_RING);
            mDistanceRingPaint.setStyle(Style.STROKE);
            mDistanceRingPaint.setAlpha(0x7F);
            /*
             * Draw the 3 distance circles now
             */
            canvas.drawCircle(x, y, ringR[DistanceRings.RING_INNER],
                    mDistanceRingPaint);
            canvas.drawCircle(x, y, ringR[DistanceRings.RING_MIDDLE],
                    mDistanceRingPaint);
            canvas.drawCircle(x, y, ringR[DistanceRings.RING_OUTER],
                    mDistanceRingPaint);
            /*
             * Restore some paint settings back to what they were so as not to
             * mess things up
             */
            mDistanceRingPaint.setAlpha(0xFF);
            mDistanceRingPaint.setStyle(Style.FILL);
            mDistanceRingPaint.setColor(Color.CYAN);
            
            /*
             * Draw the corresponding text
             */
            String text[] = DistanceRings.getRingsText();

            float adjX = (float) Math.sin((bearing - 10) * Math.PI / 180);	// Distance ring numbers, offset from
            float adjY = (float) Math.cos((bearing - 10) * Math.PI / 180);	// the course line for readability

            drawShadowedText(canvas, mDistanceRingPaint,
                    text[DistanceRings.RING_INNER], Color.DKGRAY,
                    x + ringR[DistanceRings.RING_INNER] * adjX, 
                    y - ringR[DistanceRings.RING_INNER] * adjY);
            drawShadowedText(canvas, mDistanceRingPaint,
                    text[DistanceRings.RING_MIDDLE], Color.DKGRAY,
                    x + ringR[DistanceRings.RING_MIDDLE] * adjX, 
                    y - ringR[DistanceRings.RING_MIDDLE] * adjY);
            drawShadowedText(canvas, mDistanceRingPaint,
                    text[DistanceRings.RING_OUTER], Color.DKGRAY,
                    x + ringR[DistanceRings.RING_OUTER] * adjX, 
                    y - ringR[DistanceRings.RING_OUTER] * adjY);
    
        }
        /*
         * Draw our "speed ring" if one was calculated for us
         */
        if ((ringR[DistanceRings.RING_SPEED] != 0)
                && (null == mPointProjection)) {

        	float adjX = (float) Math.sin((bearing + 10) * Math.PI / 180);	// So the speed ring number does
            float adjY = (float) Math.cos((bearing + 10) * Math.PI / 180);	// not overlap the distance ring

            mDistanceRingPaint.setStyle(Style.STROKE);
            mDistanceRingPaint.setColor(DistanceRings.COLOR_SPEED_RING);
            canvas.drawCircle(x, y, ringR[DistanceRings.RING_SPEED],
                    mDistanceRingPaint);

            
            mDistanceRingPaint.setAlpha(0xFF);
            mDistanceRingPaint.setStyle(Style.FILL);
            mDistanceRingPaint.setColor(Color.GREEN);

            drawShadowedText(canvas, mDistanceRingPaint, 
            		String.format("%d", mPref.getTimerRingSize()), Color.DKGRAY, 
            		x + ringR[DistanceRings.RING_SPEED] * adjX, 
            		y - ringR[DistanceRings.RING_SPEED] * adjY);
        }
    }

    /**
     * Draw the tracks to show our previous positions. If tracking is enabled, there is
     * a linked list of gps coordinates attached to this view with the most recent one at the end
     * of that list. Start at the end value to begin the drawing and as soon as we find one that is 
     * not in the range of this display, we can assume that we're done.
     * @param canvas
     */
    private void drawTracks(Canvas canvas) {
        /*
         * Some pre-conditions that would prevent us from drawing anything
         */
        if(mService == null) {
            return;
        }
        if(mPref.shouldDrawTracks() && (null == mPointProjection)) {
                
            /*
             *  Set the brush color and width
             */
            mPaint.setColor(Color.DKGRAY);
            mPaint.setStrokeWidth(6 * mDipToPix);
            mPaint.setStyle(Paint.Style.FILL);

            mService.getKMLRecorder().getShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace, mPref.isNightMode());
        }
    }

    /**
     * Display the text in the indicated paint with a shadow'd background. This aids in readability.
     * 
     * @param canvas where to draw
     * @param text what to display
     * @param shadowColor is the color of the shadow of course
     * @param x center position of the text on the canvas
     * @param y top edge of text on the canvas
     */
    private void drawShadowedText(Canvas canvas, Paint paint, String text, int shadowColor, float x, float y) {

        final int XMARGIN = (int) (5 * mDipToPix);
        final int YMARGIN = (int) (5 * mDipToPix);
        final int SHADOWRECTRADIUS = (int) (5 * mDipToPix);
        paint.getTextBounds(text, 0, text.length(), mTextSize);
        
        mShadowBox.bottom = mTextSize.bottom + YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.top    = mTextSize.top - YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.left   = mTextSize.left - XMARGIN + x  - (mTextSize.right / 2);
        mShadowBox.right  = mTextSize.right + XMARGIN + x  - (mTextSize.right / 2);

        
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setAlpha(0x80);
        canvas.drawRoundRect(mShadowBox, SHADOWRECTRADIUS, SHADOWRECTRADIUS, mShadowPaint);
        canvas.drawText(text,  x - (mTextSize.right / 2), y - (mTextSize.top / 2), paint);
    }

    /***
     * Draw the course deviation indicator if we have a destination set
     * @param canvas what to draw the data upon
     */
    private void drawCDI(Canvas canvas)
    {
        if(mService != null && mPointProjection == null && mErrorStatus == null) {
        	if(mPref.getShowCDI()) {
	        	Destination dest = mService.getDestination();
	        	if(dest != null) {
	        		mService.getCDI().drawCDI(canvas, getWidth(), getHeight());
	        	}
        	}
        }
    	
    }
    
    /***
     * Draw the vertical approach slope indicator if we have a destination set
     * @param canvas what to draw the data upon
     */
    private void drawVASI(Canvas canvas)
    {
        if(mService != null && mPointProjection == null && mErrorStatus == null) {
        	if(mPref.getShowCDI()) {
	        	Destination dest = mService.getDestination();
	        	if(dest != null) {
	        		mService.getVNAV().drawVNAV(canvas, getWidth(), getHeight(), dest);
	        	}
        	}
        }
    	
    }

    /***
     * Draw the edge distance markers if configured to do so
     * @param canvas what to draw them on
     */
    private void drawEdgeMarkers(Canvas canvas) {
        if(mService != null && mPointProjection == null && mPref.shouldShowEdgeTape()) {
	        int x = (int)(mOrigin.getOffsetX(mGpsParams.getLongitude()));
	        int y = (int)(mOrigin.getOffsetY(mGpsParams.getLatitude()));
	        float pixPerNm = mMovement.getNMPerLatitude(mScale);
	      	EdgeDistanceTape.draw(canvas, mPaint, mScale, pixPerNm, x, y, 
	      			(int) mInfoLines.getHeight(), getWidth(), getHeight());
        }
    }
    
    /**
     * @param canvas
     * Does pretty much all drawing on screen
     */
    private void drawMap(Canvas canvas) {

        
        if(mTrackUp && (mGpsParams != null)) {
            canvas.save();
            /*
             * Rotate around current position
             */
            float x = (float)mOrigin.getOffsetX(mGpsParams.getLongitude());
            float y = (float)mOrigin.getOffsetY(mGpsParams.getLatitude());
            canvas.rotate(-(int)mGpsParams.getBearing(), x, y);
        }
        drawTiles(canvas);
        drawNexrad(canvas);
        drawRadar(canvas);
        drawDrawing(canvas);
        drawRunways(canvas);
        drawTraffic(canvas);
        drawTFR(canvas);
        drawAirSigMet(canvas);
        drawTracks(canvas);
        drawTrack(canvas);
        drawObstacles(canvas);
        drawRunways(canvas);
        drawAircraft(canvas);
        
        if(mTrackUp) {
            canvas.restore();
        }
        
        drawDistanceRings(canvas);
        drawCDI(canvas);
        drawVASI(canvas);
      	mInfoLines.drawCornerTextsDynamic(canvas, mPaint, TEXT_COLOR, TEXT_COLOR_OPPOSITE, SHADOW);
      	drawEdgeMarkers(canvas);
    }    

    /**
     * 
     * @param threshold
     */
    public void updateThreshold(float threshold) {
        mThreshold = threshold;
        invalidate();
    }

    /**
     *
     */
    public void updateDestination() {
        /*
         * Comes from database
         */
        if(null == mService) {
            return;
        }
        if(null != mService.getDestination()) {
            if(mService.getDestination().isFound()) {
                /*
                 * Set pan to zero since we entered new destination
                 * and we want to show it without pan.
                 */
                mPan = new Pan();
                mService.setPan(mPan);
                updateCoordinates();                
            }
        }
    }

    /**
     * 
     */
    public void forceReload() {
        dbquery(true);        
    }
        
    /**
     * @param params
     */
    public void updateParams(GpsParams params) {
    	
        double tdiff = ((double)(params.getTime() - mVSIParams.getTime()) / 1000.0);
    	// Calculate the instantaneous vertical speed in ft/min
    	if(tdiff > 1) {
    		mVSI = ((double)(params.getAltitude() - mVSIParams.getAltitude())) * (60 / tdiff);
    		mVSIParams = params;
    	}
    	
        /*
         * Comes from location manager
         */
        mGpsParams = params;

        updateCoordinates();
        
        /*
         * Database query for new location / pan location.
         */
        dbquery(false);
        
        if(mObstacleTask != null) {
            /*
             * Do not overwhelm
             */
            if(mObstacleTask.getStatus() == AsyncTask.Status.RUNNING) {
                mObstacleTask.cancel(true);
            }
        }
        mObstacleTask = new ObstacleTask();
        mObstacleTask.execute(mGpsParams);
    }

    
    /**
     * @param params
     */
    public void initParams(GpsParams params, StorageService service) {
        /*
         * Comes from storage service. This will do nothing for fresh start,
         * but it will load previous combo on re-activation
         */
        mService = service;
        mMovement = mService.getMovement();
        mImageDataSource = mService.getDBResource();
        if(null == mMovement) {
            mMovement = new Movement();
        }
        mPan = mService.getPan();
        if(null == mPan) {
            mPan = new Pan();
            mService.setPan(mPan);
        }
        if(null != params) {
            mGpsParams = params;
        }
        else if (null != mService.getDestination()) {
            mGpsParams = new GpsParams(mService.getDestination().getLocation());
        }
        else {
            mGpsParams = new GpsParams(null);
        }
        mScale.setScaleAt(mGpsParams.getLatitude());
        dbquery(true);
        postInvalidate();

        // Tell the CDI the paint that we use for display text
        mService.getCDI().setSize(mPaint);
        mService.getVNAV().setSize(mPaint);
        
        // Tell the odometer how to access preferences
        mService.getOdometer().setPref(mPref);
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
     * @author zkhan
     *
     */
    private class TileDrawTask implements Runnable {
        private double offsets[] = new double[2];
        private double p[] = new double[2];
        public double lon;
        public double lat;
        private int     movex;
        private int     movey;
        private String   tileNames[];
        private Tile centerTile;
        private Tile gpsTile;
        public boolean running = true;
        private boolean runAgain = false;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Tile");

            while(running) {
                
                if(!runAgain) {
                    try {
                        Thread.sleep(1000 * 3600);
                    }
                    catch(Exception e) {
                        
                    }
                }
                runAgain = false;
                
                if(null == mService) {
                    continue;
                }
                
                if(mImageDataSource == null) {
                    continue;
                }
                
                /*
                 * Now draw in background
                 */
                int level = mScale.downSample();
                gpsTile = mImageDataSource.findClosest(lon, lat, offsets, p, level);
                
                if(gpsTile == null) {
                    continue;
                }
                
                float factor = (float)mMacro / (float)mScale.getMacroFactor();

                /*
                 * Make a copy of Pan to find next tile set in case this gets stopped, we do not 
                 * destroy our Pan information.
                 */
                Pan pan = new Pan(mPan);
                pan.setMove((float)(mPan.getMoveX() * factor), (float)(mPan.getMoveY() * factor));
                movex = pan.getTileMoveXWithoutTear();
                movey = pan.getTileMoveYWithoutTear();
                
                String newt = gpsTile.getNeighbor(movey, movex);
                centerTile = mImageDataSource.findTile(newt);
                if(null == centerTile) {
                    continue;
                }
    
                /*
                 * Neighboring tiles with center and pan
                 */
                int i = 0;
                tileNames = new String[mService.getTiles().getTilesNum()];
                for(int tiley = -(int)(mService.getTiles().getYTilesNum() / 2) ; 
                        tiley <= (mService.getTiles().getYTilesNum() / 2); tiley++) {
                    for(int tilex = -(int)(mService.getTiles().getXTilesNum() / 2); 
                            tilex <= (mService.getTiles().getXTilesNum() / 2) ; tilex++) {
                        tileNames[i++] = centerTile.getNeighbor(tiley, tilex);
                    }
                }

                /*
                 * Load tiles, draw in UI thread
                 */
                try {
                    mService.getTiles().reload(tileNames);
                }
                catch(Exception e) {
                    /*
                     * We are interrupted for new movement. Try again to load new tiles.
                     */
                    runAgain = true;
                    continue;
                }
                
                /*
                 * UI thread
                 */
                TileUpdate t = new TileUpdate();
                t.movex = movex;
                t.movey = movey;
                t.centerTile = centerTile;
                t.offsets = offsets;
                t.p = p;
                t.factor = factor;
                
                Message m = mHandler.obtainMessage();
                m.obj = t;
                mHandler.sendMessage(m);
            }
        }
    }    

    /**
     * @author zkhan
     *
     */
    private class ClosestAirportTask extends AsyncTask<Object, String, String> {
        private Double lon;
        private Double lat;
        private String text = "";
        private String textMets = "";
        private String sua;
        private String radar;
        private LinkedList<Airep> aireps;
        private LinkedList<String> freq;
        private LinkedList<String> runways;
        private Taf taf;
        private WindsAloft wa;
        private Metar metar;
        private String elev;
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */     
        @Override
        protected String doInBackground(Object... vals) {           
            Thread.currentThread().setName("Closest");
            if(null == mService) {
                return null;
            }

            String airport = null;
            lon = (Double)vals[0];
            lat = (Double)vals[1];
            
            // if the user is moving instead of doing a long press, give them a chance
            // to cancel us before we start doing anything
            try {
                Thread.sleep(200);
            }
            catch(Exception e) {
            }
            
            if(isCancelled())
                return "";
                       
            /*
             * Get TFR text if touched on its top
             */
            LinkedList<TFRShape> shapes = null;
            List<AirSigMet> mets = null;
            if(null != mService) {
                shapes = mService.getTFRShapes();
                if(!mPref.useAdsbWeather()) {
                    mets = mService.getInternetWeatherCache().getAirSigMet();
                }
            }
            if(null != shapes) {
                for(int shape = 0; shape < shapes.size(); shape++) {
                    TFRShape cshape = shapes.get(shape);
                    /*
                     * Set TFR text
                     */
                    String txt = cshape.getTextIfTouched(lon, lat);
                    if(null != txt) {
                        text += txt + "\n--\n";
                    }
                }
            }
            /*
             * Air/sigmets
             */
            if(null != mets) {
                for(int i = 0; i < mets.size(); i++) {
                    MetShape cshape = mets.get(i).shape;
                    if(null != cshape) {
                        /*
                         * Set MET text
                         */
                        String txt = cshape.getTextIfTouched(lon, lat);
                        if(null != txt) {
                            textMets += txt + "\n--\n";
                        }
                    }
                }
            }            

            airport = mService.getDBResource().findClosestAirportID(lon, lat);
            if(isCancelled())
                return "";
            
            if(null == airport) {
                airport = "" + Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
            }
            else {
                freq = mService.getDBResource().findFrequencies(airport);
                if(isCancelled())
                    return "";
            
                taf = mService.getDBResource().getTAF(airport);
                if(isCancelled())
                    return "";
                
                metar = mService.getDBResource().getMETAR(airport);   
                if(isCancelled())
                    return "";
            
                runways = mService.getDBResource().findRunways(airport);
                if(isCancelled())
                    return "";
                
                elev = mService.getDBResource().findElev(airport);
                if(isCancelled())
                    return "";

            }
            
            /*
             * ADSB gets this info from weather cache
             */
            if(!mPref.useAdsbWeather()) {              
                aireps = mService.getDBResource().getAireps(lon, lat);
                if(isCancelled())
                    return "";
                
                wa = mService.getDBResource().getWindsAloft(lon, lat);
                if(isCancelled())
                    return "";
                
                sua = mService.getDBResource().getSua(lon, lat);
                if(isCancelled())
                    return "";
                
                radar = mService.getRadar().getDate();
                if(isCancelled())
                    return "";
            }    
            
            mPointProjection = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon, lat);
            return airport;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String airport) {
            if(null != mGestureCallBack && null != mPointProjection && null != airport) {
                mLongTouchDestination = new LongTouchDestination();
                mLongTouchDestination.airport = airport;
                mLongTouchDestination.info = Math.round(mPointProjection.getDistance()) + Preferences.distanceConversionUnit +
                        "(" + mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()) + ") " +
                        Helper.correctConvertHeading(Math.round(Helper.getMagneticHeading(mPointProjection.getBearing(), mGpsParams.getDeclinition()))) + '\u00B0';
                mLongTouchDestination.chart = mOnChart;

                /*
                 * Clear old weather
                 */
                mService.getAdsbWeather().sweep();

                /*
                 * Do not background ADSB weather as its a RAM opertation and quick,
                 * also avoids concurrent mod exception.
                 */

                if(mPref.useAdsbWeather()) {
                    taf = mService.getAdsbWeather().getTaf(airport);
                    metar = mService.getAdsbWeather().getMETAR(airport);                    
                    aireps = mService.getAdsbWeather().getAireps(lon, lat);
                    wa = mService.getAdsbWeather().getWindsAloft(lon, lat);
                    radar = mService.getAdsbWeather().getNexrad().getDate();
                }
                if(null != aireps) {
                    for(Airep a : aireps) {
                        a.updateTextWithLocation(lon, lat, mGpsParams.getDeclinition());                
                    }
                }
                if(null != wa) {
                    wa.updateStationWithLocation(lon, lat, mGpsParams.getDeclinition());
                }
                mLongTouchDestination.tfr = text;
                mLongTouchDestination.taf = taf;
                mLongTouchDestination.metar = metar;
                mLongTouchDestination.airep = aireps;
                mLongTouchDestination.mets = textMets;
                mLongTouchDestination.wa = wa;
                mLongTouchDestination.freq = freq;
                mLongTouchDestination.sua = sua;
                mLongTouchDestination.radar = radar;
                if(metar != null) {
                    mLongTouchDestination.performance =
                            WeatherHelper.getMetarTime(metar.rawText) + "\n" +
                            mContext.getString(R.string.DensityAltitude) + " " +
                            WeatherHelper.getDensityAltitude(metar.rawText, elev) + "\n" +
                            mContext.getString(R.string.BestRunway) + " " +
                            WeatherHelper.getBestRunway(metar.rawText, runways);
                }
                
                // If the long press event has already occurred, we need to do the gesture callback here
                if(mDoCallbackWhenDone) {
                    mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, mLongTouchDestination);
                }
            }
            invalidate();
        }
        
    }

    
    /**
     * @author zkhan
     * Find obstacles
     */
    private class ObstacleTask extends AsyncTask<Object, Void, Object> {
        
        private LinkedList<Obstacle> obs = null;
        double elev = -1;
        
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Object... vals) {
            Thread.currentThread().setName("Obstacle");

            double lon = ((GpsParams)vals[0]).getLongitude();
            double lat = ((GpsParams)vals[0]).getLatitude();
            double alt = ((GpsParams)vals[0]).getAltitude();
            
            if(null != mImageDataSource) {
                /*
                 * Find obstacles in background as well
                 */
                if(mPref.shouldShowObstacles()) {
                    obs = mImageDataSource.findObstacles(lon, lat, (int)alt);
                }
                
                /*
                 * Find elevation tile in background, and load 
                 */
                if(mService != null) {
                    /*
                     * Elevation tile to find AGL and ground proximity warning
                     */
                    double offsets[] = new double[2];
                    double p[] = new double[2];
                    Tile t = mImageDataSource.findElevTile(lon, lat, offsets, p, 0);
                    mService.setElevationTile(t);
                    BitmapHolder elevBitmap = mService.getElevationBitmap();
                    /*
                     * Load only if needed.
                     */
                    if(null != elevBitmap) {
                        int x = (int)Math.round(offsets[0]);
                        int y = (int)Math.round(offsets[1]);
                        if(elevBitmap.getBitmap() != null) {
                        
                            if(x < elevBitmap.getBitmap().getWidth()
                                && y < elevBitmap.getBitmap().getHeight()
                                && x >= 0 && y >= 0) {
                            
                                int px = elevBitmap.getBitmap().getPixel(x, y);
                                elev = Helper.findElevationFromPixel(px);
                            }
                        }
                    }
                }
            }
            return null;                
        }
        
        @Override
        protected void onPostExecute(Object res) {
            mElev = elev;
            mObstacles = obs;
        }

    }


    /**
     * Center to the location
     */
    public void center() {
        /*
         * On double tap, move to center
         */
        mPan = new Pan();
        if(mService != null) {
            mService.setPan(mPan);
            mService.getTiles().forceReload();
        }
        dbquery(true);
        updateCoordinates();
        postInvalidate();
    }
            
    /**
     * @author zkhan
     *s
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            if(null != mService) {
                /*
                 * Add separation between chars
                 */
                mService.getDraw().addSeparation();
            }
            return true;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
        	
        	// Ignore this gesture if we are not configured to use dynamic fields
        	if((mPref.useDynamicFields() == false) || (mService == null)) {
        		return false;
        	}
        	
        	float posX = mCurrTouchPoint.getX();
        	float posY = mCurrTouchPoint.getY();
        	InfoLineFieldLoc infoLineFieldLoc = mInfoLines.findField(mPaint, posX, posY);
        	if(infoLineFieldLoc != null) {
            	// We have the row and field. Tell the selection dialog to display
            	mGestureCallBack.gestureCallBack(GestureInterface.DOUBLE_TAP, infoLineFieldLoc);
        	}
        	return true;
        }
        
        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
            /*
             * on long press, find a point where long press was done
             */
            double x = mCurrTouchPoint.getX();
            double y = mCurrTouchPoint.getY();

        	InfoLineFieldLoc infoLineFieldLoc = mInfoLines.findField(mPaint, (float)x, (float)y);
        	if(infoLineFieldLoc != null) {
            	// We have the row and field. Send the gesture off for processing
            	mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, infoLineFieldLoc);
            	return;
        	}

            /*
             * XXX:
             * For track up, currently there is no math to find anything with long press.
             */
            if(mTrackUp) {
                return;
            }

            /*
             * Notify activity of gesture.
             */
            
            if(mLongTouchDestination == null) {
                // The ClosestAirportTask must be taking a long time, make sure it knows to do the gesture
                // callback when it's done
                mDoCallbackWhenDone = true;
            }
            else {
                mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, mLongTouchDestination);
            }
        }
    }
    
    private void startClosestAirportTask(double x, double y) {
        // We won't be doing the airport long press under certain circumstances
        if(mDraw || mTrackUp) {
            return;
        }
        
        if(null != mClosestTask) {
            mClosestTask.cancel(true);
        }
        mLongTouchDestination = null;
        mClosestTask = new ClosestAirportTask();        
        
        double lon2 = mOrigin.getLongitudeOf(x);
        double lat2 = mOrigin.getLatitudeOf(y);
        
        mClosestTask.execute(lon2, lat2);
    }


    /**
     * 
     * @param gestureInterface
     */
    public void setGestureCallback(GestureInterface gestureInterface) {
        mGestureCallBack = gestureInterface;
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
     * @param tu
     */
    public void setTrackUp(boolean tu) {
        mTrackUp = tu;
        invalidate();
    }

    public double getElev() {
    	return mElev;
    	
    }
    
    public double getVSI() {
    	return mVSI;
    }
    
    public GpsParams getGpsParams() {
    	return mGpsParams;
    }
    
    public StorageService getStorageService() {
    	return mService;
    }
    public int getDisplayWidth() {
    	return getWidth();
    }
    
    public Preferences getPref() {
    	return mPref;
    }
    
    public Context getAppContext() { 
    	return mContext;
    }

    public String getErrorStatus() {
    	return mErrorStatus;
    }

    public String getPriorityMessage() {
        if(mPointProjection != null) {
        	String priorityMessage = 
        			Helper.makeLine2(mPointProjection.getDistance(),
                    Preferences.distanceConversionUnit, mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()),
                    mPointProjection.getBearing(), mGpsParams.getDeclinition());
        	return priorityMessage;
        }
        return null;
    }

    public float getThreshold() {
    	return mThreshold;
    }
    /**
     * 
     */
    public void cleanup() {
        mTileDrawTask.running = false;
        mTileDrawThread.interrupt();
    }

    
    /**
     * Use this with handler to update tiles in UI thread
     * @author zkhan
     *
     */
    private class TileUpdate {
        
        private double offsets[];
        private double p[];
        private int movex;
        private int movey;
        private float factor;
        private Tile centerTile;
        
    }
    
    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            TileUpdate t = (TileUpdate)msg.obj;
            
            mService.getTiles().flip();
            
            /*
             * Set move with pan after new tiles are finally loaded
             */
            mPan.setMove((float)(mPan.getMoveX() * t.factor), (float)(mPan.getMoveY() * t.factor));

            mScale.setScaleAt(t.centerTile.getLatitude());
            mOnChart = t.centerTile.getChart();

            /*
             * And pan
             */
            mPan.setTileMove(t.movex, t.movey);
            mMovement = new Movement(t.offsets, t.p);
            mService.setMovement(mMovement);
            mMacro = mScale.getMacroFactor();
            mScale.updateMacro();
            mPx = (float)t.centerTile.getPx();
            mPy = (float)t.centerTile.getPy();
            updateCoordinates();

            invalidate();
        }
    };
    
    /**
     * 
     */
    public void zoomOut() {
        mScale.zoomOut();
    }

}
