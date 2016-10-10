/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.content.Context;

import com.ds.avare.R;
import com.ds.avare.place.NavAid;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;

import java.util.Locale;
import java.util.Vector;

/**
 * Created by pasniak on 10/9/2016.
 */

public class NavAidHelper {

    private double lonReference;
    private double latReference;
    private double variation;
    private Context ctx;

    public NavAidHelper(Context ctx, double lon0, double lat0, double variation) {
        this.lonReference = lon0;
        this.latReference = lat0;
        this.variation = variation;
        this.ctx = ctx;
    }

    private String getNavaidLocation(Coordinate navaidCoordinate) {
        Projection p = new Projection(
                navaidCoordinate.getLongitude(), navaidCoordinate.getLatitude(),
                lonReference, latReference);
        long radial = Math.round(Helper.getMagneticHeading(p.getBearing(), variation));
        return " on " + String.format(Locale.getDefault(), "%03d", radial) + ctx.getString(R.string.degree) + " radial " +
                Math.round(p.getDistance()) + Preferences.distanceConversionUnit;

    }

    public String toHtmlString(Vector<NavAid> navaids) {
        String result = "";
        for (NavAid na: navaids) {
            result += (result != "" ? "<br>" : "") // order in Chart Supplement convention
                    + na.getLongName() + " "+ na.getType() + " " + na.getFrequency() + " " + na.getLocationId()
                    + " "+ getNavaidLocation(na.getCoords()) ;
        }
        return  result;
    }
}
