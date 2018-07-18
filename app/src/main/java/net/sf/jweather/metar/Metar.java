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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Responsible for storing METAR data and providing methods for accessing
 * the data
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.9 $
 * @see <a href="Weather.html">Weather</a>
 * @see <a href="Obscuration.html">Obscuration</a>
 * @see <a href="RunwayVisualRange.html">RunwayVisualRange</a>
 * @see <a href="SkyCondition.html">SkyCondition</a>
 * @see <a href="WeatherCondition.html">WeatherCondition</a>
 */
public class Metar {

	String dateString = "";
	Date date = null;
	String reportModifier = "";
	String stationID = "";
	Integer windDirection = null;
	Integer windDirectionMin = null;
	Integer windDirectionMax = null;
	boolean windDirectionIsVariable = false;
	Float windSpeed = null; // (in knots x 1.1508 = MPH)
	Float windGusts = null; // (in knots x 1.1508 = MPH)
	boolean isCavok = false;
	Float visibilityMiles = null; // in miles
	Float visibilityKilometers = null; // in kilometers
	Float visibilityMeters = null; // in meters
	boolean visibilityLessThan = false;
	Float pressure = null;
	Float temperature = null;
	Float temperaturePrecise = null;
	Float dewPoint = null;
	Float dewPointPrecise = null;
	ArrayList weatherConditions = new ArrayList();
	ArrayList skyConditions = new ArrayList();
	ArrayList runwayVisualRanges = new ArrayList();
	ArrayList obscurations = new ArrayList();
	private boolean isNoSignificantChange = false;

	public Metar() {
	}

    /**
     *
	 * @param value string that represents the date this METAR report was
	 * generated
     */
	protected void setDateString(String value) {
		this.dateString = value;
	}

    /**
     *
     * @return string that represents the date this METAR report was generated
     */
	public String getDateString() {
		return dateString;
	}

    /**
     *
	 * @param value the date this METAR report was generated
     */
	protected void setDate(Date value) {
		this.date = value;
	}

    /**
     *
     * @return the date this METAR report was generated
     */
	public Date getDate() {
		return date;
	}

    /**
     *
	 * @param value the modifier of the report, which specifies whether this
	 * report was an automated report or was a corrected report
     */
	protected void setReportModifier(String value) {
		this.reportModifier = value;
	}

    /**
     *
     * @return the modifier of the report, which specifies whether this report
     * was an automated report or was a corrected report
     */
	public String getReportModifier() {
		return reportModifier;
	}

    /**
     *
	 * @param value the station id of the station that generated this METAR
	 * report
     */
	protected void setStationID(String value) {
		this.stationID = value;
	}

    /**
     *
     * @return the station id of the station that generated this METAR report
     */
	public String getStationID() {
		return stationID;
	}

    /**
     *
     * @param value the direction the wind is blowing in (in degrees)
     */
	protected void setWindDirection(Integer value) {
		this.windDirection = value;
	}

    /**
     *
     * @return the direction the wind is blowing in (in degrees)
     */
	public Integer getWindDirection() {
		return windDirection;
	}

    /**
     *
	 * @param value the minimum wind direction (in degrees) for variable wind
	 * directions
     */
	protected void setWindDirectionMin(Integer value) {
		this.windDirectionMin = value;
	}

    /**
     *
	 * @return the minimum wind direction (in degrees) for variable wind
	 * directions
     */
	public Integer getWindDirectionMin() {
		return windDirectionMin;
	}

    /**
     *
	 * @param value the maximum wind direction (in degrees) for variable wind
	 * directions
     */
	protected void setWindDirectionMax(Integer value) {
		this.windDirectionMax = value;
	}

    /**
     *
	 * @return the maximum wind direction (in degrees) for variable wind
	 * directions
     */
	public Integer getWindDirectionMax() {
		return windDirectionMax;
	}

    /**
     *
     * @param value whether or not the wind direction is variable
     */
	protected void setWindDirectionIsVariable(boolean value) {
		this.windDirectionIsVariable = value;
	}

    /**
     *
     * @return whether or not the wind direction is variable
     */
	public boolean getWindDirectionIsVariable() {
		return windDirectionIsVariable;
	}

    /**
     *
     * @param value wind speed in knots
     */
	protected void setWindSpeed(Float value) {
		this.windSpeed = value;
	}

    /**
     *
     * @param value wind speed in meters per second
     */
	protected void setWindSpeedInMPS(Float value) {
		this.windSpeed = new Float(value.floatValue() / 0.5148);
	}

    /**
     *
     * @return wind speed in meters per second
     */
	public Float getWindSpeedInMPS() {
		return new Float(this.windSpeed.floatValue() * 0.5148);
	}

