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


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.webinfc.WebAppPlanInterface;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zkhan
 * An activity that deals with flight plans - loading, creating, deleting and activating
 */
public class PlanActivity extends Activity {

    /**
     * This view display location on the map.
     */
    private WebView mWebView;
    private Button mBackButton;
    private Button mActivateButton;
    private Button mForwardButton;
    private ProgressBar mProgressBarSearch;
    private WebAppPlanInterface mInfc;
    private boolean mInited;

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
    public static final int ACTIVE = 3;
    public static final int INACTIVE = 4;
    public static final int MESSAGE = 5;
    public static final int INIT = 6;


    /**
     * App preferences
     */

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
        mInited = false;

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
        	public Object callback(Object o, Object o1) {
            	Message m = mHandler.obtainMessage((Integer)o, o1);
            	mHandler.sendMessage(m);
        		return null;
        	}
        });
        mWebView.addJavascriptInterface(mInfc, "AndroidPlan");
        mWebView.setWebChromeClient(new WebChromeClient() {
	     	public void onProgressChanged(WebView view, int progress) {
                /*
                 * Now update HTML with latest plan stuff, do this every time we start the Plan screen as 
                 * things might have changed.
                 * When both service and page loaded then proceed.
                 */
	     		if(100 == progress) {
	     			mIsPageLoaded = true;
	     		}
     	    }
	     	
	     	// This is needed to remove title from Confirm dialog
	        @Override
	        public boolean onJsConfirm(WebView view, String url, String message, final android.webkit.JsResult result) {
                if(!isFinishing()) {

                    new DecoratedAlertDialogBuilder(PlanActivity.this)
                            .setTitle("")
                            .setMessage(message)
                            // on cancel is needed for without it JS may hang
                            .setCancelable(true)
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface arg0) {
                                    result.cancel();
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.cancel();
                                }
                            })
                            .create()
                            .show();
                }
	            return true;
	        }
	    });
        mWebView.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getApplicationContext(), "plan"));

        // This is need on some old phones to get focus back to webview.
        mWebView.setFocusable(true);
        mWebView.setOnTouchListener(new View.OnTouchListener() {  
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				arg0.performClick();
				arg0.requestFocus();
				return false;
			}
        });
        
        /*
         * Progress bar
         */
        mProgressBarSearch = (ProgressBar)(view.findViewById(R.id.plan_load_progress));
        mProgressBarSearch.setVisibility(View.VISIBLE);

        
        mBackButton = (Button)view.findViewById(R.id.plan_button_back);
        mBackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mInfc.moveBack();
            }
        });

        mForwardButton = (Button)view.findViewById(R.id.plan_button_forward);
        mForwardButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mInfc.moveForward();
            }
            
        });

        mActivateButton = (Button)view.findViewById(R.id.plan_button_activate);
        mActivateButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mInfc.activateToggle();
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
            mInfc.connect(binder.getService());
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);

            /*
             * When both service and page loaded then proceed.
             * The plan will be loaded either from here or from page load end event
             */
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
		mWebView.requestFocus();
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
        mInfc.cleanup();
    }
    
    /***
    * A background timer class to send off messages if we are in simulation mode
    * @author zkhan
    */
    private class UpdateTask extends TimerTask {
	    // Called whenever the timer fires.
	    public void run() {
	    	if(mService != null && mIsPageLoaded && !mInited) {
	    		// Load plans when done with service and page loading
	    		mHandler.sendEmptyMessage(INIT);
	    		mInited = true;
	    	}
	    	mInfc.timer();
	    }
    }

    /**
     * This is needed to change views from web app interface class
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
    		if(msg.what == SHOW_BUSY) {
    			mProgressBarSearch.setVisibility(View.VISIBLE);
    		}
    		else if(msg.what == UNSHOW_BUSY) {
    			mProgressBarSearch.setVisibility(View.INVISIBLE);
    		}
    		else if(msg.what == ACTIVE) {
    			mActivateButton.setText(getString(R.string.Active));
    		}
    		else if(msg.what == INACTIVE) {
    			mActivateButton.setText(getString(R.string.Inactive));
    		}
    		else if(msg.what == MESSAGE) {
    			// Show an important message
    			DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(mContext);
    			builder.setMessage((String)msg.obj)
    			       .setCancelable(false)
    			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
    			           public void onClick(DialogInterface dialog, int id) {
    			                dialog.dismiss();
    			           }
    			});
    			AlertDialog alert = builder.create();
                if(!isFinishing()) {
                    alert.show();
                }
    		}
    		else if(msg.what == INIT) {
                mProgressBarSearch.setVisibility(View.INVISIBLE);
  				mInfc.newPlan();
   				mInfc.newSavePlan();
    		}
        }
    };

}
