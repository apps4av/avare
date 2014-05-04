/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.position;

import com.ds.avare.utils.BitmapHolder;

/**
 * 
 * @author zkhan
 *
 */
public class Pan {

    /**
     * Current x mMovement pan
     */
    private float                      mMoveX;
    /**
     * Current y mMovement pan
     */
    private float                      mMoveY;

    /**
     * X and Y params for tiles outside of current area.
     * For panning beyond current tiles.
     */
    private int                        mMoveXTile;
    private int                        mMoveYTile;
    private float                      mMoveXTileOld;
    private float                      mMoveYTileOld;

    /**
     * 
     */
    public Pan() {
        mMoveX = 0;
        mMoveY = 0;
        mMoveXTile = 0;
        mMoveYTile = 0;
        mMoveXTileOld = 0;
        mMoveYTileOld = 0;
    }

    /**
     * 
     * 
     * @param p
     */
    public Pan(Pan p) {
        mMoveX = p.mMoveX;
        mMoveY = p.mMoveY;
        mMoveXTile = p.mMoveXTile;
        mMoveYTile = p.mMoveYTile;
        mMoveXTileOld = p.mMoveXTileOld;
        mMoveYTileOld = p.mMoveYTileOld;        
    }
    
    /**
     * 
     * @param x
     * @param y
     * @return
     */
    public boolean setMove(float x, float y) {
        mMoveX = x;
        mMoveY = y;
        
        /*
         * Find if moving out of current area.
         */
        boolean update = false;
        
        /*
         * Update tiles if we pan outside of current area.
         * But do not update right now so we reduce tearing.
         */
        float mMoveXTilexOld = -(int)Math.round(mMoveX / BitmapHolder.WIDTH);
        if(mMoveXTilexOld != mMoveXTileOld) {
            mMoveXTileOld = mMoveXTilexOld;
            update = true;
        }
        int mMoveYTileyOld = -(int)Math.round(mMoveY / BitmapHolder.HEIGHT);
        if(mMoveYTileyOld != mMoveYTileOld) {
            mMoveYTileOld = mMoveYTileyOld;
            update = true;
        }

        return update;
    }

    /**
     * 
     * @param x
     * @param y
     */
    public void setTileMove(int x, int y) {
        mMoveXTile = x;
        mMoveYTile = y;
    }
    
    /**
     * 
     * @return
     */
    public float getMoveX() {
        return mMoveX;
    }

    /**
     * 
     * @return
     */
    public float getMoveY() {
        return mMoveY;
    }

    /**
     * 
     * @return
     */
    public int getTileMoveX() {
        return mMoveXTile;
    }

    /**
     * 
     * @return
     */
    public int getTileMoveY() {
        return mMoveYTile;
    }

    /**
     * 
     * @return
     */
    public int getTileMoveXWithoutTear() {
        return (int)mMoveXTileOld;
    }

    /**
     * 
     * @return
     */
    public int getTileMoveYWithoutTear() {
        return (int)mMoveYTileOld;
    }
}
