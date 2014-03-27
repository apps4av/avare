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
import java.text.SimpleDateFormat;
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
import android.util.TypedValue;
import android.view.WindowManager;

/**
 * 
 * @author zkhan
 *
 */
public class Helper {
	
	/***
	 * Fetch the raw estimated time enroute given the input parameters
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
	 * @param bearing - direction to target
	 * @param heading - direction of movement
	 * @return int value of HR * 100 + MIN for the ete, -1 if not applicable
	 */
	private static int fetchRawEte(double distance, double speed, double bearing, double heading) {
        // We can't assume that we are heading DIRECTLY for the destination, so 
        // we need to figure out the multiply factor by taking the COS of the difference
        // between the bearing and the heading.
		double angDif = angularDifference(heading, bearing);
		
		// If the difference is 90 or greater, then ETE means nothing as we are not
		// closing on the target
		if(angDif >= 90)
			return -1;

		// Calculate the actual relative speed closing on the target
        double xFactor  = Math.cos(angDif * Math.PI / 180);
        double eteTotal = distance / (speed * xFactor);

        // Break that down into hours and minutes
        int eteHr = (int)eteTotal;
        int eteMin =  (int)Math.round((eteTotal - (double)eteHr) * 60);

        // account for the minutes being 60
        if(eteMin >= 60) { eteHr++; eteMin -= 60; }
        
        // Return with our estimate
        return eteHr * 100 + eteMin;
	}

	/***
	 * Fetch the estimate travel time to the indicated target
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
	 * @param bearing - direction to target
	 * @param heading - direction of movement
	 * @return String - "HH:MM" time to the target
	 */
    public static String calculateEte(double distance, double speed, double bearing, double heading) {

    	// Fetch the eteRaw value
    	int eteRaw = fetchRawEte(distance, speed, bearing, heading);

    	// If no speed or an invalid eteRaw, then return the empty display value
        if(0 == speed || eteRaw == -1){
            return "--:--";
        }

        // Break the eteRaw out into hours and minutes
        int eteHr  = eteRaw / 100;
        int eteMin = eteRaw %100;
        
        // Hours greater than 99 are not displayable
        if(eteHr > 99) {
            return "XX:XX";
        }
        
        // Format the hours and minutes en router
        String hr = String.format(Locale.getDefault(), "%02d", eteHr);
        String min = String.format(Locale.getDefault(), "%02d", eteMin);

        // BUit the string for return
        return hr + ":" + min;
    }

    /***
	 * Fetch the estimate current time of arrival at the destination
	 * @param timeZone - The timezone at the destination
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
	 * @param bearing - direction to target
	 * @param heading - direction of movement
	 * @return String - "HH:MM" current time at the target
     */
    public static String calculateEta(TimeZone timeZone, double distance, double speed, double bearing, double heading) {

    	// fetch the raw ETE
        int eteRaw = fetchRawEte(distance, speed, bearing, heading);

        // If no speed, or the eteRaw is meaningless, then return an empty display string
        if(0 == speed || eteRaw == -1){
            return "--:--";
        }

        // Break the hours and minutes out
        int eteHr  = eteRaw / 100;
        int eteMin = eteRaw %100;

        // Hours greater than 99 are not displayable
        if(eteHr > 99) {
            return "XX:XX";
        }

        // Get the current local time hours and minutes
        int etaHr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int etaMin = Calendar.getInstance().get(Calendar.MINUTE);

        // Add in our ETE to the current time, accounting for rollovers
        etaMin += eteMin;	// Add the estimated minutes enroute to "now"
        if(etaMin > 59) { etaMin -= 60; etaHr++; }	// account for minute rollover
        etaHr += eteHr;	// Now add the hours enroute
        while(etaHr > 23) { etaHr -= 24; }	// account for midnight rollover

        // Format the hours and minutes
        String strHr = String.format(Locale.getDefault(), "%02d", etaHr);
        String strMn = String.format(Locale.getDefault(), "%02d", etaMin);

        // Build string of return
        return strHr + ":" + strMn;
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
        return(String.format(Locale.getDefault(), "%d", (int)altitude));
    }

