/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
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
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ds.avare.R;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;

import android.content.Context;


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
     * @param url
     * @return
     */
    private static String getXmlFromUrl(String url) {
        String xml = null;
 
        try {
            
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
 
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            xml = EntityUtils.toString(httpEntity);
 
        } 
        catch (Exception e) {
        }
        return xml;
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
                            "TFR:: ", ctx.getString(R.string.TFRReceived) + " " + time.toString() + "-").
                            replace("Top", "\nTop").
                            replace("Low", "\nLow").
                            replace("Eff", "\nEff").
                            replace("Exp", "\nExp"));
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
                shapeList.add(shape);
            }
        }
        
        return shapeList;
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
        String xml = getXmlFromUrl("http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=K" + 
                airport + "&hoursBeforeNow=2");
        if(xml != null) {
            Document doc = getDomElement(xml);
            if(null != doc) {
                
                NodeList nl = doc.getElementsByTagName("METAR");
                if(0 == nl.getLength()) {
                    return "";
                }
                for (int temp = 0; temp < nl.getLength(); temp++) {
                    
                    Node nNode = nl.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
             
                        /*
                         * Return most recent
                         */
                        String txt = "";
                        String cat = "";
                        NodeList n = eElement.getElementsByTagName("raw_text");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    txt = n.item(0).getTextContent();
                                }
                            }
                        }
                        n = eElement.getElementsByTagName("flight_category");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    cat = n.item(0).getTextContent();
                                }
                            }
                        }
                        if(cat.equals("") || txt.equals("")) {
                            return "";
                        }
                        return(cat + "," + txt);
                    }
                }
            }
        }
        
        return null;
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
        String xml = getXmlFromUrl("http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=tafs&requestType=retrieve&format=xml&stationString=K"
                 + airport + "&hoursBeforeNow=2");
        if(xml != null) {
            Document doc = getDomElement(xml);
            if(null != doc) {
                
                NodeList nl = doc.getElementsByTagName("TAF");
                if(0 == nl.getLength()) {
                    return "";
                }
                for (int temp = 0; temp < nl.getLength(); temp++) {
                    
                    Node nNode = nl.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
             
                        /*
                         * Return most recent
                         */
                        NodeList n = eElement.getElementsByTagName("raw_text");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    return(n.item(0).getTextContent());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        /*
         * TAFS concatenate to METARS
         */
        return "";
    }

    /**
     * 
     * @param plan
     * @return
     */
    public static String getMETARPlan(String plan) {
        
        String query = 
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=metars"
                + "&requestType=retrieve&format=xml&mostRecentForEachStation=constraint&hoursBeforeNow=1.25" 
                + "&flightPath=50;" + plan;
        /*
         * Get TAF
         */
        String xml = getXmlFromUrl(query);
        if(xml != null) {
            Document doc = getDomElement(xml);
            if(null != doc) {
                String out = "";
                
                NodeList nl = doc.getElementsByTagName("METAR");
                if(0 == nl.getLength()) {
                    return "";
                }
                /*
                 * Return most recent
                 */
                for (int temp = 0; temp < nl.getLength(); temp++) {
                    
                    String txt = "";
                    String cat = "";
                    Node nNode = nl.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
             
                        NodeList n = eElement.getElementsByTagName("raw_text");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    txt = n.item(0).getTextContent();
                                }
                            }
                        }
                        n = eElement.getElementsByTagName("flight_category");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    cat = n.item(0).getTextContent();
                                }
                            }
                        }
                    }
                    if(cat.equals("") || txt.equals("")) {
                        continue;
                    }
                    out += cat + "," + txt + "::";
                }
                return out;
            }
        }
        
        return "";
    }

    /**
     * 
     * @param plan
     * @return
     */
    public static String getTAFPlan(String plan) {
        
        String query = 
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=tafs"
                + "&requestType=retrieve&format=xml&mostRecentForEachStation=constraint&hoursBeforeNow=1.25" 
                + "&flightPath=50;" + plan;
        /*
         * Get TAF
         */
        String xml = getXmlFromUrl(query);
        if(xml != null) {
            Document doc = getDomElement(xml);
            if(null != doc) {
                String out = "";
                
                NodeList nl = doc.getElementsByTagName("TAF");
                if(0 == nl.getLength()) {
                    return "";
                }
                /*
                 * Return most recent
                 */
                for (int temp = 0; temp < nl.getLength(); temp++) {
                    
                    String txt = "";
                    Node nNode = nl.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
             
                        NodeList n = eElement.getElementsByTagName("raw_text");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    txt = n.item(0).getTextContent();
                                }
                            }
                        }
                    }
                    if(txt.equals("")) {
                        continue;
                    }
                    out += txt + "::";
                }
                return out;
            }
        }
        
        return "";
    }


    /**
     * 
     * @param plan
     * @return
     */
    public static String getPIREPSPlan(String plan) {
        
        String query = 
                "http://aviationweather.gov/adds/dataserver_current/httpparam?datasource=pireps"
                + "&requestType=retrieve&format=xml&mostRecentForEachStation=constraint&hoursBeforeNow=12" 
                + "&flightPath=50;" + plan;
        /*
         * Get PIREPS
         */
        String xml = getXmlFromUrl(query);
        if(xml != null) {
            Document doc = getDomElement(xml);
            if(null != doc) {
                String out = "";
                
                NodeList nl = doc.getElementsByTagName("PIREP");
                if(0 == nl.getLength()) {
                    return "";
                }
                /*
                 * Return most recent
                 */
                for (int temp = 0; temp < nl.getLength(); temp++) {
                    
                    String txt = "";
                    Node nNode = nl.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
             
                        NodeList n = eElement.getElementsByTagName("raw_text");
                        if(n != null) {
                            if(n.item(0) != null) {
                                if(n.item(0).getTextContent() != null) {
                                    txt = n.item(0).getTextContent();
                                }
                            }
                        }
                    }
                    if(txt.equals("")) {
                        continue;
                    }
                    out += txt + "::";
                }
                return out;
            }
        }
        
        return "";
    }

    /**
     * 
     */
    public static String getHelpUrl() {
        return("file:///android_asset/avare-offlinehelp.html");
    }

    /**
     * 
     * @param version
     * @param file
     * @return
     */
    public static String getUrl(String file, String vers, String root) {
        if(file.equals("TFRs.zip")) {
            return(root + "/" + file);
        }
        return(root + vers + "/" + file);
    }
    
    /**
     * 
     * @return
     */
    public static String getVersion(String name) {
        String ret = "";
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        GregorianCalendar epoch = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        epoch.set(2013, 6, 25, 9, 0, 0);
        if(!name.equals("TFRs")) {
            while(epoch.before(now)) {
                epoch.add(Calendar.DAY_OF_MONTH, 28);
            }
            epoch.add(Calendar.DAY_OF_MONTH, -28);
            /*
             * US locale as this is a folder name not language translation
             */
            ret = String.format(Locale.US, "%02d_%02d_%04d", epoch.get(Calendar.MONTH) + 1,
                    epoch.get(Calendar.DAY_OF_MONTH),
                    epoch.get(Calendar.YEAR));
        }
        else {
            ret = String.format(Locale.US, "%02d_%02d_%04d_%02d:%02d_UTC", now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.YEAR),
                    now.get(Calendar.HOUR_OF_DAY),
                    0);
        }
        return ret;
    }
    
    /**
     * 
     * @param xml
     * @return
     */
    private static Document getDomElement(String xml){
        
        /*
         * XML parser
         */
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
 
            DocumentBuilder db = dbf.newDocumentBuilder();
 
            InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(xml));
                doc = db.parse(is); 
        }
        catch (Exception e) {
        }
        return doc;
    }

}
