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
import java.util.Observable;
import java.util.Observer;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.Helper;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
 
/**
 * 
 * @author zkhan
 *
 */
public class PlanActivity extends Activity implements Observer {

    private StorageService mService;
    
    private ListView mSearchListView;
    private EditText mSearchText;
    private Preferences mPref;
    private Toast mToast;
    private SearchAdapter mAdapter;
    private SearchTask mSearchTask;
    private ProgressBar mProgressBar;
    
    /**
     * Current destination info
     */
    private Destination mDestination;

    
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
     * @param dst
     */
    private void goTo(String dst, String type) {
        mDestination = new Destination(dst, type, mPref, mService);
        mDestination.addObserver(PlanActivity.this);
        mToast.setText(getString(R.string.Searching) + " " + dst);
        mToast.show();
        mDestination.find();
    }
    
    /**
     * 
     */
    private void initList() {
        String [] vals = mPref.getRecent();        
        mAdapter = new SearchAdapter(PlanActivity.this, vals);
        mSearchListView.setAdapter(mAdapter);
    }
    
    @Override
    /**
     * 
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
 
                
        mService = null;
        mPref = new Preferences(getApplicationContext());
        
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plan, null);        
        setContentView(view);
        
        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        /*
         * For a search query
         */
        mSearchListView = (ListView)view.findViewById(R.id.plan_list_view);
        
        /*
         * Progress bar
         */
        mProgressBar = (ProgressBar)(view.findViewById(R.id.plan_progress_bar));
        
        /*
         * Now initialize the list to recent in case someone needs to go there, and not search
         */
        initList();
        
        /*
         * Set on click
         */
        mSearchListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                    long arg3) {
                String id = StringPreference.parseHashedNameId(mAdapter.getItem(position)); 
                String destType = StringPreference.parseHashedNameDestType(mAdapter.getItem(position)); 
                if(id == null || destType == null) {
                    return;
                }
                goTo(id, destType);
            }
        });


        /*
         * For searching, start search on every new key press
         */
        mSearchText = (EditText)view.findViewById(R.id.plan_edit_text);
        mSearchText.addTextChangedListener(new TextWatcher() { 
            @Override
            public void afterTextChanged(Editable arg0) {
            }
    
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
    
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int after) {
                
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if(null != mSearchTask) {
                    if (!mSearchTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                        /*
                         * Cancel the last query
                         */
                        mSearchTask.cancel(true);
                    }
                }
                
                /*
                 * If text is 0 length or too long, then do not search, show last list
                 */
                if(0 == s.length()) {
                    initList();
                    return;
                }
                
                if(s.toString().contains(" ")) {
                    String [] vals = new String[1];
                    StringPreference sp = new StringPreference(Destination.MAPS, Destination.MAPS, Destination.MAPS, s.toString());
                    vals[0] = sp.getHashedName();
                    mAdapter = new SearchAdapter(PlanActivity.this, vals);
                    mSearchListView.setAdapter(mAdapter);
                    return;
                }
                /*
                 * This is a geo coordinate with &?
                 */
                if(s.toString().contains("&")) {
                    String [] vals = new String[1];
                    StringPreference sp = new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS, s.toString());
                    vals[0] = sp.getHashedName();
                    mAdapter = new SearchAdapter(PlanActivity.this, vals);
                    mSearchListView.setAdapter(mAdapter);
                    return;
                }
                mProgressBar.setVisibility(ProgressBar.VISIBLE);

                mSearchTask = new SearchTask();
                mSearchTask.execute(s.toString());

            }
        });
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

        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

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
        getApplicationContext().unbindService(mConnection);

    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onRestart()
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
 
    /**
     * 
     */
    @Override
    public void update(Observable arg0, Object arg1) {
        /*
         * Destination found?
         */
        if(arg0 instanceof Destination) {
            Boolean result = (Boolean)arg1;
            if(result) {
            
                /*
                 * Temporarily move to destination by giving false GPS signal.
                 */
                if(null == mDestination) {
                    mToast.setText(getString(R.string.DestinationNF));
                    mToast.show();
                    return;
                }
                if(mService != null) {
                    mService.setDestination((Destination)arg0);
                }
                mPref.addToRecent(mDestination.getStorageName());
                
                mToast.setText(getString(R.string.DestinationSet) + ((Destination)arg0).getID());
                mToast.show();
                /*
                 * Switch back to main activity
                 */
                Intent i = new Intent(PlanActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            }
            else {
                mService.setDestination((Destination)null);
                mToast.setText(getString(R.string.DestinationNF));
                mToast.show();
            }
        }
    }

    /**
     * @author zkhan
     *
     */
    private class SearchTask extends AsyncTask<Object, Void, Boolean> {

        private String[] selection;

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {
            
            String srch = (String)vals[0];
            if(null == mService) {
                return false;
            }

            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            if(mService.getDBResource().search(srch, params)) {
                selection = new String[params.size()];
                int iterator = 0;
                for(String key : params.keySet()){
                    selection[iterator] = StringPreference.getHashedName(params.get(key), key);
                    iterator++;
                }
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            /*
             * Set new search adapter
             */

            if(null == selection) {
                return;
            }
            mAdapter = new SearchAdapter(PlanActivity.this, selection);
            mSearchListView.setAdapter(mAdapter);
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

}