/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.util;

import android.opengl.Matrix;

public class MatrixHelper {
    public static void perspectiveM(float[] m, float yFovInDegrees, float aspect,
        float n, float f) {
        final float angleInRadians = (float) (yFovInDegrees * Math.PI / 180.0);
		
        final float a = (float) (1.0 / Math.tan(angleInRadians / 2.0));
        m[0] = a / aspect;
        m[1] = 0f;
        m[2] = 0f;
        m[3] = 0f;

        m[4] = 0f;
        m[5] = a;
        m[6] = 0f;
        m[7] = 0f;

        m[8] = 0f;
        m[9] = 0f;
        m[10] = -((f + n) / (f - n));
        m[11] = -1f;
        
        m[12] = 0f;
        m[13] = 0f;
        m[14] = -((2f * f * n) / (f - n));
        m[15] = 0f;        
    }


    /**
     * Rotate a point of vector around x,y,z of angle
     * @param x
     * @param y
     * @param z
     * @param angle
     */
    public static void rotatePoint(float x, float y, float z, float angle, float vector[], float resultVector[], int offset, float xx, float yx, float zx) {
        float[] modelView = new float[16];

        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, x, y, z);
        // rotate about x, y, or z axis
        Matrix.rotateM(modelView, 0, angle, xx, yx, zx);
        Matrix.translateM(modelView, 0, -x, -y, -z);
        Matrix.multiplyMV(resultVector, offset, modelView, 0, vector, 0);

    }

}
