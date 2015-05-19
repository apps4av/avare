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
			mCharts.add(new Chart(ChartIdentifier.GTF, new Coordinate(-117, 49), new Coordinate(-109, 44.5)));
			mCharts.add(new Chart(ChartIdentifier.BIL, new Coordinate(-109, 49), new Coordinate(-101, 44.5)));
			mCharts.add(new Chart(ChartIdentifier.MSP, new Coordinate(-101, 49), new Coordinate(-93, 44.5)));
			mCharts.add(new Chart(ChartIdentifier.GRB, new Coordinate(-93, 48.25), new Coordinate(-85, 44)));
			mCharts.add(new Chart(ChartIdentifier.LHN, new Coordinate(-85, 48), new Coordinate(-77, 44)));
			mCharts.add(new Chart(ChartIdentifier.MON, new Coordinate(-77, 48), new Coordinate(-69, 44)));
			mCharts.add(new Chart(ChartIdentifier.HFX, new Coordinate(-69, 48), new Coordinate(-61, 44)));
			mCharts.add(new Chart(ChartIdentifier.LMT, new Coordinate(-125, 44.5), new Coordinate(-117, 40)));
			mCharts.add(new Chart(ChartIdentifier.SLC, new Coordinate(-117, 44.5), new Coordinate(-109, 40)));
			mCharts.add(new Chart(ChartIdentifier.CYS, new Coordinate(-109, 44.5), new Coordinate(-101, 40)));
			mCharts.add(new Chart(ChartIdentifier.OMA, new Coordinate(-101, 44.5), new Coordinate(-93, 40)));
			mCharts.add(new Chart(ChartIdentifier.ORD, new Coordinate(-93, 44), new Coordinate(-85, 40)));
			mCharts.add(new Chart(ChartIdentifier.DET, new Coordinate(-85, 44), new Coordinate(-77, 40)));
			mCharts.add(new Chart(ChartIdentifier.NYC, new Coordinate(-77, 44), new Coordinate(-69, 40)));
			mCharts.add(new Chart(ChartIdentifier.SFO, new Coordinate(-125, 40), new Coordinate(-118, 36)));
			mCharts.add(new Chart(ChartIdentifier.LAS, new Coordinate(-118, 40), new Coordinate(-111, 35.75)));
			mCharts.add(new Chart(ChartIdentifier.DEN, new Coordinate(-111, 40), new Coordinate(-104, 35.75)));
			mCharts.add(new Chart(ChartIdentifier.ICT, new Coordinate(-104, 40), new Coordinate(-97, 36)));
			mCharts.add(new Chart(ChartIdentifier.MKC, new Coordinate(-97, 40), new Coordinate(-90, 36)));
			mCharts.add(new Chart(ChartIdentifier.STL, new Coordinate(-91, 40), new Coordinate(-84, 36)));
			mCharts.add(new Chart(ChartIdentifier.LUK, new Coordinate(-85, 40), new Coordinate(-78, 36)));
			mCharts.add(new Chart(ChartIdentifier.DCA, new Coordinate(-79, 40), new Coordinate(-72, 36)));
			mCharts.add(new Chart(ChartIdentifier.LAX, new Coordinate(-121.5, 36), new Coordinate(-115, 32)));
			mCharts.add(new Chart(ChartIdentifier.PHX, new Coordinate(-116, 35.75), new Coordinate(-109, 31.25)));
			mCharts.add(new Chart(ChartIdentifier.ABQ, new Coordinate(-109, 36), new Coordinate(-102, 32)));
			mCharts.add(new Chart(ChartIdentifier.DFW, new Coordinate(-102, 36), new Coordinate(-95, 32)));
			mCharts.add(new Chart(ChartIdentifier.MEM, new Coordinate(-95, 36), new Coordinate(-88, 32)));
			mCharts.add(new Chart(ChartIdentifier.ATL, new Coordinate(-88, 36), new Coordinate(-81, 32)));
			mCharts.add(new Chart(ChartIdentifier.CLT, new Coordinate(-81, 36), new Coordinate(-75, 32)));
			mCharts.add(new Chart(ChartIdentifier.ELP, new Coordinate(-109, 32), new Coordinate(-103, 28)));
			mCharts.add(new Chart(ChartIdentifier.SAT, new Coordinate(-103, 32), new Coordinate(-97, 28)));
			mCharts.add(new Chart(ChartIdentifier.HOU, new Coordinate(-97, 32), new Coordinate(-91, 28)));
			mCharts.add(new Chart(ChartIdentifier.MSY, new Coordinate(-91, 32), new Coordinate(-85, 28)));
			mCharts.add(new Chart(ChartIdentifier.JAX, new Coordinate(-85, 32), new Coordinate(-79, 28)));
			mCharts.add(new Chart(ChartIdentifier.BRO, new Coordinate(-103, 28), new Coordinate(-97, 24)));
			mCharts.add(new Chart(ChartIdentifier.MIA, new Coordinate(-83, 28), new Coordinate(-77, 24)));
		}
		
		return mCharts;
	}
}
