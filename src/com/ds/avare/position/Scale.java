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

/**
 * 
 * @author zkhan
 * Keeps scale factor and scale correction at this particular point on the map
 */
public class Scale {

    private double mScaleFactor;
    private double mScaleCorrectY;
    private int mMacro;
    
    private static final double MAX_SCALE = 2;
    private static final double MIN_SCALE = 0.5; 
    private static final int MAX_MACRO = 16;
    private static final int MIN_MACRO = 1;
    

    /**
     * 
     */
    public Scale() {
        mScaleFactor = 1;
        mScaleCorrectY = 1;
        mMacro = 1;
    }

    /**
     * 
     * @param factor
     */
    public void setScaleFactor(float factor) {
        mScaleFactor = (double)factor;
    }

    /**
     * 
     * @param latitude
     */
    public void setScaleAt(double latitude) {
        /*
         * http://mysite.du.edu/~jcalvert/math/mercator.htm
         */
        mScaleCorrectY = 1 / Math.cos(Math.toRadians(latitude));
    }

    /**
     * 
     * @return
     */
    public float getScaleFactorRaw() {
        return getScaleFactor();
    }
    
    /**
     * 
     * @return
     */
    public float getScaleFactor() {
        double s;
        if(mScaleFactor > MAX_SCALE) {
            s = MAX_SCALE;
        }
        else if(mScaleFactor < MIN_SCALE) {
            s = MIN_SCALE;
        }
        else {
            s = mScaleFactor;
        }
        return((float)s);
    }

    /**
     * 
     * @return
     */
    public int getMacroFactor() {
        return mMacro;
    }

    /**
     * 
     * @return
     */
    public boolean setMacroFactor(int macro) {
        if(macro > MAX_MACRO) {
            return false;
        }
        if(macro < MIN_MACRO) {
            return false;
        }
        mMacro = macro;
        return true;
    }

    /**
     * 
     * @return
     */
    public float getScaleCorrected() {
        double s;
        if(mScaleFactor > MAX_SCALE) {
            s = MAX_SCALE;
        }
        else if(mScaleFactor < MIN_SCALE) {
            s = MIN_SCALE;
        }
        else {
            s = mScaleFactor;
        }
        return((float)(mScaleCorrectY * s));
    }
    
    /**
     * 
     * @return
     */
    public int getZoomFactor() {
        return (int)MAX_SCALE;
    }
    
    /**
     * 
     * @return
     */
    public int downSample() {
        return (int)(Math.log(mMacro) / Math.log(2));
    }

}
