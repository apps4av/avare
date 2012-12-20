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

import com.ds.avare.MultiTouchController.MultiTouchObjectCanvas;
import com.ds.avare.MultiTouchController.PointInfo;
import com.ds.avare.MultiTouchController.PositionAndScale;
import com.ds.avare.R;
import com.ds.avare.BitmapHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
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
     * Where all tiles reside
     */
    private BitmapHolder               mTiles[];

    /**
     * The plane on screen
     */
    private BitmapHolder               mAirplaneBitmap;
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
     * Current touch mPoint
     */
    private String                      mPoint;
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
    
    private ImageDataSource             mImageDataSource;
    
    /**
     * Our current location
     */
    private Tile                        mGpsTile;
    private Tile                        mCenterTile;
    
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
    private int                         mXtiles;
    private int                         mYtiles;
    private int                         mWeatherColor;
    
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
        mPoint = null;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mShown = false;
        mTrackShape = null;
        mWeatherColor = Color.BLACK;
        
        mPref = new Preferences(context);
        mTextDiv = mPref.isPortrait() ? 24.f : 12.f;
        
        int[] tilesdim = mPref.getTilesNumber();
        mXtiles = tilesdim[0];
        mYtiles = tilesdim[1];
        
        mTiles = new BitmapHolder[mXtiles * mYtiles];

        mFace = Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf");
        mPaint.setTypeface(mFace);
        FontMetrics fm = mPaint.getFontMetrics();
        mFontHeight =  fm.bottom - fm.top;

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(mFace);

        setOnTouchListener(this);
        mAirplaneBitmap = new BitmapHolder(context, R.drawable.plane);
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
     * Should'nt Android GC do this automatically? Not yet.
     */
    public void freeTiles() {
        /*
         * Free up tiles
         */
        for(int tile = 0; tile < mXtiles * mYtiles; tile++) {
            if(null != mTiles[tile]) {
                mTiles[tile].recycle();
            }
        }
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
            mPoint = null;
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
             * Multi-touch is zoom, single touch is pan
             */
            if(mPan.setMove(newObjPosAndScale.getXOff(), newObjPosAndScale.getYOff())) {
                /*
                 * Query when we have moved one tile. This will happen in background.
                 */
                dbquery(true);
            }            
        }
        else {
            /*
             * Clamp scaling.
             */
            mPoint = null; /* this is to not support mPoint distance when zooming */
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
        
        if(!mImageDataSource.isOpen()) {
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
     * 
     * @param canvas
     */
    private void drawTiles(Canvas canvas) {
        mPaint.setShadowLayer(0, 0, 0, 0);
  
        if(null != mTiles) {
            
            for(int tilen = 0; tilen < (mXtiles * mYtiles); tilen++) {
                /*
                 * Scale, then move under the plane which is at center
                 */
                boolean nochart = false;
                if(mTiles[tilen] == null) {
                    nochart = true;
                }
                else if(mTiles[tilen].getBitmap() == null) {
                    nochart = true;
                }
                if(nochart) {
                    continue;
                }

                /*
                 * Pretty straightforward. Pan and draw individual tiles.
                 */
                mTiles[tilen].getTransform().setScale(mScale.getScaleFactor(), mScale.getScaleCorrected());
                mTiles[tilen].getTransform().postTranslate(
                        getWidth()  / 2.f
                        - BitmapHolder.WIDTH  / 2.f * mScale.getScaleFactor() 
                        + ((tilen % mXtiles) * BitmapHolder.WIDTH - BitmapHolder.WIDTH * (int)(mXtiles / 2)) * mScale.getScaleFactor()
                        + mPan.getMoveX() * mScale.getScaleFactor()
                        + mPan.getTileMoveX() * BitmapHolder.WIDTH * mScale.getScaleFactor()
                        - (float)mMovement.getOffsetLongitude() * mScale.getScaleFactor(),
                        
                        getHeight() / 2.f 
                        - BitmapHolder.HEIGHT / 2.f * mScale.getScaleCorrected()  
                        + mPan.getMoveY() * mScale.getScaleCorrected()
                        + ((tilen / mXtiles) * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT * (int)(mYtiles / 2)) * mScale.getScaleCorrected() 
                        + mPan.getTileMoveY() * BitmapHolder.HEIGHT * mScale.getScaleCorrected()
                        - (float)mMovement.getOffsetLatitude() * mScale.getScaleCorrected());
                
                Bitmap b = mTiles[tilen].getBitmap();
                if(null != b) {
                    canvas.drawBitmap(b, mTiles[tilen].getTransform(), mPaint);
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
        if(mPref.shouldTFRAndMETARShow()) {
            
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
    }


    /**
     * 
     * @param canvas
     */
    private void drawCornerTextsAndTrack(Canvas canvas) {

        /*
         * Misc text in the information text location on the view like GPS status,
         * Maps status, and point destination/destination bearing, altitude, ...
         * Add shadows for better viewing
         */
        mPaint.setShadowLayer(4, 4, 4, Color.BLACK);
        mPaint.setColor(Color.WHITE);

        mPaint.setTextAlign(Align.LEFT);
        /*
         * Speed
         */
        canvas.drawText("" + Math.round(mGpsParams.getSpeed()) + "kt",
                0, getHeight() / mTextDiv, mPaint);
        /*
         * Altitude
         */
        canvas.drawText("" + Math.round(mGpsParams.getAltitude()) + "ft",
                0, getHeight() - mFontHeight, mPaint);
        
        mPaint.setTextAlign(Align.RIGHT);

        /*
         * Heading
         */
        canvas.drawText("" + Math.round(mGpsParams.getBearing()) + '\u00B0',
                getWidth(), getHeight() - mFontHeight, mPaint);

        /*
         * Status/destination top right
         */
        if(mErrorStatus != null) {
            mPaint.setColor(Color.RED);
            canvas.drawText(mErrorStatus,
                    getWidth(), getHeight() / mTextDiv * 2, mPaint);
        }
        
        /*
         * Point above error status
         */
        mPaint.setColor(Color.WHITE);
        if(mPoint != null) {
            canvas.drawText(mPoint,
                    getWidth(), getHeight() / mTextDiv, mPaint);            
        }
        else if(mDestination != null) {
            canvas.drawText(mDestination.toString(),
                    getWidth(), getHeight() / mTextDiv, mPaint);
            if(mDestination.isFound() && mPref.isTrackEnabled() && (!mPref.isSimulationMode())) {
                if(null != mTrackShape) {
                    mPaint.setColor(Color.MAGENTA);
                    mPaint.setStrokeWidth(4);
                    mTrackShape.drawShape(canvas, mOrigin, mScale, mMovement, mPaint, mFace);
                }            
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
        if(mPref.shouldTFRAndMETARShow()) {
            /*
             * Write weather report
             * Use a static layout for showing as overlay and formatted to fit
             */
            if(null != mWeatherLayout) {
                canvas.save();
                canvas.translate(0, getHeight() / mTextDiv);
                mPaint.setColor(mWeatherColor);
                mPaint.setShadowLayer(4, 4, 4, Color.BLACK);
                canvas.drawRect(0, 0, mWeatherLayout.getWidth(), mWeatherLayout.getHeight(), mPaint);
                mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
                mWeatherLayout.draw(canvas);
                canvas.restore();        
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
        if(null != mAirplaneBitmap) {
            
            /*
             * Rotate and move to a panned location
             */                
            mAirplaneBitmap.getTransform().setRotate((float)mGpsParams.getBearing(),
                    mAirplaneBitmap.getWidth() / 2.f,
                    mAirplaneBitmap.getHeight() / 2.f);
            
            mAirplaneBitmap.getTransform().postTranslate(
                    getWidth() / 2.f
                    - mAirplaneBitmap.getWidth()  / 2.f
                    + mPan.getMoveX() * mScale.getScaleFactor(),
                    getHeight() / 2.f
                    - mAirplaneBitmap.getHeight()  / 2.f
                    + mPan.getMoveY() * mScale.getScaleCorrected());
    
            canvas.drawBitmap(mAirplaneBitmap.getBitmap(), mAirplaneBitmap.getTransform(), mPaint);
        }	
    }


    /**
     * @param canvas
     * Does pretty much all drawing on screen
     */
    private void drawMap(Canvas canvas) {
    	
    	mPaint.setTextSize(getHeight() / mTextDiv);
        mTextPaint.setTextSize(getHeight() / mTextDiv * 3 / 4);
    	
    	drawTiles(canvas);
    	drawTFR(canvas);
    	drawAircraft(canvas);

    	drawMETARText(canvas);
    	drawCornerTextsAndTrack(canvas);
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
                mTrackShape = new TrackShape(mDestination);
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
        if(mTrackShape != null) {
            mTrackShape.updateShape(params);
        }
        tfrReset();
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

    private BitmapHolder findTile(String name) {
        for(int tilen = 0; tilen < (mXtiles * mYtiles); tilen++) {
            if(null == mTiles[tilen]) {
                continue;
            }
            if(name.equals(mTiles[tilen].getName())) {
                return(mTiles[tilen]);
            }
        }
        return(null);
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
        int movex;
        int movey;
        BitmapHolder mTilesTmp[] = new BitmapHolder[mXtiles * mYtiles];
        String tileNames[] = new String[mXtiles * mYtiles];
        
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
            mGpsTile = mImageDataSource.findClosest(lon, lat, offsets, p);
            
            
            if(mGpsTile == null) {
                return false;
            }
            
            movex = mPan.getTileMoveXWithoutTear();
            movey = mPan.getTileMoveYWithoutTear();
            
            String newt = mGpsTile.getNeighbor(movey, movex);
            mCenterTile = mImageDataSource.findTile(newt);
            if(null != mCenterTile) {
                mScale.setScaleAt(mCenterTile.getLatitude());
            }
            else {
                return false;
            }
            
            /*
             * Neighboring tiles with center and pan
             */
            int i = 0;
            for(int tiley = -(int)(mYtiles / 2) ; 
                    tiley <= (mYtiles / 2); tiley++) {
                for(int tilex = -(int)(mXtiles / 2); 
                        tilex <= (mXtiles / 2) ; tilex++) {
                    tileNames[i++] = mCenterTile.getNeighbor(tiley, tilex);
                }
            }
            
            /*
             * Load tiles, draw in UI thread
             */
            
            for(int tilen = 0; tilen < (mXtiles * mYtiles); tilen++) {
                if(null != tileNames[tilen]) {
                    /*
                     * reuse if found
                     */
                    mTilesTmp[tilen] = findTile(tileNames[tilen]);
                    /*
                     * load new if not found
                     * mTilesTmp is in correct order.
                     */
                    if(mTilesTmp[tilen] == null) {
                        mTilesTmp[tilen] = new BitmapHolder(mContext, mPref, tileNames[tilen]);
                    }
                }
            }
            
            /*
             * Update TFR shapes if they exist in this area.
             */
            LinkedList<TFRShape> shapes = mService.getTFRShapes();
            if(null != shapes) {
                for(int shape = 0; shape < shapes.size(); shape++) {
                    shapes.get(shape).prepareIfVisible(mCenterTile.getLongitude(),
                            mCenterTile.getLatitude());
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
            int tilen;
            if(true == result) {
                
                for(tilen = 0; tilen < (mXtiles * mYtiles); tilen++) {
                    if(null != mTilesTmp[tilen]) {
                        mTiles[tilen] = mTilesTmp[tilen];
                        continue;
                    }
                }
                
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
    private class AirportTask extends AsyncTask<Object, Void, Boolean> {

        private String weather;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            double lon = (Double)vals[0];
            double lat = (Double)vals[1];
            String airport = mService.getDBResource().findClosestAirportID(lon, lat);
            if(null != airport) {
                weather = NetworkHelper.getMETAR(mContext, airport);
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
                    mWeatherLayout = new StaticLayout(tokens[1].trim(), mTextPaint, getWidth(),
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
             * on long press, find a point where long press was done
             */
            double x = mCurrTouchPoint.getX();
            double y = mCurrTouchPoint.getY();

            double lon2 = mOrigin.getLongitudeOf(x);
            double lat2 = mOrigin.getLatitudeOf(y);
            Projection p = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon2, lat2);
            
            double brg = p.getBearing();
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
             * Draw distance from point
             */
            mPoint = "" + (int)p.getDistance() + "nm " + p.getGeneralDirectionFrom() + 
                    "," + Math.round(brg) + '\u00B0' + mContext.getString(R.string.To);                
            
            if(mPref.shouldTFRAndMETARShow()) {
                /*
                 * If weather shows
                 */
                if(text == null) {
                    new AirportTask().execute(lon2, lat2);
                }
                else {
                    /*
                     * Take TFR text over weather text
                     */
                    mTextPaint.setColor(Color.WHITE);
                    mWeatherLayout = new StaticLayout(text, mTextPaint, getWidth() / 2,
                            Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
                }
            }           
        }        
    }
}
