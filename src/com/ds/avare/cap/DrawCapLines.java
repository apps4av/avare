/*
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.cap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import com.ds.avare.StorageService;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.ds.avare.utils.Helper;

/***
 * The class that draws cap grid lines 
 */

/**
 * 
 * @author zkhan
 *
 */
public class DrawCapLines {

    private Paint mPaint;
    private StorageService mService;

    /*
     * Setup paint and styles to draw
     */
	public DrawCapLines(StorageService service, Context context, float textSize) {
		mService = service;
    	mPaint = new Paint();
    	mPaint.setTextSize(textSize);
        mPaint.setStyle(Style.FILL);
        mPaint.setStrokeWidth(Helper.getDpiToPix(context) * 2);
    	mPaint.setAntiAlias(true);
    	mPaint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
	}

	/*
	 * Draw the cap lines in the view area that are seprated at 15 minute intervals / 0.25 on lat/lon
	 */
	public void draw(Canvas canvas, Origin origin, Scale scale) {
		
		/*
		 * 
		 * For speed, draw no more than 16 grid sections
		 */
		double latitudeCenter = origin.getLatitudeCenter();
		double longitudeCenter = origin.getLongitudeCenter();
				
		double latitudeUpper = snapToGrid(latitudeCenter + 0.5);
		double latitudeLower = snapToGrid(latitudeCenter - 0.5);
		double longitudeLeft = snapToGrid(longitudeCenter - 0.5);
		double longitudeRight = snapToGrid(longitudeCenter + 0.5);

		// dashed line
    	mPaint.setColor(Color.BLUE);

		// draw horizontal lines along latitude in increments of 0.25
		int x0 = (int)origin.getOffsetX(longitudeLeft);
		int x1 = (int)origin.getOffsetX(longitudeRight);
		for(double lat = latitudeUpper; lat >= latitudeLower; lat -= 0.25) {
			int y = (int)origin.getOffsetY(lat); 
			canvas.drawLine(x0, y , x1, y, mPaint);
		}

		// draw vertical lines along longitude in increments of 0.25
		int y0 = (int)origin.getOffsetY(latitudeUpper);
		int y1 = (int)origin.getOffsetY(latitudeLower);
		for(double lon = longitudeLeft; lon <= longitudeRight; lon += 0.25) {
			int x = (int)origin.getOffsetX(lon); 
			canvas.drawLine(x, y0 , x, y1, mPaint);
		}

        mPaint.setColor(Color.WHITE);

		// Now draw names of grids
		for(double lat = latitudeUpper; lat > latitudeLower; lat -= 0.25) {
			for(double lon = longitudeLeft; lon < longitudeRight; lon += 0.25) {
				/*
				 * Along with vertical lines, draw grid name
				 */
				String name = CapChartFetcher.getInstance().getMyName(lat, lon);
		        mService.getShadowedText().draw(canvas, mPaint,
		        		name, Color.BLACK, (int)origin.getOffsetX(lon + 0.125), (int)origin.getOffsetY(lat - 0.125));
			}
		}
	}
	
	/**
	 * Rounding of input to 2 digits decimal of 0.25
	 * @param in
	 * @return
	 */
	private double snapToGrid(double in) {
		// round to closest 0.25
		double out = (double)Math.round(in * 4) / 4.0;
		return (double)Math.round(out * 100) / 100;		
	}

}
