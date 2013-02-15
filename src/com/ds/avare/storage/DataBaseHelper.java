/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.storage;


import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.ds.avare.R;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.shapes.Tile;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author zkhan
 * The class that does the grunt wortk of dealing with the databse
 */
public class DataBaseHelper extends SQLiteOpenHelper {

    /**
     * 
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Cache this class to sqlite
     */
    private SQLiteDatabase mDataBase; 
    
    /*
     * Center tile info
     */
    private Tile mCenterTile;
    
    /*
     * Preferences
     */
    private Preferences mPref;
    
    /*
     * 
     */
    private String mPath;
    
    /*
     * How many users at this point. Used for closing the database
     * Will serve as a non blocking sem with synchronized statement
     */
    private int mUsers;
    
    public  static final String  FACILITY_NAME = "Facility Name";
    private static final String  FACILITY_NAME_DB = "FacilityName";
    private static final int    FACILITY_NAME_COL = 4;
    public  static final String  LOCATION_ID = "Location ID";
    private static final String  LOCATION_ID_DB = "LocationID";
    private static final int    LOCATION_ID_COL = 0;
    public  static final String  MAGNETIC_VARIATION = "Magnetic Variation";
    //private static final String  MAGNETIC_VARIATION_DB = "MagneticVariation";
    private static final int    MAGNETIC_VARIATION_COL = 10;
    public  static final String  TYPE= "Type";
    private static final String  TYPE_DB = "Type";
    private static final int    TYPE_COL = 3;
    public  static final String  LATITUDE = "Latitude";
    private static final String  LATITUDE_DB = "ARPLatitude";
    private static final int    LATITUDE_COL = 1;
    public  static final String  LONGITUDE = "Longitude";
    private static final String  LONGITUDE_DB = "ARPLongitude";
    private static final int    LONGITUDE_COL = 2;
    public  static final String  FUEL_TYPES = "Fuel Types";
    //private static final String  FUEL_TYPES_DB = "FuelTypes";
    private static final int    FUEL_TYPES_COL = 12;
    
    private static final String TABLE_AIRPORTS = "airports";
    private static final String TABLE_AIRPORT_DIAGS = "airportdiags";
    private static final String TABLE_AIRPORT_FREQ = "airportfreq";
    private static final String TABLE_AIRPORT_RUNWAYS = "airportrunways";
    private static final String TABLE_FILES = "files";
    private static final String TABLE_FIX = "fix";
    private static final String TABLE_NAV = "nav";
    private static final String TABLE_OBSTACLES = "obstacles";

    private static final String TILE_NAME = "name";
       
    /**
     * @param context
     */
    public DataBaseHelper(Context context) {
        super(context, context.getString(R.string.DatabaseName), null, DATABASE_VERSION);
        mPref = new Preferences(context);
        mPath = mPref.mapsFolder() + "/" + context.getString(R.string.DatabaseName);
        mCenterTile = null;
    }

