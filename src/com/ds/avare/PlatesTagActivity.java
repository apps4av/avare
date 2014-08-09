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



import java.util.LinkedList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

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
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlatesTagActivity extends Activity {
    private PlatesTagView                mPlatesView;
    private StorageService               mService;
    private PixelCoordinate              mPoint[];
    private Coordinate                   mPointLL[];
    private Button                       mGeotagButton;
    private Button                       mVerifyButton;
    private Button                       mClearButton;
    private EditText                     mText;
    private Spinner                      mSpinner;
    private Toast                        mToast;
    private Preferences                  mPref;
    private LinkedList<String>           mTags;
    private boolean                     mTagged;
    private AlertDialog                  mAlertDialog;

    
    private static final int POINTS = 2;
    private static final double MIN_SEPARATION = 100.0;
    
    /*
     * These are params once plate is tagged
     */
    private double mDx;
    private double mDy;
    private double mLonTopLeft;
    private double mLatTopLeft;
       
    /*
     * Start GPS
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

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        mPref = new Preferences(this);
        mPoint = new PixelCoordinate[POINTS];
        mPointLL = new Coordinate[POINTS];
        mTagged = false;
        
        /*
         * Get stored geotags
         */
        mTags = getTagsStorageFromat(mPref.getGeotags());
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.platestag, null);
        setContentView(view);
        mPlatesView = (PlatesTagView)view.findViewById(R.id.platestag_view);
     
        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        
        /*
         * Spinner for type of point
         */
        mSpinner = (Spinner)view.findViewById(R.id.platestag_spinner);
        mSpinner.setSelection(0);

        /*
         * The button that adds a point
         */
        mGeotagButton = (Button)view.findViewById(R.id.platestag_button_tag);
        mGeotagButton.getBackground().setAlpha(255);
        mGeotagButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                /*
                 * Already tagged
                 */
                if(mTagged) {
                    mToast.setText(getString(R.string.AlreadyTagged));
                    mToast.show();
                    return;
                }
                
                /*
                 * What to find.
                 */
                String toFind = mText.getText().toString().toUpperCase(Locale.getDefault());
                String item = mSpinner.getSelectedItem().toString();
                
                /*
                 * Cannot be null
                 */
                if(null == item || mService == null) {
                    mToast.setText(getString(R.string.InvalidPoint));
                    mToast.show();
                    return;
                }
                
                /*
                 * Find point in database
                 */
                String dat = mService.getDBResource().findLonLat(toFind, item);
                if(null == dat) {
                    mToast.setText(getString(R.string.PointNotFound));
                    mToast.show();
                    return;
                }
                
                /*
                 * Check if lon/lat are OK
                 */
                String tokens[] = dat.split(",");
                if(tokens.length != 2) {
                    mToast.setText(getString(R.string.PointNotFound));
                    mToast.show();
                    return;
                }
                
                /*
                 * Add point
                 */
                if(mPoint[0] == null) {
                    mPoint[0] = new PixelCoordinate(mPlatesView.getx(), mPlatesView.gety());
                    mPointLL[0] = new Coordinate(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]));
                    mToast.setText(getString(R.string.AddedPoint));
                    mToast.show();
                }
                else if(mPoint[1] == null) {
                    mPoint[1] = new PixelCoordinate(mPlatesView.getx(), mPlatesView.gety());
                    mPointLL[1] = new Coordinate(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]));
                    mDx = (mPoint[0].getX() - mPoint[1].getX()) / (mPointLL[0].getLongitude() - mPointLL[1].getLongitude()); 
                    mDy = (mPoint[0].getY() - mPoint[1].getY()) / (mPointLL[0].getLatitude() - mPointLL[1].getLatitude());
                    
                    if((mDx < MIN_SEPARATION) || (mDy > -MIN_SEPARATION)) {
                        mPoint[1] = null;
                        mPointLL[1] = null;
                        mToast.setText(getString(R.string.SelectOtherPoint));
                        mToast.show();  
                        return;
                    }

                    /*
                     * Now calculate params to store
                     */
                    mLonTopLeft = mPointLL[0].getLongitude() - mPoint[0].getX() / mDx;
                    mLatTopLeft = mPointLL[0].getLatitude() - mPoint[0].getY() / mDy;  
                    
                    /*
                     * Add
                     */
                    mTags.add(new String(mService.getDiagram().getName() + "," + mDx + "," + mDy + "," + mLonTopLeft + "," + mLatTopLeft));
                    
                    /*
                     * Store and show message
                     */
                    mPref.setGeotags(putTagsToStorageFormat(mTags));
                    mToast.setText(getString(R.string.Tagged));
                    mToast.show();   
                    mTagged = true;
                    mPoint[0] = null;
                    mPointLL[0] = null;
                    mPoint[1] = null;
                    mPointLL[1] = null;
                }
            }
        });      

        /*
         * Verify button
         */
        mVerifyButton = (Button)view.findViewById(R.id.platestag_button_verify);
        mVerifyButton.getBackground().setAlpha(255);
        mVerifyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTagged) {
                    mToast.setText(getString(R.string.NotTagged));
                    mToast.show();
                    return;
                }
                
                String toFind = mText.getText().toString().toUpperCase(Locale.getDefault());
                String item = mSpinner.getSelectedItem().toString();
                if(null == item || mService == null) {
                    mToast.setText(getString(R.string.InvalidPoint));
                    mToast.show();
                    return;
                }
                String dat = mService.getDBResource().findLonLat(toFind, item);
                if(null == dat) {
                    mToast.setText(getString(R.string.PointNotFound));
                    mToast.show();
                    return;
                }
                /*
                 * Found point. Verify
                 */
                String tokens[] = dat.split(",");
                if(tokens.length != 2) {
                    mToast.setText(getString(R.string.PointNotFound));
                    mToast.show();
                    return;
                }
                
                /*
                 * Do it
                 */
                double lon = Double.parseDouble(tokens[0]);
                double lat = Double.parseDouble(tokens[1]);
                    
                double x = (lon - mLonTopLeft) * mDx;
                double y = (lat - mLatTopLeft) * mDy;  
                
                mPlatesView.verify(x, y);
            }
        });      

        /*
         * Verify button
         */
        mClearButton = (Button)view.findViewById(R.id.platestag_button_clear);
        mClearButton.getBackground().setAlpha(255);
        mClearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService == null) {
                    return;
                }
                String name = mService.getDiagram().getName();
                if(null == name) {
                    return;
                }
                
                mPoint[0] = null;
                mPointLL[0] = null;
                mPoint[1] = null;
                mPointLL[1] = null;
                mTagged = false;
                mDx = 0;
                mDy = 0;
                mLonTopLeft = 0;
                mLatTopLeft = 0;
                
                for(String t : mTags) {
                    if(t.contains(name)) {
                        mTags.remove(t);
                        mPref.setGeotags(putTagsToStorageFormat(mTags));
                        mToast.setText(getString(R.string.Cleared));
                        mToast.show();
                        return;
                    }
                }
            }
        });      

        mText = (EditText)view.findViewById(R.id.platestag_text_input);
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
            
            mPoint[0] = null;
            mPointLL[0] = null;
            mPoint[1] = null;
            mPointLL[1] = null;

            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
            
            mPlatesView.setBitmap(mService.getDiagram());
            
            /*
             * See if we have this plate already tagged
             */
            String name = mService.getDiagram().getName();
            if(null == name) {
                mTagged = false;
                return;
            }
            for(String t : mTags) {
                if(t.contains(name)) {
                    /*
                     * Already tagged. Get info
                     */
                    String tokens[] = t.split(",");
                    mDx = Double.parseDouble(tokens[1]);
                    mDy = Double.parseDouble(tokens[2]);
                    mLonTopLeft = Double.parseDouble(tokens[3]);
                    mLatTopLeft = Double.parseDouble(tokens[4]);
                    mTagged = true;
                }
            }
            
            /*
             * If the plate not tagged, show help
             */
            if(!mTagged) {
                mAlertDialog = new AlertDialog.Builder(PlatesTagActivity.this).create();
                mAlertDialog.setCancelable(false);
                mAlertDialog.setCanceledOnTouchOutside(false);
                mAlertDialog.setMessage(getString(R.string.ToTag));
                mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mAlertDialog.show();
            }
            else {
                mAlertDialog = new AlertDialog.Builder(PlatesTagActivity.this).create();
                mAlertDialog.setCancelable(false);
                mAlertDialog.setCanceledOnTouchOutside(false);
                mAlertDialog.setMessage(getString(R.string.AlreadyTagged));
                mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mAlertDialog.show();
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
        
        if(null != mAlertDialog) {
            try {
                mAlertDialog.dismiss();
            }
            catch (Exception e) {
            }
        }

        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);
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
    }
    
    
    
    /**
     * Put a list of tags in JSON array
     * @param tags
     * @return
     */
    public static String putTagsToStorageFormat(LinkedList<String> tags) {
        
        JSONArray jsonArr = new JSONArray();
        for(String t : tags) {
            
            jsonArr.put(t);
        }
        
        return jsonArr.toString();
    }
    
    /**
     * Gets an array of tags from storage JSON
     * @return
     */
    public static LinkedList<String> getTagsStorageFromat(String json) {
        JSONArray jsonArr;
        LinkedList<String> ret = new LinkedList<String>();
        try {
            jsonArr = new JSONArray(json);
        } catch (JSONException e) {
            return ret;
        }
        
        for(int i = 0; i < jsonArr.length(); i++) {
            try {
                ret.add(jsonArr.getString(i));
            } catch (JSONException e) {
                continue;
            }
        }
        
        return ret;
    }
}