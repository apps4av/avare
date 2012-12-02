/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;


import java.util.LinkedHashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author zkhan
 * The class that does the grunt wortk of dealing with the databse
 */
public class DataBaseImageHelper extends SQLiteOpenHelper {

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
    
    /**
     * @param context
     */
    public DataBaseImageHelper(Context context) {
        super(context, context.getString(R.string.DatabaseName), null, DATABASE_VERSION);
        mCenterTile = new Tile();
        mPref = new Preferences(context);
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#close()
     */
    @Override
    public synchronized void close() {
   
        if(mDataBase != null) {
            mDataBase.close();
        }
        super.close();
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
     * @throws SQLException
     */
    public void openDataBase(String name) throws SQLException{
        //Open the database        
        String mPath = name;

        try {
            
            mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READONLY | 
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }
        catch(RuntimeException e) {
            mDataBase = null;
            throw e;
        }
    }


    /**
     * 
     * @param name
     * @return
     */
    public Tile findTile(String name) {
        Cursor cursor;
        
        /*
         * In case we fail
         */
        
        if(mDataBase == null) {
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            return null;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               cursor = mDataBase.rawQuery(
                       "select * from files where name==\"" + name +"\"", null);
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
                    return tile;                
                }
                else {
                    return null;
                }    
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            return null;            
        }
    }

    /**
     * Find airports in an particular area
     * @param name
     * @param params
     */
    public void findClosestAirports(double lon, double lat, Airport[] airports) {
        Cursor cursor;
        
        if(mDataBase == null) {
            return;
        }
        
        /*
         * Limit to airports taken by array airports
         */
        try {
            cursor = mDataBase.rawQuery(
                    "select * from airports where Type==\"AIRPORT\" order by ((" + 
                    lon + " - ARPLongitude) * (" + lon + "- ARPLongitude) + (" + 
                    lat + " - ARPLatitude)  * (" + lat + "- ARPLatitude)) ASC limit " + airports.length + ";",
                    null);
        }
        catch (Exception e) {
            return;
        }

        try {
            int id = 0;
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                        params.put("Location ID", cursor.getString(0));
                        params.put("Facility Name", cursor.getString(4));
                        params.put("ARP Latitude", Double.toString(cursor.getDouble(1)));
                        params.put("ARP Longitude", Double.toString(cursor.getDouble(2)));
                        params.put("Magnetic Variation", cursor.getString(10));
                        params.put("Fuel Types", cursor.getString(12));
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
    }

    /**
     * Find all information about a facility / destination based on its name
     * @param name
     * @param params
     * @return
     */
    public boolean findDestination(String name, LinkedHashMap<String, String> params) {
        
        Cursor cursor;
        Cursor cursorfreq;
        Cursor cursorrun;
        
        if(mDataBase == null) {
            return false;
        }
        
        try {
            cursor = mDataBase.rawQuery(
                    "select * from airports where LocationID==\"" + name + "\";", null);
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
                    params.put("Location ID", cursor.getString(0));
                    params.put("Facility Name", cursor.getString(4));
                    
                    /*
                     * Now find airport frequencies then put
                     */
                    try {
                        cursorfreq = mDataBase.rawQuery(
                            "select * from airportfreq where LocationID==\"" + name
                            + "\" or LocationID==\"K" + name + "\";", null);
                    }
                    catch (Exception e) {
                        cursorfreq = null;
                    }
    
                    /*
                     * Add all of them
                     */
                    if(cursorfreq != null) {
                        while(cursorfreq.moveToNext()) {
                            params.put(cursorfreq.getString(1), cursorfreq.getString(2));
                        }
                        cursorfreq.close();
                    }
    
    
                    /*
                     * Now find airport runways.
                     */
                    try {
                        cursorrun = mDataBase.rawQuery(
                            "select * from airportrunways where LocationID==\"" + name
                            + "\" or LocationID==\"K" + name + "\";", null);
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
                                Lighted = ", Unlighted";
                            }
                            else {
                                Lighted = ", Lighted";                            
                            }
                            String Closed = cursorrun.getString(5);
                            if(Closed.equals("0") || Closed.equals("")) {
                                Closed = ", Open";
                            }
                            else {
                                Closed = ", Closed";                            
                            }
                            
                            params.put("Runway " + cursorrun.getString(6) + "/" + cursorrun.getString(12), 
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
                            
                            params.put("Runway " + cursorrun.getString(6),
                                    "Elevation " + Elevation + 
                                    ", True Heading " + Heading + 
                                    ", Displaced Threshold " + DT);
    
                            
                            Elevation = cursorrun.getString(15);
                            if(Elevation.equals("")) {
                                Elevation = "0";
                            }
                            Heading = cursorrun.getString(16);
                            DT = cursorrun.getString(17);
                            if(DT.equals("")) {
                                DT = "0";
                            }
                            
                            params.put("Runway " + cursorrun.getString(12),
                                    "Elevation " + Elevation + 
                                    ", True Heading " + Heading + 
                                    ", Displaced Threshold " + DT);
    
                        }
                        cursorrun.close();
                    }
    
                    /*
                     * Less important AFD in the end.
                     */
                    params.put("ARP Latitude", Double.toString(cursor.getDouble(1)));
                    params.put("ARP Longitude", Double.toString(cursor.getDouble(2)));
                    params.put("Type", cursor.getString(3));
                    params.put("Use", cursor.getString(5));
                    params.put("Owner Phone", cursor.getString(6));
                    params.put("Manager", cursor.getString(7));
                    params.put("Manager Phone", cursor.getString(8));
                    params.put("ARP Elevation", cursor.getString(9));
                    params.put("Magnetic Variation", cursor.getString(10));
                    params.put("Traffic Pattern Altitude", cursor.getString(11));
                    params.put("Fuel Types", cursor.getString(12));
                    params.put("Airframe Repair", cursor.getString(13));
                    params.put("Power Plant Repair", cursor.getString(14));
                    params.put("Bottled Oxygen Type", cursor.getString(15));
                    params.put("Bulk Oxygen Type", cursor.getString(16));
                    params.put("ATCT", cursor.getString(17));
                    params.put("UNICOM Frequencies", cursor.getString(18));
                    params.put("CTAF Frequency", cursor.getString(19));
                    params.put("Non Commercial Landing Fee", cursor.getString(20));
                                    
                    cursor.close();
    
    
                    return true;
                }
            }
        }
        catch (Exception e) {
            return false;
        }

        
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
    public boolean isWithin(double lon, double lat, double offset[], double p[]) {
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
        Cursor cursor;
        
        if(mDataBase == null) {
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            return null;
        }
        
        /*
         * Find with sqlite query
         */
        String qry = 
                "select LocationID from airports where Type==\"AIRPORT\" and (("
                + "(ARPLongitude - " + lon + ") * (ARPLongitude - " + lon + ") + "
                + "(ARPLatitude  - " + lat + ") * (ARPLatitude  - " + lat + ")"
                + ") < 0.001) limit 1;";
        
        try {
            cursor = mDataBase.rawQuery(qry,
                null);
        }
        catch (Exception e) {
            cursor = null;
        }

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    String ret = new String(cursor.getString(0));
    
                    cursor.close();
                    return(ret);
                }
                else {
                    return null;
                }    
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            
        }
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
    public Tile findClosest(double lon, double lat, double offset[], double p[]) {
      
        Cursor cursor;
        
        /*
         * In case we fail
         */
        offset[0] = 0;
        offset[1] = 0;
        
        if(mDataBase == null) {
            return null;
        }
        
        if(!mDataBase.isOpen()) {
            return null;
        }
        
        
        
        /*
         * Find with sqlite query
         */
        try {
            String type = mPref.getChartType();
            cursor = mDataBase.rawQuery(
                "select * from files where name like \"%tiles/" + type + "/%\" and " + 
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
                  
                    cursor.close();
                    
                    /*
                     * Position on tile
                     */
                    offset[0] = mCenterTile.getOffsetX(lon);
                    offset[1] = mCenterTile.getOffsetY(lat);
                    p[0] = mCenterTile.getPx();
                    p[1] = mCenterTile.getPy();
    
                    return mCenterTile;                
                }
                else {
                    return null;
                }    
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
        }
        
        return null;
    }
}
