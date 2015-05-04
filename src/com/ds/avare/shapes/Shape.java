/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.shapes;

import java.util.LinkedList;

import com.ds.avare.place.Plan;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.sromku.polygon.Point;
import com.sromku.polygon.Polygon;
import com.sromku.polygon.Polygon.Builder;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * @author zkhan
 *
 */
public abstract class Shape {

    protected LinkedList<Coordinate> mCoords;
    protected double mLonMin;
    protected double mLonMax;
    protected double mLatMin;
    protected double mLatMax;
    
    private String mText;
    
    private Builder mPolyBuilder;
    private Polygon mPoly;
    
    /**
     * 
     */
    public Shape(String label) {
        mCoords = new LinkedList<Coordinate>();
        mLonMin = 180;
        mLonMax = -180;
        mLatMin = 180;
        mLatMax = -180;
        mText = label;
        mPolyBuilder = Polygon.Builder(); 
    }

    /**
     * 
     * @param coords
     */
    public void add(double lon, double lat, boolean issep) {
    	add(lon,lat,issep, 0);
    }
    
    public void add(double lon, double lat, boolean issep, int segment) {
        Coordinate c = new Coordinate(lon, lat);
        if(issep) {
            c.makeSeparate();
        }
        c.setSegment(segment);
        
        mCoords.add(c);
        mPolyBuilder.addVertex(new Point((float)lon, (float)lat));
        
        /*
         * Calculate start points
         */
        if(lon < mLonMin) {
             mLonMin = lon;
        }
        if(lon >= mLonMax) {
             mLonMax = lon;
        }
        if(lat < mLatMin) {
             mLatMin = lat;
        }
        if(lat >= mLatMax) {
             mLatMax = lat;
        }
    }

    public void drawShape(Canvas c, Origin origin, Scale scale, Movement movement, Paint paint, boolean night, boolean drawTrack) {
    	drawShape(c, origin, scale,movement,paint,night, drawTrack, null);
    }
    
    /**
     * This will draw the closed shape in canvas with given screen params
     * @param c
     * @param origin
     * @param scale
     * @param movement
     * @param paint
     */
	public void drawShape(Canvas c, Origin origin, Scale scale, Movement movement, Paint paint, boolean night, boolean drawTrack, Plan plan) {
        float x = (float)origin.getOffsetX(mLonMin);
        float y = (float)origin.getOffsetY(mLatMax);
        float sx = scale.getScaleFactor();
        float sy = scale.getScaleCorrected();
        float facx = sx / (float)movement.getLongitudePerPixel();
        float facy = sy / (float)movement.getLatitudePerPixel();
        
        /*
         * Do a tab on top of shape
         */
        /*
         * Draw pivots at end of track
         */
        float width = paint.getStrokeWidth();
        int color = paint.getColor();
        
        // TrackShape type is used for a flight plan destination
        if (this instanceof TrackShape) {
            
            /*
             * Draw background on track shapes, so draw twice
             */
        	int cMax = getNumCoords();
            for(int coord = 0; coord < (cMax - 1); coord++) {
                float x1 = x + (float)(mCoords.get(coord).getLongitude() - mLonMin) * facx;
                float x2 = x + (float)(mCoords.get(coord + 1).getLongitude() - mLonMin) * facx;
                float y1 = y + (float)(mCoords.get(coord).getLatitude() - mLatMax) * facy;
                float y2 = y + (float)(mCoords.get(coord + 1).getLatitude() - mLatMax) * facy;

                if(drawTrack) {
	                paint.setStrokeWidth(width + 4);
	                paint.setColor(night? Color.WHITE : Color.BLACK);
	                c.drawLine(x1, y1, x2, y2, paint);
	                paint.setStrokeWidth(width);

	                if(null == plan) {
	                	paint.setColor(color);
	                } else {
	                	paint.setColor(TrackShape.getLegColor(plan.findNextNotPassed(), mCoords.get(coord).getLeg()));
	                }

	                c.drawLine(x1, y1, x2, y2, paint);
                }

				if(mCoords.get(coord + 1).isSeparate()) {
                    paint.setColor(night? Color.WHITE : Color.BLACK);
                    c.drawCircle(x2, y2, 10, paint);
                    paint.setColor(Color.GREEN);
                    c.drawCircle(x2, y2, 8, paint);
                    paint.setColor(color);
                }
                if(mCoords.get(coord).isSeparate()) {
                    paint.setColor(night? Color.WHITE : Color.BLACK);
                    c.drawCircle(x1, y1, 10, paint);
                    paint.setColor(Color.GREEN);
                    c.drawCircle(x1, y1, 8, paint);
                    paint.setColor(color);
                }
            }
        } else {
            /*
             * Draw the shape segment by segment
             */
            for(int coord = 0; coord < (getNumCoords() - 1); coord++) {
                float x1 = x + (float)(mCoords.get(coord).getLongitude() - mLonMin) * facx;
                float x2 = x + (float)(mCoords.get(coord + 1).getLongitude() - mLonMin) * facx;
                float y1 = y + (float)(mCoords.get(coord).getLatitude() - mLatMax) * facy;
                float y2 = y + (float)(mCoords.get(coord + 1).getLatitude() - mLatMax) * facy;
                c.drawLine(x1, y1, x2, y2, paint);
            }
        }
    }
    
    /**
     * 
     * @return
     */
    public int getNumCoords() {
        return mCoords.size();
    }

    /**
     * 
     * @return
     */
    public double getLatitudeMinimum() {
        return mLatMin;
    }
    
    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public String getTextIfTouched(double lon, double lat) {
        if(null == mPoly) {
            return null;
        }
        if(mPoly.contains(new Point((float)lon, (float)lat))) {
            return mText;
        }
        return null;
    }
    
    /**
     * 
     */
    public void makePolygon() {
        if(getNumCoords() > 2) {
            mPoly = mPolyBuilder.build();
        }
    } 
}
