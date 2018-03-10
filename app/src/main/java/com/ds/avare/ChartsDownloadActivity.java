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
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.ds.avare.adapters.ChartAdapter;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.network.Delete;
import com.ds.avare.network.Download;
import com.ds.avare.content.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.RateApp;

import java.io.File;

/**
 * @author zkhan
 *
 */
public class ChartsDownloadActivity extends Activity {
    
    private String mName;
    private ProgressDialog mProgressDialog;
    private Download mDownload;
    private Delete mDelete;
    
    private Preferences mPref;
    private static ChartAdapter mChartAdapter = null;
    private Toast mToast;
    
    private StorageService mService;
    private Button mDLButton;
    private Button mUpdateButton;
    private Button mDeleteButton;
    
    /**
     * Shows warning message about Avare
     */
    private AlertDialog mAlertDialog;

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
        super.onCreate(savedInstanceState);

        mPref = new Preferences(this);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        /*
         * Show charts
         */
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.chart_download, null);
        setContentView(view);

        
        if(null == mChartAdapter) {
            /*
             * Keep states in chart adapter, so static
             */
            mChartAdapter = new ChartAdapter(this);
        }
        else {
            mChartAdapter.refreshIt();
        }
        
        ExpandableListView list = (ExpandableListView)view.findViewById(R.id.chart_download_list);
        list.setAdapter(mChartAdapter);
        list.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent,
                    View v, int groupPosition, int childPosition,
                    long id) {
                mChartAdapter.toggleChecked(groupPosition, childPosition);
                mChartAdapter.notifyDataSetChanged();        
                return false;
            }
        });

        mDLButton = (Button)view.findViewById(R.id.chart_download_button_dl);
        mDLButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                download();
            }
            
        });
        
        mUpdateButton = (Button)view.findViewById(R.id.chart_download_button_update);
        mUpdateButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mChartAdapter.checkDone();
                download();
            }
        });
        
        mDeleteButton = (Button)view.findViewById(R.id.chart_download_button_delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                delete();
            }
        });

        RateApp.rateIt(this, mPref);
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
             * Downloading
             */
            mService.setDownloading(true);
            
            /*
             * Since we are downloading new charts, clear everything old on screen.
             */
            mService.getTiles().clear();
            
            /**
             * Download database if it does not exists.
             */
            File dbase = new File(mPref.mapsFolder() + "/" + mChartAdapter.getDatabaseName());
            if(!dbase.exists()) {
                mChartAdapter.setChecked(mChartAdapter.getDatabaseName());
                mChartAdapter.notifyDataSetChanged();            
                download();
            }
            else {
                /*
                 * Create toast beforehand so multiple clicks dont throw up a new toast
                 */
                mToast.setText(getString(R.string.DownloadInst));
                mToast.show();
            }

            /*
             * See if we need to download a chart.
             * This will be done if charts do not exist.
             * LocationActivity sends this intent to download chart at GPS location for the new
             * user.
             */
            String chart = ChartsDownloadActivity.this.getIntent().getStringExtra(getString(R.string.download));
            if(null != chart) {
                mChartAdapter.setChecked(chart);
                mChartAdapter.notifyDataSetChanged();            
                download();                
            }
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();        
        Helper.setOrientationAndOn(this);

        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 
     */
    private boolean download() {
        
        if(mService == null) {
            return false;
        }
        /*
         * Download first chart in list that is checked
         */
        mName = mChartAdapter.getChecked();
        if(null == mName) {
            /*
             * Nothing to download
             */
            mToast.setText(getString(R.string.Done));
            mToast.show();
            return false;
        }
        
        mDownload = new Download(mPref.getRoot(), mHandler, mPref.getCycleAdjust());
        mDownload.start((new Preferences(getApplicationContext())).mapsFolder(), mName, mChartAdapter.isStatic(mName));
        
        mProgressDialog = new ProgressDialog(ChartsDownloadActivity.this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getString(R.string.Downloading) + "/" + 
                getString(R.string.Extracting) + " : " + mName + ".zip");
        
        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                mDownload.cancel();
                dialog.dismiss();
            }
        });
        if(!isFinishing()) {
            mProgressDialog.show();
        }
        return true;
    }

    /**
     * 
     */
    private boolean delete() {
        
        if(mService == null) {
            return false;
        }
        /*
         * Download first chart in list that is checked
         */
        mName = mChartAdapter.getDeleteChecked();
        if(null == mName) {
            /*
             * Nothing to download
             */
            mToast.setText(getString(R.string.Done));
            mToast.show();
            return false;
        }
        
        mDelete = new Delete(mHandler);
        mDelete.start((new Preferences(getApplicationContext())).mapsFolder(), mName);
        
        mProgressDialog = new ProgressDialog(ChartsDownloadActivity.this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getString(R.string.Delete) + " " + mName);
        
        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                mDelete.cancel();
                dialog.dismiss();
            }
        });
        if(!isFinishing()) {
            mProgressDialog.show();
        }
        return true;
    }

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
        if(mDownload != null) {
            mDownload.cancel();
        }
        getApplicationContext().unbindService(mConnection);
        
        if(mAlertDialog != null) {
            try {
                mAlertDialog.dismiss();
            }
            catch (Exception e){}

        }
                
        if(mProgressDialog != null) {
            try {
                mProgressDialog.dismiss();
            }
            catch (Exception e){}

        }

        /*
         * Download does update tiles
         */
        if(mService != null){
            /*
             * Not downloading
             */
            mService.setDownloading(false);
            
            /*
             *  
             */
            mService.getTiles().forceReload();
        }
        
    }
     
    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
        public void handleMessage(Message msg) {
            int result = msg.what;
                        
            /*
             * XXX: Do not know why it happens. Maybe the activity gets restarted, and then
             * download sends a message as it was a BG task.  
             */
            if(null == mName) {
                try {
                    mProgressDialog.dismiss();
                }
                catch (Exception e){}
                return;
            }

            // reset all databases on new downloads/deletes
            DataSource.reset(getApplicationContext());

            if(msg.obj instanceof Download) {
                if(Download.FAILED == result) {
                    try {
                        mProgressDialog.dismiss();
                    }
                    catch (Exception e){}

                    /*
                     * Throw a confirm dialog
                     */
                    String code = msg.getData().getString("code");
                    mAlertDialog = new DecoratedAlertDialogBuilder(ChartsDownloadActivity.this).create();
                    mAlertDialog.setMessage(getString(R.string.download) + " " + getString(R.string.Failed) + ": " + code);
                    mAlertDialog.setCanceledOnTouchOutside(false);
                    mAlertDialog.setCancelable(false);
                    mAlertDialog.setButton(ProgressDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
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
                
                
                
                if(Download.NONEED == result) {
                    try {
                        mProgressDialog.dismiss();
                    }
                    catch (Exception e){}

                }
                else if (Download.SUCCESS == result) {
                    try {
                        mProgressDialog.dismiss();
                    }
                    catch (Exception e){}

                    Toast.makeText(ChartsDownloadActivity.this, getString(R.string.download) + " "
                            + getString(R.string.Success), Toast.LENGTH_SHORT).show();
    
                    /*
                     * If TFR fetched, parse it. 
                     */
                    if(mName.equals(getString(R.string.TFRs))) {
                        mService.getTFRFetcher().parse();
                    }

                    if(mName.equals("GameTFRs")) {
                        mPref.enableGameTFRs();
                    }

                    if(mName.equals("weather")) {
                        mService.getInternetWeatherCache().parse(mService);
                        mPref.setLayerType("METAR");
                        mService.getMetarLayer().parse();
                    }
                    
                    if(mName.equals("conus")) {
                        mPref.setLayerType("NEXRAD");
                        mService.getRadarLayer().parse();
                    }
                    
                    mChartAdapter.updateVersion(mName, mDownload.getVersion());
                    mChartAdapter.unsetChecked(mName);
                    mChartAdapter.refresh();
                    download();
                }
                else {
                    if(!isFinishing()) {
                        mProgressDialog.setProgress(result);
                    }
                }
            }
            else if(msg.obj instanceof Delete) {
                if(Delete.FAILED == result) {
                    try {
                        mProgressDialog.dismiss();
                    }
                    catch (Exception e){}


                    /*
                     * Throw a confirm dialog
                     */
                    
                    mAlertDialog = new DecoratedAlertDialogBuilder(ChartsDownloadActivity.this).create();
                    mAlertDialog.setMessage(getString(R.string.Delete) + " " + getString(R.string.Failed));
                    mAlertDialog.setCanceledOnTouchOutside(false);
                    mAlertDialog.setCancelable(false);
                    mAlertDialog.setButton(ProgressDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
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
                
                if (Delete.SUCCESS == result) {
                    try {
                        mProgressDialog.dismiss();
                    }
                    catch (Exception e){}

                    Toast.makeText(ChartsDownloadActivity.this, getString(R.string.Delete) + " "
                            + getString(R.string.Success), Toast.LENGTH_SHORT).show();
    
                    if(mName.equals(getString(R.string.TFRs))) {
                        mService.deleteTFRFetcher();
                    }

                    if(mName.equals("weather")) {
                        mService.deleteInternetWeatherCache();
                    }
                    
                    if(mName.equals("conus")) {
                        mService.deleteRadar();
                    }

                    mChartAdapter.unsetChecked(mName);
                    mChartAdapter.refresh();
                    delete();
                }
                else {
                    if(!isFinishing()) {
                        mProgressDialog.setProgress(result);
                    }
                }
            }
		}
    };
}
