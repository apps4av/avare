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
 * Represents a single weather condition element that appears in a METAR report
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.2 $
 */
public class WeatherCondition {
	private String intensity = "";
	private String descriptor = "";
	private String phenomena = "";

	// intensity
	private boolean isLight = false;
	private boolean isHeavy = false;
	private boolean isModerate = false;

	// descriptor
	private boolean isShallow = false;
	private boolean isPartial = false;
	private boolean isPatches = false;
	private boolean isLowDrifting = false;
	private boolean isBlowing = false;
	private boolean isShowers = false;
	private boolean isThunderstorms = false;
	private boolean isFreezing = false;

	// phenomena
	private boolean isDrizzle = false;
	private boolean isRain = false;
	private boolean isSnow = false;
	private boolean isSnowGrains = false;
	private boolean isIceCrystals = false;
	private boolean isIcePellets = false;
	private boolean isHail = false;
	private boolean isSmallHail = false;
	private boolean isUnknownPrecipitation = false;
	private boolean isMist = false;
	private boolean isFog = false;
	private boolean isSmoke = false;
	private boolean isVolcanicAsh = false;
	private boolean isWidespreadDust = false;
	private boolean isSand = false;
	private boolean isHaze = false;
	private boolean isSpray = false;
	private boolean isDustSandWhirls = false;
	private boolean isSqualls = false;
	private boolean isSandstorm = false;
	private boolean isDuststorm = false;
	// same metar token for all three
	private boolean isFunnelCloud = false;
	private boolean isTornado = false;
	private boolean isWaterspout = false;

	public WeatherCondition() {
	}

    /**
     * Set the intensity for this Weather Condition
     *
     * @param intensity the part of a METAR weather condition token which
     * represents the intesity of the weather condition (e.g. '-' - light,
     * '+' - heavy)
     */
	protected void setIntensity(String intensity) {
		this.intensity = intensity;

		if (intensity.equals(MetarConstants.METAR_LIGHT)) {
			isLight = true;
		} else if (intensity.equals(MetarConstants.METAR_HEAVY)) {
			isHeavy = true;
		} else {
			isModerate = true;
		}
	}

    /**
     * Set the descriptor for this Weather Condition
     *
     * @param descriptor the part of a METAR weather condition token which
     * represents a description of the quality of the phenomena (e.g. 'BC' - patches,
     * 'SH' - showers)
     */
	protected void setDescriptor(String descriptor) {
		this.descriptor = descriptor;

		if (descriptor.equals(MetarConstants.METAR_SHALLOW)) {
			isShallow = true;
		} else if (descriptor.equals(MetarConstants.METAR_PARTIAL)) {
			isPartial = true;
		} else if (descriptor.equals(MetarConstants.METAR_PATCHES)) {
			isPatches = true;
		} else if (descriptor.equals(MetarConstants.METAR_LOW_DRIFTING)) {
			isLowDrifting = true;
		} else if (descriptor.equals(MetarConstants.METAR_BLOWING)) {
			isBlowing = true;
		} else if (descriptor.equals(MetarConstants.METAR_SHOWERS)) {
			isShowers = true;
		} else if (descriptor.equals(MetarConstants.METAR_THUNDERSTORMS)) {
			isThunderstorms = true;
		} else if (descriptor.equals(MetarConstants.METAR_FREEZING)) {
			isFreezing = true;
		} else {
			// shouldn't get here
		}
	}

