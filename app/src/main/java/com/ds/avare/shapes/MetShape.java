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


import android.content.Context;
import android.graphics.Color;

import com.ds.avare.R;
import com.ds.avare.weather.AirSigMet;

import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @author zkhan
 * @author plinel
 *
 */
public class MetShape extends Shape {

    /**
     * 
     */
    public MetShape(String text, Date date) {
        super(text, date);
    }

    public void updateText(String text) {
        super.mText = text;
    }

    /**
     * If this shape is touched, get color
     * @param lon
     * @param lat
     * @return
     */
    public String getHTMLMetOnTouch(Context ctx, AirSigMet met, double lon, double lat) {
        String txt = getTextIfTouched(lon, lat);
        String ret = "";
        if(null != txt) {
            String typeArray[] = ctx.getResources().getStringArray(R.array.AirSig);
            int colorArray[] = ctx.getResources().getIntArray(R.array.AirSigColor);
            int color = 0;
            String type = met.hazard + " " + met.reportType;
            for(int j = 0; j < typeArray.length; j++) {
                if(typeArray[j].equals(type)) {
                    color = colorArray[j];
                    break;
                }
            }
            ret = "<font color='" + String.format("#%02x%02x%02x", Color.red(color), Color.green(color), Color.blue(color)) + "'>" + txt + "</font>\n--\n";
        }


        return ret;
    }

    /**
     *
     * @param ctx
     * @param mets
     * @param shouldShow
     */
    public static void draw(DrawingContext ctx, LinkedList<AirSigMet> mets, boolean shouldShow) {

        ctx.paint.setShadowLayer(0, 0, 0, 0);

        if(!shouldShow) {
            return;
        }

        if(mets == null) {
            return;
        }

        int expiry = ctx.pref.getExpiryTime();

        ctx.paint.setStrokeWidth(2 * ctx.dip2pix);
        ctx.paint.setShadowLayer(0, 0, 0, 0);
        String typeArray[] = ctx.context.getResources().getStringArray(R.array.AirSig);
        int colorArray[] = ctx.context.getResources().getIntArray(R.array.AirSigColor);
        String storeType = ctx.pref.getAirSigMetType();

        for(int i = 0; i < mets.size(); i++) {
            AirSigMet met = mets.get(i);
            int color = 0;

            String type = met.hazard + " " + met.reportType;
            if(storeType.equals("ALL")) {
                /*
                 * All draw all shapes
                 */
            }
            else if(!storeType.equals(type)) {
                /*
                 * This should not be drawn.
                 */
                continue;
            }

            for(int j = 0; j < typeArray.length; j++) {
                if(typeArray[j].equals(type)) {
                    color = colorArray[j];
                    break;
                }
            }

            /*
             * Now draw shape only if belong to the screen
             */
            if(met.shape != null && color != 0) {
                ctx.paint.setColor(color);
                if (met.shape.isOld(expiry)) {
                    continue;
                }
                if( met.shape.isOnScreen(ctx.origin) ) {
                    met.shape.drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, ctx.paint, ctx.pref.isNightMode(), true);
                }
            }
        }
    }

}
