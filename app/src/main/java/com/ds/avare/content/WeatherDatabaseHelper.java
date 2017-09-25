package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Created by zkhan on 3/13/17.
 */

public class WeatherDatabaseHelper extends MainDatabaseHelper {

    private static final String DBNAME = "weather.db";

    public WeatherDatabaseHelper(Context context, String folder) {
        super(context, folder, DBNAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}