    /**
     * See the explanation in the function setThreshold. 
     * @param altitude in FL for printing
     * @return
     */
    public static String calculateAGLFromThreshold(float threshold, float elevation) {
        boolean valid = (elevation < 0) ? false : true;
        double altitude = (threshold) * Preferences.heightConversion * 50.0;
        altitude -= elevation * Preferences.heightConversion;
        if(altitude < 0) {
            altitude = 0;
        }
        if(valid) {
            return(String.format(Locale.getDefault(), "%d", (int)altitude));
        }
        return("");
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
     * Finds elevation from the above elevation pixel formula from a pixel
     * @param px
     * @return
     */
    public static double findElevationFromPixel(int px) {
        /*
         * Average the RGB value. The elevation chart is already in gray scale
         */
        return (((double)(px & 0x000000FF) + ((px & 0x0000FF00) >> 8) + ((px & 0x00FF0000) >> 16)) * (25.0 - 1.0) / 3.0);
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
    
    public static String makeLine2(double distance, String unit, String genDirection, double heading, double variation) {
        return String.format(Locale.getDefault(), "%3d", (Math.round(distance))) + 
        			unit + " " + genDirection + " BRG " + 
        			Helper.correctConvertHeading(Math.round(getMagneticHeading(heading, variation))) + '\u00B0';
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

/***
 * Center the input string into a new string that is of the indicated size
 * @param input string to center
 * @param size length of the output string
 * @return
 */
    public static String centerString(String input, int size) {
    	if (input.length() > size) {	// if input is already bigger than output
    		return input;				// just return
    	}

    	// Build an empty string of the desired size
    	char[] spaces = new char[size + 1];
    	for(int idx = 0; idx < spaces.length - 1; idx++) {
    		spaces[idx] = ' ';
    	}
    	String strEmpty = new String(spaces);

    	// Calculate how much pre and post padding to use
    	int diff = size - input.length();
    	int trailing = diff / 2;
    	int leading = trailing + diff % 2;

    	// return with the new string properly centered
    	return strEmpty.substring(0,  leading) + input + strEmpty.substring(0,  trailing);
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

        String orn = pref.getOrientation();
        if(orn.equals("Portrait")) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        else if(orn.equals("Reverse Portrait")) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        }
        if(orn.equals("Landscape")) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else if(orn.equals("Reverse Landscape")) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
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
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteDir(c);
                }
            }
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
        
        if(null == date) {
            return true;
        }
        
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        GregorianCalendar expires = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

        if(date.contains("_")) {
            int year;
            int month;
            int day;
            int hour;
            int min;
            /*
             * TFR date
             */
            String dates[] = date.split("_");
            if(dates.length < 4) {
                return true;            
            }

            month = Integer.parseInt(dates[0]) - 1;
            day = Integer.parseInt(dates[1]);
            year = Integer.parseInt(dates[2]);

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
            if(now.after(expires)) {
                return true;
            }
            
            return false;
        }

        /*
         * Parse the normal charts date designation
         * like 1400
         */
        int cycle = 1400;
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        epoch.set(2013, 11, 12, 9, 0, 0);

        while(epoch.before(now)) {
            epoch.add(Calendar.DAY_OF_MONTH, 28);
            cycle++;
        }
        epoch.add(Calendar.DAY_OF_MONTH, -28);
        cycle--;
        try {
            if(cycle > Integer.parseInt(date)) {
                return true;
            }
        }
        catch (Exception e) {
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
    
            int row = (Integer.parseInt(tokens[7]) + rowm);
            int col = (Integer.parseInt(tokens[8]) + colm);
            int lenr = tokens[7].length();
            int lenc = tokens[8].length();
            
            String rformatted = String.format("%0" + lenr + "d", row);
            String cformatted = String.format("%0" + lenc + "d", col);
            String pre = tokens[0] + "/" + tokens[1] + "/" + tokens[2] + "/" + tokens[3] + "/" + tokens[4] + "/" + row + "/";
            String post = tokens[6] + "_" + rformatted + "_" + cformatted + "." + tokens[9];
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
                        shape.makePolygon();
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
                            Double.parseDouble(tokens[id]), false);
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
    
    
    /**
     *  Converts 1 dip (device independent pixel) into its equivalent physical pixels
     */
    public static float getDpiToPix(Context ctx) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, 
                ctx.getResources().getDisplayMetrics());
    }

    /** Calculate the absolute angular difference between the two headings
     * 
     * @param hdg angle 1 in degrees (typically the heading)
     * @param brg angle 2 in degrees (typically the bearing)
     * @return difference between hdg and brg in degrees
     */
    public static double angularDifference(double hdg, double brg) {
    	double absDiff = Math.abs(hdg - brg);
    	if(absDiff > 180) {
    		return 360 - absDiff;
    	}
    	return absDiff;
    }

    /***
     * Is the brgTrue to the left of the brgCourse line (extended).
     * @param brgTrue true bearing to destination from current location
     * @param brgCourse bearing to dest on COURSE line
     * @return true if it is LEFT, false if RIGHT
     */
    public static boolean leftOfCourseLine(double brgTrue, double brgCourse) {
    	if(brgCourse <= 180) {
    		if(brgTrue >= brgCourse && brgTrue <= brgCourse + 180)
    			return true;
    		return false;
    	}

    	// brgCourse will be > 180 at this point
    	if(brgTrue > brgCourse || brgTrue < brgCourse - 180)
    		return true;
    	return false;
    }
    
    /**
     * 
     */
    public static String millisToGMT(long millis) {
        SimpleDateFormat df = new SimpleDateFormat("MM_dd_yyyy_hh_mm", Locale.getDefault());
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(millis) + "_UTC";
    }
    
    /**
     * 
     * @return
     */
    public static long getMillisGMT() {
        Calendar calendar = new GregorianCalendar();  
        TimeZone mTimeZone = calendar.getTimeZone();  
        int offset = mTimeZone.getRawOffset();  
        return System.currentTimeMillis() - offset;
    }

    /**
     * Take the speed returned from gpsParams.getSpeed() which has been converted to 
     * a value to be displayed and change it to knots.
     * Sometimes we just want knots.
     * @return
     */
    public static double getSpeedInKnots(double displayedSpeed) {
        return displayedSpeed * Preferences.MS_TO_KT / Preferences.speedConversion; // m/s to knots
    }
}