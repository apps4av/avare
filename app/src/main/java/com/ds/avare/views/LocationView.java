/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.adsb.NexradBitmap;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Scale;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.shapes.Layer;
import com.ds.avare.shapes.MetShape;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.Tile;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DisplayIcon;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines.InfoLineFieldLoc;
import com.ds.avare.utils.NavComments;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zkhan
 * @author plinel
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
   
    // Which layer to draw
    private  String                     mLayerType;
    private Layer                       mLayer;

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
     * Macro of zoom
     */
    private int                         mMacro;

    private int                mDragPlanPoint;
    private float                mDragStartedX;
    private float                mDragStartedY;

    /*
     *  Copy the existing paint to a new paint so we don't mess it up
     */
    private Paint mRunwayPaint;
    private Paint mMsgPaint;

    private Tile mGpsTile;

    private String mOnChart = "";
    
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
        mOrigin.update(mGpsTile, getWidth(), getHeight(), mGpsParams, mPan, mScale);
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
            if(mDraw && mService != null) {
                float x = mCurrTouchPoint.getX() * mScale.getScaleFactor();
                float y = mCurrTouchPoint.getY() * mScale.getScaleFactor();
                /*
                 * Threshold the drawing so we do not generate too many points
                 */
                if (mPref.isTrackUp()) {
                    double thetab = mGpsParams.getBearing();
                    double p[] = new double[2];
                    double c_x = mOrigin.getOffsetX(mGpsParams.getLongitude());
                    double c_y = mOrigin.getOffsetY(mGpsParams.getLatitude());
                    p = Helper.rotateCoord(c_x, c_y, thetab, x, y);
                    mService.getDraw().addPoint((float)p[0],(float)p[1], mOrigin);
                }
                else {
                    mService.getDraw().addPoint(x, y, mOrigin);
                }
                return true;
            }

            // Pan
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

            if(mPointProjection == null) {
                double x0 = mCurrTouchPoint.getXs()[0];
                double y0 = mCurrTouchPoint.getYs()[0];
                double x1 = mCurrTouchPoint.getXs()[1];
                double y1 = mCurrTouchPoint.getYs()[1];

                double lon0,lat0,lon1,lat1;
                // convert to origin coord if Trackup
                if(mPref.isTrackUp()) {
                    double c_x = mOrigin.getOffsetX(mGpsParams.getLongitude());
                    double c_y = mOrigin.getOffsetY(mGpsParams.getLatitude());
                    double thetab = mGpsParams.getBearing();
                    double p0[],p1[];
                    p0 = Helper.rotateCoord(c_x, c_y, thetab, x0, y0);
                    p1 = Helper.rotateCoord(c_x, c_y, thetab, x1, y1);
                    lon0 = mOrigin.getLongitudeOf(p0[0]);
                    lat0 = mOrigin.getLatitudeOf(p0[1]);
                    lon1 = mOrigin.getLongitudeOf(p1[0]);
                    lat1 = mOrigin.getLatitudeOf(p1[1]);
                }
                else {
                    lon0 = mOrigin.getLongitudeOf(x0);
                    lat0 = mOrigin.getLatitudeOf(y0);
                    lon1 = mOrigin.getLongitudeOf(x1);
                    lat1 = mOrigin.getLatitudeOf(y1);
                }
                mPointProjection = new Projection(lon0, lat0, lon1, lat1);
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
        if((!force) && (mGpsTile != null)) {
            double offsets[] = new double[2];

            if(mGpsTile.within(mGpsParams.getLongitude(), mGpsParams.getLatitude())) {
                /*
                 * We are within same tile no need for query.
                 */
            	offsets[0] = mGpsTile.getOffsetX(mGpsParams.getLongitude());
            	offsets[1] = mGpsTile.getOffsetY(mGpsParams.getLatitude());
                mMovement = new Movement(offsets);
                postInvalidate();
                return;
            }
        }

        /*
         * Find
         */
        loadTiles(mGpsParams.getLongitude(), mGpsParams.getLatitude());
    }



    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawTiles(Canvas canvas, DrawingContext ctx) {
        Tile.draw(ctx, mOnChart, mService.getTiles());
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawTFR(Canvas canvas, DrawingContext ctx) {
        TFRShape.draw(ctx, mService.getTFRShapes(), null == mPointProjection);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawAirSigMet(Canvas canvas, DrawingContext ctx) {
        MetShape.draw(ctx, mService.getInternetWeatherCache().getAirSigMet(), null == mPointProjection);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawLayers(Canvas canvas, DrawingContext ctx) {
        if(mLayerType == null || null != mPointProjection || 0 == mPref.showLayer() || ctx.pref.useAdsbWeather()) {
            return;
        }

        if(mLayerType.equals("NEXRAD")) {
            // draw nexrad
            mLayer = mService.getRadarLayer();
        }
        else if(mLayerType.equals("METAR")) {
            // draw metar flight catergory
            mLayer = ctx.service.getMetarLayer();
        }
        else {
            mLayer = null;
            return;
        }

        /*
         * layer is way too old.
         */
        if(mLayer.isOld(ctx.pref.getExpiryTime())) {
            return;
        }

        mPaint.setAlpha(mPref.showLayer());
        mLayer.draw(canvas, mPaint, mOrigin);
        mPaint.setAlpha(255);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawNexrad(Canvas canvas, DrawingContext ctx) {
        NexradBitmap.draw(ctx, mService.getAdsbWeather().getNexrad(),
                mService.getAdsbWeather().getNexradConus(), null == mPointProjection);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawTraffic(Canvas canvas, DrawingContext ctx) {
        Traffic.draw(ctx, mService.getTrafficCache().getTraffic(),
                mGpsParams.getAltitude(), null == mPointProjection);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawTrack(Canvas canvas, DrawingContext ctx) {
        TrackShape.draw(ctx, mService.getPlan(), mService.getDestination(),
                mGpsParams, mLineBitmap, mLineHeadingBitmap, mPointProjection == null);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawDrawing(Canvas canvas, DrawingContext ctx) {
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
     * @param ctx
     */
    private void drawObstacles(Canvas canvas, DrawingContext ctx) {
        if(mPref.shouldShowObstacles()) {
            if((mObstacles != null) && (null == mPointProjection)) {
                mPaint.setShadowLayer(0, 0, 0, 0);
                for (Obstacle o : mObstacles) {
                    BitmapHolder.rotateBitmapIntoPlace(mObstacleBitmap, 0, o.getLongitude(), o.getLatitude(), false, mOrigin);
                    canvas.drawBitmap(mObstacleBitmap.getBitmap(), mObstacleBitmap.getTransform(), mPaint);
                }
            }
        }
    }
    
    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawAircraft(Canvas canvas, DrawingContext ctx) {
        mPaint.setShadowLayer(0, 0, 0, 0);
        mPaint.setColor(Color.WHITE);

        if(null != mAirplaneBitmap && null == mPointProjection) {
            
            /*
             * Rotate and move to a panned location
             */
            BitmapHolder.rotateBitmapIntoPlace(mAirplaneBitmap, (float) mGpsParams.getBearing(),
                    mGpsParams.getLongitude(), mGpsParams.getLatitude(), true, mOrigin);
            canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
        }
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawRunways(Canvas canvas, DrawingContext ctx) {
        Runway.draw(ctx, mRunwayBitmap, mService.getDestination(), mGpsParams, mPointProjection == null);
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
        if(null != mPointProjection){
        	return;
        }
        
        // Tell the rings to draw themselves
        mService.getDistanceRings().draw(canvas, mOrigin, mScale, mMovement, mPref.isTrackUp(), mGpsParams);
    }

    /**
     * Draw the tracks to show our previous positions. If tracking is enabled, there is
     * a linked list of gps coordinates attached to this view with the most recent one at the end
     * of that list. Start at the end value to begin the drawing and as soon as we find one that is 
     * not in the range of this display, we can assume that we're done.
     * @param canvas
     * @param ctx
     */
    private void drawTracks(Canvas canvas, DrawingContext ctx) {
        /*
         * Some pre-conditions that would prevent us from drawing anything
         */
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
        if(mPointProjection == null && mErrorStatus == null) {
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
        if(mPointProjection == null && mErrorStatus == null) {
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
	        if(mPointProjection == null) {
		        int x = (int)(mOrigin.getOffsetX(mGpsParams.getLongitude()));
		        int y = (int)(mOrigin.getOffsetY(mGpsParams.getLatitude()));
		        float pixPerNm = mOrigin.getPixelsInNmAtLatitude(1, mGpsParams.getLatitude());
		      	mService.getEdgeTape().draw(canvas, mScale, pixPerNm, x, y, 
		      			(int) mService.getInfoLines().getHeight(), getWidth(), getHeight());
	        }
    	}
    }
    
    // Display all of the user defined waypoints if configured to do so
    private void drawUserDefinedWaypoints(Canvas canvas, DrawingContext ctx) {
        if(mPointProjection == null) {
        	mService.getUDWMgr().draw(canvas, mPref.isTrackUp(), mGpsParams, mFace, mOrigin);
        }
    }

    // Display cap grids
    private void drawCapGrids(Canvas canvas, DrawingContext ctx) {
        if(mPointProjection == null && mPref.showCAPGrids()) {
        	mService.getCap().draw(canvas, mOrigin, mScale);
        }
    }

    // Draw the top status lines
    private void drawStatusLines(Canvas canvas) {
        mService.getInfoLines().drawCornerTextsDynamic(canvas, mPaint,
                TEXT_COLOR, TEXT_COLOR_OPPOSITE, 4,
                getWidth(), getHeight(), mErrorStatus, getPriorityMessage());
    }
    
    // Display the nav comments
    private void drawNavComments(Canvas canvas) {
        NavComments navComments = mService.getNavComments();
        if(null != navComments) {
            navComments.draw(this, canvas, mMsgPaint,  mService.getShadowedText());
        }
    }
    
    /**
     * @param canvas
     * Does pretty much all drawing on screen
     */
    private void drawMap(Canvas canvas) {

        if(mService == null) {
            return;
        }

    	// If our track is supposed to be at the top, save the current
    	// canvas and rotate it based upon our bearing if we have one
    	boolean bRotated = false;
        if(mPref.isTrackUp() && (mGpsParams != null)) {
        	bRotated = true;
            canvas.save();
            /*
             * Rotate around current position
             */
            float x = (float)mOrigin.getOffsetX(mGpsParams.getLongitude());
            float y = (float)mOrigin.getOffsetY(mGpsParams.getLatitude());
            canvas.rotate(-(int)mGpsParams.getBearing(), x, y);
        }

        DrawingContext ctx = new DrawingContext();
        ctx.service = mService;
        ctx.canvas = canvas;
        ctx.context = mContext;
        ctx.dip2pix = mDipToPix;
        ctx.movement = mMovement;
        ctx.origin = mOrigin;
        ctx.paint = mPaint;
        ctx.textPaint = mMsgPaint;
        ctx.scale = mScale;
        ctx.pan = mPan;
        ctx.pref = mPref;
        ctx.runwayPaint = mRunwayPaint;
        ctx.view = this;

        // Call the draw routines for the items that rotate with
        // the chart
        drawTiles(canvas, ctx);
        drawNexrad(canvas, ctx);
        drawLayers(canvas, ctx);
        drawDrawing(canvas, ctx);
        drawCapGrids(canvas, ctx);
        drawTraffic(canvas, ctx);
        drawTFR(canvas, ctx);
        drawAirSigMet(canvas, ctx);
        drawTracks(canvas, ctx);
        drawTrack(canvas, ctx);
        drawObstacles(canvas, ctx);
        drawRunways(canvas, ctx);
        drawAircraft(canvas, ctx);
      	drawUserDefinedWaypoints(canvas, ctx);
        
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
        dbquery(true);
        postInvalidate();

        // Tell the CDI the paint that we use for display tfr
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
     * Function that loads new tiles in background
     *
     */
    private void loadTiles(final double lon, final double lat) {
        if(mService == null) {
            return;
        }

        TileMap map = mService.getTiles();
        map.loadTiles(lon, lat, mPan, mMacro, mScale, mGpsParams.getBearing(),
                new GenericCallback() {
                    @Override
                    public Object callback(Object map, Object tu) {
                        TileMap.TileUpdate t = (TileMap.TileUpdate)tu;
                        ((TileMap)map).flip();

                        /*
                         * Set move with pan after new tiles are finally loaded
                         */
                        mPan.setMove((float)(mPan.getMoveX() * t.factor), (float)(mPan.getMoveY() * t.factor));

                        int index = Integer.parseInt(mPref.getChartType());
                        String type = getResources().getStringArray(R.array.ChartType)[index];

                        mGpsTile = t.gpsTile;
                        mOnChart = type + "\n" + t.chart;
                        /*
                         * And pan
                         */
                        mPan.setTileMove(t.movex, t.movey);
                        mMovement = new Movement(t.offsets);
                        mService.setMovement(mMovement);
                        mMacro = mScale.getMacroFactor();
                        mScale.updateMacro();
                        mMultiTouchC.setMacro(mMacro);
                        updateCoordinates();
                        invalidate();

                        return null;
                    }
                });
    }

    /**
     * @author zkhan
     *
     */
    private class ClosestAirportTask extends AsyncTask<Object, String, String> {
        private Double lon;
        private Double lat;
        private String tfr = "";
        private String textMets = "";
        private String sua;
        private String layer;
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
             * Get TFR tfr if touched on its top
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
                     * Set TFR tfr
                     */
                    String txt = cshape.getTextIfTouched(lon, lat);
                    if(null != txt) {
                        tfr += txt + "\n--\n";
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
                         * Set MET tfr
                         */
                        String txt = cshape.getTextIfTouched(lon, lat);
                        if(null != txt) {
                            textMets += txt + "\n--\n";
                        }
                    }
                }
            }            

            airport = mService.getDBResource().findClosestAirportID(lon, lat);
            if(isCancelled()) {
                return "";
            }

            if(null == airport) {
                airport = "" + Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
            }
            else {
                freq = mService.getDBResource().findFrequencies(airport);
                if(isCancelled()) {
                    return "";
                }
            
                taf = mService.getDBResource().getTAF(airport);
                if(isCancelled()) {
                    return "";
                }
                
                metar = mService.getDBResource().getMETAR(airport);   
                if(isCancelled()) {
                    return "";
                }
            
                runways = mService.getDBResource().findRunways(airport);
                if(isCancelled()) {
                    return "";
                }
                
                elev = mService.getDBResource().findElev(airport);
                if(isCancelled()) {
                    return "";
                }

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
                if(isCancelled()) {
                    return "";
                }
                
                wa = mService.getDBResource().getWindsAloft(lon, lat);
                if(isCancelled()) {
                    return "";
                }
                
                sua = mService.getDBResource().getSua(lon, lat);
                if(isCancelled()) {
                    return "";
                }

                if(mLayer != null) {
                    layer = mLayer.getDate();
                }
                if(isCancelled()) {
                    return "";
                }
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
                    layer = mService.getAdsbWeather().getNexrad().getDate();
                }
                else {
                    boolean inWeatherOld = mService.getInternetWeatherCache().isOld(mPref.getExpiryTime());
                    if(inWeatherOld) { // expired weather and TFR text do not show
                        taf = null;
                        metar = null;
                        aireps = null;
                        textMets = null;
                        tfr = null;
                        wa = null;
                    }
                }
                if(null != aireps) {
                    for(Airep a : aireps) {
                        a.updateTextWithLocation(lon, lat, mGpsParams.getDeclinition());                
                    }
                }
                if(null != wa) {
                    wa.updateStationWithLocation(lon, lat, mGpsParams.getDeclinition());
                }
                mLongTouchDestination.tfr = tfr;
                mLongTouchDestination.taf = taf;
                mLongTouchDestination.metar = metar;
                mLongTouchDestination.airep = aireps;
                mLongTouchDestination.mets = textMets;
                mLongTouchDestination.wa = wa;
                mLongTouchDestination.freq = freq;
                mLongTouchDestination.sua = sua;
                mLongTouchDestination.layer = layer;
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
                Tile t = new Tile(mContext, mPref, lon, lat);

                mService.setElevationTile(t);
                BitmapHolder elevBitmap = mService.getElevationBitmap();
                double elev = -1;
                // Load only if needed.
                if(null != elevBitmap) {
                    int x = (int)Math.round(t.getOffsetX(lon)) + elevBitmap.getWidth() / 2;
                    int y = (int)Math.round(t.getOffsetY(lat)) + elevBitmap.getHeight() / 2;
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
        if(mDraw ) {

            return;
        }
        
        if(null != mClosestTask) {
            mClosestTask.cancel(true);
        }
        mLongTouchDestination = null;
        mClosestTask = new ClosestAirportTask();

        double lon2,lat2;
        if (mPref.isTrackUp()) {
            double c_x = mOrigin.getOffsetX(mGpsParams.getLongitude());
            double c_y = mOrigin.getOffsetY(mGpsParams.getLatitude());
            double thetab = mGpsParams.getBearing();

            double p[];
            p = Helper.rotateCoord(c_x, c_y, thetab,x,y);
            lon2 = mOrigin.getLongitudeOf(p[0]);
            lat2 = mOrigin.getLatitudeOf(p[1]);
        }
        else {
            lon2 = mOrigin.getLongitudeOf(x);
            lat2 = mOrigin.getLatitudeOf(y);
        }
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
        mElevationTask.running = false;
        mElevationThread.interrupt();
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
        	if(msg.obj instanceof ElevationUpdate) {
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

    public void setLayerType(String type) {
        mLayerType = type;
        if(mService == null) {

        }
        else if(mLayerType.equals("NEXRAD")) {
            mService.getRadarLayer().parse();
        }
        else if(mLayerType.equals("METAR")) {
            mService.getMetarLayer().parse();
        }

        invalidate();
    }

}
