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


import android.graphics.Color;
import android.graphics.Paint;

import com.ds.avare.place.GameTFR;
import com.ds.avare.position.LabelCoordinate;

import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @author zkhan
 * @author plinel
 *
 *
 */
public class TFRShape extends Shape {

    /**
     * 
     */
    public TFRShape(String text, Date date) {
        super(text, date);
    }

    public void updateText(String text) {
        super.mText = text;
    }

    /**
     *
     * @param ctx
     * @param gameTfrLabels
     * @param shouldShow
     */
    public static void drawGame(DrawingContext ctx, LinkedList<LabelCoordinate> gameTfrLabels, boolean shouldShow) {
        if (!shouldShow) {
            return;
        }

        /*
         * Possible game TFRs, Orange
         */
        if (ctx.pref.showGameTFRs()) {
            ctx.paint.setColor(0xFFFF4500);
            ctx.paint.setStrokeWidth(3 * ctx.dip2pix);
            ctx.paint.setShadowLayer(0, 0, 0, 0);
            Paint.Style style = ctx.paint.getStyle();
            ctx.paint.setStyle(Paint.Style.STROKE);
            for (int shape = 0; shape < GameTFR.GAME_TFR_COORDS.length; shape++) {
                double lat = GameTFR.GAME_TFR_COORDS[shape][0];
                double lon = GameTFR.GAME_TFR_COORDS[shape][1];
                float x = (float) ctx.origin.getOffsetX(lon);
                float y = (float) ctx.origin.getOffsetY(lat);
                float radius = ctx.origin.getPixelsInNmAtLatitude(GameTFR.RADIUS_NM, lat);
                ctx.canvas.drawCircle(x, y, radius, ctx.paint);
            }
            ctx.paint.setStyle(style);


            /*
             * Label Game TFRs
             */
            if (null != gameTfrLabels) {
                for (LabelCoordinate c : gameTfrLabels) {
                    double lat = c.getLatitude();
                    double lon = c.getLongitude();
                    float x = (float) ctx.origin.getOffsetX(lon);
                    float y = (float) ctx.origin.getOffsetY(lat);

                    ctx.service.getShadowedText().draw(ctx.canvas, ctx.textPaint,
                            c.getLabel(), Color.BLACK, x, y);
                }
            }
        }
    }

    /**
     *
     * @param ctx
     * @param shapes
     * @param shouldShow
     */
    public static void draw(DrawingContext ctx, LinkedList<TFRShape> shapes, boolean shouldShow) {

        ctx.paint.setColor(Color.RED);
        ctx.paint.setShadowLayer(0, 0, 0, 0);

        if(!shouldShow) {
            return;
        }

        int expiry = ctx.pref.getExpiryTime();

        /*
         * Draw TFRs only if they belong to the screen
         */
        if(null != shapes) {
            ctx.paint.setColor(Color.RED);
            ctx.paint.setStrokeWidth(3 * ctx.dip2pix);
            ctx.paint.setShadowLayer(0, 0, 0, 0);

            for(int shape = 0; shape < shapes.size(); shape++) {
                Shape todraw = shapes.get(shape);
                if(null == todraw) {
                    continue;
                }
                if (todraw.isOld(expiry)) {
                    continue;
                }
                if(todraw.isOnScreen(ctx.origin) ) {
                    todraw.drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, ctx.paint, ctx.pref.isNightMode(), true);
                }
            }
        }

    }

}
