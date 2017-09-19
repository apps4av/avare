/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.flight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * All lists get stored and get retrieved in JSON format
 * @author zkhan
 *
 */
public class WeightAndBalance {


    JSONObject mWnb;


    public WeightAndBalance() {
        try {
            mWnb = new JSONObject(getExample());
        } catch (JSONException e) {

        }
    }

    /**
     * From JSON
     * @param json
     */
    public WeightAndBalance(JSONObject json) {
        mWnb = json;
    }

    /**
     * Get in JSON format
     * @return
     */
    public JSONObject getJSON() {
        return mWnb;
    }
    
    /**
     * 
     * @return
     */
    public String getName() {
        try {
            return mWnb.getString("name");
        } catch (JSONException e) {
        }
        return "";
    }

    /**
     * Put a list of WNBs in JSON array
     * @param wnbs
     * @return
     */
    public static String putWnbsToStorageFormat(LinkedList<WeightAndBalance> wnbs) {
        
        JSONArray jsonArr = new JSONArray();
        for(WeightAndBalance w : wnbs) {
            
            JSONObject o = w.getJSON();
            jsonArr.put(o);
        }
        
        return jsonArr.toString();
    }
    
    /**
     * Gets an array of WNBs from storage JSON
     * @return
     */
    public static LinkedList<WeightAndBalance> getWnbsFromStorageFromat(String json) {
        JSONArray jsonArr;
        LinkedList<WeightAndBalance> ret = new LinkedList<WeightAndBalance>();
        try {
            jsonArr = new JSONArray(json);
        } catch (JSONException e) {
            return ret;
        }
        
        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                JSONObject o = jsonArr.getJSONObject(i);
                ret.add(new WeightAndBalance(o));
            } catch (JSONException e) {
                continue;
            }
        }
        
        return ret;
    }

    /**
     * 172EF example
     * @return
     */
    public static String getExample() {
        return "{\"name\":\"Sample C172R\",\"t_0\":\"Empty\",\"t_1\":\"Oil\",\"t_2\":\"Front passengers\",\"t_3\":\"Back passengers\",\"t_4\":\"Baggage\",\"t_5\":\"Aft Baggage\",\"t_6\":\"Fuel\",\"t_7\":\"\",\"t_8\":\"\",\"t_9\":\"\",\"w_0\":\"1666.8\",\"w_1\":\"14\",\"w_2\":\"275\",\"w_3\":\"110\",\"w_4\":\"30\",\"w_5\":\"0\",\"w_6\":\"324\",\"w_7\":\"\",\"w_8\":\"\",\"w_9\":\"\",\"a_0\":\"39.302\",\"a_1\":\"-13.1\",\"a_2\":\"37\",\"a_3\":\"73\",\"a_4\":\"95\",\"a_5\":\"123\",\"a_6\":\"48\",\"a_7\":\"\",\"a_8\":\"\",\"a_9\":\"\",\"max_w\":\"2450\",\"min_w\":\"1650\",\"max_a\":\"50\",\"min_a\":\"30\",\"points\":\"47.3,1650 35,1650 35,1950 40,2450 47.3,2450 47.3,1650\"}";
    }

}
