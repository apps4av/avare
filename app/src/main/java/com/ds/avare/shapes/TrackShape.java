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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Destination;
import com.ds.avare.place.Plan;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Origin;
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
    private static final int LEG_PAUSED = Color.YELLOW;

    // Get the color of the leg segment to draw.
    public static int getLegColor(Plan plan, int segNum) {

        // Is this plan paused ?
        boolean bPlanIsPaused = plan.isPaused();

        // get the "current" leg segment hat is active
        int dstNxt = plan.findNextNotPassed();

        //  Check for a future segment
        if (dstNxt <= segNum) {
            return bPlanIsPaused ? LEG_PAUSED :LEG_NEXT;

        // Check for the current segment, always drawn in its color
        } else if (dstNxt - 1 == segNum) {
            return LEG_CURRENT;

        // Otherwise, it's a previously completed segment
        } else {
            return bPlanIsPaused ? LEG_PAUSED :LEG_PREV;
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
        Coordinate[] coord = p.findPoints(segments);
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

    // draw all segments that make up this plan
    //
    private void drawSegments(Canvas c, Origin origin, Paint paint, boolean night, boolean drawTrack, Plan plan) {
        /*
         * Draw background on track shapes, so draw twice. There is the
         * the possibility of a "future" leg being the same as the current or prev leg, as
         * would be the case if an approach came in from a VOR, then the missed approach goes
         * back to the same VOR, or a VOR used in a procedure turn. This can be handled by
         * cycling through the list twice and drawing them in 2 passes.
         *
         * Note there is NOT a 1:1 relationship between coord's and the legs of a plan. Each
         * coord has a property that indicates its leg within the plan, use that.
         */
        int cMax = getNumCoords() - 1;
        int currentLeg = ((null == plan) ? 0 : plan.findNextNotPassed() - 1);
        if (currentLeg < 0) currentLeg = 0; // Account for the leg of "here" to first waypoint

        // Pass 1 - draw all of the legs that are AFTER the current leg
        // We can start our search at currentLeg * 2 into the coord array due to each
        // plan leg having at least 2 coord entries, saves a bit of looping time.
        for(int coord = currentLeg * 2; coord < cMax; coord++) {
            if(mCoords.get(coord).getLeg() > currentLeg) {
                drawOneSegment(c, origin, paint, night, drawTrack, plan, coord);
            }
        }

        // Pass 2 - draw all of the legs that are before AND the current leg
        for(int coord = 0; coord < cMax; coord++) {
            if(mCoords.get(coord).getLeg() <= currentLeg) {
                drawOneSegment(c, origin, paint, night, drawTrack, plan, coord);
            } else break;   // If we're past current leg, stop the for() loop
        }
    }

    // draw one specific segment of the plan
    //
    private void drawOneSegment(Canvas c, Origin origin, Paint paint, boolean night, boolean drawTrack, Plan plan, int coord) {

        // fetch the start and end coordinates of this segment
        Coordinate thisCoord = mCoords.get(coord);
        Coordinate nextCoord = mCoords.get(coord + 1);

        // screen pixel locations of same
        float x1 = (float)origin.getOffsetX(thisCoord.getLongitude());
        float x2 = (float)origin.getOffsetX(nextCoord.getLongitude());
        float y1 = (float)origin.getOffsetY(thisCoord.getLatitude());
        float y2 = (float)origin.getOffsetY(nextCoord.getLatitude());

        float width = paint.getStrokeWidth();
        int color = paint.getColor();

        if(drawTrack) {
            // Draw the background of the track line
            paint.setStrokeWidth(width + 4);
            paint.setColor(night ? Color.WHITE : Color.BLACK);
            c.drawLine(x1, y1, x2, y2, paint);

            // Now draw the track line itself in proper color
            paint.setStrokeWidth(width);
            if(null == plan) {
                paint.setColor(color);
            } else {
                paint.setColor(TrackShape.getLegColor(plan, thisCoord.getLeg()));
            }
            c.drawLine(x1, y1, x2, y2, paint);
        }

        if(nextCoord.isSeparate()) {
            paint.setColor(night? Color.WHITE : Color.BLACK);
            c.drawCircle(x2, y2, width + 8, paint);
            paint.setColor(Color.GREEN);
            c.drawCircle(x2, y2, width + 6, paint);
            paint.setColor(color);
        }

        if(thisCoord.isSeparate()) {
            paint.setColor(night? Color.WHITE : Color.BLACK);
            c.drawCircle(x1, y1, width + 8, paint);
            paint.setColor(Color.GREEN);
            c.drawCircle(x1, y1, width + 6, paint);
            paint.setColor(color);
        }
    }

    // Static method called from the main LocationView object to display either a direct track
    // line to the destination, or if a plan is active, draw that complete plan. Finally,
    // draw the intended heading and current track pointers
    public static void draw(DrawingContext ctx, Plan plan, Destination destination, GpsParams params, BitmapHolder line, BitmapHolder heading) {

        // Set our paint line color, width, transparency
        ctx.paint.setColor(Color.MAGENTA);
        ctx.paint.setStrokeWidth(5 * ctx.dip2pix);
        ctx.paint.setAlpha(162);

        // Determine which shape to draw. If we have an active plan, draw that. If not, if we have
        // a "direct to" destination then draw that. Else, nothing to draw.
        TrackShape trackShape = null;
        if(destination.isFound() && !plan.isActive()  && (!ctx.pref.isSimulationMode())) {
            trackShape = destination.getTrackShape();
        } else if (plan.isActive()) {
            trackShape = plan.getTrackShape();
        }

        if (null != trackShape) {
            trackShape.drawSegments(ctx.canvas, ctx.origin, ctx.paint, ctx.pref.isNightMode(), ctx.pref.isTrackEnabled(), plan.isActive() ? plan : null);
        }

        // if not in simulation mode, draw the track/heading lines
        if(!ctx.pref.isSimulationMode()) {

            // Draw actual track
            if(null != line && params != null) {
                BitmapHolder.rotateBitmapIntoPlace(line, (float) destination.getBearing(),
                        params.getLongitude(), params.getLatitude(), false, ctx.origin);
                ctx.canvas.drawBitmap(line.getBitmap(), line.getTransform(), ctx.paint);
            }

            // Draw actual heading
            if(null != heading && params != null) {
                BitmapHolder.rotateBitmapIntoPlace(heading, (float) params.getBearing(),
                        params.getLongitude(), params.getLatitude(), false, ctx.origin);
                ctx.canvas.drawBitmap(heading.getBitmap(), heading.getTransform(), ctx.paint);
            }
        }
    }
}