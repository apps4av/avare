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

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.utils.BitmapHolder;


/**
 * @author zkhan
 *
 */
public class TrackShape extends Shape {

    private static final int MILES_PER_SEGMENT = 50;

    private static final int LEG_PREV = Color.GRAY;
    private static final int LEG_CURRENT = Color.MAGENTA;
    private static final int LEG_NEXT = Color.CYAN;

    public static int getLegColor(int dstNxt, int segNum) {
        if (dstNxt <= segNum) {
            return LEG_NEXT;
        } else if (dstNxt - 1 == segNum) {
            return LEG_CURRENT;
        } else {
            return LEG_PREV;
        }
    }

    /**
     * Set the destination for this track
     */
    public TrackShape() {
        
        /*
         * No label for track line, tracks do not expire hence null
         */
        super("", null);
    }

    /**
     * Update track as the aircraft moves
     */
    public void updateShape(GpsParams loc, Destination destination) {
    
        /*
         * Where the aircraft is
         */
        double lastLon = loc.getLongitude();
        double lastLat = loc.getLatitude();
        double destLon = 0;
        double destLat = 0;


        if (null != destination) {
            destLon = destination.getLocation().getLongitude();
            destLat = destination.getLocation().getLatitude();
        }

        Projection p = new Projection(lastLon, lastLat, destLon, destLat);
        int segments = (int) p.getDistance() / MILES_PER_SEGMENT + 3; // Min 3 points
        Coordinate coord[] = p.findPoints(segments);
        super.mCoords.clear();
        
        /*
         * Now make shape from coordinates with segments
         */
        coord[0].makeSeparate();
        coord[segments - 1].makeSeparate();
        for (int i = 0; i < segments; i++) {
            super.add(coord[i].getLongitude(), coord[i].getLatitude(), coord[i].isSeparate());
        }
    }

    /**
     * Update track as the aircraft moves
     */
    public void updateShapeFromPlan(Coordinate[] coord) {

        super.mCoords.clear();

        if (null == coord) {
            return;
        }
        /*
         * Now make shape from coordinates with segments
         */
        for (Coordinate c : coord) {
            super.add(c.getLongitude(), c.getLatitude(), c.isSeparate(), c.getLeg());
        }
    }

    /**
     * @param ctx
     * @param shapes
     * @param shouldShow
     */
    public static void draw(DrawingContext ctx, Plan plan, Destination destination, GpsParams params, BitmapHolder line, BitmapHolder heading, boolean shouldDraw) {
        if((!shouldDraw) || (destination == null)) {
            return;
        }

        ctx.paint.setColor(Color.MAGENTA);
        ctx.paint.setStrokeWidth(5 * ctx.dip2pix);
        ctx.paint.setAlpha(162);
        if(destination.isFound() && !plan.isActive()  && (!ctx.pref.isSimulationMode())) {
            destination.getTrackShape().drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, ctx.paint, ctx.pref.isNightMode(), ctx.pref.isTrackEnabled());
        } else if (plan.isActive()) {
            plan.getTrackShape().drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, ctx.paint, ctx.pref.isNightMode(), ctx.pref.isTrackEnabled(), ctx.service.getPlan());
        }

        if(!ctx.pref.isSimulationMode()) {
            /*
             * Draw actual track
             */
            if(null != line && params != null) {
                BitmapHolder.rotateBitmapIntoPlace(line, (float) destination.getBearing(),
                        params.getLongitude(), params.getLatitude(), false, ctx.origin);
                ctx.canvas.drawBitmap(line.getBitmap(), line.getTransform(), ctx.paint);
            }
            /*
             * Draw actual heading
             */
            if(null != heading && params != null) {
                BitmapHolder.rotateBitmapIntoPlace(heading, (float) params.getBearing(),
                        params.getLongitude(), params.getLatitude(), false, ctx.origin);
                ctx.canvas.drawBitmap(heading.getBitmap(), heading.getTransform(), ctx.paint);
            }
        }
    }
}