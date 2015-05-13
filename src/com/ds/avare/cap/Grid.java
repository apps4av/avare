package com.ds.avare.cap;

import com.ds.avare.position.Origin;
import com.ds.avare.shapes.CAPGridShape;

public class Grid implements CoordinateAwareInterface {
	private CAPGridShape shape = null;
	
	private String number;
	private Chart chart;
	
	private LatLng northWestLimit;
	private LatLng southEastLimit;
	
	public Grid(String number, Chart chart, LatLng northWestLimit, LatLng southEastLimit) {
		super();
		this.number = number;
		this.chart = chart;
		this.northWestLimit = northWestLimit;
		this.southEastLimit = southEastLimit;
	}

	public CAPGridShape getShape() {
		if (shape == null) {
			shape = new CAPGridShape(chart.getIdentifier().toString() + " " + number);
			shape.add(northWestLimit.getLongitude(), northWestLimit.getLatitude(), false);
			shape.add(northWestLimit.getLongitude(), (northWestLimit.getLatitude() - 0.25), false);
			shape.add((northWestLimit.getLongitude() - 0.25), (northWestLimit.getLatitude() - 0.25), false);
			shape.add((northWestLimit.getLongitude() - 0.25), northWestLimit.getLatitude(), false);
			shape.add(northWestLimit.getLongitude(), northWestLimit.getLatitude(), false);
			shape.makePolygon();
		}
		
		return shape;
	}

	public String getNumber() {
		return number;
	}

	public LatLng getNorthWestLimit() {
		return northWestLimit;
	}

	public LatLng getSouthEastLimit() {
		return southEastLimit;
	}

	public boolean isWithinBoundaries(Origin origin) {
		return Boundaries.isSubjectTouchingOrigin(origin, this);
	}

	public float getTextX(Origin origin) {
		return (float)origin.getOffsetX(((getNorthWestLimit().getLongitude() + (getNorthWestLimit().getLongitude() + 0.25)) / 2));
	}

	public float getTextY(Origin origin) {
		return (float)origin.getOffsetY(((getSouthEastLimit().getLatitude() + (getSouthEastLimit().getLatitude() + 0.25)) / 2));
	}

	public String getLabel() {
		return chart.getIdentifier().toString() + " " + getNumber();
	}
}
