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
 *
 */
public class PixelCoordinate {

    private double mX;
    private double mY;
   
    private boolean mSeparate;
    
    /**
     * 
     * @param lon
     * @param lat
     */
    public PixelCoordinate(double x, double y) {
        mX = x;
        mY = y;
        mSeparate = false;
    }
    
    /**
     * 
     */
    public double getX() {
        return mX;
    }
    
    /**
     * 
     */
    public double getY() {
        return mY;
    }

    /**
     * 
     */
    public void makeSeparate()  {
       mSeparate = true; 
    }
    
    /**
     * 
     * @return
     */
    public boolean isSeparate() {
        return mSeparate;
    }

    /**
     * 
     * @param r
     * @param angle
     * @return
     */
    public static double rotateX(double r, double angle) {
        return r * Math.sin(Math.toRadians(angle));
    }
    
    /**
     * 
     * @param r
     * @param angle
     * @return
     */
    public static double rotateY(double r, double angle) {
        return -r * Math.cos(Math.toRadians(angle));
    }

}
