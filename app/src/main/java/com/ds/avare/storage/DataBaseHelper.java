/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.storage;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;

import com.ds.avare.R;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.NavAid;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.plan.Cifp;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Radial;
import com.ds.avare.utils.Helper;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;

/**
 * @author zkhan, jlmcgraw
 * The class that does the grunt wortk of dealing with the databse
 */
public class DataBaseHelper  {

    /**
     * Cache this class to sqlite
     */
    private SQLiteDatabase mDataBase; 
    private SQLiteDatabase mDataBaseProcedures;
    private SQLiteDatabase mDataBasePlates;
    private SQLiteDatabase mDataBaseGeoPlates;
    private SQLiteDatabase mDataBaseWeather;
    private SQLiteDatabase mDataBaseGameTFRs;

    /*
     * Preferences
     */
    private Preferences mPref;
    
    /*
     * 
     */
    private Context mContext;
    
    /*
     * How many users at this point. Used for closing the database
     * Will serve as a non blocking sem with synchronized statement
     */
    private Integer mUsers;
    private Integer mUsersPlates;
    private Integer mUsersGeoPlates;
    private Integer mUsersWeather;
    private Integer mUsersProcedures;
    private Integer mUsersGameTFRs;

    
    public  static final String  FACILITY_NAME = "Facility Name";
    private static final String  FACILITY_NAME_DB = "FacilityName";
    private static final int    FACILITY_NAME_COL = 4;
    public  static final String  LOCATION_ID = "Location ID";
    private static final String  LOCATION_ID_DB = "LocationID";
    private static final String  INFO_DB = "info";
    private static final int    LOCATION_ID_COL = 0;
    public  static final String  MAGNETIC_VARIATION = "Magnetic Variation";
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
    public  static final String  NAVAID_MAGNETIC_VARIATION = "Magnetic Variation";
    private static final String  NAVAID_MAGNETIC_VARIATION_DB = "Variation";
    private static final int    NAVAID_MAGNETIC_VARIATION_COL = 5;
    public  static final String  NAVAID_CLASS = "Class";
    private static final String  NAVAID_CLASS_DB = "Class";
    private static final int    NAVAID_CLASS_COL = 6;
    public  static final String  NAVAID_HIWAS = "HIWAS";
    private static final String  NAVAID_HIWAS_DB = "HIWAS";
    private static final int    NAVAID_HIWAS_COL = 7;
    private static final int    NAVAID_ELEVATION_COL = 8;
    public  static final String  FUEL_TYPES = "Fuel Types";
    private static final int    FUEL_TYPES_COL = 12;
    private static final int    CUSTOMS_COL = 13;
    private static final String  CUSTOMS = "Customs";
    private static final int    BEACON_COL = 14;
    private static final String  BEACON = "Beacon";
    private static final int    FSSPHONE_COL = 6;
    public static final String FSSPHONE = "FSS Phone";
    private static final int    SEGCIRCLE_COL = 16;
    private static final String SEGCIRCLE = "Segmented Circle";
    public static final String MANAGER_PHONE = "Manager Phone";
    public static final String PROC = "proc";

    public static final String ELEVATION = "Elevation";
    
    private static final String TABLE_AIRPORTS = "airports";
    private static final String TABLE_AIRPORT_DIAGS = "airportdiags";
    private static final String TABLE_AIRPORT_FREQ = "airportfreq";
    private static final String TABLE_AIRPORT_AWOS = "awos";
    private static final String TABLE_AIRPORT_RUNWAYS = "airportrunways";
    private static final String TABLE_FILES = "files";
    private static final String TABLE_FIX = "fix";
    private static final String TABLE_NAV = "nav";
    private static final String TABLE_TO = "takeoff";
    private static final String TABLE_ALT = "alternate";
    private static final String TABLE_AFD = "afd";
    private static final String TABLE_OBSTACLES = "obs";
    private static final String TABLE_SUA = "saa";
    private static final String TABLE_PROCEDURE = "procedures";
    private static final String TABLE_GEOPLATES = "geoplates";
    private static final String TABLE_AIRWAYS = "airways";
    private static final String TABLE_GAME = "gametfr";


    /**
     * 
     * @return
     */
    private static String getMainDb() {
        return "main.db";
    }

    /**
     * @param context
     */
    public DataBaseHelper(Context context) {
        mPref = new Preferences(context);
        mUsers = mUsersWeather = mUsersPlates = mUsersGeoPlates = mUsersProcedures = mUsersGameTFRs = 0;
        mContext = context;
    }

    /**
     *
     * @return
     */
    public boolean isPresent() {
        String path = mPref.mapsFolder() + "/" + getMainDb();
        File f = new File(path);
        return(f.exists());
    }
   

