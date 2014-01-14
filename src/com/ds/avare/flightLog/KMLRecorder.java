/*
Copyright (c) 2013, Avare software (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.flightLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import android.annotation.SuppressLint;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.shapes.CrumbsShape;
import com.ds.avare.shapes.Shape;

/**
 * Class to record GPS Position information to a file formatted in KML suitable
 * for consumption by GoogleEarth.
 * 
 * @author Ron
 *
 */
public class KMLRecorder {
	
	/**
	 * Configuration class. Contains all items that the caller can specify
	 * for how/when to record the position information
	 * 
	 * @author Ron
	 *
	 */
	public 	class Config {
	    private boolean			mClearListOnStart = false;	// Option to clear the linked list at every start
		private long			mUpdateTime       = 0;		// Time interval to record positions
		private boolean			mUseDetailedPositionReporting = false;	// Write extended position details to the KML file (verbose)
		private String			mFolder           = null;
		private long			mStartSpeed       = 20;		// Min speed to begin recording

		/**
		 * Constructor for configuration class of KMLRecorder
		 * 
		 * @param clearListOnStart true to clear list each time tracking starts
		 * @param updateTime Time interval to record position updates in seconds
		 * @param useDetailedPositionReporting Use verbose position information in KML file
		 * @param folder Folder to store the output KML file
		 * @param startSpeed Speed at which flight is considered to begin
		 */
		public Config(boolean clearListOnStart, long updateTime, boolean useDetailedPositionReporting, String folder, long startSpeed) {
			mClearListOnStart = clearListOnStart;
			mUpdateTime       = updateTime * 1000; // we use milliseconds internally
			mUseDetailedPositionReporting = useDetailedPositionReporting;
			mFolder           = folder;
			mStartSpeed       = startSpeed;
		}
	};
	
	/**
	 * Local runtime instance members. All item guaranteed to be zero or NULL
	 * when created by the system
	 */
	private Config			mConfig;				// Configuration record passed in at start() call
	private BufferedWriter  mTracksFile;			// File handle to use for writing the data
    private File            mFile;					// core file handler
    private LinkedList<GpsParams> mPositionHistory; // Stored GPS points 
	private URI 			mFileURI;				// The URI of the file created for these datapoints
	private int				mFlightStartIndex;		// When "start" is pressed, this is set to the size of our history list.
	private long			mTimeOfLastFix;			// Time the last fix we used occurred
	private CrumbsShape    mShape;
	
	/**
	 * Statics that all class instances share
	 */
    public static final String KMLFILENAMEFORMAT = "yyyy-MM-dd_HH-mm-ss";
    public static final String KMLFILENAMEEXTENTION = ".KML";
    public static final String KMLFILEPREFIX  = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
			"	<Document>\n" +
			"		<name>Flight Data by Avare</name>\n" +
			"		<Style id=\"AircraftFlight\">\n" +
			"			<LineStyle>\n" +
			"				<color>ffff00ff</color>\n" +
			"				<width>4</width>\n" +
			"			</LineStyle>\n" +
			"			<PolyStyle>\n" +
			"				<color>7fcccccc</color>\n" +
			"			</PolyStyle>\n" +
		    "		</Style>\n" + 
			"		<Style id=\"dot\">\n" + 
			"			<IconStyle>\n" +
			"				<color>FDFFFFFF</color>\n" +
			"				<scale>0.5</scale>\n" +
			"				<Icon>\n" +
			"					<href>root://icons/palette-4.png</href>\n" +
			"						<x>32</x>\n" +
			"						<y>128</y>\n" +
			"						<w>32</w>\n" +
			"						<h>32</h>\n" +
			"				</Icon>\n" +
			"			</IconStyle>\n" +
			"		</Style>\n";

    
    public static final String KMLCOORDINATESHEADER =
			"		<Placemark>\n" +
			"			<name>Avare Flight Path</name>\n" +
			"			<visibility>1</visibility>\n" +
			"			<description>3-D Flight Position Data</description>\n" +
			"			<styleUrl>#AircraftFlight</styleUrl>\n" +
			"			<LineString>\n" +
			"				<extrude>1</extrude>\n" +
			"				<altitudeMode>absolute</altitudeMode>\n" +
			"				<coordinates>\n";

    public static final String KMLCOORDINATESTRAILER =
            "				</coordinates>\n" +
    		"			</LineString>\n" +
    		"		</Placemark>\n";

    public static final String KMLTRACKPOINT =
		    "		<Placemark>\n" +
		    "           <name>Track %d</name>" +
		    "			<description><![CDATA[\n" +
		    "				Time: %s\n" + 
		    "				Altitude: %f\n" +
		    "				Bearing: %f\n" +
		    "				Speed: %f\n" +
		    "				Long: %f\n" +
		    "				Lat: %f]]>\n" +
		    "			</description>\n" +
		    "			<styleUrl>#dot</styleUrl>\n" +
		    "			<Point>\n" +
		    "				<altitudeMode>absolute</altitudeMode>\n" +
		    "				<coordinates>%f,%f,%f</coordinates>\n" +
		    "			</Point>\n" +
		    "		</Placemark>\n";

    public static final String KMLFILESUFFIX = 
    		"	</Document>\n" +
    		"</kml>\n";

    /**
     * Default constructor. Allocate the list that holds our collection
     * of gps points
     */
    public KMLRecorder(){
    	mPositionHistory = new LinkedList<GpsParams>();
    	mShape = new CrumbsShape();
    }
    
    /** 
     * Are we actively recording data to a file ? 
     * @return true if we are, false otherwise
     */
    public boolean isRecording() {
    	return mTracksFile != null;
    }
    
