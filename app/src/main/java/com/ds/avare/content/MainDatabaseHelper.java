package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.NotActiveException;

/**
 * Created by zkhan on 3/13/17.
 */

public class MainDatabaseHelper extends SQLiteOpenHelper {

    private String mFolder; // save this as users can change
    private String mName; // this is available in newer OS

    public MainDatabaseHelper(Context context, String folder, String name) {
        super(context, folder + File.separator + name, null, 1);

        mFolder = folder;
        mName = folder + File.separator + name;
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

    public String getName() {
        return mName;
    }

}