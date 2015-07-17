/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.userDefinedWaypoints;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

import com.ds.avare.place.Destination;

/***
 * Class to read user defined waypoints from a GPX formatted file
 * 
 * A GPX file is an XML formatted file from Garmin that defines Waypoints in a file according
 * to the following syntax:
 * 
 * <?xml version="1.0" encoding="UTF-8" ?>
	<gpx version="1.1" creator="" xmlns="http://www.topografix.com/GPX/1/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
		<wpt lat="43.98431" lon="-88.56628">
			<name>NameOfTheWayPoint</name>
			<desc>Description of the waypoint</desc>
		</wpt>
	</gpx>
 *
 * See http://www.topografix.com/GPX/1/1/gpx.xsd for a full spec of this schema
 * 
 * @author Ron
 *
 */
public class GpxUDWParser extends UDWParser {
    private static final String NS = null;
    private static final String GPX = "gpx";
    private static final String WPT = "wpt";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String NAME = "name";
    private static final String CREATOR = "creator";
    private static final String VFRGPSPROCEDURES = "vfrgpsprocedures";

	@Override
	public List<Waypoint> parse(FileInputStream inputStream) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();
            return readGPX(parser);
        } catch (Exception e) { }
        	
        return null;
	}

    // The root tag should be "<gpx>", search for the opening "<wpt>" tag
    //
    private List<Waypoint> readGPX(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Waypoint> entries = new ArrayList<Waypoint>();
        String creator = null;
        
        parser.require(XmlPullParser.START_TAG, NS, GPX);	// We must be inside the <gpx> tag now

        // Pull off attributes here that we are interested in
        for(int idx = 0; idx < parser.getAttributeCount(); idx++) {
        	String attrName = parser.getAttributeName(idx);
        	String attrValue = parser.getAttributeValue(idx);
        	if (attrName.equals(CREATOR)) {
            	creator = attrValue;
        	}
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(WPT)) {
            	Waypoint wpt = readWPT(parser); 
                entries.add(wpt);
                
                if(null != creator) {
	                if(creator.contains(VFRGPSPROCEDURES)) {
	                	wpt.setMarkerType(Waypoint.MT_NONE);
	                	wpt.setVisible(false);
	                }
                }
            } else {
                skip(parser);
            }
        }  
        return entries;
    }

    // We are in the WPT tag, Get the LAT/LON attributes then search
    // for NAME or DESC
    //
    private Waypoint readWPT(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, WPT);

        String name = null;
        float lat = 0;
        float lon = 0;

        // LAT and LON are attributes of this container
        for(int idx = 0; idx < parser.getAttributeCount(); idx++) {
        	String attrName = parser.getAttributeName(idx);
        	String attrValue = parser.getAttributeValue(idx);
        	if (attrName.equals(LAT)) {
            	lat = Float.parseFloat(attrValue);
        	} else if (attrName.equals(LON)) {
            	lon = Float.parseFloat(attrValue);
        	}
        }
        
        // The NAME and DESCRIPTION are sub tags under here
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String nodeName = parser.getName();
            if (nodeName.equals(NAME)) {
                name = readNAME(parser);
            } else {
                skip(parser);
            }
        }
        
        // We've got all the data we're going to get from this entry
        return  new Waypoint(name, Destination.UDW, lon, lat, false, Waypoint.MT_CYANDOT, true);
    }

    // Extract NAME
    //
    private String readNAME(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, NAME);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, NAME);
        return name;
    }
      
    // Read the text from the current tag
    //
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /***
     * Skip this next entire sub-block of XML tags
     * 
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
     }
}
