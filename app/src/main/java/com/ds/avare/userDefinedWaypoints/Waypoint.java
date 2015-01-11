/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.userDefinedWaypoints;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Origin;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class Waypoint {
    String 	mName;
    String 	mDescription;
    float  	mLat;
    float  	mLon;
    float  	mAlt;
    boolean mShowDist;
    int 	mMarkerType;
    boolean mVisible;

    public Waypoint(String name, String description, float lon, float lat, float alt, boolean showDist, int markerType) {
    	if(null != name) {
    		mName = name;
    	} else {
    		mName = "UNDEF";
    	}
    	
    	if(null != description) {
    		mDescription = description;
		} else {
			mDescription= "UNDEF";
		}

    	mLat = lat;
        mLon = lon;
        mAlt = alt;
        mShowDist = showDist;
        mMarkerType = markerType;
        mVisible = true;
    }
    
    // Get'ers
    public String getName() { return mName; }
    public String getDesc() { return mDescription; }
    public float getLat() { return mLat; }
    public float getLon() { return mLon; }

    // Set'ers
    public void setMarkerType(int markerType) { mMarkerType = markerType; }
    public void setVisible(boolean visible) { mVisible = visible; }

    // Constants
    public static final int MT_NONE = 0;
    public static final int MT_CYANDOT = 1;
    public static final int MT_CROSSHAIRS = 2;

    /***
     * Render the waypoint to the desired canvas at the location specified using the paint provided
     * @param canvas - canvas to paint upon
     * @param origin - x/y of the upper left 
     * @param trackUp - set up track is always up
     * @param gpsParams latest GPS tracking info
     * @param paint - paint to use
     * @param service - storage service
     * @param dstBrg - display distance and bearing info as well
     * @param size - text size to use
     */
    public void draw(Canvas canvas, Origin origin, boolean trackUp, GpsParams gpsParams, Paint paint, StorageService service, String dstBrg, float size ) {
    	if(false == mVisible) {
    		return;
    	}
    	
		// Map the lat/lon to the x/y of the current canvas
		float x = (float) origin.getOffsetX(mLon);
		float y = (float) origin.getOffsetY(mLat);
	
		switch(mMarkerType){
			case MT_NONE: {
				break;
			}
			
			case MT_CROSSHAIRS: {
		        paint.setStyle(Style.STROKE);
		        paint.setColor(Color.BLACK);
		        paint.setStrokeWidth(size);
		        canvas.drawLine(x - size * 6,  y,  x + size * 6,  y, paint);
		        canvas.drawLine(x,  y - size * 6,  x,  y + size * 6, paint);

		        // A black ring to highlight it a bit
		        canvas.drawCircle(x, y, (float) size * 3, paint);
		        
		        // Solid (almost) white chewy center
		        paint.setStyle(Style.FILL);
				paint.setColor(Color.WHITE);
				paint.setAlpha(0xF0);
		        canvas.drawCircle(x, y, (float) size * 2, paint);

		        break;
			}
			
			case MT_CYANDOT: {
				// Draw the filled circle, centered on the point
				paint.setStyle(Style.FILL);
				paint.setColor(Color.CYAN);
				paint.setAlpha(0x9F);
		        canvas.drawCircle(x, y, (float) size * 3, paint);
	
		        // A black ring around it to highlight it a bit
		        paint.setStyle(Style.STROKE);
		        paint.setColor(Color.BLACK);
		        paint.setStrokeWidth(size);
		        canvas.drawCircle(x, y, (float) size * 3, paint);
		        break;
			}
		}
		
	    // Set the display text properties
		paint.setStyle(Style.FILL);
	    paint.setColor(Color.WHITE);

	    // If we are in track up mode, then we need to rotate the text so it shows
	    // properly
	    boolean bRotated = false;
        if (trackUp && (gpsParams != null)) {
        	bRotated = true;
            canvas.save();
            canvas.rotate((int) gpsParams.getBearing(), x, y);
        }

	    // Draw the name above
	    service.getShadowedText().draw(canvas, paint, mName, Color.BLACK, x, y - size * 12);
	    
	    // and the distance/brg below IF that piece of metadata is true
	    if(true == mShowDist) {
	        service.getShadowedText().draw(canvas, paint, dstBrg, Color.BLACK, x, y + size * 12);
	    }
	    
	    // Restore canvas if we rotated it
        if (true == bRotated) {
            canvas.restore();
        }
    }
}
