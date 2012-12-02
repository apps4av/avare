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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author zkhan
 *
 */
public class ChartAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private String[] mValues;
    private String[] mIds;
    private boolean[] mChecked;
    private Preferences mPref;
    private String[] mVers;
    private BitmapHolder mOkBitmapHolder;
    private BitmapHolder mAddBitmapHolder;
    private BitmapHolder mUpdateBitmapHolder;
    private BitmapHolder mNoneBitmapHolder;
    private String mVersion;
    
    static final int blocksize = 128;
    
    /**
     * @param context
     * @param textViewResourceId
     */
    public ChartAdapter(Context context, String[] values, String ids[]) {
        super(context, R.layout.chart_list, values);
        mContext = context;
        mValues = values;
        mIds = ids;
        mVers = new String[ids.length];
        mPref = new Preferences(context);
        mChecked = new boolean[ids.length];
        new ViewTask().execute();
        mOkBitmapHolder = new BitmapHolder(mContext, R.drawable.check);
        mUpdateBitmapHolder = new BitmapHolder(mContext, R.drawable.checkred);
        mAddBitmapHolder = new BitmapHolder(mContext, R.drawable.add);
        mNoneBitmapHolder = new BitmapHolder(mContext, R.drawable.whitesq);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.chart_list, parent, false);
        }
        ImageView imgView = (ImageView)rowView.findViewById(R.id.image);
        TextView textView = (TextView)rowView.findViewById(R.id.item);
        textView.setText(mValues[position]);
        
        /*
         * Get status of chart item from preferences.
         */
        TextView textView2 = (TextView)rowView.findViewById(R.id.state);
        if(mVers[position] != null) {
            textView2.setText(mVers[position]);
            imgView.setImageBitmap(mOkBitmapHolder.getBitmap());            
            if(mVersion != null) {
                if(!mVersion.equals(mVers[position])) {
                    imgView.setImageBitmap(mUpdateBitmapHolder.getBitmap());
                }
            }
        }
        else {
            imgView.setImageBitmap(mNoneBitmapHolder.getBitmap());                
            textView2.setText("");
        }

        if(mChecked[position]) {
            imgView.setImageBitmap(mAddBitmapHolder.getBitmap());
        }

        return rowView;
    }

    /**
     * 
     */
    public void refresh() {
        new ViewTask().execute();
    }

    /**
     * 
     */
    public void checkOld() {
        for(int id = 0; id < mVers.length; id++) {
            if(mVers[id] != null) {
                if(mVersion != null) {
                    if(!mVersion.equals(mVers[id])) {
                        mChecked[id] = true;
                        continue;
                    }
                }
            }
            mChecked[id] = false;
        }
    }
    
    /**
     * 
     * @param name
     * @param version
     */
    public void updateVersion(String name, String version) {
        for(int id = 0; id < mVers.length; id++) {
            if(mIds[id].equals(name)) {
                mVers[id] = version;
                break;
            }
        }
    }

    /**
     * 
     * @param name
     * @param version
     */
    public void updateChecked(String name) {
        for(int id = 0; id < mChecked.length; id++) {
            if(mIds[id].equals(name)) {
                mChecked[id] = !mChecked[id];
                break;
            }
        }
    }

    /**
     * 
     * @param position
     * @return
     */
    public boolean getChecked(int position) {
        return mChecked[position];
    }
    
    /**
     * 
     * @author zkhan
     *
     */
    private class ViewTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            NetworkHelper helper = new NetworkHelper();
            mVersion = helper.getVersion();
                       
            /*
             * Always get version in BG because its a network operation
             *
             */
            
            /*
             * Load versions
             */
            for(int id = 0; id < mIds.length; id++) {
                /*
                 * Read version from file and if it exists.
                 */
                File file = new File(mPref.mapsFolder() + "/" + mIds[id]);
                mVers[id] = null;
                if(file.exists()) {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file), blocksize);
                        /*
                         * Preferably do assignment on UI thread in postExecute()
                         */
                        mVers[id] = br.readLine();
                        br.close();
                    }
                    catch (IOException e) {
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            notifyDataSetChanged();            
        }
    }
}
