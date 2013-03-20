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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;

import android.content.Context;

/**
 * 
 * @author zkhan
 * A cache of tiles
 */
public class TileMap {

    private HashMap<String, BitmapHolder> mBitmaps;
    
    private BitmapHolder[] mapA;
    private BitmapHolder[] mapB;
    
    private Context mContext;
    
    private Preferences mPref;
    
    private int mXtiles;
    private int mYtiles;
    
    private int numTiles;
    private int numTilesMax;
    
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
        mBitmaps = new HashMap<String, BitmapHolder>(); 
        int[] tilesdim = mPref.getTilesNumber();
        mXtiles = tilesdim[0];
        mYtiles = tilesdim[1];
        numTiles = mXtiles * mYtiles;
        numTilesMax = mXtiles * mYtiles + mXtiles + mYtiles;
        mapA = new BitmapHolder[numTiles];
        mapB = new BitmapHolder[numTiles];
    }

    /**
     * 
     * Clear the cache.
     * 
     * @return
     */
    public void clear() {

        Iterator<Entry<String, BitmapHolder>> it = mBitmaps.entrySet().iterator();
        
        /*
         * For all tiles that will be re-used, find from cache.
         */
        while(it.hasNext()) {
            HashMap.Entry<String, BitmapHolder> pairs = (HashMap.Entry<String, BitmapHolder>)it.next();
            if(null == pairs.getValue()) {
                continue;
            }
            pairs.getValue().drawInBitmap(null, null);
        }
    }

    /*
     * Delete some tiles based on LRU
     */
    private void makeSpace(String tileNames[]) {
        
        /*
         * Now delete a tile
         */
        
        Iterator<Entry<String, BitmapHolder>> it = mBitmaps.entrySet().iterator();
        
        boolean deleted = false;
        
        /*
         * For all tiles that will be re-used, find from cache.
         */
        while(it.hasNext()) {
            HashMap.Entry<String, BitmapHolder> pairs = (HashMap.Entry<String, BitmapHolder>)it.next();
            BitmapHolder b = pairs.getValue();
            String name = pairs.getKey();
            if(null == name || null == b) {
                it.remove();
                continue;
            }
            boolean found = false;
            
            /*
             * See if this tile is in new to be loaded tiles. 
             * If yes, then this cannot be deleted.
             */
            for(int tilen = 0; tilen < numTiles; tilen++) {
                if(tileNames[tilen] != null) {
                    if(name.equals(tileNames[tilen])) {
                        found = true;
                    }
                }
                if(mapA[tilen] != null) {
                    if(mapA[tilen].getName() != null) {
                        if(name.equals(mapA[tilen].getName())) {
                            found = true;
                        }
                    }
                }
            }

            if(!found) {
                /*
                 * Delete this tile
                 */
                deleted = true;
                b.recycle();
                it.remove();
            }
        }
        
        /*
         * Nothing deleted so now delete even mapA
         */
        if(!deleted) {
            /*
             * 
             */
            while(it.hasNext()) {
                HashMap.Entry<String, BitmapHolder> pairs = (HashMap.Entry<String, BitmapHolder>)it.next();
                BitmapHolder b = pairs.getValue();
                String name = pairs.getKey();
                if(null == name || null == b) {
                    it.remove();
                    continue;
                }
                boolean found = false;
                
                /*
                 * See if this tile is in new to be loaded tiles. 
                 * If yes, then this cannot be deleted.
                 */
                for(int tilen = 0; tilen < numTiles; tilen++) {
                    if(tileNames[tilen] != null) {
                        if(name.equals(tileNames[tilen])) {
                            found = true;
                        }
                    }
                }
                
                if(!found) {
                    /*
                     * Delete this tile
                     */
                    deleted = true;
                    b.recycle();
                    it.remove();
                }
            }
        }
        
        System.gc();
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

            BitmapHolder h = mBitmaps.get(tileNames[tilen]);
            if(null != h) {
                mapB[tilen] = h;
            }
            else {
                
                /*
                 * If cache limit reached, delete some tiles
                 */
                if(mBitmaps.size() >= numTilesMax) {
                    makeSpace(tileNames);
                }
                
                /*
                 * No tile re-used for this. Load new.
                 */
                BitmapHolder b = new BitmapHolder(mContext, mPref, tileNames[tilen]);
                if(null != b) {
                    mapB[tilen] = b;
                    mBitmaps.put(tileNames[tilen], b);
                }
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
        Iterator<Entry<String, BitmapHolder>> it = mBitmaps.entrySet().iterator();
        
        /*
         * For all tiles that will be re-used, find from cache.
         */
        while(it.hasNext()) {
            HashMap.Entry<String, BitmapHolder> pairs = (HashMap.Entry<String, BitmapHolder>)it.next();
            if(null == pairs.getValue()) {
                continue;
            }
            pairs.getValue().recycle();
            it.remove();
        }
        
        System.gc();
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
