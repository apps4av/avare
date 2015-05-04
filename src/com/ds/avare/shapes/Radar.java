/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.shapes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.ds.avare.position.Origin;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

/**
 * 
 * @author zkhan
 *
 * Internet radar
 *
 */
public class Radar {

    private static final long EXPIRES = 1000 * 60 * 60 * 2; // 2 hours
    
    private BitmapHolder mBitmap;
    private float mLon;
    private float mLat;
    private float mPx;
    private float mPy;
    private long mDate;

    private Preferences mPref;
    private Context mContext;
    private String mImage;
    private String mText;
    
    /**
     * 
     * @param ctx
     * @param pref
     */
    public Radar(Context ctx) {
        mContext = ctx;
        mPref = new Preferences(mContext);
        mLon = mLat = mPx = mPy = 0;
        mDate = 0;
        mBitmap = null;
    }
    
    /**
     * 
     */
    public void parse() {
        mImage = mPref.mapsFolder() + "/" + "latest_radaronly.png";
        mText = mPref.mapsFolder() + "/" + "latest.txt";

        if(new File(mText).exists() && new File(mImage).exists()) {

            try {
                BufferedReader br;
                br = new BufferedReader(new FileReader(mText));
                
                /*
                 * Read lon/lat/px/py/date
                 */
                String line = br.readLine();
                mPx = Float.parseFloat(line);

                line = br.readLine();
                mPy = Float.parseFloat(line);
                
                line = br.readLine();
                mLon = Float.parseFloat(line);

                line = br.readLine();
                mLat = Float.parseFloat(line);

                String dateText = br.readLine();
                br.close();
                if(mBitmap != null) {
                    mBitmap.recycle();                
                }
                
                mBitmap = new BitmapHolder(mImage);
                
                /*
                 * Date format YYYYMMDD_HHmm
                 */
                Date date = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).parse(dateText);
                mDate = date.getTime();
            }
            catch (Exception e) {
                return;
            }
        }
    }
    
    /**
     * 
     */
    public void flush() {
        if(mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }        
        mLon = mLat = mPx = mPy = 0;
        mDate = 0;
    }
    
    /**
     * Draw on map screen
     */
    public void draw(Canvas canvas, Paint paint, Origin origin, Scale scale, float px, float py) {
        
        if(null == mBitmap) {
            return;
        }
        if(null == mBitmap.getBitmap()) {
            return;
        }
        
        float x = (float)origin.getOffsetX(mLon);
        float y = (float)origin.getOffsetY(mLat);                        
   
        /*
         * The main image is 40% of the coordinates, hence 0.4
         */
        float scalex = (float)(mPx * (1 / 0.4) / px);
        float scaley = (float)(mPy * (1 / 0.4) / py);
        mBitmap.getTransform().setScale(scalex * scale.getScaleFactor(), 
                scaley * scale.getScaleCorrected());
        mBitmap.getTransform().postTranslate(x, y);

        canvas.drawBitmap(mBitmap.getBitmap(), mBitmap.getTransform(), paint);
    }
    
    /**
     * 
     * @return
     */
    public boolean isOld() {
        long diff = Helper.getMillisGMT();
        diff -= mDate; 
        if(diff > EXPIRES) {
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @return
     */
    public String getDate() {
    	if(mDate == 0) {
    		return null;
    	}
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()); 
        return formatter.format(new Date(mDate)) + "Z";
    }
}
