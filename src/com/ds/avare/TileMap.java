/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.content.Context;

/**
 * 
 * @author zkhan
 * A cache of tiles
 */
public class TileMap {

    private BitmapHolder mBitmaps[];
    private BitmapHolder mapA[];
    private BitmapHolder mapB[];
    
    private Context mContext;
    
    private Preferences mPref;
    
    private int mXtiles;
    private int mYtiles;
    
    private int numTiles;
    private int numTiles2;
    
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
        numTiles2 = numTiles * 2;
        mapA = new BitmapHolder[numTiles];
        mapB = new BitmapHolder[numTiles];
        mBitmaps = new BitmapHolder[numTiles2];
        for(int tile = 0; tile < numTiles2; tile++) {
            mBitmaps[tile] = new BitmapHolder();
        }
    }

    /**
     * Find a new tile that is currently neither in mapA or mapB
     * @return
     */
    private int allocateUnusedTile() {
        
        /*
         * For all tiles.
         */
        for(int tilem = 0; tilem < numTiles2; tilem++) {
            
            /*
             * See if this bitmap is part of mapA or mapB
             * If it is then move to next
             */
            BitmapHolder h = mBitmaps[tilem];
            if(null == h) {
                continue;
            }
            String n = h.getName();
            /*
             * No name, this is available
             */
            if(null == n) {
                return tilem;
            }

            /*
             * Check if this tile is part of the maps
             */
            boolean found = false;
            for(int tilen = 0; tilen < numTiles; tilen++) {
                if(null != mapA[tilen]) {
                    if(null != mapA[tilen].getName()) {
                        if(mapA[tilen].getName().equals(n)) {
                            found = true;
                            break;
                        }
                    }
                }
                if(null != mapB[tilen]) {
                    if(null != mapB[tilen].getName()) {
                        if(mapB[tilen].getName().equals(n)) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            
            if(!found) {
                return tilem;
            }
            
            /*
             * Next in cache
             */
        }
        
        return -1;
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

        /*
         * New map for background that will be flipped in UI thread
         */
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
             * Find in cache
             */
            for(int tilem = 0; tilem < numTiles2; tilem++) {
                
                /*
                 * This should never happen because bitmaps always exist but who
                 * knows how the app gets killed.
                 */
                if(mBitmaps[tilem] == null) {
                    continue;
                }
                
                /*
                 * Empty tile, cannot re-use
                 */
                String m = mBitmaps[tilem].getName();
                if(null == m) {
                    continue;
                }
                
                if(tileNames[tilen].equals(m)) {
                    /*
                     * Already loaded tile, re-use it.
                     */
                    mapB[tilen] = mBitmaps[tilem];
                    break;
                }
            }
       
            /*
             * No tile re-used for this. Load new.
             */
            if(null == mapB[tilen]) {
                int tile = allocateUnusedTile();
                if(tile < 0) {
                    /*
                     * Out of memory?
                     * This should not happen
                     */
                    continue;
                }
                mapB[tilen] = mBitmaps[tile];
                BitmapHolder b = new BitmapHolder(mContext, mPref, tileNames[tilen]);
                if(null != b && null != mapB[tilen]) {
                    mapB[tilen].drawInBitmap(b, tileNames[tilen]);
                    b.recycle();
                }
            }
        }
        
        /*
         * Now assign to B
         */
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
        for(int tile = 0; tile < numTiles2; tile++) {
            mBitmaps[tile].recycle();
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
     * @return
     */
    public BitmapHolder[] getTiles() {
        return mapA;
    }
    
    /**
     * 
     * @param tile
     * @return
     */
    public BitmapHolder getTile(int tile) {
        return mapA[tile];
    }
}
