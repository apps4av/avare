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


import java.util.Timer;
import java.util.TimerTask;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

/**
 * @author zkhan
 * An activity that deals with flight plans - loading, creating, deleting and activating
 */
public class PlanActivity extends Activity {

    /**
     * This view display location on the map.
     */
    private WebView mWebView;
    private EditText mSearchText;
    private Button mNextButton;
    private Button mLastButton;
    private ProgressBar mProgressBar;
    private ProgressBar mProgressBarSearch;
    private WebAppPlanInterface mInfc;

    // A timer object to handle things when we are in sim mode
    private Timer mTimer;
    

    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;
    
    /*
     * If page it loaded
     */
    private boolean mIsPageLoaded;

    private Context mContext;

    /*
     * Callback actions from web app
     */
    public static final int SHOW_BUSY = 1;
    public static final int UNSHOW_BUSY = 2;


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
            	mInfc.updatePlan();
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
        mContext = this;
        mService = null;
        mIsPageLoaded = false;

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plan, null);
        setContentView(view);
        mWebView = (WebView)view.findViewById(R.id.plan_mainpage);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mInfc = new WebAppPlanInterface(mContext, mWebView, new GenericCallback() {
            /*
             * (non-Javadoc)
             * @see com.ds.avare.utils.GenericCallback#callback(java.lang.Object)
             */
        	@Override
        	public Object callback(Object o) {
        		mHandler.sendEmptyMessage((Integer)o);
        		return null;
        	}
        });
        mWebView.addJavascriptInterface(mInfc, "Android");
        mWebView.setWebChromeClient(new WebChromeClient() {
	     	public void onProgressChanged(WebView view, int progress) {
                /*
                 * Now update HTML with latest plan stuff, do this every time we start the Plan screen as 
                 * things might have changed.
                 * When both service and page loaded then proceed.
                 */
	     		if(mService != null && 100 == progress) {
	     		   	mInfc.newPlan();
	                mInfc.newSavePlan();
	                mProgressBarSearch.setVisibility(View.INVISIBLE);
	     		}
	     		mIsPageLoaded = true;
     	    }
	    });
        mWebView.loadUrl("file:///android_asset/plan.html");

        /*
         * Progress bar
         */
        mProgressBar = (ProgressBar)(view.findViewById(R.id.plan_progress_bar));
        mProgressBarSearch = (ProgressBar)(view.findViewById(R.id.plan_load_progress));
        mProgressBarSearch.setVisibility(View.VISIBLE);

        /*
         * For searching, start search on every new key press
         */
        mSearchText = (EditText)view.findViewById(R.id.plan_edit_text);
        mSearchText.addTextChangedListener(new TextWatcher() { 
            @Override
            public void afterTextChanged(Editable arg0) {
            }
    
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
    
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int after) {
                
                /*
                 * If text is 0 length or too long, then do not search, show last list
                 */
                if(s.length() < 3) {
                    mWebView.clearMatches();
                    return;
                }

                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mWebView.findAll(s.toString());
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            }
        });

        mNextButton = (Button)view.findViewById(R.id.plan_button_next);
        mNextButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(true);
            }
        });

        mLastButton = (Button)view.findViewById(R.id.plan_button_last);
        mLastButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(false);
            }
            
        });
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
            mInfc.connect(mService);

            /*
             * When both service and page loaded then proceed.
             * The plan will be loaded either from here or from page load end event
             */
     		if(mIsPageLoaded) {
     		   	mInfc.newPlan();
                mInfc.newSavePlan();
                mProgressBarSearch.setVisibility(View.INVISIBLE);
     		}

            mTimer = new Timer();
            TimerTask sim = new UpdateTask();
            mTimer.scheduleAtFixedRate(sim, 0, 1000);
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

        // Cancel the timer if one is running
        if(mTimer != null) {
        	mTimer.cancel();
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
    
    /***
    * A background timer class to send off messages if we are in simulation mode
    * @author zkhan
    */
    private class UpdateTask extends TimerTask {
	    // Called whenever the timer fires.
	    public void run() {
	    	mInfc.timer();
	    }
    }

    /**
     * 
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
    		if(msg.what == SHOW_BUSY) {
    			mProgressBarSearch.setVisibility(View.VISIBLE);
    		}
    		else if(msg.what == UNSHOW_BUSY) {
    			mProgressBarSearch.setVisibility(View.INVISIBLE);
    		}
        }
    };

}
