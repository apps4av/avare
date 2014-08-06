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

public class UDWFactory {

	final String TXT = "txt";
	final String XML = "xml";
	final String CSV = "csv";
	final String KML = "kml";
	final String GPX = "gpx";
	
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
    
    public List<Placemark> parse(String fileName) {

    	// Create an input stream from the file name
    	try {
			FileInputStream  inStream = new FileInputStream(fileName);
			
			// Based upon the extension, create the proper type of parser for 
			// the file stream. Assume we are a text file
			String ext = TXT;
			
			int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex > 0) {
			    ext = fileName.substring(dotIndex + 1).toLowerCase();
			}

			// Parse the KML file that comes from GoogleEarth
			if (ext.contentEquals(KML)) {
				try {
			    	KmlUDWParser kmlParser = new KmlUDWParser();
			    	return kmlParser.parse(inStream);
				} catch (Exception e) { return null; } 
			}
			
			// Parse a CSV file
			else if (ext.contentEquals(CSV)) {
			
			}
			
			// Parse a TXT file
			else if (ext.contentEquals(TXT)) {
			
			}

			// Parse an XML file
			else if (ext.contentEquals(XML)) {
			
			}

			// Parse a GPX file
			else if (ext.contentEquals(GPX)) {
			
			}

			// Most likely an error opening the file stream
    	} catch (Exception e) { return null; }
    	
    	// Did not understand the type of input file content
    	return null;
    }
}
