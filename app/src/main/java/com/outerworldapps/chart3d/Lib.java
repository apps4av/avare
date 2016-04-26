//    Copyright (C) 2016, Mike Rieker, Beverly, MA USA
//    www.outerworldapps.com
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; version 2 of the License.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.
//
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//    http://www.gnu.org/licenses/gpl-2.0.html

package com.outerworldapps.chart3d;

public class Lib {
    // fine-tuned constants
    public final static int MPerNM         = 1852;    // metres per naut mile
    public final static int NMPerDeg       =   60;    // naut mile per deg
    public final static double EARTHRADIUS = 360.0 * NMPerDeg * MPerNM / 2.0 / Math.PI;
    public final static double MTEVEREST   = 8848.0;  // metres msl

    /**
     * Return westmost longitude, normalized to -180..+179.999999999
     */
    public static double Westmost (double lon1, double lon2)
    {
        // make sure lon2 comes at or after lon1 by less than 360deg
        while (lon2 < lon1) lon2 += 360.0;
        while (lon2 - 360.0 >= lon1) lon2 -= 360.0;

        // if lon2 is east of lon1 by at most 180deg, lon1 is westmost
        // if lon2 is east of lon1 by more than 180, lon2 is westmost
        double westmost = (lon2 - lon1 < 180.0) ? lon1 : lon2;

        return NormalLon (westmost);
    }

    /**
     * Return eastmost longitude, normalized to -180..+179.999999999
     */
    public static double Eastmost (double lon1, double lon2)
    {
        // make sure lon2 comes at or after lon1 by less than 360deg
        while (lon2 < lon1) lon2 += 360.0;
        while (lon2 - 360.0 >= lon1) lon2 -= 360.0;

        // if lon2 is east of lon1 by at most 180deg, lon2 is eastmost
        // if lon2 is east of lon1 by more than 180, lon1 is eastmost
        double eastmost = (lon2 - lon1 < 180.0) ? lon2 : lon1;

        return NormalLon (eastmost);
    }

    /**
     * Normalize a longitude in range -180.0..+179.999999999
     */
    public static double NormalLon (double lon)
    {
        while (lon < -180.0) lon += 360.0;
        while (lon >= 180.0) lon -= 360.0;
        return lon;
    }

    /**
     * Given two normalized longitudes, find the normalized average longitude.
     */
    public static double AvgLons (double lona, double lonb)
    {
        if (lonb - lona > 180.0) lona += 360.0;
        if (lona - lonb > 180.0) lonb += 360.0;
        return NormalLon ((lona + lonb) / 2.0);
    }


    /**
     * Compute great-circle distance between two lat/lon co-ordinates
     * @return distance between two points (in nm)
     */
    public static double LatLonDist (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        return Math.toDegrees (LatLonDist_rad (srcLat, srcLon, dstLat, dstLon)) * NMPerDeg;
    }

    public static double LatLonDist_rad (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        // http://en.wikipedia.org/wiki/Great-circle_distance
        double sLat = srcLat / 180 * Math.PI;
        double sLon = srcLon / 180 * Math.PI;
        double fLat = dstLat / 180 * Math.PI;
        double fLon = dstLon / 180 * Math.PI;
        double dLon = fLon - sLon;
        double t1   = Sq (Math.cos (fLat) * Math.sin (dLon));
        double t2   = Sq (Math.cos (sLat) * Math.sin (fLat) - Math.sin (sLat) * Math.cos (fLat) * Math.cos (dLon));
        double t3   = Math.sin (sLat) * Math.sin (fLat);
        double t4   = Math.cos (sLat) * Math.cos (fLat) * Math.cos (dLon);
        return Math.atan2 (Math.sqrt (t1 + t2), t3 + t4);
    }

    private static double Sq (double x) { return x*x; }

    /**
     * Compute great-circle true course from one lat/lon to another lat/lon
     * @return true course (in degrees) at source point (-180..+179.999999)
     */
    public static double LatLonTC (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        return LatLonTC_rad (srcLat, srcLon, dstLat, dstLon) * 180.0 / Math.PI;
    }

    public static double LatLonTC_rad (double srcLat, double srcLon, double dstLat, double dstLon)
    {
        // http://en.wikipedia.org/wiki/Great-circle_navigation
        double sLat = srcLat / 180 * Math.PI;
        double sLon = srcLon / 180 * Math.PI;
        double fLat = dstLat / 180 * Math.PI;
        double fLon = dstLon / 180 * Math.PI;
        double dLon = fLon - sLon;
        double t1 = Math.cos (sLat) * Math.tan (fLat);
        double t2 = Math.sin(sLat) * Math.cos(dLon);
        return Math.atan2 (Math.sin (dLon), t1 - t2);
    }

    /**
     * Compute new lat/lon given old lat/lon, heading (degrees), distance (nautical miles)
     * http://stackoverflow.com/questions/7222382/get-lat-long-given-current-point-distance-and-bearing
     */
    public static double LatHdgDist2Lat (double latdeg, double hdgdeg, double distnm)
    {
        double distrad = Math.toRadians (distnm / NMPerDeg);
        double latrad  = Math.toRadians (latdeg);
        double hdgrad  = Math.toRadians (hdgdeg);

        latrad = Math.asin (Math.sin (latrad) * Math.cos (distrad) + Math.cos (latrad) * Math.sin (distrad) * Math.cos (hdgrad));
        return Math.toDegrees (latrad);
    }

    public static double LatLonHdgDist2Lon (double latdeg, double londeg, double hdgdeg, double distnm)
    {
        double distrad = Math.toRadians (distnm / NMPerDeg);
        double latrad  = Math.toRadians (latdeg);
        double hdgrad  = Math.toRadians (hdgdeg);

        double newlatrad = Math.asin (Math.sin (latrad) * Math.cos (distrad) + Math.cos (latrad) * Math.sin (distrad) * Math.cos (hdgrad));
        double lonrad = Math.atan2 (Math.sin (hdgrad) * Math.sin (distrad) * Math.cos (latrad), Math.cos (distrad) - Math.sin (latrad) * Math.sin (newlatrad));
        return Lib.NormalLon (Math.toDegrees (lonrad) + londeg);
    }
}
