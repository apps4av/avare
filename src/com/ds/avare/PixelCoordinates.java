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
 * This class does all work and implements state machine to deal with user input on
 * the plates screen to calibrate plates.
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
	private double mOLon;
	private double mOLat;	
    private double mRotated;
    private boolean mLat0Set;
    private boolean mLon0Set;
    private boolean mLat1Set;
    private boolean mLon1Set;


    public static final int POINTS_MIN_PIXELS = 300;
    

	/**
	 * 
	 */
	public PixelCoordinates() {
	    resetPoints();
	}

	/**
	 * 
	 */
	public void resetPoints() {
	    mX0 = mY0 = mX1 = mY1 = mLon0 = mLat0 = mLon1 = mLat1 = 0;
	    mOLon = mOLat = mPx = mPy = 0;
	    mLon0Set = mLat0Set = mLon1Set = mLat1Set = false;
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
	public boolean setX1(double x) {
		mX1 = x;
		if(Math.abs(mX0 - mX1) < POINTS_MIN_PIXELS) {
		    mLon1Set = mLat1Set = false;
		    return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param y
	 */
	public boolean setY1(double y) {
		mY1 = y;
        if(Math.abs(mY0 - mY1) < POINTS_MIN_PIXELS) {
            mLon1Set = mLat1Set = false;
            return false;
        }
        return true;		
	}

	/**
	 * 
	 */
	public void unsetPoint0() {
        mLon0Set = false;
        mLat0Set = false;
	}

	/**
     * 
     */
    public void unsetPoint1() {
        mLon1Set = false;
        mLat1Set = false;
    }

	/**
	 * 
	 * @param lon
	 * @param minsec
	 */
	public boolean setLongitude0(String lon, String minsec) {
        mLon0Set = false;
		try {
			mLon0 = Double.parseDouble(lon);
			mLon0 = mLon0 + Double.parseDouble(minsec) / 60;
			mLon0 = -mLon0;
			
			if(mLon0 >= 0 || mLon0 < -180) {
			    return false;
			}
			mLon0Set = true;
			return true;
		}
		catch (Exception e) {
			mLon0 = 0;
		}
		return false;
	}
	
	/**
	 * 
	 * @param lat
	 * @param minsec
	 */
	public boolean setLatitude0(String lat, String minsec) {
        mLat0Set = false;
		try {
			mLat0 = Double.parseDouble(lat);
			mLat0 = mLat0 + Double.parseDouble(minsec) / 60;				
            if(mLat0 <= 0 || mLat0 > 90) {
                return false;
            }
            mLat0Set = true;
			return true;
		}
		catch (Exception e) {
			mLat0 = 0;
		}
        return false;
	}

	/**
	 * 
	 * @param lon
	 * @param minsec
	 */
	public boolean setLongitude1(String lon, String minsec) {
        mLon1Set = false;
		try {
			mLon1 = Double.parseDouble(lon);
			mLon1 = mLon1 + Double.parseDouble(minsec) / 60;
			mLon1 = -mLon1;
	        if(mLon1 >= 0 || mLon1 < -180) {
	            return false;
	        }
	        if((mLon0 - mLon1) == 0) {
                return false;	            
	        }
	        mLon1Set = true;
			return true;
		}
		catch (Exception e) {
			mLon1 = 0;
		}
        return false;
	}
	
	/**
	 * 
	 * @param lat
	 * @param minsec
	 */
	public boolean setLatitude1(String lat, String minsec) {
        mLat1Set = false;
		try {
			mLat1 = Double.parseDouble(lat);
			mLat1 = mLat1 + Double.parseDouble(minsec) / 60;				
            if(mLat1 <= 0 || mLat1 > 90) {
                return false;
            }
            if((mLat0 - mLat1) == 0) {
                return false;               
            }
            mLat1Set = true;
	        return true;
		}
		catch (Exception e) {
			mLat1 = 0;
		}
        return false;
	}
	
	/**
	 * @return
	 */
	public boolean secondPointAcquired() {
	    
		if(mLon0Set && mLat0Set && mLon1Set && mLat1Set) {
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean firstPointAcquired() {
        if(mLon0Set && mLat0Set && (!mLon1Set) && (!mLat1Set)) {
            return true;
        }
		return false;
	}

   /**
     * @return
     */
    public boolean noPointAcquired() {
        
        if((!mLon0Set) && (!mLat0Set) && (!mLon1Set) && (!mLat1Set)) {
            return true;
        }
        return false;
    }

	/**
	 * @return return origin, lat/lon of origin and px, py
	 */
	public double[] get() {
		if(secondPointAcquired()) {
			
		    
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
