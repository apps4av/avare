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

    private static final String TILE_NAME = "name";
    
    /**
     * @param context
     */
    public DataBaseHelper(Context context) {
        super(context, context.getString(R.string.DatabaseName), null, DATABASE_VERSION);
        mPref = new Preferences(context);
        mPath = mPref.mapsFolder() + "/" + context.getString(R.string.DatabaseName);
        mCenterTile = new Tile();
    }

    /**
     * 
     * @return
     */
    public synchronized boolean isPresent() {
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
     * Open database
     */
    private synchronized void opens() {
        if(mPath == null) {
            return;
        }
        if(mDataBase != null) {
            return;
        }
        try {
            
            mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY | 
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }
        catch(RuntimeException e) {
            mDataBase = null;
        }
        
    }

    /**
     * Close database
     */
    private synchronized void closes() {
        if(mDataBase != null) {
            try {
                mDataBase.close();
                super.close();
            }
            catch (Exception e) {
            }
            mDataBase = null;
        }
    }

    /**
     * 
     * @param name
     * @return
     */
    public synchronized float[] findDiagramMatrix(String name) {
        Cursor cursor;
        float ret[] = new float[12];
        int it;
        
        for(it = 0; it < 12; it++) {
            ret[it] = 0;
        }
        
        opens();
        /*
         * In case we fail
         */
        
        if(mDataBase == null) {
            closes();
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            closes();
            return null;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               cursor = mDataBase.rawQuery(
                       "select * from " + TABLE_AIRPORT_DIAGS + " where " + LOCATION_ID_DB + "==\"" + name +"\"", null);
        }
        catch (Exception e) {
            cursor = null;
        }

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
                cursor.close();
            }
        }
        catch (Exception e) {
            
        }
        closes();
        return ret;
    }

    /**
     * 
     * @param name
     * @return
     */
    public synchronized Tile findTile(String name) {
        Cursor cursor;
        opens();
        /*
         * In case we fail
         */
        
        if(mDataBase == null) {
            closes();
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            closes();
            return null;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               cursor = mDataBase.rawQuery(
                       "select * from " + TABLE_FILES + " where " + TILE_NAME + "==\"" + name +"\"", null);           
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
        
                    /*
                     * Database
                     */
                    Tile tile = new Tile(
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
                  
                    cursor.close();
                    
                    /*
                     * Position on tile
                     */
                    closes();
                    return tile;                
                }
                else {
                    cursor.close();
                    closes();
                    return null;
                }    
            }
            else {
                closes();
                return null;
            }
        }
        catch (Exception e) {
            closes();
            return null;            
        }
    }

    /**
     * Find airports in an particular area
     * @param name
     * @param params
     */
    public synchronized void findClosestAirports(double lon, double lat, Airport[] airports) {
        Cursor cursor;

        opens();
        if(mDataBase == null) {
            closes();
            return;
        }
        
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

        try {
            cursor = mDataBase.rawQuery(qry, null);
        }
        catch (Exception e) {
            closes();
            return;
        }

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
                cursor.close();
            }  
        }
        catch (Exception e) {
            
        }
        closes();
    }

    /**
     * Search something in database
     * @param name
     * @param params
     * @return
     */
    public synchronized boolean search(String name, LinkedHashMap<String, String> params) {
        
        Cursor cursor;
        String query;
        String qbasic = "select " + LOCATION_ID_DB + "," + FACILITY_NAME_DB + "," + TYPE_DB + " from ";
        String qend = " (" + LOCATION_ID_DB + " like '" + name + "%' " + ") order by " + LOCATION_ID_DB + " asc"; 
        
        /*
         * All queries for airports, navaids, fixes
         */

        query = qbasic + TABLE_NAV + " where " + qend;

        opens();
        if(mDataBase == null) {
            closes();
            return false;
        }
        
        try {
            cursor = mDataBase.rawQuery(query, null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.NAVAID, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
                cursor.close();
            }
        }
        catch (Exception e) {
            cursor = null;
        }
        closes();

        query = qbasic + TABLE_AIRPORTS + " where ";
        if(!mPref.shouldShowAllFacilities()) {
            query += TYPE_DB + "=='AIRPORT' and ";
        }
        query += qend;

        opens();
        if(mDataBase == null) {
            closes();
            return false;
        }
        
        try {
            cursor = mDataBase.rawQuery(query, null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.BASE, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
                cursor.close();
            }
        }
        catch (Exception e) {
            cursor = null;
        }
        closes();


        query = qbasic + TABLE_FIX + " where " + qend;

        opens();
        if(mDataBase == null) {
            closes();
            return false;
        }
        
        try {
            cursor = mDataBase.rawQuery(query, null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.FIX, cursor.getString(2), cursor.getString(1), cursor.getString(0));
                    s.putInHash(params);
                }
                cursor.close();
            }
        }
        catch (Exception e) {
            cursor = null;
        }
        closes();
        
        return true;
    }

    /**
     * Find all information about a facility / destination based on its name
     * @param name
     * @param params
     * @return
     */
    public synchronized boolean findDestination(String name, String type, LinkedHashMap<String, String> params, LinkedList<Runway> runways) {
        
        Cursor cursor;
        Cursor cursorfreq;
        Cursor cursorrun;
        
        String types = "";
        if(type.equals(Destination.BASE)) {
            types = TABLE_AIRPORTS;
        }
        else if(type.equals("Destination.NAVAID")) {
            types = TABLE_NAV;
        }
        else if(type.equals("Destination.FIX")) {
            types = TABLE_FIX;
        }

        opens();
        if(mDataBase == null) {
            closes();
            return false;
        }
        
        try {
            cursor = mDataBase.rawQuery(
                    "select * from " + types + " where " + LOCATION_ID_DB + "==\"" + name + "\";", null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    /*
                     * Put ID and name first
                     */
                    params.put(LOCATION_ID, cursor.getString(LOCATION_ID_COL));
                    params.put(FACILITY_NAME, cursor.getString(FACILITY_NAME_COL));
                    
                    if(type.equals(Destination.BASE)) {
                        /*
                         * Now find airport frequencies then put
                         */
                        try {
                            cursorfreq = mDataBase.rawQuery(
                                "select * from " + TABLE_AIRPORT_FREQ + " where " + LOCATION_ID_DB + "==\"" + name                            
                                + "\" or " + LOCATION_ID_DB + "==\"K" + name + "\";", null);
                        }
                        catch (Exception e) {
                            cursorfreq = null;
                        }
        
                        /*
                         * Add all of them
                         */
                        if(cursorfreq != null) {
                            while(cursorfreq.moveToNext()) {
                                if(params.containsKey(cursorfreq.getString(1))) {
                                    /*
                                     * Add a hash if duplicate value
                                     */
                                    params.put(cursorfreq.getString(1) + "#", cursorfreq.getString(2));                                
                                }
                                else {
                                    params.put(cursorfreq.getString(1), cursorfreq.getString(2));
                                }
                            }
                            cursorfreq.close();
                        }
        
        
                        /*
                         * Now find airport runways.
                         */
                        try {
                            cursorrun = mDataBase.rawQuery(
                                "select * from " + TABLE_AIRPORT_RUNWAYS + " where " + LOCATION_ID_DB + "==\"" + name
                                + "\" or " + LOCATION_ID_DB + "==\"K" + name + "\";", null);
                        }
                        catch (Exception e) {
                            cursorrun = null;
                        }
        
                        /*
                         * Add all of them
                         */
                        if(cursorrun != null) {
                            while(cursorrun.moveToNext()) {
                                
                                String Length = cursorrun.getString(1);
                                String Width = cursorrun.getString(2);
                                String Surface = cursorrun.getString(3);
                                if(Surface.equals("")) {
                                    Surface = "Unknown";
                                }
                                String Lighted = cursorrun.getString(4);
                                if(Lighted.equals("0") || Lighted.equals("")) {
                                    Lighted = "";
                                }
                                else {
                                    Lighted = ", " + Lighted;                            
                                }
                                String Closed = cursorrun.getString(5);
                                if(Closed.equals("0") || Closed.equals("")) {
                                    Closed = ", Open";
                                }
                                else {
                                    Closed = ", Closed";                            
                                }
                                
                                String runl = Helper.removeLeadingZeros(cursorrun.getString(6));
                                String runh = Helper.removeLeadingZeros(cursorrun.getString(12));
                                
                                params.put("Runway " + runl + "/" + runh, 
                                        "Length " + Length + 
                                        ", Width " + Width + 
                                        ", Surface " + Surface +
                                        Lighted +
                                        Closed);
                                
                                String Elevation = cursorrun.getString(9);
                                if(Elevation.equals("")) {
                                    Elevation = "0";
                                }
                                String Heading = cursorrun.getString(10);
                                String DT = cursorrun.getString(11);
                                if(DT.equals("")) {
                                    DT = "0";
                                }
                                
                                params.put("Runway " + runl,
                                        "Elevation " + Elevation + 
                                        ", True Heading " + Heading + 
                                        ", Displaced Threshold " + DT);
                                Runway l = new Runway(runl, cursor.getString(10).trim(), Heading, cursorrun.getString(8), cursorrun.getString(7));
                                runways.add(l);        
                                
                                Elevation = cursorrun.getString(15);
                                if(Elevation.equals("")) {
                                    Elevation = "0";
                                }
                                Heading = cursorrun.getString(16);
                                DT = cursorrun.getString(17);
                                if(DT.equals("")) {
                                    DT = "0";
                                }
                                
                                params.put("Runway " + runh,
                                        "Elevation " + Elevation + 
                                        ", True Heading " + Heading + 
                                        ", Displaced Threshold " + DT);
                                
                                Runway h = new Runway(runh, cursor.getString(10).trim(), Heading,  cursorrun.getString(14), cursorrun.getString(13));
                                runways.add(h);        
        
                            }
                            cursorrun.close();
                        }
                    }
    
                    /*
                     * Less important AFD in the end.
                     */
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
                                    
                    cursor.close();
                    
                    closes();
                    return true;
                }
            }
        }
        catch (Exception e) {
            closes();
            return false;
        }
        
        closes();
        return false;
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
    public synchronized boolean isWithin(double lon, double lat, double offset[], double p[]) {
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
    public synchronized String findClosestAirportID(double lon, double lat) {
        Cursor cursor;

        opens();
        if(mDataBase == null) {
            closes();
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            closes();
            return null;
        }
        
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
        
        try {
            cursor = mDataBase.rawQuery(qry, null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    String ret = new String(cursor.getString(0));
    
                    cursor.close();
                    closes();
                    return(ret);
                }
                else {
                    cursor.close();
                    closes();
                    return null;
                }    
            }
            else {
                closes();
                return null;
            }
        }
        catch (Exception e) {
            
        }
        closes();
        return null;
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
    public synchronized Tile findClosest(double lon, double lat, double offset[], double p[]) {
      
        Cursor cursor;

        opens();
        /*
         * In case we fail
         */
        offset[0] = 0;
        offset[1] = 0;
        
        if(mDataBase == null) {
            closes();
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            closes();
            return null;
        }
        
        
        
        /*
         * Find with sqlite query
         */
        try {
            String type = mPref.getChartType();
            cursor = mDataBase.rawQuery(
                "select * from " + TABLE_FILES + " where " + TILE_NAME + " like \"%tiles/" + type + "/%\" and " + 
                "((latul - " + lat + ") > 0) and " +
                "((latll - " + lat + ") < 0) and " + 
                "((lonul - " + lon + ") < 0) and " + 
                "((lonur - " + lon + ") > 0);" ,
                null);
        }
        catch (Exception e) {
            cursor = null;
        }

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
    
                    cursor.close();
                    closes();
                    return mCenterTile;                
                }
                else {
                    cursor.close();
                    closes();
                    return null;
                }    
            }
            else {
                closes();
                return null;
            }
        }
        catch (Exception e) {
        }
        
        closes();
        return null;
    }
}
