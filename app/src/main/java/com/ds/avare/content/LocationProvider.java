package com.ds.avare.content;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by zkhan on 3/10/17.
 */

public class LocationProvider extends MainProvider {


    public static final int AIRPORTS = 200;
    public static final int AIRPORTS_ID = 220;
    public static final int AIRPORT_DIAGS = 201;
    public static final int AIRPORT_DIAGS_ID = 221;
    public static final int AIRPORT_FREQ = 202;
    public static final int AIRPORT_FREQ_ID = 222;
    public static final int AIRPORT_AWOS = 203;
    public static final int AIRPORT_AWOS_ID = 223;
    public static final int AIRPORT_RUNWAYS = 204;
    public static final int AIRPORT_RUNWAYS_ID = 224;
    public static final int FIX = 205;
    public static final int FIX_ID = 225;
    public static final int NAV = 206;
    public static final int NAV_ID = 226;
    public static final int TAKEOFF = 207;
    public static final int TAKEOFF_ID = 227;
    public static final int ALTERNATE = 208;
    public static final int ALTERNATE_ID = 228;
    public static final int AFD = 209;
    public static final int AFD_ID = 229;
    public static final int SUA = 210;
    public static final int SUA_ID = 230;
    public static final int AIRWAYS = 211;
    public static final int AIRWAYS_ID = 231;
    public static final int NEAR = 212;
    public static final int NEAR_ID = 232;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/rv-location";

    private static final UriMatcher mURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORTS, AIRPORTS);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORTS + "/#", AIRPORTS_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_DIAGS, AIRPORT_DIAGS);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_DIAGS + "/#", AIRPORT_DIAGS_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_FREQ, AIRPORT_FREQ);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_FREQ + "/#", AIRPORT_FREQ_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_AWOS, AIRPORT_AWOS);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_AWOS + "/#", AIRPORT_AWOS_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_RUNWAYS, AIRPORT_RUNWAYS);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRPORT_RUNWAYS + "/#", AIRPORT_RUNWAYS_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_FIX, FIX);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_FIX + "/#", FIX_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_NAV, NAV);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_NAV + "/#", NAV_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_TAKEOFF, TAKEOFF);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_TAKEOFF + "/#", TAKEOFF_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_ALTERNATE, ALTERNATE);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_ALTERNATE + "/#", ALTERNATE_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AFD, AFD);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AFD + "/#", AFD_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_SUA, SUA);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_SUA + "/#", SUA_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRWAYS, AIRWAYS);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_AIRWAYS + "/#", AIRWAYS_ID);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_NEAR, NEAR);
        mURIMatcher.addURI(LocationContract.AUTHORITY, LocationContract.BASE_NEAR + "/#", NEAR_ID);
    }

    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case AIRPORTS:
            case AIRPORT_DIAGS:
            case AIRPORT_FREQ:
            case AIRPORT_AWOS:
            case AIRPORT_RUNWAYS:
            case FIX:
            case NAV:
            case TAKEOFF:
            case ALTERNATE:
            case AFD:
            case SUA:
            case AIRWAYS:
                return CONTENT_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case AIRPORTS:
                queryBuilder.setTables(LocationContract.TABLE_AIRPORTS);
                break;
            case AIRPORT_DIAGS:
                queryBuilder.setTables(LocationContract.TABLE_AIRPORT_DIAGS);
                break;
            case AIRPORT_FREQ:
                queryBuilder.setTables(LocationContract.TABLE_AIRPORT_FREQ);
                break;
            case AIRPORT_AWOS:
                queryBuilder.setTables(LocationContract.TABLE_AIRPORT_AWOS);
                break;
            case AIRPORT_RUNWAYS:
                queryBuilder.setTables(LocationContract.TABLE_AIRPORT_RUNWAYS);
                break;
            case FIX:
                queryBuilder.setTables(LocationContract.TABLE_FIX);
                break;
            case NAV:
                queryBuilder.setTables(LocationContract.TABLE_NAV);
                break;
            case TAKEOFF:
                queryBuilder.setTables(LocationContract.TABLE_TAKEOFF);
                break;
            case ALTERNATE:
                queryBuilder.setTables(LocationContract.TABLE_ALTERNATE);
                break;
            case AFD:
                queryBuilder.setTables(LocationContract.TABLE_AFD);
                break;
            case SUA:
                queryBuilder.setTables(LocationContract.TABLE_SUA);
                break;
            case AIRWAYS:
                queryBuilder.setTables(LocationContract.TABLE_AIRWAYS);
                break;
            case NEAR:
                // join two tables for near airports
                queryBuilder.setTables(LocationContract.TABLE_AIRPORTS + " LEFT JOIN " +
                        LocationContract.TABLE_AIRPORT_RUNWAYS + " ON " +
                        LocationContract.TABLE_AIRPORTS + "." + LocationContract.AIRPORTS_LOCATION_ID +
                        " = " + LocationContract.TABLE_AIRPORT_RUNWAYS + "." + LocationContract.AIRPORT_RUNWAYS_LOCATION_ID);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }

        try {
            Cursor cursor = queryBuilder.query(mDatabaseHelper.getReadableDatabase(),
                    projection, selection, selectionArgs, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        }
        catch (Exception e) {
            // Something wrong, missing or deleted database from download
            resetDatabase();
        }
        return null;
    }


    @Override
    public boolean onCreate() {
        super.onCreate();
        mDatabaseHelper = new LocationDatabaseHelper(getContext(), mPref.mapsFolder());
        return true;
    }

    /**
     * Sync database on folder change, deleted database, new database, and other conditions
     */
    public void resetDatabase() {
        if(mDatabaseHelper != null) {
            mDatabaseHelper.close();
        }
        onCreate();
    }
}
