/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import com.ds.avare.shapes.ChartShape;
import com.ds.avare.shapes.Shape;
import com.ds.avare.utils.BitmapHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by zkhan on 8/18/15.
 * Chart boundaries class. Need to be created once like a database.
 */
public class Boundaries {

    private HashMap<String, Shape> mPolygons;
    private static Boundaries mInstance = null;

    private Boundaries() {
        makePolygons();
    }


    /**
     * Loop through to get name of chart we are on.
     *
     * @param lon
     * @param lat
     * @return
     */
    public String findChartOn(String chartIndex, double lon, double lat) {
        Iterator it = mPolygons.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ChartShape> pair = (HashMap.Entry<String, ChartShape>)it.next();
            ChartShape s = pair.getValue();
            String name = pair.getKey();
            String type = s.getName();
            // Search only specific type
            if(type.equals(chartIndex)) {
                // find if this point is in this chart
                type = s.getTextIfTouched(lon, lat);
                if(type != null) {
                    return name;
                }
            }
        }
        return "";
    }

    /*
     * Create once
     */
    public static Boundaries getInstance() {
        if(mInstance == null) {
            mInstance = new Boundaries();
        }
        return mInstance;
    }


    // Make chart boundary shapes
    private void makePolygons() {
        mPolygons = new HashMap<String, Shape>();

        // loop and add shapes
        for (int i = 0; i < mData.length; i += 4) {
            String type = mData[i + 0];
            String name = mData[i + 1];
            double lon = Double.parseDouble(mData[i + 2]);
            double lat = Double.parseDouble(mData[i + 3]);

            // add to hash, but check if it exists first
            Shape s = mPolygons.get(name);
            if (s == null) {
                // hashmap will save name, shape will save type
                s = new ChartShape(type);
                mPolygons.put(name, s);
            }
            // add point to shape
            s.add(lon, lat, false);
        }

        // Make all shapes
        Iterator it = mPolygons.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ChartShape> pair = (HashMap.Entry<String, ChartShape>)it.next();
            ChartShape s = pair.getValue();
            s.makePolygon();
        }
    }


    public static int getZoom(int index) {
        switch (BitmapHolder.WIDTH) {
            case 256:
                return mZooms[index] + 1;
            case 512:
                return mZooms[index];

        }
        return 0;
    }


    public static String getChartExtension(int index) {
        return mExtension[index];
    }

    public static String getChartType(int index) {
        return mChartType[index];
    }

    public static ArrayList<String> getChartTypes() {
        return new ArrayList<String>(Arrays.asList(mChartType));
    }

    private static String[] mChartType = {
            "VFR",
    };


    // Zooms are for 512x512 tiles on charts
    private static int[] mZooms = {
            11,
    };

    private static String[] mExtension = {
            ".png",
    };


    // name,lon,lat
    private static String[] mData = {
        "0","EU VFR","-40.000","70.000",
        "0","EU VFR","30.000","70.000",
        "0","EU VFR","30.000","30.000",
        "0","EU VFR","-40.000","30.000",
    };

}
