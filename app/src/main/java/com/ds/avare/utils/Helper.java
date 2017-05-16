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

import com.ds.avare.R;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author zkhan
 *
 */
public class Helper {

    // All elevation is calculated in feet
    // ranges -364 to 20150 feet (hence 20150 in 3D is +z)
    public static final double ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE     = 24.5276170372963 * Preferences.heightConversion;
    public static final double ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT = -364.431597044586;
    public static final double ALTITUDE_FT_ELEVATION_PLUSZ               = ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE * 255.0 + ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT;

    /**
     * Finds elevation from the above elevation pixel formula from a pixel in meters
     * @param px
     * @return
     */
    public static double findElevationFromPixel(int px) {
        /*
         * No need to average. Gray scale has all three colors at the same value
         */
        return (((double)(px & 0x000000FF)) *
                ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE + ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT);
    }

    /**
     * Finds elevation from the above elevation pixel formula from a pixel in meters
     * @param px
     * @return
     */
    public static double findElevationFromPixelNormalized(int px) {
        /*
         * No need to average. Gray scale has all three colors at the same value
         */
        return findElevationFromPixel(px) / ALTITUDE_FT_ELEVATION_PLUSZ;
    }

    /**
     * Find pixel value from elevation
     * @param elev
     * @return
     */
    public static double findPixelFromElevation(double elev) {
        return (elev - ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT) / ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE;
    }

    /**
     * Find pixel value from elevation
     * @param elev
     * @return
     */
    public static double findPixelFromElevationNormalized(double elev) {
        return findPixelFromElevation(elev) / 255.0;
    }

    public static double findElevationFromNormalizedElevation(double elev) {
        return (elev * ALTITUDE_FT_ELEVATION_PLUSZ);
    }

    /***
	 * Fetch the raw estimated time enroute given the input parameters
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
     * @param calculated - already calculated value
     * @param calc - or should I calculate?
	 * @return int value of HR * 100 + MIN for the ete, -1 if not applicable
	 */
	private static Time fetchRawEte(double distance, double speed, long calculated, boolean calc) {

        double eteTotal = calculated;
        if(calc) {
            // Calculate the travel time in seconds
            eteTotal = (distance / speed) * 3600;
        }

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
     * @param calculated - already calculated value
     * @param calc - or should I calculate?
	 * @return String - "HH:MM" or "MM.SS" time to the target
	 */
    public static String calculateEte(double distance, double speed, long calculated, boolean calc) {

    	// If no speed, then return the empty display value
        if(0 == speed && calc){
            return "--:--";
        }

        // Fetch the eteRaw value
    	Time eteRaw = fetchRawEte(distance, speed, calculated, calc);

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
            return String.format(Locale.getDefault(), "%02d:%02d", eteHr, eteMin);
        }

        // Hours is zero, so return MM.SS
        return String.format(Locale.getDefault(), "%02d.%02d", eteMin, eteSecond);

    }

