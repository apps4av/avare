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

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Pan;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Scale;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.shapes.Tile;
import com.ds.avare.shapes.TileMap;
import com.ds.avare.shapes.TrackShape;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.GestureInterface;
import com.ds.avare.touch.MultiTouchController;
import com.ds.avare.touch.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.touch.MultiTouchController.PointInfo;
import com.ds.avare.touch.MultiTouchController.PositionAndScale;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

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
     * Current destination
     */
    private Destination                 mDestination;
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
     * For use of text alignment
     */
    private float                       mFontHeight;
   
    /**
     * Task that would draw tiles on bitmap.
     */
    private TileDrawTask                mTileDrawTask; 
    
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
    
    private boolean                    mShown;
    
    private Preferences                 mPref;
    
    private float                      mTextDiv;
    
    private TextPaint                   mTextPaint;
    
    private Layout                      mWeatherLayout;
    
    private Typeface                    mFace;
    
    /**
     * These are longitude and latitude at top left (0,0)
     */
    private Origin                      mOrigin;
    
    /*
     * Track to destination
     */
    private TrackShape                  mTrackShape;
    
    /*
     * Max tiles in each dimension.
     */
    private TileMap                      mTiles;
    private int                         mWeatherColor;
    
    /*
     * Projection of a touch point
     */
    private Projection                  mPointProjection;
    
    /*
     * When the last time around obstacles were calculated
     */
    private Long                        mObstaclesLastTime;
    
    /*
     * Obstacles
     */
    private LinkedList<Obstacle>        mObstacles;
    
    /*
     * Obstacles task
     */
    private ObstaclesTask              mObstaclesTask;
    
    /*
     * If showing track up
     */
    private boolean                   mTrackUp;
    
    /*
     * Shadow length 
     */
    private static final int SHADOW = 4;
    
    /*
     * Text on screen color
     */
    private static final int TEXT_COLOR = Color.WHITE; 
    private static final int TEXT_COLOR_OPPOSITE = Color.BLACK; 
    
    /*
     * Periodic async task
     */
    private static final long PERIODIC_QUERY_TIME = 10 * 1000; 
    
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
        mDestination = null;
        mImageDataSource = null;
        mGpsParams = new GpsParams(null);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mShown = false;
        mTrackShape = null;
        mWeatherColor = Color.BLACK;
        mPointProjection = null;
        mObstaclesLastTime = System.currentTimeMillis();
        mTrackUp = false;
        
        mPref = new Preferences(context);
        mTextDiv = mPref.isPortrait() ? 24.f : 12.f;
        
        mFace = Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf");
        mPaint.setTypeface(mFace);
        mFontHeight = 8; // This is just double of all shadows

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(mFace);

        setOnTouchListener(this);
        mAirplaneBitmap = new BitmapHolder(context, R.drawable.plane);
        mLineBitmap = new BitmapHolder(context, R.drawable.line);
        mLineHeadingBitmap = new BitmapHolder(context, R.drawable.line_heading);
        mRunwayBitmap = new BitmapHolder(context, R.drawable.runway_extension);
        mObstacleBitmap = new BitmapHolder(context, R.drawable.obstacle);
        mMultiTouchC = new MultiTouchController<Object>(this);
        mCurrTouchPoint = new PointInfo();
        
        mGestureDetector = new GestureDetector(context, new GestureListener());
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
            mWeatherLayout = null;
        }
        mGestureDetector.onTouchEvent(e);
        return mMultiTouchC.onTouchEvent(e);
    }


    /* (non-Javadoc)
     * @see com.ds.avare.MultiTouchController.MultiTouchObjectCanvas#getDraggableObjectAtPoint(com.ds.avare.MultiTouchController.PointInfo)
     */
    public Object getDraggableObjectAtPoint(PointInfo pt) {
        return mTiles;
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
             * XXX: Till track up pan in problematic, freeze it to current location.
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
        
        if(null == mTiles) {
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
         * Do not overwhelm.
         */
        if(null != mTileDrawTask) {
            if (!mTileDrawTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                /*
                 * Always honor the latest query
                 */
                mTileDrawTask.cancel(true);
            }
        }

        mTileDrawTask = new TileDrawTask();
        mTileDrawTask.execute(mGpsParams.getLongitude(), mGpsParams.getLatitude());
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
  
        if(null != mTiles) {
            
            for(int tilen = 0; tilen < mTiles.getTilesNum(); tilen++) {
                
                BitmapHolder tile = mTiles.getTile(tilen);
                
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
                 * Pretty straightforward. Pan and draw individual tiles.
                 */
                tile.getTransform().setScale(mScale.getScaleFactor(), mScale.getScaleCorrected());
                tile.getTransform().postTranslate(
                        getWidth()  / 2.f
                        - BitmapHolder.WIDTH  / 2.f * mScale.getScaleFactor() 
                        + ((tilen % mTiles.getXTilesNum()) * BitmapHolder.WIDTH - BitmapHolder.WIDTH * (int)(mTiles.getXTilesNum() / 2)) * mScale.getScaleFactor()
                        + mPan.getMoveX() * mScale.getScaleFactor()
                        + mPan.getTileMoveX() * BitmapHolder.WIDTH * mScale.getScaleFactor()
                        - (float)mMovement.getOffsetLongitude() * mScale.getScaleFactor(),
                        
                        getHeight() / 2.f 
                        - BitmapHolder.HEIGHT / 2.f * mScale.getScaleCorrected()  
                        + mPan.getMoveY() * mScale.getScaleCorrected()
                        + ((tilen / mTiles.getXTilesNum()) * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT * (int)(mTiles.getYTilesNum() / 2)) * mScale.getScaleCorrected() 
                        + mPan.getTileMoveY() * BitmapHolder.HEIGHT * mScale.getScaleCorrected()
                        - (float)mMovement.getOffsetLatitude() * mScale.getScaleCorrected());
                
                Bitmap b = tile.getBitmap();
                if(null != b) {
                    canvas.drawBitmap(b, tile.getTransform(), mPaint);
                }
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
         * Draw TFRs, weather
         */            
        LinkedList<TFRShape> shapes = null;
        if(null != mService) {
            shapes = mService.getTFRShapes();
        }
        if(null != shapes) {
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(8);
            mPaint.setShadowLayer(0, 0, 0, 0);
            for(int shape = 0; shape < shapes.size(); shape++) {
                TFRShape cshape = shapes.get(shape);
                if(cshape.isVisible()) {
                    /*
                     * Find offsets of TFR then draw it
                     */
                    cshape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
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
                    Helper.makeLine(mGpsParams.getSpeed(), Preferences.speedConversionUnit, "     ", mGpsParams.getBearing()),
                    getWidth(), getHeight() / mTextDiv * 2, mPaint);
        }
        
        /*
         * Point top right
         */
        mPaint.setColor(TEXT_COLOR);
        if(mPointProjection != null) {
            mPaint.setTextAlign(Align.RIGHT);
            /*
             * Draw distance from point
             */
            canvas.drawText(Helper.makeLine(mPointProjection.getDistance(), Preferences.distanceConversionUnit, "     ", mPointProjection.getBearing()),
                    getWidth(), getHeight() / mTextDiv, mPaint);
            mPaint.setTextAlign(Align.LEFT);
            canvas.drawText(mPointProjection.getGeneralDirectionFrom(),
                    0, getHeight() / mTextDiv, mPaint);
        }
        else if(mDestination != null) {
            mPaint.setTextAlign(Align.RIGHT);
            /*
             * Else dest
             */
            canvas.drawText(mDestination.toString(),
                    getWidth(), getHeight() / mTextDiv, mPaint);
            mPaint.setTextAlign(Align.LEFT);
            String name = mDestination.getID();
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
        if(mDestination != null && null == mPointProjection) {
            if(mDestination.isFound() && mPref.isTrackEnabled() && (!mPref.isSimulationMode())) {
                if(null != mTrackShape) {
                    mPaint.setColor(Color.MAGENTA);
                    mPaint.setStrokeWidth(4);
                    mTrackShape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
                }
            }
            /*
             * Draw actual track
             */
            if(null != mLineBitmap && mGpsParams != null) {
                rotateBitmapIntoPlace(mLineBitmap, (float)mDestination.getBearing(),
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

    /**
     * 
     * @param canvas
     */
    private void drawMETARText(Canvas canvas) {
        /*
         * Draw TFRs, weather
         */
        /*
         * Write weather report
         * Use a static layout for showing as overlay and formatted to fit
         */
        float top = getHeight() / mTextDiv * 2 + mFontHeight;
        if(null != mWeatherLayout) {
            mPaint.setColor(mWeatherColor);
            mPaint.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
            canvas.drawRect(SHADOW, top, getWidth() - SHADOW, mWeatherLayout.getHeight() + top, mPaint);
            canvas.save();
            canvas.translate(SHADOW + 2, top);
            mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
            mWeatherLayout.draw(canvas);
            canvas.restore();        
        }
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
        
        if(!mPref.shouldExtendRunways()) {
            return;
        }
        if(null != mRunwayBitmap && null != mDestination && null == mPointProjection) {

            LinkedList<Runway> runways = mDestination.getRunways();
            if(runways != null) {
                
                for(Runway r : runways) {
                    /*
                     * Rotate and move to a panned location
                     */
                    float heading = r.getTrue();
                    if(Runway.INVALID == heading) {
                        continue;
                    }
                    
                    double lon = r.getLongitude();
                    double lat = r.getLatitude();
                    if(Runway.INVALID == lon || Runway.INVALID == lat) {
                        /*
                         * If we did not get any lon/lat of this runway, use airport lon/lat
                         */
                        lon = mDestination.getLocation().getLongitude();
                        lat = mDestination.getLocation().getLatitude();
                    }
                    
                    rotateBitmapIntoPlace(mRunwayBitmap, heading, lon, lat, false);
                    canvas.drawBitmap(mLineBitmap.getBitmap(), mLineBitmap.getTransform(), mPaint);

                    /*
                     * Draw it.
                     */
                    canvas.drawBitmap(mRunwayBitmap.getBitmap(), mRunwayBitmap.getTransform(), mPaint);
                }

                /*
                 * Loop again to over write text
                 */
                mPaint.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
                mPaint.setColor(TEXT_COLOR);
                mPaint.setTextAlign(Align.CENTER);

                for(Runway r : runways) {
                    /*
                     * Rotate and move to a panned location
                     */
                    float heading = r.getTrue();
                    if(Runway.INVALID == heading) {
                        continue;
                    }
                    
                    double lon = r.getLongitude();
                    double lat = r.getLatitude();
                    float x;
                    float y;
                    if(Runway.INVALID == lon || Runway.INVALID == lat) {
                        /*
                         * If we did not get any lon/lat of this runway, use airport lon/lat
                         */
                        lon = mDestination.getLocation().getLongitude();
                        lat = mDestination.getLocation().getLatitude();
                    }
                    x = (float)mOrigin.getOffsetX(lon);
                    y = (float)mOrigin.getOffsetY(lat);                        
                                        
                    /*
                     * Draw it.
                     */
                    String num = r.getNumber();
                    int xfact;
                    int yfact;
                    /*
                     * If parallel runways, draw their text displaced so it does not overlap
                     */
                    if(num.contains("C")) {
                        xfact = yfact = mRunwayBitmap.getHeight() * 3 / 4;
                    }
                    else if(num.contains("L")){
                        xfact = yfact = mRunwayBitmap.getHeight() / 2;                        
                    }
                    else {
                        xfact = yfact = mRunwayBitmap.getHeight();                                                
                    }
                    
                    /*
                     * Draw text with simple rotation math.
                     */
                    canvas.drawText(num,
                            x + xfact * (float)Math.sin(Math.toRadians(heading - 180)),
                            y - yfact * (float)Math.cos(Math.toRadians(heading - 180)), mPaint);
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
        drawRunways(canvas);
    	drawTFR(canvas);
        drawTrack(canvas);
        drawObstacles(canvas);
        drawAircraft(canvas);
        if(mTrackUp) {
            canvas.restore();
        }
    	drawMETARText(canvas);
    	drawCornerTexts(canvas);
    }    
    
    /**
     * @param destination
     */
    public void updateDestination(Destination destination) {
        /*
         * Comes from database
         */
        mDestination = destination;
        if(null != destination) {
            if(destination.isFound()) {
                /*
                 * Set pan to zero since we entered new destination
                 * and we want to show it without pan.
                 */
                mPan = new Pan();
                tfrReset();
                
                /*
                 * Reset track.
                 */
                mTrackShape = null;
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
        if(mDestination != null) {
            if(mDestination.isFound()) {
                /*
                 * Set track to current destination from init point.
                 * This should not be moved here because this is the only place we know we have
                 * correct initial GPS lon/lat.
                 */
                if(mTrackShape == null) {
                    mTrackShape = new TrackShape(mDestination);
                    mTrackShape.updateShape(new GpsParams(mDestination.getLocationInit()));
                }
            }
        }
        tfrReset();
        /*
         * Database query for new location / pan location.
         */
        dbquery(false);
        
        
        /*
         * Query obstacles every periodic time
         */
        if(Math.abs(System.currentTimeMillis() - mObstaclesLastTime) > PERIODIC_QUERY_TIME) {
            mObstaclesLastTime = System.currentTimeMillis();
            if(mPref.shouldShowObstacles()) {
                if(null != mObstaclesTask) {
                    if (!mObstaclesTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                        /*
                         * Always honor the latest query
                         */
                        mObstaclesTask.cancel(true);
                    }
                }
                mObstaclesTask = new ObstaclesTask();
                mObstaclesTask.execute(params.getLongitude(), params.getLatitude(), (int)params.getAltitude());
            }
            else {
                mObstacles = null;
            }
        }
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
        else if (null != mDestination) {
            mGpsParams = new GpsParams(mDestination.getLocation());
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
     * Set tiles to use. Set to null at finish. 
     * @param xtiles
     * @param ytiles
     */
    public void setTiles(TileMap tiles) {
        if(null == tiles) {
            if(null != mTileDrawTask) {
                if (!mTileDrawTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                    /*
                     * Always honor the latest query
                     */
                    mTileDrawTask.cancel(true);
                }
            }
        }
        else {
            tiles.setOrientation();
        }
        mTiles = tiles;
    }
    

    /**
     * @author zkhan
     *
     */
    private class TileDrawTask extends AsyncTask<Object, Void, Boolean> {
        double offsets[] = new double[2];
        double p[] = new double[2];
        double lon;
        double lat;
        int     movex;
        int     movey;
        String        tileNames[];
        private Tile centerTile;
        private Tile gpsTile;
        

        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {
            

            /*
             * Load tiles from image files in background, then send them to handler.
             */
            lon = (Double)vals[0];
            lat = (Double)vals[1];

            /*
             * Now draw in background
             */
            gpsTile = mImageDataSource.findClosest(lon, lat, offsets, p);
            
            
            if(gpsTile == null) {
                return false;
            }
            
            movex = mPan.getTileMoveXWithoutTear();
            movey = mPan.getTileMoveYWithoutTear();
            
            String newt = gpsTile.getNeighbor(movey, movex);
            centerTile = mImageDataSource.findTile(newt);
            if(null != centerTile) {
                mScale.setScaleAt(centerTile.getLatitude());
            }
            else {
                return false;
            }
            
            if(null == mTiles) {
                return false;
            }
            
            /*
             * Neighboring tiles with center and pan
             */
            int i = 0;
            tileNames = new String[mTiles.getTilesNum()];
            for(int tiley = -(int)(mTiles.getYTilesNum() / 2) ; 
                    tiley <= (mTiles.getYTilesNum() / 2); tiley++) {
                for(int tilex = -(int)(mTiles.getXTilesNum() / 2); 
                        tilex <= (mTiles.getXTilesNum() / 2) ; tilex++) {
                    tileNames[i++] = centerTile.getNeighbor(tiley, tilex);
                }
            }
            
            /*
             * Load tiles, draw in UI thread
             */
            mTiles.reload(tileNames);
            
            /*
             * Update TFR shapes if they exist in this area.
             */
            LinkedList<TFRShape> shapes = mService.getTFRShapes();
            if(null != shapes) {
                for(int shape = 0; shape < shapes.size(); shape++) {
                    shapes.get(shape).prepareIfVisible(centerTile.getLongitude(),
                            centerTile.getLatitude());
                }
            }
                        
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            /*
             * This runs on UI
             */
            if(true == result) {
                
                /*
                 * Back buffer to front buffer
                 */
                if(null == mTiles) {
                    return;
                }
                mTiles.flip();
                
                /*
                 * And show message if we leave current area to double touch.
                 */
                if(
                        ((movex != 0 && mPan.getTileMoveX() == 0) ||
                        (movey != 0 && mPan.getTileMoveY() == 0)) && 
                        (mShown == false)
                        ) {
                    mShown = true;
                    Toast.makeText(mContext, mContext.getString(R.string.ReturnOriginal),
                            Toast.LENGTH_SHORT).show();
                }
                                
                /*
                 * And pan
                 */
                mPan.setTileMove(movex, movey);
                mService.setPan(mPan);
                mMovement = new Movement(offsets, p);
                mService.setMovement(mMovement);
            }

            invalidate();
        }
    }    
    
    
    /**
     * @author zkhan
     *
     */
    private class ObstaclesTask extends AsyncTask<Object, Void, Boolean> {

        LinkedList<Obstacle> obs;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {
            
            if(null == mService || null == mGpsParams) {
                return false;
            }
            obs = mService.getDBResource().findObstacles(
                    (Double)vals[0],
                    (Double)vals[1],
                    (Integer)vals[2]);
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if(true == result) {
                mObstacles = obs;
            }
            invalidate();
        }
    }

    
    /**
     * @author zkhan
     *
     */
    private class WeatherTask extends AsyncTask<Object, Void, Boolean> {

        private String weather;
        private String airport;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {
            
            airport = (String)vals[0];
            if(null == mService) {
                weather = null;
                return false;
            }
            if(null != airport) {
                weather = mService.getWeatherCache().get(airport);
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if(null != weather) {
                String tokens[] = weather.split(",");
                if(tokens.length >= 2) {
                    mWeatherColor = WeatherHelper.metarColor(tokens[0]);
                    mTextPaint.setColor(Color.WHITE);
                    mWeatherLayout = new StaticLayout(tokens[1].trim(), mTextPaint, getWidth() - SHADOW * 2,
                            Layout.Alignment.ALIGN_NORMAL, 1, 0, true);               
                }
            }
            invalidate();
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
            dbquery(true);
            mShown = false;
            tfrReset();
            return true;
        }

        /* (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
            
            /*
             * XXX:
             * For track up, currently there is no math to find anything with long press.
             */
            if(mTrackUp) {
                return;
            }
            
            /*
             * on long press, find a point where long press was done
             */
            double x = mCurrTouchPoint.getX();
            double y = mCurrTouchPoint.getY();

            /*
             * Notify activity of gesture.
             */
            
            double lon2 = mOrigin.getLongitudeOf(x);
            double lat2 = mOrigin.getLatitudeOf(y);
            mPointProjection = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon2, lat2);
            
            String text = null;
                       
            /*
             * Get TFR text if touched on its top
             */
            LinkedList<TFRShape> shapes = null;
            if(null != mService) {
                shapes = mService.getTFRShapes();
            }
            if(null != shapes) {
                for(int shape = 0; shape < shapes.size(); shape++) {
                    TFRShape cshape = shapes.get(shape);
                    if(cshape.isVisible()) {

                        /*
                         * Hijack weather color
                         */
                        mWeatherColor = Color.RED;
                        text = cshape.getTextIfTouched(x, y);
                        if(null != text) {
                            break;
                        }
                    }
                }
            }

            /*
             * Get airport touched on, but it can block and not be in BG task because user
             * is pressing on a point and it does not matter if zoom/pan hangs for a fraction sec.
             */
            String airport = null;
            if(null != mService) {
                airport = mService.getDBResource().findClosestAirportID(lon2, lat2);
                if((null != airport) && (null != mGestureCallBack)) {
                    mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, airport);
                }
                if((null == airport) && (null != mGestureCallBack)) {
                    /*
                     * Not found; send lat/lon
                     */
                    airport = "" + Helper.truncGeo(lat2) + "&" + Helper.truncGeo(lon2);
                    mGestureCallBack.gestureCallBack(GestureInterface.LONG_PRESS, airport);
                }
            }
        
            /*
             * If weather shows
             */
            if(text == null) {
                if(null != airport) {
                    new WeatherTask().execute(airport);
                }
            }
            else {
                /*
                 * Take TFR text over weather text
                 */
                mTextPaint.setColor(Color.WHITE);
                mWeatherLayout = new StaticLayout(text.trim(), mTextPaint, getWidth(),
                        Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
            }
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
    public void setTrackUp(boolean b) {
        mTrackUp = b;
        postInvalidate();
    }
}
