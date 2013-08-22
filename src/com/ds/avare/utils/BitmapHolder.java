/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import com.ds.avare.R;
import com.ds.avare.storage.Preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * @author zkhan
 * This class hides all details of handling a bitmap
 */
public class BitmapHolder {
    
    /**
     * 
     */
    private Bitmap mBitmap = null;
    
    /**
     * 
     */
    private Canvas mCanvas;
    
    /**
     * 
     */
    private int mWidth = 0;
    /**
     * 
     */
    private int mHeight = 0;
    /**
     * 
     */
    private String mName = null;

    /**
     * Transform for scale/translate
     */
    private Matrix mTransform = new Matrix();

    /**
     * 
     */
    public static final int WIDTH = 512;
    /**
     * 
     */
    public static final int HEIGHT = 512;

    /**
     * @param name
     * Get bitmap from renderer
     */
    public BitmapHolder() {
        Bitmap.Config conf = Bitmap.Config.RGB_565;
        try {
            mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, conf);
            mBitmap.setDensity(Bitmap.DENSITY_NONE);
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
            mCanvas = new Canvas(mBitmap);
            mName = null;
        }
        catch(OutOfMemoryError e){
        }
    }
    
    /**
     * 
     * @param b is bitmap to draw
     * @param name is the name to store
     */
    public void drawInBitmap(BitmapHolder b, String name) {
        /*
         * This should mark bitmap dirty
         */
        mName = name;
        if(null == name) {
            return;
        }
        if((null == b) || (null == mCanvas)) {
            return;
        }
        if(null == b.getBitmap()) {
            return;
        }
        mTransform.setTranslate(0, 0);
        mCanvas.drawBitmap(b.getBitmap(), mTransform, null);
    }

    /**
     * 
     * @param pref
     * @param name
     * @param opts
     */
    public static void getTileOptions(String name, int opts[]) {
        
        /*
         * XXX: No need to decode bitmap header.
         */
        opts[0] = BitmapHolder.WIDTH;
        opts[1] = BitmapHolder.HEIGHT;
    }

    /**
     * @param name
     * Get bitmap from a file
     */
    public BitmapHolder(Context context, Preferences pref, String name) {
        try {
            mBitmap = BitmapFactory.decodeFile(pref.mapsFolder() + "/" + name);
        }
        catch(OutOfMemoryError e) {
        }
        if(null != mBitmap) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
            mName = name;
        }
        else {
            try {
                mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nochart);
            }
            catch(OutOfMemoryError e){
            }
            if(null != mBitmap) {
                mWidth = mBitmap.getWidth();
                mHeight = mBitmap.getHeight();
                mName = name;
            }
            else {
                mWidth = 0;
                mHeight = 0;
                mName = null;
            }
        }
    }

    /**
     * @param name
     * Get bitmap from a diagram / plate file
     */
    public BitmapHolder(String name) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 2;

        try {
            mBitmap = BitmapFactory.decodeFile(name, opt);
        }
        catch(OutOfMemoryError e){
        }
        if(null != mBitmap) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
            mName = name;
        }
        else {
            mWidth = 0;
            mHeight = 0;
            mName = null;
        }
    }

    /**
     * @param context
     * @param id
     * Get bitmap from resources
     */
    public BitmapHolder(Context context, int id) {
        try {
            mBitmap = BitmapFactory.decodeResource(context.getResources(), id);
        }
        catch(OutOfMemoryError e){
        }
        if(null != mBitmap) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
        }
    }

    /**
     * Android does not free memory for a bitmap. Have to call this explicitly
     * especially for large bitmaps
     */
    public void recycle() {
        if(null != mBitmap) {
            mBitmap.recycle();
        }
        mBitmap = null;
        mName = null;
        mWidth = 0;
        mHeight = 0;
    }
    
    /**
     * @return
     */
    public int getWidth() {
        return mWidth;
    }
    
    /**
     * @return
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * @return
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }
        
    /**
     * 
     * @return
     */
    public Matrix getTransform() {
        return mTransform;
    }
}
