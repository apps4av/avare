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



import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

/**
 * @author zkhan
 *
 */
public class WebActivity extends Activity  {
    
    private WebView mWebView;
    private EditText mSearchText;
    private Button mNextButton;
    private Button mLastButton;
    private ProgressBar mProgressBar;
    private StorageService mService;

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }
    };

    /*
     * Show views from web
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.web, null);
        setContentView(view);
        mWebView = (WebView) view.findViewById(R.id.web_mainpage);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.loadUrl(getIntent().getStringExtra("url"));
        mWebView.getSettings().setJavaScriptEnabled(true);
        // This is need on some old phones to get focus back to webview.
        mWebView.setOnTouchListener(new View.OnTouchListener() {  
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				arg0.performClick();
				arg0.requestFocus();
				return false;
			}
        });

        mWebView.setOnLongClickListener(new OnLongClickListener() {
        	@Override
        	public boolean onLongClick(View v) {
        	    return true;
        	}
        });
        mWebView.setLongClickable(false);

        /*
         * Progress bar
         */
        mProgressBar = (ProgressBar)(view.findViewById(R.id.web_progress_bar));
                
        /*
         * For searching, start search on every new key press
         */
        mSearchText = (EditText)view.findViewById(R.id.web_edit_text);
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

        mNextButton = (Button)view.findViewById(R.id.web_button_next);
        mNextButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(true);
            }
            
        });


        mLastButton = (Button)view.findViewById(R.id.web_button_last);
        mLastButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(false);
            }
            
        });

    }    
    
    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();

        Helper.setOrientationAndOn(this);
        
        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
		mWebView.requestFocus();

    }

    /** Defines callbacks for service binding, passed to bindService() */
    /**
     * 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();

            mService.registerGpsListener(mGpsInfc);

        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /**
     * 
     */
    @Override
    public void onPause() {
        super.onPause();

        getApplicationContext().unbindService(mConnection);
        
        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }
    }
            

}
