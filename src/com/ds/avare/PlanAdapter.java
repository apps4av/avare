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

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * @author zkhan
 *
 */
public class PlanAdapter extends ArrayAdapter<String> {

    private Context  mContext;
    private ArrayList<String> mName;
    private ArrayList<String> mInfo;
    private ArrayList<Boolean> mPassed;
    private int mNext;
        
    /**
     * @param context
     * @param textViewResourceId
     */
    public PlanAdapter(Context context, ArrayList<String> name, ArrayList<String> info, ArrayList<Boolean> passed) {
        super(context, R.layout.plan_list, name);
        mContext = context;
        mName = name;
        mInfo = info;
        mPassed = passed;
    }

    /**
     * 
     * @param distance
     * @param name
     * @param bearing
     * @param eta
     */
    public void updateList(ArrayList<String> name, ArrayList<String> info, ArrayList<Boolean> passed) {
        mName = name;
        mInfo = info;
        mPassed = passed;
        notifyDataSetChanged();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;
        
        /*
         * Find the next not passed.
         */
        for(int id = 0; id < mPassed.size(); id++) {
            if(!mPassed.get(id)) {
                mNext = id;
                break;
            }
        }

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.plan_list, parent, false);
        }
        TextView textView = (TextView)rowView.findViewById(R.id.plan_list_aid_name);
        textView.setText(mName.get(position));            
        if(mNext == position) {
            /*
             * Add an arrow to indicate next way point in the plan
             */
            textView.setText("> " + mName.get(position));
            textView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        else if (position < mNext) {
            /*
             * Show passed points smaller
             */
            textView.setTypeface(Typeface.DEFAULT);
        }
        else {
            textView.setTypeface(Typeface.DEFAULT);
        }
        textView = (TextView)rowView.findViewById(R.id.plan_list_info);
        textView.setText(mInfo.get(position));
        
        return rowView;
    }
    
}
