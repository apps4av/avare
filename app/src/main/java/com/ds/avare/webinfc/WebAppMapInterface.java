/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.webinfc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.utils.WindsAloftHelper;
import com.ds.avare.weather.Airep;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppMapInterface {
    private Context mContext;
    private WebView mWebView;
    private Preferences mPref;
    private GenericCallback mCallback;

    private static final int MSG_SET_DATA = 1;
    private static final int MSG_ACTION = 2;

    /**
     * Instantiate the interface and set the context
     */
    public WebAppMapInterface(Context c, WebView v, GenericCallback cb) {
        mWebView = v;
        mContext = c;
        mPref = new Preferences(c);
        mCallback = cb;
    }

    /**
     * Do something on a button press
     */
    @JavascriptInterface
    public void doAction(String action) {
        Message m = mHandler.obtainMessage();
        m.obj = action;
        m.what = MSG_ACTION;
        mHandler.sendMessage(m);
    }


    public void setData(LongTouchDestination data) {
        Message m = mHandler.obtainMessage();
        m.obj = data;
        m.what = MSG_SET_DATA;
        mHandler.sendMessage(m);
    }



    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     * Must use handler for functions called from JS, but for uniformity, call all JS from this handler
     */
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (MSG_SET_DATA == msg.what) {


                LongTouchDestination data = (LongTouchDestination)msg.obj;
                String taf = "";
                if(data.taf != null) {
                    String split[] = data.taf.rawText.split(data.taf.stationId, 2);
                    // Do not color code airport name
                    if(split.length == 2) {
                        taf = "<hr><b><font color=\"yellow\">TAF </font></b>" + data.taf.stationId + " " + WeatherHelper.formatVisibilityHTML(WeatherHelper.formatTafHTML(WeatherHelper.formatWindsHTML(WeatherHelper.formatWeatherHTML(split[1], mPref.isWeatherTranslated()), mPref.isWeatherTranslated()), mPref.isWeatherTranslated()));
                    }
                }

                String metar = "";
                if(data.metar != null) {
                    metar = WeatherHelper.formatMetarHTML(data.metar.rawText, mPref.isWeatherTranslated());
                    metar = "<hr><b><font color=\"yellow\">METAR </font></b>" + "<font color=\"" + WeatherHelper.metarColorString(data.metar.flightCategory) + "\">" + metar +  "</font>";
                }

                String airep = "";
                if(data.airep != null) {
                    for(Airep a : data.airep) {
                        String p = WeatherHelper.formatPirepHTML(a.rawText, mPref.isWeatherTranslated());
                        airep += p + "<br><br>";
                    }
                    if(!airep.equals("")) {
                        airep = "<hr><b><font color=\"yellow\">PIREP</font></b><br>" + airep;
                    }
                }

                String sua = "";
                if(data.sua != null) {
                    sua = "<hr><b><font color=\"yellow\">Special Use Airspace</font></b><br>";
                    sua += data.sua.replace("\n", "<br>");
                }

                String tfr = "";
                if(data.tfr != null) {
                    if(!data.tfr.equals("")) {
                        tfr = "<hr><b><font color=\"yellow\">TFR</font></b><br>";
                        tfr += data.tfr.replace("\n", "<br>");
                    }
                }

                String layer = mPref.useAdsbWeather() ?
                        "<hr><b><font color=\"yellow\">Weather/SUA Source</font></b> ADS-B<br>" :
                        "<hr><b><font color=\"yellow\">Weather/SUA Source</font></b> Internet<br>";
                if(data.layer != null) {
                    if(!data.layer.equals("")) {
                        layer += "<b><font color=\"yellow\">Weather Layer Time</font></b> ";
                        layer += data.layer;
                    }
                }

                String mets = "";
                if(data.mets != null) {
                    if(!data.mets.equals("")) {
                        mets = "<hr><b><font color=\"yellow\">SIG/AIRMETs</font></b><br>";
                        mets += data.mets.replace("\n", "<br>");
                    }
                }

                String performance = "";
                if(data.performance != null) {
                    if(!data.performance.equals("")) {
                        performance = "<hr><b><font color=\"yellow\">Performance</font></b> ";
                        performance += data.performance.replace("\n", "<br>");
                    }
                }

                String winds = "";
                if(data.wa != null) {
                    winds = "<hr><b><font color=\"yellow\">Winds/Temp. Aloft</font></b> ";
                    winds += WindsAloftHelper.formatWindsHTML(data.wa, mPref.getWindsAloftCeiling());
                }

                String navaids = "";
                if (data.navaids != null) {
                    data.info += "<br>" + data.navaids;
                }

                mWebView.loadUrl("javascript:plan_clear()");
                String func = "javascript:setData('" +
                        Helper.formatJsArgs(data.airport) + "','" +
                        "<b><font color=\"yellow\">Position </font></b>" + Helper.formatJsArgs(data.info) + "','" +
                        Helper.formatJsArgs(metar) + "','" +
                        Helper.formatJsArgs(taf) + "','" +
                        Helper.formatJsArgs(airep) + "','" +
                        Helper.formatJsArgs(tfr) + "','" +
                        Helper.formatJsArgs(sua) + "','" +
                        Helper.formatJsArgs(mets) + "','" +
                        Helper.formatJsArgs(performance) + "','" +
                        Helper.formatJsArgs(winds) + "','" +
                        Helper.formatJsArgs(layer) +
                        "')";



                mWebView.loadUrl(func);
            }
            else if (MSG_ACTION == msg.what) {
                mCallback.callback((String)msg.obj, null);
            }
        }
    };
}