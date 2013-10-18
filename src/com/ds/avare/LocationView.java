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


import java.util.LinkedList;
import java.util.List;

import com.ds.avare.gdl90.NexradBitmap;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Pan;
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
import com.ds.avare.utils.Helper;
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
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * @author zkhan
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
    private ObstacleTask                mObstacleTask; 
    private Thread                      mObstacleThread;

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
    
    private float                      mTextDiv;
    
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
     * Shadow length 
     */
    private static final int SHADOW = 4;
    
    /**
     * Resolution pixel / lon , lat of center tile
     */
    private double                     mPx;
    private double                     mPy;
    
    private double                     mAdjustPan;
    
    /*
     *  Copy the existing paint to a new paint so we don't mess it up
     */
    Paint mRunwayPaint;

    /*
     * Text on screen color
     */
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    

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
        mPx = 1;
        mPy = 1;
        mAdjustPan = 1;
        mOnChart = null;
        mTrackUp = false;
        mImageDataSource = null;
        mGpsParams = new GpsParams(null);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPointProjection = null;
        mDraw = false;
        
        mPref = new Preferences(context);
        mTextDiv = mPref.isPortrait() ? 24.f : 15.f;
        
        mFace = Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf");
        mPaint.setTypeface(mFace);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(mFace);

        mTileDrawTask = new TileDrawTask();
        mTileDrawThread = new Thread(mTileDrawTask);
        mTileDrawThread.start();

        mObstacleTask = new ObstacleTask();
        mObstacleThread = new Thread(mObstacleTask);
        mObstacleThread.start();

        setOnTouchListener(this);
        mAirplaneBitmap = new BitmapHolder(context, mPref.isHelicopter() ? R.drawable.heli : R.drawable.plane);
        mLineBitmap = new BitmapHolder(context, R.drawable.line);
        mLineHeadingBitmap = new BitmapHolder(context, R.drawable.line_heading);
        mRunwayBitmap = new BitmapHolder(context, R.drawable.runway_extension);
        mObstacleBitmap = new BitmapHolder(context, R.drawable.obstacle);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        
        mGestureDetector = new GestureDetector(context, new GestureListener());
        
        mRunwayPaint = new Paint(mPaint);
    }
    
    /**
     * 
     */
    private void tfrReset() {
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
       
    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent e) {
        if(e.getAction() == MotionEvent.ACTION_UP) {
            /*
             * Do not draw point. Only when long press and down.
             */
            mPointProjection = null;
        }
        mGestureDetector.onTouchEvent(e);
        return mMultiTouchC.onTouchEvent(e);
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
             * TODO: track up pan is problematic, so freeze it to current location.
             * 
             */
            if(mPan.setMove(
                    mTrackUp ? 
                            mPan.getMoveX() : 
                            newObjPosAndScale.getXOff(),
                    mTrackUp ? 
                            mPan.getMoveY() : 
                            newObjPosAndScale.getYOff())) {
                /*
                 * Query when we have moved one tile. This will happen in background.
                 */
                dbquery(true);
            }
        }
        else {
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
        tfrReset();
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
            
            for(int tilen = 0; tilen < mService.getTiles().getTilesNum(); tilen++) {
                
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
        if(null != shapes) {
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(4); //TODO Should probably be dynamic based on device resolution
            mPaint.setShadowLayer(0, 0, 0, 0);
            for(int shape = 0; shape < shapes.size(); shape++) {
                shapes.get(shape).drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
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
        if(null != mService) {
            mets = mService.getInternetWeatherCache().getAirSigMet();
        }
        if(null != mets) {
            mPaint.setStrokeWidth(4); //TODO Should probably be dynamic based on device resolution
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
                    met.shape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
                }
            }
        }
    }

    /**
     * 
     * @param canvas
     */
    private void drawCornerTexts(Canvas canvas) {

        /*
         * Misc text in the information text location on the view like GPS status,
         * Maps status, and point destination/destination bearing, altitude, ...
         * Add shadows for better viewing
         */
        if(mPref.shouldShowBackground()) {
            mPaint.setShadowLayer(0, 0, 0, 0);
            mPaint.setColor(TEXT_COLOR_OPPOSITE);
            mPaint.setAlpha(0x7f);
            canvas.drawRect(0, 0, getWidth(), getHeight() / mTextDiv * 2 + SHADOW, mPaint);
            mPaint.setAlpha(0xff);
        }
        mPaint.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
        

        /*
         * Status
         */
        if(mErrorStatus != null) {
            mPaint.setTextAlign(Align.RIGHT);
            mPaint.setColor(Color.RED);
            canvas.drawText(mErrorStatus,
                    getWidth(), getHeight() / mTextDiv * 2, mPaint);
        }
        else {
            
            mPaint.setColor(TEXT_COLOR);

            mPaint.setTextAlign(Align.RIGHT);
            /*
             * Heading, Speed
             */
            canvas.drawText(
                    Helper.makeLine(mGpsParams.getSpeed(), Preferences.speedConversionUnit, null, mGpsParams.getBearing(), mGpsParams.getDeclinition()),
                    getWidth(), getHeight() / mTextDiv * 2, mPaint);
            
        }

        /*
         * Variation
         */
        /*
         * Altitude
         */
        mPaint.setColor(TEXT_COLOR);
        mPaint.setTextAlign(Align.LEFT);
        canvas.drawText(Helper.calculateAltitudeFromThreshold(mThreshold), 0, getHeight() / mTextDiv * 2, mPaint);

        /*
         * Point top right
         */
        mPaint.setColor(TEXT_COLOR);
        if(mPointProjection != null) {
            mPaint.setTextAlign(Align.RIGHT);
            /*
             * Draw distance from point
             */
            canvas.drawText(Helper.makeLine(mPointProjection.getDistance(), Preferences.distanceConversionUnit, "     ", mPointProjection.getBearing(), mGpsParams.getDeclinition()),
                    getWidth(), getHeight() / mTextDiv, mPaint);
            mPaint.setTextAlign(Align.LEFT);
            canvas.drawText(mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()),
                    0, getHeight() / mTextDiv, mPaint);
        }
        else if(mService != null && mService.getDestination() != null) {
            mPaint.setTextAlign(Align.RIGHT);
            /*
             * Else dest
             */
            canvas.drawText(mService.getDestination().toString(),
                    getWidth(), getHeight() / mTextDiv, mPaint);
            mPaint.setTextAlign(Align.LEFT);
            String name = mService.getDestination().getID();
            if(name.contains("&")) {
                /*
                 * If this string is too long, cut it.
                 */
                name = Destination.GPS;
            }
            else if (name.length() > 8) {
                name = name.substring(0, 7);
            }
            canvas.drawText(name,
                    0, getHeight() / mTextDiv, mPaint);
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
            if(mPref.isTrackEnabled() && (!mPref.isSimulationMode())) {
                mPaint.setColor(Color.MAGENTA);
                mPaint.setStrokeWidth(4);
                if(mService.getDestination().isFound() && !mService.getPlan().isActive()) {
                    mService.getDestination().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
                }
                else if (mService.getPlan().isActive()) {
                    mService.getPlan().getTrackShape().drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);                    
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
        mPaint.setStrokeWidth(4);
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
    private void drawNexrad(Canvas canvas) {
        if(mService == null) {
            return;
        }
        SparseArray<NexradBitmap> bitmaps = mService.getNexradImages();
        if(null == bitmaps) {
            return;
        }
        for(int i = 0; i < bitmaps.size(); i++) {
            int key = bitmaps.keyAt(i);
            NexradBitmap b = bitmaps.get(key);
            BitmapHolder bitmap = b.getBitmap();
            
            if(null != bitmap) {                 
                /*
                 * 
                 */
                float scalex = (float)(b.getScaleX() / mPx);
                float scaley = (float)(b.getScaleY() / mPy);
                float x = (float)mOrigin.getOffsetX(b.getLonTopLeft());
                float y = (float)mOrigin.getOffsetY(b.getLatTopLeft());
                bitmap.getTransform().setScale(scalex * mScale.getScaleFactor(), scaley * mScale.getScaleCorrected());
                bitmap.getTransform().postTranslate(x, y);
    
                canvas.drawBitmap(bitmap.getBitmap(), bitmap.getTransform(), mPaint);
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
				 *  Converts 1 dip (device independent pixel) into its equivalent
				 */
				float px = TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 1, getResources()
								.getDisplayMetrics());

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
							mRunwayBitmap.getTransform(), mPaint);
					/*
					 * Get the canvas x/y coordinates of the runway itself
					 */
					float x = (float) mOrigin.getOffsetX(lon);
					float y = (float) mOrigin.getOffsetY(lat);

					/*
					 * The runway number, i.e. What's
                     * painted on the runway
					 */

					String num = r.getNumber(); 
					/*
					 * If there are parallel runways, offset their text
					 * so it does not overlap
					 */
				
					if (num.contains("C")) {
						xfactor = yfactor = mRunwayBitmap.getHeight() * 3 / 4;
					} else if (num.contains("L")) {
						xfactor = yfactor = mRunwayBitmap.getHeight() / 2;
					} else {
						xfactor = yfactor = mRunwayBitmap.getHeight();
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
					/*
					 *  set the width of the line. dips->px
					 */
					mRunwayPaint.setStrokeWidth(px * 4);

					/*
					 *  Get a vector perpendicular to the vector of the
					 *  runway heading bitmap
					 */
					float vXP = -(runwayNumberCoordinatesY - y);
					float vYP = (runwayNumberCoordinatesX - x);

					/*
					 * Reverse the vector of the pattern line if right
					 * traffic is indicated for this runway
					 */
					if (r.getPattern().equalsIgnoreCase("Right")) {
						vXP = -(vXP);
						vYP = -(vYP);
					}
					/*
					 * Draw the base leg of the pattern
					 */
					canvas.drawLine(runwayNumberCoordinatesX,
							runwayNumberCoordinatesY,
							runwayNumberCoordinatesX + vXP / 3,
							runwayNumberCoordinatesY + vYP / 3, mRunwayPaint);
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
					 *  Get the size of the text to draw and create a new
					 *  rectangle of that size
					 */
					Rect rect = new Rect();
					mRunwayPaint.getTextBounds(num, 0, num.length(), rect);
					if (mPref.shouldShowBackground()) {
						/*
						 * If the "show background" preference is true, draw a
						 * shaded square behind runway numbers to make them easier
						 * to see. Perhaps there's a simpler way to do this? TODO:
						 * Make this into a nice,rounded rectangle with a border
						 */				
			
						/*
						 *  Make that rectangle a little bigger to account for
						 *  the shadow effect
						 */
						rect.set(0, 0, rect.width() + SHADOW * 3, rect.height()
								+ SHADOW * 3);
						mRunwayPaint.setShadowLayer(0, 0, 0, 0);
						mRunwayPaint.setColor(TEXT_COLOR_OPPOSITE);
						mRunwayPaint.setAlpha(0x7f);

						/*
						 * Draw the rectangle off the end of its associated
						 * runway
						 */
						canvas.save();
						canvas.translate(
								runwayNumberCoordinatesX - (rect.width() / 2),
								runwayNumberCoordinatesY - (rect.height() / 2));
						canvas.drawRect(rect, mRunwayPaint);
						canvas.restore();

					}
					mRunwayPaint.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
					mRunwayPaint.setColor(TEXT_COLOR);
					mRunwayPaint.setAlpha(0xff);
					mRunwayPaint.setTextAlign(Paint.Align.CENTER);

					/*
					 * Draw the text so it's centered within the shadow
                     * rectangle, which is itself centered at the end of the
                     * extended runway centerline
					 */
					canvas.drawText(num,
							runwayNumberCoordinatesX,
							runwayNumberCoordinatesY
									+ (rect.height() / 2 - SHADOW * 2), mRunwayPaint);
					if (mTrackUp) {
						canvas.restore();
					}
				}
			}
		}
	}
    
    /**
     * @param canvas
     * Does pretty much all drawing on screen
     */
    private void drawMap(Canvas canvas) {
    	
    	mPaint.setTextSize(getHeight() / mTextDiv);
    	mRunwayPaint.setTextSize(getHeight() / mTextDiv);
        mTextPaint.setTextSize(getHeight() / mTextDiv * 3 / 4);
    	
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
        drawDrawing(canvas);
        drawRunways(canvas);
    	drawTFR(canvas);
    	drawAirSigMet(canvas);
        drawTrack(canvas);
        drawObstacles(canvas);
        drawAircraft(canvas);
        if(mTrackUp) {
            canvas.restore();
        }
    	drawCornerTexts(canvas);
    }    

    /**
     * 
     * @param factor
     */
    public void updateThreshold(float threshold) {
        mThreshold = threshold;
        invalidate();
    }

    /**
     * @param destination
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
                tfrReset();                
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

        tfrReset();
        /*
         * Database query for new location / pan location.
         */
        dbquery(false);
        
        
        /*
         * Do not overwhelm, obstacles
         */
        mObstacleTask.alt = mGpsParams.getAltitude();
        mObstacleTask.lon = mGpsParams.getLongitude();
        mObstacleTask.lat = mGpsParams.getLatitude();
        
        mObstacleThread.interrupt();
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

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Tile");

            while(running) {
                try {
                    Thread.sleep(1000 * 3600);
                }
                catch(Exception e) {
                    
                }

                if(mImageDataSource == null) {
                    continue;
                }
                
                /*
                 * Now draw in background
                 */
                gpsTile = mImageDataSource.findClosest(lon, lat, offsets, p, mScale.downSample());
                
                if(gpsTile == null) {
                    continue;
                }
                
                movex = mPan.getTileMoveXWithoutTear();
                movey = mPan.getTileMoveYWithoutTear();
                
                String newt = gpsTile.getNeighbor(movey, movex);
                centerTile = mImageDataSource.findTile(newt);
                if(null == centerTile) {
                    continue;
                }
                
                if(null == mService) {
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
                mPan.setMove((float)(mPan.getMoveX() * mAdjustPan), (float)(mPan.getMoveY() * mAdjustPan));
                mService.getTiles().reload(tileNames, mAdjustPan != 1);
                mService.getTiles().flip();

                mScale.setScaleAt(centerTile.getLatitude());
                mOnChart = centerTile.getChart();

                /*
                 * And pan
                 */
                mPan.setTileMove(movex, movey);
                mService.setPan(mPan);
                mMovement = new Movement(offsets, p);
                mService.setMovement(mMovement);
                mPy = centerTile.getPy();
                mPx = centerTile.getPx();
                
                synchronized(LocationView.this) {
                    mAdjustPan = 1;
                }
    
                LocationView.this.postInvalidate();
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
        private String text;
        private String textMets;
        private LinkedList<Airep> aireps;
        private LinkedList<String> freq;
        private Taf taf;
        private WindsAloft wa;
        private Metar metar;
        
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
            text = (String)vals[2];
            textMets = (String)vals[3];
            airport = mService.getDBResource().findClosestAirportID(lon, lat);
            if(null == airport) {
                airport = "" + Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
            }
            else {
                taf = mService.getDBResource().getTAF(airport);
                metar = mService.getDBResource().getMETAR(airport);
                freq = mService.getDBResource().findFrequencies(airport);
            }
            aireps = mService.getDBResource().getAireps(lon, lat);
            for(Airep a : aireps) {
                a.updateTextWithLocation(lon, lat, mGpsParams.getDeclinition());                
            }
            wa = mService.getDBResource().getWindsAloft(lon, lat);
            if(null != wa) {
                wa.updateStationWithLocation(lon, lat, mGpsParams.getDeclinition());
            }
            return airport;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String airport) {
            if(null != mGestureCallBack && null != mPointProjection && null != airport) {
                LongTouchDestination data = new LongTouchDestination();
                data.airport = airport;
                data.info = Math.round(mPointProjection.getDistance()) + Preferences.distanceConversionUnit +
                        "(" + mPointProjection.getGeneralDirectionFrom(mGpsParams.getDeclinition()) + ") " +
                        Helper.correctConvertHeading(Math.round(Helper.getMagneticHeading(mPointProjection.getBearing(), mGpsParams.getDeclinition()))) + '\u00B0';
                data.chart = mOnChart;
                data.tfr = text;
                data.taf = taf;
                data.metar = metar;
                data.airep = aireps;
                data.mets = textMets;
                data.wa = wa;
                data.freq = freq;
                mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, data);
            }
            invalidate();
        }
        
    }

    
    /**
     * @author zkhan
     * Find obstacles
     */
    private class ObstacleTask implements Runnable {
        public Double lon;
        public Double lat;
        public Double alt;
        
        public boolean running = true;
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        public void run() {
            Thread.currentThread().setName("Obstacle");

            while(running) {

                try {
                    Thread.sleep(1000 * 3600);
                }
                catch (Exception e) {
                    
                }
                if(null != mImageDataSource) {
                    /*
                     * Find obstacles in background as well
                     */
                    mObstacles = mImageDataSource.findObstacles(lon, lat, alt.intValue());
                }                
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
        mScale.setScaleFactor(1);
        mScale.setScaleAt(mGpsParams.getLatitude());
        if(mService != null) {
            mService.getTiles().forceReload();
        }
        dbquery(true);
        tfrReset();
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

            /*
             * In draw, long press has no meaning other than to clear the output from the activity
             */
            if(mDraw) {
                mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, null);
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
            
            double lon2 = mOrigin.getLongitudeOf(x);
            double lat2 = mOrigin.getLatitudeOf(y);
            mPointProjection = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon2, lat2);
            
            String text = null;
            String textMets = null;
                       
            /*
             * Get TFR text if touched on its top
             */
            LinkedList<TFRShape> shapes = null;
            List<AirSigMet> mets = null;
            if(null != mService) {
                shapes = mService.getTFRShapes();
                mets = mService.getInternetWeatherCache().getAirSigMet();
            }
            if(null != shapes) {
                for(int shape = 0; shape < shapes.size(); shape++) {
                    TFRShape cshape = shapes.get(shape);
                    /*
                     * Set TFR text
                     */
                    text = cshape.getTextIfTouched(x, y);
                    if(null != text) {
                        break;
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
                        textMets = cshape.getTextIfTouched(x, y);
                        if(null != textMets) {
                            break;
                        }
                    }
                }
            }

            /*
             * Get airport touched on
             */
            if(null != mClosestTask) {
                mClosestTask.cancel(true);
            }
            mClosestTask = new ClosestAirportTask();
            mClosestTask.execute(lon2, lat2, text, textMets);
        }
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
     * @param b
     */
    public boolean getDraw() {
        return mDraw;
    }

    /*
     * Gets chart on which this view is.
     */
    public String getChart() {
        return mOnChart;
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
     */
    public void cleanup() {
        mObstacleTask.running = false;
        mTileDrawTask.running = false;
        mTileDrawThread.interrupt();
        mObstacleThread.interrupt();
    }
    
    /**
     * 
     * @param in
     */
    public boolean zoomIn(boolean in) {
        if(mAdjustPan != 1) {
            /**
             * Make sure that last operation finished. 
             */
            return true;
        }
        boolean changed;
        synchronized(this) {
            int fac = mScale.getMacroFactor();
            int scale = mScale.getZoomFactor();
            if(in) {
                changed = mScale.setMacroFactor(fac / scale);
                if(changed) {
                    mAdjustPan = (double)scale;
                }
            }
            else {
                changed = mScale.setMacroFactor(fac * scale);
                if(changed) {
                    mAdjustPan = 1.0 / (double)scale;
                }
            }
            if(changed && mService != null) {
                forceReload();
            }
        }
        
        return changed;
    }
}
