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
 * Represents a single Runway Visual Range (RVR) element that appears in a METAR
 * report
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.2 $
 */
public class RunwayVisualRange {
	int runwayNumber = 0;			// runway number
	char approachDirection = ' ';	// L/R/C
	char reportableModifier = ' ';	// P - below, M - above
	int lowestReportable = 0;		// (ft)
	int highestReportable = 0;		// (ft)

	public RunwayVisualRange() {
	}

    /**
     *
     * @param runwayNumber the part of a METAR RVR token which represents a
     * runway number
     */
	protected void setRunwayNumber(int runwayNumber) {
		this.runwayNumber = runwayNumber;
	}

    /**
     *
     * @param direction the part of a METAR RVR token which represents an
     * approach direction (e.g. 'L', 'R')
     */
	protected void setApproachDirection(char direction) {
		this.approachDirection = direction;
	}

    /**
     *
     * @param modifier the part of a METAR RVR token which represents a
     * modifier used to specify if the visual range is above or below the
     * following value
     */
	protected void setReportableModifier(char modifier) {
		this.reportableModifier = modifier;
	}

    /**
     *
	 * @param lowestReportable the part of a METAR RVR token which represents
	 * the lowest reportable value for visual range
     */
	protected void setLowestReportable(int lowestReportable) {
		this.lowestReportable = lowestReportable;
	}

    /**
     *
	 * @param highestReportable the part of a METAR RVR token which represents
	 * the highest reportable value for visual range
     */
	protected void setHighestReportable(int highestReportable) {
		this.highestReportable = highestReportable;
	}

    /**
     *
     * @return a string that represents the runway visual range in natural language
     */
	public String getNaturalLanguageString() {
		String temp = new Integer(runwayNumber).toString();

		temp += approachDirection;

		if (reportableModifier == 'M') {
			temp += " less than";
		} else if (reportableModifier == 'P') {
			temp += " greater than";
		}

		if (highestReportable > 0) {
			temp += " " + new Integer(lowestReportable);
			temp += " to " + new Integer(highestReportable) + "feet.";
		} else {
			temp += " " + new Integer(lowestReportable) + "feet.";
		}

		return temp;
	}
}
