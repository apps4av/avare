/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.fragment;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.adapters.SearchAdapter;
import com.ds.avare.animation.AnimateButton;
import com.ds.avare.place.Destination;
import com.ds.avare.place.DestinationFactory;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;

import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author zkhan
 *
 */
public class SearchFragment extends StorageServiceGpsListenerFragment implements Observer {

    public static final String TAG = "SearchFragment";

    private ListView mSearchListView;
    private EditText mSearchText;
    private SearchAdapter mAdapter;
    private SearchTask mSearchTask;
    private ProgressBar mProgressBar;
    private String mSelected;
    private Button mSelectedButton;
    private Button mEditButton;
    private Button mPlanButton;
    private Button mPlatesButton;
    private boolean mIsWaypoint;

    private AnimateButton mAnimatePlates;
    private AnimateButton mAnimatePlan;
    private AnimateButton mAnimateSelect;
    private AnimateButton mAnimateEdit;

    /**
     * Shows edit dialog
     */
    private AlertDialog mAlertDialogEdit;


    /**
     * Current destination info
     */
    private Destination mDestination;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsWaypoint = false;
        // Lose info
        mSelected = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        /*
         * For a search query
         */
        mSearchListView = (ListView) view.findViewById(R.id.search_list_view);

        /*
         * Progress bar
         */
        mProgressBar = (ProgressBar) (view.findViewById(R.id.search_progress_bar));

