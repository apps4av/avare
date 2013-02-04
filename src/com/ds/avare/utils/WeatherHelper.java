/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import com.ds.avare.R;


public class WeatherHelper {
    
    private static int BLUE = 0x7F0000BF;
    private static int RED = 0x7FBF0000;
    private static int GREEN = 0x7F00BF00;
    private static int MAGENTA = 0x7FBF00BF;
    private static int WHITE = 0x7FFFFFFF;
    
    /**
     * 
     * @param TAF
     * @return
     */
    public static int metarSquare(String metar) {
        if(null == metar) {
            return R.drawable.white_square;
        }
        String[] token = metar.split(",");
        if(token[0] == null) {
            return R.drawable.white_square;
        }
        int color = metarColor(token[0]);
        
        if(color == GREEN) {
            return(R.drawable.green_square);
        }
        else if(color == RED) {
            return(R.drawable.red_square);            
        }
        else if(color == BLUE) {
            return(R.drawable.blue_square);            
        }
        else if(color == MAGENTA) {
            return(R.drawable.pink_square);            
        }
        else if(color == WHITE) {
            return R.drawable.white_square;
        }
        return R.drawable.white_square;
    }
    
    /**
     * 
     * @param TAF
     * @return
     */
    public static int metarColor(String type) {
        if(type.equals("VFR")) {
            return(GREEN);
        }
        else if(type.equals("IFR")) {
            return(RED);                        
        }
        else if(type.equals("MVFR")) {
            return(BLUE);                        
        }
        else if(type.equals("LIFR")) {
            return(MAGENTA);                       
        }
        return(WHITE);
    }
    
    
    /**
     * 
     * @param weather
     * @return
     */
    public static String formatWeather(String weather) {
        weather = weather.replace("\n\n", "\n");
        weather = weather.replace(" FM", "\nFM");
        weather = weather.replace("BECMG", "\nBECMG"); 
        return weather;
    }
}