    /**
     * Set the phenomena for this Weather Condition
	 *
     * @param phenomena the part of a METAR weather condition token which
     * represents a specific type of phenomena (e.g. 'SN', 'HZ')
     */
	protected void setPhenomena(String phenomena) {
		this.phenomena = phenomena;

		if (phenomena.equals(MetarConstants.METAR_DRIZZLE)) {
			isDrizzle = true;
		} else if (phenomena.equals(MetarConstants.METAR_RAIN)) {
			isRain = true;
		} else if (phenomena.equals(MetarConstants.METAR_SNOW)) {
			isSnow = true;
		} else if (phenomena.equals(MetarConstants.METAR_SNOW_GRAINS)) {
			isSnowGrains = true;
		} else if (phenomena.equals(MetarConstants.METAR_ICE_CRYSTALS)) {
			isIceCrystals = true;
		} else if (phenomena.equals(MetarConstants.METAR_ICE_PELLETS)) {
			isIcePellets = true;
		} else if (phenomena.equals(MetarConstants.METAR_HAIL)) {
			isHail = true;
		} else if (phenomena.equals(MetarConstants.METAR_SMALL_HAIL)) {
			isSmallHail = true;
		} else if (phenomena.equals(MetarConstants.METAR_UNKNOWN_PRECIPITATION)) {
			isUnknownPrecipitation = true;
		} else if (phenomena.equals(MetarConstants.METAR_MIST)) {
			isMist = true;
		} else if (phenomena.equals(MetarConstants.METAR_FOG)) {
			isFog = true;
		} else if (phenomena.equals(MetarConstants.METAR_SMOKE)) {
			isSmoke = true;
		} else if (phenomena.equals(MetarConstants.METAR_VOLCANIC_ASH)) {
			isVolcanicAsh = true;
		} else if (phenomena.equals(MetarConstants.METAR_WIDESPREAD_DUST)) {
			isWidespreadDust = true;
		} else if (phenomena.equals(MetarConstants.METAR_SAND)) {
			isSand = true;
		} else if (phenomena.equals(MetarConstants.METAR_HAZE)) {
			isHaze = true;
		} else if (phenomena.equals(MetarConstants.METAR_SPRAY)) {
			isSpray = true;
		} else if (phenomena.equals(MetarConstants.METAR_DUST_SAND_WHIRLS)) {
			isDustSandWhirls = true;
		} else if (phenomena.equals(MetarConstants.METAR_SQUALLS)) {
			isSqualls = true;
		} else if (phenomena.equals(MetarConstants.METAR_FUNNEL_CLOUD)) {
			isFunnelCloud = true;
			isTornado = true;
			isWaterspout = false;
		} else if (phenomena.equals(MetarConstants.METAR_SAND_STORM)) {
			isSandstorm = true;
		} else if (phenomena.equals(MetarConstants.METAR_DUST_STORM)) {
			isDuststorm = true;
		} else {
			// shouldn't get here
		}
	}

	/**
	 * Use this function to determine if a weather condition is of "light"
	 * intensity.
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * an intensity of "light"
	 */
	public boolean isLight() {
		return isLight;
	}


	/**
	 * Use this function to determine if a weather condition is of "heavy"
	 * intensity.
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * an intensity of "heavy"
	 */
	public boolean isHeavy() {
		return isHeavy;
	}


