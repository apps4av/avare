/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.adsb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ds.avare.utils.Helper;

import android.util.SparseArray;

/**
 * 
 * @author zkhan
 *
 */
public class NexradImage {
    
    private static final long EXPIRES = 1000 * 60 * 60 * 2; // 2 hours

    /*
     * Northern hemisphere only
     * Cover 0 to 60 degrees latitude
     * With each box of 4 minutes = 60 * 60 / 4 = 900 rows
     * 
     * Cover -180 to 180, longitude
     * With each box 48 minutes = 360 * 60 / 48 = 450 columns
     * 
     * For practical purposes, only cover an area of 7.2 degree (lon) by 12 degree (lat). This 
     * should be sufficient to have an area of about 350 miles by 350 miles at 60 latitude, more lower
     * Given regional nexrad is 250 miles by 250 miles, apply this limit
     * 
     * = 12 * 60 / 4 = 180 rows
     * = 7.2 * 60 / 48 = 9 columns
     * = 1620 entries
     */
    private static final int MAX_ENTRIES = 1620;
    private SparseArray<NexradBitmap> mImg;
    private long mUpdated;
    
    public NexradImage() { 
        mImg = new SparseArray<NexradBitmap>();
        mUpdated = 0;
    }
    
    /**
     * 
     * @param product
     */
    public void putImg(long time, int block, int empty[], boolean isConus, int data[], int cols, int rows) {
        
        if(null != empty) {
            /*
             * Empty, make dummy bitmaps of all.
             */
            for(int i = 0; i < empty.length; i++) {
                if(mImg.get(i) != null) {
                    /*
                     * Clears the bitmap and discards it, since nothing draws here.
                     */
                    mImg.get(i).discard();
                    mImg.delete(i);
                }
            }
            mUpdated = time;
        }
        if(null != data) {
            if(mImg.get(block) != null) {
                /*
                 * Replace same block
                 */
                mImg.get(block).discard();
                mImg.delete(block);
            }
            if(mImg.size() > MAX_ENTRIES) {
                /*
                 * Sorry no more space.
                 */
                return;
            }
            mImg.put(block, new NexradBitmap(time, data, block, isConus, cols, rows));
            mUpdated = time;
        }
    }
    
    /**
     * 
     * @return
     */
    public SparseArray<NexradBitmap> getImages() {
        return mImg;
    }
    
    /**
     * 
     * @return
     */
    public boolean isOld() {
        long diff = Helper.getMillisGMT();
        diff -= mUpdated; 
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()); 
        return formatter.format(new Date(mUpdated)) + "Z";
    }

}
