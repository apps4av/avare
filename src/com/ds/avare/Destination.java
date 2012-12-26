/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import java.io.File;
import java.util.LinkedHashMap;

import java.util.Observable;
import android.location.Location;
import android.os.AsyncTask;

/**
 * @author zkhan
 * Stores everything about destination, its name (ICAO code)
 * Does databse query to find the destination as well.
 */
public class Destination extends Observable {

    /**
     * 
     */
    private String mName;
    /**
     * Cache it for database query from async task
     */
    private ImageDataSource mDataSource;
    /**
     * 
     */
    private double mDistance;
    /**
     * 
     */
    private double mBearing;
    /**
     * If a destination is found?
     */
    private boolean mFound;
    /**
     * ETA to destination
     */
    private String mEta;
        
    /*
     * Its lon/lat
     */
    private double mLond;
    private double mLatd;

    private String mDiagramFound;
    
    private Preferences mPref;
    
    private StorageService mService;
    
    private boolean mLooking;
    
    /**
     * Contains all info in a hash map for the destination
     * Dozens of parameters in a linked map because simple map would rearrange the importance
     */
    private LinkedHashMap <String, String>mParams;
    
	/**
	 * @param name
	 * @param DataSource
	 */
	public Destination(String name, Preferences pref, StorageService service) {
	    mName = name.toUpperCase();
	    mFound = mLooking = false;
	    mService = service;
	    mDataSource = mService.getDBResource(); 
	    mPref = pref;
	    mEta = new String("--:--");
    	mParams = new LinkedHashMap<String, String>();
    	mDiagramFound = null;
    	mLond = mLatd = 0;
	}
    	
	/**
     * Update the current speed, lat, lon, that will update
     * ETA, distance and bearing to the destination
	 * @param params
	 */
	public void updateTo(GpsParams params) {
	    
	    /*
	     */
        double mLon = params.getLongitude();
        double mLat = params.getLatitude();
        double speed = params.getSpeed();

		if(!mFound) {
			return;
		}		

		/*
		 * Project and find distance
		 */
		Projection p = new Projection(mLon, mLat, mLond, mLatd);
		
    	mDistance = p.getDistance();

    	mBearing = p.getBearing();
    	
    	/*
    	 * ETA when speed != 0
    	 */
    	if(speed != 0) {
	    	int etahr = (int)(mDistance / speed);
	    	int etamin =  (int)Math.round((mDistance / speed - (double)etahr) * 60);
	    	String hr = String.format("%02d", etahr);
	    	String min = String.format("%02d", etamin);
        	mEta = new String(hr + ":" + min);        	
    	}
    	else {
    	    /*
    	     * NaN avoid
    	     */
    		mEta = new String("--:--");
    	}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    /*
	     * For display purpose
	     */
		if(!mFound) {
			return(mName + "? ");
		}
		else {
			return(mParams.get("Location ID") + ":" + 
			        Math.round(mDistance) + "nm " +  mEta + " " + 
			        Math.round(mBearing) + '\u00B0');
		}
	}
	
	/**
	 * Database  query to find destination
	 */
	public void find() {
	    /*
	     * Do in background as database queries are disruptive
	     */
        mLooking = true;
        DataBaseLocationTask locmDataBaseTask = new DataBaseLocationTask();
        locmDataBaseTask.execute(mName);
	}
	
    /**
     * @author zkhan
     * Query for destination task
     */
    private class DataBaseLocationTask extends AsyncTask<Object, Void, Boolean> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {        	
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

	        if(null == mDataSource) {
	        	return false;
        	}
        	
	        boolean ret = mDataSource.findDestination((String)vals[0], mParams);
	        if(ret) {
	            /*
	             * Found destination extract its airport diagram
	             */
	            String file = mPref.mapsFolder() + "/plates/" + mName + ".jpg";
	            File f = new File(file);
	            if(f.exists()) {
	                mDiagramFound = file;
	                mService.loadDiagram(Destination.this.getDiagram());
	            }
	            else {
                    mService.loadDiagram(null);
	                mDiagramFound = null;
	            }
	        }
			return(ret);
        }
        

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
        	/*
        	 * This runs on UI
        	 */
			mFound = result;
			if(mFound) {
    		    mLond = Double.parseDouble(mParams.get("ARP Longitude"));
    		    mLatd = Double.parseDouble(mParams.get("ARP Latitude"));
			}
			/*
			 * Anyone watching if destination found?
			 */
			Destination.this.setChanged();
            Destination.this.notifyObservers(Boolean.valueOf(mFound));
            mLooking = false;
	    }
    }
    
    /**
     * @return
     */
    public boolean isFound() {
    	return(mFound);
    }

    /**
     * @return
     */
    public boolean isLooking() {
        return(mLooking);
    }

    /**
     * @return
     */
    public String getDiagram() {
        return(mDiagramFound);
    }

    /**
     * @return
     */
    public String getFacilityName() {
    	return(mParams.get("Facility Name"));
    }

    /**
     * @return
     */
    public String getID() {
        return(mName);
    }

    /**
     * @return
     */
    public BitmapHolder getBitmap() {
        return(mService.getDiagram());
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getParams() {
    	return(mParams);
    }
    
    /**
     * @return
     */
    public double getBearing() {
        return mBearing;
    }

    /**
     * @return
     */
    public double getDistance() {
        return mDistance;
    }

    /**
     * 
     * @return
     */
    public Location getLocation() {
        Location l = new Location("");
        l.setLatitude(mLatd);
        l.setLongitude(mLond);
        return l;
    }    
}
