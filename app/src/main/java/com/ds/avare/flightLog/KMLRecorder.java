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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RunnableFuture;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.shapes.CrumbsShape;
import com.ds.avare.shapes.Shape;
import com.ds.avare.utils.Helper;

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
	}

	/**
	 * Writer class. This class will write the track history to disk
	 */
	private class Writer {
		private static final long		WriteDelta = 300000;	// update the file this often (5min)
		private String					mFolder;				// folder to store file in
		private String 					mFilename;				// filename to use
		private List<GpsParams> 		mList;					// list of gps positions
		private int						mStartPosition;			// start position in history
		private File 					mFile;					// file information
		private long					mLastWrite;				// last time the file was written

		/**
		 * Writer constructor
		 * @param folder			folder to write to
		 * @param filename			filename to write to
		 * @param list				list of gps coordinates
         * @param startPosition		start position within list
         */
		public Writer(String folder,String filename,List<GpsParams> list,int startPosition) {
			mFolder = folder;
			mFilename = filename;
			mList = list;
			mStartPosition = startPosition;
			initialise();
		}

		/**
		 * Initialise the writer
		 */
		private void initialise() {
			mLastWrite=System.currentTimeMillis();
			try {
				mFile = new File(mFolder, mFilename);
				// Ensure the full path to the file area exists
				File mDirPath = new File(mConfig.mFolder, "");
				if (!mDirPath.exists()) {
					mDirPath.mkdirs();
				}

				// If the file does not exist, then create it.
				if (!mFile.exists()) {
					mFile.createNewFile();    // Create the new file
				}
			}
			catch(Exception e) {
			}
		}

		/**
		 * Write the file
		 * @return the URI of the written file on success
         */
		public URI write() {
			URI uri=null;
			// don't write an empty file
			if(mList.size()>mStartPosition) {
				// File handling can throw some exceptions
				try {
					// Create a new writer, then a buffered writer for this file
					FileWriter fileWriter = new FileWriter(mFile);
					BufferedWriter tracksFile = new BufferedWriter(fileWriter, 8192);

					// Write out the opening file prefix
					tracksFile.write(KMLFILEPREFIX);            // Overall file prelude
					tracksFile.write(KMLCOORDINATESHEADER);    // Open coordinates data
					for (int i = mStartPosition; i < mList.size(); ++i) {
						GpsParams gpsParams = mList.get(i - mStartPosition);
						tracksFile.write("\t\t\t\t\t" + gpsParams.getLongitude() + "," +
								gpsParams.getLatitude() + "," +
								(gpsParams.getAltitude() * .3048) + "\n");
					}
					tracksFile.write(KMLCOORDINATESTRAILER);    // Close off the coordinates section

					for (int i = mStartPosition; i < mList.size(); ++i) {
						GpsParams gpsParams = mList.get(i - mStartPosition);
						String trackPoint = String.format(KMLTRACKPOINT,
								1 + i - mStartPosition,
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
						tracksFile.write(trackPoint);
					}

					// Close off the overall KML file now
					tracksFile.write(KMLFILESUFFIX);    // The last of the file data

					tracksFile.close();
					// Return the URI on success
					uri = mFile.toURI();

				} catch (Exception e) { // Catch all exceptions here
					System.out.println("Error: "+e);
				}
			}

			return uri;
		}

		/**
		 * New position update
		 * @param gpsParams	position
         */
		public void newPosition(GpsParams gpsParams) {
			long now=System.currentTimeMillis();
			// greater than delta?
			if(now-mLastWrite>=WriteDelta) {
				mLastWrite=now;
				// write in the background
				(new Thread(new Runnable() {
					public void run() {
						write();
					}
				})).start();
			}
		}
	}
	
	/**
	 * Local runtime instance members. All item guaranteed to be zero or NULL
	 * when created by the system
	 */
	private Config			mConfig;				// Configuration record passed in at start() call
	private List<GpsParams> mPositionHistory; 		// Stored GPS points
	private int				mFlightStartIndex;		// When "start" is pressed, this is set to the size of our history list.
	private GpsParams		mLastFix;				// the last time we wrote a position			
	private CrumbsShape    	mShape;
	private Writer			mWriter;				// file writer
	
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
			"					<x>32</x>\n" +
			"					<y>128</y>\n" +
			"					<w>32</w>\n" +
			"					<h>32</h>\n" +
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
		    "           <name>%d</name>\n" +
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
    	mPositionHistory
				= (List<GpsParams>) Collections.synchronizedList(new ArrayList<GpsParams>());
    	mShape = new CrumbsShape();
    	mLastFix = new GpsParams(null);
    }
    
    /** 
     * Are we actively recording data to a file ? 
     * @return true if we are, false otherwise
     */
    public boolean isRecording() {
    	return mWriter != null;
    }
    
    /**
     * Stop saving datapoints to the file and the historical list
     * @return A URI of the file just closed
     */
	public URI stop(){
		URI uri=null;
	    mShape.clearShape();
    	if(mWriter != null) {
			// write the last of the data
    		uri=mWriter.write();
			// clean up
			mWriter=null;
    	}
    	return uri;
    }
    

	/**
	 * Start with default config
	 */
	public void start() {
        KMLRecorder.Config config = this.new Config(
                true,   /* always remove tracks from display on start */
                30,     /* Max of 30 seconds between position updates */
                true,   /* use verbose details */
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar + "com.ds.avare" + File.separatorChar + "Tracks",
                3);     // Adjust down to 3 knots to capture taxi
        start(config);
	}
	
    /**
     * Begin recording position points to a file and our memory linked list
     * @param config Details on how/when to write the data
     */
    @SuppressLint("SimpleDateFormat")
	public void start(Config config) {
		// Build the file name based upon the current date/time
		String fileName = new SimpleDateFormat(KMLFILENAMEFORMAT)
								.format(Calendar.getInstance().getTime()) + KMLFILENAMEEXTENTION;

        mShape.clearShape();
    	mConfig = config;

		// If we are supposed to clear the linked list each time
		// we start timing then do so now
		if(mConfig.mClearListOnStart) {
			clearPositionHistory();
		}

		// Mark the starting entry of our history list. This is required in order
		// to save off the individual points of our trip at close
		mFlightStartIndex = mPositionHistory.size();

		// create a new writer
		mWriter=new Writer(mConfig.mFolder,fileName,mPositionHistory,mFlightStartIndex);
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

    	if(mWriter == null) {
    		// File closed means nothing to do
    		return;
    	}
		
		if((gpsParams.getSpeed() < mConfig.mStartSpeed)) {
			// Not going fast enough yet to record
			return;
		}
		
		// Set if we are to record this position
		//
		boolean bRecordPoint = false;
		
		// If the speed has changed more than 5 knots
		if (Math.abs(gpsParams.getSpeed() - mLastFix.getSpeed()) > 5) {
			bRecordPoint = true;
		}
		
		// If the altitude is 100' or greater different
		if((Math.abs(gpsParams.getAltitude() - mLastFix.getAltitude())) > 100) {
			bRecordPoint = true;
		}
		
		// If the bearing is 15 degrees or more different - that's 24 samples per 360 turn
    	if(Helper.angularDifference(gpsParams.getBearing(), mLastFix.getBearing()) > 15) {
    		bRecordPoint = true;
    	}

    	// If the time of the last point and now is greater than our configured time 
		if(((gpsParams.getTime() - mLastFix.getTime()) > mConfig.mUpdateTime)) {
			bRecordPoint = true;
		}

		// After all those tests, if nothing says to record, then get out of here
		if (!bRecordPoint) {
			return;
		}

		// Add this position to our linked list for possible display
		// on the charts
		mPositionHistory.add(GpsParams.copy(gpsParams));
		mShape.updateShape(gpsParams);

		// update the writer
		mWriter.newPosition(gpsParams);

		// Update our last known position
		mLastFix = gpsParams;

	}
    
    /**
     * 
     * @return The shape of our tracks to draw
     */
    public Shape getShape() {
       return mShape; 
    }
}
