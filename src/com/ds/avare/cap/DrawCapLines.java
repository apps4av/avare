/*
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.cap;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
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
	LinkedList<Chart> mCharts;


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
    	mPaint.setPathEffect(new DashPathEffect(new float[] {10, 20}, 0));
    	
    	// Get charts from static list
    	mCharts = CapChartFetcher.getInstance().getCharts();
	}

	/*
	 * Draw the cap lines in the view area that are seprated at 15 minute intervals / 0.25 on lat/lon
	 */
	public void draw(Canvas canvas, Origin origin, Scale scale) {
		
		/*
		 * 
		 * For speed, draw no more than 16 grid sections 4x4 (2x2 X 2x2)
		 */
		double latitudeCenter = origin.getLatitudeCenter();
		double longitudeCenter = origin.getLongitudeCenter();
				
		double latitudeUpper = snapToGrid(latitudeCenter + CapChartFetcher.QUARTER * 2);
		double latitudeLower = snapToGrid(latitudeCenter - CapChartFetcher.QUARTER * 2);
		double longitudeLeft = snapToGrid(longitudeCenter - CapChartFetcher.QUARTER * 2);
		double longitudeRight = snapToGrid(longitudeCenter + CapChartFetcher.QUARTER * 2);

		// dashed line
    	mPaint.setColor(Color.BLUE);

		// draw horizontal lines along latitude in increments of 0.25
		int x0 = (int)origin.getOffsetX(longitudeLeft);
		int x1 = (int)origin.getOffsetX(longitudeRight);
		for(double lat = latitudeUpper; lat >= latitudeLower; lat -= CapChartFetcher.QUARTER) {
			int y = (int)origin.getOffsetY(lat); 
			canvas.drawLine(x0, y , x1, y, mPaint);
		}

		// draw vertical lines along longitude in increments of 0.25
		int y0 = (int)origin.getOffsetY(latitudeUpper);
		int y1 = (int)origin.getOffsetY(latitudeLower);
		for(double lon = longitudeLeft; lon <= longitudeRight; lon += CapChartFetcher.QUARTER) {
			int x = (int)origin.getOffsetX(lon); 
			canvas.drawLine(x, y0 , x, y1, mPaint);
		}

        mPaint.setColor(Color.WHITE);

		// Now draw names of grids
		for(double lat = latitudeUpper; lat > latitudeLower; lat -= CapChartFetcher.QUARTER) {
			for(double lon = longitudeLeft; lon < longitudeRight; lon += CapChartFetcher.QUARTER) {
				/*
				 * Along with vertical lines, draw grid name
				 */
				String name = getGridName(lat, lon);
		        mService.getShadowedText().draw(canvas, mPaint,
		        		name, Color.BLACK, 
		        		(int)origin.getOffsetX(lon + CapChartFetcher.QUARTER / 2), 
		        		(int)origin.getOffsetY(lat - CapChartFetcher.QUARTER / 2));
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
		double out = (double)Math.round(in / CapChartFetcher.QUARTER) * CapChartFetcher.QUARTER;
		return (double)Math.round(out * 100) / 100;		
	}

	/**
	 * Get the name of CAP grid from the latitude and longitude of top left of the grid
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public String getGridName(double latitude, double longitude) {
		
		// Graphics rect increases values in x,y down and to right, but latitude decreases down hence negative sign
		Rect grid = new Rect(
				Chart.makeCapCoordinate(longitude),
				Chart.makeCapCoordinate(-latitude),
				Chart.makeCapCoordinate(longitude + CapChartFetcher.QUARTER),
				Chart.makeCapCoordinate(-(latitude - CapChartFetcher.QUARTER)));
		
		// Intersect with a chart to find this gird's place
		for (Chart chart : mCharts) {
			Rect ch = chart.getRect();
			if(Rect.intersects(grid, ch)) {
				
				/*
				 * Found chart it lies on. Now find the grid
				 */
				String name = chart.getIdentifier();
				
				/*
				 * Now find index
				 */
				int distx = Math.abs(chart.getRect().left - grid.left);
				int disty = Math.abs(chart.getRect().top - grid.top);

				// This is how many sections in one row
				int xdivs = Math.abs(chart.getRect().left - chart.getRect().right);
				
				// Join to form the name, index of 1
				return name + (disty * xdivs + distx + 1);
			}
		}
		return "";
	}

}
