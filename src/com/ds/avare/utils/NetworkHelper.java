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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


/**
 * 
 * @author zkhan
 *
 */
public class NetworkHelper {
    
    public static final int EXPIRES = 10;
    
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
            URL url = new URL("http://www.nws.noaa.gov/cgi-bin/mos/getmet.pl?sta=K" + airport);
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
        String xml = "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=K" + 
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
            e.printStackTrace();
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
        String xml = "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=tafs&requestType=retrieve&format=xml&stationString=K"
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
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=pireps"
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
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=metars"
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
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=tafs"
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
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=pireps"
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
    public static String getHelpUrl() {
        return("file:///android_asset/avare-offlinehelp.html");
    }

    /**
     * 
     * @param file
     * @param vers
     * @param root
     * @return
     */
    public static String getUrl(String file, String vers, String root) {
        if(file.equals("TFRs.zip")) {
            return(root + "/" + file);
        }
        else if(file.equals("weather.zip")) {
            return(root + "/" + file);
        }
        else if(file.equals("conus.zip")) {
            return(root + "/" + file);
        }
        return(root + vers + "/" + file);
    }
    
    /**
     * 
     * @return
     */
    public static String getVersion(String root, String name) {
        int cycle = 1400;
        String ret = "";
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        epoch.set(2013, 11, 12, 9, 0, 0);
        /*
         * Expires every so many mins
         */
        if(name.equals("TFRs") || name.equals("weather") || name.equals("conus")) {
            ret = String.format(Locale.US, "%02d_%02d_%04d_%02d:%02d_UTC", now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.YEAR),
                    now.get(Calendar.HOUR_OF_DAY),
                    EXPIRES * (int)(now.get(Calendar.MINUTE) / EXPIRES));
        }
        else {

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


            while(epoch.before(now)) {
                epoch.add(Calendar.DAY_OF_MONTH, 28);
                cycle++;
            }
            epoch.add(Calendar.DAY_OF_MONTH, -28);
            cycle--;
            /*
             * US locale as this is a folder name not language translation
             */
            ret = "" + cycle;
        }
        return ret;
    }
    
}
