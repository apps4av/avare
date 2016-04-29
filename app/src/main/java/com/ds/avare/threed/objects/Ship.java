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

import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.glDrawArrays;
import static com.ds.avare.threed.Constants.BYTES_PER_FLOAT;


public class Ship {
    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STRIDE = 
        (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) 
        * BYTES_PER_FLOAT;
    private static final float[] VERTEX_DATA = {
        // Order of coordinates: X, Y, Z, W, R, G, B, A
        0f, -0.4f, 0.6f, 1f, 0f, 1f, 0f, 1f,
        0f,  0.4f, 0.6f, 1f, 1f, 0f, 0f, 1f };
    private final VertexArray vertexArray;

    public Ship() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }
    
    public void bindData(ColorShaderProgram colorProgram) {
        vertexArray.setVertexAttribPointer(
            0,
            colorProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, 
            STRIDE);
        vertexArray.setVertexAttribPointer(
            POSITION_COMPONENT_COUNT,
            colorProgram.getColorAttributeLocation(), 
            COLOR_COMPONENT_COUNT,
            STRIDE);
    }

    public void draw() {        
        glDrawArrays(GL_POINTS, 0, 2);
    }
}