    /**
     * Close database
     */
    private void closes(Cursor c) {
        if(null != c) {
            try {
                c.close();
            }
            catch (Exception e) {
                
            }
        }

        synchronized(mUsers) {
            mUsers--;
            if((mDataBase != null) && (mUsers <= 0)) {
                try {
                    mDataBase.close();
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
    private Cursor doQuery(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsers) {
            if(mDataBase == null) {
                mUsers = 0;
                try {
                    
                    mDataBase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
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
        
        String qry = "select * from " + TABLE_AIRPORT_DIAGS + " where " + LOCATION_ID_DB + "=='" + name +"'";
        Cursor cursor = doQuery(qry, getMainDb());
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
     * Find airports in an particular area
     * @param name
     * @param params
     */
    public Airport[] findClosestAirports(double lon, double lat, String minRunwayLength) {

        Airport airports[] = null;
        
        /*
         * Limit to airports taken by array airports
         */
        String qry = "select * from " + TABLE_AIRPORTS + "," + TABLE_AIRPORT_RUNWAYS + " where ";
        if(!mPref.isShowAllFacilities()) {
            qry += TABLE_AIRPORTS + "." + TYPE_DB + "=='AIRPORT' and ";
        }

        // Find in both tables, find based on distance
        qry +=  TABLE_AIRPORTS + "." + LOCATION_ID_DB + "=" + 
        		TABLE_AIRPORT_RUNWAYS + "." + LOCATION_ID_DB;
        
        // runway length > certain length
        qry += " and " + "CAST(" + TABLE_AIRPORT_RUNWAYS + ".Length AS INTEGER) >= " + minRunwayLength;
        
        // order by distance then by runway length, and limit by max * 4 (to remove duplicate runways)
        qry += " order by ((" + 
                lon + " - " + TABLE_AIRPORTS + "." + LONGITUDE_DB + ") * (" + lon + "- " + TABLE_AIRPORTS + "." + LONGITUDE_DB +") + (" + 
                lat + " - " + TABLE_AIRPORTS + "." + LATITUDE_DB + ") * (" + lat + "- " + TABLE_AIRPORTS + "." + LATITUDE_DB + ")) ASC " +
                ", " + "CAST(" + TABLE_AIRPORT_RUNWAYS + ".Length AS INTEGER) DESC " +
                " limit " + Preferences.MAX_AREA_AIRPORTS * 2 + ";";

        Cursor cursor = doQuery(qry, getMainDb());

        try {
            int id = 0;
            if(cursor != null) {
                airports = new Airport[Preferences.MAX_AREA_AIRPORTS];
                if(cursor.moveToFirst()) {
                    do {
                        LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                        params.put(LOCATION_ID, cursor.getString(LOCATION_ID_COL));
                        params.put(FACILITY_NAME, cursor.getString(FACILITY_NAME_COL));
                        params.put(FUEL_TYPES, cursor.getString(FUEL_TYPES_COL));
                        params.put(LATITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LATITUDE_COL))));
                        params.put(LONGITUDE, Double.toString(Helper.truncGeo(cursor.getDouble(LONGITUDE_COL))));
                        params.put(MAGNETIC_VARIATION, cursor.getString(MAGNETIC_VARIATION_COL).trim());
                        String parts[] = cursor.getString(9).trim().split("[.]");
                        params.put(ELEVATION, parts[0] + "ft");
                        airports[id] = new Airport(params, lon, lat);
                        // runway length / width in combined table
                        String runway = cursor.getString(24) + "X" + cursor.getString(25);
                        airports[id].setLongestRunway(runway);
                        if(id > 0) {
                        	// eliminate duplicate airports and show longest runway
                        	if(airports[id].getName().equals(airports[id - 1].getName())) {
                        		continue;
                        	}
                        }
                        id++;
                        if(id >= Preferences.MAX_AREA_AIRPORTS) {
                        	break;
                        }
                    }
                    while(cursor.moveToNext());
                }
            }  
        }
        catch (Exception e) {
        }
        closes(cursor);
        
        return airports;
    }

    /**
     * Find coordinate of center tile.
     */
    public Coordinate getCoordinate(String name) {
            
        Cursor cursor;
        
        String types = TABLE_AIRPORTS;
        Coordinate c = null;

        String qry = "select * from " + types + " where " + LOCATION_ID_DB + "=='" + name + "';";
        cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                        
                        /*
                         * Put ID and name first
                         */
                    c = new Coordinate(
                            cursor.getDouble(LONGITUDE_COL),
                            cursor.getDouble(LATITUDE_COL));
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        return c;
    }
    
    /**
     * 
     */
    private StringPreference stringQuery(String name, String type, String table) {
        
        Cursor cursor;

        String qry = "select * from " + table + " where " + LOCATION_ID_DB + "=='" + name + "' and Type != 'VOT' limit 1;";
        /*
         * NAV
         */
        cursor = doQuery(qry, getMainDb());
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    StringPreference s = new StringPreference(type, "", name, cursor.getString(0));
                    closes(cursor);
                    return s;
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);

        return null;
        
    }





    public StringPreference getNavaidOrFixFromCoordinate(Coordinate c) {

        Cursor cursor;

        // Find FIX here first
        String qry = "select * from " + TABLE_FIX + " where " +
                "(" + LONGITUDE_DB + " - " + c.getLongitude() + ")*" +
                "(" + LONGITUDE_DB + " - " + c.getLongitude() + ")+" +
                "(" + LATITUDE_DB + " - " + c.getLatitude() + ")*" +
                "(" + LATITUDE_DB + " - " + c.getLatitude() + ")"  + " < 0.0000000001;";
        /*
         * NAV
         */
        cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    StringPreference s = new StringPreference(Destination.FIX, cursor.getString(3), cursor.getString(4), cursor.getString(0));
                    closes(cursor);
                    return s;
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);

        qry = "select * from " + TABLE_NAV + " where " +
                "(" + LONGITUDE_DB + " - " + c.getLongitude() + ")*" +
                "(" + LONGITUDE_DB + " - " + c.getLongitude() + ")+" +
                "(" + LATITUDE_DB + " - " + c.getLatitude() + ")*" +
                "(" + LATITUDE_DB + " - " + c.getLatitude() + ")"  + " < 0.0000000001 and (Type != 'VOT');";

        cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    StringPreference s = new StringPreference(Destination.NAVAID, cursor.getString(3), cursor.getString(4), cursor.getString(0));
                    closes(cursor);
                    return s;
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);

        return null;
    }

    /**
     * Search with I am feeling lucky. Best guess
     * @param name
     * @param params
     */
    public  StringPreference searchOne(String name) {
        
        if(null == name) {
            return null;
        }
        if(name.contains(" ")) {
            return null;            
        }
        
        int len = name.length();
        
        if(name.contains("&")) {
            /*
             * GPS
             */
            String c[] = name.split("&");
            if(c.length == 2) {
                try {
                    double lat = Double.parseDouble(c[0]);
                    double lon = Double.parseDouble(c[1]);
                    StringPreference s = new StringPreference(Destination.GPS, "GPS", name, 
                            Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon));
                    return s;
                }
                catch (Exception e) {                    
                }
            }
            return null;
        }
        
        /*
         * Length base preference of search
         */
        StringPreference s;
        switch(len) {
        
            case 0:
                return null;
            case 1:
            case 2:
            case 3:
                /*
                 * Search Nav, if not then
                 * Search airports
                 */
                 s = stringQuery(name, Destination.NAVAID, TABLE_NAV);
                 if(s != null) {
                     return s;
                 }
                 
                 return stringQuery(name, Destination.BASE, TABLE_AIRPORTS);                
            case 4:
                /*
                 * Search airport, if not then
                 * Search Nav, if not then
                 * Search Fix
                 */
                String name1 = name;
                if(name1.startsWith("K")) {
                    name1 = name.substring(1);
                }
                s = stringQuery(name1, Destination.BASE, TABLE_AIRPORTS);
                if(s != null) {
                    return s;
                }
                s = stringQuery(name, Destination.NAVAID, TABLE_NAV);
                if(s != null) {
                    return s;
                }
                s = stringQuery(name, Destination.FIX, TABLE_FIX);
                if(s != null) {
                    return s;
                }

                break;
                
            case 5:
                /*
                 * Search airport, if not then
                 * Search Fix
                 */
                name1 = name;
                if(name1.startsWith("K")) {
                    name1 = name.substring(1);
                }
                s = stringQuery(name1, Destination.BASE, TABLE_AIRPORTS);
                if(s != null) {
                    return s;
                }
                s = stringQuery(name, Destination.FIX, TABLE_FIX);
                if(s != null) {
                    return s;
                }

                break;
                
            case 6:
                s = stringQuery(name, Destination.FIX, TABLE_FIX);
                if(s != null) {
                    return s;
                }

                break;
                
            default:
                /*
                 * Radials
                 */
                return searchRadial(name);
        }
        
        return null;
    }

    /**
     * 
     * @param name
     * @param params
     */
    private StringPreference searchRadial(String name) {
        int len = name.length();
        Cursor cursor;
        /*
         * Of the form XXXRRRDDD like BOS270010
         */
        String chop = name.substring(len - 6);
        String chopname = name.substring(0, len - 6).toUpperCase(Locale.getDefault());
        if(chop.matches("[0-9][0-9][0-9][0-9][0-9][0-9]")) {

            String qry = "select * from " + TABLE_NAV + " where (" + LOCATION_ID_DB + "=='" + chopname + "') and Type != 'VOT';";
            cursor = doQuery(qry, getMainDb());
            
            try {
                if(cursor != null) {
                    if(cursor.moveToFirst()) {
                        
                        /*
                         * Put ID and name as if GPS
                         */                
                                                            
                                
                        double lon = cursor.getDouble(LONGITUDE_COL);                            
                        double lat = cursor.getDouble(LATITUDE_COL);
                        double distance = Double.parseDouble(chop.substring(3, 6));

                        /*
                         * Radials are magnetic
                         */
                        GeomagneticField gmf = new GeomagneticField((float)lat,
                                (float)lon, 0, System.currentTimeMillis());
                        double bearing = Double.parseDouble(chop.substring(0, 3)) + gmf.getDeclination();
                        Coordinate c = Radial.findCoordinate(lon, lat, distance, bearing);
                        StringPreference s = new StringPreference(Destination.GPS, "GPS", name, 
                                Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude()));
                        closes(cursor);
                        return s;
                    }
                    else {
                        
                        /*
                         * Did not find in NAV? Find in Fix
                         */
                        closes(cursor);

                        String qry2 = "select * from " + TABLE_FIX + " where " + LOCATION_ID_DB + "=='" + chopname + "';";
                        cursor = doQuery(qry2, getMainDb());

                        if(cursor != null) {
                            if(cursor.moveToFirst()) {
                                
                                /*
                                 * Put ID and name as if GPS
                                 */


                                double lon = cursor.getDouble(LONGITUDE_COL);                            
                                double lat = cursor.getDouble(LATITUDE_COL);
                                double distance = Double.parseDouble(chop.substring(3, 6));

                                /*
                                 * Radials are magnetic
                                 */
                                GeomagneticField gmf = new GeomagneticField((float)lat,
                                        (float)lon, 0, System.currentTimeMillis());
                                double bearing = Double.parseDouble(chop.substring(0, 3)) + gmf.getDeclination();
                                Coordinate c = Radial.findCoordinate(lon, lat, distance, bearing);
                                StringPreference s = new StringPreference(Destination.GPS, "GPS", name, 
                                        Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude()));
                                closes(cursor);
                                return s;
                            }
                        }

                    }
                }
            }
            catch (Exception e) {
            }
            
