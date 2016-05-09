package com.ds.avare.threed.data;

/**
 * Created by zkhan on 5/9/16.
 */
public class Vector3d {
    private float mX;
    private float mY;
    private float mZ;

    public Vector3d(float x, float y, float z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    public void set(Vector3d in) {
        mX = in.getX();
        mY = in.getY();
        mZ = in.getZ();
    }

    public float getX() {
        return mX;
    }
    public float getY() {
        return mY;
    }
    public float getZ() {
        return mZ;
    }
}
