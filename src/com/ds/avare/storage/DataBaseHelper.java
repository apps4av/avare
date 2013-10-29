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
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.ds.avare.R;
import com.ds.avare.place.Airport;

import com.ds.avare.place.Destination;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Coordinate;
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
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author zkhan, jlmcgraw
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
    private SQLiteDatabase mDataBaseFiles; 
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
    private Integer mUsersWeather;
    
    
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

    private static final String TABLE_AIRPORTS = "airports";
    private static final String TABLE_AIRPORT_DIAGS = "airportdiags";
    private static final String TABLE_AIRPORT_FREQ = "airportfreq";
    private static final String TABLE_AIRPORT_AWOS_STATIONS = "awos";
    private static final String TABLE_AIRPORT_RUNWAYS = "airportrunways";
    private static final String TABLE_AIRPORT_REMARKS = "airportremarks";
    private static final String TABLE_FILES = "files";
    private static final String TABLE_FIX = "fix";
    private static final String TABLE_NAV = "nav";
    private static final String TABLE_TO = "takeoff";
    private static final String TABLE_ALT = "alternate";
    private static final String TABLE_AFD = "afd";
    private static final String TABLE_OBSTACLES = "obs";


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
        super(context, null, null, DATABASE_VERSION);
        mPref = new Preferences(context);
        mCenterTile = null;
        mUsers = mUsersFiles = mUsersWeather = 0;
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

        synchronized(mUsers) {
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
    private Cursor doQuery(String statement, String name) {
        Cursor c = null;
        
        String path = mPref.mapsFolder() + "/" + name;

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
     * 
     * @param name
     * @return
     */
    public LinkedList<String> findFilesToDelete(String name) {
        String dbs[] = mContext.getResources().getStringArray(R.array.ChartDbNames);

        String query = "select name from " + TABLE_FILES + " where " + INFO_DB + "=='" + name +"'";

        LinkedList<String> list = new LinkedList<String>();
        
        /*
         * This is to delete the main file, partially downloaded zip file
         */
        list.add(name);
        list.add(name + ".zip");
        
        /*
         * Delete weather
         */
        if(name.equals("weather")) {
            list.add(name + ".db");
            return list;
        }

        if(name.equals("TFRs")) {
            list.add("tfr.txt");
            return list;
        }

        /*
         * Delete databases
         */
        if(name.startsWith("databases") && dbs != null) {
            for(int i = 0; i < dbs.length; i++) {
                list.add(dbs[i]);
            }
            list.add(getMainDb());
            return list;                    
        }

        /*
         * Delete files from all databases
         */
        for(int i = 0; i < dbs.length; i++) {
            Cursor cursor = doQuery(query, dbs[i]);
    
            try {
                if(cursor != null) {
                    for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        list.add(cursor.getString(0));
                    }
                }
            }
            catch (Exception e) {
            }
            
            closes(cursor);
        }
        return list;            
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
            qry += " where " + TYPE_DB + "=='AIRPORT' ";
        }
        qry += " order by ((" + 
                lon + " - " + LONGITUDE_DB + ") * (" + lon + "- " + LONGITUDE_DB +") + (" + 
                lat + " - " + LATITUDE_DB + ") * (" + lat + "- " + LATITUDE_DB + ")) ASC limit " + airports.length + ";";            

        Cursor cursor = doQuery(qry, getMainDb());

        try {
            int id = 0;
            if(cursor != null) {
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
                        params.put("Elevation", parts[0] + "ft");
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
        Cursor cursor = doQuery(qry, getMainDb());

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
    public void findDestination(String name, String type, LinkedHashMap<String, String> params, LinkedList<Runway> runways, LinkedHashMap<String, String> freq) {
        
        Cursor cursor;
        String landingSiteFacilityNumber = null;
      
        
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

        String qry = "select * from " + types + " where " + LOCATION_ID_DB + "=='" + name + "';";
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
                       
                        params.put("Owner", cursor.getString(21));                       
                        params.put("Use", cursor.getString(5).trim());
                        params.put("Manager", cursor.getString(7).trim());
                        params.put(MANAGER_PHONE, cursor.getString(8).trim());
                        params.put("Elevation", cursor.getString(9).trim());
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

                        params.put(BEACON, cursor.getString(BEACON_COL));
                        String sc = cursor.getString(SEGCIRCLE_COL);
                        if(sc.equals("Y") || sc.equals("Y-L")) {
                            params.put(SEGCIRCLE, mContext.getString(R.string.Yes));
                        }
                        else {
                            params.put(SEGCIRCLE, mContext.getString(R.string.No));                            
                        }
                        params.put("Pattern Altitude", cursor.getString(11));
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
                        /*
                         * Store the LFSN for this airtport, it is the key for joining the other tables
                         */
                        landingSiteFacilityNumber = cursor.getString(22);
                        

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
	 * Get AWOS frequency/telephone info for this particular airport
	 */
	qry = "select * from " + TABLE_AIRPORT_AWOS_STATIONS + " where LandingFacilitySiteNumber" + "=='" + landingSiteFacilityNumber + "';";
	cursor = doQuery(qry, getMainDb());
	
	// 0     1    2     3   
	// ident,type,lfsn, data
	try {
	    /*
	     * Add each AWOS
	     */
	    if (cursor != null) {
		while (cursor.moveToNext()) {
		    
		    String typeof = cursor.getString(1);
		

		    if (freq.containsKey(typeof)) {
			/*
			 * Append this string to the existing one if duplicate
			 * key
			 */
			freq.put(typeof,
				freq.get(typeof) + "\n\n" + cursor.getString(3));
		    } else {
			freq.put(typeof, cursor.getString(3));
		    }
		}

	    }
	} catch (Exception e) {
	}
	closes(cursor); 
	
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
         *Find runways        
         */

        qry = "select * from " + TABLE_AIRPORT_RUNWAYS + " where " + LOCATION_ID_DB + "=='" + name
                + "' or " + LOCATION_ID_DB + "=='K" + name + "';";
        cursor = doQuery(qry, getMainDb());
        
	try {
	    /*
	     * Add all of them
	     */
	    if (cursor != null) {
		while (cursor.moveToNext()) {

		    String Length = cursor.getString(1);
		    String Width = cursor.getString(2);
		    String Surface = cursor.getString(3);
		    String Variation = params.get(MAGNETIC_VARIATION);

		    String run = Helper.removeLeadingZeros(cursor.getString(4));
		    String lat = Helper.removeLeadingZeros(cursor.getString(6));
		    String lon = Helper.removeLeadingZeros(cursor.getString(8));

		    String Elevation = cursor.getString(10);
		    if (Elevation.equals("")) {
			Elevation = params.get("Elevation");
		    }
		    String Heading = cursor.getString(12);
		    String DT = cursor.getString(14);
		    if (DT.equals("")) {
			DT = "0";
		    }
		    String Lighted = cursor.getString(16);
		    if (Lighted.equals("0") || Lighted.equals("")) {
			Lighted = mContext.getString(R.string.No);
		    }
		    String ILS = cursor.getString(18);
		    if (ILS.equals("")) {
			ILS = mContext.getString(R.string.No);
		    }
		    String VGSI = cursor.getString(20);
		    if (VGSI.equals("")) {
			VGSI = mContext.getString(R.string.No);
		    }
		    String Pattern = cursor.getString(22);
		    if (Pattern.equals("Y")) {
			Pattern = "Right";
		    } else {
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
		     * If runway name is null don't add the runway
		     */
		    run = Helper.removeLeadingZeros(cursor.getString(5));
		    
		    if (run == "")
			continue;
		    run = Helper.removeLeadingZeros(cursor.getString(5));
		    lat = Helper.removeLeadingZeros(cursor.getString(7));
		    lon = Helper.removeLeadingZeros(cursor.getString(9));

		    Elevation = cursor.getString(11);
		    if (Elevation.equals("")) {
			Elevation = params.get("Elevation");
		    }
		    Heading = cursor.getString(13);
		    DT = cursor.getString(15);
		    if (DT.equals("")) {
			DT = "0";
		    }
		    Lighted = cursor.getString(17);
		    if (Lighted.equals("0") || Lighted.equals("")) {
			Lighted = mContext.getString(R.string.No);
		    }
		    ILS = cursor.getString(19);
		    if (ILS.equals("")) {
			ILS = mContext.getString(R.string.No);
		    }
		    VGSI = cursor.getString(21);
		    if (VGSI.equals("")) {
			VGSI = mContext.getString(R.string.No);
		    }
		    Pattern = cursor.getString(23);
		    if (Pattern.equals("Y")) {
			Pattern = "Right";
		    } else {
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
        catch (Exception e) {
        }

        closes(cursor);
        
        //------------
        /*
         * Find remarks via matching the landingSiteFacilityNumber from "airports"
         */
        
        qry = "select * from " + TABLE_AIRPORT_REMARKS + " where LandingFacilitySiteNumber" + "=='" + landingSiteFacilityNumber + "';";
        cursor = doQuery(qry, getMainDb());

	try {
	    /*
	     * Add all of them
	     */
	    if (cursor != null) {
		while (cursor.moveToNext()) {
		    String element = cursor.getString(1);
		    String newValue = cursor.getString(2);
		    if (params.containsKey(element)) {
			/*
			 * Append this string to the existing one if it is a  duplicate
			 * key
			 */
			params.put(element, params.get(element) + "\n\n"
				+ newValue);
		    } else {
			params.put(element, newValue);
		    }
		}
	    }
	}
        catch (Exception e) {
        }
        closes(cursor);
        
        //-----------
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
	String qry = null;

	/*
	 * Get AWOS info
	 */

	qry = "select * from " + TABLE_AIRPORT_AWOS_STATIONS + " where "
		+ LOCATION_ID_DB + "=='" + name + "' or " + LOCATION_ID_DB
		+ "=='K" + name + "';";
	cursor = doQuery(qry, getMainDb());
	// 0     1    2     3  
	// ident,type,lfsn, data
	try {
	    /*
	     * Add each AWOS
	     */
	    if (cursor != null) {
		while (cursor.moveToNext()) {
		    String typeof = cursor.getString(1);
		    freq.add(typeof + " " + cursor.getString(3));
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
			    if (!Helper.isFrequencyUHF(frequency)) {
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
			    if (!Helper.isFrequencyUHF(frequency)) {
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

	/*
	 * Find frequencies (ATIS, TOWER, GROUND, etc) Not AWOS
	 */

	qry = "select * from " + TABLE_AIRPORT_FREQ + " where "
		+ LOCATION_ID_DB + "=='" + name + "' or " + LOCATION_ID_DB
		+ "=='K" + name + "';";
	cursor = doQuery(qry, getMainDb());

	try {
	    /*
	     * Add all of them
	     */
	    if (cursor != null) {
		while (cursor.moveToNext()) {
		    String typeof = cursor.getString(1);
		    typeof = typeof.replace("LCL", "TWR");
		    /*
		     * Filter out silly frequencies
		     */
		    if (typeof.equals("EMERG") || typeof.contains("GATE")
			    || typeof.equals("EMERGENCY")) {
			continue;
		    }
		    /*
		     * Filter out UHF
		     */
		    try {
			double frequency = Double.parseDouble(cursor
				.getString(2));
			if (Helper.isFrequencyUHF(frequency)) {
			    continue;
			}
		    } catch (Exception e) {
		    }

		    freq.add(typeof + " " + cursor.getString(2));
		}
	    }
	} catch (Exception e) {
	}
	closes(cursor);

	return freq;
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
            qry +=  " where " + TYPE_DB + "=='AIRPORT' and ((";
        }
        else {
            qry += " where ((";
        }

        qry += "(" + LONGITUDE_DB + " - " + lon + ") * (" + LONGITUDE_DB  + " - " + lon + ") + "
                + "(" + LATITUDE_DB + " - " + lat + ") * (" + LATITUDE_DB + " - " + lat + ")"
                + ") < 0.001) limit 1;";
        
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
    public String findAFD(String airportId) {
        
        String ret = null;
        String qry = "select File from " + TABLE_AFD + " where " + LOCATION_ID_DB + "==" + "'" + airportId + "'";
        
        Cursor cursor = doQuery(qry, getMainDb());

        try {
            if(cursor != null) {
                if(cursor.moveToNext()) {
                    ret = cursor.getString(0);
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
        if(null != c) {
            c.close();
        }

        synchronized(mUsersFiles) {
            mUsersFiles--;
            if((mDataBaseFiles != null) && (mUsersFiles <= 0)) {
                try {
                    mDataBaseFiles.close();
                    super.close();
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
        if(null != c) {
            c.close();
        }

        synchronized(mUsersWeather) {
            mUsersWeather--;
            if((mDataBaseWeather != null) && (mUsersWeather <= 0)) {
                try {
                    mDataBaseWeather.close();
                    super.close();
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

}
