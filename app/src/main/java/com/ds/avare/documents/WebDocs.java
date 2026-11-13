/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.documents;

import java.util.ArrayList;
import java.util.List;

/**
 * Internet weather pictures shown in the Documents screen. Same list and order
 * as avarex {@code DocumentsScreenState.productsStatic}.
 */
public final class WebDocs {

    /** One internet picture. */
    public static final class Item {
        public final String type;
        public final String name;
        public final String url;

        Item(String type, String name, String url) {
            this.type = type;
            this.name = name;
            this.url = url;
        }
    }

    private static final String[][] DATA = {
        {"WPC", "WPC Surface Analysis", "https://aviationweather.gov/data/products/progs/F000_wpc_sfc.gif"},
        {"WPC", "WPC 6HR Prognostic", "https://aviationweather.gov/data/products/progs/F006_wpc_prog.gif"},
        {"WPC", "WPC 12HR Prognostic", "https://aviationweather.gov/data/products/progs/F012_wpc_prog.gif"},
        {"WPC", "WPC 18HR Prognostic", "https://aviationweather.gov/data/products/progs/F018_wpc_prog.gif"},
        {"WPC", "WPC 24HR Prognostic", "https://aviationweather.gov/data/products/progs/F024_wpc_prog.gif"},
        {"WPC", "WPC 30HR Prognostic", "https://aviationweather.gov/data/products/progs/F030_wpc_prog.gif"},
        {"WPC", "WPC 36HR Prognostic", "https://aviationweather.gov/data/products/progs/F036_wpc_prog.gif"},
        {"WPC", "WPC 48HR Prognostic", "https://aviationweather.gov/data/products/progs/F048_wpc_prog.gif"},
        {"WPC", "WPC 60HR Prognostic", "https://aviationweather.gov/data/products/progs/F060_wpc_prog.gif"},
        {"WPC", "WPC 72HR Prognostic", "https://aviationweather.gov/data/products/progs/F072_wpc_prog.gif"},
        {"SigWx", "SigWx Low Level 00HR", "https://www.aviationweather.gov/data/products/swl/00_sigwx_lo_us.gif"},
        {"SigWx", "SigWx Low Level 06HR", "https://www.aviationweather.gov/data/products/swl/06_sigwx_lo_us.gif"},
        {"SigWx", "SigWx Low Level 12HR", "https://www.aviationweather.gov/data/products/swl/12_sigwx_lo_us.gif"},
        {"SigWx", "SigWx Low Level 18HR", "https://www.aviationweather.gov/data/products/swl/18_sigwx_lo_us.gif"},
        {"SigWx", "SigWx Mid Level 00HR", "https://www.aviationweather.gov/data/products/swm/00_sigwx_mid_nat.gif"},
        {"SigWx", "SigWx Mid Level 06HR", "https://www.aviationweather.gov/data/products/swm/06_sigwx_mid_nat.gif"},
        {"SigWx", "SigWx Mid Level 12HR", "https://www.aviationweather.gov/data/products/swm/12_sigwx_mid_nat.gif"},
        {"SigWx", "SigWx Mid Level 18HR", "https://www.aviationweather.gov/data/products/swm/18_sigwx_mid_nat.gif"},
        {"Misc.", "Radar", "https://radar.weather.gov/ridge/standard/CONUS_loop.gif"},
        {"Misc.", "SIGMETs", "https://www.aviationweather.gov/data/products/sigmet/sigmet_all.gif"},
        {"AIRMET Tango", "AIRMET Tango 00HR", "https://www.aviationweather.gov/data/products/gairmet/F00_gairmet_tango_us.gif"},
        {"AIRMET Tango", "AIRMET Tango 03HR", "https://www.aviationweather.gov/data/products/gairmet/F03_gairmet_tango_us.gif"},
        {"AIRMET Tango", "AIRMET Tango 06HR", "https://www.aviationweather.gov/data/products/gairmet/F06_gairmet_tango_us.gif"},
        {"AIRMET Tango", "AIRMET Tango 09HR", "https://www.aviationweather.gov/data/products/gairmet/F09_gairmet_tango_us.gif"},
        {"AIRMET Tango", "AIRMET Tango 12HR", "https://www.aviationweather.gov/data/products/gairmet/F12_gairmet_tango_us.gif"},
        {"AIRMET Sierra", "AIRMET Sierra 00HR", "https://www.aviationweather.gov/data/products/gairmet/F00_gairmet_sierra_us.gif"},
        {"AIRMET Sierra", "AIRMET Sierra 03HR", "https://www.aviationweather.gov/data/products/gairmet/F03_gairmet_sierra_us.gif"},
        {"AIRMET Sierra", "AIRMET Sierra 06HR", "https://www.aviationweather.gov/data/products/gairmet/F06_gairmet_sierra_us.gif"},
        {"AIRMET Sierra", "AIRMET Sierra 09HR", "https://www.aviationweather.gov/data/products/gairmet/F09_gairmet_sierra_us.gif"},
        {"AIRMET Sierra", "AIRMET Sierra 12HR", "https://www.aviationweather.gov/data/products/gairmet/F12_gairmet_sierra_us.gif"},
        {"AIRMET Zulu", "AIRMET Zulu 00HR", "https://www.aviationweather.gov/data/products/gairmet/F00_gairmet_zulu-f_us.gif"},
        {"AIRMET Zulu", "AIRMET Zulu 03HR", "https://www.aviationweather.gov/data/products/gairmet/F03_gairmet_zulu-f_us.gif"},
        {"AIRMET Zulu", "AIRMET Zulu 06HR", "https://www.aviationweather.gov/data/products/gairmet/F06_gairmet_zulu-f_us.gif"},
        {"AIRMET Zulu", "AIRMET Zulu 09HR", "https://www.aviationweather.gov/data/products/gairmet/F09_gairmet_zulu-f_us.gif"},
        {"AIRMET Zulu", "AIRMET Zulu 12HR", "https://www.aviationweather.gov/data/products/gairmet/F12_gairmet_zulu-f_us.gif"},
        {"Surface Forecast", "Surface Forecast 03H", "https://www.aviationweather.gov/data/products/gfa/F03_gfa_sfc_us.png"},
        {"Surface Forecast", "Surface Forecast 06H", "https://www.aviationweather.gov/data/products/gfa/F06_gfa_sfc_us.png"},
        {"Surface Forecast", "Surface Forecast 09H", "https://www.aviationweather.gov/data/products/gfa/F09_gfa_sfc_us.png"},
        {"Surface Forecast", "Surface Forecast 12H", "https://www.aviationweather.gov/data/products/gfa/F12_gfa_sfc_us.png"},
        {"Surface Forecast", "Surface Forecast 15H", "https://www.aviationweather.gov/data/products/gfa/F15_gfa_sfc_us.png"},
        {"Surface Forecast", "Surface Forecast 18H", "https://www.aviationweather.gov/data/products/gfa/F18_gfa_sfc_us.png"},
        {"Clouds Forecast", "Clouds Forecast 03H", "https://www.aviationweather.gov/data/products/gfa/F03_gfa_clouds_us.png"},
        {"Clouds Forecast", "Clouds Forecast 06H", "https://www.aviationweather.gov/data/products/gfa/F06_gfa_clouds_us.png"},
        {"Clouds Forecast", "Clouds Forecast 09H", "https://www.aviationweather.gov/data/products/gfa/F09_gfa_clouds_us.png"},
        {"Clouds Forecast", "Clouds Forecast 12H", "https://www.aviationweather.gov/data/products/gfa/F12_gfa_clouds_us.png"},
        {"Clouds Forecast", "Clouds Forecast 15H", "https://www.aviationweather.gov/data/products/gfa/F15_gfa_clouds_us.png"},
        {"Clouds Forecast", "Clouds Forecast 18H", "https://www.aviationweather.gov/data/products/gfa/F18_gfa_clouds_us.png"},
        {"Winds/Temp", "Winds/Temp 5000 06HR", "https://aviationweather.gov/data/products/fax/F06_wind_050_b1.gif"},
        {"Winds/Temp", "Winds/Temp 10000 06HR", "https://aviationweather.gov/data/products/fax/F06_wind_100_b1.gif"},
        {"Winds/Temp", "Winds/Temp 18000 06HR", "https://aviationweather.gov/data/products/fax/F06_wind_180_b1.gif"},
        {"Winds/Temp", "Winds/Temp 24000 06HR", "https://aviationweather.gov/data/products/fax/F06_wind_240_b1.gif"},
        {"Winds/Temp", "Winds/Temp 30000 06HR", "https://aviationweather.gov/data/products/fax/F06_wind_300_b1.gif"},
        {"Winds/Temp", "Winds/Temp 5000 12HR", "https://aviationweather.gov/data/products/fax/F12_wind_050_b1.gif"},
        {"Winds/Temp", "Winds/Temp 10000 12HR", "https://aviationweather.gov/data/products/fax/F12_wind_100_b1.gif"},
        {"Winds/Temp", "Winds/Temp 18000 12HR", "https://aviationweather.gov/data/products/fax/F12_wind_180_b1.gif"},
        {"Winds/Temp", "Winds/Temp 24000 12HR", "https://aviationweather.gov/data/products/fax/F12_wind_240_b1.gif"},
        {"Winds/Temp", "Winds/Temp 30000 12HR", "https://aviationweather.gov/data/products/fax/F12_wind_300_b1.gif"},
    };

    private WebDocs() { }

    /** @return all internet pictures, in display order. */
    public static List<Item> all() {
        ArrayList<Item> list = new ArrayList<Item>();
        for (String[] d : DATA) {
            list.add(new Item(d[0], d[1], d[2]));
        }
        return list;
    }
}
