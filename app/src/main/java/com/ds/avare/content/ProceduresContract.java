package com.ds.avare.content;

import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class ProceduresContract {

    public static final String AUTHORITY = "com.ds.avare.provider.procedures";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE = "procedures";

    public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, BASE);

    public static final String TABLE = "procedures";

    public static final String AIRPORT = "Airport";
    public static final String APPROACH_TYPE = "AppType";
    public static final String RUNWAY = "Runway";
    public static final String CHANGE_CYCLE = "ChangeCycle";
    public static final String INITIAL_COURSE = "InitialCourse";
    public static final String INITIAL_ALTITUDE = "InitialAltitude";
    public static final String FINAL_COURSE = "FinalCourse";
    public static final String FINAL_ALTITUDE = "FinalAltitude";
    public static final String MISSED_COURSE = "MissedCourse";
    public static final String MISSED_ALTITUDE = "MissedAltitude";

}
