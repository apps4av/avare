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
public class NexradImageConus {
    
    private static final long EXPIRES = 1000 * 60 * 60 * 2; // 2 hours

    /*
     * Northern hemisphere only
     * Cover 0 to 60 degrees latitude
     * With each box of 20 minutes = 60 * 60 / 20 = 180 rows
     * 
     * Cover -180 to 180, longitude
     * With each box 240 minutes = 360 * 60 / 240 = 90 columns
     * 
     * For practical purposes, only cover an area of 60 degree (lon) by 30 degree (lat). This 
     * should be sufficient to have an area of entire USA 48 states
     * 
     * = 30 * 60 / 20 = 90 rows
     * = 60 * 60 / 240 = 15 columns
     * = 1350 entries
     */
    private static final int MAX_ENTRIES = 1350;
    private SparseArray<NexradBitmap> mImg;
    private long mUpdated;
    
    public NexradImageConus() { 
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
    /**
     * 
     * @return
     */
    public String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()); 
        return formatter.format(new Date(mUpdated)) + "Z";
    }

}
