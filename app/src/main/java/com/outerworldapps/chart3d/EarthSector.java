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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * 3D sector of the earth.
 */
public class EarthSector {
    public final static int FUNNYFACTOR = 4;

    private final static int FLOAT_SIZE_BYTES = 4;
    private final static int TRIANGLES_DATA_STRIDE = 5;
    private final static int TRIANGLES_DATA_STRIDE_BYTES = TRIANGLES_DATA_STRIDE * FLOAT_SIZE_BYTES;

    private final static Object loaderLock = new Object ();
    private static LinkedList<EarthSector> loaderSectors = new LinkedList<> ();
    private static LoaderThread loaderThread;

    public boolean refd;        // seen by camera and is one of MAXSECTORS closest to camera
    public double slat, wlon;   // southwest corner lat/lon
    public double nlat, elon;   // northeast corner lat/lon
    public double nmfromcam;    // naut miles from camera
    public int swelev;          // elevation (metres msl) of southwest corner
    public int hashkey;

    public EarthSector nextAll;
    public EarthSector nextDist;

    private Bitmap mBitmap;
    private boolean corrupt;
    private boolean loaded;
    private FloatBuffer mTriangles;
    private int mTextureID;
    private DisplayableChart displayableChart;

    public EarthSector(@NonNull DisplayableChart sm, double slat, double nlat, double wlon, double elon)
    {
        displayableChart = sm;
        this.slat = slat;
        this.nlat = nlat;
        this.wlon = wlon;
        this.elon = elon;
    }

    /**
     * Clear out all sectors queued for loading.
     * If one is in progress, leave it alone.
     */
    public static void ClearLoader ()
    {
        synchronized (loaderLock) {
            loaderSectors.clear ();
        }
    }

    /**
     * Set up the sector contents.
     * Can be called from any thread.
     * @return true if all sector contents are now valid
     */
    public boolean setup ()
    {
        synchronized (loaderLock) {
            if (corrupt) return false;
            if (loaded) return true;
            loaderSectors.addLast (this);
            if (loaderThread == null) {
                loaderThread = new LoaderThread ();
                loaderThread.start ();
            }
        }
        return false;
    }

    /**
     * Called in loader thread to load as much as we can in a non-GL thread.
     * We can read in the bitmap image and make the FloatBuffer with the vertices.
     * But we cannot allocate a texture-ID.
     */
    private void loadIt ()
    {
        try {
            mBitmap = displayableChart.getBitmap (slat, nlat, wlon, elon);
            mTriangles = MakeEarthSector (displayableChart, slat, nlat, wlon, elon);
            swelev = Topography.getElevMetres (slat, wlon);
            synchronized (loaderLock) {
                loaded = true;
            }
        } catch (Throwable t) {
            Log.e ("EarthSector", "error loading bitmap", t);
            recycle ();
            synchronized (loaderLock) {
                corrupt = true;
            }
        }
    }

    @Override
    protected void finalize () throws Throwable
    {
        recycle ();
        super.finalize ();
    }

    /**
     * All done with sector.
     * Should only be called from the GL thread.
     */
    public void recycle ()
    {
        if (mBitmap != null) {
            mBitmap.recycle ();
            mBitmap = null;
        }
        mTriangles = null;
        if (mTextureID != 0) {
            int[] textureIDs = new int[] { mTextureID };
            GLES20.glDeleteTextures (1, textureIDs, 0);
            mTextureID = 0;
        }
    }

    /**
     * Retrieve triangles that make up the sector.
     * Can be called from any thread.
     */
    public @NonNull
    FloatBuffer getTriangles ()
    {
        return mTriangles;
    }

    /**
     * Retrieve texture for the sector.
     * Should only be called from the GL thread.
     */
    public int getTextureID ()
    {
        if (mBitmap != null) {
            mTextureID = MakeTextureFromBitmap (mBitmap);
            mBitmap.recycle ();
            mBitmap = null;
        }
        return mTextureID;
    }