    /**
     * 
     * @return
     */
    public boolean isPresent() {
        if(null == mPath) {
            return false;
        }
        File f = new File(mPath);
        return(f.exists());
    }
   
    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase database) {        
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Close database
     */
    private void closes(Cursor c) {
        if(null != c) {
            c.close();
        }

        synchronized (this) {
            mUsers--;
            if((mDataBase != null) && (mUsers <= 0)) {
                try {
                    mDataBase.close();
                    super.close();
                }
                catch (Exception e) {
                }
                mDataBase = null;
                mUsers = 0;
            }
        }
    }

    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQuery(String statement) {
        Cursor c = null;
        
        /*
         * 
         */
        synchronized (this) {
            if(mPath == null) {
                return c;
            }
            if(mDataBase == null) {
                mUsers = 0;
                try {
                    
                    mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBase = null;
                }
            }
            if(mDataBase == null) {
                return c;
            }
            mUsers++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBase == null) {
            return c;
        }
        
        if(!mDataBase.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBase.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public float[] findDiagramMatrix(String name) {
        float ret[] = new float[12];
        int it;
        
        for(it = 0; it < 12; it++) {
            ret[it] = 0;
        }
        
        Cursor cursor = doQuery("select * from " + TABLE_AIRPORT_DIAGS + " where " + LOCATION_ID_DB + "==\"" + name +"\"");
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
        
                    /*
                     * Database
                     */
                    for(it = 0; it < 12; it++) {
                        ret[it] = cursor.getFloat(it + 1);
                    }                    
                }
            }
        }
        catch (Exception e) {
            
        }
        closes(cursor);
        return ret;
    }

    /**
     * 
     * @param name
     * @return
     */
    public Tile findTile(String name) {
        Cursor cursor = doQuery("select * from " + TABLE_FILES + " where " + TILE_NAME + "==\"" + name +"\"");
        Tile tile = null;

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
        
                    /*
                     * Database
                     */
                    tile = new Tile(
                            mPref,
                            cursor.getString(0),
                            cursor.getDouble(1),
                            cursor.getDouble(2),
                            cursor.getDouble(3),
                            cursor.getDouble(4),
                            cursor.getDouble(5),
                            cursor.getDouble(6),
                            cursor.getDouble(7),
                            cursor.getDouble(8),
                            cursor.getDouble(9),
                            cursor.getDouble(10));
                    /*
                     * Position on tile
                     */
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);
        return tile;            

    }

    /**
     * Find airports in an particular area
     * @param name
     * @param params
     */
    public void findClosestAirports(double lon, double lat, Airport[] airports) {

        /*
         * Limit to airports taken by array airports
         */
        String qry = "select * from " + TABLE_AIRPORTS;
        if(!mPref.shouldShowAllFacilities()) {
            qry += " where " + TYPE_DB + "==\"AIRPORT\" ";
        }
        qry += " order by ((" + 
                lon + " - " + LONGITUDE_DB + ") * (" + lon + "- " + LONGITUDE_DB +") + (" + 
                lat + " - " + LATITUDE_DB + ") * (" + lat + "- " + LATITUDE_DB + ")) ASC limit " + airports.length + ";";            

        Cursor cursor = doQuery(qry);

        try {
            int id = 0;
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                        params.put(LOCATION_ID, cursor.getString(LOCATION_ID_COL));
                        params.put(FACILITY_NAME, cursor.getString(FACILITY_NAME_COL));
                        params.put(LATITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LATITUDE_COL))));
                        params.put(LONGITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LONGITUDE_COL))));
                        params.put(MAGNETIC_VARIATION, cursor.getString(MAGNETIC_VARIATION_COL));
                        params.put(FUEL_TYPES, cursor.getString(FUEL_TYPES_COL));
                        airports[id] = new Airport(params, lon, lat);
                        id++;
                    }
                    while(cursor.moveToNext());
                }
            }  
        }
        catch (Exception e) {
        }
        closes(cursor);
    }

    /**
     * Search something in database
     * @param name
     * @param params
     */
    public void search(String name, LinkedHashMap<String, String> params) {
        
        String qry;
        String qbasic = "select " + LOCATION_ID_DB + "," + FACILITY_NAME_DB + "," + TYPE_DB + " from ";
        String qend = " (" + LOCATION_ID_DB + " like '" + name + "%' " + ") order by " + LOCATION_ID_DB + " asc"; 
        
        /*
         * All queries for airports, navaids, fixes
         */

        qry = qbasic + TABLE_NAV + " where " + qend;
        Cursor cursor = doQuery(qry);

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.NAVAID, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);

        qry = qbasic + TABLE_AIRPORTS + " where ";
        if(!mPref.shouldShowAllFacilities()) {
            qry += TYPE_DB + "=='AIRPORT' and ";
        }
        qry += qend;

        cursor = doQuery(qry);
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.BASE, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);


        qry = qbasic + TABLE_FIX + " where " + qend;
        cursor = doQuery(qry);
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.FIX, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
    }

    /**
     * Find all information about a facility / destination based on its name
     * @param name
     * @param params
     * @return
     */
    public void findDestination(String name, String type, LinkedHashMap<String, String> params, LinkedList<Runway> runways) {
        
        Cursor cursor;
        
        String types = "";
        if(type.equals(Destination.BASE)) {
            types = TABLE_AIRPORTS;
        }
        else if(type.equals(Destination.NAVAID)) {
            types = TABLE_NAV;
        }
        else if(type.equals(Destination.FIX)) {
            types = TABLE_FIX;
        }

        cursor = doQuery("select * from " + types + " where " + LOCATION_ID_DB + "==\"" + name + "\";");

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    /*
                     * Put ID and name first
                     */
                    params.put(LOCATION_ID, cursor.getString(LOCATION_ID_COL));
                    params.put(FACILITY_NAME, cursor.getString(FACILITY_NAME_COL));
                    params.put(LATITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LATITUDE_COL))));
                    params.put(LONGITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LONGITUDE_COL))));
                    params.put(TYPE, cursor.getString(TYPE_COL).trim());
                    if(type.equals(Destination.BASE)) {
                        params.put("Use", cursor.getString(5).trim());
                        params.put("Owner Phone", cursor.getString(6).trim());
                        params.put("Manager", cursor.getString(7).trim());
                        params.put("Manager Phone", cursor.getString(8).trim());
                        params.put("Elevation", cursor.getString(9).trim());
                        params.put(MAGNETIC_VARIATION, cursor.getString(MAGNETIC_VARIATION_COL).trim());
                        params.put("Traffic Pattern Altitude", cursor.getString(11).trim());
                        params.put(FUEL_TYPES, cursor.getString(FUEL_TYPES_COL).trim());
                        params.put("Airframe Repair", cursor.getString(13).trim());
                        params.put("Power Plant Repair", cursor.getString(14).trim());
                        params.put("Bottled Oxygen Type", cursor.getString(15).trim());
                        params.put("Bulk Oxygen Type", cursor.getString(16).trim());
                        params.put("Control Tower", cursor.getString(17).trim());
                        params.put("UNICOM Frequencies", cursor.getString(18).trim());
                        params.put("CTAF Frequency", cursor.getString(19).trim());
                        params.put("Non Commercial Landing Fee", cursor.getString(20).trim());
                    }
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);

        if(!type.equals(Destination.BASE)) {
            return;
        }
            
        cursor = doQuery("select * from " + TABLE_AIRPORT_FREQ + " where " + LOCATION_ID_DB + "==\"" + name                            
                + "\" or " + LOCATION_ID_DB + "==\"K" + name + "\";");

        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    if(params.containsKey(cursor.getString(1))) {
                        /*
                         * Add a hash if duplicate value
                         */
                        params.put(cursor.getString(1) + "#", cursor.getString(2));                                
                    }
                    else {
                        params.put(cursor.getString(1), cursor.getString(2));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        cursor.close();


        cursor = doQuery("select * from " + TABLE_AIRPORT_RUNWAYS + " where " + LOCATION_ID_DB + "==\"" + name
                + "\" or " + LOCATION_ID_DB + "==\"K" + name + "\";");
        
        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    
                    String Length = cursor.getString(1);
                    String Width = cursor.getString(2);
                    String Surface = cursor.getString(3);
                    if(Surface.equals("")) {
                        Surface = "Unknown";
                    }
                    String Lighted = cursor.getString(4);
                    if(Lighted.equals("0") || Lighted.equals("")) {
                        Lighted = "";
                    }
                    else {
                        Lighted = ", " + Lighted;                            
                    }
                    String Closed = cursor.getString(5);
                    if(Closed.equals("0") || Closed.equals("")) {
                        Closed = ", Open";
                    }
                    else {
                        Closed = ", Closed";                            
                    }
                    
                    String runl = Helper.removeLeadingZeros(cursor.getString(6));
                    String runh = Helper.removeLeadingZeros(cursor.getString(12));
                    
                    params.put("Runway " + runl + "/" + runh, 
                            "Length " + Length + 
                            ", Width " + Width + 
                            ", Surface " + Surface +
                            Lighted +
                            Closed);
                    
                    String Elevation = cursor.getString(9);
                    if(Elevation.equals("")) {
                        Elevation = "0";
                    }
                    String Heading = cursor.getString(10);
                    String DT = cursor.getString(11);
                    if(DT.equals("")) {
                        DT = "0";
                    }
                    
                    params.put("Runway " + runl,
                            "Elevation " + Elevation + 
                            ", True Heading " + Heading + 
                            ", Displaced Threshold " + DT);
                    Runway l = new Runway(runl, cursor.getString(10).trim(), Heading, cursor.getString(8), cursor.getString(7));
                    runways.add(l);        
                    
                    Elevation = cursor.getString(15);
                    if(Elevation.equals("")) {
                        Elevation = "0";
                    }
                    Heading = cursor.getString(16);
                    DT = cursor.getString(17);
                    if(DT.equals("")) {
                        DT = "0";
                    }
                    
                    params.put("Runway " + runh,
                            "Elevation " + Elevation + 
                            ", True Heading " + Heading + 
                            ", Displaced Threshold " + DT);
                    
                    Runway h = new Runway(runh, cursor.getString(10).trim(), Heading,  cursor.getString(14), cursor.getString(13));
                    runways.add(h);        
    
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);        
    }

    
    /**
     * If we are within the tile of last query, return just offsets.
     * Always call this before calling sister function findClosest() which does the 
     * expensive DB query.
     * @param lon
     * @param lat
     * @param offset
     * @param p
     * @return
     */
    public boolean isWithin(double lon, double lat, double offset[], double p[]) {
        if(null == mCenterTile) {
            return false;
        }
        if(mCenterTile.within(lon, lat)) {
            offset[0] = mCenterTile.getOffsetX(lon);
            offset[1] = mCenterTile.getOffsetY(lat);
            p[0] = mCenterTile.getPx();
            p[1] = mCenterTile.getPy();
            return true;
        }
        return false;
    }
    
    /**
     * Find the closets tiles to current position
     * @param lon
     * @param lat
     * @param offset
     * @param p
     * @param names
     * @return
     */
    public String findClosestAirportID(double lon, double lat) {

        /*
         * Find with sqlite query
         */
        String qry = "select " + LOCATION_ID_DB + " from " + TABLE_AIRPORTS;
        if(!mPref.shouldShowAllFacilities()) {
            qry +=  " where " + TYPE_DB + "==\"AIRPORT\" and ((";
        }
        else {
            qry += " where ((";
        }

        qry += "(" + LONGITUDE_DB + " - " + lon + ") * (" + LONGITUDE_DB  + " - " + lon + ") + "
                + "(" + LATITUDE_DB + " - " + lat + ") * (" + LATITUDE_DB + " - " + lat + ")"
                + ") < 0.001) limit 1;";
        
        Cursor cursor = doQuery(qry);
        String ret = null;

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    ret = new String(cursor.getString(0));
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        return ret;
    }
      
    /**
     * Find the closets tiles to current position
     * @param lon
     * @param lat
     * @param offset
     * @param p
     * @param names
     * @return
     */
    public Tile findClosest(double lon, double lat, double offset[], double p[]) {
      
        /*
         * In case we fail
         */
        offset[0] = 0;
        offset[1] = 0;
        
        Cursor cursor = doQuery(
                "select * from " + TABLE_FILES + " where " + TILE_NAME + " like \"%tiles/" + mPref.getChartType() + "/%\" and " + 
                "((latul - " + lat + ") > 0) and " +
                "((latll - " + lat + ") < 0) and " + 
                "((lonul - " + lon + ") < 0) and " + 
                "((lonur - " + lon + ") > 0);");
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
    
                    /*
                     * Database only return center tile, we find tiles around it using arithmetic
                     */
                    mCenterTile = new Tile(
                            mPref,
                            cursor.getString(0),
                            cursor.getDouble(1),
                            cursor.getDouble(2),
                            cursor.getDouble(3),
                            cursor.getDouble(4),
                            cursor.getDouble(5),
                            cursor.getDouble(6),
                            cursor.getDouble(7),
                            cursor.getDouble(8),
                            cursor.getDouble(9),
                            cursor.getDouble(10));
                  
                    /*
                     * Position on tile
                     */
                    offset[0] = mCenterTile.getOffsetX(lon);
                    offset[1] = mCenterTile.getOffsetY(lat);
                    p[0] = mCenterTile.getPx();
                    p[1] = mCenterTile.getPy();
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);
        return mCenterTile;        
    }
    
    /**
     * 
     * @param lon
     * @param lat
     * @param height
     * @return
     */
    public LinkedList<Obstacle> findObstacles(double lon, double lat, int height) {
        
        LinkedList<Obstacle> list = new LinkedList<Obstacle>();
        
        /*
         * Find obstacles 200 feet below or higher in lon/lat radius
         */
        Cursor cursor = doQuery("select * from " + TABLE_OBSTACLES + " where (height > " + height + ") and " +
                "(lat > " + (lat - Obstacle.RADIUS) + ") and (lat < "  + (lat + Obstacle.RADIUS) + ") and " +
                "(lon > " + (lon - Obstacle.RADIUS) + ") and (lon < "  + (lon + Obstacle.RADIUS) + ");");
        
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    list.add(new Obstacle(cursor.getFloat(0), cursor.getFloat(1), cursor.getInt(2)));
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);
        return list;
    }
}
