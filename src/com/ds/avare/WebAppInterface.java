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
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppInterface {
    private Context mContext;
    private StorageService mService; 
    private WeatherTask mWeatherTask;
    private Thread mWeatherThread;
    private WebView mWebView;
    private Preferences mPref;


    /** 
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c, WebView v) {
        mWebView = v;
        mContext = c;
        mPref = new Preferences(c);
        mWeatherTask = null;
        mWeatherTask = new WeatherTask();
        mWeatherThread = new Thread(mWeatherTask);
        mWeatherThread.start();
    }
    
    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
    }

    /** 
     * Get weather data async
     */
    @JavascriptInterface
    public void getWeather() {
        mWeatherThread.interrupt();
    }


    /**
     * @author zkhan
     *
     */
    private class WeatherTask implements Runnable {

        private boolean running = true;

        /* (non-Javadoc)
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Weather");
            
            while(running) {
    
                try {
                    Thread.sleep(1000 * 3600 * 100);
                }
                catch (Exception e) {
                    
                }

                String Pirep = "";
                String Metar = "";
                String Taf = "";
    
                String miles = "30";
                String planf = "";
                String plan = "";
                if(null == mService) {
                    Message m = new Message();
                    m.obj = mContext.getString(R.string.WeatherPlan);
                    mHandler.sendMessage(m);
                    continue;
                }
    
                int num = mService.getPlan().getDestinationNumber();
                if(num < 2) {
                    /*
                     * Not a route.
                     */
                    Message m = new Message();
                    m.obj = mContext.getString(R.string.WeatherPlan);
                    mHandler.sendMessage(m);
                    continue;
                }
                for(int i = 0; i < num; i++) {
                    Location l = mService.getPlan().getDestination(i).getLocation();
                    planf += l.getLongitude() + "," + l.getLatitude() + ";";
                    plan += mService.getPlan().getDestination(i).getID() + "(" +
                            mService.getPlan().getDestination(i).getType() + ") ";
                }
                if(planf.equals("")) {
                    Message m = new Message();
                    m.obj = mContext.getString(R.string.WeatherPlan);
                    mHandler.sendMessage(m);
                    continue;
                }                
                
                /*
                 *  Get PIREP
                 */
                try {
                    String out = NetworkHelper.getPIREPSPlan(planf, miles);
                    String outm[] = out.split("::::");
                    for(int i = 0; i < outm.length; i++) {
                        outm[i] = WeatherHelper.formatPirepHTML(outm[i], mPref.isWeatherTranslated());
                        Pirep += "<font size='5' color='black'>" + outm[i] + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Pirep = mContext.getString(R.string.WeatherError);
                }
    
                try {
                    /*
                     *  Get TAFs 
                     */
                    String out = NetworkHelper.getTAFPlan(planf, miles);
                    String outm[] = out.split("::::");
                    for(int i = 0; i < outm.length; i++) {
                        String taf = WeatherHelper.formatWeatherHTML(outm[i], mPref.isWeatherTranslated());
                        String vals[] = taf.split(" ");
                        taf = WeatherHelper.formatVisibilityHTML(WeatherHelper.formatTafHTML(WeatherHelper.formatWindsHTML(taf.replace(vals[0], ""), mPref.isWeatherTranslated()), mPref.isWeatherTranslated()));
                        Taf += "<b><font size='5' color='black'>" + vals[0] + "</b><br>";
                        Taf += "<font size='5' color='black'>" + taf + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Taf = mContext.getString(R.string.WeatherError);
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
                        Metar += "<b><font size='5' + color='" + color + "'>" + vals2[0] + "</b><br>";
                        Metar += "<font size='5' color='" + color + "'>" + WeatherHelper.formatMetarHTML(vals[1].replace(vals2[0], ""), mPref.isWeatherTranslated()) + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Metar = mContext.getString(R.string.WeatherError);
                }
    
                plan = "<font size='5' color='black'>" + plan + "</font><br></br>";
                plan = "<form>" + plan.replaceAll("'", "\"") + "</form>";
                Metar = "<font size='6' color='black'>METARs</font><br></br>" + Metar; 
                Metar = "<form>" + Metar.replaceAll("'", "\"") + "</form>";
                Taf = "<font size='6' color='black'>TAFs</font><br></br>" + Taf; 
                Taf = "<form>" + Taf.replaceAll("'", "\"") + "</form>";
                Pirep = "<font size='6' color='black'>PIREPs</font><br></br>" + Pirep; 
                Pirep = "<form>" + Pirep.replaceAll("'", "\"") + "</form>";
                
                String weather = plan + Metar + Taf + Pirep;
    
                Message m = new Message();
                m.obj = weather;
                mHandler.sendMessage(m);
            }        
        }
    }

    
    /**
     * 
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String data = (String)msg.obj;
            String load = "javascript:updateData('" + data + "');";
            mWebView.loadUrl(load);                        
        }
    };

    /**
     * 
     */
    public void cleanup() {
        mWeatherTask.running = false;
        mWeatherThread.interrupt();
    }
}