	/**
	 * Use this function to determine if a weather condition is of "moderate"
	 * intensity.
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * an intensity of "moderate"
	 */
	public boolean isModerate() {
		return isModerate;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "shallow"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "shallow"
	 */
	public boolean isShallow() {
		return isShallow;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "partial"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "partial"
	 */
	public boolean isPartial() {
		return isPartial;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "patches"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "patches"
	 */
	public boolean isPatches() {
		return isPatches;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "low drifting"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "low drifting"
	 */
	public boolean isLowDrifting() {
		return isLowDrifting;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "blowing"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "blowing"
	 */
	public boolean isBlowing() {
		return isBlowing;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "showers"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "showers"
	 */
	public boolean isShowers() {
		return isShowers;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "thunderstorms"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "thunderstorms"
	 */
	public boolean isThunderstorms() {
		return isThunderstorms;
	}

	/**
	 * Use this function to determine if a weather condition has a descriptor
	 * of "freezing"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a descriptor of "freezing"
	 */
	public boolean isFreezing() {
		return isFreezing;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "drizzle"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "drizzle"
	 */
	public boolean isDrizzle() {
		return isDrizzle;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "rain"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "rain"
	 */
	public boolean isRain() {
		return isRain;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "snow"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "snow"
	 */
	public boolean isSnow() {
		return isSnow;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "snow grains"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "snow grains"
	 */
	public boolean isSnowGrains() {
		return isSnowGrains;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "ice crystals"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "ice crystals"
	 */
	public boolean isIceCrystals() {
		return isIceCrystals;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "ice pellets"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "ice pellets"
	 */
	public boolean isIcePellets() {
		return isIcePellets;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "hail"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "hail"
	 */
	public boolean isHail() {
		return isHail;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "small hail"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "small hail"
	 */
	public boolean isSmallHail() {
		return isSmallHail;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "unknown precipitation"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "unknown precipitation"
	 */
	public boolean isUnknownPrecipitation() {
		return isUnknownPrecipitation;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "mist"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "mist"
	 */
	public boolean isMist() {
		return isMist;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "fog"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "fog"
	 */
	public boolean isFog() {
		return isFog;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "smoke"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "smoke"
	 */
	public boolean isSmoke() {
		return isSmoke;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "volcanic ash"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "volcanic ash"
	 */
	public boolean isVolcanicAsh() {
		return isVolcanicAsh;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "widespread dust"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "widespread dust"
	 */
	public boolean isWidespreadDust() {
		return isWidespreadDust;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "sand"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "sand"
	 */
	public boolean isSand() {
		return isSand;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "haze"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "haze"
	 */
	public boolean isHaze() {
		return isHaze;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "spray"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "spray"
	 */
	public boolean isSpray() {
		return isSpray;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "dust/sand swirls"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "dust/sand swirls"
	 */
	public boolean isDustSandWhirls() {
		return isDustSandWhirls;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "squalls"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "squalls"
	 */
	public boolean isSqualls() {
		return isSqualls;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "sandstorm"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "sandstorm"
	 */
	public boolean isSandstorm() {
		return isSandstorm;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "duststorm"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "duststorm"
	 */
	public boolean isDuststorm() {
		return isDuststorm;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "funnel cloud"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "funnel cloud"
	 */
	public boolean isFunnelCloud() {
		return isFunnelCloud;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "tornado"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "tornado"
	 */
	public boolean isTornado() {
		return isTornado;
	}

	/**
	 * Use this function to determine if a weather condition has a phenomena
	 * of "water spout"
	 *
	 * @return boolean that represents whether or not this WeatherCondition has
	 * a phenomena of "water spout"
	 */
	public boolean isWaterspout() {
		return isWaterspout;
	}

    /**
     * This method will return a string that represents this weather condition
	 * using natural language (as opposed to METAR)
	 *
     * @return a string that represents the weather condition in natural language
     */
	public String getNaturalLanguageString() {
		String temp = "";

		if (isLight) {
			temp += "Light";
		} else if (isHeavy) {
			temp += "Heavy";
		} else {
			temp += "Moderate";
		}

		if (isShallow) {
			temp += " Shallow";
		} else if (isPartial) {
			temp += " Partial";
		} else if (isPatches) {
			temp += " Patches";
		} else if (isLowDrifting) {
			temp += " Low Drifting";
		} else if (isBlowing) {
			temp += " Blowing";
		} else if (isShowers) {
			temp += " Showers";
		} else if (isThunderstorms) {
			temp += " Thunderstorms";
		} else if (isFreezing) {
			temp += " Freezing";
		} else {
			// shouldn't get here
		}

		if (isDrizzle) {
			temp += " Drizzle";
		} else if (isRain) {
			temp += " Rain";
		} else if (isSnow) {
			temp += " Snow";
		} else if (isSnowGrains) {
			temp += " Snow Grains";
		} else if (isIceCrystals) {
			temp += " Ice Crystals";
		} else if (isIcePellets) {
			temp += " Ice Pellets";
		} else if (isHail) {
			temp += " Hail";
		} else if (isSmallHail) {
			temp += " Small Hail";
		} else if (isUnknownPrecipitation) {
			temp += " Unknown Precipitation";
		} else if (isMist) {
			temp += " Mist";
		} else if (isFog) {
			temp += " Fog";
		} else if (isSmoke) {
			temp += " Smoke";
		} else if (isVolcanicAsh) {
			temp += " Volcanic Ash";
		} else if (isWidespreadDust) {
			temp += " Widespread Dust";
		} else if (isSand) {
			temp += " Sand";
		} else if (isHaze) {
			temp += " Haze";
		} else if (isSpray) {
			temp += " Spray";
		} else if (isDustSandWhirls) {
			temp += " Well-developed Dust/Sand Whirls";
		} else if (isSqualls) {
			temp += " Squalls";
		} else if (isFunnelCloud) {
			temp += " Funnel Cloud/Tornado/Waterspout";
		} else if (isSandstorm) {
			temp += " Sandstorm";
		} else if (isDuststorm) {
			temp += " Duststorm";
		}

		return temp;
	}
}
