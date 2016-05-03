/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.objects;

import android.content.Context;
import android.graphics.Bitmap;

import com.ds.avare.R;
import com.ds.avare.threed.Constants;
import com.ds.avare.threed.data.VertexArray;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.utils.BitmapHolder;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

public class Map {

    private static final int ROWS = 512;
    private static final int COLS = 512;

    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT 
        + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constants.BYTES_PER_FLOAT;


    private VertexArray mVertexArray;

    public Map(Context ctx) {
        mVertexArray = new VertexArray(genTerrainFromBitmap(new BitmapHolder(ctx, R.drawable.elev_9_98_314).getBitmap()));
    }
    
    public void bindData(TextureShaderProgram textureProgram) {
        mVertexArray.setVertexAttribPointer(
                0,
                textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);
        
        mVertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }
    
    public void draw() {                                
        glDrawArrays(GL_TRIANGLE_STRIP, 0, numVertices());
    }


    /**
     *
     * @return
     */
    private static int numVertices() {
        return (ROWS - 1) * (COLS * 4) / 2 + (ROWS - 1) * 2;
    }

    /**
     * rows, cols must be even
     * @param vertices
     * @param count
     * @param row
     * @param col
     * @param b
     * @return
     */
    private static int makeVertix(float vertices[], int count, int row, int col, Bitmap b) {

        float metersInz = 75.0f / 58.79f; // zoom level 9


        int px;
        float pxf;
        px = b.getPixel(col, row);
        px = px & 255;
        pxf = ((float)px) * 0.003921569f * metersInz;
        //-1,1    1,1
        //-1,-1   1,-1
        vertices[count++] = (float)((float)col * 2.0f - (float)COLS) / (float)COLS; //x
        vertices[count++] = (float)(-(float)row * 2.0f + (float)ROWS) / (float)ROWS; //y
        vertices[count++] = pxf; //z
        vertices[count++] = 1.f; //w
        vertices[count++] = ((float)col) / ((float)COLS); //s
        vertices[count++] = ((float)row) / ((float)ROWS); //t

        return count;
    }

    //http://www.learnopengles.com/android-lesson-eight-an-introduction-to-index-buffer-objects-ibos/ibo_with_degenerate_triangles/

    /**
     * Make terrain index buffer from bitmap
     * @param b
     * @return
     */
    private static float[] genTerrainFromBitmap(Bitmap b) {
        float vertices[] = new float[numVertices() * ((POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT))];

        int count = 0;
        int col;
        int row;
        for (row = 0; row < (ROWS - 1); row++) {
            for (col = 0; col < (COLS - 1); col += 2) {

                // 1
                count = makeVertix(vertices, count, row + 0, col + 0, b);
                // 6
                count = makeVertix(vertices, count, row + 1, col + 0, b);
                // 2
                count = makeVertix(vertices, count, row + 0, col + 1, b);
                // 7
                count = makeVertix(vertices, count, row + 1, col + 1, b);
            }

            // degenerate 10
            count = makeVertix(vertices, count, row + 1, col - 1, b);

            // degenerate 6
            count = makeVertix(vertices, count, row + 1, 0, b);
        }
        return vertices;
    }

}
