/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.weather;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.WeatherHelper;


import android.content.Context;
import android.os.SystemClock;

/**
 * @author zkhan
 *
 */
public class WeatherCache {

    private HashMap<String, String> mMap;
    private Context mContext;
    
    /*
     * Drop METAR after 30 minutes.
     */
    private static long UPDATE_TIME = 30 * 60 * 1000;
    
    /**
     * 
     */
    public WeatherCache(Context context) {
        mMap = new HashMap<String, String>();
        mContext = context;
    }

    /**
     * 
     * @param id
     * @return
     */
    public String get(String id) {
       
        /*
         * Remove all entries older than update time.
         */
        String weather = null;
        /*
         * Save concurrent mod as many tasks update mMap.
         */
        synchronized(mMap) {
            Iterator<Entry<String, String>> it = mMap.entrySet().iterator();
            long now = SystemClock.elapsedRealtime();
            while (it.hasNext()) {
                HashMap.Entry<String, String> pairs = (HashMap.Entry <String, String>)it.next();
                String[] tokens = pairs.getValue().split("@");
                long then;
                try {
                    then = Long.parseLong(tokens[1]);
                    if(Math.abs(now - then) > UPDATE_TIME) {
                        it.remove();
                    }
                }
                catch (Exception e) {
                    it.remove();                    
                }
            }
            weather = mMap.get(id);
        }
        
        if(null == weather) {
            /*
             * Not found in cache
             */

            weather = NetworkHelper.getMETAR(mContext, id);
            
            /*
             * This is some sort of network error, return.
             */
            if(null == weather) {
                return "";
            }
            
            weather += NetworkHelper.getTAF(mContext, id);
            weather = WeatherHelper.formatWeather(weather);

            /*
             * Put in hash
             * @ time it was obtained
             */
            synchronized(mMap) {
                mMap.put(id, weather + "@" + SystemClock.elapsedRealtime());
            }
        }
                
        /*
         * Now get, remove @ sign
         */
        if(null != weather) {
            String tokens[] = weather.split("@");
            weather = tokens[0]; 
        }
        
        return weather;
    }
}
