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

/***
 * A class that will read flight plans from user created files
 * 
 * @author Ron
 *
 */
public class PlanFactory {
    /***
     * Parse the file with the given name
     * @param fileName - File to open/read/parse
     * @return A collection(List) of FlightPlans that were found or null
     */
    public ExternalFlightPlan parse(String fileName) {

    	FileInputStream  inStream = null;
    	
    	// Create an input stream from the file name
    	try {
			inStream = new FileInputStream(fileName);

			// Get this files extension. We use that to find out
			// what parser is required to interpret the data.
			String ext = null;
			int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex > 0) {
			    ext = fileName.substring(dotIndex + 1);
			}

			// Define a GPX parser and use if appropriate
			PlanParser parser = new GpxPlanParser();
			if(parser.getType().equalsIgnoreCase(ext)) {
				return parser.parse(fileName, inStream);
			}

			// Define a SKYVECTOR plan object and see if that
			// can parse the data
			parser = new SkvPlanParser();
			if(parser.getType().equalsIgnoreCase(ext)) {
				return parser.parse(fileName, inStream);
			}

		// An exception is most likely an error opening the file stream.
		// we'll just ignore it
    	} catch (Exception e) { return null; }

    	// Last thing we need to do is close the input stream.
    	finally {
    		if(null != inStream) {
    			try { inStream.close(); } catch (Exception e) { }
    		}
    	}
    	
    	// Did not understand the type of input file content
    	return null;
    }
}
