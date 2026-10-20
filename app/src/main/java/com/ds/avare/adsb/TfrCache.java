/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adsb;


import android.graphics.Color;

import com.ds.avare.StorageService;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

/**
 * 
 * @author zkhan
 *
 */
public class TfrCache {

    private HashMap<String, AdsbTfr> mTfrs;
    private Preferences mPref;

    public TfrCache() {
        mTfrs = new HashMap<String, AdsbTfr>();
        mPref = StorageService.getInstance().getPreferences();
    }

    public synchronized LinkedList<TFRShape> getShapes() {
        LinkedList<TFRShape> ret = new LinkedList<TFRShape>();
        long now = System.currentTimeMillis();

        for (AdsbTfr s : mTfrs.values()) {
            if(s.shape == null) {
                continue;
            }
            // Hide a TFR once its own end time has passed. Reception age is separate.
            if(s.expiresMs > 0 && now > s.expiresMs) {
                continue;
            }
            s.shape.setColor(now < s.effectiveMs ? TFRShape.COLOR_FUTURE : Color.RED);
            ret.add(s.shape);
        }
        return ret;
    }


    /**
     * Store an ADS-B NOTAM graphic. The outline is drawn, and a long press inside it
     * returns this text from the same TFR section as a downloaded TFR.
     */
    public synchronized void putTfr(long time, String id, String shape, String points, String text, String from, String to, String altBottom, String altTop) {
        if(!mPref.useAdsbWeather() || null == id) {
            return;
        }
        if(text == null) {
            text = "";
        }
        if(shape == null) {
            shape = "";
        }

        boolean outline = isOutline(shape) && points != null && points.length() > 0;
        if(!outline && text.length() == 0) {
            return;
        }

        AdsbTfr t = mTfrs.get(id);
        if(null == t) {
            t = new AdsbTfr();
        }
        t.timestamp = System.currentTimeMillis();

        if(text.length() > 0) {
            t.notam = text;
        }

        if(from != null && from.length() > 0) {
            t.timeFrom = from;
        }

        if(to != null && to.length() > 0) {
            t.timeTo = to;
        }

        // Text-only records report 0 ft. Keep altitudes from the graphic.
        if(outline) {
            if(altBottom != null && altBottom.length() > 0) {
                t.altBottom = altBottom;
            }
            if(altTop != null && altTop.length() > 0) {
                t.altTop = altTop;
            }
        }

        t.effectiveMs = parseFisTime(t.timeFrom, yearUtc(2000));
        t.expiresMs = parseFisTime(t.timeTo, yearUtc(2099));

        if(outline) {
            t.points = points;
            t.shape = new TFRShape(textFor(t, id), new java.util.Date(time));
            String tokens[] = t.points.split("[;]");
            for(int j = 0; j < tokens.length; j++) {
                String point[] = tokens[j].split("[:]");
                if(point.length < 2) {
                    continue;
                }
                try {
                    double lon = Double.parseDouble(point[0]);
                    double lat = Double.parseDouble(point[1]);
                    if(0 == lat || 0 == lon) {
                        continue;
                    }
                    t.shape.add(lon, lat, false);
                }
                catch (Exception e) {
                }
            }
            t.shape.makePolygon();
        }
        else if(t.shape != null) {
            t.shape.updateText(textFor(t, id));
        }

        mTfrs.put(id, t);
    }

    /**
     * Same fields the downloaded TFR list puts in the long-press TFR section.
     */
    private static String textFor(AdsbTfr t, String id) {
        StringBuilder b = new StringBuilder();
        b.append("ADS-B TFR #").append(id);
        b.append("\nTop      ").append(t.altTop == null ? "0" : t.altTop).append(" ft");
        b.append("\nBottom   ").append(t.altBottom == null ? "0" : t.altBottom).append(" ft");
        if(t.timeFrom != null && t.timeFrom.length() > 0) {
            b.append("\nEffective ").append(t.timeFrom);
        }
        if(t.timeTo != null && t.timeTo.length() > 0) {
            b.append("\nExpires  ").append(t.timeTo);
        }
        if(t.notam != null && t.notam.length() > 0) {
            b.append("\n").append(t.notam);
        }
        return b.toString();
    }

    private static boolean isOutline(String shape) {
        return "polygon".equals(shape) || "prism".equals(shape)
                || "prism.msl".equals(shape) || "point".equals(shape);
    }

    private static long yearUtc(int year) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /**
     * FIS-B times are month-day, day, or hours, filled from the current UTC date.
     * Empty start means already in effect. Empty end means no scheduled end.
     */
    private static long parseFisTime(String raw, long fallback) {
        if(raw == null || raw.length() == 0) {
            return fallback;
        }
        try {
            String s = raw.trim();
            if(s.endsWith("Z")) {
                s = s.substring(0, s.length() - 1);
            }
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if(s.length() >= 16 && s.charAt(4) == '-') {
                int year = Integer.parseInt(s.substring(0, 4));
                int month = Integer.parseInt(s.substring(5, 7));
                int day = Integer.parseInt(s.substring(8, 10));
                int hour = Integer.parseInt(s.substring(11, 13));
                int minute = Integer.parseInt(s.substring(14, 16));
                cal.set(year, month - 1, day, hour, minute, 0);
                return cal.getTimeInMillis();
            }
            if(s.length() >= 14 && s.charAt(2) == '-') {
                int month = Integer.parseInt(s.substring(0, 2));
                int day = Integer.parseInt(s.substring(3, 5));
                int hour = Integer.parseInt(s.substring(6, 8));
                int minute = Integer.parseInt(s.substring(9, 11));
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                return cal.getTimeInMillis();
            }
            int t = s.indexOf('T');
            if(t > 0) {
                int day = Integer.parseInt(s.substring(0, t));
                String[] hm = s.substring(t + 1).split(":");
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
                cal.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
                return cal.getTimeInMillis();
            }
            String[] hm = s.split(":");
            if(hm.length >= 2) {
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
                cal.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
                if(hm.length >= 3) {
                    cal.set(Calendar.SECOND, Integer.parseInt(hm[2]));
                }
                return cal.getTimeInMillis();
            }
        }
        catch (Exception ignored) {
        }
        return fallback;
    }

    /*
     * ALL ADSB TFRs should be kaput after expiry
     */
    public synchronized void sweep() {
        long now = System.currentTimeMillis();
        int expiry = mPref.getExpiryTime() * 60 * 1000;

        LinkedList<String> keys;

        /*
         * Winds
         */
        keys = new LinkedList<String>();
        for (String key : mTfrs.keySet()) {
            AdsbTfr t = mTfrs.get(key);
            long diff = (now - t.timestamp) - expiry;
            if(diff > 0) {
                keys.add(key);
            }
        }
        for(String key : keys) {
            mTfrs.remove(key);
        }
    }


    private class AdsbTfr {
        public TFRShape shape;
        public String notam;
        public String timeFrom;
        public String timeTo;
        public String points;
        public String altBottom;
        public String altTop;
        public long   timestamp;
        public long   effectiveMs;
        public long   expiresMs;
    }
}
