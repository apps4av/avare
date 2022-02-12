package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by zkhan on 3/13/17.
 */

public class UserDatabaseHelper extends MainDatabaseHelper {

    private static final String DBNAME = "user.db";
    private static final int DB_VERSION = 2;

    public UserDatabaseHelper(Context context, String folder) {
        super(context, folder, DBNAME, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
                    "CREATE TABLE " + UserContract.TABLE_PLAN + " (" +
                            UserContract.PLAN_COLUMN_ID + " TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE, " +
                            UserContract.PLAN_COLUMN_PATH + " TEXT NOT NULL);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + UserContract.TABLE_LIST + " (" +
                            UserContract.LIST_COLUMN_ID + " TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE, " +
                            UserContract.LIST_COLUMN_TEXT + " TEXT NOT NULL);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + UserContract.TABLE_WNB + " (" +
                            UserContract.WNB_COLUMN_ID + " TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE, " +
                            UserContract.WNB_COLUMN_TEXT + " TEXT NOT NULL);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + UserContract.TABLE_RECENT + " (" +
                            UserContract.RECENT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            UserContract.RECENT_COLUMN_WID + " TEXT UNIQUE ON CONFLICT REPLACE, " +
                            UserContract.RECENT_COLUMN_DESTTYPE + " TEXT, " +
                            UserContract.RECENT_COLUMN_DBTYPE + " TEXT, " +
                            UserContract.RECENT_COLUMN_NAME + " TEXT);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + UserContract.TABLE_TAG + " (" +
                            UserContract.TAG_COLUMN_ID + " TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE, " +
                            UserContract.TAG_COLUMN_TEXT + " TEXT NOT NULL);");
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}