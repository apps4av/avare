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

import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @author zkhan
 * @author plinel
 *
 *
 */
public class ShapeFileShape extends Shape {

    /**
     *
     */
    public ShapeFileShape(String text, Date date) {
        super(text, date);
    }


    /**
     * Function to parse shape files
     * @param path
     * @return
     * @throws IOException
     * @throws InvalidShapeFileException
     */
    public static LinkedList<ShapeFileShape> readFiles(String path) throws IOException,
            InvalidShapeFileException {

        LinkedList<ShapeFileShape> ret = new LinkedList<ShapeFileShape>();
        // List here shape files to be parsed
        String files[] = {path + "/class_b.shp", path + "/class_c.shp", path + "/class_d.shp"};

        for (String file : files) {

            // for all shape files
            FileInputStream is = new FileInputStream(file);
            ShapeFileReader r = new ShapeFileReader(is);

            AbstractShape s;
            while ((s = r.next()) != null) {

                switch (s.getShapeType()) {
                    // deal with polygons only at this time
                    case POLYGON:
                        // make internal shape from shape file .shp
                        ShapeFileShape shape = new ShapeFileShape(file, new Date());

                        PolygonShape aPolygon = (PolygonShape) s;
                        for (int i = 0; i < aPolygon.getNumberOfParts(); i++) {
                            PointData[] points = aPolygon.getPointsOfPart(i);
                            for (PointData po : points) {
                                shape.add(po.getX(), po.getY(), false);
                            }
                        }
                        ret.add(shape);
                        break;
                    default:
                        break;
                }
            }

            is.close();
        }
        return ret;
    }

    /**
     *
     * @param ctx
     * @param shapes
     * @param shouldShow
     */
    public static void draw(DrawingContext ctx, LinkedList<ShapeFileShape> shapes, boolean shouldShow) {

        ctx.paint.setShadowLayer(0, 0, 0, 0);

        if(!shouldShow) {
            return;
        }

        // Shape files can overwhelm drawing. Only support couple of macro levels to include
        // only a few shapes to draw
        if(ctx.scale.getMacroFactor() > 1) {
            return;
        }

        /*
         * Draw all shapes. Choose color based on hash of name
         */
        if(null != shapes) {
            ctx.paint.setStrokeWidth(3 * ctx.dip2pix);
            ctx.paint.setShadowLayer(0, 0, 0, 0);

            for (int shape = 0; shape < shapes.size(); shape++) {
                Shape todraw = shapes.get(shape);
                if (null == todraw) {
                    continue;
                }
                if(todraw.getLabel().endsWith("class_b.shp")) {
                    ctx.paint.setColor(Color.BLUE);
                }
                else if(todraw.getLabel().endsWith("class_c.shp")) {
                    ctx.paint.setColor(Color.MAGENTA);
                }
                else if(todraw.getLabel().endsWith("class_d.shp")) {
                    ctx.paint.setColor(Color.CYAN);
                }
                if (todraw.isOnScreen(ctx.origin)) {
                    todraw.drawShape(ctx.canvas, ctx.origin, ctx.scale, ctx.movement, ctx.paint, ctx.pref.isNightMode(), true);
                }
            }
        }
    }
}
