package com.ds.avare.cap;

import java.util.LinkedList;

public class Chart implements CoordinateAwareInterface {
	private ChartIdentifier identifier;
	private LatLng northWestLimit;
	private LatLng southEastLimit;
	private LinkedList<Grid> mGrids;
	
	public Chart(ChartIdentifier identifier, LatLng northWestLimit, LatLng southEastLimit) {
		this.identifier = identifier;
		this.northWestLimit = northWestLimit;
		this.southEastLimit = southEastLimit;
	}

	public ChartIdentifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(ChartIdentifier identifier) {
		this.identifier = identifier;
	}

	public LatLng getNorthWestLimit() {
		return northWestLimit;
	}

	public void setNorthWestLimit(LatLng northWestLimit) {
		this.northWestLimit = northWestLimit;
	}

	public LatLng getSouthEastLimit() {
		return southEastLimit;
	}

	public void setSouthEastLimit(LatLng southEastLimit) {
		this.southEastLimit = southEastLimit;
	}

	public LinkedList<Grid> getGrids() {
		if (mGrids == null) {
			mGrids = new LinkedList<Grid>();
			
			int gridNumber = 1;
			for (double upperLatitude = getNorthWestLimit().getLatitude(); upperLatitude > getSouthEastLimit().getLatitude(); upperLatitude -= 0.25) {
				for (double leftLongitude = getNorthWestLimit().getLongitude(); leftLongitude < getSouthEastLimit().getLongitude(); leftLongitude += 0.25) {
					mGrids.add(
							new Grid(
									String.valueOf(gridNumber),
									this,
									new LatLng(upperLatitude, leftLongitude),
									new LatLng(upperLatitude-0.25, leftLongitude+0.25)
							)
					);
					gridNumber++;
				}
			}
		}
		return mGrids;
	}
}
