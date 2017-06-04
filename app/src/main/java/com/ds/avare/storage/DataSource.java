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

import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Fix;
import com.ds.avare.place.NavAid;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.plan.Cifp;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.userDefinedWaypoints.Waypoint;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

/**
 * @author zkhan, jlmcgraw
 * Gets entries from database
 * The class that actually does something is DataBaseHelper
 */
public class DataSource {

    /**
     * 
     */
    private DataBaseHelper dbHelper;

    /**
     * @param context
     */
    public DataSource(Context context) {
        dbHelper = new DataBaseHelper(context);
    }

    /**
     * 
     * @return
     */
    public boolean isPresent() {
        return(dbHelper.isPresent());
    }
    
    /**
     * @param name
     * @param params
     */
    public void findDestination(String name, String type, String dbType, LinkedHashMap<String, String> params, LinkedList<Runway> runways, LinkedHashMap<String, String> freq,  LinkedList<Awos> awos) {
        dbHelper.findDestination(name, type, dbType, params, runways, freq, awos);
    }
    
    /**
     */
    public Coordinate getCoordinate(String name) {
        return dbHelper.getCoordinate(name);  
    }
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public Airport[] findClosestAirports(double lon, double lat, String minRunwayLength) {
        return dbHelper.findClosestAirports(lon, lat, minRunwayLength);        
    }
    
    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public String findClosestAirportID(double lon, double lat) {
        return(dbHelper.findClosestAirportID(lon, lat));
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public float[] findDiagramMatrix(String name) {
        return dbHelper.findDiagramMatrix(name);
    }

    /**
     * 
     * @param name
     * @param params
     * @return
     */
    public void search(String name, LinkedHashMap<String, String> params, boolean exact) {
        dbHelper.search(name, params, exact);    
    }

    /**
     * 
     * @param name
     * @return
     */
    public StringPreference searchOne(String name) {
        return dbHelper.searchOne(name);    
    }

    /**
     * 
     * @param airportId
     * @return Name of Minimums file
     */
    public String[] findMinimums(String airportId) {
        return dbHelper.findMinimums(airportId);
    }

    /**
     * 
     * @param airportId
     * @return Name of AFD file
     */
    public LinkedList<String> findAFD(String airportId) {
        return dbHelper.findAFD(airportId);
    }

    /**
     * 
     * @param lon
     * @param lat
     * @param height
     * @return Obstacles list that are dangerous
     */
    public LinkedList<Obstacle> findObstacles(double lon, double lat, int height) {
        return dbHelper.findObstacles(lon, lat, height);
    }

    /**
     *
     * @return All Game TFRs
     */
    public LinkedList<LabelCoordinate> findGameTFRs() {
        return dbHelper.findGameTFRs();
    }

    /**
     * 
     * @param name
     * @param type
     * @return
     */
    public String findLonLat(String name, String type) {
        return dbHelper.findLonLat(name, type);          
    }

    /**
     *
     * @param metars
     * @return
     */
    public void findLonLatMetar(HashMap<String, Metar> metars) {
        dbHelper.findLonLatMetar(metars);
    }

    /**
     * 
     * @return
     */
    public String findObstacle(String height, Destination dest) {
        return dbHelper.findObstacle(height, dest);          
    }

    /**
     * 
     * @param station
     * @return
     */
    public Taf getTAF(String station) {
        return dbHelper.getTAF(station);          
    }

    /**
     *
     * @param station
     * @return
     */
    public Metar getMETAR(String station) {
        return dbHelper.getMETAR(station);
    }

    /**
     *
     * @param lat
     * @param lon
     * @return
     */
    public Metar getClosestMETAR(Double lat, Double lon) {
        return dbHelper.getClosestMETAR(lat, lon);
    }

    /**
     * 
     * @return
     */
    public LinkedList<Airep> getAireps(double lon, double lat) {
        return dbHelper.getAireps(lon, lat);          
    }

    /**
     * 
     * @return
     */
    public WindsAloft getWindsAloft(double lon, double lat) {
        return dbHelper.getWindsAloft(lon, lat);          
    }

    /**
     * 
     * @return
     */
    public LinkedList<AirSigMet> getAirSigMets() {
        return dbHelper.getAirSigMets();
    }

    /**
     * 
     * @return
     */
    public String getSua(double lon, double lat) {
        return dbHelper.getSua(lon, lat);
    }

    /**
     * 
     * @param name
     * @return
     */
    public LinkedList<String> findRunways(String name) {
        return  dbHelper.findRunways(name);
    }

    /**
     * 
     * @param name
     * @return
     */
    public LinkedList<String> findFrequencies(String name) {
        return  dbHelper.findFrequencies(name);
    }

    /**
     * 
     * @param name
     * @return
     */
    public String findElev(String name) {
        return dbHelper.findElev(name);
    }

    
    /**
     * 
     * @return
     */
    public LinkedList<Cifp> findProcedure(String name, String approach) {
        return  dbHelper.findProcedure(name, approach);
    }
    
    /**
     * 
     * @return
     */
    public LinkedList<Coordinate> findAirway(String name) {
        return  dbHelper.findAirway(name);
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public Coordinate findNavaid(String name) {
        return  dbHelper.findNavaid(name);    	
    }

    /**
     *
     * @param lat
     * @param  lon
     * @return vector of the closest navaids (excluding NDBs); the closest first
     */
    public Vector<NavAid> findNavaidsNearby(Double lat, Double lon) {
        return  dbHelper.findNavaidsNearby(lat, lon);
    }

    public ArrayList<Fix> findClosestFixes(Double lat, Double lon) {
        return dbHelper.findClosestFixes(lat, lon);
    }

    /**
     *
     * @param name
     * @param airport
     */
    public Coordinate findRunwayCoordinates(String name, String airport) {
        return dbHelper.findRunwayCoordinates(name, airport);
    }

    public StringPreference getNavaidOrFixFromCoordinate(Coordinate c) {
        return dbHelper.getNavaidOrFixFromCoordinate(c);
    }
}
