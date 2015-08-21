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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Origin;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private float mLonL;
    private float mLatU;
    private float mLonR;
    private float mLatD;
    private long mDate;

    private Preferences mPref;
    private Context mContext;
    private String mImage;
    private String mText;
    
    /**
     * 
     * @param ctx
     */
    public Radar(Context ctx) {
        mContext = ctx;
        mPref = new Preferences(mContext);
        mLonR = mLatU = mLonL = mLatD = 0;
        mDate = 0;
        mBitmap = null;
    }

    /**
     * Parse lon / lat from line in nexrad
     * @param line
     * @return
     */
    private Coordinate parseLonLat(String line) {

        //127d37'45.70"W, 50d24'56.20"N
        double lon, lat;
        line = line.replaceAll(" ", "");
        String tokens[] = line.split("[d'\",]");
        try {
            lon = Double.parseDouble(tokens[0]) + Double.parseDouble(tokens[1]) / 60.0 + Double.parseDouble(tokens[2]) / 3600.0;
            if (tokens[3].equals("W")) {
                lon = -lon;
            }
            lat = Double.parseDouble(tokens[4]) + Double.parseDouble(tokens[5]) / 60.0 + Double.parseDouble(tokens[6]) / 3600.0;
            if (tokens[7].equals("S")) {
                lat = -lat;
            }
        }
        catch (Exception e) {
            return null;
        }

        return new Coordinate(lon, lat);
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
                 * Read lon/lat/date
                 */
                String line = br.readLine();
                Coordinate c = parseLonLat(line);
                if(c == null) {
                    return;
                }
                mLatU = (float)c.getLatitude();
                mLonL = (float)c.getLongitude();
                line = br.readLine();
                line = br.readLine();
                line = br.readLine();
                c = parseLonLat(line);
                if(c == null) {
                    return;
                }
                mLatD = (float)c.getLatitude();
                mLonR = (float)c.getLongitude();

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
        mLonR = mLatU = mLonL = mLatD = 0;
        mDate = 0;
    }
    
    /**
     * Draw on map screen
     */
    public void draw(Canvas canvas, Paint paint, Origin origin) {
        
        if(null == mBitmap) {
            return;
        }
        if(null == mBitmap.getBitmap()) {
            return;
        }
        
        float x0 = (float)origin.getOffsetX(mLonL);
        float y0 = (float)origin.getOffsetY(mLatU);
        float x1 = (float)origin.getOffsetX(mLonR);
        float y1 = (float)origin.getOffsetY(mLatD);

        /*
         * Stretch out the image to fit the projection of US 48
         */
        float scalex = (float)(x1 - x0) / mBitmap.getWidth();
        float scaley = (float)(y1 - y0) / mBitmap.getHeight();
        mBitmap.getTransform().setScale(scalex, scaley);
        mBitmap.getTransform().postTranslate(x0, y0);

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
