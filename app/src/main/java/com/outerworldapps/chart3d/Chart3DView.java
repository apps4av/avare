//    Copyright (C) 2016, Mike Rieker, Beverly, MA USA
//    www.outerworldapps.com
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; version 2 of the License.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    EXPECT it to FAIL when someone's HeALTh or PROpeRTy is at RISk.
//
//    You should have received a copy of the GNU General Public License
//    along with this program; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//    http://www.gnu.org/licenses/gpl-2.0.html

package com.outerworldapps.chart3d;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Show a chart as a 3D object.
 *
 * Instantiate one of these views and put it on screen somewhere.
 * Provide a flat chart that implements the DisplayableChart interface to the setMap() method.
 * Call either setLocation() or pointCamera() with GPS positioning.
 * Call recycle() when view no longer needed.
 *
 * Topography info is provided via Topography.getElevMetres().
 */
public class Chart3DView extends GLSurfaceView {
    public final static int BGCOLOR = 0x00BFFF;
    public final static double STEP = 0.25;
    public final static int MAXSECTORS = 100;

    private final Runnable sceneUpdater = new Runnable () {
        @Override
        public void run ()
        {
            myRenderer.updateScene ();
        }
    };

    private int mWidth, mHeight;
    private int slatStep, nlatStep, wlonStep, elonStep;
    private long glThreadID;
    private MyRenderer myRenderer;

    public Chart3DView(Context ctx)
    {
        super (ctx);

        myRenderer = new MyRenderer ();

        // request an OpenGL ES 2.0 compatible context
        setEGLContextClientVersion (2);

        // set how to draw onto the surface
        setRenderer (myRenderer);
    }

    @Override
    protected void finalize () throws Throwable
    {
        recycle ();
        super.finalize ();
    }

    /**
     * Say what map to use for texturing the surface.
     */
    public void setMap (DisplayableChart sm)
    {
        myRenderer.displayableChart = sm;
        queueEvent (sceneUpdater);
    }

    /**
     * Wrapper for pointCamera using a GPS-supplied location.
     */
    public void setLocation (double lat, double lon, double alt)
    {
        myRenderer.setLocation (lat, lon, alt);
    }

    /**
     * Point the camera.
     * @param position = position of the camera, usually same as airplane
     * @param upaxis = what the camera's up axis is, usually outward toward space
     * @param lookat = what direction the camera is looking at, usually forward along airplane's path
     */
    public void pointCamera (Vector3 position, Vector3 upaxis, Vector3 lookat)
    {
        myRenderer.pointCamera (position, upaxis, lookat);
    }

    /**
     * All done with this object, release all resources.
     * Object not usable any more after this.
     */
    public void recycle ()
    {
        if ((glThreadID != 0) && (Thread.currentThread ().getId () != glThreadID)) {
            queueEvent (new Runnable () {
                @Override
                public void run ()
                {
                    recycle ();
                }
            });
        } else {
            myRenderer.recycle ();
            myRenderer = null;

            Topography.purge ();
        }
    }

