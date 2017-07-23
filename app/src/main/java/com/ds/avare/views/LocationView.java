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
import android.support.v4.view.GravityCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.adsb.NexradBitmap;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Boundaries;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Fix;
import com.ds.avare.place.NavAid;
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
import com.ds.avare.shapes.ShapeFileShape;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.Tile;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.BasicOnScaleGestureListener;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.LongPressedDestination;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.userDefinedWaypoints.Waypoint;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DisplayIcon;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.InfoLines.InfoLineFieldLoc;
import com.ds.avare.utils.NavAidHelper;
import com.ds.avare.utils.NavComments;
import com.ds.avare.utils.ViewParams;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.AdsbWeatherCache;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * @author zkhan
 * @author plinel
 * 
 * This is a view that user sees 99% of the time. Has moving map on it.
 */
public class LocationView extends View implements OnTouchListener {
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
    private BitmapHolder               mLineHeadingBitmap;
    private BitmapHolder               mObstacleBitmap;

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
     * Task that finds closets airport.
     */
    private ClosestAirportTask          mClosestTask; 

    /**
     * Storage service that contains all the state
     */
    private StorageService              mService;
    
    private DataSource                  mImageDataSource;
    
    /**
     * To tell activity to do something on a gesture or touch
     */
    private GestureInterface            mGestureCallBack; 
    
    
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
     * Is it drawing?
     */
    private boolean                   mDraw;

    private ViewParams                  mViewParams;
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

    /*
     * dip to pix scaling factor
     */
    private float                      mDipToPix;

    Point mDownFocusPoint;
    int mTouchSlopSquare;
    boolean mDoCallbackWhenDone;
    LongTouchDestination mLongTouchDestination;

    private ScaleGestureDetector mScaleDetector;

    public LongTouchDestination getLongTouchDestination() {return mLongTouchDestination;}

    /**
     * @param context
     */
    private void setup(Context context) {
        
        /*
         * Set up all graphics.
         */
        mContext = context;
        mViewParams = new ViewParams();
        mViewParams.setPan(new Pan());
        mViewParams.setScale(new Scale(mViewParams.getMaxScale()));
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


        setOnTouchListener(this);
        mAirplaneBitmap = DisplayIcon.getDisplayIcon(context, mPref);
        mLineBitmap = new BitmapHolder(context, R.drawable.line);
        mLineHeadingBitmap = new BitmapHolder(context, R.drawable.line_heading);
        mRunwayBitmap = new BitmapHolder(context, R.drawable.runway_extension);
        mObstacleBitmap = new BitmapHolder(context, R.drawable.obstacle);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        // We're going to give the user twice the slop as normal
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop() * 2;
        mTouchSlopSquare = touchSlop * touchSlop;
        mDoCallbackWhenDone = false;
             
        mDipToPix = Helper.getDpiToPix(context);
        mScaleDetector = new ScaleGestureDetector(context, new ComplexOnScaleGestureListener(mViewParams, this));

        // set zoom level?
        if(mPref.getPersistZoom()) {
            float zoom=mPref.getZoomLevel();
            if(zoom>=mViewParams.getMinScale()&&zoom<=mViewParams.getMaxScale()) {
                mViewParams.setScaleFactor(zoom);
                mViewParams.getScale().setScaleFactor(mViewParams.getScaleFactor());
            }
        }
    }
    
