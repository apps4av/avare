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

import java.util.Locale;

import com.ds.avare.R;


public class WeatherHelper {
    
    private static int BLUE = 0xFF0000BF;
    private static int RED = 0xFFBF0000;
    private static int GREEN = 0xFF00BF00;
    private static int MAGENTA = 0xFFBF00BF;
    private static int WHITE = 0xFFFFFFFF;
    
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
     * @param TAF
     * @return
     */
    public static String metarColorString(String type) {
        if(type.equals("VFR")) {
            return("green");
        }
        else if(type.equals("IFR")) {
            return("red");                        
        }
        else if(type.equals("MVFR")) {
            return("blue");                        
        }
        else if(type.equals("LIFR")) {
            return("magenta");                       
        }
        return("black");
    }
    
    /**
     * 
     * @param weather
     * @return
     */
    public static String formatWeather(String weather) {
        weather = weather.replace("TAF ", "");
        weather = weather.replace("AMD ", "");
        weather = weather.replace("\n\n", "\n");
        weather = weather.replace(" FM", "\nFM");
        weather = weather.replace("BECMG", "\nBECMG"); 
        return weather;
    }
    
    /**
     * 
     * @param weather
     * @return
     */
    public static String formatWeatherHTML(String weather, boolean translate) {
        weather = weather.replace("TAF ", "");
        weather = weather.replace("AMD ", "");
        weather = weather.replace("\n", "<br>");
        if(translate) {
            weather = weather.replace(" FM", "<br></br>FM(From)<br>");
            weather = weather.replace("BECMG", "<br></br>BECMG(Becoming)<br>");
        }
        else {
            weather = weather.replace(" FM", "<br></br>FM");
            weather = weather.replace("BECMG", "<br></br>BECMG");            
        }
        return weather;
    }

