/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.threed;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.ds.avare.R;
import com.ds.avare.threed.objects.Map;
import com.ds.avare.threed.objects.Ship;
import com.ds.avare.threed.programs.ColorShaderProgram;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.threed.util.MatrixHelper;
import com.ds.avare.threed.util.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by zkhan on 4/28/16.
 */
public class TerrainRenderer implements GLSurfaceView.Renderer {

    private final Context mContext;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private int mWidth;
    private int mHeight;

    private Map mMap;
    private Ship mShip;

    private TextureShaderProgram mTextureProgram;
    private ColorShaderProgram mColorProgram;

    private int mTexture;

    public TerrainRenderer(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {


        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mMap = new Map(mContext);
        mShip = new Ship();

        mTextureProgram = new TextureShaderProgram(mContext);
        mColorProgram = new ColorShaderProgram(mContext);

        mTexture = TextureHelper.loadTexture(mContext, R.drawable.sec_9_98_314);

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
        mWidth = width;
        mHeight = height;
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        MatrixHelper.perspectiveM(mProjectionMatrix, 45, (float) mWidth
                / (float) mHeight, 1f, 10f);

        setIdentityM(mModelMatrix, 0);
        translateM(mModelMatrix, 0, 0f, 0f, -4.0f);
        rotateM(mModelMatrix, 0, -60f, 1f, 0f, 0f);

        final float[] temp = new float[16];
        multiplyMM(temp, 0, mProjectionMatrix, 0, mModelMatrix, 0);
        System.arraycopy(temp, 0, mProjectionMatrix, 0, temp.length);


        // Draw the map.
        mTextureProgram.useProgram();
        mTextureProgram.setUniforms(mProjectionMatrix, mTexture);
        mMap.bindData(mTextureProgram);
        mMap.draw();

        // Draw the ships
        mColorProgram.useProgram();
        mColorProgram.setUniforms(mProjectionMatrix);
        mShip.bindData(mColorProgram);
        mShip.draw();
    }
}


