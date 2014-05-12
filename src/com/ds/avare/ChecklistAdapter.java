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
public class ChecklistAdapter extends ArrayAdapter<String> {

    private Context  mContext;
    private ArrayList<String> mInfo;
    private int mChecked;
        
    /**
     * @param context
     * @param textViewResourceId
     */
    public ChecklistAdapter(Context context, ArrayList<String> info) {
        super(context, R.layout.check_list, info);
        mContext = context;
        mInfo = info;
        mChecked = 0;
    }

    /**
     * 
     * @param distance
     * @param name
     * @param bearing
     * @param eta
     */
    public void updateList(ArrayList<String> info) {
        mInfo = info;
        notifyDataSetChanged();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;
        
        if(null == rowView) {
            rowView = inflater.inflate(R.layout.check_list, parent, false);
        }
        TextView textView = (TextView)rowView.findViewById(R.id.check_list_info);
        if(mChecked == position) {
            textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            textView.setText("> " + mInfo.get(position));            
        }
        else {
            textView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            textView.setText(mInfo.get(position));            
        }
        return rowView;
    }
    
    
    public void setChecked(int pos) {
        mChecked = pos;
        notifyDataSetChanged();
    }
}
