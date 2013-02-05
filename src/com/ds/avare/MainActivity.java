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

import com.ds.avare.utils.Helper;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
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
    float mTabHeight;
    
    private static float DIV_TAB = 60;
    
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
        float ar = (float)display.getHeight() / (float)display.getWidth();
        if(ar < 1) {
            ar = 1 / ar;
        }
        if(Surface.ROTATION_0 == display.getRotation() || Surface.ROTATION_180 == display.getRotation()) {
            /*
             * Portrait
             * 1.5 aspect ratio assumed and magic number for tabs height
             */
            mTabHeight = (float)display.getHeight() / DIV_TAB;
        }
        else {
            /*
             * Landscape
             */
            mTabHeight = (float)display.getHeight() / (DIV_TAB / ar);
        }
                
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
        setupTab(new TextView(this), getString(R.string.AFD), new Intent(this, AirportActivity.class));
        setupTab(new TextView(this), getString(R.string.Nearest), new Intent(this, NearestActivity.class));
        
        //Mock mMockGps;
        //mMockGps = new Mock();
        //mMockGps.execute(mGps); // execute the listener where GPS location is
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
    private View createTabView(Context context, String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabs_text);
        tv.setText(text);
        tv.setTextSize(mTabHeight);
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
 
    /**
     * For switching tab from any tab activity
     */
    public void switchTab(int tab){
        mTabHost.setCurrentTab(tab);
    }

}