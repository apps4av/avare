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

/**
 * 
 * @author zkhan
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
    
    private BitmapHolder mNoImg;
        
    private BitmapHolder[] mBitmapCache;
    
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
        mNoImg = new BitmapHolder(context, R.drawable.nochart);
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

    /**
     * 
     * @param name
     * @return
     */
    private BitmapHolder findTile(String name) {
        if(null == name) {
            return null;
        }
        for(int tile = 0; tile < numTilesMax; tile++) {
            if(mBitmapCache[tile] != null) {
                if(mBitmapCache[tile].getName() != null) {
                    if(mBitmapCache[tile].getName().equals(name)) {
                        return(mBitmapCache[tile]);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param name
     * @return
     */
    private BitmapHolder findTileNotInMapB() {
        boolean found;
        for(int tile = 0; tile < numTilesMax; tile++) {
            if(mBitmapCache[tile] == null) {
                continue;
            }
            found = false;
            for(int tileb = 0; tileb < numTiles; tileb++) {
                if(mapB[tileb] == null) {
                    continue;
                }
                if(mapB[tileb].getName() == null) {
                    continue;
                }
                if(mBitmapCache[tile].getName() == null) {
                    return(mBitmapCache[tile]);
                }
                if(mBitmapCache[tile].getName().equals(mapB[tileb].getName())) {
                    found = true;
                }
            }
            if(!found) {
                return(mBitmapCache[tile]);
            }
        }
        return null;
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

        mapB = new BitmapHolder[numTiles];
        
        /*
         * For all tiles that will be re-used, find from cache.
         */
        for(int tilen = 0; tilen < numTiles; tilen++) {
            if(force) {
                /*
                 * Discard everything
                 */
                mapB[tilen] = null;
                mapA[tilen].getBitmap().eraseColor(0);
                mapA[tilen].drawInBitmap(null, null, 0, 0);
            }
            else {
                mapB[tilen] = findTile(tileNames[tilen]);                
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
            
            BitmapHolder h = findTileNotInMapB();
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
