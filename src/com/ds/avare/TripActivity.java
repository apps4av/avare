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



import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.ds.avare.R;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.Helper;

import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author zkhan trip / hotel / car etc. activity
 */
public class TripActivity extends Activity {

    /**
     * This view display location on the map.
     */
    private WebView mWebView;
    private Button mFindButton;
    AlertDialog mAlertDialog;
    private Toast mToast;
    private ProgressBar mProgress;
    

    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;
        
    /**
     * App preferences
     */

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if (location != null && mService != null) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity) this.getParent()).showMapTab();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Helper.setTheme(this);
        super.onCreate(savedInstanceState);
     
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.trip, null);
        setContentView(view);
        
        mProgress = (ProgressBar)view.findViewById(R.id.trip_load_progress);
        
        mWebView = (WebView)view.findViewById(R.id.trip_mainpage);
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        mWebView.setWebViewClient(new WebViewClient() {
   
        	/*
        	 * For hiding loading bar
        	 * (non-Javadoc)
        	 * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
        	 */
        	@Override
        	public void onPageFinished(WebView view, String url) {
        		mProgress.setVisibility(View.INVISIBLE);
        	}

        	/*
        	 * We let browser open the result page's deep links
        	 */
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        		startActivity(browserIntent);
        		return true;
        	}
    	});
        
        
        /*
         * Show zoom
         */
        mWebView.getSettings().setBuiltInZoomControls(true);
        
        /*
         * Create toast beforehand so multiple clicks don't throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        /*
         * This is hotel find button that has all the action from it
         */
        mFindButton = (Button)view.findViewById(R.id.trip_button_find);
        mFindButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	if(mService != null) {
            		/*
            		 * Reload
            		 */
                    final View findView = layoutInflater.inflate(R.layout.hotel, null);
           
                    // arrival and departure date obtain from today and tomorrow
                	EditText from = (EditText)findView.findViewById(R.id.hotel_datefrom_text);
                	EditText to = (EditText)findView.findViewById(R.id.hotel_dateto_text);

                	Calendar now = Calendar.getInstance(); // Current time
                	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                	from.setText(sdf.format(now.getTime())); // Get Date String according to date format
                	
                	now.add(Calendar.DAY_OF_MONTH, 1);
                	to.setText(sdf.format(now.getTime()));
                    
                    AlertDialog.Builder askd = new AlertDialog.Builder(TripActivity.this);
                    askd.setView(findView);
                    askd.setCancelable(false);
                    mAlertDialog = askd.create();
                    /*
                     * Cancel last progress
                     */
          		    mProgress.setVisibility(View.INVISIBLE);
                    
          		    /*
          		     * Find button on dialog
          		     */
                    mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Find), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                        	/*
                        	 * Make a URL from stuff
                        	 */
                        	String url = makeURL(findView);
                        	if(url != null) {
                        		/*
                        		 * Show loading progress for page in case internet is slow, it can take time
                        		 */
                      		    mProgress.setVisibility(View.VISIBLE);
                        		mWebView.loadUrl(url);
                        	}
                        	else {
                        		mWebView.loadData("<h1>!<h1>", "text/html", null);
                        	}
                        	dialog.dismiss();
                        }
                    });
                    mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        	dialog.dismiss();
                        }                    	
                    });
                    mAlertDialog.show();

            	}
            }
            
        });


        mService = null;
    }

    private boolean isDateWrong(String date) {
    	
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
		sdf.setLenient(false);
 
		try {
			//if not valid, it will throw ParseException
			sdf.parse(date);
		} 
		catch (Exception e) {
			return true;
		}
    	return false;
    }
    
    /**
     * Make a URL from view
     */
    private String makeURL(View view) {
    	
    	/*
    	 * Get all the user input and make a URL
    	 */
    	EditText from = (EditText)view.findViewById(R.id.hotel_datefrom_text);
    	EditText to = (EditText)view.findViewById(R.id.hotel_dateto_text);
    	String fromtext = from.getText().toString();
    	String totext = to.getText().toString();
    	Spinner price = (Spinner)view.findViewById(R.id.hotel_price);
    	Spinner stars = (Spinner)view.findViewById(R.id.hotel_stars);
    	Spinner distance = (Spinner)view.findViewById(R.id.hotel_radius);
    	Spinner adults = (Spinner)view.findViewById(R.id.hotel_adults);
    	Spinner child1 = (Spinner)view.findViewById(R.id.hotel_child_1);
    	Spinner child2 = (Spinner)view.findViewById(R.id.hotel_child_2);
    	Spinner child3 = (Spinner)view.findViewById(R.id.hotel_child_3);
    	Spinner child4 = (Spinner)view.findViewById(R.id.hotel_child_4);
    	Spinner child5 = (Spinner)view.findViewById(R.id.hotel_child_5);

    	/*
    	 * Set Dest from other screens
    	 */
    	if(mService == null || mService.getDestination() == null) {
    		mToast.setText(getString(R.string.ValidDest));
    		mToast.show();
    		return null;
    	}

    	/*
    	 * Validate date
    	 */
    	if(isDateWrong(fromtext) || isDateWrong(totext)) {
    		mToast.setText(getString(R.string.IncorrectDateFormat));
    		mToast.show();
    		return null;
    	}
    	
    	/*
    	 * Validate coordinates
    	 */
    	if(Helper.isLatitudeSane(mService.getDestination().getLocation().getLatitude()) && 
    			Helper.isLongitudeSane(mService.getDestination().getLocation().getLongitude())) {
    		
    		/*
    		 * Children are comma separated
    		 */
    		String children = "";
    		children +=	child1.getSelectedItemPosition() == 0 ? "" : child1.getSelectedItem().toString() + ",";
    		children += child2.getSelectedItemPosition() == 0 ? "" : child2.getSelectedItem().toString() + ",";
    		children += child3.getSelectedItemPosition() == 0 ? "" : child3.getSelectedItem().toString() + ",";
    		children += child4.getSelectedItemPosition() == 0 ? "" : child4.getSelectedItem().toString() + ",";
    		children += child5.getSelectedItemPosition() == 0 ? "" : child5.getSelectedItem().toString() + ",";
    		children = children.replaceAll(",$", "");
    		if(children.length() > 0) {
    			children = "&children=" + children;
    		}
    		
    		/*
    		 * URL as the PHP needs
    		 */
    		String url = "https://apps4av.net/expedia.php?" 
    				+ "latitude=" + mService.getDestination().getLocation().getLatitude() + "&"
    				+ "longitude=" + mService.getDestination().getLocation().getLongitude() + "&"
    				+ "radius=" + distance.getSelectedItem().toString() + "&"
    				+ "minstar=" + stars.getSelectedItem().toString() + "&"
    				+ "maxrate=" + price.getSelectedItem().toString() + "&"
    				+ "adults=" + adults.getSelectedItem().toString() + "&"
    				+ "arrival=" + fromtext + "&"
    				+ "departure=" + totext +
    				children;
    		return url;
    	}
    	
		mToast.setText(getString(R.string.ValidDest));
		mToast.show();

    	return null;
    }
    
    /** Defines callbacks for service binding, passed to bindService() */
    /**
     * 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.ServiceConnection#onServiceConnected(android.content
         * .ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            /*
             * We've bound to LocalService, cast the IBinder and get
             * LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.ServiceConnection#onServiceDisconnected(android.content
         * .ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        
        Helper.setOrientationAndOn(this);

        /*
         * Registering our receiver Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);

        try {
        	mAlertDialog.dismiss();
        }
        catch(Exception e) {
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
