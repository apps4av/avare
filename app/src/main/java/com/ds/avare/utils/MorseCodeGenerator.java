/*
Copyright (c) 2017, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by zkhan on 12/23/16.
 */

public class MorseCodeGenerator {

    private HashMap<String, String> mMap;
    private static MorseCodeGenerator mInstance;


    /**
     * Singleton
     */
    private MorseCodeGenerator() {
        mMap = new HashMap<String, String>();
        mMap.put("A", ".-");
        mMap.put("B", "-...");
        mMap.put("C", "-.-.");
        mMap.put("D", "-..");
        mMap.put("E", ".");
        mMap.put("F", "..-.");
        mMap.put("G", "--.");
        mMap.put("H", "....");
        mMap.put("I", "..");
        mMap.put("J", ".---");
        mMap.put("K", "-.-");
        mMap.put("L", ".-..");
        mMap.put("M", "--");
        mMap.put("N", "-.");
        mMap.put("O", "---");
        mMap.put("P", ".--.");
        mMap.put("Q", "--.-");
        mMap.put("R", ".-.");
        mMap.put("S", "...");
        mMap.put("T", "-");
        mMap.put("U", "..-");
        mMap.put("V", "...-");
        mMap.put("W", ".--");
        mMap.put("X", "-..-");
        mMap.put("Y", "-.--");
        mMap.put("Z", "--..");
        mMap.put("0", "-----");
        mMap.put("1", ".----");
        mMap.put("2", "..---");
        mMap.put("3", "...--");
        mMap.put("4", "....-");
        mMap.put("5", ".....");
        mMap.put("6", "-....");
        mMap.put("7", "--...");
        mMap.put("8", "---..");
        mMap.put("9", "----.");
    }

    /**
     * Make hash map.
     */
    public static MorseCodeGenerator getInstance() {
        if(null == mInstance) {
            mInstance = new MorseCodeGenerator();
        }
        return mInstance;
    }

    /**
     * Get morse code of string
     * @param arg
     * @return
     */
    public String getCodeHtml(String arg){
        arg = arg.toUpperCase(Locale.US);
        String[] data = arg.split("");
        String out = "";
        for(String c : data) {
            if(c.isEmpty()) {
                continue;
            }
            String code = mMap.get(c);
            if (null == code) {
                continue;
            }
            if (code.isEmpty()) {
                continue;
            }
            out += code + "&nbsp;";
        }
        out = out.replace("-", "&#8211;");
        out = out.replace(".", "&#8226;");
        return out;
    }

}
