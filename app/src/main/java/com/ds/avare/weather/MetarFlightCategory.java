package com.ds.avare.weather;

import net.sf.jweather.metar.Metar;
import net.sf.jweather.metar.MetarParser;
import net.sf.jweather.metar.SkyCondition;

import java.util.ArrayList;

/**
 * Created by zkhan on 12/12/15.
 */
public class MetarFlightCategory {


    public static String getFlightCategory(String stationId, String rawText) {

        String flightCategory = "Unknown";

        // parse it to find flight category
        try {
            Metar metar = MetarParser.parse(stationId + " " + rawText);
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
