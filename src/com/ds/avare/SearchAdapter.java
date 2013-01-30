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


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
    
    /**
     * @param context
     * @param vals
     */
    public SearchAdapter(Context context, String[] vals) {
        super(context, R.layout.searchlist, vals);
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
        mNoBitmapHolder = new BitmapHolder(mContext, R.drawable.no);
        mFixBitmapHolder = new BitmapHolder(mContext, R.drawable.fix);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(R.layout.searchlist, parent, false);
        }
        ImageView imgView = (ImageView)rowView.findViewById(R.id.imageSearch);
        TextView textView = (TextView)rowView.findViewById(R.id.idSearch);
        TextView textView2 = (TextView)rowView.findViewById(R.id.nameSearch);
        
        /*
         * This string is formatted as
         * ID::Base;TYPE;NAME
         */
        String val[] = mVals[position].split("::");
        if(val.length >= 2) {
            String dst = val[0];
            textView.setText(dst);
            String vals[] = val[1].split(";");
            if(vals.length > 2) {
                textView2.setText(vals[2]);
                if(vals[1].equals("TACAN")) {
                    imgView.setImageBitmap(mTACANBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("NDB/DME")) {
                    imgView.setImageBitmap(mNDBDMEBitmapHolder.getBitmap());
                }
                else if(
                        vals[1].equals("MARINE NDB") ||
                        vals[1].equals("UHF/NDB") ||
                        vals[1].equals("NDB")) {
                    imgView.setImageBitmap(mNDBBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("VOR/DME")) {
                    imgView.setImageBitmap(mVORDMEBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("VOT")) {
                    imgView.setImageBitmap(mVOTBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("VORTAC")) {
                    imgView.setImageBitmap(mVORTACBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("FAN MARKER")) {
                    imgView.setImageBitmap(mMakerBitmapHolder.getBitmap());
                }
                else if(vals[1].equals("VOR")) {
                    imgView.setImageBitmap(mVORBitmapHolder.getBitmap());
                }
                else if(
                        vals[1].equals("AIRPORT") ||
                        vals[1].equals("SEAPLANE BAS") ||
                        vals[1].equals("HELIPORT") ||
                        vals[1].equals("ULTRALIGHT") ||
                        vals[1].equals("GLIDERPORT") ||
                        vals[1].equals("BALLOONPORT")) {
                    imgView.setImageBitmap(mAirportBitmapHolder.getBitmap());
                }
                else if(
                        /*
                         * All strings direct from FAA database
                         */
                        vals[1].equals("YREP-PT") ||
                        vals[1].equals("YRNAV-WP") ||
                        vals[1].equals("NARTCC-BDRY") ||
                        vals[1].equals("NAWY-INTXN") ||
                        vals[1].equals("NTURN-PT") ||
                        vals[1].equals("YWAYPOINT") ||
                        vals[1].equals("YMIL-REP-PT") ||
                        vals[1].equals("YCOORDN-FIX") ||
                        vals[1].equals("YMIL-WAYPOINT") ||
                        vals[1].equals("YNRS-WAYPOINT") ||
                        vals[1].equals("YVFR-WP") ||
                        vals[1].equals("YGPS-WP") ||
                        vals[1].equals("YCNF") ||
                        vals[1].equals("YRADAR") ||
                        vals[1].equals("NDME-FIX") ||
                        vals[1].equals("NNOT-ASSIGNED") ||
                        vals[1].equals("NDP-TRANS-XING") ||
                        vals[1].equals("NSTAR-TRANS-XIN") ||
                        vals[1].equals("NBRG-INTXN")) {
                    imgView.setImageBitmap(mFixBitmapHolder.getBitmap());
                }
                else {
                    /*
                     * Unrecognized, dont show any but take space
                     */
                    imgView.setImageBitmap(mNoBitmapHolder.getBitmap());                    
                }
            }
        }

        return rowView;
    }

    
}
