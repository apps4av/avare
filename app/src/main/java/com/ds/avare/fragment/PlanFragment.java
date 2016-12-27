/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;

import com.ds.avare.R;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.webinfc.WebAppPlanInterface;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zkhan
 * An activity that deals with flight plans - loading, creating, deleting and activating
 */
public class PlanFragment extends StorageServiceGpsListenerFragment {

    public static final String TAG = "PlanFragment";

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

    /*
     * If page it loaded
     */
    private boolean mIsPageLoaded;

    /*
     * Callback actions from web app
     */
    public static final int SHOW_BUSY = 1;
    public static final int UNSHOW_BUSY = 2;
    public static final int ACTIVE = 3;
    public static final int INACTIVE = 4;
    public static final int MESSAGE = 5;
    public static final int INIT = 6;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsPageLoaded = false;
        mInited = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.plan, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mWebView = (WebView) view.findViewById(R.id.plan_mainpage);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mInfc = new WebAppPlanInterface(getContext(), mWebView, new GenericCallback() {
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
                new AlertDialog.Builder(getContext())
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
                return true;
            }
        });
        mWebView.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getContext(), "plan"));

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
        // Do not let selecting text
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
        mProgressBarSearch = (ProgressBar) (view.findViewById(R.id.plan_load_progress));
        mProgressBarSearch.setVisibility(View.VISIBLE);


        mBackButton = (Button) view.findViewById(R.id.plan_button_back);
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfc.moveBack();
            }
        });

        mForwardButton = (Button) view.findViewById(R.id.plan_button_forward);
        mForwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfc.moveForward();
            }
        });

        mActivateButton = (Button) view.findViewById(R.id.plan_button_activate);
        mActivateButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mInfc.activateToggle();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.requestFocus();
    }

    @Override
    public  void onPause() {
        super.onPause();
        // Cancel the timer if one is running
        if (mTimer != null) mTimer.cancel();
    }

    @Override
    protected void postServiceConnected() {
        mInfc.connect(mService);
        /*
         * When both service and page loaded then proceed.
         * The plan will be loaded either from here or from page load end event
         */
        mTimer = new Timer();
        TimerTask sim = new UpdateTask();
        mTimer.scheduleAtFixedRate(sim, 0, 1000);
    }

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
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isAdded()) return;

            if(msg.what == SHOW_BUSY) {
                mProgressBarSearch.setVisibility(View.VISIBLE);
            }
            else if(msg.what == UNSHOW_BUSY) {
                mProgressBarSearch.setVisibility(View.INVISIBLE);
            }
            else if(msg.what == ACTIVE) {
                if (mActivateButton.getText().equals(getString(R.string.Inactive))) {
                    mActivateButton.setText(getString(R.string.Active));
                }
            }
            else if(msg.what == INACTIVE) {
                if (mActivateButton.getText().equals(getString(R.string.Active))) {
                    mActivateButton.setText(getString(R.string.Inactive));
                }
            }
            else if(msg.what == MESSAGE) {
                // Show an important message
                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(getContext());
                builder.setMessage((String)msg.obj)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else if(msg.what == INIT) {
                mProgressBarSearch.setVisibility(View.INVISIBLE);
                mInfc.newPlan();
                mInfc.newSavePlan();
            }
        }
    };

}
