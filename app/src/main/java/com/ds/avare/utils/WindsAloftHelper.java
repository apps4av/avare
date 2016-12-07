package com.ds.avare.utils;

import android.util.Pair;

import com.ds.avare.weather.WindsAloft;

import java.util.Locale;

/**
 * @author pasniak
 */
 public class WindsAloftHelper {
    /**
     *  Wind decoder: public interface
     * @param wa
     * @param upToAltitude
     * @return HTML fragment with a table containing decoded Winds Aloft up to up to altitude
     */
    public static String formatWindsHTML(WindsAloft wa, int upToAltitude) {
        String header = wa.station + "<br>" + wa.time + "<br>";
        String winds;
        winds =  (upToAltitude > 0) ? formatWindRow("3000", wa.w3k) : "";
        winds += (upToAltitude > 3) ? formatWindRow("6000", wa.w6k) : "";
        winds += (upToAltitude > 6) ? formatWindRow("9000", wa.w9k) : "";
        winds += (upToAltitude > 9) ? formatWindRow("12000", wa.w12k) : "";
        winds += (upToAltitude > 12) ? formatWindRow("18000", wa.w18k) : "";
        winds += (upToAltitude > 18) ? formatWindRow("24000", wa.w24k) : "";
        winds += (upToAltitude > 24) ? formatWindRow("30000", wa.w30k) : "";
        winds += (upToAltitude > 30) ? formatWindRow("34000", wa.w34k) : "";
        winds += (upToAltitude > 34) ? formatWindRow("39000", wa.w39k) : "";
        return header + table(winds);
    }

    /**
     * Wind decoder: format table row with altitude, decoded wind and temperature
     * @param wind
     * @return formatted HTML table row
     */
    private static String formatWindRow(String alt, String wind) {
        DirSpeedTemp w = parseWindAndTemperature(wind);
        return (w.IsNull) ? tr("") : tr(td(alt) + td(w.Dir) + td(w.Speed) + td(w.Temp));
    }

    //nbsp is used here to nicely space out cells in the row 
    private static String td(String c) { return "<td align='right'>&nbsp;"+c+"</td>"; }
    private static String tr(String r) { return "<tr>"+r+"</tr>"; }
    private static String table(String t) { return "<table>"+t+"</table>"; }

    //WA string lengths
    private static int ONLY_WIND_LEN = 4, WIND_NEG_TEMP_LEN = 6, WIND_AND_TEMP_LEN = 7, TEMP_LEN = 4;

    /**
     * Wind decoder : parse wind and temperature
     * @param wind
     * @return
     */
    private static DirSpeedTemp parseWindAndTemperature(String wind) {
        final int wl = wind.length();
        if (wl == ONLY_WIND_LEN || wl == WIND_AND_TEMP_LEN || wl == WIND_NEG_TEMP_LEN) {
            DirSpeed ds;
            try {
                ds = DirSpeed.parseFrom(wind);
            } catch (Exception e) {
                return new DirSpeedTemp();
            }
            String temperature = (wl == ONLY_WIND_LEN) ? "" :
                    Integer.parseInt((wl == WIND_NEG_TEMP_LEN ? "-" : "") + wind.substring(TEMP_LEN, wl))
                    + "C";
            return new DirSpeedTemp(formatDirAndSpeed(ds), temperature);
        } else {
            return new DirSpeedTemp();
        }
    }

    /**
     * Wind decoder:
     * @param ds Direction and Speed
     * @return (direction, speed) strings pair
     */
    private static Pair<String,String> formatDirAndSpeed(DirSpeed ds) {
        return new Pair(String.format(Locale.getDefault(), "%03d°", ds.Dir),
                String.format(Locale.getDefault(), "%dkt", ds.Speed));
    }


    private static class DirSpeedTemp {
        final public String Dir, Speed, Temp;
        final boolean IsNull;

        private DirSpeedTemp(Pair<String,String> wind, String t)
        {
            Dir = wind.first; Speed = wind.second; Temp = t;
            IsNull = false;
        }
        private DirSpeedTemp() {
            Dir = Speed = Temp = "";
            IsNull = true;
        }
    }

    /**
     * Wind decoder: parser for numeric wind direction and speed
     */
    public static class DirSpeed {
        final public int Dir, Speed;

        /**
         * Decode wind and direction values based on WA table logic
         * @param wind
         */
        private DirSpeed(String wind) {
            if (wind.startsWith("9900")) // Light and variable
            {
                Dir = 0;
                Speed = 0;
            }
            else
            {
                int dir = Integer.parseInt(wind.substring(0, 2)) * 10;
                int speed = Integer.parseInt(wind.substring(2, 4));
                if (dir >= 510) {
                    dir -= 500;
                    speed += 100;
                }
                Dir = dir;
                Speed = speed;
            }
        }

        /**
         * Wind decoder: public interface
         * @param wind
         * @return
         */
        public static DirSpeed parseFrom(String wind) {
            return new DirSpeed(wind);
        }
    }
}