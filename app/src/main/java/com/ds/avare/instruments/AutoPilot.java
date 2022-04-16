/*
Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.instruments;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.nmea.BODPacket;
import com.ds.avare.nmea.GGAPacket;
import com.ds.avare.nmea.RMBPacket;
import com.ds.avare.nmea.RMCPacket;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Projection;
import com.ds.avare.utils.Helper;

/**
 *
 * @author rwalker
 * All functionality required to generate the NMEA sentances to drive an autopilot. No instance
 * data is part of this object
 */
public class AutoPilot {
    public static String apCreateSentences(GpsParams gpsParams, Plan plan, Destination dest) {
        // Create NMEA packet #1
        RMCPacket rmcPacket = new RMCPacket(
                gpsParams.getTime(),
                gpsParams.getLatitude(),
                gpsParams.getLongitude(),
                gpsParams.getSpeedInKnots(),
                gpsParams.getBearing(),
                gpsParams.getDeclinition()
        );
        String apText = rmcPacket.getPacket();

        // Create NMEA packet #2
        GGAPacket ggaPacket = new GGAPacket(
                gpsParams.getTime(),
                gpsParams.getLatitude(),
                gpsParams.getLongitude(),
                gpsParams.getAltitudeInMeters(),
                gpsParams.getSatCount(),
                gpsParams.getGeoid(),
                gpsParams.getHorDil()
        );
        apText += ggaPacket.getPacket();

        // If we have a destination set, then we need to add some more sentences
        // to tell the autopilot how to steer
        if (null != dest) {
            String startID = "";

            // This is the bearing from our starting point, to our current
            // destination
            double brgOrig = Projection.getStaticBearing(
                    dest.getLocationInit().getLongitude(),
                    dest.getLocationInit().getLatitude(),
                    dest.getLocation().getLongitude(),
                    dest.getLocation().getLatitude());

            // If we have a flight plan active, then we may need to re-calc the
            // original bearing based upon the most recently passed waypoint in the plan.
            if (null != plan) {
                if (plan.isActive()) {
                    int nnp = plan.findNextNotPassed();
                    if (nnp > 0) {
                        Destination prevDest = plan.getDestination(nnp - 1);
                        if (null != prevDest) {
                            startID = prevDest.getID();
                            brgOrig = Projection.getStaticBearing(
                                    prevDest.getLocation().getLongitude(),
                                    prevDest.getLocation().getLatitude(),
                                    dest.getLocation().getLongitude(),
                                    dest.getLocation().getLatitude());
                        }
                    }
                }
            }

            // Calculate how many miles we are to the side of the course line
            double deviation = dest.getDistanceInNM() *
                    Math.sin(Math.toRadians(Helper.angularDifference(brgOrig, dest.getBearing())));

            // If we are to the left of the course line, then make our deviation negative.
            if(Helper.leftOfCourseLine(dest.getBearing(),  brgOrig)) {
                deviation = -deviation;
            }

            // Limit our station IDs to 4 chars max so we don't exceed the 80 char
            // sentence limit. A "GPS" fix has a temp name that is quite long
            if(startID.length() > 4) {
                startID = "gSRC";
            }

            String endID = dest.getID();
            if(endID.length() > 4) {
                endID = "gDST";
            }

            // We now have all the info to create NMEA packet #3
            RMBPacket rmbPacket = new RMBPacket(
                    dest.getDistanceInNM(),
                    dest.getBearing(),
                    dest.getLocation().getLongitude(),
                    dest.getLocation().getLatitude(),
                    endID,
                    startID,
                    deviation,
                    gpsParams.getSpeedInKnots(),
                    (null != plan) && (!plan.isActive() && plan.allWaypointsPassed())
            );
            apText += rmbPacket.getPacket();

            // Now for the final NMEA packet
            BODPacket bodPacket = new BODPacket(
                    endID,
                    startID,
                    brgOrig,
                    (brgOrig + dest.getDeclination())
            );
            apText += bodPacket.getPacket();
        }
        return apText;
    }
}
