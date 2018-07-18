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

import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/*
 * examples:
 *  KCNO 060653Z 32004KT 10SM BKN043 13/11 A2993 RMK AO2 SLP133 T01280106
 *  KCNO 070353Z AUTO 29009KT 10SM CLR 13/11 A2991 RMK AO2 SLP127 T01280106
 *  KCNO 071853Z 24010KT 10SM BKN038 OVC048 17/09 A2998 RMK AO2 SLP147 T01670089
 *  KCNO 231653Z VRB04KT 1 3/4SM HZ BKN010 18/15 A2997 RMK AO2 SLP145 HZ FEW000 T01780150
 *  KCNO 291753Z 26006KT 4SM HZ CLR A2991 RMK AO2 SLPNO 57007
 *
 * Body of report:
 *  (1)  Type of report - METAR/SPECI
 *  (2)  Station Identifier - CCCC
 *  (3)  Date and Time of Report (UTC) - YYGGggZ
 *  (4)  Report Modifier - AUTO/COR
 *  (5)  Wind - ddff(f)Gf f (f )KT_d d d Vd d d
 *                       m m  m     n n n  x x x
 *  (6)  Visibility - VVVVVSM
 *  (7)  Runaway Visual Range - RD D /V V V V FT  or  RD D /V V V V VV V V V FT
 *                                r r  r r r r          r r  n n n n  x x x x
 *  (8)  Present Weather - w'w'
 *  (9)  Sky Condition - N N N h h h  or  VVh h h  or  SKC/CLR
 *                        s s s s s s        s s s
 *  (10) Temperature and Dew Point - T'T'/T' T'
 *                                          d  d
 *  (11) Altimeter - AP P P P
 *                     h h h h
 * Remarks section of report:
 *  (1)  Automated, Manual, Plain Language
 *  (2)  Additive and Maintenance Data
 *
 * *note: '_' denotes a required space
 *
 *
 * Table 12-2 Present Weather
 *
 * _________________________________________________________________________________
 * | Intensity  |   Descriptor  |   Precipitation  |   Obscuration |   Other       |
 * +------------+---------------+------------------+---------------+---------------+
 * | - Light    | MI Shallow    | DZ Drizzle       | BR Mist       | PO Well-      |
 * |   Moderate | PR Partial    | RA Rain          | FG Fog        |    Developed  |
 * | + Heavy    | BC Patches    | SN Snow          | FU Smoke      |    Dust/Sand  |
 * |            | DR Low        | SG Snow Grains   | VA Volcanic   |    Whirls     |
 * |            |    Drifting   | IC Ice Crystals  |    Ash        | SQ Squalls    |
 * |            | BL Blowing    | PL Ice Pellets   | DU Widespread | FC Funnel     |
 * |            | SH Shower(s)  | GR Hail          |    Dust       |    Cloud,     |
 * |            | TS Thunder-   | GS Small Hail    | SA Sand       |    Tornado,   |
 * |            |    storm      |    and/or        | HZ Haze       |    Waterspout |
 * |            | FZ Freezing   |    Snow Pellets  | PY Spray      | SS Sandstorm  |
 * |            |               | UP Unknown       |               | DS Duststorm  |
 * |            |               |    Precipitation |               |               |
 * +------------+---------------+------------------+---------------+---------------+
 *
 * up to 3 weather groups can be reported
 */

/**
 * Responsible for parsing raw METAR data and providing methods for accessing
 * the data
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.11 $
 * @see <a href="Weather.html">Weather</a>
 * @see <a href="Obscuration.html">Obscuration</a>
 * @see <a href="RunwayVisualRange.html">RunwayVisualRange</a>
 * @see <a href="SkyCondition.html">SkyCondition</a>
 * @see <a href="WeatherCondition.html">WeatherCondition</a>
 */
public class MetarParser {
	private static Perl5Util utility = new Perl5Util();
	private static Perl5Matcher matcher = new Perl5Matcher();

	private ArrayList tokens = new ArrayList();

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	private static TimeZone gmtZone = TimeZone.getTimeZone("GMT");

	private WeatherCondition weatherCondition = null;
	private SkyCondition skyCondition = null;
	private RunwayVisualRange runwayVisualRange = null;
	private Obscuration obscuration = null;

	private int index = 0;
	private int numTokens = 0;
	private String temp = null;