    private static int createProgram (String vertexSource, String fragmentSource)
    {
        int vertexShader = loadShader (GLES20.GL_VERTEX_SHADER, vertexSource);
        try {
            int pixelShader = loadShader (GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            try {
                int program = GLES20.glCreateProgram ();
                if (program == 0) throw new RuntimeException ("glCreateProgram failed");

                GLES20.glAttachShader (program, vertexShader);
                checkGlError ("glAttachShader");
                GLES20.glAttachShader (program, pixelShader);
                checkGlError ("glAttachShader");
                GLES20.glLinkProgram (program);

                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv (program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    String msg = GLES20.glGetProgramInfoLog (program);
                    GLES20.glDeleteProgram (program);
                    throw new RuntimeException ("Could not link program: " + msg);
                }
                return program;
            } finally {
                GLES20.glDeleteShader (pixelShader);
            }
        } finally {
            GLES20.glDeleteShader (vertexShader);
        }
    }

    private static int loadShader (int shaderType, String source)
    {
        int shader = GLES20.glCreateShader (shaderType);
        if (shader == 0) throw new RuntimeException ("glCreateShader failed");

        GLES20.glShaderSource (shader, source);
        GLES20.glCompileShader (shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv (shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String msg = GLES20.glGetShaderInfoLog (shader);
            GLES20.glDeleteShader (shader);
            throw new RuntimeException ("Could not compile shader " + shaderType + ":" + msg);
        }

        return shader;
    }

    private static void checkGlError (String op)
    {
        int error = GLES20.glGetError ();
        if (error != GLES20.GL_NO_ERROR) {
            throw new RuntimeException (op + ": glError " + error);
        }
    }

    private class MyRenderer implements Renderer {
        private final static int FLOAT_SIZE_BYTES = 4;
        private final static int TRIANGLES_DATA_STRIDE = 5;
        private final static int TRIANGLES_DATA_POS_OFFSET = 0;
        private final static int TRIANGLES_DATA_UV_OFFSET = 3;
        private final static int TRIANGLES_DATA_STRIDE_BYTES = TRIANGLES_DATA_STRIDE * FLOAT_SIZE_BYTES;

        private final static String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTextureCoord = aTextureCoord;\n" +
                        "}\n";

        private final static String mFragmentShader =
                "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform sampler2D sTexture;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";

        private final Runnable cameraPointer = new Runnable () {
            @Override
            public void run ()
            {
                // make copy of values so updateCamera() has stable values to work with
                synchronized (camPosLock) {
                    cameraPos.set (newCamPos);
                    cameraUp.set (newCamUp);
                    cameraLook.set (newCamLook);
                    newCamQueued = false;
                }

                // remember that the camera has been pointed somewhere
                cameraPointed = true;

                // update camera (assuming we know screen dimensions)
                if (mHeight != 0) {
                    updateCamera ();
                }
            }
        };

        private final Object camPosLock = new Object ();

        private int mProgramHandle;
        private int muMVPMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        private boolean cameraPointed;
        private boolean newCamQueued;
        private boolean surfaceCreated;
        private boolean[] visibles;
        private float[] mIdnMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];
        private EarthSector closeSectors;
        private EarthSector knownSectors;
        private Point screenpoint = new Point ();           // gl thread only
        private DisplayableChart displayableChart;
        private Vector3 latlonpoint = new Vector3 ();       // gl thread only
        private Vector3 cameraLook = new Vector3 ();        // gl thread only
        private Vector3 cameraPos = new Vector3 ();
        private Vector3 cameraUp = new Vector3 ();
        private Vector3 newCamLook = new Vector3 ();        // camPosLock only
        private Vector3 newCamPos = new Vector3 ();
        private Vector3 newCamUp = new Vector3 ();

        public MyRenderer ()
        {
            Matrix.setIdentityM (mIdnMatrix, 0);
        }

        @Override
        protected void finalize () throws Throwable
        {
            recycle ();
            super.finalize ();
        }

        /**
         * All done with this object, free everything off.
         */
        public void recycle ()
        {
            for (EarthSector es = closeSectors; es != null; es = es.nextDist) {
                es.recycle ();
            }
            closeSectors = null;
            displayableChart = null;
            cameraPointed = false;
            surfaceCreated = false;
            visibles = null;
            mHeight = 0;

            GLES20.glDeleteProgram (mProgramHandle);
            mProgramHandle = 0;
            muMVPMatrixHandle = 0;
            maPositionHandle = 0;
            maTextureHandle = 0;
        }

        /**
         * Wrapper for pointCamera using a GPS-supplied location.
         */
        public void setLocation (double lat, double lon, double alt)
        {
            synchronized (myRenderer.camPosLock) {
                newCamPos.set (newCamLook);
                newCamUp.set (newCamLook);
                EarthSector.LatLonAlt2XYZ (lat, lon, alt, newCamLook);
                if (!newCamQueued) {
                    newCamQueued = true;
                    queueEvent (cameraPointer);
                }
            }
        }

        /**
         * Point the camera at something and adjust display.
         * Can be called in any thread.
         * @param position = location of the camera
         * @param upaxis = the up-axis of the camera
         * @param lookat = what the camera is pointed at
         */
        public void pointCamera (Vector3 position, Vector3 upaxis, Vector3 lookat)
        {
            synchronized (myRenderer.camPosLock) {
                newCamPos.set (position);
                newCamUp.set (upaxis);
                newCamLook.set (lookat);
                if (!newCamQueued) {
                    newCamQueued = true;
                    queueEvent (cameraPointer);
                }
            }
        }

        /**
         * Surface was created, do one-time initializations in GL thread.
         */
        @Override
        public void onSurfaceCreated (GL10 glUnused, EGLConfig config)
        {
            glThreadID = Thread.currentThread ().getId ();

            // don't display backsides of triangles
            // and don't display if something in front of it
            GLES20.glEnable (GLES20.GL_CULL_FACE);
            GLES20.glEnable (GLES20.GL_DEPTH_TEST);

            // compile the rendering program and get references to variables therein
            mProgramHandle    = createProgram (mVertexShader, mFragmentShader);
            maPositionHandle  = getAttrLoc ("aPosition");
            maTextureHandle   = getAttrLoc ("aTextureCoord");
            muMVPMatrixHandle = getUnifLoc ("uMVPMatrix");

            //TODO mBallTextureID = MakeTextureFromBitmap ("robot.png");
            //TODO mBallTriangles = MakeSphere (1.0, 1.0, 0.0, 0.1, 15);

            //TODO mEarthTextureID = MakeTextureFromBitmap ("earthtruecolor_nasa_big_stripe.jpg");
            //TODO mEarthTriangles = MakeSphere (0.0, 0.0, 0.0, 1.0, 60);

            // try to draw the scene now
            surfaceCreated = true;
            updateScene ();
        }

        private int getAttrLoc (String attrname)
        {
            int handle = GLES20.glGetAttribLocation (mProgramHandle, attrname);
            checkGlError ("glGetAttribLocation " + attrname);
            if (handle == -1) {
                throw new RuntimeException ("could not get attrib location for " + attrname);
            }
            return handle;
        }

        private int getUnifLoc (String attrname)
        {
            int handle = GLES20.glGetUniformLocation (mProgramHandle, attrname);
            checkGlError ("glGetUniformLocation " + attrname);
            if (handle == -1) {
                throw new RuntimeException ("could not get uniform location for " + attrname);
            }
            return handle;
        }

        /**
         * Size of the surface first known or has changed.
         */
        @Override
        public void onSurfaceChanged (GL10 glUnused, int width, int height)
        {
            // set what part of canvas the world is mapped to
            // 0,0 is in lower left corner (not upper left corner)
            GLES20.glViewport (0, 0, width, height);

            // save screen dimensions
            mWidth  = width;
            mHeight = height;

            // maybe update camera
            if (cameraPointed) {
                updateCamera ();
            }
        }

        /**
         * Draw triangles to the screen.
         */
        @Override
        public void onDrawFrame (GL10 glUnused)
        {
            GLES20.glClearColor (
                    Color.red (BGCOLOR) / 255.0F,
                    Color.green (BGCOLOR) / 255.0F,
                    Color.blue (BGCOLOR) / 255.0F, 1.0F);
            GLES20.glClear (GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram (mProgramHandle);
            checkGlError ("glUseProgram");

            GLES20.glUniformMatrix4fv (muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            EarthSector.ClearLoader ();
            int nobjs = 0;
            for (EarthSector es = closeSectors; es != null; es = es.nextDist) {
                if (es.setup ()) {
                    drawAnObject (es.getTextureID (), es.getTriangles ());
                }
                if (++ nobjs >= MAXSECTORS) break;
            }

            //TODO drawAnObject (mEarthTextureID, mEarthTriangles);
            //TODO drawAnObject (mBallTextureID, mBallTriangles);
        }

        /**
         * Draw the object with the given texture.
         */
        private void drawAnObject (int textureID, FloatBuffer triangles)
        {
            // select texture which has been bound to the bitmap in onSurfaceCreated()
            GLES20.glActiveTexture (GLES20.GL_TEXTURE0);
            GLES20.glBindTexture (GLES20.GL_TEXTURE_2D, textureID);

            // tell program where to get vertex x,y,z's from (aPosition)
            triangles.position (TRIANGLES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer (maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLES_DATA_STRIDE_BYTES, triangles);
            checkGlError ("glVertexAttribPointer maPosition");

            // tell program where to get image u,v's from (aPosition)
            triangles.position (TRIANGLES_DATA_UV_OFFSET);
            GLES20.glEnableVertexAttribArray (maPositionHandle);
            checkGlError ("glEnableVertexAttribArray maPositionHandle");

            GLES20.glVertexAttribPointer (maTextureHandle, 2, GLES20.GL_FLOAT, false,
                    TRIANGLES_DATA_STRIDE_BYTES, triangles);
            checkGlError ("glVertexAttribPointer maTextureHandle");

            GLES20.glEnableVertexAttribArray (maTextureHandle);
            checkGlError ("glEnableVertexAttribArray maTextureHandle");

            GLES20.glDrawArrays (GLES20.GL_TRIANGLES, 0,
                    triangles.capacity () / TRIANGLES_DATA_STRIDE);
            checkGlError ("glDrawArrays");
        }

        /**
         * Update camera to position described by cameraPos,cameraLook,cameraUp.
         */
        public void updateCamera ()
        {
            assertGLThread ();

            /*
             * Calculate lat,lon,alt of camera
             */
            EarthSector.XYZ2LatLonAlt (cameraPos, latlonpoint);
            double camlat = latlonpoint.x;
            double camlon = latlonpoint.y;

            /*
             * Calculate how many degrees along earth surface can be seen from camera
             * to earth's edge assuming earth's edge is at sea level, no matter where
             * the camera is pointed.
             */
            double camang = Math.acos (Lib.EARTHRADIUS / (latlonpoint.z + Lib.EARTHRADIUS));

            /*
             * See how many degrees away Mt. Everest can be and still see the peak from sea level.
             * About 3 deg or 180 nm.
             */
            double mevang = Math.acos (Lib.EARTHRADIUS / (Lib.MTEVEREST + Lib.EARTHRADIUS));

            /*
             * See how far away from the camera Mt. Everest could possibly be to see the peak
             * that is not obscured by a sea-level surface between camera and peak.
             */
            double sightnm = Math.toDegrees (camang + mevang) * Lib.NMPerDeg;

            /*
             * Set up camera frustum planes so that the camera can see that far away.
             */
            float neardist = 1.0F / 16384;
            float fardist = (float) (sightnm * Lib.MPerNM / Lib.EARTHRADIUS);

            /*
             * Set up camera matrix.
             */
            float aspect = (float) mHeight / (float) mWidth;
            float[] projMatrix = new float[16];
            Matrix.frustumM (projMatrix, 0,
                    -neardist, neardist,                    // left, right,
                    -aspect * neardist, aspect * neardist,  // bottom, top
                    neardist, fardist);                     // near, far

            float[] viewMatrix = new float[16];
            Matrix.setLookAtM (viewMatrix, 0,
                    (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z,
                    (float) cameraLook.x, (float) cameraLook.y, (float) cameraLook.z,
                    (float) cameraUp.x, (float) cameraUp.y, (float) cameraUp.z);

            Matrix.multiplyMM (mMVPMatrix, 0, projMatrix, 0, viewMatrix, 0);

            /*
             * Get range of lat/lon (whole STEPs) visible to camera.
             * We can't possibly see anything farther away than this.
             */
            double camlatcos = Math.cos (Math.toRadians (camlat));
            slatStep = (int) Math.floor ((camlat - sightnm / Lib.NMPerDeg) / STEP);
            nlatStep = (int) Math.ceil  ((camlat + sightnm / Lib.NMPerDeg) / STEP);
            wlonStep = (int) Math.floor ((camlon - sightnm / Lib.NMPerDeg * camlatcos) / STEP);
            elonStep = (int) Math.ceil  ((camlon + sightnm / Lib.NMPerDeg * camlatcos) / STEP);

            /*
             * Make matrix of visible step-by-step-sized squares within that range.
             */
            int rowlen = elonStep - wlonStep + 1;
            int visize = (nlatStep - slatStep + 1) * rowlen;
            if ((visibles == null) || (visibles.length < visize)) {
                visibles = new boolean[visize];
            } else {
                Arrays.fill (visibles, false);
            }

            for (int ilat = slatStep; ilat <= nlatStep; ilat ++) {
                for (int ilon = wlonStep; ilon <= elonStep; ilon++) {
                    double lat = ilat * STEP;
                    double lon = ilon * STEP;

                    /*
                     * Don't bother if more than sightnm away from camera
                     * cuz we can't possibly see farther than that.
                     */
                    double nmfromcam = Lib.LatLonDist (camlat, camlon, lat, lon);
                    if (nmfromcam > sightnm) continue;

                    int alt = Topography.getElevMetres (lat, lon);
                    EarthSector.LatLonAlt2XYZ (lat, lon, alt, latlonpoint);

                    int i = (ilat - slatStep) * rowlen + (ilon - wlonStep);
                    visibles[i] = WorldXYZ2PixelXY (latlonpoint, screenpoint);
                }
            }

            // make sector where camera is always visible so we don't get an edge case
            int i = ((int) (camlat / STEP) - slatStep) * rowlen + ((int) (camlon / STEP) - wlonStep);
            if ((i >= 0) && (i < visize)) visibles[i] = true;

            /*
             * Consolidate the four corners into the SW corner,
             * ie, if any of four corners is visible, the whole
             * square is visible.
             */
            for (int ilat = slatStep; ilat < nlatStep; ilat ++) {
                for (int ilon = wlonStep; ilon < elonStep; ilon++) {
                    i = (ilat - slatStep) * rowlen + (ilon - wlonStep);
                    visibles[i] |= visibles[i+1] |
                            visibles[i+rowlen] | visibles[i+1+rowlen];
                }
            }

            /*
             * Update scene accordingly.
             */
            updateScene ();
        }

        /**
         * Update scene objects to cover area viewable by the camera.
         * The earth sphere can't cover the entire globe as it would take way too much memory,
         * so it needs to be adjusted to define only the parts of the earth that is visible to
         * the camera.
         */
        private void updateScene ()
        {
            /*
             * Make sure we are all initialized.
             */
            if ((displayableChart == null) || !cameraPointed || !surfaceCreated || (mHeight == 0)) return;

            assertGLThread ();

            /*
             * Try once, purge everything if oom and try just once again.
             */
            try {
                updateSceneWork ();
            } catch (OutOfMemoryError oome) {
                Log.w ("Chart3DView", "first out of memory error");
                Topography.purge ();
                for (EarthSector es = closeSectors; es != null; es = es.nextDist) {
                    es.recycle ();
                }
                closeSectors = null;
                System.gc ();
                System.runFinalization ();
                try {
                    updateSceneWork ();
                } catch (OutOfMemoryError oome2) {
                    Log.w ("Chart3DView", "second out of memory error");
                }
            }
        }

        private void updateSceneWork ()
        {
            /*
             * Calculate lat,lon,alt of camera
             */
            EarthSector.XYZ2LatLonAlt (cameraPos, latlonpoint);
            double camlat = latlonpoint.x;
            double camlon = latlonpoint.y;

            /*
             * Mark any loaded sectors as unreferenced cuz we will mark them if still needed.
             */
            for (EarthSector es = closeSectors; es != null; es = es.nextDist) {
                es.refd = false;
                es.nextAll = knownSectors;
                knownSectors = es;
            }
            closeSectors = null;

            /*
             * Loop through all lat/lon in view of the camera.
             * Find the MAXSECTORS ones closest to the camera.
             */
            int rowlen = elonStep - wlonStep + 1;
            for (int ilat = slatStep; ilat <= nlatStep; ilat++) {
                for (int ilon = wlonStep; ilon <= elonStep; ilon++) {
                    if (!visibles[(ilat - slatStep) * rowlen + (ilon - wlonStep)]) continue;

                    /*
                     * Make sure we have the earth sector object for that lat/lon.
                     * Don't waste time reading in the bitmap cuz we might not want it.
                     */
                    int hashkey = (ilon << 16) + (ilat & 0xFFFF);
                    EarthSector sector;
                    for (sector = knownSectors; sector != null; sector = sector.nextAll) {
                        if (sector.hashkey == hashkey) break;
                    }
                    if (sector == null) {
                        sector = new EarthSector (displayableChart,
                                ilat * STEP, (ilat + 1) * STEP,
                                ilon * STEP, (ilon + 1) * STEP);
                        sector.hashkey = hashkey;
                        sector.nextAll = knownSectors;
                        knownSectors = sector;
                    }

                    /*
                     * Remember how far it is from the camera
                     * and put in list sorted by distance.
                     */
                    double nmfromcam = Lib.LatLonDist (camlat, camlon, sector.slat, sector.wlon);
                    sector.nmfromcam = nmfromcam;
                    EarthSector prev, next;
                    prev = null;
                    for (next = closeSectors; next != null; next = next.nextDist) {
                        if (next.nmfromcam > nmfromcam) break;
                        prev = next;
                    }
                    if (prev == null) closeSectors = sector;
                                else prev.nextDist = sector;
                    sector.nextDist = next;

                    /*
                     * Mark it as referenced as it is in view of the camera.
                     */
                    sector.refd = true;
                }
            }

            /*
             * Destroy any unreferenced sector objects to save memory.
             * These are ones not in view of camera.
             */
            for (EarthSector sector = knownSectors; sector != null; sector = sector.nextAll) {
                if (!sector.refd) {
                    sector.recycle ();
                }
            }
            knownSectors = null;
        }

        /**
         * Convert a world XYZ to an on-the-screen pixel XY
         * @return whether the point is visible or not
         */
        private boolean WorldXYZ2PixelXY (Vector3 xyz, Point xy)
        {
            // simplified version of GLU.gluProject()
            float[] m = mMVPMatrix;
            double rawx = m[ 0] * xyz.x + m[ 4] * xyz.y + m[ 8] * xyz.z + m[12];
            double rawy = m[ 1] * xyz.x + m[ 5] * xyz.y + m[ 9] * xyz.z + m[13];
            double rawh = m[ 3] * xyz.x + m[ 7] * xyz.y + m[11] * xyz.z + m[15];
            if (rawh == 0) return false;
            rawx = (1 + rawx / rawh) / 2 * mWidth;
            rawy = (1 - rawy / rawh) / 2 * mHeight;
            if (rawx < Integer.MIN_VALUE + 1) rawx = Integer.MIN_VALUE + 1;
            if (rawy < Integer.MIN_VALUE + 1) rawy = Integer.MIN_VALUE + 1;
            if (rawx > Integer.MAX_VALUE - 1) rawx = Integer.MAX_VALUE - 1;
            if (rawy > Integer.MAX_VALUE - 1) rawy = Integer.MAX_VALUE - 1;
            xy.x = (int) Math.round (rawx);
            xy.y = (int) Math.round (rawy);
            double dot =
                    (cameraLook.x - cameraPos.x) * (xyz.x - cameraPos.x) +
                    (cameraLook.y - cameraPos.y) * (xyz.y - cameraPos.y) +
                    (cameraLook.z - cameraPos.z) * (xyz.z - cameraPos.z);
            return (dot > 0) && (xy.x >= 0) && (xy.x < mWidth) && (xy.y >= 0) && (xy.y < mHeight);
        }
    }

    /**
     * Make sure the current thread is the OpenGL thread
     * which means it is safe to call the GLES20 functions.
     */
    private void assertGLThread ()
    {
        long thisid = Thread.currentThread ().getId ();
        if (thisid != glThreadID) {
            throw new RuntimeException ("not on GL thread, thisid=" + thisid + ", glthreadid=" + glThreadID);
        }
    }
}
