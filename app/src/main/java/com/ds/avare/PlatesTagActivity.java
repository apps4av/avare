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
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.OptionButton;
import com.ds.avare.views.PlatesTagView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlatesTagActivity extends Activity implements Observer {
    private PlatesTagView                mPlatesView;
    private StorageService               mService;
    private PixelCoordinate              mPoint[];
    private Coordinate                   mPointLL[];
    private Toast                        mToast;
    private Preferences                  mPref;
    private AlertDialog                  mAlertDialog;
    private Destination                  mDest;
    private Destination                  mDestVerify;
    private Destination                  mDestPoint[] = new Destination[POINTS];
    private String                       mName;
    private String                       mAirport;
    private boolean                      mTagged;
    private Button                       mSetButton[] = new Button[POINTS];


    private static final int POINTS = 2;
    private static final double MIN_SEPARATION = 10.0;
    private static final double MIN_SEPARATION_COORD = 0.01;


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
    private void drawAirport() {
        /*
         * Draw airport
         */
        if(mDest.isFound()) {
            float ax = (float) ((mDest.getLocation().getLongitude() - mLonTopLeft) * mDx);
            float ay = (float) ((mDest.getLocation().getLatitude() - mLatTopLeft) * mDy);
            mPlatesView.setAirport(mAirport, ax, ay);
        }
    }

    /**
     *
     */
    private void store() {
        
        /*
         * Store and show message
         */
        mAlertDialog = new DecoratedAlertDialogBuilder(PlatesTagActivity.this).create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.setMessage(getString(R.string.Tagged));
        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if(!isFinishing()) {
            mAlertDialog.show();
        }

        mService.getDBResource().setUserTag(mName, mDx + "," + mDy + "," + mLonTopLeft + "," + mLatTopLeft);
        mTagged = true;

        drawAirport();
    }

    /**
     *
     */
    private void clearParams() {
        mDx = 0;
        mDy = 0;
        mLonTopLeft = 0;
        mLatTopLeft = 0;
        mPlatesView.unsetAirport();
    }

    /**
     *
     */
    private void clear() {

        mToast.setText(getString(R.string.Cleared));
        mToast.show();

        clearParams();
        mService.getDBResource().deleteUserTag(mName);
    }

    private void tagDone() {

        if(mPointLL[0] == null || mPointLL[1] == null || mPoint[0] == null || mPoint[1] == null) {
            mToast.setText(getString(R.string.InvalidPoint));
            mToast.show();
            return;
        }
        mDx = (mPoint[0].getX() - mPoint[1].getX()) / (mPointLL[0].getLongitude() - mPointLL[1].getLongitude());
        mDy = (mPoint[0].getY() - mPoint[1].getY()) / (mPointLL[0].getLatitude() - mPointLL[1].getLatitude());

        if((Math.abs(mPoint[0].getX() - mPoint[1].getX()) < MIN_SEPARATION) ||
                (Math.abs(mPointLL[0].getLongitude() - mPointLL[1].getLongitude()) < MIN_SEPARATION_COORD)) {
            mToast.setText(getString(R.string.SelectOtherPoint));
            mToast.show();
            return;
        }
        if((Math.abs(mPoint[0].getY() - mPoint[1].getY()) < MIN_SEPARATION) ||
                        (Math.abs(mPointLL[0].getLatitude() - mPointLL[1].getLatitude()) < MIN_SEPARATION_COORD)) {
            mToast.setText(getString(R.string.SelectOtherPoint));
            mToast.show();
            return;
        }

        /*
         * Now calculate params to store
         */
        mLonTopLeft = mPointLL[0].getLongitude() - mPoint[0].getX() / mDx;
        mLatTopLeft = mPointLL[0].getLatitude() - mPoint[0].getY() / mDy;

        store();
    }



    private void tag(int point) {

        mPoint[point] = null;
        mPointLL[point] = null;
        /*
         * Cannot be null
         */
        if(mService == null) {
            mToast.setText(getString(R.string.InvalidPoint));
            mToast.show();
            return;
        }

        mPoint[point] = new PixelCoordinate(mPlatesView.getx(), mPlatesView.gety());

        String item = ((OptionButton)findViewById(R.id.platestag_spinner)).getCurrentValue();
        String toFind = ((EditText)findViewById(R.id.platestag_text_input)).getText().toString().toUpperCase(Locale.getDefault());
        ((TextView)findViewById(R.id.platestag_text_input)).setText("");
        ((OptionButton)findViewById(R.id.platestag_spinner)).setCurrentSelectionIndex(0);
        /*
         * Find point in database
         */
        Destination d;
        if(item.equals("GPS")) {
            d = DestinationFactory.build(mService, toFind, Destination.GPS);
        }
        else if(item.equals("Maps")) {
            d = DestinationFactory.build(mService, toFind, Destination.MAPS);
        }
        else {
            d = DestinationFactory.build(mService, toFind, item);
        }
        mDestPoint[point] = d;
        d.addObserver(PlatesTagActivity.this);
        d.find();
    }

    private void verify() {

        /*
         * Cannot be null
         */
        if(mService == null) {
            mToast.setText(getString(R.string.InvalidPoint));
            mToast.show();
            return;
        }

        String item = ((OptionButton)findViewById(R.id.platestag_spinner)).getCurrentValue();
        String toFind = ((EditText)findViewById(R.id.platestag_text_input)).getText().toString().toUpperCase(Locale.getDefault());
        ((TextView)findViewById(R.id.platestag_text_input)).setText("");
        ((OptionButton)findViewById(R.id.platestag_spinner)).setCurrentSelectionIndex(0);
        /*
         * Find point in database
         */
        Destination d;
        if(item.equals("GPS")) {
            d = DestinationFactory.build(mService, toFind, Destination.GPS);
        }
        else if(item.equals("Maps")) {
            d = DestinationFactory.build(mService, toFind, Destination.MAPS);
        }
        else {
            d = DestinationFactory.build(mService, toFind, item);
        }
        mDestVerify = d;
        d.addObserver(PlatesTagActivity.this);
        d.find();
    }


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

        /*
         * Get stored geotags
         */
        mName = mAirport = "";
        mTagged = false;
        
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
         * The button that adds a point
         */
        mSetButton[0] = (Button)view.findViewById(R.id.platestag_button_tag1);
        mSetButton[0].setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tag(0);
            }
        });


        /*
         * The button that adds a point
         */
        mSetButton[1] = (Button)view.findViewById(R.id.platestag_button_tag2);
        mSetButton[1].setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tag(1);
            }
        });


        /*
         * The button that adds a point
         */
        Button b = (Button)view.findViewById(R.id.platestag_button_process);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tagDone();
            }
        });

        /*
         * The button that adds a point
         */
        b = (Button)view.findViewById(R.id.platestag_button_verify);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verify();
            }
        });


        /*
         * Clear button
         */
        b = (Button)view.findViewById(R.id.platestag_button_clear);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mTagged) {
                    mToast.setText(getString(R.string.NotTagged));
                    mToast.show();
                    return;
                }

                mAlertDialog = new DecoratedAlertDialogBuilder(PlatesTagActivity.this).create();
                mAlertDialog.setCancelable(false);
                mAlertDialog.setCanceledOnTouchOutside(false);
                mAlertDialog.setMessage(getString(R.string.ClearedPrompt));
                mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        clear();
                        dialog.dismiss();
                    }
                });
                mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                if(!isFinishing()) {
                    mAlertDialog.show();
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
            
            /*
             * Get proc name
             */
            if(null == mService.getPlateDiagram()) {
                mTagged = false;
                return;
            }
            mName = PlatesActivity.getNameFromPath(mService.getPlateDiagram().getName());
            if(mName != null) {
                mAirport = mName.split("/")[0];
            }
            else {
                mTagged = false;
                return;
            }

            mPlatesView.setBitmap(mService.getPlateDiagram());

            
            /*
             * Find who this plate is for, so we can verify its sane.
             * By the time user is ready to tag, this should be found
             */
            mDest = DestinationFactory.build(mService, mAirport, Destination.BASE);
            mDest.addObserver(PlatesTagActivity.this);
            mDest.find();

            String tag = mService.getDBResource().getUserTag(mName);
            if(null != tag) {
                String tokens[] = tag.split(",");
                mDx = Double.parseDouble(tokens[0]);
                mDy = Double.parseDouble(tokens[1]);
                mLonTopLeft = Double.parseDouble(tokens[2]);
                mLatTopLeft = Double.parseDouble(tokens[3]);
                mTagged = true;
            }

            /*
             * If the plate not tagged, show help
             */
            if(!mTagged) {
                mAlertDialog = new DecoratedAlertDialogBuilder(PlatesTagActivity.this).create();
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
                if(!isFinishing()) {
                    mAlertDialog.show();
                }
            }
            else {
                mAlertDialog = new DecoratedAlertDialogBuilder(PlatesTagActivity.this).create();
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
                if(!isFinishing()) {
                    mAlertDialog.show();
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
        getApplicationContext().bindService(intent, mConnection, 0);
    }


    @Override
    public void update(Observable observable, Object data) {

        Destination d = (Destination) observable;

        if (d == mDest && d.isFound() && mTagged) {
            drawAirport();
        }
        else if(d == mDestVerify) {
            if(!mTagged) {
                mToast.setText(getString(R.string.NotTagged));
                mToast.show();
            }
            else if(d.isFound()) {
                double lon = d.getLocation().getLongitude();
                double lat = d.getLocation().getLatitude();

                double x = (lon - mLonTopLeft) * mDx;
                double y = (lat - mLatTopLeft) * mDy;

                mPlatesView.verify(x, y);
                mToast.setText(getString(R.string.VerifyMessage));
                mToast.show();
            }
            else {
                mToast.setText(getString(R.string.VerifyMessageFailed));
                mToast.show();
            }
        }

        for (int i = 0; i < POINTS; i++) {
            if (d == mDestPoint[i]) {
                if (d.isFound()) {
                    mPointLL[i] = new Coordinate(d.getLocation().getLongitude(), d.getLocation().getLatitude());
                    mToast.setText(getString(R.string.AddedPoint));
                    mToast.show();
                    mSetButton[i].setBackgroundColor(0xFF00FF00);
                } else {
                    mPointLL[i] = null;
                    mToast.setText(getString(R.string.InvalidPoint));
                    mToast.show();
                    mSetButton[i].setBackgroundColor(0xFFFF0000);
                }
            }
        }
    }
}