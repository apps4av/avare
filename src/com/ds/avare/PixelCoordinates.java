/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


/**
 * @author zkhan
 *
 */
public class PixelCoordinates {
	
	private double mX0;
	private double mY0;
	private double mX1;
	private double mY1;
	private double mLon0;
	private double mLat0;
	private double mLon1;
	private double mLat1;
	private double mWidth;
	private double mHeight;
    private int mPointsAcquired;


    private static final int POINTS_MIN_PIXELS = 300;
    

	/**
	 * 
	 */
	public PixelCoordinates(double width, double height) {
		mX0 = mY0 = mX1 = mY1 = mLon0 = mLat0 = mLon1 = mLat1 = 0;
		mPointsAcquired = 0;
		mWidth = width;
		mHeight = height;
	}

	/**
	 * 
	 * @return
	 */
	public double getX0() {
		return mX0;
	}

	/**
	 * 
	 * @return
	 */
	public double getY0() {
		return mY0;
	}

	/**
	 * 
	 * @param x
	 */
	public void setX0(double x) {
		mX0 = x;
	}

	/**
	 * 
	 * @param y
	 */
	public void setY0(double y) {
		mY0 = y;
	}

	/**
	 * 
	 * @param x
	 */
	public void setX1(double x) {
		mX1 = x;
	}

	/**
	 * 
	 * @param y
	 */
	public void setY1(double y) {
		mY1 = y;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isPixelDimensionAcceptable() {
    	if((Math.abs(mX0 - mX1) < POINTS_MIN_PIXELS) || 
    			(Math.abs(mY0 - mY1) < POINTS_MIN_PIXELS) || mWidth == 0 || mHeight == 0) {
    		return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param lon
	 * @param minsec
	 */
	public void setLongitude0(String lon, String minsec) {
		try {
			mLon0 = Double.parseDouble(lon) + Double.parseDouble(minsec) / 60;
		}
		catch (Exception e) {
			mLon0 = 0;
		}
	}
	
	/**
	 * 
	 * @param lat
	 * @param minsec
	 */
	public void setLatitude0(String lat, String minsec) {
		try {
			mLat0 = Double.parseDouble(lat) + Double.parseDouble(minsec) / 60;
		}
		catch (Exception e) {
			mLat0 = 0;
		}
	}

	/**
	 * 
	 * @param lon
	 * @param minsec
	 */
	public void setLongitude1(String lon, String minsec) {
		try {
			mLon1 = Double.parseDouble(lon) + Double.parseDouble(minsec) / 60;
		}
		catch (Exception e) {
			mLon1 = 0;
		}
	}
	
	/**
	 * 
	 * @param lat
	 * @param minsec
	 */
	public void setLatitude1(String lat, String minsec) {
		try {
			mLat1 = Double.parseDouble(lat) + Double.parseDouble(minsec) / 60;
		}
		catch (Exception e) {
			mLat1 = 0;
		}
	}
	
	/**
	 * 
	 */
	public void addPoint() {
		if(mPointsAcquired == 2) {
			mPointsAcquired = 0;
		}
		mPointsAcquired++;
	}
	
	/**
	 * @return
	 */
	public boolean secondPointAcquired() {
		if(mPointsAcquired == 2) {
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean firstPointAcquired() {
		if(mPointsAcquired == 1) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public boolean gpsCoordsCorrect() {
		
		/*
		 * For US only. Does not accept E lon, and S lat.
		 */
		if(
				mLon0 >= 0 || mLon0 < -180 || mLat0 <= 0 || mLat0 > 90 ||
				mLon1 >= 0 || mLon1 < -180 || mLat1 <= 0 || mLat1 > 90) {
			return false;		
		}
		
		/*
		 * Save divide by zero
		 */
		if((mLon0 - mLon1) == 0 || (mLat0 - mLat1) == 0) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return return origin, lat/lon of origin and px, py
	 */
	public double[] get() {
		if(secondPointAcquired() && gpsCoordsCorrect() && isPixelDimensionAcceptable()) {
			double ret[] = new double[4];
			ret[0] = mX0;
			ret[1] = mY0;
			ret[2] = (mX0 - mX1) / (mLon0 - mLon1);
			ret[3] = (mY0 - mY1) / (mLat0 - mLat1);
			return ret;
		}
		return null;
	}
}
