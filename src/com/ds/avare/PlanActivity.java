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



import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Destination;
import com.ds.avare.utils.Helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlanActivity extends Activity {
    
    private StorageService mService;
    private ListView mPlan;
    private PlanAdapter mPlanAdapter;
    private Toast mToast;
    private Button mDeleteButton;
    private Button mDestButton;
    private int mIndex;

    private ToggleButton mActivateButton;
    private TextView mTotalText;
    private Destination mDestination;

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if(location != null && mService != null) {
                updateAdapter();
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }          
    };

    /*
     * For being on tab this activity discards back to main activity
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).switchTab(0);
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plan, null);
        setContentView(view);
        

        mPlan = (ListView)view.findViewById(R.id.plan_list);

        
        mService = null;
        mIndex = -1;
        mTotalText = (TextView)view.findViewById(R.id.plan_total_text);
        
        mDeleteButton = (Button)view.findViewById(R.id.plan_button_delete);
        mDeleteButton.getBackground().setAlpha(255);
        mDeleteButton.setOnClickListener(new OnClickListener() {

            @Override
            /*
             * (non-Javadoc)
             * Delete the plan destination
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            public void onClick(View v) {
                if(mIndex >= 0) {
                    mService.getPlan().remove(mIndex);
                    prepareAdapter();
                    mPlan.setAdapter(mPlanAdapter);
                    mPlanAdapter.notifyDataSetChanged();
                    if(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()) != null) {
                        mService.setDestinationPlan(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()));
                    }
                    mIndex = -1;
                }
            }
            
        });

        /*
         * Dest button
         */
        mDestButton = (Button)view.findViewById(R.id.plan_button_dest);
        mDestButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * On click, find destination that was pressed on in view
                 */
                mService.setDestination(mDestination);
                mToast.setText(getString(R.string.DestinationSet) + mDestination.getID());
                mToast.show();
                mDestination = null;
                /*
                 * Set everything behind this as passed.
                 */
                for(int i = 0; i < mIndex; i++) {
                    mService.getPlan().setPassed(i);
                }
                /*
                 * Set everything after this as not yet passed.
                 */
                for(int i = mIndex; i < mService.getPlan().getDestinationNumber(); i++) {
                    mService.getPlan().setNotPassed(i);
                }
                mIndex = -1;
                ((MainActivity) PlanActivity.this.getParent()).switchTab(0);
            }   
        });

        mActivateButton = (ToggleButton)view.findViewById(R.id.plan_button_activate);
        mActivateButton.getBackground().setAlpha(255);
        mActivateButton.setOnClickListener(new OnClickListener() {

            @Override
            /*
             * (non-Javadoc)
             * Delete the plan destination
             * @see android.view.View.OnClickListener#onClick(android.view.View)
             */
            public void onClick(View v) {
                /*
                 * Make plan active/inactive in which case, track will be drawn to dest/plan
                 */
                if(null != mService) {
                    if(mActivateButton.getText().equals(getString(R.string.Inactive))) {
                        mService.getPlan().makeInactive();
                        mService.getGpsParams();
                    }
                    else {
                        if(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()) != null) {
                            mService.setDestinationPlan(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()));
                        }
                    }
                }
            }
            
        });
        
    }

    /**
     * 
     */
    private boolean updateAdapter() {
        if(null == mPlanAdapter) {
            return false;
        }
        int destnum = mService.getPlan().getDestinationNumber();
        
        final String [] name = new String[destnum];
        final String [] info = new String[destnum];
        final boolean [] passed = new boolean[destnum];

        for(int id = 0; id < destnum; id++) {
            name[id] = mService.getPlan().getDestination(id).getID() + "(" + mService.getPlan().getDestination(id).getType() + ")";
            info[id] = mService.getPlan().getDestination(id).toString();
            passed[id] = mService.getPlan().isPassed(id);
        }
        mPlanAdapter.updateList(name, info, passed);
        mTotalText.setText(getString(R.string.Total) + " " + mService.getPlan().toString());
        return true;
    }

    /**
     * 
     */
    private boolean prepareAdapter() {
        int destnum = mService.getPlan().getDestinationNumber();
        if(0 == destnum) {
            mToast.setText(getString(R.string.PlanNF));
            mToast.show();
        }
        
        final String [] name = new String[destnum];
        final String [] info = new String[destnum];
        final boolean [] passed = new boolean[destnum];

        for(int id = 0; id < destnum; id++) {
            name[id] = mService.getPlan().getDestination(id).getID() + "(" + mService.getPlan().getDestination(id).getType() + ")";
            info[id] = mService.getPlan().getDestination(id).toString();
            passed[id] = mService.getPlan().isPassed(id);
        }
        mPlanAdapter = new PlanAdapter(PlanActivity.this, name, info, passed);
        return true;
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

            prepareAdapter();
            mPlan.setAdapter(mPlanAdapter);
            
            mPlan.setClickable(true);
            mPlan.setDividerHeight(10);
            mPlan.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
                    mIndex = index;
                            
                    mDestination = mService.getPlan().getDestination(index);
                    mDestButton.setText(mDestination.getID());
                    AnimateButton f = new AnimateButton(getApplicationContext(), mDeleteButton, AnimateButton.DIRECTION_L_R, (View[])null);
                    AnimateButton g = new AnimateButton(getApplicationContext(), mDestButton, AnimateButton.DIRECTION_L_R, (View[])null);
                    f.animate(true);
                    g.animate(true);

                    return true;
                }
            }); 

            /*
             * Set proper state of the active button.
             * Plan only active when more than one dest.
             */
            if((!mService.getPlan().isActive()) || (mService.getPlan().getDestinationNumber() <= 0)) {
                mActivateButton.setChecked(false);                
            }
            else {
                mActivateButton.setChecked(true);
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
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

}
