package com.ds.avare.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.ds.avare.flight.Checklist;
import com.ds.avare.flight.WeightAndBalance;
import com.ds.avare.place.Obstacle;
import com.ds.avare.plan.Cifp;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.TreeMap;


/**
 * Created by zkhan on 2/8/17.
 */

public class ContentProviderHelper {


    public static LinkedList<Obstacle> getObstacles(final Context ctx, double longitude, double latitude, double height) {

        Cursor c = null;
        LinkedList<Obstacle> ret = new LinkedList<Obstacle>();

        String qry =
                "(" + ObstaclesContract.HEIGHT + " > ?)" + " and " +
                        "(" + ObstaclesContract.LATITUDE + " > ?)" + " and " +
                        "(" + ObstaclesContract.LATITUDE + " < ?)" + " and " +
                        "(" + ObstaclesContract.LONGITUDE + " > ?)" + " and " +
                        "(" + ObstaclesContract.LONGITUDE + " < ?)";


        String v0 = String.valueOf(height - (int) Obstacle.HEIGHT_BELOW);
        String v1 = String.valueOf(latitude - Obstacle.RADIUS);
        String v2 = String.valueOf(latitude + Obstacle.RADIUS);
        String v3 = String.valueOf(longitude - Obstacle.RADIUS);
        String v4 = String.valueOf(longitude + Obstacle.RADIUS);

        String arguments[] = new String[] {v0, v1, v2, v3, v4};

        try {
            c = ctx.getContentResolver().query(ObstaclesContract.CONTENT_URI, null, qry, arguments, null);
            if (c != null) {
                while (c.moveToNext()) {
                    ret.add(new Obstacle(
                            c.getFloat(c.getColumnIndex(ObstaclesContract.LONGITUDE)),
                            c.getFloat(c.getColumnIndex(ObstaclesContract.LATITUDE)),
                            (int) c.getFloat(c.getColumnIndex(ObstaclesContract.HEIGHT))));
                }
            }

        }
        catch (Exception e) {

        }
        CursorManager.close(c);
        return ret;
    }


    /**
     *
     * @return
     */
    public static LinkedList<Cifp> findProcedure(final Context ctx, String name, String approach) {

        Cursor c = null;
        TreeMap<String, Cifp> map = new TreeMap<String, Cifp>();
        String params[] = Cifp.getParams(approach);
        if(params[0] == null || params[1] == null) {
            return new LinkedList<Cifp>();
        }

        String qry =
                "((" + ProceduresContract.AIRPORT + " = ?) or (" + ProceduresContract.AIRPORT + " = ?)) and " +
                        "(" + ProceduresContract.APPROACH_TYPE + " like ?) and " +
                        "(" + ProceduresContract.RUNWAY + " like ?)";

        String arguments[] = new String[] {name, "K" + name, params[0] + "%", "%" + params[1] + "%"};

        try {
            c = ctx.getContentResolver().query(ProceduresContract.CONTENT_URI, null, qry, arguments, null);
            if (c != null) {
                while (c.moveToNext()) {
                    Cifp cifp = new Cifp(
                            name,
                            c.getString(c.getColumnIndex(ProceduresContract.INITIAL_COURSE)),
                            c.getString(c.getColumnIndex(ProceduresContract.INITIAL_ALTITUDE)),
                            c.getString(c.getColumnIndex(ProceduresContract.FINAL_COURSE)),
                            c.getString(c.getColumnIndex(ProceduresContract.FINAL_ALTITUDE)),
                            c.getString(c.getColumnIndex(ProceduresContract.MISSED_COURSE)),
                            c.getString(c.getColumnIndex(ProceduresContract.MISSED_ALTITUDE))
                    );
                    map.put(cifp.getInitialCourse(), cifp);
                }
            }
        }
        catch (Exception e) {

        }

        CursorManager.close(c);
        return new LinkedList<Cifp>(map.values());
    }



