/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.content;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;

import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.NavAid;
import com.ds.avare.place.Obstacle;
import com.ds.avare.place.Runway;
import com.ds.avare.plan.Cifp;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

/**
 * @author zkhan
 */
public class DataSource {

    /**
     * 
     */
    private Context mContext;
    private Preferences mPref;

    /**
     * @param context
     */
    public DataSource(Context context) {
        mContext = context;
        mPref = new Preferences(context);
    }

    public Preferences getPreferences() {
        return mPref;
    }

    public boolean isPresent() {
        // see if databases are downloaded
        return null != LocationContentProviderHelper.findNavaid(mContext, "BOS");
    }

    public static void reset(Context context) {
        ContentProviderClient client;
        ContentResolver resolver = context.getContentResolver();

        client = resolver.acquireContentProviderClient(ObstaclesContract.AUTHORITY_URI);
        ObstaclesProvider oprovider = (ObstaclesProvider) client.getLocalContentProvider();
        oprovider.resetDatabase();
        client.release();

        client = resolver.acquireContentProviderClient(ProceduresContract.AUTHORITY_URI);
        ProceduresProvider pprovider = (ProceduresProvider) client.getLocalContentProvider();
        pprovider.resetDatabase();
        client.release();

        client = resolver.acquireContentProviderClient(WeatherContract.AUTHORITY_URI);
        WeatherProvider wprovider = (WeatherProvider) client.getLocalContentProvider();
        wprovider.resetDatabase();
        client.release();

        client = resolver.acquireContentProviderClient(GameTfrContract.AUTHORITY_URI);
        GameTfrProvider gprovider = (GameTfrProvider) client.getLocalContentProvider();
        gprovider.resetDatabase();
        client.release();

        client = resolver.acquireContentProviderClient(LocationContract.AUTHORITY_URI);
        LocationProvider lprovider = (LocationProvider) client.getLocalContentProvider();
        lprovider.resetDatabase();
        client.release();

    }

    // location helper

    public void findDestination(String name, String type, String dbType, LinkedHashMap<String, String> params, LinkedList<Runway> runways, LinkedHashMap<String, String> freq,  LinkedList<Awos> awos) {
        LocationContentProviderHelper.findDestination(mContext, name, type, dbType, params, runways, freq, awos);
    }

    public HashMap<String, Airport> findClosestAirports(double lon, double lat, HashMap<String, Airport> airports, String minRunwayLength) {
        return LocationContentProviderHelper.findClosestAirports(mContext, lon, lat, airports, minRunwayLength, mPref.isShowAllFacilities());
    }

    public String findClosestAirportID(double lon, double lat) {
        return(LocationContentProviderHelper.findClosestAirportID(mContext, lon, lat, mPref.isShowAllFacilities()));
    }

    public float[] findDiagramMatrix(String name) {
        return LocationContentProviderHelper.findDiagramMatrix(mContext, name);
    }

    public void search(String name, LinkedHashMap<String, String> params, boolean exact) {
        LocationContentProviderHelper.search(mContext, name, params, exact, mPref.isShowAllFacilities());
    }

    public StringPreference searchOne(String name) {
        return LocationContentProviderHelper.searchOne(mContext, name);
    }

    public String[] findMinimums(String airportId) {
        return LocationContentProviderHelper.findMinimums(mContext, airportId);
    }

    public LinkedList<String> findAFD(String airportId) {
        return LocationContentProviderHelper.findAFD(mContext, airportId);
    }

    public String findLonLat(String name, String type) {
        return LocationContentProviderHelper.findLonLat(mContext, name, type);
    }

    public void findLonLatMetar(HashMap<String, Metar> metars) {
        LocationContentProviderHelper.findLonLatMetar(mContext, metars);
    }

    public String getSua(double lon, double lat) {
        return LocationContentProviderHelper.getSua(mContext, lon, lat);
    }

    public LinkedList<String> findRunways(String name) {
        return LocationContentProviderHelper.findRunways(mContext, name);
    }

    public String findElev(String name) {
        return LocationContentProviderHelper.findElev(mContext, name);
    }

    public LinkedList<Coordinate> findAirway(String name) {
        return  LocationContentProviderHelper.findAirway(mContext, name);
    }

    public Coordinate findNavaid(String name) {
        return  LocationContentProviderHelper.findNavaid(mContext, name);
    }

    public Vector<NavAid> findNavaidsNearby(double lat, double lon) {
        return  LocationContentProviderHelper.findNavaidsNearby(mContext, lat, lon);
    }

    public Coordinate findRunwayCoordinates(String name, String airport) {
        return LocationContentProviderHelper.findRunwayCoordinates(mContext, name, airport);
    }

    public StringPreference getNavaidOrFixFromCoordinate(Coordinate c) {
        return LocationContentProviderHelper.getNavaidOrFixFromCoordinate(mContext, c);
    }


    // other helper

    public LinkedList<Obstacle> getObstacles(double lon, double lat, double height) {
        return ContentProviderHelper.getObstacles(mContext, lon, lat, height);
    }

    public LinkedList<LabelCoordinate> findGameTFRs() {
        return ContentProviderHelper.findGameTFRs(mContext);
    }

    public Taf getTaf(String station) {
        return ContentProviderHelper.getTaf(mContext, station);
    }

    public Metar getMetar(String station) {
        return ContentProviderHelper.getMetar(mContext, station);
    }

    public LinkedList<Airep> getAireps(double lon, double lat) {
        return ContentProviderHelper.getAireps(mContext, lon, lat);
    }

    public WindsAloft getWindsAloft(double lon, double lat) {
        return ContentProviderHelper.getWindsAloft(mContext, lon, lat);
    }

    public LinkedList<AirSigMet> getAirSigMets() {
        return ContentProviderHelper.getAirSigMets(mContext);
    }

    public LinkedList<Cifp> findProcedure(String name, String approach) {
        return  ContentProviderHelper.findProcedure(mContext, name, approach);
    }



}
