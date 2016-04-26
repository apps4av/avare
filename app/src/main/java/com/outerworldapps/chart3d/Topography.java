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

import android.os.StrictMode;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Contains topography info (ground elevation at a given lat/lon).
 */
public class Topography {
    public final static String TAG = "WairToNow";
    public final static short INVALID_ELEV = (short) 0x8000;

    public static String dbdir;
    public final static String dldir = "http://toutatis.nii.net/WairToNow";

    private final static HashMap<Integer,short[]> loadedTopos = new HashMap<> ();

    private static boolean initialised;

    /**
     * Get elevation in metres at a given lat/lon.
     */
    public static int getElevMetres (double lat, double lon)
    {
        if (!initialised) {
            initialised = true;
            // downloads tiles in main thread; change to background thread
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder ().permitAll ().build ();
                StrictMode.setThreadPolicy (policy);
            }
        }

        /*
         * Split given lat,lon into degrees and minutes.
         */
        int ilatmin = (int) Math.round (lat * 60.0) + 60000;
        int ilonmin = (int) Math.round (lon * 60.0) + 60000;
        int ilatdeg = ilatmin / 60 - 1000;
        int ilondeg = ilonmin / 60 - 1000;

        if (ilatdeg < -90) return INVALID_ELEV;
        if (ilatdeg >= 90) return INVALID_ELEV;

        if (ilondeg < -180) ilondeg += 360;
        if (ilondeg >= 180) ilondeg -= 360;

        /*
         * See if corresponding file is already loaded in memory.
         * If not, put on requested queue and return that it is invalid.
         */
        int key = (ilatdeg << 16) + (ilondeg & 0xFFFF);
        short[] topos;
        synchronized (loadedTopos) {
            if (!loadedTopos.containsKey (key)) {
                topos = loadFile (ilatdeg, ilondeg);
                loadedTopos.put (key, topos);
            }
            topos = loadedTopos.get (key);
        }
        if (topos == null) {
            return INVALID_ELEV;
        }

        /*
         * Loaded in memory, return value.
         */
        ilatmin %= 60;
        ilonmin %= 60;
        return topos[ilatmin*60+ilonmin];
    }

    public static void purge ()
    {
        synchronized (loadedTopos) {
            loadedTopos.clear ();
        }
    }

    /**
     * Read topo file into memory, downloading from web server if necessary.
     * @param ilatdeg = latitude, -90..89
     * @param ilondeg = longitude, -180..179
     * @return null: can't read file
     *         else: shorts giving metres msl at each minute [ilatmin*60+ilonmin]
     */
    private static short[] loadFile (int ilatdeg, int ilondeg)
    {
        final int bsize = 7200;
        byte[] bytes = new byte[bsize];
        int rc;

        /*
         * Build filename and see if it exists.
         */
        String subname = "/datums/topo/" + ilatdeg + "/" + ilondeg;
        String name = dbdir + subname;
        File file = new File (name);
        if (!file.exists ()) {
            Log.d ("Topography", "downloading " + name);
            try {

                /*
                 * File doesn't exist, read data from web server.
                 */
                URL url = new URL (dldir + subname);
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection ();
                httpCon.setRequestMethod ("GET");
                httpCon.connect ();
                rc = httpCon.getResponseCode ();
                if (rc != HttpURLConnection.HTTP_OK) {
                    throw new IOException ("http response code " + rc);
                }
                InputStream wis = httpCon.getInputStream ();
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    for (int ofs = 0; ofs < bsize; ofs += rc) {
                        rc = wis.read (bytes, ofs, bsize - ofs);
                        if (rc <= 0) throw new IOException ("read web file error");
                    }
                } finally {
                    wis.close ();
                }

                /*
                 * Write data to local file.
                 */
                File dir = file.getParentFile ();
                if (!dir.exists () && !dir.mkdirs ()) {
                    throw new IOException ("mkdirs " + dir.getPath () + " failed");
                }
                FileOutputStream fos = new FileOutputStream (file);
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    fos.write (bytes);
                } finally {
                    fos.close ();
                }
            } catch (IOException ioe) {
                Log.e (TAG, "error downloading " + name, ioe);
                return null;
            }
        }

        /*
         * File (now) exists, read into memory and put in cache.
         */
        try {
            FileInputStream fis = new FileInputStream (file);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                rc = fis.read (bytes, 0, bsize);
                if (rc != bsize) {
                    throw new IOException ("only read " + rc + " of " + bsize + " bytes");
                }
                short[] shorts = new short[bsize/2];
                int j = 0;
                for (int i = 0; i < bsize / 2; i ++) {
                    int lo = bytes[j++] & 0xFF;
                    int hi = bytes[j++] & 0xFF;
                    shorts[i] = (short) ((hi << 8) + lo);
                }
                return shorts;
            } finally {
                fis.close ();
            }
        } catch (IOException ioe) {
            Log.e (TAG, "error reading " + name, ioe);
            return null;
        }
    }
}