    /**
     * Stop saving datapoints to the file and the historical list
     * @return A URI of the file just closed
     */
	public URI stop(){
	    mShape.clearShape();
    	if(mTracksFile != null) {
    		// File operations can cause exceptions and we need to account for that
    		try {
    			mTracksFile.write(KMLCOORDINATESTRAILER);	// Close off the coordinates section
    			if(mConfig.mUseDetailedPositionReporting == true) {
	    			// Write out each track point of this flight as its own entry. This
	    			// saves out more detail than just lat/long of the point.
	    			for(int idx = mFlightStartIndex, max = mPositionHistory.size(); idx < max; idx++) {
						GpsParams gpsParams = mPositionHistory.get(idx);
						String trackPoint = String.format(KMLTRACKPOINT,
								idx + 1,
	    						new Date(gpsParams.getTime()).toString(),
	    						gpsParams.getAltitude(),
	    						gpsParams.getBearing(),
	    						gpsParams.getSpeed(),
	    						gpsParams.getLongitude(),
	    						gpsParams.getLatitude(),
	    						gpsParams.getLongitude(),
	    						gpsParams.getLatitude(),
	    						gpsParams.getAltitude() * .3048 /* meter per feet */
	    						);
						mTracksFile.write(trackPoint);
	    			}
    			}
    			
    			// Close off the overall KML file now
        		mTracksFile.write(KMLFILESUFFIX);	// The last of the file data 
    			mTracksFile.close();				// close the file
    		} catch (IOException ioe) { }

    		// Clear out our control objects
    		mTracksFile = null;	// No track file anymore
    		return mFileURI;	// return with the URI of the file we just closed
    	}
    	return null;
    }
    
    /**
     * Begin recording position points to a file and our memory linked list
     * @param folder Directory to store the KML file
     * @param updateTime Number of seconds between datapoints
     */
    @SuppressLint("SimpleDateFormat")
	public void start(Config config) {
        mShape.clearShape();
    	mConfig = config;
    	
		// Build the file name based upon the current date/time
		String fileName = new SimpleDateFormat(KMLFILENAMEFORMAT).format(Calendar.getInstance().getTime()) + KMLFILENAMEEXTENTION;
    	mFile = new File(mConfig.mFolder, fileName);
    	
    	// File handling can throw some exceptions
    	try {
    		
    		// Ensure the full path to the file area exists
    		File mDirPath = new File(mConfig.mFolder, "");
    		if(mDirPath.exists() == false) {
    			mDirPath.mkdirs();
    		}
    		
    		// If the file does not exist, then create it. 
        	if(mFile.exists() == false){
        		mFile.createNewFile();	// Create the new file
        	}

        	// Save off the URI of this file we just created. This value
        	// is returned at the stop() method.
        	mFileURI = mFile.toURI();
        	
        	// Create a new writer, then a buffered writer for this file
        	FileWriter fileWriter = new FileWriter(mFile);
    		mTracksFile = new BufferedWriter(fileWriter, 8192);

    		// Write out the opening file prefix
    		mTracksFile.write(KMLFILEPREFIX);			// Overall file prelude
    		mTracksFile.write(KMLCOORDINATESHEADER);	// Open coordinates data

            // If we are supposed to clear the linked list each time
            // we start timing then do so now
            if(mConfig.mClearListOnStart == true) {
            	clearPositionHistory();
            }
            
            // Mark the starting entry of our history list. This is required in order
            // to save off the individual points of our trip at close
            mFlightStartIndex = mPositionHistory.size();
            
    	} catch (IOException ioe) { 
    	    
    	}
    }
    
    /**
     * The positionhistory is a collection of points that a caller can use
     * to examine historical position information
     * @return LinkedList of coordinates of all previous positions. Most recent is
     * at the end.
     */
    public LinkedList<GpsParams> getPositionHistory() {
    	return mPositionHistory;
    }
    
    /**
     * Clear out the linked list of historical position data
     */
    public void clearPositionHistory() {
    	mPositionHistory.clear();
    }
    
    /**
     * This object requires notification of when the position changes. This is
     * done by the caller sending the information periodically via the GpsParams
     * object to this method. If it is greater than our stall speed,  if we have a file
     * open to write it to.
     * @param gpsParams Current location information
     */
    public void setGpsParams(GpsParams gpsParams) {
    	if(mTracksFile != null) {
    		// File open means we are actively tracking the position
	    	if(((gpsParams.getTime() - mTimeOfLastFix) > mConfig.mUpdateTime)) {
	    		// A proper amount of time has lapsed since we last reported
				if((gpsParams.getSpeed() >= mConfig.mStartSpeed)) {
					// We are traveling greater than "flight" speed
					
					mTimeOfLastFix = gpsParams.getTime();	// Mark this time as last 'fix' time

	        		// Write out the position. Convert the altitude from feet to meters for the KML file
	    			try {
	    				mTracksFile.write ("\t\t\t\t\t" + gpsParams.getLongitude() + "," + 
	    												  gpsParams.getLatitude() + "," + 
	    												 (gpsParams.getAltitude() * .3048) + "\n");
	
	    				// Add this position to our linked list for possible display
	    				// on the charts
	    				mPositionHistory.add(GpsParams.copy(gpsParams));
	    				mShape.updateShape(gpsParams);
	    			} catch (IOException ioe) { }
	    		}
    		}
    	}
	}
    
    /**
     * Config setting to auto-clear the linked list every time start
     * is called.
     * @param clearListOnStart boolean to indicate whether to clear list or keep it when start is called.
     */
    public void setClearListOnStart(boolean clearListOnStart){
    	mConfig.mClearListOnStart = clearListOnStart;
    }
    
    /**
     * 
     * @return
     */
    public Shape getShape() {
       return mShape; 
    }
}
