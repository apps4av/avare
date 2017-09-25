package com.ds.avare.content;

import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class WeatherContract {

    public static final String AUTHORITY = "com.ds.avare.provider.weather";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE_AIRMET = "airsig";
    public static final String BASE_PIREP = "apirep";
    public static final String BASE_TAF = "tafs";
    public static final String BASE_METAR = "metars";
    public static final String BASE_WIND = "wa";

    public static final Uri CONTENT_URI_AIRMET = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRMET);
    public static final Uri CONTENT_URI_PIREP = Uri.withAppendedPath(AUTHORITY_URI, BASE_PIREP);
    public static final Uri CONTENT_URI_TAF = Uri.withAppendedPath(AUTHORITY_URI, BASE_TAF);
    public static final Uri CONTENT_URI_METAR = Uri.withAppendedPath(AUTHORITY_URI, BASE_METAR);
    public static final Uri CONTENT_URI_WIND = Uri.withAppendedPath(AUTHORITY_URI, BASE_WIND);

    public static final String TABLE_AIRMET = "airsig";
    public static final String TABLE_PIREP = "apirep";
    public static final String TABLE_TAF = "tafs";
    public static final String TABLE_METAR = "metars";
    public static final String TABLE_WIND = "wa";

    public static final String AIRMET_TEXT = "raw_text";
    public static final String AIRMET_TIME_FROM = "valid_time_from";
    public static final String AIRMET_TIME_TO = "valid_time_to";
    public static final String AIRMET_POINTS = "point";
    public static final String AIRMET_MSL_MIN = "min_ft_msl";
    public static final String AIRMET_MSL_MAX = "max_ft_msl";
    public static final String AIRMET_MOVEMENT_DIRECTION = "movement_dir_degrees";
    public static final String AIRMET_MOVEMENT_SPEED = "movement_speed_kt";
    public static final String AIRMET_HAZARD = "hazard";
    public static final String AIRMET_SEVERITY = "severity";
    public static final String AIRMET_TYPE = "airsigmet_type";

    public static final String PIREP_TEXT = "raw_text";
    public static final String PIREP_TIME = "observation_time";
    public static final String PIREP_LONGITUDE = "longitude";
    public static final String PIREP_LATITUDE = "latitude";
    public static final String PIREP_TYPE = "report_type";

    public static final String TAF_TEXT = "raw_text";
    public static final String TAF_TIME = "issue_time";
    public static final String TAF_STATION = "station_id";

    public static final String METAR_TEXT = "raw_text";
    public static final String METAR_TIME = "issue_time";
    public static final String METAR_STATION = "station_id";
    public static final String METAR_FLIGHT_CATEGORY = "flight_category";
    public static final String METAR_LONGITUDE = "longitude";
    public static final String METAR_LATITUDE = "latitude";

    public static final String WIND_STATION = "stationid";
    public static final String WIND_TIME = "valid";
    public static final String WIND_LONGITUDE = "longitude";
    public static final String WIND_LATITUDE = "latitude";
    public static final String WIND_3K = "w3k";
    public static final String WIND_6K = "w6k";
    public static final String WIND_9K = "w9k";
    public static final String WIND_12K = "w12k";
    public static final String WIND_18K = "w18k";
    public static final String WIND_24K = "w24k";
    public static final String WIND_30K = "w30k";
    public static final String WIND_34K = "w34k";
    public static final String WIND_39K = "w39k";

}
