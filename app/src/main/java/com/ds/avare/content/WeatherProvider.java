package com.ds.avare.content;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ds.avare.storage.Preferences;

/**
 * Created by zkhan on 3/10/17.
 */

public class WeatherProvider extends MainProvider {


    public static final int AIRMET = 500;
    public static final int AIRMET_ID = 501;
    public static final int PIREP = 510;
    public static final int PIREP_ID = 511;
    public static final int METAR = 520;
    public static final int METAR_ID = 521;
    public static final int TAF = 530;
    public static final int TAF_ID = 531;
    public static final int WIND = 540;
    public static final int WIND_ID = 541;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/rv-weather";

    private static final UriMatcher mURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_AIRMET, AIRMET);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_AIRMET + "/#", AIRMET_ID);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_PIREP, PIREP);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_PIREP + "/#", PIREP_ID);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_METAR, METAR);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_METAR + "/#", METAR_ID);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_TAF, TAF);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_TAF + "/#", TAF_ID);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_WIND, WIND);
        mURIMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.BASE_WIND + "/#", WIND_ID);
    }

    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case AIRMET:
            case PIREP:
            case METAR:
            case TAF:
            case WIND:
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
            case AIRMET:
                queryBuilder.setTables(WeatherContract.TABLE_AIRMET);
                break;
            case PIREP:
                queryBuilder.setTables(WeatherContract.TABLE_PIREP);
                break;
            case METAR:
                queryBuilder.setTables(WeatherContract.TABLE_METAR);
                break;
            case TAF:
                queryBuilder.setTables(WeatherContract.TABLE_TAF);
                break;
            case WIND:
                queryBuilder.setTables(WeatherContract.TABLE_WIND);
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
        mDatabaseHelper = new WeatherDatabaseHelper(getContext(), mPref.mapsFolder());
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
