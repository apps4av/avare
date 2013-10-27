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



import java.util.ArrayList;

import com.ds.avare.animation.AnimateButton;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.TouchListView;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private TouchListView mPlan;
    private PlanAdapter mPlanAdapter;
    private ListView mPlanSave;
    private ArrayAdapter<String> mPlanSaveAdapter;
    private Toast mToast;
    private Button mDestButton;
    private Button mSaveButton;
    private int mIndex;
    private Preferences mPref;
    /**
     * Shows Chooseing message about Avare
     */
    private AlertDialog mAlertDialogChoose;

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
    private void inactivatePlan() {
        mService.getPlan().makeInactive();
        mActivateButton.setChecked(false);
    }
    
    /**
     * 
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if(mService == null) {
                return;
            }
            String item = mPlanAdapter.getItem(from);
            mService.getPlan().move(from, to);
            mPlanAdapter.remove(item);
            mPlanAdapter.insert(item, to);
            PlanActivity.this.updateAdapter();
            inactivatePlan();
        }
    };
    
    /**
     * 
     */
    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            if(mService == null) {
                return;
            }
            String item = mPlanAdapter.getItem(which);
            mService.getPlan().remove(which);
            mPlanAdapter.remove(item);
            PlanActivity.this.updateAdapter();
            if(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()) != null) {
                mService.setDestinationPlan(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()));
            }
            inactivatePlan();
        }
    };
    
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
        
        mPref = new Preferences(getApplicationContext());
                
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plan, null);
        setContentView(view);
        

        mPlan = (TouchListView)view.findViewById(R.id.plan_list);
        mPlan.setDropListener(onDrop);
        mPlan.setRemoveListener(onRemove);
        mPlanSave = (ListView)view.findViewById(R.id.plan_list_save);

        mService = null;
        mIndex = -1;
        mTotalText = (TextView)view.findViewById(R.id.plan_total_text);    

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
                if(mIndex >= 0) {
                    /*
                     * Set everything after this as not yet passed.
                     */
                    for(int i = mIndex; i < mService.getPlan().getDestinationNumber(); i++) {
                        mService.getPlan().setNotPassed(i);
                    }
                }
                mIndex = -1;
                ((MainActivity) PlanActivity.this.getParent()).switchTab(0);
            }   
        });

        /*
         * Save button
         */
        mSaveButton = (Button)view.findViewById(R.id.plan_button_save);
        mSaveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                Plan p;
                /*
                 * Get plan, make its string, then send to storage
                 */
                if(mService == null) {
                    return;
                }
                p = mService.getPlan();
                if(p == null) {
                    return;
                }
                int num = p.getDestinationNumber();
                if(num < 2) {
                    return;
                }
                String planStr = "";
                for(int i = 0; i < (num - 1); i++) {
                    /*
                     * Separate by >
                     */
                    planStr += p.getDestination(i).getID() + "(" + p.getDestination(i).getType() + ")" +  ">";
                }
                planStr += p.getDestination(num - 1).getID() + "(" + p.getDestination(num - 1).getType() + ")";
                mPref.addToPlans(planStr);
                prepareAdapterSave();
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
        
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> info = new ArrayList<String>();
        ArrayList<Boolean> passed = new ArrayList<Boolean>();

        for(int id = 0; id < destnum; id++) {
            name.add(mService.getPlan().getDestination(id).getID() + "(" + mService.getPlan().getDestination(id).getType() + ")");
            info.add(mService.getPlan().getDestination(id).toString());
            passed.add(mService.getPlan().isPassed(id));
        }
        mPlanAdapter.updateList(name, info, passed);
        mTotalText.setText(getString(R.string.Total) + " " + mService.getPlan().toString());
        return true;
    }

    /**
     * 
     */
    private boolean prepareAdapterSave() {
        
        ArrayList<String> list = new ArrayList<String>();
        String [] name = mPref.getPlans();
        for (int i = 0; i < name.length; i++) {
            if(name[i].equals("")) {
                continue;
            }
            list.add(name[i]);
        }
        
        mPlanSaveAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list);
        
        mPlanSave.setAdapter(mPlanSaveAdapter);

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
        
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> info = new ArrayList<String>();
        ArrayList<Boolean> passed = new ArrayList<Boolean>();

        for(int id = 0; id < destnum; id++) {
            name.add(mService.getPlan().getDestination(id).getID() + "(" + mService.getPlan().getDestination(id).getType() + ")");
            info.add(mService.getPlan().getDestination(id).toString());
            passed.add(mService.getPlan().isPassed(id));
        }
        mPlanAdapter = new PlanAdapter(PlanActivity.this, name, info, passed);
        return true;
    }

    /**
     * 
     * @param dst
     */
    private void planTo(String dst, String type) {
        /*
         * Add to plan
         */
        Destination d = new Destination(dst, type, mPref, mService);
        mService.getPlan().appendDestination(d);
        d.find();
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
            prepareAdapterSave();
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
                    AnimateButton g = new AnimateButton(getApplicationContext(), mDestButton, AnimateButton.DIRECTION_L_R, (View[])null);
                    g.animate(true);

                    return true;
                }
            }); 

            mPlanSave.setClickable(true);
            mPlanSave.setDividerHeight(10);
            mPlanSave.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {

                    final int mxindex = index;
                    
                    /*
                     * Confirm what needs to be done
                     * Delete a plan or load it
                     */
                    mAlertDialogChoose = new AlertDialog.Builder(PlanActivity.this).create();
                    mAlertDialogChoose.setCanceledOnTouchOutside(false);
                    mAlertDialogChoose.setCancelable(true);
                    mAlertDialogChoose.setTitle(getString(R.string.Plan));
                    mAlertDialogChoose.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Load), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            mService.newPlan();
                            inactivatePlan();
                            String item = mPlanSaveAdapter.getItem(mxindex).toString();
                            String tokens[] = item.split("\\)>");
                            for(int i = 0; i < tokens.length; i++) {
                                tokens[i] = tokens[i].replaceAll("\\)", "");
                                String pair[] = tokens[i].split("\\(");
                                if(pair.length < 2) {
                                    continue;
                                }
                                planTo(pair[0], pair[1]);
                            }
                            prepareAdapter();
                            mPlan.setAdapter(mPlanAdapter);            
                        }
                    });
                    mAlertDialogChoose.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.LoadReverse), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            mService.newPlan();
                            inactivatePlan();
                            String item = mPlanSaveAdapter.getItem(mxindex).toString();
                            String tokens[] = item.split("\\)>");
                            for(int i = tokens.length - 1; i >= 0; i--) {
                                tokens[i] = tokens[i].replaceAll("\\)", "");
                                String pair[] = tokens[i].split("\\(");
                                if(pair.length < 2) {
                                    continue;
                                }
                                planTo(pair[0], pair[1]);
                            }
                            prepareAdapter();
                            mPlan.setAdapter(mPlanAdapter);            
                        }
                    });
                    mAlertDialogChoose.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Delete), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            String item = mPlanSaveAdapter.getItem(mxindex).toString();
                            mPref.deleteAPlan(item);
                            prepareAdapterSave();
                        }
                    });

                    mAlertDialogChoose.show();


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

        if(null != mAlertDialogChoose) {
            try {
                mAlertDialogChoose.dismiss();
            }
            catch (Exception e) {
            }
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
