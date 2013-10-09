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



import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

/**
 * @author zkhan
 *
 */
public class PopoutAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    private String[] mGroups;
    private String[] mChildrenText;
    private String[][] mChildren;
    
    private Metar mMetar;
    private Taf mTaf;
    private String mAirep;
    private String mTfr;
    private String mMets;
    private WindsAloft mWa;
    private Typeface mFace;

    private static final int GROUP_METAR = 0;
    private static final int GROUP_TAF = 1;
    private static final int GROUP_PIREP = 2;
    private static final int GROUP_TFR = 3;
    private static final int GROUP_METS = 4;
    private static final int GROUP_WA = 5;
    private static final int GROUP_NUM = 6;
    
    /**
     * @param context
     * @param textViewResourceId
     */
    public PopoutAdapter(Context context, LongTouchDestination data) {
        
        
        mContext = context;
        mFace = Typeface.createFromAsset(context.getAssets(), "LiberationMono-Bold.ttf");
        
        /*
         * Get all groups
         */
        mGroups = context.getResources().getStringArray(R.array.resGroupsPopout);
        /*
         * Assign children
         */
        mChildren = new String[GROUP_NUM][];
        mChildren[GROUP_METAR] = new String[1];
        mChildren[GROUP_TAF] = new String[1];
        mChildren[GROUP_PIREP] = new String[1];
        mChildren[GROUP_TFR] = new String[1];
        mChildren[GROUP_METS] = new String[1];
        mChildren[GROUP_WA] = new String[1];
        
        mChildrenText = new String[GROUP_NUM];

        /*
         * Show view
         */
        mTfr = data.tfr;
        mMets = data.mets;
        mMetar = data.metar;
        mTaf = data.taf;
        mAirep = data.airep;
        mWa = data.wa;

        
        mChildrenText[GROUP_TFR] = mTfr == null ? "" : mTfr;
        mChildrenText[GROUP_METS] = mMets == null ? "" : mMets;

        if(mMetar == null) {
            mChildrenText[GROUP_METAR] = "";
        }
        else {
            mChildrenText[GROUP_METAR] = "@ " + mMetar.time + "\n" + 
                    WeatherHelper.formatWeather(mMetar.rawText);          
        }

        if(mTaf == null) {
            mChildrenText[GROUP_TAF] = "";
        }
        else {
            mChildrenText[GROUP_TAF] = "@ " + mTaf.time + "\n" + 
                    WeatherHelper.formatWeather(mTaf.rawText);          
        }
        
        if(mWa == null) {
            mChildrenText[GROUP_WA] = "";
        }
        else {
            mChildrenText[GROUP_WA] = mWa.station + " " + mWa.time + "\n" +
                    "@030 "  + mWa.w3k + "\n" + 
                    "@060 " + mWa.w6k + "\n" +
                    "@090 " + mWa.w9k + "\n" +
                    "@120 " + mWa.w12k + "\n" +
                    "@180 " + mWa.w18k + "\n" +
                    "@240 " + mWa.w24k + "\n" +
                    "@300 " + mWa.w30k + "\n" +
                    "@340 " + mWa.w34k + "\n" +
                    "@390 " + mWa.w39k;
        }

        if(mAirep == null) {
            mChildrenText[GROUP_PIREP] = "";
        }
        else {
            mChildrenText[GROUP_PIREP] = mAirep;
        }

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
        
        /*
         * Set different values from different outputs.
         */
        switch(group) {
            case GROUP_METAR:
                int col = (mMetar == null) ? 0xFFFFFFFF : WeatherHelper.metarColor(mMetar.flightCategory);
                tv.setText(mGroups[group]);
                tv.setTextColor(col);
                break;
            case GROUP_TAF:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_PIREP:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_TFR:
                tv.setTextColor((mTfr == null) ? 0xFFFFFFFF : 0xFFFF0000);
                tv.setText(mGroups[group]);
                break;
            case GROUP_METS:
                tv.setTextColor((mMets == null) ? 0xFFFFFFFF : 0xFF0000FF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_WA:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
        }
       
        /*
         * If not available show gray
         */
        if(mChildrenText[group].equals("")) {
            tv.setTextColor(0xFF7F7F7F);
            tv.setTypeface(null, Typeface.ITALIC);
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
            rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tv = (TextView)rowView;
        tv.setTextColor(0xFFFFFFFF);
        tv.setTypeface(mFace);
        tv.setText(mChildrenText[groupPosition]);
        
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
