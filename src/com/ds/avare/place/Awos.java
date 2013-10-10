/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
Jesse McGraw (jlmcgraw@gmail.com)

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ds.avare.place;

import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 * 
 * 
 */
public class Awos {

	private String mLocationID;
	private String mType;
	private String mFacilityName;
	private String mPhone;
	private double mLatitude;
	private double mLongitude;
	private String mFrequency;
	private String mAptId;
	private String mRemarks;

	public static final float INVALID = -1000;

	/**
     * 
     */
	public Awos(String type) {
		mType = type;
		mLongitude = INVALID;
		mLatitude = INVALID;

	}

	/**
	 * 
	 * @return
	 */
	public double getLongitude() {
		return mLongitude;
	}

	/**
	 * 
	 * @return
	 */
	public double getLatitude() {
		return mLatitude;
	}

	public String getType() {
		return mType;
	}

	public String getFreq() {
		return mFrequency;
	}

	public String getPhone() {
		return mPhone;
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

	public void setType(String type) {
		mType = type;

	}

	public void setName(String name) {
		mFacilityName = name;

	}

	public void setPhone(String phone) {
		mPhone = phone;

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

	public void setFreq(String frequency) {
		mFrequency = frequency;

	}

	public void setAptId(String aptId) {
		mAptId = aptId;

	}

	public void setRemark(String remark) {
		mRemarks = remark;

	}

}
