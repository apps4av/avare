package com.ds.avare.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

/**
 * Created by zkhan on 3/13/17.
 */

public class ProceduresDatabaseHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "procedures.db";
    private String mFolder; // save this as users can change

    public ProceduresDatabaseHelper(Context context, String folder) {
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