	static {
		sdf.setTimeZone(gmtZone);
	}

	public static Metar parse(String metarData) throws MetarParseException {
		MetarParser mp = new MetarParser();
		return mp.parseData(metarData);
	}

	private Metar parseData(String metarData) throws MetarParseException {


		if (metarData == null) {
			throw new MetarParseException("empty metar data");
		}



		Metar metar = new Metar();

		// test data
		//metarData += "KCNO 070353Z AUTO 29009KT 1 1/2SM R01L/0800V1600FT CLR 13/11 A2991 RMK AO2 SLP127 T01280106\n";

		// split the two lines of raw metar data apart


		// split the second line, the METAR data, on whitespace into tokens for
		// processing
		try {
			utility.split(tokens, ((String)metarData));
		} catch(MalformedPerl5PatternException e) {

			throw new MetarParseException("error spliting metar data on whitespace: "+e);
		}

		// the number of tokens we have
		numTokens = tokens.size();


		// type of report should be present (METAR/SPECI)???

		// station id will always be present in
		// format: CCCC
		//     CCCC - alphabetic characters only [a-zA-Z]
		metar.setStationID((String)tokens.get(index++));




		// date and time of the report
		// format: YYGGggZ
		//     YY - date
		//     GG - hours
		//     gg - minutes
		//     Z  - Zulu (UTC)
		if (((String)tokens.get(index)).endsWith("Z")) {
			// steal year and month from date string
			Calendar calendar = Calendar.getInstance(gmtZone);




			int dayInt, hourInt, minuteInt;
			try {
				String day = ((String)tokens.get(index)).substring(0,2);


				String hour = ((String)tokens.get(index)).substring(2,4);


				String minute = ((String)tokens.get(index)).substring(4,6);


				dayInt = new Integer(day).intValue();
				hourInt = new Integer(hour).intValue();
				minuteInt = new Integer(minute).intValue();

				// case where the month may have rolled. In this case, the
				// calendar should be rolled back one day
				if (dayInt > calendar.get(Calendar.DAY_OF_MONTH)) {
					calendar.roll(Calendar.DAY_OF_MONTH, false);
				}

				calendar.set(Calendar.DAY_OF_MONTH, dayInt);
				calendar.set(Calendar.HOUR_OF_DAY, hourInt);
				calendar.set(Calendar.MINUTE, minuteInt);
			} catch (NumberFormatException nfe) {

				throw new MetarParseException("unable to parse Metar date value: "+nfe);
			}

			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);

			metar.setDate(calendar.getTime());

			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}

		} else {

			// unexpected token...should have been data in Zulu (UTC)
		}



		// report modifier
		// format: (AUTO or COR)
		//     AUTO - fully automated with no human intervention or oversight
		//     COR  - corrected report
		if (((String)tokens.get(index)).equals(MetarConstants.METAR_AUTOMATED) ||
		    ((String)tokens.get(index)).equals(MetarConstants.METAR_CORRECTED))
		{
			metar.setReportModifier((String)tokens.get(index));
			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}

		} else {

		}



		// wind group (speed and direction)
		// format: dddff(f)Gf f (f )KT_d d d Vd d d
		//                   m m  m     n n n  x x x
		//     ddd           - wind direction (may be VRB (variable))
		//     ff(f)         - wind speed
		//     Gf f (f )     - wind gust speed
		//       m m  m
		//     KT (or) MPS   - knots (or) meters per second
		//     d d d Vd d d  - variable wind direction > 6 knots, degree=>degree
		//      n n n  x x x   e.g. 180V210 => variable from 180deg to 210deg

		temp = (String)tokens.get(index);
		if (temp.endsWith("KT") || temp.endsWith("MPS")) {
			int pos = 0;
			boolean windInKnots = false;

			if (temp.endsWith("KT")) {

				windInKnots = true;
			} else {

			}

			if (!((String)tokens.get(index)).substring(0,3).equals("VRB")) {
				// we have gusts
				Integer windDirection = new Integer(((String)tokens.get(index)).substring(0,3));
				metar.setWindDirection(windDirection);
			} else {

				metar.setWindDirectionIsVariable(true);
			}

			temp = ((String)tokens.get(index)).substring(5,5);
			try {
				if (matcher.matches(temp, new Perl5Compiler().compile("\\d"))) {
					// have three-digit wind speed

					if (windInKnots) {
						metar.setWindSpeed(new Float(((String)tokens.get(index)).substring(3,6)));
					} else {
						metar.setWindSpeedInMPS(new Float(((String)tokens.get(index)).substring(3,6)));
					}
					pos = 6;
				} else {
					// have two-digit wind speed

					if (windInKnots) {
						metar.setWindSpeed(new Float(((String)tokens.get(index)).substring(3,5)));
					} else {
						metar.setWindSpeedInMPS(new Float(((String)tokens.get(index)).substring(3,5)));
					}
					pos = 5;
				}
			} catch(MalformedPatternException e) {

			}

			if (((String)tokens.get(index)).charAt(pos) == 'G') {
				// we have wind gusts

				pos++;

				temp = ((String)tokens.get(index)).substring(pos+2,pos+2);
				//if (((String)tokens.get(index)).substring(pos+2,pos+2).matches("\\d")) {
				try {
					if (matcher.matches(temp, new Perl5Compiler().compile("\\d"))) {
						// have three-digit wind speed

						if (windInKnots) {
							metar.setWindGusts(new Float(((String)tokens.get(index)).substring(pos,pos+3)));
						} else {
							metar.setWindGustsInMPS(new Float(((String)tokens.get(index)).substring(pos,pos+3)));
						}
					} else {
						// have two-digit wind speed

						if (windInKnots) {
							metar.setWindGusts(new Float(((String)tokens.get(index)).substring(pos,pos+2)));
						} else {
							metar.setWindGustsInMPS(new Float(((String)tokens.get(index)).substring(pos,pos+2)));
						}
					}
				} catch(MalformedPatternException e) {

				}
			} else {
				// we don't have gusts

			}


			if (windInKnots) {

			} else {

			}


			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}


			// if we have variable wind direction
			temp = ((String)tokens.get(index));
			try {
				if (matcher.matches(temp, new Perl5Compiler().compile(".*\\d\\d\\dV\\d\\d\\d")))
				{
					metar.setWindDirectionIsVariable(true);

					metar.setWindDirectionMin(new Integer(((String)tokens.get(index)).substring(0,3)));
					metar.setWindDirectionMax(new Integer(((String)tokens.get(index)).substring(4,7)));




					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}
				}
			} catch(MalformedPatternException e) {

			}
		} else {
			// unexpected token...should have been wind speed

		}



		try {
			// CAVOK
			//
			// Visibility greater than 10Km, no cloud below 5000 ft or minimum
			// sector altitude, whichever is the lowest and no CB (Cumulonimbus) or
			// over development and no significant weather.
			if (((String)tokens.get(index)).equals(MetarConstants.METAR_CAVOK)) {
				metar.setIsCavok(true);

				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}
			// Horizontal visibility in meters
			} else if (matcher.matches(((String)tokens.get(index)), new Perl5Compiler().compile("/^(\\d+)$/"))) {
				int tmp = Integer.parseInt(matcher.getMatch().toString());
				metar.setVisibilityInMeters(new Float(tmp));

				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}
			// Horizontal visibility of 10Km and above
			} else if (((String)tokens.get(index)).equals("9999")) {
				metar.setVisibilityInKilometers(new Float(10));

				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}

			// get visibility
			// format: (M)VVVVVSM
			//     (M)   - used to indicate less than
			//     VVVVV - miles (00001SM)
			//     SM    - statute miles
			} else if (((String)tokens.get(index)).endsWith("SM") ||
					   ((index+1 < numTokens) && ((String)tokens.get(index+1)).endsWith("SM")) ||
					   ((String)tokens.get(index)).endsWith("KM") ||
					   ((index+1 < numTokens) && ((String)tokens.get(index+1)).endsWith("KM")))
			{


				String whole, fraction = "";
				Float visibility = null;
				boolean isLessThan = false;
				String token = (String)tokens.get(index);
				boolean visibilityInStatuteMiles = false;

				if (((String)tokens.get(index)).endsWith("SM") ||
					((index+1 < numTokens) && ((String)tokens.get(index+1)).endsWith("SM")))
				{
					visibilityInStatuteMiles = true;
				}

				if (token.startsWith("M")) {

					isLessThan = true;
					token = token.substring(1, token.length());
				}

				if (token.endsWith("SM") || token.endsWith("KM")) {
					if (token.indexOf('/') == -1) {
						// no fractions to deal with
						whole = token.substring(0, token.length()-2);
					} else {
						whole = "0";
						fraction = token.substring(0, token.length()-2);
					}
				} else {
					whole = token;
					// next token is the fraction part
					index++;
					fraction = ((String)tokens.get(index)).substring(0,((String)tokens.get(index)).length()-2);
				}

				visibility = new Float(whole);

				if (!fraction.equals("")) {
					// we have a fraction to convert
					ArrayList frac = new ArrayList();
					try {
						utility.split(frac, "/\\//", fraction);
					} catch(MalformedPerl5PatternException e) {

						throw new MetarParseException("error spliting fraction on /: "+e);
					}

					visibility = new Float(visibility.floatValue() + new Float((String)frac.get(0)).floatValue() / new Float((String)frac.get(1)).floatValue());
				}

				if (visibilityInStatuteMiles) {
					metar.setVisibility(visibility);
				} else {
					metar.setVisibilityInKilometers(visibility);
				}
				metar.setVisibilityLessThan(isLessThan);

				// on to the next token
				if (index < numTokens - 1) {
					index++;
				}

			} else {
				String token = (String)tokens.get(index);
				boolean isLessThan = false;

				if (utility.match("/M?\\d+/", token)) {


					if (token.startsWith("M")) {

						isLessThan = true;
						token = token.substring(1, token.length());
					}

					metar.setVisibilityInMeters(new Float(token));
					metar.setVisibilityLessThan(isLessThan);

					// on to the next token
					if (index < numTokens - 1) {
						index++;
					}
				} else {
					// unexpected token...should have been visibility

				}
			}
		} catch(MalformedPatternException e) {

		}



		// see if we have a Runaway Visual Range Group token
		// format: RD D /V V V V FT  or  RD D /V V V V VV V V V FT
		//           r r  r r r r          r r  n n n n  x x x x
		//    R        - runway number follows
		//    D D      - runway number
		//     r r
		//    (D )     - runway approach directions
		//      r        L (left), R (right), C (center)
		//    (M/P)    - M (less than 0600FT), P (greater than 6000FT)
		//    V V V V  - (lowest) visual range, constant reportable value
		//     r r r r
		//    V        - separates lowest/highest visual range
		//    V V V V  - (highest) visual range, constant reportable value
		//     x x x x
		//    FT       - feet
		//
		while (((String)tokens.get(index)).startsWith("R")) {
			// check that first character after the R is a digit. this helps
			// qualify this as a real RVR. Otherwise we could be grabbing the
			// wx descriptor 'RA'
			if (!Character.isDigit(((String) tokens.get(index)).charAt(1))) {
				break;
			}
		


			// we have a runway visual range
			runwayVisualRange = new RunwayVisualRange();

			// get our runway number
			runwayVisualRange.setRunwayNumber(new Integer(((String)tokens.get(index)).substring(1,3)).intValue());


			int pos = 3;
			if (((String)tokens.get(index)).charAt(pos) != '/') {
				runwayVisualRange.setApproachDirection(((String)tokens.get(index)).charAt(pos));

				pos += 2; // increment past the '/'
			} else {
				pos++;
			}

			// determine if we have a modifier for above 6000ft or below 600ft
			switch (((String)tokens.get(index)).charAt(pos)) {
				case 'P': // below 600ft
				case 'M': // above 6000ft
					runwayVisualRange.setReportableModifier(((String)tokens.get(index)).charAt(pos));

					pos++;
			}
			runwayVisualRange.setLowestReportable(new Integer(((String)tokens.get(index)).substring(pos,pos+4)).intValue());

			pos += 4;
			// if we are using the format with highest reportable
			if (((String)tokens.get(index)).charAt(pos) == 'V') {
				pos++; // increment past V
				runwayVisualRange.setHighestReportable(new Integer(((String)tokens.get(index)).substring(pos,pos+4)).intValue());

			}

			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}

			metar.addRunwayVisualRange(runwayVisualRange);
		}



		// weather groups
		// format: (+/-)ddpp
		//     (+/-) - intensity, light (-), moderate (default), heavy (+)
		//     dd    - descriptor, qualifier/adjective for phenomena
		//     pp    - phenomena (rain, hail, tornado, etc.)
		// we know we have a weather group if the token starts with one of:
		// _________________________________________________________________________________
		// | Intensity  |   Descriptor  |   Precipitation  |   Obscuration |   Other       |
		// +------------+---------------+------------------+---------------+---------------+
		// | - Light    | MI Shallow    | DZ Drizzle       | BR Mist       | PO Well-      |
		// |   Moderate | PR Partial    | RA Rain          | FG Fog        |    Developed  |
		// | + Heavy    | BC Patches    | SN Snow          | FU Smoke      |    Dust/Sand  |
		// |            | DR Low        | SG Snow Grains   | VA Volcanic   |    Whirls     |
		// |            |    Drifting   | IC Ice Crystals  |    Ash        | SQ Squalls    |
		// |            | BL Blowing    | PL Ice Pellets   | DU Widespread | FC Funnel     |
		// |            | SH Shower(s)  | GR Hail          |    Dust       |    Cloud,     |
		// |            | TS Thunder-   | GS Small Hail    | SA Sand       |    Tornado,   |
		// |            |    storm      |    and/or        | HZ Haze       |    Waterspout |
		// |            | FZ Freezing   |    Snow Pellets  | PY Spray      | SS Sandstorm  |
		// |            |               | UP Unknown       |               | DS Duststorm  |
		// |            |               |    Precipitation |               |               |
		// +------------+---------------+------------------+---------------+---------------+
		while (((String)tokens.get(index)).startsWith(MetarConstants.METAR_HEAVY) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_LIGHT) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SHALLOW) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_PARTIAL) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_PATCHES) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_LOW_DRIFTING) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_BLOWING) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SHOWERS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_THUNDERSTORMS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_FREEZING) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_DRIZZLE) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_RAIN) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SNOW) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SNOW_GRAINS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_ICE_CRYSTALS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_ICE_PELLETS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_HAIL) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SMALL_HAIL) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_UNKNOWN_PRECIPITATION) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_MIST) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_FOG) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SMOKE) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_VOLCANIC_ASH) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_WIDESPREAD_DUST) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SAND) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_HAZE) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SPRAY) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_DUST_SAND_WHIRLS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SQUALLS) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_FUNNEL_CLOUD) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SAND_STORM) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_DUST_STORM) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CHANGE))
		{


			int pos = 0;

			// we have a weather condition
			weatherCondition = new WeatherCondition();

			if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_HEAVY) || 
			    ((String)tokens.get(index)).startsWith(MetarConstants.METAR_LIGHT))
			{
				weatherCondition.setIntensity(String.valueOf(((String)tokens.get(index)).charAt(0)));

				pos++;
			} else {

			}

			// if we have a descriptor
		    if (((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_SHALLOW) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_PARTIAL) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_PATCHES) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_LOW_DRIFTING) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_BLOWING) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_SHOWERS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_THUNDERSTORMS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).startsWith(MetarConstants.METAR_FREEZING))
			{
				weatherCondition.setDescriptor(((String)tokens.get(index)).substring(pos,pos+2));

				pos += 2;
			} else {

			}

			// if we have phenomena (we should always!)
		    if (((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_DRIZZLE) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_RAIN) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SNOW) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SNOW_GRAINS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_ICE_CRYSTALS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_ICE_PELLETS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_HAIL) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SMALL_HAIL) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_UNKNOWN_PRECIPITATION) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_MIST) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_FOG) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SMOKE) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_VOLCANIC_ASH) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_WIDESPREAD_DUST) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SAND) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_HAZE) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SPRAY) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_DUST_SAND_WHIRLS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SQUALLS) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_FUNNEL_CLOUD) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_SAND_STORM) ||
		        ((String)tokens.get(index)).substring(pos,pos+2).equals(MetarConstants.METAR_DUST_STORM))
			{
				weatherCondition.setPhenomena(((String)tokens.get(index)).substring(pos,pos+2));

				metar.addWeatherCondition(weatherCondition);

			} else {

			}

			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}
		}



		// sky condition
		// format: NNNhhh or VVhhh or CLR/SKC
		//     NNN - amount of sky cover
		//     hhh - height of layer (in hundreds of feet above the surface)
		//     VV  - vertical visibility, indefinite ceiling
		//     SKC - clear skies (reported by manual station)
		//     CLR - clear skies (reported by automated station)
		while (((String)tokens.get(index)).startsWith(MetarConstants.METAR_VERTICAL_VISIBILITY) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SKY_CLEAR) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_CLEAR) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_FEW) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SCATTERED) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_BROKEN) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_OVERCAST) ||
		       ((String)tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CLOUDS))
		{


			// we have a sky condition
			skyCondition = new SkyCondition();

			if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_FEW) ||
		        ((String)tokens.get(index)).startsWith(MetarConstants.METAR_SCATTERED) ||
		        ((String)tokens.get(index)).startsWith(MetarConstants.METAR_BROKEN) ||
		        ((String)tokens.get(index)).startsWith(MetarConstants.METAR_OVERCAST))
			{
				skyCondition.setContraction(((String)tokens.get(index)).substring(0,3));

				skyCondition.setHeight(new Integer(((String)tokens.get(index)).substring(3,6)).intValue());

				if (((String)tokens.get(index)).length() > 6) {
					// we have a modifier
					skyCondition.setModifier(((String)tokens.get(index)).substring(6,((String)tokens.get(index)).length()));

				}
			} else if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_SKY_CLEAR) ||
		               ((String)tokens.get(index)).startsWith(MetarConstants.METAR_CLEAR))
			{
				skyCondition.setContraction(((String)tokens.get(index)).substring(0,3));

			} else if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_VERTICAL_VISIBILITY)) {
				skyCondition.setContraction(((String)tokens.get(index)).substring(0,2));

				skyCondition.setHeight(new Integer(((String)tokens.get(index)).substring(2,5)).intValue());

			} else if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_NO_SIGNIFICANT_CLOUDS)) {
				skyCondition.setContraction(((String)tokens.get(index)).substring(0,3));

			} else {

			}

			metar.addSkyCondition(skyCondition);


			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}
		}



		// temperature / dew point
		// format: (M)T'T'/(M)T' T'
		//                d  d
		//     (M)    - sub-zero temperature
		//     T'T'   - temerature (in celsius)
		//     T' T'  - dew point (in celsius)
		//       d  d
		//
		// TF = ( 9 / 5 ) x TC + 32 (conversion from celsius to fahrenheit)
		if (((String)tokens.get(index)).indexOf("/") != -1) {

			ArrayList temps = new ArrayList();

			try {
				utility.split(temps, "/\\//", ((String)tokens.get(index)));
			} catch(MalformedPerl5PatternException e) {

				throw new MetarParseException("error spliting temperature on /: "+e);
			}

			// we have a sub-zero temperature
			Float temperature = null;
			if (((String)temps.get(0)).startsWith("M")) {
				temperature = new Float(((String)temps.get(0)).substring(1,3));
				temperature = new Float(temperature.floatValue() - temperature.floatValue()*2); // negate
				metar.setTemperature(temperature);
			} else {
				temperature = new Float(((String)temps.get(0)));
				metar.setTemperature(temperature);
			}



			// we have a sub-zero temperature
			Float dewPoint = null;
			if (((String)temps.get(1)).startsWith("M")) {
				dewPoint = new Float(((String)temps.get(1)).substring(1,3));
				dewPoint = new Float(dewPoint.floatValue() - dewPoint.floatValue()*2); // negate
				metar.setDewPoint(dewPoint);
			} else {
				dewPoint = new Float(((String)temps.get(1)));
				metar.setDewPoint(dewPoint);
			}


			//temperature = new Float(((String)tokens.get(index)).substring(1,5)).floatValue();
			//dewPoint = new Float(((String)tokens.get(index)).substring(5,9)).floatValue();

			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}
		} else {

			metar.setTemperature(null);
			metar.setDewPoint(null);
		}



		// altimeter
		// get pressure, which is reported in hundreths
		//
		// format: AP P P P
		//           h h h h
		//     A        - altimeter in inches of mercury
		//     P P P P  - tens, units, tenths and hundreths inches mercury
		//      h h h h   (no decimal point coded)
		if (((String)tokens.get(index)).startsWith("A")) {
			Float pressure = new Float(((String)tokens.get(index)).substring(1,5));
			// correct for no decimal point
			pressure = new Float(pressure.floatValue() / 100);
			metar.setPressure(pressure);



			// on to the next token
			if (index < numTokens - 1) {
				index++;
			}
		} else {

		}



		// remarks
		if (!((String)tokens.get(index)).equals(MetarConstants.METAR_REMARKS)) {
			// we have no remarks

		} else {

			index++;
		}

		// remarks
		// -------
		// volcanic eruptions
		// funnel cloud
		// type of automated station (A01/A02)
		//     A01 - stations without a precipitation descriminator
		//     A02 - stations with a precipitation descriminator
		// peak wind, PK_WND_dddff(f)/(hh)mm
		// wind shift, WSHFT_(hh)mm (FROPA)
		// tower or surface visibility
		// variable prevailing visbility
		// sector visbility
		// visbility at second location
		// lightning
		// beginning and ending of precipitation
		// beginning and ending of thunderstorms
		// thunderstorm location
		// hailstone size
		// virga
		// variable ceiling height
		// obscurations
		// variable sky condition
		// significant cloud types
		// ceiling height at second location
		// pressure rising or falling rapidly
		// sea-level pressure
		// aircraft mishap
		// no SPECI reports taken
		// snow increasing rapidly
		// other significant information

		// additive data
		// -------------
		// precipitation
		// cloud types
		// duration of sunshine


		// hourly temperature and dewpoint
		// format: Ts T'T'T's T' T' T'
		//           n       n  d  d  d
		//     T         - group indicator
		//     s         - sign of the temperature (1=sub-zero, 0=zero+)
		//      n
		//     T'T'T'    - temperature
		//     T' T' T'  - dew point
		//       d  d  d
		//
		// see if we have hourly temperature
		while (index < numTokens) {


			// if we have temperature
			temp = (String)tokens.get(index);
			//if (((String)tokens.get(index)).matches("T\\d{8}")) {
			try {
				if (matcher.matches(temp, new Perl5Compiler().compile("T\\d{8}"))) {

					// we have a sub-zero temperature
					Float temperaturePrecise = new Float(((String)tokens.get(index)).substring(2,5));
					if (((String)tokens.get(index)).charAt(1) == '1') {
						temperaturePrecise = new Float(temperaturePrecise.floatValue() - temperaturePrecise.floatValue()*2); // negate
					}
					// it is in tenths
					temperaturePrecise = new Float(temperaturePrecise.floatValue() / 10);
					metar.setTemperaturePrecise(temperaturePrecise);

					// we have a sub-zero dew point
					Float dewPointPrecise = new Float(((String)tokens.get(index)).substring(6,9));
					if (((String)tokens.get(index)).charAt(5) == '1') {
						dewPointPrecise = new Float(dewPointPrecise.floatValue() - dewPointPrecise.floatValue()*2); // negate
					}
					// it is in tenths
					dewPointPrecise = new Float(dewPointPrecise.floatValue() / 10);
					metar.setDewPointPrecise(dewPointPrecise);



				// if we have an obscuration
				} else if (((String)tokens.get(index)).equals(MetarConstants.METAR_MIST) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_FOG) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_SMOKE) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_VOLCANIC_ASH) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_WIDESPREAD_DUST) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_SAND) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_HAZE) ||
						   ((String)tokens.get(index)).equals(MetarConstants.METAR_SPRAY))
				{
					// we have an obscuration
					obscuration = new Obscuration();
					obscuration.setPhenomena(((String)tokens.get(index)));


					// move to quantity and height token
					index++;

					// we have a quantity and height too
					if (((String)tokens.get(index)).startsWith(MetarConstants.METAR_FEW) ||
						((String)tokens.get(index)).startsWith(MetarConstants.METAR_SCATTERED) ||
						((String)tokens.get(index)).startsWith(MetarConstants.METAR_BROKEN) ||
						((String)tokens.get(index)).startsWith(MetarConstants.METAR_OVERCAST))
					{
						obscuration.setContraction(((String)tokens.get(index)).substring(0,3));
						obscuration.setHeight(new Integer(((String)tokens.get(index)).substring(3,6)).intValue());

						metar.addObscuration(obscuration);

					}

					index++;
				// there has been no significant change in weather
				} else if (((String)tokens.get(index)).equals(MetarConstants.METAR_NO_SIGNIFICANT_CHANGE)) {
					// have no significant change
					metar.setIsNoSignificantChange(true);
				}
			} catch(MalformedPatternException e) {

			}

			index++;
		}



		// 6-hourly maximum temperature
		// 6-hourly minimum temperature
		// 24-hour maximum and minimum temperature
		// 3-hourly pressure tendency

		return metar;
	}
}

