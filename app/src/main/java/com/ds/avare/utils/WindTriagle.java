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


}
