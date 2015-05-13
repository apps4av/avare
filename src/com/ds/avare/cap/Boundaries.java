package com.ds.avare.cap;

import com.ds.avare.position.Origin;

public class Boundaries {
	public static boolean isOriginWithinSubject(Origin origin, CoordinateAwareInterface subject) {
		return origin.getLongitudeLeft() >= subject.getNorthWestLimit().getLongitude() &&
				origin.getLongitudeRight() <= subject.getSouthEastLimit().getLongitude() &&
				origin.getLatitudeUpper() <= subject.getNorthWestLimit().getLatitude() && 
				origin.getLatitudeLower() >= subject.getSouthEastLimit().getLatitude();
	}
	
	public static boolean longitudeLeftWithinBoundaries(Origin origin, CoordinateAwareInterface subject) {
		return subject.getNorthWestLimit().getLongitude() >= origin.getLongitudeLeft();
	}

	public static boolean longitudeRightWithinBoundaries(Origin origin, CoordinateAwareInterface subject) {
		return origin.getLongitudeRight() >= subject.getSouthEastLimit().getLongitude();
	}

	public static boolean latitudeUpperWithinBoundaries(Origin origin, CoordinateAwareInterface subject) {
		return origin.getLatitudeUpper() <= subject.getNorthWestLimit().getLatitude();
	}

	public static boolean latitudeLowerWithinBoundaries(Origin origin, CoordinateAwareInterface subject) {
		return origin.getLatitudeLower() >= subject.getSouthEastLimit().getLatitude();
	}

	public static boolean isSubjectTouchingOrigin(Origin origin, CoordinateAwareInterface subject) {
		int score = 0;
		
		if (isLatitudeUpperWithinOrigin(origin, subject)) {
			score++;
		}
		
		if (isLatitudeLowerWithinOrigin(origin, subject)) {
			score++;
		}
		
		if (score < 2 && isLongitudeLeftWithinOrigin(origin, subject)) {
			score++;
		}
		
		if (score < 2 && isLongitudeRightWithinOrigin(origin, subject)) {
			score++;
		}
		
		return score >= 2;
	}

	private static boolean isLatitudeUpperWithinOrigin(Origin origin,
			CoordinateAwareInterface subject) {
		return subject.getNorthWestLimit().getLatitude() <= origin.getLatitudeUpper() && subject.getNorthWestLimit().getLatitude() >= origin.getLatitudeLower();
	}
	
	private static boolean isLatitudeLowerWithinOrigin(Origin origin,
			CoordinateAwareInterface subject) {
		return subject.getSouthEastLimit().getLatitude() <= origin.getLatitudeUpper() && subject.getSouthEastLimit().getLatitude() >= origin.getLatitudeLower();
	}
	
	private static boolean isLongitudeLeftWithinOrigin(Origin origin,
			CoordinateAwareInterface subject) {
		return subject.getNorthWestLimit().getLongitude() >= origin.getLongitudeLeft() && subject.getNorthWestLimit().getLongitude() <= origin.getLongitudeRight();
	}
	
	private static boolean isLongitudeRightWithinOrigin(Origin origin,
			CoordinateAwareInterface subject) {
		return subject.getSouthEastLimit().getLongitude() >= origin.getLongitudeLeft() && subject.getSouthEastLimit().getLongitude() <= origin.getLongitudeRight();
	}
}