    /**
     *
     * @return wind speed in knots
     */
	public Float getWindSpeedInKnots() {
		return this.windSpeed;
	}

    /**
     *
     * @return wind speed in MPH
     */
	public Float getWindSpeedInMPH() {
		if (this.windSpeed == null) {
			return null;
		}

		double f = this.windSpeed.floatValue() * 1.1508;

		// round to the nearest MPH
		f = Math.round(f);

		return new Float(f);
	}

    /**
     *
     * @param value wind gust speed in knots
     */
	protected void setWindGusts(Float value) {
		this.windGusts = value;
	}

    /**
     *
     * @param value wind gust speed in meters per second
     */
	protected void setWindGustsInMPS(Float value) {
		this.windGusts = new Float(value.floatValue() / 0.5148);
	}

    /**
     *
     * @return wind gust speed in meters per second
     */
	public Float getWindGustsInMPS() {
		return new Float(this.windGusts.floatValue() * 0.5148);
	}

    /**
     *
     * @return wind gust speed in knots
     */
	public Float getWindGustsInKnots() {
		return this.windGusts;
	}

    /**
     *
     * @return wind gust speed in MPH
     */
	public Float getWindGustsInMPH() {
		if (this.windGusts == null) {
			return null;
		}

		double f = this.windGusts.floatValue() * 1.1508;

		// round to the nearest MPH
		f = Math.round(f);

		return new Float(f);
	}

    /**
	 * this function will also set visibility to 10KM, since
	 * CAVOK means visibility is greater than 10KM
     *
     * @param value boolean whether or not CAVOK was given
     */
	protected void setIsCavok(boolean value) {
		this.isCavok = value;
		// set visibility to 10
		setVisibilityInKilometers(new Float(10));
	}

    /**
     * @return value boolean whether or not CAVOK is true
     */
	public boolean getIsCavok() {
		return this.isCavok;
	}

    /**
     *
     * @param value visibility in miles
     */
	protected void setVisibility(Float value) {
		this.visibilityMiles = value;
		this.visibilityKilometers = null;
		this.visibilityMeters = null;
	}

    /**
     *
     * @param value visibility in kilometers
     */
	protected void setVisibilityInKilometers(Float value) {
		this.visibilityKilometers = value;
		this.visibilityMiles = null;
		this.visibilityMeters = null;
	}

    /**
     *
     * @param value visibility in meters
     */
	protected void setVisibilityInMeters(Float value) {
		this.visibilityMeters = value;
		this.visibilityMiles = null;
		this.visibilityKilometers = null;
	}

    /**
     *
     * @return visibility in miles
     */
	public Float getVisibility() {
		if (visibilityMiles != null) {
			return visibilityMiles;
		} else if (visibilityKilometers != null) {
			return new Float(visibilityKilometers.floatValue() / 1.609344);
		} else if (visibilityMeters != null) {
			return new Float(visibilityMeters.floatValue() / 1609.344);
		}
		return null;
	}

    /**
     *
     * @return visibility in kilometers
     */
	public Float getVisibilityInKilometers() {
		if (visibilityKilometers != null) {
			return visibilityKilometers;
		} else if (visibilityMeters != null) {
			return new Float(visibilityMeters.floatValue() / 1000);
		} else if (visibilityMiles != null) {
			return new Float(visibilityMiles.floatValue() * 1.609344);
		}
		return null;
	}

    /**
     *
     * @return visibility in meters
     */
	public Float getVisibilityInMeters() {
		if (visibilityMeters != null) {
			return visibilityMeters;
		} else if (visibilityKilometers != null) {
			return new Float(visibilityKilometers.floatValue() * 1000);
		} else if (visibilityKilometers != null) {
			return new Float(visibilityKilometers.floatValue() * 1609.344);
		}
		return null;
	}

    /**
     *
     * @param value visibility less than
     */
	protected void setVisibilityLessThan(boolean value) {
		this.visibilityLessThan = value;
	}

    /**
     *
     * @return visibility less than
     */
	public boolean getVisibilityLessThan() {
		return visibilityLessThan;
	}

    /**
     *
     * @param value pressure in inches Hg
     */
	protected void setPressure(Float value) {
		this.pressure = value;
	}

    /**
     *
     * @return pressure in inches Hg
     */
	public Float getPressure() {
		return pressure;
	}

    /**
     *
     * @param value temperature in celsius
     */
	protected void setTemperature(Float value) {
		this.temperature = value;
	}

    /**
     *
     * @return temperature in celsius
     */
	public Float getTemperatureInCelsius() {
		return this.temperature;
	}

