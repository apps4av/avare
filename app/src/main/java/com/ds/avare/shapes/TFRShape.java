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
     * @param shapes
     * @param shouldShow
     */
    public static void draw(DrawingContext ctx, LinkedList<TFRShape> shapes, boolean shouldShow) {

        if(!shouldShow) {
            return;
        }

        int expiry = ctx.pref.getExpiryTime();

        /*
         * Draw TFRs only if they belong to the screen
         */
        if(null != shapes) {

            Paint strokePaint = new Paint();
            strokePaint.setDither(true);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setAntiAlias(true);
            strokePaint.setColor(Color.RED);
            strokePaint.setStrokeWidth(3 * ctx.dip2pix);
            strokePaint.setShadowLayer(0, 0, 0, 0);

            Paint fillPaint = new Paint();
            fillPaint.setDither(true);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAntiAlias(true);
            fillPaint.setColor(Color.RED);
            fillPaint.setStrokeWidth(3 * ctx.dip2pix);
            fillPaint.setShadowLayer(0, 0, 0, 0);
            fillPaint.setAlpha(50);


            for(int shape = 0; shape < shapes.size(); shape++) {
                Shape todraw = shapes.get(shape);
                if(null == todraw) {
                    continue;
                }
                if (todraw.isOld(expiry)) {
                    continue;
                }
                if(todraw.isOnScreen(ctx.origin)) {
                    todraw.drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, strokePaint, ctx.pref.isNightMode(), true);
                    if (ctx.pref.isTFRShading()) {
                        todraw.drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, fillPaint, ctx.pref.isNightMode(), true);
                    }
                }
            }
        }

    }
}
