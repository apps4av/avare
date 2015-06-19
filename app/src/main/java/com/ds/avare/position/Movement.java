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

import com.ds.avare.storage.Preferences;

/**
 * @author zkhan
 * A class that holds movement on a tile
 */
public class Movement {

    /**
     * Pixels per longitude degree
     */
    private double px;
    /**
     * Pixels per latitude degree
     */
    private double py;
    /**
     * Offset of location on a tile - x
     */
    private double offsetx;
    /**
     * Offset of location on a tile - y
     */
    private double offsety;
        
    /**
     * @param offset
     * @param pp
     */
    public Movement(double offset[], double pp[]) {
        /*
         * Store movement
         */
        px = pp[0];
        py = pp[1];
        offsetx = offset[0];
        offsety = offset[1];
    }

    /**
     * This is for no movement
     */
    public Movement() {
        px = 0.00001f; /* This is to avoid divide by zero */
        py = 0.00001f;
        offsetx = 0.f;
        offsety = 0.f;
    }

    /**
     * @return
     */
    public double getLongitudePerPixel() {
        return px;
    }
    
    /**
     * @return
     */
    public double getLatitudePerPixel() {
        return py;
    }

    /**
     * @return
     */
    public double getOffsetLongitude() {
        return offsetx;
    }
    
    /**
     * @return
     */
    public double getOffsetLatitude() {
        return offsety;
    }
    

    /**
     * 
     * @param scale
     * @return
     */
    public float getNMPerLatitude(Scale scale) {
        float sy = scale.getScaleCorrected();
        float facy = sy / (float)getLatitudePerPixel();
        return Math.abs(facy * (float)Preferences.NM_TO_LATITUDE);
    }

}
