/**
 * Wind triangle solution
 * zkhan
 */
package com.ds.avare.utils;

public class WindTriagle {


    /**
     * | Triangle ABC
     * |    <a = ta - ga; <c = 180 - ta - wa; <b = wa + ga
     * |    a * a = b * b + c * c - 2 b c cos(<a)
     * |    ts * ts =  gs * gs + ws * ws + - 2 gs ws cos (ga - wa - 180)
     * |          C
     * |         .------------------
     * |        /  . ws     wa
     * |       /     . A
     * |      /     /
     * |     /    gs
     * |    ts  /
     * |   /  /
     * |  ta/     ga
     * | //
     * |/______________________________________________________
     * A
     */
    public static double[] getTrueFromGroundAndWind(double gs, double ga, double ws, double wa) {
        double tr[] = new double[2];
        if(gs == 0) {
            gs = 0.01; // avoid NaN when gs = 0
        }
        double angle = (ga - wa - 180) * Math.PI / 180.0;
        double ts = Math.sqrt(ws * ws + gs * gs - 2.0 * ws * gs * Math.cos(angle));
        if(ts == 0) {
            ts = 0.01; // avoid NaN when ts = 0
        }
        double ta = Math.acos((ts * ts + gs * gs - ws * ws) / (2 * ts * gs)) * 180 / Math.PI + ga;
        tr[0] = ts;
        tr[1] = ta;
        return(tr);
    }

    /**
     * Solve the wind triangle to find ground speed and wind correction angle from a
     * known true airspeed, course and wind. Mirrors avarex WindSolution.solveWindTriangle.
     *
     * @param windSpeed        wind speed
     * @param windDirectionDeg wind direction (FROM), degrees
     * @param courseDeg        desired true course, degrees
     * @param trueAirspeed     true airspeed
     * @return array of [wcaDeg (+right / -left), headingDeg, groundSpeed]
     */
    public static double[] solveWindTriangle(double windSpeed, double windDirectionDeg, double courseDeg, double trueAirspeed) {

        double windDirRad = Math.toRadians(windDirectionDeg);
        double courseRad = Math.toRadians(courseDeg);

        double theta = windDirRad - courseRad;

        // components
        double crosswind = windSpeed * Math.sin(theta);
        double headwind = windSpeed * Math.cos(theta);

        // clamp for safety
        double ratio = trueAirspeed == 0 ? 0 : crosswind / trueAirspeed;
        if(ratio > 1.0) {
            ratio = 1.0;
        }
        if(ratio < -1.0) {
            ratio = -1.0;
        }

        // wind correction angle
        double wcaRad = Math.asin(ratio);

        // heading
        double headingDeg = Math.toDegrees(courseRad + wcaRad);
        headingDeg = (headingDeg % 360 + 360) % 360;

        // ground speed (corrected)
        double groundSpeed = Math.sqrt(trueAirspeed * trueAirspeed - crosswind * crosswind) - headwind;

        return new double[] {Math.toDegrees(wcaRad), headingDeg, groundSpeed};
    }


}
