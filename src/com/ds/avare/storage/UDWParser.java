/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.storage;

import java.io.FileInputStream;
import java.util.List;

/***
 * Base class to implement a parser for User Defined Waypoints
 * All UDW Parsers are to derive from this class
 * @author Ron
 *
 */
public abstract class UDWParser {

    // Class to hold the definition of a placemark
    //
    public static class Placemark {
        public final String mName;
        public final String mDescription;
        public final float  mLat;
        public final float  mLon;
        public final float  mAlt;
        public final boolean mShowDist;
        public final int mMarkerType;

        public Placemark(String name, String description, float lat, float lon, float alt, boolean showDist, int markerType) {
            this.mName = name;
            this.mDescription = description;
            this.mLat = lat;
            this.mLon = lon;
            this.mAlt = alt;
            this.mShowDist = showDist;
            this.mMarkerType = markerType;
        }
        
        public static final int CYANDOT = 0;
    }

	// Methods that each derived class need to implement
	public abstract List<Placemark> parse(FileInputStream inputStream);
}
