/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
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
import com.ds.avare.utils.GenericCallback;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zkhan on 9/1/15.
 */
public class MapBase {


    protected BitmapHolder[] mapA;
    protected BitmapHolder[] mapB;

    protected Context mContext;

    protected Preferences mPref;

    protected int mXtiles;
    protected int mYtiles;
    protected int mOverhead;

    protected int mNumTiles;

    private int mSize;

    protected LruCache<String, BitmapHolder> mBitmapCache;


    protected MapBase(Context context, int size, int tilesdim[]) {

        /*
         * Allocate mem for tiles.
         * Keep tiles for the life of activity
         */
        mContext = context;
        mPref = new Preferences(context);
        mSize = size;

        mXtiles = tilesdim[0];
        mYtiles = tilesdim[1];
        mOverhead = tilesdim[2];
        mNumTiles = mXtiles * mYtiles;
        mapA = new BitmapHolder[mNumTiles];
        mapB = new BitmapHolder[mNumTiles];
        mBitmapCache = new LruCache<String, BitmapHolder>(mSize * (mNumTiles + getOverhead())) {

            @Override
            protected int sizeOf(String key, BitmapHolder value) {
                return mSize;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, BitmapHolder oldValue, BitmapHolder newValue) {
                oldValue.recycle();
            }
        };
    }

    /**
     * Overhead needed for smooth tile switching, 1x becomes a double buffer.
     * Warning: Liberal overhead could cause OOM exception because we are talking about pictures of size 512x512.
     *
     * @return
     */
    public int getOverhead() {
        return mOverhead;
    }

    /**
     * Clear the cache.
     *
     * @return
     */
    public void clear() {
        mBitmapCache.evictAll();
    }

    /*
     * Force a reload.
     */
    public void forceReload() {
        clear();
    }

    /**
     * Load tiles in CCW expanding spiral from center tile, just return indexes
     * @return
     */
    private ArrayList<Integer> ccwSpiral(int m, int n) {
        ArrayList<Integer> result = new ArrayList<Integer>();

        int matrix[] = new int[m * n];
        for(int i = 0; i < m * n; i++) {
            matrix[i] = i;
        }

        int left = 0;
        int right = n - 1;
        int top = 0;
        int bottom = m - 1;

        while(result.size() < m * n){
            for(int j = left; j <= right; j++){
                result.add(matrix[top * n + j]);
            }
            top++;

            for(int i = top; i <= bottom; i++){
                result.add(matrix[i * n + right]);
            }
            right--;

            //prevent duplicate row
            if(bottom < top)
                break;

            for(int j = right; j >= left; j--){
                result.add(matrix[bottom * n + j]);
            }
            bottom--;

            // prevent duplicate column
            if(right < left)
                break;

            for(int i = bottom; i >= top; i--){
                result.add(matrix[i * n + left]);
            }
            left++;
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * When a new string of names are available for a new region, reload
     * will load and reuse older tiles.
     *
     * @param tileNames
     * @return
     */
    protected int reloadMap(String[] tileNames, GenericCallback c) {

        // how many tiles missing?
        int showing = 0;

        /*
         * For all tiles that will be loaded.
         */
        for (int tilen = 0; tilen < mNumTiles; tilen++) {

            mapB[tilen] = mBitmapCache.get(tileNames[tilen]);

            if (mapB[tilen] == null) {
                mapB[tilen] = new BitmapHolder(mContext, mPref, tileNames[tilen], 1);
                if (mapB[tilen].getBitmap() != null) {
                    c.callback(this, mapB[tilen]);
                    showing++;
                }
            } else {
                showing++;
            }
        }
        return showing;
    }

    /**
     * Call this from UI thread so that tiles can be flipped without tear
     */
    public void flip() {
        for (int tilen = 0; tilen < mNumTiles; tilen++) {
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
     * @return
     */
    public int getTilesNum() {
        return mNumTiles;
    }

    /**
     * @return
     */
    public int getXTilesNum() {
        return mXtiles;
    }

    /**
     * @return
     */
    public int getYTilesNum() {
        return mYtiles;
    }

    /**
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

        if (display.getHeight() > display.getWidth()) {
            /*
             * Have more tiles in Y on portrait
             */
            if (mXtiles > mYtiles) {
                int tmp = mXtiles;
                mXtiles = mYtiles;
                mYtiles = tmp;
            }
        } else {
            /*
             * Have more tiles in X on landscape
             */
            if (mYtiles > mXtiles) {
                int tmp = mXtiles;
                mXtiles = mYtiles;
                mYtiles = tmp;
            }
        }
    }


    // deal with LRU cache in UI thread, this class will call into UI thread through generic callback when a tile is loaded,
    // then the addInCache will be called by UI thread to add tile in cache, and invalidate view
    // deal with LRU cache in UI thread
    public void addInCache(BitmapHolder h) {
        if (mBitmapCache.get(h.getName()) == null) {
            mBitmapCache.put(h.getName(), h);
        }
    }

    public boolean isChartPartial() {
        return true;
    }



}