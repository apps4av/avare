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
 * <p>
 * Accumulates GDL90 messages, joins fragments.
 */
public class DataBuffer {

    int mSize;
    int mElem;
    byte mBuffer[];
    byte mBuffer2[];
    LinkedList<Integer> mIndexes;

    /**
     * @param size
     */
    public DataBuffer(int size) {
        mSize = size;
        mIndexes = new LinkedList<Integer>();
        mElem = 0;
        mBuffer = new byte[size];
        mBuffer2 = new byte[size];
    }

    /**
     *
     */
    private void flush() {
        mElem = 0;
        mIndexes.clear();
    }

    /**
     *
     */
    private void compute() {
        int i = 0;
        mIndexes.clear();
        if (mElem <= 0) {
            return;
        }
        if (mBuffer[0] != (byte) 0x7E) {
            /*
             * Partial packet
             */
            for (i = 0; i < mElem; i++) {
                if (mBuffer[i] == (byte) 0x7E) {
                    i++;
                    break;
                }
            }
        }
        for (int j = i; j < mElem; j++) {
            if (mBuffer[j] == (byte) 0x7E) {
                mIndexes.add(j);
            }
        }
    }

    /**
     * @param len
     * @return
     */
    private byte[] getAtBegin(int len) {
        byte buffer[] = new byte[len];
        System.arraycopy(mBuffer, 0, buffer, 0, len);
        mElem -= len;
        System.arraycopy(mBuffer, len, mBuffer2, 0, mElem);

        byte tmp[] = mBuffer;
        mBuffer = mBuffer2;
        mBuffer2 = tmp;

        compute();
        return buffer;
    }

    /**
     * @return
     */
    private int getNext() {
        if (mIndexes.isEmpty()) {
            return -1;
        }
        return mIndexes.remove();
    }

    /**
     * @return
     */
    public byte[] get() {
        int beg = getNext();

        if (beg < 0) {
            /*
             * Empty, or bad data. No 0x7E in it.
             */
            flush();
            return null;
        } else if (beg > 0) {
            /*
             * Bad data. Mid stream. Move to first 0x7E
             */
            getAtBegin(beg);
            beg = getNext();
        }

        int end = getNext();
        if (end < 0) {
            /*
             * Not complete yet. Wait for complete packet
             */
            return null;
        }

        byte buf[] = getAtBegin(end - beg + 1);
        return buf;

    }

    /**
     * @param data
     * @param len
     */
    public void put(byte data[], int len) {
        if ((mElem + len) >= mBuffer.length) {
            /*
             * Something wrong.
             */
            flush();
            return;
        }
        System.arraycopy(data, 0, mBuffer, mElem, len);
        mElem += len;
        compute();
    }

}
