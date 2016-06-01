/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ds.avare.threed.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_UNSIGNED_SHORT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.ds.avare.threed.Constants.BYTES_PER_SHORT;

public class VertexArrayShort {
    private final ShortBuffer shortBuffer;

    public VertexArrayShort(short[] vertexData) {
        shortBuffer = ByteBuffer
            .allocateDirect(vertexData.length * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(vertexData);
    }
        
    public void setVertexAttribPointer(int dataOffset, int attributeLocation,
        int componentCount, int stride) {        
        shortBuffer.position(dataOffset);
        glVertexAttribPointer(attributeLocation, componentCount, GL_UNSIGNED_SHORT,
            false, stride, shortBuffer);
        glEnableVertexAttribArray(attributeLocation);

        shortBuffer.position(0);
    }

    /**
     * Get data from position
     */
    public float get(int index) {
        return shortBuffer.get(index);
    }

}
