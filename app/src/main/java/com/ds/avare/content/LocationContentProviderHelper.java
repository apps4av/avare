package com.ds.avare.content;

import android.content.Context;
import android.database.Cursor;
import android.hardware.GeomagneticField;

import com.ds.avare.R;
import com.ds.avare.place.Airport;
import com.ds.avare.place.Awos;
import com.ds.avare.place.Destination;
import com.ds.avare.place.NavAid;
import com.ds.avare.place.Runway;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Radial;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;
import com.ds.avare.weather.Metar;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;


/**
 * Created by zkhan on 2/8/17.
 */

public class LocationContentProviderHelper {


    public static final String LOCATION_ID = "Location ID";
    public static final String CUSTOMS = "Customs";
    public static final String BEACON = "Beacon";
    public static final String FUEL_TYPES = "Fuel Types";
    public static final String FSSPHONE = "FSS Phone";
    public static final String SEGCIRCLE = "Segmented Circle";
    public static final String MANAGER_PHONE = "Manager Phone";
    public static final String MANAGER = "Manager";
    public static final String ELEVATION = "Elevation";
    public static final String FACILITY_NAME = "Facility Name";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String TYPE = "Type";
    public static final String MAGNETIC_VARIATION = "Magnetic Variation";
    public static final String USE = "Use";
    public static final String CONTROL_TOWER = "Control Tower";
    public static final String CTAF = "CTAF";
    public static final String LANDING_FEE = "Landing Fee";
    public static final String UNICOM = "UNICOM";
    public static final String TPA = "Pattern Altitude";

