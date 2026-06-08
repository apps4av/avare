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
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.ds.avare.adapters.ChartAdapter;
import com.ds.avare.gps.Gps;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.network.Delete;
import com.ds.avare.network.Download;
import com.ds.avare.content.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.RateApp;
import com.ds.avare.utils.RevenueCatService;
import com.ds.avare.utils.Telemetry;
import com.ds.avare.utils.TelemetryParams;

import java.io.File;

/**
 * @author zkhan
 *
 */
public class ChartsDownloadActivity extends BaseActivity {
    
    private String mName;
    private ProgressDialog mProgressDialog;
    private Download mDownload;
    private Delete mDelete;
    
    private static ChartAdapter mChartAdapter = null;
    private Toast mToast;
    
    private Button mDLButton;
    private Button mUpdateButton;
    private Button mDeleteButton;
    private Button mLegendButton;

    private WebView mWebview;

    /**
     * Shows warning message about Avare
     */
    private AlertDialog mAlertDialog;

    /**
     * Cached Paid-entitlement result for the current download batch. Null
     * until the first chart in the batch trips the per-category gate;
     * after that, reused for the rest of the batch so we don't ask
     * RevenueCat once per skipped item. Reset to null when the batch
     * finishes (no more checked items) or when the activity pauses.
     */
    private Boolean mBatchProEntitled = null;

    /**
     * True once we've shown the "Paid Subscription Required" dialog at
     * least once during the current batch, so that skipping several
     * gated charts back-to-back doesn't spawn a stack of dialogs.
     */
    private boolean mBatchProWarned = false;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        super.onBackPressedExit();
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mWebview = (WebView)view.findViewById(R.id.chart_download_webview);
        mWebview.loadUrl((com.ds.avare.utils.Helper.getWebViewFile(getApplicationContext(), "chart")));
        mWebview.getSettings().setBuiltInZoomControls(true);

        mDLButton = (Button)view.findViewById(R.id.chart_download_button_dl);
        mDLButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                download();
            }
            
        });

        mLegendButton = (Button)view.findViewById(R.id.chart_download_button_legend);
        mLegendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mWebview.getVisibility() == View.INVISIBLE) {
                    mWebview.setVisibility(View.VISIBLE);
                }
                else {
                    mWebview.setVisibility(View.INVISIBLE);
                }
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

        RateApp.rateIt(ChartsDownloadActivity.this, mPref);
    }
            
    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();

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
         * Download database if it does not exists. Download sectional at current position as well.
         */
        File dbase = new File(mPref.getServerDataFolder() + File.separator + mChartAdapter.getDatabaseName());
        if(!dbase.exists()) {
            mChartAdapter.setChecked(mChartAdapter.getSectional(Gps.getLastLocation()));
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


    /**
     * Entry point used by the Download / Update buttons and by the
     * recursive continuation in {@link #mHandler}. Peeks at the next
     * checked chart and runs the per-chart Paid-subscription gate, then
     * either starts the download or skips/blocks the chart.
     *
     * The gate is re-evaluated on every call so that the on-disk state
     * updated by the previous successful download is respected live: if
     * the user checked two new sectionals, the first downloads, the
     * second is then blocked because the category now has one
     * downloaded chart.
     */
    private boolean download() {
        final String name = mChartAdapter.getChecked();
        if (name == null) {
            endBatch();
            return false;
        }

        if (!mChartAdapter.requiresProForChart(name)) {
            return downloadOne(name);
        }

        // This chart needs Paid. Use cached entitlement if we already
        // know the answer for this batch.
        if (mBatchProEntitled != null) {
            if (mBatchProEntitled) {
                return downloadOne(name);
            }
            return skipGatedChart(name);
        }

        // First gated chart of the batch — ask RevenueCat.
        RevenueCatService.isProEntitled(new RevenueCatService.EntitlementCallback() {
            @Override
            public void onResult(final boolean entitled) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        mBatchProEntitled = entitled;
                        // Re-enter the gate with the cached entitlement;
                        // the new state will route the same chart to
                        // downloadOne() or skipGatedChart() as needed.
                        download();
                    }
                });
            }
        });
        return true;
    }

    /**
     * Free user tried to download a chart that would exceed the
     * one-per-category limit. Uncheck it so the batch can keep moving
     * through any remaining (exempt or update) items, and surface the
     * Paid-subscription dialog the first time it happens this batch.
     */
    private boolean skipGatedChart(String name) {
        mChartAdapter.unsetChecked(name);
        mChartAdapter.notifyDataSetChanged();
        if (!mBatchProWarned) {
            mBatchProWarned = true;
            showProRequiredDialog();
        }
        return download();
    }

    /**
     * Reset per-batch state when the chain runs dry.
     */
    private void endBatch() {
        mBatchProEntitled = null;
        mBatchProWarned = false;
        mToast.setText(getString(R.string.Done));
        mToast.show();
    }

    /**
     * Show the "Paid subscription required" prompt offering a Subscribe
     * shortcut into {@link ProActivity}.
     */
    private void showProRequiredDialog() {
        if (isFinishing()) {
            return;
        }
        DecoratedAlertDialogBuilder builder =
                new DecoratedAlertDialogBuilder(ChartsDownloadActivity.this);
        builder.setTitle(getString(R.string.ProDownloadLimitTitle));
        builder.setMessage(getString(R.string.ProDownloadLimitMessage));
        builder.setCancelable(false);
        builder.setNegativeButton(getString(R.string.Cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setPositiveButton(getString(R.string.ProServicesSubscribe),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        try {
                            startActivity(new Intent(
                                    ChartsDownloadActivity.this,
                                    ProActivity.class));
                        } catch (Throwable ignored) {
                            // Paid screen is optional
                        }
                    }
                });
        mAlertDialog = builder.create();
        try {
            mAlertDialog.show();
        } catch (Throwable ignored) {
            // ignore - window may be gone
        }
    }

    /**
     * Actually kicks off the download for the given chart. Caller is
     * responsible for having peeked the next checked item via {@link
     * ChartAdapter#getChecked()} and passed the per-chart Paid gate.
     */
    private boolean downloadOne(String name) {
        mName = name;

        mDownload = new Download(mPref.getRoot(), mHandler, mPref.getCycleAdjust());
        mDownload.start(StorageService.getInstance().getPreferences().getServerDataFolder(), mName, mChartAdapter.isStatic(mName));

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
        mDelete.start(StorageService.getInstance().getPreferences().getServerDataFolder(), mName);
        
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
        
        mService.unregisterGpsListener(mGpsInfc);
        /*
         * Clean up on pause that was started in on resume
         */
        if(mDownload != null) {
            mDownload.cancel();
        }

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
         * Not downloading
         */
        mService.setDownloading(false);

        // Force re-checking the Paid gate on the next batch.
        mBatchProEntitled = null;
        mBatchProWarned = false;

        /*
         *
         */
        mService.getTiles().forceReload();

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
            DataSource.reset();

            if(msg.obj instanceof Download) {
                Telemetry t = new Telemetry(getApplicationContext());
                TelemetryParams p = new TelemetryParams();
                if(Download.FAILED == result) {
                    p.add(TelemetryParams.CHART_NAME, mName);
                    p.add(TelemetryParams.STATUS, TelemetryParams.FAILED);
                    t.sendEvent(Telemetry.CHART_DOWNLOAD, p);
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
                    p.add(TelemetryParams.CHART_NAME, mName);
                    p.add(TelemetryParams.STATUS, TelemetryParams.SUCCESS);
                    t.sendEvent(Telemetry.CHART_DOWNLOAD, p);
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
                        mService.getInternetWeatherCache().parse();
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
