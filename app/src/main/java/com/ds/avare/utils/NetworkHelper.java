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

import android.content.Context;


import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import javax.xml.parsers.SAXParserFactory;


/**
 * 
 * @author zkhan
 *
 */
public class NetworkHelper {
    

    /**
     * 
     */
    public NetworkHelper() {
    }
    
    /**
     * 
     * @return
     */
    public static String getDonationURL(String root) {
        return root + "donate.html";
    }

    /**
     * 
     * @param airport
     * @return
     */
    public static String getNAMMET(String airport) {
        
        //http://www.nws.noaa.gov/cgi-bin/mos/getmet.pl?sta=KALX
        
        try {
            URL url = new URL("https://www.nws.noaa.gov/cgi-bin/mos/getmet.pl?sta=K" + airport);
            Scanner s = new Scanner(url.openStream());
            int state = 0;
            String sb = "";
            while(s.hasNextLine()) {

                /*
                 * Parse pre formatted text from the NOAA website
                 * Strip out all other HTML.
                 */
                String line = s.nextLine();
                if(line.contains("<PRE>")) {
                    state = 1;
                }
                else if(line.contains("</PRE>")) {
                    state = 0;
                    break;
                }
                else if(state == 1) {
                    /*
                     * Only text that describes forecast
                     */
                    sb = sb + "<pre>" + line + "</pre>";
                }
            }
            s.close();
            return sb;
        }
        catch (Exception e) {
            
        }

        return "";
    }   
         
