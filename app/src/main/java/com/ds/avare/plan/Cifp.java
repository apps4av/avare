/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.plan;

import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.position.Coordinate;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

import java.util.LinkedList;

/**
 * Created by zkhan on 11/8/15.
 */
public class Cifp {

    private String mInitialCourse;
    private String mFinalCourse;
    private String mMissedCourse;
    private String mAirport;
    private CreateTask mCreateTask;

    public Cifp(String airport, String initialCourse, String initialAltitude, String finalCourse, String finalAltitude, String missedCourse, String missedAltitude) {
        // Make CIFP from CIFP database
        mInitialCourse = initialCourse.replaceAll(",$", "").replace(",", " ");
        mFinalCourse = finalCourse.replaceAll(",$", "").replace(","," ");
        mMissedCourse = missedCourse.replaceAll(",$", "").replace(",", " ");
        mAirport = airport;
    }

    public String getInitialCourse() {
        return mInitialCourse;
    }
    public String getFinalCourse() {
        return mFinalCourse;
    }
    public String getMissedCourse() {
        return mMissedCourse;
    }

    public void setApproach(StorageService service, Preferences prefs) {
        String wp = mInitialCourse + " " + mFinalCourse + " " + mMissedCourse;

        if(mCreateTask != null && mCreateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mCreateTask.cancel(true);
        }
        mCreateTask = new CreateTask();
        mCreateTask.execute(wp, service, prefs);
    }

    /**
     * Parse type of approach [0] and runway [1] of procedure
     * @param procedure
     * @return
     */
    public static String[] getParams(String procedure) {
        String ret[] = new String[2];
        // get approach mapped to CIFP database
        if(procedure.startsWith("RNAV-GPS-")) {
            ret[0] = "RNAV";
            procedure = procedure.replace("RNAV-GPS-", "");
        }
        else if(procedure.startsWith("RNAV-RNP-")) {
            ret[0] = "RNAV-RNP";
            procedure = procedure.replace("RNAV-RNP-", "");
        }
        else if(procedure.startsWith("ILS-OR-LOC-")) {
            ret[0] = "ILS";
            procedure = procedure.replace("ILS-OR-LOC-", "");
        }
        else if(procedure.startsWith("ILS-")) {
            ret[0] = "ILS";
            procedure = procedure.replace("ILS-", "");
        }
        else if(procedure.startsWith("LOC-BC-")) {
            ret[0] = "LOC/BC";
            procedure = procedure.replace("LOC-BC-", "");
        }
        else if(procedure.startsWith("LOC-")) {
            ret[0] = "LOC";
            procedure = procedure.replace("LOC-", "");
        }
        else if(procedure.startsWith("GPS-")) {
            ret[0] = "IGS";
            procedure = procedure.replace("GPS-", "");
        }
        else if(procedure.startsWith("NDB-")) {
            ret[0] = "NDB";
            procedure = procedure.replace("NDB-", "");
        }
        else if(procedure.startsWith("VOR-DME-")) {
            ret[0] = "VOR/DME";
            procedure = procedure.replace("VOR-DME-", "");
        }
        else if(procedure.startsWith("VOR-")) {
            ret[0] = "VOR";
            procedure = procedure.replace("VOR-", "");
        }

        String tokens[] = procedure.split("-");
        // Find RWY
        int len = tokens.length;
        for (int tkn = 0; tkn < tokens.length; tkn++) {
            if(tokens[tkn].equals("RWY")) {
                String runway = "";
                if((tkn + 1) < len) {
                    runway = tokens[tkn + 1];
                    if(tkn > 0) {
                        if(tokens[tkn - 1].matches("[A-Z]")) {
                            runway = runway + "-" + tokens[tkn - 1];
                        }
                    }
                }
                ret[1] = runway;
                break;
            }
        }

        return ret;
    }

    /**
     * @author zkhan
     *
     */
    private class CreateTask extends AsyncTask<Object, Void, Boolean> {



        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            Thread.currentThread().setName("CreateApproach");

            StorageService service = (StorageService)vals[1];
            Preferences pref = (Preferences)vals[2];

            service.newPlan();

            // Eliminate duplicates. Could be done with sorted sets
            LinkedList<String> set = new LinkedList<String>();
            String last = "";
            String wps[] = ((String)vals[0]).split(" ");
            for (String wp : wps) {
                if(last.equals(wp) || wp.equals("")) {
                    continue;
                }
                set.add(wp);
                last = wp;
            }

            /*
             * Here we guess types since we do not have user select
             */

            for(String wp : set) {

                if(wp.matches("RW\\d{2}.*")) {
                    String rw = wp.replace("RW", "");
                    Coordinate c = service.getDBResource().findRunwayCoordinates(rw, mAirport);
                    if(c != null) {
                        Destination d = DestinationFactory.build(service, Helper.getGpsAddress(c.getLongitude(), c.getLatitude()), Destination.GPS);
                        service.getPlan().appendDestination(d);
                    }
                }

	            /*
	             * Search from database. Make this a simple one off search
	             */
                StringPreference s = service.getDBResource().searchOne(wp);
                if(s != null) {
                    String found = s.getHashedName();
                    String id = StringPreference.parseHashedNameId(found);
                    String type = StringPreference.parseHashedNameDestType(found);
                    String dbtype = StringPreference.parseHashedNameDbType(found);

                    /*
                     * Add each
                     */
                    Destination d = DestinationFactory.build(service, id, type);
                    d.find(dbtype);
                    service.getPlan().appendDestination(d);
                }
            }
            return true;
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
        }
    }
}
