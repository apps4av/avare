/*
jWeather(TM) is a Java library for parsing raw weather data
Copyright (C) 2004 David Castro

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information, please email arimus@users.sourceforge.net
*/
package net.sf.jweather.metar;

/**
 * Simple container for METAR weather token constants.
 * 
 * @author dennis@bullamanka.com
 */
public interface MetarConstants {
	/** Metar string value for fully automated report ('AUTO') */
	public static final String METAR_AUTOMATED = "AUTO";
	/** Metar string value for corrected report ('COR') */
	public static final String METAR_CORRECTED = "COR";
	/** Metar string value for Vertical Visibility ('VV') */
	public static final String METAR_VERTICAL_VISIBILITY = "VV";
	/** Metar string value for Sky Clear ('SKC') */
	public static final String METAR_SKY_CLEAR = "SKC";
	/** Metar string value for Clear ('CLR') */
	public static final String METAR_CLEAR = "CLR";
	/** Metar string value for Few ('FEW') */
	public static final String METAR_FEW = "FEW";
	/** Metar string value for Scattered ('SCT') */
	public static final String METAR_SCATTERED = "SCT";
	/** Metar string value for Broken ('BKN') */
	public static final String METAR_BROKEN = "BKN";
	/** Metar string value for Overcast ('OVC') */
	public static final String METAR_OVERCAST = "OVC";
	/** Metar string value for Cumulonimbus ('CB') */
	public static final String METAR_CUMULONIMBUS = "CB";
	/** Metar string value for Towering Cumulus ('TCU') */
	public static final String METAR_TOWERING_CUMULUS = "TCU";
	/** Metar string value for Heavy ('+') */
	public static final String METAR_HEAVY = "+";
	/** Metar string value for Light ('-') */
	public static final String METAR_LIGHT = "-";
	/** Metar string value for Shallow ('MI') */
	public static final String METAR_SHALLOW = "MI";
	/** Metar string value for Partial ('PR') */
	public static final String METAR_PARTIAL = "PR";
	/** Metar string value for Patches ('BC') */
	public static final String METAR_PATCHES = "BC";
	/** Metar string value for LowDrifting ('DR') */
	public static final String METAR_LOW_DRIFTING = "DR";
	/** Metar string value for Blowing ('BL') */
	public static final String METAR_BLOWING = "BL";
	/** Metar string value for Showers ('SH') */
	public static final String METAR_SHOWERS = "SH";
	/** Metar string value for Thunderstorms ('TS') */
	public static final String METAR_THUNDERSTORMS = "TS";
	/** Metar string value for Freezing ('FZ') */
	public static final String METAR_FREEZING = "FZ";
	/** Metar string value for Drizzle ('DZ') */
	public static final String METAR_DRIZZLE = "DZ";
	/** Metar string value for Rain ('RA') */
	public static final String METAR_RAIN = "RA";
	/** Metar string value for Snow ('SN') */
	public static final String METAR_SNOW = "SN";
	/** Metar string value for Snow Grains ('SG') */
	public static final String METAR_SNOW_GRAINS = "SG";
	/** Metar string value for Ice Crystals ('IC') */
	public static final String METAR_ICE_CRYSTALS = "IC";
	/** Metar string value for Ice Pellets ('PL') */
	public static final String METAR_ICE_PELLETS = "PL";
	/** Metar string value for Hail ('GR') */
	public static final String METAR_HAIL = "GR";
	/** Metar string value for Small Hail ('GS') */
	public static final String METAR_SMALL_HAIL = "GS";
	/** Metar string value for Unknown Precip ('UP') */
	public static final String METAR_UNKNOWN_PRECIPITATION = "UP";
	/** Metar string value for Mist ('BR') */
	public static final String METAR_MIST = "BR";
	/** Metar string value for Fog ('FG') */
	public static final String METAR_FOG = "FG";
	/** Metar string value for Smoke ('FU') */
	public static final String METAR_SMOKE = "FU";
	/** Metar string value for Volcanic Ash ('VA') */
	public static final String METAR_VOLCANIC_ASH = "VA";
	/** Metar string value for Widespread Dust ('DU') */
	public static final String METAR_WIDESPREAD_DUST = "DU";
	/** Metar string value for Sand ('SA') */
	public static final String METAR_SAND = "SA";
	/** Metar string value for Haze ('HZ') */
	public static final String METAR_HAZE = "HZ";
	/** Metar string value for Spray ('PY') */
	public static final String METAR_SPRAY = "PY";
	/** Metar string value for Dust Sand Whirls ('PO') */
	public static final String METAR_DUST_SAND_WHIRLS = "PO";
	/** Metar string value for Squalls ('SQ') */
	public static final String METAR_SQUALLS = "SQ";
	/** Metar string value for Funnel Cloud ('FC') */
	public static final String METAR_FUNNEL_CLOUD = "FC";
	/** Metar string value for Sand Storm ('SS') */
	public static final String METAR_SAND_STORM = "SS";
	/** Metar string value for Dust Storm ('DS') */
	public static final String METAR_DUST_STORM = "DS";
	/** Metar string value for Remarks ('RMK') */
	public static final String METAR_REMARKS = "RMK";

