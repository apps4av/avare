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

import com.ds.avare.threed.util.ShaderHelper;
import com.ds.avare.threed.util.TextResourceReader;

import static android.opengl.GLES20.glUseProgram;

abstract class ShaderProgram {
    // Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    protected static final String U_NORMAL = "u_Normal";
    protected static final String U_SLOPE = "u_Slope";
    protected static final String U_INTERCEPT = "u_Intercept";
    protected static final String U_HEIGHT = "u_Height";

    // Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_S0 = "a_S0";
    protected static final String A_S1 = "a_S1";

    // Shader program
    protected final int program;
    protected ShaderProgram(Context context, int vertexShaderResourceId,
        int fragmentShaderResourceId) {
        // Compile the shaders and link the program.
        program = ShaderHelper.buildProgram(
            TextResourceReader.readTextFileFromResource(
                context, vertexShaderResourceId),
            TextResourceReader.readTextFileFromResource(
                context, fragmentShaderResourceId));
    }        

    public void useProgram() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program);
    }
}
