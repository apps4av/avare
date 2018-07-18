/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.adsb.gdl90;

import java.util.LinkedList;

/**
 * @author zkhan
 */
public class Nexrad {

    public static final int INTENSITY[] = {
            0x00000000,
            0x00000000,
            0xFF007F00, // dark green
            0xFF00AF00, // light green
            0xFF00FF00, // lighter green
            0xFFFFFF00, // yellow
            0xFFFF7F00, // orange
            0xFFFF0000  // red
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
     * Parse graphics
     */
    public void parse(byte msg[]) {
        /*
         * Get blocks, skip first 3.
         */
        boolean elementIdentifier = (((int) msg[0]) & 0x80) != 0; // RLE or Empty?

        int len = msg.length;
        mBlock = ((int) msg[0] & 0x0F) << 16;
        mBlock += (((int) msg[1] & 0xFF) << 8);
        mBlock += (int) msg[2] & 0xFF;

        int index = 3;

        /*
         * Decode blocks RLE encoded
         */
        if (elementIdentifier) {
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
            while (index < len) {
                int numberOfBins = ((msg[index] & 0xF8) >> 3) + 1;
                for (i = 0; i < numberOfBins; i++) {
                    if (j >= mData.length) {
                        /*
                         * Some sort of error.
                         */
                        mData = null;
                        return;
                    }
                    mData[j] = INTENSITY[(msg[index] & 0x07)];
                    j++;
                }
                index++;
            }
        } else {
            /*
             * Make a list of empty blocks
             */
            mData = null;
            mEmpty = new LinkedList<Integer>();
            mEmpty.add(mBlock);
            int bitmaplen = (int) msg[index] & 0x0F;

            if (((int) msg[index] & 0x10) != 0) {
                mEmpty.add(mBlock + 1);
            }

            if (((int) msg[index] & 0x20) != 0) {
                mEmpty.add(mBlock + 2);
            }

            if (((int) msg[index] & 0x30) != 0) {
                mEmpty.add(mBlock + 3);
            }

            if (((int) msg[index] & 0x40) != 0) {
                mEmpty.add(mBlock + 4);
            }

            for (int i = 1; i < bitmaplen; i++) {
                if (((int) msg[index + i] & 0x01) != 0) {
                    mEmpty.add(mBlock + i * 8 - 3);
                }

                if (((int) msg[index + i] & 0x02) != 0) {
                    mEmpty.add(mBlock + i * 8 - 2);
                }

                if (((int) msg[index + i] & 0x04) != 0) {
                    mEmpty.add(mBlock + i * 8 - 1);
                }

                if (((int) msg[index + i] & 0x08) != 0) {
                    mEmpty.add(mBlock + i * 8 - 0);
                }

                if (((int) msg[index + i] & 0x10) != 0) {
                    mEmpty.add(mBlock + i * 8 + 1);
                }

                if (((int) msg[index + i] & 0x20) != 0) {
                    mEmpty.add(mBlock + i * 8 + 2);
                }

                if (((int) msg[index + i] & 0x40) != 0) {
                    mEmpty.add(mBlock + i * 8 + 3);
                }

                if (((int) msg[index + i] & 0x80) != 0) {
                    mEmpty.add(mBlock + i * 8 + 4);
                }
            }
        }
    }

    /**
     * @return
     */
    public int getBlock() {
        return mBlock;
    }

    /**
     * @return
     */
    public int[] getData() {
        return mData;
    }

    /**
     * @return
     */
    public LinkedList<Integer> getEmpty() {
        return mEmpty;
    }
}
