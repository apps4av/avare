/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.objects;

import com.ds.avare.threed.data.VertexArray;
import com.ds.avare.threed.programs.ColorShaderProgram;
import com.ds.avare.threed.util.MatrixHelper;

import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static com.ds.avare.threed.Constants.BYTES_PER_FLOAT;


public class Ship {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) 
        * BYTES_PER_FLOAT;

    private static final int ELEMS = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * 6;
    private static final int ELEMS_AXIS = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * 6;
    private float[] mShips;
    private int mShipCount;

    private VertexArray mVertexArray;


    // Make a triangle representing the ship, rotate in ship's heading
    private static float[] getShip(float tr[], int offset, float x, float y, float z, float angle) {

        // make a paper bird with two sides
        final float disp = 0.05f;
        final float intensity = 0.9f;
        final float blue = offset == 0 ? 1.0f : 0; // first ship is ownship, show its left wing as magenta

        float vector[] = new float[4];


        // center base is location of ship triangle, left side wing
        vector[0] = x;
        vector[1] = y -disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 0 + offset * ELEMS, 0, 0, 1);
        tr[4  + offset * ELEMS] = intensity;
        tr[5  + offset * ELEMS] = 0;
        tr[6  + offset * ELEMS] = blue;
        tr[7  + offset * ELEMS] = 1f;

        vector[0] = x -disp / 2;
        vector[1] = y;
        vector[2] = z + disp / 2;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 8 + offset * ELEMS, 0, 0, 1);
        tr[12 + offset * ELEMS] = intensity;
        tr[13 + offset * ELEMS] = 0;
        tr[14 + offset * ELEMS] = blue;
        tr[15 + offset * ELEMS] = 1f;

        vector[0] = x;
        vector[1] = y + disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 16 + offset * ELEMS, 0, 0, 1);
        tr[20 + offset * ELEMS] = intensity;
        tr[21 + offset * ELEMS] = 0;
        tr[22 + offset * ELEMS] = blue;
        tr[23 + offset * ELEMS] = 1f;

        // center base is location of ship triangle, right side wing
        vector[0] = x;
        vector[1] = y + disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 24 + offset * ELEMS, 0, 0, 1);
        tr[28 + offset * ELEMS] = 0;
        tr[29 + offset * ELEMS] = intensity;
        tr[30 + offset * ELEMS] = 0;
        tr[31 + offset * ELEMS] = 1f;

        vector[0] = x + disp / 2;
        vector[1] = y;
        vector[2] = z + disp / 2;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 32 + offset * ELEMS, 0, 0, 1);
        tr[36 + offset * ELEMS] = 0;
        tr[37 + offset * ELEMS] = intensity;
        tr[38 + offset * ELEMS] = 0;
        tr[39 + offset * ELEMS] = 1f;

        vector[0] = x;
        vector[1] = y -disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 40 + offset * ELEMS, 0, 0, 1);
        tr[44 + offset * ELEMS] = 0;
        tr[45 + offset * ELEMS] = intensity;
        tr[46 + offset * ELEMS] = 0;
        tr[47 + offset * ELEMS] = 1f;

        return tr;
    }

    // Make axis
    private static float[] getAxis(float tr[], int offset, float x, float y, float z, float angle) {

        float vector[] = new float[4];

        // x-axis
        tr[0  + offset * ELEMS] = x;
        tr[1  + offset * ELEMS] = y;
        tr[2  + offset * ELEMS] = z;
        tr[3  + offset * ELEMS] = 1f;
        tr[4  + offset * ELEMS] = 1;
        tr[5  + offset * ELEMS] = 0;
        tr[6  + offset * ELEMS] = 0;
        tr[7  + offset * ELEMS] = 1f;

        vector[0] = x + 2f;
        vector[1] = y + 0;
        vector[2] = z + 0;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 8 + offset * ELEMS, 0, 0, 1);
        tr[12 + offset * ELEMS] = 1;
        tr[13 + offset * ELEMS] = 0;
        tr[14 + offset * ELEMS] = 0;
        tr[15 + offset * ELEMS] = 1f;

        // y-axis
        tr[16 + offset * ELEMS] = x;
        tr[17 + offset * ELEMS] = y;
        tr[18 + offset * ELEMS] = z;
        tr[19 + offset * ELEMS] = 1f;
        tr[20 + offset * ELEMS] = 0;
        tr[21 + offset * ELEMS] = 1;
        tr[22 + offset * ELEMS] = 0;
        tr[23 + offset * ELEMS] = 1f;

        vector[0] = x + 0f;
        vector[1] = y + 2f;
        vector[2] = z + 0;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 24 + offset * ELEMS, 0, 0, 1);
        tr[28 + offset * ELEMS] = 0;
        tr[29 + offset * ELEMS] = 1;
        tr[30 + offset * ELEMS] = 0;
        tr[31 + offset * ELEMS] = 1f;

        // z-axis
        tr[32 + offset * ELEMS] = x;
        tr[33 + offset * ELEMS] = y;
        tr[34 + offset * ELEMS] = z;
        tr[35 + offset * ELEMS] = 1f;
        tr[36 + offset * ELEMS] = 0;
        tr[37 + offset * ELEMS] = 0;
        tr[38 + offset * ELEMS] = 1;
        tr[39 + offset * ELEMS] = 1f;

        vector[0] = x + 0f;
        vector[1] = y + 0f;
        vector[2] = z + 2f;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 40 + offset * ELEMS, 0, 0, 1);
        tr[44 + offset * ELEMS] = 0;
        tr[45 + offset * ELEMS] = 0;
        tr[46 + offset * ELEMS] = 1;
        tr[47 + offset * ELEMS] = 1f;

        return tr;
    }


    public void initShips(int shipNum, float x, float y, float z, float angle) {
        mVertexArray = null;
        mShipCount = 0;
        mShips = new float[ELEMS * shipNum + ELEMS_AXIS];
        // first ship is our ship
        getShip(mShips, mShipCount, x, y, z, angle);
        mShipCount++;

        // draw axis around our ship
        getAxis(mShips, shipNum, x, y, z, angle);
    }

    public void addShip(float x, float y, float z, float angle) {
        getShip(mShips, mShipCount, x, y, z, angle);
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

        // Draw ships
        glDrawArrays(GL_TRIANGLES, 0, mShipCount * 6);

        // Draw axis around ownship
        glDrawArrays(GL_LINES, mShipCount * 6, 6);


    }

}
