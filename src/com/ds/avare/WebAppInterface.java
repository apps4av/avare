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


import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.WeatherHelper;

import android.content.Context;
import android.os.AsyncTask;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppInterface {
    Context mContext;
    StorageService mService; 
    Preferences mPref;
    String mHtmlPirep;
    String mHtmlMetar;
    String mHtmlTaf;
    WeatherTask mWeatherTask;
    WebView mWebView;
    

    /** 
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c, StorageService s, WebView v) {
        mContext = c;
        mService = s;
        mWebView = v;
        mHtmlPirep = "";
        mHtmlTaf = "";
        mWeatherTask = null;
        mHtmlMetar = "";
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
        
        String query = "";
        
        String tokens[] = plan.split("\\)>");
        for(int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replaceAll("\\)", "");
            String pair[] = tokens[i].split("\\(");
            if(pair.length < 2) {
                continue;
            }
            if(!pair[1].equals(Destination.GPS)) {
                String lonlat = mService.getDBResource().findLonLat(pair[0], pair[1]) + ";";
                if(null != lonlat) {
                    query += lonlat;
                }
            }
            else {
                String latlon[] = pair[0].split("&");
                query += latlon[1] + "," + latlon[0] + ";";
            }
        }

        if(query.equals("")) {
            return("");
        }

        return query;
    }

    /** 
     * Get plans list
     */
    @JavascriptInterface
    public String getPlans() { 
        String plans[] = mPref.getPlans();
        String str = "<option value=''>Select a Plan</option>";
        int i;
        for(i = 0; i < plans.length; i++) {
            str += "<option value='" + plans[i] + "'>" + plans[i] + "</option>";
        }
        return(str);
    }

    /** 
     * Get weather data async
     */
    @JavascriptInterface
    public void getWeather(String plan, String miles) {
        mHtmlTaf = "";
        mHtmlPirep = "";
        mHtmlMetar = "";
        if(mWeatherTask != null) {
            if(mWeatherTask.getStatus() != AsyncTask.Status.FINISHED) {
                mWeatherTask.cancel(true);
            }
        }
        mWeatherTask = new WeatherTask();
        mWeatherTask.execute(plan, miles);
    }


    /**
     * @author zkhan
     *
     */
    private class WeatherTask extends AsyncTask<String, Void, Boolean> {


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(String... input) {
            
            String plan = (String)input[0];
            String miles = (String)input[1];
            String planf = getPlan(plan);
            if(planf.equals("")) {
                return false;
            }
            
            if(null == mService) {
                return false;
            }

            /*
             *  Get PIREP
             */
            try {
                String out = NetworkHelper.getPIREPSPlan(planf, miles);
                String outm[] = out.split("::::");
                for(int i = 0; i < outm.length; i++) {
                    mHtmlPirep += "<font size='5' color='black'>" + outm[i] + "<br></br>";
                }
            }
            catch(Exception e) {
                mHtmlPirep = mContext.getString(R.string.WeatherError);
            }

            try {
                /*
                 *  Get TAFs 
                 */
                String out = NetworkHelper.getTAFPlan(planf, miles);
                String outm[] = out.split("::::");
                for(int i = 0; i < outm.length; i++) {
                    String taf = WeatherHelper.formatWeatherHTML(outm[i]);
                    String vals[] = taf.split(" ");
                    taf = WeatherHelper.formatVisibilityHTML(WeatherHelper.formatWeatherTypeHTML(WeatherHelper.formatWindsHTML(taf.replace(vals[0], ""))));
                    mHtmlTaf += "<b><font size='5' color='black'>" + vals[0] + "</b><br>";
                    mHtmlTaf += "<font size='5' color='black'>" + taf + "<br></br>";
                }
            }
            catch(Exception e) {
                mHtmlTaf = mContext.getString(R.string.WeatherError);
            }
            
            try {
                /*
                 * 
                 */
                String out = NetworkHelper.getMETARPlan(planf, miles);
                String outm[] = out.split("::::");
                for(int i = 0; i < outm.length; i++) {
                    String vals[] = outm[i].split(",");
                    String vals2[] = vals[1].split(" ");
                    String color = WeatherHelper.metarColorString(vals[0]);
                    mHtmlMetar += "<b><font size='5' + color='" + color + "'>" + vals2[0] + "</b><br>";
                    mHtmlMetar += "<font size='5' color='" + color + "'>" + vals[1] + "<br></br>";
                }
            }
            catch(Exception e) {
                mHtmlMetar = mContext.getString(R.string.WeatherError);
            }

            mHtmlMetar = mHtmlMetar.replaceAll("'", "\"");
            mHtmlTaf = mHtmlTaf.replaceAll("'", "\"");
            mHtmlPirep = mHtmlPirep.replaceAll("'", "\"");
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            /*
             * Must run on UI thread!
             */
            String load = "javascript:updateData(" + 
                    "'" + mHtmlMetar + "'" + "," + 
                    "'" + mHtmlTaf + "'" + "," +
                    "'" + mHtmlPirep + "'" + 
                    ");";            
            mWebView.loadUrl(load);
        }
    }


}