/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.weather;


import android.content.Context;

import com.ds.avare.StorageService;

public class ContentGenerator {

    public static String makeContentImage(Context context, StorageService service) {       
        
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
                 * Refresh
                 */
                + "function refresh()\n"
                + "{\n"
                + "location.reload(true);\n"
                + "}\n"
                /*
                 * FInd image WInds
                 */
                + "function getImageWinds(type,level,ftime){"
                + "var elem = document.getElementById('weatherImage');"
                + "if( level == '030' ) level = '900';"
                + "else if( level == '060' ) level = '800';"
                + "else if( level == '090' ) level = '725';"
                + "else if( level == '120' ) level = '650';"
                + "else if( level == '150' ) level = '575';"
                + "else if( level == '180' ) level = '500';"
                + "else if( level == '240' ) level = '400';"
                + "else if( level == '300' ) level = '300';"
                + "else if( level == '360' ) level = '225';"
                + "else if( level == '420' ) level = '175';"
                + "else if( level == '480' ) level = '125';"
                + "var filename = 'http://aviationweather.gov/adds/data/winds/'+ftime+'_'+level+'_'+type+'.gif';"
                + "elem.src = filename;"
                + "}"
                + "function updateImageWinds(){"
                + "var telem = document.getElementById('type');"
                + "var type = telem.options[telem.selectedIndex].value;"
                + "var lelem = document.getElementById('level');"
                + "var level = lelem.options[lelem.selectedIndex].value;"
                + "var felem = document.getElementById('fore');"
                + "var ftime = felem.options[felem.selectedIndex].value;"
                + "getImageWinds( type, level, ftime );"
                + "}"
                
                /*
                 * Turbulence image
                 */
                + "function getImageTurb(level,ftime){"
                + "var elem = document.getElementById('weatherImage');"
                + "var filename = 'http://aviationweather.gov/adds/data/turbulence/'+ftime+'_gtg_'+level+'.gif';"
                + "elem.src = filename;"
                + "}"
                + "function updateImageTurb(){"
                + "var lelem = document.getElementById('levelTurb');"
                + "var level = lelem.options[lelem.selectedIndex].value;"
                + "var felem = document.getElementById('foreTurb');"
                + "var fore = felem.options[felem.selectedIndex].value;" 
                + "getImageTurb( level, fore );"
                + "}"

                /*
                 * Icing image
                 */
                + "function getImageIce(type,level,ftime){"
                + "var elem = document.getElementById('weatherImage');"
                + "var filename = 'http://aviationweather.gov/adds/data/icing/'+ftime+'_'+type+'_'+level+'.gif';"
                + "elem.src = filename;"
                + "}"
                + "function updateImageIce(){"
                + "var lelem = document.getElementById('levelIce');"
                + "var level = lelem.options[lelem.selectedIndex].value;"
                + "var felem = document.getElementById('foreIce');"
                + "var fore = felem.options[felem.selectedIndex].value;" 
                + "var telem = document.getElementById('typeIce');"
                + "var type = telem.options[telem.selectedIndex].value;" 
                + "if( fore == '00' ) type = 'cip' + type; else type = 'fip' + type;"
                + "getImageIce(type, level, fore );"
                + "}"

                /*
                 * Load image
                 */
                + "function weatherImg(type) {\n"
                + "var img = document.getElementById('weatherImage');"
                + "if(type == '') {"
                + "img.src = '';"
                + "}"
                + "else if(type == 'WSA') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_sfc_analysis.gif';"
                + "}"
                + "else if(type == 'W12P') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_12_fcst.gif';"
                + "}"
                + "else if(type == 'W24P') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_24_fcst.gif';"
                + "}"
                + "else if(type == 'W36P') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_36_fcst.gif';"
                + "}"
                + "else if(type == 'W48P') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_48_fcst.gif';"
                + "}"
                + "else if(type == 'W3DP') {"
                + "img.src = 'http://aviationweather.gov/adds/data/progs/hpc_mid_072.gif';"
                + "}"
                + "else if(type == 'LLS00') {"
                + "img.src = 'http://aviationweather.gov/data/products/swl/ll_00_4_cl_new.gif';"
                + "}"
                + "else if(type == 'LLS06') {"
                + "img.src = 'http://aviationweather.gov/data/products/swl/ll_06_4_cl_new.gif';"
                + "}"
                + "else if(type == 'LLS12') {"
                + "img.src = 'http://aviationweather.gov/data/products/swl/ll_12_4_cl_new.gif';"
                + "}"
                + "else if(type == 'LLS18') {"
                + "img.src = 'http://aviationweather.gov/data/products/swl/ll_18_4_cl_new.gif';"
                + "}"
                + "else if(type == 'MLS00') {"
                + "img.src = 'http://aviationweather.gov/data/products/swm/PGNE14_00_CL.gif';"
                + "}"
                + "else if(type == 'MLS06') {"
                + "img.src = 'http://aviationweather.gov/data/products/swm/PGNE14_06_CL.gif';"
                + "}"
                + "else if(type == 'MLS12') {"
                + "img.src = 'http://aviationweather.gov/data/products/swm/PGNE14_12_CL.gif';"
                + "}"
                + "else if(type == 'MLS18') {"
                + "img.src = 'http://aviationweather.gov/data/products/swm/PGNE14_18_CL.gif';"
                + "}"
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
                + "getData();"
                + "}\n"
                /*
                 * This function calls back in Android to get METARs for a plan
                 */
                + "function getData() {\n"
                + "var img=document.getElementById('planimg');\n"
                + "img.style.visibility = 'visible';\n"
                + "Android.getWeather();\n"
                + "}\n"
                /*
                 * This function is called by Android async task, when done
                 */
                + "function updateData(weather) {\n"
                + "var table = document.getElementById('plantable');\n"
                + "table.innerHTML = weather;\n"
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
                + "<h1>Charts</h1>\n"
                + "<label>Prog.</label><br>\n"
                + "<select onchange='weatherImg(this.value)'>"
                + "<option value='WSA'>WPC Surface Analysis</option>"
                + "<option value='W12P'>WPC 12 HR Prognostic</option>"
                + "<option value='W24P'>WPC 24 HR Prognostic</option>"
                + "<option value='W36P'>WPC 36 HR Prognostic</option>"
                + "<option value='W48P'>WPC 48 HR Prognostic</option>"
                + "<option value='W3DP'>WPC 3 day Prognostic</option>"
                + "<option value='LLS00'>Low Level Significant 0000Z</option>"
                + "<option value='LLS06'>Low Level Significant 0600Z</option>"
                + "<option value='LLS12'>Low Level Significant 1200Z</option>"
                + "<option value='LLS18'>Low Level Significant 1800Z</option>"
                + "<option value='MLS00'>Mid Level Significant 0000Z</option>"
                + "<option value='MLS06'>Mid Level Significant 0600Z</option>"
                + "<option value='MLS12'>Mid Level Significant 1200Z</option>"
                + "<option value='MLS18'>Mid Level Significant 1800Z</option>"
                + "</select><br>" 
                + "<label>Winds/Temp.</label><br>\n"
                + "<select id='type' name='type' onChange='updateImageWinds()'>"
                + "<OPTION VALUE='wind'>Wind Speed</OPTION>"
                + "<OPTION VALUE='temp'>Temperature</OPTION>"
                + "<OPTION VALUE='isa'>Temp. Difference</OPTION>"
                + "<OPTION VALUE='windstrm'>Streamlines</OPTION>"
                + "</select>"
                + "<select id='level' name='level' onChange='updateImageWinds()'>"
                + "<OPTION VALUE='480'>FL480</OPTION>"
                + "<OPTION VALUE='420'>FL420</OPTION>"
                + "<OPTION VALUE='360'>FL360</OPTION>"
                + "<OPTION VALUE='300'>FL300</OPTION>"
                + "<OPTION VALUE='240'>FL240</OPTION>" 
                + "<OPTION VALUE='180'>FL180</OPTION>"
                + "<OPTION VALUE='150'>15,000</OPTION>"
                + "<OPTION VALUE='120'>12,000</OPTION>"
                + "<OPTION VALUE='090'>9,000</OPTION>"
                + "<OPTION VALUE='060'>6,000</OPTION>"
                + "<OPTION VALUE='030'>3,000</OPTION>"
                + "<OPTION VALUE='sfc'>sfc</OPTION>"
                + "</select>"
                + "<select id='fore' name='fore' onChange='updateImageWinds()'>"
                + "<option value='ruc00hr'>0hr</option>"
                + "<option value='ruc01hr'>1hr</option>"
                + "<option value='ruc02hr'>2hr</option>"
                + "<option value='ruc03hr'>3hr</option>"
                + "<option value='ruc04hr'>4hr</option>"
                + "<option value='ruc05hr'>5hr</option>"
                + "<option value='ruc06hr'>6hr</option>"
                + "<option value='ruc07hr'>7hr</option>"
                + "<option value='ruc08hr'>8hr</option>"
                + "<option value='ruc09hr'>9hr</option>"
                + "<option value='ruc10hr'>10hr</option>"
                + "<option value='ruc11hr'>11hr</option>"
                + "<option value='ruc12hr'>12hr</option>"
                + "<option value='ruc13hr'>13hr</option>"
                + "<option value='ruc14hr'>14hr</option>"
                + "<option value='ruc15hr'>15hr</option>"
                + "<option value='ruc16hr'>16hr</option>"
                + "<option value='ruc17hr'>17hr</option>"
                + "<option value='ruc18hr'>18hr</option>"
                + "</select><br>"
                + "<label>Turbulence</label><br>\n"
                + "<select id='levelTurb' name='levelTurb' onChange='updateImageTurb()'>"
                + "<OPTION VALUE='450'>FL450</OPTION>"
                + "<OPTION VALUE='430'>FL430</OPTION>"
                + "<OPTION VALUE='410'>FL410</OPTION>"
                + "<OPTION VALUE='390'>FL390</OPTION>"
                + "<OPTION VALUE='370'>FL370</OPTION>"
                + "<OPTION VALUE='350'>FL350</OPTION>"
                + "<OPTION VALUE='330'>FL330</OPTION>"
                + "<OPTION VALUE='310'>FL310</OPTION>"
                + "<OPTION VALUE='290'>FL290</OPTION>"
                + "<OPTION VALUE='270'>FL270</OPTION>"
                + "<OPTION VALUE='250'>FL250</OPTION>"
                + "<OPTION VALUE='230'>FL230</OPTION>"
                + "<OPTION VALUE='210'>FL210</OPTION>"
                + "<OPTION VALUE='190'>FL190</OPTION>"
                + "<OPTION VALUE='170'>17,000</OPTION>"
                + "<OPTION VALUE='150'>15,000</OPTION>"
                + "<OPTION VALUE='130'>13,000</OPTION>"
                + "<OPTION VALUE='110'>11,000</OPTION>"
                + "<OPTION VALUE='max'>max</OPTION>"
                + "</select>"
                + "<select id='foreTurb' name='foreTurb' onChange='updateImageTurb()'>"
                + "<option value='00'>0hr</option>"
                + "<option value='01'>1hr</option>"
                + "<option value='02'>2hr</option>"
                + "<option value='03'>3hr</option>"
                + "<option value='06'>6hr</option>"
                + "<option value='09'>9hr</option>"
                + "<option value='12'>12hr</option>"
                + "</select><br>"
                + "<label>Icing</label><br>\n"
                + "<select id='typeIce' name='typeIce' onChange='updateImageIce()'>"
                + "<OPTION VALUE='sev'>Severity</OPTION>"
                + "<OPTION VALUE='sev25'>Severity(Prob>25%)</OPTION>"
                + "<OPTION VALUE='sev50'>Severity(Prob>50%)</OPTION>"
                + "<OPTION VALUE=''>Probability</OPTION>"
                + "</select>"
                + "<select id='levelIce' name='levelIce' onChange='updateImageIce()'>"
                + "<OPTION VALUE='290'>FL290</OPTION>"
                + "<OPTION VALUE='270'>FL270</OPTION>"
                + "<OPTION VALUE='250'>FL250</OPTION>"
                + "<OPTION VALUE='230'>FL230</OPTION>"
                + "<OPTION VALUE='210'>FL210</OPTION>"
                + "<OPTION VALUE='190'>FL190</OPTION>"
                + "<OPTION VALUE='170'>17,000</OPTION>"
                + "<OPTION VALUE='150'>15,000</OPTION>"
                + "<OPTION VALUE='130'>13,000</OPTION>"
                + "<OPTION VALUE='110'>11,000</OPTION>"
                + "<OPTION VALUE='090'>9,000</OPTION>"
                + "<OPTION VALUE='070'>7,000</OPTION>"
                + "<OPTION VALUE='050'>5,000</OPTION>"
                + "<OPTION VALUE='030'>3,000</OPTION>"
                + "<OPTION VALUE='010'>1,000</OPTION>"
                + "<OPTION VALUE='max'>max</OPTION>"
                + "</select>"
                + "<select id='foreIce' name='foreIce' onChange='updateImageIce()'>"
                + "<option value='00'>0hr</option>"
                + "<option value='02'>2hr</option>"
                + "<option value='03'>3hr</option>"
                + "<option value='06'>6hr</option>"
                + "<option value='09'>9hr</option>"
                + "<option value='12'>12hr</option>"
                + "</select><br>"
                + "<img id='weatherImage' src='http://aviationweather.gov/adds/data/progs/hpc_sfc_analysis.gif'><br>"
                + "<h1>Plan Area</h1>\n"
                + "<input type='text' id='timetxt' readonly>"
                + "<button type='button' id='refreshbutton' onClick='refresh()' >Update</button>"
                /*
                 * Plan
                 */
                + "<img id='planimg' src='data:image/gif;base64,R0lGODlhEAAQAPQAAP///wAAAPDw8IqKiuDg4EZGRnp6e"
                + "gAAAFhYWCQkJKysrL6+vhQUFJycnAQEBDY2NmhoaAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAA"
                + "AAAEAAQAAAFdyAgAgIJIeWoAkRCCMdBkKtIHIngyMKsErPBYbADpkSCwhDmQCBethRB6Vj4kFCkQPG4IlWDgrNRIwn"
                + "O4UKBXDufzQvDMaoSDBgFb886MiQadgNABAokfCwzBA8LCg0Egl8jAggGAA1kBIA1BAYzlyILczULC2UhACH5BAkK"
                + "AAAALAAAAAAQABAAAAV2ICACAmlAZTmOREEIyUEQjLKKxPHADhEvqxlgcGgkGI1DYSVAIAWMx+lwSKkICJ0QsHi9RgKB"
                + "wnVTiRQQgwF4I4UFDQQEwi6/3YSGWRRmjhEETAJfIgMFCnAKM0KDV4EEEAQLiF18TAYNXDaSe3x6mjidN1s3IQAh+QQ"
                + "JCgAAACwAAAAAEAAQAAAFeCAgAgLZDGU5jgRECEUiCI+yioSDwDJyLKsXoHFQxBSHAoAAFBhqtMJg8DgQBgfrEsJAE"
                + "Ag4YhZIEiwgKtHiMBgtpg3wbUZXGO7kOb1MUKRFMysCChAoggJCIg0GC2aNe4gqQldfL4l/Ag1AXySJgn5LcoE3QX"
                + "I3IQAh+QQJCgAAACwAAAAAEAAQAAAFdiAgAgLZNGU5joQhCEjxIssqEo8bC9BRjy9Ag7GILQ4QEoE0gBAEBcOpcBA0"
                + "DoxSK/e8LRIHn+i1cK0IyKdg0VAoljYIg+GgnRrwVS/8IAkICyosBIQpBAMoKy9dImxPhS+GKkFrkX+TigtLlIyKXU"
                + "F+NjagNiEAIfkECQoAAAAsAAAAABAAEAAABWwgIAICaRhlOY4EIgjH8R7LKhKHGwsMvb4AAy3WODBIBBKCsYA9TjuhD"
                + "NDKEVSERezQEL0WrhXucRUQGuik7bFlngzqVW9LMl9XWvLdjFaJtDFqZ1cEZUB0dUgvL3dgP4WJZn4jkomWNpSTIyE"
                + "AIfkECQoAAAAsAAAAABAAEAAABX4gIAICuSxlOY6CIgiD8RrEKgqGOwxwUrMlAoSwIzAGpJpgoSDAGifDY5kopBYD"
                + "lEpAQBwevxfBtRIUGi8xwWkDNBCIwmC9Vq0aiQQDQuK+VgQPDXV9hCJjBwcFYU5pLwwHXQcMKSmNLQcIAExlbH8"
                + "JBwttaX0ABAcNbWVbKyEAIfkECQoAAAAsAAAAABAAEAAABXkgIAICSRBlOY7CIghN8zbEKsKoIjdFzZaEgUBHKChMJtR"
                + "wcWpAWoWnifm6ESAMhO8lQK0EEAV3rFopIBCEcGwDKAqPh4HUrY4ICHH1dSoTFgcHUiZjBhAJB2AHDykpKAwHAwdzf1"
                + "9KkASIPl9cDgcnDkdtNwiMJCshACH5BAkKAAAALAAAAAAQABAAAAV3ICACAkkQZTmOAiosiyAoxCq+KPxCNVsSMRgB"
                + "siClWrLTSWFoIQZHl6pleBh6suxKMIhlvzbAwkBWfFWrBQTxNLq2RG2yhSUkDs2b63AYDAoJXAcFRwADeAkJDX"
                + "0AQCsEfAQMDAIPBz0rCgcxky0JRWE1AmwpKyEAIfkECQoAAAAsAAAAABAAEAAABXkgIAICKZzkqJ4nQZxLqZKv4N"
                + "qNLKK2/Q4Ek4lFXChsg5ypJjs1II3gEDUSRInEGYAw6B6zM4JhrDAtEosVkLUtHA7RHaHAGJQEjsODcEg0FBAF"
                + "VgkQJQ1pAwcDDw8KcFtSInwJAowCCA6RIwqZAgkPNgVpWndjdyohACH5BAkKAAAALAAAAAAQABAAAAV5ICACAimc"
                + "5KieLEuUKvm2xAKLqDCfC2GaO9eL0LABWTiBYmA06W6kHgvCqEJiAIJiu3gcvgUsscHUERm+kaCxyxa+zRPk0SgJ"
                + "EgfIvbAdIAQLCAYlCj4DBw0IBQsMCjIqBAcPAooCBg9pKgsJLwUFOhCZKyQDA3YqIQAh+QQJCgAAACwAAAAAEAAQ"
                + "AAAFdSAgAgIpnOSonmxbqiThCrJKEHFbo8JxDDOZYFFb+A41E4H4OhkOipXwBElYITDAckFEOBgMQ3arkMkUBdxIU"
                + "GZpEb7kaQBRlASPg0FQQHAbEEMGDSVEAA1QBhAED1E0NgwFAooCDWljaQIQCE5qMHcNhCkjIQAh+QQJCgAAACwAA"
                + "AAAEAAQAAAFeSAgAgIpnOSoLgxxvqgKLEcCC65KEAByKK8cSpA4DAiHQ/DkKhGKh4ZCtCyZGo6F6iYYPAqFgYy02x"
                + "kSaLEMV34tELyRYNEsCQyHlvWkGCzsPgMCEAY7Cg04Uk48LAsDhRA8MVQPEF0GAgqYYwSRlycNcWskCkApIyEAOwA"
                + "AAAAAAAAAAA==' alt='Loading...' />\n"
                + "<form id='plantable' readonly></form>\n"
                + "</body>\n"
                + "</html>\n";
        return data;

    }
    
}
