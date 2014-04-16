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
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author zkhan
 * An activity that deals with plates
 */
public class PlanActivity extends Activity  implements Observer {
    
    private StorageService mService;
    private TouchListView mPlan;
    private PlanAdapter mPlanAdapter;
    private ListView mPlanSave;
    private ArrayAdapter<String> mPlanSaveAdapter;
    private Toast mToast;
    private Button mDestButton;
    private Button mSaveButton;
    private Button mDeleteButton;
    private Button mPlanButton;
    private Button mPlatesButton;
    private EditText mSaveText;
    private EditText mPlanText;
    private int mIndex;
    private Preferences mPref;
    private String mPlans[];
    private String mSearchDests[];
    private ProgressBar mProgressBar;
    
    /**
     * Shows Choose message about Avare
     */
    private AlertDialog mAlertDialogChoose;
    private AlertDialog mAlertDialogPlan;

    private ToggleButton mActivateButton;
    private TextView mTotalText;
    private Destination mDestination;
    private AnimateButton mAnimateDest;
    private AnimateButton mAnimateDelete;
    private AnimateButton mAnimatePlates;
    private Timer mTimer;


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
        ((MainActivity)this.getParent()).showMapTab();
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
     * @param which
     */
    private void removePlan(int which) {
        if(mService == null) {
            return;
        }
        if(which >= mPlanAdapter.getCount()) {
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
    
    
    /**
     * 
     */
    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            removePlan(which);
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
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        
        mPref = new Preferences(getApplicationContext());
              
        mPlans = mPref.getPlans();
        
        /*
         * This keeps a copy of destinations under search when composite plan is entered.
         */
        mSearchDests = null;
        
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

        mProgressBar = (ProgressBar)view.findViewById(R.id.plan_progress_bar);

        /*
         * Dest button
         */
        mDeleteButton = (Button)view.findViewById(R.id.plan_button_delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mIndex >= 0) {
                    removePlan(mIndex);
                }
                mIndex = -1;
            }   
        });
        
        /*
         * 
         */
        mPlatesButton = (Button)view.findViewById(R.id.plan_button_plates);
        mPlatesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mService != null) {
                    mService.setLastPlateAirport(mDestination.getID());
                    mService.setLastPlateIndex(0);
                }
                ((MainActivity) PlanActivity.this.getParent()).showPlatesTab();
            }   
        });        

        /*
         * Plan button
         */
        mPlanText = (EditText)view.findViewById(R.id.plan_edit_text);
        mPlanButton = (Button)view.findViewById(R.id.plan_button_find);
        mPlanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * Confirm what needs to be done
                 * Load a new plan or not
                 */
                /*
                 * Now start the search.
                 * Clear everything before that
                 * If still searching, dont do another search;
                 */
                if(mSearchDests != null) {
                    return;
                }
                final String plan = mPlanText.getText().toString().toUpperCase(Locale.getDefault());
                mAlertDialogPlan = new AlertDialog.Builder(PlanActivity.this).create();
                mAlertDialogPlan.setCanceledOnTouchOutside(false);
                mAlertDialogPlan.setCancelable(true);
                mAlertDialogPlan.setTitle(getString(R.string.Plan));
                mAlertDialogPlan.setMessage(getString(R.string.PlanWarning) + " \"" + plan + "\" ?");
                mAlertDialogPlan.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        /*
                         * Show that we are searching by showing progress bar
                         */
                        mProgressBar.setVisibility(View.VISIBLE);
                        mService.getPlan().clear();
                        prepareAdapter();
                        prepareAdapterSave();
                        mPlan.setAdapter(mPlanAdapter);

                        mSearchDests = plan.split(" ");
                        searchDest();
                    }
                });
                mAlertDialogPlan.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                mAlertDialogPlan.show();              
            }   
        });

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
                ((MainActivity) PlanActivity.this.getParent()).showMapTab();
            }   
        });

        /*
         * Save button
         */
        mSaveText = (EditText)view.findViewById(R.id.plan_text_save);
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
                /*
                 * Put plan name with :: and since comma separates, remove it. 
                 */
                String planName =  mSaveText.getText().toString().replace(",", " ");
                if(planName.equals("")) {
                    planName = getString(R.string.Plan);
                }
                String planStr = planName + "::";
                for(int i = 0; i < (num - 1); i++) {
                    /*
                     * Separate by >, add type in ()
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

        mAnimateDest = new AnimateButton(getApplicationContext(), mDestButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateDelete = new AnimateButton(getApplicationContext(), mDeleteButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimatePlates = new AnimateButton(getApplicationContext(), mPlatesButton, AnimateButton.DIRECTION_L_R, (View[])null);
    }

    /**
     * Search in list format
     */
    private void searchDest() {
        if(null != mSearchDests) {
            if(mSearchDests.length > 0) {
                /*
                 * Make a list of waypoints to find. Then find one by one.
                 * No guarantee that they will be found in order. So order them.
                 */
                int wp = 0;
                for(wp = 0; wp < mSearchDests.length; wp++) {
                    if(mSearchDests[wp] == null) {
                        continue;
                    }
                    if(mSearchDests[wp].length() == 0) {
                        mSearchDests[wp] = null;
                        continue;
                    }
                    
                    planToWithVerify(mSearchDests[wp]);
                    break;
                }
                /*
                 * All done.
                 */
                if(mSearchDests.length == wp) {
                    prepareAdapter();
                    mPlan.setAdapter(mPlanAdapter);
                    /*
                     * Show that we are looking
                     */
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mSearchDests = null;
                }
            }
        }
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
        mPlans = mPref.getPlans();
        for (int i = 0; i < mPlans.length; i++) {
            if(mPlans[i].equals("")) {
                continue;
            }
            String[] split = mPlans[i].split("::");
            if(split.length < 2) {
                continue;
            }
            list.add(split[0]);
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

    /**
     * 
     * @param dst
     */
    private void planToWithVerify(String dst) {
        /*
         * Add to plan
         */
        Destination d = new Destination(dst, "", mPref, mService);
        d.addObserver(this);
        d.findGuessType();
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
                    mAnimateDest.animate(true);
                    mAnimateDelete.animate(true);
                    if(PlatesActivity.doesAirportHavePlates(mPref.mapsFolder(), mDestination.getID())) {
                    	mAnimatePlates.animate(true);
                    }
                    else {
                    	mAnimatePlates.stopAndHide();
                    }

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
                            if(mxindex < 0 || mxindex >= mPlans.length) {
                                return;
                            }
                            String item = mPlans[mxindex];
                            String items[] = item.split("::");
                            if(items.length < 2) { 
                                return;
                            }
                  
                            mSaveText.setText(items[0]);
                            String tokens[] = items[1].split("\\)>");
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
                            if(mxindex < 0 || mxindex >= mPlans.length) {
                                return;
                            }
                            String item = mPlans[mxindex];
                            String items[] = item.split("::");
                            if(items.length < 2) { 
                                return;
                            }
                  
                            mSaveText.setText(items[0]);
                            String tokens[] = items[1].split("\\)>");
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
                            if(mxindex < 0 || mxindex >= mPlans.length) {
                                return;
                            }
                            mPref.deleteAPlan(mPlans[mxindex]);
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

        if(null != mAlertDialogPlan) {
            try {
                mAlertDialogPlan.dismiss();
            }
            catch (Exception e) {
            }
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);
        
        if(mTimer != null) {
            mTimer.cancel();
        }
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
        
        /*
         * Create sim timer
         */
        mTimer = new Timer();
        TimerTask sim = new UpdateTask();
        mTimer.scheduleAtFixedRate(sim, 0, 1000);
    }

    /**
     * 
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
        Destination dest = (Destination)arg0;
        if(null == mSearchDests) {
            return;
        }
        if(null == dest) {
            return;
        }
        
        /*
         * Find next
         */
        for(int wp = 0; wp < mSearchDests.length; wp++) {
            if(null != mSearchDests[wp]) {
                if(dest.isFound()) {
                    /*
                     * Add to plan
                     */
                    mService.getPlan().appendDestination(dest);
                }
                else {
                    mToast.setText(getString(R.string.PlanDestinationNF));
                    mToast.show();
                }
                mSearchDests[wp] = null;
                break;
            }
        }
        searchDest();        
    }
    
    /**
     * @author zkhan
     *
     */
    private class UpdateTask extends TimerTask {
        
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        public void run() {

            /*
             * In sim mode, keep feeding location
             */
            if(mPref.isSimulationMode()) {
                mHandler.sendEmptyMessage(0);
            }

        }
    }
    
    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(mService != null) {
                mService.getPlan().simulate();
                updateAdapter();
            }
        }
    };
}
