/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;


import androidx.core.app.ActivityCompat;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
 
/**
 * 
 * @author zkhan
 *
 */
@SuppressWarnings("deprecation")
public class MainActivity extends TabActivity {

    TabHost mTabHost;
    float    mTabHeight;
    HorizontalScrollView mScrollView;
    int      mScrollWidth;
    Preferences mPref;
    TextView mTextView;
    Button mButton;

    // Tab panels that can display at the bottom of the screen. These manifest as 
    // separate display panes with their own intent to handle the content. Each one
    // except tabMain is configurable on or off by the user. 
    public static final int tabMain = 0; 
    public static final int tabPlates = 1;
    public static final int tabAFD = 2;
    public static final int tabFind = 3;
    public static final int tabPlan = 4;
    public static final int tabNear = 5;
    public static final int tabPfd = 6;
    public static final int tabThreeD = 7;
    public static final int tabChecklist = 8;
    public static final int tabWXB = 9;
    public static final int tabTools = 10;
    public static final int tabWnb = 11;

    public void setup() {
        mTextView.setVisibility(View.INVISIBLE);
        mButton.setVisibility(View.INVISIBLE);

        // start service
        final Intent intent = new Intent(MainActivity.this, StorageService.class);
        startService(intent);

        /*
         * Make a tab host
         */
        mTabHost = getTabHost();

        /*
         * Add tabs, NOTE: if the order changes or new tabs are added change the constants above (like tabMain = 0 )
         * also add the new tab to the preferences.getTabs() method.
         */
        long tabItems = mPref.getTabs();

        // We will always show the main chart tab
        setupTab(new TextView(this), getString(R.string.Main), new Intent(this, LocationActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.Plates), new Intent(this, PlatesActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.AFD), new Intent(this, AirportActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.Find), new Intent(this, SearchActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.Plan), new Intent(this, PlanActivity.class), getIntent());

        if (0 != (tabItems & (1 << tabNear))) {
            setupTab(new TextView(this), getString(R.string.Near), new Intent(this, NearestActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabPfd))) {
            setupTab(new TextView(this), getString(R.string.PFD), new Intent(this, PfdActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabThreeD))) {
            setupTab(new TextView(this), getString(R.string.ThreeD), new Intent(this, ThreeDActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabChecklist))) {
            setupTab(new TextView(this), getString(R.string.List), new Intent(this, ChecklistActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabWXB))) {
            setupTab(new TextView(this), getString(R.string.WXB), new Intent(this, FaaFileActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabTools))) {
            setupTab(new TextView(this), getString(R.string.Tools), new Intent(this, SatelliteActivity.class), getIntent());
        }

        if (0 != (tabItems & (1 << tabWnb))) {
            setupTab(new TextView(this), getString(R.string.Wnb), new Intent(this, WnbActivity.class), getIntent());
        }

        // Hide keyboard from another tab
        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mTabHost.getApplicationWindowToken(), 0);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // all need to be 0
        int res = 0;
        if(permissions.length > 0) {
            if(permissions.length > 0) {
                for (int g : grantResults) {
                    res = res + g;
                }
            }
        }
        if(0 == res) {
            setup();
        }
    }

    /**
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        mPref = new Preferences(this);
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);
         
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);
        mScrollView = (HorizontalScrollView)findViewById(R.id.tabscroll);
        mTextView = (TextView)findViewById(R.id.main_textview);
        mButton = (Button)findViewById(R.id.main_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        ViewTreeObserver vto = mScrollView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mScrollWidth = mScrollView.getChildAt(0).getMeasuredWidth() 
                        - getWindowManager().getDefaultDisplay().getWidth();

            }
        });

        // check permissions
        final Activity ctx = MainActivity.this;
        int PERMISSION_ALL = 101;
        String[] PERMISSIONS = {
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean granted = true;
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(ctx, PERMISSIONS, PERMISSION_ALL);
            mTextView.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.VISIBLE);
        }
        else {
            //granted
            setup();
        }

    }
    
    /**
     * 
     * @param view
     * @param tag
     * @param i
     */
    private void setupTab(View view, String tag, Intent i, Intent original) {
        /*
         * Pass on all original.
         */
        if(original.getExtras() != null) {
            i.putExtras(original);
        }
        View tabview = createTabView(mTabHost.getContext(), tag);

        TabSpec setContent = mTabHost.newTabSpec(tag).setIndicator(tabview).setContent(i);
        mTabHost.addTab(setContent);
    }
    
    /**
     * 
     * @param context
     * @param text
     * @return
     */
    private View createTabView(Context context, String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabs_text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Helper.adjustTextSize(context, R.dimen.TextSize) * 0.9f);
        tv.setText(text);
        return view;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(this);
    }

    @Override 
    public void onDestroy() {
        /*
         * Do not kill on orientation change
         */
        Intent intent = new Intent(this, StorageService.class);
        stopService(intent);
        super.onDestroy();
    }
    
    /**
     * For switching tab from any tab activity
     */
    private void switchTab(int tab){
        mTabHost.setCurrentTab(tab);
        /*
         * Hide soft keyboard that may be open
         */
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTabHost.getApplicationWindowToken(), 0);
    }

    /**
     * Display the main/maps tab
     */
    public void showMapTab() {
        switchTab(tabMain);
    }

    /**
     * Display the Plan tab
     */
    public void showPlanTab() {
        switchTab(tabPlan);
    }

    /**
     * Show the Plates view 
     */
    public void showPlatesTab() {
        switchTab(tabPlates);
    }

    /**
     * Show the AFD view 
     */
    public void showAfdTab() {
        switchTab(tabAFD);
    }

}
