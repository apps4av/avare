/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.DataBaseHelper;
import com.ds.avare.storage.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Manage DME display on the IAP plates.
 */
public class PlateDME {
    private final static String TAG = "WairToNow";

    private final static String dbname = "dmecheckboxes.db";

    private final static int DMECB_DIST = 1;
    private final static int DMECB_TIME = 2;
    private final static int DMECB_RADL = 4;

    private final static float FtPerM = 3.28084F;
    private final static float MPerNM = 1852.0F;

    private final static Object dbLock = new Object ();

    private boolean  dmeShowing;
    private Button   button;
    private Context  context;
    private DataBaseHelper dbh;
    private float    gpslat;
    private float    gpslon;
    private float    gpsalt;
    private float    originalTextSize;
    private long     gpstime;
    private String   dbpath;
    private String   faaid;
    private String   originalString;
    private String   plateid;
    private TreeMap<String,DMECheckboxes> dmeCheckboxeses = new TreeMap<> ();
    private Typeface originalTypeface;

    /**
     * Constructor
     */
    public PlateDME (Context ctx, Button but)
    {
        context = ctx;
        button  = but;

        originalString   = but.getText ().toString ();
        originalTextSize = but.getTextSize ();
        originalTypeface = but.getTypeface ();

        dbh = new DataBaseHelper (ctx);
        dbpath = new Preferences (ctx).mapsFolder() + "/" + dbname;

        button.setOnClickListener (new View.OnClickListener () {
            @Override
            public void onClick (View view)
            {
                DMEButtonClicked (true);
            }
        });

        button.setOnLongClickListener (new View.OnLongClickListener () {
            @Override
            public boolean onLongClick (View view)
            {
                DMEButtonClicked (false);
                return false;
            }
        });
    }

    /**
     * Select which plate we are dealing with.
     * Each plate has its own DME table entries.
     */
    public void Select (String fi, String pi)
    {
        faaid   = fi;  // eg, "BVY"
        plateid = pi;  // eg, "LOC-RWY-16" or "" if non-DMEable like airport diagram
        //Log.d ("Avare", "PlateDME.Select*: faaid=<" + faaid + "> plateid=<" + plateid + ">");

        // see what DMEs the user has enabled in the past for this plate
        FillDMECheckboxes ();

        // set button state to match what was just read from the database
        UpdateDisplay ();
    }

