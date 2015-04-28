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

import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Projection;
import com.ds.avare.utils.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/***
 * Implementation of a Course Deviation Indicator
 */
public class CDI {
	Paint 	mCDIPaint;	// Our very own paint object
	Rect  	mTextSize;	// Used to determine text bounds
	int   	mBarCount;	// How many bars to display
	int   	mBarWidth;	// Display width of 1 bar
	int		mBarHeight;	// How tall is each bar
	int		mBarSpace;	// Width of the space between bars
	int 	mInstWidth;	// Total width of the instrument
	int 	mInstHeight;	// Total height of the instrument
	int 	mInstTop;		// The top line of the CDI
	int 	mInstLeft;	// Left position of the CDI
	final int mColorLeft   = Color.RED;
	final int mColorRight  = Color.rgb(0x00,0xa0,0x00);
	private double mBarDegrees = BAR_DEGREES_VOR;

	// Calc'd instrument values
	int		mDspOffset;	// Display offset value for deviation
	double 	mDeviation;	// How far off course are we
	int		mBackColor;	// Background color of inst
	
    private static final double BAR_DEGREES_VOR = 2f;
    private static final double BAR_DEGREES_LOC = 0.5f;

	/***
	 * Return the actual course deviation value. It's an absolute
	 * number without any direction information
	 * @return How many miles/km/hm off the course line
	 */
	public double getDeviation() {
		return mDeviation;
	}
	
	/**
	 * If we are left of course
	 * @return
	 */
	public boolean isLeft() {
	    return mBackColor == mColorLeft;
	}
	
	/***
	 * Course Deviation Indicator
	 */
	public CDI() {	}
	
	/***
	 * Allocate the paint object and figuring out how
	 * large the instrument is on the display based upon the text size in the 
	 * source paint object
	 * @param textPaint Used to figure out our overall size
	 */
	public void setSize(Paint textPaint, int minSize)
	{
		// Ignore an invalid size request
		if (0 == minSize) {
			return;
		}
		
		// A total of 11 bars
		mBarCount = 11;
		
		// The height of each bar is the basis for the entire instrument size
		mBarHeight = (int) (minSize / 16);
		
		// Width is 1/4 of the height
		mBarWidth = mBarHeight / 4;
		
		// Space between bars is 3x the width of the bar
		mBarSpace = 3 * mBarWidth;
		
		// Width of the entire instrument
		mInstWidth = mBarCount * mBarWidth + 	// The bars themselves
				(mBarCount - 1) * mBarSpace + 	// Space between the bars
				2 * mBarWidth;					// Space to the left and right of the end bars

		// Height of the entire instrument
		mInstHeight = mBarHeight + 				// The main bar itself
				mBarHeight / 2 + 				// The triangle indicator height
				mBarWidth * 3;					// Border padding at top/bottom/middle
		
		// Allocate some objects to save time during draw cycle
		mCDIPaint = new Paint(textPaint);
		mTextSize = new Rect();
		
	}

	/***
	 * Draw the instrument on the canvas given the current screen size
	 * This is called during the screen refresh/repaint - be quick about it; no "new" calls.
	 * @param canvas What to draw upon
	 * @param screenX Total width of the display canvas
	 * @param screenY Total height of the display canvas
	 */
	public void drawCDI(Canvas canvas, int screenX, int screenY)
	{
		if(mCDIPaint == null) {
			// In case paint is not inited yet
			return;
		}
		
		// Calculate the left position of the instrument
        mInstLeft = (screenX - mInstWidth) / 2;

        // Now where is the top of it
        mInstTop  = screenY * 3 / 4;
	    
        // Draw the background
	    mCDIPaint.setColor(mBackColor);	// Color
	    mCDIPaint.setAlpha(0x7F);		// Make it see-thru
	    mCDIPaint.setStrokeWidth(mInstHeight);	// How tall the inst is
	    mCDIPaint.setStyle(Paint.Style.STROKE);	// Type of brush
	    
	    // Draw the background of the instrument. This is a horo swipe left to right,
	    // so we need to specify the vertical middle as the source Y, not the top
	    int instCenterY = mInstTop + mInstHeight / 2;
        canvas.drawLine(mInstLeft, instCenterY, mInstLeft + mInstWidth, instCenterY, mCDIPaint);

        // Draw all of the vertical bars
        if(mBarDegrees == BAR_DEGREES_LOC) {
            mCDIPaint.setColor(Color.CYAN);        // cyan for localizer            
        }
        else {
            mCDIPaint.setColor(Color.WHITE);		// white for VOR
        }
	    mCDIPaint.setStrokeWidth(mBarWidth);	// Width of each bar
	    for(int idx = 0; idx < mBarCount; idx++) {
	        int extend = (idx == (int)(mBarCount / 2)) ? mInstHeight / 3 : 0;
	    	int barLeft = mInstLeft + (int)((float)mBarWidth * 1.5) + 
	    			idx * (mBarWidth + mBarSpace);
	        canvas.drawLine(barLeft, mInstTop + mBarWidth, barLeft, 
	        		mInstTop + mBarHeight + mBarWidth + extend, mCDIPaint);
	    }
	    
	    // Now draw the needle indicator at the horizontal center
	    drawIndicator(canvas, screenX / 2 - mDspOffset);
	    
	}
	
