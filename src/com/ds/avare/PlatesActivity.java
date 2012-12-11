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


import java.util.LinkedHashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlatesActivity extends Activity {
    
    private Preferences mPref;
    private PlatesView mPlatesView;
    private BitmapHolder mBitmap;
    private String mName;
    private StorageService mService;
    private Destination mDestination;
    private ListView mAfd;
    
    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * This matches main activity.
         */
        mPref = new Preferences(getApplicationContext());
        if(mPref.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);            
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.sliderafd, null);
        setContentView(view);
        mPlatesView = (PlatesView)view.findViewById(R.id.plates);
        mAfd = (ListView)view.findViewById(R.id.listafd);

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


            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();
            mName = mDestination.getDiagram();
            mBitmap = new BitmapHolder(mName);
            mPlatesView.setBitmap(mBitmap);
            PlatesActivity.this.setTitle(mDestination.getFacilityName());

            LinkedHashMap <String, String>map = mDestination.getParams();
            String[] views = new String[map.size()];
            String[] values = new String[map.size()];
            int iterator = 0;
            for(String key : map.keySet()){
                views[iterator] = key;
                values[iterator] = map.get(key);
                iterator++;
            }
            mAfd.setClickable(false);
            mAfd.setCacheColorHint(Color.WHITE);
            mAfd.setDividerHeight(10);
            mAfd.setBackgroundColor(Color.WHITE);
            mAfd.setAdapter(new TypeValueAdapter(PlatesActivity.this, views, values));

            /*
             * This should be true. Get plate coords if stored
             */
            if(null != mName) {
                String value = mPref.loadString(mName);
                if(null != value) {
                    /*
                     * mOLon, mOLat, mPx, mPy
                     */
                    String[] vals = value.split(",");
                    double valsDouble[] = new double[5];
                    try {
                        if(5 == vals.length) {
                            valsDouble[0] = Double.parseDouble(vals[0]);
                            valsDouble[1] = Double.parseDouble(vals[1]);
                            valsDouble[2] = Double.parseDouble(vals[2]);
                            valsDouble[3] = Double.parseDouble(vals[3]);
                            valsDouble[4] = Double.parseDouble(vals[4]);
                            mPlatesView.setParams(valsDouble);
                        }
                    }
                    catch (Exception e) {
                        
                    }
                }
            }
        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        /*
         * Clean up on pause that was started in on resume
         */
        unbindService(mConnection);
    }

    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();
        
        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if(mPref.shouldScreenStayOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);            
        }
    }

    /**
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(null != mBitmap) {
        	mBitmap.recycle();
        }
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        if(null == mService || null == mName) {
            return false;
        }
        getMenuInflater().inflate(R.menu.menu_plates, menu);
        return true;
    }

    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        /*
         * We dont have a diagram?
         */
        
        switch(item.getItemId()) {
        
            case R.id.mark:
            	
            	/*
            	 * Present a dialog to add a point and ask user for lon/lat
            	 */
                final AlertDialog dialogd = new AlertDialog.Builder(this).create();
                dialogd.setTitle(getString(R.string.markthis));
                dialogd.setCancelable(false);
                
                LayoutInflater inflater = getLayoutInflater();
                final View dv = inflater.inflate(R.layout.lonlat, (ViewGroup)getCurrentFocus());
                dialogd.setView(dv);
                dialogd.show();
                Button ok = (Button)dv.findViewById(R.id.lonlatbuttonOK);
                ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	/*
                    	 * On OK click, save pixel points in the view's coordinate
                    	 */
                    	mPlatesView.runState();
                    	PixelCoordinates pc = mPlatesView.getPoints();
                    	/*
                    	 * Also save lon/lat
                    	 */
                    	if(pc.firstPointAcquired()) {
                			pc.setLatitude0(
                					((EditText)dv.findViewById(R.id.latitude)).getText().toString(),
                					((EditText)dv.findViewById(R.id.latitudems)).getText().toString()
                					);
                			pc.setLongitude0(
                					((EditText)dv.findViewById(R.id.longitude)).getText().toString(),
                					((EditText)dv.findViewById(R.id.longitudems)).getText().toString()
                					);
                    	}
                    	else if(pc.secondPointAcquired()) {
                    		/*
                    		 * Do the same for second point
                    		 */
                			pc.setLatitude1(
                					((EditText)dv.findViewById(R.id.latitude)).getText().toString(),
                					((EditText)dv.findViewById(R.id.latitudems)).getText().toString()
                					);
                			pc.setLongitude1(
                					((EditText)dv.findViewById(R.id.longitude)).getText().toString(),
                					((EditText)dv.findViewById(R.id.longitudems)).getText().toString()
                					);
                    		if(!pc.isPixelDimensionAcceptable()) {
                    			/*
                    			 * Min dim required so calculation is correct. Warn user.
                    			 */
	                    		Toast.makeText(getApplicationContext(), getString(R.string.PointsTooClose), Toast.LENGTH_LONG).show();
	                		}
                    		else if(!pc.gpsCoordsCorrect()) {
                    			/*
                    			 * Bad coordinates for GPS.
                    			 */
	                    		Toast.makeText(getApplicationContext(), getString(R.string.BadCoords), Toast.LENGTH_LONG).show();
                    		}
                    		else {
                    		    /*
                    		     * If everything is good, save the just received params
                    		     */
                    		    double[] params = pc.get();
                    		    if(null != params) {
                    		        mPlatesView.setParams(params);
                    		        String value = "" + params[0] + "," + params[1] + "," + params[2] + "," + params[3] + "," + params[4];
                                    mPref.saveString(mName, value);
                    		    }
                    		}
                    	}
                    	dialogd.dismiss();
                    }
                });
                Button cancel = (Button)dv.findViewById(R.id.lonlatbuttonCancel);
                cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	dialogd.dismiss();
                    }
                });

            	break;
            	
            case R.id.cancel:
            	/*
            	 * Start again
            	 */
            	mPlatesView.cancelState();
            	
            	break;
        }
		return true;
    }
    
}
