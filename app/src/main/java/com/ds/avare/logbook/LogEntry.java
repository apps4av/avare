/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.logbook;

import android.content.ContentValues;
import android.database.Cursor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One logbook row. Column names match the AvareX CSV so a file exported from
 * either app imports into the other.
 */
public class LogEntry {

    public String id = "";
    public String date = "";
    public String aircraftMakeModel = "";
    public String aircraftIdentification = "";
    public String route = "";
    public double totalFlightTime;
    public double dayTime;
    public double nightTime;
    public double crossCountryTime;
    public double soloTime;
    public double simulatedInstruments;
    public double actualInstruments;
    public double dualReceived;
    public double pilotInCommand;
    public double copilot;
    public double instructor;
    public double examiner;
    public double flightSimulator;
    public int dayLandings;
    public int nightLandings;
    public double holdingProcedures;
    public double groundTime;
    public int instrumentApproaches;
    public String instructorName = "";
    public String instructorCertificate = "";
    public String remarks = "";

    /** Column order used by the CSV header. Same order as AvareX LogEntry.toMap. */
    public static final String[] COLUMNS = {
            "id", "date", "aircraftMakeModel", "aircraftIdentification", "route",
            "totalFlightTime", "dayTime", "nightTime", "crossCountryTime", "soloTime",
            "simulatedInstruments", "actualInstruments", "dualReceived", "pilotInCommand",
            "copilot", "instructor", "examiner", "flightSimulator", "dayLandings",
            "nightLandings", "holdingProcedures", "groundTime", "instrumentApproaches",
            "instructorName", "instructorCertificate", "remarks"
    };

    public String dateLabel() {
        if (date != null && date.length() >= 10) {
            return date.substring(0, 10);
        }
        return date == null ? "" : date;
    }

    public int year() {
        try {
            return Integer.parseInt(dateLabel().substring(0, 4));
        } catch (Exception e) {
            return 0;
        }
    }

    public long epochMs() {
        if (date == null || date.isEmpty()) {
            return 0;
        }
        try {
            return Instant.parse(date).toEpochMilli();
        } catch (Exception ignored) {
        }
        try {
            String day = date.length() >= 10 ? date.substring(0, 10) : date;
            return LocalDate.parse(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }
        return 0;
    }

    public ContentValues toValues() {
        ContentValues v = new ContentValues();
        Map<String, String> map = toMap();
        for (Map.Entry<String, String> e : map.entrySet()) {
            v.put(e.getKey(), e.getValue());
        }
        return v;
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("id", id);
        map.put("date", date);
        map.put("aircraftMakeModel", aircraftMakeModel);
        map.put("aircraftIdentification", aircraftIdentification);
        map.put("route", route);
        map.put("totalFlightTime", num(totalFlightTime));
        map.put("dayTime", num(dayTime));
        map.put("nightTime", num(nightTime));
        map.put("crossCountryTime", num(crossCountryTime));
        map.put("soloTime", num(soloTime));
        map.put("simulatedInstruments", num(simulatedInstruments));
        map.put("actualInstruments", num(actualInstruments));
        map.put("dualReceived", num(dualReceived));
        map.put("pilotInCommand", num(pilotInCommand));
        map.put("copilot", num(copilot));
        map.put("instructor", num(instructor));
        map.put("examiner", num(examiner));
        map.put("flightSimulator", num(flightSimulator));
        map.put("dayLandings", Integer.toString(dayLandings));
        map.put("nightLandings", Integer.toString(nightLandings));
        map.put("holdingProcedures", num(holdingProcedures));
        map.put("groundTime", num(groundTime));
        map.put("instrumentApproaches", Integer.toString(instrumentApproaches));
        map.put("instructorName", instructorName);
        map.put("instructorCertificate", instructorCertificate);
        map.put("remarks", remarks);
        return map;
    }

    public static LogEntry fromCursor(Cursor c) {
        LogEntry e = new LogEntry();
        e.id = str(c, "id");
        e.date = str(c, "date");
        e.aircraftMakeModel = str(c, "aircraftMakeModel");
        e.aircraftIdentification = str(c, "aircraftIdentification");
        e.route = str(c, "route");
        e.totalFlightTime = dbl(c, "totalFlightTime");
        e.dayTime = dbl(c, "dayTime");
        e.nightTime = dbl(c, "nightTime");
        e.crossCountryTime = dbl(c, "crossCountryTime");
        e.soloTime = dbl(c, "soloTime");
        e.simulatedInstruments = dbl(c, "simulatedInstruments");
        e.actualInstruments = dbl(c, "actualInstruments");
        e.dualReceived = dbl(c, "dualReceived");
        e.pilotInCommand = dbl(c, "pilotInCommand");
        e.copilot = dbl(c, "copilot");
        e.instructor = dbl(c, "instructor");
        e.examiner = dbl(c, "examiner");
        e.flightSimulator = dbl(c, "flightSimulator");
        e.dayLandings = integer(c, "dayLandings");
        e.nightLandings = integer(c, "nightLandings");
        e.holdingProcedures = dbl(c, "holdingProcedures");
        e.groundTime = dbl(c, "groundTime");
        e.instrumentApproaches = integer(c, "instrumentApproaches");
        e.instructorName = str(c, "instructorName");
        e.instructorCertificate = str(c, "instructorCertificate");
        e.remarks = str(c, "remarks");
        return e;
    }

    public static LogEntry fromMap(Map<String, String> map) {
        LogEntry e = new LogEntry();
        e.id = text(map, "id");
        e.date = text(map, "date");
        e.aircraftMakeModel = text(map, "aircraftMakeModel");
        e.aircraftIdentification = text(map, "aircraftIdentification");
        e.route = text(map, "route");
        e.totalFlightTime = number(map, "totalFlightTime");
        e.dayTime = number(map, "dayTime");
        e.nightTime = number(map, "nightTime");
        e.crossCountryTime = number(map, "crossCountryTime");
        e.soloTime = number(map, "soloTime");
        e.simulatedInstruments = number(map, "simulatedInstruments");
        e.actualInstruments = number(map, "actualInstruments");
        e.dualReceived = number(map, "dualReceived");
        e.pilotInCommand = number(map, "pilotInCommand");
        e.copilot = number(map, "copilot");
        e.instructor = number(map, "instructor");
        e.examiner = number(map, "examiner");
        e.flightSimulator = number(map, "flightSimulator");
        e.dayLandings = whole(map, "dayLandings");
        e.nightLandings = whole(map, "nightLandings");
        e.holdingProcedures = number(map, "holdingProcedures");
        e.groundTime = number(map, "groundTime");
        e.instrumentApproaches = whole(map, "instrumentApproaches");
        e.instructorName = text(map, "instructorName");
        e.instructorCertificate = text(map, "instructorCertificate");
        e.remarks = text(map, "remarks");
        if (e.id.isEmpty()) {
            e.id = Long.toString(System.currentTimeMillis());
        }
        return e;
    }

    private static String num(double v) {
        if (v == Math.rint(v)) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    private static String str(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) {
            return "";
        }
        return c.getString(i);
    }

    private static double dbl(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) {
            return 0;
        }
        return c.getDouble(i);
    }

    private static int integer(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) {
            return 0;
        }
        return c.getInt(i);
    }

    private static String text(Map<String, String> map, String key) {
        String v = map.get(key);
        return v == null ? "" : v;
    }

    private static double number(Map<String, String> map, String key) {
        try {
            return Double.parseDouble(text(map, key).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int whole(Map<String, String> map, String key) {
        try {
            return (int) Double.parseDouble(text(map, key).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
