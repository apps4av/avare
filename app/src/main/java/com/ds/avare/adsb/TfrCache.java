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


import android.content.Context;

import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;
import com.ds.avare.weather.WindsAloft;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * 
 * @author zkhan
 *
 */
public class TfrCache {

    private HashMap<String, AdsbTfr> mTfrs;
    private Preferences mPref;

    public TfrCache(Context ctx) {
        mTfrs = new HashMap<String, AdsbTfr>();
        mPref = new Preferences(ctx);
    }

    public LinkedList<TFRShape> getShapes() {
        LinkedList<TFRShape> ret = new LinkedList<TFRShape>();

        for (AdsbTfr s : mTfrs.values()) {
            if(s.shape != null && s.text != null) {
                // only complete tfr
                ret.add(s.shape);
            }
        }
        return ret;
    }


    /**
     * 
     * @param
     */
    public void putTfr(long time, String id, String shape, String points, String text, String from, String to) {
        if(!mPref.useAdsbWeather() || null == id) {
            return;
        }

        if(!text.contains("NOTAM-TFR")) {
            // ignore notams for now
            return;
        }

        AdsbTfr t = mTfrs.get(id);
        if(null == t) {
            t = new AdsbTfr();
        }
        t.timestamp = System.currentTimeMillis();

        if(text != null && (!text.equals(""))) {

            t.text = text;
            if(t.shape != null) {
                t.shape.updateText(text); //update text as it may arrive after shape is made
            }
        }

        if(from != null && (!from.equals(""))) {
            t.timeFrom = from;
        }

        if(to != null && (!to.equals(""))) {
            t.timeTo = to;
        }


        // Make shapes
        if(shape.equals("polygon") && points != null && (!points.equals(""))) {
            t.points = points;
            // Only draw polygons
            t.shape = new TFRShape(t.text == null ? "" : t.text, new java.util.Date(time));
            String tokens[] = t.points.split("[;]");
            for(int j = 0; j < tokens.length; j++) {
                String point[] = tokens[j].split("[:]");
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

        mTfrs.put(id, t);
    }

    /*
     * ALL ADSB TFRs should be kaput after expiry
     */
    public void sweep() {
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
        public String text;
        public TFRShape shape;
        public String timeFrom;
        public String timeTo;
        public String points;
        public long   timestamp;
    }
}
