/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;

import com.ds.avare.R;

/***
 * This class is about the bottom are of the display panel that exists between the left,
 * center and right buttons. This is an area of display text small in size that may contain 
 * various pieces of nav information.
 * 
 * Known uses:
 * 1) For external GPX plans, these areas are fed with information about the plan and the
 * current active flight leg when one is active
 * 2) Configurable to display current GPS lat/lon and display sizes
 * @author Ron
 *
 */
public class NavComments {

	private String	mLeftComment;
	private String	mRightComment;
	
	/***
	 * Ensure the object is set up to use
	 */
	public NavComments() {
		clear();
	}
	
	/***
	 * Clean this object out
	 */
	public void clear() {
		mLeftComment = null;
		mRightComment = null;
	}
	
	/***
	 * Set the text of what is displayed on the left side
	 * @param leftComment string to display at left bottom
	 */
	public void setLeft(String leftComment) {
		mLeftComment = leftComment;
	}
	
	/***
	 * Set the text of what is displayed on the right side
	 * @param rightComment string to display at right bottom
	 */
	public void setRight(String rightComment) {
		mRightComment = rightComment;
	}

	/***
	 * Time to draw this information to the display panel
	 * @param view Used to find the vertical position of where to draw
	 * @param canvas What to draw opon
	 * @param paint Paint to use for the text
	 * @param shadowedText Object to draw the text and shadow
	 */
	public void draw(View view, Canvas canvas, Paint paint, ShadowedText shadowedText) {
		
		// Search for the spot on the screen to place the text
	 	View parent = (View) view.getParent();
        Button mMenuButton = (Button)parent.findViewById(R.id.location_button_menu);
        
        // If we found the position then get the rest of the measurements
        if(null != mMenuButton) {
        	
        	// the left text is centered on the left side 
            int leftX = view.getWidth() / 4; 
	        
        	// the right text is centered on the rightside 
            int rightX = view.getWidth() / 4 * 3; 

            // Centered vertically within the size of the bottom pushbuttons
            int topY = (mMenuButton.getHeight() / 2) + mMenuButton.getTop();

            // Draw them with background shadow
	        paint.setColor(Color.WHITE);
        	shadowedText.draw(canvas, paint, mLeftComment, Color.BLACK, leftX, topY);
        	shadowedText.draw(canvas, paint, mRightComment, Color.BLACK, rightX, topY);
        }
	}
}
