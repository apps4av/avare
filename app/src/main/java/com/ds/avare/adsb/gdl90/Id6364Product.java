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
public class Id6364Product extends Product {

    private Nexrad mNexrad;
    private boolean mConus;

    public Id6364Product() {
        super(ProductType.PRODUCT_TYPE_NEXRAD);
        setConus(false);
    }

    public boolean isConus() {
        return mConus;
    }

    public void setConus(boolean conus) {
        mConus = conus;
    }

    @Override
    protected void parse(byte[] msg) {
        mNexrad = new Nexrad();
        mNexrad.parse(msg);
    }

    /**
     * @return
     */
    public int getBlockNumber() {
        return mNexrad.getBlock();
    }

    /**
     * @return
     */
    public int[] getData() {
        return mNexrad.getData();
    }

    /**
     * @return
     */
    public LinkedList<Integer> getEmpty() {
        return mNexrad.getEmpty();
    }

}
