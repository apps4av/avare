/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.externalFlightPlan;

import com.ds.avare.place.Destination;
import com.ds.avare.userDefinedWaypoints.Waypoint;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ron on 7/1/2015.
 */
public class SkvPlanParser extends PlanParser {
    private static final String SKV = "skv";
    private static final String PLAN = "plan=";
    private static final String WPSEP = ":";
    private static final String FIELDSEP = "\\.";
    private static final String ALTFIELDSEP = ",";

    @Override
    public String getType() { return SKV; }

    @Override
    public ExternalFlightPlan parse(String fileName, FileInputStream inputStream) {
        try {
            // Break the name of the plan out of the fileName
            String planName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf("."));

            // Create a buffered reader from the input stream
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            // Read the first line of data
            String fileLine = br.readLine();

            // If we read a valid line, then try and process it
            while (null != fileLine) {

                // Find the plan portion in the line
                int planLoc = fileLine.lastIndexOf(PLAN);

                // Did we find the start of the plan ? ie: NOT -1
                if(-1 != planLoc) {

                    List<Waypoint> points = new ArrayList<Waypoint>();
                    String planText = fileLine.substring(planLoc + PLAN.length());

                    // At this point we have a string of points in the following format:
                    // A.K4.40XS:F.K4.KALLA:A.K4.KGTU:V.K4.CWK:N.K4.HLR:A.K4.9TX4:G.30.867928704762814,-97.7460479685386:

                    // Split the string at each colon ':' character
                    String[] wp = planText.split(WPSEP);

                    // Now walk through all the waypoints found
                    for(String w : wp) {
                        String[] wpf = w.split(FIELDSEP);

                        // First field is the TYPE of point
                        String type =
                                wpf[0].equals("A") ? Destination.BASE :     // Airport
                                wpf[0].equals("F") ? Destination.FIX :      // FIX
                                wpf[0].equals("V") ? Destination.NAVAID :   // VOR
                                wpf[0].equals("N") ? Destination.NAVAID :   // NDB
                                wpf[0].equals("G") ? Destination.GPS :      // GPS Co-ord
                                null;                                       // Unknown currently

                        // Third field is the name. If it starts with a K and is 4 letters
                        // in length, then strip the first character.
                        String name = wpf[2];
                        if(4 == name.length() && true == name.startsWith("K")) {
                            name = name.substring(1);
                        }

                        // If this is a GPS point, then we parse things a bit differently
                        // G.30.781840081788935,-97.21321105444247
                        if(Destination.GPS == type) {

                            // Re-split this field based upon the alternate field separator
                            wpf = w.split(ALTFIELDSEP);

                            // Make the name equal to "lat&lon";
                            name = wpf[0].substring(2) + "&" + wpf[1];
                        }

                        // Now add this waypoint to our collection if we were able to parse it
                        if(null != type) {
                            points.add(new Waypoint(name, type, 0, 0, false, Waypoint.MT_NONE, false));
                        }
                    }

                    // All the points have been parsed from the line. Now return with the new plan
                    return new ExternalFlightPlan(planName, "", SKV, points);
                }

                // We did not find a valid line containing the plan. Read the next line in the file
                fileLine = br.readLine();
            }
        } catch (Exception  e) { }
        return null;
    }

    @Override
    public void generate(String fileName, FileOutputStream outputStream, ExternalFlightPlan externalFlightPlan) {

    }
}
