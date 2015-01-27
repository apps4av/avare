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
