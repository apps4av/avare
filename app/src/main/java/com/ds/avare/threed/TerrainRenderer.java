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

import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.threed.objects.Map;
import com.ds.avare.threed.objects.Obstacles;
import com.ds.avare.threed.objects.OwnShip;
import com.ds.avare.threed.objects.Ship;
import com.ds.avare.threed.programs.ColorShaderProgram;
import com.ds.avare.threed.programs.TextureShaderProgram;
import com.ds.avare.threed.util.Camera;
import com.ds.avare.threed.util.MatrixHelper;
import com.ds.avare.threed.util.Orientation;
import com.ds.avare.threed.util.TextureHelper;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glLineWidth;
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
    private Orientation mOrientation;

    private float mAltitude; // current height
    private Map mMap;
    private Ship mShip;
    private OwnShip mOwnShip;
    private Obstacles mObs;

    private boolean mMapSet;
    private boolean mTextureSet;

    private TextureShaderProgram mTextureProgram;
    private ColorShaderProgram mColorProgram;
    private GenericCallback mCallback;

    private int mTexture;

    public TerrainRenderer(Context ctx, GenericCallback cb) {
        mContext = ctx;
        mCallback = cb;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Set line to be thick and dependant on dpi
        glLineWidth(Helper.getDpiToPix(mContext) * 3);

        // hide surfaces
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);//less or equal, use with DEPTH_BUFFER_BIT

        mMap = new Map();
        mShip = new Ship();
        mObs = new Obstacles();
        mOwnShip = new OwnShip();
        mCamera = new Camera();
        mOrientation = new Orientation();
        mTextureSet = false;
        mMapSet = false;

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

        mCallback.callback(this, DRAW_FRAME);

        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        MatrixHelper.perspectiveM(mProjectionMatrix, mOrientation.getViewAngle(), (float) mWidth
                / (float) mHeight, 0.001f, 10f);

        //Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mOrientation.getDisplacementX(mCamera.isFirstPerson()),
                mOrientation.getDisplacementY(mCamera.isFirstPerson()), 0.0f);
        Matrix.rotateM(mModelMatrix, 0, mOrientation.getMapRotation(mCamera.isFirstPerson()), 0.0f, 0.0f, 1.0f);


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
            mTextureProgram.setUniformsHeight(
                    (float)Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE,
                    (float)Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT,
                    mMap.getRatio() / (float)Helper.ALTITUDE_FT_ELEVATION_PLUSZ,
                    mAltitude);
            mMap.bindData(mTextureProgram);
            mMap.draw();

            // Draw the ships
            mColorProgram.useProgram();
            mColorProgram.setUniforms(mMVPMatrix);
            mShip.bindData(mColorProgram);
            mShip.draw();

            // Draw the obstacles
            mObs.bindData(mColorProgram);
            mObs.draw();

            // Draw the ownship
            mOwnShip.bindData(mColorProgram);
            mOwnShip.draw();

        }
    }

    public void setTexture(BitmapHolder b) {
        mTexture = TextureHelper.loadTexture(b);
        mTextureSet = mTexture != 0;
    }

    public void setTerrain(short vertexArray[], float ratio) {
        mMapSet = mMap.loadTerrain(vertexArray, ratio);
    }

    public void setAltitude(float alt) {
        mAltitude = alt;
    }


    public void setShips(Vector4d traffic[]) {
        if(traffic != null) {
            mShip.initShips(traffic.length);
            for (Vector4d t : traffic) {
                mShip.addShip(t.getX(), t.getY(), t.getZ(), t.getAngle()); // others
            }
        }
        else {
            mShip.initShips(0);
        }
        mShip.doneShips();
    }

    public void setOwnShip(Vector4d self) {
        mOwnShip.initOwnShip(self.getX(), self.getY(), self.getZ(), self.getAngle());
        mOwnShip.doneOwnShips();
    }


    public void setObstacles(Vector4d[] obstacles) {
        if(obstacles != null) {
            mObs.initObstacles(obstacles.length);
            for (Vector4d o : obstacles) {
                mObs.addObstacles(o.getX(), o.getY(), o.getZ());
            }
        }
        else {
            mObs.initObstacles(0);
        }
        mObs.doneObstacles();
    }

    public Camera getCamera() {
        return mCamera;
    }

    public Orientation getOrientation() {
        return mOrientation;
    }

    public boolean isMapSet() {
        return mMapSet;
    }

    public boolean isTextureSet() {
        return mTextureSet;
    }

    public float getElevationNormalized(int row, int col, float ratio) {
        return mMap.getZ(row, col, ratio);
    }
}


