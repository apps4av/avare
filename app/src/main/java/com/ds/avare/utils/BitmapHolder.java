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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

import com.ds.avare.position.Origin;
import com.ds.avare.storage.Preferences;

import java.io.File;
import java.io.IOException;

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
     * @param
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
     * @param
     *
     */
    public BitmapHolder(Bitmap.Config config) {
        try {
            mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, config);
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
     * @param b is bitmap to draw
     */
    public void drawInBitmap(BitmapHolder b, Rect src, Rect dst) {
        if((null == b) || (null == mCanvas)) {
            return;
        }
        if(null == b.getBitmap()) {
            return;
        }
        mCanvas.drawBitmap(b.getBitmap(), src, dst, null);
    }

    /**
     *
     * @return
     */
    public Canvas getCanvas() {
        return mCanvas;
    }


    private String getName(Preferences pref, String name) {
        return getName(pref.mapsFolder() + "/" + name);
    }

    private String getName(String name) {

        String jpgname = name + ".jpg";
        String pngname = name + ".png";
        String webname = name + ".webp";

        boolean jpg = (new File(jpgname)).exists();
        boolean png = (new File(pngname)).exists();
        boolean web = (new File(webname)).exists();

        if(jpg) {
            return jpgname;
        }
        else if(png) {
            return pngname;
        }
        else if(web) {
            return webname;
        }
        else {
            mWidth = 0;
            mHeight = 0;
            mName = null;
            return null;
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

        name = getName(pref, name);
        if(null == name) {
            return;
        }

        try {
            mBitmap = BitmapFactory.decodeFile(name, opt);
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
     * Get bitmap from a file
     */
    public BitmapHolder(Context context, Preferences pref, String name, int sampleSize, Bitmap.Config type) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = type;
        opt.inSampleSize = sampleSize;

        name = getName(pref, name);
        if(null == name) {
            return;
        }

        try {
            mBitmap = BitmapFactory.decodeFile(name, opt);
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

        name = getName(name);
        if(null == name) {
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
     * @param name
     * Get bitmap from a diagram / plate file
     */
    public BitmapHolder(String name, Bitmap.Config type) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = type;
        opt.inSampleSize = 1;

        name = getName(name);
        if(null == name) {
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
     * @param name
     * Get bitmap from a diagram / plate file
     */
    public BitmapHolder(String name, Bitmap.Config type, Rect r) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = type;
        opt.inSampleSize = 1;

        name = getName(name);
        if(null == name) {
            return;
        }

        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(name, false);
            mBitmap = decoder.decodeRegion(r, opt);
        }
        catch(OutOfMemoryError e) {
        }
        catch (IOException e) {
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
     * @param
     */
    public void setFound(boolean found) {
        mFound = found;
    }
    
    /**
     * 
     * @param
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

    /**
     * This function will rotate and move a bitmap to a given lon/lat on screen
     * @param b
     * @param angle
     * @param lon
     * @param lat
     * @param div Shift the image half way up so it could be centered on y axis
     */
    public static void rotateBitmapIntoPlace(BitmapHolder b, float angle, double lon, double lat, boolean div, Origin origin) {
        float x = (float)origin.getOffsetX(lon);
        float y = (float)origin.getOffsetY(lat);

        b.getTransform().setTranslate(
                x - b.getWidth() / 2,
                y - (div ? b.getHeight() / 2 : b.getHeight()));

        b.getTransform().postRotate(angle, x, y);
    }


}
