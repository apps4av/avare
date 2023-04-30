/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2023, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.flight;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Aircraft data
 * @author zkhan
 *
 */
public class Aircraft {

    private String mId;
    private String mType;
    private int mICao;
    private String mWake;
    private String mEquipment;
    private float mCruiseTas;
    private String mSurveillance;
    private float mEndurance;
    private String mColor;
    private String mPic;
    private String mPilotInfo;
    private float mSinkRate;
    private float mFuelBurnRate;
    private String mHomeBase;

    // Default
    public Aircraft() {
        defaultAc();
    }

    private void defaultAc() {
        mId = "N1TEST";
        mType = "C172";
        mICao = 123456;
        mWake = "LIGHT";
        mEquipment = "N";
        mCruiseTas = 110;
        mSurveillance = "N";
        mEndurance = 5.5f;
        mColor = "W/B";
        mPic = "Test Pilot";
        mPilotInfo = "Test Pilot N/A";
        mSinkRate = 700;
        mFuelBurnRate = 10;
        mHomeBase = "KTTT";
    }

    public Aircraft(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            mId = obj.getString("id");
            mType = obj.getString("type");
            mICao = obj.getInt("icao");
            mWake = obj.getString("wake");
            mEquipment = obj.getString("equipment");
            mCruiseTas = (float)obj.getDouble("cruise_tas");
            mSurveillance = obj.getString("surveillance");
            mEndurance = (float)obj.getDouble("endurance");
            mColor = obj.getString("color");
            mPic = obj.getString("pic");
            mPilotInfo = obj.getString("pilot_info");
            mSinkRate = (float)obj.getDouble("sink_rate");
            mFuelBurnRate = (float)obj.getDouble("fuel_burn_rate");
            mHomeBase = obj.getString("home_base");
        } catch (JSONException e) {
            defaultAc();
            e.printStackTrace();
        }
    }

    public String getJSON() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", mId);
            obj.put("type", mType);
            obj.put("icao", mICao);
            obj.put("wake", mWake);
            obj.put("equipment", mEquipment);
            obj.put("cruise_tas", mCruiseTas);
            obj.put("surveillance", mSurveillance);
            obj.put("endurance", mEndurance);
            obj.put("color", mColor);
            obj.put("pic", mPic);
            obj.put("pilot_info", mPilotInfo);
            obj.put("sink_rate", mSinkRate);
            obj.put("fuel_burn_rate", mFuelBurnRate);
            obj.put("home_base", mHomeBase);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return obj.toString();
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getType() {
        return mType;
    }

    public void setType(String mType) {
        this.mType = mType;
    }

    public int getICao() {
        return mICao;
    }

    public void setICao(int mICao) {
        this.mICao = mICao;
    }

    public String getWake() {
        return mWake;
    }

    public void setWake(String mWake) {
        this.mWake = mWake;
    }

    public String getEquipment() {
        return mEquipment;
    }

    public void setEquipment(String mEquipment) {
        this.mEquipment = mEquipment;
    }

    public float getCruiseTas() {
        return mCruiseTas;
    }

    public void setCruiseTas(float mCruiseTas) {
        this.mCruiseTas = mCruiseTas;
    }

    public String getSurveillance() {
        return mSurveillance;
    }

    public void setSurveillance(String mSurveillance) {
        this.mSurveillance = mSurveillance;
    }

    public float getEndurance() {
        return mEndurance;
    }

    public void setEndurance(float mEndurance) {
        this.mEndurance = mEndurance;
    }

    public String getColor() {
        return mColor;
    }

    public void setColor(String mColor) {
        this.mColor = mColor;
    }

    public String getPic() {
        return mPic;
    }

    public void setPic(String mPic) {
        this.mPic = mPic;
    }

    public String getPilotInfo() {
        return mPilotInfo;
    }

    public void setPilotInfo(String mPilotInfo) {
        this.mPilotInfo = mPilotInfo;
    }

    public float getSinkRate() {
        return mSinkRate;
    }

    public void setSinkRate(float mSinkRate) {
        this.mSinkRate = mSinkRate;
    }

    public float getFuelBurnRate() {
        return mFuelBurnRate;
    }

    public void setFuelBurnRate(float mFuelBurnRate) {
        this.mFuelBurnRate = mFuelBurnRate;
    }

    public String getHomeBase() {
        return mHomeBase;
    }

    public void setHomeBase(String mHomeBase) {
        this.mHomeBase = mHomeBase;
    }


}
