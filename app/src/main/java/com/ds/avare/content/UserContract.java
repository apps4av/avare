package com.ds.avare.content;

import android.content.ContentUris;
import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class UserContract {

    public static final String AUTHORITY = "com.ds.avare.provider.user";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE_PLAN = "plan";
    public static final Uri CONTENT_URI_PLAN = Uri.withAppendedPath(AUTHORITY_URI, BASE_PLAN);
    public static final String TABLE_PLAN = "plans";
    public static final String PLAN_COLUMN_ID = "name";
    public static final String PLAN_COLUMN_PATH = "path";

    public static Uri buildPlansUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_PLAN, id);
    }

    public static final String BASE_LIST = "list";
    public static final Uri CONTENT_URI_LIST = Uri.withAppendedPath(AUTHORITY_URI, BASE_LIST);
    public static final String TABLE_LIST = "lists";
    public static final String LIST_COLUMN_ID = "name";
    public static final String LIST_COLUMN_TEXT = "text";

    public static Uri buildListsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_LIST, id);
    }

    public static final String BASE_WNB = "wnb";
    public static final Uri CONTENT_URI_WNB = Uri.withAppendedPath(AUTHORITY_URI, BASE_WNB);
    public static final String TABLE_WNB = "wnbs";
    public static final String WNB_COLUMN_ID = "name";
    public static final String WNB_COLUMN_TEXT = "text";

    public static Uri buildWnbsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_WNB, id);
    }

    public static final String BASE_RECENT = "recent";
    public static final Uri CONTENT_URI_RECENT = Uri.withAppendedPath(AUTHORITY_URI, BASE_RECENT);
    public static final String TABLE_RECENT = "recents";
    public static final String RECENT_COLUMN_ID = "id";
    public static final String RECENT_COLUMN_WID = "wid";
    public static final String RECENT_COLUMN_DESTTYPE = "desttype";
    public static final String RECENT_COLUMN_DBTYPE = "dbtype";
    public static final String RECENT_COLUMN_NAME = "name";

    public static Uri buildRecentsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_RECENT, id);
    }

    public static final String BASE_TAG = "tag";
    public static final Uri CONTENT_URI_TAG = Uri.withAppendedPath(AUTHORITY_URI, BASE_TAG);
    public static final String TABLE_TAG = "tags";
    public static final String TAG_COLUMN_ID = "name";
    public static final String TAG_COLUMN_TEXT = "text";

    public static Uri buildTagsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_TAG, id);
    }

    public static final String BASE_DRAW = "draw";
    public static final Uri CONTENT_URI_DRAW = Uri.withAppendedPath(AUTHORITY_URI, BASE_DRAW);
    public static final String TABLE_DRAW = "draws";
    public static final String DRAW_COLUMN_ID = "id";
    public static final String DRAW_COLUMN_POINTS_X = "x";
    public static final String DRAW_COLUMN_POINTS_Y = "y";
    public static final String DRAW_COLUMN_SEP = "separate";

    public static Uri buildDrawsUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_DRAW, id);
    }

    public static final String BASE_AIRCRAFT = "ac";
    public static final Uri CONTENT_URI_AIRCRAFT = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRCRAFT);
    public static final String TABLE_AIRCRAFT = "aircraft";
    public static final String AIRCRAFT_COLUMN_ID = "id";
    public static final String AIRCRAFT_COLUMN_TYPE = "type";
    public static final String AIRCRAFT_COLUMN_WAKE = "wake";
    public static final String AIRCRAFT_COLUMN_EQUIPMENT = "equipment";
    public static final String AIRCRAFT_COLUMN_ICAO = "icao";
    public static final String AIRCRAFT_COLUMN_CRUISE_TAS = "cruise_tas";
    public static final String AIRCRAFT_COLUMN_SURVEILLANCE = "surveillance";
    public static final String AIRCRAFT_COLUMN_FUEL_ENDURANCE = "endurance";
    public static final String AIRCRAFT_COLUMN_COLOR = "color";
    public static final String AIRCRAFT_COLUMN_PIC = "pic";
    public static final String AIRCRAFT_COLUMN_PILOT = "pilot";
    public static final String AIRCRAFT_COLUMN_SINK_RATE = "sink_rate";
    public static final String AIRCRAFT_COLUMN_FUEL_BURN = "fuel_burn";
    public static final String AIRCRAFT_COLUMN_BASE = "base";

    public static Uri buildAircraftUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_AIRCRAFT, id);
    }

}
