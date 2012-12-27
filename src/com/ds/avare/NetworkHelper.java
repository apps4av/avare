/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.LinkedList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;


/**
 * 
 * @author zkhan
 *
 */
public class NetworkHelper {
    
    private String mVersion = null;
    
    private static final int blocksize = 8192;

    /**
     * 
     */
    public NetworkHelper() {
    }

    /*
     * Delete a folder
     */
    static void deleteRecursive(File fileOrDirectory, Context ctx) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child, ctx);
            }
        }
        /*
         * Do not delete database
         */
        if(fileOrDirectory.getName().equals("database") || 
                fileOrDirectory.getName().equals(ctx.getString(R.string.DatabaseName))) {
            return;
        }
        fileOrDirectory.delete();
    }

    /**
     * 
     * @param data
     */
    private static void writeToFile(String filename, String data) {
        File file = new File(filename);
        try {
            file.createNewFile();
            if(file.exists()) {
                 OutputStream fo = new FileOutputStream(file);              
                 fo.write(data.getBytes());
                 fo.close();
            }
        }
        catch (Exception e) {
            
        }
    }

    /**
     * 
     * @return
     */
    public static String getDonationURL() {
        return "http://apps4av.net/";
    }

    /**
     * 
     * @return
     */
    public static String getHelpGeoTag() {
        return "http://youtu.be/mKOgguJ5bpo";
    }

    /**
     * 
     * @return
     */
    public static String getHelpDownload() {
        return "http://youtu.be/0Q0d4LIoTqY";
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
    static LinkedList<TFRShape> getTFRShapes(Context ctx, boolean[] metrics) {
        
        /*
         * Create a shapes list
         */
        LinkedList<TFRShape> shapeList = new LinkedList<TFRShape>();
        metrics[0] = true;
        
        /*
         * Do not download if not enabled
         */
        if(!(new Preferences(ctx)).shouldTFRAndMETARShow()) {
            return shapeList;
        }
        
        try {
            /*
             * Get weather if the point is on airport
             */
            String url = "http://apps4av.net/cgi-bin/tfr2.cgi";
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            
            if(null != entity) {
                String ent = EntityUtils.toString(entity);
                if(ent != null) {
                    Spanned html = Html.fromHtml(ent);
                    if(null != html) {
                        String data = html.toString();
                        if(null != data) {
                            /*
                             * Store for offline usage
                             */
                            String filename = new Preferences(ctx).mapsFolder() + "/tfr2.txt";
                            writeToFile(filename, data);
                            metrics[0] = false;
                        }
                    }
                }
            }
        }
        catch (Exception e) {

        }


        String filename = new Preferences(ctx).mapsFolder() + "/tfr2.txt";
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
    static String getMETAR(Context ctx, String airport) {
        
        /*
         * Do not download if not enabled
         */
        if(!(new Preferences(ctx)).shouldTFRAndMETARShow()) {
            return null;
        }

        try {
            /*
             * Get weather if the point is on airport
             */
            String url = getMETARUrl(airport);
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if(null != entity) {
                String ent = EntityUtils.toString(entity);
                if(ent != null) {
                    Spanned html = Html.fromHtml(ent);
                    if(null != html) {
                        String data = html.toString();
                        if(null != data) {
                            if(data.contains("Not Found")) {
                                return "";
                            }
                            return(data);
                        }
                    }
                }
            }
        }
        catch (Exception e) {

        }
        
        return null;
    }

    /**
     * 
     * @param airport
     * @return
     */
    static String getTAF(Context ctx, String airport) {
        
        /*
         * Do not download if not enabled
         */
        if(!(new Preferences(ctx)).shouldTFRAndMETARShow()) {
            return "";
        }

        try {
            /*
             * Get weather if the point is on airport
             */
            String url = getTAFUrl(airport);
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if(null != entity) {
                String ent = EntityUtils.toString(entity);
                if(ent != null) {
                    Spanned html = Html.fromHtml(ent);
                    if(null != html) {
                        String data = html.toString();
                        if(null != data) {
                            return(data);
                        }
                    }
                }
            }
        }
        catch (Exception e) {

        }
        
        /*
         * TAFS concatenate to METARS
         */
        return "";
    }

    /**
     * 
     * @param airports
     * @return
     */
    public static String getMETARUrl(String airport) {
        String query = "http://apps4av.net/cgi-bin/metar2.cgi?station=" + airport;

        return query;
    }

    /**
     * 
     * @param airports
     * @return
     */
    public static String getTAFUrl(String airport) {
        String query = "http://apps4av.net/cgi-bin/taf.cgi?station=" + airport;

        return query;
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
    public String getUrl(String file) {
        return("http://apps4av.net/" + mVersion + "/" + file);
    }
    
    /**
     * 
     * @return
     */
    public String getVersion() {
        /*
         * Do this on background task if possible
         */
        try {

            /*
             * Location of file on internet
             * Download which is current folder?
             */
            URL url = new URL("http://apps4av.net/update2.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()), blocksize);

            mVersion = in.readLine();
            in.close();
        }
        catch (Exception e) {
        }
        return mVersion;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public int getFileLength(String name) {
        /*
         * Path with file name on local storage
         */
        if(null == mVersion) {
            getVersion();
        }
        try {
            URL url = new URL("http://apps4av.net/" + mVersion + "/" + name);
            URLConnection connection = url.openConnection();
            connection.connect();
            /* 
             * this will be useful so that you can show a typical 0-100% progress bar
             */
            return(connection.getContentLength());
        } 
        catch (Exception e) {
        }

        return(0);
    }
}