            closes(cursor);
            
        }
        
        return null;
    }

    /**
     * 
     * @param name
     * @param params
     */
    private void searchCity(String name, LinkedHashMap<String, String> params) {
        Cursor cursor;
        /*
         * City in upper case in DB
         */
        String uname = name.toUpperCase(Locale.getDefault());

        String qry = "select " + LOCATION_ID_DB + "," + FACILITY_NAME_DB + "," + TYPE_DB + " from " + TABLE_AIRPORTS + " where City=='" + uname + "';";
        cursor = doQuery(qry, getMainDb());

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
    }

    /**
     * Search something in database
     * @param name
     * @param params
     */
    public void search(String name, LinkedHashMap<String, String> params, boolean exact) {
        
        Cursor cursor;

        /*
         * This is a radial search?
         */
        int len = name.length();
        if(len > 6) {
            StringPreference s = searchRadial(name);
            if(null != s) {
                s.putInHash(params);
                return;
            }
        }
        
        // Search city first
        searchCity(name, params);
        
        String qry;
        String qbasic = "select " + LOCATION_ID_DB + "," + FACILITY_NAME_DB + "," + TYPE_DB + " from ";
        
        /*
         * We don't want to throw in too many results, but we also want to allow K as a prefix for airport names
         * If the user has typed enough, let's start looking for K prefixed airports as well
         */
        if(len > 2 && name.charAt(0) == 'K' || name.charAt(0) == 'k') {
        	
        	String qendK = "";
        	if(exact) {
                qendK = " (" + LOCATION_ID_DB + "=='" + name.substring(1) + "'" + ") order by " + LOCATION_ID_DB + " asc";
        	}
        	else {
        		qendK = " (" + LOCATION_ID_DB + " like '" + name.substring(1) + "%' " + ") order by " + LOCATION_ID_DB + " asc";
        	}
            qry = qbasic + TABLE_AIRPORTS + " where ";
            if(!mPref.isShowAllFacilities()) {
                qry += TYPE_DB + "=='AIRPORT' and ";
            }
            qry += qendK;
            cursor = doQuery(qry, getMainDb());
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
        }
        
        /*
         * All queries for airports, navaids, fixes
         */
        String qend = "";
        if(exact) {
            qend = " (" + LOCATION_ID_DB + "=='" + name + "'" + ") and Type != 'VOT' order by " + LOCATION_ID_DB + " asc";         	
        }
        else {
        	qend = " (" + LOCATION_ID_DB + " like '" + name + "%' " + ") and Type != 'VOT' order by " + LOCATION_ID_DB + " asc";
        }
        qry = qbasic + TABLE_NAV + " where " + qend;
        cursor = doQuery(qry, getMainDb());

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
        if(!mPref.isShowAllFacilities()) {
            qry += TYPE_DB + "=='AIRPORT' and ";
        }
        qry += qend;

        cursor = doQuery(qry, getMainDb());
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
        cursor = doQuery(qry, getMainDb());
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
    public void findDestination(String name, String type, String dbType, LinkedHashMap<String, String> params, LinkedList<Runway> runways, LinkedHashMap<String, String> freq, LinkedList<Awos> awos) {
        
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

        String qry = "select * from " + types + " where " + LOCATION_ID_DB + "=='" + name + "'";
        if(null != dbType && dbType.length() > 0) {
            if(false == dbType.equalsIgnoreCase("null")) {
                qry += " and " + TYPE_DB + "=='" + dbType + "'";
            }
        }
        // Order by type desc will cause VOR to be ahead of NDB if both are available.
        // This is a bit of a hack, but the user probably wants the VOR more than the NDB
        // Put our-ap in last
        qry += " and Type != 'VOT' order by " + TYPE_DB + "," + TYPE_DB + "='OUR-AP' " + "desc;";
        
        cursor = doQuery(qry, getMainDb());

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
                        String use = cursor.getString(5).trim();
                        if(use.equals("PU")) {
                            use = "PUBLIC";
                        }
                        else if(use.equals("PR")) {
                            use = "PRIVATE";                            
                        }
                        else  {
                            use = "MILITARY";                            
                        }
                        params.put("Use", use);
                        params.put("Manager", cursor.getString(7).trim());
                        params.put(MANAGER_PHONE, cursor.getString(8).trim());
                        params.put(ELEVATION, cursor.getString(9).trim());
                        String customs = cursor.getString(CUSTOMS_COL);
                        if(customs.equals("YN")) {
                            params.put(CUSTOMS, "Intl. Entry");
                        }
                        else if(customs.equals("NY")) {
                            params.put(CUSTOMS, "Lndg. Rights");
                        }
                        else if(customs.equals("YY")) {
                            params.put(CUSTOMS, "Lndg. Rights, Intl. Entry");
                        }
                        else {
                            params.put(CUSTOMS, mContext.getString(R.string.No));                            
                        }
                        String bcn = cursor.getString(BEACON_COL);
                        if(bcn.equals("")) {
                            bcn = mContext.getString(R.string.No);
                        }
                        params.put(BEACON, bcn);
                        String sc = cursor.getString(SEGCIRCLE_COL);
                        if(sc.equals("Y")) {
                            params.put(SEGCIRCLE, mContext.getString(R.string.Yes));
                        }
                        else {
                            params.put(SEGCIRCLE, mContext.getString(R.string.No));                            
                        }
                        String pa = cursor.getString(11).trim();
                        String paout = "";
                        if(pa.equals("")) {
                            try {
                                paout = "" + (Double.parseDouble(params.get(ELEVATION)) + 1000);
                            }
                            catch (Exception e) {
                                
                            }
                        }
                        else {
                            try {
                                paout = "" + (Double.parseDouble(params.get(ELEVATION)) + 
                                        (Double.parseDouble(pa)));
                            }
                            catch (Exception e) {
                                
                            }                            
                        }
                        params.put("Pattern Altitude", paout);
                        String fuel = cursor.getString(FUEL_TYPES_COL).trim();
                        if(fuel.equals("")) {
                            fuel = mContext.getString(R.string.No);
                        }
                        params.put(FUEL_TYPES, fuel);
                        String ct = cursor.getString(17).trim();
                        if(ct.equals("Y")) {
                            ct = mContext.getString(R.string.Yes);
                        }
                        else {
                            ct = mContext.getString(R.string.No);
                        }
                        params.put("Control Tower", ct);
                        
                        String unicom = cursor.getString(18).trim();
                        if(!unicom.equals("")) {
                            freq.put("UNICOM", unicom);
                        }
                        String ctaf = cursor.getString(19).trim();
                        if(!ctaf.equals("")) {
                            freq.put("CTAF", ctaf);
                        }
                        
                        String fee = cursor.getString(20).trim();
                        if(fee.equals("Y")) {
                            fee = mContext.getString(R.string.Yes);
                        }
                        else {
                            fee = mContext.getString(R.string.No);
                        }
                        params.put("Landing Fee", fee);
                        String fss = cursor.getString(FSSPHONE_COL);
						if (fss.equals("1-800-WX-BRIEF")) {
							fss = "1-800-992-7433";
						}
                        params.put(FSSPHONE, fss);

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
        
        /*
         * Find frequencies (ATIS, TOWER, GROUND, etc)  Not AWOS    
         */
        
        qry = "select * from " + TABLE_AIRPORT_FREQ + " where " + LOCATION_ID_DB + "=='" + name                            
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());

        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    String typeof = cursor.getString(1);
                    typeof = typeof.replace("LCL", "TWR");
                    /*
                     * Filter out silly frequencies
                     */
                    if(typeof.equals("EMERG") || typeof.contains("GATE") || typeof.equals("EMERGENCY")) {
                        continue;
                    }
                    /*
                     * Filter out UHF
                     */
                    try {
                        double frequency = Double.parseDouble(cursor.getString(2));
                        if(Helper.isFrequencyUHF(frequency)) {
                            continue;
                        }
                    }
                    catch (Exception e) {
                    }
                    
                    if(freq.containsKey(typeof)) {
                        /*
                         * Append this string to the existing one if duplicate key
                         */
                        freq.put(typeof, freq.get(typeof)+"\n\n"+cursor.getString(2));                                
                    }
                    else {
                        freq.put(typeof, cursor.getString(2));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        
		/*
		 * Get AWOS info
		 */

		qry = "select * from " + TABLE_AIRPORT_AWOS + " where "
				+ LOCATION_ID_DB + "=='" + name + "' or " + LOCATION_ID_DB
				+ "=='K" + name + "';";
		cursor = doQuery(qry, getMainDb());
		// 0     1    2          3  4  5    6     7     8    9    10
		// ident,type,commstatus,lt,ln,elev,freq1,freq2,tel1,tel2,remark
		try {
			/*
			 * Add each AWOS
			 */
			if (cursor != null) {
				while (cursor.moveToNext()) {

					Awos a = new Awos(cursor.getString(0)); // New AWOS instance

					a.setType(cursor.getString(1));

					a.setLat(Helper.removeLeadingZeros(cursor.getString(3)));
					a.setLon(Helper.removeLeadingZeros(cursor.getString(4)));
					a.setFreq1(cursor.getString(6));
					a.setFreq2(cursor.getString(7));
					a.setPhone1(cursor.getString(8));
					a.setPhone2(cursor.getString(9));
					a.setRemark(cursor.getString(10));

					awos.add(a);

				}
			}
		} catch (Exception e) {
		}
		closes(cursor);

        /*
         *Find runways        
         */

        qry = "select * from " + TABLE_AIRPORT_RUNWAYS + " where " + LOCATION_ID_DB + "=='" + name
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());
        
        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    
                    String Length = cursor.getString(1);
                    String Width = cursor.getString(2);
                    String Surface = cursor.getString(3);
                    String Variation = params.get(MAGNETIC_VARIATION);
                    
                    String run = Helper.removeLeadingZeros(cursor.getString(4));
                    String lat = Helper.removeLeadingZeros(cursor.getString(6));
                    String lon = Helper.removeLeadingZeros(cursor.getString(8));
                    
                    String Elevation = cursor.getString(10);
                    if(Elevation.equals("")) {
                        Elevation = params.get(ELEVATION);
                    }
                    String Heading = cursor.getString(12);
                    String DT = cursor.getString(14);
                    if(DT.equals("")) {
                        DT = "0";
                    }
                    String Lighted = cursor.getString(16);
                    if(Lighted.equals("0") || Lighted.equals("")) {
                        Lighted = mContext.getString(R.string.No);
                    }
                    String ILS = cursor.getString(18);
                    if(ILS.equals("")) {
                        ILS = mContext.getString(R.string.No);
                    }
                    String VGSI = cursor.getString(20);
                    if(VGSI.equals("")) {
                        VGSI = mContext.getString(R.string.No);
                    }
                    String Pattern = cursor.getString(22);
                    if(Pattern.equals("Y")) {
                        Pattern = "Right";
                    }
                    else {
                        Pattern = "Left";                        
                    }
                    
                    Runway r = new Runway(run);
                    r.setElevation(Elevation);
                    r.setHeading(Heading);
                    r.setSurface(Surface);
                    r.setLength(Length);
                    r.setWidth(Width);
                    r.setThreshold(DT);
                    r.setLights(Lighted);
                    r.setPattern(Pattern);
                    r.setLongitude(lon);
                    r.setLatitude(lat);
                    r.setVariation(Variation);
                    r.setILS(ILS);
                    r.setVGSI(VGSI);
                    
                    runways.add(r);
                   
                    /*
                     * If the first runway is a helipad, don't add a second end
                     */
					if(!(run.startsWith("H") || run.startsWith("h"))) {
						run = Helper.removeLeadingZeros(cursor.getString(5));
						lat = Helper.removeLeadingZeros(cursor.getString(7));
						lon = Helper.removeLeadingZeros(cursor.getString(9));

						Elevation = cursor.getString(11);
						if(Elevation.equals("")) {
							Elevation = params.get(ELEVATION);
						}
						Heading = cursor.getString(13);
						DT = cursor.getString(15);
						if(DT.equals("")) {
							DT = "0";
						}
						Lighted = cursor.getString(17);
						if(Lighted.equals("0") || Lighted.equals("")) {
							Lighted = mContext.getString(R.string.No);
						}
						ILS = cursor.getString(19);
						if(ILS.equals("")) {
							ILS = mContext.getString(R.string.No);
						}
						VGSI = cursor.getString(21);
						if(VGSI.equals("")) {
							VGSI = mContext.getString(R.string.No);
						}
						Pattern = cursor.getString(23);
						if(Pattern.equals("Y")) {
							Pattern = "Right";
						}else {
							Pattern = "Left";
						}

						r = new Runway(run);
						r.setElevation(Elevation);
						r.setHeading(Heading);
						r.setSurface(Surface);
						r.setLength(Length);
						r.setWidth(Width);
						r.setThreshold(DT);
						r.setLights(Lighted);
						r.setPattern(Pattern);
						r.setLongitude(lon);
						r.setLatitude(lat);
						r.setVariation(Variation);
						r.setILS(ILS);
						r.setVGSI(VGSI);

						runways.add(r);

					}
    
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);        
    }


    /**
     * Find all frequencies based on its name
     * @param name
     * @param params
     * @return
     */
    public LinkedList<String> findFrequencies(String name) {
        
        Cursor cursor;
        LinkedList<String> freq = new LinkedList<String>();
        
        /*
         * Find frequencies (ATIS, TOWER, GROUND, etc)  Not AWOS    
         */
        
        String qry = "select * from " + TABLE_AIRPORT_FREQ + " where " + LOCATION_ID_DB + "=='" + name                            
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());

        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    String typeof = cursor.getString(1);
                    typeof = typeof.replace("LCL", "TWR");
                    /*
                     * Filter out silly frequencies
                     */
                    if(typeof.equals("EMERG") || typeof.contains("GATE") || typeof.equals("EMERGENCY")) {
                        continue;
                    }
                    /*
                     * Filter out UHF
                     */
                    try {
                        double frequency = Double.parseDouble(cursor.getString(2));
                        if(Helper.isFrequencyUHF(frequency)) {
                            continue;
                        }
                    }
                    catch (Exception e) {
                    }
                    
                    freq.add(typeof + " " + cursor.getString(2));
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        
		/*
		 * Get AWOS info
		 */

		qry = "select * from " + TABLE_AIRPORT_AWOS + " where "
				+ LOCATION_ID_DB + "=='" + name + "' or " + LOCATION_ID_DB
				+ "=='K" + name + "';";
		cursor = doQuery(qry, getMainDb());
		// 0     1    2          3  4  5    6     7     8    9    10
		// ident,type,commstatus,lt,ln,elev,freq1,freq2,tel1,tel2,remark
		try {
			/*
			 * Add each AWOS
			 */
			if (cursor != null) {
				while (cursor.moveToNext()) {
					String typeof = cursor.getString(1);
					/*
					 * Filter out UHF
					 */
					try {
						double frequency = Double.parseDouble(cursor
								.getString(6));
                        if(Helper.isFrequencyUHF(frequency)) {
                            continue;
                        }
					} catch (Exception e) {
					    continue;
					}

					freq.add(typeof + " " + cursor.getString(6));
				}
			}
		} catch (Exception e) {
		}
		closes(cursor);
		/*
		 * Get CTAF and UNICOM info
		 */
		qry = "select * from " + TABLE_AIRPORTS + " where " + LOCATION_ID_DB
				+ "=='" + name + "' or " + LOCATION_ID_DB + "=='K" + name
				+ "';";
		cursor = doQuery(qry, getMainDb());

		try {
			if (cursor != null) {
				if (cursor.moveToFirst()) {

					String unicomfreq = cursor.getString(18).trim();
					String ctaffreq = cursor.getString(19).trim();

					String typeof = "UNICOM";
					if (!unicomfreq.equals("")) {
						try {
							/*
							 * Filter out UHF
							 */
							double frequency = Double.parseDouble(unicomfreq);
	                        if(!Helper.isFrequencyUHF(frequency)) {
								freq.add(typeof + " " + unicomfreq);
							}
						} catch (Exception e) {
						}

					}
					typeof = "CTAF";
					if (!ctaffreq.equals("")) {
						try {
							/*
							 * Filter out UHF
							 */
							double frequency = Double.parseDouble(ctaffreq);
	                        if(!Helper.isFrequencyUHF(frequency)) {
								freq.add(typeof + " " + ctaffreq);
							}
						} catch (Exception e) {
						}

					}
				}
			}
		} catch (Exception e) {
		}

		closes(cursor);
		
        
        return freq;
    }

    /**
     * Find all runways based on its name
     * @param name
     * @param params
     * @return
     */
    public LinkedList<String> findRunways(String name) {
        
        Cursor cursor;
        LinkedList<String> run = new LinkedList<String>();
        
        /*
         * Find frequencies (ATIS, TOWER, GROUND, etc)  Not AWOS    
         */
        
        String qry = "select * from " + TABLE_AIRPORT_RUNWAYS + " where " + LOCATION_ID_DB + "=='" + name                            
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());

        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    // return ident and true heading of LE runway
                    if(cursor.getString(4).contains("H")) {
                        // No heliport
                        continue;
                    }
                    String trueh = cursor.getString(4) + "," + cursor.getString(12);
                    run.add(trueh);
                    trueh = cursor.getString(5) + "," + cursor.getString(13);
                    run.add(trueh);
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        
        return run;
    }

    /**
     * Find runway coordinate on its name, and airport name
     * @param name
     * @param airport
     * @param params
     * @return
     */
    public Coordinate findRunwayCoordinates(String name, String airport) {

        Cursor cursor;
        Coordinate c = null;

        String qry = "select * from " + TABLE_AIRPORT_RUNWAYS + " where (" + LOCATION_ID_DB + "=='" + airport
                + "' or " + LOCATION_ID_DB + "=='K" + airport + "') and (LEIdent=='" + name + "' or HEIdent=='" + name + "');";
        cursor = doQuery(qry, getMainDb());

        try {
            /*
             */
            if(cursor != null) {
                if(cursor.moveToNext()) {
                    if(cursor.getString(4).equals(name)) { //LE
                        c = new Coordinate(cursor.getDouble(8), cursor.getDouble(6));
                    }
                    else if(cursor.getString(5).equals(name)) { //HE
                        c = new Coordinate(cursor.getDouble(9), cursor.getDouble(7));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);

        return c;
    }


    /**
     * Find elevation based on its name
     * @param name
     * @param params
     * @return
     */
    public String findElev(String name) {
        
        String elev = "";
        Cursor cursor;
        
        /*
         * Find frequencies (ATIS, TOWER, GROUND, etc)  Not AWOS    
         */
        
        String qry = "select ARPElevation from " + TABLE_AIRPORTS + " where " + LOCATION_ID_DB + "=='" + name                            
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());

        try {
            /*
             * Add all of them
             */
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    elev = cursor.getString(0);
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        return elev;
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
        double corrFactor = Math.pow(Math.cos(Math.toRadians(lat)),2);
        String asDist = ", ((" + LONGITUDE_DB + " - " + lon + ") * (" + LONGITUDE_DB  + " - " + lon + ") * " + corrFactor + " + "
                + " (" + LATITUDE_DB + " - " + lat + ") * (" + LATITUDE_DB + " - " + lat + ")"
                + ") as dist";
        String qry = "select " + LOCATION_ID_DB + asDist + " from " + TABLE_AIRPORTS;
        if(!mPref.isShowAllFacilities()) {
            qry +=  " where " + TYPE_DB + "=='AIRPORT' and ";
        }
        else {
            qry += " where ";
        }

        qry += "dist < " + Preferences.MIN_TOUCH_MOVEMENT_SQ_DISTANCE + " order by dist limit 1;";
        
        Cursor cursor = doQuery(qry, getMainDb());
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
     * @return
     */
    public String getSua(double lon, double lat) {

        /*
         * Find with sqlite query
         */
        String qry = "select * from " + TABLE_SUA + " where ((";
        qry += "(" + "lon" + " - " + lon + ") * (" + "lon"  + " - " + lon + ") + "
                + "(" + "lat" + " - " + lat + ") * (" + "lat" + " - " + lat + ")"
                + ") < 1);";
        
        Cursor cursor = doQuery(qry, getMainDb());
        String ret = "";

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    String sua = 
                            cursor.getString(0) + "(" + cursor.getString(1) + ")\n" + 
                            cursor.getString(3) + " to " + cursor.getString(2) + "\n"; 
                    String freqtx = cursor.getString(10);
                    if(!freqtx.equals("")) {
                        sua += "TX " + freqtx + "\n";
                    }
                    String freqrx = cursor.getString(11);
                    if(!freqrx.equals("")) {
                        sua += "RX " + freqrx + "\n";
                    }
                    
                    sua += "NOTE " + cursor.getString(9) + "\n";

                    ret += sua + "\n";
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        if(ret.equals("")) {
            ret = null;
        }
        return ret;
    }

    public String findObstacle(String height, Destination dest) {

        String ret = null;
        if(null == dest) {
            return ret;
        }
        double lon = dest.getLocation().getLongitude();
        double lat = dest.getLocation().getLatitude();
        
        /*
         * Find with sqlite query
         */
        String qry = "select * from " + TABLE_OBSTACLES + " where Height =='" + height + "' and " + 
                "(" + LATITUDE_DB  + " > " + (lat - Obstacle.RADIUS) + ") and (" + LATITUDE_DB  + " < " + (lat + Obstacle.RADIUS) + ") and " +
                "(" + LONGITUDE_DB + " > " + (lon - Obstacle.RADIUS) + ") and (" + LONGITUDE_DB + " < " + (lon + Obstacle.RADIUS) + ");";
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    ret = new String(cursor.getString(1) + "," + cursor.getString(0));
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        return ret;
    }

    /**
     * Find the lat/lon of an airport
     * @param name
     * @param type
     * @return
     */
    public String findLonLat(String name, String type) {

        String table = null;
        if(type.equals(Destination.BASE)) {
            table = TABLE_AIRPORTS;
        }
        else if(type.equals(Destination.NAVAID)) {
            table = TABLE_NAV;
        }
        else if(type.equals(Destination.FIX)) {
            table = TABLE_FIX;
        }
        
        if(null == table) {
            return null;
        }
        
        /*
         * Find with sqlite query
         */
        String qry = "select * from " + table + 
                " where " + LOCATION_ID_DB + "=='" + name + "';";
        Cursor cursor = doQuery(qry, getMainDb());
        String ret = null;

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    
                    ret = new String(cursor.getString(LONGITUDE_COL) + "," + cursor.getString(LATITUDE_COL));
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        return ret;
    }

    /**
     * Find the lat/lon of an array of airports, and update in the objects
     * @param metars
     * @return
     */
    public void findLonLatMetar(HashMap<String, Metar> metars) {

        String name = "";
        Set<String> keys = metars.keySet();
        for (String k : keys) {
            // Make a long query instead of several long queries
            name += LOCATION_ID_DB + "=='" + k + "' or ";
        }
        name += LOCATION_ID_DB + "=='BOS'"; // Dummy

        /*
         * Find with sqlite query
         */
        String qry = "select * from " + TABLE_AIRPORTS +
                " where " + TABLE_AIRPORTS + "." + TYPE_DB + "=='AIRPORT' and (" + name + ");";
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    // populate the metar objects with lon/lat
                    double lon = cursor.getDouble(LONGITUDE_COL);
                    double lat = cursor.getDouble(LATITUDE_COL);
                    String id = cursor.getString(LOCATION_ID_COL);
                    Metar m = metars.get(id);
                    if(m != null) {
                        m.lat = lat;
                        m.lon = lon;
                    }
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
    }

    /**
     * Search Minimums plates for this airport
     * @param airportId
     * @return Minimums
     */
    public String[] findMinimums(String airportId) {
        
        String ret2[] = new String[2];
        String ret[] = new String[1];
        
        /*
         * Silly that FAA gives K and P for some airports as ICAO
         */
        String qry = "select File from " + TABLE_ALT + " where " + LOCATION_ID_DB + "==" + "'" + airportId + "'" +
                " or " + LOCATION_ID_DB + "==" + "'K" + airportId + "'" +
                " or " + LOCATION_ID_DB + "==" + "'P" + airportId + "'";
        
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToNext()) {
                    ret2[0] = cursor.getString(0);
                    ret[0] = ret2[0];
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);

        qry = "select File from " + TABLE_TO + " where " + LOCATION_ID_DB + "==" + "'" + airportId + "'" +
                " or " + LOCATION_ID_DB + "==" + "'K" + airportId + "'" +
                " or " + LOCATION_ID_DB + "==" + "'P" + airportId + "'";
        
        cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToNext()) {
                    ret2[1] = cursor.getString(0);
                    ret[0] = ret2[1];
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);

        /*
         * Only return approp sized array
         */
        if(ret[0] == null) {
            return null;
        }
        else if(ret2[0] == null || ret2[1] == null) {
            return ret;
        }
        
        return ret2;
    }

    
    /**
     * Search A/FD name for this airport
     * @param airportId
     * @return A/FD
     */
    public LinkedList<String> findAFD(String airportId) {

        LinkedList<String> ret = new LinkedList<String>();
        String qry = "select File from " + TABLE_AFD + " where " + LOCATION_ID_DB + "==" + "'" + airportId + "'";
        
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    ret.add(cursor.getString(0));
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
     * @param lon
     * @param lat
     * @param height
     * @return
     */
    public LinkedList<Obstacle> findObstacles(double lon, double lat, int height) {
        
        LinkedList<Obstacle> list = new LinkedList<Obstacle>();
        
        String qry = "select * from " + TABLE_OBSTACLES + " where (Height > " + (height - (int)Obstacle.HEIGHT_BELOW) + ") and " +
                "(" + LATITUDE_DB  + " > " + (lat - Obstacle.RADIUS) + ") and (" + LATITUDE_DB  + " < " + (lat + Obstacle.RADIUS) + ") and " +
                "(" + LONGITUDE_DB + " > " + (lon - Obstacle.RADIUS) + ") and (" + LONGITUDE_DB + " < " + (lon + Obstacle.RADIUS) + ");";
        /*
         * Find obstacles at below or higher in lon/lat radius
         * We ignore all obstacles 500 AGL below in our script
         */
        Cursor cursor = doQuery(qry, getMainDb());
        
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    list.add(new Obstacle(cursor.getFloat(1), cursor.getFloat(0), (int)cursor.getFloat(2)));
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);
        return list;
    }


    /**
     * 
     * @return
     */
    private String getWeatherDb() {
        return "weather.db";
    }

    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryWeather(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersWeather) {
            if(mDataBaseWeather == null) {
                mUsersWeather = 0;
                try {
                    
                    mDataBaseWeather = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseWeather = null;
                }
            }
            if(mDataBaseWeather == null) {
                return c;
            }
            mUsersWeather++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseWeather == null) {
            return c;
        }
        
        if(!mDataBaseWeather.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseWeather.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    /**
     * Close database
     */
    private void closesWeather(Cursor c) {

        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersWeather) {
            mUsersWeather--;
            if((mDataBaseWeather != null) && (mUsersWeather <= 0)) {
                try {
                    mDataBaseWeather.close();
                }
                catch (Exception e) {
                }
                mDataBaseWeather = null;
                mUsersWeather = 0;
            }
        }
    }


    /**
     * 
     * @param station
     * @return
     */
    public Taf getTAF(String station) {
      
        Taf taf = null;
        String qry =
                "select * from tafs where station_id='K" + station + "';";
        
        Cursor cursor = doQueryWeather(qry, getWeatherDb());
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {

                    taf = new Taf();
                    taf.rawText = cursor.getString(0);
                    taf.time = cursor.getString(1);
                    taf.stationId = cursor.getString(2);
                }
            }
        }
        catch (Exception e) {
        }
        
        closesWeather(cursor);
        return taf;        
    }

    /**
     * 
     * @param station
     * @return
     */
    public Metar getMETAR(String station) {
      
        Metar metar = null;
        String qry =
                "select * from metars where station_id='K" + station + "';";
        
        Cursor cursor = doQueryWeather(qry, getWeatherDb());
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {

                    metar = new Metar();
                    metar.rawText = cursor.getString(0);
                    metar.time = cursor.getString(1);
                    metar.stationId = cursor.getString(2);
                    metar.flightCategory = cursor.getString(3);
                }
            }
        }
        catch (Exception e) {
        }
        
        closesWeather(cursor);
        return metar;        
    }

    /**
     *  Return metar closest to the input coordinates using query on weather.metars table
     *  We query in a bounded 100mi wide/tall lat/lon box then sort based on distance from the input
     *  see also http://stackoverflow.com/questions/3695224/sqlite-getting-nearest-locations-with-latitude-and-longitude#
     * @param lat of the central point
     * @param lon of the central point
     * @return
     */
    public Metar getClosestMETAR(Double lat, Double lon) {

        Metar metar = null;
        final int searchRadius = mPref.getClosestMetarSearchRadius();
        if( searchRadius == 0 ) return null;
        SquareBoxSearchHelper squareBoxSearchHelper = new SquareBoxSearchHelper(lat, lon, searchRadius);

        String qry =
                "select * from metars where 1=1 "
                        + squareBoxSearchHelper.getWhereClause("latitude", "longitude")
                        +";";

        Cursor cursor = doQueryWeather(qry, getWeatherDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {

                    metar = new Metar();
                    metar.rawText = cursor.getString(0);
                    metar.time = cursor.getString(1);
                    metar.stationId = cursor.getString(2);
                    metar.flightCategory = cursor.getString(3);
                    if (!cursor.isNull(4) && !cursor.isNull(5)) {
                        metar.distance = Projection.getStaticDistance(lon, lat, cursor.getDouble(4), cursor.getDouble(5));

                        GeomagneticField gmf = new GeomagneticField(lat.floatValue(), lon.floatValue(), 0, System.currentTimeMillis());
                        Projection p = new Projection(cursor.getDouble(4), cursor.getDouble(5), lon, lat);
                        metar.position =  Math.round(p.getDistance()) + Preferences.distanceConversionUnit + " " +
                                p.getGeneralDirectionFrom(-gmf.getDeclination());
                    }
                }
            }
        }
        catch (Exception e) {
        }

        closesWeather(cursor);
        return metar;
    }

    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public WindsAloft getWindsAloft(double lon, double lat) {
      
        WindsAloft wa = null;
        String qry =
                "select * from wa order by " +
                "((longitude - " + lon + ")*" + "(longitude - " + lon + ") + " +    
                "(latitude - " + lat + ")*" + "(latitude - " + lat + ")) limit 1;";

        Cursor cursor = doQueryWeather(qry, getWeatherDb());
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {

                    wa = new WindsAloft();
                    wa.station = cursor.getString(0);
                    wa.time = cursor.getString(1);
                    wa.lon = cursor.getFloat(2);
                    wa.lat = cursor.getFloat(3);
                    wa.w3k = cursor.getString(4).replaceAll("[ ]", "");
                    wa.w6k = cursor.getString(5).replaceAll("[ ]", "");
                    wa.w9k = cursor.getString(6).replaceAll("[ ]", "");
                    wa.w12k = cursor.getString(7).replaceAll("[ ]", "");
                    wa.w18k = cursor.getString(8).replaceAll("[ ]", "");
                    wa.w24k = cursor.getString(9).replaceAll("[ ]", "");
                    wa.w30k = cursor.getString(10).replaceAll("[ ]", "");
                    wa.w34k = cursor.getString(11).replaceAll("[ ]", "");
                    wa.w39k = cursor.getString(12).replaceAll("[ ]", "");
                }
            }
        }
        catch (Exception e) {
        }
        
        closesWeather(cursor);
        return wa;        
    }

    /**
     * 
     * @param station
     * @return
     */
    public LinkedList<Airep> getAireps(double lon, double lat) {

        LinkedList<Airep> airep = new LinkedList<Airep>();

        /*
         * All aireps/pireps sep by \n
         */
        
        String qry =
                "select * from apirep where " +                
                "(" + "latitude"  + " > " + (lat - Airep.RADIUS) + ") and (" + "latitude"  + " < " + (lat + Airep.RADIUS) + ") and " +
                "(" + "longitude" + " > " + (lon - Airep.RADIUS) + ") and (" + "longitude" + " < " + (lon + Airep.RADIUS) + ");";
     
        Cursor cursor = doQueryWeather(qry, getWeatherDb());
        
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    Airep a = new Airep();
                    a.rawText = cursor.getString(0);
                    a.time = cursor.getString(1);
                    a.lon = cursor.getFloat(2);
                    a.lat = cursor.getFloat(3);
                    a.reportType = cursor.getString(4);
                    airep.add(a);
                }
            }
        }
        catch (Exception e) {
        }
        
        closesWeather(cursor);
        return airep;
    }

    
    /**
     * 
     * @param station
     * @return
     */
    public LinkedList<AirSigMet> getAirSigMets() {

        LinkedList<AirSigMet> airsig = new LinkedList<AirSigMet>();
        
        /*
         * Get all
         */
        String qry =
                "select * from airsig"; 
     
        Cursor cursor = doQueryWeather(qry, getWeatherDb());
        
        try {
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    AirSigMet a = new AirSigMet();
                    a.rawText = cursor.getString(0);
                    a.timeFrom = cursor.getString(1);
                    a.timeTo = cursor.getString(2);
                    a.points = cursor.getString(3);
                    a.minFt = cursor.getString(4);
                    a.maxFt = cursor.getString(5);
                    a.movementDeg = cursor.getString(6);
                    a.movementKt = cursor.getString(7);
                    a.hazard = cursor.getString(8);
                    a.severity = cursor.getString(9);
                    a.reportType = cursor.getString(10);
                    airsig.add(a);
                }
            }
        }
        catch (Exception e) {
        }
        
        closesWeather(cursor);
        return airsig;
    }
    
    

    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryProcedures(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersProcedures) {
            if(mDataBaseProcedures == null) {
                mUsersProcedures = 0;
                try {
                    
                    mDataBaseProcedures = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseProcedures = null;
                }
            }
            if(mDataBaseProcedures == null) {
                return c;
            }
            mUsersProcedures++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseProcedures == null) {
            return c;
        }
        
        if(!mDataBaseProcedures.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseProcedures.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    /**
     * Close database
     */
    private void closesProcedures(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersProcedures) {
            mUsersProcedures--;
            if((mDataBaseProcedures != null) && (mUsersProcedures <= 0)) {
                try {
                    mDataBaseProcedures.close();
                }
                catch (Exception e) {
                }
                mDataBaseProcedures = null;
                mUsersProcedures = 0;
            }
        }
    }


    /**
     * 
     * @param name
     * @param approach
     * @return
     */
    public LinkedList<Cifp> findProcedure(String name, String approach) {

        TreeMap<String, Cifp> map = new TreeMap<String, Cifp>();
        String params[] = Cifp.getParams(approach);
        if(params[0] == null || params[1] == null) {
            return new LinkedList<Cifp>();
        }

        // get runway matched to CIFP database

        String qry =
                "select * from " + TABLE_PROCEDURE + " where (Airport='" + name + "' or Airport='K" + name +
                "') and AppType='" + params[0] + "' and runway like'%"  + params[1]  + "%';";

        Cursor cursor = doQueryProcedures(qry, "procedures.db");

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {

                        /*
                         * Add as inital course, initial alts, final course, final alts, missed course, missed alts
                         */
                        Cifp cifp = new Cifp(name, cursor.getString(4), cursor.getString(5), cursor.getString(6),
                                cursor.getString(7), cursor.getString(8), cursor.getString(9));
                        map.put(cifp.getInitialCourse(), cifp);
                    } while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        
        closesProcedures(cursor);
        
        return new LinkedList<Cifp>(map.values());
    }
    
    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryPlates(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersPlates) {
            if(mDataBasePlates == null) {
                mUsersPlates = 0;
                try {
                    
                    mDataBasePlates = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBasePlates = null;
                }
            }
            if(mDataBasePlates == null) {
                return c;
            }
            mUsersPlates++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBasePlates == null) {
            return c;
        }
        
        if(!mDataBasePlates.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBasePlates.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    /**
     * Close database
     */
    private void closesPlates(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersPlates) {
            mUsersPlates--;
            if((mDataBasePlates != null) && (mUsersPlates <= 0)) {
                try {
                    mDataBasePlates.close();
                }
                catch (Exception e) {
                }
                mDataBasePlates = null;
                mUsersPlates = 0;
            }
        }
    }

    
    /**
     * 
     * @param name
     * @return
     */
    public HashMap<String, float[]> findPlatesMatrix(String name) {
        
        HashMap<String, float[]> ret = new HashMap<String, float[]>();
        
        String qry =
                "select * from VisionFix" + " where AirportID='" + name + "';";
        
        Cursor cursor = doQueryPlates(qry, "geoplates.db");
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        
                        /*
                         * Add to hash table, make transpose matrix from points
                         */
                        String plate = cursor.getString(0);
                        plate = plate.substring(0, plate.lastIndexOf('.'));

                        float x1 = cursor.getFloat(2);
                        float y1 = cursor.getFloat(3);
                        float lat1 = cursor.getFloat(4);
                        float lon1 = cursor.getFloat(5);
                        float x2 = cursor.getFloat(6);
                        float y2 = cursor.getFloat(7);
                        float lat2 = cursor.getFloat(8);
                        float lon2 = cursor.getFloat(9);
                        
                        /*
                         * Math to find px/py from two points on the plate
                         */
                        float px = (x1 - x2) / (lon1 - lon2);
                        float py = (y1 - y2) / (lat1 - lat2); 
                        
                        float array[] = new float[12];
                        array[0] = lon1 - x1 / px;
                        array[1] = px;
                        array[2] = lat1 - y1 / py;
                        array[3] = py;
                        
                        ret.put(plate, array);
         
                    } while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        
        closesPlates(cursor);
        
        if(ret.size() > 0) {
            return ret;      
        }
        
        return null;
    }

    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryGeoPlates(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersGeoPlates) {
            if(mDataBaseGeoPlates == null) {
                mUsersGeoPlates = 0;
                try {
                    
                    mDataBaseGeoPlates = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseGeoPlates = null;
                }
            }
            if(mDataBaseGeoPlates == null) {
                return c;
            }
            mUsersGeoPlates++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseGeoPlates == null) {
            return c;
        }
        
        if(!mDataBaseGeoPlates.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseGeoPlates.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    
    /**
     * Close database
     */
    private void closesGeoPlates(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersGeoPlates) {
            mUsersGeoPlates--;
            if((mDataBaseGeoPlates != null) && (mUsersGeoPlates <= 0)) {
                try {
                    mDataBaseGeoPlates.close();
                }
                
                
                catch (Exception e) {
                }
                mDataBaseGeoPlates = null;
                mUsersGeoPlates = 0;
            }
        }
    }

    /**
     * 
     * @param name
     * @return
     */
    public float[] findGeoPlateMatrix(String name) {
        float ret[] = new float[4];
        boolean found = false;
        
        String qry = "select * from " + TABLE_GEOPLATES + " where " + PROC + "=='" + name +"'";
        Cursor cursor = doQueryGeoPlates(qry, "geoplates.db");
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
        
                    /*
                     * Database
                     */
                    ret[0] = cursor.getFloat(1);
                    ret[1] = cursor.getFloat(2);
                    ret[2] = cursor.getFloat(3);
                    ret[3] = cursor.getFloat(4);
                    found = true;
                }
            }
        }
        catch (Exception e) {
        }
        closesGeoPlates(cursor);

        if(found == false) {
            return null;
        }
        
        return ret;
    }

    /**
     * 
     * @param lat
     * @param lon
     * @return
     */
    public Vector<NavAid> findNavaidsNearby(Double lat, Double lon) {

        final Double NAVAID_SEARCH_RADIUS = 150.; // 150 seems reasonable, it is VOR Line Of Sight at 18000
        SquareBoxSearchHelper squareBoxSearchHelper = new SquareBoxSearchHelper(lat, lon, NAVAID_SEARCH_RADIUS);
        String qry = "select * from " + TABLE_NAV
                + " where Type == 'VOR' or type == 'VOR/DME' or type == 'VORTAC' "
                + squareBoxSearchHelper.getWhereClause("ARPlatitude", "ARPlongitude")
                +" limit 4;"; // we need 2 coordinates for a fix; get 3 in case we hit NDB

	    Cursor cursor = doQuery(qry, getMainDb());

        Vector result = null;
        try {
	        if(cursor != null) {
	            if(cursor.moveToFirst()) {
                    // proper display of navaid radials requires historical magnetic variation
                    if (cursor.getColumnCount() > NAVAID_MAGNETIC_VARIATION_COL) {
                        result = new Vector<>();
                        do {
                            String locationId = cursor.getString(LOCATION_ID_COL);
                            Coordinate coord = new Coordinate(cursor.getFloat(LONGITUDE_COL), cursor.getFloat(LATITUDE_COL));
                            String name = cursor.getString(FACILITY_NAME_COL);
                            String type = cursor.getString(TYPE_COL);

                            int variation = cursor.getInt(NAVAID_MAGNETIC_VARIATION_COL);
                            String navaidClass = cursor.getString(NAVAID_CLASS_COL);
                            String hiwas = cursor.getString(NAVAID_HIWAS_COL);
                            boolean hasHiwas = hiwas.equals("Y");
                            String elevationString = cursor.getString(NAVAID_ELEVATION_COL);
                            double elevation = elevationString.isEmpty() ? 0 : Double.parseDouble(elevationString);

                            result.add(new NavAid(locationId, type, name, coord, variation, navaidClass, hasHiwas, elevation));

                        } while (cursor.moveToNext());
                    }
	            }
	        }
	    }
	    catch (Exception e) {
	    }

	    closes(cursor);

	    return result;
    }


    /**
     *
     * @param name
     * @return
     */
    public Coordinate findNavaid(String name) {
        Coordinate coord = null;
        String qry = "select * from " + TABLE_NAV + " where " + LOCATION_ID_DB + "=='" + name + "' and Type != 'VOT' limit 1;";
	    /*
	     * NAV
	     */
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    coord = new Coordinate(cursor.getFloat(LONGITUDE_COL), cursor.getFloat(LATITUDE_COL));
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);

        if(null != coord) {
            return coord;
        }

        qry = "select * from " + TABLE_FIX + " where " + LOCATION_ID_DB + "=='" + name + "' limit 1;";
	    /*
	     * Fix
	     */
        cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    coord = new Coordinate(cursor.getFloat(LONGITUDE_COL), cursor.getFloat(LATITUDE_COL));
                }
            }
        }
        catch (Exception e) {
        }

        closes(cursor);

        return coord;
    }


    /**
     * Find all coordinates of a airway
     * @param name
     * @return
     */
	public LinkedList<Coordinate> findAirway(String name) {
        LinkedList<Coordinate> points = new LinkedList<Coordinate>();
        
        /*
         * Limit to airports taken by array airports
         */
        String qry = "select * from " + TABLE_AIRWAYS + " where name='" + name + "'" +
        		" order by cast(sequence as integer)";
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                    	float latitude = cursor.getFloat(2);
                    	float longitude = cursor.getFloat(3);
                    	points.add(new Coordinate(longitude, latitude));
                    }
                    while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        closes(cursor);
        
        return points;
	}

    
    /**
     *
     * @param statement
     * @return
     */
    private Cursor doQueryGameTFRs(String statement, String name) {
        Cursor c = null;

        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         *
         */
        synchronized(mUsersGameTFRs) {
            if(mDataBaseGameTFRs == null) {
                mUsersGameTFRs = 0;
                try {

                    mDataBaseGameTFRs = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY |
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseGameTFRs = null;
                }
            }
            if(mDataBaseGameTFRs == null) {
                return c;
            }
            mUsersGameTFRs++;
        }

        /*
         * In case we fail
         */

        if(mDataBaseGameTFRs == null) {
            return c;
        }

        if(!mDataBaseGameTFRs.isOpen()) {
            return c;
        }

        /*
         * Find with sqlite query
         */
        try {
            c = mDataBaseGameTFRs.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }


    /**
     * Close database
     */
    private void closesGameTFRs(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {

        }

        synchronized(mUsersGameTFRs) {
            mUsersGameTFRs--;
            if((mDataBaseGameTFRs != null) && (mUsersGameTFRs <= 0)) {
                try {
                    mDataBaseGameTFRs.close();
                }


                catch (Exception e) {
                }
                mDataBaseGameTFRs = null;
                mUsersGameTFRs = 0;
            }
        }
    }


    public LinkedList<LabelCoordinate> findGameTFRs() {
        LinkedList<LabelCoordinate> ret = new LinkedList<LabelCoordinate>();

        // Find -6 hours to +12 hours
        Calendar begin = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        begin.add(Calendar.HOUR_OF_DAY, -6);
        end.add(Calendar.HOUR_OF_DAY, 12);


        // Game TFRs in EST
        SimpleDateFormat formatterIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatterIso.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        String bS = formatterIso.format(begin.getTime());
        String eS = formatterIso.format(end.getTime());

        SimpleDateFormat formatterZulu = new SimpleDateFormat("ddHH:mm'Z'");
        formatterZulu.setTimeZone(TimeZone.getTimeZone("GMT"));


        String qry = "select * from " + TABLE_GAME + " where effective between '" + bS +  "' and '"  + eS + "'";



        Cursor cursor = doQueryGameTFRs(qry, "gametfr.db");

        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        String date = cursor.getString(0);
                        Date effective = formatterIso.parse(date);
                        // print in zulu
                        String toprint = formatterZulu.format(effective);

                        LabelCoordinate c = new LabelCoordinate(cursor.getFloat(3), cursor.getFloat(2), toprint + " " + cursor.getString(1));
                        ret.add(c);
                    }
                    while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        closesGameTFRs(cursor);
        return ret;
    }

    private class SquareBoxSearchHelper {
        private double lat, lon;
        private Coordinate top;
        private Coordinate bottom;
        private Coordinate left;
        private Coordinate right;
        private double fudge;

        public SquareBoxSearchHelper(double lat, double lon, double search_radius) {
            this.lat = lat; this.lon = lon;
            top = Projection.findStaticPoint(lon, lat, 0, search_radius);
            bottom = Projection.findStaticPoint(lon, lat, 180, search_radius);
            left = Projection.findStaticPoint(lon, lat, 270, search_radius);
            right = Projection.findStaticPoint(lon, lat, 90, search_radius);
            fudge = Math.pow(Math.cos(Math.toRadians(lat)), 2);
        }

        public String getWhereClause(String latField, String lonField) {
            return " and "+latField+" < "+top.getLatitude()+
                   " and "+latField+" > "+bottom.getLatitude()+
                   " and "+lonField+" < "+right.getLongitude()+
                   " and "+lonField+" > "+left.getLongitude()+
                   " order by (("    +lat+" - "+latField+") * ("+lat+" - "+latField
                             +") + ("+lon+" - "+lonField+") * ("+lon+" - "+lonField
                             +") * "+fudge+")";
        }

    }
}
