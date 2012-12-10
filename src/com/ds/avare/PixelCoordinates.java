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
	private double mPx;
	private double mPy;
	private double mLon1;
	private double mLat1;
	private double mWidth;
	private double mHeight;
	private double mOLon;
	private double mOLat;	
    private double mRotated;
    private int mPointsAcquired;


    private static final int POINTS_MIN_PIXELS = 300;
    

	/**
	 * 
	 */
	public PixelCoordinates(double width, double height) {
		mX0 = mY0 = mX1 = mY1 = mLon0 = mLat0 = mLon1 = mLat1 = 0;
		mOLon = mOLat = mPx = mPy = 0;
		mPointsAcquired = 0;
		mWidth = width;
		mHeight = height;
		mRotated = 0;
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
			mLon0 = Double.parseDouble(lon);
			if(mLon0 < 0) {
				 mLon0 = mLon0 - Double.parseDouble(minsec) / 60;
			}
			else {
				 mLon0 = mLon0 + Double.parseDouble(minsec) / 60;				
			}
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
			mLat0 = Double.parseDouble(lat);
			if(mLat0 < 0) {
				mLat0 = mLat0 - Double.parseDouble(minsec) / 60;
			}
			else {
				mLat0 = mLat0 + Double.parseDouble(minsec) / 60;				
			}
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
			mLon1 = Double.parseDouble(lon);
			if(mLon1 < 0) {
				 mLon1 = mLon1 - Double.parseDouble(minsec) / 60;
			}
			else {
				 mLon1 = mLon1 + Double.parseDouble(minsec) / 60;				
			}
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
			mLat1 = Double.parseDouble(lat);
			if(mLat1 < 0) {
				mLat1 = mLat1 - Double.parseDouble(minsec) / 60;
			}
			else {
				mLat1 = mLat1 + Double.parseDouble(minsec) / 60;				
			}
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
			
		    
		    /*
		     * Do the math to find origin and px, py
		     */
		    String moved = "";
		    
		    /*
		     * Which direction did the mark move from point 1 to 2
		     */
		    if((mX0 > mX1) && (mY0 < mY1)) {
		        moved = "ne";
		    }
		    else if((mX0 > mX1) && (mY0 > mY1)) {
                moved = "se";
            }
		    else if((mX0 < mX1) && (mY0 < mY1)) {
                moved = "nw";
            }
		    else if((mX0 < mX1) && (mY0 > mY1)) {
                moved = "sw";
            }
            
		    /*
		     * Now if lon increases from left to right and lat increases from bottom to up then not rotated
		     */
            if(
                    (moved.equals("sw") && (mLon0 > mLon1) && (mLat0 > mLat1)) ||
                    (moved.equals("nw") && (mLon0 > mLon1) && (mLat0 < mLat1)) ||
                    (moved.equals("se") && (mLon0 < mLon1) && (mLat0 > mLat1)) ||
                    (moved.equals("ne") && (mLon0 < mLon1) && (mLat0 < mLat1))                                       
                    ) {
                mRotated = 0;
            }
            else {
                /*
                 * Rotated plate
                 */
                mRotated = 90;
            }
		    
            if(0 == mRotated) {
    			double difflon = mLon0 - mLon1;
    			double difflat = mLat0 - mLat1;			
    			double diffx = mX0 - mX1;
    			double diffy = mY0 - mY1;
    			mPx = Math.abs(difflon / diffx); /* lon / pixel */
    			mPy = -Math.abs(difflat / diffy); /* lat / pixel */
    			
    			/*
    			 * Find lon/lat of origin now
    			 */
    			mOLon = mLon0 + mX0 * mPx;
    			mOLat = mLat0 + mY0 * mPy;
            }
            else {
                /*
                 * x is y, y is x
                 */
                double difflon = mLon0 - mLon1;
                double difflat = mLat0 - mLat1;         
                double diffx = mX0 - mX1;
                double diffy = mY0 - mY1;
                mPx = -Math.abs(difflat / diffx); /* lat / pixel */
                mPy = -Math.abs(difflon / diffy); /* lon / pixel */
                
                /*
                 * Find lon/lat of origin now
                 */
                mOLon = mLon0 + mY0 * mPy;
                mOLat = mLat0 + mX0 * mPx;
            }
						
			/*
			 * Return for storage
			 */
			double ret[] = new double[5];
			ret[0] = mOLon;
			ret[1] = mOLat;
			ret[2] = mPx;
			ret[3] = mPy;
			ret[4] = mRotated;
			return ret;
		}
		return null;
	}
}
