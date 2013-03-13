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

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
 
/**
 * 
 * @author zkhan
 *
 */
public class MainActivity extends TabActivity {

    TabHost mTabHost;
    float mTabHeight;
    
    
    @Override
    /**
     * 
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
 
        /*
         * Set tabs height to something non obstructive but decent size.
         */
        Display display = getWindowManager().getDefaultDisplay();
        
        /*
         * Get screen density for that reason
         */
        mTabHeight = ((float)display.getHeight() + (float)display.getWidth()) / 
                (60 * getResources().getDisplayMetrics().density);
                
        setContentView(R.layout.main);
        
        /*
         * Start service now, bind later. This will be no-op if service is already running
         */
        Intent intent = new Intent(this, StorageService.class);
        startService(intent);

        /*
         * Make a tab host
         */
        mTabHost = getTabHost();
 
        /*
         * Add tabs
         */
        setupTab(new TextView(this), getString(R.string.main), new Intent(this, LocationActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.plates), new Intent(this, PlatesActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.AFD), new Intent(this, AirportActivity.class), getIntent());
        setupTab(new TextView(this), getString(R.string.Nearest), new Intent(this, NearestActivity.class), getIntent());        
        setupTab(new TextView(this), getString(R.string.Search), new Intent(this, PlanActivity.class), getIntent());        
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
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mTabHeight);
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
         * Start service now, bind later. This will be no-op if service is already running
         */
        Preferences mPref = new Preferences(this);
        if(!mPref.shouldLeaveRunning()) {
            if (isFinishing()) {
                /*
                 * Do not kill on orientation change
                 */
                Intent intent = new Intent(this, StorageService.class);
                stopService(intent);
            }
        }
        super.onDestroy();
    }
    
    /**
     * For switching tab from any tab activity
     */
    public void switchTab(int tab){
        mTabHost.setCurrentTab(tab);
        /*
         * Hide soft keyboard that may be open
         */
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTabHost.getApplicationWindowToken(), 0);

    }

}