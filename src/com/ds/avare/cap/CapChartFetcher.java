package com.ds.avare.cap;

import java.util.LinkedList;

import com.ds.avare.position.Coordinate;

/**
 * Class to fetch CAP Chart data.  Currently, it's hard-coded, but someday, this may be pulled
 * out into a data file.
 */
public class CapChartFetcher {
	
	private LinkedList<Chart> mCharts;
	
	public CapChartFetcher() {
		mCharts = null;
	}
	
	public LinkedList<Chart> getCharts() {
		if (mCharts == null) {
			mCharts = new LinkedList<Chart>();
			mCharts.add(new Chart(ChartIdentifier.SEA, new Coordinate(-125, 49), new Coordinate(-117, 44.5)));
		}
		
		return mCharts;
	}
}
