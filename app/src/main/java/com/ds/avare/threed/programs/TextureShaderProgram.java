/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.programs;

import android.content.Context;

import com.ds.avare.R;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;

public class TextureShaderProgram extends ShaderProgram {
    // Uniform locations
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;
    private final int uNormalLocation;
    private final int uHeightLocation;
    private final int uSlopeLocation;
    private final int uInterceptLocation;

    // Attribute locations
    private final int aS0;
    private final int aS1;

    public TextureShaderProgram(Context context) {
        super(context, R.raw.texture_vertex_shader,
                R.raw.texture_fragment_shader);

        // Retrieve uniform locations for the shader program.
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);

        uSlopeLocation = glGetUniformLocation(program, U_SLOPE);
        uInterceptLocation = glGetUniformLocation(program, U_INTERCEPT);
        uNormalLocation = glGetUniformLocation(program, U_NORMAL);
        uHeightLocation = glGetUniformLocation(program, U_HEIGHT);

        // Retrieve attribute locations for the shader program.
        aS0 = glGetAttribLocation(program, A_S0);
        aS1 = glGetAttribLocation(program, A_S1);
    }

    public void setUniforms(float[] matrix, int textureId) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnitLocation, 0);
    }


    public int getS0AttributeLocation() {
        return aS0;
    }

    public int getS1AttributeLocation() {
        return aS1;
    }

    public void setUniformsHeight(float slope, float intercept, float normal, float height) {
        glUniform1f(uSlopeLocation, slope);
        glUniform1f(uInterceptLocation, intercept);
        glUniform1f(uNormalLocation, normal);
        glUniform1f(uHeightLocation, height);
    }
}