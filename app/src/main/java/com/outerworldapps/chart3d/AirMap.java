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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Map covering a specific aeronautical chart.
 */
public class AirMap implements DisplayableChart {
    public final static int TILESIZE = 384;  // server tile size
    public final static int BMSIZE = 512;    // created tile size (power-of-two)

    private double e_e, e_F_rada, e_lam0, e_phi0, e_n, e_rho0;
    private double tfw_a, tfw_b, tfw_c, tfw_d, tfw_e, tfw_f;
    private double wft_a, wft_b, wft_c, wft_d, wft_e, wft_f;
    private double pixelsize;
    private double eastmostlon, northmostlat, southmostlat, westmostlon;
    private int chartWidth, chartHeight;
    private int topytile;
    private int leftxtile;
    private int numxtiles;
    private int numytiles;
    private String spacename;     // eg 'Denver SEC'
    private String undervername;  // eg 'Denver_SEC_94'

    // spacename = "Denver SEC"
    public AirMap(String spacename) throws IOException
    {
        // downloads tiles in main thread; change to background thread
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy (policy);
        }

        this.spacename = spacename;
        DownloadMeta ();
    }

    /**
     * Get bitmap covering given lat/lon range.
     * Generated bitmap may actually cover more than what is given.
     */
    @Override  // DisplayableChart
    public Bitmap getBitmap (double slat, double nlat, double wlon, double elon) throws IOException
    {
        if (slat < southmostlat) slat = southmostlat;
        if (nlat > northmostlat) nlat = northmostlat;
        if (wlon < westmostlon)  wlon = westmostlon;
        if (elon > eastmostlon)  elon = eastmostlon;

        // see how many tiles wide and high to map the given lat/lon range
        double boty  = Math.max (getTileY (slat, wlon), getTileY (slat, elon));
        double topy  = Math.min (getTileY (nlat, wlon), getTileY (nlat, elon));
        double leftx = Math.min (getTileX (slat, wlon), getTileX (nlat, wlon));
        double ritex = Math.max (getTileX (slat, elon), getTileX (nlat, elon));

        // get tile number range
        int ltx  = (int) Math.floor (leftx);
        int tpy  = (int) Math.floor (topy);
        int numx = (int) Math.ceil (ritex) - ltx;
        int numy = (int) Math.ceil (boty)  - tpy;

        leftxtile = ltx;
        topytile  = tpy;
        numxtiles = numx;
        numytiles = numy;

        /*
         * Get a big bitmap to cover the whole range needed.
         */
        Bitmap bmbig = Bitmap.createBitmap (BMSIZE, BMSIZE, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas (bmbig);
        Rect dst = new Rect ();

        /*
         * Loop through each source tile needed to fill the big bitmap.
         */
        for (int ny = 0; ny < numytiles; ny ++) {
            for (int nx = 0; nx < numxtiles; nx ++) {
                int x = leftxtile + nx;
                int y = topytile  + ny;

                if (x * TILESIZE >= chartWidth)  continue;
                if (y * TILESIZE >= chartHeight) continue;

                /*
                 * Read tile bitmap from flash.
                 */
                File bitmapfile = new File (Topography.dbdir + "/charts/" + undervername + "/" + y + "/" + x + ".png");
                DownloadBitmapFile (bitmapfile);
                if (bitmapfile.exists ()) {
                    Bitmap bm = BitmapFactory.decodeFile (bitmapfile.getPath ());
                    if (bm == null) {
                        throw new IOException ("bitmap corrupt " + bitmapfile.getPath ());
                    }

                    /*
                     * Copy tile bitmap to big bitmap.
                     */
                    dst.left = nx * BMSIZE / numxtiles;
                    dst.right = (nx + 1) * BMSIZE / numxtiles;
                    dst.top = ny * BMSIZE / numytiles;
                    dst.bottom = (ny + 1) * BMSIZE / numytiles;
                    try {
                        canvas.drawBitmap (bm, null, dst, null);
                    } finally {
                        bm.recycle ();
                    }
                }
            }
        }

        return bmbig;
    }

    /**
     * Make sure we have the chart metadata downloaded.
     */
    private void DownloadMeta () throws IOException
    {
        String undername = spacename.replace (' ', '_');

        /*
         * Download list of files needed for this chart.
         * Then download all the files except for the tiles named in that list.
         */
        File chartsdatfile = new File (Topography.dbdir + "/charts/" + undername + ".dat");
        if (!chartsdatfile.exists ()) {
            File chartstmpfile = new File (chartsdatfile.getPath () + ".tmp");
            DownloadFile (chartstmpfile, "charts_filelist.php?undername=" + undername);
            BufferedReader rdr = new BufferedReader (new FileReader (chartstmpfile), 4096);
            for (String line; (line = rdr.readLine ()) != null;) {
                if (!line.endsWith (".png")) DownloadFile (new File (Topography.dbdir + "/" + line), line);
            }
            rdr.close ();
            if (!chartstmpfile.renameTo (chartsdatfile)) {
                throw new IOException ("rename error " + chartstmpfile.getPath () + " to " + chartsdatfile.getPath ());
            }
        }

        BufferedReader rdr = new BufferedReader (new FileReader (chartsdatfile), 4096);
        for (String line; (line = rdr.readLine ()) != null;) {
            if (line.startsWith ("charts/") && line.endsWith (".csv")) {
                undervername = line.substring (7, line.length () - 4);
            }
        }
        rdr.close ();

        /*
         * Decode the chart parameters from the .csv file.
         */
        File undercsvfile = new File (Topography.dbdir + "/charts/" + undervername + ".csv");
        String undercsvline = new BufferedReader (new FileReader (undercsvfile), 256).readLine ();
        String[] values = QuotedCSVSplit (undercsvline);

        double centerLat = Double.parseDouble (values[0]);
        double centerLon = Double.parseDouble (values[1]);
        double stanPar1 = Double.parseDouble (values[2]);
        double stanPar2  = Double.parseDouble (values[3]);
        chartWidth  = Integer.parseInt (values[4]);
        chartHeight = Integer.parseInt (values[5]);
        double rada = Double.parseDouble (values[6]);
        double radb = Double.parseDouble (values[7]);
        tfw_a = Double.parseDouble (values[8]);
        tfw_b = Double.parseDouble (values[9]);
        tfw_c = Double.parseDouble (values[10]);
        tfw_d = Double.parseDouble (values[11]);
        tfw_e = Double.parseDouble (values[12]);
        tfw_f = Double.parseDouble (values[13]);

        pixelsize = Math.hypot (tfw_b, tfw_d);
        e_lam0 = Math.toRadians (centerLon);
        e_phi0 = Math.toRadians (centerLat);
        double phi1 = Math.toRadians (stanPar1);
        double phi2 = Math.toRadians (stanPar2);

        e_e = Math.sqrt (1 - (radb * radb) / (rada * rada));

        double m1 = eq1415 (phi1);
        double m2 = eq1415 (phi2);

        double t0 = eq159a (e_phi0);
        double t1 = eq159a (phi1);
        double t2 = eq159a (phi2);

        e_n = ((Math.log (m1) - Math.log (m2)) / (Math.log (t1) - Math.log (t2)));

        double F = m1 / (e_n * Math.pow (t1, e_n));
        e_F_rada = F * rada;

        e_rho0 = e_F_rada * Math.pow (t0, e_n);

        double[][] mat = new double[][] {
                new double[] { tfw_a, tfw_b, 0, 1, 0 },
                new double[] { tfw_c, tfw_d, 0, 0, 1 },
                new double[] { tfw_e, tfw_f, 1, 0, 0 }
        };
        RowReduce (mat);
        wft_a = mat[0][3];
        wft_b = mat[0][4];
        wft_c = mat[1][3];
        wft_d = mat[1][4];
        wft_e = mat[2][3];
        wft_f = mat[2][4];

        northmostlat = Math.max (chartXY2Lat (         0,           0), chartXY2Lat (chartWidth,           0));
        southmostlat = Math.min (chartXY2Lat (         0, chartHeight), chartXY2Lat (chartWidth, chartHeight));
        eastmostlon  = Math.max (chartXY2Lon (chartWidth,           0), chartXY2Lon (chartWidth, chartHeight));
        westmostlon  = Math.min (chartXY2Lon (         0,           0), chartXY2Lon (         0, chartHeight));
    }

    private static void DownloadBitmapFile (File flashfile) throws IOException
    {
        String flashpath = flashfile.getPath ();
        int i = flashpath.indexOf ("charts/");
        String suburl = flashpath.substring (i);
        DownloadFile (flashfile, suburl);
    }

    /**
     * Download a single file from the server.
     * @param flashfile = name of file on flash
     * @param suburl = name of file relative to dldir on the server
     */
    private static void DownloadFile (File flashfile, String suburl) throws IOException
    {
        if (!flashfile.exists ()) {
            Log.i ("AirMap", "downloading " + flashfile.getPath ());
            URL url = new URL (Topography.dldir + "/" + suburl);
            InputStream wis = url.openStream ();
            File flashdirfile = flashfile.getParentFile ();
            if (!flashdirfile.exists () && !flashdirfile.mkdirs ()) {
                throw new IOException ("error creating directory " + flashdirfile.getPath ());
            }
            OutputStream fos = new FileOutputStream (flashfile);
            byte[] buf = new byte[4096];
            for (int rc; (rc = wis.read (buf)) > 0; ) {
                fos.write (buf, 0, rc);
            }
            wis.close ();
            fos.close ();
        }
    }

    /**
     * Convert a given lat/lon to bitmap X,Y.
     * @param xy = where to return bitmap X,Y (as a pixel ratio)
     */
    @Override  // DisplayableChart
    public void latLon2BitmapXY (double lat, double lon, PointD xy)
    {
        xy.x = (getTileX (lat, lon) - leftxtile) / numxtiles;
        xy.y = (getTileY (lat, lon) - topytile)  / numytiles;
    }

    /**
     * Get tile X,Y corresponding to a latitude,longitude.
     * May include a fractional portion indicating a specific pixel within the tile.
     */
    private double getTileX (double lat, double lon)
    {
        double chartX = latLon2ChartX (lat, lon);
        return chartX / TILESIZE;
    }

    private double getTileY (double lat, double lon)
    {
        double chartY = latLon2ChartY (lat, lon);
        return chartY / TILESIZE;
    }

    /**
     * Get chart X pixel within the whole chart.
     */
    private double latLon2ChartX (double lat, double lon)
    {
        double phi = Math.toRadians (lat);
        double lam = Math.toRadians (lon);

        while (lam < e_lam0 - Math.PI) lam += Math.PI * 2;
        while (lam > e_lam0 + Math.PI) lam -= Math.PI * 2;

        double t = eq159a (phi);
        double rho = (e_F_rada * Math.pow (t, e_n));
        double theta = e_n * (lam - e_lam0);
        double easting = rho * Math.sin (theta);
        double northing = e_rho0 - rho * Math.cos (theta);

        return easting * wft_a + northing * wft_c + wft_e;
    }

    /**
     * Get chart Y pixel within the whole chart.
     */
    private double latLon2ChartY (double lat, double lon)
    {
        double phi = Math.toRadians (lat);
        double lam = Math.toRadians (lon);

        while (lam < e_lam0 - Math.PI) lam += Math.PI * 2;
        while (lam > e_lam0 + Math.PI) lam -= Math.PI * 2;

        double t = eq159a (phi);
        double rho = (e_F_rada * Math.pow (t, e_n));
        double theta = e_n * (lam - e_lam0);
        double easting = rho * Math.sin (theta);
        double northing = e_rho0 - rho * Math.cos (theta);

        return easting * wft_b + northing * wft_d + wft_f;
    }

    /**
     * Get latitude of a pixel within the whole chart.
     */
    private double chartXY2Lat (double x, double y)
    {
        // opposite steps of LatLon2ChartPixelExact()

        double easting  = tfw_a * x + tfw_c * y + tfw_e;
        double northing = tfw_b * x + tfw_d * y + tfw_f;

        // easting = rho * sin (theta)
        // easting / rho = sin (theta)

        // northing = e_rho0 - rho * cos (theta)
        // rho * cos (theta) = e_rho0 - northing
        // cos (theta) = (e_rho0 - northing) / rho

        double theta = Math.atan (easting / (e_rho0 - northing));

        // theta = e_n * (lam - e_lam0)
        // theta / e_n = lam - e_lam0
        // theta / e_n + e_lam0 = lam

        double lam = theta / e_n + e_lam0;

        // v108: equation 14-4
        double costheta = Math.cos (e_n * (lam - e_lam0));

        // must calculate phi (latitude) with successive approximation
        // usually takes 3 or 4 iterations to resolve latitude within one pixel

        double phi = e_phi0;
        double metresneedtogonorth;
        do {
            // v108: equation 15-9a
            double t = eq159a (phi);

            // v108: equation 15-7
            double rho = e_F_rada * Math.pow (t, e_n);

            // v107: equation 14-2 -> how far north of centerLat
            double n = e_rho0 - rho * costheta;

            // update based on how far off our guess is
            // - we are trying to get phi that gives us 'northing'
            // - but we have phi that gives us 'n'
            metresneedtogonorth = northing - n;
            phi += metresneedtogonorth / (Lib.MPerNM * Lib.NMPerDeg * 180.0 / Math.PI);
        } while (Math.abs (metresneedtogonorth) > pixelsize);

        return Math.toDegrees (phi);
    }

    /**
     * Get longitude of a pixel within the whole chart.
     */
    private double chartXY2Lon (double x, double y)
    {
        // opposite steps of LatLon2ChartPixelExact()

        double easting  = tfw_a * x + tfw_c * y + tfw_e;
        double northing = tfw_b * x + tfw_d * y + tfw_f;

        // easting = rho * sin (theta)
        // easting / rho = sin (theta)

        // northing = e_rho0 - rho * cos (theta)
        // rho * cos (theta) = e_rho0 - northing
        // cos (theta) = (e_rho0 - northing) / rho

        double theta = Math.atan (easting / (e_rho0 - northing));

        // theta = e_n * (lam - e_lam0)
        // theta / e_n = lam - e_lam0
        // theta / e_n + e_lam0 = lam

        double lam = theta / e_n + e_lam0;

        return Math.toDegrees (lam);
    }

    private double eq1415 (double phi)
    {
        double w = e_e * Math.sin (phi);
        return Math.cos (phi) / Math.sqrt (1 - w * w);
    }

    private double eq159a (double phi)
    {
        double sinphi = Math.sin (phi);
        double u = (1 - sinphi) / (1 + sinphi);
        double v = (1 + e_e * sinphi) / (1 - e_e * sinphi);
        return Math.sqrt (u * Math.pow (v, e_e));
    }

    /**
     * Split a comma-separated value string into its various substrings.
     * @param line = comma-separated line
     * @return array of the various substrings
     */
    private static String[] QuotedCSVSplit (String line)
    {
        int len = line.length ();
        ArrayList<String> cols = new ArrayList<> (len + 1);
        boolean quoted = false;
        boolean escapd = false;
        StringBuilder sb = new StringBuilder (len);
        for (int i = 0;; i ++) {
            char c = 0;
            if (i < len) c = line.charAt (i);
            if (!escapd && (c == '"')) {
                quoted = !quoted;
                continue;
            }
            if (!escapd && (c == '\\')) {
                escapd = true;
                continue;
            }
            if ((!escapd && !quoted && (c == ',')) || (c == 0)) {
                cols.add (sb.toString ());
                if (c == 0) break;
                sb.delete (0, sb.length ());
                continue;
            }
            if (escapd && (c == 'n')) c = '\n';
            if (escapd && (c == 'z')) c = 0;
            sb.append (c);
            escapd = false;
        }
        String[] array = new String[cols.size()];
        return cols.toArray (array);
    }

    /**
     * Row reduce the given matrix.
     */
    public static void RowReduce (double[][] T)
    {
        double pivot;
        int trows = T.length;
        int tcols = T[0].length;

        for (int row = 0; row < trows; row ++) {
            double[] T_row_ = T[row];

            /*
             * Make this row's major diagonal colum one by
             * swapping it with a row below that has the
             * largest value in this row's major diagonal
             * column, then dividing the row by that number.
             */
            pivot = T_row_[row];
            int bestRow = row;
            for (int swapRow = row; ++ swapRow < trows;) {
                double swapPivot = T[swapRow][row];
                if (Math.abs (pivot) < Math.abs (swapPivot)) {
                    pivot   = swapPivot;
                    bestRow = swapRow;
                }
            }
            if (pivot == 0.0) throw new ArithmeticException ("not invertable");
            if (bestRow != row) {
                double[] tmp = T_row_;
                T[row] = T_row_ = T[bestRow];
                T[bestRow] = tmp;
            }
            if (pivot != 1.0) {
                for (int col = row; col < tcols; col ++) {
                    T_row_[col] /= pivot;
                }
            }

            /*
             * Subtract this row from all below it such that we zero out
             * this row's major diagonal column in all rows below.
             */
            for (int rr = row; ++ rr < trows;) {
                double[] T_rr_ = T[rr];
                pivot = T_rr_[row];
                if (pivot != 0.0) {
                    for (int cc = row; cc < tcols; cc ++) {
                        T_rr_[cc] -= pivot * T_row_[cc];
                    }
                }
            }
        }

        for (int row = trows; -- row >= 0;) {
            double[] T_row_ = T[row];
            for (int rr = row; -- rr >= 0;) {
                double[] T_rr_ = T[rr];
                pivot = T_rr_[row];
                if (pivot != 0.0) {
                    for (int cc = row; cc < tcols; cc ++) {
                        T_rr_[cc] -= pivot * T_row_[cc];
                    }
                }
            }
        }
    }
}
