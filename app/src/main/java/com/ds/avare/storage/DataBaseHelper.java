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


import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;

import com.ds.avare.R;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Radial;
import com.ds.avare.shapes.Tile;
import com.ds.avare.utils.Helper;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.GeomagneticField;

/**
 * @author zkhan, jlmcgraw
 * The class that does the grunt wortk of dealing with the databse
 */
public class DataBaseHelper  {

    /**
     * Cache this class to sqlite
     */
    private SQLiteDatabase mDataBase; 
    private SQLiteDatabase mDataBaseFiles; 
    private SQLiteDatabase mDataBaseElev; 
    private SQLiteDatabase mDataBaseProcedures; 
    private SQLiteDatabase mDataBasePlates; 
    private SQLiteDatabase mDataBaseGeoPlates; 
    private SQLiteDatabase mDataBaseFuel; 
    private SQLiteDatabase mDataBaseRatings; 
    private SQLiteDatabase mDataBaseWeather; 
    
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
    private Context mContext;
    
    /*
     * How many users at this point. Used for closing the database
     * Will serve as a non blocking sem with synchronized statement
     */
    private Integer mUsers;
    private Integer mUsersFiles;
    private Integer mUsersElev;
    private Integer mUsersPlates;
    private Integer mUsersGeoPlates;
    private Integer mUsersWeather;
    private Integer mUsersProcedures;
    private Integer mUsersFuel;
    private Integer mUsersRatings;
    
    
    public  static final String  FACILITY_NAME = "Facility Name";
    private static final String  FACILITY_NAME_DB = "FacilityName";
    private static final int    FACILITY_NAME_COL = 4;
    public  static final String  LOCATION_ID = "Location ID";
    private static final String  LOCATION_ID_DB = "LocationID";
    private static final String  INFO_DB = "info";
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
    private static final String TABLE_FUEL = "fuel";
    private static final String TABLE_RATINGS = "ratings";


    private static final String TILE_NAME = "name";
    
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
        mCenterTile = null;
        mUsers = mUsersFiles = mUsersWeather = mUsersElev = mUsersPlates = mUsersGeoPlates = mUsersProcedures = mUsersFuel = mUsersRatings = 0;
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
    
    private String addPath(String name, String path) {
    	return path + "/" + name;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public LinkedList<String> findFilesToDelete(String name, String path) {
        String dbs[] = mContext.getResources().getStringArray(R.array.ChartDbNames);

        LinkedList<String> list = new LinkedList<String>();
        
        /*
         * This is to delete the main file, partially downloaded zip file
         */
        list.add(addPath(name, path));
        list.add(addPath(name + ".zip", path));
        
        /*
         * Delete weather
         */
        if(name.equals("weather")) {
            list.add(addPath(name + ".db", path));
            return list;
        }

        if(name.equals("TFRs")) {
            list.add(addPath("tfr.txt", path));
            return list;
        }
        
        if(name.equals("fuel")) {
            list.add(addPath(name + ".db", path));
            return list;
       }
        
       if(name.equals("ratings")) {
           list.add(addPath(name + ".db", path));
           return list;
       }
        
        if(name.equals("conus")) {
            list.add(addPath("latest.txt", path));
            list.add(addPath("latest_radaronly.png", path));
            return list;
        }

        if(name.equals("alternates")) {
            list.add(addPath("minimums", path));
            return list;
        }

        /*
         * Delete georef
         */
        if(name.equals("geoplates")) {
            list.add(addPath(name + ".db", path));
            return list;
        }

        /*
         * Delete databases
         */
        if(name.startsWith("databases") && dbs != null) {
            for(int i = 0; i < dbs.length; i++) {
                list.add(addPath(dbs[i], path));
            }
            list.add(addPath(getMainDb(), path));
            return list;                    
        }

        /*
         * Delete A/FD
         */
        if(name.startsWith("AFD_")) {

        	final String loc = name.split("_")[1].toLowerCase(Locale.US); // AFD names are lower case

        	FilenameFilter filter = new FilenameFilter() {
        		@Override
                public boolean accept(File dir, String namef) {
        			if(namef.startsWith(loc)) {
                        return true;
        			}
        			return false;
        		}
            };

        	String files[] = new File(addPath("afd/", path)).list(filter);
            if(null != files) {
	        	for(String file : files) {
	        		list.add(addPath("afd/" + file, path));
	        	}
            }

            return list;                    
        }

        /*
         * If none of the above then its tiles
         * Dont delete level 4
         */
        String query = "select name from " + TABLE_FILES + " where " + INFO_DB + "=='" + name +"'"
                + "and level != '4'";
        /*
         * Delete files from all databases
         */
        for(int i = 0; i < dbs.length; i++) {
            Cursor cursor = doQuery(query, dbs[i]);
    
            try {
                if(cursor != null) {
                    for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        list.add(addPath(cursor.getString(0), path));
                    }
                }
            }
            catch (Exception e) {
            }
            
            closes(cursor);
        }
        
        /*
         * Now plates: d-tpp / area
         */
        query = "select " + LOCATION_ID_DB + " from " + TABLE_AIRPORTS + " where State=\"" + name.replace("Area", "") + "\";";
        Cursor cursor = doQuery(query, getMainDb());

        try {
            if(cursor != null) {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                	if(name.contains("Area")) {
                        list.add(addPath("area/" + cursor.getString(0), path));
                	}
                	else {
                		list.add(addPath("plates/" + cursor.getString(0), path));
                	}
                }
            }
        }
        catch (Exception e) {
        }
        
