/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.WindowManager;

/**
 * 
 * @author zkhan
 *
 */
public class Helper {

    public static String calculateEta(double distance, double speed) {
        String eta = "--:--"; 
        if(0 == speed) {
            return eta;
        }
        int etahr = (int)(distance / speed);
        int etamin =  (int)Math.round((distance / speed - (double)etahr) * 60);
        if(etahr > 99) {
            return "XX:XX";
        }
        else {
            String hr = String.format(Locale.getDefault(), "%02d", etahr);
            String min = String.format(Locale.getDefault(), "%02d", etamin);
            eta = new String(hr + ":" + min);
        }
        return eta;
    }
    
    /**
     * 
     * @param lonlat
     */
    public static double truncGeo(double lonlat) {
        lonlat *= 10000;
        lonlat = Math.round(lonlat);
        lonlat /= 10000;
        return lonlat;
    }
    
    /**
     * Same as query for a location in DatabaseHelper.findClosestAirportID() 
     * @param lon
     * @param lat
     * @param lon1
     * @param lat1
     * @return
     */
    public static boolean isSameGPSLocation(double lon, double lat, double lon1, double lat1) {
        if(((lon - lon1) * (lon - lon1) + (lat - lat1) * (lat - lat1)) < 0.001) {
            return true;
        }
        return false;
    }
    

    /**
     * 
     * @param paint
     */
    public static void invertCanvasColors(Paint paint) {
       float mx [] = {
                -1.0f,  0.0f,  0.0f,  1.0f,  0.0f,
                0.0f,  -1.0f,  0.0f,  1.0f,  0.0f,
                0.0f,  0.0f,  -1.0f,  1.0f,  0.0f,
                1.0f,  1.0f,  1.0f,  1.0f,  0.0f 
       };
       ColorMatrix cm = new ColorMatrix(mx);
       paint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    /**
     * See the explanation in the function setThreshold. 
     * @param altitude in FL for printing
     * @return
     */
    public static String calculateAltitudeFromThreshold(float threshold) {
        double altitude = (threshold) * Preferences.heightConversion * 50.0;
        return(String.format(Locale.getDefault(), "%04dft", (int)altitude));
    }

    /**
     * See the explanation in the function setThreshold. 
     * @param altitude
     * @return
     */
    public static float calculateThreshold(double altitude) {
        float threshold = (float)(altitude / Preferences.heightConversion / 50.0); 
        return(threshold);
    }

    /**
     * 
     * @param paint
     */
    public static void setThreshold(Paint paint, float threshold) {
        /*
         * Elevation matrix. This will threshold the elevation with GPS altitude.
         * The factor is used to increase the brightness for a given elevation map.
         * Elevation map is prepared so that altitudes from 0-5000 meter are encoded with 0-200 pixel values.
         * Each pixel level is 25 meter. 
         * 
         * Negative sign for black threshold instead of white.
         * Threshold of to 0 to 100 translated to 0 - 200 for all pixels thresholded at 5000 meters.
         * 
         * Calibrated (visually) at 
         * KRNO - 1346 meters, threshold = 28
         * KLXV - 3027 meters, threshold = 61
         * L70  - 811  meters, threshold = 16
         * KHIE - 326  meters, threshold = 7
         *--------------------------------------------
         *       5510                    = 112  ~ 50 meters per px
         * Formula to calculate threshold is:
         * threshold = altitude / 3 (meters per foot) / 50
         * Give 2 levels margin of safety
         */
        float factor = 4.f;
        float mx [] = {
                factor, 0,             0,             0,  -(factor) * (threshold - 5) * 2.0f,
                0,      factor / 1.5f, 0,             0,  -(factor) * (threshold - 5) * 2.0f,
                0,      0,             factor / 2.0f, 0,  -(factor) * (threshold - 5) * 2.0f,
                0     , 0,             0,             1,  0
       };
       ColorMatrix cm = new ColorMatrix(mx);
       paint.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    /**
     * 
     * @param paint
     */
    public static void restoreCanvasColors(Paint paint) {
       paint.setColorFilter(null);
    }
    
    /**
     * 
     * @param lon
     * @return
     */
    public static boolean isLongitudeSane(double lon) {
        return (lon < 180) && (lon > -180);
    }
    
    /**
     * 
     * @param lat
     * @return
     */
    public static boolean isLatitudeSane(double lat) {
        return (lat > -90) && (lat < 90); 
    }
    
    
    /**
     * 
     * @param distance
     * @param eta
     * @param heading
     * @return
     */
    public static String makeLine(double value, String unit, String eta, double heading, double variation) {
        String valTrunc = String.format(Locale.getDefault(), "%3d", (Math.round(value)));
        if(eta == null) {
            eta = "     ";
        }
        String ret = 
                valTrunc + unit + " " +  eta + " " +
                        Helper.correctConvertHeading(Math.round(getMagneticHeading(heading, variation))) + '\u00B0';
        return ret; 
    }
    
    /**
     * 
     * @param heading
     * @return
     */
    public static String correctConvertHeading(long heading) {
        String ret = String.format(Locale.getDefault(), "%03d", heading);
        if(ret.equals("000")) {
            ret = "360";
        }
        return ret;
    }
    
    /**
     * 
     * @param val
     * @return
     */
    public static String removeLeadingZeros(String val) {
        return val.replaceFirst("^0+(?!$)", ""); 
    }
    
    /**
     * 
     * @param variation
     * @return
     */
    public static double parseVariation(String variation) {
        double var = 0;
        if((null == variation) || (variation.length() < 3)) {
            return 0;
        }
        else {
            var = Double.parseDouble(variation.substring(0, 2));            
            if(variation.contains("E")) {
                var = -var;                 
            }
        }
        return var;
    }

    /**
     * 
     * @param variation
     * @return
     */
    public static String makeVariation(double variation) {
        int var = (int)Math.round(variation);
        String ret = String.format(Locale.getDefault(), "%02d", var);
        if(var < 0) {
            ret = "E" + var + "\u00B0 ";
        }
        else {
            ret = "W" + var + "\u00B0 ";
        }
        return ret;
    }

    /**
     * Set theme
     * @param act
     */
    public static void setTheme(Activity act) {
        Preferences p = new Preferences(act.getApplicationContext()); 
        if(p.isNightMode()) {
            act.setTheme(android.R.style.Theme_Black);
        }
        else {
            act.setTheme(android.R.style.Theme_Light);            
        }
    }
    
    /**
     * Set common features of all activities in the framework
     * @param act
     */
    public static void setOrientationAndOn(Activity act) {
        
        Preferences pref = new Preferences(act.getApplicationContext());
        if(pref.shouldScreenStayOn()) {
            act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);            
        }

        if(pref.isPortrait()) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);            
        }
        else {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }


