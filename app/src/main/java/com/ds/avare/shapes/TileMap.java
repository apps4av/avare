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
import android.support.v4.util.LruCache;
import android.view.Display;
import android.view.WindowManager;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;



/**
 * 
 * @author zkhan, SteveAtChartbundle
 * A cache of tiles
 */
public class TileMap {

    private BitmapHolder[] mapA;
    private BitmapHolder[] mapB;
    
    private Context mContext;
    
    private Preferences mPref;
    
    private int mXtiles;
    private int mYtiles;
    
    private int mNumTiles;

    private int mNumShowing;

    LruCache<String, BitmapHolder> mBitmapCache;

    private static final int SIZE =  BitmapHolder.HEIGHT * BitmapHolder.WIDTH * 2; // RGB565 = 2

    /**
     * 
     * @param context
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
        mNumShowing = 0;
        mNumTiles = mXtiles * mYtiles;
        mapA = new BitmapHolder[mNumTiles];
        mapB = new BitmapHolder[mNumTiles];
        mBitmapCache = new LruCache<String, BitmapHolder>(SIZE * (mNumTiles + getOverhead()))  {

            @Override
            protected int sizeOf(String key, BitmapHolder value) {
                return SIZE;
            }

            @Override
            protected void entryRemoved (boolean evicted, String key, BitmapHolder oldValue, BitmapHolder newValue) {
                oldValue.recycle();
            }
        };
    }

    /**
     * Overhead needed for smooth tile switching, 1x becomes a double buffer.
     * Warning: Liberal overhead could cause OOM exception because we are talking about pictures of size 512x512.
     * @return
     */
    public int getOverhead() {
        return (int)(0.2 * (double)mNumTiles); // 20% overhead
    }

    /**
     * 
     * Clear the cache.
     * 
     * @return
     */
    public void clear() {
        mBitmapCache.evictAll();
        mNumShowing = 0;
    }

    /*
     * Force a reload.
     */
    public void forceReload() {
        clear();
    }

    /**
     * 
     * When a new string of names are available for a new region, reload
     * will load and reuse older tiles.
     * 
     * @param tileNames
     * @return
     */
    public void reload(String[] tileNames) {

        // how many tiles missing?
        int showing = 0;

        /*
         * For all tiles that will be loaded.
         */
        for(int tilen = 0; tilen < mNumTiles; tilen++) {

            mapB[tilen] = mBitmapCache.get(tileNames[tilen]);

            if(mapB[tilen] == null) {
                mapB[tilen] = new BitmapHolder(mContext, mPref, tileNames[tilen], 1);
                if (mapB[tilen].getBitmap() != null) {
                    synchronized (mBitmapCache) {
                        if (mBitmapCache.get(tileNames[tilen]) == null) {
                            mBitmapCache.put(tileNames[tilen], mapB[tilen]);
                        }
                    }
                    showing++;
                }
            }
            else {
                showing++;
            }
        }
        mNumShowing = showing;
    }

    /**
     * Call this from UI thread so that tiles can be flipped without tear
     */
    public void flip() {
        for(int tilen = 0; tilen < mNumTiles; tilen++) {
            mapA[tilen] = mapB[tilen];
        }
    }
    
    /**
     * 
     */
    public void recycleBitmaps() {
        clear();
    }
    
    /**
     * 
     * @return
     */
    public int getTilesNum() {
        return mNumTiles;
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
    	
    	WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	
        if(display.getHeight() > display.getWidth()) {
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


    /**
     * Lets call chart showing partial when tiles showing are below a threshold
     * @return
     */
    public boolean isChartPartial() {
        return mNumShowing <= 0;
    }

}
