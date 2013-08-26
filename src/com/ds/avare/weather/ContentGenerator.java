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

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;

import com.ds.avare.utils.NetworkHelper;

public class ContentGenerator {

    public static String makeContentImage() {
        
        /*
         * Download the airmet time file
         */
        String airmetString = "";
        String dates[] = new String[1];
        try {
            URL url = new URL(NetworkHelper.getAirmetTimesURL());

            URLConnection connection = url.openConnection();
            connection.connect();
            BufferedInputStream input = new BufferedInputStream(
                    url.openStream(), 4096);
            byte airmet[] = new byte[4096];
            input.read(airmet, 0, 4096);
            input.close();
            String a = new String(airmet);
            dates = a.split("\n");
            for (int i = 0; i < dates.length; i++) {
                if (!dates[i].matches("[0-9]{12}")) {
                    continue;
                }
                /*
                 * Make image names from timestamp
                 */
                airmetString += "<option value='" + dates[i] + "'>" + dates[i]
                        + "</option><br>\n";
            }
        } 
        catch (Exception e) {
            return "";
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
                + "function zeroPad(num, places) {\n"
                + "var zero = places - num.toString().length + 1;\n"
                + "return Array(+(zero > 0 && zero)).join(\"0\") + num;\n"
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
                + "var txt=document.getElementById('timetxt');\n"
                + "var now = new Date();"
                + "var utc =  (now.getUTCMonth()) + 1 + '/' + now.getUTCDate() + ' ' + zeroPad(now.getUTCHours()) + '' + zeroPad(now.getUTCMinutes()) + ' UTC';\n"
                + "txt.value=utc;\n"
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
                 * On start set time
                 */
                + "window.onload = settext;"
                + "</script>\n"
                /*
                 * HTML
                 */
                + "</head>\n"
                + "<body>\n"
                + "<input type='text' id='timetxt' readonly><br>"
                + "<button type='button' id='refreshbutton' onClick='refresh()' >Update</button>"
                /*
                 * Plan
                 */
                + "<h1>Plan</h1>Coming soon<br>\n"
                /*
                 * Images
                 */
                + "<h1>Images</h1><br>\n"
                /*
                 * Main
                 */
                + "<h2>Main</h2><br>\n"
                + "<img src='http://aviationweather.gov/data/front/front_page_2color.gif'><br>\n"
                /*
                 * Conus radar
                 */
                + "<h2>Radar Loop</h2><br>\n"
                + "<img src='http://radar.weather.gov/Conus/Loop/NatLoop_Small.gif'><br>\n"
                /*
                 * Winds
                 */
                + "<h2>Winds</h2><br>\n"
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
                + "<h2>Sigmets</h2><br>\n"
                + "<img src='http://aviationweather.gov/adds/data/airmets/airmets_ALL.gif'><br>\n"
                /*
                 * Airmet
                 */
                + "<h2>Airmets</h2><br>\n"
                + "<select id='gairmet' onChange='airmets()'>\n"
                + airmetString
                + "</select><br>\n"
                + "<h3>Tango</h3><br>\n"
                + "<img id='tango' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_TANGO.gif'><br>\n"
                + "<h3>Sierra</h3><br>\n"
                + "<img id='sierra' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_SIERRA.gif'><br>\n"
                + "<h3>Icing</h3><br>\n"
                + "<img id='ice' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_ICE.gif'><br>\n"
                + "<h3>Freezing Level</h3><br>\n"
                + "<img id='fzlvl' src='http://aviationweather.gov/data/products/gairmet/combined/" + dates[0] + "_us_FZLVL.gif'><br>\n"
                /*
                 * Weather prognostic
                 */
                + "<h2>Low Level Prognostic (Surface-24000)</h2><br>\n"
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
