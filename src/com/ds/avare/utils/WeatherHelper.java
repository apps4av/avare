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
    public static String formatWeatherHTML(String weather) {
        weather = weather.replace("TAF ", "");
        weather = weather.replace("AMD ", "");
        weather = weather.replace("\n", "<br>");
        weather = weather.replace(" FM", "<br></br>FM(From)<br>");
        weather = weather.replace("BECMG", "<br></br>BECMG(Becoming)<br>"); 
        return weather;
    }

    /**
     * Color code weather type 
     * @param weather
     * @return
     */
    public static String formatTafHTML(String weatherAll) {

        String strip[] = weatherAll.split("RMK");
        String weather = strip[0];

        /*
         * Qualifiers
         */
        weather = weather.replaceAll("\\+", "<font color='magenta'>+(Heavy)<font color='black'>");
        weather = weather.replaceAll("\\-", "<font color='red'>-(Light)<font color='black'>");
        
        /*
         * Description
         */
        weather = weather.replaceAll("MI", "<font color='blue'>MI(Shallow)<font color='black'>");
        weather = weather.replaceAll("BC", "<font color='blue'>BC(Patches)<font color='black'>");
        weather = weather.replaceAll("DR", "<font color='blue'>DR(Low Drifting)<font color='black'>");
        weather = weather.replaceAll("BL", "<font color='blue'>BL(BLowing)<font color='black'>");
        weather = weather.replaceAll("SH", "<font color='blue'>SH(Showers)<font color='black'>");
        weather = weather.replaceAll("TS", "<font color='red'>TS(Thunderstorm)<font color='black'>");
        weather = weather.replaceAll("FZ", "<font color='red'>FZ(Freezing)<font color='black'>");
        weather = weather.replaceAll("PR", "<font color='blue'>PR(Partial)<font color='black'>");
        weather = weather.replaceAll("AMD", "AMD(Amended)");
        weather = weather.replaceAll("WSCONDS", "WSCONDS(Wind Shear Possible)");
        
        /*
         * Precip
         */
        weather = weather.replaceAll("DZ", "<font color='blue'>DZ(Drizzle)<font color='black'>");
        weather = weather.replaceAll("RA", "<font color='red'>RA(Rain)<font color='black'>");
        weather = weather.replaceAll("SN", "<font color='red'>SN(Snow)<font color='black'>");
        weather = weather.replaceAll("SG", "<font color='red'>SG(Snow Grains)<font color='black'>");
        weather = weather.replaceAll("IC", "<font color='red'>IC(Ice Crystals)<font color='black'>");
        weather = weather.replaceAll("PL", "<font color='red'>PL(Ice Pellets)<font color='black'>");
        weather = weather.replaceAll("GR", "<font color='red'>GR(Hail)<font color='black'>");
        weather = weather.replaceAll("GS", "<font color='blue'>GS(Small Hail)<font color='black'>");
        weather = weather.replaceAll("UP", "<font color='red'>UP(Unknown Precip.)<font color='black'>");

        /*
         * Obstruction
         */
        weather = weather.replaceAll("BR", "<font color='red'>BR(Mist)<font color='black'>");
        weather = weather.replaceAll("FG", "<font color='red'>FG(Fog)<font color='black'>");
        weather = weather.replaceAll("FU", "<font color='red'>FU(Smoke)<font color='black'>");
        weather = weather.replaceAll("DU", "<font color='red'>DU(Dust)<font color='black'>");
        weather = weather.replaceAll("SA", "<font color='red'>SA(Sand)<font color='black'>");
        weather = weather.replaceAll("HZ", "<font color='red'>HZ(Haze)<font color='black'>");
        weather = weather.replaceAll("PY", "<font color='red'>PY(Spray)<font color='black'>");
        weather = weather.replaceAll("VA", "<font color='red'>VA(Volcanic Ash)<font color='black'>");

        /*
         * Other
         */
        weather = weather.replaceAll("P0", "<font color='red'>P0(Dust Whirls)<font color='black'>");
        weather = weather.replaceAll("SQ", "<font color='red'>SQ(Squalls)<font color='black'>");
        weather = weather.replaceAll("FC", "<font color='red'>FC(Funnel Cloud)<font color='black'>");
        weather = weather.replaceAll("SS", "<font color='red'>SS(Sand Storm)<font color='black'>");
        weather = weather.replaceAll("DS", "<font color='red'>DS(Dust Storm)<font color='black'>");
        weather = weather.replaceAll(" VC", "<font color='black'> VC(In Vicinity)<font color='black'>");

        weather = weather.replaceAll("SKC", "SKC(Sky Clear)");
        weather = weather.replaceAll("CLR", "CLR(Sky Clear)");
        weather = weather.replaceAll("BKN", "BKN(Broken)");
        weather = weather.replaceAll("SCT", "SCT(Scattered)");
        weather = weather.replaceAll("OVC", "OVC(Overcast)");
        weather = weather.replaceAll("PROB", "PROB(Probibility%)");
        weather = weather.replaceAll("VV", "<font color='red'>VV(Vertical Visibility)<font color='black'>");
        weather = weather.replaceAll("CB", "<font color='red'>CB(Cumulonimbus)<font color='black'>");
        weather = weather.replaceAll("WS", "<font color='magenta'>WS(Wind Shear)<font color='black'>");
        
        weather = weather.replaceAll(" 9999 ", " 9999(Visibility > 7SM) ");
        weather = weather.replaceAll("QNH", "QNH(Minimum Altimeter)");
        weather = weather.replaceAll("INS", "INS(Inches)");

        for(int i = 1 ; i < strip.length; i++) {
            
            String weather1 = strip[i];
            
            weather += " RMK(Remark) " + weather1;
        }

        return weather;
    }

    /**
     * Color code winds
     * @param weather
     * @return
     */
    public static String formatMetarHTML(String weatherAll) {
        
        String strip[] = weatherAll.split("RMK");
        String weather = strip[0];
        
        /*
         * Remarks
         */
        weather = weather.replaceAll("\\+", "+(Heavy)");
        weather = weather.replaceAll("\\-", "-(Light)");
        weather = weather.replaceAll("IR", "IR(Runway Ice)");
        weather = weather.replaceAll("WR", "WR(Wet Runway)");
        weather = weather.replaceAll("LSR", "LSR(Loose Runway Snow)");
        weather = weather.replaceAll("PSR", "PSR(Packed Runway Snow)");
        weather = weather.replaceAll("LTG", "LTG(Lightning)");
        weather = weather.replaceAll("TCU","TCU(Towering Cumulus)");
        weather = weather.replaceAll("VRB", "VRB(Variable)");
        
        weather = weather.replaceAll("AUTO", "AUTO(Automated)");
        weather = weather.replaceAll("COR", "COR(Corrected)");
        weather = weather.replaceAll(" 9999 ", " 9999(Visibility > 7SM) ");
        weather = weather.replaceAll("SPECI", "SPECI(Special)");
        
        /*
         * Description
         */
        weather = weather.replaceAll("MI", "MI(Shallow)");
        weather = weather.replaceAll("BC", "BC(Patches)");
        weather = weather.replaceAll("DR", "DR(Low Drifting)");
        weather = weather.replaceAll("BL", "BL(BLowing)");
        weather = weather.replaceAll("SH", "SH(Showers)");
        weather = weather.replaceAll("TS", "TS(Thunderstorm)");
        weather = weather.replaceAll("FZ", "FZ(Freezing)");
        weather = weather.replaceAll("PR", "PR(Partial)");
        /*
         * Precip
         */
        weather = weather.replaceAll("DZ", "DZ(Drizzle)");
        weather = weather.replaceAll("RA ", "RA (Rain)");
        weather = weather.replaceAll("SN", "SN(Snow)");
        weather = weather.replaceAll("SG", "SG(Snow Grains)");
        weather = weather.replaceAll("IC", "IC(Ice Crystals)");
        weather = weather.replaceAll("PL", "PL(Ice Pellets)");
        weather = weather.replaceAll("GR", "GR(Hail)");
        weather = weather.replaceAll("GS", "GS(Small Hail)");
        weather = weather.replaceAll("UP", "UP(Unknown Precip.)");

        /*
         * Obstruction
         */
        weather = weather.replaceAll("BR", "BR(Mist)");
        weather = weather.replaceAll("FG", "FG(Fog)");
        weather = weather.replaceAll("FU", "FU(Smoke)");
        weather = weather.replaceAll("DU", "DU(Dust)");
        weather = weather.replaceAll("SA", "SA(Sand)");
        weather = weather.replaceAll("HZ", "HZ(Haze)");
        weather = weather.replaceAll("PY", "PY(Spray)");
        weather = weather.replaceAll("VA", "VA(Volcanic Ash)");

        /*
         * Other
         */
        weather = weather.replaceAll("SQ", "SQ(Squalls)");
        weather = weather.replaceAll("FC", "FC(Funnel Cloud)");
        weather = weather.replaceAll("SS", "SS(Sand Storm)");
        weather = weather.replaceAll("DS", "DS(Dust Storm)");
        weather = weather.replaceAll(" VC", " VC(In Vicinity)");

        weather = weather.replaceAll("SKC", "SKC(Sky Clear)");
        weather = weather.replaceAll("CLR", "CLR(Sky Clear)");
        weather = weather.replaceAll("BKN", "BKN(Broken)");
        weather = weather.replaceAll("SCT", "SCT(Scattered)");
        weather = weather.replaceAll("OVC", "OVC(Overcast)");
        weather = weather.replaceAll("PROB", "PROB(Probibility%)");
        weather = weather.replaceAll("VV", "VV(Vertical Visibility)");
        weather = weather.replaceAll("CB", "CB(Cumulonimbus)");
        weather = weather.replaceAll("WS", "WS(Wind Shear)");

        /*
         * These are remarks
         */
        
        for(int i = 1 ; i < strip.length; i++) {
        
            String weather1 = strip[i];
            
            weather1 = weather1.replaceAll("AO", "AO(Station Type)");
            weather1 = weather1.replaceAll("RAB", "RAB(Rain Began)");
            weather1 = weather1.replaceAll("RAE", "RAE(Rain Ended)");
            weather1 = weather1.replaceAll("CIG", "CIG(Variable Ceiling)");
            weather1 = weather1.replaceAll("SLP", "SLP(Sea Level Pressure)");
            weather1 = weather1.replaceAll("RVRNO","RVRNO(No RVR reported)");
            weather1 = weather1.replaceAll("NOSIG", "NOSIG(No Significant Change Expected)");
            weather1 = weather1.replaceAll("TSNO", "TSNO(Thunderstom Info Not Available)");
            weather1 = weather1.replaceAll("PK WND", "PK WND(Peak Wind)");
            weather1 = weather1.replaceAll("WSHFT", "WSHFT(Wind Shift)");
            weather1 = weather1.replaceAll("VIS", "VIS(Visibility)");
            weather1 = weather1.replaceAll("PRESFR", "PRESFR(Rapid Pressure Change)");
            weather1 = weather1.replaceAll("\\$", "\\$(Station Maintenance Needed)");
            
            weather += " RMK(Remark) " + weather1;
        }


        return weather;
    }

    /**
     * Color code winds
     * @param weather
     * @return
     */
    public static String formatWindsHTML(String weather) {
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
        output = output.replaceAll("VRB", "VRB(Variable)");
        return output;
    }
    
    /**
     * Color code PIREPs
     * @param weather
     * @return
     */
    public static String formatPirepHTML(String weather) {
        weather = weather.replaceAll("UA", "UA(Upper Air)");
        weather = weather.replaceAll("UUA", "UUA(Urgent)");
        weather = weather.replaceAll("/OV", "/OV(Location)");
        weather = weather.replaceAll("/TM", "/TM(Time UTC)");
        weather = weather.replaceAll("/FL", "/FL(Altimeter MSL)");
        weather = weather.replaceAll("UNKN", "UNKN(Unknown)");
        weather = weather.replaceAll("DURC", "DURC(During Climb)");
        weather = weather.replaceAll("DURD", "DURD(During Descent)");
        weather = weather.replaceAll("/TP", "/TP(Aircraft Type)");
        weather = weather.replaceAll("/SK", "/SK(Sky Condition)");
        weather = weather.replaceAll("SKC", "SKC(Sky Clear)");
        weather = weather.replaceAll("BKN", "BKN(Broken)");
        weather = weather.replaceAll("SCT", "SCT(Scattered)");
        weather = weather.replaceAll("OVC", "OVC(Overcast)");
        weather = weather.replaceAll("/WX", "/WX(Weather)");
        weather = weather.replaceAll("/TA", "/TA(Temperature)");
        weather = weather.replaceAll("/WV", "/WV(Wind Velocity)");
        weather = weather.replaceAll("/IAS", "/IAS(Indicated Airspeed)");
        weather = weather.replaceAll("/IC", "/IC(Ice)");
        weather = weather.replaceAll("CLR", "CLR(Clear)");
        weather = weather.replaceAll("MXD", "MXD(Mixed)");
        weather = weather.replaceAll("RIM", "RIM(Rime)");
        weather = weather.replaceAll("TRC", "TRC(Trace)");
        weather = weather.replaceAll("MOD", "MOD(Moderate)");
        weather = weather.replaceAll("LGT", "LGT(Light)");
        weather = weather.replaceAll("SVR", "SVR(Severe)");
        weather = weather.replaceAll("HVY", "HVY(Heavy)");
        weather = weather.replaceAll("/RM", "/RM(Remarks)");
        weather = weather.replaceAll("/TB", "/TB(Turbulence)");

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
                    portion = portion.substring(0, visstart) + "<font color='green'>" + "P6(6+)" + "<font color='black'>" + "SM";                    
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

}
