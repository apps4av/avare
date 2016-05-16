/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.objects;

import android.graphics.Color;

import com.ds.avare.threed.data.VertexArray;
import com.ds.avare.threed.programs.ColorShaderProgram;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static com.ds.avare.threed.Constants.BYTES_PER_FLOAT;


public class Ship {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) 
        * BYTES_PER_FLOAT;

    private static final int ELEMS = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * 3;
    private float[] mShips;
    private int mShipCount;

    private VertexArray mVertexArray;

    // Make a triangle representing the ship
    private static float[] getTriangle(float tr[], int offset, float x, float y, float z, int color) {

        final float disp = 0.05f;
        float r = Color.red(color);
        float g = Color.green(color);
        float b = Color.blue(color);

        // center base is location of ship triangle
        tr[0  + offset * ELEMS] = x;
        tr[1  + offset * ELEMS] = y;
        tr[2  + offset * ELEMS] = z + disp; // top
        tr[3  + offset * ELEMS] = 1f;
        tr[4  + offset * ELEMS] = r;
        tr[5  + offset * ELEMS] = g;
        tr[6  + offset * ELEMS] = b;
        tr[7  + offset * ELEMS] = 1f;

        tr[8  + offset * ELEMS] = x - disp; // left
        tr[9  + offset * ELEMS] = y;
        tr[10 + offset * ELEMS] = z;
        tr[11 + offset * ELEMS] = 1f;
        tr[12 + offset * ELEMS] = r;
        tr[13 + offset * ELEMS] = g;
        tr[14 + offset * ELEMS] = b;
        tr[15 + offset * ELEMS] = 1f;

        tr[16 + offset * ELEMS] = x + disp; //right
        tr[17 + offset * ELEMS] = y;
        tr[18 + offset * ELEMS] = z;
        tr[19 + offset * ELEMS] = 1f;
        tr[20 + offset * ELEMS] = r;
        tr[21 + offset * ELEMS] = g;
        tr[22 + offset * ELEMS] = b;
        tr[23 + offset * ELEMS] = 1f;

        return tr;
    }


    public void initShips(int shipNum) {
        mVertexArray = null;
        mShips = new float[ELEMS * shipNum];
        mShipCount = 0;
    }

    public void addShip(float x, float y, float z, int color) {
        getTriangle(mShips, 0, x, y, z, color);
        mShipCount++;
    }

    public void doneShips() {
        mVertexArray = new VertexArray(mShips);
    }
    
    public void bindData(ColorShaderProgram colorProgram) {
        if(mVertexArray == null) {
            return;
        }

        mVertexArray.setVertexAttribPointer(
                0,
                colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);
        mVertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                colorProgram.getColorAttributeLocation(),
                COLOR_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        if(mVertexArray == null) {
            return;
        }
        glDrawArrays(GL_TRIANGLES, 0, mShipCount * 3);
    }

}
