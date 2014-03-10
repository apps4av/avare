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
 * A static class that handles the drawing of the distance tape 
 * markings along the top and left of the display screen
 */
public class EdgeDistanceTape {
	
	/***
	 * Is the specified X parameter going to show on the screen ? 
	 * @param x horizontal display location
	 * @param left side of the screen
	 * @param right side of the screen
	 * @return true if it is visible
	 */
	static boolean inRangeX(float x, float left, float right) {
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
	static boolean inRangeY(float y, float top, float bottom) {
		if(y < top || y > bottom) {
		return false;
		}
		return true;
	}

	/***
	 * Draw the horizontal and vertical distance tape
	 * @param canvas what to draw upon
	 * @param paint to use
	 * @param scale object for zoom determinations
	 * @param pixPerUnit how many pixels per mile/km/nm
	 * @param homeX horizontal position of current location on display
	 * @param homeY vertical position of current location on display
	 * @param top the usable top of the display
	 * @param width of the display
	 * @param height of the display
	 */
	@SuppressLint("DefaultLocale")
	static public void draw(Canvas canvas, Paint paint, Scale scale, float pixPerUnit, 
			int homeX, int homeY, int top, int width, int height) {
		
		// Save current text size and set new one
	    float oldTextSize = paint.getTextSize();
	    paint.setTextSize(oldTextSize / (float) 2.5);

	    // Figure out how large our display values can be
	    Rect textBounds = new Rect();
	    paint.setTextAlign(Align.LEFT);
	    paint.getTextBounds("000", 0, 3, textBounds);
	    
	    // Determine the margin pixel values
		int bgndWidth  = (int) (textBounds.width()  * 1.4);
		int leftMargin = (int) (textBounds.width()  * 0.2);
		int bgndHeight = (int) (textBounds.height() * 1.4);
		int botmMargin = (int) (textBounds.height() * 1.2);

	    paint.setColor(Color.BLACK);// shadow color is black
	    paint.setAlpha(0x7F);		// Make it see-thru

		// the vertical shadow down the left of the display
	    paint.setStrokeWidth(bgndWidth);		// Line width
	    canvas.drawLine(bgndWidth / 2,  top,  bgndWidth / 2, height, paint);
	    
	    // the horizontal shadow along the top of the display under the display rows
	    paint.setStrokeWidth(bgndHeight);
	    canvas.drawLine(bgndWidth - 1,  top + bgndHeight / 2,  width, top + bgndHeight / 2, paint);

	    // The interval values for the scale indicator
	    double step = scale.getStep();

	    // text is white in color
	    paint.setColor(Color.WHITE);

	    // the horizontal values use posX and posY
	    int posX = homeX - (textBounds.width() / 2);
	    int posY = top + botmMargin;

	    // Display the tape values for 20 distances
	    for(int idx = 0; idx < 21; idx++) {
	    	
	    	// The current range. Make its label. Calc its display offset
	    	double inc = idx * step;
	    	String strLabel = String.format(inc < 10 ? "%1.1f" : "%.0f", inc);
	    	float offset = (float)step * idx * pixPerUnit;

	    	// vertical tape on the left side. Show if on the screen
		    if(inRangeY(homeY - offset, posY, height))
		    	canvas.drawText(strLabel, leftMargin, homeY - offset,  paint);
		    if(inRangeY(homeY + offset, posY, height))
		    	canvas.drawText(strLabel, leftMargin, homeY + offset,  paint);

	    	// Horizontal tape on the top edge. Show if on the screen
		    if(inRangeX(posX - offset, bgndWidth, width))
		    	canvas.drawText(strLabel, posX - offset, posY, paint);
		    if(inRangeX(posX + offset, bgndWidth, width))
		    	canvas.drawText(strLabel, posX + offset, posY, paint);
	    }

	    // Restore previous text size
	    paint.setTextSize(oldTextSize);

	    return;
	}
}
