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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.content.Context;

import com.ds.avare.StorageService;

public class ContentGenerator {

    public static String makeContentImage(Context context, StorageService service) {
        
        /*
         * Download the airmet time file
         */
        GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int day = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        /*
         * G-Airmet 3 hour issue
         */
        hour = ((int)(hour / 3)) * 3;
        now.set(Calendar.HOUR_OF_DAY, hour);

        String airmetString = "";
        String dates[] = new String[4];
        for(int i = 0; i < 4; i++) {
            dates[i] = String.format("%04d%02d%02d%02d00", year , month , day, hour);
            airmetString += "<option value='" + dates[i] + "'>" + dates[i] + "</option><br>\n";
            now.add(Calendar.HOUR_OF_DAY, 3);
            hour = now.get(Calendar.HOUR_OF_DAY);
            day = now.get(Calendar.DAY_OF_MONTH);
            month = now.get(Calendar.MONTH) + 1;
            year = now.get(Calendar.YEAR);
        }
        
        /*
         * Generate the page
         */
        String data = "<html>\n"
                /*
                 * Title
                 */
                + "<head><meta content='text/html; charset=ISO-8859-1' http-equiv='content-type'>\n"
                + "<title>Weather</title>\n"
                /*
                 * Javascript functions
                 */
                + "<script>\n"
                + "function zeroPad(num) {\n"
                + "var out = num.toString();\n"
                + "if(out.length == 2) {return out;} else {return '0' + out;}\n"
                + "}\n"
                /*
                 * winds
                 */
                + "function winds()\n"
                + "{\n"
                + "var list=document.getElementById('windlist');\n"
                + "var listfc=document.getElementById('windlistforecast');\n"
                + "var gif='http://aviationweather.gov/adds/data/winds/ruc' + listfc.options[listfc.selectedIndex].value + 'hr_' + list.options[list.selectedIndex].value + '_wind.gif';\n"
                + "document.getElementById('windimage').src=gif;\n"
                + "}\n"
                /*
                 * Refresh
                 */
                + "function refresh()\n"
                + "{\n"
                + "location.reload(true);\n"
                + "}\n"
                /*
                 * Time settext
                 */
                + "function settext()\n"
                + "{\n"
                + "var img=document.getElementById('planimg');\n"
                + "img.style.visibility = 'hidden';\n"
                + "var txt=document.getElementById('timetxt');\n"
                + "var now = new Date();"
                + "var utc =  (now.getUTCMonth()) + 1 + '/' + now.getUTCDate() + ' ' + zeroPad(now.getUTCHours()) + '' + zeroPad(now.getUTCMinutes()) + ' UTC';\n"
                + "txt.value=utc;\n"
                + "var list=document.getElementById('plans');\n"
                + "list.options.length=0;\n"
                + "var pla=Android.getPlans();\n"
                + "list.innerHTML=pla;"
                + "}\n"
                /*
                 * LL weather prog
                 */
                + "function sigllload()\n"
                + "{\n"
                + "var list=document.getElementById('sigll');\n"
                + "var gif='http://aviationweather.gov/data/products/swl/ll_' + list.options[list.selectedIndex].value + '_4_cl_new.gif';\n"
                + "document.getElementById('llimg').src=gif;\n"
                + "}\n"
                + "function airmets()\n"
                + "{\n"
                + "var list=document.getElementById('gairmet');\n"
                + "var gift='http://aviationweather.gov/data/products/gairmet/combined/' + list.options[list.selectedIndex].value + '_us_TANGO.gif';\n"
                + "var gifs='http://aviationweather.gov/data/products/gairmet/combined/' + list.options[list.selectedIndex].value + '_us_SIERRA.gif'\n;"
                + "var gifi='http://aviationweather.gov/data/products/gairmet/combined/' + list.options[list.selectedIndex].value + '_us_ICE.gif';\n"
                + "var giff='http://aviationweather.gov/data/products/gairmet/combined/' + list.options[list.selectedIndex].value + '_us_FZLVL.gif';\n"
                + "document.getElementById('tango').src=gift;\n"
                + "document.getElementById('sierra').src=gifs;\n"
                + "document.getElementById('ice').src=gifi;\n"
                + "document.getElementById('fzlvl').src=giff;\n"
                + "}\n"
                /*
                 * This function calls back in Android to get METARs for a plan
                 */
                + "function getMETARs() {\n"
                + "var list=document.getElementById('plans');\n"
                + "var plan=list.options[list.selectedIndex].value;\n"
                + "}\n"
                + "function getTAFs() {\n"
                + "var list=document.getElementById('plans');\n"
                + "var plan=list.options[list.selectedIndex].value;\n"
                + "}\n"
                + "function getPIREPS() {\n"
                + "var list=document.getElementById('plans');\n"
                + "var plan=list.options[list.selectedIndex].value;\n"
                + "}\n"
                + "function getData() {\n"
                + "var img=document.getElementById('planimg');\n"
                + "img.style.visibility = 'visible';\n"
                + "var list=document.getElementById('plans');\n"
                + "var plan=list.options[list.selectedIndex].value;\n"
                + "Android.getWeather(plan);\n"
                + "}\n"
                /*
                 * This function is called by Android async task, when done
                 */
                + "function updateData() {\n"
                + "var table=document.getElementById('pireptable');\n"
                + "table.innerHTML=Android.getPIREPS();\n"
                + "var table=document.getElementById('taftable');\n"
                + "table.innerHTML=Android.getTAFs();\n"
                + "var table=document.getElementById('metartable');\n"
                + "table.innerHTML=Android.getMETARs();\n"
                + "var img=document.getElementById('planimg');\n"
                + "img.style.visibility = 'hidden';\n"
                + "}\n"
                /*
                 * On start set time
                 */
                + "window.onload = settext;\n"
                + "</script>\n"
                /*
                 * HTML
                 */
                + "</head>\n"
                + "<body>\n"
                + "<input type='text' id='timetxt' readonly>"
                + "<button type='button' id='refreshbutton' onClick='refresh()' >Update</button>"
                /*
                 * Plan
                 */
                + "<h1>Plan Area</h1>\n"
                + "<select id='plans' onChange='getData()'>\n"
                + "</select><br>\n"
                + "<img id='planimg' src='data:image/gif;base64,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' alt='Loading...' />"
                + "<h3>METARs</h3>\n"
                + "<form id='metartable' readonly></form>\n"
                + "<h3>TAFs</h3>\n"
                + "<form id='taftable' readonly></form>\n"
                + "<h3>PIREPs</h3>\n"
                + "<form id='pireptable' readonly></form>\n"
                /*
                 * Images
                 */
                + "<h1>Images</h1><br>\n"
                /*
                 * Main
                 */
                + "<h2>Main</h2>\n"
                + "<img src='http://aviationweather.gov/data/front/front_page_2color.gif'><br>\n"
                /*
                 * Conus radar
                 */
                + "<h2>Radar Loop</h2>\n"
                + "<img src='http://radar.weather.gov/Conus/Loop/NatLoop_Small.gif'><br>\n"
                /*
                 * Winds
                 */
                + "<h2>Winds</h2>\n"
                + "<select id='windlist' onChange='winds()'>\n"
                + "<option value='sfc'>sfc</option>\n"
                + "<option value='900'>3000</option>\n"
                + "<option value='800'>6000</option>\n"
                + "<option value='725'>9000</option>\n"
                + "<option value='650'>12000</option>\n"
                + "<option value='575'>15000</option>\n"
                + "<option value='500'>18000</option>\n"
                + "<option value='400'>24000</option>\n"
                + "<option value='300'>30000</option>\n"
                + "<option value='225'>36000</option>\n"
                + "</select>\n"
                + "<select id='windlistforecast' onChange='winds()'>\n"
                + "<option value='00'>now</option>\n"
                + "<option value='01'>1 hour later</option>\n"
                + "<option value='02'>2 hour later</option>\n"
                + "<option value='03'>3 hour later</option>\n"
                + "<option value='04'>4 hour later</option>\n"
                + "<option value='05'>5 hour later</option>\n"
                + "<option value='06'>6 hour later</option>\n"
                + "<option value='07'>7 hour later</option>\n"
                + "<option value='08'>8 hour later</option>\n"
                + "<option value='09'>9 hour later</option>\n"
                + "<option value='10'>10 hour later</option>\n"
                + "<option value='11'>11 hour later</option>\n"
                + "<option value='12'>12 hour later</option>\n"
                + "<option value='13'>13 hour later</option>\n"
                + "<option value='14'>14 hour later</option>\n"
                + "<option value='15'>15 hour later</option>\n"
                + "<option value='16'>16 hour later</option>\n"
                + "</select><br>\n"
                + "<img id='windimage' src='http://aviationweather.gov/adds/data/winds/ruc00hr_sfc_wind.gif'><br>\n"
                /*
                 * Sigmet
                 */
                + "<h2>Sigmets</h2>\n"
                + "<img src='http://aviationweather.gov/adds/data/airmets/airmets_ALL.gif'><br>\n"
                /*
                 * Airmet
                 */
                + "<h2>Airmets</h2>\n"
                + "<select id='gairmet' onChange='airmets()'>\n"
                + airmetString
                + "</select><br>\n"
                + "<h3>Tango</h3>\n"
                + "<img id='tango' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_TANGO.gif'><br>\n"
                + "<h3>Sierra</h3>\n"
                + "<img id='sierra' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_SIERRA.gif'><br>\n"
                + "<h3>Icing</h3>\n"
                + "<img id='ice' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_ICE.gif'><br>\n"
                + "<h3>Freezing Level</h3>\n"
                + "<img id='fzlvl' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_FZLVL.gif'><br>\n"
                /*
                 * Weather prognostic
                 */
                + "<h2>Low Level Prognostic (Surface-24000)</h2>\n"
                + "<select id='sigll' onChange='sigllload()'>\n"
                + "<option value='12'>Valid at 0000 and 1200 UTC</option>\n"
                + "<option value='18'>Valid at 0600 and 1800 UTC</option>\n"
                + "<option value='00'>Valid at 1200 and 0000 UTC</option>\n"
                + "<option value='06'>Valid at 1800 and 0600 UTC</option>\n"
                + "</select><br>\n"
                + "<img id='llimg' src='http://aviationweather.gov/data/products/swl/ll_12_4_cl_new.gif'><br>\n"
                + "</body>\n"
                + "</html>\n";
        return data;

    }
    
}