	/** Metar string value for Clouds and Visibility Okay ('CAVOK') */
	public static final String METAR_CAVOK = "CAVOK";
	/** Metar string value for No Significant Change ('NOSIG') */
	public static final String METAR_NO_SIGNIFICANT_CHANGE = "NOSIG";
	/** Metar string value for No Significant Clouds ('NSC') */
	public static final String METAR_NO_SIGNIFICANT_CLOUDS = "NSC";
	

	/** Metar decoded string value for Vertical Visibility ('VV') */
	public static final String METAR_DECODED_VERTICAL_VISIBILITY = "Vertical Visibility";
	/** Metar decoded string value for Sky Clear ('SKC') */
	public static final String METAR_DECODED_SKY_CLEAR = "Sky Clear";
	/** Metar decoded string value for Clear ('CLR') */
	public static final String METAR_DECODED_CLEAR = "Clear";
	/** Metar decoded string value for Few ('FEW') */
	public static final String METAR_DECODED_FEW = "Few";
	/** Metar decoded string value for Scattered ('SCT') */
	public static final String METAR_DECODED_SCATTERED = "Scattered";
	/** Metar decoded string value for Broken ('BKN') */
	public static final String METAR_DECODED_BROKEN = "Broken";
	/** Metar decoded string value for Overcase ('OVC') */
	public static final String METAR_DECODED_OVERCAST = "Overcast";
	/** Metar decoded string value for Cumulonimbus ('CB') */
	public static final String METAR_DECODED_CUMULONIMBUS = "Cumulonimbus";
	/** Metar decoded string value for Towering Cumulonimbus ('TCU') */
	public static final String METAR_DECODED_TOWERING_CUMULONIMBUS = "Tower Cumulonimbus";
	/** Metar decoded string value for Severe */
	 public static final String METAR_DECODED_SEVERE = "Severe";
	/** Metar decoded string value for Heavy ('+') */
	 public static final String METAR_DECODED_HEAVY = "Heavy";
	 /** Metar decoded string value for Light ('-') */
	 public static final String METAR_DECODED_LIGHT = "Light";
	/** Metar decoded string value for Slight */
	public static final String METAR_DECODED_SLIGHT = "Slight";
	/** Metar decoded string value for Light ('-') */
	public static final String METAR_DECODED_MODERATE = "Moderate";	
	/** Metar decoded string value for Shallow ('MI') */
	public static final String METAR_DECODED_SHALLOW = "Shallow";	
	/** Metar decoded string value for Partial ('PR') */
	public static final String METAR_DECODED_PARTIAL = "Partial";
	/** Metar decoded string value for Patches ('BC') */
	public static final String METAR_DECODED_PATCHES = "Patches";
	/** Metar decoded string value for LowDrifting ('DR') */
	public static final String METAR_DECODED_LOW_DRIFTING = "Low Drifting";
	/** Metar decoded string value for Blowing ('BL') */
	public static final String METAR_DECODED_BLOWING = "Blowing";
	/** Metar decoded string value for Showers ('SH') */
	public static final String METAR_DECODED_SHOWERS = "Showers";
	/** Metar decoded string value for Thunderstorms ('TS') */
	public static final String METAR_DECODED_THUNDERSTORMS = "Thunderstorms";
	/** Metar decoded string value for Freezing ('FZ') */
	public static final String METAR_DECODED_FREEZING = "Freezing";
	/** Metar decoded string value for Drizzle ('DZ') */
	public static final String METAR_DECODED_DRIZZLE = "Drizzle";
	/** Metar decoded string value for Rain ('RA') */
	public static final String METAR_DECODED_RAIN = "Rain";
	/** Metar decoded string value for Snow ('SN') */
	public static final String METAR_DECODED_SNOW = "Snow";
	/** Metar decoded string value for Snow Grains ('SG') */
	public static final String METAR_DECODED_SNOW_GRAINS = "Snow Grains";
	/** Metar decoded string value for Ice Crystals ('IC') */
	public static final String METAR_DECODED_ICE_CRYSTALS = "Ice Crystals";
	/** Metar decoded string value for Ice Pellets ('PL') */
	public static final String METAR_DECODED_ICE_PELLETS = "Ice Pellets";
	/** Metar decoded string value for Hail ('GR') */
	public static final String METAR_DECODED_HAIL = "Hail";
	/** Metar decoded string value for Small Hail ('GS') */
	public static final String METAR_DECODED_SMALL_HAIL = "Small Hail";
	/** Metar decoded string value for Unknown Precip ('UP') */
	public static final String METAR_DECODED_UNKNOWN_PRECIP = "Unknown Precip";
	/** Metar decoded string value for Mist ('BR') */
	public static final String METAR_DECODED_MIST = "Mist";
	/** Metar decoded string value for Fog ('FG') */
	public static final String METAR_DECODED_FOG = "Fog";
	/** Metar decoded string value for Smoke ('FU') */
	public static final String METAR_DECODED_SMOKE = "Smoke";
	/** Metar decoded string value for Volcanic Ash ('VA') */
	public static final String METAR_DECODED_VOLCANIC_ASH = "Volcanic Ash";
	/** Metar decoded string value for Widespread Dust ('DU') */
	public static final String METAR_DECODED_WIDESPREAD_DUST = "Widespread Dust";
	/** Metar decoded string value for Sand ('SA') */
	public static final String METAR_DECODED_SAND = "Sand";
	/** Metar decoded string value for Haze ('HZ') */
	public static final String METAR_DECODED_HAZE = "Haze";
	/** Metar decoded string value for Spray ('PY') */
	public static final String METAR_DECODED_SPRAY = "Spray";
	/** Metar decoded string value for Dust Sand Whirls ('PO') */
	public static final String METAR_DECODED_DUST_SAND_WHIRLS = "Dust Sand Whirls";
	/** Metar decoded string value for Squalls ('SQ') */
	public static final String METAR_DECODED_SQUALLS = "Squalls";
	/** Metar decoded string value for Funnel Cloud ('FC') */
	public static final String METAR_DECODED_FUNNEL_CLOUD = "Funnel Cloud";
	/** Metar decoded string value for Sand Storm ('SS') */
	public static final String METAR_DECODED_SAND_STORM = "Sand Storm";
	/** Metar decoded string value for Dust Storm ('DS') */
	public static final String METAR_DECODED_DUST_STORM = "Dust Storm";	

	/** Metar decoded string value for Clouds and Visibility Okay ('CAVOK') */
	public static final String METAR_DECODED_CAVOK = "Clouds and Visibility Okay";
	/** Metar decoded string value for No Significant Change ('NOSIG') */
	public static final String METAR_DECODED_NO_SIGNIFICANT_CHANGE = "No significant change";
	/** Metar decoded string value for No Significant Clouds ('NSC') */
	public static final String METAR_DECODED_NO_SIGNIFICANT_CLOUDS = "No significant clouds";
}