    /***
	 * Fetch the estimate current time of arrival at the destination
	 * @param calendar - The calendar at the destination
	 * @param distance - how far to the target
	 * @param speed - how fast we are moving
	 * @return String - "HH:MM" current time at the target
     */
    public static String calculateEta(CalendarHelper calendar, double distance, double speed) {

        // If no speed, then return an empty display string
        if(0 == speed ){
            return "--:--";
        }

    	// fetch the raw ETE
        Time eteRaw = fetchRawEte(distance, speed, 0, true);

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
        int etaHr = calendar.getHour();
        int etaMin = calendar.getMinute();

        // Add in our ETE to the current time, accounting for rollovers
        etaMin += eteMin;	// Add the estimated minutes enroute to "now"
        if(etaMin > 59) { etaMin -= 60; etaHr++; }	// account for minute rollover
        etaHr += eteHr;	// Now add the hours enroute
        while(etaHr > 23) { etaHr -= 24; }	// account for midnight rollover

        // Format the hours and minutes
        return String.format(Locale.getDefault(), "%02d:%02d", etaHr, etaMin);
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
     * @return
     */
    public static String calculateAltitudeFromMSL(float msl) {
        double altitude = msl;
        return(String.format(Locale.getDefault(), "%d", (int)altitude));
    }

    /**
     * @return
     */
    public static String calculateAGLFromMSL(float msl, float elevation) {
        boolean valid = (elevation >= (Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT - 0.5)) ? true : false; // 0.5 for rounding error
        double altitude = msl;
        altitude -= elevation;
        if(altitude < 0) {
            altitude = 0;
        }
        if(valid) {
            return(String.format(Locale.getDefault(), "%d", (int)altitude));
        }
        return("");
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
        if(pref.isKeepScreenOn()) {
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
     * @param filename
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
     * @param filename
     */
    public static String readTimestampFromFile(String filename) {
        File file = new File(filename);
        try {
            if(file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                return br.readLine();
            }
        }
        catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * 
     * @param ctx
     * @return
     */
    public static LinkedList<TFRShape> getShapesInTFR(Context ctx) {
        
        /*
         * Create a shapes list
         */
        LinkedList<TFRShape> shapeList = new LinkedList<TFRShape>();

        String filename = new Preferences(ctx).mapsFolder() + "/tfr.txt";
        String filenameManifest = new Preferences(ctx).mapsFolder() + "/TFRs";
        String data = readFromFile(filename);
        String dataManifest = readTimestampFromFile(filenameManifest);
        if(null != data && null != dataManifest) {
            /*
             * Find date of last file download
             */
            File file = new File(filename);

            // Find date of TFRs of format 09_03_2015_15:30_UTC, first line in manifest
            SimpleDateFormat format = new SimpleDateFormat("MM_dd_yyyy_HH:mm", Locale.getDefault());

            Date time;
            try {
                time = format.parse(dataManifest.replace("_UTC", "")); // internal times of products in UTC
            }
            catch (Exception e) {
                // nothing to return
                return shapeList;
            }

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
                            replace("Exp", "\n" + "Expires  "), time);
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
     * @param context
     * @param name
     * @return
     */
    public static String getWebViewFile(Context context, String name) {
    	return "file:///android_asset/" + name + context.getString(R.string.lang) + ".html";
    }

    /**
     * Rotate a set of coordinates by a given angle
     * @param c_x
     * @param c_y
     * @param thetab
     * @param x
     * @param y
     * @return
     */
    public static double[] rotateCoord(double c_x,double c_y,double thetab,double x,double y){
        double prc_x = x - c_x;
        double prc_y = y - c_y;
        double r = Math.sqrt(prc_x * prc_x + prc_y * prc_y);
        double theta = Math.atan2(prc_y, prc_x) ;
        theta = theta + thetab* Math.PI / 180.0;
        double pc_x = r * Math.cos(theta );
        double pc_y = r * Math.sin(theta);
        double p[]=new double[2];
        p[0] = pc_x + c_x;
        p[1] = pc_y + c_y;
        return p;
    }

    public static String getGpsAddress(double lon, double lat) {
        return Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
    }


    private static final Pattern ICAO_GPS_PATTERN = Pattern.compile(
            "(([^@]*)@)?" +
                    "([0-8][0-9])([0-5][0-9])([0-5][0-9])([NSns])"+
                    "([01][0-9][0-9])([0-5][0-9])([0-5][0-9])([EWew])");

    public static boolean isGPSCoordinate(String coords) {
        return coords.contains("&") || ICAO_GPS_PATTERN.matcher(coords).matches();
    }

    public static String decodeGpsAddress(String name, double coords[]) {
        /*
         * Match predictable GPS pattern of DDMMSS[N|S]DDDMMSS[E|W]
         */
        Matcher m = ICAO_GPS_PATTERN.matcher(name);
        if(m.matches()) {
            String label;
            try {
                label = m.group(1) == null ? "" : m.group(1);
                double  lat_deg = Double.parseDouble(m.group(3)),
                        lat_min = Double.parseDouble(m.group(4)),
                        lat_sec = Double.parseDouble(m.group(5)),
                        lat_south = m.group(6).equalsIgnoreCase("S") ? -1 : 1,
                        lon_deg = Double.parseDouble(m.group(7)),
                        lon_min = Double.parseDouble(m.group(8)),
                        lon_sec = Double.parseDouble(m.group(9)),
                        lon_west = m.group(10).equalsIgnoreCase("W") ? -1 : 1;
                coords[0] = lon_west * truncGeo(lon_deg + lon_min / 60.0 + lon_sec / (60.0 * 60.0));
                coords[1] = lat_south * truncGeo(lat_deg + lat_min / 60.0 + lat_sec / (60.0 * 60.0));
            }
            catch (Exception e) {
                return null;
            }
            /*
             * Sane input
             */
            if((!isLatitudeSane(coords[1])) || (!isLongitudeSane(coords[0]))) {
                return null;
            }
            return label;
        }
        else if(name.contains("&")) {
            String token[] = new String[2];
            token[1] = token[0] = name;
            if(name.contains("@")) {
                /*
                 * This could be the geo point from maps
                 */
                token = name.split("@");
            }
            /*
             * This is lon/lat destination
             */
            String tokens[] = token[1].split("&");

            try {
                coords[0] = Double.parseDouble(tokens[1]);
                coords[1] = Double.parseDouble(tokens[0]);
            }
            catch (Exception e) {
                return null;
            }

            /*
             * Sane input
             */
            if((!isLatitudeSane(coords[1])) || (!isLongitudeSane(coords[0]))) {
                return null;
            }

            return token[0];

        }
        return null;
    }
}

