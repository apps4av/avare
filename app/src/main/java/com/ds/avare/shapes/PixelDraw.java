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

import android.graphics.Canvas;
import android.graphics.Paint;

import com.ds.avare.position.PixelCoordinate;

/**
 * A class that fakes lon
 * @author zkhan
 *
 */
public class PixelDraw {

    private float mLastXDraw;
    private float mLastYDraw;
    private static final int MAX_DRAW_POINTS = 2048;
    private static final int DRAW_POINT_THRESHOLD = 4;

    private float mMinX;
    private float mMaxX;
    private float mMinY;
    private float mMaxY;
    
    /*
     * A list of draw points
     */
    protected LinkedList<PixelCoordinate> mDrawPoints;

    /**
     * 
     */
    public PixelDraw() {
        mLastXDraw = 0;
        mLastYDraw = 0;
        mDrawPoints = new LinkedList<PixelCoordinate>();
    }
    
    /**
     * 
     * @param x
     * @param y
     */
    public void addPoint(float x, float y) {
        /*
         * Threshold the drawing so we do not generate too many points
         */
        if((Math.abs(mLastXDraw - x) < DRAW_POINT_THRESHOLD)
                && (Math.abs(mLastYDraw - y) < DRAW_POINT_THRESHOLD)) {
            return;
        }
        mLastXDraw = x;
        mLastYDraw = y;
        
        /*
         * Start deleting oldest points if too many points.
         */
        if(mDrawPoints.size() >= MAX_DRAW_POINTS) {
            mDrawPoints.remove(0);
        }
        mDrawPoints.add(new PixelCoordinate(x, y));
    }

    /**
     * 
     */
    public void addSeparation() {
       if(mDrawPoints.isEmpty()) {
           return;
       }
       PixelCoordinate c = mDrawPoints.getLast();
       /*
        * Add separation
        */
       if(null != c) {
           c.makeSeparate();
       }
    }
    
    /**
     * 
     */
    public void clear() {
       mDrawPoints.clear(); 
    }
    
    /**
     * 
     */
    public void drawShape(Canvas canvas, Paint paint) {
        PixelCoordinate c0 = null;
        PixelCoordinate c1 = null;
        for (PixelCoordinate c : mDrawPoints) {
            if(c0 == null) {
                c0 = c;
                continue;
            }
            c1 = c0;
            c0 = c;
            
            /*
             * This logic will draw a continuous line between points. However, a discontinuity is required.
             */
            float x0 = (float) c0.getX();
            float y0 = (float) c0.getY();
            float x1 = (float) c1.getX();
            float y1 = (float) c1.getY();
            if(!c1.isSeparate()) {
                canvas.drawLine(x0, y0, x1, y1, paint); 
            }
        }

    }

    public void setMapPoints(float minx, float miny, float maxx, float maxy){
        if(this.mMinX != minx ||
                this.mMaxX != maxx ||
                this.mMinY != miny ||
                this.mMaxY != maxy){
            for(int i =0; i < mDrawPoints.size(); i++){
                float existingX = (float)mDrawPoints.get(i).getX() - this.mMinX;
                float existingY = (float)mDrawPoints.get(i).getY() - this.mMinY;
                float newX = existingX/(this.mMaxX - this.mMinX) * (maxx-minx) + minx;
                float newY = existingY/(this.mMaxY - this.mMinY) * (maxy-miny) + miny;
                PixelCoordinate updated = new PixelCoordinate(newX,newY);
                if(mDrawPoints.get(i).isSeparate()){
                    updated.makeSeparate();
                }
                mDrawPoints.set(i,updated);
            }
            this.mMinX = minx;
            this.mMaxX = maxx;
            this.mMinY = miny;
            this.mMaxY = maxy;
        }
    }
    
}
