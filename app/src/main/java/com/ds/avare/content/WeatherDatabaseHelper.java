package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by zkhan on 3/13/17.
 */

public class WeatherDatabaseHelper extends MainDatabaseHelper {

    private static final String DBNAME = "weather.db";
    private static final int DB_VERSION = 1;

    public WeatherDatabaseHelper(Context context, String folder) {
        super(context, folder, DBNAME, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}