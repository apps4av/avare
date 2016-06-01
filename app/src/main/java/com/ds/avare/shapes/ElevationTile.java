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
import android.graphics.Bitmap;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;

/**
 * 
 * @author zkhan
 *
 */
public class ElevationTile {
    private BitmapHolder mElevBitmap;    
    private Preferences mPref;
    private Tile mTile;

    private Context mContext;

    /**
     * 
     * @param x
     * @param y
     */
    public ElevationTile(Context context) {
        mContext = context;
        mPref = new Preferences(context);
        mElevBitmap = new BitmapHolder(Bitmap.Config.ARGB_8888);
    }
    
    /**
     * 
     */
    public void recycleBitmaps() {
    	if(null == mElevBitmap) {
    		return;
    	}
        mElevBitmap.recycle();
        mElevBitmap = null;
    }

    /**
     * 
     */
    public BitmapHolder getElevationBitmap() {
    	if(null == mElevBitmap) {
    		return null;
    	}
        if(mElevBitmap.getName() == null) {
            return null;
        }
        return mElevBitmap;
    }

    /**
     * 
     */
    public void setElevationTile(Tile t) {
    	if(null == mElevBitmap) {
    		return;
    	}

        if(t == null) {
            return;
        }
        if(t.getName() == null) {
            return;
        }
        
        /*
         * Same tile, do not reload.
         */
        if(mElevBitmap.getName() != null) {
            if(t.getName().equals(mElevBitmap.getName())) {
                return;
            }
        }
        
        /*
         * New tile
         */
        mTile = t;
        BitmapHolder b = new BitmapHolder(mContext, mPref, t.getName(), 1, Bitmap.Config.ARGB_8888);
        if(b.getName() == null) {
            return;
        }
        else {
            mElevBitmap.drawInBitmap(b, t.getName(), 0, 0);
            b.recycle();
            b = null;
        }

    }
    
    /**
     * 
     * @return
     */
    public Tile getTile() {
        return mTile;
    }
}

