/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.externalFlightPlan;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.ds.avare.place.Destination;
import com.ds.avare.userDefinedWaypoints.Waypoint;

import android.util.Xml;

/***
 * Class to read a flight plan from a GPX formatted file
 * 
 * A GPX file is an XML formatted file from Garmin that defines the plan in a file according
 * to the following syntax:
 * 
 * <?xml version="1.0" encoding="UTF-8" ?>
	<gpx version="1.1" creator="" xmlns="http://www.topografix.com/GPX/1/1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
		<rte>
			<name>NameOfThePlan</name>
			<desc>Description of the plan</desc>
			<cmt>comment to display in bottom left of screen</cmt>
		
			<rtept lat="43.98431" lon="-88.56628">
				<name>Point 1</name>
				<desc>Description of the waypoint</desc>
			</rtept>

			<rtept lat="43.98431" lon="-88.56628">
				<name>Point 2</name>
				<desc>Description of the waypoint</desc>
			</rtept>
		</rte>
	</gpx>
 *
 * See http://www.topografix.com/GPX/1/1/gpx.xsd for a full spec of this schema
 * 
 * @author Ron
 *
 */
public class GpxPlanParser  extends PlanParser {
    private static final String NS = null;
    private static final String GPX = "gpx";
    private static final String RTE = "rte";
    private static final String RTEPT = "rtept";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String NAME = "name";
    private static final String CMT  = "cmt";
    
    @Override 
    public String getType() {
    	return GPX;
    }
    
	@Override
	public ExternalFlightPlan parse(String fileName, FileInputStream inputStream) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();
            return readGPX(parser);
        } catch (Exception e) { }
        return null;
	}

	@Override
	public void generate(String fileName, FileOutputStream outputStream, ExternalFlightPlan externalFlightPlan) {
	}
	
    // The root tag should be "<gpx>", search for the opening "<rte>" tag
    //
    private ExternalFlightPlan readGPX(XmlPullParser parser) throws XmlPullParserException, IOException {
    	ExternalFlightPlan plan = null;
    	
        parser.require(XmlPullParser.START_TAG, NS, GPX);	// We must be inside the <gpx> tag now

        // Now process all the sub-tags
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(RTE)) {
                plan = readRTE(parser);
            } else {
                skip(parser);
            }
        }

        return plan;
    }

    // Found the RTE tag. Read the NAME and CMT from here along with all the RTEPT data
    private ExternalFlightPlan readRTE(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, RTE);

        String name = gUndef;
    	String cmt  = "";
    	List<Waypoint> points = new ArrayList<Waypoint>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String nodeName = parser.getName();
            if (nodeName.equals(NAME)) {
                name = readNAME(parser);
            } else if (nodeName.equals(CMT)) {
                cmt = readCMT(parser);
            } else if (nodeName.equals(RTEPT)) {
            	points.add(readRTEPT(parser));
            } else {
                skip(parser);
            }
        }
        
        // We have all the data for the plan. Create one and return
        return new ExternalFlightPlan(name, cmt, GPX, points);
    }
        
    /***
     * We are in the RTEPT tag, Get the LAT/LON attributes then search
     * for NAME or CMT - return that info as a Waypoint (RoutePoint)
     * 
     * @param parser The XML parser
     * @return the Waypoint object
     * @throws XmlPullParserException
     * @throws IOException
     */
    private Waypoint readRTEPT(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, NS, RTEPT);

        String name = gUndef;	// Waypoint name
        String cmt  = "";		// Comment about the waypoint
        float lat = 0;	// latitude
        float lon = 0;	// longitude

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
        
        // The NAME, DESC, and CMT are sub tags under here
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String nodeName = parser.getName();
            if (nodeName.equals(NAME)) {
                name = readNAME(parser);
            } else if (nodeName.equals(CMT)) {
                cmt = readCMT(parser);
            } else {
                skip(parser);
            }
        }
        
        // Create a new waypoint from this data. Set the comment and make sure it's not
        // visible. It is made visible when the plan is enabled
        Waypoint wp = new Waypoint(name, Destination.UDW, lon, lat, false, Waypoint.MT_NONE, false);
        wp.setCmt(cmt);
        wp.setVisible(false);
        
        return wp;
    }

    // Extract NAME
    //
    private String readNAME(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, NAME);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, NAME);
        return name;
    }
      
    // Extract CMT
    //
    private String readCMT(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NS, CMT);
        String cmt = readText(parser);
        parser.require(XmlPullParser.END_TAG, NS, CMT);
        return cmt;
    }

    // Read the text from the current tag
    //
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        return text;
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
