/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.cap;

import java.util.LinkedList;

import com.ds.avare.position.Coordinate;

/**
 * Class to fetch CAP Chart data.  Currently, it's hard-coded, but someday, this may be pulled
 * out into a data file.
 * 
 * @author postalservice14, zkhan
 *
 */
public class CapChartFetcher {
	
	// 15 minute, quarter degree
	public static final double GRID_SIZE = 0.25;
	
	private LinkedList<Chart> mCharts;
	private static CapChartFetcher mInstance;

	/*
	 * This is a singleton
	 */
	private CapChartFetcher() {
		mCharts = new LinkedList<Chart>();
		mCharts.add(new Chart("SEA", new Coordinate(-125, 49), new Coordinate(-117, 44.5)));
		mCharts.add(new Chart("GTF", new Coordinate(-117, 49), new Coordinate(-109, 44.5)));
		mCharts.add(new Chart("BIL", new Coordinate(-109, 49), new Coordinate(-101, 44.5)));
		mCharts.add(new Chart("MSP", new Coordinate(-101, 49), new Coordinate(-93, 44.5)));
		mCharts.add(new Chart("GRB", new Coordinate(-93, 48.25), new Coordinate(-85, 44)));
		mCharts.add(new Chart("LHN", new Coordinate(-85, 48), new Coordinate(-77, 44)));
		mCharts.add(new Chart("MON", new Coordinate(-77, 48), new Coordinate(-69, 44)));
		mCharts.add(new Chart("HFX", new Coordinate(-69, 48), new Coordinate(-61, 44)));
		mCharts.add(new Chart("LMT", new Coordinate(-125, 44.5), new Coordinate(-117, 40)));
		mCharts.add(new Chart("SLC", new Coordinate(-117, 44.5), new Coordinate(-109, 40)));
		mCharts.add(new Chart("CYS", new Coordinate(-109, 44.5), new Coordinate(-101, 40)));
		mCharts.add(new Chart("OMA", new Coordinate(-101, 44.5), new Coordinate(-93, 40)));
		mCharts.add(new Chart("ORD", new Coordinate(-93, 44), new Coordinate(-85, 40)));
		mCharts.add(new Chart("DET", new Coordinate(-85, 44), new Coordinate(-77, 40)));
		mCharts.add(new Chart("NYC", new Coordinate(-77, 44), new Coordinate(-69, 40)));
		mCharts.add(new Chart("SFO", new Coordinate(-125, 40), new Coordinate(-118, 36)));
		mCharts.add(new Chart("LAS", new Coordinate(-118, 40), new Coordinate(-111, 35.75)));
		mCharts.add(new Chart("DEN", new Coordinate(-111, 40), new Coordinate(-104, 35.75)));
		mCharts.add(new Chart("ICT", new Coordinate(-104, 40), new Coordinate(-97, 36)));
		mCharts.add(new Chart("MKC", new Coordinate(-97, 40), new Coordinate(-90, 36)));
		mCharts.add(new Chart("STL", new Coordinate(-91, 40), new Coordinate(-84, 36)));
		mCharts.add(new Chart("LUK", new Coordinate(-85, 40), new Coordinate(-78, 36)));
		mCharts.add(new Chart("DCA", new Coordinate(-79, 40), new Coordinate(-72, 36)));
		mCharts.add(new Chart("LAX", new Coordinate(-121.5, 36), new Coordinate(-115, 32)));
		mCharts.add(new Chart("PHX", new Coordinate(-116, 35.75), new Coordinate(-109, 31.25)));
		mCharts.add(new Chart("ABQ", new Coordinate(-109, 36), new Coordinate(-102, 32)));
		mCharts.add(new Chart("DFW", new Coordinate(-102, 36), new Coordinate(-95, 32)));
		mCharts.add(new Chart("MEM", new Coordinate(-95, 36), new Coordinate(-88, 32)));
		mCharts.add(new Chart("ATL", new Coordinate(-88, 36), new Coordinate(-81, 32)));
		mCharts.add(new Chart("CLT", new Coordinate(-81, 36), new Coordinate(-75, 32)));
		mCharts.add(new Chart("ELP", new Coordinate(-109, 32), new Coordinate(-103, 28)));
		mCharts.add(new Chart("SAT", new Coordinate(-103, 32), new Coordinate(-97, 28)));
		mCharts.add(new Chart("HOU", new Coordinate(-97, 32), new Coordinate(-91, 28)));
		mCharts.add(new Chart("MSY", new Coordinate(-91, 32), new Coordinate(-85, 28)));
		mCharts.add(new Chart("JAX", new Coordinate(-85, 32), new Coordinate(-79, 28)));
		mCharts.add(new Chart("BRO", new Coordinate(-103, 28), new Coordinate(-97, 24)));
		mCharts.add(new Chart("MIA", new Coordinate(-83, 28), new Coordinate(-77, 24)));
	}

	/*
	 * Get the instance
	 */
	public static CapChartFetcher getInstance() {
		if(mInstance == null) {
			mInstance = new CapChartFetcher();
		}
		return mInstance;
	}

	/**
	 * Get charts and their boundaries
	 * @return
	 */
	public LinkedList<Chart> getCharts() {
		return mCharts;
	}
}
