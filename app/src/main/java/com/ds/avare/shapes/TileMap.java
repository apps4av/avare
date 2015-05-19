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

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;

import android.content.Context;
import android.graphics.Color;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashMap;


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
    
    private int numTiles;
    private int numTilesMax;
    
    private BitmapHolder[] mBitmapCache;
    private BitmapHolder[] mFreeList;
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
        numTilesMax = mXtiles * mYtiles;
        mapA = new BitmapHolder[numTiles];
        mapB = new BitmapHolder[numTiles];
        mBitmapCache = new BitmapHolder[numTilesMax];
        mFreeList = new BitmapHolder[numTilesMax];
        for(int tile = 0; tile < numTilesMax; tile++) {
            mBitmapCache[tile] = new BitmapHolder();
        }
    }

    /**
     * 
     * Clear the cache.
     * 
     * @return
     */
    public void clear() {
    }

    /*
     * Force a reload.
     */
    public void forceReload() {
        for(int tile = 0; tile < numTilesMax; tile++) {
            if(mBitmapCache[tile] != null) {
                if(mBitmapCache[tile].getName() != null) {
                    mBitmapCache[tile].drawInBitmap(null, null, 0, 0);
                }
            }
        }
    }

    /**
     * 
     * When a new string of names are available for a new region, reload
     * will load and reuse older tiles.
     * 
     * @param name
     * @return
     * @throws InterruptedException 
     */
    public void reload(String[] tileNames) throws InterruptedException {
    	HashMap<String,BitmapHolder> hm = new HashMap<String,BitmapHolder> ();
    	int freeIndex = 0;
        mapB = new BitmapHolder[numTiles];
        /* 
         * Initial setup, mark all as candidates for the freelist.
         * Next section will mark the used ones.
         * Also populate the hashmap for fast name->BitmapHolder mapping
         */
        
        for (int tilen = 0 ; tilen < numTilesMax ; tilen++ ) {
        	mFreeList[tilen] = null;
        	if (mBitmapCache[tilen] != null) {
        		mBitmapCache[tilen].setFree(true);
        		if (mBitmapCache[tilen].getName() != null ){
        			hm.put(mBitmapCache[tilen].getName(), mBitmapCache[tilen]);
        		}
        	}
        }
        /*
         * For all tiles that will be re-used, find from cache.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
        	/* 
        	 * Setup for later mark as not free.
        	 */
            mapB[tilen] = hm.get(tileNames[tilen]);
            if (mapB[tilen] != null) {
            	mapB[tilen].setFree(false);
            }
        }
        /*
         * Build the list of free tiles based on the flags
         */
        for (int tilen = 0 ; tilen < numTilesMax ; tilen++ ) {
        	if (mBitmapCache[tilen] != null && mBitmapCache[tilen].getFree()) {
        		mFreeList[freeIndex] = mBitmapCache[tilen];
        		freeIndex++;
        	}
        }

        /*
         * For all tiles that will be loaded.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
            
            /*
             * Move beyond the move? interrupt.
             */
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            if(null == tileNames[tilen]) {
                /*
                 * Map out?
                 */
                continue;
            }
            
            if(null != mapB[tilen]) {
                /*
                 * This is reused
                 */
                continue;
            }
            /*
             * Pull a free bitmap off the list
             */
            BitmapHolder h = null;
            if (freeIndex > 0 ) {
            	freeIndex--;
            	h = mFreeList[freeIndex];
            }
            if(h != null) {
                /*
                 * At max scale, down sample by down sampling 
                 */
                BitmapHolder b = new BitmapHolder(mContext, mPref, tileNames[tilen], 1);
                if(b.getBitmap() == null) {
                    h.setFound(false);
                }
                else {
                    h.setFound(true);
                }
                h.getBitmap().eraseColor(Color.GRAY);
                h.drawInBitmap(b, tileNames[tilen], 0, 0);
                b.recycle();
                b = null;
                mapB[tilen] = h;
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
        for(int tile = 0; tile < numTilesMax; tile++) {
            mBitmapCache[tile].recycle();
            mBitmapCache[tile] = null;
        }
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

}
