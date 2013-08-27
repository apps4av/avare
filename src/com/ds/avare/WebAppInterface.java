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

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.WeatherHelper;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppInterface {
    Context mContext;
    StorageService mService; 
    Preferences mPref;

    /** 
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c, StorageService s) {
        mContext = c;
        mService = s;
        mPref = new Preferences(c);
    }

    /**
     * 
     * @param plan
     * @return
     */
    private String getPlan(String plan) {
        if(plan.equals("")) {
            return("");
        }
        String places[] = plan.split(">");
        String query = "";
        /*
         * Non GPS waypoints must be converted to GPS.
         */
        for(int i = 0; i < places.length; i++) {
            if(!places[i].contains("&")) {
                query += mService.getDBResource().findAirportLonLat(places[i]) + ";";
            }
            else {
                String latlon[] = places[i].split("&");
                query += latlon[1] + "," +latlon[0] + ";";
            }
        }

        if(query.equals("")) {
            return("");
        }

        return query;
    }
    
    /** 
     * Show METARS
     */
    @JavascriptInterface
    public String getMETARs(String plan) {
      
        String planf = getPlan(plan);
        if(planf.equals("")) {
            return "";
        }
        /*
         * 
         */
        String out = NetworkHelper.getMETARPlan(planf);
        String outm[] = out.split("::");
        String html = "";
        for(int i = 0; i < outm.length; i++) {
            String vals[] = outm[i].split(",");
            String color = WeatherHelper.metarColorString(vals[0]);
            html += "<font size='5' color='" + color + "'>" + vals[1] + "<br></br>";
        }
        
        return (html);
    }
    
    /** 
     * Show TAFS
     */
    @JavascriptInterface
    public String getTAFs(String plan) {
      
        String planf = getPlan(plan);
        if(planf.equals("")) {
            return "";
        }
        
        /*
         *  Get TAFs 
         */
        String out = NetworkHelper.getTAFPlan(planf);
        String outm[] = out.split("::");
        String html = "";
        for(int i = 0; i < outm.length; i++) {
            html += "<font size='5' color='black'>" + WeatherHelper.formatWeather(outm[i]) + "<br></br>";
        }
        
        return (html);
    }
    
    /** 
     * Show PIREPS
     */
    @JavascriptInterface
    public String getPIREPS(String plan) {
      
        String planf = getPlan(plan);
        if(planf.equals("")) {
            return "";
        }
        
        /*
         *  Get TAFs 
         */
        String out = NetworkHelper.getPIREPSPlan(planf);
        String outm[] = out.split("::");
        String html = "";
        for(int i = 0; i < outm.length; i++) {
            html += "<font size='5' color='black'>" + outm[i] + "<br></br>";
        }
        
        return (html);
    }
}