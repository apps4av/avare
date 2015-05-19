package com.ds.avare.cap;

import java.util.LinkedList;

import com.ds.avare.position.Coordinate;

public class Chart implements CoordinateAwareInterface {
	private ChartIdentifier identifier;
	private Coordinate northWestLimit;
	private Coordinate southEastLimit;
	private LinkedList<Grid> mGrids;
	
	public Chart(ChartIdentifier identifier, Coordinate northWestLimit, Coordinate southEastLimit) {
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

	public Coordinate getNorthWestLimit() {
		return northWestLimit;
	}

	public void setNorthWestLimit(Coordinate northWestLimit) {
		this.northWestLimit = northWestLimit;
	}

	public Coordinate getSouthEastLimit() {
		return southEastLimit;
	}

	public void setSouthEastLimit(Coordinate southEastLimit) {
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
									new Coordinate(leftLongitude, upperLatitude),
									new Coordinate(leftLongitude+0.25, upperLatitude-0.25)
							)
					);
					gridNumber++;
				}
			}
		}
		return mGrids;
	}
}
