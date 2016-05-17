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
import android.opengl.Matrix;

import com.ds.avare.threed.data.Vector3d;
import com.ds.avare.threed.objects.Map;
import com.ds.avare.threed.objects.Ship;
import com.ds.avare.threed.programs.ColorShaderProgram;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.threed.util.Camera;
import com.ds.avare.threed.util.TextureHelper;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.GenericCallback;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zkhan on 4/28/16.
 */
public class TerrainRenderer implements GLSurfaceView.Renderer {

    public static final String SURFACE_CHANGED = "SurfaceChanged";
    public static final String SURFACE_CREATED = "SurfaceCreated";
    public static final String DRAW_FRAME = "DrawFrame";

    private Context mContext;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private int mWidth;
    private int mHeight;
    private Camera mCamera;

    private Map mMap;
    private Ship mShip;

    private float mAngle;

    private boolean mMapSet;
    private boolean mTextureSet;

    private TextureShaderProgram mTextureProgram;
    private ColorShaderProgram mColorProgram;
    private GenericCallback mCallback;

    private int mTexture;
    private float mDisplacementX;
    private float mDisplacementY;

    public TerrainRenderer(Context ctx, GenericCallback cb) {
        mContext = ctx;
        mCallback = cb;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mMap = new Map();
        mShip = new Ship();
        mCamera = new Camera();
        mTextureSet = false;
        mMapSet = false;
        mAngle = 0;
        mDisplacementX = 0;
        mDisplacementY = 0;

        mTextureProgram = new TextureShaderProgram(mContext);
        mColorProgram = new ColorShaderProgram(mContext);
        mCallback.callback(this, SURFACE_CREATED);
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
        mCallback.callback(this, SURFACE_CHANGED);
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);


        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) mWidth / mHeight;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near =  1f;
        final float far = 10.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mDisplacementX, mDisplacementY, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, mAngle, 0.0f, 0.0f, 1.0f);


        // View matrix from cam
        mCamera.setViewMatrix(mViewMatrix);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        if(mTextureSet && mMapSet) {
            // Draw the map.
            mTextureProgram.useProgram();
            mTextureProgram.setUniforms(mMVPMatrix, mTexture);
            mMap.bindData(mTextureProgram);
            mMap.draw();

            // Draw the ships
            mColorProgram.useProgram();
            mColorProgram.setUniforms(mMVPMatrix);
            mShip.bindData(mColorProgram);
            mShip.draw();
        }


        mCallback.callback(this, DRAW_FRAME);
    }

    // Make camera from current position and target
    public void setCamera(Vector3d pos, Vector3d look) {
        mCamera.set(pos, look);
    }

    public void setTexture(BitmapHolder b) {
        mTexture = TextureHelper.loadTexture(b);
        mTextureSet = true;
    }

    public void setTerrain(BitmapHolder b) {
        mMap.loadTerrain(b);
        mMapSet = true;
    }

    public void setShips(Vector3d traffic[], Vector3d self) {

        if(traffic != null) {
            mShip.initShips(traffic.length + 1); // +1 for self
            for (Vector3d t : traffic) {
                mShip.addShip(t.getX(), t.getY(), t.getZ(), 0xFF00FF00); // green other
            }
        }
        else {
            mShip.initShips(1);
        }
        mShip.addShip(self.getX(), self.getY(), self.getZ(), 0XFFFF0000); // red self
        mShip.doneShips();
    }

    public void setOrientation(float angle, float displacementX, float displacementY) {
        mAngle = angle;
        mDisplacementX = displacementX;
        mDisplacementY = displacementY;
    }
}


