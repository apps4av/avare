/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.preference.DialogPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.R;
import com.ds.avare.content.DataSource;
import com.ds.avare.storage.Preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;


/**
 * 
 * @author zkhan
 *
 */
public class FolderPreference extends DialogPreference {

    private Context mContext;
    
    private ListView mListView;
    private String mChosenFile;
    private ArrayList<String> mStr;
    private File mPath;
    private Item[] mFileList;
    private ListAdapter mAdapter;
    private Boolean mFirstLevel;
    private TextView mPathView;
    private Button mButton;
    private Button mButtonSDCard;
    private Button mButtonExternal;
    private Preferences mPref;
    
    /**
     * 
     * @param path
     */
    private void init(String path) {
    	if(path.length() == 0) {
    		path = "/";
    	}
        if(path.equals("/")) {
            mFirstLevel = true;
        }
        else {
            mFirstLevel = false;
        }
        mPath = new File(path);
        String tokens[] = path.split("/");
        mStr = new ArrayList<String>(tokens.length);
        for (String s : tokens) {  
            mStr.add(s);
        }
    }
    
    /**
     * 
     * @param context
     * @param attrs
     */
    public FolderPreference(Context context, AttributeSet attrs) {        
        super(context, attrs);
        mContext = context;
        mPref = new Preferences(context);
        
        // A "generic" handler that is used for several different config items
        // we need to find out what control this is to see where to read the
        // initial value from
        //
        // User defined Waypoints
        if(getKey().equals(mContext.getString(R.string.UDWLocation))) {
            init(mPref.getUDWLocation());
        }
        
        // The chart/map location
        else if(getKey().equals(mContext.getString(R.string.Maps))) {
            init(mPref.mapsFolder());
        }
    }

