/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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
    private float[] mShips;
    private int mShipCount;

    private VertexArray mVertexArray;


    // Make a triangle representing the ship, rotate in ship's heading
    private static float[] getShip(float tr[], int offset, float x, float y, float z, float angle) {

        // make a paper bird with two sides
        final float disp = 0.05f;
        final float intensity = 0.9f;

        float vector[] = new float[4];


        // center base is location of ship triangle, left side wing
        vector[0] = x;
        vector[1] = y -disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 0 + offset * ELEMS, 0, 0, 1);
        tr[4  + offset * ELEMS] = intensity;
        tr[5  + offset * ELEMS] = 0;
        tr[6  + offset * ELEMS] = 0;
        tr[7  + offset * ELEMS] = 1f;

        vector[0] = x -disp / 2;
        vector[1] = y;
        vector[2] = z + disp / 2;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 8 + offset * ELEMS, 0, 0, 1);
        tr[12 + offset * ELEMS] = intensity;
        tr[13 + offset * ELEMS] = 0;
        tr[14 + offset * ELEMS] = 0;
        tr[15 + offset * ELEMS] = 1f;

        vector[0] = x;
        vector[1] = y + disp / 2;
        vector[2] = z;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 16 + offset * ELEMS, 0, 0, 1);
        tr[20 + offset * ELEMS] = intensity;
        tr[21 + offset * ELEMS] = 0;
        tr[22 + offset * ELEMS] = 0;
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

    public void initShips(int shipNum) {
        mVertexArray = null;
        mShipCount = 0;
        mShips = new float[ELEMS * shipNum];
    }

    public void addShip(float x, float y, float z, float angle) {
        getShip(mShips, mShipCount, x, y, z, angle);
        mShipCount++;
    }

    public void doneShips() {
        if(mShipCount != 0) {
            mVertexArray = new VertexArray(mShips);
        }
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

    }

}
