/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * 
 * @author zkhan
 * A cache of tiles
 */
public class TileMap {

    private LruCache<String, Bitmap> mBitmapCache;
    
    
    private BitmapHolder[] mapA;
    private BitmapHolder[] mapB;
    
    private Context mContext;
    
    private Preferences mPref;
    
    private int mXtiles;
    private int mYtiles;
    
    private int numTiles;
    private int numTilesMax;
    
    private int mNumRem;
    
    /**
     * 
     * @param x
     * @param y
     */
    public TileMap(Context context) {
        /*
         * Allocate mem for tiles.
         * Keep tiles for the life of activity
         */
        mContext = context;
        mPref = new Preferences(context);

        int[] tilesdim = mPref.getTilesNumber();
        mXtiles = tilesdim[0];
        mYtiles = tilesdim[1];
        numTiles = mXtiles * mYtiles;
        numTilesMax = mXtiles * mYtiles + mXtiles + mYtiles; /* Only one row and one col can go out dated at once */
        mapA = new BitmapHolder[numTiles];
        mapB = new BitmapHolder[numTiles];
        mNumRem = 0;
        mBitmapCache = new LruCache<String, Bitmap>(numTilesMax * BitmapHolder.WIDTH * BitmapHolder.HEIGHT * 2) {
            /**
             * 
             */
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return BitmapHolder.WIDTH * BitmapHolder.HEIGHT * 2;
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
                oldBitmap.recycle();
                oldBitmap = null;
                mNumRem++;
                if(mNumRem >= numTilesMax) {
                    /*
                     * GC is required for some older phones
                     */
                    System.gc();
                    mNumRem = 0;
                }
            }
        };
    }

    /**
     * 
     * Clear the cache.
     * 
     * @return
     */
    public void clear() {
    }

    
    /**
     * 
     * When a new string of names are available for a new region, reload
     * will load and reuse older tiles.
     * 
     * @param name
     * @return
     */
    public void reload(String[] tileNames) {

        mapB = new BitmapHolder[numTiles];
        
        /*
         * For all tiles that will be re-used, find from cache.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
                        
            mapB[tilen] = null;
            
            /*
             * Only happens when out of chart?
             */
            if(null == tileNames[tilen]) {
                continue;
            }

            /*
             * Put in cache
             */
            Bitmap m = mBitmapCache.get(tileNames[tilen]);
            if (m == null) {
                BitmapHolder b = new BitmapHolder(mContext, mPref, tileNames[tilen]);
                m = b.getBitmap();
                if(m != null) {
                    mBitmapCache.put(tileNames[tilen], m);
                }
            } 

            /*
             * Tack a BitmapHolder header on top.
             */
            if(null != m) {
                mapB[tilen] = new BitmapHolder(m, tileNames[tilen]);
            }
        }        
    }

    /**
     * Call this from UI thread so that tiles can be flipped without tear
     */
    public void flip() {
        mapA = mapB;
    }
    
    /**
     * 
     */
    public void recycleBitmaps() {
        mBitmapCache.evictAll();
    }
    
    /**
     * 
     * @return
     */
    public int getTilesNum() {
        return numTiles;
    }
    
    /**
     * 
     * @return
     */
    public int getXTilesNum() {
        return mXtiles;
    }
    
    /**
     * 
     * @return
     */
    public int getYTilesNum() {
        return mYtiles;
    }
    
    /**
     * 
     * @param tile
     * @return
     */
    public BitmapHolder getTile(int tile) {
        return mapA[tile];
    }
    
    /**
     * Set the correct tile orientation
     */
    public void setOrientation() {
        if((new Preferences(mContext)).isPortrait()) {
            /*
             * Have more tiles in Y on portrait
             */
            if(mXtiles > mYtiles) {
                int tmp = mXtiles;
                mXtiles = mYtiles;
                mYtiles = tmp;
            }
        }
        else {
            /*
             * Have more tiles in X on landscape
             */
            if(mYtiles > mXtiles) {
                int tmp = mXtiles;
                mXtiles = mYtiles;
                mYtiles = tmp;
            }            
        }
    }
}