    /**
     * Read local database to see what DMEs the user has enabled in the past.
     */
    private void FillDMECheckboxes ()
    {
        dmeCheckboxeses.clear ();
        dmeShowing = false;
        try {
            if (new File (dbpath).exists ()) {
                SQLiteDatabase sqldb = SQLiteDatabase.openDatabase (dbpath, null, SQLiteDatabase.OPEN_READONLY |
                        SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor result = sqldb.query ("dmecheckboxes",
                        new String[] { "dc_dmeid", "dc_checked" },
                        "dc_faaid='" + faaid + "' AND dc_plate='" + plateid + "'",
                        null, null, null, null, null);
                try {
                    if (result.moveToFirst ()) {
                        do {
                            String id = result.getString (0);
                            Waypoint wp = FindWaypoint (id);
                            if (wp != null) {
                                int checked = result.getInt (1);
                                dmeShowing |= checked != 0;
                                DMECheckboxes dmecb = new DMECheckboxes (wp);
                                dmecb.setChecked (checked);
                                dmeCheckboxeses.put (wp.ident, dmecb);
                            }
                        } while (result.moveToNext ());
                    }
                } finally {
                    result.close ();
                }
            }
        } catch (Exception e) {
            Log.e (TAG, "error reading " + dbpath, e);
        }
    }

    /**
     * Incoming GPS update, re-draw DME strings on button.
     * If no DME strings enabled, draw "DME" on button.
     */
    public void updateParams (GpsParams params)
    {
        gpslat  = (float) params.getLatitude ();
        gpslon  = (float) params.getLongitude ();
        gpsalt  = (float) params.getAltitude ();
        gpstime = params.getTime ();
        if (dmeShowing) UpdateDisplay ();
    }

    /**
     * Set button text according to latest GPS data and checkboxes.
     */
    private void UpdateDisplay ()
    {
        // see if any checkboxes check (or maybe whole thing is disabled)
        int allChecked = 0;
        if (dmeShowing) {
            for (DMECheckboxes dmecb : dmeCheckboxeses.values ()) {
                int checked = dmecb.getChecked ();
                allChecked |= checked;
            }
        }

        if (allChecked == 0) {

            // user doesn't want any DMEs shown, draw a little button instead
            button.setText (originalString);
            button.setTypeface (originalTypeface);
        } else {
            StringBuilder sb = new StringBuilder ();

            // generate DME strings
            int longestLine = 0;
            for (String dmeIdent : dmeCheckboxeses.keySet ()) {
                DMECheckboxes dmecb = dmeCheckboxeses.get (dmeIdent);
                int checked = dmecb.getChecked ();
                if (checked != 0) {
                    Waypoint wp = dmecb.waypoint;
                    int len0 = sb.length ();
                    if (len0 > 0) {
                        sb.append ('\n');
                        len0 ++;
                    }
                    sb.append (dmeIdent);
                    while (sb.length () - len0 < 5) sb.append (' ');

                    // distance (in nautical miles) to DME station
                    float distBin = LatLonDist (gpslat, gpslon, wp.lat, wp.lon);
                    if (wp.elev != Waypoint.ELEV_UNKNOWN) {
                        distBin = (float) Math.hypot (distBin, (gpsalt - wp.elev / FtPerM) / MPerNM);
                    }
                    if ((checked & DMECB_DIST) != 0) {
                        int dist10Bin = (int) (distBin * 10 + 0.5);
                        int len = sb.length ();
                        if (dist10Bin > 9999) {
                            sb.append ("--.-");
                        } else {
                            sb.append (dist10Bin / 10);
                            sb.append ('.');
                            sb.append (dist10Bin % 10);
                        }
                        while (sb.length () - len < 5) sb.insert (len, ' ');
                    }

                    // compute nautical miles per second we are closing in on the station
                    if (dmecb.dmeLastTime != 0) {
                        long dtms = gpstime - dmecb.dmeLastTime;
                        if (dtms > 0) dmecb.dmeLastSpeed = (dmecb.dmeLastDist - distBin) * 1000.0F / dtms;
                    } else {
                        dmecb.dmeLastSpeed = 0.0F;
                    }

                    // save the latest position
                    dmecb.dmeLastDist = distBin;
                    dmecb.dmeLastTime = gpstime;

                    // see if user wants time-to-station displayed
                    if ((checked & DMECB_TIME) != 0) {
                        int len1 = sb.length ();

                        // make sure heading is valid and that the divide below won't overflow
                        if (distBin <= Math.abs (dmecb.dmeLastSpeed) * 60000.0) {

                            // how many seconds TO the DME antenna
                            // negative means it is behind us
                            int seconds = (int) (distBin / dmecb.dmeLastSpeed + 0.5);

                            // we do from -99:59 to 99:59
                            if ((seconds > -6000) && (seconds < 6000)) {
                                if (seconds < 0) {
                                    sb.append ('-');
                                    seconds = - seconds;
                                }
                                sb.append (seconds / 60);
                                sb.append (':');
                                int len2 = sb.length ();
                                sb.append (seconds % 60);
                                while (sb.length () - len2 < 2) sb.insert (len2, '0');
                            }
                        }
                        if (sb.length () == len1) sb.append ("--:--");
                        while (sb.length () - len1 < 6) sb.insert (len1, ' ');
                    }

                    // get what magnetic radial we are on from the navaid (should match what's on an IAP plate)
                    if ((checked & DMECB_RADL) != 0) {
                        float hdgDeg = LatLonTC (wp.lat, wp.lon, gpslat, gpslon);
                        int hdgMag = ((int) (hdgDeg + wp.magvar + 0.5F) + 359) % 360 + 1;
                        int len = sb.length ();
                        sb.append (hdgMag);
                        sb.append ((char) 0x00B0);
                        while (sb.length () - len < 5) sb.insert (len, ' ');
                    }

                    int lineLen = sb.length () - len0;
                    if (longestLine < lineLen) longestLine = lineLen;
                }
            }

            // make all lines just as long as the longest line
            // so they show up nice
            int j = sb.length ();
            for (int i = j; i >= 0; -- i) {
                char c = (i > 0) ? sb.charAt (i - 1) : '\n';
                if (c == '\n') {
                    while (j - i < longestLine) {
                        sb.insert (j ++, ' ');
                    }
                    j = i - 1;
                }
            }

            // display on button
            button.setText (sb.toString ());
            button.setTypeface (Typeface.MONOSPACE);
        }
    }

    /**
     * Lower left corner of plate view was clicked and so we bring up the DME selection alert.
     */
    private void DMEButtonClicked (boolean shortClick)
    {
        // if there are some dme fixes enabled, short click will show/hide them
        if (shortClick) {
            if (dmeShowing) {
                dmeShowing = false;
                UpdateDisplay ();
                return;
            }
            for (DMECheckboxes dmecb : dmeCheckboxeses.values ()) {
                if (dmecb.getChecked () != 0) {
                    dmeShowing = true;
                    UpdateDisplay ();
                    return;
                }
            }
        }

        // long click or short click with nothing enabled brings up menu

        // build list of existing DME waypoints
        LinearLayout llv = new LinearLayout (context);
        llv.setOrientation (LinearLayout.VERTICAL);
        for (DMECheckboxes dmecb : dmeCheckboxeses.values ()) {
            dmecb.dmeWasChecked = dmecb.getChecked ();
            ViewParent parent = dmecb.getParent ();
            if (parent != null) ((ViewGroup) parent).removeAllViews ();
            llv.addView (dmecb);
        }

        // add an entry box so user can select some other waypoint
        final DMECheckboxes dmeTextEntry = new DMECheckboxes (null);
        llv.addView (dmeTextEntry);

        // put it in a dialog box
        ScrollView sv = new ScrollView (context);
        sv.addView (llv);
        AlertDialog.Builder adb = new AlertDialog.Builder (context);
        adb.setTitle ("- D - T - R - Waypoint");
        adb.setView (sv);

        // if user clicks OK, save changes to database
        adb.setPositiveButton ("OK", new DialogInterface.OnClickListener () {
            @Override
            public void onClick (DialogInterface dialogInterface, int i)
            {
                dmeShowing = false;

                try {
                    SQLiteDatabase sqldb;
                    synchronized (dbLock) {
                        if (!new File (dbpath).exists ()) {
                            sqldb = SQLiteDatabase.openDatabase (dbpath + ".tmp", null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                            try {
                                sqldb.execSQL ("CREATE TABLE dmecheckboxes (dc_faaid TEXT NOT NULL, dc_plate TEXT NOT NULL, " +
                                        "dc_dmeid TEXT NOT NULL, dc_checked INTEGER NOT NULL)");
                                sqldb.execSQL ("CREATE INDEX dmecbs_idplate ON dmecheckboxes (dc_faaid,dc_plate)");
                                sqldb.execSQL ("CREATE UNIQUE INDEX dmecbs_idplateid ON dmecheckboxes (dc_faaid,dc_plate,dc_dmeid)");
                            } finally {
                                sqldb.close ();
                            }
                            if (!new File (dbpath + ".tmp").renameTo (new File (dbpath))) {
                                throw new IOException ("error renaming " + dbpath + ".tmp to " + dbpath);
                            }
                        }
                        sqldb = SQLiteDatabase.openDatabase (dbpath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    }
                    try {

                        // if a new ident was typed in the box, add it to the checkbox list as checked
                        String ident = dmeTextEntry.identtv.getText ().toString ().replace (" ", "").toUpperCase (Locale.US);
                        if (!ident.equals ("")) {
                            Waypoint wp = FindWaypoint (ident);
                            if (wp != null) {
                                DMECheckboxes dmecb = new DMECheckboxes (wp);
                                int checked = dmeTextEntry.getChecked ();
                                if (checked == 0) checked = DMECB_DIST;
                                dmecb.setChecked (checked);
                                dmeCheckboxeses.put (wp.ident, dmecb);

                                ContentValues values = new ContentValues (4);
                                values.put ("dc_faaid", faaid);
                                values.put ("dc_plate", plateid);
                                values.put ("dc_dmeid", wp.ident);
                                values.put ("dc_checked", checked);
                                sqldb.insertWithOnConflict ("dmecheckboxes", null, values, SQLiteDatabase.CONFLICT_REPLACE);

                                dmeShowing = true;
                            }
                        }

                        // save the checkbox state of the existing checkboxes to the database
                        // also enable showing DMEs if any are enabled
                        SQLiteStatement updateStmt = sqldb.compileStatement ("UPDATE dmecheckboxes SET dc_checked=? WHERE dc_faaid=? AND dc_plate=? AND dc_dmeid=?");
                        for (DMECheckboxes dmecb : dmeCheckboxeses.values ()) {
                            Waypoint wp = dmecb.waypoint;
                            dmecb.dmeLastTime = 0;
                            int checked = dmecb.getChecked ();
                            dmeShowing |= checked != 0;
                            if (checked != dmecb.dmeWasChecked) {
                                updateStmt.clearBindings ();
                                updateStmt.bindLong (1, checked);
                                updateStmt.bindString (2, faaid);
                                updateStmt.bindString (3, plateid);
                                updateStmt.bindString (4, wp.ident);
                                updateStmt.execute ();
                            }
                        }
                    } finally {
                        sqldb.close ();
                    }
                } catch (Exception e) {
                    Log.e (TAG, "error writing " + dbname, e);
                }

                UpdateDisplay ();
            }
        });

        // if user clicks Cancel, reload button states and dismiss dialog
        adb.setNegativeButton ("Cancel", new DialogInterface.OnClickListener () {
            @Override
            public void onClick (DialogInterface dialogInterface, int i)
            {
                for (DMECheckboxes dmecb : dmeCheckboxeses.values ()) {
                    dmecb.dmeLastTime = 0;
                    dmecb.setChecked (dmecb.dmeWasChecked);
                }
            }
        });

        // display dialog box
        adb.show ();
    }

    /**
     * Get true course (degrees) from point 1 to point 2.
     */
    private static float LatLonTC (float lat1, float lon1, float lat2, float lon2)
    {
        return (float) Projection.getStaticBearing (lon1, lat1, lon2, lat2);
    }

    /**
     * Get distance (always nm cuz DME's are always nm) between the two points.
     */
    private static float LatLonDist (float lat1, float lon1, float lat2, float lon2)
    {
        lon1 = (float) Math.toRadians(lon1);
        lon2 = (float) Math.toRadians(lon2);
        lat1 = (float) Math.toRadians(lat1);
        lat2 = (float) Math.toRadians(lat2);

        //http://www.movable-type.co.uk/scripts/latlong.html
        double dLon = lon2 - lon1;
        double dLat = lat2 - lat1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (c * 3440.069);
    }

    /**
     * Find the lat/lon of a waypoint given its identifier as typed by the user.
     */
    private static class Waypoint {
        public final static float ELEV_UNKNOWN = -65536.0F;

        public String ident;
        public float  elev;    // feet
        public float  lat;
        public float  lon;
        public float  magvar;  // magnetic = true + magvar
                               // eg, BVY magvar = +15

        public Waypoint (LinkedHashMap<String,String> params)
        {
            ident = params.get (DataBaseHelper.LOCATION_ID);
            elev  = params.containsKey (DataBaseHelper.ELEVATION) ?
                        Float.parseFloat (params.get (DataBaseHelper.ELEVATION)) : ELEV_UNKNOWN;
            lat   = Float.parseFloat (params.get (DataBaseHelper.LATITUDE));
            lon   = Float.parseFloat (params.get (DataBaseHelper.LONGITUDE));
            if (params.containsKey (DataBaseHelper.MAGNETIC_VARIATION)) {
                magvar = Float.parseFloat (params.get (DataBaseHelper.MAGNETIC_VARIATION));
            } else {
                setupMagVar ();
            }
        }

        public Waypoint (Runway rwy)
        {
            ident = "RW" + rwy.getNumber ();
            elev  = Float.parseFloat (rwy.getElevation ());
            lat   = (float) rwy.getLatitude ();
            lon   = (float) rwy.getLongitude ();
            setupMagVar ();
        }

        private void setupMagVar ()
        {
            GeomagneticField gmf = new GeomagneticField (lat, lon, elev, System.currentTimeMillis());
            magvar = -gmf.getDeclination();
        }
    }

    private Waypoint FindWaypoint (String ident)
    {
        ident = ident.toUpperCase (Locale.US);

        LinkedHashMap<String, String> params = new LinkedHashMap<> ();

        // first try navaids as it is the common case
        Log.d ("Avare", "PlateDME.FindWaypoint*: navaid <" + ident + ">");
        dbh.findDestination (ident, Destination.NAVAID, null, params, null, null, null);
        if (params.size () > 0) return new Waypoint (params);

        // try intersection name
        Log.d ("Avare", "PlateDME.FindWaypoint*: intersection <" + ident + ">");
        dbh.findDestination (ident, Destination.FIX, null, params, null, null, null);
        if (params.size () > 0) return new Waypoint (params);

        // try airport
        Log.d ("Avare", "FindWaypoint*: airport <" + ident + ">");
        dbh.findDestination (ident, Destination.BASE, null, params, null, null, null);
        if (params.size () > 0) return new Waypoint (params);

        // maybe its a runway of the current airport
        if (ident.startsWith ("RW")) {
            Log.d ("Avare", "PlateDME.FindWaypoint*: runway <" + ident + "> at <" + faaid + ">");
            LinkedList<Runway> runways = new LinkedList<> ();
            dbh.findDestination (faaid, Destination.BASE, null, params, runways, null, null);
            String rwyno = ident.substring (2);
            if (rwyno.startsWith ("0")) rwyno = rwyno.substring (1);
            for (Runway rwy : runways) {
                if (rwy.getNumber ().equals (rwyno)) {
                    return new Waypoint (rwy);
                }
            }
        }

        // maybe it is a localizer without the '-' after the 'I'
        if (ident.startsWith ("I") && !ident.startsWith ("I-")) {
            ident = "I-" + ident.substring (2);
            Log.d ("Avare", "PlateDME.FindWaypoint*: localizer <" + ident + ">");
            dbh.findDestination (ident, Destination.NAVAID, null, params, null, null, null);
            if (params.size () > 0) return new Waypoint (params);
        }

        Log.d ("Avare", "PlateDME.FindWaypoint*: not found <" + ident + ">");
        return null;
    }

    /**
     * A row of checkboxes and ident text for DME display.
     */
    private class DMECheckboxes extends LinearLayout {
        public CheckBox cbdist;     // distance enabling checkbox
        public CheckBox cbtime;     // time enabling checkbox
        public CheckBox cbradl;     // radial enabling checkbox
        public TextView identtv;    // waypoint ident
        public Waypoint waypoint;   // corresponding waypoint or null for input box

        public int   dmeWasChecked;
        public float dmeLastDist;
        public float dmeLastSpeed;
        public long  dmeLastTime;

        public DMECheckboxes (Waypoint wp)
        {
            super (context);

            waypoint = wp;

            cbdist = new CheckBox (context);
            cbtime = new CheckBox (context);
            cbradl = new CheckBox (context);

            if (waypoint != null) {
                identtv = new TextView (context);
                identtv.setText (waypoint.ident);
                identtv.setTextColor (Color.WHITE);
                identtv.setTextSize (TypedValue.COMPLEX_UNIT_PX, originalTextSize);
                identtv.setTypeface (originalTypeface);
            } else {
                EditText dmeTextEntry = new EditText (context);
                dmeTextEntry.setEms (5);
                dmeTextEntry.setInputType (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                dmeTextEntry.setSingleLine ();
                identtv = dmeTextEntry;
            }

            setOrientation (HORIZONTAL);
            addView (cbdist);
            addView (cbtime);
            addView (cbradl);
            addView (identtv);
        }

        public int getChecked ()
        {
            int checked = 0;
            if (cbdist.isChecked ()) checked |= DMECB_DIST;
            if (cbtime.isChecked ()) checked |= DMECB_TIME;
            if (cbradl.isChecked ()) checked |= DMECB_RADL;
            return checked;
        }

        public void setChecked (int checked)
        {
            cbdist.setChecked ((checked & DMECB_DIST) != 0);
            cbtime.setChecked ((checked & DMECB_TIME) != 0);
            cbradl.setChecked ((checked & DMECB_RADL) != 0);
        }
    }
}
