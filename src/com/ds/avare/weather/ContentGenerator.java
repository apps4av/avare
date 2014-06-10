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
                + "<select onchange='weatherImg(this.value)'>"
                + "<option value=''>Select a weather product</option>"
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
                + "<img id='weatherImage'>"
                + "</body>\n"
                + "</html>\n";
        return data;

    }
    
}
