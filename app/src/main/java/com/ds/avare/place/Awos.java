/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ds.avare.place;

/**
 * 
 * @author jmcgraw
 * 
 * 
 */
public class Awos {
	private String mType;
	private double mLatitude;
	private double mLongitude;
	private String mFrequency1;
	private String mFrequency2;
	private String mPhone1;
	private String mPhone2;
	private String mRemarks;

	public static final float INVALID = -1000;

	/**
	 * Create a new awos instance of type
	 */
	public Awos(String type) {
		mType = type;
		mLongitude = INVALID;
		mLatitude = INVALID;
	}

	// Get operations
	public String getType() {
		return mType;
	}

	public double getLat() {
		return mLatitude;
	}

	public double getLon() {
		return mLongitude;
	}

	public String getRemarks() {
		return mRemarks;
	}

	public String getFreq1() {

		return mFrequency1;
	}

	public String getPhone1() {

		return mPhone1;
	}

	public String getFreq2() {
		return mFrequency2;
	}

	public String getPhone2() {
		return mPhone2;
	}

	// Set operations
	public void setType(String type) {
		mType = type;

	}

	public void setLat(String lat) {
		try {
			mLatitude = Double.parseDouble(lat);
		} catch (Exception e) {
		}
	}

	public void setLon(String lon) {
		try {
			mLongitude = Double.parseDouble(lon);
		} catch (Exception e) {
		}

	}

	public void setRemark(String remark) {
		mRemarks = remark;
	}

	public void setFreq1(String freq1) {
		mFrequency1 = freq1;

	}

	public void setFreq2(String freq2) {
		mFrequency2 = freq2;

	}

	public void setPhone1(String phone1) {
		mPhone1 = phone1;

	}

	public void setPhone2(String phone2) {
		mPhone2 = phone2;

	}

}
