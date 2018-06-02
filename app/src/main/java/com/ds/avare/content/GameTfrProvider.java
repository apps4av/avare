package com.ds.avare.content;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by zkhan on 3/10/17.
 */

public class GameTfrProvider extends MainProvider {


    public static final int GAMETFR = 600;
    public static final int GAMETFR_ID = 601;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/rv-gametfr";

    private static final UriMatcher mURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(GameTfrContract.AUTHORITY, GameTfrContract.BASE, GAMETFR);
        mURIMatcher.addURI(GameTfrContract.AUTHORITY, GameTfrContract.BASE + "/#", GAMETFR_ID);
    }


    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case GAMETFR:
                return CONTENT_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(GameTfrContract.TABLE);

        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case GAMETFR:
                // no filter
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
        mDatabaseHelper = new GameTfrDatabaseHelper(getContext(), mPref.mapsFolder());
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