    /**
     *
     * @return temperature in fahrenheit
     */
	public Float getTemperatureInFahrenheit() {
		if (this.temperature == null) {
			return null;
		}

		// round to the nearest 1/10th
		float f = (float)Math.round((this.temperature.floatValue()*9/5+32)*10)/10;

		return new Float(f);
	}

    /**
     *
     * @param value precise temperature in celsius
     */
	protected void setTemperaturePrecise(Float value) {
		this.temperaturePrecise = value;
	}

    /**
     *
     * @return precise temperature in celsius (nearest 1/10th degree)
     */
	public Float getTemperaturePreciseInCelsius() {
		return this.temperaturePrecise;
	}

    /**
     *
     * @return precise temperature in fahrenheit (nearest 1/10th degree)
     */
	public Float getTemperaturePreciseInFahrenheit() {
		if (this.temperaturePrecise == null) {
			return null;
		}

		// round to the nearest 1/10th
		float f = (float)Math.round((this.temperaturePrecise.floatValue()*9/5+32)*10)/10;
		return new Float(f);
	}

    /**
     *
     * @return most precise temperature in celsius (nearest 1/10th degree)
     */
	public Float getTemperatureMostPreciseInCelsius() {
		if (this.temperaturePrecise != null) {
			return this.temperaturePrecise;
		} else {
			return this.temperature;
		}
	}

    /**
     *
     * @return most precise temperature in fahrenheit (nearest 1/10th degree)
     */
	public Float getTemperatureMostPreciseInFahrenheit() {
		if (this.temperaturePrecise != null) {
			// round to the nearest 1/10th
			float f = (float)Math.round((this.temperaturePrecise.floatValue()*9/5+32)*10)/10;

			return new Float(f);
		} else if (this.temperature != null) {
			// round to the nearest 1/10th
			float f = (float)Math.round((this.temperature.floatValue()*9/5+32)*10)/10;

			return new Float(f);
		} else {
			return null;
		}
	}

    /**
     *
     * @param value dew point in celsius
     */
	protected void setDewPoint(Float value) {
		this.dewPoint = value;
	}

    /**
     *
     * @return dew point in celsius
     */
	public Float getDewPointInCelsius() {
		return this.dewPoint;
	}

    /**
     *
     * @return dew point in fahrenheit
     */
	public Float getDewPointInFahrenheit() {
		if (this.dewPoint == null) {
			return null;
		}

		// round to the nearest 1/10th
		float f = (float)Math.round((this.dewPoint.floatValue()*9/5+32)*10)/10;

		return new Float(f);
	}

    /**
     *
     * @param value precise dew point in celsius
     */
	protected void setDewPointPrecise(Float value) {
		this.dewPointPrecise = value;
	}

    /**
     *
     * @return dew point in celsius (nearest 1/10th degree)
     */
	public Float getDewPointPreciseInCelsius() {
		return this.dewPointPrecise;
	}

    /**
     *
     * @return dew point in fahrenheit (nearest 1/10th degree)
     */
	public Float getDewPointPreciseInFahrenheit() {
		if (this.dewPointPrecise == null) {
			return null;
		}

		// round to the nearest 1/10th
		float f = (float)Math.round((this.dewPointPrecise.floatValue()*9/5+32)*10)/10;

		return new Float(f);
	}

    /**
     *
     * @return most precise dew point in celsius (nearest 1/10th degree)
     */
	public Float getDewPointMostPreciseInCelsius() {
		if (this.dewPointPrecise != null) {
			return this.dewPointPrecise;
		} else {
			return this.dewPoint;
		}
	}

    /**
     *
     * @return most precise dew point in fahrenheit (nearest 1/10th degree)
     */
	public Float getDewPointMostPreciseInFahrenheit() {
		if (this.dewPointPrecise != null) {
			// round to the nearest 1/10th
			float f = (float)Math.round((this.dewPointPrecise.floatValue()*9/5+32)*10)/10;

			return new Float(f);
		} else if (this.dewPoint != null) {
			// round to the nearest 1/10th
			float f = (float)Math.round((this.dewPoint.floatValue()*9/5+32)*10)/10;

			return new Float(f);
		} else {
			return null;
		}
	}

    /**
     *
     * @param value whether or not the weather has changed significantly
     */
	protected void setIsNoSignificantChange(boolean value) {
		this.isNoSignificantChange = value;
	}

    /**
     *
     * @return whether or not there has been a significant change in
	 * weather
     */
	public boolean getIsNoSignificantChange() {
		return isNoSignificantChange;
	}

	/**
	 *
	 * @param wc a WeatherCondition object
     * @see WeatherCondition
	 */
	public void addWeatherCondition(WeatherCondition wc) {
		weatherConditions.add(wc);
	}

