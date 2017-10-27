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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ds.avare.R;

/**
 * @author zkhan
 *
 */
public class NearestAdapter extends ArrayAdapter<String> {

    private Context  mContext;
    private String[] mDistance;
    private String[] mName;
    private String[] mBearing;
    private String[] mFuel;
    private String[] mElevation;
    private String[] mLongestRunway;
    private boolean[] mGlide;
        
    /**
     * @param context
     */
    public NearestAdapter(Context context, String[] distance, String name[], 
            String bearing[], String[] fuel, String[] elev, String[] runway, boolean[] glide) {
        super(context, R.layout.nearest, distance);
        mContext = context;
        mBearing = bearing;
        mDistance = distance;
        mName = name;
        mFuel = fuel;
        mElevation = elev;
        mLongestRunway = runway;
        mGlide = glide;
    }

    /**
     * 
     * @param distance
     * @param name
     * @param bearing
     * @param fuel
     * @param elevation
     */
    public void updateList(String[] distance, String name[], 
            String bearing[], String[] fuel, String[] elevation, String[] runway,
            boolean[] glide) {
        mBearing = bearing;
        mDistance = distance;
        mName = name;
        mFuel = fuel;
        mElevation = elevation;
        mLongestRunway = runway;
        mGlide = glide;
        notifyDataSetChanged();
    }
    
    /**
     * 
     * @return
     */
    public int getClosestWith100LL() {
        if(mFuel == null) {
            return -1;
        }
        
        for(int i = 0; i < mFuel.length; i++) {
            if(mFuel[i] != null) {
                if(mFuel[i].contains("100LL")) {
                    return i;
                }
            }
        }
            
        return -1;
    }

    /**
     * Get the closest airport with runway of length of least length
     * @return
     */
    public int getClosestRunwayWithMinLength(int length) {
        if(mLongestRunway == null) {
            return -1;
        }

        for(int i = 0; i < mLongestRunway.length; i++) {
            if(mLongestRunway[i] != null) {
                int len = -1;
                try {
                    len = Integer.parseInt(mLongestRunway[i].split("X")[0]);
                }
                catch (Exception e) {
                    continue;
                }
                if(len >= length) {
                    return i;
                }
            }
        }
            
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.nearest_list, parent, false);
        }
        TextView textView = (TextView)rowView.findViewById(R.id.nearest_list_distance);
        textView.setText(mDistance[position]);
        textView = (TextView)rowView.findViewById(R.id.nearest_list_bearing);
        textView.setText(mBearing[position]);
        textView = (TextView)rowView.findViewById(R.id.nearest_list_aid_name);
        textView.setText(mName[position] + (mFuel[position].equals("") ? "" : "$ " + mContext.getString(R.string.Fuel)));
        /*
         * If cannot glide, mark it reddish, or yellowish if can
         */
        if(mGlide[position] == false) {
            textView.setTextColor(0xffe35327);
        }
        else {
            textView.setTextColor(0xffabb149);            
        }
        /*
         * Fuel shows as Fuel or none
         */
        textView = (TextView)rowView.findViewById(R.id.nearest_list_elevation);
        textView.setText(mLongestRunway[position].equals("") ? "" : (mLongestRunway[position] + "ft @" + mElevation[position]));
        
        return rowView;
    }
    
}
