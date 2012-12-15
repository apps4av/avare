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

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
    private View mCalibrateView;
    private AlertDialog mCalibrateDialog;
    private Toast mToast;
    private String mLatC;
    private String mLonC;
    private String mLatCMin;
    private String mLonCMin;

    /*
     * Start GPS
     */
    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
                GpsParams params = new GpsParams(location); 
                
                /*
                 * Store GPS last location in case activity dies, we want to start from same loc
                 */
                mPlatesView.updateParams(params); 
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            /*
             *  No GPS signal
             *  Tell location view to show GPS status
             */
            if(mPref.isSimulationMode()) {
                mPlatesView.updateErrorStatus(getString(R.string.SimulationMode));                
            }
            else if(Gps.isGpsDisabled(getApplicationContext(), mPref)) {
                /*
                 * Prompt user to enable GPS.
                 */
                mPlatesView.updateErrorStatus(getString(R.string.GPSEnable)); 
            }
            else if(timeout) {
                mPlatesView.updateErrorStatus(getString(R.string.GPSLost));
            }
            else {
                /*
                 *  GPS kicking.
                 */
                mPlatesView.updateErrorStatus(null);
            }           
        }          
    };

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
        View view = layoutInflater.inflate(R.layout.plates, null);
        setContentView(view);
        mPlatesView = (PlatesView)view.findViewById(R.id.plates);

        mCalibrateDialog = new AlertDialog.Builder(this).create();
        mCalibrateDialog.setTitle(getString(R.string.calibratethis));
        
        mCalibrateView = layoutInflater.inflate(R.layout.lonlat, null);
        mCalibrateDialog.setView(mCalibrateView);
        mCalibrateDialog.setCancelable(false);


        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

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

            /*
             * Now get all stored data
             */
            mDestination = mService.getDestination();
            if(null == mDestination) {
                mToast.setText(getString(R.string.ValidDest));
                mToast.show();
                mName = null;
                return;
            }
            
            /*
             * Find lon/lat of the center of airport
             */
            Location l = mDestination.getLocation();
            double lonc = Math.abs(l.getLongitude());
            double latc = Math.abs(l.getLatitude());
            latc = ((int)latc);
            lonc = ((int)lonc);
            double latcm = Math.abs(l.getLatitude()) - latc;
            double loncm = Math.abs(l.getLongitude()) - lonc;
            latcm *= 60;
            loncm *= 60;
            DecimalFormat f = new DecimalFormat("00.0");
            mLatCMin = f.format(latcm);
            mLonCMin = f.format(loncm);
            mLatC = String.valueOf((int)latc);
            mLonC = String.valueOf((int)lonc);
            /*
             * Show the lon/lat of center of airport to begin with
             */
            ((EditText)mCalibrateView.findViewById(R.id.latitude)).setText(mLatC);
            ((EditText)mCalibrateView.findViewById(R.id.longitude)).setText(mLonC);
            ((EditText)mCalibrateView.findViewById(R.id.latitudems)).setText(mLatCMin);
            ((EditText)mCalibrateView.findViewById(R.id.longitudems)).setText(mLonCMin);

            
            mName = mDestination.getDiagram();
            mBitmap = new BitmapHolder(mName);
            mPlatesView.setBitmap(mBitmap);

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
        
        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);
        
        if(null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }

        if(null != mCalibrateDialog) {
            try {
                mCalibrateDialog.dismiss();
            }
            catch (Exception e) {
            }
        }

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
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

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
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        if(null == mService || null == mName) {
            return false;
        }
        getMenuInflater().inflate(R.menu.plates, menu);
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
                
                mCalibrateDialog.show();
                Button ok = (Button)mCalibrateView.findViewById(R.id.lonlatbuttonOK);
                ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	/*
                    	 * On OK click, save pixel points in the view's coordinate
                    	 */
                    	PixelCoordinates pc = mPlatesView.getPoints();
                    	
                    	/*
                    	 * This will reset in case we are doing mark input again
                    	 */
                        if(pc.secondPointAcquired()) {
                            pc.resetPoints();
                        }

                    	if(pc.noPointAcquired()) {
                            /*
                             * Acquire first point
                             */
                            if(!pc.setLatitude0(
                                    ((EditText)mCalibrateView.findViewById(R.id.latitude)).getText().toString(),
                                    ((EditText)mCalibrateView.findViewById(R.id.latitudems)).getText().toString()
                                    )) {
                                Toast.makeText(getApplicationContext(), getString(R.string.BadCoords), Toast.LENGTH_LONG).show();
                                mCalibrateDialog.dismiss();
                                return;
                            }
                            if(!pc.setLongitude0(
                                    ((EditText)mCalibrateView.findViewById(R.id.longitude)).getText().toString(),
                                    ((EditText)mCalibrateView.findViewById(R.id.longitudems)).getText().toString()
                                    )) {
                                Toast.makeText(getApplicationContext(), getString(R.string.BadCoords), Toast.LENGTH_LONG).show();
                                mCalibrateDialog.dismiss();
                                return;
                            }
                            pc.setX0(mPlatesView.getX());
                            pc.setY0(mPlatesView.getY());
                            mCalibrateDialog.dismiss();
                            return;
                    	}
                    	
                    	if(pc.firstPointAcquired()) {
                            /*
                             * Do the same for second point
                             */
                			if(!pc.setLatitude1(
                					((EditText)mCalibrateView.findViewById(R.id.latitude)).getText().toString(),
                					((EditText)mCalibrateView.findViewById(R.id.latitudems)).getText().toString()
                					)) {
                                Toast.makeText(getApplicationContext(), getString(R.string.BadCoords), Toast.LENGTH_LONG).show();
                                mCalibrateDialog.dismiss();
                                return;
                			}
                			if(!pc.setLongitude1(
                					((EditText)mCalibrateView.findViewById(R.id.longitude)).getText().toString(),
                					((EditText)mCalibrateView.findViewById(R.id.longitudems)).getText().toString()
                					)) {
                                Toast.makeText(getApplicationContext(), getString(R.string.BadCoords), Toast.LENGTH_LONG).show();
                                mCalibrateDialog.dismiss();
                                return;
                			}
                            if(!pc.setX1(mPlatesView.getX())) {
                                Toast.makeText(getApplicationContext(), getString(R.string.PointsTooClose), Toast.LENGTH_LONG).show();                                
                                mCalibrateDialog.dismiss();
                                return;
                            }
                            if(!pc.setY1(mPlatesView.getY())) {
                                Toast.makeText(getApplicationContext(), getString(R.string.PointsTooClose), Toast.LENGTH_LONG).show();                                
                                mCalibrateDialog.dismiss();
                                return;
                            }
                    	}
                    	
                    	if(pc.secondPointAcquired()) {
                		    /*
                		     * If everything is good, save the just received params
                		     */
                		    double[] params = pc.get();
                		    if(null != params) {
                		        mPlatesView.setParams(params);
                		        String value = "" + params[0] + "," + params[1] + "," + params[2] + "," + params[3] + "," + params[4];
                                mPref.saveString(mName, value);
                                Toast.makeText(getApplicationContext(), getString(R.string.GoodCoords), Toast.LENGTH_LONG).show();                                
                		    }
                    	}
                    	mCalibrateDialog.dismiss();
                    }
                });

            	break;
            	
            case R.id.cancel:
            	/*
            	 * Start again
            	 */
                PixelCoordinates pc = mPlatesView.getPoints();
                pc.resetPoints();
                mPlatesView.postInvalidate();
            	
            	break;
        }
		return true;
    }
    
}
