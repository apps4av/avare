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

import com.ds.avare.place.NavAid;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;

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
    private String getNavaidLocationAsHtml(Coordinate navaidCoordinate, int navaidVariation, String navaidClass, double navaidElevation) {
        Projection p = new Projection(
                navaidCoordinate.getLongitude(), navaidCoordinate.getLatitude(),
                lonReference, latReference);
        double distanceToNavAid = p.getDistance();
        boolean isReceived = isVorReceived(distanceToNavAid, navaidClass, altitudeReference - navaidElevation)
                || !("TLH".contains(navaidClass) || navaidClass.isEmpty());
        long radial = Math.round(Helper.getMagneticHeading(p.getBearing(), -navaidVariation));

        final String LIGHT_RED = "#ff6666", LIGHT_GREEN = "#99ff66";
        return  String.format(Locale.getDefault(), "%03d", radial)
                + "<font color='" + (isReceived ? LIGHT_GREEN : LIGHT_RED) + "'>"
                + String.format(Locale.getDefault(), "%03d", Math.round(distanceToNavAid))
                + "</font>";
    }

    /** format vector of navaids as a string */
    public String toHtmlString(Vector<NavAid> navaids) {
        String result = "<table>";
        if (navaids != null) {
            for (NavAid na : navaids) {

                result += "<tr><td>";

                result += // fields order same as Chart Supplement convention
                        na.getLocationId()
                        + getNavaidLocationAsHtml(na.getCoords(), na.getVariation(), na.getNavaidClass(), na.getElevation()) + " "
                        + "</td><td>&nbsp;"
                        + na.getFrequency()
                        + "</td><td>&nbsp;"
                        + (na.hasHiwas() ? "(H)" : "")
                        + "</td><td>&nbsp;"
                        + MorseCodeGenerator.getInstance().getCodeHtml(na.getLocationId());

                result += "</td></tr>";
            }
        }
        return result + "</table>";
    }
}
