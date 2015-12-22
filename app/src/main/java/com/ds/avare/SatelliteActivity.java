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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.MemView;
import com.ds.avare.views.SatelliteView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author zkhan
 * Main activity
 */
public class SatelliteActivity extends Activity  {

    /**
     * Shows satellites
     */
    private SatelliteView mSatelliteView;
    private MemView mMemView;
    private TextView mMemText;
    private TextView mMapAreaText;
    
    private StorageService mService;
    
    private SeekBar mBrightnessBar;
    
    private TextView mGpsText;
    
    /*
     * Start GPS
     */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
            mSatelliteView.updateGpsStatus(gpsStatus);                
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null) {
                double latitude = Helper.truncGeo(location.getLatitude());
                double longitude = Helper.truncGeo(location.getLongitude());
                int accuracy = (int) Math.round(location.getAccuracy() * Preferences.heightConversion);
                Date dt = new Date(System.currentTimeMillis());
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String lastTime = sdf.format(dt);
                sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
                lastTime += "/" + sdf.format(dt) + "Z";
                
                mGpsText.setText(
                		latitude + "," + longitude + "\n" +
                		lastTime + "\n" +
                		getString(R.string.AltitudeAccuracy) + ": " + accuracy
                		);
            }
            else {
            	mSatelliteView.updateGpsStatus(null);
                mGpsText.setText("");
            }
            
            updateMem();
            updateMapArea();
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            if(timeout) {
                mSatelliteView.updateGpsStatus(null);
            }
        }

        @Override
        public void enabledCallback(boolean enabled) {
            if(!enabled) {
                mSatelliteView.updateGpsStatus(null);
            }
        }
    };
    
    private void updateMem() {
    	/*
    	 * Memory numbers
    	 */
		Runtime rt = Runtime.getRuntime();
		long vmAlloc = rt.totalMemory() - rt.freeMemory();
		long nativeAlloc = Debug.getNativeHeapAllocatedSize();
		long totalAlloc = (nativeAlloc + vmAlloc) / (1024 * 1024);

		long max = rt.maxMemory() / (1024 * 1024);

		mMemText.setText(totalAlloc + "MB/" + max + "MB");
        mMemView.updateMemStatus((float)totalAlloc / (float)max);
    }

    private void updateMapArea() {
    	/*
    	 * Map area numbers
    	 */
    	
        /*
         * Find various metrics for user info
         */
        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        int height = display.getHeight();
    
 	    // Subtract one tile from map width / height
		mMapAreaText.setText(
				getString(R.string.MapSize) + " " + (mService.getTiles().getXTilesNum() * BitmapHolder.WIDTH - BitmapHolder.WIDTH)+ "x" + (mService.getTiles().getYTilesNum() * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT) + "px\n" +
        		getString(R.string.ScreenSize) + " " + width + "x" + height + "px" + "\n" + getString(R.string.Tiles) + " " + (mService.getTiles().getOverhead() + mService.getTiles().getTilesNum()));
    }

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).showMapTab();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.satellite, null);
        setContentView(view);
        mSatelliteView = (SatelliteView)view.findViewById(R.id.satellite);

        mGpsText = (TextView)view.findViewById(R.id.satellite_text_gps_details);
        mMemView = (MemView)view.findViewById(R.id.memory);
        mMemText = (TextView)view.findViewById(R.id.satellite_text_mem_details);
        mMapAreaText = (TextView)view.findViewById(R.id.satellite_text_map_details);

        /*
         * Set brightness bar
         */        
        mBrightnessBar = (SeekBar)view.findViewById(R.id.satellite_slider);
        mBrightnessBar.setMax(255);
        mBrightnessBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
                if (Build.VERSION.SDK_INT >= 23) {
                    // Need special permission
                    if (!Settings.System.canWrite(SatelliteActivity.this)) {
                        Intent i = new Intent();
                        i.setAction(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        startActivity(i);
                        return;
                    }
                }

            }

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (!Settings.System.canWrite(SatelliteActivity.this)) {
                        return;
                    }
                }

                if(fromUser) {

					/*
					 * Manually set brightness
					 */
					android.provider.Settings.System.putInt(getContentResolver(), 
							android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
							android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				    android.provider.Settings.System.putInt(getContentResolver(),
				    	    android.provider.Settings.System.SCREEN_BRIGHTNESS,
				    	    progress);				
				}
			}
        });
        mService = null;      
        
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
            updateMem();
            updateMapArea();
        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
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
        
        /*
         * Set brightness bar to current value
         */
        try {
        	float curBrightnessValue = android.provider.Settings.System.getInt(
        	     getContentResolver(),
        	     android.provider.Settings.System.SCREEN_BRIGHTNESS);
            mBrightnessBar.setProgress((int)curBrightnessValue);        	
        } 
        catch (Exception e) {
        }

    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().unbindService(mConnection);
        
        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }
    }    
}
