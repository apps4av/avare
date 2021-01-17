package com.ds.avare.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

/**
 * Created by zkhan on 3/10/17.
 */

public class UserProvider extends MainProvider {


    public static final int PLANS = 900;
    public static final int PLANS_ID = 901;
    public static final int LISTS = 902;
    public static final int LISTS_ID = 903;
    public static final int WNBS = 904;
    public static final int WNBS_ID = 905;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/rv-user";

    private static final UriMatcher mURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_PLAN, PLANS);
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_PLAN + "/#", PLANS_ID);
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_LIST, LISTS);
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_LIST + "/#", LISTS_ID);
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_WNB, WNBS);
        mURIMatcher.addURI(UserContract.AUTHORITY, UserContract.BASE_WNB + "/#", WNBS_ID);
    }

    @Override
    public String getType(Uri uri) {
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case PLANS:
                return CONTENT_TYPE;
            case LISTS:
                return CONTENT_TYPE;
            case WNBS:
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
            case PLANS:
                queryBuilder.setTables(UserContract.TABLE_PLAN);
                break;
            case LISTS:
                queryBuilder.setTables(UserContract.TABLE_LIST);
                break;
            case WNBS:
                queryBuilder.setTables(UserContract.TABLE_WNB);
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
    public Uri insert(Uri uri, ContentValues values) {

        String table = null;
        int uriType = mURIMatcher.match(uri);
        switch (uriType) {
            case PLANS:
                table = UserContract.TABLE_PLAN;
                break;
            case LISTS:
                table = UserContract.TABLE_LIST;
                break;
            case WNBS:
                table = UserContract.TABLE_WNB;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }

        try {
            SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
            long id = db.insert(table, null, values);
            if (id > 0) {
                switch (uriType) {
                    case PLANS:
                        return UserContract.buildPlansUri(id);
                    case LISTS:
                        return UserContract.buildListsUri(id);
                    case WNBS:
                        return UserContract.buildWnbsUri(id);
                    default:
                        throw new IllegalArgumentException("Unknown URI");
                }
            }
            else {
                throw new android.database.SQLException("Failed to insert row into: " + uri);
            }
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
        // user data goes in downloads
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + getContext().getPackageName();
        mDatabaseHelper = new UserDatabaseHelper(getContext(), path);
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