        mSelectedButton = (Button) view.findViewById(R.id.search_button_delete);
        mSelectedButton.getBackground().setAlpha(255);
        mSelectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mSelected) {
                    mPref.deleteARecent(mSelected);
                    initList();
                    mSearchText.setText("");
                }
                mSelected = null;
                hideMenu();
            }
        });

        mEditButton = (Button) view.findViewById(R.id.search_button_note);
        mEditButton.getBackground().setAlpha(255);
        mEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mSelected) {
                    final EditText edit = new EditText(getContext());
                    String type = StringPreference.parseHashedNameDbType(mSelected);
                    if(type == null) {
                        showSnackbar(getString(R.string.GpsOnly), Snackbar.LENGTH_SHORT);
                        return;
                    }
                    if(!type.equals(Destination.GPS)) {
                        showSnackbar(getString(R.string.GpsOnly), Snackbar.LENGTH_SHORT);
                        return;
                    }

                    edit.setText(StringPreference.parseHashedNameIdBefore(mSelected));

                    mAlertDialogEdit = new DecoratedAlertDialogBuilder(getContext()).create();
                    mAlertDialogEdit.setTitle(getString(R.string.Label));
                    mAlertDialogEdit.setCanceledOnTouchOutside(true);
                    mAlertDialogEdit.setCancelable(true);
                    mAlertDialogEdit.setView(edit);
                    mAlertDialogEdit.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            /*
                             * Edit and save description field
                             */

                            mPref.modifyARecent(mSelected, edit.getText().toString().toUpperCase());
                            initList();
                            mSelected = null;
                            dialog.dismiss();

                        }
                    });
                    mAlertDialogEdit.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mSelected = null;
                            dialog.dismiss();
                        }
                    });

                    mAlertDialogEdit.show();
                }
            }

        });

        mPlanButton = (Button) view.findViewById(R.id.search_button_plan);
        mPlanButton.getBackground().setAlpha(255);
        mPlanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mSelected) {
                    String id = StringPreference.parseHashedNameId(mSelected);
                    String destType = StringPreference.parseHashedNameDestType(mSelected);
                    String dbType = StringPreference.parseHashedNameDbType(mSelected);
                    if(id == null || destType == null) {
                        return;
                    }
                    // It's ok if dbType is null
                    planTo(id, destType, dbType);
                }
            }
        });

        mPlatesButton = (Button) view.findViewById(R.id.search_button_plates);
        mPlatesButton.getBackground().setAlpha(255);
        mPlatesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mSelected) {
                    String id = StringPreference.parseHashedNameId(mSelected);
                    if(id == null) {
                        return;
                    }

                    if(mService != null) {
                        mService.setLastPlateAirport(id);
                        mService.setLastPlateIndex(0);
                        ((MainActivity) getContext()).showPlatesViewAndCenter();
                    }
                }
            }
        });


        /*
         * Set on click
         */
        mSearchListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                /*
                 * Commas not allowed
                 */
                String txt = mAdapter.getItem(position).replace(",", " ");
                String id = StringPreference.parseHashedNameId(txt);
                String destType = StringPreference.parseHashedNameDestType(txt);
                String dbType = StringPreference.parseHashedNameDbType(txt);
                if(id == null || destType == null) {
                    return;
                }
                // It's ok if dbType is null
                goTo(id, destType, dbType);
            }
        });

        mSearchListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3) {
                mSelected = mAdapter.getItem(index);
                if(mSelected == null) {
                    return false;
                }

                // Don't display the plates button if there are no plates
                String id = StringPreference.parseHashedNameId(mSelected);

                if (PlatesFragment.doesAirportHavePlates(mPref.mapsFolder(), id)) {
                    mAnimatePlates.animate(true);
                }
                else {
                    mAnimatePlates.stopAndHide();
                }

                mAnimateSelect.animate(true);
                mAnimatePlan.animate(true);

                // Don't display the edit button if we can't edit
                String type = StringPreference.parseHashedNameDbType(mSelected);
                if(type == null || !type.equals(Destination.GPS)) {
                    mAnimateEdit.stopAndHide();
                }
                else {
                    mAnimateEdit.animate(true);
                }

                return true;
            }
        });


        /*
         * For searching, start search on every new key press
         */
        mSearchText = (EditText) view.findViewById(R.id.search_edit_text);
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

                if(s.toString().startsWith("address,")) {
                    String [] vals = new String[1];
                    String addr = s.toString().substring(8); // 8 = length of "address,"
                    if(addr.length() > 1) {
                        StringPreference sp = new StringPreference(Destination.MAPS, Destination.MAPS, Destination.MAPS, addr);
                        vals[0] = sp.getHashedName();
                        mAdapter = new SearchAdapter(getContext(), vals);
                        mSearchListView.setAdapter(mAdapter);
                    }
                    return;
                }

                /*
                 * This is a geo coordinate with &?
                 */
                if(s.toString().contains("&")) {
                    String [] vals = new String[1];
                    StringPreference sp = new StringPreference(Destination.GPS, Destination.GPS, Destination.GPS, s.toString());
                    vals[0] = sp.getHashedName();
                    mAdapter = new SearchAdapter(getContext(), vals);
                    mSearchListView.setAdapter(mAdapter);
                    return;
                }
                mProgressBar.setVisibility(ProgressBar.VISIBLE);

                mSearchTask = new SearchTask();
                mSearchTask.execute(s.toString());

            }
        });

        mAnimatePlates = new AnimateButton(getContext(), mPlatesButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimatePlan = new AnimateButton(getContext(), mPlanButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateSelect = new AnimateButton(getContext(), mSelectedButton, AnimateButton.DIRECTION_L_R, (View[])null);
        mAnimateEdit = new AnimateButton(getContext(), mEditButton, AnimateButton.DIRECTION_L_R, (View[])null);
    }

    /**
     *
     */
    private void hideMenu() {
        mAnimatePlan.stopAndHide();
        mAnimatePlates.stopAndHide();
        mAnimateSelect.stopAndHide();
        mAnimateEdit.stopAndHide();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSearchText != null) {
            mSearchText.setText("");
        }

        try { mAlertDialogEdit.dismiss(); }
        catch (Exception e) { }
    }

    @Override
    protected void postServiceConnected() {
        initList();
    }

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
                    showSnackbar(getString(R.string.DestinationNF), Snackbar.LENGTH_SHORT);
                    return;
                }
                if((Destination)arg0 != mDestination) {
                    /*
                     * If user presses a selection repeatedly, reject previous
                     */
                    return;
                }
                mPref.addToRecent(mDestination.getStorageName());

                if(!mIsWaypoint) {
                    if(mService != null) {
                        mService.setDestination((Destination)arg0);
                    }
                    showSnackbar(getString(R.string.DestinationSet) + ((Destination)arg0).getID(), Snackbar.LENGTH_SHORT);
                    ((MainActivity) getContext()).showMapView();
                }
                else {
                    if(mService != null) {
                        if(mService.getPlan().appendDestination((Destination)arg0)) {
                            showSnackbar(((Destination)arg0).getID() + getString(R.string.PlanSet), Snackbar.LENGTH_SHORT);
                        }
                        else {
                            showSnackbar(((Destination)arg0).getID() + getString(R.string.PlanNoset), Snackbar.LENGTH_SHORT);
                        }
                    }
                }
            }
            else {
                showSnackbar(getString(R.string.DestinationNF), Snackbar.LENGTH_SHORT);
            }
        }
    }

    /**
     *
     * @param dst
     */
    private void goTo(String dst, String type, String dbType) {
        mIsWaypoint = false;
        mDestination = DestinationFactory.build(mService, dst, type);
        mDestination.addObserver(SearchFragment.this);
        showSnackbar(getString(R.string.Searching) + " " + dst, Snackbar.LENGTH_SHORT);
        mDestination.find(dbType);
        mSearchText.setText("");
    }

    /**
     *
     * @param dst
     */
    private void planTo(String dst, String type, String dbType) {
        mIsWaypoint = true;
        mDestination = DestinationFactory.build(mService, dst, type);
        mDestination.addObserver(SearchFragment.this);
        showSnackbar(getString(R.string.Searching) + " " + dst, Snackbar.LENGTH_SHORT);
        mDestination.find(dbType);
        mSearchText.setText("");
    }

    private void initList() {
        String [] vals = mPref.getRecent();
        mAdapter = new SearchAdapter(getContext(), vals);
        mSearchListView.setAdapter(mAdapter);
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

            Thread.currentThread().setName("Search");

            String srch = (String)vals[0];
            if(null == mService) {
                return false;
            }

            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
            synchronized (SearchFragment.class) {
                /*
                 * This is not to be done repeatedly with new text input so sync.
                 */
                mService.getDBResource().search(srch, params, false);
                mService.getUDWMgr().search(srch, params);			// From user defined points of interest
                if(params.size() > 0) {
                    selection = new String[params.size()];
                    int iterator = 0;
                    for(String key : params.keySet()){
                        selection[iterator] = StringPreference.getHashedName(params.get(key), key);
                        iterator++;
                    }
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
            mAdapter = new SearchAdapter(getContext(), selection);
            mSearchListView.setAdapter(mAdapter);
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

}
