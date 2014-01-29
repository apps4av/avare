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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
        
    /**
     * @param context
     * @param textViewResourceId
     */
    public NearestAdapter(Context context, String[] distance, String name[], 
            String bearing[], String[] fuel, String[] runway) {
        super(context, R.layout.nearest, distance);
        mContext = context;
        mBearing = bearing;
        mDistance = distance;
        mName = name;
        mFuel = fuel;
        mElevation = runway;
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
            String bearing[], String[] fuel, String[] elevation) {
        mBearing = bearing;
        mDistance = distance;
        mName = name;
        mFuel = fuel;
        mElevation = elevation;
        notifyDataSetChanged();
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
        textView.setText(mName[position]);
        textView = (TextView)rowView.findViewById(R.id.nearest_list_fuel);
        textView.setText(mFuel[position]);
        textView = (TextView)rowView.findViewById(R.id.nearest_list_elevation);
        textView.setText(mElevation[position]);
        
        return rowView;
    }
    
}