	/**
	 *
	 * @return a WeatherCondition object
     * @see WeatherCondition
	 */
	public WeatherCondition getWeatherCondition(int i) {
		if (weatherConditions.size() >= i) {
			return (WeatherCondition)weatherConditions.get(i);
		} else {
			return null;
		}
	}

    /**
     *
     * @return an ArrayList of WeatherCondition objects
     * @see WeatherCondition
     */
	public ArrayList getWeatherConditions() {
		return weatherConditions;
	}

	/**
	 *
	 * @param sc a SkyCondition object
     * @see SkyCondition
	 */
	public void addSkyCondition(SkyCondition sc) {
		skyConditions.add(sc);
	}

	/**
	 *
	 * @return a SkyCondition object
     * @see SkyCondition
	 */
	public SkyCondition getSkyCondition(int i) {
		if (skyConditions.size() >= i) {
			return (SkyCondition)skyConditions.get(i);
		} else {
			return null;
		}
	}

    /**
     *
     * @return an ArrayList of SkyCondition objects
     * @see SkyCondition
     */
	public ArrayList getSkyConditions() {
		return skyConditions;
	}

	/**
	 *
	 * @param rvr a RunwayVisualRange object
     * @see RunwayVisualRange
	 */
	public void addRunwayVisualRange(RunwayVisualRange rvr) {
		runwayVisualRanges.add(rvr);
	}

	/**
	 *
	 * @return a RunwayVisualRange object
     * @see RunwayVisualRange
	 */
	public RunwayVisualRange getRunwayVisualRange(int i) {
		if (runwayVisualRanges.size() >= i) {
			return (RunwayVisualRange) runwayVisualRanges.get(i);
		} else {
			return null;
		}
	}

    /**
     *
     * @return an ArrayList of RunwayVisualRange objects
     * @see RunwayVisualRange
     */
	public ArrayList getRunwayVisualRanges() {
		return runwayVisualRanges;
	}

	/**
	 *
	 * @param o an Obscuration object
     * @see Obscuration
	 */
	public void addObscuration(Obscuration o) {
		obscurations.add(o);
	}

	/**
	 *
	 * @return a Obscuration object
     * @see Obscuration
	 */
	public Obscuration getObscuration(int i) {
		if (obscurations.size() >= i) {
			return (Obscuration)obscurations.get(i);
		} else {
			return null;
		}
	}

    /**
     *
     * @return an ArrayList of Obscuration objects
     * @see Obscuration
     */
	public ArrayList getObscurations() {
		return obscurations;
	}
	
    /**
     * display metar data in a human-readable format
     */
    public void print() {
		System.out.println("station id : " + getStationID());
		System.out.println("wind dir   : " + getWindDirection() + " degrees");
		System.out.println("wind speed : " + getWindSpeedInMPH() + " mph, " +
				                             getWindSpeedInKnots() + " knots");
		System.out.println("wind gusts : " + getWindGustsInMPH() + " mph, " +
				                             getWindGustsInKnots() + " knots");
		if (!getVisibilityLessThan()) {
			System.out.println("visibility : " + getVisibility() + " mile(s)");
		} else {
			System.out.println("visibility : < " + getVisibility() + " mile(s)");
		}

		System.out.println("pressure   : " + getPressure() + " in Hg");
		System.out.println("temperaturePrecise: " +
				           getTemperaturePreciseInCelsius() + " C, " +
				           getTemperaturePreciseInFahrenheit() + " F");
		System.out.println("temperature: " +
				           getTemperatureInCelsius() + " C, " +
				           getTemperatureInFahrenheit() + " F");
		System.out.println("temperatureMostPrecise: " +
				           getTemperatureMostPreciseInCelsius() + " C, " +
				           getTemperatureMostPreciseInFahrenheit() + " F");

		System.out.println("dewPointPrecise: " +
				           getDewPointPreciseInCelsius() + " C, " +
				           getDewPointPreciseInFahrenheit() + " F");
		System.out.println("dewPoint: " +
				           getDewPointInCelsius() + " C, " +
				           getDewPointInFahrenheit() + " F");
		System.out.println("dewPointMostPrecise: " +
				           getDewPointMostPreciseInCelsius() + " C, " +
				           getDewPointMostPreciseInFahrenheit() + " F");

		if (getWeatherConditions() != null) {
			Iterator i = getWeatherConditions().iterator();
			while (i.hasNext()) {
				WeatherCondition weatherCondition = (WeatherCondition)i.next();
				System.out.println(weatherCondition.getNaturalLanguageString());
			}
		}
		if (getSkyConditions() != null) {
			Iterator i = getSkyConditions().iterator();
			while (i.hasNext()) {
				SkyCondition skyCondition = (SkyCondition)i.next();
				System.out.println(skyCondition.getNaturalLanguageString());
			}
		}
	}
}