    /**
     * Search something in database
     * @param name
     * @param params
     */
    public static void search(Context ctx, String name, LinkedHashMap<String, String> params, boolean exact, boolean showAll) {
        Cursor c = null;

        String order;
        String arguments[];
        String qry;

        /*
         * This is a radial search?
         */
        int len = name.length();
        if(len > 6) {
            StringPreference s = searchRadial(ctx, name);
            if(null != s) {
                s.putInHash(params);
                return;
            }
        }

        // Search city first
        searchCity(ctx, name, params);

        /*
         * We don't want to throw in too many results, but we also want to allow K as a prefix for airport names
         * If the user has typed enough, let's start looking for K prefixed airports as well
         */
        if(len > 2 && name.charAt(0) == 'K' || name.charAt(0) == 'k') {

            order = LocationContract.AIRPORTS_LOCATION_ID + " ASC";
            if(exact) {
                qry = LocationContract.AIRPORTS_LOCATION_ID + " = ? ";
                arguments = new String[] {name.substring(1)};
            }
            else {
                qry = LocationContract.AIRPORTS_LOCATION_ID + " like ? ";
                arguments = new String[] {name.substring(1) + "%"};
            }
            if(!showAll) {
                qry += " and " + LocationContract.AIRPORTS_TYPE + "=='AIRPORT'";
            }
            try {
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, order);
                if(c != null) {
                    while(c.moveToNext()) {
                        StringPreference s = new StringPreference(Destination.BASE,
                                c.getString(c.getColumnIndex(LocationContract.AIRPORTS_TYPE)),
                                c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FACILITY_NAME)),
                                c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID)));
                        s.putInHash(params);
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);
        }

        /*
         * All queries for airports, navaids, fixes
         */

        //navaids
        order = LocationContract.NAV_LOCATION_ID + " ASC";
        if(exact) {
            qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " != ?";
            arguments = new String[] {name, "VOT"};
        }
        else {
            qry = LocationContract.NAV_LOCATION_ID + " like ? and " + LocationContract.NAV_TYPE + " != ?";
            arguments = new String[] {name + "%", "VOT"};
        }

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, order);
            if(c != null) {
                while(c.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.NAVAID,
                            c.getString(c.getColumnIndex(LocationContract.NAV_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.NAV_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.NAV_LOCATION_ID)));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        // airports
        order = LocationContract.AIRPORTS_LOCATION_ID + " ASC";
        if(exact) {
            qry = LocationContract.AIRPORTS_LOCATION_ID + " = ? ";
            arguments = new String[] {name};
        }
        else {
            qry = LocationContract.AIRPORTS_LOCATION_ID + " like ? ";
            arguments = new String[] {name + "%"};
        }
        if(!showAll) {
            qry += " and " + LocationContract.AIRPORTS_TYPE + "=='AIRPORT'";
        }

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, order);
            if(c != null) {
                while(c.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.BASE,
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID)));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        //fixes
        order = LocationContract.FIX_LOCATION_ID + " ASC";
        if(exact) {
            qry = LocationContract.FIX_LOCATION_ID + " = ?";
            arguments = new String[] {name};
        }
        else {
            qry = LocationContract.FIX_LOCATION_ID + " like ?";
            arguments = new String[] {name + "%"};
        }
        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, order);
            if(c != null) {
                while(c.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.FIX,
                            c.getString(c.getColumnIndex(LocationContract.FIX_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.FIX_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.FIX_LOCATION_ID)));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);
    }


    /**
     * Find airports in an particular area
     */
    public static HashMap<String, Airport> findClosestAirports(Context ctx, double lon, double lat, HashMap<String, Airport> airports, String minRunwayLength, boolean showAll) {

        Cursor c = null;
        // Make a new hashmap and reuse values out of it that are still in the area
        HashMap<String, Airport> airportsnew = new LinkedHashMap<String, Airport>();

        // Query runways for distance

        double corrFactor = Math.pow(Math.cos(Math.toRadians(lat)), 2);
        String asdistance = "((" +
                LocationContract.AIRPORTS_LONGITUDE + " - " + lon + ") * (" +
                LocationContract.AIRPORTS_LONGITUDE + " - " + lon + ") * " + corrFactor + " + " + "(" +
                LocationContract.AIRPORTS_LATITUDE  + " - " + lat + ") * (" +
                LocationContract.AIRPORTS_LATITUDE  + " - " + lat + "))";

        String qry = "cast(" + LocationContract.AIRPORT_RUNWAYS_LENGTH + " as decimal) >= " + String.valueOf(minRunwayLength);
        if(!showAll) {
            qry += " and " + LocationContract.AIRPORTS_TYPE + "='AIRPORT'";
        }
        // LocationID is ambiguous hence add table name with it
        String projection[] = new String[] {
                LocationContract.TABLE_AIRPORTS + "." + LocationContract.AIRPORTS_LOCATION_ID,
                LocationContract.AIRPORT_RUNWAYS_LENGTH,
                LocationContract.AIRPORT_RUNWAYS_WIDTH,
                LocationContract.AIRPORT_RUNWAYS_HE_ELEVATION,
                LocationContract.AIRPORTS_LATITUDE,
                LocationContract.AIRPORTS_LONGITUDE,
                LocationContract.AIRPORTS_FACILITY_NAME,
                LocationContract.AIRPORTS_FUEL_TYPES,
                asdistance + " as distance"};
        String order = "distance ASC";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NEAR, projection, qry, null, order);
            if(c != null) {
                while(c.moveToNext()) {
                    String id = c.getString(0); // LocationContract.AIRPORT_RUNWAYS_LOCATION_ID
                    if(airportsnew.size() >= Preferences.MAX_AREA_AIRPORTS) {
                        // got how many were needed
                        break;
                    }
                    if(airports.containsKey(id)) {
                        // eliminate duplicate airports and show longest runway
                        airportsnew.put(id, airports.get(id));
                        continue;
                    }

                    LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
                    // find airport
                    String parts[] = c.getString(3).trim().split("[.]"); //LocationContract.AIRPORT_RUNWAYS_HE_ELEVATION
                    params.put(ELEVATION, parts[0] + "ft");
                    params.put(LOCATION_ID, c.getString(0));
                    params.put(FACILITY_NAME, c.getString(6));
                    params.put(LATITUDE, Double.toString(Helper.truncGeo(c.getDouble(4))));
                    params.put(LONGITUDE, Double.toString(Helper.truncGeo(c.getDouble(5))));
                    params.put(FUEL_TYPES, c.getString(7).trim());

                    Airport a = new Airport(params, lon, lat);
                    // runway length / width in combined table
                    String runway = c.getString(1) + "X" + c.getString(2); //LocationContract.AIRPORT_RUNWAYS_HE_LENGTH X LocationContract.AIRPORT_RUNWAYS_HE_WIDTH
                    a.setLongestRunway(runway);
                    airportsnew.put(id, a);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return airportsnew;
    }



    private static StringPreference stringQuery(Context ctx, String name, String type) {

        Cursor c = null;

        if(type.equals(Destination.NAVAID)) {
            String qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " != ?";
            String arguments[] = new String[] {name, "VOT"};
            String order = LocationContract.NAV_LOCATION_ID + " limit 1";
            try {
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, order);
                if(c != null) {
                    if(c.moveToFirst()) {
                        StringPreference s = new StringPreference(type, "", name, c.getString(c.getColumnIndex(LocationContract.NAV_LOCATION_ID)));
                        return s;
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);
        }

        if(type.equals(Destination.FIX)) {
            String qry = LocationContract.FIX_LOCATION_ID + " = ?";
            String arguments[] = new String[] {name};
            String order = LocationContract.FIX_LOCATION_ID + " limit 1";
            try {
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, order);
                if(c != null) {
                    if(c.moveToFirst()) {
                        StringPreference s = new StringPreference(type, "", name, c.getString(c.getColumnIndex(LocationContract.FIX_LOCATION_ID)));
                        return s;
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);
        }

        if(type.equals(Destination.BASE)) {
            String qry = LocationContract.AIRPORTS_LOCATION_ID + " = ?";
            String arguments[] = new String[] {name};
            String order = LocationContract.AIRPORTS_LOCATION_ID + " limit 1";
            try {
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, order);
                if(c != null) {
                    if(c.moveToFirst()) {
                        StringPreference s = new StringPreference(type, "", name, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID)));
                        return s;
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);
        }


        return null;
    }


    /**
     * Search with I am feeling lucky. Best guess
     */
    public static StringPreference searchOne(Context ctx, String name) {

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
                s = stringQuery(ctx, name, Destination.NAVAID);
                if(s != null) {
                    return s;
                }

                return stringQuery(ctx, name, Destination.BASE);
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
                s = stringQuery(ctx, name1, Destination.BASE);
                if(s != null) {
                    return s;
                }
                s = stringQuery(ctx, name, Destination.NAVAID);
                if(s != null) {
                    return s;
                }
                s = stringQuery(ctx, name, Destination.FIX);
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
                s = stringQuery(ctx, name1, Destination.BASE);
                if(s != null) {
                    return s;
                }
                s = stringQuery(ctx, name, Destination.FIX);
                if(s != null) {
                    return s;
                }

                break;

            case 6:
                s = stringQuery(ctx, name, Destination.FIX);
                if(s != null) {
                    return s;
                }

                break;

            default:
                /*
                 * Radials
                 */
                return searchRadial(ctx, name);
        }

        return null;
    }

    public static StringPreference getNavaidOrFixFromCoordinate(Context ctx, Coordinate coordinate) {

        // Find Fix here first
        String qry =
                "(" + LocationContract.FIX_LONGITUDE + " - ? )*" +
                "(" + LocationContract.FIX_LONGITUDE + " - ? )+" +
                "(" + LocationContract.FIX_LATITUDE +  " - ? )*" +
                "(" + LocationContract.FIX_LATITUDE +  " - ? )"  + " < 0.0000000001";

        String v0 = String.valueOf(coordinate.getLongitude());
        String v1 = String.valueOf(coordinate.getLatitude());

        String arguments[] = new String[] {v0, v0, v1, v1};

        Cursor c = null;

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, null);
            if (c != null) {
                if(c.moveToFirst()) {
                    StringPreference s = new StringPreference(Destination.FIX,
                            c.getString(c.getColumnIndex(LocationContract.FIX_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.FIX_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.FIX_LOCATION_ID)));
                    return s;
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);


        // Find Navaid
        qry =
                "(" + LocationContract.NAV_LONGITUDE + " - ? )*" +
                "(" + LocationContract.NAV_LONGITUDE + " - ? )+" +
                "(" + LocationContract.NAV_LATITUDE +  " - ? )*" +
                "(" + LocationContract.NAV_LATITUDE +  " - ? )"  + " < 0.0000000001 and (Type != 'VOT')"; // no VOT

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, null);
            if (c != null) {
                if(c.moveToFirst()) {
                    StringPreference s = new StringPreference(Destination.NAVAID,
                            c.getString(c.getColumnIndex(LocationContract.NAV_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.NAV_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.NAV_LOCATION_ID)));
                    return s;
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return null;
    }


    public static void searchCity(Context ctx, String name, LinkedHashMap<String, String> params) {
        /*
         * City in upper case in DB
         */
        String qry = LocationContract.AIRPORTS_CITY + " = ?";

        String arguments[] = new String[] {name.toUpperCase(Locale.getDefault())};

        Cursor c = null;

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    StringPreference s = new StringPreference(Destination.BASE,
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_TYPE)),
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FACILITY_NAME)),
                            c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID)));
                    s.putInHash(params);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

    }


    public static LinkedList<Coordinate> findAirway(Context ctx, String name) {
        LinkedList<Coordinate> points = new LinkedList<Coordinate>();
        Cursor c = null;

        /*
         * airway points in sequence
         */
        String qry = LocationContract.AIRWAY_NAME + " = ?";

        String arguments[] = new String[] {name};

        String order = "cast(" + LocationContract.AIRWAY_SEQUENCE + " as integer)";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRWAYS, null, qry, arguments, order);
            if(c != null) {
                while(c.moveToNext()) {
                    float latitude = c.getFloat(c.getColumnIndex(LocationContract.AIRWAY_LATITUDE));
                    float longitude = c.getFloat(c.getColumnIndex(LocationContract.AIRWAY_LONGITUDE));
                    points.add(new Coordinate(longitude, latitude));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return points;
    }

    public static LinkedList<String> findAFD(Context ctx, String airportId) {

        Cursor c = null;
        LinkedList<String> ret = new LinkedList<String>();

        String qry = LocationContract.AFD_LOCATION_ID + " = ?";

        String arguments[] = new String[] {airportId};

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AFD, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    ret.add(c.getString(c.getColumnIndex(LocationContract.AFD_FILE)));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return ret;
    }

    public static String[] findMinimums(Context ctx, String airportId) {

        Cursor c = null;
        /**
         * Search Minimums plates for this airport
         */
        String ret2[] = new String[2];
        String ret[] = new String[1];

        /*
         * Silly that FAA gives K and P for some airports as ICAO
         */
        String qry =
                LocationContract.ALTERNATE_LOCATION_ID + " = ?" + " or " +
                LocationContract.ALTERNATE_LOCATION_ID + " = ?" + " or " +
                LocationContract.ALTERNATE_LOCATION_ID + " = ?";

        String arguments[] = new String[] {airportId, "K" + airportId, "P" + airportId};

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_ALTERNATE, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToNext()) {
                    ret2[0] = c.getString(c.getColumnIndex(LocationContract.ALTERNATE_FILE));
                    ret[0] = ret2[0];
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        qry =
                LocationContract.TAKEOFF_LOCATION_ID + " = ?" + " or " +
                LocationContract.TAKEOFF_LOCATION_ID + " = ?" + " or " +
                LocationContract.TAKEOFF_LOCATION_ID + " = ?";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_TAKEOFF, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToNext()) {
                    ret2[1] = c.getString(c.getColumnIndex(LocationContract.TAKEOFF_FILE));
                    ret[0] = ret2[1];
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        /*
         * Only return appropriate sized array
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
     * Find the lat/lon of an airport/navaid/fix
     */
    public static String findLonLat(Context ctx, String name, String type) {
        Cursor c = null;

        String arguments[] = new String[] {name};

        try {
            if(type.equals(Destination.BASE)) {
                String qry = LocationContract.AIRPORTS_LOCATION_ID + " = ?";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, null);
                if(c != null) {
                    if(c.moveToFirst()) {
                        return new String(c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LONGITUDE)) + "," +
                                c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LATITUDE)));
                    }
                }
            }
            else if(type.equals(Destination.NAVAID)) {
                String qry = LocationContract.NAV_LOCATION_ID + " = ?";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, null);
                if(c != null) {
                    if(c.moveToFirst()) {
                        return new String(c.getString(c.getColumnIndex(LocationContract.NAV_LONGITUDE)) + "," +
                                c.getString(c.getColumnIndex(LocationContract.NAV_LATITUDE)));
                    }
                }
            }
            else if(type.equals(Destination.FIX)) {
                String qry = LocationContract.FIX_LOCATION_ID + " = ?";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, null);
                if(c != null) {
                    if(c.moveToFirst()) {
                        return new String(c.getString(c.getColumnIndex(LocationContract.FIX_LONGITUDE)) + "," +
                                c.getString(c.getColumnIndex(LocationContract.FIX_LATITUDE)));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return null;
    }

    public static Coordinate findNavaid(Context ctx, String name) {
        Coordinate coord = null;
        Cursor c = null;

        String qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " != ?";
        String arguments[] = new String[] {name, "VOT"};
        String order = LocationContract.NAV_LOCATION_ID +  " limit 1";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, order);
            if(c != null) {
                if(c.moveToFirst()) {
                    coord = new Coordinate(c.getFloat(c.getColumnIndex(LocationContract.NAV_LONGITUDE)),
                            c.getFloat(c.getColumnIndex(LocationContract.NAV_LATITUDE)));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        if(null != coord) {
            return coord;
        }

        // Not found, find it in fix

        qry = LocationContract.FIX_LOCATION_ID + " = ?";
        arguments = new String[] {name};
        order = LocationContract.FIX_LOCATION_ID +  " limit 1";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, order);
            if(c != null) {
                if(c.moveToFirst()) {
                    coord = new Coordinate(c.getFloat(c.getColumnIndex(LocationContract.FIX_LONGITUDE)),
                            c.getFloat(c.getColumnIndex(LocationContract.FIX_LATITUDE)));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return coord;
    }


    /**
     * Find the lat/lon of an array of airports, and update in the objects
     */
    public static void findLonLatMetar(Context ctx, HashMap<String, Metar> metars) {

        Cursor c = null;

        if (null == metars || metars.size() <= 0) {
            return;
        }

        String qry = "(";
        Set<String> keys = metars.keySet();
        for (int count = 0; count < (keys.size() - 1); count++) {
            // Make a long query instead of several long queries
            qry += LocationContract.AIRPORTS_LOCATION_ID + " = ? or ";
        }
        qry += LocationContract.AIRPORTS_LOCATION_ID + " = ?) and "; // last index without or
        qry += LocationContract.AIRPORTS_TYPE + " = 'AIRPORT'";

        String arguments[] = keys.toArray(new String[0]);

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, null);
            if (c != null) {
                while (c.moveToNext()) {
                    // populate the metar objects with lon/lat
                    double lon = c.getDouble(c.getColumnIndex(LocationContract.AIRPORTS_LONGITUDE));
                    double lat = c.getDouble(c.getColumnIndex(LocationContract.AIRPORTS_LATITUDE));
                    String id = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID));
                    Metar m = metars.get(id);
                    if (m != null) {
                        m.lat = lat;
                        m.lon = lon;
                    }
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

    }

    public static String getSua(Context ctx, double lon, double lat) {

        Cursor c = null;
        String ret = "";


        String qry =
                "(" + LocationContract.SUA_LONGITUDE + " - ? )*" +
                "(" + LocationContract.SUA_LONGITUDE + " - ? )+" +
                "(" + LocationContract.SUA_LATITUDE + " - ? )*" +
                "(" + LocationContract.SUA_LATITUDE + " - ? )"  + " < 1";


        String v0 = String.valueOf(lon);
        String v1 = String.valueOf(lat);

        String arguments[] = new String[] {v0, v0, v1, v1};


        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_SUA, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    String sua =
                            c.getString(c.getColumnIndex(LocationContract.SUA_DESIGNATOR)) + "(" +
                            c.getString(c.getColumnIndex(LocationContract.SUA_NAME)) + ")\n" +
                            c.getString(c.getColumnIndex(LocationContract.SUA_LOWER_LIMIT)) + " to " +
                            c.getString(c.getColumnIndex(LocationContract.SUA_UPPER_LIMIT)) + "\n";
                    String freqtx = c.getString(c.getColumnIndex(LocationContract.SUA_TX_FREQUENCY));
                    if(!freqtx.equals("")) {
                        sua += "TX " + freqtx + "\n";
                    }
                    String freqrx = c.getString(c.getColumnIndex(LocationContract.SUA_RX_FREQUENCY));
                    if(!freqrx.equals("")) {
                        sua += "RX " + freqrx + "\n";
                    }

                    sua += "NOTE " + c.getString(c.getColumnIndex(LocationContract.SUA_DAY)) + "\n";

                    ret += sua + "\n";
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        if(ret.equals("")) {
            ret = null;
        }
        return ret;
    }


    public static LinkedList<String> findRunways(Context ctx, String name) {

        Cursor c = null;
        LinkedList<String> run = new LinkedList<String>();

        String qry = LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ? or " + LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ? ";

        String arguments[] = new String[] {name, "K" + name};

        try {
            /*
             * Add all of them
             */
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_RUNWAYS, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    // return ident and true heading of LE runway
                    if(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_IDENT)).contains("H")) {
                        // No heliport
                        continue;
                    }
                    String trueh = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_IDENT)) + "," +
                            c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_HEADING));
                    run.add(trueh);
                    trueh = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_IDENT)) + "," +
                            c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_HEADING));
                    run.add(trueh);
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return run;
    }

    public static Coordinate findRunwayCoordinates(Context ctx, String name, String airport) {

        Cursor c = null;
        Coordinate coordinate = null;

        String qry =  "(" +
                LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ? or " +
                LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ? ) and (" +
                LocationContract.AIRPORT_RUNWAYS_LE_IDENT + " = ? or " +
                LocationContract.AIRPORT_RUNWAYS_HE_IDENT + " = ? )";

        String arguments[] = new String[] {airport, "K" + airport, name, name};

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_RUNWAYS, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToNext()) {
                    if(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_IDENT)).equals(name)) { //LE
                        coordinate = new Coordinate(c.getDouble(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_LONGITUDE)),
                                c.getDouble(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_LATITUDE)));
                    }
                    else if(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_IDENT)).equals(name)) { //HE
                        coordinate = new Coordinate(c.getDouble(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_LONGITUDE)),
                                c.getDouble(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_LATITUDE)));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return coordinate;
    }


    public static String findElev(Context ctx, String airport) {

        Cursor c = null;
        String elev = "";

        String arguments[] = new String[] {airport, "K" + airport};

        String qry = LocationContract.AIRPORTS_LOCATION_ID + " = ? or " + LocationContract.AIRPORTS_LOCATION_ID + " = ? ";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    elev = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_ELEVATION));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return elev;
    }


    public static String findClosestAirportID(Context ctx, double lon, double lat, boolean showAll) {
        String ret = null;

        Cursor c = null;
        double corrFactor = Math.pow(Math.cos(Math.toRadians(lat)), 2);

        String asdistance = "((" +
                LocationContract.AIRPORTS_LONGITUDE + " - " + lon + ") * (" +
                LocationContract.AIRPORTS_LONGITUDE + " - " + lon + ") * " + corrFactor + " + " + "(" +
                LocationContract.AIRPORTS_LATITUDE  + " - " + lat + ") * (" +
                LocationContract.AIRPORTS_LATITUDE  + " - " + lat + "))";

        String projection[] = new String[] {LocationContract.AIRPORTS_LOCATION_ID, asdistance + " as distance"};
        String order = "distance limit 1";

        String qry;

        if(!showAll) {
            qry =  LocationContract.AIRPORTS_TYPE + "= 'AIRPORT' and distance < " + String.valueOf(Preferences.MIN_TOUCH_MOVEMENT_SQ_DISTANCE);
        }
        else {
            qry =  "distance < " + String.valueOf(Preferences.MIN_TOUCH_MOVEMENT_SQ_DISTANCE);
        }


        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, projection, qry, null, order);
            if(c != null) {
                if(c.moveToFirst()) {
                    ret = new String(c.getString(0)); // LocationContract.AIRPORTS_LOCATION_ID
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return ret;
    }


    public static Vector<NavAid> findNavaidsNearby(Context ctx, double lat, double lon) {
        Vector result = null;
        Cursor c = null;

        final double NAVAID_SEARCH_RADIUS = 150.; // 150 seems reasonable, it is VOR Line Of Sight at 18000
        Coordinate top = Projection.findStaticPoint(lon, lat, 0, NAVAID_SEARCH_RADIUS);
        Coordinate bottom = Projection.findStaticPoint(lon, lat, 180, NAVAID_SEARCH_RADIUS);
        Coordinate left = Projection.findStaticPoint(lon, lat, 270,NAVAID_SEARCH_RADIUS);
        Coordinate right  = Projection.findStaticPoint(lon, lat, 90, NAVAID_SEARCH_RADIUS);
        double corrFactor = Math.pow(Math.cos(Math.toRadians(lat)), 2); // we need 2 coordinates for a fix; get 3 in case we hit NDB

        String v0 = String.valueOf(top.getLatitude());
        String v1 = String.valueOf(bottom.getLatitude());
        String v2 = String.valueOf(right.getLongitude());
        String v3 = String.valueOf(left.getLongitude());
        String arguments[] = new String[] {"VOR", "VOR/DME", "VORTAC", v0, v1, v2, v3};

        String order = "((" +
                LocationContract.NAV_LONGITUDE + " - " + lon + ") * (" +
                LocationContract.NAV_LONGITUDE + " - " + lon + ") * " + corrFactor + " + " + "(" +
                LocationContract.NAV_LATITUDE  + " - " + lat + ") * (" +
                LocationContract.NAV_LATITUDE  + " - " + lat + ")) limit 4";

        String qry =
                LocationContract.NAV_TYPE + " = ? or " +
                LocationContract.NAV_TYPE + " = ? or " +
                LocationContract.NAV_TYPE + " = ? and " +
                LocationContract.NAV_LATITUDE + " < ? and " +
                LocationContract.NAV_LATITUDE + " > ? and " +
                LocationContract.NAV_LONGITUDE + " < ? and " +
                LocationContract.NAV_LONGITUDE + " > ?";

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, order);
            if (c != null) {
                // proper display of navaid radials requires historical magnetic variation
                result = new Vector<>();
                while (c.moveToNext()) {
                    String locationId = c.getString(c.getColumnIndex(LocationContract.NAV_LOCATION_ID));
                    Coordinate coord = new Coordinate(c.getFloat(c.getColumnIndex(LocationContract.NAV_LONGITUDE)),
                            c.getFloat(c.getColumnIndex(LocationContract.NAV_LATITUDE)));
                    String name = c.getString(c.getColumnIndex(LocationContract.NAV_FACILITY_NAME));
                    String type = c.getString(c.getColumnIndex(LocationContract.NAV_TYPE));

                    int variation = c.getInt(c.getColumnIndex(LocationContract.NAV_VARIATION));
                    String navaidClass = c.getString(c.getColumnIndex(LocationContract.NAV_CLASS));
                    String hiwas = c.getString(c.getColumnIndex(LocationContract.NAV_HIWAS));
                    boolean hasHiwas = hiwas.equals("Y");
                    String elevationString = c.getString(c.getColumnIndex(LocationContract.NAV_ELEVATION));
                    double elevation = elevationString.isEmpty() ? 0 : Double.parseDouble(elevationString);

                    result.add(new NavAid(locationId, type, name, coord, variation, navaidClass, hasHiwas, elevation));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        return result;
    }

    public static float[] findDiagramMatrix(Context ctx, String name) {
        Cursor c = null;
        float ret[] = new float[12];
        int it;

        for(it = 0; it < 12; it++) { // zero out
            ret[it] = 0;
        }

        String qry = LocationContract.AIRPORT_DIAGS_LOCATION_ID + "= ?";
        String arguments[] = new String[] {name};

        try {
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_DIAGS, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToFirst()) {
                    ret[0] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM0));
                    ret[1] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM1));
                    ret[2] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM2));
                    ret[3] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM3));
                    ret[4] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM4));
                    ret[5] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORM5));
                    ret[6] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI0));
                    ret[7] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI1));
                    ret[8] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI2));
                    ret[9] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI3));
                    ret[10] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI4));
                    ret[11] = c.getFloat(c.getColumnIndex(LocationContract.AIRPORT_DIAGS_TRANSFORMI5));
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);
        return ret;
    }


    private static StringPreference searchRadial(Context ctx, String name) {
        Cursor c = null;
        int len = name.length();
        /*
         * Of the form XXXRRRDDD like BOS270010
         */
        String chop = name.substring(len - 6);
        String chopname = name.substring(0, len - 6).toUpperCase(Locale.getDefault());
        if(chop.matches("[0-9][0-9][0-9][0-9][0-9][0-9]")) {


            try {
                String qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " != ?";
                String arguments[] = new String[] {chopname, "VOT"};

                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, null);
                if (c != null) {
                    if (c.moveToFirst()) {

                        double lon = c.getDouble(c.getColumnIndex(LocationContract.NAV_LONGITUDE));
                        double lat = c.getDouble(c.getColumnIndex(LocationContract.NAV_LATITUDE));
                        double distance = Double.parseDouble(chop.substring(3, 6));

                        /*
                         * Radials are magnetic
                         */
                        GeomagneticField gmf = new GeomagneticField((float) lat,
                                (float) lon, 0, System.currentTimeMillis());
                        double bearing = Double.parseDouble(chop.substring(0, 3)) + gmf.getDeclination();
                        Coordinate coordinate = Radial.findCoordinate(lon, lat, distance, bearing);
                        StringPreference s = new StringPreference(Destination.GPS, "GPS", name,
                                Helper.truncGeo(coordinate.getLatitude()) + "&" + Helper.truncGeo(coordinate.getLongitude()));
                        CursorManager.close(c);
                        return s;
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);

            try {
                /*
                 * Did not find in NAV? Find in Fix
                 */
                String qry = LocationContract.FIX_LOCATION_ID + " = ?";
                String arguments[] = new String[] {chopname};

                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, null);
                if(c != null) {
                    if(c.moveToFirst()) {

                        double lon = c.getDouble(c.getColumnIndex(LocationContract.FIX_LONGITUDE));
                        double lat = c.getDouble(c.getColumnIndex(LocationContract.FIX_LATITUDE));
                        double distance = Double.parseDouble(chop.substring(3, 6));
                        /*
                         * Radials are magnetic
                         */
                        GeomagneticField gmf = new GeomagneticField((float)lat,
                                (float)lon, 0, System.currentTimeMillis());
                        double bearing = Double.parseDouble(chop.substring(0, 3)) + gmf.getDeclination();
                        Coordinate coordinate = Radial.findCoordinate(lon, lat, distance, bearing);
                        StringPreference s = new StringPreference(Destination.GPS, "GPS", name,
                                Helper.truncGeo(coordinate.getLatitude()) + "&" + Helper.truncGeo(coordinate.getLongitude()));
                        CursorManager.close(c);
                        return s;
                    }
                }
            }
            catch (Exception e) {
            }
            CursorManager.close(c);
        }

        return null;
    }



    /**
     * Find all information about a facility / destination based on its name
     */
    public static void findDestination(Context ctx, String name, String type, String dbType, LinkedHashMap<String, String> params, LinkedList<Runway> runways, LinkedHashMap<String, String> freq, LinkedList<Awos> awos) {
        Cursor c = null;

        try {

            if(type.equals(Destination.NAVAID)) {
                String qry;
                String[] arguments;
                if((null != dbType) && (dbType.length() > 0) && (false == dbType.equalsIgnoreCase("null"))) {
                    qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " = ? ";
                    arguments = new String[] {name, dbType};
                }
                else {
                    qry = LocationContract.NAV_LOCATION_ID + " = ? and " + LocationContract.NAV_TYPE + " != ? ";
                    arguments = new String[] {name, "VOT"};
                }
                // Order by type desc will cause VOR to be ahead of NDB if both are available.
                // This is a bit of a hack, but the user probably wants the VOR more than the NDB
                String order = LocationContract.NAV_TYPE + " DESC";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_NAV, null, qry, arguments, order);
            }
            if(type.equals(Destination.FIX)) {
                String qry;
                String[] arguments;
                if((null != dbType) && (dbType.length() > 0) && (false == dbType.equalsIgnoreCase("null"))) {
                    qry = LocationContract.FIX_LOCATION_ID + " = ? and " + LocationContract.FIX_TYPE + " = ?";
                    arguments = new String[] {name, dbType};
                }
                else {
                    qry = LocationContract.FIX_LOCATION_ID + " = ? ";
                    arguments = new String[] {name};
                }
                String order = LocationContract.FIX_TYPE + " DESC";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_FIX, null, qry, arguments, order);
            }
            if(type.equals(Destination.BASE)) {
                String qry;
                String[] arguments;
                if((null != dbType) && (dbType.length() > 0) && (false == dbType.equalsIgnoreCase("null"))) {
                    qry = LocationContract.AIRPORTS_LOCATION_ID + " = ? and " + LocationContract.AIRPORTS_TYPE + " = ?";
                    arguments = new String[] {name, dbType};
                }
                else {
                    qry = LocationContract.AIRPORTS_LOCATION_ID + " = ? ";
                    arguments = new String[] {name};
                }
                // Put OUR-AP in last
                String order = LocationContract.AIRPORTS_TYPE + "," + LocationContract.AIRPORTS_TYPE + "='OUR-AP' DESC";
                c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORTS, null, qry, arguments, order);
            }
            if(c != null) {
                if(c.moveToFirst()) {

                    /*
                     * Put ID and name first
                     */
                    if(type.equals(Destination.NAVAID)) {
                        params.put(LOCATION_ID, c.getString(c.getColumnIndex(LocationContract.NAV_LOCATION_ID)));
                        params.put(FACILITY_NAME, c.getString(c.getColumnIndex(LocationContract.NAV_FACILITY_NAME)));
                        params.put(LATITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.NAV_LATITUDE)))));
                        params.put(LONGITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.NAV_LONGITUDE)))));
                        params.put(TYPE, c.getString(c.getColumnIndex(LocationContract.NAV_TYPE)).trim());
                    }

                    else if(type.equals(Destination.FIX)) {
                        params.put(LOCATION_ID, c.getString(c.getColumnIndex(LocationContract.FIX_LOCATION_ID)));
                        params.put(FACILITY_NAME, c.getString(c.getColumnIndex(LocationContract.FIX_FACILITY_NAME)));
                        params.put(LATITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.FIX_LATITUDE)))));
                        params.put(LONGITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.FIX_LONGITUDE)))));
                        params.put(TYPE, c.getString(c.getColumnIndex(LocationContract.FIX_TYPE)).trim());
                    }

                    else if(type.equals(Destination.BASE)) {

                        params.put(LOCATION_ID, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_LOCATION_ID)));
                        params.put(FACILITY_NAME, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FACILITY_NAME)));
                        params.put(LATITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.AIRPORTS_LATITUDE)))));
                        params.put(LONGITUDE, Double.toString(Helper.truncGeo(c.getDouble(c.getColumnIndex(LocationContract.AIRPORTS_LONGITUDE)))));
                        params.put(TYPE, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_TYPE)).trim());
                        String use = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_USE)).trim();

                        if(use.equals("PU")) {
                            use = "PUBLIC";
                        }
                        else if(use.equals("PR")) {
                            use = "PRIVATE";
                        }
                        else  {
                            use = "MILITARY";
                        }
                        params.put(USE, use);
                        params.put(MANAGER, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_MANAGER)).trim());
                        params.put(MANAGER_PHONE, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_MANAGER_PHONE)).trim());
                        params.put(ELEVATION, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_ELEVATION)).trim());
                        params.put(MAGNETIC_VARIATION, c.getString(c.getColumnIndex(LocationContract.AIRPORTS_VARIATION)).trim());
                        String customs = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_CUSTOMS));
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
                            params.put(CUSTOMS, ctx.getString(R.string.No));
                        }
                        String bcn = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_BEACON));
                        if(bcn.equals("")) {
                            bcn = ctx.getString(R.string.No);
                        }
                        params.put(BEACON, bcn);
                        String sc = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_SEGMENTED_CIRCLE));
                        if(sc.equals("Y")) {
                            params.put(SEGCIRCLE, ctx.getString(R.string.Yes));
                        }
                        else {
                            params.put(SEGCIRCLE, ctx.getString(R.string.No));
                        }
                        String pa = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_TPA)).trim();
                        String paout = "";
                        if(pa.equals("")) {
                            try {
                                paout = "" + Math.round(Double.parseDouble(params.get(ELEVATION)) + 1000);
                            }
                            catch (Exception e) {

                            }
                        }
                        else {
                            try {
                                paout = "" + Math.round((Double.parseDouble(params.get(ELEVATION)) +
                                        (Double.parseDouble(pa))));
                            }
                            catch (Exception e) {

                            }
                        }
                        params.put(TPA, paout);
                        String fuel = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FUEL_TYPES)).trim();
                        if(fuel.equals("")) {
                            fuel = ctx.getString(R.string.No);
                        }
                        params.put(FUEL_TYPES, fuel);
                        String ct = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_ATCT)).trim();
                        if(ct.equals("Y")) {
                            ct = ctx.getString(R.string.Yes);
                        }
                        else {
                            ct = ctx.getString(R.string.No);
                        }
                        params.put(CONTROL_TOWER, ct);

                        String unicom = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_UNICOM_FREQUENCIES)).trim();
                        if(!unicom.equals("")) {
                            freq.put(UNICOM, unicom);
                        }
                        String ctaf = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_CTAF_FREQUNCY)).trim();
                        if(!ctaf.equals("")) {
                            freq.put(CTAF, ctaf);
                        }

                        String fee = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_NON_COMMERCIAL_LANDING_FEE)).trim();
                        if(fee.equals("Y")) {
                            fee = ctx.getString(R.string.Yes);
                        }
                        else {
                            fee = ctx.getString(R.string.No);
                        }
                        params.put(LANDING_FEE, fee);
                        String fss = c.getString(c.getColumnIndex(LocationContract.AIRPORTS_FSS_PHONE));
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
        CursorManager.close(c);


        if(!type.equals(Destination.BASE)) {
            return;
        }

        if(null == runways || null == awos || null == freq) {
            return;
        }

        /*
         * Find frequencies (ATIS, TOWER, GROUND, etc)  Not AWOS
         */
        try {
            String qry = LocationContract.AIRPORT_FREQ_LOCATION_ID + " = ? or " + LocationContract.AIRPORT_FREQ_LOCATION_ID + " = ?";
            String arguments[] = new String[] {name, "K" + name};

            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_FREQ, null, qry, arguments, null);
            /*
             * Add all of them
             */
            if(c != null) {
                while(c.moveToNext()) {
                    String typeof = c.getString(c.getColumnIndex(LocationContract.AIRPORT_FREQ_TYPE));
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
                        double frequency = Double.parseDouble(c.getString(c.getColumnIndex(LocationContract.AIRPORT_FREQ_FREQUENCY)));
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
                        freq.put(typeof, freq.get(typeof)+"\n\n" + c.getString(c.getColumnIndex(LocationContract.AIRPORT_FREQ_FREQUENCY)));
                    }
                    else {
                        freq.put(typeof, c.getString(c.getColumnIndex(LocationContract.AIRPORT_FREQ_FREQUENCY)));
                    }
                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

		/*
		 * Get AWOS info
		 */
        try {
			/*
			 * Add each AWOS
			 */
            String qry = LocationContract.AIRPORT_AWOS_LOCATION_ID + " = ? or " + LocationContract.AIRPORT_AWOS_LOCATION_ID + " = ?";
            String arguments[] = new String[] {name, "K" + name};
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_AWOS, null, qry, arguments, null);

            if (c != null) {
                while (c.moveToNext()) {

                    Awos a = new Awos(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_LOCATION_ID))); // New AWOS instance

                    a.setType(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_TYPE)));

                    a.setLat(Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_LATITUDE))));
                    a.setLon(Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_LONGITUDE))));
                    a.setFreq1(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_FREQUENCY1)));
                    a.setFreq2(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_FREQUENCY2)));
                    a.setPhone1(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_TELEPHONE1)));
                    a.setPhone2(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_TELEPHONE2)));
                    a.setRemark(c.getString(c.getColumnIndex(LocationContract.AIRPORT_AWOS_REMARK)));

                    awos.add(a);

                }
            }
        }
        catch (Exception e) {
        }
        CursorManager.close(c);

        /*
         *Find runways
         */
        try {
            String qry = LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ? or " + LocationContract.AIRPORT_RUNWAYS_LOCATION_ID + " = ?";
            String arguments[] = new String[] {name, "K" + name};
            c = ctx.getContentResolver().query(LocationContract.CONTENT_URI_AIRPORT_RUNWAYS, null, qry, arguments, null);

            /*
             * Add all of them
             */
            if(c != null) {
                while(c.moveToNext()) {

                    String Length = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LENGTH));
                    String Width = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_WIDTH));
                    String Surface = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_SURFACE));
                    String Variation = params.get(MAGNETIC_VARIATION).trim();

                    String run = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_IDENT)));
                    String lat = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_LATITUDE)));
                    String lon = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_LONGITUDE)));

                    String Elevation = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_ELEVATION));
                    if(Elevation.equals("")) {
                        Elevation = params.get(ELEVATION);
                    }
                    String Heading = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_HEADING));
                    String DT = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_DT));
                    if(DT.equals("")) {
                        DT = "0";
                    }
                    String Lighted = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_LIGHTS));
                    if(Lighted.equals("0") || Lighted.equals("")) {
                        Lighted = ctx.getString(R.string.No);
                    }
                    String ILS = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_ILS));
                    if(ILS.equals("")) {
                        ILS = ctx.getString(R.string.No);
                    }
                    String VGSI = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_VGSI));
                    if(VGSI.equals("")) {
                        VGSI = ctx.getString(R.string.No);
                    }
                    String Pattern = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_LE_PATTERN));
                    if(Pattern.equals("Y")) {
                        Pattern = ctx.getString(R.string.Right);
                    }
                    else {
                        Pattern = ctx.getString(R.string.Left);
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
                        run = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_IDENT)));
                        lat = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_LATITUDE)));
                        lon = Helper.removeLeadingZeros(c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_LONGITUDE)));

                        Elevation = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_ELEVATION));
                        if(Elevation.equals("")) {
                            Elevation = params.get(ELEVATION);
                        }
                        Heading = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_HEADING));
                        DT = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_DT));
                        if(DT.equals("")) {
                            DT = "0";
                        }
                        Lighted = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_LIGHTS));
                        if(Lighted.equals("0") || Lighted.equals("")) {
                            Lighted = ctx.getString(R.string.No);
                        }
                        ILS = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_ILS));
                        if(ILS.equals("")) {
                            ILS = ctx.getString(R.string.No);
                        }
                        VGSI = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_VGSI));
                        if(VGSI.equals("")) {
                            VGSI = ctx.getString(R.string.No);
                        }
                        Pattern = c.getString(c.getColumnIndex(LocationContract.AIRPORT_RUNWAYS_HE_PATTERN));
                        if(Pattern.equals("Y")) {
                            Pattern = ctx.getString(R.string.Right);
                        }
                        else {
                            Pattern = ctx.getString(R.string.Left);
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
        CursorManager.close(c);

    }

}

