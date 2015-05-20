/*
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;


import java.util.LinkedList;
import java.util.List;

import com.ds.avare.adsb.NexradBitmap;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.GameTFR;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Pan;
import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Scale;
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
import com.ds.avare.utils.DisplayIcon;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines.InfoLineFieldLoc;
import com.ds.avare.utils.NavComments;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;
import com.ds.avare.R;
import com.ds.avare.StorageService;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
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
    
    /**
     * The plane on screen
     */
    private BitmapHolder               mAirplaneBitmap;
    private BitmapHolder               mRunwayBitmap;
    private BitmapHolder               mLineBitmap;
    private BitmapHolder               mObstacleBitmap;
    private BitmapHolder               mLineHeadingBitmap;
    
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
    private ElevationTask               mElevationTask; 
    private Thread                      mElevationThread;
    private long                        mElevationLastRun;

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

    private boolean                    mTrackUp;
    
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
     *  Copy the existing paint to a new paint so we don't mess it up
     */
    private Paint mRunwayPaint;
    private Paint mMsgPaint;

    /*
     * Text on screen color
     */
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    
    private static final float MOVEMENT_THRESHOLD = 32.f;
    
    private static final int MAX_SCALE = 4;
    
    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

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
        mScale = new Scale(MAX_SCALE);
        mOrigin = new Origin();
        mMovement = new Movement();
        mErrorStatus = null;
        mOnChart = null;
        mTrackUp = false;
        mMacro = 1;
        mDragPlanPoint = -1;
        mImageDataSource = null;
        mGpsParams = new GpsParams(null);
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
         * Set up the paint for misc messages to display
         */
        mMsgPaint = new Paint();
        mMsgPaint.setAntiAlias(true);
        mMsgPaint.setTextSize(getResources().getDimension(R.dimen.distanceRingNumberTextSize));
        
        /*
         * Set up the paint for the runways as much as possible here
         */
        mRunwayPaint = new Paint(mPaint);
        mRunwayPaint.setTextSize(getResources().getDimension(R.dimen.runwayNumberTextSize));

        
        mTileDrawTask = new TileDrawTask();
        mTileDrawThread = new Thread(mTileDrawTask);
        mTileDrawThread.start();
        mElevationTask = new ElevationTask();
        mElevationThread = new Thread(mElevationTask);
        mElevationLastRun = System.currentTimeMillis();
        mElevationThread.start();

        setOnTouchListener(this);
        mAirplaneBitmap = DisplayIcon.getDisplayIcon(context, mPref);
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
                
                mService.getPlan().replaceDestination(mPref, mDragPlanPoint, lon, lat, finish);
               
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
                 * This is where rubberbanding starts
                 */
                if(mService.getPlan() != null && mDragPlanPoint < 0 && mPref.allowRubberBanding()) {
                    double lon = mOrigin.getLongitudeOf(e.getX());
                    double lat = mOrigin.getLatitudeOf(e.getY());
                    mDragPlanPoint = mService.getPlan().findClosePointId(lon, lat, mScale.getScaleFactor());
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
                float x = mCurrTouchPoint.getX() * mScale.getScaleFactor();
                float y = mCurrTouchPoint.getY() * mScale.getScaleFactor();
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

        // Do run run onbstacle task more frequenct than 10 seconds
        if(((System.currentTimeMillis() - mElevationLastRun) > 1000 * 10) || force) {
            mElevationLastRun = System.currentTimeMillis();
	        mElevationTask.lat = mGpsParams.getLatitude();
	        mElevationTask.lon = mGpsParams.getLongitude();
	        mElevationTask.alt = mGpsParams.getAltitude();
	        mElevationThread.interrupt();
        }

        /*
         * Find
         */
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
                    Helper.setThreshold(mPaint, (float)mService.getThreshold());
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
                mMsgPaint.setColor(Color.WHITE);
                mService.getShadowedText().draw(canvas, mMsgPaint,
                        mContext.getString(R.string.MissingMaps) + "- " + mOnChart, 
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
                shapes.get(shape).drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mPref.isNightMode(), true);
            }
        }
        
        /*
         * Possible game TFRs, Orange
         */
        if(null == mPointProjection && mPref.showGameTFRs()) {
            mPaint.setColor(0xFFFF4500); 
            mPaint.setStrokeWidth(3 * mDipToPix);
            mPaint.setShadowLayer(0, 0, 0, 0);
            Style style = mPaint.getStyle();
            mPaint.setStyle(Style.STROKE);
            float radius = Math.abs((float)((GameTFR.RADIUS_NM * Preferences.NM_TO_LATITUDE) * (1.0 / mMovement.getLatitudePerPixel()) * mScale.getScaleCorrected()));
            for(int shape = 0; shape < GameTFR.GAME_TFR_COORDS.length; shape++) {
                double lat = GameTFR.GAME_TFR_COORDS[shape][0];
                double lon = GameTFR.GAME_TFR_COORDS[shape][1];
                float x = (float)mOrigin.getOffsetX(lon);
                float y = (float)mOrigin.getOffsetY(lat);
                canvas.drawCircle(x, y, radius, mPaint);
            }
            mPaint.setStyle(style);
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
                    met.shape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mPref.isNightMode(), true);
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
         * Get traffic to draw.
         */
        SparseArray<Traffic> traffic = mService.getTrafficCache().getTraffic();

        if((!mPref.showAdsbTraffic()) || (null == traffic) || (null != mPointProjection)) {
            return;
        }

        mMsgPaint.setColor(Color.WHITE);
        for(int i = 0; i < traffic.size(); i++) {
            int key = traffic.keyAt(i);
            Traffic t = traffic.get(key);
            if(t.isOld()) {
                traffic.delete(key);
                continue;
            }
            
            /*
             * Make traffic line and info
             */
            float x = (float)mOrigin.getOffsetX(t.mLon);
            float y = (float)mOrigin.getOffsetY(t.mLat);
            
            /*
             * Find color from altitude
             */
            int color = Traffic.getColorFromAltitude(mGpsParams.getAltitude(), t.mAltitude);
            
            
            float radius = mDipToPix * 8;
            String text = t.mAltitude + "'"; 
            /*
             * Draw outline to show it clearly
             */
            mPaint.setColor((~color) | 0xFF000000);
            canvas.drawCircle(x, y, radius + 2, mPaint);
            
            mPaint.setColor(color);
            canvas.drawCircle(x, y, radius, mPaint);
            /*
             * Show a barb for heading with length based on speed
             * Vel can be 0 to 4096 knots (practically it can be 0 to 500 knots), so set from length 0 to 100 pixels (1/5)
             */
            float speedLength = radius + (float)t.mHorizVelocity * (float)mDipToPix / 5.f;
            /*
             * Rotation of points to show direction
             */
            double xr = x + PixelCoordinate.rotateX(speedLength, t.mHeading);
            double yr = y + PixelCoordinate.rotateY(speedLength, t.mHeading);
            canvas.drawLine(x, y, (float)xr, (float)yr, mPaint);
            mService.getShadowedText().draw(canvas, mMsgPaint,
                    text, Color.DKGRAY, (float)x, (float)y + radius + mMsgPaint.getTextSize());
            
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
            mPaint.setColor(Color.MAGENTA);
            mPaint.setStrokeWidth(5 * mDipToPix);
            mPaint.setAlpha(162);
            if(mService.getDestination().isFound() && !mService.getPlan().isActive()  && (!mPref.isSimulationMode())) {
                mService.getDestination().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mPref.isNightMode(), mPref.isTrackEnabled());
            } else if (mService.getPlan().isActive()) {
                mService.getPlan().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mPref.isNightMode(), mPref.isTrackEnabled(), mService.getPlan());                    
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
                    boolean bRotated = false;
                    if (mTrackUp && (mGpsParams != null)) {
                    	bRotated = true;
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
                    
                    mRunwayPaint.setColor(Color.WHITE);
                    mService.getShadowedText().draw(canvas, mRunwayPaint, num, Color.DKGRAY,
                            runwayNumberCoordinatesX, runwayNumberCoordinatesY);

                    if (true == bRotated) {
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
        if((mService == null) || (null != mPointProjection)){
        	return;
        }
        
        // Tell the rings to draw themselves
        mService.getDistanceRings().draw(canvas, mOrigin, mScale, mMovement, mTrackUp, mGpsParams);
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
            mPaint.setColor(Color.CYAN);
            mPaint.setStrokeWidth(6 * mDipToPix);
            mPaint.setStyle(Paint.Style.FILL);

            mService.getKMLRecorder().getShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mPref.isNightMode(), true);
        }
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
    	if(mPref.shouldShowEdgeTape()) {
	        if(mService != null && mPointProjection == null) {
		        int x = (int)(mOrigin.getOffsetX(mGpsParams.getLongitude()));
		        int y = (int)(mOrigin.getOffsetY(mGpsParams.getLatitude()));
		        float pixPerNm = mMovement.getNMPerLatitude(mScale);
		      	mService.getEdgeTape().draw(canvas, mScale, pixPerNm, x, y, 
		      			(int) mService.getInfoLines().getHeight(), getWidth(), getHeight());
	        }
    	}
    }
    
    // Display all of the user defined waypoints if configured to do so
    private void drawUserDefinedWaypoints(Canvas canvas) {
        if(mService != null && mPointProjection == null) {
        	mService.getUDWMgr().draw(canvas, mTrackUp, mGpsParams, mFace, mOrigin);
        }
    }

    // Display cap grids
    private void drawCapGrids(Canvas canvas) {
        if(mService != null && mPointProjection == null && mPref.showCAPGrids()) {
        	mService.getCap().draw(canvas, mOrigin, mScale);
        }
    }

    // Draw the top status lines
    private void drawStatusLines(Canvas canvas) {
        if(mService != null) {
          	mService.getInfoLines().drawCornerTextsDynamic(canvas, mPaint, 
          	        TEXT_COLOR, TEXT_COLOR_OPPOSITE, 4,
          	        getWidth(), getHeight(), mErrorStatus, getPriorityMessage());
        }
    }
    
    // Display the nav comments
    private void drawNavComments(Canvas canvas) {
        if(mService != null) {
        	NavComments navComments = mService.getNavComments();
        	if(null != navComments) {
        		navComments.draw(this, canvas, mMsgPaint,  mService.getShadowedText());
        	}
        }
    }
    
    /**
     * @param canvas
     * Does pretty much all drawing on screen
     */
    private void drawMap(Canvas canvas) {
    	// If our track is supposed to be at the top, save the current
    	// canvas and rotate it based upon our bearing if we have one
    	boolean bRotated = false;
        if(mTrackUp && (mGpsParams != null)) {
        	bRotated = true;
            canvas.save();
            /*
             * Rotate around current position
             */
            float x = (float)mOrigin.getOffsetX(mGpsParams.getLongitude());
            float y = (float)mOrigin.getOffsetY(mGpsParams.getLatitude());
            canvas.rotate(-(int)mGpsParams.getBearing(), x, y);
        }
        
        // Call the draw routines for the items that rotate with
        // the chart
        drawTiles(canvas);
        drawNexrad(canvas);
        drawRadar(canvas);
        drawDrawing(canvas);
        drawCapGrids(canvas);
        drawTraffic(canvas);
        drawTFR(canvas);
        drawAirSigMet(canvas);
        drawTracks(canvas);
        drawTrack(canvas);
        drawObstacles(canvas);
        drawRunways(canvas);
        drawAircraft(canvas);
      	drawUserDefinedWaypoints(canvas);
        
      	// Restore the canvas to be upright again
        if(true == bRotated) {
            canvas.restore();
        }
        
        // Now draw the items that do NOT rotate with the chart
        drawDistanceRings(canvas);
        drawCDI(canvas);
        drawVASI(canvas);
        drawStatusLines(canvas);
      	drawEdgeMarkers(canvas); // Must be after the infolines
      	drawNavComments(canvas);
    }    

    /**
     * 
     * @param threshold
     */
    public void updateThreshold(float threshold) {
        if(mService != null) {
            mService.setThreshold(threshold);
        }
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

    	/*
         * Comes from location manager
         */
        mGpsParams = params;

        updateCoordinates();
        
        /*
         * Database query for new location / pan location.
         */
        dbquery(false);
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
        mService.getCDI().setSize(mPaint, Math.min(getWidth(),  getHeight()));
        mService.getVNAV().setSize(mPaint, Math.min(getWidth(),  getHeight()));
        
        // Tell the odometer how to access preferences
        mService.getOdometer().setPref(mPref);

        mService.getEdgeTape().setPaint(mPaint);
        
        
        // Resize our runway icon based upon the size of the display.
        // We want the icon no more than 1/3 the size of the screen. Since we show 2 images
        // of this icon, that means the total size is no more than 2/3 of the available space. 
        // This leaves room to print the runway numbers with some real estate left over.
        Bitmap newRunway = Helper.getResizedBitmap(mRunwayBitmap.getBitmap(), getWidth(), getHeight(), (double) 1/3);
        
        // If a new bitmap was generated, then load it in.
        if(newRunway != mRunwayBitmap.getBitmap()) {
        	mRunwayBitmap = new BitmapHolder(newRunway);
        }
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
                int ty = (int)(mService.getTiles().getYTilesNum() / 2);
                int tx = (int)(mService.getTiles().getXTilesNum() / 2);
                for(int tiley = -ty; tiley <= ty; tiley++) {
                    for(int tilex = -tx; tilex <= tx; tilex++) {
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
        private String fuel;
        private String ratings;
        
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

                LinkedList<String> fl = mService.getDBResource().findFuelCost(airport);
                if(fl.size() == 0) {
                	// If fuel not available, show its not
                	fuel = mContext.getString(R.string.NotAvailable);
                }
                else {
                	fuel = "";
                }
                // Concat all fuel reports
                for(String s : fl) {
                	fuel += s + "\n\n";
                }
                if(isCancelled())
                    return "";
                
                
                LinkedList<String> cm = mService.getDBResource().findRatings(airport);
                if(cm.size() == 0) {
                	// If ratings not available, show its not
                	ratings = mContext.getString(R.string.NotAvailable);
                }
                else {
                	ratings = "";
                }
                // Concat all fuel reports
                for(String s : cm) {
                	ratings += s + "\n\n";
                }
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
                mLongTouchDestination.fuel = fuel;
                mLongTouchDestination.ratings = ratings;
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
    
    private class ElevationTask implements Runnable {
        public boolean running = true;
        private boolean runAgain = false;
        public double lon;
        public double lat;
        public double alt;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Elevation");

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
                 * Find obstacles in background as well
                 */
                LinkedList<Obstacle> obs = null;
                if(mPref.shouldShowObstacles()) {
                    obs = mImageDataSource.findObstacles(lon, lat, (int)alt);
                }

                /*
                 * Elevation tile to find AGL and ground proximity warning
                 */
                double offsets[] = new double[2];
                double p[] = new double[2];
                Tile t = mImageDataSource.findElevTile(lon, lat, offsets, p, 0);

                mService.setElevationTile(t);
                BitmapHolder elevBitmap = mService.getElevationBitmap();
                double elev = -1;
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

                ElevationUpdate ou = new ElevationUpdate();
                ou.elev = elev;
                ou.obs = obs;

                Message m = mHandler.obtainMessage();
                m.obj = ou;
                mHandler.sendMessage(m);
            }
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
        public boolean onSingleTapConfirmed(MotionEvent e) {
        	
        	// Ignore this gesture if we are not configured to use dynamic fields
        	if((mPref.useDynamicFields() == false) || (mService == null)) {
        		return false;
        	}
        	
        	float posX = e.getX();
        	float posY = e.getY();
        	InfoLineFieldLoc infoLineFieldLoc = mService.getInfoLines().findField(mPaint, posX, posY);
        	if(infoLineFieldLoc != null) {
            	// We have the row and field. Tell the selection dialog to display
            	mGestureCallBack.gestureCallBack(GestureInterface.TOUCH, infoLineFieldLoc);
        	}
        	return true;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
        	
        	// Ignore this gesture if we are not configured to use dynamic fields
        	if((mPref.useDynamicFields() == false) || (mService == null)) {
        		return false;
        	}
        	
        	float posX = e.getX();
        	float posY = e.getY();
        	InfoLineFieldLoc infoLineFieldLoc = mService.getInfoLines().findField(mPaint, posX, posY);
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
            double x = e.getX();
            double y = e.getY();

            if(mService != null) {
            	InfoLineFieldLoc infoLineFieldLoc = mService.getInfoLines().findField(mPaint, (float)x, (float)y);
            	if(infoLineFieldLoc != null) {
                	// We have the row and field. Send the gesture off for processing
                	mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, infoLineFieldLoc);
                	return;
            	}
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

    /**
     * 
     * @return
     */
    private String getPriorityMessage() {
        if(mPointProjection != null) {
        	String priorityMessage = 
        			Helper.makeLine2(mPointProjection.getDistance(),
                    Preferences.distanceConversionUnit, mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()),
                    mPointProjection.getBearing(), mGpsParams.getDeclinition());
        	return priorityMessage;
        }
        return null;
    }

    /**
     * 
     */
    public void cleanup() {
        mTileDrawTask.running = false;
        mTileDrawThread.interrupt();
        mElevationTask.running = false;
        mElevationThread.interrupt();
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
     * Use this with handler to update obstacles in UI thread
     * @author zkhan
     *
     */
    private class ElevationUpdate {
        private LinkedList<Obstacle> obs;
        private double elev;        
    }

    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        	if(msg.obj instanceof TileUpdate) {
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
	            mMultiTouchC.setMacro(mMacro);
	            mPx = (float)t.centerTile.getPx();
	            mPy = (float)t.centerTile.getPy();
	            updateCoordinates();
	
	            invalidate();
        	}
        	else if(msg.obj instanceof ElevationUpdate) {
        		ElevationUpdate o = (ElevationUpdate)msg.obj;
        		mService.setElevation(o.elev);
        		mObstacles = o.obs;
        	}
        }
    };
    
    /**
     * 
     */
    public void zoomOut() {
        mScale.zoomOut();
    }

}
