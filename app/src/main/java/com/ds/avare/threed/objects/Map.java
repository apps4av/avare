/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.objects;

import android.graphics.Bitmap;

import com.ds.avare.shapes.SubTile;
import com.ds.avare.threed.Constants;
import com.ds.avare.threed.data.VertexArrayShort;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.utils.Helper;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

public class Map {

    private static final int ROWS = SubTile.DIM;
    private static final int COLS = SubTile.DIM;

    public static final int COMPONENTS = 2;
    private static final int STRIDE = COMPONENTS * Constants.BYTES_PER_SHORT;

    private static final int NUM_VERTICES = (ROWS - 1) * (((COLS / 2) * 4) + 2); // (524286 = 1048572 / 2) for 512x512

    private float mRatio;

    private VertexArrayShort mVertexArrayShort;

    public Map() {
        mRatio = 0;
    }

    public boolean loadTerrain(short vertexArray[], float ratio) {

        if(null == vertexArray) {
            return false;
        }
        mVertexArrayShort = new VertexArrayShort(vertexArray);
        mRatio = ratio;
        return true;
    }

    public void bindData(TextureShaderProgram textureProgram) {
        if(mVertexArrayShort == null) {
            return;
        }
        mVertexArrayShort.setVertexAttribPointer(
                0,
                textureProgram.getS0AttributeLocation(),
                1,
                STRIDE);
        
        mVertexArrayShort.setVertexAttribPointer(
                1,
                textureProgram.getS1AttributeLocation(),
                1,
                STRIDE);
    }
    
    public void draw() {
        if(mVertexArrayShort == null) {
            return;
        }

        glDrawArrays(GL_TRIANGLE_STRIP, 0, NUM_VERTICES);
    }

    //http://www.learnopengles.com/android-lesson-eight-an-introduction-to-index-buffer-objects-ibos/ibo_with_degenerate_triangles/

    /**
     * Elevation from vertex buffer
     * @param row
     * @param col
     * @return
     */
    public float getZ(int row, int col, float ratio) {
        if(mVertexArrayShort == null || row >= ROWS || col >= COLS || row < 0 || col < 0) {
            return -1;
        }
        int colp = 0;
        if(row == (ROWS - 1)) {
            row--; // there is no last row
            colp = 1; // but +1 as that row
        }
        int index = (row * (((COLS / 2) * 4) + 2) + col * 2 + colp) * COMPONENTS;
        int r = (int)mVertexArrayShort.get(index) & 0x3F;
        int c = (int)mVertexArrayShort.get(index + 1) & 0x3F;
        int px = r * 64 + c;
        return (float)Helper.findElevationFromPixelNormalized(px);
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
    private static int makeVertix(short vertices[], int count, int row, int col, Bitmap b) {

        int px = b.getPixel(col, row) & 0xFF;
        int pxr = (px / 64) & 0x3F;
        int pxc = px & 0x3F;

        // Change this in GLSL vertex shader, if changed here
        // pack data : 10 bit row + 6 high bits of pixel then 10 bit col + 6 low bits of pixel
        vertices[count++] = (short) (row * 64 + pxr);
        vertices[count++] = (short) (col * 64 + pxc);
        return count;
    }


    /**
     * Make terrain index buffer from bitmap
     * @param b
     * @return
     */
    public static short[] genTerrainFromBitmap(Bitmap b) {
        if(null == b) {
            return null;
        }
        short vertices[] = new short[NUM_VERTICES * COMPONENTS];
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

    public float getRatio() {
        return mRatio;
    }
}
