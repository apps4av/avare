package com.ds.avare.threed;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import com.ds.avare.R;
import com.ds.avare.utils.BitmapHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by zkhan on 4/28/16.
 */
public class TerrainRenderer implements GLSurfaceView.Renderer {


    private static final String U_MATRIX = "u_Matrix";

    private static final String A_POSITION = "a_Position";
    private static final String A_COLOR = "a_Color";

    private static final int POSITION_COMPONENT_COUNT = 4;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int BYTES_PER_FLOAT = 4;

    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    int uMatrixLocation;
    int aPositionLocation;
    int aColorLocation;

    private static final int ROWS = 128;
    private static final int COLS = 128;

    private final FloatBuffer mVertexData;
    private final Context mContext;

    private float[] mVertices;

    private final float[] mProjectionMatrix = new float[16];

    private final float[] mModelMatrix = new float[16];

    public TerrainRenderer(Context mContext) {
        this.mContext = mContext;

        mVertices = genTerrainFromBitmap(new BitmapHolder(mContext, R.drawable.test).getBitmap());

        mVertexData = ByteBuffer
                .allocateDirect(mVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        mVertexData.put(mVertices);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        int program;


        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        String vertexShaderSource =
                "uniform mat4 u_Matrix;\n" +
                        "attribute vec4 a_Position;\n" +
                        "attribute vec4 a_Color;\n" +
                        "varying vec4 v_Color;\n" +
                        "void main()\n" +
                        "{" +
                        "    v_Color = a_Color;\n" +
                        "    gl_Position = u_Matrix * a_Position;\n" +
                        "    gl_PointSize = 10.0;\n" +
                        "}";

        String fragmentShaderSource =
                "precision mediump float; \n" +
                        "varying vec4 v_Color;\n" +
                        "void main()\n" +
                        "{\n" +
                        "    gl_FragColor = v_Color;\n" +
                        "}";

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper
                .compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        ShaderHelper.validateProgram(program);

        glUseProgram(program);

        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aColorLocation = glGetAttribLocation(program, A_COLOR);

        // Bind our data, specified by the variable mVertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
        mVertexData.position(0);
        glVertexAttribPointer(aPositionLocation,
                POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, mVertexData);

        glEnableVertexAttribArray(aPositionLocation);

        // Bind our data, specified by the variable mVertexData, to the vertex
        // attribute at location A_COLOR_LOCATION.
        mVertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation,
                COLOR_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, mVertexData);

        glEnableVertexAttribArray(aColorLocation);
    }

    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     *
     * @param width  The new width, in pixels.
     * @param height The new height, in pixels.
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        MatrixHelper.perspectiveM(mProjectionMatrix, 45, (float) width
                / (float) height, 1f, 10f);

        setIdentityM(mModelMatrix, 0);

        translateM(mModelMatrix, 0, 0f, 0f, -2.5f);
        rotateM(mModelMatrix, 0, 150f, 1f, 0f, 0f);

        final float[] temp = new float[16];
        multiplyMM(temp, 0, mProjectionMatrix, 0, mModelMatrix, 0);
        System.arraycopy(temp, 0, mProjectionMatrix, 0, temp.length);
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        // Assign the matrix
        glUniformMatrix4fv(uMatrixLocation, 1, false, mProjectionMatrix, 0);

        // Draw the table.
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

        int px;
        float pxf;
        px = b.getPixel(col, row);
        px = px & 255;
        pxf = ((float)px) * 0.003921569f;


        vertices[count++] = (float)(col - COLS / 2) / (float)COLS; //x
        vertices[count++] = (float)(row - ROWS / 2) / (float)ROWS; //y
        vertices[count++] = -pxf; //z
        vertices[count++] = 1.f; //w

        vertices[count++] = pxf; //r
        vertices[count++] = pxf; //g
        vertices[count++] = pxf; //b
        vertices[count++] = 1.f; //a


        return count;
    }

    //http://www.learnopengles.com/android-lesson-eight-an-introduction-to-index-buffer-objects-ibos/ibo_with_degenerate_triangles/

    /**
     * Make terrain index buffer from bitmap
     * @param b
     * @return
     */
    private static float[] genTerrainFromBitmap(Bitmap b) {
        float vertices[] = new float[numVertices() * (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT)];

        int count = 0;
        float pxf;
        int px;
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


