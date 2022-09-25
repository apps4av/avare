/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import android.util.Pair;

import java.util.LinkedList;
import java.util.Locale;


public class WeatherHelper {

    /**
     *
     * @param type
     * @return
     */
    public static int metarColor(String type) {
        if(type.equals("VFR")) {
            //green
            return(0xff78e825);
        }
        else if(type.equals("IFR")) {
            // red
            return(0xffff2a00);
        }
        else if(type.equals("MVFR")) {
            // blue
            return(0xff4884ff);
        }
        else if(type.equals("LIFR")) {
            // magenta
            return(0xffff54f9);
        }
        return(0xff333333);
    }

    /**
     *
     * @param type
     * @return
     */
    public static String metarColorString(String type) {
        if(type.equals("VFR")) {
            return("#78e825");
        }
        else if(type.equals("IFR")) {
            return("#ff2a00");
        }
        else if(type.equals("MVFR")) {
            return("#008aff");
        }
        else if(type.equals("LIFR")) {
            return("#ff54f9");
        }
        return("white");
    }

    public static String addColorWithStroke(String input, String color) {
        return "<font style='-webkit-text-stroke:0.1vw white; -webkit-text-fill-color: " + color + "; '>" + input +  "</font>";
    }

    public static String addColor(String input, String color) {
        return "<font color='" + color + "'>" + input + "</font>";
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
            weather = weather.replace(" FM", "</br>FM(From)<br>");
            weather = weather.replace("BECMG", "</br>BECMG(Becoming)<br>");
        }
        else {
            weather = weather.replace(" FM", "</br>FM");
            weather = weather.replace("BECMG", "</br>BECMG");
        }
        return weather;
    }

    /**
     * Color code weather type
     * @param weatherAll
     * @param translate
     * @return
     */
    public static String formatTafHTML(String weatherAll, boolean translate) {

        String strip[] = weatherAll.split("RMK");
        String weather = strip[0];

        /*
         * Qualifiers
         */
        weather = weather.replaceAll("\\+", WeatherHelper.addColorWithStroke((translate ? "(Heavy)" : ""), "#ff54f9"));
        weather = weather.replaceAll("\\+", WeatherHelper.addColorWithStroke((translate ? "(Light)" : ""), "#ff2a00"));

        /*
         * Description
         */
        weather = weather.replaceAll("MI", WeatherHelper.addColorWithStroke("MI" + (translate ? "(Shallow)" : ""), "#008aff"));
        weather = weather.replaceAll("BC", WeatherHelper.addColorWithStroke("BC" + (translate ? "(Patches)" : ""), "#008aff"));
        weather = weather.replaceAll("DR", WeatherHelper.addColorWithStroke("DR" + (translate ? "(Low Drifting)" : ""), "#008aff"));
        weather = weather.replaceAll("BL", WeatherHelper.addColorWithStroke("BL" + (translate ? "(Blowing)" : ""), "#008aff"));
        weather = weather.replaceAll("SH", WeatherHelper.addColorWithStroke("SH" + (translate ? "(Showers)" : ""), "#008aff"));
        weather = weather.replaceAll("TS", WeatherHelper.addColorWithStroke("TS" + (translate ? "(Thunderstorm)" : ""), "#ff2a00"));
        weather = weather.replaceAll("FZ", WeatherHelper.addColorWithStroke("FZ" + (translate ? "(Freezing)" : ""), "#008aff"));
        weather = weather.replaceAll("PR", WeatherHelper.addColorWithStroke("PR" + (translate ? "(Partial)" : ""), "#008aff"));
        weather = weather.replaceAll("AMD", WeatherHelper.addColorWithStroke("AMD" + (translate ? "(Amended)" : ""), "#ffffff"));
        weather = weather.replaceAll("WSCONDS", WeatherHelper.addColorWithStroke("WSCONDS" + (translate ? "(Wind Shear Possible)" : ""), "#ffffff"));

        /*
         * Precip
         */
        weather = weather.replaceAll("DZ", WeatherHelper.addColorWithStroke("DZ" + (translate ? "(Drizzle)" : ""), "#008aff"));
        weather = weather.replaceAll("RA", WeatherHelper.addColorWithStroke("RA" + (translate ? "(Rain)" : ""), "#ff2a00"));
        weather = weather.replaceAll("SN", WeatherHelper.addColorWithStroke("SN" + (translate ? "(Snow)" : ""), "#ff2a00"));
        weather = weather.replaceAll("SG", WeatherHelper.addColorWithStroke("SG" + (translate ? "(Snow Grains)" : ""), "#ff2a00"));
        weather = weather.replaceAll("IC", WeatherHelper.addColorWithStroke("IC" + (translate ? "(Ice Crystals)" : ""), "#ff2a00"));
        weather = weather.replaceAll("PL", WeatherHelper.addColorWithStroke("PL" + (translate ? "(Ice Pellets)" : ""), "#ff2a00"));
        weather = weather.replaceAll("GR", WeatherHelper.addColorWithStroke("GR" + (translate ? "(Hail)" : ""), "#ff2a00"));
        weather = weather.replaceAll("GS", WeatherHelper.addColorWithStroke("GS" + (translate ? "(Small Hail)" : ""), "#008aff"));
        weather = weather.replaceAll("UP", WeatherHelper.addColorWithStroke("UP" + (translate ? "(Unknown Precip.)" : ""), "#ff2a00"));

        /*
         * Obstruction
         */
        weather = weather.replaceAll("BR", WeatherHelper.addColorWithStroke("BR" + (translate ? "(Mist)" : ""), "#ff2a00"));
        weather = weather.replaceAll("FG", WeatherHelper.addColorWithStroke("FG" + (translate ? "(Fog)" : ""), "#ff2a00"));
        weather = weather.replaceAll("FU", WeatherHelper.addColorWithStroke("FU" + (translate ? "(Smoke)" : ""), "#ff2a00"));
        weather = weather.replaceAll("DU", WeatherHelper.addColorWithStroke("DU" + (translate ? "(Dust)" : ""), "#ff2a00"));
        weather = weather.replaceAll("SA", WeatherHelper.addColorWithStroke("SA" + (translate ? "(Sand)" : ""), "#ff2a00"));
        weather = weather.replaceAll("HZ", WeatherHelper.addColorWithStroke("HZ" + (translate ? "(Haze)" : ""), "#ff2a00"));
        weather = weather.replaceAll("PY", WeatherHelper.addColorWithStroke("PY" + (translate ? "(Spray)" : ""), "#ff2a00"));
        weather = weather.replaceAll("VA", WeatherHelper.addColorWithStroke("VA" + (translate ? "(Volcanic Ash)" : ""), "#ff2a00"));

        /*
         * Other
         */
        weather = weather.replaceAll("P0", WeatherHelper.addColorWithStroke("P0" + (translate ? "(Dust Whirls)" : ""), "#ff2a00"));
        weather = weather.replaceAll("SQ", WeatherHelper.addColorWithStroke("SQ" + (translate ? "(Squalls)" : ""), "#ff2a00"));
        weather = weather.replaceAll("FC", WeatherHelper.addColorWithStroke("FC" + (translate ? "(Funnel Cloud)" : ""), "#ff2a00"));
        weather = weather.replaceAll("SS", WeatherHelper.addColorWithStroke("SS" + (translate ? "(Sand Storm)" : ""), "#ff2a00"));
        weather = weather.replaceAll("DS", WeatherHelper.addColorWithStroke("DS" + (translate ? "(Dust Storm)" : ""), "#ff2a00"));
        weather = weather.replaceAll(" VC", WeatherHelper.addColorWithStroke(" VC" + (translate ? "(In Vicinity)" : ""), "#ffffff"));

        weather = weather.replaceAll("SKC", "SKC" + (translate ? "(Sky Clear)" : ""));
        weather = weather.replaceAll("CLR", "CLR" + (translate ? "(Clear <12,000ft)" : "")); // see http://www.weather.gov/media/grb/misc/TAF_Card.pdf
        weather = weather.replaceAll("BKN", "BKN" + (translate ? "(Broken)" : ""));
        weather = weather.replaceAll("SCT", "SCT" + (translate ? "(Scattered)" : ""));
        weather = weather.replaceAll("OVC", "OVC" + (translate ? "(Overcast)" : ""));
        weather = weather.replaceAll("PROB", "PROB" + (translate ? "(Probability%)" : ""));
        weather = weather.replaceAll("VV", WeatherHelper.addColorWithStroke("VV" + (translate ? "(Vertical Visibility)" : ""), "#ff2a00"));
        weather = weather.replaceAll("CB", WeatherHelper.addColorWithStroke("CB" + (translate ? "(Cumulonimbus)" : ""), "#ff2a00"));
        weather = weather.replaceAll("WS", WeatherHelper.addColorWithStroke("WS" + (translate ? "(Wind Shear)" : ""), "#ff54f9"));

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
     * Split string on the second space
     * @param s
     * @return pair of strings split
     */
    private static Pair splitOnSecondSpace(String s) {
        int i = s.indexOf(' ', 1 + s.indexOf(' '));
        String firstPart = s.substring(0, i);
        String secondPart = s.substring(i+1);
        return Pair.create(firstPart, secondPart);
    }

    /**
     * Color code winds
     * @param weatherAll
     * @param translate
     * @return
     */
    public static String formatMetarHTML(String weatherAll, boolean translate) {

        String strip[] = weatherAll.split("RMK");

        // a bit of a simplification but typically first 2 items are identifier and time; they need no translation
        Pair<String,String> p = splitOnSecondSpace(strip[0]);
        String identAndTime = p.first;
        String weather = p.second;

        /*
         * Identifier
         */
        identAndTime = identAndTime.replaceAll("SPECI", "SPECI" + (translate ? "(Special/unscheduled)" : ""));

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

        /*
         * Description
         */
        weather = weather.replaceAll("MI", "MI" + (translate ? "(Shallow)" : ""));
        weather = weather.replaceAll("BC", "BC" + (translate ? "(Patches)" : ""));
        weather = weather.replaceAll("DR", "DR" + (translate ? "(Low Drifting)" : ""));
        weather = weather.replaceAll("BL", "BL" + (translate ? "(Blowing)" : ""));
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
         * Obscuration
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
        weather = weather.replaceAll("CLR", "CLR" + (translate ? "(Clear <12,000ft)" : "")); // see http://www.faraim.org/aim/aim-4-03-14-494.html
        weather = weather.replaceAll("BKN", "BKN" + (translate ? "(Broken)" : ""));
        weather = weather.replaceAll("SCT", "SCT" + (translate ? "(Scattered)" : ""));
        weather = weather.replaceAll("OVC", "OVC" + (translate ? "(Overcast)" : ""));
        weather = weather.replaceAll("PROB", "PROB" + (translate ? "(Probability%)" : ""));
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
            weather1 = weather1.replaceAll("TSNO", "TSNO" + (translate ? "(Thunderstorm Info Not Available)" : ""));
            weather1 = weather1.replaceAll("PK WND", "PK WND" + (translate ? "(Peak Wind)" : ""));
            weather1 = weather1.replaceAll("WSHFT", "WSHFT" + (translate ? "(Wind Shift)" : ""));
            weather1 = weather1.replaceAll("VIS", "VIS" + (translate ? "(Visibility)" : ""));
            weather1 = weather1.replaceAll("PRESRR", "PRESFR" + (translate ? "(Pressure Raising Rapidly)" : ""));
            weather1 = weather1.replaceAll("PRESFR", "PRESFR" + (translate ? "(Pressure Falling Rapidly)" : ""));
            weather1 = weather1.replaceAll("\\$", "\\$" + (translate ? "(Station Maintenance Needed)" : ""));

            weather += " RMK" + (translate ? "(Remark)" : " ") + weather1;
        }

        return identAndTime + " " + weather;
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
                    portion = portion.substring(0, portion.length() - 2) + WeatherHelper.addColorWithStroke(windString, "#78e825") + "KT";
                }
                else if(winds < 20) {
                    portion = portion.substring(0, portion.length() - 2) + WeatherHelper.addColorWithStroke(windString, "#008aff") + "KT";
                }
                else if(winds < 30) {
                    portion = portion.substring(0, portion.length() - 2) + WeatherHelper.addColorWithStroke(windString, "#ff2a00") + "KT";
                }
                else {
                    portion = portion.substring(0, portion.length() - 2) + WeatherHelper.addColorWithStroke(windString, "#ff54f9") + "KT";
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
            output = WeatherHelper.addColorWithStroke(original, "#ff54f9");
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
                    portion = portion.substring(0, visstart) + WeatherHelper.addColorWithStroke("P6", "#78e825") + "SM";
                }
                else if(vis >= 5) {
                    portion = portion.substring(0, visstart) + WeatherHelper.addColorWithStroke(visString, "#78e825") + "SM";
                }
                else if(vis >= 3) {
                    portion = portion.substring(0, visstart) + WeatherHelper.addColorWithStroke(visString, "#008aff") + "SM";
                }
                else if(vis >= 1) {
                    portion = portion.substring(0, visstart) + WeatherHelper.addColorWithStroke(visString, "#ff2a00") + "SM";
                }
                else {
                    portion = portion.substring(0, visstart) + WeatherHelper.addColorWithStroke(visString, "#ff54f9") + "SM";
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
            output = WeatherHelper.addColorWithStroke(original, "#ff54f9");
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

    /**
     * See decodeWind
     * @param wind
     * @return
     */
    public static int decodeWindSpeed(String wind) {
        String windsd = decodeWind(wind);
        String w[] = windsd.split("°");
        int speed = 0;
        try {
            speed = Integer.parseInt(w[1].split("kt")[0]);
        }
        catch (Exception e) {

        }
        return speed;
    }

    /**
     * See decodeWind
     * @param wind
     * @return
     */
    public static int decodeWindDir(String wind) {
        String windsd = decodeWind(wind);
        String w[] = windsd.split("°");
        int dir = 0;
        try {
            dir = Integer.parseInt(w[0]);
        }
        catch (Exception e) {

        }
        return dir;
    }

    /**
     *
     * @return
     */
    public static String getNamMosLegend() {
        /*
         * Legend
         */
        return
                "<a href='http://www.nws.noaa.gov/mdl/synop/namcard.php'>NAM Forecast Legend</a><br>";

    }

    /**
     * Returns time from METAR
     * @param metar
     * @return
     */
    public static String getMetarTime(String metar) {
        String time = "";
        // parse time, temp, altitude setting
        String tokens[] = metar.split(" ");
        if(tokens.length > 1) {
            time = tokens[1];
        }

        return time;
    }

    /**
     * Returns density altitude for a field from its METAR and elevation
     * @param metar
     * @param elev
     * @return
     */
    public static String getDensityAltitude(String metar, String elev) {

        if(null == elev || null == metar) {
            return "";
        }

        double da = 0;
        double temp = 0;
        double as = 0;

        double st = 0;
        double at = 0;

        double pa = 0;
        double elevation = 0;

        boolean tmpset = false;
        boolean aset = false;
        boolean melev = false;

        // parse time, temp, altitude setting
        String tokens[] = metar.split(" ");

        try {
            for(int i = 0; i < tokens.length; i++) {
                if(tokens[i].equals("RMK")) {
                    break;
                }
                if(tokens[i].matches("M?[0-9]*/M?[0-9]*")) {
                    String t = tokens[i].split("/")[0];
                    if(t.startsWith("M")) {
                        t = t.substring(1);
                        temp = Double.parseDouble(t);
                        temp = -temp;
                    }
                    else {
                        temp = Double.parseDouble(t);
                    }
                    tmpset = true;
                    continue;
                }
                if(tokens[i].matches("A[0-9][0-9][0-9][0-9]")) {
                    as = Double.parseDouble(tokens[i].split("A")[1]) / 100;
                    aset = true;
                    continue;
                }
            }
            elevation = Double.parseDouble(elev);
            melev = true;
        }
        catch (Exception e) {
        }

        if(tmpset && aset && melev) {

            // pressure altitude, correct for non standard
            pa = elevation + (29.92 - as) * 1000.0;

            // standard temp Kelvin
            st = 273.15 - (15 - 0.0019812 * pa);

            // reported temp Kelvin
            at = 273.15 - temp;

            // density altitude, aviation formulary
            da = pa + 118.6 * (st - at);

            // round to nearest 100
            da = ((int)(da / 100)) * 100;

            return "" + (int)da + " ft";
        }

        return "";
    }

    public static double[] getWindFromMetar(String metar) {

        boolean windset = false;

        if(null == metar) {
            return null;
        }

        double wnd[] = new double[3];
        wnd[0] = 0;
        wnd[1] = 0;
        wnd[2] = 0;

        String wind = "";

        // parse time, temp, altitude setting
        String tokens[] = metar.split(" ");

        try {
            for(int i = 0; i < tokens.length; i++) {
                if(tokens[i].equals("RMK")) {
                    break;
                }
                if(tokens[i].matches(".*KT")) {
                    wind = tokens[i];
                    // first 3 digits are direction true, or VRB.
                    String tmp = wind.substring(0, 3);
                    if(tmp.equals("VRB")) {
                        // variable, almost calm
                        tmp = "000";
                    }
                    wnd[0] = Double.parseDouble(tmp);
                    // next 2 digits are speed
                    wnd[1] = Double.parseDouble(wind.substring(3, 5));
                    if(wind.contains("G")) {
                        // gusting to
                        wnd[2] = Double.parseDouble(wind.substring(6, 8));
                    }
                    windset = true;
                    continue;
                }
            }
        }
        catch (Exception e) {
        }
        if(windset) {
            return wnd;
        }
        return null;
    }

    /**
     * Returns best wind aligned runway from METAR
     * @param metar
     * @param runways
     * @return
     */
    public static String getBestRunway(String metar, LinkedList<String> runways) {

        if(null == runways || null == metar) {
            return "";
        }



        double head1 = 0;
        double head0 = 0;
        double cross1 = 0;
        double cross0 = 0;

        double[] windset = getWindFromMetar(metar);
        if(windset != null) {

            double dir = windset[0];
            double spd0 = windset[1];
            double spd1 = windset[2];

            /*
             * Find best wind aligned runway
             */
            double maxW = -1E10;
            String best = "";
            for(String s : runways) {
                String run[] = s.split(",");
                try {
                    double rhead = Double.parseDouble(run[1]);
                    // find cross and head wind components
                    // aviation formulary
                    head0 = spd0 * Math.cos(Math.toRadians(dir - rhead));
                    if(head0 > maxW) {
                        // find runway with max headwind component
                        maxW = head0;
                        cross0 = spd0 * Math.sin(Math.toRadians(dir - rhead));
                        if(spd1 != 0) {
                            head1 = spd1 * Math.cos(Math.toRadians(dir - rhead));
                            cross1 = spd1 * Math.sin(Math.toRadians(dir - rhead));
                        }
                        best = run[0];
                        best += "\n " + Math.abs((int)head0);
                        if(spd1 != 0) {
                            best += "G" + Math.abs((int)head1);
                        }
                        // T = tail, H = head, L = left, R = right
                        best += (head0 < 0 ? "KT Tail" : "KT Head");
                        best += "\n " + Math.abs((int)cross0);
                        if(spd1 != 0) {
                            best += "G" + Math.abs((int)cross1);
                        }

                        best += (cross0 < 0 ? "KT Left X" : "KT Right X");
                    }
                }
                catch (Exception e) {
                    continue;
                }

            }
            return best;
        }

        return "";
    }

}
