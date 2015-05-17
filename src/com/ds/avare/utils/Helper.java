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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import com.ds.avare.R;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.format.Time;
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
	private static Time fetchRawEte(boolean useBearing, double distance, double speed, double bearing, double heading) {
	    double xFactor = 1;
	    if(useBearing) {
            // We can't assume that we are heading DIRECTLY for the destination, so 
            // we need to figure out the multiply factor by taking the COS of the difference
            // between the bearing and the heading.
    		double angDif = angularDifference(heading, bearing);
    		
    		// If the difference is 90 or greater, then ETE means nothing as we are not
    		// closing on the target
    		if(angDif >= 90)
    			return null;
    
    		// Calculate the actual relative speed closing on the target
            xFactor  = Math.cos(angDif * Math.PI / 180);
	    }
	    
	    // Calculate the travel time in seconds
	    double eteTotal = (distance / (speed * xFactor)) * 3600;

	    // Allocate an empty time object
	    Time ete = new Time();

	    // Extract the hours
	    ete.hour  = (int)(eteTotal / 3600);	// take whole int value as the hours
	    eteTotal -= (ete.hour * 3600);		// Remove the hours that we extracted
	    
	    // Convert what's left to fractional minutes
        ete.minute = (int)(eteTotal / 60);	// Get the int value as the minutes now
        eteTotal  -= (ete.minute * 60);		// remove the minutes we just extracted

        // What's left is the remaining seconds 
        ete.second = Math.round((int)eteTotal);	// round as appropriate
        
        // Account for the seconds being 60
        if(ete.second >= 60) { ete.minute++; ete.second -= 60; }
        
        // account for the minutes being 60
        if(ete.minute >= 60) { ete.hour++; ete.minute -= 60; }
        
        // Time object is good to go now
        return ete;
	}

	/***
	 * Fetch the estimate travel time to the indicated target
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
	 * @param bearing - direction to target
	 * @param heading - direction of movement
	 * @return String - "HH:MM" or "MM.SS" time to the target
	 */
    public static String calculateEte(boolean useBearing, double distance, double speed, double bearing, double heading) {

    	// If no speed, then return the empty display value
        if(0 == speed){
            return "--:--";
        }

        // Fetch the eteRaw value
    	Time eteRaw = fetchRawEte(useBearing, distance, speed, bearing, heading);

    	// If an invalid eteRaw, then return the empty display value
        if(null == eteRaw){
            return "--:--";
        }

        // Break the eteRaw out into hours and minutes
        int eteHr  = eteRaw.hour;
        int eteMin = eteRaw.minute;
        int eteSecond = eteRaw.second;
        
        // Hours greater than 99 are not displayable
        if(eteHr > 99) {
            return "XX:XX";
        }

        // If hours is non zero then return HH:MM
        if(eteHr > 0) {
	        // Format the hours and minutes en router
	        String hr = String.format(Locale.getDefault(), "%02d", eteHr);
	        String min = String.format(Locale.getDefault(), "%02d", eteMin);
	
	        // Build the string for return
	        return hr + ":" + min;
        }

        // Hours is zero, so return MM.SS
        String min = String.format(Locale.getDefault(), "%02d", eteMin);
        String sec = String.format(Locale.getDefault(), "%02d", eteSecond);

        // Build the string for return
        return min + "." + sec;
        
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
    public static String calculateEta(boolean useBearing, TimeZone timeZone, double distance, double speed, double bearing, double heading) {

        // If no speed, then return an empty display string
        if(0 == speed ){
            return "--:--";
        }

    	// fetch the raw ETE
        Time eteRaw = fetchRawEte(useBearing, distance, speed, bearing, heading);

        // If the eteRaw is meaningless, then return an empty display string
        if(null == eteRaw){
            return "--:--";
        }

        // Break the hours and minutes out
        int eteHr  = eteRaw.hour;
        int eteMin = eteRaw.minute;

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
        if(orn.equals("Sensor")) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
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
        int offset = mTimeZone.getOffset(System.currentTimeMillis());  
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
 
    /***
     * Downsize the bitmap to at most the indicated ratio of the max specified size
     * @param bm Bitmap to resize
     * @param maxX pixels of max width
     * @param maxY pixels of max height
     * @param maxRatio The max ratio wrt the display size
     * @return the new bitmap, OR input bitmap if no change needed
     */
    public static Bitmap getResizedBitmap(Bitmap bm, int maxX, int maxY, double maxRatio) {

    	// we have an starting bitmap object to work from
    	if(null == bm) {
    		return bm;
    	}
    	
    	// Get current size and h:w ratio
    	int height = bm.getHeight();
    	int width = bm.getWidth();
    	
    	// ensure bitmap size is valid
    	if(0 == height || 0 == width) {
    		return bm;
    	}
    	
    	// What is the height to width ratio - will always be > 0 at this point
    	double ratio = height / width;
    	
    	// Figure out new max size
    	int newHeight = (int) (Math.min(maxX,  maxY) * maxRatio);
    	int newWidth = (int) (newHeight / ratio);
    	
    	// If we don't need to downsize, then return with the original
    	if(newHeight >= height && newWidth >= width) {
    		return bm;
    	}

    	// Calculate the scaling factors in both the x and y direction
    	float scaleWidth = ((float) newWidth) / width;
    	float scaleHeight = ((float) newHeight) / height;
    	 
    	// create a matrix for the manipulation
    	Matrix matrix = new Matrix();
    	 
    	// resize the bit map
    	matrix.postScale(scaleWidth, scaleHeight);
    	 
    	// recreate the new Bitmap, allowing for failure
    	try {
    		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    		return resizedBitmap;
    	} catch (Exception e ) { return bm; }
	}
    
    /**
     * Read a file, used for reading weather file from assets
     * @param fileName
     * @param context
     * @return
     */
    public static String readFromAssetsFile(String fileName, Context context) {
	    StringBuilder returnString = new StringBuilder();
	    InputStream fIn = null;
	    InputStreamReader isr = null;
	    BufferedReader input = null;
	    try {
	        fIn = context.getResources().getAssets()
	                .open(fileName, Context.MODE_WORLD_READABLE);
	        isr = new InputStreamReader(fIn);
	        input = new BufferedReader(isr);
	        String line = "";
	        while ((line = input.readLine()) != null) {
	            returnString.append(line);
	        }
	    }
	    catch (Exception e) {
	    } 
	    finally {
	        try {
	            if (isr != null) {
	                isr.close();
	            }
	            if (fIn != null) {
	                fIn.close();
	            }
	            if (input != null) {
	                input.close();
	            }
	        } 
	        catch (Exception e2) {
	        }
	    }
	    return returnString.toString();
	}

    
    /**
     * Write to file in given folder
     * @param fcontent
     * @return
     */
    public static boolean writeFile(String fcontent, String path){
    	
    	/*
    	 * Write file contents to file path
    	 */
        try {
            File file = new File(path);
            // If file does not exists, then create it
            if (!file.exists()) {
              file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(fcontent);
            bw.close();
            return true;
        } 
        catch (Exception e) {
            return false;
        }
    }    

    /**
     * Avoid ' in JS function calls
     * @param args
     * @return
     */
    public static String formatJsArgs(String args) {
    	if(args == null) {
    		return null;
    	}
    	return args.replace("'", "\\'");
    }
    
    /**
     * Get HMTL file location for webview
     * @param args
     * @return
     */
    public static String getWebViewFile(Context context, String name) {
    	return "file:///android_asset/" + name + context.getString(R.string.lang) + ".html";
    }

}