    /**
     * Color code weather type 
     * @param weather
     * @return
     */
    public static String formatTafHTML(String weatherAll, boolean translate) {

        String strip[] = weatherAll.split("RMK");
        String weather = strip[0];

        /*
         * Qualifiers
         */
        weather = weather.replaceAll("\\+", "<font color='magenta'+>" + (translate ? "(Heavy)" : "") + "<font color='black'>");
        weather = weather.replaceAll("\\-", "<font color='red'>-" + (translate ? "(Light)" : "") + "<font color='black'>");
        
        /*
         * Description
         */
        weather = weather.replaceAll("MI", "<font color='blue'>MI" + (translate ? "(Shallow)" : "") + "<font color='black'>");
        weather = weather.replaceAll("BC", "<font color='blue'>BC" + (translate ? "(Patches)" : "") + "<font color='black'>");
        weather = weather.replaceAll("DR", "<font color='blue'>DR" + (translate ? "(Low Drifting)" : "") + "<font color='black'>");
        weather = weather.replaceAll("BL", "<font color='blue'>BL" + (translate ? "(BLowing)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SH", "<font color='blue'>SH" + (translate ? "(Showers)" : "") + "<font color='black'>");
        weather = weather.replaceAll("TS", "<font color='red'>TS" + (translate ? "(Thunderstorm)" : "") + "<font color='black'>");
        weather = weather.replaceAll("FZ", "<font color='red'>FZ" + (translate ? "(Freezing)" : "") + "<font color='black'>");
        weather = weather.replaceAll("PR", "<font color='blue'>PR" + (translate ? "(Partial)" : "") + "<font color='black'>");
        weather = weather.replaceAll("AMD", "AMD" + (translate ? "(Amended)" : ""));
        weather = weather.replaceAll("WSCONDS", "WSCONDS" + (translate ? "(Wind Shear Possible)" : ""));
        
        /*
         * Precip
         */
        weather = weather.replaceAll("DZ", "<font color='blue'>DZ" + (translate ? "(Drizzle)" : "") + "<font color='black'>");
        weather = weather.replaceAll("RA", "<font color='red'>RA" + (translate ? "(Rain)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SN", "<font color='red'>SN" + (translate ? "(Snow)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SG", "<font color='red'>SG" + (translate ? "(Snow Grains)" : "") + "<font color='black'>");
        weather = weather.replaceAll("IC", "<font color='red'>IC" + (translate ? "(Ice Crystals)" : "") + "<font color='black'>");
        weather = weather.replaceAll("PL", "<font color='red'>PL" + (translate ? "(Ice Pellets)" : "") + "<font color='black'>");
        weather = weather.replaceAll("GR", "<font color='red'>GR" + (translate ? "(Hail)" : "") + "<font color='black'>");
        weather = weather.replaceAll("GS", "<font color='blue'>GS" + (translate ? "(Small Hail)" : "") + "<font color='black'>");
        weather = weather.replaceAll("UP", "<font color='red'>UP" + (translate ? "(Unknown Precip.)" : "") + "<font color='black'>");

        /*
         * Obstruction
         */
        weather = weather.replaceAll("BR", "<font color='red'>BR" + (translate ? "(Mist)" : "") + "<font color='black'>");
        weather = weather.replaceAll("FG", "<font color='red'>FG" + (translate ? "(Fog)" : "") + "<font color='black'>");
        weather = weather.replaceAll("FU", "<font color='red'>FU" + (translate ? "(Smoke)" : "") + "<font color='black'>");
        weather = weather.replaceAll("DU", "<font color='red'>DU" + (translate ? "(Dust)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SA", "<font color='red'>SA" + (translate ? "(Sand)" : "") + "<font color='black'>");
        weather = weather.replaceAll("HZ", "<font color='red'>HZ" + (translate ? "(Haze)" : "") + "<font color='black'>");
        weather = weather.replaceAll("PY", "<font color='red'>PY" + (translate ? "(Spray)" : "") + "<font color='black'>");
        weather = weather.replaceAll("VA", "<font color='red'>VA" + (translate ? "(Volcanic Ash)" : "") + "<font color='black'>");

        /*
         * Other
         */
        weather = weather.replaceAll("P0", "<font color='red'>P0" + (translate ? "(Dust Whirls)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SQ", "<font color='red'>SQ" + (translate ? "(Squalls)" : "") + "<font color='black'>");
        weather = weather.replaceAll("FC", "<font color='red'>FC" + (translate ? "(Funnel Cloud)" : "") + "<font color='black'>");
        weather = weather.replaceAll("SS", "<font color='red'>SS" + (translate ? "(Sand Storm)" : "") + "<font color='black'>");
        weather = weather.replaceAll("DS", "<font color='red'>DS" + (translate ? "(Dust Storm)" : "") + "<font color='black'>");
        weather = weather.replaceAll(" VC", "<font color='black'> VC" + (translate ? "(In Vicinity)" : "") + "<font color='black'>");

        weather = weather.replaceAll("SKC", "SKC" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("CLR", "CLR" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("BKN", "BKN" + (translate ? "(Broken)" : ""));
        weather = weather.replaceAll("SCT", "SCT" + (translate ? "(Scattered)" : ""));
        weather = weather.replaceAll("OVC", "OVC" + (translate ? "(Overcast)" : ""));
        weather = weather.replaceAll("PROB", "PROB" + (translate ? "(Probibility%)" : ""));
        weather = weather.replaceAll("VV", "<font color='red'>VV" + (translate ? "(Vertical Visibility)" : "") + "<font color='black'>");
        weather = weather.replaceAll("CB", "<font color='red'>CB" + (translate ? "(Cumulonimbus)" : "") + "<font color='black'>");
        weather = weather.replaceAll("WS", "<font color='magenta'>WS" + (translate ? "(Wind Shear)" : "") + "<font color='black'>");
        
        weather = weather.replaceAll(" 9999 ", " 9999" + (translate ? "(Visibility > 7SM) " : ""));
        weather = weather.replaceAll("QNH", "QNH" + (translate ? "(Minimum Altimeter)" : ""));
        weather = weather.replaceAll("INS", "INS" + (translate ? "(Inches)" : ""));

        for(int i = 1 ; i < strip.length; i++) {
            
            String weather1 = strip[i];
            
            weather += " RMK" + (translate ? "(Remark) " : " ") + weather1;
        }

        return weather;
    }

    /**
     * Color code winds
     * @param weather
     * @return
     */
    public static String formatMetarHTML(String weatherAll, boolean translate) {
        
        String strip[] = weatherAll.split("RMK");
        String weather = strip[0];
        
        /*
         * Remarks
         */
        weather = weather.replaceAll("\\+", "+" + (translate ? "(Heavy)" : ""));
        weather = weather.replaceAll("\\-", "-" + (translate ? "(Light)" : ""));
        weather = weather.replaceAll("IR", "IR" + (translate ? "(Runway Ice)" : ""));
        weather = weather.replaceAll("WR", "WR" + (translate ? "(Wet Runway)" : ""));
        weather = weather.replaceAll("LSR", "LSR" + (translate ? "(Loose Runway Snow)" : ""));
        weather = weather.replaceAll("PSR", "PSR" + (translate ? "(Packed Runway Snow)" : ""));
        weather = weather.replaceAll("LTG", "LTG" + (translate ? "(Lightning)" : ""));
        weather = weather.replaceAll("TCU","TCU" + (translate ? "(Towering Cumulus)" : ""));
        weather = weather.replaceAll("VRB", "VRB" + (translate ? "(Variable)" : ""));
        
        weather = weather.replaceAll("AUTO", "AUTO" + (translate ? "(Automated)" : ""));
        weather = weather.replaceAll("COR", "COR" + (translate ? "(Corrected)" : ""));
        weather = weather.replaceAll(" 9999 ", " 9999" + (translate ? "(Visibility > 7SM) " : ""));
        weather = weather.replaceAll("SPECI", "SPECI" + (translate ? "(Special)" : ""));
        
        /*
         * Description
         */
        weather = weather.replaceAll("MI", "MI" + (translate ? "(Shallow)" : ""));
        weather = weather.replaceAll("BC", "BC" + (translate ? "(Patches)" : ""));
        weather = weather.replaceAll("DR", "DR" + (translate ? "(Low Drifting)" : ""));
        weather = weather.replaceAll("BL", "BL" + (translate ? "(BLowing)" : ""));
        weather = weather.replaceAll("SH", "SH" + (translate ? "(Showers)" : ""));
        weather = weather.replaceAll("TS", "TS" + (translate ? "(Thunderstorm)" : ""));
        weather = weather.replaceAll("FZ", "FZ" + (translate ? "(Freezing)" : ""));
        weather = weather.replaceAll("PR", "PR" + (translate ? "(Partial)" : ""));
        /*
         * Precip
         */
        weather = weather.replaceAll("DZ", "DZ" + (translate ? "(Drizzle)" : ""));
        weather = weather.replaceAll("RA ", "RA " + (translate ? "(Rain)" : ""));
        weather = weather.replaceAll("SN", "SN" + (translate ? "(Snow)" : ""));
        weather = weather.replaceAll("SG", "SG" + (translate ? "(Snow Grains)" : ""));
        weather = weather.replaceAll("IC", "IC" + (translate ? "(Ice Crystals)" : ""));
        weather = weather.replaceAll("PL", "PL" + (translate ? "(Ice Pellets)" : ""));
        weather = weather.replaceAll("GR", "GR" + (translate ? "(Hail)" : ""));
        weather = weather.replaceAll("GS", "GS" + (translate ? "(Small Hail)" : ""));
        weather = weather.replaceAll("UP", "UP" + (translate ? "(Unknown Precip.)" : ""));

        /*
         * Obstruction
         */
        weather = weather.replaceAll("BR", "BR" + (translate ? "(Mist)" : ""));
        weather = weather.replaceAll("FG", "FG" + (translate ? "(Fog)" : ""));
        weather = weather.replaceAll("FU", "FU" + (translate ? "(Smoke)" : ""));
        weather = weather.replaceAll("DU", "DU" + (translate ? "(Dust)" : ""));
        weather = weather.replaceAll("SA", "SA" + (translate ? "(Sand)" : ""));
        weather = weather.replaceAll("HZ", "HZ" + (translate ? "(Haze)" : ""));
        weather = weather.replaceAll("PY", "PY" + (translate ? "(Spray)" : ""));
        weather = weather.replaceAll("VA", "VA" + (translate ? "(Volcanic Ash)" : ""));

        /*
         * Other
         */
        weather = weather.replaceAll("SQ", "SQ" + (translate ? "(Squalls)" : ""));
        weather = weather.replaceAll("FC", "FC" + (translate ? "(Funnel Cloud)" : ""));
        weather = weather.replaceAll("SS", "SS" + (translate ? "(Sand Storm)" : ""));
        weather = weather.replaceAll("DS", "DS" + (translate ? "(Dust Storm)" : ""));
        weather = weather.replaceAll(" VC", " VC" + (translate ? "(In Vicinity)" : ""));

        weather = weather.replaceAll("SKC", "SKC" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("CLR", "CLR" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("BKN", "BKN" + (translate ? "(Broken)" : ""));
        weather = weather.replaceAll("SCT", "SCT" + (translate ? "(Scattered)" : ""));
        weather = weather.replaceAll("OVC", "OVC" + (translate ? "(Overcast)" : ""));
        weather = weather.replaceAll("PROB", "PROB" + (translate ? "(Probibility%)" : ""));
        weather = weather.replaceAll("VV", "VV" + (translate ? "(Vertical Visibility)" : ""));
        weather = weather.replaceAll("CB", "CB" + (translate ? "(Cumulonimbus)" : ""));
        weather = weather.replaceAll("WS", "WS" + (translate ? "(Wind Shear)" : ""));

        /*
         * These are remarks
         */
        
        for(int i = 1 ; i < strip.length; i++) {
        
            String weather1 = strip[i];
            
            weather1 = weather1.replaceAll("AO", "AO" + (translate ? "(Station Type)" : ""));
            weather1 = weather1.replaceAll("RAB", "RAB" + (translate ? "(Rain Began)" : ""));
            weather1 = weather1.replaceAll("RAE", "RAE" + (translate ? "(Rain Ended)" : ""));
            weather1 = weather1.replaceAll("CIG", "CIG" + (translate ? "(Variable Ceiling)" : ""));
            weather1 = weather1.replaceAll("SLP", "SLP" + (translate ? "(Sea Level Pressure)" : ""));
            weather1 = weather1.replaceAll("RVRNO","RVRNO" + (translate ? "(No RVR reported)" : ""));
            weather1 = weather1.replaceAll("NOSIG", "NOSIG" + (translate ? "(No Significant Change Expected)" : ""));
            weather1 = weather1.replaceAll("TSNO", "TSNO" + (translate ? "(Thunderstom Info Not Available)" : ""));
            weather1 = weather1.replaceAll("PK WND", "PK WND" + (translate ? "(Peak Wind)" : ""));
            weather1 = weather1.replaceAll("WSHFT", "WSHFT" + (translate ? "(Wind Shift)" : ""));
            weather1 = weather1.replaceAll("VIS", "VIS" + (translate ? "(Visibility)" : ""));
            weather1 = weather1.replaceAll("PRESFR", "PRESFR" + (translate ? "(Rapid Pressure Change)" : ""));
            weather1 = weather1.replaceAll("\\$", "\\$" + (translate ? "(Station Maintenance Needed)" : ""));
            
            weather += " RMK" + (translate ? "(Remark)" : " ") + weather1;
        }


        return weather;
    }

    /**
     * Color code winds
     * @param weather
     * @return
     */
    public static String formatWindsHTML(String weather, boolean translate) {
        /*
         * Read wind as XXKT
         */
        int start;
        String output = "";
        String original = weather;
        /*
         * Cannot crash if transmission from HTTP has a bug
         */
        try {
            while((start = weather.indexOf("KT")) >= 0) {
                String portion = weather.substring(0, start);
                String windString = portion.substring(portion.length() - 2, portion.length());
                int winds = Integer.parseInt(windString);
                 
                if(winds < 10) {
                    portion = portion.substring(0, portion.length() - 2) + "<font color='green'>" + windString + "<font color='black'>" + "KT";
                }
                else if(winds < 20) {
                    portion = portion.substring(0, portion.length() - 2) + "<font color='blue'>" + windString + "<font color='black'>" + "KT";
                }
                else if(winds < 30) {
                    portion = portion.substring(0, portion.length() - 2) + "<font color='red'>" + windString + "<font color='black'>" + "KT";
                }
                else {
                    portion = portion.substring(0, portion.length() - 2) + "<font color='magenta'>" + windString + "<font color='black'>" + "KT";
                }
                output += portion;
                weather = weather.substring(start + 2, weather.length());
            }
            output += weather;
        }
        catch (Exception e) {
            /*
             * Mark magenta as we do not know whats in it
             */
            output = "<font color='magenta'>" + original + "<font color='black'>";
        }
        output = output.replaceAll("VRB", "VRB" + (translate ? "(Variable)" : ""));
        return output;
    }
    
    /**
     * Color code PIREPs
     * @param weather
     * @return
     */
    public static String formatPirepHTML(String weather, boolean translate) {
        weather = weather.replaceAll("UA", "UA" + (translate ? "(Upper Air)" : ""));
        weather = weather.replaceAll("UUA", "UUA" + (translate ? "(Urgent)" : ""));
        weather = weather.replaceAll("/OV", "/OV" + (translate ? "(Location)" : ""));
        weather = weather.replaceAll("/TM", "/TM" + (translate ? "(Time UTC)" : ""));
        weather = weather.replaceAll("/FL", "/FL" + (translate ? "(Altimeter MSL)" : ""));
        weather = weather.replaceAll("UNKN", "UNKN" + (translate ? "(Unknown)" : ""));
        weather = weather.replaceAll("DURC", "DURC" + (translate ? "(During Climb)" : ""));
        weather = weather.replaceAll("DURD", "DURD" + (translate ? "(During Descent)" : ""));
        weather = weather.replaceAll("/TP", "/TP" + (translate ? "(Aircraft Type)" : ""));
        weather = weather.replaceAll("/SK", "/SK" + (translate ? "(Sky Condition)" : ""));
        weather = weather.replaceAll("SKC", "SKC" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("BKN", "BKN" + (translate ? "(Broken)" : ""));
        weather = weather.replaceAll("SCT", "SCT" + (translate ? "(Scattered)" : ""));
        weather = weather.replaceAll("OVC", "OVC" + (translate ? "(Overcast)" : ""));
        weather = weather.replaceAll("/WX", "/WX" + (translate ? "(Weather)" : ""));
        weather = weather.replaceAll("/TA", "/TA" + (translate ? "(Temperature)" : ""));
        weather = weather.replaceAll("/WV", "/WV" + (translate ? "(Wind Velocity)" : ""));
        weather = weather.replaceAll("/IAS", "/IAS" + (translate ? "(Indicated Airspeed)" : ""));
        weather = weather.replaceAll("/IC", "/IC" + (translate ? "(Ice)" : ""));
        weather = weather.replaceAll("CLR", "CLR" + (translate ? "(Clear)" : ""));
        weather = weather.replaceAll("MXD", "MXD" + (translate ? "(Mixed)" : ""));
        weather = weather.replaceAll("RIM", "RIM" + (translate ? "(Rime)" : ""));
        weather = weather.replaceAll("TRC", "TRC" + (translate ? "(Trace)" : ""));
        weather = weather.replaceAll("MOD", "MOD" + (translate ? "(Moderate)" : ""));
        weather = weather.replaceAll("LGT", "LGT" + (translate ? "(Light)" : ""));
        weather = weather.replaceAll("SVR", "SVR" + (translate ? "(Severe)" : ""));
        weather = weather.replaceAll("HVY", "HVY" + (translate ? "(Heavy)" : ""));
        weather = weather.replaceAll("/RM", "/RM" + (translate ? "(Remarks)" : ""));
        weather = weather.replaceAll("/TB", "/TB" + (translate ? "(Turbulence)" : ""));

        return weather;
    }
    
    /**
     * Color code winds
     * @param weather
     * @return
     */
    public static String formatVisibilityHTML(String weather) {
        /*
         * Read wind as XXKT
         */
        int start;
        String output = "";
        String original = weather;
        /*
         * Cannot crash if transmission from HTTP has a bug
         */
        try {
            while((start = weather.indexOf("SM")) >= 0) {
                String portion = weather.substring(0, start);
                int portionlen = portion.length();
                int vis;
                int visstart;
                String visString;
                boolean P6 = false;
                String subportion = portion.substring(portionlen - 8);
                /*
                 * Too hard to parse this nonsense. Brute force.
                 */
                if(subportion.matches(".* P6")) {
                    visstart = portionlen - 2;
                    vis = 6;  
                    P6 = true;
                }
                else if(subportion.matches(".* [1-9]{1} [1-3]{1}\\/[2-4]{1}")) {
                    /*
                     * Like " 1 1/4"
                     */
                    visstart = portionlen - 5;
                    vis = Integer.parseInt(portion.substring(visstart, visstart + 1));  
                }
                else if(subportion.matches(".* [1]{1}[1-3]{1}\\/[2-4]{1}")) {
                    /*
                     * Like " 11/4"
                     */
                    visstart = portionlen - 4;
                    vis = Integer.parseInt(portion.substring(visstart, visstart + 1));  
                }
                else if(subportion.matches(".* [1-3]{1}\\/[2-4]{1}")) {
                    /*
                     * Like " 3/4"
                     */
                    visstart = portionlen - 3;
                    vis = 0;  
                }
                else {
                    /*
                     * Like " 3"
                     */
                    visstart = portionlen - 1;
                    vis = Integer.parseInt(portion.substring(visstart, visstart + 1));  
                }

                visString = portion.substring(visstart);
                
                if(P6) {
                    portion = portion.substring(0, visstart) + "<font color='green'>" + "P6" + "<font color='black'>" + "SM";                    
                }
                else if(vis >= 5) {
                    portion = portion.substring(0, visstart) + "<font color='green'>" + visString + "<font color='black'>" + "SM";
                }
                else if(vis >= 3) {
                    portion = portion.substring(0, visstart) + "<font color='blue'>" + visString + "<font color='black'>" + "SM";
                }
                else if(vis >= 1) {
                    portion = portion.substring(0, visstart) + "<font color='red'>" + visString + "<font color='black'>" + "SM";
                }
                else {
                    portion = portion.substring(0, visstart) + "<font color='magenta'>" + visString + "<font color='black'>" + "SM";
                }
                output += portion;
                weather = weather.substring(start + 2, weather.length());
            }
            output += weather;
        }
        catch (Exception e) {
            /*
             * Mark magenta as we do not know whats in it
             */
            output = "<font color='magenta'>" + original + "<font color='black'>";
        }
        return output;
    }

    /**
     * Wind decoder
     * @param wind
     * @return
     */
    public static String decodeWind(String wind) {
        
        if(wind.length() < 4) {
            return "";
        }
        
        int dir;
        int speed;
        try {
            dir = Integer.parseInt(wind.substring(0, 2)) * 10; 
            speed = Integer.parseInt(wind.substring(2, 4));
        }
        catch(Exception e) {
            return "";            
        }
        
        if(wind.length() == 4) {

            if(dir == 990 && speed == 0) {
                /*
                 * Light and variable
                 */
                return "000°000kt";
            }
            if(dir >= 510) {
                dir -= 500;
                speed += 100;
            }

            String out = String.format(Locale.getDefault(), "%03d°%03dkt", dir, speed);
            return(out);
        }

        if(wind.length() == 7) {
            String temp = wind.substring(4, 7);

            if(dir == 990 && speed == 0) {
                /*
                 * Light and variable
                 */
                return "000°000kt" + temp + "C";
            }
            if(dir >= 510) {
                dir -= 500;
                speed += 100;
            }

            String out = String.format(Locale.getDefault(), "%03d°%03dkt", dir, speed) + temp + "C";
            return(out);
            
        }

        if(wind.length() == 6) {
            String temp = "-" + wind.substring(4, 6);

            if(dir == 990 && speed == 0) {
                /*
                 * Light and variable
                 */
                return "000°000kt" + temp + "C";
            }
            if(dir >= 510) {
                dir -= 500;
                speed += 100;
            }

            String out = String.format(Locale.getDefault(), "%03d°%03dkt", dir, speed) + temp + "C";
            return(out);
        }

        return "";
    }
}
