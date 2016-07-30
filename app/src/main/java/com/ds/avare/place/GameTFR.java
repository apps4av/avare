/**
 * 
 */
package com.ds.avare.place;

import android.graphics.Color;
import android.graphics.Paint;

import com.ds.avare.StorageService;
import com.ds.avare.position.LabelCoordinate;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.utils.Helper;

import java.util.LinkedList;

/**
 * @author zkhan
 * Game TFR areas
 * 
 */
public class GameTFR {



    private static final double RADIUS_NM = 3.0; // NM
    private static final int HOURS = 6;

    LinkedList<LabelCoordinate> mTFRs;

    public void loadGames(StorageService service) {
        if(service != null) {
            mTFRs = service.getDBResource().findGameTFRs();
        }
    }

    /**
     *
     * @param ctx
     */
    public void draw(DrawingContext ctx) {

        if(null == mTFRs) {
            return;
        }

        long now = Helper.getMillisGMT();

        /*
         * Possible game TFRs, Orange
         */
        ctx.paint.setColor(0xFFFF4500);
        ctx.paint.setStrokeWidth(3 * ctx.dip2pix);
        ctx.paint.setShadowLayer(0, 0, 0, 0);
        Paint.Style style = ctx.paint.getStyle();
        ctx.paint.setStyle(Paint.Style.STROKE);
        // Show date time of TFR
        for(LabelCoordinate tfr : mTFRs) {
            double lat = tfr.getLatitude();
            double lon = tfr.getLongitude();

            // Draw TFR
            float x = (float) ctx.origin.getOffsetX(lon);
            float y = (float) ctx.origin.getOffsetY(lat);
            float radius = ctx.origin.getPixelsInNmAtLatitude(GameTFR.RADIUS_NM, lat);
            ctx.canvas.drawCircle(x, y, radius, ctx.paint);
            ctx.service.getShadowedText().draw(ctx.canvas, ctx.textPaint,
                    tfr.getLabel(), Color.BLACK, (float) x, (float) y + radius + ctx.textPaint.getTextSize());
        }
        ctx.paint.setStyle(style);

    }

}
