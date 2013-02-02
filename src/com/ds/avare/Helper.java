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


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.WindowManager;

/**
 * 
 * @author zkhan
 *
 */
public class Helper {

    /**
     * 
     * @param heading
     * @return
     */
    public static String correctConvertHeading(long heading) {
        String ret = String.format("%03d", heading);
        if(ret.equals("000")) {
            ret = "360";
        }
        return ret;
    }
    
    /**
     * 
     * @param val
     * @return
     */
    public static String removeLeadingZeros(String val) {
        return val.replaceFirst("^0+(?!$)", ""); 
    }
    
    /**
     * 
     * @param variation
     * @return
     */
    static double parseVariation(String variation) {
        double var = 0;
        if((null == variation) || (variation.length() < 3)) {
            return 0;
        }
        else {
            var = Double.parseDouble(variation.substring(0, 2));            
            if(variation.contains("E")) {
                var = -var;                 
            }
        }
        return var;
    }

    /**
     * Set common features of all activities in the framework
     * @param act
     */
    public static void setOrientationAndOn(Activity act) {
        
        Preferences pref = new Preferences(act.getApplicationContext());
        if(pref.shouldScreenStayOn()) {
            act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);            
        }

        if(pref.isPortrait()) {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);            
        }
        else {
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

    }    
}
