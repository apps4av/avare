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

import com.googlecode.jcsv.annotations.MapToColumn;

/**
 * Auto generated from CSV header
 * 
 * XXX: Violates coding standrad by adding _ in names
 * @author zkhan
 *
 */
public class Airep {

    @MapToColumn(column=0)
    public String receipt_time;
    @MapToColumn(column=1)
    public String observation_time;
    @MapToColumn(column=2)
    public String mid_point_assumed;
    @MapToColumn(column=3)
    public String no_time_stamp;
    @MapToColumn(column=4)
    public String flt_lvl_range;
    @MapToColumn(column=5)
    public String above_ground_level_indicated;
    @MapToColumn(column=6)
    public String no_flt_lvl;
    @MapToColumn(column=7)
    public String bad_location;
    @MapToColumn(column=8)
    public String aircraft_ref;
    @MapToColumn(column=9)
    public String latitude;
    @MapToColumn(column=10)
    public String longitude;
    @MapToColumn(column=11)
    public String altitude_ft_msl;
    @MapToColumn(column=12)
    public String sky_cover;
    @MapToColumn(column=13)
    public String cloud_base_ft_msl;
    @MapToColumn(column=14)
    public String cloud_top_ft_msl;
    @MapToColumn(column=15)
    public String sky_cover_2;
    @MapToColumn(column=16)
    public String cloud_base_ft_msl_2;
    @MapToColumn(column=17)
    public String cloud_top_ft_msl_2;
    @MapToColumn(column=18)
    public String turbulence_type;
    @MapToColumn(column=19)
    public String turbulence_intensity;
    @MapToColumn(column=20)
    public String turbulence_base_ft_msl;
    @MapToColumn(column=21)
    public String turbulence_top_ft_msl;
    @MapToColumn(column=22)
    public String turbulence_freq;
    @MapToColumn(column=23)
    public String turbulence_type_2;
    @MapToColumn(column=24)
    public String turbulence_intensity_2;
    @MapToColumn(column=25)
    public String turbulence_base_ft_msl_2;
    @MapToColumn(column=26)
    public String turbulence_top_ft_msl_2;
    @MapToColumn(column=27)
    public String turbulence_freq_2;
    @MapToColumn(column=28)
    public String icing_type;
    @MapToColumn(column=29)
    public String icing_intensity;
    @MapToColumn(column=30)
    public String icing_base_ft_msl;
    @MapToColumn(column=31)
    public String icing_top_ft_msl;
    @MapToColumn(column=32)
    public String icing_type_2;
    @MapToColumn(column=33)
    public String icing_intensity_2;
    @MapToColumn(column=34)
    public String icing_base_ft_msl_2;
    @MapToColumn(column=35)
    public String icing_top_ft_msl_2;
    @MapToColumn(column=36)
    public String visibility_statute_mi;
    @MapToColumn(column=37)
    public String wx_string;
    @MapToColumn(column=38)
    public String temp_c;
    @MapToColumn(column=39)
    public String wind_dir_degrees;
    @MapToColumn(column=40)
    public String wind_speed_kt;
    @MapToColumn(column=41)
    public String vert_gust_kt;
    @MapToColumn(column=42)
    public String report_type;
    @MapToColumn(column=43)
    public String raw_text;    
}