        /*
         * Do not open keyboard automatically.
         */
        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }    
    
    /**
     * 
     * @param f
     */
    public static void deleteDir(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteDir(c);
            }
        }
        try {
            f.delete();
        }
        catch (Exception e) {
            
        }
    }
    
    /**
     * 
     * @param date
     * @return
     */
    public static boolean isExpired(String date) {
        
        int year;
        int month;
        int day;
        int hour;
        int min;
        
        if(null == date) {
            return true;
        }
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        GregorianCalendar expires = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        /*
         * Parse the normal charts date designation
         * like 08_22_2013
         */
        String dates[] = date.split("_");
        if(dates.length < 3) {
            return true;            
        }
        try {
            month = Integer.parseInt(dates[0]) - 1;
            day = Integer.parseInt(dates[1]);
            year = Integer.parseInt(dates[2]);
            if(dates.length > 3) {
                /*
                 * TFR date
                 */
                String time[] = dates[3].split(":");
                hour = Integer.parseInt(time[0]);
                min = Integer.parseInt(time[1]);
                if(year < 1 || month < 0 || day < 1 || hour < 0 || min < 0) {
                    return true;
                }
                /*
                 * so many min expiry
                 */
                expires.set(year, month, day, hour, min);
                expires.add(Calendar.MINUTE, NetworkHelper.EXPIRES);
            }
            else {
                /*
                 * Chart date
                 */
                hour = 9;
                min = 0;
                if(year < 1 || month < 0 || day < 1 || hour < 0 || min < 0) {
                    return true;
                }
                expires.set(year, month, day, hour, min);
                expires.add(Calendar.DAY_OF_MONTH, 28);
            }
        }
        catch (Exception e) {
            return true;
        }

        /*
         * expired?
         */
        if(now.after(expires)) {
            return true;
        }

        return false;
    }
    
    /**
     * 
     * @param heading
     * @param variation
     * @return
     */
    public static double getMagneticHeading(double heading, double variation) {
        return (heading + variation + 360) % 360;
    }
    
    /**
     * 
     * @param heading
     * @param variation
     * @return
     */
    public static String incTileName(String name, int rowm, int colm) {
        
        /*
         * This is all magic. Check database specification.
         * Tiles are stored row/col as:
         * 0/row/master_row_col where row, col have leading zeros
         */

        try {
            /*
             * This is all magic. Check database specification.
             * Tiles are stored row/col as:
             * 0/row/master_row_col where row, col have leading zeros
             */
            String [] tokens = name.split("[/_.]");
    
            int row = (Integer.parseInt(tokens[6]) + rowm);
            int col = (Integer.parseInt(tokens[7]) + colm);
            int lenr = tokens[6].length();
            int lenc = tokens[7].length();
            
            String rformatted = String.format("%0" + lenr + "d", row);
            String cformatted = String.format("%0" + lenc + "d", col);
            String pre = tokens[0] + "/" + tokens[1] + "/" + tokens[2] + "/" + tokens[3] + "/" + row + "/";
            String post = tokens[5] + "_" + rformatted + "_" + cformatted + "." + tokens[8];
            return(pre + post);
        }
        catch(Exception e) {
        }
        return null;
    }
    
    /**
     * 
     * @param data
     */
    private static String readFromFile(String filename) {
        File file = new File(filename);
        byte b[] = null;
        try {
            if(file.exists()) {
                b = new byte[(int)file.length()];
                InputStream fi = new FileInputStream(file);              
                fi.read(b);
                fi.close();
            }
        }
        catch (Exception e) {
            return null;
        }
        
        if(null != b) {
            return new String(b);
        }
        return null;
    }
    

    /**
     * 
     * @param airport
     * @return
     */
    public static LinkedList<TFRShape> getShapesInTFR(Context ctx) {
        
        /*
         * Create a shapes list
         */
        LinkedList<TFRShape> shapeList = new LinkedList<TFRShape>();

        String filename = new Preferences(ctx).mapsFolder() + "/tfr.txt";
        String data = readFromFile(filename);
        if(null != data) {
            /*
             * Find date of last file download
             */
            File file = new File(filename);
            Date time = new Date(file.lastModified());
   
            /*
             * Now read from file
             */
            String tokens[] = data.split(",");
            TFRShape shape = null;
            /*
             * Add shapes from latitude, longitude
             */
            for(int id = 0; id < tokens.length; id++) {
                if(tokens[id].contains("TFR:: ")) {
                    if(null != shape) {
                        shapeList.add(shape);
                    }                                 
                    shape = new TFRShape(tokens[id].replace(
                            "TFR:: ", "@ " + time.toString()).
                            replace("Top", "\n" + "Top      ").
                            replace("Low", "\n" + "Bottom   ").
                            replace("Eff", "\n" + "Effective").
                            replace("Exp", "\n" + "Expires  "));
                    continue;
                }
                try {
                    /*
                     * If we get bad input from Govt. site. 
                     */
                    shape.add(Double.parseDouble(tokens[id + 1]),
                            Double.parseDouble(tokens[id]));
                }
                catch (Exception e) {
                    
                }
                id++;
            }
            if(null != shape) {
                shape.makePolygon();
                shapeList.add(shape);
            }
        }
        
        return shapeList;
    }  

    /**
     * 
     * @param freq
     * @return
     */
    public static boolean isFrequencyUHF(double freq) {
        return freq > 136;
    }

}