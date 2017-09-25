package com.ds.avare.content;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.ds.avare.place.Obstacle;
import com.ds.avare.plan.Cifp;
import com.ds.avare.utils.GenericCallback;

import java.util.LinkedList;
import java.util.TreeMap;


/**
 * Created by zkhan on 2/8/17.
 */

public class ContentProviderHelper {

    public static LinkedList<Obstacle> getObstacles(final Context ctx, double longitude, double latitude, double height) {


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
        String v4 = String.valueOf(latitude + Obstacle.RADIUS);

        String arguments[] = new String[] {v0, v1, v2, v3, v4};

        Cursor c = ctx.getContentResolver().query(ObstaclesContract.CONTENT_URI, null, qry, arguments, null);
        if (c != null) {
            while (c.moveToNext()) {
                ret.add(new Obstacle(
                        c.getFloat(c.getColumnIndex(ObstaclesContract.LONGITUDE)),
                        c.getFloat(c.getColumnIndex(ObstaclesContract.LATITUDE)),
                        (int)c.getFloat(c.getColumnIndex(ObstaclesContract.HEIGHT))));
            }
        }

        return ret;
    }


    /**
     *
     * @return
     */
    public static LinkedList<Cifp> findProcedure(final Context ctx, String name, String approach) {

        TreeMap<String, Cifp> map = new TreeMap<String, Cifp>();
        String params[] = Cifp.getParams(approach);
        if(params[0] == null || params[1] == null) {
            return new LinkedList<Cifp>();
        }

        String qry =
                "((" + ProceduresContract.AIRPORT + " = ?) or (" + ProceduresContract.AIRPORT + " = ?)) and " +
                        "(" + ProceduresContract.APPROACH_TYPE + " = ?) and " +
                        "(" + ProceduresContract.RUNWAY + " like ?)";

        String arguments[] = new String[] {name, "K" + name, params[0], "%" + params[1] + "%"};

        Cursor c = ctx.getContentResolver().query(ProceduresContract.CONTENT_URI, null, qry, arguments, null);
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

        return new LinkedList<Cifp>(map.values());
    }




}

