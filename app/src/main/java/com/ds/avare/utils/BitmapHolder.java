/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import java.io.File;

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
     *
     */    
	private boolean mFree = true;
    
	/**
	 * 
	 */
	private boolean mFound = false;
	
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
     * @param name
     * Get bitmap from renderer
     */
    public BitmapHolder(int width, int height) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        try {
            mBitmap = Bitmap.createBitmap(width, height, conf);
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
    public void drawInBitmap(BitmapHolder b, String name, int x, int y) {
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
        mTransform.setTranslate(x, y);
        mCanvas.drawBitmap(b.getBitmap(), mTransform, null);
    }

    /**
     * 
     * @param pref
     * @param name
     * @param opts
     */
    public static void getTileOptions(String name, Preferences pref, int opts[]) {
        
        if(!(new File(pref.mapsFolder() + "/" + name)).exists()) {
            opts[0] = WIDTH;
            opts[1] = HEIGHT;
            return;
        }
        
        /*
         * Bitmap dims without decoding
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pref.mapsFolder() + "/" + name, options);
        opts[0] = options.outWidth;
        opts[1] = options.outHeight;
        if(opts[0] == 0) {
            opts[0] = WIDTH;
        }
        if(opts[1] == 0) {
            opts[1] = HEIGHT;
        }
    }

    /**
     * @param name
     * Get bitmap from a file
     */
    public BitmapHolder(Context context, Preferences pref, String name, int sampleSize) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = sampleSize;

        if(!(new File(pref.mapsFolder() + "/" + name)).exists()) {
            mName = null;
            return;
        }
        try {
            mBitmap = BitmapFactory.decodeFile(pref.mapsFolder() + "/" + name, opt);
        }
        catch(OutOfMemoryError e) {
        }
        if(null != mBitmap) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
            mName = name;
        }
        else {
            mName = null;
        }
    }

    /**
     * @param name
     * Get bitmap from a diagram / plate file
     */
    public BitmapHolder(String name) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 1;
        
        if(!(new File(name).exists())) {
            mWidth = 0;
            mHeight = 0;
            mName = null;
            return;
        }

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
        else {
        }
    }

    // Create a bitmapholder from an already loaded bitmap
    public BitmapHolder(Bitmap bitMap) {
        mBitmap = bitMap;
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
    public boolean getFree() {
    	return mFree;
    }
    
    /**
     * 
     * @param free
     */
    public void setFree(boolean free) {
        mFree = free;
    }
    
    /**
     * 
     * @param nothing in it?
     */
    public void setFound(boolean found) {
        mFound = found;
    }
    
    /**
     * 
     * @param nothing in it?
     */
    public boolean getFound() {
        return mFound;
    }
        
    /**
     * 
     * @return
     */
    public Matrix getTransform() {
        return mTransform;
    }
}
