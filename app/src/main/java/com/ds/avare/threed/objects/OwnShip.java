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

import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.glDrawArrays;
import static com.ds.avare.threed.Constants.BYTES_PER_FLOAT;


public class OwnShip {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) 
        * BYTES_PER_FLOAT;

    private static final int ELEMS = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * (4 * 2 + 180 * 2);
    private float[] mShips;

    private VertexArray mVertexArray;

    private static float mScanAngle = 0;

    // Make axis
    private static float[] getAxis(float tr[], int offset, float x, float y, float z, float angle) {

        float vector[] = new float[4];

        // miniature aircraft wings right
        tr[0  + offset * ELEMS] = x + 0.001f;
        tr[1  + offset * ELEMS] = y + 0.01f;
        tr[2  + offset * ELEMS] = z;
        tr[3  + offset * ELEMS] = 1f;
        tr[4  + offset * ELEMS] = 1;
        tr[5  + offset * ELEMS] = 0.75f;
        tr[6  + offset * ELEMS] = 0;
        tr[7  + offset * ELEMS] = 1f;

        tr[8  + offset * ELEMS] = x + 0.005f;
        tr[9  + offset * ELEMS] = y + 0.01f;
        tr[10 + offset * ELEMS] = z;
        tr[11 + offset * ELEMS] = 1f;
        tr[12 + offset * ELEMS] = 1;
        tr[13 + offset * ELEMS] = 0.75f;
        tr[14 + offset * ELEMS] = 0;
        tr[15 + offset * ELEMS] = 1f;

        // y-axis
        tr[16 + offset * ELEMS] = x;
        tr[17 + offset * ELEMS] = y;
        tr[18 + offset * ELEMS] = z;
        tr[19 + offset * ELEMS] = 1f;
        tr[20 + offset * ELEMS] = 1;
        tr[21 + offset * ELEMS] = 0.75f;
        tr[22 + offset * ELEMS] = 0;
        tr[23 + offset * ELEMS] = 1f;

        vector[0] = x + 0f;
        vector[1] = y + 2f;
        vector[2] = z + 0;
        vector[3] = 1f;
        MatrixHelper.rotatePoint(x, y, z, -angle, vector, tr, 24 + offset * ELEMS, 0, 0, 1);
        tr[28 + offset * ELEMS] = 1;
        tr[29 + offset * ELEMS] = 0.75f;
        tr[30 + offset * ELEMS] = 0;
        tr[31 + offset * ELEMS] = 1f;

        // z-axis
        tr[32 + offset * ELEMS] = x;
        tr[33 + offset * ELEMS] = y;
        tr[34 + offset * ELEMS] = z - 2f;
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

        // miniature aircraft wings left
        tr[48 + offset * ELEMS] = x - 0.001f;
        tr[49 + offset * ELEMS] = y + 0.01f;
        tr[50 + offset * ELEMS] = z;
        tr[51 + offset * ELEMS] = 1f;
        tr[52 + offset * ELEMS] = 1;
        tr[53 + offset * ELEMS] = 0.75f;
        tr[54 + offset * ELEMS] = 0;
        tr[55 + offset * ELEMS] = 1f;

        tr[56 + offset * ELEMS] = x - 0.005f;
        tr[57 + offset * ELEMS] = y + 0.01f;
        tr[58 + offset * ELEMS] = z;
        tr[59 + offset * ELEMS] = 1f;
        tr[60 + offset * ELEMS] = 1;
        tr[61 + offset * ELEMS] = 0.75f;
        tr[62 + offset * ELEMS] = 0;
        tr[63 + offset * ELEMS] = 1f;

        // y-axis scan green
        // make 180 lines with 1 degree increment so we do not regenerate them
        int coord = 0;
        for(int deg = -90; deg < 90; deg ++) {
            tr[64 + offset * ELEMS + coord * 16] = x;
            tr[65 + offset * ELEMS + coord * 16] = y;
            tr[66 + offset * ELEMS + coord * 16] = z;
            tr[67 + offset * ELEMS + coord * 16] = 1f;
            tr[68 + offset * ELEMS + coord * 16] = 0;
            tr[69 + offset * ELEMS + coord * 16] = 1;
            tr[70 + offset * ELEMS + coord * 16] = 0;
            tr[71 + offset * ELEMS + coord * 16] = 1f;

            vector[0] = x + 0f;
            vector[1] = y + 2f;
            vector[2] = z + 0;
            vector[3] = 1f;
            MatrixHelper.rotatePoint(x, y, z, -angle - deg, vector, tr, 72 + offset * ELEMS + coord * 16, 0, 0, 1);
            tr[76 + offset * ELEMS + coord * 16] = 0;
            tr[77 + offset * ELEMS + coord * 16] = 1;
            tr[78 + offset * ELEMS + coord * 16] = 0;
            tr[79 + offset * ELEMS + coord * 16] = 1f;
            coord++;
        }


        return tr;
    }


    public void initOwnShip(float x, float y, float z, float angle) {
        mVertexArray = null;
        mShips = new float[ELEMS];

        // draw axis around our ship, but not the ship itself
        getAxis(mShips, 0, x, y, z, angle);
    }

    public void doneOwnShips() {
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

        mScanAngle += 0.25;

        // Draw axis around ownship
        glDrawArrays(GL_LINES, 0, 4 * 2);
        glDrawArrays(GL_LINES, 4 * 2 + ((((int)mScanAngle) % 180) * 2), 2);

    }

}
