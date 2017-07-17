/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ds.avare.R;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.NetworkHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author zkhan
 *
 */
public class ChartAdapter extends BaseExpandableListAdapter {

    private static final int STATE_UNCHECKED = 0;
    private static final int STATE_CHECKED = 1;
    private static final int STATE_DELETE = 2;
    
    private Context mContext;
    private Preferences mPref;
    private BitmapHolder mOkBitmapHolder;
    private BitmapHolder mAddBitmapHolder;
    private BitmapHolder mUpdateBitmapHolder;
    private BitmapHolder mDeleteBitmapHolder;
    private BitmapHolder mNoneBitmapHolder;
    private String[] mGroups;
    private String[][] mChildrenFiles;
    private String[][] mChildren;
    private int[][] mChecked;
    private String[][] mVers;
    
    static final int blocksize = 128;
    
    private static final int GROUP_DATABASE = 0;
    private static final int GROUP_SECTIONAL = 1;
    private static final int GROUP_NUM = 2;

    /**
     * @param context
     */
    public ChartAdapter(Context context) {
        mPref = new Preferences(context);
        mContext = context;
        
        /*
         * Get all groups
         */
        mGroups = context.getResources().getStringArray(R.array.resGroups);
        /*
         * Assign children
         */
        mChildren = new String[GROUP_NUM][];
        mChildren[GROUP_DATABASE] = context.getResources().getStringArray(R.array.resNameDatabase);
        mChildren[GROUP_SECTIONAL] = context.getResources().getStringArray(R.array.resNameSectional);

        /*
         * Assign children file names
         */
        mChildrenFiles = new String[GROUP_NUM][];
        mChildrenFiles[GROUP_DATABASE] = context.getResources().getStringArray(R.array.resFilesDatabase);
        mChildrenFiles[GROUP_SECTIONAL] = context.getResources().getStringArray(R.array.resFilesSectional);

        /*
         * Allocate space for versions
         * This will be filled later. For now, init them with any value.
         */
        mVers = new String[GROUP_NUM][];
        mVers[GROUP_DATABASE] = context.getResources().getStringArray(R.array.resFilesDatabase);
        mVers[GROUP_SECTIONAL] = context.getResources().getStringArray(R.array.resFilesSectional);

        /*
         * Allocate space for checked charts
         */
        mChecked = new int[GROUP_NUM][];
        mChecked[GROUP_DATABASE] = new int[mVers[GROUP_DATABASE].length];
        mChecked[GROUP_SECTIONAL] = new int[mVers[GROUP_SECTIONAL].length];

        /*
         * Get various bitmaps
         */
        mOkBitmapHolder = new BitmapHolder(mContext, R.drawable.check);
        mUpdateBitmapHolder = new BitmapHolder(mContext, R.drawable.check_red);
        mDeleteBitmapHolder = new BitmapHolder(mContext, R.drawable.delete);
        mAddBitmapHolder = new BitmapHolder(mContext, R.drawable.add);
        mNoneBitmapHolder = new BitmapHolder(mContext, R.drawable.unknown);
        
        refreshIt();
        
    }

    
    /**
     * 
     */
    public void refreshIt() {
        /*
         * Update versions
         */
        new ViewTask().execute();        
    }
    
    /**
     * 
     * @author zkhan
     *
     */
    private class ViewTask extends AsyncTask<Void, Void, Boolean> {

        String[][] vers;

        @Override
        protected Boolean doInBackground(Void... params) {

            vers = mVers.clone();
                       
            /*
             * Always get version in BG because its a network operation
             *
             */
            
            /*
             * Load versions
             */
            for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
                for(int child = 0; child < vers[group].length; child++) {
                    /*
                     * Read version from file and if it exists.
                     */
                    File file = new File(mPref.mapsFolder() + "/" + mChildrenFiles[group][child]);
                    vers[group][child] = null;
                    if(file.exists()) {
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(file), blocksize);
                            /*
                             * Preferably do assignment on UI thread in postExecute()
                             */
                            vers[group][child] = br.readLine();
                            br.close();
                        }
                        catch (IOException e) {
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mVers = vers;
            notifyDataSetChanged();            
        }
    }

    /**
     * Just update the version numbers.
     */
    public void refresh() {
        new ViewTask().execute();
    }

