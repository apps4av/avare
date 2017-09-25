package com.ds.avare.content;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ds.avare.storage.Preferences;

/**
 * Created by zkhan on 3/10/17.
 */

public class ObstaclesProvider extends ContentProvider {


    public static final int OBSTACLES = 300;
    public static final int OBSTACLES_ID = 301;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/rv-obs";

    private static final UriMatcher mURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(ObstaclesContract.AUTHORITY, ObstaclesContract.BASE, OBSTACLES);
        mURIMatcher.addURI(ObstaclesContract.AUTHORITY, ObstaclesContract.BASE + "/#", OBSTACLES_ID);
    }

    private ObstaclesDatabaseHelper mObstaclesDatabaseHelper;

    private Preferences mPref;

    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case OBSTACLES:
                return CONTENT_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        checkChange();

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(ObstaclesContract.TABLE);

        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case OBSTACLES:
                // no filter
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }


        Cursor cursor = queryBuilder.query(mObstaclesDatabaseHelper.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    private synchronized void checkChange() {
        // allow changing folder
        if (!mPref.mapsFolder().equals(mObstaclesDatabaseHelper.getFolder())) {
            mObstaclesDatabaseHelper.close();
            mObstaclesDatabaseHelper = new ObstaclesDatabaseHelper(getContext(), mPref.mapsFolder());
        }
    }


    @Override
    public boolean onCreate() {
        mPref = new Preferences(getContext());
        mObstaclesDatabaseHelper = new ObstaclesDatabaseHelper(getContext(), mPref.mapsFolder());
        return true;
    }

}