    /**
     * 
     * @param airport
     * @return
     */
    public static String getMETAR(String airport) {
        
        /*
         * Get TAF
         */
        String xml = "https://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=K" +
                airport + "&hoursBeforeNow=2";
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerMETAR saxHandler = new SAXXMLHandlerMETAR();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                return text;
            }
        }
        catch (Exception e) {
        }
        return "";
    }
        
    /**
     * 
     * @param airport
     * @return
     */
    public static String getTAF(String airport) {
        
        /*
         * Get TAF
         */
        String xml = "https://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=tafs&requestType=retrieve&format=xml&stationString=K"
                 + airport + "&hoursBeforeNow=2";
        
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerTAF saxHandler = new SAXXMLHandlerTAF();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                return text;
            }
        }
        catch (Exception e) {
            
        }
        return "";
    }

    /**
     * 
     * @param plan
     * @param miles
     * @return
     */
    public static String getPIREPS(String plan, String miles) {
        
        /*
         * Get PIREPS
         */
        String xml = 
                "https://aviationweather.gov/adds/dataserver_current/httpparam?datasource=pireps"
                + "&requestType=retrieve&format=xml&hoursBeforeNow=12" 
                + "&radialDistance=" + miles + ";" + plan;
        /*
         * Get PIREPS
         */
        String out = "";
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerPIREP saxHandler = new SAXXMLHandlerPIREP();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                out += text + "::::";
            }
        }
        catch (Exception e) {
            
        }        
        return out;
    }

    /**
     * 
     * @param plan
     * @return
     */
    public static String getMETARPlan(String plan, String miles) {
        
        String xml = 
                "https://aviationweather.gov/adds/dataserver_current/httpparam?datasource=metars"
                + "&requestType=retrieve&format=xml&mostRecentForEachStation=constraint&hoursBeforeNow=1.25" 
                + "&flightPath=" + miles + ";" + plan;
        /*
         * Get METAR
         */
        String out = "";
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerMETAR saxHandler = new SAXXMLHandlerMETAR();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                out += text + "::::";
            }
        }
        catch (Exception e) {
            
        }
        
        return out;
    }

    /**
     * 
     * @param plan
     * @return
     */
    public static String getTAFPlan(String plan, String miles) {
        
        String xml = 
                "https://aviationweather.gov/adds/dataserver_current/httpparam?datasource=tafs"
                + "&requestType=retrieve&format=xml&mostRecentForEachStation=constraint&hoursBeforeNow=1.25" 
                + "&flightPath=" + miles + ";" + plan;
        /*
         * Get TAF
         */
        String out = "";
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerTAF saxHandler = new SAXXMLHandlerTAF();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                out += text + "::::";
            }
        }
        catch (Exception e) {
            
        }
        
        return out;
    }


    /**
     * 
     * @param plan
     * @return
     */
    public static String getPIREPSPlan(String plan, String miles) {
        
        String xml = 
                "https://aviationweather.gov/adds/dataserver_current/httpparam?datasource=pireps"
                + "&requestType=retrieve&format=xml&hoursBeforeNow=12" 
                + "&flightPath=" + miles + ";" + plan;
        /*
         * Get PIREPS
         */
        String out = "";
        XMLReader xmlReader;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            SAXXMLHandlerPIREP saxHandler = new SAXXMLHandlerPIREP();
            xmlReader.setContentHandler(saxHandler);
            xmlReader.parse(new InputSource(xml));
            List<String> texts = saxHandler.getText();
            for(String text : texts) {
                out += text + "::::";
            }
        }
        catch (Exception e) {
            
        }
        
        return out;
    }

    /**
     * 
     */
    public static String getHelpUrl(Context ctx) {
        return(com.ds.avare.utils.Helper.getWebViewFile(ctx, "help"));
    }

    /**
     * 
     * @param file
     * @param vers
     * @param root
     * @return
     */
    public static String getUrl(String file, String vers, String root, boolean isStatic) {
        if(file.equals("TFRs.zip")) {
            return(root + "/" + file);
        }
        if(file.equals("GameTFRs.zip")) {
            return(root + "/" + file);
        }
        else if(file.equals("weather.zip")) {
            return(root + "/" + file);
        }
        else if(file.equals("conus.zip")) {
            return(root + "/" + file);
        }

        // See if it is a static chart (not updated every 28 days)
        if(!isStatic) {
            return (root + vers + "/" + file);
        }
        else {
            return (root + "static" + "/" + file);
        }
    }

    /*
     * 
     */
    private static final int getFirstDate(int year) {
        // Date for first cycle every year in January starting 2014
    	switch(year) {
    		case 2014:
    			return 9;
    		case 2015:
    			return 8;
    		case 2016:
    			return 7;
    		case 2017:
    			return 5;
    		case 2018:
    			return 4;
    		case 2019:
    			return 3;
    		case 2020:
    			return 2;
    		default:
    			return 0;
    	}
    }
    
    /**
     * Find the date in January when first cycle begins 
     */
    private static String getCycle() {
        /*
         * US locale as this is a folder name not language translation
         */
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        int year = now.get(Calendar.YEAR);
        int firstdate = getFirstDate(year);
        GregorianCalendar now2 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        now2.set(year, Calendar.JANUARY, firstdate, 9, 0, 0);
        if (now2.after(now)) {
        	/*
        	 * Lets handle the case when year has just turned
        	 */
        	year--;
        }
    	
    	// cycle's upper two digit are year
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    	int cycle = (year - 2000) * 100;
        
        if(firstdate < 1) {
        	return "";
        }
        
        // now find cycle on todays date
        epoch.set(year, Calendar.JANUARY, firstdate, 9, 0, 0);
        cycle++;
        epoch.add(Calendar.DAY_OF_MONTH, 28);
        if(!epoch.after(now)) {
            while(true) {
                epoch.add(Calendar.DAY_OF_MONTH, 28);
                cycle++;
                if(epoch.after(now)) {
                	break;
                }
            }
        }

        return "" + cycle;
    }
    
    /**
     * 
     * @param date
     * @return
     */
    public static boolean isExpired(String date, int timeout) {
        
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

            try {
                month = Integer.parseInt(dates[0]) - 1;
                day = Integer.parseInt(dates[1]);
                year = Integer.parseInt(dates[2]);

                String time[] = dates[3].split(":");
                hour = Integer.parseInt(time[0]);
                min = Integer.parseInt(time[1]);
            }
            catch (Exception e) {
                return true;
            }
            if(year < 1 || month < 0 || day < 1 || hour < 0 || min < 0) {
                return true;
            }
            /*
             * so many min expiry
             */
            expires.set(year, month, day, hour, min);
            expires.add(Calendar.MINUTE, timeout);
            if(now.after(expires)) {
                return true;
            }
            
            return false;
        }
        
        if(!getCycle().equals(date)) {
        	return true;
        }
        
        return false;
    }

    /**
     * 
     * @return
     */
    public static String getVersion(int offset) {
    	return findCycleOffset(getVersion("", "", null), offset);
    }

    /**
     * 
     * @return
     */
    public static String getVersion(String root, String name, boolean[] networkState) {
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String netVers = getVersionNetwork(root);
        
        if(networkState != null && networkState.length != 0) {
        	networkState[0] = netVers != null;
        }

        /*
         * Expires every so many mins
         */
        if(name.equals("TFRs") || name.equals("weather") || name.equals("conus")) {
            return String.format(Locale.US, "%02d_%02d_%04d_%02d:%02d_UTC", now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.YEAR),
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE));
        }
        else if(netVers != null) {
        	return netVers;
        }
        return getCycle();
    }


    /**
     * 
     * @return
     */
    private static String getVersionNetwork(String root) {

        /*
         * Download version from the internet first, then if not found,
         * calculate what it should be
         */
        try {
            URL u = new URL(root + "version.php");
            URLConnection c = u.openConnection();
            InputStream r = c.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(r));
            String line = reader.readLine();
            if(null != line) {
                return line;
            }
        }
        catch (Exception e) {

        }

        return null;
    }

    /**
     * Find a range for the FAA cycle
     * @return
     */
    public static String getVersionRange(String cycleName) {
        int cycle;
        try {
        	cycle = Integer.parseInt(cycleName);
        }
        catch (Exception e) {
        	return "";
        }
        
        // like 1510 = 15, 10 (15 means 2015, 10 means #28 days)
        int cycleupper = (int)(cycle / 100);
        int cyclelower = cycle - (cycleupper * 100);
        int firstdate = getFirstDate(2000 + cycleupper);
        if(firstdate < 1) {
        	return "";
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        String ret = "";
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        epoch.set(2000 + cycleupper, Calendar.JANUARY, firstdate, 9, 0, 0);
        epoch.add(Calendar.DAY_OF_MONTH, 28 * (cyclelower - 1));
        ret = "(" + sdf.format(epoch.getTime());
        epoch.add(Calendar.DAY_OF_MONTH, 28);
        ret += "-" + sdf.format(epoch.getTime()) + ")";
        return ret;
    }

    
    /**
     * Find cycle + or - offset
     * @return
     */
    public static String findCycleOffset(String cycleName, int offset) {        
        
        int cycle;
        try {
        	cycle = Integer.parseInt(cycleName);
        }
        catch (Exception e) {
        	return cycleName;
        }
        
        // like 1510 = 15, 10 (15 means 2015, 10 means #28 days)
        int cycleupper = (int)(cycle / 100);
        int cyclelower = cycle - (cycleupper * 100);
        int firstdate = getFirstDate(2000 + cycleupper);
        if(firstdate < 1) {
        	return cycleName;
        }
        
        // find cycle time with offset
        GregorianCalendar then = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        then.set(2000 + cycleupper, Calendar.JANUARY, firstdate, 9, 0, 0);
        then.add(Calendar.DAY_OF_MONTH, 28 * (cyclelower - 1 + offset));
        
        // find upper two digits of cycle.
    	cycleupper = (then.get(Calendar.YEAR) - 2000);
    	
    	// find cyclelower
    	firstdate = getFirstDate(2000 + cycleupper);
        GregorianCalendar first = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        first.set(2000 + cycleupper, Calendar.JANUARY, firstdate, 9, 0, 0);

        cycle = cycleupper * 100 + 1;
        while(first.before(then)) {
            first.add(Calendar.DAY_OF_MONTH, 28);
            cycle++;
        }

        return "" + cycle;
    }

    /**
     * Get notams from FAA in the plan form KBOS,BOS,KLWM
     * @param plan
     * @return
     */
    public static String getNotams(String plan) {
        String ret = null;
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("retrieveLocId", plan);
            params.put("reportType", "Raw");
            params.put("actionType", "notamRetrievalByICAOs");
            params.put("submit", "View+NOTAMSs");
            ret = com.ds.avare.message.NetworkHelper.post("https://www.notams.faa.gov/dinsQueryWeb/queryRetrievalMapAction.do",
                    params);
        } catch (Exception e) {

        }

        // NOTAMS are in form <PRE></PRE>. Parse them, and convert \n to BR
        String notams = "";
        if(ret != null) {
            String rets[] = ret.split("\\<PRE\\>");
            for (String ret1 : rets) {
                if(ret1.contains("</PRE>")) {
                    String parsed[] = ret1.split("</PRE>");
                    notams += parsed[0] + "\n\n";
                }
            }
            notams = notams.replaceAll("(\r\n|\n)", "<br />");
        }

        return notams;
    }
}


