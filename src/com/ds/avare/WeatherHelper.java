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

import android.graphics.Color;


public class WeatherHelper {
    
    /**
     * 
     * @param TAF
     * @return
     */
    public static int metarSquare(String metar) {
        if(null == metar) {
            return R.drawable.whitesq;
        }
        String[] token = metar.split(",");
        if(token[0] == null) {
            return R.drawable.whitesq;
        }
        int color = metarColor(token[0]);
        
        if(color == Color.GREEN) {
            return(R.drawable.greensq);
        }
        else if(color == Color.RED) {
            return(R.drawable.redsq);            
        }
        else if(color == Color.BLUE) {
            return(R.drawable.bluesq);            
        }
        else if(color == Color.MAGENTA) {
            return(R.drawable.pinksq);            
        }
        else if(color == Color.WHITE) {
            return R.drawable.whitesq;
        }
        return R.drawable.whitesq;
    }
    
    /**
     * 
     * @param TAF
     * @return
     */
    public static int metarColor(String type) {
        if(type.equals("VFR")) {
            return(Color.GREEN);
        }
        else if(type.equals("IFR")) {
            return(Color.RED);                        
        }
        else if(type.equals("MVFR")) {
            return(Color.BLUE);                        
        }
        else if(type.equals("LIFR")) {
            return(Color.MAGENTA);                       
        }
        return(Color.WHITE);
    }
    
}
