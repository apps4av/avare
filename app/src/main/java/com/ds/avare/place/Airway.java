/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.place;

import java.util.LinkedList;
import java.util.Observable;

import com.ds.avare.StorageService;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;

public class Airway extends Observable {

	// Max segment length is 500NM this is to keep airways in AK/HI separated from US48
	public static final double MAX_SEGMENT_LENGTH = 500;
	
	// If coordinates are same
	private static boolean isSame(Coordinate c0, Coordinate c1) {
		return c0.getLatitude() == c1.getLatitude() && c0.getLongitude() == c1.getLongitude();
	}
	
	/**
	 * Test case BOS V16 V167 V44 CMK DXR
	 * Find where two airways intersect.
	 * @return
	 */
	public static Coordinate findIntersectionOfAirways(StorageService service, String name, LinkedList<Coordinate> coords0) {

		LinkedList<Coordinate> coords1 = service.getDBResource().findAirway(name);
		if(coords1.size() <= 0) {
			return null;
		}

		// Find the point that intersects. This is a complex operation that works on 2 dimension.
		// XXX need speed improvement
		for(Coordinate c0 : coords0) {
			for(Coordinate c1 : coords1) {
				if(isSame(c0, c1)) {
					return c0;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Find an airway between two navaids, and return the path in GPS coordinates
	 * @param service
	 * @param start Name of starting Navaid/Fix
	 * @param name Name of airway
	 * @param end Name of ending Navaid/Fix
	 * @return
	 */
	public static LinkedList<String> find(StorageService service, String start, String name, String end) {
		
		String match = "[A-Z]\\d+";
		
		LinkedList<String> ret = new LinkedList<String>();
		
		if(!name.matches(match)) {
			// Not an airway
			return null;
		}
		
		// Now find airway
		LinkedList<Coordinate> coords = service.getDBResource().findAirway(name);
		if(coords.size() <= 0) {
			return null;
		}

		Coordinate startCoord;
		if(start.matches(match)) {
			startCoord = findIntersectionOfAirways(service, start, coords);
		}
		else {
			// Find airway start point from navaid
			startCoord = service.getDBResource().findNavaid(start);
		}
		if(startCoord == null) {
			return null;
		}

		// Find airway end point
		Coordinate endCoord;
		if(end.matches(match)) {
			endCoord = findIntersectionOfAirways(service, end, coords);
		}
		else {
			// Find airway end point from navaid
			endCoord = service.getDBResource().findNavaid(end);
		}
		if(endCoord == null) {
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
			double dist = Projection.getStaticDistance(c.getLongitude(), c.getLatitude(), startCoord.getLongitude(), startCoord.getLatitude());
			if(dist < minD) {
				startIndex = i;
				minD = dist;
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
			double dist = Projection.getStaticDistance(c.getLongitude(), c.getLatitude(), endCoord.getLongitude(), endCoord.getLatitude());
			if(dist < minD) {
				endIndex = i;
				minD = dist;
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
			Coordinate lastcoord = coords.get(startIndex);
			for(i = startIndex; i < endIndex; i++) {
				Coordinate c = coords.get(i);
				// Keep far away airways out
				if(Projection.getStaticDistance(c.getLongitude(), c.getLatitude(), lastcoord.getLongitude(), lastcoord.getLatitude()) > MAX_SEGMENT_LENGTH) {
					continue;
				}
				lastcoord = c;
                // find a fix/navaid here.
                StringPreference nav = service.getDBResource().getNavaidOrFixFromCoordinate(lastcoord);

                if(nav == null) {
                    // not found, add GPS
                    nav = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS,
                            name + "@" + Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude())));
                }
                ret.add(nav.getHashedName());
			}
		}
		else {
			// Flying it reverse
			Coordinate lastcoord = coords.get(startIndex);
			for(i = startIndex; i >= endIndex; i--) {
				Coordinate c = coords.get(i);
				// Keep far away airways out
				if(Projection.getStaticDistance(c.getLongitude(), c.getLatitude(), lastcoord.getLongitude(), lastcoord.getLatitude()) > MAX_SEGMENT_LENGTH) {
					continue;
				}
				lastcoord = c;

				// find a fix/navaid here.
                StringPreference nav = service.getDBResource().getNavaidOrFixFromCoordinate(lastcoord);
                if(nav == null) {
                    // not found, add GPS
                    nav = (new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS,
                            name + "@" + Helper.truncGeo(c.getLatitude()) + "&" + Helper.truncGeo(c.getLongitude())));
                }

				ret.add(nav.getHashedName());
			}			
		}

		/*
		 * Check for not found
		 */
		if(ret.size() <= 0) {
			return null;
		}
		
		return ret;
	}
}
