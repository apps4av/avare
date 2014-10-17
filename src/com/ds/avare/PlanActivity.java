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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author zkhan
 * An activity that deals with flight plans - loading, creating, deleting and activating
 */
public class PlanActivity extends Activity  implements Observer {
	// System objects we need to survive
    private Preferences    mPref;
    private StorageService mService;

    // Currently loaded plan detail
    private TouchListView  mPlanDetail;
    private PlanAdapter    mPlanDetailAdapter;
    private TextView       mPlanDetailSummary;

    // Collection of known saved plans
    private ListView             mSavedPlanList;
    private ArrayAdapter<String> mPlanListAdapter;

    // Items enabling the user to save the in memory plan 
    private Button        mSaveButton;
    private EditText      mSaveText;

    // The saved plans drawer control
    private SlidingDrawer mPlanDrawer;
    
    // To create a plan from a text string
    private Button   mCreatePlanFromString;
    private EditText mPlanStringText;
    private String   mSearchWaypoints[];
    private boolean  mClearPlanStringText;
    
    // Used for long press events of the detail plan
    private int         mIndex;
    private Destination mDestination;

    // Collection of all known saved plans
    private String mAllPlans[];
    
    // Used when we have long background searches going on
    private ProgressBar mProgressBar;
    
    // Dialogs to get user input on some actions
    private AlertDialog mDlgLoadOrDelete;
    private AlertDialog mDlgCreatePlan;

    // For displaying some status messages
    private Toast mToast;

    // Plan control buttons
    private ToggleButton mActivateButton;
    private Button 		 mAdvanceButton;
    private Button 		 mRegressButton;
    
    // The 3 sliding animation buttons on the left
    private Button mDestButton;
    private Button mDeleteButton;
    private Button mPlatesButton;
    private AnimateButton mAnimateDest;
    private AnimateButton mAnimateDelete;
    private AnimateButton mAnimatePlates;

    // A timer object to handle things when we are in sim mode 
    private Timer mTimer;
    
	/***
	 * Declare a new GPS interface to handle position notifications
	 * as we move throughout the sky 
	 */
    private GpsInterface mGpsInfc = new GpsInterface() {

    	// Status. Nothing we care about here
        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        // A location message. 
        @Override
        public void locationCallback(Location location) {
        	// If the location is valid and we have a background
        	// service, then tell the plan detail adapter to update its info
            if(location != null && mService != null) {
                updatePlanDetailAdapter();
            }
        }

        // Timeout, we don't care
        @Override
        public void timeoutCallback(boolean timeout) {
        }

        // Enabled, we don't care
        @Override
        public void enabledCallback(boolean enabled) {
        }
    };

	/***
	 * When the BACK button is pressed, go directly to the 
	 * MAP/CHART tab 
	 */
    @Override
    public void onBackPressed() {
        ((MainActivity)this.getParent()).showMapTab();
    }

    /**
     * Helper method to deactivate the current plan 
     */
    private void inactivatePlan() {
        mService.getPlan().makeInactive();
        mActivateButton.setChecked(false);
    }
    
    /**
     * To handle when a line from the plan detail is dropped to a new location 
     * in the plan detail
     */
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {

    	// A line of text was just dropped
    	@Override
        public void drop(int from, int to) {
    		
    		// We need the background service
            if(mService == null) {
                return;
            }
            
            // Fetch the line item that corresponds to where this came from
            String item = mPlanDetailAdapter.getItem(from);
            
            // Tell the plan to move the line FROM to TO
            mService.getPlan().move(from, to);
            
            // Remove this item from the displayed plan detail
            mPlanDetailAdapter.remove(item);
            
            // Insert the item back into the detail at the TO location
            mPlanDetailAdapter.insert(item, to);
            
            // Update the display detail of the displayed plan
            updatePlanDetailAdapter();
            
            // Ensure the plan is turned OFF
            inactivatePlan();
        }
    };
    
