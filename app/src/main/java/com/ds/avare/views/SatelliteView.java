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

import java.util.Iterator;

import com.ds.avare.utils.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.util.AttributeSet;
import android.view.View;

/**
 * 
 * @author zkhan
 *
 * Draws all infromation recieved from satellites and GPS 
 */
public class SatelliteView extends View {

    /*
     * Satellite view
     */
    private GpsStatus       mGpsStatus;
    private Paint           mPaint;
    private float          min;
    private Context         mContext;
    private float 	        mDipToPix;

    /**
     * 
     */
    private void setup(Context context) {
        mContext = context;        
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "LiberationMono-Bold.ttf"));

        mDipToPix = Helper.getDpiToPix(context);
        mPaint.setStrokeWidth(1 * mDipToPix);
        mPaint.setShadowLayer(0, 0, 0, Color.BLACK);
    }
    
    /**
     * 
     * @param context
     */
    public SatelliteView(Context context) {
        super(context);
        setup(context);
    }

    /**
     * 
     * @param context
     * @param aset
     */
    public SatelliteView(Context context, AttributeSet aset) {
        super(context, aset);
        setup(context);
    }

    /**
     * @param context
     * Default for tools, do not call
     */
    public SatelliteView(Context context, AttributeSet aset, int arg) {
        super(context, aset, arg);
        setup(context);
    }

    /**
     * 
     * @param status
     */
    public void updateGpsStatus(GpsStatus status) {
        mGpsStatus = status;
        postInvalidate();
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    public void onDraw(Canvas canvas) {

        /*
         * Move the GPS circle pic on corner so we have space for text
         */
        canvas.save();
        min = Math.min(getWidth(), getHeight()) - 8;
        if(min == (getHeight() - 8)) {
            canvas.translate(getWidth() / 2 - min / 2 - 8, 0);
        }
        else {
            canvas.translate(0, getHeight() / 2 - min / 2 - 8);            
        }
        
        /*
         * Now draw the target cross hair
         */
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        canvas.drawLine(getWidth() / 2 - min / 2, getHeight() / 2, getWidth() / 2 + min / 2, getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, getHeight() / 2 - min / 2, getWidth() / 2, getHeight() / 2 + min / 2, mPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, min / 2, mPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, min / 4, mPaint);

        if(mGpsStatus != null) {
	        Iterable<GpsSatellite>satellites = mGpsStatus.getSatellites();
	        Iterator<GpsSatellite>sat = satellites.iterator();
	        mPaint.setColor(Color.WHITE);
	        mPaint.setStyle(Paint.Style.STROKE);
	
	        /*
	         * Now draw a circle for each satellite, use simple projections of x = sin(theta), y = cos(theta)
	         * Arm for each projection is sin(elevation). Theta = azimuth.
	         */
	        while (sat.hasNext()) {
	            GpsSatellite satellite = sat.next();
	            if(satellite.usedInFix()) {
	                mPaint.setColor(Color.GREEN);
	            }
	            else {
	                mPaint.setColor(Color.RED);
	            }
	            
	            double angle = Math.toRadians(satellite.getAzimuth());
	            double e = Math.cos(Math.toRadians(satellite.getElevation())) * min / 2;
	            canvas.drawCircle(
	                    (float)(getWidth() / 2 + e * Math.sin(angle)), 
	                    (float)(getHeight() / 2 - e * Math.cos(angle)),
	                    (satellite.getSnr() / 100) * min / 16,
	                    mPaint);
	        }
	        canvas.restore();

        }
        
        else {
        	
            canvas.restore();
        }
    }
}
