/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare.threed.util;

import android.opengl.Matrix;

import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Projection;
import com.ds.avare.threed.AreaMapper;
import com.ds.avare.threed.data.Vector3d;

/**
 * Created by zkhan on 5/9/16.
 */
public class Camera {
    private Vector3d mCameraPos;
    private Vector3d mCameraLook;
    private Vector3d mCameraUp;
    private boolean mFirstPerson;

    /**
     * Camera always up +ve z axis
     */
    public Camera() {
        mCameraPos = new Vector3d(0, 0, 0);
        mCameraLook = new Vector3d(0, 0, 0);
        mCameraUp = new Vector3d(0, 0, 0);
        mFirstPerson = false;
    }

    /**
     * Set camera position and target
     */
    public void set(AreaMapper mapper, Orientation orientation) {
        Vector3d pos, look;
        if (mFirstPerson) {
            pos = getCameraVectorPositionFirstPerson(mapper);
            look = getCameraVectorLookAtFirstPerson(mapper);
            // let user rotate around ownship to see whats there
            // angle Z rotate is reverse of satellite view because this is first person
            // angle x rotate, this is pitch
            MatrixHelper.rotatePoint(pos.getX(), pos.getY(), pos.getZ(),
                    orientation.getRotationX(true), look.getVectorArray(), look.getVectorArrayScratch(), 0,
                    1, 0, 0);
            float out[] = look.getVectorArrayScratch();
            look = new Vector3d(out[0], out[1], out[2]);
            // this is yaw
            MatrixHelper.rotatePoint(pos.getX(), pos.getY(), pos.getZ(),
                    orientation.getRotationZ(true), look.getVectorArray(), look.getVectorArrayScratch(), 0,
                    0, 0, 1);
            out = look.getVectorArrayScratch();
            look = new Vector3d(out[0], out[1], out[2]);
        }
        else {
            pos = Camera.getCameraVectorPosition();
            look = Camera.getCameraVectorLookAt();
        }

        Vector3d up = new Vector3d(pos.getX(), pos.getY(), 1000f);
        mCameraPos.set(pos);
        mCameraLook.set(look);
        mCameraUp.set(up); // do not rotate up vector, assume no bank
    }

    /**
     * Get view matrix from camera position and target
     * @param viewMatrix
     */
    public void setViewMatrix(float[] viewMatrix) {
        Matrix.setLookAtM(viewMatrix, 0,
                mCameraPos.getX(), mCameraPos.getY(), mCameraPos.getZ(),
                mCameraLook.getX(), mCameraLook.getY(), mCameraLook.getZ(),
                mCameraUp.getX(), mCameraUp.getY(), mCameraUp.getZ());

    }

    public static Vector3d getCameraVectorLookAtFirstPerson(AreaMapper map) {
        GpsParams params = map.getGpsParams();
        // Find a point ahead on horizon in bearing direction and look at it
        Coordinate c = Projection.findStaticPoint(params.getLongitude(), params.getLatitude(),
                params.getBearing(), Projection.horizonDistance(params.getAltitude()));
        Vector3d vec = map.gpsToAxis(c.getLongitude(), c.getLatitude(), params.getAltitude(), 0);
        Vector3d cameraVectorLookAt = new Vector3d(vec.getX(), vec.getY(), vec.getZ());
        return cameraVectorLookAt;
    }

    public static Vector3d getCameraVectorPositionFirstPerson(AreaMapper map) {
        // camera is where ownship is
        GpsParams params = map.getGpsParams();
        Vector3d vec = map.gpsToAxis(params.getLongitude(), params.getLatitude(), params.getAltitude(), 0);
        Vector3d cameraVectorPosition = new Vector3d(vec.getX(), vec.getY(), vec.getZ());
        return cameraVectorPosition;
    }

    public static Vector3d getCameraVectorLookAt() {
        // Bird eye camera
        Vector3d cameraVectorLookAt = new Vector3d(0, 1f, 0);
        return cameraVectorLookAt;
    }

    public static Vector3d getCameraVectorPosition() {
        // to south and high up
        Vector3d cameraVectorPosition = new Vector3d(0, -1f, 1f);
        return cameraVectorPosition;
    }

    public void setFirstPerson(boolean fp) {
        mFirstPerson = fp;
    }

    public boolean isFirstPerson() {
        return mFirstPerson;
    }
}
