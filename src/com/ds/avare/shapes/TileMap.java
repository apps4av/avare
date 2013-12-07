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

import com.ds.avare.R;
import android.content.Context;
import java.util.HashMap;


/**
 * 
 * @author zkhan
 * A cache of tiles
 */
public class TileMap {

    private BitmapHolder[] mapA;
    private BitmapHolder[] mapB;
    
    private BitmapHolder mElevBitmap;
    
    private Context mContext;
    
    private Preferences mPref;
    
    private int mXtiles;
    private int mYtiles;
    
    private int numTiles;
    private int numTilesMax;
    
    private BitmapHolder mNoImg;
        
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
        mNoImg = new BitmapHolder(context, R.drawable.nochart);
        for(int tile = 0; tile < numTilesMax; tile++) {
            mBitmapCache[tile] = new BitmapHolder();
        }
        mElevBitmap = new BitmapHolder();
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
     */
    public void reload(String[] tileNames, boolean force) {
    	HashMap<String,BitmapHolder> hm = new HashMap<String,BitmapHolder> ();
    	int freeIndex = 0;
        mapB = new BitmapHolder[numTiles];
        /* 
         * Initial setup, mark all as candidates for the freelist.
         * Next section will mark the used ones.
         * Also populate the hashmap for fast name->BitmapHolder mapping
         */
        
        for (int tilen = 0 ; tilen < numTilesMax ; tilen++ ) {
        	mFreeList[tilen]=null;
        	if (mBitmapCache[tilen] != null) {
        		mBitmapCache[tilen].setFree(1);
        		if (mBitmapCache[tilen].getName() != null ){
        			hm.put(mBitmapCache[tilen].getName(),mBitmapCache[tilen]);
        		}
        	}
        }
        /*
         * For all tiles that will be re-used, find from cache.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
            if(force) {
                /*
                 * Discard everything
                 */
                mapB[tilen] = null;
                if(mapA[tilen] != null) {
                    if(mapA[tilen].getBitmap() != null) {
                        mapA[tilen].getBitmap().eraseColor(0);
                    }
                    mapA[tilen].drawInBitmap(null, null, 0, 0);    
                }
            }
            else {
            	/* 
            	 * Setup for later mark as not free.
            	 */
                mapB[tilen] = hm.get(tileNames[tilen]);
                if (mapB[tilen] != null) {
                	mapB[tilen].setFree(0);
                }
            }
        }
        /*
         * Build the list of free tiles based on the flags
         */
        for (int tilen = 0 ; tilen < numTilesMax ; tilen++ ) {
        	if (mBitmapCache[tilen] != null && mBitmapCache[tilen].getFree() == 1) {
        		mFreeList[freeIndex] = mBitmapCache[tilen];
        		freeIndex++;
        	}
        }

        /*
         * For all tiles that will be loaded.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
            
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
                if(b.getName() == null) {
                    h.drawInBitmap(mNoImg, tileNames[tilen], 0, 0);
                }
                else {
                    h.drawInBitmap(b, tileNames[tilen], 0, 0);
                    b.recycle();
                    b = null;
                }
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
        mNoImg.recycle();
        mElevBitmap.recycle();
        mNoImg = null;
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
        if((new Preferences(mContext)).getOrientation().contains("Portrait")) {
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
     * 
     */
    public BitmapHolder getElevationBitmap() {
        if(mElevBitmap.getName() == null) {
            return null;
        }
        return mElevBitmap;
    }

    /**
     * 
     */
    public void setElevationTile(Tile t) {
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
        BitmapHolder b = new BitmapHolder(mContext, mPref, t.getName(), 1);
        if(b.getName() == null) {
            return;
        }
        else {
            mElevBitmap.drawInBitmap(b, t.getName(), 0, 0);
            b.recycle();
            b = null;
        }

    }
}
