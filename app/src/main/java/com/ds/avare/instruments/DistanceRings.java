/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.instruments;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

/**
 * 
 * A static class because we do not want allocations in the draw task where this
 * is called.
 * 
 * @author zkhan, rwalker
 *
 */
public class DistanceRings {

    /**
     * Distance ring drawing constants
     */
    public static final int COLOR_SPEED_RING     = Color.rgb(178, 255, 102);
    
    public static final int RING_INNER  = 0;
    public static final int RING_MIDDLE = 1;
    public static final int RING_OUTER  = 2;
    public static final int RING_SPEED  = 3;
    
    private static final int STALLSPEED = 25;
    private static final int RING_INNER_SIZE[]  = { 1,  2,  5, 10, 20,  40};
    private static final int RING_MIDDLE_SIZE[] = { 2,  5, 10, 20, 40,  80};
    private static final int RING_OUTER_SIZE[]  = { 5, 10, 20, 40, 80, 160};
    private static final int RINGS_1_2_5     = 0;
    private static final int RINGS_2_5_10    = 1;
    private static final int RINGS_5_10_20   = 2;
    private static final int RINGS_10_20_40  = 3;
    private static final int RINGS_20_40_80  = 4;
    private static final int RINGS_40_80_160 = 5;
    
    private static float mRings[]      = {0, 0, 0, 0};
    private static String mRingsText[] = {null, null, null, null};

    // Members that get set at object construction
    private StorageService mService;
    private Context mContext;
    private Paint mPaint;
    private float mDipToPix;
    private Preferences mPref;
    
    /***
     * Instrument to handle displaying of rings of various radius based
     * upon speed and zoom factors
     * 
     * @param service background storage service
     * @param context application context
     * @param textSize size of the text to draw
     */
    public DistanceRings(StorageService service, Context context, float textSize) {
    	mService = service;
    	mContext = context;
        mDipToPix = Helper.getDpiToPix(context);
        mPref = new Preferences(context);
    	mPaint = new Paint();
    	mPaint.setAntiAlias(true);
    	mPaint.setTextSize(textSize);
    }
    
    /***
     * Render the distance and speed rings to the display canvas
     * 
     * @param canvas draw on this canvas
     * @param origin the x/y origin of the upper left of the canvas
     * @param scale zoom scale
     * @param move movement
     * @param trackUp north vs track always up
     * @param gpsParams current gps location data
     */
    public void draw(Canvas canvas, Origin origin, Scale scale, Movement move, 
    		boolean trackUp, GpsParams gpsParams) {

    	// If the preference to even draw these rings is not enabled, then return
    	if (mPref.getDistanceRingType() == 0) {
        	return;
        }
        
         // Calculate the size of distance and speed rings
        double currentSpeed = gpsParams.getSpeed();
        calculateRings(mContext, scale, origin, gpsParams.getLatitude(), currentSpeed);

        // Get our current position. That will be the center of all the rings
        float x = (float) (origin.getOffsetX(gpsParams.getLongitude()));
        float y = (float) (origin.getOffsetY(gpsParams.getLatitude()));
        
        double bearing = gpsParams.getBearing();	/* What direction are we headed */
        if(trackUp) {		// If our direction is always to the top, then set
        	bearing = 0;	// our bearing to due up as well
        }

        /*
         * Set the paint accordingly
         */
    	mPaint.setStrokeWidth(3 * mDipToPix);
    	mPaint.setShadowLayer(0, 0, 0, 0);
        mPaint.setColor(mPref.getDistanceRingColor());
        mPaint.setStyle(Style.STROKE);
    	mPaint.setAlpha(0x7F);

    	/*
         * Draw the 3 distance circles now
         */
        canvas.drawCircle(x, y, mRings[DistanceRings.RING_INNER], mPaint);
        canvas.drawCircle(x, y, mRings[DistanceRings.RING_MIDDLE], mPaint);
        canvas.drawCircle(x, y, mRings[DistanceRings.RING_OUTER], mPaint);

        /*
         * Restore some paint settings back to what they were so as not to
         * mess things up
         */
        mPaint.setStyle(Style.FILL);
        mPaint.setColor(Color.WHITE);
        
        float adjX = (float) Math.sin((bearing - 10) * Math.PI / 180);	// Distance ring numbers, offset from
        float adjY = (float) Math.cos((bearing - 10) * Math.PI / 180);	// the course line for readability

        mService.getShadowedText().draw(canvas, mPaint,
        		mRingsText[DistanceRings.RING_INNER], Color.BLACK,
                x + mRings[DistanceRings.RING_INNER] * adjX, 
                y - mRings[DistanceRings.RING_INNER] * adjY);
        mService.getShadowedText().draw(canvas, mPaint,
        		mRingsText[DistanceRings.RING_MIDDLE], Color.BLACK,
                x + mRings[DistanceRings.RING_MIDDLE] * adjX, 
                y - mRings[DistanceRings.RING_MIDDLE] * adjY);
        mService.getShadowedText().draw(canvas, mPaint,
        		mRingsText[DistanceRings.RING_OUTER], Color.BLACK,
                x + mRings[DistanceRings.RING_OUTER] * adjX, 
                y - mRings[DistanceRings.RING_OUTER] * adjY);
    
        /*
         * Draw our "speed ring" if one was calculated for us
         */
        if ((mRings[DistanceRings.RING_SPEED] != 0)) {

        	adjX = (float) Math.sin((bearing + 10) * Math.PI / 180);	// So the speed ring number does
            adjY = (float) Math.cos((bearing + 10) * Math.PI / 180);	// not overlap the distance ring

            mPaint.setStyle(Style.STROKE);
            mPaint.setColor(DistanceRings.COLOR_SPEED_RING);
            canvas.drawCircle(x, y, mRings[DistanceRings.RING_SPEED], mPaint);

            
            mPaint.setStyle(Style.FILL);
            mPaint.setColor(Color.WHITE);

            mService.getShadowedText().draw(canvas, mPaint, 
            		String.format("%d", mPref.getTimerRingSize()), Color.BLACK, 
            		x + mRings[DistanceRings.RING_SPEED] * adjX, 
            		y - mRings[DistanceRings.RING_SPEED] * adjY);
        }
    }
    
