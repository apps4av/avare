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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;

// A class that handles the drawing of text to the display screen
// with a rectangular shadow curved at the corners
// All object allocation occurs during construction so that the paint 
// process is as quick as possible
public class ShadowedText {

    private Rect mTextSize;
    private RectF mShadowBox;
    private Paint mTextPaintShadow;
    private Paint mShadowPaint;
    private float mDipToPix;
    private int XMARGIN;
    private int YMARGIN;
    private int SHADOWRECTRADIUS;

    private static final int SHADOW = 4;
    
    // Build the shadowText object. This means allocating the paint
    // and required rectangle objects that we use during the draw
    // phase
	public ShadowedText(Context context) {
        mTextPaintShadow = new Paint();
        mTextPaintShadow.setTypeface(Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf"));
        mTextPaintShadow.setAntiAlias(true);
        mTextPaintShadow.setShadowLayer(SHADOW, SHADOW, SHADOW, Color.BLACK);
        mTextPaintShadow.setStyle(Paint.Style.FILL);
        
        mShadowPaint = new Paint(mTextPaintShadow);
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

    	// How big is the text we are about to draw
        paint.getTextBounds(text, 0, text.length(), mTextSize);
        
        // Calculate the size of the shadow
        mShadowBox.bottom = mTextSize.bottom + YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.top    = mTextSize.top - YMARGIN + y - (mTextSize.top / 2);
        mShadowBox.left   = mTextSize.left - XMARGIN + x  - (mTextSize.right / 2);
        mShadowBox.right  = mTextSize.right + XMARGIN + x  - (mTextSize.right / 2);

        // Set our shadow paint color and transparency 
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setAlpha(0x80);

        // Draw the background
        canvas.drawRoundRect(mShadowBox, SHADOWRECTRADIUS, SHADOWRECTRADIUS, mShadowPaint);
        
        // Draw the text over it
        canvas.drawText(text,  x - (mTextSize.right / 2), y - (mTextSize.top / 2), paint);
    }

    // Ordinals to lay out where we want the text to display relative to the provided point
    public static final int ABOVE = 0;
    public static final int ABOVERIGHT = 1;
    public static final int RIGHT = 2;
    public static final int BELOWRIGHT = 3;
    public static final int BELOW = 4;
    public static final int BELOWLEFT = 5;
    public static final int LEFT = 6;
    public static final int TOPLEFT = 7;

    public void draw(Canvas canvas, Paint paint, String text, int shadowColor, int sector, float x, float y) {
    	
    	// Find out how much room this text will take
        paint.getTextBounds(text, 0, text.length(), mTextSize);

        // Depending upon which sector we paint, we adjust position
        switch(sector) {

	        // No change for default location or above
	        default:
	    	case ABOVE:
	    		break;
	
	    	// Adjust Y down and X to the right
	    	case RIGHT:
	    		break;
	    	
	       	// Adjust Y down to underneath
	    	case BELOW:
	    		break;
	    	
	       	// Adjust Y down and X to the left
	    	case LEFT:
	    		break;
    	}
        
        // We have the "where", no display the text
        draw(canvas, paint, text, shadowColor, x, y);
    }
}
