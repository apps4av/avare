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



import java.util.LinkedList;

import com.ds.avare.SubmitActivity;
import com.ds.avare.R;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
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
    private LinkedList<Airep> mAirep;
    private LinkedList<String> mFreq;
    private String mTfr;
    private String mSua;
    private String mPerformance;
    private String mRadar;
    private String mMets;
    private WindsAloft mWa;
    private Typeface mFace;
    private String mFuel;
    private String mRatings;
    private String mAirport;

    private static final int GROUP_COMM = 0;
    private static final int GROUP_PERFORMANCE = 1;
    private static final int GROUP_METAR = 2;
    private static final int GROUP_TAF = 3;
    private static final int GROUP_WA = 4;
    private static final int GROUP_PIREP = 5;
    private static final int GROUP_METS = 6;
    private static final int GROUP_TFR = 7;
    private static final int GROUP_SUA = 8;
    private static final int GROUP_FUEL = 9;
    private static final int GROUP_RATINGS = 10;
    private static final int GROUP_RADAR = 11;
    private static final int GROUP_NUM = 12;
    
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
        mChildren[GROUP_COMM] = new String[1];
        mChildren[GROUP_PERFORMANCE] = new String[1];
        mChildren[GROUP_FUEL] = new String[1];
        mChildren[GROUP_METAR] = new String[1];
        mChildren[GROUP_TAF] = new String[1];
        mChildren[GROUP_RATINGS] = new String[1];
        mChildren[GROUP_WA] = new String[1];
        mChildren[GROUP_PIREP] = new String[1];
        mChildren[GROUP_METS] = new String[1];
        mChildren[GROUP_TFR] = new String[1];
        mChildren[GROUP_SUA] = new String[1];
        mChildren[GROUP_RADAR] = new String[1];
        
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
        mFreq = data.freq;
        mSua = data.sua;
        mRadar = data.radar;
        mPerformance = data.performance;
        mFuel = data.fuel;
        mAirport = data.airport;
        mRatings = data.ratings;
        
        mChildrenText[GROUP_PERFORMANCE] = mPerformance == null ? "" : mPerformance;
        mChildrenText[GROUP_TFR] = mTfr == null ? "" : mTfr;
        mChildrenText[GROUP_FUEL] = mFuel == null ? "" : mFuel;
        mChildrenText[GROUP_METS] = mMets == null ? "" : mMets;
        mChildrenText[GROUP_SUA] = mSua == null ? "" : mSua;
        mChildrenText[GROUP_RADAR] = mRadar == null ? "" : mRadar;
        mChildrenText[GROUP_RATINGS] = mRatings == null ? "" : mRatings;
        
        if(mMetar == null) {
            mChildrenText[GROUP_METAR] = "";
        }
        else {
            mChildrenText[GROUP_METAR] = WeatherHelper.formatWeather(mMetar.rawText) + "\n";          
        }

        if(mTaf == null) {
            mChildrenText[GROUP_TAF] = "";
        }
        else {
            mChildrenText[GROUP_TAF] = WeatherHelper.formatWeather(mTaf.rawText) +"\n";          
        }
        
        if(mWa == null) {
            mChildrenText[GROUP_WA] = "";
        }
        else {
            mChildrenText[GROUP_WA] = mWa.station + " " + mWa.time + "\n" +
                    "@030 " + WeatherHelper.decodeWind(mWa.w3k) + "\n" + 
                    "@060 " + WeatherHelper.decodeWind(mWa.w6k) + "\n" +
                    "@090 " + WeatherHelper.decodeWind(mWa.w9k) + "\n" +
                    "@120 " + WeatherHelper.decodeWind(mWa.w12k) + "\n" +
                    "@180 " + WeatherHelper.decodeWind(mWa.w18k) + "\n" +
                    "@240 " + WeatherHelper.decodeWind(mWa.w24k) + "\n" +
                    "@300 " + WeatherHelper.decodeWind(mWa.w30k) + "\n" +
                    "@340 " + WeatherHelper.decodeWind(mWa.w34k) + "\n" +
                    "@390 " + WeatherHelper.decodeWind(mWa.w39k);
        }


        if(mAirep == null) {
            mChildrenText[GROUP_PIREP] = "";
        }
        else {

            String txt = "";
            for(Airep a : mAirep) {
                txt += a.reportType + "@ " + a.time + "\n" + a.rawText + "\n\n";                
            }
    
            /*
             * Remove last \n
             */
            if(txt.length() > 1) {
                txt = txt.substring(0, txt.length() - 2);
            }

            mChildrenText[GROUP_PIREP] = txt;
        }

        if(mFreq == null) {
            mChildrenText[GROUP_COMM] = "";
        }
        else {

            String txt = "";
            for(String f : mFreq) {
                txt += f + "\n\n";                
            }
    
            /*
             * Remove last \n
             */
            if(txt.length() > 1) {
                txt = txt.substring(0, txt.length() - 2);
            }

            mChildrenText[GROUP_COMM] = txt;
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
            case GROUP_COMM:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_PERFORMANCE:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_FUEL:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_RATINGS:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
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
            case GROUP_SUA:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_RADAR:
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
            boolean isLastChild, View convertView, final ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.textview_wrap, parent, false);
        }

        TextView tv = (TextView)rowView.findViewById(R.id.textview_textview_wrap);
        Button but = (Button)rowView.findViewById(R.id.textview_wrap_action);
        
        if(groupPosition == GROUP_FUEL && (!mChildrenText[groupPosition].equals(""))) {
        	// Fuel report button
	        but.setText(mContext.getString(R.string.Report));
	        but.setVisibility(View.VISIBLE);
	        but.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// Allow submission of fuel cost via a new activity
	                Intent intent = new Intent(parent.getContext(), SubmitActivity.class);
	                intent.putExtra(SubmitActivity.FUEL_AIRPORT, mAirport);
	                intent.putExtra(SubmitActivity.SUBMIT, SubmitActivity.FUEL);
					parent.getContext().startActivity(intent);
				}
	        });
        }
        else if(groupPosition == GROUP_RATINGS && (!mChildrenText[groupPosition].equals(""))) {
        	// report button
	        but.setText(mContext.getString(R.string.Report));
	        but.setVisibility(View.VISIBLE);
	        but.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// Allow submission of ratings via a new activity
	                Intent intent = new Intent(parent.getContext(), SubmitActivity.class);
	                intent.putExtra(SubmitActivity.RATINGS_AIRPORT, mAirport);
	                intent.putExtra(SubmitActivity.SUBMIT, SubmitActivity.RATINGS);
					parent.getContext().startActivity(intent);
				}
	        });
        }
        else {
	        but.setVisibility(View.INVISIBLE);        	
        }
        
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