        closes(cursor);

        
        return list;            
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
        if(!mPref.shouldShowAllFacilities()) {
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
            if(!mPref.shouldShowAllFacilities()) {
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
        if(!mPref.shouldShowAllFacilities()) {
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
        double corrFactor = Math.pow(Math.cos(Math.toRadians(lat)),2);
        String asDist = ", ((" + LONGITUDE_DB + " - " + lon + ") * (" + LONGITUDE_DB  + " - " + lon + ") * " + corrFactor + " + "
                + " (" + LATITUDE_DB + " - " + lat + ") * (" + LATITUDE_DB + " - " + lat + ")"
                + ") as dist";
        String qry = "select " + LOCATION_ID_DB + asDist + " from " + TABLE_AIRPORTS;
        if(!mPref.shouldShowAllFacilities()) {
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
    private String getFilesDb() {
        int db = Integer.parseInt(mPref.getChartType());
        String dbs[] = mContext.getResources().getStringArray(R.array.ChartDbNames);
        return dbs[db];
    }

    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryFiles(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersFiles) {
            if(mDataBaseFiles == null) {
                mUsersFiles = 0;
                try {
                    
                    mDataBaseFiles = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseFiles = null;
                }
            }
            if(mDataBaseFiles == null) {
                return c;
            }
            mUsersFiles++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseFiles == null) {
            return c;
        }
        
        if(!mDataBaseFiles.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseFiles.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    /**
     * Close database
     */
    private void closesFiles(Cursor c) {
        
        try {
            c.close();
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersFiles) {
            mUsersFiles--;
            if((mDataBaseFiles != null) && (mUsersFiles <= 0)) {
                try {
                    mDataBaseFiles.close();
                }
                catch (Exception e) {
                }
                mDataBaseFiles = null;
                mUsersFiles = 0;
            }
        }
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
    public Tile findClosest(double lon, double lat, double offset[], double p[], int factor) {
      
        String qry =
                "select * from " + TABLE_FILES + " where " + 
                "((latul - " + lat + ") > 0) and " +
                "((latll - " + lat + ") < 0) and " + 
                "((lonul - " + lon + ") < 0) and " + 
                "((lonur - " + lon + ") > 0) and " +
                "level like '%" + factor + "%';";
        
        /*
         * In case we fail
         */
        offset[0] = 0;
        offset[1] = 0;
        
        Cursor cursor = doQueryFiles(qry, getFilesDb());
        
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
                            cursor.getDouble(10),
                            cursor.getString(11));
                  
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
        
        closesFiles(cursor);
        return mCenterTile;        
    }

    
    /**
     * 
     * @param name
     * @return
     */
    public Tile findTile(String name) {
        String query = "select * from " + TABLE_FILES + " where " + TILE_NAME + "=='" + name +"'";
        Cursor cursor = doQueryFiles(query, getFilesDb());
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
                            cursor.getDouble(10),
                            cursor.getString(11));
                    /*
                     * Position on tile
                     */
                }
            }
        }
        catch (Exception e) {
        }
        
        closesFiles(cursor);
        return tile;            

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
                    taf.stationId = cursor.getString(1);
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
    private Cursor doQueryElev(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersElev) {
            if(mDataBaseElev == null) {
                mUsersElev = 0;
                try {
                    
                    mDataBaseElev = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseElev = null;
                }
            }
            if(mDataBaseElev == null) {
                return c;
            }
            mUsersElev++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseElev == null) {
            return c;
        }
        
        if(!mDataBaseElev.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseElev.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    /**
     * Close database
     */
    private void closesElev(Cursor c) {

        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersElev) {
            mUsersElev--;
            if((mDataBaseElev != null) && (mUsersElev <= 0)) {
                try {
                    mDataBaseElev.close();
                }
                catch (Exception e) {
                }
                mDataBaseElev = null;
                mUsersElev = 0;
            }
        }
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
    public Tile findElevTile(double lon, double lat, double offset[], double p[], int factor) {
      
        String qry =
                "select * from " + TABLE_FILES + " where " + 
                "((latul - " + lat + ") > 0) and " +
                "((latll - " + lat + ") < 0) and " + 
                "((lonul - " + lon + ") < 0) and " + 
                "((lonur - " + lon + ") > 0) and " +
                "level like '%" + factor + "%';"; /* Get highest level tile for elev */
        
        Tile t = null;
        Cursor cursor = doQueryElev(qry, "maps.elv.db");
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
    
                    /*
                     * Database only return center tile, we find tiles around it using arithmetic
                     */
                    t = new Tile(
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
                            cursor.getDouble(10),
                            cursor.getString(11));
                    /*
                     * Position on tile
                     */
                    offset[0] = t.getOffsetTopX(lon);
                    offset[1] = t.getOffsetTopY(lat);
                    p[0] = t.getPx();
                    p[1] = t.getPy();
                }
            }
        }
        catch (Exception e) {
        }
        
        closesElev(cursor);
        return t;        
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
     * @param type
     * @param runway
     * @return
     */
    public LinkedList<String> findProcedure(String name, String type, String runway) {
        
        LinkedList<String> ret = new LinkedList<String>();
        
        String qry =
                "select * from " + TABLE_PROCEDURE + " where Airport='" + name + "' and AppType='" + type + "' and runway='"  + runway  + "';";
        
        Cursor cursor = doQueryProcedures(qry, "procedures.db");
        
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        
                        /*
                         * Add as inital course, initial alts, final course, final alts, missed course, missed alts
                         */
                        ret.add(cursor.getString(4));
                        ret.add(cursor.getString(5));
                        ret.add(cursor.getString(6));
                        ret.add(cursor.getString(7));
                        ret.add(cursor.getString(8));
                        ret.add(cursor.getString(9));
                    } while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        
        closesProcedures(cursor);
        
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
    private Cursor doQueryFuel(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersFuel) {
            if(mDataBaseFuel == null) {
                mUsersFuel = 0;
                try {
                    
                    mDataBaseFuel = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseFuel = null;
                }
            }
            if(mDataBaseFuel == null) {
                return c;
            }
            mUsersFuel++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseFuel == null) {
            return c;
        }
        
        if(!mDataBaseFuel.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseFuel.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    
    /**
     * Close database
     */
    private void closesFuel(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersFuel) {
            mUsersFuel--;
            if((mDataBaseFuel != null) && (mUsersFuel <= 0)) {
                try {
                    mDataBaseFuel.close();
                }
                
                
                catch (Exception e) {
                }
                mDataBaseFuel = null;
                mUsersFuel = 0;
            }
        }
    }

    /**
     * 
     * @param name
     * @return
     */
    public LinkedList<String> findFuelCost(String name) {

    	LinkedList<String> ret = new LinkedList<String>();
    	
        String qry = "select * from " + TABLE_FUEL + " where airport =='" + name +"'" + 
        		" order by reported desc limit 6";
        Cursor cursor = doQueryFuel(qry, "fuel.db");
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
	                    /*
	                     * Make entries to show
	                     * 100LL 10.30 @ Chevron 2015-03-07
	                     */
	                	String tokens[] = cursor.getString(4).split(" ");
	                	ret.add(cursor.getString(2) + " $" + cursor.getString(1)	+ " @ " + cursor.getString(3) + " " + tokens[0]);
                    }
                    while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        closesFuel(cursor);
        return ret;
    }

    
    /**
     * 
     * @param statement
     * @return
     */
    private Cursor doQueryRatings(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;
        if(!(new File(path).exists())) {
            return null;
        }

        /*
         * 
         */
        synchronized(mUsersRatings) {
            if(mDataBaseRatings == null) {
                mUsersRatings = 0;
                try {
                    
                    mDataBaseRatings = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | 
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
                catch(RuntimeException e) {
                    mDataBaseRatings = null;
                }
            }
            if(mDataBaseRatings == null) {
                return c;
            }
            mUsersRatings++;
        }
        
        /*
         * In case we fail
         */
        
        if(mDataBaseRatings == null) {
            return c;
        }
        
        if(!mDataBaseRatings.isOpen()) {
            return c;
        }
        
        /*
         * Find with sqlite query
         */
        try {
               c = mDataBaseRatings.rawQuery(statement, null);
        }
        catch (Exception e) {
            c = null;
        }

        return c;
    }

    
    /**
     * Close database
     */
    private void closesRatings(Cursor c) {
        try {
            if(null != c) {
                c.close();
            }
        }
        catch (Exception e) {
            
        }

        synchronized(mUsersRatings) {
            mUsersRatings--;
            if((mDataBaseRatings != null) && (mUsersRatings <= 0)) {
                try {
                    mDataBaseRatings.close();
                }
                
                
                catch (Exception e) {
                }
                mDataBaseRatings = null;
                mUsersRatings = 0;
            }
        }
    }

    
    /**
     * 
     * @param name
     * @return
     */
	public LinkedList<String> findRatings(String name) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
        String qry = "select * from " + TABLE_RATINGS + " where airport =='" + name +"'" + 
        		" order by reported desc";
        Cursor cursor = doQueryRatings(qry, "ratings.db");
        try {
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
	                    /*
	                     * Make entries to show
	                     * 4 star, userid (2015-03-07): Comments
	                     */
	                	String tokens[] = cursor.getString(4).split(" ");
	                	ret.add(cursor.getString(2) + " " + mContext.getString(R.string.Stars) + ", " + cursor.getString(0) + " (" + tokens[0] + "): " + cursor.getString(3));
                    }
                    while(cursor.moveToNext());
                }
            }
        }
        catch (Exception e) {
        }
        closesRatings(cursor);
        return ret;
	}

}
