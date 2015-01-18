/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare;

import java.io.File;

import com.ds.avare.place.Destination;
import com.ds.avare.plan.LmfsInterface;
import com.ds.avare.plan.LmfsPlan;
import com.ds.avare.plan.LmfsPlanList;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.PossibleEmail;
import com.ds.avare.utils.WeatherHelper;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppInterface {
    private Context mContext;
    private StorageService mService; 
    private WeatherTask mWeatherTask;
    private Thread mWeatherThread;
    private WebView mWebView;
    private Preferences mPref;
	private LmfsPlanList mFaaPlans;
    private GenericCallback mCallback;

    private static final int MSG_NOTBUSY = 9;
    private static final int MSG_BUSY = 10;
    private static final int MSG_FILL_FORM = 13;
    private static final int MSG_ERROR = 15;
    private static final int MSG_FAA_PLANS = 16;
    private static final int MSG_SET_EMAIL = 17;

    /** 
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c, WebView v, GenericCallback cb) {
        mWebView = v;
        mContext = c;
        mPref = new Preferences(c);
        mWeatherTask = null;
        mWeatherTask = new WeatherTask();
        mWeatherThread = new Thread(mWeatherTask);
        mWeatherThread.start();
        mCallback = cb;
    }
    
    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
    }

    /** 
     * Get weather data async
     */
    public void getWeather() {
        mWeatherThread.interrupt();
    }

    /**
     * Fill plan form with data stored
     */
    @JavascriptInterface
    public void fillPlan() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// Fill in from storage, this is going to be mostly reflecting the user's most 
    	// used settings in the form
    	LmfsPlan pl = new LmfsPlan(mPref.getLMFSPlan());
    	
    	// If plan has valid BASE origin and destinations, fill them in
    	if(mService != null) {
    		pl.setFromPlan(mService.getPlan());
    	}
    	
    	// Fill form
    	Message m = mHandler.obtainMessage(MSG_FILL_FORM, (Object)(
    	    	"'" +  pl.flightRules  + "'," +
    			"'" +  pl.aircraftIdentifier + "'," +
    			"'" +  pl.departure + "'," +
    			"'" +  pl.destination + "'," +
    			"'" +  pl.departureInstant + "'," + 
    			"'" +  LmfsPlan.durationToTime(pl.flightDuration) + "'," +
    			"'" +  pl.altDestination1 + "'," + 
    			"'" +  pl.altDestination2 + "'," + 
    			"'" +  pl.aircraftType + "'," +
    			"'" +  pl.numberOfAircraft + "'," +
    			"'" +  pl.heavyWakeTurbulence + "'," +
    			"'" +  pl.aircraftEquipment + "'," +
    			"'" +  pl.speedKnots + "'," + 
    			"'" +  pl.altitudeFL + "'," +
    			"'" +  LmfsPlan.durationToTime(pl.fuelOnBoard) + "'," + 
    			"'" +  pl.pilotData + "'," +
    			"'" +  pl.peopleOnBoard + "'," + 
    			"'" +  pl.aircraftColor + "'," +
    			"'" +  pl.route + "'," +
    			"'" +  pl.type + "'," +
    			"'" +  pl.remarks + "'"
    			));
    	mHandler.sendMessage(m);
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /** 
     * Select the plan on the FAA list
     */
    @JavascriptInterface
    public void moveTo(int index) {
    	if(null == mFaaPlans) {
    		return;
    	}
    	
    	// refresh
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mFaaPlans.mSelectedIndex = index;
    	mHandler.sendEmptyMessage(MSG_FAA_PLANS);

    	mHandler.sendEmptyMessage(MSG_NOTBUSY);

    }

    
    /** 
     * File an FAA plan and save it
     */
    @JavascriptInterface
    public void filePlan(
    	String flightRules,
    	String aircraftIdentifier,
    	String departure,
    	String destination,
    	String departureInstant, 
    	String flightDuration,
    	String altDestination1, 
    	String altDestination2, 
    	String aircraftType,
    	String numberOfAircraft,
    	String heavyWakeTurbulence,
    	String aircraftEquipment,
    	String speedKnots, 
    	String altitudeFL,
    	String fuelOnBoard, 
    	String pilotData,
    	String peopleOnBoard, 
    	String aircraftColor,
    	String route,
    	String type,
    	String remarks) {
        
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	LmfsPlan pl = new LmfsPlan();
    	pl.flightRules = flightRules;
    	pl.aircraftIdentifier = aircraftIdentifier;
    	pl.departure = departure;
    	pl.destination = destination;
    	pl.departureInstant = LmfsPlan.getTimeFromInput(departureInstant);
    	pl.flightDuration = LmfsPlan.getDurationFromInput(flightDuration);
    	pl.altDestination1 = altDestination1; 
    	pl.altDestination2 = altDestination2; 
    	pl.aircraftType = aircraftType;
    	pl.numberOfAircraft = numberOfAircraft;
    	pl.heavyWakeTurbulence = heavyWakeTurbulence;
    	pl.aircraftEquipment = aircraftEquipment;
    	pl.speedKnots = speedKnots; 
    	pl.altitudeFL = altitudeFL;
    	pl.fuelOnBoard = LmfsPlan.getDurationFromInput(fuelOnBoard); 
    	pl.pilotData = pilotData;
    	pl.peopleOnBoard = peopleOnBoard; 
    	pl.aircraftColor = aircraftColor;
    	pl.route = route;
    	pl.type = type;
    	pl.remarks = remarks;
 
    	// Save user input for auto fill
    	mPref.saveLMFSPlan(pl.makeJSON());
    	
    	// Now file and show error messages
    	LmfsInterface infc = new LmfsInterface(mContext);
    	infc.fileFlightPlan(pl);
    	String err = infc.getError();
    	if(null == err) {
    		// success filing
    		getPlans();
    		return;
    	}
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);
    	
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    
    /**
     * Close, open plan at FAA
     */
    @JavascriptInterface
    public void planChangeState(String action, String arg) {
    	if(null == mFaaPlans || null == mFaaPlans.getPlans() || mFaaPlans.mSelectedIndex >= mFaaPlans.getPlans().size()) {
    		return;
    	}
    	
    	/*
    	 * Do the action of the plan
    	 */
    	LmfsInterface infc = new LmfsInterface(mContext);

    	String err = null;
    	String id = mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).getId();
    	String ver = mFaaPlans.getPlans().get(mFaaPlans.mSelectedIndex).versionStamp;
    	if(id == null) {
    		return;
    	}
    	mHandler.sendEmptyMessage(MSG_BUSY);
    	if(action.equals("Activate")) {
    		// Activate plan with given ID
    		infc.activateFlightPlan(id, ver, arg);
    	}
    	else if(action.equals("Close")) {
    		// Activate plan with given ID
    		infc.closeFlightPlan(id, arg);
    	}
    	else if(action.equals("Cancel")) {
    		// Activate plan with given ID
    		infc.cancelFlightPlan(id);
    	}
    	err = infc.getError();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    	if(null == err) {
    		// success changing, update state
    		getPlans();
    		return;
    	}
    	
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);    	
    }


    /** 
     * Get a list of FAA plans
     */
    @JavascriptInterface
    public void getPlans() {      
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	LmfsInterface infc = new LmfsInterface(mContext);

    	mFaaPlans = infc.getFlightPlans();
    	String err = infc.getError();
    	if(null == err) {
    		// success filing
    		err = mContext.getString(R.string.Success);
    	}
    	
    	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)err);
    	mHandler.sendMessage(m);
    	
    	mHandler.sendEmptyMessage(MSG_FAA_PLANS);

    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    public void setEmail() {
        // Set email in page for user to know where to register with
        mHandler.sendEmptyMessage(MSG_SET_EMAIL);
    }

    /**
     * @author zkhan
     *
     */
    private class WeatherTask implements Runnable {

        private boolean running = true;

        /* (non-Javadoc)
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Weather");
            
            while(running) {
    
                try {
                    Thread.sleep(1000 * 3600 * 100);
                }
                catch (Exception e) {
                    
                }

            	mHandler.sendEmptyMessage(MSG_BUSY);

                String Pirep = "";
                String Metar = "";
                String Taf = "";
    
                String miles = "30";
                String planf = "";
                String plan = "";
                if(null == mService) {
                	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)mContext.getString(R.string.WeatherPlan));
                	mHandler.sendMessage(m);
                	mHandler.sendEmptyMessage(MSG_NOTBUSY);
                    continue;
                }
    
                int num = mService.getPlan().getDestinationNumber();
                if(num < 2) {
                    /*
                     * Not a route.
                     */
                	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)mContext.getString(R.string.WeatherPlan));
                	mHandler.sendMessage(m);
                	mHandler.sendEmptyMessage(MSG_NOTBUSY);
                    continue;
                }
                for(int i = 0; i < num; i++) {
                    Location l = mService.getPlan().getDestination(i).getLocation();
                    planf += l.getLongitude() + "," + l.getLatitude() + ";";
                    plan += mService.getPlan().getDestination(i).getID() + "(" +
                            mService.getPlan().getDestination(i).getType() + ") ";
                }
                if(planf.equals("")) {
                	Message m = mHandler.obtainMessage(MSG_ERROR, (Object)mContext.getString(R.string.WeatherPlan));
                	mHandler.sendMessage(m);
                	mHandler.sendEmptyMessage(MSG_NOTBUSY);
                    continue;
                }                
                
                /*
                 *  Get PIREP
                 */
                try {
                    String out = NetworkHelper.getPIREPSPlan(planf, miles);
                    String outm[] = out.split("::::");
                    for(int i = 0; i < outm.length; i++) {
                        outm[i] = WeatherHelper.formatPirepHTML(outm[i], mPref.isWeatherTranslated());
                        Pirep += "<font size='5' color='white'>" + outm[i] + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Pirep = mContext.getString(R.string.WeatherError);
                }
    
                try {
                    /*
                     *  Get TAFs 
                     */
                    String out = NetworkHelper.getTAFPlan(planf, miles);
                    String outm[] = out.split("::::");
                    for(int i = 0; i < outm.length; i++) {
                        String taf = WeatherHelper.formatWeatherHTML(outm[i], mPref.isWeatherTranslated());
                        String vals[] = taf.split(" ");
                        taf = WeatherHelper.formatVisibilityHTML(WeatherHelper.formatTafHTML(WeatherHelper.formatWindsHTML(taf.replace(vals[0], ""), mPref.isWeatherTranslated()), mPref.isWeatherTranslated()));
                        Taf += "<b><font size='5' color='white'>" + vals[0] + "</b><br>";
                        Taf += "<font size='5' color='white'>" + taf + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Taf = mContext.getString(R.string.WeatherError);
                }
                
                try {
                    /*
                     * 
                     */
                    String out = NetworkHelper.getMETARPlan(planf, miles);
                    String outm[] = out.split("::::");
                    for(int i = 0; i < outm.length; i++) {
                        String vals[] = outm[i].split(",");
                        String vals2[] = vals[1].split(" ");
                        String color = WeatherHelper.metarColorString(vals[0]);
                        Metar += "<b><font size='5' + color='" + color + "'>" + vals2[0] + "</b><br>";
                        Metar += "<font size='5' color='" + color + "'>" + WeatherHelper.formatMetarHTML(vals[1].replace(vals2[0], ""), mPref.isWeatherTranslated()) + "<br></br>";
                    }
                }
                catch(Exception e) {
                    Metar = mContext.getString(R.string.WeatherError);
                }
      
                String nam = "";
                /*
                 * NAM MOS exists for airports only
                 */
                for(int ap = 0; ap < num; ap++) {
                    Destination d = mService.getPlan().getDestination(ap);
                    if(d != null) {
                        if(d.getType().equals(Destination.BASE)) {
                            nam += NetworkHelper.getNAMMET(d.getID()); 
                        }
                    }
                }
                
                plan = "<font size='5' color='white'>" + plan + "</font><br></br>";
                plan = "<form>" + plan.replaceAll("'", "\"") + "</form>";
                Metar = "<font size='6' color='white'>METARs</font><br></br>" + Metar; 
                Metar = "<form>" + Metar.replaceAll("'", "\"") + "</form>";
                Taf = "<font size='6' color='white'>TAFs</font><br></br>" + Taf; 
                Taf = "<form>" + Taf.replaceAll("'", "\"") + "</form>";
                Pirep = "<font size='6' color='white'>PIREPs</font><br></br>" + Pirep; 
                Pirep = "<form>" + Pirep.replaceAll("'", "\"") + "</form>";
                nam = "<font size='6' color='white'>Forecast</font><br></br>" +  
                        WeatherHelper.getNamMosLegend() + nam;
                nam = "<form>" + nam.replaceAll("'", "\"") + "</form>";

                String time = NetworkHelper.getVersion("", "weather");
                String weather = time + "<br></br>" + plan + Metar + Taf + Pirep + nam;
                
                // Read weather template
                String html = Helper.readFromAssetsFile("weather.html", mContext);
                // Fill in weather where the placeholder is then write to a file in download folder
                String fpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/weather_" + time + ".html";
                Helper.writeFile(html.replace("placeholder", weather), fpath);
                // Send to browser.
        		Intent i = new Intent(Intent.ACTION_VIEW);
        		File file = new File(fpath);
        		Uri uri = Uri.fromFile(file);
        		i.setDataAndType(uri, "multipart/related");
        		mContext.startActivity(i);
            	mHandler.sendEmptyMessage(MSG_NOTBUSY);
            }        
        }
    }
    
    /**
     * 
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            
        	if(MSG_NOTBUSY == msg.what) {
        		mCallback.callback((Object)WeatherActivity.UNSHOW_BUSY, null);
        	}
        	else if(MSG_BUSY == msg.what) {
        		mCallback.callback((Object)WeatherActivity.SHOW_BUSY, null);
        	}
           	else if(MSG_FILL_FORM == msg.what) {	
            	String func = "javascript:plan_fill(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ERROR == msg.what) {	
        		mCallback.callback((Object)WeatherActivity.MESSAGE, msg.obj);
        	}
        	else if(MSG_FAA_PLANS == msg.what) {
        		/*
        		 * Fill the table of plans
        		 */
        		if(mFaaPlans.getPlans() == null) {
        			return;
        		}
        		String p = "";
        		int i = 0;
        		// Sent out plans as text separated by commas like selected,name,state
        		for (LmfsPlan pl : mFaaPlans.getPlans()) {
        			p += ((i == mFaaPlans.mSelectedIndex) ? "1" : "0") + "," + pl.departure + "-" + pl.destination + "-" + pl.aircraftIdentifier + "," + pl.currentState + ",";
        			i++;
        		}
        		String func = "javascript:set_faa_plans('" + p + "')";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_SET_EMAIL == msg.what) {
	    		String func = "javascript:set_email('" + PossibleEmail.get(mContext) + "')";
	        	mWebView.loadUrl(func);
        	}
        }
    };

    /**
     * 
     */
    public void cleanup() {
        mWeatherTask.running = false;
        mWeatherThread.interrupt();
    }
}