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
    private Context ctx;
    private double altitudeReference;

    public NavAidHelper(Context ctx, double lon0, double lat0, double altitude) {
        this.lonReference = lon0;
        this.latReference = lat0;
        this.ctx = ctx;
        this.altitudeReference = altitude;
    }

    /** transcode reception at altitude table
     * from https://nfdc.faa.gov/webContent/56DaySub/2016-11-10/Layout_Data/nav_rf.txt
            L AN 0001 00577 N29     PROTECTED FREQUENCY ALTITUDE
                          H=HIGH, L=LOW, T=TERMINAL

                          CLASS     ALTITIUDE            MILES
                          -----     ---------            -----
                          T         12,000' AND BELOW      25
                          L         BELOW 18,000'          40
                          H         BELOW 18,000'          40
                          H         WITHIN THE CONTER-     100
                                    MINOUS 48 STATES
                                    ONLY BETWEEN 14,500'
                                    AND 17,999'
                          H         18,000' FL 450         130
                          H         ABOVE FL 450           100
        */
    private static boolean isVorReceived(double d, String vorClass, double agl)
    {
        return     (vorClass.equals("T") && agl < 12000 && d <= 25)
                || (vorClass.equals("L") && agl < 18000 && d <= 40)
                || (vorClass.equals("H") && agl < 18000 && d <= 40)
                || (vorClass.equals("H") && 14500 < agl && agl <= 18000 && d <= 100)
                || (vorClass.equals("H") && 18000 < agl && agl <= 45000 && d <= 130)
                || (vorClass.equals("H") && 45000 < agl && d <= 100);
    }

    /** format distance to a particular navaid as a string */
    private String getNavaidLocation(Coordinate navaidCoordinate, int navaidVariation, String navaidClass) {
        Projection p = new Projection(
                navaidCoordinate.getLongitude(), navaidCoordinate.getLatitude(),
                lonReference, latReference);
        double distanceToNavAid = p.getDistance();
        boolean isReceived = isVorReceived(distanceToNavAid, navaidClass, altitudeReference)
                || !("TLH".contains(navaidClass) || navaidClass.isEmpty());
        long radial = Math.round(Helper.getMagneticHeading(p.getBearing(), navaidVariation));
        return " on " + String.format(Locale.getDefault(), "%03d", radial) + ctx.getString(R.string.degree) + " radial "
                + (!isReceived ? "<font color='yellow'>" : "")
                + Math.round(distanceToNavAid) + Preferences.distanceConversionUnit
                + (!isReceived ? "</font>" : "")
            ;
    }

    /** format vector of navaids as a string */
    public String toHtmlString(Vector<NavAid> navaids) {
        String result = "";
        if (navaids != null) {
            for (NavAid na : navaids) {
                result += (result != "" ? "<br>" : "") // fields' order same as Chart Supplement convention
                        + na.getLongName() + " " + na.getType()
                        + (na.hasHiwas() ? "<sup>(H)</sup>" : "")
                        + " " + na.getFrequency() + " " + na.getLocationId()
                        + " " + getNavaidLocation(na.getCoords(), na.getVariation(), na.getNavaidClass());
            }
        }
        return result;
    }
}