    /**
     * 
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if(positiveResult) {

            // reset all databases on new folder
            DataSource.reset(mContext);

            // Create a default toast message that assumes failure
            Toast t = Toast.makeText(mContext, 
                    mContext.getString(R.string.FileStoreInvalid) + mPath.getAbsolutePath(), Toast.LENGTH_LONG);;

             // Dir should be read/write to be a valid android folder.
            if(mPath.isDirectory() && mPath.canWrite() && mPath.canRead()) {
            	String absPath = mPath.getAbsolutePath();

            	// If this is for the charts, then set it and display some toast
                if(getKey().equals(mContext.getString(R.string.Maps))) {

                	// Save this in the preferences for maps
                	mPref.setMapsFolder(absPath);

                	// Set our message to success for setting the maps folder
                	t = Toast.makeText(mContext, 
	                        mContext.getString(R.string.FileStore) + absPath, Toast.LENGTH_LONG);               
                }
                
                // If this is for the UserDefinedWaypoints setting ...
                else if (getKey().equals(mContext.getString(R.string.UDWLocation))) {
                	
                	// Save this in preferences for the UDWaypoints
                	mPref.setUDWLocation(absPath);
                	
                	// Set our toast to success for setting the UDW directory
	            	t = Toast.makeText(mContext, 
	                        mContext.getString(R.string.UDWSearch) + absPath, Toast.LENGTH_LONG);               
                }
            }
            
            // Show our status
            t.show();
        }
    }
    
    @Override
    protected View onCreateDialogView() {
        LayoutInflater layoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.folder, null);
        
        mListView = (ListView)view.findViewById(R.id.folder_list);
        mPathView = (TextView)view.findViewById(R.id.folder_text_path);
        mButton = (Button)view.findViewById(R.id.folder_button_internal);
        mButtonSDCard = (Button)view.findViewById(R.id.folder_button_sdcard);
        mButtonExternal = (Button)view.findViewById(R.id.folder_button_external);

        /*
         * Bring up permission
         */
        if(PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                init(mContext.getFilesDir().getAbsolutePath());
                loadFileList();
                mListView.setAdapter(mAdapter);
                
            }
            
        });

        mButtonExternal.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                
                /*
                 * Bring up preferences
                 */
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + "com.ds.avare";
                new File(path).mkdirs();
                init(path);
                loadFileList();
                mListView.setAdapter(mAdapter);
                // Show help for kitkat+ users
                Toast.makeText(mContext, mContext.getString(R.string.folderHelp), 
                        Toast.LENGTH_LONG).show();
                
            }
            
        });

        mButtonSDCard.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                /*
                 * Bring up preferences
                 */
                String path = "/" + "sdcard" + "/Android/data/" + "com.ds.avare";
                new File(path).mkdirs();
                init(path);
                loadFileList();
                mListView.setAdapter(mAdapter);
                // Show help for kitkat+ users
                Toast.makeText(mContext, mContext.getString(R.string.folderHelp),
                        Toast.LENGTH_LONG).show();

            }

        });

        /*
         * Load files from the disk in a list
         */
        loadFileList();
        mListView.setAdapter(mAdapter);
        
        /*
         * On click, change folders
         */
        OnItemClickListener l = new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int which, long id) {
                                   
                mChosenFile = mFileList[which].file;
                File sel = new File(mPath + "/" + mChosenFile);
                if (sel.isDirectory() && sel.canRead()) {
                    mFirstLevel = false;

                    /* 
                     * Adds chosen directory to list
                     */
                    mStr.add(mChosenFile);
                    mFileList = null;
                    mPath = new File(sel + "");
                }
                /*
                 * Checks if 'up' was clicked
                 */
                else if (mChosenFile.equals(mContext.getString(R.string.Up)) && !sel.exists()) {

                    /*
                     * present directory removed from list
                     */
                    String s = mStr.remove(mStr.size() - 1);

                    /* 
                     * mPath modified to exclude present directory
                     */
                    mPath = new File(mPath.toString().substring(0,
                            mPath.toString().lastIndexOf(s)));
                    mFileList = null;

                    /*
                     * if there are no more directories in the list, then
                     * its the first level
                     */
                    if (mStr.isEmpty()) {
                        mFirstLevel = true;
                    }   
                }
                else {
                    /*
                     * This directory is not writeable
                     */
                    return;
                }
                /*
                 * Reload after click
                 */
                loadFileList();
                mListView.setAdapter(mAdapter);
            }
        };

        mListView.setOnItemClickListener(l);
        
        return view;
    }

    /**
     * 
     */
    private void loadFileList() {
        
        /*
         * Checks whether mPath exists
         */
        if (mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    /*
                     * Filters based on whether the file is hidden or not, and is a folder
                     */
                    return (!sel.isHidden());
                }
            };

            String[] fList = mPath.list(filter);
            if(fList == null) {
                fList = new String[0];
            }
            mFileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {
                mFileList[i] = new Item(fList[i], android.R.drawable.alert_light_frame);

                /*
                 * Convert into file mPath
                 */
                File sel = new File(mPath, fList[i]);

                /*
                 * Set drawables
                 */
                if(sel.isDirectory()) {
                    if (sel.canWrite()) {
                        mFileList[i].icon = android.R.drawable.ic_menu_save;
                    } 
                    else {
                        mFileList[i].icon = android.R.drawable.ic_lock_lock;
                    }
                }
                /*
                 * File is for information only
                 */
                else {
                    mFileList[i].icon = android.R.drawable.alert_light_frame;
                }
            }

            if (!mFirstLevel) {
                Item temp[] = new Item[mFileList.length + 1];
                for (int i = 0; i < mFileList.length; i++) {
                    temp[i + 1] = mFileList[i];
                }
                temp[0] = new Item(mContext.getString(R.string.Up), android.R.drawable.ic_menu_revert);
                mFileList = temp;
            }
        }

        if(mFileList == null) {
            return;
        }
        /*
         * Set the adapter with file list
         */
        mAdapter = new ArrayAdapter<Item>(mContext,
                android.R.layout.select_dialog_item, android.R.id.text1,
                mFileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                /*
                 * creates view
                 */
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView)view.findViewById(android.R.id.text1);

                /* 
                 * put the image on the text view
                 */
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        mFileList[position].icon, 0, 0, 0);

                /*
                 *  add margin between image and text (support various screen
                 */
                textView.setCompoundDrawablePadding(10);
                textView.setTextColor(Color.parseColor("#FF71BC78"));
                return view;
            }
        };

        /*
         * Show where we are
         */
        mPathView.setText(mPath.getAbsolutePath());
    }


    /*Copyright 2011 Manish Burman

     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
    */
    private class Item {
        public String file;
        public int icon;

        public Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }

}