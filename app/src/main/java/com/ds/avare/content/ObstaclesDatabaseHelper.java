package com.ds.avare.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.Preference;

import com.ds.avare.storage.Preferences;

import java.io.File;

/**
 * Created by zkhan on 3/13/17.
 */

public class ObstaclesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "obs.db";
    private String mFolder; // save this as users can change

    public ObstaclesDatabaseHelper(Context context, String folder) {
        super(context, folder + File.separator + DBNAME, null, 1);
        mFolder = folder;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public String getFolder() {
        return mFolder;
    }

}