    /**
     *
     * @param station
     * @return
     */
    public static Taf getTaf(Context ctx, String station) {

        Cursor c = null;
        Taf taf = null;

        String qry = WeatherContract.TAF_STATION + " = ?";

        String arguments[] = new String[] {"K" + station};

        try {
            c = ctx.getContentResolver().query(WeatherContract.CONTENT_URI_TAF, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToFirst()) {

                    taf = new Taf();
                    taf.rawText = c.getString(c.getColumnIndex(WeatherContract.TAF_TEXT));
                    taf.time = c.getString(c.getColumnIndex(WeatherContract.TAF_TIME));
                    taf.stationId = c.getString(c.getColumnIndex(WeatherContract.TAF_STATION));
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return taf;
    }

    /**
     *
     * @param station
     * @return
     */
    public static Metar getMetar(Context ctx, String station) {

        Cursor c = null;
        Metar metar = null;

        String qry = WeatherContract.METAR_STATION + " = ?";

        String arguments[] = new String[] {"K" + station};

        try {
            c = ctx.getContentResolver().query(WeatherContract.CONTENT_URI_METAR, null, qry, arguments, null);
            if(c != null) {
                if(c.moveToFirst()) {

                    metar = new Metar();
                    metar.rawText = c.getString(c.getColumnIndex(WeatherContract.METAR_TEXT));
                    metar.time = c.getString(c.getColumnIndex(WeatherContract.METAR_TIME));
                    metar.stationId = c.getString(c.getColumnIndex(WeatherContract.METAR_STATION));
                    metar.flightCategory = c.getString(c.getColumnIndex(WeatherContract.METAR_FLIGHT_CATEGORY));
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return metar;
    }


    /**
     *
     * @return
     */
    public static LinkedList<AirSigMet> getAirSigMets(Context ctx) {

        Cursor c = null;
        LinkedList<AirSigMet> airsig = new LinkedList<AirSigMet>();

        /*
         * Get all
         */
        try {
            c = ctx.getContentResolver().query(WeatherContract.CONTENT_URI_AIRMET, null, null, null, null);
            if(c != null) {
                while(c.moveToNext()) {
                    AirSigMet a = new AirSigMet();
                    a.rawText = c.getString(c.getColumnIndex(WeatherContract.AIRMET_TEXT));
                    a.timeFrom = c.getString(c.getColumnIndex(WeatherContract.AIRMET_TIME_FROM));
                    a.timeTo = c.getString(c.getColumnIndex(WeatherContract.AIRMET_TIME_TO));
                    a.points = c.getString(c.getColumnIndex(WeatherContract.AIRMET_POINTS));
                    a.minFt = c.getString(c.getColumnIndex(WeatherContract.AIRMET_MSL_MIN));
                    a.maxFt = c.getString(c.getColumnIndex(WeatherContract.AIRMET_MSL_MAX));
                    a.movementDeg = c.getString(c.getColumnIndex(WeatherContract.AIRMET_MOVEMENT_DIRECTION));
                    a.movementKt = c.getString(c.getColumnIndex(WeatherContract.AIRMET_MOVEMENT_SPEED));
                    a.hazard = c.getString(c.getColumnIndex(WeatherContract.AIRMET_HAZARD));
                    a.severity = c.getString(c.getColumnIndex(WeatherContract.AIRMET_SEVERITY));
                    a.reportType = c.getString(c.getColumnIndex(WeatherContract.AIRMET_TYPE));
                    airsig.add(a);
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return airsig;
    }

    /**
     *
     * @return
     */
    public static LinkedList<Airep> getAireps(Context ctx, double longitude, double latitude) {

        Cursor c = null;
        HashMap<String, Airep> aireps = new HashMap<>();

        /*
         * All aireps/pireps sep by \n
         */

        String qry = "(" + WeatherContract.PIREP_LATITUDE + " > ?)" + " and " +
                        "(" + WeatherContract.PIREP_LATITUDE + " < ?)" + " and " +
                        "(" + WeatherContract.PIREP_LONGITUDE + " > ?)" + " and " +
                        "(" + WeatherContract.PIREP_LONGITUDE + " < ?)";

        String v0 = String.valueOf(latitude - Airep.RADIUS);
        String v1 = String.valueOf(latitude + Airep.RADIUS);
        String v2 = String.valueOf(longitude - Airep.RADIUS);
        String v3 = String.valueOf(longitude + Airep.RADIUS);

        String arguments[] = new String[] {v0, v1, v2, v3};


        try {
            c = ctx.getContentResolver().query(WeatherContract.CONTENT_URI_PIREP, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    Airep a = new Airep();
                    a.rawText = c.getString(c.getColumnIndex(WeatherContract.PIREP_TEXT));
                    a.time = c.getString(c.getColumnIndex(WeatherContract.PIREP_TIME));
                    a.lon = c.getFloat(c.getColumnIndex(WeatherContract.PIREP_LONGITUDE));
                    a.lat = c.getFloat(c.getColumnIndex(WeatherContract.PIREP_LATITUDE));
                    a.reportType = c.getString(c.getColumnIndex(WeatherContract.PIREP_TYPE));
                    aireps.put(a.rawText, a);
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);

        // PIREPs can be duplicate. Make unique, and sort
        LinkedList<Airep> list = new LinkedList<Airep>(aireps.values());
        Collections.sort(list, new Comparator<Airep>() {
            @Override
            public int compare(Airep a1, Airep a2) {
                return a1.rawText.compareTo(a2.rawText);
            }
        });
        return list;
    }


    /**
     *
     * @param lon
     * @param lat
     * @return
     */
    public static WindsAloft getWindsAloft(Context ctx, double lon, double lat) {

        Cursor c = null;
        WindsAloft wa = null;

        // crude distance formula
        String order = "(" +
                "(" + WeatherContract.WIND_LONGITUDE + " - " + lon + ")*" + "(" + WeatherContract.WIND_LONGITUDE + " - " + lon + ") + " +
                "(" + WeatherContract.WIND_LATITUDE  + " - " + lat + ")*" + "(" + WeatherContract.WIND_LATITUDE  + " - " + lat + ") " +
                ") limit 1";



        try {
            c = ctx.getContentResolver().query(WeatherContract.CONTENT_URI_WIND, null, null, null, order);
            if(c != null) {
                if(c.moveToFirst()) {

                    wa = new WindsAloft();
                    wa.station = c.getString(c.getColumnIndex(WeatherContract.WIND_STATION));
                    wa.time = c.getString(c.getColumnIndex(WeatherContract.WIND_TIME));
                    wa.lon = c.getFloat(c.getColumnIndex(WeatherContract.WIND_LONGITUDE));
                    wa.lat = c.getFloat(c.getColumnIndex(WeatherContract.WIND_LATITUDE));
                    wa.w3k = c.getString(c.getColumnIndex(WeatherContract.WIND_3K)).replaceAll("[ ]", "");
                    wa.w6k = c.getString(c.getColumnIndex(WeatherContract.WIND_6K)).replaceAll("[ ]", "");
                    wa.w9k = c.getString(c.getColumnIndex(WeatherContract.WIND_9K)).replaceAll("[ ]", "");
                    wa.w12k = c.getString(c.getColumnIndex(WeatherContract.WIND_12K)).replaceAll("[ ]", "");
                    wa.w18k = c.getString(c.getColumnIndex(WeatherContract.WIND_18K)).replaceAll("[ ]", "");
                    wa.w24k = c.getString(c.getColumnIndex(WeatherContract.WIND_24K)).replaceAll("[ ]", "");
                    wa.w30k = c.getString(c.getColumnIndex(WeatherContract.WIND_30K)).replaceAll("[ ]", "");
                    wa.w34k = c.getString(c.getColumnIndex(WeatherContract.WIND_34K)).replaceAll("[ ]", "");
                    wa.w39k = c.getString(c.getColumnIndex(WeatherContract.WIND_39K)).replaceAll("[ ]", "");
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return wa;
    }

    /**
     *
     * @return
     */
    public static LinkedList<LabelCoordinate> findGameTFRs(Context ctx) {

        Cursor c = null;
        LinkedList<LabelCoordinate> ret = new LinkedList<LabelCoordinate>();

        // Find -6 hours to +12 hours
        Calendar begin = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        begin.add(Calendar.HOUR_OF_DAY, -6);
        end.add(Calendar.HOUR_OF_DAY, 12);

        long mb = begin.getTimeInMillis();
        long me = end.getTimeInMillis();

        SimpleDateFormat formatterZulu = new SimpleDateFormat("ddHH:mm'Z'");
        formatterZulu.setTimeZone(TimeZone.getTimeZone("GMT"));

        String qry = GameTfrContract.TIME + " between ? and ?";

        String arguments[] = new String[] {String.valueOf(mb), String.valueOf(me)};


        try {
            c = ctx.getContentResolver().query(GameTfrContract.CONTENT_URI, null, qry, arguments, null);
            if(c != null) {
                while(c.moveToNext()) {
                    long time = c.getLong(c.getColumnIndex(GameTfrContract.TIME));
                    // print in zulu
                    String toprint = formatterZulu.format(new Date(time));

                    LabelCoordinate lc = new LabelCoordinate(
                            c.getFloat(c.getColumnIndex(GameTfrContract.LONGITUDE)),
                            c.getFloat(c.getColumnIndex(GameTfrContract.LATITUDE)),
                            toprint + " " + c.getString(c.getColumnIndex(GameTfrContract.STADIUM)));
                    ret.add(lc);
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static void setUserLists(Context ctx, LinkedList<Checklist> lists) {
        for (Checklist l : lists) {
            setUserList(ctx, l);
        }
    }

    public static void setUserList(Context ctx, Checklist list) {

        ContentValues newValues = new ContentValues();

        newValues.put(UserContract.LIST_COLUMN_ID, list.getName());
        newValues.put(UserContract.LIST_COLUMN_TEXT, list.getSteps());
        ctx.getContentResolver().insert(UserContract.CONTENT_URI_LIST, newValues);
    }

    public static void deleteUserList(Context ctx, String name) {
        String selection = "(" + UserContract.LIST_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{name};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_LIST, selection, selectionArg);
    }

    public static Checklist getUserList(Context ctx, String name) {
        Cursor c = null;
        Checklist ret = null;

        String selection = UserContract.LIST_COLUMN_ID + " = ?";
        String[] selectionArgs = new String[]{name};

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_LIST, null, selection, selectionArgs, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String text = c.getString(c.getColumnIndex(UserContract.LIST_COLUMN_TEXT));
                    String id = c.getString(c.getColumnIndex(UserContract.LIST_COLUMN_ID));
                    ret = new Checklist(id, text);
                    break;
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static LinkedList<Checklist> getUserLists(Context ctx) {
        Cursor c = null;
        LinkedList<Checklist> ret = new LinkedList<>();

        String[] proj = new String[]{UserContract.LIST_COLUMN_ID, UserContract.LIST_COLUMN_TEXT};
        String order = UserContract.LIST_COLUMN_ID + " asc";

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_LIST, proj, null, null, order);
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndex(UserContract.LIST_COLUMN_ID));
                    String text = c.getString(c.getColumnIndex(UserContract.LIST_COLUMN_TEXT));
                    ret.add(new Checklist(name, text));
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static void setUserWnb(Context ctx, WeightAndBalance wnb) {
        ContentValues newValues = new ContentValues();

        newValues.put(UserContract.WNB_COLUMN_ID, wnb.getName());
        newValues.put(UserContract.WNB_COLUMN_TEXT, wnb.getJSON().toString());
        ctx.getContentResolver().insert(UserContract.CONTENT_URI_WNB, newValues);
    }

    public static void setUserWnbs(Context ctx, LinkedList<WeightAndBalance> wnbs) {
        for (WeightAndBalance w : wnbs) {
            setUserWnb(ctx, w);
        }
    }

    public static void deleteUserWnb(Context ctx, String name) {
        String selection = "(" + UserContract.WNB_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{name};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_WNB, selection, selectionArg);
    }


    public static LinkedList<WeightAndBalance> getUserWnbs(Context ctx) {
        Cursor c = null;
        LinkedList<WeightAndBalance> ret = new LinkedList<>();

        String[] proj = new String[]{UserContract.WNB_COLUMN_ID, UserContract.WNB_COLUMN_TEXT};
        String order = UserContract.WNB_COLUMN_ID + " asc";

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_WNB, proj, null, null, order);
            if (c != null) {
                while (c.moveToNext()) {
                    String text = c.getString(c.getColumnIndex(UserContract.WNB_COLUMN_TEXT));
                    ret.add(new WeightAndBalance(text));
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static WeightAndBalance getUserWnb(Context ctx, String name) {
        Cursor c = null;
        WeightAndBalance ret = null;

        String selection = UserContract.WNB_COLUMN_ID + " = ?";
        String[] selectionArgs = new String[]{name};

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_WNB, null, selection, selectionArgs, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String text = c.getString(c.getColumnIndex(UserContract.WNB_COLUMN_TEXT));
                    ret = new WeightAndBalance(text);
                    break;
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static void deleteUserPlan(Context ctx, String name) {
        String selection = "(" + UserContract.PLAN_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{name};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_PLAN, selection, selectionArg);
    }

    public static void setUserPlans(Context ctx, LinkedHashMap<String, String> plans) {

        for (String key : plans.keySet()) {
            ContentValues newValues = new ContentValues();

            newValues.put(UserContract.PLAN_COLUMN_ID, key);
            newValues.put(UserContract.PLAN_COLUMN_PATH, plans.get(key));
            ctx.getContentResolver().insert(UserContract.CONTENT_URI_PLAN, newValues);
        }
    }

    public static LinkedHashMap<String, String> getUserPlans(Context ctx) {
        Cursor c = null;
        LinkedHashMap<String, String> ret = new LinkedHashMap();

        String[] proj = new String[]{UserContract.PLAN_COLUMN_ID, UserContract.PLAN_COLUMN_PATH};
        String order = UserContract.PLAN_COLUMN_ID + " asc";

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_PLAN, proj, null, null, order);
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndex(UserContract.PLAN_COLUMN_ID));
                    String path = c.getString(c.getColumnIndex(UserContract.PLAN_COLUMN_PATH));
                    ret.put(name, path);
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static String[] getUserRecents(Context ctx) {
        Cursor c = null;
        LinkedList<String> ret = new LinkedList<>();

        String[] proj = new String[]{UserContract.RECENT_COLUMN_WID, UserContract.RECENT_COLUMN_DESTTYPE, UserContract.RECENT_COLUMN_DBTYPE, UserContract.RECENT_COLUMN_NAME};
        String order = UserContract.RECENT_COLUMN_ID + " desc";

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_RECENT, proj, null, null, order);
            if (c != null) {
                while (c.moveToNext()) {
                    StringPreference s = new StringPreference(
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_DESTTYPE)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_DBTYPE)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_NAME)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_WID)));

                    ret.add(s.getHashedName());
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);

        return ret.toArray(new String[ret.size()]);
    }

    public static void setUserRecent(Context ctx, StringPreference s) {
        ContentValues newValues = new ContentValues();

        newValues.put(UserContract.RECENT_COLUMN_WID, s.getId());
        newValues.put(UserContract.RECENT_COLUMN_DESTTYPE, s.getType());
        newValues.put(UserContract.RECENT_COLUMN_DBTYPE, s.getDbType());
        newValues.put(UserContract.RECENT_COLUMN_NAME, s.getName());
        ctx.getContentResolver().insert(UserContract.CONTENT_URI_RECENT, newValues);
    }

    public static void setUserRecents(Context ctx, LinkedList<StringPreference> recents) {
        for (StringPreference s : recents) {
            setUserRecent(ctx, s);
        }
    }

    public static void deleteUserRecent(Context ctx, String id) {
        String selection = "(" + UserContract.RECENT_COLUMN_WID + " = ?)";
        String[] selectionArg = new String[]{id};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_RECENT, selection, selectionArg);
    }

    /**
     * @return
     */
    public static StringPreference getUserRecent(Context ctx, String id) {

        Cursor c = null;
        String qry = UserContract.RECENT_COLUMN_WID + " like ?";
        StringPreference s = null;

        String arguments[] = new String[]{id};

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_RECENT, null, qry, arguments, null);
            if (c != null) {
                while (c.moveToNext()) {
                    s = new StringPreference(
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_DESTTYPE)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_DBTYPE)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_NAME)),
                            c.getString(c.getColumnIndex(UserContract.RECENT_COLUMN_WID)));
                    break;
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return s;
    }

    public static void replaceUserRecentName(Context ctx, String id, String newName) {
        ContentValues newValues = new ContentValues();

        String selection = UserContract.RECENT_COLUMN_WID + " = ?";
        String selectionArg[] = new String[]{id};

        newValues.put(UserContract.RECENT_COLUMN_WID, newName);
        ctx.getContentResolver().update(UserContract.CONTENT_URI_RECENT, newValues, selection, selectionArg);
    }

    public static void setUserTag(Context ctx, String name, String tag) {
        ContentValues newValues = new ContentValues();

        newValues.put(UserContract.TAG_COLUMN_ID, name);
        newValues.put(UserContract.TAG_COLUMN_TEXT, tag);
        ctx.getContentResolver().insert(UserContract.CONTENT_URI_TAG, newValues);
    }

    public static void deleteUserTag(Context ctx, String name) {
        String selection = "(" + UserContract.TAG_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{name};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_TAG, selection, selectionArg);
    }

    public static String getUserTag(Context ctx, String name) {
        Cursor c = null;
        String ret = null;

        String selection = UserContract.TAG_COLUMN_ID + " = ?";
        String[] selectionArgs = new String[]{name};

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_TAG, null, selection, selectionArgs, null);
            if (c != null) {
                while (c.moveToNext()) {
                    ret = c.getString(c.getColumnIndex(UserContract.TAG_COLUMN_TEXT));
                    break;
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);
        return ret;
    }

    public static void setUserTags(Context ctx, HashMap<String, String> tags) {
        for (String key : tags.keySet()) {
            setUserTag(ctx, key, tags.get(key));
        }
    }
}