    /**
     * 
     * @param context
     * @param pref
     * @param scale
     * @param movement
     * @param speed
     */
    public void calculateRings(Context context,
            Scale scale, Origin origin, double lat, double speed) {
        
        mRings[0] = 0;
        mRings[1] = 0;
        mRings[2] = 0;
        mRings[3] = 0;
        
        /*
         * Conversion factor for pixPerNm in case we are configured in some other units
         */
        double fac = 1;
        if(mPref.getDistanceUnit().equals(context.getString(R.string.UnitMile))) {
            fac *= Preferences.NM_TO_MI;
        }
        else if(mPref.getDistanceUnit().equals(context.getString(R.string.UnitKilometer))) {
            fac *= Preferences.NM_TO_KM;
        }

        /*
         * Set the ring sizes to 2/5/10 nm/mi/km
         */
        int ringScale = RINGS_2_5_10;
        
        /*
         * If we are supposed to dynamically scale the rings, then do that now
         */
        if(mPref.getDistanceRingType() == 1) {
            int macro = scale.getMacroFactor();
            /* the larger totalZoom is, the more zoomed in we are  */
            if(macro <= 1 && scale.getScaleFactorRaw() > 1) {  
                ringScale = RINGS_1_2_5;        
            } 
            else if(macro <= 1 && scale.getScaleFactorRaw() <= 1) {  
                ringScale = RINGS_2_5_10;
            } 
            else if (macro <= 2) {
                ringScale = RINGS_5_10_20;
            } 
            else if (macro <= 4) {
                ringScale = RINGS_10_20_40;
            } 
            else if (macro <= 8) {
                ringScale = RINGS_20_40_80;
            }  
            else {
                ringScale = RINGS_40_80_160;
            } 
        }
        
        /*
         *  Draw our "speed ring" if we are going faster than stall speed 
         */
        if(speed >= STALLSPEED && mPref.getTimerRingSize() != 0) {
            /*
             * its / 60 as units is in minutes
             */
            mRings[RING_SPEED] = (float) ((float)origin.getPixelsInNmAtLatitude((speed / 60) * mPref.getTimerRingSize() / fac, lat));
        }

        /*
         * Calculate the radius of the 3 rings to display
         */
        mRings[RING_INNER]  = (float)(origin.getPixelsInNmAtLatitude(RING_INNER_SIZE[ringScale] / fac, lat));
        mRings[RING_MIDDLE] = (float)(origin.getPixelsInNmAtLatitude(RING_MIDDLE_SIZE[ringScale] / fac, lat));
        mRings[RING_OUTER]  = (float)(origin.getPixelsInNmAtLatitude(RING_OUTER_SIZE[ringScale] / fac, lat));
        
        mRingsText[RING_INNER]  = String.format("%d", RING_INNER_SIZE[ringScale]);
        mRingsText[RING_MIDDLE] = String.format("%d", RING_MIDDLE_SIZE[ringScale]);
        mRingsText[RING_OUTER]  = String.format("%d", RING_OUTER_SIZE[ringScale]);
    }
}
