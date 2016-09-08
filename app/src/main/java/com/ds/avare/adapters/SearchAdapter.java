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
import android.widget.ImageView;
import android.widget.TextView;

import com.ds.avare.R;
import com.ds.avare.place.Destination;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.BitmapHolder;

/**
 * @author zkhan
 * A list that displays search results.
 */
public class SearchAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private String[] mVals;
    private BitmapHolder mNDBBitmapHolder;
    private BitmapHolder mMakerBitmapHolder;
    private BitmapHolder mNDBDMEBitmapHolder;
    private BitmapHolder mVORBitmapHolder;
    private BitmapHolder mVOTBitmapHolder;
    private BitmapHolder mTACANBitmapHolder;
    private BitmapHolder mVORTACBitmapHolder;
    private BitmapHolder mVORDMEBitmapHolder;
    private BitmapHolder mAirportBitmapHolder;
    private BitmapHolder mFixBitmapHolder;
    private BitmapHolder mNoBitmapHolder;
    private BitmapHolder mGeoBitmapHolder;
    private BitmapHolder mMapBitmapHolder;
    private BitmapHolder mUDWBitmapHolder;
    
    /**
     * @param context
     * @param vals
     */
    public SearchAdapter(Context context, String[] vals) {
        super(context, R.layout.search_list, vals);
        mContext = context;
        mVals = vals;
        /*
         * Load various bitmaps that will be shown
         */
        mNDBBitmapHolder = new BitmapHolder(mContext, R.drawable.ndb);
        mMakerBitmapHolder = new BitmapHolder(mContext, R.drawable.marker);
        mNDBDMEBitmapHolder = new BitmapHolder(mContext, R.drawable.ndbdme);
        mVORBitmapHolder = new BitmapHolder(mContext, R.drawable.vor);
        mVOTBitmapHolder = new BitmapHolder(mContext, R.drawable.vot);
        mTACANBitmapHolder = new BitmapHolder(mContext, R.drawable.tacan);
        mVORTACBitmapHolder = new BitmapHolder(mContext, R.drawable.vortac);
        mVORDMEBitmapHolder = new BitmapHolder(mContext, R.drawable.vordme);
        mAirportBitmapHolder = new BitmapHolder(mContext, R.drawable.airport);
        mNoBitmapHolder = new BitmapHolder(mContext, R.drawable.unknown);
        mFixBitmapHolder = new BitmapHolder(mContext, R.drawable.fix);
        mGeoBitmapHolder = new BitmapHolder(mContext, R.drawable.geo);
        mMapBitmapHolder = new BitmapHolder(mContext, R.drawable.maps);
        mUDWBitmapHolder = new BitmapHolder(mContext, android.R.drawable.ic_dialog_map);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.search_list, parent, false);
        }
        ImageView imgView = (ImageView)rowView.findViewById(R.id.search_list_image);
        TextView textView = (TextView)rowView.findViewById(R.id.search_list_text);
        TextView textView2 = (TextView)rowView.findViewById(R.id.search_list_name);
        
        /*
         * This string is formatted as
         * ID::Base;TYPE;NAME
         */
        String id = StringPreference.parseHashedNameId(mVals[position]); 
        String dbType = StringPreference.parseHashedNameDbType(mVals[position]); 
        String name = StringPreference.parseHashedNameFacilityName(mVals[position]);
        if(id != null && dbType != null && name != null) {
            textView.setText(id);
            textView2.setText(name);
            
            if(dbType.equals("TACAN")) {
                imgView.setImageBitmap(mTACANBitmapHolder.getBitmap());
            }
            else if(dbType.equals("NDB/DME")) {
                imgView.setImageBitmap(mNDBDMEBitmapHolder.getBitmap());
            }
            else if(
                    dbType.equals("MARINE NDB") ||
                    dbType.equals("UHF/NDB") ||
                    dbType.equals("NDB")) {
                imgView.setImageBitmap(mNDBBitmapHolder.getBitmap());
            }
            else if(dbType.equals("VOR/DME")) {
                imgView.setImageBitmap(mVORDMEBitmapHolder.getBitmap());
            }
            else if(dbType.equals("VOT")) {
                imgView.setImageBitmap(mVOTBitmapHolder.getBitmap());
            }
            else if(dbType.equals("VORTAC")) {
                imgView.setImageBitmap(mVORTACBitmapHolder.getBitmap());
            }
            else if(dbType.equals("FAN MARKER")) {
                imgView.setImageBitmap(mMakerBitmapHolder.getBitmap());
            }
            else if(dbType.equals("VOR")) {
                imgView.setImageBitmap(mVORBitmapHolder.getBitmap());
            }
            else if(
                    /*
                     * These are placeholders for future addition for appropriate icons
                     */
                    dbType.equals("AIRPORT") ||
                    dbType.equals("SEAPLANE BAS") ||
                    dbType.equals("HELIPORT") ||
                    dbType.equals("ULTRALIGHT") ||
                    dbType.equals("GLIDERPORT") ||
                    dbType.equals("BALLOONPORT")) {
                imgView.setImageBitmap(mAirportBitmapHolder.getBitmap());
            }
            else if(
                    /*
                     * All strings direct from FAA database
                     */
                    dbType.equals("YREP-PT") ||
                    dbType.equals("YRNAV-WP") ||
                    dbType.equals("NARTCC-BDRY") ||
                    dbType.equals("NAWY-INTXN") ||
                    dbType.equals("NTURN-PT") ||
                    dbType.equals("YWAYPOINT") ||
                    dbType.equals("YMIL-REP-PT") ||
                    dbType.equals("YCOORDN-FIX") ||
                    dbType.equals("YMIL-WAYPOINT") ||
                    dbType.equals("YNRS-WAYPOINT") ||
                    dbType.equals("YVFR-WP") ||
                    dbType.equals("YGPS-WP") ||
                    dbType.equals("YCNF") ||
                    dbType.equals("YRADAR") ||
                    dbType.equals("NDME-FIX") ||
                    dbType.equals("NNOT-ASSIGNED") ||
                    dbType.equals("NDP-TRANS-XING") ||
                    dbType.equals("NSTAR-TRANS-XIN") ||
                    dbType.equals("NBRG-INTXN")) {
                imgView.setImageBitmap(mFixBitmapHolder.getBitmap());
            }
            else if(dbType.equals(Destination.GPS)) {
                imgView.setImageBitmap(mGeoBitmapHolder.getBitmap());
            }
            else if(dbType.equals(Destination.MAPS)) {
                imgView.setImageBitmap(mMapBitmapHolder.getBitmap());
            }
            else if(dbType.equals(Destination.UDW)) {
                imgView.setImageBitmap(mUDWBitmapHolder.getBitmap());
            }
            else {
                /*
                 * Unrecognized, dont show any but take space
                 */
                imgView.setImageBitmap(mNoBitmapHolder.getBitmap());                    
            }
        }

        return rowView;
    }

    
}
