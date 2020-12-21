/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.place;

import android.graphics.Color;

import com.ds.avare.StorageService;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.storage.StringPreference;
import com.ds.avare.utils.ShadowedText;

/**
 * 
 * @author zkhan
 *
 */
public class Favorites {

    private String mVals[];

    public Favorites(StorageService service) {
        mVals = service.getDBResource().getUserRecents();
    }

    public void update(StorageService service) {
        mVals = service.getDBResource().getUserRecents();
    }

    public void draw(DrawingContext ctx, boolean shouldShow) {
        String vals[] = mVals.clone();
        if((vals == null) || (!shouldShow)) {
            return;
        }
        for (String s : vals) {
            String destType = StringPreference.parseHashedNameDestType(s);
            String dbType = StringPreference.parseHashedNameDbType(s);
            String id = "";
            double lon = 0;
            double lat = 0;
            if(dbType == null || destType == null) {
                continue;
            }

            if((dbType.equals(Destination.GPS) && destType.equals(Destination.GPS) || (dbType.equals(Destination.MAPS) && destType.equals(Destination.MAPS)))) {
                s = StringPreference.parseHashedNameId(s);
                id = StringPreference.parseHashedNameIdBefore(s);
                if(id.equals("")) { // not a favorite GPS destination
                    continue;
                }
                String after = StringPreference.parseHashedNameIdAfter(s);
                /*
                 * This is lon/lat destination
                 */
                String tokens[] = after.split("&");

                try {
                    lon = Double.parseDouble(tokens[1]);
                    lat = Double.parseDouble(tokens[0]);
                }
                catch (Exception e) {
                    continue;
                }

            }
            else {
                continue;
            }

            ctx.textPaint.setColor(Color.CYAN);
            ctx.canvas.drawCircle((float)ctx.origin.getOffsetX(lon), (float)ctx.origin.getOffsetY(lat), 4 * ctx.dip2pix, ctx.textPaint);
            ctx.textPaint.setColor(Color.WHITE);
            ctx.service.getShadowedText().draw(ctx.canvas, ctx.textPaint, id, Color.DKGRAY, ShadowedText.BELOW,
                    (float)ctx.origin.getOffsetX(lon), (float)ctx.origin.getOffsetY(lat));

        }
    }
}

