/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.shapes;

import com.ds.avare.R;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;

import android.content.Context;
import android.graphics.Color;

/**
 * 
 * A static class because we do not want allocations in the draw task where this
 * is called.
 * 
 * @author zkhan, rwalker
 *
 */
public class DistanceRings {

    /**
     * Distance ring drawing constants
     */
    public static final int COLOR_DISTANCE_RING = Color.rgb(102, 0, 51);
    public static final int COLOR_SPEED_RING =  Color.rgb(178, 255, 102);
    
    public static final int RING_INNER = 0;
    public static final int RING_MIDDLE = 1;
    public static final int RING_OUTER = 2;
    public static final int RING_SPEED = 3;
    
    private static final int STALLSPEED = 25;
    private static final int RING_INNER_SIZE[] = { 1,  2,  5, 10, 20,  40};
    private static final int RING_MIDDLE_SIZE[] = { 2,  5, 10, 20, 40,  80};
    private static final int RING_OUTER_SIZE[] = { 5, 10, 20, 40, 80, 160};
    private static final int RINGS_1_2_5     = 0;
    private static final int RINGS_2_5_10    = 1;
    private static final int RINGS_5_10_20   = 2;
    private static final int RINGS_10_20_40  = 3;
    private static final int RINGS_20_40_80  = 4;
    private static final int RINGS_40_80_160 = 5;
    
    private static float mRings[] = {0, 0, 0, 0};
    private static String mRingsText[] = {null, null, null, null};
        
    /**
     * 
     * @param context
     * @param pref
     * @param scale
     * @param movement
     * @param speed
     */
    public static void calculateRings(Context context,
            Preferences pref, Scale scale, Movement movement, double speed) {
        
        mRings[0] = 0;
        mRings[1] = 0;
        mRings[2] = 0;
        mRings[3] = 0;
        
        /*
         * Find pixels per nautical mile
         */
        float pixPerNm = movement.getNMPerLatitude(scale);
        
        /*
         * Conversion factor for pixPerNm in case we are configured in some other units
         */
        double fac = 1;
        if(pref.getDistanceUnit().equals(context.getString(R.string.UnitMile))) {
            fac *= Preferences.NM_TO_MI;
        }
        else if(pref.getDistanceUnit().equals(context.getString(R.string.UnitKilometer))) {
            fac *= Preferences.NM_TO_KM;
        }

        /*
         * Set the ring sizes to 2/5/10 nm/mi/km
         */
        int ringScale = RINGS_2_5_10;
        
        /*
         * If we are supposed to dynamically scale the rings, then do that now
         */
        if(pref.getDistanceRingType() == 1) {
            float totalZoom = (scale.getScaleFactor() * scale.getZoomFactor()) / scale.getMacroFactor();
            if(totalZoom >= 8) {                /* the larger totalZoom is, the more zoomed in we are   */
                ringScale = RINGS_1_2_5;        
            } else if (totalZoom >= 4) {
                ringScale = RINGS_2_5_10;
            } else if (totalZoom >= 2.0) {
                ringScale = RINGS_5_10_20;
            } else if (totalZoom >= 1) {
                ringScale = RINGS_10_20_40;
            }  else if (totalZoom >= .5) {
                ringScale = RINGS_20_40_80;
            } else {
                ringScale = RINGS_40_80_160;
            }
        }
        
        /*
         *  Draw our "speed ring" if we are going faster than stall speed 
         */
        if(speed >= STALLSPEED && pref.getTimerRingSize() != 0) {
            /*
             * its / 60 as units is in minutes
             */
            mRings[RING_SPEED] = (float) ((float)(speed / 60) * pixPerNm * pref.getTimerRingSize() / fac); 
        }

        /*
         * Calculate the radius of the 3 rings to display
         */
        mRings[RING_INNER] = (float)(pixPerNm * RING_INNER_SIZE[ringScale] / fac);
        mRings[RING_MIDDLE] = (float)(pixPerNm * RING_MIDDLE_SIZE[ringScale] / fac);
        mRings[RING_OUTER] = (float)(pixPerNm * RING_OUTER_SIZE[ringScale] / fac);
        
        mRingsText[RING_INNER] = String.format("%d", RING_INNER_SIZE[ringScale]);
        mRingsText[RING_MIDDLE] = String.format("%d", RING_MIDDLE_SIZE[ringScale]);
        mRingsText[RING_OUTER] = String.format("%d", RING_OUTER_SIZE[ringScale]);
    }

    /**
     * 
     * @return
     */
    public static String[] getRingsText() {
        return mRingsText;
    }
    
    /**
     * 
     * @return
     */
    public static float[] getRings() {
        return mRings;
    }

}
