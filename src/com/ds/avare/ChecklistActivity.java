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



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

import com.ds.avare.animation.AnimateButton;
import com.ds.avare.flight.Checklist;
import com.ds.avare.gps.GpsInterface;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author zkhan
 * An activity that deals with check lists
 */
public class ChecklistActivity extends Activity {
    
    private StorageService mService;
    private TouchListView mList;
    private ChecklistAdapter mListAdapter;
    private ListView mListSave;
    private ArrayAdapter<String> mListSaveAdapter;
    private Toast mToast;
    private Button mSaveButton;
    private Button mImportButton;
    private Button mDeleteButton;
    private Button mListButton;
    private EditText mSaveText;
    private ToggleButton mLock;
    private int mIndex;
    private int mIndexSave;
    private Preferences mPref;
    private Checklist mWorkingList;
    private int mWorkingIndex;
    private AlertDialog mAlertDialogChoose;
    private AlertDialog mAlertDialogEnter;

    private static final int MAX_FILE_LINE_SIZE = 256;
    private static final int MAX_FILE_LINES = 100;
    
    private AnimateButton mAnimateDelete;


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
    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if(mLock.isChecked()) {
                /*
                 * Do not modify list if locked
                 */
                mWorkingIndex = 0;
                mWorkingList.moveStep(from, to);
                // Delay till this function returns
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                      prepareAdapter();                
                  }
                }, 100);
            }
        }
    };

    
    /**
     * 
     */
    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            if(mLock.isChecked()) {
                mWorkingIndex = 0;
                mWorkingList.removeStep(which);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                      prepareAdapter();                
                  }
                }, 100);
            }
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
                      
        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.checklist, null);
        setContentView(view);
        
        mList = (TouchListView)view.findViewById(R.id.checklist_list);
        mList.setDropListener(onDrop);
        mList.setRemoveListener(onRemove);
        mListSave = (ListView)view.findViewById(R.id.checklist_list_save);

        mService = null;
        mIndex = -1;
        mIndexSave = -1;

        mWorkingList = new Checklist("");
        mWorkingIndex = 0;
        
        /*
         * Edit lock button
         */
        mLock = (ToggleButton)view.findViewById(R.id.checklist_button_lock);
        
        /*
         * Delete button
         */
        mDeleteButton = (Button)view.findViewById(R.id.checklist_button_delete);
        mDeleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * Delete on saved lists
                 */
                if(mIndexSave >= 0) {
                    if(mService == null) {
                        return;
                    }
                    
                    final LinkedList<Checklist> lists = mService.getCheckLists();
                    if(lists == null) {
                        return;
                    }
                    
                    if(mIndexSave < 0 || mIndexSave >= lists.size()) {
                        mIndexSave = -1;
                        return;
                    }
                    
                    // Give confirm because lists are valuable
                    mAlertDialogChoose = new AlertDialog.Builder(ChecklistActivity.this).create();
                    mAlertDialogChoose.setCanceledOnTouchOutside(false);
                    mAlertDialogChoose.setCancelable(true);
                    mAlertDialogChoose.setTitle(lists.get(mIndexSave).getName() + ": " + getString(R.string.DeleteListQuestion));
                    mAlertDialogChoose.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Yes), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            lists.remove(mIndexSave);
                            
                            mIndexSave = -1;
                            prepareAdapterSave();
                            dialog.dismiss();
                        }
                    });
                    mAlertDialogChoose.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.No), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            mIndexSave = -1;
                            dialog.dismiss();
                        }
                    });

                    mAlertDialogChoose.show();
                    
                }
                else if(mIndex >= 0) {
                    /*
                     * Delete on step, but do not delete both as delete button is used for both
                     */
                    mWorkingList.removeStep(mIndex);
                    mWorkingIndex = 0;
                    prepareAdapter();
                }
                
                /*
                 * Always clear index
                 */
                mIndex = -1;
            }   
        });
        

        /*
         * List button
         */
        mListButton = (Button)view.findViewById(R.id.checklist_button_insert);
        mListButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final EditText eText = new EditText(ChecklistActivity.this);
                
                // Give confirm for add a step
                mAlertDialogEnter = new AlertDialog.Builder(ChecklistActivity.this).create();
                mAlertDialogEnter.setCanceledOnTouchOutside(false);
                mAlertDialogEnter.setCancelable(true);
                mAlertDialogEnter.setView(eText);
                mAlertDialogEnter.setTitle(getString(R.string.AddAction));
                mAlertDialogEnter.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        
                        String txt = eText.getText().toString();
                        if(txt.equals("")) {
                            mToast.setText(getString(R.string.InvalidText));
                            mToast.show();
                            return;
                        }
                        mWorkingIndex = 0;
                        mWorkingList.addStep(txt);
                        prepareAdapter();

                        dialog.dismiss();
                    }
                });
                mAlertDialogEnter.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mAlertDialogEnter.show();
            }   
        });


        /*
         * List button
         */
        mImportButton = (Button)view.findViewById(R.id.checklist_button_import);
        mImportButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final EditText eText = new EditText(ChecklistActivity.this);
                eText.setHint(getString(R.string.ImportActionFilePath));
                // Give confirm for add a step
                mAlertDialogEnter = new AlertDialog.Builder(ChecklistActivity.this).create();
                mAlertDialogEnter.setCanceledOnTouchOutside(false);
                mAlertDialogEnter.setCancelable(true);
                mAlertDialogEnter.setView(eText);
                mAlertDialogEnter.setTitle(getString(R.string.ImportAction));
                mAlertDialogEnter.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        
                        /*
                         * Get from file
                         */
                        mWorkingList = new Checklist("");
                        mWorkingIndex = 0;
                        String txt = eText.getText().toString();
                        new FileOperationTask().execute(txt);
                        dialog.dismiss();
                    }
                });
                mAlertDialogEnter.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    /* (non-Javadoc)
                     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                     */
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mAlertDialogEnter.show();
            }   
        });

        /*
         * Save button
         */
        mSaveText = (EditText)view.findViewById(R.id.checklist_text_save);
        mSaveButton = (Button)view.findViewById(R.id.checklist_button_save);
        mSaveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Get List, make its string, then send to storage
                 */
                if(mService == null) {
                    return;
                }
                
                String name = mSaveText.getText().toString();
                if(name.equals("")) {
                    return;
                }
                
                mWorkingList.changeName(name);
                
                mService.getCheckLists().add(mWorkingList);
                
                // Saved. Now show message
                mToast.setText(getString(R.string.SavedChecklist));
                mToast.show();
                
                prepareAdapterSave();
            }
        });
        
        mAnimateDelete = new AnimateButton(getApplicationContext(), mDeleteButton, AnimateButton.DIRECTION_L_R, (View[])null);

    }

    /**
     * 
     */
    private boolean prepareAdapter() {
        
        ArrayList<String> info = new ArrayList<String>();
        String steps[] = mWorkingList.getStepsArray();
        if(steps == null) {
            return false;
        }
        for(int i = 0; i < steps.length; i++) {
            info.add(steps[i]);
        }
        
        mListAdapter = new ChecklistAdapter(this, info);
        
        mList.setAdapter(mListAdapter);

        mListAdapter.setChecked(mWorkingIndex);
        mListAdapter.notifyDataSetChanged();
        return true;
    }


    /**
     * 
     */
    private boolean prepareAdapterSave() {
        
        if(mService == null) {
            return false;
        }
        ArrayList<String> list = new ArrayList<String>();
        LinkedList<Checklist> lists = mService.getCheckLists();
        for (Checklist cl : lists) {
            list.add(cl.getName());
        }
        
        mListSaveAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list);
        
        mListSave.setAdapter(mListSaveAdapter);

        /*
         * Save to storage on save button
         */
        mPref.putLists(Checklist.putCheckListsToStorageFormat(lists));
        
        /*
         * Make a new working list since last one stored already 
         */
        mWorkingList = new Checklist(mWorkingList.getName(), mWorkingList.getSteps());

        
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
            mService.setCheckLists(Checklist.getCheckListsFromStorageFromat(mPref.getLists()));

            prepareAdapter();
            prepareAdapterSave();
            
            mList.setClickable(true);
            mList.setDividerHeight(10);
            mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
              
                    if(mLock.isChecked()) {

                        // If list lock do not delete animate
                        mIndex = index;
                        mAnimateDelete.animate(true);
                    }
                    return true;
                }
            }); 
            
            /*
             * Click for keeping track of step on which we are
             */
            mList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    mWorkingIndex = position;
                    mListAdapter.setChecked(mWorkingIndex);
                }
            }); 

            mListSave.setClickable(true);
            mListSave.setDividerHeight(10);
            mListSave.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    if(mService == null) {
                        return;
                    }
                    
                    LinkedList<Checklist> lists = mService.getCheckLists();
                    if(lists == null) {
                        return;
                    }
                    
                    if(position < 0 || position >= lists.size()) {
                        return;
                    }

                    mWorkingList = new Checklist(lists.get(position).getName(), 
                            lists.get(position).getSteps());
                    
                    mWorkingIndex = 0;
                    mSaveText.setText(mWorkingList.getName());
                    prepareAdapter();
                                
                }
            }); 

            mListSave.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View v,
                        int index, long arg3) {
                    mIndexSave = index;
                            
                    mAnimateDelete.animate(true);
                    return true;
                }
            }); 

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
        
        if(null != mAlertDialogChoose) {
            try {
                mAlertDialogChoose.dismiss();
            }
            catch (Exception e) {
            }
        }
        if(null != mAlertDialogEnter) {
            try {
                mAlertDialogEnter.dismiss();
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
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
    }

    /**
     * 
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    /**
     * Read / import a file in check list
     * @author zkhan
     *
     */
    private class FileOperationTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
 
            String txt = params[0];
            
            try {
                InputStream instream = new FileInputStream(txt);
                
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                String line;
                int i = 0;
                do {
                    line = buffreader.readLine();
                    /*
                     * Do not crash if user screws up input
                     */
                    if(i > MAX_FILE_LINES || line.length() > MAX_FILE_LINE_SIZE) {
                        break;
                    }
                    i++;
                    publishProgress(line);
                } while (line != null);
            } 
            catch (Exception e) {
                return null;
            }
            
            // dummy
            return txt;
        }

        @Override
        protected void onPostExecute(String result) {
            if(null == result) {
                mToast.setText(getString(R.string.InvalidText));
                mToast.show();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if(values == null) {
                return;
            }
            if(values[0] == null) {
                return;
            }
            mWorkingList.addStep(values[0]);
            prepareAdapter();
        }
    }
    
}
