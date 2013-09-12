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

import java.util.LinkedList;

/**
 * 
 * @author zkhan
 *
 */
public class Nexrad {

    public static final int INTENSITY[] = {
        0x00000000,
        0xFF00FF00,
        0xFF00FF00,
        0xFFFFFF00,
        0xFFFF0000,
        0xFFFF0000,
        0xFFFF007F,
        0xFFFF00FF
    };    

    private int mBlock;
    private int mData[];
    private LinkedList<Integer> mEmpty;

    public Nexrad() {
        mEmpty = null;
        mData = null;
        mBlock = -1;
    }

    /**
     * 
     * @param blockNumber
     */
    public static void convertBlockNumberToLatLon(int blockNumber, double lonlat[]) {

        /*
         *  Determine lat/lon for block number
         */
        int numberOfBlocksInRing = 0;
        char blockLongitudeWidth = 0;
        char blockLatitudeHeight = 4;
        int completeRings = 0;
        float blocksInPartialRing = 0;
        float fracRings, fracLat, fracLon;
        
        if (blockNumber < 405000) {
            numberOfBlocksInRing = 450;
            blockLongitudeWidth = 48;
        }
        else {
            numberOfBlocksInRing = 225;
            blockLongitudeWidth = 96;
        }
        
        fracRings = (float)blockNumber / (float)numberOfBlocksInRing;
        completeRings = (int)Math.floor(fracRings);
        blocksInPartialRing = (fracRings - completeRings) * numberOfBlocksInRing;
        
        fracLat = (float)completeRings * (float)blockLatitudeHeight / 60.0f;
        lonlat[1] = fracLat;
        
        fracLon = blocksInPartialRing * (float)blockLongitudeWidth / 60.0f;
        if (fracLon > 180) {
            fracLon = 360.0f - fracLon;
        }
        lonlat[0] = -fracLon; // XXX: -ve sign?
    }

    /**
     * Parse graphics
     */
    public void parse(byte msg[]) {
        /*
         * Get blocks, skip first 3.
         */
        boolean elementIdentifier = (((int)msg[0]) & 0x80) != 0; // RLE or Empty?
        
        int len = msg.length;
        mBlock = ((int)msg[0] & 0x0F) << 16;
        mBlock += (((int)msg[1] & 0xFF) << 8);
        mBlock += (int)msg[2] & 0xFF;
  
        int index = 3;
        
        /*
         * Decode blocks RLE encoded
         */
        if(elementIdentifier) {
            mData = new int[Constants.COLS_PER_BIN * Constants.ROWS_PER_BIN];
            mEmpty = null;
            
            /*
             * Each row element is 1 minute (4 minutes total)
             * Each col element is 1.5 minute (48 minutes total)
             */
            for (int i = 0; i < Constants.COLS_PER_BIN * Constants.ROWS_PER_BIN; i++) {
                mData[i] = INTENSITY[0];
            }
            
            int j = 0;
            int i;
            while(index < len) {
                int numberOfBins = ((msg[index] & 0xF8) >> 3) + 1;
                for(i = 0; i < numberOfBins; i++) {
                    mData[j] = INTENSITY[(msg[index] & 0x07)];
                    j++;
                }
                index++;
            }
        }
        else {
            /*
             * Make a list of empty blocks
             */
            mData = null;
            mEmpty = new LinkedList<Integer>();
            mEmpty.add(mBlock);
            int bitmaplen = (int)msg[index] & 0x0F;
            
            if(((int)msg[index] & 0x10) != 0) {
                mEmpty.add(mBlock + 1);
            }
     
            if(((int)msg[index] & 0x20) != 0) {
                mEmpty.add(mBlock + 2);
            }
            
            if(((int)msg[index] & 0x30) != 0) {
                mEmpty.add(mBlock + 3);
            }
            
            if(((int)msg[index] & 0x40) != 0) {
                mEmpty.add(mBlock + 4);
            }
            
            for(int i = 1; i < bitmaplen; i++) {
                if(((int)msg[index + i] & 0x01) != 0) {
                    mEmpty.add(mBlock + i * 8 - 3);
                }

                if(((int)msg[index + i] & 0x02) != 0) {
                    mEmpty.add(mBlock + i * 8 - 2);
                }
                
                if(((int)msg[index + i] & 0x04) != 0) {
                    mEmpty.add(mBlock + i * 8 - 1);
                }
                
                if(((int)msg[index + i] & 0x08) != 0) {
                    mEmpty.add(mBlock + i * 8 - 0);
                }
                
                if(((int)msg[index + i] & 0x10) != 0) {
                    mEmpty.add(mBlock + i * 8 + 1);
                }

                if(((int)msg[index + i] & 0x20) != 0) {
                    mEmpty.add(mBlock + i * 8 + 2);
                }
                
                if(((int)msg[index + i] & 0x40) != 0) {
                    mEmpty.add(mBlock + i * 8 + 3);
                }
                
                if(((int)msg[index + i] & 0x80) != 0) {
                    mEmpty.add(mBlock + i * 8 + 4);
                }
            }
        }
    }

    /**
     * 
     * @return
     */
    public int getBlock() {
        return mBlock;
    }
    
    /**
     * 
     * @return
     */
    public int[] getData() {
        return mData;
    }
    
    /**
     * 
     * @return
     */
    public LinkedList<Integer> getEmpty() {
        return mEmpty;
    }    
}
