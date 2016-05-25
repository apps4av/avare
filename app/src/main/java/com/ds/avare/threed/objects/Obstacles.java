/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.threed.objects;

import com.ds.avare.threed.data.VertexArray;
import com.ds.avare.threed.programs.ColorShaderProgram;

import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.glDrawArrays;
import static com.ds.avare.threed.Constants.BYTES_PER_FLOAT;


public class Obstacles {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) 
        * BYTES_PER_FLOAT;

    private static final int ELEMS = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * 2;
    private float[] mObs;
    private int mObsCount;

    private VertexArray mVertexArray;


    // Make a triangle representing the obstacle, rotate
    private static float[] getObstacle(float tr[], int offset, float x, float y, float z) {

        tr[0  + offset * ELEMS] = x;
        tr[1  + offset * ELEMS] = y;
        tr[2  + offset * ELEMS] = z;
        tr[3  + offset * ELEMS] = 1f;
        tr[4  + offset * ELEMS] = 1;
        tr[5  + offset * ELEMS] = 0;
        tr[6  + offset * ELEMS] = 0;
        tr[7  + offset * ELEMS] = 1f;

        tr[8  + offset * ELEMS] = x;
        tr[9  + offset * ELEMS] = y;
        tr[10 + offset * ELEMS] = 0;
        tr[11 + offset * ELEMS] = 1f;
        tr[12 + offset * ELEMS] = 1;
        tr[13 + offset * ELEMS] = 0;
        tr[14 + offset * ELEMS] = 0;
        tr[15 + offset * ELEMS] = 1f;
        return tr;
    }


    public void initObstacles(int obsNum) {
        mVertexArray = null;
        mObsCount = 0;
        mObs = new float[ELEMS * obsNum];
    }

    public void addObstacles(float x, float y, float z) {
        getObstacle(mObs, mObsCount, x, y, z);
        mObsCount++;
    }

    public void doneObstacles() {
        if(mObsCount != 0) {
            mVertexArray = new VertexArray(mObs);
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

        glDrawArrays(GL_LINES, 0, mObsCount * 2);

    }

}
