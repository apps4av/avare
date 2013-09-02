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

import android.util.SparseArray;

/**
 * 
 * @author zkhan
 *
 */
public class NexradImage {

    SparseArray<NexradBitmap> mImg;
    
    public NexradImage() {
        mImg = new SparseArray<NexradBitmap>();
    }
    
    /**
     * 
     * @param product
     */
    public void putImg(Id6364Product product) {
        
        int data[] = product.getData();
        LinkedList<Integer> empty = product.getEmpty();
        int block = product.getBlockNumber();
        if(null == data && null != empty) {
            /*
             * Empty, make dummy bitmaps of all.
             */
            for(int b : empty) {
                if(mImg.get(b) != null) {
                    /*
                     * Replace same block, but clears the bitmap
                     */
                    mImg.get(b).discard();
                }
                mImg.put((Integer)b, new NexradBitmap(null, b, product.isConus()));
            }
        }
        else {
            if(mImg.get(block) != null) {
                /*
                 * Replace same block
                 */
                mImg.get(block).discard();
            }
            mImg.put((Integer)block, new NexradBitmap(data, block, product.isConus()));
        }
        
    }
    
    /**
     * 
     * @return
     */
    public SparseArray<NexradBitmap> getImages() {
        return mImg;
    }    
}