    /**
     * Update the version of a chart after it has been downloaded. 
     * @param name
     * @param version
     */
    public void updateVersion(String name, String version) {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(mChildrenFiles[group][child].equals(name)) {
                    mVers[group][child] = version;
                    break;
                }
            }
        }
    }

    /**
     * Toggle the checked state of a chart
     */
    public String getDatabaseName() {
        return mChildrenFiles[GROUP_DATABASE][0];
    }
    
    /**
     * checked state of a chart
     * @param name
     */
    public void setChecked(String name) {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(mChildrenFiles[group][child].equals(name)) {
                    mChecked[group][child] = STATE_CHECKED;                        
                    break;
                }
            }
        }
    }

    /*
     * if chart is static
     * @param name
     */
    public boolean isStatic(String name) {
        return true;
    }

    /**
     * checked state of a chart
     * @param name
     */
    public void unsetChecked(String name) {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(mChildrenFiles[group][child].equals(name)) {
                    mChecked[group][child] = STATE_UNCHECKED;                        
                    break;
                }
            }
        }
    }

    /**
     * checked state of a chart
     * @param name
     */
    public void setDelete(String name) {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(mChildrenFiles[group][child].equals(name)) {
                    mChecked[group][child] = STATE_DELETE;                        
                    break;
                }
            }
        }
    }

    /**
     * Toggle the checked state of a chart
     */
    public void toggleChecked(int group, int child) {
        if(mChecked[group][child] == STATE_CHECKED) {
            mChecked[group][child] = STATE_DELETE;
        }
        else if(mChecked[group][child] == STATE_DELETE) {
            mChecked[group][child] = STATE_UNCHECKED;                        
        }
        else if(mChecked[group][child] == STATE_UNCHECKED) {
            mChecked[group][child] = STATE_CHECKED;                        
        }
    }

    /**
     * Get the next checked chart
     * @return
     */
    public String getChecked() {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(STATE_CHECKED == mChecked[group][child]) {
                    return mChildrenFiles[group][child];
                }
            }
        }
        /*
         * Nothing checked
         */
        return null;
    }

    /**
     * Get the next checked chart
     * @return
     */
    public String getDeleteChecked() {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(STATE_DELETE == mChecked[group][child]) {
                    return mChildrenFiles[group][child];
                }
            }
        }
        /*
         * Nothing checked
         */
        return null;
    }

    /**
     * Check the downloaded.
     * @return
     */
    public void checkDone() {
        for(int group = GROUP_DATABASE; group < GROUP_NUM; group++) {
            for(int child = 0; child < mVers[group].length; child++) {
                if(mVers[group][child] == null) {
                    continue;
                }
                if(NetworkHelper.isExpired(mVers[group][child], mPref.getExpiryTime()) && doesChartExpire(group)) {
                    mChecked[group][child] = STATE_CHECKED;
                }
            }
        }
    }

    /**
     *
     * @param group
     * @return
     */
    private boolean doesChartExpire(int group) {
        return false;
    }

    /**
     * 
     */
    @Override
    public View getGroupView(int group, boolean isExpanded, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        View rowView = convertView;

        /*
         * Do not inflate if not needed. Speeds up things quite a bit
         */
        if(null == rowView) {
            rowView = inflater.inflate(R.layout.textview, parent, false);
        }
        
        TextView tv = (TextView)rowView.findViewById(R.id.textview_textview);
        
        int total = mChildren[group].length;
        boolean expired = false;
        
        /*
         * Inform with red color if any child is expired
         */
        for(int child = 0; child < total; child++) {
            if(mVers[group][child] != null) {
                expired |= NetworkHelper.isExpired(mVers[group][child], mPref.getExpiryTime());
            }
        }
        if(expired && doesChartExpire(group)) {
            tv.setTextColor(0xFF7F0000);
        }
        else {
            tv.setTextColor(0xFF007F00);
        }
        tv.setText(mGroups[group] + "(" + total + ")");
        if(isExpanded) {
            tv.setTypeface(null, Typeface.BOLD_ITALIC);
        }
        else {
            tv.setTypeface(null, Typeface.BOLD);
        }
        return rowView;
    }
    
    /**
     * 
     */
    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.chart_download_list, parent, false);
        }
        ImageView imgView = (ImageView)rowView.findViewById(R.id.chart_download_list_image);
        TextView textView = (TextView)rowView.findViewById(R.id.chart_download_list_item);
        textView.setText(mChildren[groupPosition][childPosition]);
        
        /*
         * Get status of chart item from preferences.
         */
        TextView textView2 = (TextView)rowView.findViewById(R.id.chart_download_list_state);
        if(mVers[groupPosition][childPosition] != null) {
            textView2.setText(mVers[groupPosition][childPosition] + " " + NetworkHelper.getVersionRange(mVers[groupPosition][childPosition]));
            imgView.setImageBitmap(mOkBitmapHolder.getBitmap());
            
            if(NetworkHelper.isExpired(mVers[groupPosition][childPosition], mPref.getExpiryTime()) && doesChartExpire(groupPosition)) {
                imgView.setImageBitmap(mUpdateBitmapHolder.getBitmap());                    
            }
        }
        else {
            imgView.setImageBitmap(mNoneBitmapHolder.getBitmap());                
            textView2.setText("");
        }

        if(mChecked[groupPosition][childPosition] == STATE_CHECKED) {
            imgView.setImageBitmap(mAddBitmapHolder.getBitmap());
        }
        else if(mChecked[groupPosition][childPosition] == STATE_DELETE) {
            imgView.setImageBitmap(mDeleteBitmapHolder.getBitmap());
        }

        return rowView;
    }

    /**
     * 
     */
    @Override
    public Object getChild(int arg0, int arg1) {
        return mChildren[arg0][arg1];
    }

    /**
     * 
     */
    @Override
    public long getChildId(int arg0, int arg1) {
        return arg1;
    }

    /**
     * 
     */
    @Override
    public int getChildrenCount(int arg0) {
        int count = 0;
        try {
            count = mChildren[arg0].length;
        } 
        catch (Exception e) {
        }

        return count;
    }

    /**
     * 
     */
    @Override
    public Object getGroup(int arg0) {
        return mGroups[arg0];
    }

    /**
     * 
     */
    @Override
    public int getGroupCount() {
        return mGroups.length;
    }

    /**
     * 
     */
    @Override
    public long getGroupId(int arg0) {
        return arg0;
    }

    /**
     * 
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * 
     */
    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }
}