	/***
	 * Draw the triangle position indicator under the bar
	 * @param canvas what to draw upon
	 * @param posX left/right center position of the pointer
	 */
	private void drawIndicator(Canvas canvas, int posX)
	{
		// Top point
	    int X1 = posX;
	    int Y1 = mInstTop + mBarHeight + 2 * mBarWidth;
	    
	    // Bottom right point
	    int X2 = X1 + mBarHeight / 4;
	    int Y2 = Y1 + mBarHeight / 2;
	    
	    // Bottom left point
	    int X3 = X1 - mBarHeight / 4;
	    int Y3 = Y2;

	    mCDIPaint.setColor(Color.WHITE);	// white
	    mCDIPaint.setStrokeWidth(5);		// Line width
	    
	    // Draw
	    canvas.drawLine(X1, Y1, X2, Y2, mCDIPaint);	// Right leg
	    canvas.drawLine(X2, Y2, X3, Y3, mCDIPaint); // base
	    canvas.drawLine(X3, Y3, X1, Y1, mCDIPaint); // Left leg
		
	}

	/***
	 * Calculate the deviation of our current position from
	 * the plotted course
	 * @param dest what our destination is
	 * @param plan the current flight plan (if any)
	 */
	public void calcDeviation(Destination dest, Plan plan)
	{
		// If we have no destination there there is nothing to do
		if(dest == null) {
			return;
		}

		// Assume an on-course display
		mDspOffset = 0;
		mBackColor = mColorLeft;
		
		// If there is no plan, then use a bearing based upon the
		// destination object only. The "init location" of the destination will
		// be set with the lat/lon of where we were when this was set as our 
		// destination.
		//
		// If there is an active plan, then the bearing used is from the PREVIOUS
		// waypoint in the plan to the current destination as long as we are passed
		// the first waypoint.
		double brgOrg;
		int nnp;

		// Are we passed the first waypoint on an active plan ?  
		if ((null != plan) && (true == plan.isActive()) && ((nnp = plan.findNextNotPassed()) > 0)) {
			// We have an active plan and beyond the first waypoint. 
			// Calc our deviation using the
			// previous waypoint and our current destination
			Destination prevDest = plan.getDestination(nnp - 1);
			brgOrg = Projection.getStaticBearing(
					prevDest.getLocation().getLongitude(),
					prevDest.getLocation().getLatitude(),
					dest.getLocation().getLongitude(),
					dest.getLocation().getLatitude());
		} else {
			// There is no active plan after the first waypoint, 
			// so figure out our deviation using
			// solely the information in the destination
			brgOrg = Projection.getStaticBearing(
				dest.getLocationInit().getLongitude(),
				dest.getLocationInit().getLatitude(),
				dest.getLocation().getLongitude(),
				dest.getLocation().getLatitude());
		}
		
		// The bearing from our CURRENT location to the target
		double brgCur = dest.getBearing();

		// Relative bearing we are FROM destination is the difference
		// of these two
		double brgDif = Helper.angularDifference(brgOrg, brgCur);

		// Distance from our CURRENT position to the destination
		double dstCur = dest.getDistance();
		
		/*
		 * Within given miles convert to Localizer
		 * This must match the distance for Glide slope.
		 */
		if(dstCur > VNAV.APPROACH_DISTANCE) {
		    mBarDegrees = BAR_DEGREES_VOR;
		}
		else {
            mBarDegrees = BAR_DEGREES_LOC;		    
		}

		// Distance we are from the direct course line
		mDeviation = dstCur * Math.sin(Math.toRadians(brgDif));

		// The amount of display offset varies depending upon how large the deviation is
		double maxDeflection = mBarDegrees * (double)(mBarCount - 1) / 2f; 
		if(brgDif > maxDeflection) {
		    brgDif = maxDeflection;
		}
        mDspOffset = (int) ((mBarWidth + mBarSpace) * (brgDif / mBarDegrees));

		// Assume we are to the RIGHT of the courseline
		mBackColor = mColorRight;
		
		// Now determine whether we are LEFT. That will
		// dictate the color of the shadow and the sign of the mDeviation.
		// Account for REVERSE SENSING if we are already BEYOND the target (>90deg)
		boolean bLeftOfCourseLine = Helper.leftOfCourseLine(brgCur,  brgOrg); 
		if ((bLeftOfCourseLine && brgDif <= 90) || (!bLeftOfCourseLine && brgDif >= 90)) {
			mBackColor = mColorLeft;
			mDspOffset = -mDspOffset;
		}
	}
}
