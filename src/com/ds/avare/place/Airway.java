package com.ds.avare.place;

import java.util.LinkedList;
import java.util.Observable;

import com.ds.avare.StorageService;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

public class Airway extends Observable {

	/**
	 * Find an airway between two navaids, and return the path in GPS coordinates
	 * @param service
	 * @param start Name of starting Navaid/Fix
	 * @param name Name of airway
	 * @param end Name of ending Navaid/Fix
	 * @return
	 */
	public static LinkedList<String> find(StorageService service, String start, String name, String end) {
		
		LinkedList<String> ret = new LinkedList<String>();
		
		// Find airway start point
		Coordinate startCoord = service.getDBResource().findNavaid(start);
		if(startCoord == null) {
			return null;
		}
		// Find airway end point
		Coordinate endCoord = service.getDBResource().findNavaid(end);
		if(endCoord == null) {
			return null;
		}

		// Now find airway
		LinkedList<Coordinate> coords = service.getDBResource().findAirway(name);
		if(coords.size() <= 0) {
			return null;
		}
		
		// Now find start to end of an airway
		int startIndex = -1;
		int endIndex = -1;
		
		int i;
		double minD;
		
		// find where we start
		i = 0;
		minD = Double.MAX_VALUE;
		for(Coordinate c : coords) {
			Projection p = new Projection(c.getLongitude(), c.getLatitude(), startCoord.getLongitude(), startCoord.getLatitude());
			if(p.getDistance() < minD) {
				startIndex = i;
				minD = p.getDistance();
			}
			i++;
		}
		
		// Some sort of error
		if(startIndex < 0) {
			return null;
		}

		// find where we end
		i = 0;
		minD = Double.MAX_VALUE;
		for(Coordinate c : coords) {
			Projection p = new Projection(c.getLongitude(), c.getLatitude(), endCoord.getLongitude(), endCoord.getLatitude());
			if(p.getDistance() < minD) {
				endIndex = i;
				minD = p.getDistance();
			}
			i++;
		}

		// Some sort of error
		if(endIndex < 0) {
			return null;
		}

		// Some sort of error
		if(endIndex == startIndex) {
			return null;
		}

		// Add all of them on the route
		if(startIndex < endIndex) {
			for(i = startIndex; i < endIndex; i++) {
				Coordinate c = coords.get(i);
				String s = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS,
						name + "@" + Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude()))).getHashedName();
				ret.add(s);
			}
		}
		else {
			// Flying it reverse
			for(i = startIndex; i >= endIndex; i--) {
				Coordinate c = coords.get(i);
				String s = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS,
						name + "@" + Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude()))).getHashedName();
				ret.add(s);
			}			
		}

		/*
		 * At least three points make an airway because starting and end are navaids
		 */
		if(ret.size() < 3) {
			return null;
		}
		
		// Remove navaids
		ret.remove(ret.size() - 1);
		ret.remove(0);
		
		return ret;
	}
}
