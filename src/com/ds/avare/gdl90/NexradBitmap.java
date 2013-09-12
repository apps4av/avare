/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.gdl90;

import com.ds.avare.utils.BitmapHolder;


public class NexradBitmap {

    private BitmapHolder mBitmap;
    private double mCoords[];
    private double mScaleX;
    private double mScaleY;
    private int mBlock;
    
    /**
     * 
     * @param data
     * @param block
     */
    public NexradBitmap(int data[], int block, boolean conus) {
       
        mBlock = block;
        mCoords = new double[2];
        
        /*
         * Scales are in minutes as well.
         */
        if(conus) {
            mScaleX = 1.5;
            mScaleY = 1;
        }
        else {
            mScaleX = 7.5;
            mScaleY = 5;
        }
        Nexrad.convertBlockNumberToLatLon(block, mCoords);
        
        /*
         * If empty block, do not waste bitmap memory
         */
        if(null == data) {
            mBitmap = null;
            return;        
        }
        else if(data.length < Constants.COLS_PER_BIN * Constants.ROWS_PER_BIN) {
            mBitmap = null;
            return;            
        }
        mBitmap = new BitmapHolder(Constants.COLS_PER_BIN, Constants.ROWS_PER_BIN); // this creates a MUTABLE bitmap
        for(int row = 0; row < Constants.ROWS_PER_BIN; row++) {
            for(int col = 0; col < Constants.COLS_PER_BIN; col++) {
                mBitmap.getBitmap().setPixel(col, row, data[col + row * Constants.COLS_PER_BIN]);
            }
        }
    }
    
    /**
     * 
     */
    public void discard() {
        if(null != mBitmap) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
    
    public double getLatTopLeft() {
        return mCoords[1];
    }
    
    public double getLonTopLeft() {
        return mCoords[0];
    }

    public double getScaleX() {
        return mScaleX / 60.0;
    }
    
    public double getScaleY() {
        return mScaleY / 60.0;
    }    
    
    public BitmapHolder getBitmap() {
        return mBitmap;
    }
    
    public int getBlock() {
        return mBlock;
    }
}
