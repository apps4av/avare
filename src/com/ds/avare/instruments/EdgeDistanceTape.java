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

import com.ds.avare.position.Scale;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;

/***
 * A class that handles the drawing of the distance tape 
 * markings along the top and left of the display screen
 */
public class EdgeDistanceTape {

	// Private stuff that we need to do our work
	private Paint mPaint;
	private int mBgndWidth;
	private int mLeftMargin;
	private int mBgndHeight;
	private int mBotmMargin;
	private int mTextWidth;
	private int mTextHeight;
	private Rect mTextBounds;
	
	/***
	 * The distance tape object. Allocate objects that we use during the draw cycle in here
	 * if possible
	 */
	public EdgeDistanceTape() {
	    mTextBounds = new Rect();
	}
	
	/***
	 * The main display is telling us what paint it is using. Make a copy of it and
	 * adjust it for what we need it to do. Perform some size calculations here instead
	 * of at draw time
	 * @param paint
	 */
	public void setPaint(Paint paint) {
		// Allocate our own paint based upon what is in use
		mPaint = new Paint(paint); 	

		// Set our desired attributes
		mPaint.setTextSize(mPaint.getTextSize() / (float) 2.5);
		mPaint.setTextAlign(Align.LEFT);

	    // Find out how big our shadow needs to be
	    mPaint.getTextBounds("000", 0, 3, mTextBounds);
	    
	    // Determine the margin pixel values
	    mTextWidth  = mTextBounds.width();
	    mTextHeight = mTextBounds.height();
		mBgndWidth  = (int) (mTextWidth  * 1.4);
		mLeftMargin = (int) (mTextWidth  * 0.2);
		mBgndHeight = (int) (mTextHeight * 1.4);
		mBotmMargin = (int) (mTextHeight * 1.2);
	}
	
	/***
	 * Is the specified X parameter going to show on the screen ? 
	 * @param x horizontal display location
	 * @param left side of the screen
	 * @param right side of the screen
	 * @return true if it is visible
	 */
	boolean inRangeX(float x, float left, float right) {
		if(x < left || x > right) {
			return false;
		}
		return true;
	}
	
	/***
	 * Is the specified Y parmeter going to show on the screen ?
	 * @param y vertical display location
	 * @param top of screen
	 * @param bottom of screen
	 * @return true if it is visible
	 */
	boolean inRangeY(float y, float top, float bottom) {
		if(y < top || y > bottom) {
			return false;
		}
		return true;
	}

	/***
	 * Draw the horizontal and vertical distance tape
	 * @param canvas what to draw upon
	 * @param scale object for zoom determinations
	 * @param pixPerUnit how many pixels per mile/km/nm
	 * @param homeX horizontal position of current location on display
	 * @param homeY vertical position of current location on display
	 * @param top the usable top of the display
	 * @param width of the display
	 * @param height of the display
	 */
	@SuppressLint("DefaultLocale")
	public void draw(Canvas canvas, Scale scale, float pixPerUnit, 
			int homeX, int homeY, int top, int width, int height) {
		
		// Set color and transparency for the shadow
	    mPaint.setColor(Color.BLACK);// shadow color is black
	    mPaint.setAlpha(0x7F);		// Make it see-thru

		// the vertical shadow down the left of the display
	    mPaint.setStrokeWidth(mBgndWidth);		// Line width
	    canvas.drawLine(mBgndWidth / 2,  top,  mBgndWidth / 2, height, mPaint);
	    
	    // the horizontal shadow along the top of the display under the display rows
	    mPaint.setStrokeWidth(mBgndHeight);
	    canvas.drawLine(mBgndWidth - 1,  top + mBgndHeight / 2,  width, top + mBgndHeight / 2, mPaint);

	    // The interval values for the scale indicator
	    double step = scale.getStep();

	    // text is white in color
	    mPaint.setColor(Color.WHITE);

	    // the horizontal values use posX and posY
	    int posX = homeX - (mTextWidth / 2);
	    int posY = top + mBotmMargin;

	    // Display the tape values for 20 distances
	    for(int idx = 0; idx < 21; idx++) {
	    	
	    	// The current range. Make its label. Calc its display offset
	    	double inc = idx * step;
	    	String strLabel = String.format(inc < 10 ? "%1.1f" : "%.0f", inc);
	    	float offset = (float)step * idx * pixPerUnit;

	    	// vertical tape on the left side. Show if on the screen
		    if(inRangeY(homeY - offset, posY, height))
		    	canvas.drawText(strLabel, mLeftMargin, homeY - offset,  mPaint);
		    if(inRangeY(homeY + offset, posY, height))
		    	canvas.drawText(strLabel, mLeftMargin, homeY + offset,  mPaint);

	    	// Horizontal tape on the top edge. Show if on the screen
		    if(inRangeX(posX - offset, mBgndWidth, width))
		    	canvas.drawText(strLabel, posX - offset, posY, mPaint);
		    if(inRangeX(posX + offset, mBgndWidth, width))
		    	canvas.drawText(strLabel, posX + offset, posY, mPaint);
	    }

	    return;
	}
}
