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

import com.ds.avare.connections.Connection;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.nmea.BODPacket;
import com.ds.avare.nmea.GGAPacket;
import com.ds.avare.nmea.RMBPacket;
import com.ds.avare.nmea.RMCPacket;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Projection;
import com.ds.avare.utils.Helper;

import java.util.concurrent.ConcurrentLinkedQueue;

import static android.os.SystemClock.sleep;

/**
 *
 * @author rwalker
 * All functionality required to generate the NMEA sentances to drive an autopilot
 */
public class AutoPilot {

    private ConcurrentLinkedQueue<WorkItem> mWorkItems; // A collection of position reports
    private Connection mConnection;                     // Where to send the NMEA data
    private boolean    mShutdown;                       // true when we need to shutdown
    private Thread mThread;                             // thread to do the bulk of the work

    // an item of GPS data that needs processing. We maintain a queue of these items
    // that gets processed on a background thread.
    private class WorkItem {
        private Plan         mPlan;
        private Destination  mDest;
        private GpsParams    mGpsParams;

        private WorkItem(GpsParams gpsParams, Plan plan, Destination dest) {
            mGpsParams = gpsParams;
            mPlan = plan;
            mDest = dest;
        }
    }

    // Create an autopilot object.
    public AutoPilot(Connection aConnection) {

        // Init our member variables
        mConnection = aConnection;
        mShutdown   = false;
        mWorkItems  = new ConcurrentLinkedQueue<>();

        // Create a thread that will do the bulk of the work.
        // Read an item from the work queue, formulate the NMEA sentences, write
        // those sentences to the connection.
        mThread = new Thread(new Runnable() {
            public void run() {
                WorkItem workItem;
                while(!mShutdown) { // Stay here till told to shutdown
                    // data packets are added to the mWorkItems list
                    while (null != (workItem = mWorkItems.poll())) {

                        // extract the items for clarity and speed
                        GpsParams gpsParams = workItem.mGpsParams;
                        Destination dest    = workItem.mDest;
                        Plan plan           = workItem.mPlan;

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

                        // Write this block of NMEA data to the output connection
                        mConnection.write(apText.getBytes());

                        // A single workitem has just been processed, sleep for a bit then go
                        // look for the next one
                        try {
                            sleep(500);
                        } catch (Exception ignore) {  }

                    }
                    // There was nothing in the queue. Sleep for a bit then go look again
                    try {
                        sleep(500);
                    } catch (Exception ignore) {  }
                }
            }
        });

        // Fire up that background thread
        mThread.start();
    }

    // Called when we have new GPS data to process against the plan.
    public void setGpsData(GpsParams gpsParams, Plan plan, Destination dest) {
        if(!mConnection.isDead()) {
            mWorkItems.add(new WorkItem(gpsParams, plan, dest));
        }
    }

    // System is shutting down. Shut down our connection, then tell the
    // background thread to terminate.
    public void shutdown() {
        mShutdown = true;
        try { mThread.join();
        } catch (Exception ignore) { }
        mConnection.disconnect();
    }

    // Is the autopilot fully connected
    public boolean isConnected() {
        return mConnection.isConnected();
    }
}
