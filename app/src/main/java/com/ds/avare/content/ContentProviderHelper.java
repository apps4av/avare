package com.ds.avare.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.ds.avare.flight.Aircraft;
import com.ds.avare.flight.Checklist;
import com.ds.avare.flight.WeightAndBalance;
import com.ds.avare.place.Obstacle;
import com.ds.avare.plan.Cifp;
import com.ds.avare.position.Coordinate;
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

    static int getIndex(Cursor c, String id) {
        return c.getColumnIndex(id);
    }

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
                            c.getFloat(getIndex(c, ObstaclesContract.LONGITUDE)),
                            c.getFloat(getIndex(c, ObstaclesContract.LATITUDE)),
                            (int) c.getFloat(getIndex(c, ObstaclesContract.HEIGHT))));
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
                            c.getString(getIndex(c, ProceduresContract.INITIAL_COURSE)),
                            c.getString(getIndex(c, ProceduresContract.INITIAL_ALTITUDE)),
                            c.getString(getIndex(c, ProceduresContract.FINAL_COURSE)),
                            c.getString(getIndex(c, ProceduresContract.FINAL_ALTITUDE)),
                            c.getString(getIndex(c, ProceduresContract.MISSED_COURSE)),
                            c.getString(getIndex(c, ProceduresContract.MISSED_ALTITUDE))
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
                    taf.setRawText(c.getString(getIndex(c, WeatherContract.TAF_TEXT)));
                    taf.setTime(c.getString(getIndex(c, WeatherContract.TAF_TIME)));
                    taf.setStationId(c.getString(getIndex(c, WeatherContract.TAF_STATION)));
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
                    metar.setRawText(c.getString(getIndex(c, WeatherContract.METAR_TEXT)));
                    metar.setTime(c.getString(getIndex(c, WeatherContract.METAR_TIME)));
                    metar.setStationId(c.getString(getIndex(c, WeatherContract.METAR_STATION)));
                    metar.setFlightCategory(c.getString(getIndex(c, WeatherContract.METAR_FLIGHT_CATEGORY)));
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
                    a.setRawText(c.getString(getIndex(c, WeatherContract.AIRMET_TEXT)));
                    a.setTimeFrom(c.getString(getIndex(c, WeatherContract.AIRMET_TIME_FROM)));
                    a.setTimeTo(c.getString(getIndex(c, WeatherContract.AIRMET_TIME_TO)));
                    a.setPoints(c.getString(getIndex(c, WeatherContract.AIRMET_POINTS)));
                    a.setMinFt(c.getString(getIndex(c, WeatherContract.AIRMET_MSL_MIN)));
                    a.setMaxFt(c.getString(getIndex(c, WeatherContract.AIRMET_MSL_MAX)));
                    a.setMovementDeg(c.getString(getIndex(c, WeatherContract.AIRMET_MOVEMENT_DIRECTION)));
                    a.setMovementKt(c.getString(getIndex(c, WeatherContract.AIRMET_MOVEMENT_SPEED)));
                    a.setHazard(c.getString(getIndex(c, WeatherContract.AIRMET_HAZARD)));
                    a.setSeverity(c.getString(getIndex(c, WeatherContract.AIRMET_SEVERITY)));
                    a.setReportType(c.getString(getIndex(c, WeatherContract.AIRMET_TYPE)));
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
                    a.setRawText(c.getString(getIndex(c, WeatherContract.PIREP_TEXT)));
                    a.setTime(c.getString(getIndex(c, WeatherContract.PIREP_TIME)));
                    a.setLon(c.getFloat(getIndex(c, WeatherContract.PIREP_LONGITUDE)));
                    a.setLat(c.getFloat(getIndex(c, WeatherContract.PIREP_LATITUDE)));
                    a.setReportType(c.getString(getIndex(c, WeatherContract.PIREP_TYPE)));
                    aireps.put(a.getRawText(), a);
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
                return a1.getRawText().compareTo(a2.getRawText());
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
                    wa.setStation(c.getString(getIndex(c, WeatherContract.WIND_STATION)));
                    wa.setTime(c.getString(getIndex(c, WeatherContract.WIND_TIME)));
                    wa.setLon(c.getFloat(getIndex(c, WeatherContract.WIND_LONGITUDE)));
                    wa.setLat(c.getFloat(getIndex(c, WeatherContract.WIND_LATITUDE)));
                    wa.setW3k(c.getString(getIndex(c, WeatherContract.WIND_3K)).replaceAll("[ ]", ""));
                    wa.setW6k(c.getString(getIndex(c, WeatherContract.WIND_6K)).replaceAll("[ ]", ""));
                    wa.setW9k(c.getString(getIndex(c, WeatherContract.WIND_9K)).replaceAll("[ ]", ""));
                    wa.setW12k(c.getString(getIndex(c, WeatherContract.WIND_12K)).replaceAll("[ ]", ""));
                    wa.setW18k(c.getString(getIndex(c, WeatherContract.WIND_18K)).replaceAll("[ ]", ""));
                    wa.setW24k(c.getString(getIndex(c, WeatherContract.WIND_24K)).replaceAll("[ ]", ""));
                    wa.setW30k(c.getString(getIndex(c, WeatherContract.WIND_30K)).replaceAll("[ ]", ""));
                    wa.setW34k(c.getString(getIndex(c, WeatherContract.WIND_34K)).replaceAll("[ ]", ""));
                    wa.setW39k(c.getString(getIndex(c, WeatherContract.WIND_39K)).replaceAll("[ ]", ""));
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
                    long time = c.getLong(getIndex(c, GameTfrContract.TIME));
                    // print in zulu
                    String toprint = formatterZulu.format(new Date(time));

                    LabelCoordinate lc = new LabelCoordinate(
                            c.getFloat(getIndex(c, GameTfrContract.LONGITUDE)),
                            c.getFloat(getIndex(c, GameTfrContract.LATITUDE)),
                            toprint + " " + c.getString(getIndex(c, GameTfrContract.STADIUM)));
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
                    String text = c.getString(getIndex(c, UserContract.LIST_COLUMN_TEXT));
                    String id = c.getString(getIndex(c, UserContract.LIST_COLUMN_ID));
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
                    String name = c.getString(getIndex(c, UserContract.LIST_COLUMN_ID));
                    String text = c.getString(getIndex(c, UserContract.LIST_COLUMN_TEXT));
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
                    String text = c.getString(getIndex(c, UserContract.WNB_COLUMN_TEXT));
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
                    String text = c.getString(getIndex(c, UserContract.WNB_COLUMN_TEXT));
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
                    String name = c.getString(getIndex(c, UserContract.PLAN_COLUMN_ID));
                    String path = c.getString(getIndex(c, UserContract.PLAN_COLUMN_PATH));
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
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_DESTTYPE)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_DBTYPE)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_NAME)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_WID)));

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
    
    public static LinkedList<Coordinate> getUserDraw(Context ctx) {
        Cursor c = null;
        LinkedList<Coordinate> ret = new LinkedList<>();

        String[] proj = new String[]{UserContract.DRAW_COLUMN_POINTS_X, UserContract.DRAW_COLUMN_POINTS_Y, UserContract.DRAW_COLUMN_SEP};
        String order = UserContract.DRAW_COLUMN_ID + " asc";

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_DRAW, proj, null, null, order);
            if (c != null) {
                while (c.moveToNext()) {
                    Coordinate p = new Coordinate(c.getFloat(getIndex(c, UserContract.DRAW_COLUMN_POINTS_X)),
                            c.getFloat(getIndex(c, UserContract.DRAW_COLUMN_POINTS_Y)));
                    int sep = c.getInt(getIndex(c, UserContract.DRAW_COLUMN_SEP));
                    if(0 != sep) {
                        p.makeSeparate();
                    }
                    ret.add(p);
                }
            }
        } catch (Exception e) {
        }

        CursorManager.close(c);

        return ret;
    }

    public static void setUserDraw(Context ctx, LinkedList<Coordinate> points) {

        // delete all
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_DRAW, null, null);

        for (Coordinate p : points) {
            ContentValues newValues = new ContentValues();

            newValues.put(UserContract.DRAW_COLUMN_POINTS_X, (float)p.getLatitude());
            newValues.put(UserContract.DRAW_COLUMN_POINTS_Y, (float)p.getLongitude());
            newValues.put(UserContract.DRAW_COLUMN_SEP, p.isSeparate() ? 1 : 0);
            ctx.getContentResolver().insert(UserContract.CONTENT_URI_DRAW, newValues);
        }
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
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_DESTTYPE)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_DBTYPE)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_NAME)),
                            c.getString(getIndex(c, UserContract.RECENT_COLUMN_WID)));
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
                    ret = c.getString(getIndex(c, UserContract.TAG_COLUMN_TEXT));
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

    /**
     *
     */
    public static LinkedList<Aircraft> getUserAircraft(Context ctx) {

        Cursor c = null;
        LinkedList<Aircraft> aircraft = new LinkedList<>();

        try {
            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_AIRCRAFT, null, null, null, null);
            if(c != null) {
                while(c.moveToNext()) {

                    Aircraft a = new Aircraft();

                    a.setId(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_ID)));
                    a.setType(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_TYPE)));
                    a.setWake(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_WAKE)));
                    a.setEquipment(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_EQUIPMENT)));
                    a.setICao(c.getInt(getIndex(c, UserContract.AIRCRAFT_COLUMN_ICAO)));
                    a.setCruiseTas(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_CRUISE_TAS)));
                    a.setSurveillance(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_SURVEILLANCE)));
                    a.setEndurance(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_FUEL_ENDURANCE)));
                    a.setColor(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_COLOR)));
                    a.setPic(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_PIC)));
                    a.setPilotInfo(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_PILOT)));
                    a.setSinkRate(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_SINK_RATE)));
                    a.setFuelBurnRate(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_FUEL_BURN)));
                    a.setHomeBase(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_BASE)));
                    aircraft.add(a);
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return aircraft;
    }


    /**
     *
     */
    public static Aircraft getUserAircraft(Context ctx, String id) {

        Cursor c = null;
        Aircraft a = null;

        String selection = "(" + UserContract.AIRCRAFT_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{id};

        try {

            c = ctx.getContentResolver().query(UserContract.CONTENT_URI_AIRCRAFT, null, selection, selectionArg, null);
            if(c != null) {
                if(c.moveToNext()) {

                    a = new Aircraft();

                    a.setId(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_ID)));
                    a.setType(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_TYPE)));
                    a.setWake(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_WAKE)));
                    a.setEquipment(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_EQUIPMENT)));
                    a.setICao(c.getInt(getIndex(c, UserContract.AIRCRAFT_COLUMN_ICAO)));
                    a.setCruiseTas(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_CRUISE_TAS)));
                    a.setSurveillance(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_SURVEILLANCE)));
                    a.setEndurance(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_FUEL_ENDURANCE)));
                    a.setColor(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_COLOR)));
                    a.setPic(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_PIC)));
                    a.setPilotInfo(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_PILOT)));
                    a.setSinkRate(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_SINK_RATE)));
                    a.setFuelBurnRate(c.getFloat(getIndex(c, UserContract.AIRCRAFT_COLUMN_FUEL_BURN)));
                    a.setHomeBase(c.getString(getIndex(c, UserContract.AIRCRAFT_COLUMN_BASE)));
                }
            }
        }
        catch (Exception e) {
        }

        CursorManager.close(c);
        return a;
    }

    public static void setUserAircraft(Context ctx, Aircraft a) {
        ContentValues newValues = new ContentValues();

        newValues.put(UserContract.AIRCRAFT_COLUMN_ID, a.getId());
        newValues.put(UserContract.AIRCRAFT_COLUMN_TYPE, a.getType());
        newValues.put(UserContract.AIRCRAFT_COLUMN_WAKE, a.getWake());
        newValues.put(UserContract.AIRCRAFT_COLUMN_EQUIPMENT, a.getEquipment());
        newValues.put(UserContract.AIRCRAFT_COLUMN_ICAO, a.getICao());
        newValues.put(UserContract.AIRCRAFT_COLUMN_CRUISE_TAS, a.getCruiseTas());
        newValues.put(UserContract.AIRCRAFT_COLUMN_SURVEILLANCE, a.getSurveillance());
        newValues.put(UserContract.AIRCRAFT_COLUMN_FUEL_ENDURANCE, a.getEndurance());
        newValues.put(UserContract.AIRCRAFT_COLUMN_COLOR, a.getColor());
        newValues.put(UserContract.AIRCRAFT_COLUMN_PIC, a.getPic());
        newValues.put(UserContract.AIRCRAFT_COLUMN_PILOT, a.getPilotInfo());
        newValues.put(UserContract.AIRCRAFT_COLUMN_SINK_RATE, a.getSinkRate());
        newValues.put(UserContract.AIRCRAFT_COLUMN_FUEL_BURN, a.getFuelBurnRate());
        newValues.put(UserContract.AIRCRAFT_COLUMN_BASE, a.getHomeBase());

        ctx.getContentResolver().insert(UserContract.CONTENT_URI_AIRCRAFT, newValues);
    }

    public static void setUserAircraft(Context ctx, LinkedList<Aircraft> aircraft) {
        for (Aircraft a : aircraft) {
            setUserAircraft(ctx, a);
        }
    }

    public static void deleteUserAircraft(Context ctx, String id) {
        String selection = "(" + UserContract.AIRCRAFT_COLUMN_ID + " = ?)";
        String[] selectionArg = new String[]{id};
        ctx.getContentResolver().delete(UserContract.CONTENT_URI_AIRCRAFT, selection, selectionArg);
    }

}