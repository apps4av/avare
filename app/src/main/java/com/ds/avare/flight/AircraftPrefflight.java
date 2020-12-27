/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.flight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

/**
 * All lists get stored and get retrieved in JSON format
 * @author zkhan
 * Modified by kbabbert for aircraft preferences from WeightAndBalance.java
 */
public class AircraftPrefflight {


    JSONObject mAcp;

    public static final int ACP_SAMPLE1 = 1;
    public static final int ACP_SAMPLE2 = 2;


    public AircraftPrefflight(int type) {
        String ex;

        if(ACP_SAMPLE2 == type) {
            ex = getExample2();
        }
        else {
            ex = getExample1();
        }

        try {
            mAcp = new JSONObject(ex);
        } catch (JSONException e) {

        }
    }

    /**
     * From JSON
     * @param json
     */
    public AircraftPrefflight(JSONObject json) {
        mAcp = json;
    }

    /**
     * Get in JSON format
     * @return
     */
    public JSONObject getJSON() {
        return mAcp;
    }
    
    /**
     * 
     * @return
     */
    public String getName() {
        try {
            return mAcp.getString("name");
        } catch (JSONException e) {
        }
        return "";
    }

    /**
     * Put a list of ACPs in JSON array
     * @param acps
     * @return
     */
    public static String putAcpsToStorageFormat(LinkedList<AircraftPrefflight> acps) {
        
        JSONArray jsonArr = new JSONArray();
        for(AircraftPrefflight w : acps) {
            
            JSONObject o = w.getJSON();
            jsonArr.put(o);
        }
        
        return jsonArr.toString();
    }
    
    /**
     * Gets an array of ACPs from storage JSON
     * @return
     */
    public static LinkedList<AircraftPrefflight> getAcpsFromStorageFormat(String json) {
        JSONArray jsonArr;
        LinkedList<AircraftPrefflight> ret = new LinkedList<>();
        try {
            jsonArr = new JSONArray(json);
        } catch (JSONException e) {
            return ret;
        }
        
        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                JSONObject o = jsonArr.getJSONObject(i);
                ret.add(new AircraftPrefflight(o));
            } catch (JSONException e) {
                continue;
            }
        }
        
        return ret;
    }

    /**
     * Examples
     * @return
     */
    public static String getExample1() {
        return "{" +
                "'name'  :'Sample 1'," +
                "'t_0'   :'Pilot Contact'," +
                "'w_0'   :'John Pilot 614-555-1234'," +
                "'t_1'   :'Home Base'," +
                "'w_1'   :'KABC'," +
                "'t_2'   :'Tail Number'," +
                "'w_2'   :'N12345'," +
                "'t_3'   :'Aircraft Type'," +
                "'w_3'   :'C172'," +
                "'t_4'   :'Aircraft ICAO Code'," +
                "'w_4'   :'A1234A'," +
                "'t_5'   :'Aircraft Color - First'," +
                "'w_5'   :'W'," +
                "'t_6'   :'Aircraft Color - Second'," +
                "'w_6'   :'R'," +
                "'t_7'   :'Aircraft Equipment'," +
                "'w_7'   :'SBG'," +
                "'t_8'   :'Aircraft Surveillance'," +
                "'w_8'   :'SU2'," +
                "'t_9'   :'Aircraft Other Info'," +
                "'w_9'   :'PBN/B2C2'," +
                "'t_10'   :'Aircraft TAS'," +
                "'w_10'   :'90'," +
                "'t_11'   :'Aircraft Best Glide Sink Rate'," +
                "'w_11'   :'700'," +
                "'t_12'   :'Aircraft Fuel Burn Rate'," +
                "'w_12'   :'8'," +
                "'t_13'   :'Emergency Checklist'," +
                "'w_13'   :''" +
                "}";
    }

    private String getExample2() {
        return "{" +
                "'name'  :'Sample 2'," +
                "'t_0'   :'Pilot Contact'," +
                "'w_0'   :'Mary Pilot 614-555-1234'," +
                "'t_1'   :'Home Base'," +
                "'w_1'   :'KABC'," +
                "'t_2'   :'Tail Number'," +
                "'w_2'   :'N1234P'," +
                "'t_3'   :'Aircraft Type'," +
                "'w_3'   :'PA24'," +
                "'t_4'   :'Aircraft ICAO Code'," +
                "'w_4'   :'A6711A'," +
                "'t_5'   :'Aircraft Color - First'," +
                "'w_5'   :'W'," +
                "'t_6'   :'Aircraft Color - Second'," +
                "'w_6'   :'B'," +
                "'t_7'   :'Aircraft Equipment'," +
                "'w_7'   :'SBG'," +
                "'t_8'   :'Aircraft Surveillance'," +
                "'w_8'   :'SU1'," +
                "'t_9'   :'Aircraft Other Info'," +
                "'w_9'   :''," +
                "'t_10'   :'Aircraft TAS'," +
                "'w_10'   :'145'," +
                "'t_11'   :'Aircraft Best Glide Sink Rate'," +
                "'w_11'   :'700'," +
                "'t_12'   :'Aircraft Fuel Burn Rate'," +
                "'w_12'   :'14'," +
                "'t_13'   :'Emergency Checklist'," +
                "'w_13'   :'Electrical'" +
                "}";
    }



}
