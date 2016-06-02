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

import com.ds.avare.threed.Constants;
import com.ds.avare.threed.data.VertexArrayShort;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

public class Map {

    private static final int ROWS = 512;
    private static final int COLS = 512;

    private static final int POSITION_COMPONENT_COUNT = 1;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 1;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT 
        + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constants.BYTES_PER_SHORT;


    // Change this in GLSL vertex shader, if changed here
    private static final float OFFSET_ELEVATION = 0.1f;

    private VertexArrayShort mVertexArrayShort;

    public Map() {
    }

    public boolean loadTerrain(BitmapHolder b, float ratio) {
        if(null == b) {
            return false;
        }
        Bitmap bitmap = b.getBitmap();
        if (bitmap == null) {
            return false;
        }

        mVertexArrayShort = new VertexArrayShort(genTerrainFromBitmap(bitmap, ratio));
        return true;
    }

    public void bindData(TextureShaderProgram textureProgram) {
        if(mVertexArrayShort == null) {
            return;
        }
        mVertexArrayShort.setVertexAttribPointer(
                0,
                textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);
        
        mVertexArrayShort.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }
    
    public void draw() {
        if(mVertexArrayShort == null) {
            return;
        }

        glDrawArrays(GL_TRIANGLE_STRIP, 0, numVertices());
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
    private int makeVertix(short vertices[], int count, int row, int col, Bitmap b, float ratio) {

        int px;
        float pxf;
        px = b.getPixel(col, row);
        pxf = (float) Helper.findElevationFromPixelNormalized(px) * ratio;
        int pxi = (int)((pxf + OFFSET_ELEVATION) * 512.0 + 0.5);
        int pxr = (pxi / 32) & 0x1F;
        int pxc = pxi & 0x1F;

        // Change this in GLSL vertex shader, if changed here
        // pack: row * 32, col * 32, and use lower 5 bits of each for elevation
        // pack 10 bits for col, row, and reuse for texture as there is 1-1 texture/pixel mapping

        // send row in position, col in texture coordinates, and half of elevation in position, half in texture coord
        vertices[count++] = (short) (row * 32 + pxr);
        vertices[count++] = (short) (col * 32 + pxc);
        return count;
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
        int index = (row * (((COLS / 2) * 4) + 2) + col * 2 + colp) * (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT);
        int r = (int)mVertexArrayShort.get(index) & 0x1F;
        int c = (int)mVertexArrayShort.get(index + 1) & 0x1F;
        return (((float)(r * 32 + c)) / 512.f - OFFSET_ELEVATION) / ratio;
    }

    /**
     *
     * @return
     */
    private int numVertices() {
        return (ROWS - 1) * (((COLS / 2) * 4) + 2); // (524286 = 1048572 / 2) for 512x512
    }

    /**
     * Make terrain index buffer from bitmap
     * @param b
     * @return
     */
    private short[] genTerrainFromBitmap(Bitmap b, float ratio) {
        short vertices[] = new short[numVertices() * ((POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT))];
        int count = 0;
        int col;
        int row;
        for (row = 0; row < (ROWS - 1); row++) {
            for (col = 0; col < (COLS - 1); col += 2) {

                // 1
                count = makeVertix(vertices, count, row + 0, col + 0, b, ratio);
                // 6
                count = makeVertix(vertices, count, row + 1, col + 0, b, ratio);
                // 2
                count = makeVertix(vertices, count, row + 0, col + 1, b, ratio);
                // 7
                count = makeVertix(vertices, count, row + 1, col + 1, b, ratio);
            }

            // degenerate 10
            count = makeVertix(vertices, count, row + 1, col - 1, b, ratio);

            // degenerate 6
            count = makeVertix(vertices, count, row + 1, 0, b, ratio);
        }
        return vertices;
    }

}