    /**
     * 
     */
    private void updateCoordinates() {
        mOrigin.update(mGpsTile, getWidth(), getHeight(), mGpsParams, mViewParams.getPan(), mViewParams.getScale());
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public LocationView(Context context) {
        this(context, null, 0);
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public LocationView(Context context, AttributeSet aset) {
        this(context, aset, 0);
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
            if(mMacro != mViewParams.getScale().getMacroFactor()) {
                loadTiles();
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

                    if( mService.getPlan().findClosePointId(lon, lat, mViewParams.getScale().getScaleFactor()) > -1 ) {
                        mDragPlanPoint = mService.getPlan().findClosePointId(lon, lat, mViewParams.getScale().getScaleFactor());
                        mDragStartedX = e.getX();
                        mDragStartedY = e.getY();
                    } else {
                        startClosestAirportTask(e.getX(), e.getY());
                    }
                } else {
                    startClosestAirportTask(e.getX(), e.getY());
                }
                
            }

            mGestureCallBack.gestureCallBack(GestureInterface.TOUCH, (LongTouchDestination)null);
            
            // Remember this point so we can make sure we move far enough before losing the long press
            mDoCallbackWhenDone = false;
            mDownFocusPoint = getFocusPoint(e);

            //startClosestAirportTask(e.getX(), e.getY());
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

        // Any time there are two fingers on the screen, set the PointProjection to get
        //  the distance/bearing between the two points. If there aren't two pointers
        //  make sure to nullify the projection
        if( e.getPointerCount() == 2) {
            double x0 = e.getX(0);
            double y0 = e.getY(0);
            double x1 = e.getX(1);
            double y1 = e.getY(1);

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

        if(bPassToGestureDetector) {
            // Once we break out of the square or stop the long press, keep sending
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_UP) {
                mDownFocusPoint = null;
                //mPointProjection = null;
                if(mClosestTask != null) {
                    mClosestTask.cancel(true);
                }
            }
            boolean retVal = mScaleDetector.onTouchEvent(e);
            retVal = mGestureDetector.onTouchEvent(e) || retVal;
        }
        return true;
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
        if(ctx.pref.useAdsbWeather()) {
            TFRShape.draw(ctx, mService.getAdsbTFRShapes(), null == mPointProjection);
        }
        else {
            TFRShape.draw(ctx, mService.getTFRShapes(), null == mPointProjection);
        }
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawShapes(Canvas canvas, DrawingContext ctx) {
        ShapeFileShape.draw(ctx, mService.getShapeShapes(), null == mPointProjection);
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawAirSigMet(Canvas canvas, DrawingContext ctx) {
        /*
         * Draw Air/Sigmet
         */
        if(ctx.pref.useAdsbWeather()) {
            MetShape.draw(ctx, mService.getAdsbWeather().getAirSigMet(), null == mPointProjection);
        }
        else {
            MetShape.draw(ctx, mService.getInternetWeatherCache().getAirSigMet(), null == mPointProjection);
        }
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawLayers(Canvas canvas, DrawingContext ctx) {
        if(mLayerType == null || null != mPointProjection || 0 == mPref.showLayer()) {
            return;
        }

        if(mLayerType.equals("Plate")) {
            BitmapHolder b = mService.getPlateDiagram();

            if(b == null || b.getBitmap() == null) {
                return;
            }

            // find georef info
            float[] matrix = mService.getMatrix();
            if(null == matrix) {
                return;
            }

            //draw
            float dx = matrix[0];
            float dy = matrix[1];
            float lonTopLeft = matrix[2];
            float latTopLeft = matrix[3];
            float lonBottomRight = lonTopLeft + b.getBitmap().getWidth() / dx;
            float latBottomRight = latTopLeft + b.getBitmap().getHeight() / dy;


            float x0 = (float)mOrigin.getOffsetX(lonTopLeft);
            float y0 = (float)mOrigin.getOffsetY(latTopLeft);
            float x1 = (float)mOrigin.getOffsetX(lonBottomRight);
            float y1 = (float)mOrigin.getOffsetY(latBottomRight);

            float scalex = (x1 - x0) / b.getBitmap().getWidth();
            float scaley = (y1 - y0) / b.getBitmap().getHeight();

            b.getTransform().setScale(scalex, scaley);
            b.getTransform().postTranslate(x0, y0);

            mPaint.setAlpha(mPref.showLayer());
            canvas.drawBitmap(b.getBitmap(), b.getTransform(), mPaint);
            mPaint.setAlpha(mPref.showLayer());

        }
        else if(ctx.pref.useAdsbWeather()) {
            if (mLayerType.equals("NEXRAD")) {
                NexradBitmap.draw(ctx, mService.getAdsbWeather().getNexrad(),
                        mService.getAdsbWeather().getNexradConus(), null == mPointProjection);
            }
            else if (mLayerType.equals("METAR")) {
                AdsbWeatherCache.drawMetars(ctx, mService.getAdsbWeather().getAllMetars(), null == mPointProjection);
                AdsbWeatherCache.drawUATTowers(ctx, mService.getAdsbWeather().getAllUatTowers(), null == mPointProjection);
            }
        }
        else {

            if (mLayerType.equals("NEXRAD")) {
                // draw nexrad
                mLayer = mService.getRadarLayer();
            } else if (mLayerType.equals("METAR")) {
                // draw metar flight catergory
                mLayer = ctx.service.getMetarLayer();
            } else {
                mLayer = null;
                return;
            }

            /*
             * layer is way too old.
             */
            if (mLayer.isOld(ctx.pref.getExpiryTime())) {
                return;
            }

            mPaint.setAlpha(mPref.showLayer());
            mLayer.draw(canvas, mPaint, mOrigin);
            mPaint.setAlpha(255);
        }
    }

    /**
     *
     * @param canvas
     * @param ctx
     */
    private void drawTraffic(Canvas canvas, DrawingContext ctx) {
        Traffic.draw(ctx, mService.getTrafficCache().getTraffic(),
                mService.getTrafficCache().getOwnAltitude(), mGpsParams, mPref.getAircraftICAOCode(), null == mPointProjection);
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

        // Blue inside, black outside
        Paint.Cap oldCaps = mPaint.getStrokeCap();
        mPaint.setStrokeCap(Paint.Cap.ROUND); // We use a wide line. Without ROUND the line looks broken.
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(6 * mDipToPix);
        mService.getDraw().drawShape(canvas, mPaint, mOrigin);

        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(2 * mDipToPix);
        mService.getDraw().drawShape(canvas, mPaint, mOrigin);
        mPaint.setStrokeCap(oldCaps); // Restore the Cap we had before drawing
    }

    private void drawObstacles(Canvas canvas, DrawingContext ctx) {
        if(mPref.showObstacles()) {
            LinkedList<Obstacle> obs = mService.getObstacles();
            if((obs != null) && (null == mPointProjection)) {
                mPaint.setShadowLayer(0, 0, 0, 0);
                for (Obstacle o : obs) {
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
        mService.getDistanceRings().draw(canvas, mOrigin, mViewParams.getScale(), mMovement, mPref.isTrackUp(), mGpsParams);
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
        if(mPref.isDrawTracks() && (null == mPointProjection)) {
                
            /*
             *  Set the brush color and width
             */
            mPaint.setColor(Color.CYAN);
            mPaint.setStrokeWidth(6 * mDipToPix);
            mPaint.setStyle(Paint.Style.FILL);

            mService.getKMLRecorder().getShape().drawShape(canvas, mOrigin, mViewParams.getScale(), mMovement, mPaint, mPref.isNightMode(), true);
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
    private void drawVASI(Canvas canvas) {
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
     */
    private void drawGameTFRs(DrawingContext ctx) {
        if(mPointProjection == null) {
            mService.getGameTFRs().draw(ctx);
        }

    }

    /***
     * Draw the edge distance markers if configured to do so
     * @param canvas what to draw them on
     */
    private void drawEdgeMarkers(Canvas canvas) {
    	if(mPref.isShowEdgeTape()) {
	        if(mPointProjection == null) {
		        int x = (int)(mOrigin.getOffsetX(mGpsParams.getLongitude()));
		        int y = (int)(mOrigin.getOffsetY(mGpsParams.getLatitude()));
		        float pixPerNm = mOrigin.getPixelsInNmAtLatitude(1, mGpsParams.getLatitude());
		      	mService.getEdgeTape().draw(canvas, mViewParams.getScale(), pixPerNm, x, y,
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
        	mService.getCap().draw(canvas, mOrigin, mViewParams.getScale());
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

        MainActivity mainActivity = (MainActivity) mContext;
        if (mainActivity.getDrawerLayout() != null && mainActivity.getDrawerLayout().isDrawerVisible(GravityCompat.START)) {
            // when the left nav drawer is slid open, an ACTION_DOWN event is registered but for some reason the ACTION_UP
            // event is not and therefore the mPointProjection does not get nulled out. so we manually prevent going
            // into projection mode whenever the drawer is open.
            mPointProjection = null;
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
        ctx.scale = mViewParams.getScale();
        ctx.pan = mViewParams.getPan();
        ctx.pref = mPref;
        ctx.runwayPaint = mRunwayPaint;
        ctx.view = this;

        // Call the draw routines for the items that rotate with
        // the chart
        drawTiles(canvas, ctx);
        drawLayers(canvas, ctx);
        drawDrawing(canvas, ctx);
        drawCapGrids(canvas, ctx);
        drawTraffic(canvas, ctx);
        drawObstacles(canvas, ctx);
        drawTFR(canvas, ctx);
        drawGameTFRs(ctx);
        drawShapes(canvas, ctx);
        drawAirSigMet(canvas, ctx);
        drawTracks(canvas, ctx);
        drawTrack(canvas, ctx);
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
                mViewParams.setPan(new Pan());
                mService.setPan(mViewParams.getPan());
                updateCoordinates();                
            }
        }
    }

    /**
     * 
     */
    public void forceReload() {
        loadTiles();
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
        if(mGpsTile != null) {
            double offsets[] = new double[2];

            /*
             * No need to load tiles when we are on current tile
             */
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
        loadTiles();
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
        mViewParams.setPan(mService.getPan());
        if(null == mViewParams.getPan()) {
            mViewParams.setPan(new Pan());
            mService.setPan(mViewParams.getPan());
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
        loadTiles();
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
    private void loadTiles() {
        if(mService == null) {
            return;
        }

        if(mImageDataSource == null) {
            return;
        }

        TileMap map = mService.getTiles();
        map.loadTiles(mGpsParams.getLongitude(), mGpsParams.getLatitude(), mViewParams.getPan(), mMacro, mViewParams.getScale(), mGpsParams.getBearing(),
                new GenericCallback() {
                    @Override
                    public Object callback(Object map, Object tu) {
                        TileMap.TileUpdate t = (TileMap.TileUpdate) tu;
                        ((TileMap) map).flip();

                        /*
                         * Set move with pan after new tiles are finally loaded
                         */
                        mViewParams.getPan().setMove((float) (mViewParams.getPan().getMoveX() * t.factor), (float) (mViewParams.getPan().getMoveY() * t.factor));

                        int index = Integer.parseInt(mPref.getChartType());
                        String type = Boundaries.getChartType(index);

                        mGpsTile = t.gpsTile;
                        mOnChart = type + "\n" + t.chart;
                        /*
                         * And pan
                         */
                        mViewParams.getPan().setTileMove(t.movex, t.movey);
                        mMovement = new Movement(t.offsets);
                        mService.setMovement(mMovement);
                        mMacro = mViewParams.getScale().getMacroFactor();
                        mViewParams.getScale().updateMacro();
                        updateCoordinates();
                        invalidate();

                        return null;
                    }
                });
    }


    private class ComplexOnScaleGestureListener
            extends BasicOnScaleGestureListener {

        private int macroScaleFactor;

        public ComplexOnScaleGestureListener(ViewParams viewParams, View view) {
            super(viewParams, view);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // Stop any rubber banding
            mDragPlanPoint = -1;

            macroScaleFactor = mViewParams.getScale().getMacroFactor();
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Stop any rubber banding
            mPointProjection = null;
            super.onScaleEnd(detector);
            invalidate();
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            mViewParams.setScaleFactor(mViewParams.getScaleFactor() * scaleFactor);
            mViewParams.setScaleFactor(Math.max(mViewParams.getMinScale(), Math.min(mViewParams.getScaleFactor(), mViewParams.getMaxScale())));
            mViewParams.getScale().setScaleFactor(mViewParams.getScaleFactor());

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float moveX = mViewParams.getPan().getMoveX() + ((focusX - mLastFocusX) / mViewParams.getScaleFactor()) / mMacro;
            float moveY = mViewParams.getPan().getMoveY() + ((focusY - mLastFocusY) / mViewParams.getScaleFactor()) / mMacro;
            mLastFocusX = focusX;
            mLastFocusY = focusY;

            mViewParams.getPan().setMove(moveX, moveY);

            mPref.setZoomLevel(mViewParams.getScaleFactor());

            updateCoordinates();
            invalidate();

            return true;
        }
    }

    /**
     * @author zkhan
     *
     */
    private class ClosestAirportTask extends AsyncTask<Object, String, LongPressedDestination> {
        private Double lon;
        private Double lat;
        private String tfr = "";
        private String textMets = "";
        private String sua;
        private String layer;
        private LinkedList<Airep> aireps;
        private LinkedList<String> runways;
        private Taf taf;
        private WindsAloft wa;
        private Metar metar;
        private String elev;
        private Vector<NavAid> navaids;
        private ArrayList<LongPressedDestination> locations;

        /* (non-Javadoc)
         * @param vals[0] longitude of point to find. mutually exclusive with vals[2]
         * @param vals[1] latitude of point to find. mutually exclusive with vals[2]
         * @param vals[2] LongPressedDestination of destination to look up.
         * @see android.os.AsyncTask#doInBackground(Params[])
         */     
        @Override
        protected LongPressedDestination doInBackground(Object... vals) {

            locations = new ArrayList<LongPressedDestination>();

            Thread.currentThread().setName("Closest");
            if(null == mService) {
                return null;
            }

            // if the user is moving instead of doing a long press, give them a chance
            // to cancel us before we start doing anything
            try {
                Thread.sleep(200);
            }
            catch(Exception e) {
            }
            
            if(isCancelled())
                return null;

            // If a set destination was passed in, we'll use it. Get its GPS coords and if it's a
            //  base get the weather for it. Otherwise parse out the GPS data and add it as a location
            LongPressedDestination setDest = null;
            if( vals.length > 2 ) {
                setDest = (LongPressedDestination)vals[2];

                if( setDest.getType().equals(Destination.GPS) ) {
                    String[] coords = setDest.getName().split("&");
                    lat = Double.parseDouble(coords[0]);
                    lon = Double.parseDouble(coords[1]);
                } else {
                    String loc = mService.getDBResource().findLonLat(setDest.getName(), setDest.getType());
                    if (loc != null) {
                        String[] coords = loc.split(",");
                        lon = Double.parseDouble(coords[0]);
                        lat = Double.parseDouble(coords[1]);
                    } else
                        return null;

                    if( setDest.getType().equals(Destination.BASE)) {
                        setLocationWeather(setDest);
                    }
                }
            } else {
                lon = (Double)vals[0];
                lat = (Double)vals[1];
                locations.add(new LongPressedDestination("" + Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon), Destination.GPS, 0.0, lat, lon));
            }

            if(isCancelled())
                return null;

            // Collect nearby points

            // Airports
            Airport[] airports = mService.getDBResource().findClosestAirports(lon, lat, "0");
            if( airports != null ) {
                for (Airport a : airports) {
                    if (a == null) break;

                    double navaidDistance = Projection.getStaticDistance(lon, lat,
                            a.getLon(), a.getLat());

                    // Don't add the airport if it's already the set destination
                    // For now, don't limit the distance
                    if ( /*navaidDistance < (Preferences.NEARBY_TOUCH_DISTANCE / mViewParams.getScaleFactor()) &&*/ setDest == null ||
                            !(a.getId().equals(setDest.getName()) && setDest.getType().equals(Destination.BASE))) {

                        locations.add(new LongPressedDestination(a.getId(), Destination.BASE, navaidDistance, a.getLat(), a.getLon()));
                    }
                }
            }

            if(isCancelled())
                return null;

            // Navaids
            navaids = mService.getDBResource().findNavaidsNearby(lat, lon);
            if( navaids != null) {
                for (NavAid n : navaids) {
                    double navaidDistance = Projection.getStaticDistance(lon, lat,
                            n.getCoords().getLongitude(), n.getCoords().getLatitude());
                    // For now, don't limit based on distance
                    if ( /*navaidDistance < (Preferences.NEARBY_TOUCH_DISTANCE / mViewParams.getScaleFactor()) &&*/ setDest == null ||
                            !(n.getLocationId().equals(setDest.getName()) && setDest.getType().equals(Destination.NAVAID))) {
                        locations.add(new LongPressedDestination(n.getLocationId(), Destination.NAVAID, navaidDistance, n.getCoords().getLatitude(), n.getCoords().getLongitude()));
                    }
                }
            }

            if(isCancelled())
                return null;

            // Fixes
            ArrayList<Fix> fixes = mService.getDBResource().findClosestFixes(lat, lon);
            for (Fix fix : fixes) {
                double navaidDistance = Projection.getStaticDistance(lon, lat,
                        fix.getLon(), fix.getLat());
                // For now, don't limit based on distance
                // Don't add RNAV WP or Mil Rep fix types
                if ( /*navaidDistance < (Preferences.NEARBY_TOUCH_DISTANCE / mViewParams.getScaleFactor()) &&*/ ( setDest == null ||
                        !( fix.getId().equals(setDest.getName()) && setDest.getType().equals(Destination.FIX))) &&
                        !(fix.getType().equals("YRNAV-WP") || fix.getType().equals("YMIL-REP-PT") || fix.getType().equals("NAWY-INTXN")))
                {
                    locations.add(new LongPressedDestination(fix.getId(), Destination.FIX, navaidDistance, fix.getLat(), fix.getLon()));
                }
            }

            if(isCancelled())
                return null;

            // Sort and truncate
            Collections.sort(locations);
            if( locations.size() > Preferences.MAX_NEARBY_POINTS ) locations = new ArrayList<LongPressedDestination>(locations.subList(0, Preferences.MAX_NEARBY_POINTS));

            // Get the METARs for the airports in the list if the weather is new enough
            for (LongPressedDestination nearbyLoc : locations) {
                if (nearbyLoc.getType() == Destination.BASE) {
                    setLocationWeather(nearbyLoc);
                }
            }

            if(isCancelled())
                return null;

            // If not a set dest, figure out which to use
            int indexToUse = 0;
            if( setDest == null ) {
                String currentType = "";
                for( int i = 0; i < locations.size(); i++) {
                    LongPressedDestination dest = locations.get(i);
                    if( dest.getDistance() < ((Preferences.NEARBY_TOUCH_DISTANCE / mViewParams.getScaleFactor() ) / 5) ) {
                        if( currentType.equals("") || currentType.equals(Destination.GPS) || currentType.equals(Destination.NAVAID) || currentType.equals(Destination.FIX) ) {
                            indexToUse = i;
                            currentType = dest.getType();
                        }
                    }
                }
                setDest = locations.remove(indexToUse);
                lat = setDest.getLat();
                lon = setDest.getLon();

                // If we're changing the destination, get the new distances
                if( indexToUse > 0 ) {
                    for( int i = 0; i < locations.size(); i++) {
                        LongPressedDestination dest = locations.get(i);
                        double navaidDistance = Projection.getStaticDistance(lon, lat,
                                dest.getLon(), dest.getLat());
                        dest.setDistance(navaidDistance);
                    }
                }
            }

            if(isCancelled())
                return null;

            /*
             * Get TFR tfr if touched on its top
             */
            LinkedList<TFRShape> shapes = null;
            List<AirSigMet> mets = null;
            if(null != mService) {
                if(mPref.useAdsbWeather()) {
                    shapes = mService.getAdsbTFRShapes();
                    mets = mService.getAdsbWeather().getAirSigMet();
                }
                else {
                    shapes = mService.getTFRShapes();
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
                         * Set MET
                         */
                        textMets += cshape.getHTMLMetOnTouch(mContext, mets.get(i), lon, lat);
                    }
                }
            }            

            if(isCancelled()) {
                return null;
            }

            if( setDest.getType().equals(Destination.BASE) ){
                final String airport = setDest.getName();

                taf = mService.getDBResource().getTAF(airport);
                if(isCancelled()) {
                    return null;
                }
                
                metar = mService.getDBResource().getMETAR(airport);   
                if(isCancelled()) {
                    return null;
                }
                if (metar==null) { // in no metar on the field, try to find the closest metar
                    metar = mService.getDBResource().getClosestMETAR(lat,lon);
                    if(isCancelled()) {
                        return null;
                    }
                }

                runways = mService.getDBResource().findRunways(airport);
                if(isCancelled()) {
                    return null;
                }
                
                elev = mService.getDBResource().findElev(airport);
                if(isCancelled()) {
                    return null;
                }
            }
            
            /*
             * ADSB gets this info from weather cache
             */
            if(!mPref.useAdsbWeather()) {              
                aireps = mService.getDBResource().getAireps(lon, lat);
                if(isCancelled()) {
                    return null;
                }
                
                wa = mService.getDBResource().getWindsAloft(lon, lat);
                if(isCancelled()) {
                    return null;
                }
                
                sua = mService.getDBResource().getSua(lon, lat);
                if(isCancelled()) {
                    return null;
                }

                if(mLayer != null) {
                    layer = mLayer.getDate();
                }
                if(isCancelled()) {
                    return null;
                }
            }

            mPointProjection = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon, lat);

            return setDest;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(LongPressedDestination destination) {
            if(null != mGestureCallBack && null != mPointProjection && null != destination) {
                mLongTouchDestination = new LongTouchDestination();
                mLongTouchDestination.destination = destination; // here we assign destination name
                mLongTouchDestination.info = Math.round(mPointProjection.getDistance()) + Preferences.distanceConversionUnit +
                        "(" + mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()) + ") " +
                        Helper.correctConvertHeading(Math.round(Helper.getMagneticHeading(mPointProjection.getBearing(), mGpsParams.getDeclinition()))) + '\u00B0';

                /*
                 * Clear old weather
                 */
                mService.getAdsbWeather().sweep();
                mService.getAdsbTfrCache().sweep();

                /*
                 * Do not background ADSB weather as its a RAM opertation and quick,
                 * also avoids concurrent mod exception.
                 */

                if(mPref.useAdsbWeather()) {
                    taf = mService.getAdsbWeather().getTaf(destination.getName()); // MAYBE SHOULD MAKE THIS CONDITIONAL ON TYPE?
                    metar = mService.getAdsbWeather().getMETAR(destination.getName());
                    aireps = mService.getAdsbWeather().getAireps(lon, lat);
                    wa = mService.getAdsbWeather().getWindsAloft(lon, lat);
                    layer = mService.getAdsbWeather().getNexrad().getDate();
                    sua = mService.getAdsbWeather().getSua();
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
                mLongTouchDestination.sua = sua;
                mLongTouchDestination.layer = layer;
                mLongTouchDestination.locations = locations;
                //ideally we would pass altitude AGL for navaid reception calculations
                mLongTouchDestination.navaids = new NavAidHelper(mContext, lon, lat, mGpsParams.getAltitude()).toHtmlString(navaids);
                if(metar != null) {
                    String bestRunway = WeatherHelper.getBestRunway(metar.rawText, runways);
                    mLongTouchDestination.performance =
                            WeatherHelper.getMetarTime(metar.rawText) + "\n" +
                            mContext.getString(R.string.DensityAltitude) + " " +
                            WeatherHelper.getDensityAltitude(metar.rawText, elev) + "\n" +
                                    (!bestRunway.isEmpty() ?
                                    (mContext.getString(R.string.BestRunway) + " " + bestRunway) : "");
                }
                
                // If the long press event has already occurred, we need to do the gesture callback here
                if(mDoCallbackWhenDone) {
                    mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, mLongTouchDestination);
                }
            }
            invalidate();
        }


        private void setLocationWeather(LongPressedDestination dest ) {

            // Only get weather if it's new or we're using ADSB weather
            boolean isWeatherOld = mService.getInternetWeatherCache().isOld(mPref.getExpiryTime());
            boolean useAdsbWeather = mPref.useAdsbWeather();

            // Return if the weather is old and we're not using ADSB
            if( isWeatherOld && !useAdsbWeather) return;

            Metar metar;
            if( useAdsbWeather ) {
                metar = mService.getAdsbWeather().getMETAR(dest.getName());
            } else {
                metar = mService.getDBResource().getMETAR(dest.getName());

                if (metar == null) { // in no metar on the field, try to find the closest metar
                    metar = mService.getDBResource().getClosestMETAR(dest.getLat(), dest.getLon());
                }
            }
            if (metar != null) {
                dest.setWeatherColor(WeatherHelper.metarColorString(metar.flightCategory));
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
        mViewParams.setPan(new Pan());
        if(mService != null) {
            mService.setPan(mViewParams.getPan());
            mService.getTiles().forceReload();
        }
        loadTiles();
        updateCoordinates();
        postInvalidate();
    }
            
    /**
     * @author zkhan
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            // Don't pan if rubber-banding is in progress
            if(mDragPlanPoint >= 0) {
                invalidate();
                return true;
            }

            // Don't pan or draw if multi-touch scaling is under way
            if( mViewParams.isScaling() ) {
                return false;
            }

            if(mDraw && mService != null) {
                float x = e2.getX() ;
                float y = e2.getY() ;

                /*
                 * Threshold the drawing so we do not generate too many points
                 */
                if (mPref.isTrackUp()) {
                    double thetab = mGpsParams.getBearing();
                    double p[] = new double[2];
                    double c_x = mOrigin.getOffsetX(mGpsParams.getLongitude());
                    double c_y = mOrigin.getOffsetY(mGpsParams.getLatitude());
                    p = Helper.rotateCoord(c_x, c_y, thetab, x, y);
                    mService.getDraw().addPoint((float) p[0], (float) p[1], mOrigin);
                } else {
                    mService.getDraw().addPoint(x, y, mOrigin);
                }
                invalidate();
                return true;
            }

            // Panning
            if( !mDraw ) {
                float moveX = mViewParams.getPan().getMoveX() - (distanceX / mViewParams.getScaleFactor()) / mMacro;
                float moveY = mViewParams.getPan().getMoveY() - (distanceY / mViewParams.getScaleFactor()) / mMacro;

                mViewParams.getPan().setMove(moveX, moveY);
                loadTiles();
            }

            updateCoordinates();
            invalidate();
            return true;
        }

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

    public void showLocationModal(LongPressedDestination dest) {
        mDoCallbackWhenDone = true;
        mClosestTask = new ClosestAirportTask();
        mClosestTask.execute(null, null, dest);
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
    public void zoomOut() {
        mViewParams.getScale().zoomOut();
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
