package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.ds.avare.utils.WeatherHelper;

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

        try {
            db.execSQL(
                    "CREATE TABLE " + WeatherContract.TABLE_AIRMET + " (" +
                            WeatherContract.AIRMET_TEXT + " TEXT, " +
                            WeatherContract.AIRMET_TIME_FROM + " TEXT, " +
                            WeatherContract.AIRMET_TIME_TO + " TEXT, " +
                            WeatherContract.AIRMET_POINTS + " TEXT, " +
                            WeatherContract.AIRMET_MSL_MIN + " TEXT, " +
                            WeatherContract.AIRMET_MSL_MAX + " TEXT, " +
                            WeatherContract.AIRMET_MOVEMENT_DIRECTION + " TEXT, " +
                            WeatherContract.AIRMET_MOVEMENT_SPEED + " TEXT, " +
                            WeatherContract.AIRMET_HAZARD + " TEXT, " +
                            WeatherContract.AIRMET_SEVERITY + " TEXT, " +
                            WeatherContract.AIRMET_TYPE + " TEXT);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + WeatherContract.TABLE_PIREP + " (" +
                            WeatherContract.PIREP_TEXT + " TEXT, " +
                            WeatherContract.PIREP_TIME + " TEXT, " +
                            WeatherContract.PIREP_LONGITUDE + " FLOAT, " +
                            WeatherContract.PIREP_LATITUDE + " FLOAT, " +
                            WeatherContract.PIREP_TYPE + " TEXT);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + WeatherContract.TABLE_TAF + " (" +
                            WeatherContract.TAF_TEXT + " TEXT, " +
                            WeatherContract.TAF_TIME + " TEXT, " +
                            WeatherContract.TAF_STATION + " TEXT UNIQUE ON CONFLICT REPLACE);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + WeatherContract.TABLE_METAR + " (" +
                            WeatherContract.METAR_TEXT + " TEXT, " +
                            WeatherContract.METAR_TIME + " TEXT, " +
                            WeatherContract.METAR_STATION + " TEXT UNIQUE ON CONFLICT REPLACE, " +
                            WeatherContract.METAR_FLIGHT_CATEGORY + " TEXT, " +
                            WeatherContract.METAR_LONGITUDE + " FLOAT, " +
                            WeatherContract.METAR_LATITUDE + " FLOAT);");
        }
        catch (Exception e) {
        }

        try {
            db.execSQL(
                    "CREATE TABLE " + WeatherContract.TABLE_WIND + " (" +
                            WeatherContract.WIND_STATION + " TEXT, " +
                            WeatherContract.WIND_TIME + " TEXT, " +
                            WeatherContract.WIND_LONGITUDE + " FLOAT, " +
                            WeatherContract.WIND_LATITUDE + " FLOAT, " +
                            WeatherContract.WIND_3K + " TEXT, " +
                            WeatherContract.WIND_6K + " TEXT, " +
                            WeatherContract.WIND_9K + " TEXT, " +
                            WeatherContract.WIND_12K + " TEXT, " +
                            WeatherContract.WIND_18K + " TEXT, " +
                            WeatherContract.WIND_24K + " TEXT, " +
                            WeatherContract.WIND_30K + " TEXT, " +
                            WeatherContract.WIND_34K + " TEXT, " +
                            WeatherContract.WIND_39K + " TEXT);");
        }
        catch (Exception e) {
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}