    /**
     * Generate triangles for a sector of the earth.
     */
    private static FloatBuffer MakeEarthSector (DisplayableChart sm, double slat, double nlat, double wlon, double elon)
    {
        // do vertices in steps of a minute cuz that's Topography.getElevMetres() resolution
        int islat = (int) Math.floor (slat * 60);
        int inlat = (int) Math.ceil (nlat * 60);
        int iwlon = (int) Math.floor (wlon * 60);
        int ielon = (int) Math.ceil (elon * 60);
        int nlats = inlat - islat;
        int nlons = ielon - iwlon;

        // calculate minutess per step to get at most 30 steps for lat,lon
        int maxsteps    = 30;
        int minsperlat  = (nlats + maxsteps - 1) / maxsteps;
        int minsperlon  = (nlons + maxsteps - 1) / maxsteps;
        int minsperstep = Math.max (minsperlat, minsperlon);
        nlats = (inlat - islat + minsperlat - 1) / minsperstep;
        nlons = (ielon - iwlon + minsperlon - 1) / minsperstep;
        inlat = islat + minsperstep * nlats;
        ielon = iwlon + minsperstep * nlons;

        // make array of vertices in x,y,z and where they are in the bitmap image.
        int numVertices = (nlats + 1) * (nlons + 1);
        float[] vertices = new float[numVertices * TRIANGLES_DATA_STRIDE];
        PointD xy = new PointD();
        Vector3 xyz = new Vector3();

        int k = 0;

        for (int ilat = islat; ilat <= inlat; ilat += minsperstep) {
            for (int ilon = iwlon; ilon <= ielon; ilon += minsperstep) {
                double lat = (double) ilat / 60.0;
                double lon = (double) ilon / 60.0;
                int alt = Topography.getElevMetres (lat, lon);
                if ((alt < 0) || (alt == Topography.INVALID_ELEV)) {
                    alt = 0;
                }

                // fill in x,y,z for the lat/lon
                LatLonAlt2XYZ (lat, lon, alt, xyz);
                vertices[k++] = (float) xyz.x;
                vertices[k++] = (float) xyz.y;
                vertices[k++] = (float) xyz.z;

                // fill in x,y of corresponding point of mBitmap
                sm.latLon2BitmapXY (lat, lon, xy);
                vertices[k++] = (float) xy.x;
                vertices[k++] = (float) xy.y;
            }
        }

        /*
         * Make triangles out of those vertices.
         * The vertices form squares and we make two triangles out of each square.
         */
        FloatBuffer triangles = ByteBuffer.allocateDirect (
                nlats * nlons * 6 * TRIANGLES_DATA_STRIDE_BYTES).
                order (ByteOrder.nativeOrder ()).asFloatBuffer ();
        triangles.position (0);

        for (int j = 0; j < nlats; j ++) {
            for (int i = 0; i < nlons; i ++) {
                int a = (nlons + 1) * (j + 1) + i + 1;
                //noinspection PointlessArithmeticExpression
                int b = (nlons + 1) * (j + 1) + i + 0;
                //noinspection PointlessArithmeticExpression
                int c = (nlons + 1) * (j + 0) + i + 0;
                //noinspection PointlessArithmeticExpression
                int d = (nlons + 1) * (j + 0) + i + 1;

                copyToTriangles (triangles, vertices, a);
                copyToTriangles (triangles, vertices, b);
                copyToTriangles (triangles, vertices, c);
                copyToTriangles (triangles, vertices, a);
                copyToTriangles (triangles, vertices, c);
                copyToTriangles (triangles, vertices, d);
            }
        }

        triangles.position (0);
        return triangles;
    }

    private static void copyToTriangles (FloatBuffer triangles, float[] squares, int m)
    {
        m *= TRIANGLES_DATA_STRIDE;
        triangles.put (squares[m++]);
        triangles.put (squares[m++]);
        triangles.put (squares[m++]);
        triangles.put (squares[m++]);
        triangles.put (squares[m]);
    }

    /**
     * Create an OpenGL texture from the bitmap.
     * Should only be called from the GL thread.
     */
    private static int MakeTextureFromBitmap (Bitmap bitmap)
    {
        int[] textures = new int[1];
        GLES20.glGenTextures (1, textures, 0);

        int textureID = textures[0];
        GLES20.glBindTexture (GLES20.GL_TEXTURE_2D, textureID);

        GLES20.glTexParameterf (GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf (GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri (GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri (GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        GLUtils.texImage2D (GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        return textureID;
    }

    /**
     * Convert lat, lon, alt to xyz
     * @param lat = latitude degrees
     * @param lon = longitude degrees
     * @param alt = altitude msl metres
     * @param xyz = x,y,z on the earthSphere
     *   +X comes out indian ocean
     *   +Y comes out north pole
     *   +Z comes out near ghana
     */
    public static void LatLonAlt2XYZ (double lat, double lon, double alt, @NonNull Vector3 xyz)
    {
        double r = alt * FUNNYFACTOR / Lib.EARTHRADIUS + 1.0;
        double latrad = Math.toRadians (lat);
        double lonrad = Math.toRadians (lon);
        double latcos = Math.cos (latrad);
        xyz.x = r * Math.sin (lonrad) * latcos;
        xyz.y = r * Math.sin (latrad);
        xyz.z = r * Math.cos (lonrad) * latcos;
    }

    /**
     * Convert xyz to lat, lon, alt
     * @param xyz = given x,y,z on the earthSphere
     * @param lla = x:lat (deg); y:lon (deg); z:alt (met msl)
     */
    public static void XYZ2LatLonAlt (@NonNull Vector3 xyz, @NonNull Vector3 lla)
    {
        double x = xyz.x;
        double y = xyz.y;
        double z = xyz.z;
        double r = Math.sqrt (x * x + y * y + z * z);
        double latrad = Math.atan2 (y, Math.hypot (x, z));
        @SuppressWarnings("SuspiciousNameCombination")
        double lonrad = Math.atan2 (x, z);
        lla.x = Math.toDegrees (latrad);
        lla.y = Math.toDegrees (lonrad);
        lla.z = (r - 1.0) * Lib.EARTHRADIUS * FUNNYFACTOR;
    }

    private static class LoaderThread extends Thread {
        @Override
        public void run ()
        {
            while (true) {
                EarthSector es;
                synchronized (loaderLock) {
                    if (loaderSectors.isEmpty ()) {
                        loaderThread = null;
                        return;
                    }
                    es = loaderSectors.removeFirst ();
                }
                es.loadIt ();
            }
        }
    }
}
