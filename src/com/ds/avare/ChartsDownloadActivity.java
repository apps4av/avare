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



import java.io.File;
import java.util.Observable;
import java.util.Observer;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * @author zkhan
 *
 */
public class ChartsDownloadActivity extends ListActivity implements Observer {
    
    private String mName;
    private ProgressDialog mProgressDialog;
    private Download mDownload;
    
    private String[] resNames; 

    private String[] resFiles;
    private Preferences mPref;
    private ChartAdapter mChartAdapter;

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref = new Preferences(this);
        File file = new File(mPref.mapsFolder() + "/" + 
                getResources().getStringArray(R.array.resFiles)[0]);
        if(file.exists()) {
            resNames = 
                    getResources().getStringArray(R.array.resNames);
            resFiles = 
                    getResources().getStringArray(R.array.resFiles);            
        }
        else {
            resNames = 
                    getResources().getStringArray(R.array.resNamesNoDb);
            resFiles = 
                    getResources().getStringArray(R.array.resFilesNoDb);
        }

        /*
         * Show charts
         */
        mChartAdapter = new ChartAdapter(this, resNames, resFiles); 
        setListAdapter(mChartAdapter);
    }
    

    /**
     * 
     */
    @Override
    public void onResume() {
        super.onResume();        
        Helper.setOrientationAndOn(this);
    }

    /**
     * 
     */
    private void download() {
        
        /*
         * Download first chart in list that is checked
         */
        int i;
        for(i = 0; i < resFiles.length; i++) {
            if(mChartAdapter.getChecked(i)) {
                mName = resFiles[i];
                break;
            }
        }
        if(i == resFiles.length) {
            return;
        }

        mDownload = new Download(getApplicationContext());
        mDownload.addObserver(ChartsDownloadActivity.this);
        mDownload.start((new Preferences(getApplicationContext())).mapsFolder(), mName);
        
        mProgressDialog = new ProgressDialog(ChartsDownloadActivity.this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMessage(getString(R.string.Downloading) + "/" + 
                getString(R.string.Extracting) + " : " + mName + ".zip");
        
        mProgressDialog.setButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            /* (non-Javadoc)
             * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
             */
            public void onClick(DialogInterface dialog, int which) {
                mDownload.cancel();
                try {
                    dialog.dismiss();
                }
                catch (Exception e) {
                    
                }
            }
        });
        
        /*
         * This should consume all keys
         */
        mProgressDialog.setOnKeyListener(new OnKeyListener(){
            @Override
            public boolean onKey(DialogInterface dialog, 
                                 int keyCode,
                                 KeyEvent event) {
                mDownload.cancel();
                try {
                    dialog.dismiss();
                }
                catch (Exception e) {
                    
                }
                return true;
            }
        });

        mProgressDialog.show();

    }
    
    
    /**
     * 
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        mChartAdapter.updateChecked(resFiles[position]);
        mChartAdapter.notifyDataSetChanged();        

        return;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.download, menu);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()) {
        
            case R.id.help:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(NetworkHelper.getHelpDownload())));
                break;

            case R.id.downloadchart:
                download();
                break;
                
            case R.id.updatechart:
                mChartAdapter.checkOld();
                mChartAdapter.notifyDataSetChanged();
                download();
                break;
        }
        return false;
    }

    /**
     * 
     */
    @Override
    public void update(Observable arg0, Object result) {
        
        Message msg = mHandler.obtainMessage();
        
        msg.what = (Integer)result;
        mHandler.sendMessage(msg);
    }
    
    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
	private Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
            int result = msg.what;
            if(Download.FAILED == result) {
                try {
                    mProgressDialog.dismiss();
                }
                catch (Exception e) {
                    
                }
                Toast.makeText(ChartsDownloadActivity.this, getString(R.string.download) + " " 
                        + getString(R.string.Failed), Toast.LENGTH_SHORT).show();
            }
            if(Download.NONEED == result) {
                try {
                    mProgressDialog.dismiss();
                }
                catch (Exception e) {
                }
            }
            else if (Download.SUCCESS == result) {
                try {
                    mProgressDialog.dismiss();
                }
                catch (Exception e) {
                    
                }
                Toast.makeText(ChartsDownloadActivity.this, getString(R.string.download) + " " 
                        + getString(R.string.Success), Toast.LENGTH_SHORT).show();

                mChartAdapter.updateVersion(mName, mDownload.getVersion());
                mChartAdapter.updateChecked(mName);
                mChartAdapter.refresh();
                download();
            }
            else {
                try {
                    mProgressDialog.setProgress(result);
                }
                catch (Exception e) {                    
                }
            }
            
        }
    };
}
