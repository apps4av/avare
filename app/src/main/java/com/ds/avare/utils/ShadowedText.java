/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;

// A class that handles the drawing of text to the display screen
// with a rectangular shadow curved at the corners
// All object allocation occurs during construction so that the paint 
// process is as quick as possible
public class ShadowedText {

    private Rect mTextSize;
    private RectF mShadowBox;
    private Paint mShadowPaint;
    private float mDipToPix;
    private int XMARGIN;
    private int YMARGIN;
    private int SHADOWRECTRADIUS;

    // Build the shadowText object. This means allocating the paint
    // and required rectangle objects that we use during the draw
    // phase
	public ShadowedText(Context context) {
        mShadowPaint = new Paint();
        mShadowPaint.setTypeface(Helper.getTypeFace(context));
        mShadowPaint.setShadowLayer(0, 0, 0, 0);
        mShadowPaint.setAlpha(0x7f);
        mShadowPaint.setStyle(Style.FILL);

        mTextSize = new Rect();
        mShadowBox = new RectF(mTextSize);
        
        mDipToPix = Helper.getDpiToPix(context);

        XMARGIN = (int) (5 * mDipToPix);
        YMARGIN = (int) (5 * mDipToPix);
        SHADOWRECTRADIUS = (int) (5 * mDipToPix);
	}
	
    /**
     * Display the text in the indicated paint with a shadow'd background. This aids in readability.
     * 
     * @param canvas where to draw
     * @param text what to display
     * @param paint what to paint the text with
     * @param shadowColor is the color of the shadow of course
     * @param x center position of the text on the canvas
     * @param y top edge of text on the canvas
     */
    public void draw(Canvas canvas, Paint paint, String text, int shadowColor, float x, float y) {

    	// If nothing to draw, then get out of here now
    	if((null == text) || 0 == text.length()) {
    		return;
    	}
    	
    	// How big is the text we are about to draw
        paint.getTextBounds(text, 0, text.length(), mTextSize);
        
        // Calculate the size of the shadow
        mShadowBox.bottom = mTextSize.bottom + YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.top    = mTextSize.top    - YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.left   = mTextSize.left   - XMARGIN + x - (mTextSize.right / 2);
        mShadowBox.right  = mTextSize.right  + XMARGIN + x - (mTextSize.right / 2);

        // Set our shadow paint color and transparency 
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setAlpha(0x80);

        // Draw the background
        canvas.drawRoundRect(mShadowBox, SHADOWRECTRADIUS, SHADOWRECTRADIUS, mShadowPaint);
        
        // Draw the text over it
        canvas.drawText(text,  x - (mTextSize.right / 2), y - (mTextSize.top / 2), paint);
    }

    // Ordinals to lay out where we want the text to display relative to the provided point
    public static final int ABOVE = 0x01;
    public static final int RIGHT = 0x02;
    public static final int BELOW = 0x04;
    public static final int LEFT  = 0x08;

    public static final int ABOVE_RIGHT = ABOVE | RIGHT;
    public static final int BELOW_RIGHT = BELOW | RIGHT;
    public static final int ABOVE_LEFT  = ABOVE | LEFT;
    public static final int BELOW_LEFT  = BELOW | LEFT;
    
    /***
     * Draw the shadowed text using the "sector" as a reference around the center position specified
     * by x and y
     * @param canvas
     * @param paint
     * @param text
     * @param shadowColor
     * @param sector
     * @param x
     * @param y
     */
    public void draw(Canvas canvas, Paint paint, String text, int shadowColor, int sector, float x, float y) {
    	
    	// If nothing to draw, then get out of here now
    	if((null == text) || 0 == text.length()) {
    		return;
    	}

    	// Only do this work if we need to re-calc based on a new sector
    	if(0 != sector) {
	    	// Find out how much room this text will take
	        paint.getTextBounds(text, 0, text.length(), mTextSize);
	        
	        // Now calculate the offsets to handle the relative position
	        int xText = mTextSize.right - mTextSize.left;
	        int yText = mTextSize.bottom - mTextSize.top;
	        int xAdjust = (yText * 2 + xText / 2);
	        int yAdjust = (yText + (yText / 2));
	
	        // sector is a bitmapped field that defines what adjustments we need
	        // to make to the position of the text
	
	        // Do we need to move the text to the right ? 
	        if(0 != (sector & RIGHT)) {
	        	x += xAdjust;
	        }
	
	        // How about moving it left ?
	        if(0 != (sector & LEFT)) {
	        	x -= xAdjust;
	        }
	
	        // Above ?
	        if(0 != (sector & ABOVE)) {
	        	y -= yAdjust;
	        }
	        
	        // Now check for below
	        if(0 != (sector & BELOW)) {
	        	y += yAdjust;
	        }
    	}
    	
        // We have the "where", now display the text
        draw(canvas, paint, text, shadowColor, x, y);
    }
    /***
     * Draw the shadowed text using the "sector" as a reference around the center position specified
     * by x and y
     * @param canvas
     * @param paint
     * @param text
     * @param shadowColor
     * @param x
     * @param y
     * @param alpha
     */
    public void drawAlpha(Canvas canvas, Paint paint, String text, int shadowColor, float x, float y, int alpha) {

        // If nothing to draw, then get out of here now
        if((null == text) || 0 == text.length()) {
            return;
        }

        // How big is the text we are about to draw
        paint.getTextBounds(text, 0, text.length(), mTextSize);

        // Calculate the size of the shadow
        mShadowBox.bottom = mTextSize.bottom + YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.top    = mTextSize.top    - YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.left   = mTextSize.left   - XMARGIN + x - (mTextSize.right / 2);
        mShadowBox.right  = mTextSize.right  + XMARGIN + x - (mTextSize.right / 2);

        // Set our shadow paint color and transparency
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setAlpha(alpha);

        // Draw the background
        canvas.drawRoundRect(mShadowBox, SHADOWRECTRADIUS, SHADOWRECTRADIUS, mShadowPaint);

        // Draw the text over it
        canvas.drawText(text,  x - (mTextSize.right / 2), y - (mTextSize.top / 2), paint);
    }

}