    /**
     * Handle when a waypoint is interactively removed form the plan detail. This
     * is done by "grabbing" the hand portion of the waypoint detail and "flinging" 
     * it to the top right of the screen 
     */
    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            removeFromPlanDetail(which);
        }
    };
    
    /**
     * Remove the indicated waypoint from the detailed plan 
     * @param which
     */
    private void removeFromPlanDetail(int which) {
    	
    	// Ensure we have the background service
        if(mService == null) {
            return;
        }
        
        // The index needs to be in range of what we have showing
        if(which >= mPlanDetailAdapter.getCount()) {
            return;
        }
        
        // Get the line item
        String item = mPlanDetailAdapter.getItem(which);
        
        // Remove this indexed waypoint from the active plan
        mService.getPlan().remove(which);
        
        // Remove this waypoint item from the detail display
        mPlanDetailAdapter.remove(item);
        
        // Tell the plan detail to update its content
        updatePlanDetailAdapter();
        
        // Calculate the next waypoint that we have not yet passed
        Destination nextWaypoint = mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()); 
        if(null != nextWaypoint) {
            mService.setDestinationPlan(nextWaypoint);
        }
        
        // Deactivate the current plan
        inactivatePlan();        
    }

    /**
     * We are just starting up. Get this tab in shape to be doing some work 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

    	// Helper method to set the day/nite theme of the tab
        Helper.setTheme(this);
        
        // Call the super method so it can do its work
        super.onCreate(savedInstanceState);

        // Create toast beforehand so multiple clicks dont throw up a new toast
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        // Get our preferences object in order
        mPref = new Preferences(getApplicationContext());
              
        // This keeps a copy of destinations under search when composite plan is entered.
        mSearchWaypoints = null;
        
        // Get views from XML
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.plan, null);
        setContentView(view);
        
        // Set our drop and remove listeners for the plan detail area
        mPlanDetail = (TouchListView)view.findViewById(R.id.plan_list);
        mPlanDetail.setDropListener(onDrop);
        mPlanDetail.setRemoveListener(onRemove);

        // The control that handles the list of saved plans
        mSavedPlanList = (ListView)view.findViewById(R.id.plan_list_save);

        // Ensure we have no service pointer yet. That comes in when we get 
        // a connection notification
        mService = null;
        
        // The currently selected waypoint index from the displayed plan
        mIndex = -1;
        
        // Top line of the plan detail area
        mPlanDetailSummary = (TextView)view.findViewById(R.id.plan_total_text);    

        // Progress bar, normally not visible
        mProgressBar = (ProgressBar)view.findViewById(R.id.plan_progress_bar);

        // Handle when the Direct/Destination button is pressed. This is normally hidden, but 
        // comes in to view when one of the waypoints in the plan detail is long pressed
        mDestButton = (Button)view.findViewById(R.id.plan_button_dest);
        mDestButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            	// mDestination was set when the user long pressed on the waypoint
            	// tell the service that is where we want to go next
                mService.setDestination(mDestination);
                
                // Tell the user via a slice o toast
                mToast.setText(getString(R.string.DestinationSet) + mDestination.getID());
                mToast.show();

                // Clear out our working destination object
                mDestination = null;

                // If the index is valid - set when the user long pressed
                if(mIndex >= 0) {

                    // Everything before this index is behind us
                	for(int i = 0; i < mIndex; i++) {
                        mService.getPlan().setPassed(i);
                    }

                	// And everything in front of us is not yet passed
                    for(int i = mIndex; i < mService.getPlan().getDestinationNumber(); i++) {
                        mService.getPlan().setNotPassed(i);
                    }
                }
                
                // Clear out our working index
                mIndex = -1;
                
                // Switch view to the chart/map tab
                ((MainActivity) PlanActivity.this.getParent()).showMapTab();
            }   
        });

        // Delete a waypoint from the plan detail list. The button is normally hidden
        // but comes up when you long press on a waypoint in the list.
        mDeleteButton = (Button)view.findViewById(R.id.plan_button_delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	// If the index is valid, then remove that line from the plan
                if(mIndex >= 0) {
                    removeFromPlanDetail(mIndex);
                }
                mIndex = -1;
            }   
        });
        
        // The PLATES button. Normally hidden, but comes into view when 
        // the user long presses on a waypoint in the plan detail list AND
        // that waypoint has plates associated with it.
        mPlatesButton = (Button)view.findViewById(R.id.plan_button_plates);
        mPlatesButton.setOnClickListener(new OnClickListener() {

        	// User has clicked the button. Tell the service about the ID that
        	// was selected then switch to the plates tab
            @Override
            public void onClick(View v) {
            	
            	// Ensure we have the background service
                if(mService != null) {
                	
                	// Tell it the ID of the plate
                	// mDestination is set on the long press action
                    mService.setLastPlateAirport(mDestination.getID());
                    mService.setLastPlateIndex(0);
                }
                
                // Switch to that tab now
                ((MainActivity) PlanActivity.this.getParent()).showPlatesTab();
            }   
        });        

        // The plan string text field
        mPlanStringText = (EditText)view.findViewById(R.id.plan_edit_text);
        
        // Create the plan from the mPlanStringText field content
        mCreatePlanFromString = (Button)view.findViewById(R.id.plan_button_find);
        mCreatePlanFromString.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            	// If we have waypoints, we are already in the process of searching the database
            	// for plan waypoints - just exit
                if(mSearchWaypoints != null) {
                    return;
                }
                
                // Get the manually entered waypoints
                final String planString = mPlanStringText.getText().toString().toUpperCase(Locale.getDefault());
                
                // Create a dialog box to allow the user to confirm or cancel this operation
                mDlgCreatePlan = new AlertDialog.Builder(PlanActivity.this).create();
                mDlgCreatePlan.setCanceledOnTouchOutside(false);
                mDlgCreatePlan.setCancelable(true);
                mDlgCreatePlan.setTitle(getString(R.string.Plan));
                String msg = getString(R.string.PlanWarning);
                if(planString.length() > 0) {
                	msg += getString(R.string.AndLoad) + " \"" + planString + "\"";
                }
                msg += "?";
                mDlgCreatePlan.setMessage(msg);

                // OK button. The use wishes to continue the action 
                mDlgCreatePlan.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                	// Clicked action.
                    public void onClick(DialogInterface dialog, int which) {
                    	// Display a progress bar
                        mProgressBar.setVisibility(View.VISIBLE);
                        
                        // Clear out current in memory plan
                        mService.getPlan().clear();
                        
                        // Fill out the plan detail area
                        preparePlanDetailAdapter();
                        
                        // Fill out the list of saved plans
                        preparePlanListAdapter();
                        
                        // Give the detail data to our adapter
                        mPlanDetail.setAdapter(mPlanDetailAdapter);

                        // Split up all possible waypoints that were entered
                        mSearchWaypoints = planString.split(" ");
                        
                        // Assume we will find all of the waypoints
                        mClearPlanStringText = true;
                        
                        // If we have no waypoints, the in-memory plan will be cleared.
                        // Also clear out the "plan name" and tell the external plan to shut off
                        // as well
                        if(0 == planString.length()) {
                        	// TODO: More internal/external plan handling cleanup
                        	mService.getExternalPlanMgr().setActive(mSaveText.getText().toString(), false);
                        	inactivatePlan();
                        	mService.setDestination(null);	// Clear out any destination also
                        	mSaveText.setText(null);
                        };
                        
                        // Start searching for them all 
                        searchForNextWaypoint();
                    }
                });
                
                // User does NOT want to clear current plan and start over
                mDlgCreatePlan.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                	// Clicked action.
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                // Display the dialog for the user to interact with
                mDlgCreatePlan.show();
                
                // Slide the input keyboard out of the way
                hideKeyboard();
            }
        });

        // Get the drawer control
        mPlanDrawer = (SlidingDrawer)view.findViewById(R.id.plan_drawer);
        
        // The SAVE functionality
        mSaveText   = (EditText)view.findViewById(R.id.plan_text_save);
        mSaveButton = (Button)view.findViewById(R.id.plan_button_save);
        mSaveButton.setOnClickListener(new OnClickListener() {

        	// User has pressed the save button. Save the in memory plan 
        	// to the preferences storage area. 
        	// TODO: Option to export externally as GPX/XML 
            @Override
            public void onClick(View v) {

                // Ensure we have a background service
                if(mService == null) {
                    return;
                }
                
                // Get the loaded plan. It's not necessarily active, just "in memory"
                // If we don't have one, then nothing to do
                Plan plan = mService.getPlan();
                if(plan == null) {
                    return;
                }
                
                // How many points are in this plan ? If zero or one, then nothing
                // to save
                int num = plan.getDestinationNumber();
                if(num < 2) {
                    return;
                }

                // Get the name that the user entered. Replace any commas with spaces
                String planName =  mSaveText.getText().toString().replace(",", " ");
                
                // If no name entered, then use the default name
                if(planName.equals("")) {
                    planName = getString(R.string.Plan);
                }
                
                // Build the plan string that we will use to save this to storage
                // [PlanName]::[DestID]([DestType])>[DestID]([DestType])
                // TODO: Knowledge of how this is stored is none of this objects business
                String planStr = planName + "::";

                // For each waypoint in the plan EXCEPT the final one
                for(int i = 0; i < (num - 1); i++) {
                	// Append "[DestID]([DestType])>"
                    planStr += plan.getDestination(i).getID() + "(" + plan.getDestination(i).getType() + ")" +  ">";
                }
                
                // Now add the final waypoint
                planStr += plan.getDestination(num - 1).getID() + "(" + plan.getDestination(num - 1).getType() + ")";
                
                // Tell preferences to save this off
                mPref.addToPlans(planStr);
                
                // Update the list of saved plans
                preparePlanListAdapter();
                
                // Clear the text namd field and slide the keyboard out of the way.
                mSaveText.setText(null);
                hideKeyboard();
            }   
        });

        // Handle the activation/deactivation of the current in memory plan
        mActivateButton = (ToggleButton)view.findViewById(R.id.plan_button_activate);
        mActivateButton.getBackground().setAlpha(255);
        mActivateButton.setOnClickListener(new OnClickListener() {

        	// User has pressed the button. The text of the button has already been 
        	// toggled at this point by the system
            @Override
            public void onClick(View v) {

            	// Ensure we have the background service
                if(null != mService) {
                	
                	// Read the name of the plan from the text field
                    String planName =  mSaveText.getText().toString().replace(",", " ");
                    
                    // TODO
                    // This needs improvement. Internal and external flight plans are 
                    // different kinds of ducks. They should both be handled in an identical fashion
                    if(true == mActivateButton.getText().equals(getString(R.string.Inactive))) {
                    	// The plan is currently ACTIVE. Make it inactive and clear out our destination
                        mService.getPlan().makeInactive();
                    	mService.getExternalPlanMgr().setActive(planName, false);
                    	mService.setDestination(null);
                    } else {
                    	// The plan is currently INACTIVE. Set the name, then start this plan
                    	mService.getExternalPlanMgr().setActive(planName, true);
                    	mService.getPlan().setExtPlanMgr(mService.getExternalPlanMgr());
                    	mService.getPlan().setName(planName);
                    	if(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()) != null) {
                            mService.setDestinationPlan(mService.getPlan().getDestination(mService.getPlan().findNextNotPassed()));
                        }
                    }
                }
            }
            
        });

        // Advance to the next waypoint of the in-memory plan
        mAdvanceButton = (Button)view.findViewById(R.id.plan_button_advance);
        mAdvanceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	// Service needs to be valid
            	if(null == mService) {
            		return;
            	}
            	
            	// Must have a plan on file
            	Plan plan = mService.getPlan();
            	if(null == plan) {
            		return;
            	}
            	
            	// Advance to the next waypoint in the plan
            	// Search each one to find the first point we have
            	// not yet passed. Mark that as passed and set destination to the one
            	// after.
            	int planSize = plan.getDestinationNumber();
            	for(int idx = 0; idx < planSize; idx++) {
            		if(false == plan.isPassed(idx)){
            			plan.setPassed(idx);
            			if(idx < (planSize - 1)) {
            				Destination dest = plan.getDestination(idx + 1);
            				mService.setDestinationPlan(dest);
            				updatePlanDetailAdapter();
            				return;
            			}
            		}
            	}
            	
            	// We are at the end of our waypoint list - nothing to
            	// advance toward, so just cancel the plan
            	inactivatePlan();
            	mService.setDestination(null);
            }
        });

        // Regress to the previous waypoint of the in-memory plan
        mRegressButton = (Button)view.findViewById(R.id.plan_button_regress);
        mRegressButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            	// We need the background service
            	if(null == mService) {
            		return;
            	}
            	
            	// And an in-memory plan
            	Plan plan = mService.getPlan();
            	if(null == plan) {
            		return;
            	}

            	// Regress to the PREVIOUS waypoint in the plan
            	// We do this by searching from the end of the plan
            	// to find the first waypoint that is marked as passed and changing
            	// it to NOT passed, then setting a new destination
            	int planSize = plan.getDestinationNumber();
            	for(int idx = planSize - 1; idx >= 0 ; idx--) {
            		if(true == plan.isPassed(idx)){
            			plan.setNotPassed(idx);
            			if(idx >= 0) {
            				Destination dest = plan.getDestination(idx);
            				mService.setDestinationPlan(dest);
            				updatePlanDetailAdapter();
            				return;
            			}
            		}
            	}
            	
            	// We are at the end of our waypoint list - nothing to
            	// advance toward, so just cancel the plan
            	inactivatePlan();
            	mService.setDestination(null);
            }
        });

        // Create our 3 animated buttons that slide in from the left when a user long 
        // presses any waypoint in the plan detail list
        mAnimateDest   = new AnimateButton(getApplicationContext(), mDestButton,   AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateDelete = new AnimateButton(getApplicationContext(), mDeleteButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimatePlates = new AnimateButton(getApplicationContext(), mPlatesButton, AnimateButton.DIRECTION_L_R, (View[])null);
    }

    /**
     * Look for the waypoints in the mSearchWaypoints array. Find
     * the first non-null entry and tell the background thread to look for 
     * it. If all of the waypoints have been nulled out, then we are done
     * and we have a plan ready to go.
     */
    private void searchForNextWaypoint() {
    	
    	// If no waypoint list, then just get out of here
    	if(null == mSearchWaypoints) {
    		return;
    	}

    	// If the size of our list shows it is empty
    	if(0 == mSearchWaypoints.length) {
    		return;
    	}

    	// Used to index into our list
        int wpIdx = 0;
        
        // Find the first non null waypoint in the array
        for(wpIdx = 0; wpIdx < mSearchWaypoints.length; wpIdx++) {
        	
        	// Skip over a NULL string
            if(mSearchWaypoints[wpIdx] == null) {
                continue;
            }
            
            // Skip over a zero length string
            if(mSearchWaypoints[wpIdx].length() == 0) {
                mSearchWaypoints[wpIdx] = null;
                continue;
            }
            
            // Give this to the background search logic to find and load
            planToWithVerify(mSearchWaypoints[wpIdx]);
            break;
        }

        // If we are not at the end of the list yet, then just
        // return. There will be a search result waiting in the future
        if(wpIdx != mSearchWaypoints.length) {
        	return;
        }

        // Set the plan detail information according to the plan
        // we just parsed
        preparePlanDetailAdapter();
        mPlanDetail.setAdapter(mPlanDetailAdapter);

        // Turn off the progress bar
        mProgressBar.setVisibility(View.INVISIBLE);

        // If all the waypoints were found, then clear out the plan string text
        if(true == mClearPlanStringText) {
        	mPlanStringText.setText(null);
        }
    
        // Now clear out our working collection of search waypoints
        mSearchWaypoints = null;
    }
    
	/***
	 * Update the data that gets displayed in the plan detail area.
	 * There are 3 pieces of data for each waypoint in the plan:
	 * 1) Name of Waypoint
	 * 2) Type of waypoint
	 * 3) Bearing and distance to waypoint
	 */
    private void updatePlanDetailAdapter() {

    	// If we don't have a reference back to the adapter then
    	// do nothing
    	if(null == mPlanDetailAdapter) {
            return;
        }
    	
    	// How many waypoints make up the in memory plan
        int destnum = mService.getPlan().getDestinationNumber();
        
        // New arrays to hold all of our line data
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> info = new ArrayList<String>();
        ArrayList<Boolean> passed = new ArrayList<Boolean>();

        // Loop through each of our plan waypoints to add its data
        for(int id = 0; id < destnum; id++) {
            name.add(mService.getPlan().getDestination(id).getID() + "(" + mService.getPlan().getDestination(id).getType() + ")");
            info.add(mService.getPlan().getDestination(id).toString());
            passed.add(mService.getPlan().isPassed(id));
        }
        
        // Give the adapter this new data
        mPlanDetailAdapter.updateList(name, info, passed);
        
        // Set the top summary line
        mPlanDetailSummary.setText(getString(R.string.Total) + " " + mService.getPlan().toString());
    }


    /***
     * Build a list of plans that are displayed in the SavedPlans selection list
     */
    private void preparePlanListAdapter() {
        
    	// Allocate a new string array
        ArrayList<String> planList = new ArrayList<String>();
        
        // Refresh our internal collection of plans
        refreshAllPlans();
        
        // For each plan we know about, extract its name
        // and add it to the planList
        for (int i = 0; i < mAllPlans.length; i++) {
            if(mAllPlans[i].equals("")) {
                continue;
            }
            String[] split = mAllPlans[i].split("::");
            if(split.length < 2) {
                continue;
            }
            planList.add(split[0]);
        }
        
        // Allocate a new ArrayAdapter initialized with our plan list
        mPlanListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, planList);
        
        // Set our adapter to be this newly allocated one
        mSavedPlanList.setAdapter(mPlanListAdapter);
    }

	/***
	 * Prepare the detailed plan display for initial use.
	 * There are 3 pieces of data for each waypoint in the plan:
	 * 1) Name of Waypoint
	 * 2) Type of waypoint
	 * 3) Bearing and distance to waypoint
	 */
    private void preparePlanDetailAdapter() {
    	// We need the service defined for this to work
    	if(null == mService) {
    		return;
    	}
    	
    	// How many waypoints are in this plan
    	Plan plan = mService.getPlan();
        int destnum = plan.getDestinationNumber();
        
        // Allocate storage for each piece of data in the waypoints
        ArrayList<String>  name   = new ArrayList<String>();
        ArrayList<String>  info   = new ArrayList<String>();
        ArrayList<Boolean> passed = new ArrayList<Boolean>();

        // For each waypoint, build up its line data
        for(int id = 0; id < destnum; id++) {
        	Destination dest = plan.getDestination(id);
            name.add(dest.getID() + "(" + dest.getType() + ")");
            info.add(dest.toString());
            passed.add(plan.isPassed(id));
        }

        // Pass all of this data off to a new PlanAdapter
        mPlanDetailAdapter = new PlanAdapter(PlanActivity.this, name, info, passed);
    }


    /***
     * Add a new waypoint to the end of the active plan
     * @param dst - destination ID
     * @param type - type of waypoint
     */
    private void planTo(String dst, String type) {
        Destination dest = new Destination(dst, type, mPref, mService);
        mService.getPlan().appendDestination(dest);
        dest.find();
    }

	/***
	 * Search for the indicated destination. Add THIS class as the observer
	 * to get the search results.
	 * @param dst - Destination Identifier
	 */
    private void planToWithVerify(String dst) {
    	// Create the destination
        Destination dest = new Destination(dst, "", mPref, mService);
        dest.addObserver(this);
        dest.findGuessType();
    }

	/***
	 * Read both internal and external plans and place them
	 * in a single string array.
	 */
    private void refreshAllPlans()
    {
        String intPlans[] = mPref.getPlans();
        String extPlans[] = mService.getExternalPlanMgr().getPlans();

        int planCount = 0;
        if(null != intPlans) { planCount += intPlans.length; }
        if(null != extPlans) { planCount += extPlans.length; }
        mAllPlans = new String[planCount];
        
        int planIdx = 0;
        if(intPlans != null) {
            for(int idx = 0, max = intPlans.length; idx < max; idx++) {
            	mAllPlans[planIdx++] = intPlans[idx];
            }
        }
        if(null != extPlans) {
            for(int idx = 0, max = extPlans.length; idx < max; idx++) {
            	mAllPlans[planIdx++] = extPlans[idx];
            }
        }
    }
    
    /** 
     * Defines callbacks for service binding, passed to bindService() 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

    	// We've received a connection to the service. Time to get the 
    	// ball rolling
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	 
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();

            // Register our GPS object with the base service
            mService.registerGpsListener(mGpsInfc);

            // Refresh our list of all the plans
            refreshAllPlans();
            
            // Ready the plan details
            preparePlanDetailAdapter();
            
            // Set up some properties of the plan details
            mPlanDetail.setAdapter(mPlanDetailAdapter);
            mPlanDetail.setClickable(true);
            mPlanDetail.setDividerHeight(10);

            // Ready the entire list of plans
            preparePlanListAdapter();

            // Long press on a waypoint in the plan detail list
            mPlanDetail.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
                	
                	// Index of the item that was long pressed
                    mIndex = index;

                    // Get the destination object based on this index
                    mDestination = mService.getPlan().getDestination(index);
                    String destID = mDestination.getID();
                    
                    // Set the text of our animated button to be the Navaid ID
                    mDestButton.setText(destID);
                    
                    // Turn on the DESTINATION button
                    mAnimateDest.animate(true);
                    
                    // Turn on the DELETE button
                    mAnimateDelete.animate(true);
                    
                    // If the destination has plates, then turn on the PLATE button
                    if(PlatesActivity.doesAirportHavePlates(mPref.mapsFolder(), destID)) {
                    	mAnimatePlates.animate(true);
                    } else {
                    	mAnimatePlates.stopAndHide();
                    }

                    // Done
                    return true;
                }
            }); 

            // Some properties of the list of saved plans
            mSavedPlanList.setClickable(true);
            mSavedPlanList.setDividerHeight(10);
            
            /***
             * Long press on the list of all saved plans
             */
            mSavedPlanList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
                	
                	// Which item in the list was pressed
                    final int mxindex = index;
                    
                    // Ensure the list index is valid
                    if(mxindex < 0 || mxindex >= mAllPlans.length) {
                        return true;
                    }

                    // Ensure the plan detail string that we have
                    // at that index is formatted properly
                    // TODO: We shouldn't be looking at the plan formatting
                    final String item = mAllPlans[mxindex];
                    final String items[] = item.split("::");
                    if(items.length < 2) { 
                        return true;
                    }

                    // Build a dialog to let the user load/reverse load/delete the selected 
                    // plan
                    mDlgLoadOrDelete = new AlertDialog.Builder(PlanActivity.this).create();
                    mDlgLoadOrDelete.setCanceledOnTouchOutside(false);
                    mDlgLoadOrDelete.setCancelable(true);
                    mDlgLoadOrDelete.setTitle(getString(R.string.Plan) + ": " + items[0]);
                    
                    // Leftmost button - LOAD
                    mDlgLoadOrDelete.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Load), 
                    		new DialogInterface.OnClickListener() {

                    	// Click action will load the plan
                        public void onClick(DialogInterface dialog, int which) {
                            inactivatePlan();
                            mService.newPlan();

                            // TODO: Parsing the plan is too much to know about
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

                            preparePlanDetailAdapter();
                            mPlanDetail.setAdapter(mPlanDetailAdapter);
                            mPlanDrawer.animateClose();
                        }
                    });
                    
                    // Middle button - REVERSE LOAD
                    mDlgLoadOrDelete.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.LoadReverse), 
                    		new DialogInterface.OnClickListener() {

                    	// Click will load the plan, then reverse all the waypoints
                        public void onClick(DialogInterface dialog, int which) {
                            inactivatePlan();
                            mService.newPlan();

                            // TODO: Parsing the plan is too much to know about
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

                            preparePlanDetailAdapter();
                            mPlanDetail.setAdapter(mPlanDetailAdapter);            
                            mPlanDrawer.animateClose();
                        }
                    });
                    
                    // Right button - DELETE the plan
                    mDlgLoadOrDelete.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Delete), 
                    		new DialogInterface.OnClickListener() {

                    	// Click will try and remove the plan from storage
                        public void onClick(DialogInterface dialog, int which) {
                            // Try deleting the plan from external storage first. If that fails
                            // then tell the preferences to delete it.
                            if(false == mService.getExternalPlanMgr().delete(items[0])) { 
                            	mPref.deleteAPlan(mAllPlans[mxindex]);
                            }
                            preparePlanListAdapter();
                        }
                    });

                    // Display the dialog to the user for action
                    mDlgLoadOrDelete.show();

                    // All done, time to leave
                    return true;
                }
            }); 

             // Set proper state of the active button.
             // Plan only active when more than one dest.
            if((!mService.getPlan().isActive()) || (mService.getPlan().getDestinationNumber() <= 0)) {
                mActivateButton.setChecked(false);                
            } else {
                mActivateButton.setChecked(true);
            }
        }    

        // We have been disconnected from the service
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    // We are being paused. Most likely switching to a different tab
    // Ensure all dialogs are removed and remove our GPS listener
    @Override
    protected void onPause() {
    	// Call superclass onPause
    	super.onPause();
        
    	// If we have the underlying service, then remove our GPS
    	// listener from it
        if(null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        // If we are in the middle of a LOAD/DELETE action, then dismiss it
        if(null != mDlgLoadOrDelete) {
            try {
                mDlgLoadOrDelete.dismiss();
            }
            catch (Exception e) {
            }
        }

        // Are we prompting to load the plan whose name was typed in ?
        if(null != mDlgCreatePlan) {
            try {
                mDlgCreatePlan.dismiss();
            }
            catch (Exception e) {
            }
        }

         // Clean up on pause that was started in on resume
        getApplicationContext().unbindService(mConnection);

        // Cancel the timer if one is running
        if(mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     * We resuming operation. 
     */
    @Override
    public void onResume() {
 
    	// Let the super class do its work
        super.onResume();
        
        // Restore some common display elements
        Helper.setOrientationAndOn(this);
        
        // Re-bind the connection to the storage service
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        // Create sim timer
        mTimer = new Timer();
        TimerTask sim = new UpdateTask();
        mTimer.scheduleAtFixedRate(sim, 0, 1000);
    }

    /**
     * We are being destroyed. Called after onPause when we need to go away 
     */
    @Override
    public void onDestroy() {
    	// Tell super class to do its thing
        super.onDestroy();

    }

    /**
     * An update event needs to be processed. This will come as a result of adding this
     * class as an observer to a destination object with planToWithVerify(); 
     */
    @Override
    public void update(Observable arg0, Object arg1) {

    	// Extract the destination given to us. If there isn't one, then
    	// nothing else to do
        Destination dest = (Destination)arg0;
        if(null == dest) {
            return;
        }

        // Do we have a collection of waypoints we are searching for ?
        if(null == mSearchWaypoints) {
            return;
        }
        
        // Walk through all of the waypoints in our search list
        // The mWaypoints array of strings gets filled earlier 
        // and nulled out here one at a time when results are found
        for(int wpIdx = 0; wpIdx < mSearchWaypoints.length; wpIdx++) {

        	// If there is an entry here
        	if(null != mSearchWaypoints[wpIdx]) {
        		
        		// Is this a valid waypoint ? ie: did we find it
                if(dest.isFound()) {

                	// Append this waypoint to the end of the in memory plan
                    mService.getPlan().appendDestination(dest);
                } else {
                	
                	// Not found - display some toast to indicate such
                	mClearPlanStringText = false;
                    mToast.setText(getString(R.string.PlanDestinationNF));
                    mToast.show();
                }
                
                // NULL out the current waypoint index
                mSearchWaypoints[wpIdx] = null;
                break;
            }
        }
        
        // Start the search for the next waypoint in the collection
        searchForNextWaypoint();        
    }
    
    /***
     * A background timer class to send off messages if we are in simulation mode
     * @author zkhan
     */
    private class UpdateTask extends TimerTask {

    	// Called whenever the timer fires.
    	public void run() {
        	// If we are in sim mode, then send a message
            if(mPref.isSimulationMode()) {
                mHandler.sendEmptyMessage(0);
            }
        }
    }
    
    /**
	 * Declare a new message handler that will receive items from the UpdateTask
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private Handler mHandler = new Handler() {

    	// Single method to handle the inbound message
    	@Override
        public void handleMessage(Message msg) {
    		
    		// If the background service is defined, then send off a simulation
    		// message to the active plan. This might change the current GPS position
            if(mService != null) {
                mService.getPlan().simulate();
                
                // Update the plan detail list
                updatePlanDetailAdapter();
            }
        }
    };

    /***
     * Helper function to slide the keyboard out of the way
     */
    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
