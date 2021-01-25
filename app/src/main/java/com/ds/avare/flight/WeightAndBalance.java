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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * All lists get stored and get retrieved in JSON format
 * @author zkhan
 *
 */
public class WeightAndBalance {


    JSONObject mWnb;

    public static final int WNB_C172R = 1;
    public static final int WNB_PA28R_200B = 2;
    public static final int WNB_PA23_250 = 3;

    public WeightAndBalance(int type) {
        String ex;

        if(WNB_PA28R_200B == type) {
            ex = getExamplePA28R200B();
        }
        else if(WNB_PA23_250 == type) {
            ex = getExamplePA23250();
        }
        else {
            ex = getExampleC172R();
        }

        try {
            mWnb = new JSONObject(ex);
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
     * From JSON
     * @param json
     */
    public WeightAndBalance(String json) {
        try {
            mWnb = new JSONObject(json);
        } catch (JSONException e) {
            mWnb = null;
        }
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
     * 172R example
     * @return
     */
    public static String getExampleC172R() {
        return "{" +
                "'name'  :'Sample C172R'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'1666.8'," +
                "'a_0'   :'39.302',"+
                "'t_1'   :'Oil'," +
                "'w_1'   :'14'," +
                "'a_1'   :'-13.1'," +
                "'t_2'   :'Front passengers'," +
                "'w_2'   :'275'," +
                "'a_2'   :'37'," +
                "'t_3'   :'Back passengers'," +
                "'w_3'   :'110'," +
                "'a_3'   :'73'," +
                "'t_4'   :'Baggage'," +
                "'w_4'   :'30'," +
                "'a_4'   :'95'," +
                "'t_5'   :'Aft Baggage'," +
                "'w_5'   :'0'," +
                "'a_5'   :'123'," +
                "'t_6'   :'Fuel'," +
                "'w_6'   :'324'," +
                "'a_6'   :'48'," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'2450'," +
                "'min_w' :'1650'," +
                "'max_a' :'50'," +
                "'min_a' :'30'," +
                "'points':'47.3,1650 35,1650 35,1950 40,2450 47.3,2450 47.3,1650'" +
                "}";
    }

    private String getExamplePA23250() {
        return "{" +
                "'name'  :'Sample PA23 250'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'3178'," +
                "'a_0'   :'90.71',"+
                "'t_1'   :'Oil'," +
                "'w_1'   :'45'," +
                "'a_1'   :'55'," +
                "'t_2'   :'Front passengers'," +
                "'w_2'   :'275'," +
                "'a_2'   :'89'," +
                "'t_3'   :'Back passengers'," +
                "'w_3'   :'110'," +
                "'a_3'   :'157'," +
                "'t_4'   :'Nose Baggage'," +
                "'w_4'   :'30'," +
                "'a_4'   :'10'," +
                "'t_5'   :'Aft Baggage'," +
                "'w_5'   :'10'," +
                "'a_5'   :'183'," +
                "'t_6'   :'Fuel'," +
                "'w_6'   :'840'," +
                "'a_6'   :'113'," +
                "'t_7'   :'Rear passengers'," +
                "'w_7'   :'140'," +
                "'a_7'   :'126'," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'5300'," +
                "'min_w' :'2800'," +
                "'max_a' :'101'," +
                "'min_a' :'85'," +
                "'points':'100.5,2800 100.5,5200 99,5200 85.7,3250 86.3,2800 100.5,2800'" +
                "}";
    }

    private String getExamplePA28R200B() {
        return "{" +
                "'name'  :'Sample PA28R 200B'," +
                "'t_0'   :'Empty'," +
                "'w_0'   :'1577'," +
                "'a_0'   :'85.5',"+
                "'t_1'   :'Oil'," +
                "'w_1'   :'15'," +
                "'a_1'   :'29.5'," +
                "'t_2'   :'Front passengers'," +
                "'w_2'   :'275'," +
                "'a_2'   :'85.5'," +
                "'t_3'   :'Back passengers'," +
                "'w_3'   :'110'," +
                "'a_3'   :'118.1'," +
                "'t_4'   :'Baggage'," +
                "'w_4'   :'100'," +
                "'a_4'   :'142.8'," +
                "'t_5'   :''," +
                "'w_5'   :''," +
                "'a_5'   :''," +
                "'t_6'   :'Fuel'," +
                "'w_6'   :'288'," +
                "'a_6'   :'95'," +
                "'t_7'   :''," +
                "'w_7'   :''," +
                "'a_7'   :''," +
                "'t_8'   :''," +
                "'w_8'   :''," +
                "'a_8'   :''," +
                "'t_9'   :''," +
                "'w_9'   :''," +
                "'a_9'   :''," +
                "'max_w' :'2700'," +
                "'min_w' :'1400'," +
                "'max_a' :'96'," +
                "'min_a' :'80'," +
                "'points':'95.5,1400 95.5,2600 90,2600 81,1950 81,1400 95.5,1400'" +
                "}";
    }


}
