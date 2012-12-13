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

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
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
    
    @Override
    /**
     * 
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        requestWindowFeature(Window.FEATURE_NO_TITLE);

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
        setupTab(new TextView(this), getString(R.string.main), new Intent(this, LocationActivity.class));
        setupTab(new TextView(this), getString(R.string.plates), new Intent(this, PlatesActivity.class));
        setupTab(new TextView(this), getString(R.string.airport), new Intent(this, AirportActivity.class));
        setupTab(new TextView(this), getString(R.string.Nearest), new Intent(this, NearestActivity.class));
        setupTab(new TextView(this), getString(R.string.gps), new Intent(this, SatelliteActivity.class));
        
    }
    
    /**
     * 
     * @param view
     * @param tag
     * @param i
     */
    private void setupTab(View view, String tag, Intent i) {
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
    private static View createTabView(Context context, String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabsbg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }
}