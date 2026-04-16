package com.ds.avare.weather;

import net.sf.jweather.metar.Metar;
import net.sf.jweather.metar.MetarParser;
import net.sf.jweather.metar.SkyCondition;

import java.util.ArrayList;

/**
 * Created by zkhan on 12/12/15.
 */
public class MetarFlightCategory {

    /** Real METAR/SPECI bodies are far smaller; cap avoids parser OOM on garbage uplinks. */
    private static final int MAX_RAW_METAR_CHARS = 4096;

    public static String getFlightCategory(String stationId, String rawText) {

        String flightCategory = "Unknown";

        if (stationId == null || rawText == null) {
            return flightCategory;
        }

        String s = rawText.trim();
        if (s.isEmpty()) {
            return flightCategory;
        }

        int nl = s.indexOf('\n');
        if (nl >= 0) {
            s = s.substring(0, nl).trim();
            if (s.isEmpty()) {
                return flightCategory;
            }
        }

        if (s.length() > MAX_RAW_METAR_CHARS) {
            s = s.substring(0, MAX_RAW_METAR_CHARS);
        }

        String id = stationId.trim();
        if (id.isEmpty()) {
            return flightCategory;
        }

        // parse it to find flight category
        try {
            Metar metar = MetarParser.parse(id + " " + s);
            float vis = metar.getVisibility().floatValue();
            int ovc = Integer.MAX_VALUE;
            boolean isCeiling = false;
            boolean visLessThan = metar.getVisibilityLessThan();
            ArrayList<SkyCondition> sky = metar.getSkyConditions();
            for (SkyCondition cond : sky) {
                if(cond.isBrokenClouds() || cond.isOvercast() || cond.isVerticalVisibility()) {
                    ovc = cond.getHeight();
                    isCeiling = true;
                    break;
                }
            }
            flightCategory = getFlightCategory(isCeiling, ovc, vis, visLessThan);
        }
        catch (Exception e) {
        }
        return flightCategory;
    }


    /**
     * Find flight category
     * https://www.aviationweather.gov/adds/metars/description/page_no/4
     * @param isCeiling
     * @param ceilingFt
     * @param visibility
     * @return
     */
    private static String getFlightCategory(boolean isCeiling, int ceilingFt, float visibility, boolean visLessThan) {
        if(visLessThan) {
            visibility -= 0.01;
        }
        if((isCeiling && ceilingFt < 500) || visibility < 1) {
            return "LIFR";
        }
        if((isCeiling && ceilingFt < 1000 && ceilingFt >= 500) || (visibility >= 1 && visibility < 3)) {
            return "IFR";
        }
        if((isCeiling && ceilingFt < 3000 && ceilingFt >= 1000) || (visibility >= 3 && visibility <= 5)) {
            return "MVFR";
        }
        if((ceilingFt > 3000 || (!isCeiling)) && (visibility > 5)) {
            return "VFR";
        }
        return "Unknown";
    }
}
