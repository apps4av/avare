package com.ds.avare.content;

import android.net.Uri;

/**
 * Created by zkhan on 3/13/17.
 */

public class LocationContract {

    public static final String AUTHORITY = "com.ds.avare.provider.location";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String BASE_AIRPORTS = "airports";
    public static final String BASE_AIRPORT_DIAGS = "airportdiags";
    public static final String BASE_AIRPORT_FREQ = "airportfreq";
    public static final String BASE_AIRPORT_AWOS = "awos";
    public static final String BASE_AIRPORT_RUNWAYS = "airportrunways";
    public static final String BASE_FIX = "fix";
    public static final String BASE_NAV = "nav";
    public static final String BASE_TAKEOFF = "takeoff";
    public static final String BASE_ALTERNATE = "alternate";
    public static final String BASE_AFD = "afd";
    public static final String BASE_SUA = "saa";
    public static final String BASE_AIRWAYS = "airways";
    public static final String BASE_NEAR = "near";

    public static final Uri CONTENT_URI_AIRPORTS = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRPORTS);
    public static final Uri CONTENT_URI_AIRPORT_DIAGS = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRPORT_DIAGS);
    public static final Uri CONTENT_URI_AIRPORT_FREQ = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRPORT_FREQ);
    public static final Uri CONTENT_URI_AIRPORT_AWOS = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRPORT_AWOS);
    public static final Uri CONTENT_URI_AIRPORT_RUNWAYS = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRPORT_RUNWAYS);
    public static final Uri CONTENT_URI_FIX = Uri.withAppendedPath(AUTHORITY_URI, BASE_FIX);
    public static final Uri CONTENT_URI_NAV = Uri.withAppendedPath(AUTHORITY_URI, BASE_NAV);
    public static final Uri CONTENT_URI_TAKEOFF = Uri.withAppendedPath(AUTHORITY_URI, BASE_TAKEOFF);
    public static final Uri CONTENT_URI_ALTERNATE = Uri.withAppendedPath(AUTHORITY_URI, BASE_ALTERNATE);
    public static final Uri CONTENT_URI_AFD = Uri.withAppendedPath(AUTHORITY_URI, BASE_AFD);
    public static final Uri CONTENT_URI_SUA = Uri.withAppendedPath(AUTHORITY_URI, BASE_SUA);
    public static final Uri CONTENT_URI_AIRWAYS = Uri.withAppendedPath(AUTHORITY_URI, BASE_AIRWAYS);
    public static final Uri CONTENT_URI_NEAR = Uri.withAppendedPath(AUTHORITY_URI, BASE_NEAR);

    public static final String TABLE_AIRPORTS = "airports";
    public static final String TABLE_AIRPORT_DIAGS = "airportdiags";
    public static final String TABLE_AIRPORT_FREQ = "airportfreq";
    public static final String TABLE_AIRPORT_AWOS = "awos";
    public static final String TABLE_AIRPORT_RUNWAYS = "airportrunways";
    public static final String TABLE_FIX = "fix";
    public static final String TABLE_NAV = "nav";
    public static final String TABLE_TAKEOFF = "takeoff";
    public static final String TABLE_ALTERNATE = "alternate";
    public static final String TABLE_AFD = "afd";
    public static final String TABLE_SUA = "saa";
    public static final String TABLE_AIRWAYS = "airways";


    public static final String AIRPORTS_LOCATION_ID = "LocationID";
    public static final String AIRPORTS_LATITUDE = "ARPLatitude";
    public static final String AIRPORTS_LONGITUDE = "ARPLongitude";
    public static final String AIRPORTS_TYPE = "Type";
    public static final String AIRPORTS_FACILITY_NAME = "FacilityName";
    public static final String AIRPORTS_USE = "Use";
    public static final String AIRPORTS_FSS_PHONE = "FSSPhone";
    public static final String AIRPORTS_MANAGER = "Manager";
    public static final String AIRPORTS_MANAGER_PHONE = "ManagerPhone";
    public static final String AIRPORTS_ELEVATION = "ARPElevation";
    public static final String AIRPORTS_VARIATION = "MagneticVariation";
    public static final String AIRPORTS_TPA = "TrafficPatternAltitude";
    public static final String AIRPORTS_FUEL_TYPES = "FuelTypes";
    public static final String AIRPORTS_CUSTOMS = "Customs";
    public static final String AIRPORTS_BEACON = "Beacon";
    public static final String AIRPORTS_LIGHT_SCHEDULE = "LightSchedule";
    public static final String AIRPORTS_SEGMENTED_CIRCLE = "SegCircle";
    public static final String AIRPORTS_ATCT = "ATCT";
    public static final String AIRPORTS_UNICOM_FREQUENCIES = "UNICOMFrequencies";
    public static final String AIRPORTS_CTAF_FREQUNCY = "CTAFFrequency";
    public static final String AIRPORTS_NON_COMMERCIAL_LANDING_FEE = "NonCommercialLandingFee";
    public static final String AIRPORTS_STATE = "State";
    public static final String AIRPORTS_CITY = "City";

    public static final String FIX_LOCATION_ID = "LocationID";
    public static final String FIX_LATITUDE = "ARPLatitude";
    public static final String FIX_LONGITUDE = "ARPLongitude";
    public static final String FIX_TYPE = "Type";
    public static final String FIX_FACILITY_NAME = "FacilityName";


    public static final String NAV_LOCATION_ID = "LocationID";
    public static final String NAV_LATITUDE = "ARPLatitude";
    public static final String NAV_LONGITUDE = "ARPLongitude";
    public static final String NAV_TYPE = "Type";
    public static final String NAV_FACILITY_NAME = "FacilityName";
    public static final String NAV_VARIATION = "Variation";
    public static final String NAV_CLASS = "Class";
    public static final String NAV_HIWAS = "Hiwas";
    public static final String NAV_ELEVATION = "Elevation";

    public static final String AIRWAY_NAME = "name";
    public static final String AIRWAY_SEQUENCE = "sequence";
    public static final String AIRWAY_LATITUDE = "Latitude";
    public static final String AIRWAY_LONGITUDE = "Longitude";

    public static final String AFD_LOCATION_ID = "LocationID";
    public static final String AFD_FILE = "File";

    public static final String TAKEOFF_LOCATION_ID = "LocationID";
    public static final String TAKEOFF_FILE = "File";

    public static final String ALTERNATE_LOCATION_ID = "LocationID";
    public static final String ALTERNATE_FILE = "File";

    public static final String SUA_DESIGNATOR = "designator";
    public static final String SUA_NAME = "name";
    public static final String SUA_UPPER_LIMIT = "upperlimit";
    public static final String SUA_LOWER_LIMIT = "lowerlimit";
    public static final String SUA_BEGIN_TIME = "begintime";
    public static final String SUA_END_TIME = "endtime";
    public static final String SUA_TIME_REFERENCE = "timeref";
    public static final String SUA_BEGIN_DAY = "beginday";
    public static final String SUA_END_DAY = "endday";
    public static final String SUA_DAY = "day";
    public static final String SUA_TX_FREQUENCY = "FreqTx";
    public static final String SUA_RX_FREQUENCY = "FreqRx";
    public static final String SUA_LATITUDE = "lat";
    public static final String SUA_LONGITUDE = "lon";

    public static final String AIRPORT_RUNWAYS_LOCATION_ID = "LocationID";
    public static final String AIRPORT_RUNWAYS_LENGTH = "Length";
    public static final String AIRPORT_RUNWAYS_WIDTH = "Width";
    public static final String AIRPORT_RUNWAYS_SURFACE = "Surface";
    public static final String AIRPORT_RUNWAYS_LE_IDENT = "LEIdent";
    public static final String AIRPORT_RUNWAYS_HE_IDENT = "HEIdent";
    public static final String AIRPORT_RUNWAYS_LE_LATITUDE = "LELatitude";
    public static final String AIRPORT_RUNWAYS_HE_LATITUDE = "HELatitude";
    public static final String AIRPORT_RUNWAYS_LE_LONGITUDE = "LELongitude";
    public static final String AIRPORT_RUNWAYS_HE_LONGITUDE = "HELongitude";
    public static final String AIRPORT_RUNWAYS_LE_ELEVATION = "LEElevation";
    public static final String AIRPORT_RUNWAYS_HE_ELEVATION = "HEElevation";
    public static final String AIRPORT_RUNWAYS_LE_HEADING = "LEHeadingT"; // fix db
    public static final String AIRPORT_RUNWAYS_HE_HEADING = "HEHeading";
    public static final String AIRPORT_RUNWAYS_LE_DT = "LEDT";
    public static final String AIRPORT_RUNWAYS_HE_DT = "HEDT";
    public static final String AIRPORT_RUNWAYS_LE_LIGHTS = "LELights";
    public static final String AIRPORT_RUNWAYS_HE_LIGHTS = "HELights";
    public static final String AIRPORT_RUNWAYS_LE_ILS = "LEILS";
    public static final String AIRPORT_RUNWAYS_HE_ILS = "HEILS";
    public static final String AIRPORT_RUNWAYS_LE_VGSI = "LEVGSI";
    public static final String AIRPORT_RUNWAYS_HE_VGSI = "HEVGSI";
    public static final String AIRPORT_RUNWAYS_LE_PATTERN = "LEPattern";
    public static final String AIRPORT_RUNWAYS_HE_PATTERN = "HEPattern";


    public static final String AIRPORT_DIAGS_LOCATION_ID  = "LocationID";
    public static final String AIRPORT_DIAGS_TRANSFORM0   = "tfwA";
    public static final String AIRPORT_DIAGS_TRANSFORM1   = "tfwB";
    public static final String AIRPORT_DIAGS_TRANSFORM2   = "tfwC";
    public static final String AIRPORT_DIAGS_TRANSFORM3   = "tfwD";
    public static final String AIRPORT_DIAGS_TRANSFORM4   = "tfwE";
    public static final String AIRPORT_DIAGS_TRANSFORM5   = "tfwF";
    public static final String AIRPORT_DIAGS_TRANSFORMI0  = "wftA";
    public static final String AIRPORT_DIAGS_TRANSFORMI1  = "wftB";
    public static final String AIRPORT_DIAGS_TRANSFORMI2  = "wftC";
    public static final String AIRPORT_DIAGS_TRANSFORMI3  = "wftD";
    public static final String AIRPORT_DIAGS_TRANSFORMI4  = "wftE";
    public static final String AIRPORT_DIAGS_TRANSFORMI5  = "wftF";

    public static final String AIRPORT_FREQ_LOCATION_ID  = "LocationID";
    public static final String AIRPORT_FREQ_TYPE  = "Type";
    public static final String AIRPORT_FREQ_FREQUENCY  = "Freq";

    public static final String AIRPORT_AWOS_LOCATION_ID  = "LocationID";
    public static final String AIRPORT_AWOS_TYPE  = "Type";
    public static final String AIRPORT_AWOS_STATUS  = "Status";
    public static final String AIRPORT_AWOS_LATITUDE  = "Latitude";
    public static final String AIRPORT_AWOS_LONGITUDE  = "Longitude";
    public static final String AIRPORT_AWOS_ELEVATION  = "Elevation";
    public static final String AIRPORT_AWOS_FREQUENCY1  = "Frequency1";
    public static final String AIRPORT_AWOS_FREQUENCY2  = "Frequency2";
    public static final String AIRPORT_AWOS_TELEPHONE1  = "Telephone1";
    public static final String AIRPORT_AWOS_TELEPHONE2  = "Telephone2";
    public static final String AIRPORT_AWOS_REMARK  = "Remark";

}
