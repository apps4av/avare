/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import com.ds.avare.position.Coordinate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Created by pasniak on 10/9/2016.
 */

public class NavAid {

    private String mLocationId;
    private String mFacilityName;
    private String mType;
    private Coordinate mCoords;
    private int mVariation;
    private String mNavaidClass;
    private boolean mHiwas;
    private double mElevation;

    public String getLocationId() {
        return mLocationId;
    }

    private static final Pattern FrequencyPattern = Pattern.compile("(\\d.+)");

    public String getFrequency() {
        Matcher m = FrequencyPattern.matcher(mFacilityName);
        return m.find() ? m.group(1) : "";
    }

    public String getLongName() {
        return FrequencyPattern.split(mFacilityName)[0];
    }

    public String getType() {
        return mType;
    }
    public Coordinate getCoords() {
        return mCoords;
    }
    public int getVariation() { return mVariation; }
    public String getNavaidClass() { return mNavaidClass; }
    public boolean hasHiwas() { return mHiwas; }
    public double getElevation() { return mElevation;  }

    public NavAid(String location, String type, String facility, Coordinate coords, int variation, String navaidClass, boolean hiwas, double elevation) {
        mLocationId = location;
        mType = type;
        mFacilityName = facility;
        mCoords = coords;
        mVariation = variation;
        mNavaidClass = navaidClass;
        mHiwas = hiwas;
        mElevation = elevation;
    }

}
