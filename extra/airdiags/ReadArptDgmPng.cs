//+++2013-01-10
//    Copyright (C) 2013, Mike Rieker, Beverly, MA USA
//    Avare, open source moving map aviation GPS (support@apps4av.net)
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


/**
 *  Read an airport diagram .PNG file and find the lat/lon marks and correlate them to pixel number.
 *
 *  yum install libgdiplus
 *  yum install libgdiplus-devel
 *  gmcs -debug -out:ReadArptDgmPng.exe -reference:System.Drawing.dll ReadArptDgmPng.cs
 *
 *  Download airport diagram PDF file from http://www.faa.gov/airports/runway_safety/diagrams/
 *  Convert to png with:
 *    gs -q -dQuiet -dSAFER -dBATCH -dNOPAUSE -dNOPROMT -dMaxBitmap=500000000 -dAlignToPixels=0 -dGridFitTT=2 \
 *        -sDEVICE=pngalpha -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -r300x300 -dFirstPage=1 -dLastPage=1 \
 *        -sOutputFile=<pngname> <pdfname>
 *  mono --debug ReadArptDgmPng.exe <pngname> ...
 *
 */

using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Text;

/**
 * @brief A lat/lon line consisting of several segments in the original chart.
 *        One of these per numeric lat/lon string, eg, "42^30.5'N" and "70^59.5'W"
 *        each have one of these.
 */
public class LongLine {
    public int begx;    // one end of the lat/lon line
    public int begy;
    public int endx;    // other end of the lat/lon line
    public int endy;
    public int count;   // number of segments that make up the line

    /**
     * @brief See if the given x,y is inline with the existing line.
     */
    public bool Matches (int x, int y)
    {
        int oldDx = endx - begx;
        int oldDy = endy - begy;
        double oldlen = Math.Sqrt (oldDx * oldDx + oldDy * oldDy);
        double olddx  = oldDx / oldlen;
        double olddy  = oldDy / oldlen;

        int newDx = x - begx;
        int newDy = y - begy;
        double newlen = Math.Sqrt (newDx * newDx + newDy * newDy);
        double newdx  = newDx / newlen;
        double newdy  = newDy / newlen;

        double dot = Math.Abs (olddx * newdx + olddy * newdy);
        return dot >= Math.Cos (3.0 / 180.0 * Math.PI);
    }

    /**
     * @brief Insert the given x,y into this line's definition.
     */
    public void Insert (int x, int y)
    {
        count ++;

        /*
         * Calc 'f' to be ratio of the new point compared to delta.
         */
        int dx = endx - begx;
        int dy = endy - begy;
        int absdx = (dx > 0) ? dx : -dx;
        int absdy = (dy > 0) ? dy : -dy;
        double f;
        if (absdy > absdx) {
            f = (double)(y - begy) / (double)dy;
        } else {
            f = (double)(x - begx) / (double)dx;
        }

        /*
         * If 'f' is negative, it comes before the old point and so
         * we just replace the old point with the new point.
         */
        if (f < 0.0) {
            begx = x;
            begy = y;
        }

        /*
         * If 'f' is gt 1, it means new point is beyond the delta and
         * so set up new delta based on new point.
         */
        else if (f > 1.0) {
            endx = x;
            endy = y;
        }
    }

    /**
     * @brief See if this line is valid, ie, there are lots of segments.
     *        We get some fake lines where two unassociated segments are
     *        co-incidentally inline.
     */
    public bool IsValid ()
    {
        return true;
    }
}

/**
 * @brief Contains a collection of small line segments
 *        that have the same coarse slope.
 */
public class Slope {
    public int dx;
    public int dy;
    public LinkedList<XY> xys = new LinkedList<XY> ();

    private LinkedList<LongLine> longLines = null;

    /**
     * @brief Get list of long lines of this same slope.
     *        xys contains a list of short line segments of all the same slope.
     *        Match them up into groups based on which line they belong to.
     */
    public LinkedList<LongLine> GetLongLines ()
    {
        if (longLines == null) {
            longLines = new LinkedList<LongLine> ();
            foreach (XY xy in xys) {

                /*
                 * See if the xy segment belongs to an already known line.
                 */
                LongLine line = null;
                foreach (LongLine ll in longLines) {
                    if (ll.Matches (xy.x, xy.y)) {
                        line = ll;
                        break;
                    }
                }

                /*
                 * If not, create a new line.
                 */
                if (line == null) {
                    line = new LongLine ();
                    line.begx = xy.x;
                    line.begy = xy.y;
                    line.endx = xy.x + dx;
                    line.endy = xy.y + dy;
                    longLines.AddLast (line);
                }

                /*
                 * Now insert segment into line.
                 */
                line.Insert (xy.x, xy.y);
            }
        }

        /*
         * Return list of lines.
         * There are usually 2 or 3.
         */
        return longLines;
    }

    /**
     * @brief Determine average slope.
     */
    public void GetFineSlope (out int avgdx, out int avgdy)
    {
        avgdx = 0;
        avgdy = 0;
        foreach (LongLine line in GetLongLines ()) {
            avgdx += line.endx - line.begx;
            avgdy += line.endy - line.begy;
        }
    }
}

public struct XY {
    public int x;
    public int y;

    public XY (int xx, int yy)
    {
        x = xx;
        y = yy;
    }
}

public class ReadArptDgmPng {
    public const int BORXLEFT =   78;               // border dimensions
    public const int BORXRITE = 1535;
    public const int BORYTOP  =  190;
    public const int BORYBOT  = 2285;
    public const int BLOCK    =    4;               // characters and line segments never have a BLOCK x BLOCK blob of black pixels
                                                    // see WRI - has thick lat/lon lines
    public const int D09W     =   13;               // digit recognition resolution width
    public const int D09H     =   17;               // digit recognition resolution height
    public const int D09R     =    3;               // pixel size to write to dump file (ReadArptDgmPng_learnt.png)
    public const int MAXGAP   =   15;               // spacing between '1' '1' (as on KLAX diagram)
    public const int MAXWH    =   50;               // maximum width and height of a character box
    public const int TICKLEN  =    7;               // a tick mark on a lat/lon line must be at least this many pixels long
    public const int YOVER    =    9;               // tops of characters must match this amount
                                                    // to be considered part of the same string
                                                    // see KFLO 79^44'W
    public const int    VFYPIXERR = 70;             // some charts (eg ASE,BLI,GTB) are non-linear
    public const double VFYDEGERR = 0.1/60;
    public const double VFYPIXRAT = 0.95;           // pixels should be within 95% square, else add to notsquare list

    public static bool stages;                      // -stages
    public static bool verbose;                     // -verbose
    public static string csvoutfile;                // -csvoutfile
    public static string csvoutid;                  // -csvoutid
    public static string markedpng;                 // -markedpng

    public static Bitmap bmp;                       // current operating bitmap
    public static Bitmap portBitmap;                // bitmap of original image rotated by theta so lat/lon lines and strings are
                                                    // vertical and/or horizontal
    public static bool landscape;                   // false=portrait; true=landscape
    public static Dictionary<string,double> notsquare = new Dictionary<string,double> ();
                                                    // pixels known to not be square
    public static byte[,] blacks;                   // currently operating array of black pixels [y,x]
    public static Dictionary<string,string> oneLiners = new Dictionary<string,string> ();
    public static Dictionary<string,string> badStrings;
    public static Dictionary<XY,Slope> slopes = new Dictionary<XY,Slope> ();
    public static int latLonSlopeX;                 // slope of lat/lon lines in original image
    public static int latLonSlopeY;
    public static int origHeight;                   // height of original image
    public static int origWidth;                    // width of original image
    public static int portHeight;                   // height of portBitmap
    public static int portWidth;                    // width of portBitmap
    public static int height;                       // height of blacks[h,w]
    public static int width;                        // width of blacks[h,w]
    public static int slopesz;
    public static int[] longlenhorzs;               // indexed by rotated y, counts number of longlen horizontal lines found
    public static int[] longlenverts;               // indexed by rotated x, counts number of longlen vertical lines found
    public static LinkedList<Cluster> clusters;     // all strings found in rotated image
    public static LinkedList<Deco> allDecos;        // all decoded characters found in rotated image
    public static LinkedList<Given> givens = new LinkedList<Given> ();
                                                    // list of strings given on command line
    public static LinkedList<Rectangle> portBoxList = new LinkedList<Rectangle> ();
                                                    // list of character boxes in rotated image
    public static Rectangle[] boxes;                // list of character boxes in rotated & flipped image
    public static Slope bestSlope;                  // the most frequently occuring lat/lon slope in original image
    public static Slope recipSlope;                 // the reciprocal slope to bestSlope
    public static SortedDictionary<char,int> counts = new SortedDictionary<char,int> ();
    public static SortedDictionary<char,int[,]> learnt = new SortedDictionary<char,int[,]> ();
    public static string pngname;                   // original .png filename

    public static void Main (string[] args)
    {
        for (int i = 0; i < args.Length; i ++) {
            string arg = args[i];

            if (arg == "-csvoutfile") {
                if (++ i >= args.Length) goto usage;
                csvoutfile = args[i];
                continue;
            }
            if (arg == "-csvoutid") {
                if (++ i >= args.Length) goto usage;
                csvoutid = args[i];
                continue;
            }
            if (arg == "-givens") {
                // x,y=string
                while (++ i < args.Length) {
                    string s = args[i];
                    int j = s.IndexOf (',');
                    int k = s.IndexOf ('=');
                    if ((j <= 0) || (k <= j)) {
                        -- i;
                        break;
                    }
                    int x = int.Parse (s.Substring (0, j ++));
                    int y = int.Parse (s.Substring (j, k - j));
                    givens.AddLast (new Given (x, y, s.Substring (++ k)));
                }
                continue;
            }
            if (arg == "-markedpng") {
                if (++ i >= args.Length) goto usage;
                markedpng = args[i];
                continue;
            }
            if (arg == "-stages") {
                stages = true;
                continue;
            }
            if (arg == "-verbose") {
                verbose = true;
                continue;
            }
            if (arg[0] == '-') goto usage;
            if (pngname != null) goto usage;
            pngname = arg;
        }
        if (pngname == null) goto usage;

        /*
         * Some charts simply contain broken strings.
         * List any here.
         */
        badStrings = new Dictionary<string,string> ();
        badStrings["29^42'5'N"]  = "29^42.5'N";     // KBAZ
        badStrings["29^02.5'W"]  = "29^02.5'N";     // KEVB
        badStrings["29^03.0'W"]  = "29^03.0'N";     // KEVB
        badStrings["29^03.5'W"]  = "29^03.5'N";     // KEVB
        badStrings["29^04.0'W"]  = "29^04.0'N";     // KEVB
        badStrings["73^24.5'"]   = "73^24.5'W";     // KFRG
        badStrings["73^25.0'"]   = "73^25.0'W";     // KFRG
        badStrings["44^05'N"]    = "--";            // KGTB
        badStrings["86^41W"]     = "86^41'W";       // KHRT
        badStrings["38^58.0N"]   = "38^58.0'N";     // KLXT
        badStrings["31^41.5'N"]  = "32^41.5'N";     // KMCN
        badStrings["85^42'30\""] = "85^42'30\"W";   // KOZR
        badStrings["88^46.0'N"]  = "88^46.0'W";     // KPAH
        badStrings["88^46.5'N"]  = "88^46.5'W";     // KPAH
        badStrings["88^47.0'N"]  = "88^47.0'W";     // KPAH
        badStrings["73^27.0'N"]  = "73^27.0'W";     // KPBG
        badStrings["73^28.0'N"]  = "73^28.0'W";     // KPBG
        badStrings["73^29.0'N"]  = "73^29.0'W";     // KPBG

        // technically not a broken chart
        // but characters on ends obscured by chart graphics
        badStrings["31^32.0'"]   = "31^32.0'N";     // KABY
        badStrings["33^39'"]     = "33^39'N";       // KATL
        badStrings["0^37.0'N"]   = "30^37.0'N";     // KFHB
        badStrings["1^40.0'N"]   = "41^40.0'N";     // KFMH
        badStrings["79^^00'W"]   = "79^00'W";       // KPOB
        badStrings["8^54.5'N"]   = "38^54.5'N";     // KTVL
        badStrings["83^36'"]     = "83^36'W";       // KWRB

        /*
         * Non-square pixels.
         * Index is the lowestlat,lowestlon for the airport.
         * Value is our computed non-square ratio.
         */
        notsquare["31^08'N,97^43'W"]   = 1.094;     // KHLR
        notsquare["38^30'N,77^18.5'W"] = 0.664;     // KNYG

        /*
         * Some charts just have one latitude or longitude line.
         * So assume the pixels are square and use the other for scaling.
         * Index is the lowestlat,lowestlon given for the airport.
         * Value is the missing lat or lon to add.
         */
        oneLiners["34^40'N,86^41'W"] = "86^41.5'W"; // KHUA
        oneLiners["40^02'N,74^21'W"] = "40^02.5'N"; // KNEL

        /*
         * Take original image and figure out where the lat/lon lines are
         * and what their slope is.
         */
        FindLatLonLines ();

        /*
         * Rotate the original image such that the lat/lon lines are
         * vertical & horizontal, and presumable so the lat/lon strings
         * will also be vertical & horizontal.
         */
        CreateRotatedImage ();

        /*
         * Now that lat/lon strings are supposedly all horizontal and vertical,
         * scan the image to locate and decode them all.
         */
        FindLatLonStrings ();

        if (givens.Count > 0) {

            /*
             * Accumulate learned lat/lon string characters.
             */
            BuildLearning ();
        } else {

            /*
             * Build and print latlon <-> pixel transformation matrix.
             */
            BuildXformMatrix ();
        }
        return;

    usage:
        Console.WriteLine ("usage: mono ReadArptDgmPng.exe <inputpngfile>");
        Console.WriteLine ("           -csvoutfile <filename>         - append data to given file");
        Console.WriteLine ("           -csvoutid <airportid>          - use this airport id in csvoutfile");
        Console.WriteLine ("           -givens \"x,y=string\" ...       - learning mode");
        Console.WriteLine ("           -markedpng <outputpngfilename> - write out marked-up png file");
        Console.WriteLine ("           -stages                        - create intermediate png files (debugging)");
        Console.WriteLine ("           -verbose                       - output intermediate messages (debugging)");
    }

    /**
     * @brief Scan the original image to determine position and slope of all lat/lon lines.
     * @returns bestSlope = most frequent slope encountered
     *         recipSlope = the corresponding reciprocal slope
     *     latLonSlopeX,Y = resultant slope
     */
    public static void FindLatLonLines ()
    {
        /*
         * Open original image file.
         */
        bmp        = new Bitmap (pngname);
        origWidth  = bmp.Width;
        origHeight = bmp.Height;

        /*
         * Get strict black/white pixel array for easier decoding.
         */
        Monochromaticise (bmp);
        if (stages) bmp.Save ("stage1.png", System.Drawing.Imaging.ImageFormat.Png);

        /*
         * Clear off border lines.  Sometimes they get confused with lat/lons.
         */
        ClearBorderLines ();
        if (stages) bmp.Save ("stage1m.png", System.Drawing.Imaging.ImageFormat.Png);

        /*
         * Filter out every BLOCK x BLOCK or larger size of black pixels.
         * This gets rid of things like runways and buildings.
         */
        ClearLargeBlobs ();
        if (stages) bmp.Save ("stage2.png", System.Drawing.Imaging.ImageFormat.Png);

        /*
         * Filter out every little blob that is smaller than MAXWH.
         * This gets rid of most characters.
         */
        ClearSmallBlobs ();
        if (stages) bmp.Save ("stage3.png", System.Drawing.Imaging.ImageFormat.Png);

        /*
         * Find sloped line segments with tick marks.
         * Try various sizes as the tick mark must fit completely inside the box.
         */
        for (slopesz = 24; slopesz <= 38; slopesz += 2) {
            FindSegments ();
        }

        if (stages || verbose) {
            foreach (Slope sl in slopes.Values) {
                if (verbose) Console.WriteLine ("slope: " + sl.dy + "/" + sl.dx + ": " + sl.xys.Count);
                foreach (LongLine line in sl.GetLongLines ()) {
                    if (!line.IsValid ()) continue;
                    int x1 = line.begx;
                    int y1 = line.begy;
                    int x2 = line.endx;
                    int y2 = line.endy;
                    int dx = x2 - x1;
                    int dy = y2 - y1;
                    if (verbose) {
                        Console.WriteLine ("    line: " + x1 + "," + y1 + " .. " + x2 + "," + y2 + 
                                           " = " + ((double)dy / (double)dx));
                    }
                    if (stages) {
                        for (int w = 4; w <= 8; w ++) {
                            double d = Math.Sqrt (dx * dx + dy * dy) / 2;
                            dx = (int)(dx / d + 0.5);
                            dy = (int)(dy / d + 0.5);
                            DrawLine (x1 + dy, y1 - dx, x1 - dy, y1 + dx, Color.Green);
                            DrawLine (x2 + dy, y2 - dx, x2 - dy, y2 + dx, Color.Green);
                            DrawLine (x1 + dy, y1 - dx, x2 + dy, y2 - dx, Color.Green);
                            DrawLine (x1 - dy, y1 + dx, x2 - dy, y2 + dx, Color.Green);
                        }
                    }
                }
            }
            if (stages) bmp.Save ("stage4.png", System.Drawing.Imaging.ImageFormat.Png);
        }

        /*
         * Find slope that occurred the most times.
         * Assume that it is the latitude or longitude.
         */
        foreach (Slope sl in slopes.Values) {
            if ((bestSlope == null) || (bestSlope.xys.Count < sl.xys.Count)) {
                bestSlope = sl;
            }
        }
        int bestdx = bestSlope.dx;
        int bestdy = bestSlope.dy;

        /*
         * Now take the average of the slopes of all those lines,
         * giving weight to the longer lines.
         */
        bestSlope.GetFineSlope (out latLonSlopeX, out latLonSlopeY);
        double majorangle = Math.Atan2 (latLonSlopeY, latLonSlopeX) * 180.0 / Math.PI;
        if (verbose) {
            Console.WriteLine ("major slope " + bestdy + "/" + bestdx + " = " + 
                               latLonSlopeY + "/" + latLonSlopeX + " = " + majorangle);
        }
        double llsl = Math.Sqrt (latLonSlopeX * latLonSlopeX + latLonSlopeY * latLonSlopeY);
        double llsx = latLonSlopeX / llsl;
        double llsy = latLonSlopeY / llsl;

        /*
         * Find reciprocal slope and assume that it is the other of latitude or longitude.
         */
        int recipdx, recipdy;
        foreach (Slope sl in slopes.Values) {
            sl.GetFineSlope (out recipdx, out recipdy);
            double recipl = Math.Sqrt (recipdx * recipdx + recipdy * recipdy);
            double recipx = recipdx / recipl;
            double recipy = recipdy / recipl;
            double dot = Math.Abs (recipx * llsx + recipy * llsy);
            if (dot > 2.0 / slopesz) continue;
            if ((recipSlope == null) || (recipSlope.xys.Count < sl.xys.Count)) {
                recipSlope = sl;
            }
        }
        if (recipSlope == null) {
            throw new Exception ("no reciprocal slope found");
        }
        recipSlope.GetFineSlope (out recipdx, out recipdy);
        double minorangle = Math.Atan2 (recipdy, recipdx) * 180.0 / Math.PI;
        if (verbose) {
            Console.WriteLine ("minor slope " + recipSlope.dy + "/" + recipSlope.dx + " = " +
                               recipdy + "/" + recipdx + " = " + minorangle);
            Console.WriteLine ("major - minor angle = " + (majorangle - minorangle));
        }

        /*
         * Rotate by 90,180,270 to make it positive, ie, in range 0..89.
         * Then maybe rotate by -90 to put in range -44..+45.
         */
        while ((latLonSlopeX | latLonSlopeY) < 0) {
            int tmp = latLonSlopeY;
            latLonSlopeY = -latLonSlopeX;
            latLonSlopeX = tmp;
        }
        if (latLonSlopeY > latLonSlopeX) {
            int tmp = latLonSlopeY;
            latLonSlopeY = -latLonSlopeX;
            latLonSlopeX = tmp;
        }
        if (verbose) {
            Console.WriteLine ("lat/lon slope = " + latLonSlopeY + "/" + latLonSlopeX + " = " + 
                               ((double)latLonSlopeY / (double)latLonSlopeX));
            Console.WriteLine ("        theta = " + (Math.Atan2 (latLonSlopeY, latLonSlopeX) * 180.0 / Math.PI));
        }
    }

    /**
     * @brief Locate blobs of black pixels that are at least BLOCK x BLOCK
     *        size and wipe them out.  This gets rid of things like 
     *        runways and buildings.
     *
     *   Input:
     *     blacks,bmp,width,height = monochromatic image
     *
     *   Output:
     *     blacks,bmp = updated
     */
    private static void ClearLargeBlobs ()
    {
        for (int y = 0; y < height - BLOCK; y ++) {
            for (int x = 0; x < width - BLOCK; x ++) {
                int nblacks = 0;
                for (int yy = 0; yy < BLOCK; yy ++) {
                    for (int xx = 0; xx < BLOCK; xx ++) {
                        nblacks += blacks[y+yy,x+xx];
                    }
                }
                if (nblacks < BLOCK*BLOCK) continue;
                for (int yy = 0; yy < BLOCK; yy ++) {
                    for (int xx = 0; xx < BLOCK; xx ++) {
                        bmp.SetPixel (x+xx, y+yy, Color.White);
                    }
                }
            }
        }
        for (int y = 0; y < height - BLOCK; y ++) {
            for (int x = 0; x < width - BLOCK; x ++) {
                Color c = bmp.GetPixel (x, y);
                blacks[y,x] = ColorIsBlack (c);
            }
        }
    }

    /**
     * @brief Locate boxes around things that might be characters and clear them out.
     *
     *   Input:
     *     blacks,bmp,width,height = monochromatic image
     *
     *   Output:
     *     blacks,bmp = updated
     */
    private static void ClearSmallBlobs ()
    {
        for (int y = 0; y < height; y ++) {
            for (int x = 0; x < width; x ++) {
                if (blacks[y,x] == 0) continue;
                int ytop = y;
                int ybot = y;
                int xlef = x;
                int xrit = x;
                if (ExpandPerimeter (ref xlef, ref xrit, ref ytop, ref ybot)) {
                    ClearRectangle (xlef, ytop, xrit, ybot);
                }
            }
        }
    }

    /**
     * @brief Clear border lines from original image
     *        so they can't be confused as lat/lon lines.
     */
    private static void ClearBorderLines ()
    {
        int x, y;

        for (x = 0; !IsVertBorderLine (x);) {
            if (++ x > BORXLEFT) return;
        }
        for (; IsVertBorderLine (x);) {
            if (++ x > BORXLEFT) return;
        }
        ClearRectangle (0, 0, x - 1, height - 1);

        for (x = width; !IsVertBorderLine (-- x);) {
            if (x <= BORXRITE) return;
        }
        for (; IsVertBorderLine (x);) {
            if (-- x < BORXRITE) return;
        }
        ClearRectangle (x + 1, 0, width - 1, height - 1);

        for (y = 0; !IsHorizBorderLine (y);) {
            if (++ y > BORYTOP) return;
        }
        for (; IsHorizBorderLine (y);) {
            if (++ y > BORYTOP) return;
        }
        ClearRectangle (0, 0, width - 1, y - 1);

        for (y = height; !IsHorizBorderLine (-- y);) {
            if (y <= BORYBOT) return;
        }
        for (; IsHorizBorderLine (y);) {
            if (-- y < BORYBOT) return;
        }
        ClearRectangle (0, y + 1, width - 1, height - 1);
    }

    private static bool IsVertBorderLine (int x)
    {
        for (int y = BORYTOP; y <= BORYBOT; y ++) {
            if (blacks[y,x] == 0) return false;
        }
        return true;
    }
    private static bool IsHorizBorderLine (int y)
    {
        for (int x = BORXLEFT; x <= BORXRITE; x ++) {
            if (blacks[y,x] == 0) return false;
        }
        return true;
    }

    /**
     * @brief Set a rectangle of the image to all white
     */
    private static void ClearRectangle (int xlef, int ytop, int xrit, int ybot)
    {
        for (int yy = ytop; yy <= ybot; yy ++) {
            for (int xx = xlef; xx <= xrit; xx ++) {
                bmp.SetPixel (xx, yy, Color.White);
                blacks[yy,xx] = 0;
            }
        }
    }

    /**
     * @brief Scan through image looking for line segments of length slopesz (on either X or Y axis).
     *        Measure their slope and enter them into 'slopes' list.
     *        Remove each used segment from blacks[,] array.
     *        Overwrite the segments with red in the bitmap image for debugging.
     */
    public static int FindSegments ()
    {
        byte lastBlack, thisBlack;
        int ni, nn, xx, yy;
        int nsegs = 0;
        int[] transits = new int[4];

        for (int cy = slopesz / 2; cy < height - slopesz / 2 - 1; cy ++) {
            for (int cx = slopesz / 2; cx < width - slopesz / 2 - 1; cx ++) {

                /*
                 * Make sure we have a black pixel in center of box.
                 */
                if (blacks[cy,cx] == 0) goto next;

                /*
                 * Find first white-to-black transition around edge of box.
                 */
                lastBlack = TransitBlack (cx, cy, 0);
                for (ni = 0; ++ ni < 4 * slopesz;) {
                    thisBlack = TransitBlack (cx, cy, ni);
                    if ((lastBlack == 0) && (thisBlack != 0)) break;
                    lastBlack = thisBlack;
                }
                if (ni >= 4 * slopesz) goto next;

                /*
                 * Find remaining transitions going around the edge.
                 * There should be exactly four, starting with a white-to-black.
                 */
                int ntrans = 0;
                for (nn = 0; nn < 4 * slopesz; nn ++) {
                    int nt = (ni + nn) % (4 * slopesz);
                    thisBlack = TransitBlack (cx, cy, nt);
                    if (thisBlack != lastBlack) {
                        if (ntrans == 4) goto next;
                        transits[ntrans++] = nt;
                    }
                    lastBlack = thisBlack;
                }
                if (ntrans != 4) goto next;

                /*
                 * Make the black-to-white transitions to point to the black pixel.
                 * Leave the white-to-black transitions pointing at the black pixel.
                 *
                 * This lets, eg, a single-pixel width horizontal line have a slope
                 * of exactly 0.0.
                 */
                transits[1] = (transits[1] + 4 * slopesz - 1) % (4 * slopesz);
                transits[3] = (transits[3] + 4 * slopesz - 1) % (4 * slopesz);

                /*
                 * Make sure the transitions are on opposite edges.
                 * This will tell us that the line is straight and going through the center.
                 */
                int wbdiff = 4 * slopesz + transits[0] - transits[2];
                int bwdiff = 4 * slopesz + transits[1] - transits[3];
                wbdiff %= 4 * slopesz;
                bwdiff %= 4 * slopesz;
                if (wbdiff < 2 * slopesz - 1) goto next;
                if (wbdiff > 2 * slopesz + 1) goto next;
                if (bwdiff < 2 * slopesz - 1) goto next;
                if (bwdiff > 2 * slopesz + 1) goto next;

                /*
                 * Get XY of all transitions.
                 */
                int bw1x, bw1y, bw2x, bw2y;  // black-to-white transitions
                int wb1x, wb1y, wb2x, wb2y;  // white-to-black transitions
                TransitToXY (cx, cy, transits[0], out wb1x, out wb1y);
                TransitToXY (cx, cy, transits[1], out bw1x, out bw1y);
                TransitToXY (cx, cy, transits[2], out wb2x, out wb2y);
                TransitToXY (cx, cy, transits[3], out bw2x, out bw2y);

                /*
                 * Calculate slope of edges.
                 *
                 *       wb1     bw1
                 *    -  [*]  *  [-]  -   -
                 *    -   -   *   *   -   -
                 *    -   -   *   *   -   -
                 *    -   -   -   *   *   -
                 *    -   -   -   *   *   -
                 *    -   -   -  [-]  *  [*]  wb2
                 *               bw2
                 */
                int adx = wb2x - bw1x;
                int ady = wb2y - bw1y;
                int bdx = bw2x - wb1x;
                int bdy = bw2y - wb1y;
                if (adx < 0) { adx = -adx; ady = -ady; }
                if (bdx < 0) { bdx = -bdx; bdy = -bdy; }
                int avgdx = (adx + bdx) / 2;
                int avgdy = (ady + bdy) / 2;

                /*
                 * Check for centered tick mark.
                 */
                if (!HasCenteredTickMark (cx, cy, avgdx, avgdy)) goto next;

                bool debug = false;
                if (debug) {
                    Console.WriteLine ("FindSegments*: " + cx + "," + cy + " " + avgdx + "," + avgdy);
                    Console.WriteLine ("FindSegments*:   " + transits[0] + " " + transits[1] + " " + transits[2] + " " + transits[3]);
                    Console.WriteLine ("FindSegments*:   wb1=" + (wb1x + slopesz / 2 - cx) + "," + (wb1y + slopesz / 2 - cy));
                    Console.WriteLine ("FindSegments*:   bw1=" + (bw1x + slopesz / 2 - cx) + "," + (bw1y + slopesz / 2 - cy));
                    Console.WriteLine ("FindSegments*:   wb2=" + (wb2x + slopesz / 2 - cx) + "," + (wb2y + slopesz / 2 - cy));
                    Console.WriteLine ("FindSegments*:   bw2=" + (bw2x + slopesz / 2 - cx) + "," + (bw2y + slopesz / 2 - cy));
                    Console.WriteLine ("FindSegments*:  avgd=" + avgdy + "/" + avgdx + "=" + ((double)avgdy / (double)avgdx));
                    char[] line = "".PadRight (2 * slopesz + 1).ToCharArray ();
                    for (yy = -slopesz/2; yy <= slopesz/2; yy ++) {
                        for (xx = -slopesz/2; xx <= slopesz/2; xx ++) {
                            line[2*xx+slopesz] = (blacks[cy+yy,cx+xx] == 0) ? '-' : '*';
                        }
                        Console.WriteLine ("FindSegments*:    " + new string (line));
                    }
                }

                /*
                 * Add segment to list of known slopes.
                 */
                Slope sl;
                XY delta = new XY (avgdx, avgdy);
                if (!slopes.TryGetValue (delta, out sl)) {
                    sl = new Slope ();
                    sl.dx = delta.x;
                    sl.dy = delta.y;
                    slopes.Add (delta, sl);
                }
                sl.xys.AddLast (new XY (cx, cy));
                nsegs ++;

                /*
                 * Draw red line in image for debugging.
                 */
                DrawLine (cx - avgdx / 2, cy - avgdy / 2, cx + avgdx / 2, cy + avgdy / 2, Color.Red);

                /*
                 * Erase the blacks[,] entries so we don't try to
                 * use these same pixels for another box.
                 */
                for (yy = -slopesz / 2; yy < slopesz / 2; yy ++) {
                    for (xx = -slopesz / 2; xx < slopesz / 2; xx ++) {
                        blacks[cy+yy,cx+xx] = 0;
                    }
                }
            next:;
            }
        }
        return nsegs;
    }

    /**
     * @brief Get a transition pixel
     * @param cx,cy = center of the box
     * @param n = transition index (0..4*slopesz-1)
     * @returns 0: pixel is white; else: pixel is black
     */
    private static byte TransitBlack (int cx, int cy, int n)
    {
        int x, y;
        TransitToXY (cx, cy, n, out x, out y);
        return blacks[y,x];
    }

    /**
     * @brief Convert a transition index to the corresponding x,y
     * @param cx,cy = center of the box
     * @param n = transition index (0..4*slopesz-1)
     * @returns x,y = resultant x,y
     *
     *   a a a a a a b    a = first quarter
     *   d - - - - - b    b = second quarter
     *   d - - - - - b    c = third quarter
     *   d - - O - - b    d = fourth quarter
     *   d - - - - - b
     *   d - - - - - b    O = center (cx,cy)
     *   d c c c c c c
     */
    private static void TransitToXY (int cx, int cy, int n, out int x, out int y)
    {
        int edge = n / slopesz;
        n %= slopesz;
        switch (edge) {
            case 0: y = cy - slopesz / 2;      x = cx - slopesz / 2 + n;  break;
            case 1: y = cy - slopesz / 2 + n;  x = cx + slopesz / 2;      break;
            case 2: y = cy + slopesz / 2;      x = cx + slopesz / 2 - n;  break;
            case 3: y = cy + slopesz / 2 - n;  x = cx - slopesz / 2;      break;
            default: throw new Exception ();
        }
    }

    /**
     * @brief See if a line segment going through a slopesz x slopesz box has a centered tick mark.
     * @param cx,cy = the x,y of the center of the box
     * @param avgdy/avgdx = slop of the line segment (tick slope is -avgdx/avgdy)
     * @returns true iff there is a centered tick mark
     */
    private static bool HasCenteredTickMark (int cx, int cy, int avgdx, int avgdy)
    {
        int absavgdx = (avgdx < 0) ? -avgdx : avgdx;
        int absavgdy = (avgdy < 0) ? -avgdy : avgdy;

        if ((absavgdy | absavgdx) == 0) return false;

        if (absavgdy > absavgdx) {

            /*
             * More vertical than horizontal.
             *
             *      - - - - L -
             *      - - - L - -
             *      - - - L - -
             *      - - L - T -
             *      - - L - - -
             *      - L - - - -
             */
            for (int linedy = 3; linedy <= TICKLEN; linedy ++) {
                int linedx = linedy * avgdx / avgdy;
                byte nb = (byte)(blacks[cy-linedx,cx+linedy] | blacks[cy+linedx,cx-linedy]);
                nb |= (byte)(blacks[cy-linedx-1,cx+linedy] | blacks[cy+linedx-1,cx-linedy]);
                nb |= (byte)(blacks[cy-linedx+1,cx+linedy] | blacks[cy+linedx+1,cx-linedy]);
                if (nb == 0) return false;
            }
        } else {

            /*
             * More horizontal than vertical.
             *
             *      - - - - - -
             *      - - T - - L
             *      - - - L L -
             *      - L L - - -
             *      L - - - - -
             *      - - - - - -
             */
            for (int linedx = 3; linedx <= TICKLEN; linedx ++) {
                int linedy = linedx * avgdy / avgdx;
                byte nb = (byte)(blacks[cy-linedx,cx+linedy] | blacks[cy+linedx,cx-linedy]);
                nb |= (byte)(blacks[cy-linedx-1,cx+linedy] | blacks[cy+linedx-1,cx-linedy]);
                nb |= (byte)(blacks[cy-linedx+1,cx+linedy] | blacks[cy+linedx+1,cx-linedy]);
                if (nb == 0) return false;
            }
        }

        return true;
    }


    /**
     * @brief Create rotated image such that the lat/lon lines are 
     *        vertical and horizontal.  Presumably the corresponding 
     *        text will be vertical and horizontal too.
     */
    public static void CreateRotatedImage ()
    {
        /*
         * Create a black-and-white rotated image from original grayscale image.
         */
        Bitmap grayOrig = new Bitmap (pngname);
        int origWidth  = grayOrig.Width;
        int origHeight = grayOrig.Height;

        int newwh  = (int)(Math.Max (origWidth, origHeight) * Math.Sqrt (2.0));
        portWidth  = newwh;
        portHeight = newwh;
        portBitmap = new Bitmap (newwh, newwh);
        for (int y = 0; y < newwh; y ++) {
            for (int x = 0; x < newwh; x ++) {
                portBitmap.SetPixel (x, y, Color.White);
            }
        }

        DrawRotatedImage (portBitmap, grayOrig, latLonSlopeX, latLonSlopeY);
        Monochromaticise (portBitmap);

        /*
         * Now say where within that image the horizontal and vertical lat/lon lines are.
         */
        longlenhorzs = new int[portHeight];
        longlenverts = new int[portWidth];
        foreach (Slope sl in slopes.Values) {
            ComputeVertHorzLatLons (sl);
        }

        if (stages) portBitmap.Save ("stage5.png", System.Drawing.Imaging.ImageFormat.Png);
    }

    /**
     * @brief Draw image, rotating it about its center.
     * @param dstbmp = destination bitmap
     * @param srcbmp = source bitmap
     * @param dy/dx = arctan of rotation angle
     *                eg, 24/15 means rotate 60deg counter-clockwise
     */
    public static void DrawRotatedImage (Bitmap dstbmp, Bitmap srcbmp, int dx, int dy)
    {
        int srcwid = srcbmp.Width;
        int srchit = srcbmp.Height;
        int dstwid = dstbmp.Width;
        int dsthit = dstbmp.Height;

        double d = Math.Sqrt (dx * dx + dy * dy);
        double costh = dx / d;
        double sinth = dy / d;

        for (int dsty = 0; dsty < dsthit; dsty ++) {
            for (int dstx = 0; dstx < dstwid; dstx ++) {

                // get destination point relative to destination center
                int cendstx = dstx - dstwid / 2;
                int cendsty = dsty - dsthit / 2;

                // calc corresponding source point relative to source center
                //   [ costh -sinth ] * [ cendstx ]  =  [ censrcx ]
                //   [ sinth  costh ]   [ cendsty ]     [ censrcy ]
                int censrcx = (int)(costh * cendstx - sinth * cendsty + 0.5);
                int censrcy = (int)(costh * cendsty + sinth * cendstx + 0.5);

                // calc absolute source point
                int srcx = censrcx + srcwid / 2;
                int srcy = censrcy + srchit / 2;

                // copy pixel from source to destination
                if ((srcx >= 0) && (srcx < srcwid) && (srcy >= 0) && (srcy < srchit)) {
                    Color c = srcbmp.GetPixel (srcx, srcy);
                    dstbmp.SetPixel (dstx, dsty, c);
                }
            }
        }
    }

    /**
     * @brief Given a list of lines within the original image, compute their 
     *        horizontal Y pixels or vertical X pixels within the rotated image.
     *
     *   Input:
     *     origWidth,origHeight = width and height of original image
     *     portBitmap,portWidth,portHeight = rotated image
     *     slope = collection of line segments of equal slope
     *
     *   Output:
     *     longlenverts,longlenhorzs = filled in to indicate where lat/lon lines
     *                                 are in the rotated image
     */
    public static void ComputeVertHorzLatLons (Slope slope)
    {
        // scan through each line of the given slope
        foreach (LongLine line in slope.GetLongLines ()) {
            if (!line.IsValid ()) continue;

            // rotate the line's endpoints from original image coords to rotated image coords
            XY begRot = RotateImagePoint (line.begx, line.begy, latLonSlopeX, latLonSlopeY);
            XY endRot = RotateImagePoint (line.endx, line.endy, latLonSlopeX, latLonSlopeY);

            int xdiff = begRot.x - endRot.x;
            int ydiff = begRot.y - endRot.y;
            if (xdiff < 0) xdiff = -xdiff;
            if (ydiff < 0) ydiff = -ydiff;

            // small X difference means vertical
            if (xdiff <= ydiff / 16) {
                int xavg = (begRot.x + endRot.x) / 2;
                if ((xavg >= 0) && (xavg < portWidth)) {
                    longlenverts[xavg] += ydiff;
                }
            }

            // small Y difference means horizontal
            if (ydiff <= xdiff / 16) {
                int yavg = (begRot.y + endRot.y) / 2;
                if ((yavg >= 0) && (yavg < portHeight)) {
                    longlenhorzs[yavg] += xdiff;
                }
            }
        }
    }

    /**
     * @brief Draw image, rotating it about its center.
     * @param ox,oy = point in original image
     * @param dy/dx = arctan of rotation angle
     *                eg, 24/15 means rotate 60deg counter-clockwise
     * @returns point in rotated image (portBitmap)
     */
    public static XY RotateImagePoint (int ox, int oy, int dx, int dy)
    {
        double d = Math.Sqrt (dx * dx + dy * dy);
        double costh = dx / d;
        double sinth = dy / d;

        // get original point relative to original image center
        ox -= origWidth  / 2;
        oy -= origHeight / 2;

        // calc corresponding rotated point relative to rotated image center
        //   [  costh sinth ] * [ ox ]  =  [ rx ]
        //   [ -sinth costh ]   [ oy ]     [ ry ]
        int rx = (int)(costh * ox + sinth * oy + 0.5);
        int ry = (int)(costh * oy - sinth * ox + 0.5);

        // calc absolute rotated point
        rx += portBitmap.Width  / 2;
        ry += portBitmap.Height / 2;

        return new XY (rx, ry);
    }


    /**
     * @brief Find all lat/lon strings in the rotated image.
     *        Flip the rotated image around from portrait<->landscape to get them all.
     *
     *   Input:
     *     portBitmap,portWidth,portHeight = portrait-orientated rotated image
     */
    public static void FindLatLonStrings ()
    {
        /*
         * Read previously learned digit pixel patterns.
         */
        StreamReader learntreader = null;
        try {
            learntreader = new StreamReader ("ReadArptDgmPng_learnt.csv");
        } catch {
        }
        if (learntreader != null) {
            string line;
            while ((line = learntreader.ReadLine ()) != null) {
                string[] tokens = line.Split (new char[] { ',' });
                int i = 0;
                char key = tokens[i++][0];
                counts[key] = int.Parse (tokens[i++]);
                learnt[key] = new int[D09H,D09W];
                for (int y = 0; y < D09H; y ++) {
                    for (int x = 0; x < D09W; x ++) {
                        learnt[key][y,x] = int.Parse (tokens[i++]);
                    }
                }
            }
            learntreader.Close ();
        }

        /*
         * Find all character boxes in the blacks[,] array.
         */
        if (verbose) Console.WriteLine ("find character boxes");
        clusters  = new LinkedList<Cluster> ();
        landscape = false;
        FindCharacterBoxes ();
        if (stages) portBitmap.Save ("stage6.png", System.Drawing.Imaging.ImageFormat.Png);

        /*
         * Some diagrams have reverse landscape (rotated 90deg counter-clockwise) lat/lon strings.
         * See KNFL.
         */
        if (verbose) Console.WriteLine ("reverse landscape scan");
        blacks = new byte[portWidth,portHeight];
        for (int porty = 0; porty < portHeight; porty ++) {
            for (int portx = 0; portx < portWidth; portx ++) {
                Color c = portBitmap.GetPixel (portx, porty);
                int x = porty;
                int y = portWidth - 1 - portx;
                blacks[y,x] = ColorIsBlack (c);
            }
        }
        int bi = 0;
        boxes  = new Rectangle[portBoxList.Count];
        foreach (Rectangle pr in portBoxList) {
            boxes[bi].X = pr.Y;
            boxes[bi].Y = portWidth - pr.X - pr.Width;
            boxes[bi].Width  = pr.Height;
            boxes[bi].Height = pr.Width;
            bi ++;
        }
        clusters  = new LinkedList<Cluster> ();
        width     = portHeight;
        height    = portWidth;
        landscape = true;
        DecodeLatLonStrings ("revland");
        LinkedList<Cluster> revLandClusters = clusters;

        /*
         * Scan image in landscape (rotated 90deg clockwise) orientation.
         * Collect all recognized lat/lon strings in 'clusters'.
         */
        if (verbose) Console.WriteLine ("normal landscape scan");
        for (int porty = 0; porty < portHeight; porty ++) {
            for (int portx = 0; portx < portWidth; portx ++) {
                Color c = portBitmap.GetPixel (portx, porty);
                int x = portHeight - 1 - porty;
                int y = portx;
                blacks[y,x] = ColorIsBlack (c);
            }
        }
        bi = 0;
        foreach (Rectangle pr in portBoxList) {
            boxes[bi].X = portHeight - pr.Y - pr.Height;
            boxes[bi].Y = pr.X;
            boxes[bi].Width  = pr.Height;
            boxes[bi].Height = pr.Width;
            bi ++;
        }
        clusters  = new LinkedList<Cluster> ();
        width     = portHeight;
        height    = portWidth;
        landscape = true;
        DecodeLatLonStrings ("normland");
        LinkedList<Cluster> normLandClusters = clusters;

        /*
         * Scan image in reverse portrait (downside-up) orientation.
         */
        if (verbose) Console.WriteLine ("reverse portrait scan");
        for (int porty = 0; porty < portHeight; porty ++) {
            for (int portx = 0; portx < portWidth; portx ++) {
                Color c = portBitmap.GetPixel (portx, porty);
                int x = portWidth  - 1 - portx;
                int y = portHeight - 1 - porty;
                blacks[y,x] = ColorIsBlack (c);
            }
        }
        bi = 0;
        foreach (Rectangle pr in portBoxList) {
            boxes[bi].X = portWidth  - pr.X - pr.Width;
            boxes[bi].Y = portHeight - pr.Y - pr.Height;
            boxes[bi].Width  = pr.Height;
            boxes[bi].Height = pr.Width;
            bi ++;
        }
        clusters  = new LinkedList<Cluster> ();
        width     = portHeight;
        height    = portWidth;
        landscape = true;
        DecodeLatLonStrings ("revport");
        LinkedList<Cluster> revPortClusters = clusters;

        /*
         * Scan image in portrait (normal) orientation.
         * Add all recognized lat/lon strings to 'clusters'.
         */
        if (verbose) Console.WriteLine ("normal portrait scan");
        blacks = new byte[portHeight,portWidth];
        for (int porty = 0; porty < portHeight; porty ++) {
            for (int portx = 0; portx < portWidth; portx ++) {
                Color c = portBitmap.GetPixel (portx, porty);
                blacks[porty,portx] = ColorIsBlack (c);
            }
        }
        bi = 0;
        foreach (Rectangle pr in portBoxList) {
            boxes[bi++] = pr;
        }
        clusters  = new LinkedList<Cluster> ();
        width     = portWidth;
        height    = portHeight;
        landscape = false;
        DecodeLatLonStrings ("portrait");

        /*
         * Convert all landscape cluster X,Y to portrait X,Y.
         */
        foreach (Cluster cluster in revLandClusters) {
            int landlox = cluster.lox;
            int landloy = cluster.loy;
            int landhix = cluster.hix;
            int landhiy = cluster.hiy;
            cluster.lox = portWidth - 1 - landhiy;
            cluster.loy = landlox;
            cluster.hix = portWidth - 1 - landloy;
            cluster.hiy = landhix;
            clusters.AddLast (cluster);
        }
        foreach (Cluster cluster in normLandClusters) {
            int landlox = cluster.lox;
            int landloy = cluster.loy;
            int landhix = cluster.hix;
            int landhiy = cluster.hiy;
            cluster.lox = landloy;
            cluster.loy = portHeight - 1 - landhix;
            cluster.hix = landhiy;
            cluster.hiy = portHeight - 1 - landlox;
            clusters.AddLast (cluster);
        }
        foreach (Cluster cluster in revPortClusters) {
            int revlox  = cluster.lox;
            int revloy  = cluster.loy;
            int revhix  = cluster.hix;
            int revhiy  = cluster.hiy;
            cluster.lox = portWidth  - revhix;
            cluster.loy = portHeight - revhiy;
            cluster.hix = portWidth  - revlox;
            cluster.hiy = portHeight - revloy;
            clusters.AddLast (cluster);
        }

        /*
         * If more than one cluster reports the same CenterX or CenterY,
         * assume that is a bogus report and clear it out.
         * This only matters for 'confused' clusters, ie, those that report
         * they are associated with both an X and a Y line marker.
         * See DEC cluster 88^53.0'W,88^52.5'W,88^52.0'W,88^51.5'W,88^51.0'W
         * and note that we must leave 39^49.5'N alone.
         */
        if (givens.Count == 0) {
            CheckMultpleAxisRefs ();
        }

        /*
         * Print out all the clusters (ie, lat/lon strings) that we found.
         */
        if (verbose) {
            foreach (Cluster cluster in clusters) {
                Console.WriteLine ("cluster (" + cluster.lox + "," + cluster.loy + ")=" + cluster.Result + "  " +
                                   cluster.CenterX + "," + cluster.CenterY);
            }
        }
    }

    /**
     * @brief Locate boxes around things that might be characters.
     *        Draw red boxes around them for debugging.
     */
    private static void FindCharacterBoxes ()
    {
        bmp = portBitmap; // for DrawBox()

        for (int y = 0; y < height; y ++) {
            for (int x = 0; x < width; x ++) {
                if (blacks[y,x] == 0) continue;
                int ytop = y;
                int ybot = y;
                int xlef = x;
                int xrit = x;
                if (ExpandPerimeter (ref xlef, ref xrit, ref ytop, ref ybot)) {
                    int xx = xlef;
                    int yy = ytop;
                    int ww = xrit - xlef + 1;
                    int hh = ybot - ytop + 1;
                    if ((ww <= Deco.MAXWH) && (hh > 1) && (hh <= Deco.MAXWH)) {
                        portBoxList.AddLast (new Rectangle (xx, yy, ww, hh));
                        DrawBox (xlef - 1, ytop - 1, xrit + 1, ybot + 1, Color.Red);
                    }
                    for (yy = ytop; yy <= ybot; yy ++) {
                        for (xx = xlef; xx <= xrit; xx ++) {
                            blacks[yy,xx] = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * @brief Decode pixels to corresponding characters and strings.
     */
    private static void DecodeLatLonStrings (string orientation)
    {
        allDecos = new LinkedList<Deco> ();

        /*
         * Scan the character boxes to see if we can decode characters therein.
         */
        foreach (Rectangle box in boxes) {
            int x = box.X;
            int y = box.Y;
            int w = box.Width;
            int h = box.Height;

            bool debug = false; // (x >= 1350) && (x <= 1470) && (y >= 720) && (y <= 750);
            if (debug) Console.WriteLine ("DecodeLatLonStrings*: " + x + "," + y + " " + w + "x" + h);

            /*
             * Sometimes we have a digit followed immediately by the degree symbol.
             * Make sure cell is at least 18x18 and is wider than it is high.
             * And make sure the lower right quarter (under degree symbol) is blank.
             * Eg, CBM 33^38'N.
             */
            if ((w > 17) && (h > 17) && (w > h - 2)) {

                /*
                 * Find largest rectangular blank area in lower right corner of box.
                 */
                int bestA = 0;
                int bestW = 0;
                int bestH = 0;
                int wlim  = w;
                int ww, hh;
                for (hh = 0; ++ hh <= h;) {
                    for (ww = 0; ++ ww <= wlim;) {
                        if (blacks[y+h-hh,x+w-ww] != 0) break;
                    }
                    wlim = -- ww;
                    int aa = hh * ww;
                    if (aa > bestA) {
                        bestA = aa;
                        bestW = ww;
                        bestH = hh;
                    }
                }
                if (debug) Console.WriteLine ("DecodeLatLonStrings*: bestWH=" + bestW + "x" + bestH);

                /*
                 * If there is at least a 9x9 area, assume it is a digit/degree
                 * stuck together.  Decode them as two characters.
                 */
                if ((bestW > 8) && (bestH > 8)) {
                    DecodeCharacter (x, y, w - bestW, h);
                    DecodeCharacter (x + w - bestW, y, bestW, h - bestH);
                    continue;
                }
            }

            /*
             * Presumably just a single character, decode it.
             */
            DecodeCharacter (x, y, w, h);
        }

        /*
         * Gather decoded characters into clusters.
         */
        GatherIntoClusters ();

        /*
         * Write out debug image.
         */
        if (stages) {
            bmp = new Bitmap (width, height);
            for (int y = 0; y < height; y ++) {
                for (int x = 0; x < width; x ++) {
                    bmp.SetPixel (x, y, (blacks[y,x] == 0) ? Color.White : Color.Black);
                }
            }
            foreach (Rectangle box in boxes) {
                DrawBox (box.X - 1, box.Y - 1, box.X + box.Width, box.Y + box.Height, Color.Red);
            }
            foreach (Cluster cluster in clusters) {
                DrawBox (cluster.lox - 2, cluster.loy - 2, cluster.hix + 1, cluster.hiy + 1, Color.Blue);
            }
            bmp.Save ("stage7_" + orientation + ".png");
            bmp.Dispose ();
        }
    }

    /**
     * @brief Try to decode the character at upper-left corner blacks[y,x]
     * @param x,y = upper left corner
     * @param w,h = width/height of box (exclusive)
     */
    private static void DecodeCharacter (int x, int y, int w, int h)
    {
        byte[,] grays = null;
        char ch;

        bool debug = false; // (x >= 2420) && (x <= 2580) && (y >= 1540) && (y <= 1580);
        if (debug) Console.WriteLine ("DecodeCharacter*: " + x + "," + y + " " + w + "x" + h);

        if ((w <= 0) || (w > Deco.MAXWH)) return;
        if ((h <= 0) || (h > Deco.MAXWH)) return;

        // decimal point
        if ((w < 5) && (h < 5)) {
            ch = '.';
        }

        // seconds mark
        else if (CheckForSecMark (x, y, w, h)) {
            ch = '\'';
        }

        // degrees mark
        else if ((w < 13) && (h < 13)) {
            ch = '^';
        }

        // digit '1'
        else if (CheckForOneDigit (x, y, w, h)) {
            ch = '1';
        }

        // something that needs to be decoded
        else if ((w > 7) && (h > 13)) {
            grays = BuildGraysArray (x, y, w, h);
            ch = DecodeGraysArray (grays, false);

            /*
             * Sometimes we misread what is really a 2 as a 7.
             * Real 7's don't have any black pixels in lower right corner.
             * See NUW 122^38'W (the second 2).
             */
            if (ch == '7') {
                int nb = blacks[y+h-1,x+w-1] + blacks[y+h-1,x+w-2] + blacks[y+h-1,x+w-3] +
                         blacks[y+h-2,x+w-1] + blacks[y+h-2,x+w-2] + blacks[y+h-2,x+w-3];
                if (nb > 0) ch = '2';
            }
        }

        // some garbage
        else return;

        if (debug) Console.WriteLine ("DecodeCharacter*: " + x + "," + y + " " + w + "x" + h + " => " + ch);

        // add to list of all decoded characters by ascending X value.
        // because strings go from left-to-right (ascending X value).
        // we sort out the Y values in GatherIntoClusters().
        Deco deco = new Deco ();
        deco.x = x;
        deco.y = y;
        deco.w = w;
        deco.h = h;
        deco.c = ch;
        deco.grays = grays;

        for (LinkedListNode<Deco> ptr = allDecos.First; ptr != null; ptr = ptr.Next) {
            Deco d = ptr.Value;
            if ((deco.x < d.x) || ((deco.x == d.x) && (deco.y < d.y))) {
                allDecos.AddBefore (ptr, deco);
                return;
            }
        }
        allDecos.AddLast (deco);
    }

    /**
     * @brief See if we have an apostrophe character.
     *        Detect black spot in the center as there are small degree marks
     *        eg, GTB 44^04'N.
     */
    private static bool CheckForSecMark (int x, int y, int w, int h)
    {
        if (w >= 7) return false;
        if (h >= 9) return false;

        /*
         * If there's a black spot in the center, it's an apostrophe.
         */
        double xc = w / 2.0 + x;
        double yc = h / 2.0 + y;

        int xleft = (int)(xc - 0.499);
        int xrite = (int)(xc + 0.499);
        int ytop  = (int)(yc - 0.499);
        int ybot  = (int)(yc + 0.499);

        int c = blacks[ytop,xleft] + blacks[ytop,xrite] + blacks[ybot,xleft] + blacks[ybot,xrite];
        if (c >= 2) return true;

        /*
         * If there is black in upper left corner AND in lower right,
         * it's a degree mark, else it's an apostrophe.
         */
        c = 0;
        for (int yy = 0; yy < h / 2; yy ++) {
            for (int xx = 0; xx < w / 2; xx ++) {
                c += blacks[y+yy,x+xx];
            }
        }
        if (c != 0) {
            for (int yy = h; -- yy >= h / 2;) {
                for (int xx = w; -- x >= w / 2;) {
                    c += blacks[y+yy,x+xx];
                }
            }
        }
        return (c == 0);
    }

    /**
     * @brief See if we have a '1' digit character.
     *        They are all black down the right side.
     *        They are all black along the top.
     *        They are white all down the left side except for the top part.
     */
    private static bool CheckForOneDigit (int x, int y, int w, int h)
    {
        if (w <  3) return false;
        if (w >  9) return false;
        if (h < 15) return false;
        if (h > 25) return false;
        for (int yy = 1; yy < h - 1; yy ++) {
            if ((blacks[y+yy,x+w-1] | blacks[y+yy,x+w-2] | blacks[y+yy,x+w-3]) == 0) return false;
        }
        for (int xx = 1; xx < w - 1; xx ++) {
            if ((blacks[y,x+xx] | blacks[y+1,x+xx] | blacks[y+2,x+xx]) == 0) return false;
        }
        for (int yy = 4; ++ y < h;) {
            if ((blacks[y+yy,x] | blacks[y+yy,x+1]) != 0) return false;
        }
        return true;
    }

    /**
     * @brief Pixellate the given pixels into a gray-scale array.
     * @param x,y = upper-left corner pixel
     * @param w,h = size of the input array
     * @returns grayscale array of size [D09H,D09W]
     */
    public static byte[,] BuildGraysArray (int x, int y, int w, int h)
    {
        /*
         * Re-size input black/white matrix into D09WxD09H gray-scale matrix.
         */
        double dmax = (double)h / (double)D09H * (double)w / (double)D09W;
        double[,] dubs = new double[D09H,D09W];
        for (int dy = 0; dy < D09H; dy ++) {
            double blktop = (double)h / (double)D09H * (double)dy;
            double blkbot = (double)h / (double)D09H * (double)(dy+1);
            for (int dx = 0; dx < D09W; dx ++) {
                double blklef = (double)w / (double)D09W * (double)dx;
                double blkrit = (double)w / (double)D09W * (double)(dx+1);
                for (int by = (int)blktop; (double)by < blkbot; by ++) {
                    for (int bx = (int)blklef; (double)bx < blkrit; bx ++) {
                        if (blacks[y+by,x+bx] > 0) {
                            double z = 1.0;
                            if ((double)by < blktop) z *= blktop - (double)by;
                            if ((double)bx < blklef) z *= blklef - (double)bx;
                            if ((double)(by+1) > blkbot) z *= (double)(by+1) - blkbot;
                            if ((double)(bx+1) > blkrit) z *= (double)(bx+1) - blkrit;
                            dubs[dy,dx] += z;
                        }
                    }
                }
            }
        }

        /*
         * Return grayscale to caller.
         */
        byte[,] grays = new byte[D09H,D09W];
        for (int dy = 0; dy < D09H; dy ++) {
            for (int dx = 0; dx < D09W; dx ++) {
                int gray = (int)((1.0 - dubs[dy,dx] / dmax) * 256.0);
                if (gray <   0) gray =   0;
                if (gray > 255) gray = 255;
                grays[dy,dx] = (byte)gray;
            }
        }

        return grays;
    }

    /**
     * @brief Decode a given grays array to the character it represents.
     */
    private static char DecodeGraysArray (byte[,] grays, bool debug)
    {
        char bestChar = '?';
        if (givens.Count == 0) {
            double bestScore = 0;
            foreach (char c in counts.Keys) {
                int count = counts[c];
                if (count > 0) {
                    int[,] learntc = learnt[c];
                    double score = 0;
                    for (int y = 0; y < D09H; y ++) {
                        for (int x = 0; x < D09W; x ++) {
                            double diff = (double)(int)grays[y,x] - (double)learntc[y,x] / (double)count;
                            score += diff * diff;
                        }
                    }
                    if (debug) Console.WriteLine ("DecodeGraysArray*: [" + c + "] score " + score);
                    if ((bestChar == '?') || (score < bestScore)) {
                        bestChar = c;
                        bestScore = score;
                    }
                }
            }
        }
        return bestChar;
    }

    /**
     * @brief Gather the separate decoded characters into clusters.
     */
    private static void GatherIntoClusters ()
    {
        while (allDecos.Count > 0) {

            /*
             * Find leftmost character.
             */
            Deco deco = allDecos.First.Value;
            allDecos.RemoveFirst ();

            bool debug = false; // (deco.x >= 1050) && (deco.x <= 1190) && (deco.y >= 1320) && (deco.y <= 1360);
            if (debug) Console.WriteLine ("GatherIntoClusters*: " + deco.x + "," + deco.y);

            /*
             * Sometimes there are stray marks around that look like ' and ..
             * Skip over them so they don't obscure a nearby legitimate number.
             * See BAB 39^07'N.
             */
            if ((deco.c == '\'') || (deco.c == '.')) continue;

            /*
             * Create a cluster for it.
             */
            Cluster cluster = new Cluster ();
            cluster.vertical = landscape;

            /*
             * Append first character to cluster and likewise with all
             * subsequent nearby characters to its right that overlap on
             * the Y axis.
             */
        useit:
            if (debug) Console.WriteLine ("GatherIntoClusters*:   " + deco.c);
            cluster.InsertDeco (deco);
            if ((deco.c == 'N') || (deco.c == 'S') || (deco.c == 'E') || (deco.c == 'W')) goto endclus;

            for (LinkedListNode<Deco> ptr = allDecos.First; ptr != null; ptr = ptr.Next) {
                Deco dd = ptr.Value;                      // scan by ascending X value
                if (debug) Console.WriteLine ("GatherIntoClusters*: ? " + dd.c + " " + dd.x + "," + dd.y);
                if (dd.x > cluster.hix + MAXGAP) break;   // if too far right, nothing more can match
                if (dd.x < cluster.hix - 2) continue;     // if too far left, just ignore it

                /*
                 * We have char dd just to the right of char deco
                 * but we don't know if they are vertically aligned.
                 *
                 * Sometimes we mis-decode an apostrophe as a dot.
                 * Eg, HOP 36^40'N
                 * So we have to fix it in context.
                 */
                int ydiff = dd.y - deco.y;
                if (dd.c == '.') {
                    if ((ydiff >= -YOVER) && (ydiff <= YOVER)) {
                        // the dot is near the top of the line of chars
                        dd.c = '\'';
                    } else {
                        ydiff += dd.h - deco.h;
                        if ((ydiff < -YOVER) || (ydiff > YOVER)) continue;
                        // the dot is near the bottom of the line of chars
                    }
                    dd.y = deco.y;
                    dd.h = deco.h;
                } else {
                    if ((ydiff < -YOVER) || (ydiff > YOVER)) continue;
                }

                /*
                 * Remove from allDecos and append to cluster.
                 */
                allDecos.Remove (ptr);
                deco = dd;
                goto useit;
            }

            /*
             * If valid size, append to list of all clusters and draw box around it.
             */
        endclus:
            if (cluster.IsLatLon) {
                clusters.AddLast (cluster);
            }
        }
    }

    /**
     * @brief If multiple lats or lons refer to the same horizontal or vertical tick line,
     *        then it is a bogus reference.  Like we might have this:
     *
     *                              |                 |                 |
     *          34^50.5N -----------+-----------------+-----------------+--------
     *                              |                 |                 |
     *                          72^25.5'W         72^25.0'W         72^24.5'W
     *
     *        and all the longitudes will refer to the 34^50.5N's line as well as their own.
     *        So we want to get rid of their reference to the 34^50.5N's line.  But leave the
     *        34^50.5N's reference to the horizontal line alone.
     */
    private static void CheckMultpleAxisRefs ()
    {
        /*
         * See which kind of axes each of the lat/lon strings refer to.
         */
        int latsAreHoriz = 0;  // how many lat strings refer to horizontal tick marked lines
        int latsAreVert  = 0;  // how many lat strings refer to vertical tick marked lines
        int lonsAreHoriz = 0;  // how many lon strings refer to horizontal tick marked lines
        int lonsAreVert  = 0;  // how many lon strings refer to vertical tick marked lines

        foreach (Cluster cluster in clusters) {
            if (cluster.Latitude != 0) {
                if (cluster.CenterX != 0) latsAreVert  ++;
                if (cluster.CenterY != 0) latsAreHoriz ++;
            }
            if (cluster.Longitude != 0) {
                if (cluster.CenterX != 0) lonsAreVert  ++;
                if (cluster.CenterY != 0) lonsAreHoriz ++;
            }
        }

        if (verbose) {
            Console.WriteLine ("latsAreHoriz = " + latsAreHoriz);
            Console.WriteLine ("latsAreVert  = " + latsAreVert);
            Console.WriteLine ("lonsAreHoriz = " + lonsAreHoriz);
            Console.WriteLine ("lonsAreVert  = " + lonsAreVert);
        }

        /*
         * Consider us to be in portrait mode (eg BVY) if there are more vertical longitude and 
         * horizontal latitudetick marked lines than there are horizontal longitude and vertical 
         * latitude lines.  Otherwise we are in landscape mode (eg BED).
         */
        bool port = (lonsAreVert + latsAreHoriz > lonsAreHoriz + latsAreVert);
        bool land = (latsAreVert + lonsAreHoriz > latsAreHoriz + lonsAreVert);
        if (!port && !land) {
            throw new Exception ("can't determine if portrait or landscape orientation");
        }
        landscape = land;

        /*
         * Now zero out any references that are contrary to that.
         */
        foreach (Cluster cluster in clusters) {
            if (cluster.Latitude != 0) {
                if (port) cluster.ZeroCenterX ();   // portrait latitude lines are not vertical
                if (land) cluster.ZeroCenterY ();   // landscape latitude lines are not horizontal
            }
            if (cluster.Longitude != 0) {
                if (port) cluster.ZeroCenterY ();   // portrait longitude lines are not horizontal
                if (land) cluster.ZeroCenterX ();   // landscape longitude lines are not vertical
            }
        }
    }

    /**
     * @brief We have some givens on the command line so we are in learning mode.
     *        Locate the given strings in the image then add the characters vs pixel patterns to the
     *        learned characters file.
     */
    private static void BuildLearning ()
    {
        /*
         * Scan clusters, looking for those matched by a given.
         */
        foreach (Given gv in givens) {
            XY gvRot = RotateImagePoint (gv.x, gv.y, latLonSlopeX, latLonSlopeY);
            foreach (Cluster cl in clusters) {
                string result = cl.Result;

                /*
                 * Match the string lengths and approximate positions.
                 */
                if (result.Length != gv.s.Length) continue;
                if ((gvRot.x - cl.lox) * (gvRot.x - cl.lox) + (gvRot.y - cl.loy) * (gvRot.y - cl.loy) > 100) continue;

                /*
                 * For learnable characters, average the pixel values in learnt array.
                 * For non-learnables, the characters should exactly match.
                 */
                for (int i = 0; i < gv.s.Length; i ++) {
                    char c = gv.s[i];
                    Deco deco = cl.GetDeco (i);
                    if (deco.grays == null) {

                        /*
                         * Not learnable, decoded character should exactly match given character.
                         */
                        if (deco.c != c) break;
                    } else {

                        /*
                         * Learnable, maybe make array entry for never seen before character.
                         */
                        if (!counts.ContainsKey (c)) {
                            counts[c] = 0;
                            learnt[c] = new int[D09H,D09W];
                        }

                        /*
                         * Then accumulate the new grays array info.
                         */
                        counts[c] = counts[c] + 1;
                        int[,] learntc = learnt[c];
                        for (int y = 0; y < D09H; y ++) {
                            for (int x = 0; x < D09W; x ++) {
                                learntc[y,x] += deco.grays[y,x];
                            }
                        }
                    }
                }

                /*
                 * Given found and completely matched.
                 */
                gv.found = true;
                break;
            }
        }

        /*
         * Print out any givens that weren't found as an error message.
         */
        foreach (Given gv in givens) {
            if (!gv.found) {
                Console.WriteLine ("given " + gv.x + "," + gv.y + "=" + gv.s + " not found");
            }
        }

        /*
         * Write out new learning.
         */
        StreamWriter learntwriter = new StreamWriter ("ReadArptDgmPng_learnt.csv");
        foreach (char c in counts.Keys) {
            StringBuilder line = new StringBuilder ();
            line.Append (c);
            line.Append (',');
            line.Append (counts[c]);
            int[,] learntc = learnt[c];
            for (int y = 0; y < D09H; y ++) {
                for (int x = 0; x < D09W; x ++) {
                    line.Append (',');
                    line.Append (learntc[y,x]);
                }
            }
            learntwriter.WriteLine (line.ToString ());
        }
        learntwriter.Close ();

        /*
         * Write resultant learned digits to ReadArptDgmPng_learnt.png (debugging only).
         */
        Bitmap learntbmp = new Bitmap (counts.Count * (D09R * D09W + 1) + 1, D09R * D09H + 2);
        for (int y = D09R * D09H + 2; -- y >= 0;) {
            for (int x = counts.Count * (D09R * D09W + 1) + 1; -- x >= 0;) {
                learntbmp.SetPixel (x, y, Color.Pink);
            }
        }
        int j = 0;
        foreach (char c in counts.Keys) {
            int count = counts[c];
            int[,] learntc = learnt[c];
            int xc = j * (D09W * D09R + 1);
            for (int y = 0; y < D09H; y ++) {
                for (int x = 0; x < D09W; x ++) {
                    int gray = learntc[y,x] / count;
                    Color color = Color.FromArgb (gray, gray, gray);
                    for (int yy = 0; yy < D09R; yy ++) {
                        for (int xx = 0; xx < D09R; xx ++) {
                            learntbmp.SetPixel (xc + x * D09R + xx + 1, y * D09R + yy + 1, color);
                        }
                    }
                }
            }
            j ++;
        }
        learntbmp.Save ("ReadArptDgmPng_learnt.png", System.Drawing.Imaging.ImageFormat.Png);
    }

    /**
     * @brief Build and print latlon <-> pixel transformation matrix.
     */
    private static void BuildXformMatrix ()
    {
        /*
         * Tell each cluster what the decided orientation is, in case they are confused
         * (ie, at the intersection of a vertical and horizontal line).
         */
        foreach (Cluster cluster in clusters) {
            cluster.SetOrientation (landscape);
        }

        /*
         * Find the lowest and highest lon's and lat's.
         */
        Cluster highestLat = null;
        Cluster highestLon = null;
        Cluster lowestLat  = null;
        Cluster lowestLon  = null;
        foreach (Cluster cluster in clusters) {
            if ((cluster.CenterX == 0) && (cluster.CenterY == 0)) continue;
            double lat = cluster.Latitude;
            double lon = cluster.Longitude;
            if (lat != 0) {
                if ((highestLat == null) || (highestLat.Latitude < lat)) highestLat = cluster;
                if ((lowestLat  == null) || (lowestLat.Latitude  > lat)) lowestLat  = cluster;
            }
            if (lon != 0) {
                if ((highestLon == null) || (highestLon.Longitude < lon)) highestLon = cluster;
                if ((lowestLon  == null) || (lowestLon.Longitude  > lon)) lowestLon  = cluster;
            }
        }

        /*
         * If fewer than two of each complain.
         * But some charts only have one of one of them (two of the other),
         * so we can fake them by assuming 1:1 pixel ratio.
         */
        string oneLinerValue = null;
        if ((lowestLat == highestLat) || (lowestLon == highestLon)) {
            if ((lowestLat == null) || (lowestLon == null) || 
                ((lowestLat == highestLat) && (lowestLon == highestLon)) ||
                !oneLiners.TryGetValue (lowestLat.Result + "," + lowestLon.Result, out oneLinerValue)) {
                throw new Exception ("fewer than two lats or lons found");
            }
        }

        /*
         * Just one latitude given, make second one by using the given two longitude lines.
         */
        if (highestLat == lowestLat) {
            Cluster madeUpLat = new Cluster (oneLinerValue);
            double f = madeUpLat.Latitude - lowestLat.Latitude;
            f /= highestLon.Longitude - lowestLon.Longitude;
            f /= Math.Cos ((lowestLat.Latitude + madeUpLat.Latitude) / 360.0 * Math.PI);
            int madeUpLatPixX = (int)((highestLon.CenterY - lowestLon.CenterY) * f + 0.5) + lowestLat.CenterX;
            int madeUpLatPixY = (int)((highestLon.CenterX - lowestLon.CenterX) * f + 0.5) + lowestLat.CenterY;
            madeUpLat.SetCenter (madeUpLatPixX, madeUpLatPixY);
            clusters.AddLast (madeUpLat);
            if (madeUpLat.Latitude > lowestLat.Latitude) {
                highestLat = madeUpLat;
            } else {
                lowestLat = madeUpLat;
            }
        }

        /*
         * Just one longitude given, make second one by using the given two latitude lines.
         */
        if (highestLon == lowestLon) {
            Cluster madeUpLon = new Cluster (oneLinerValue);
            double f = madeUpLon.Longitude - lowestLon.Longitude;
            f /= highestLat.Latitude - lowestLat.Latitude;
            f *= Math.Cos ((lowestLat.Latitude + highestLat.Latitude) / 360.0 * Math.PI);
            int madeUpLonPixX = (int)((lowestLat.CenterY - highestLat.CenterY) * f + 0.5) + lowestLon.CenterX;
            int madeUpLonPixY = (int)((lowestLat.CenterX - highestLat.CenterX) * f + 0.5) + lowestLon.CenterY;
            madeUpLon.SetCenter (madeUpLonPixX, madeUpLonPixY);
            clusters.AddLast (madeUpLon);
            if (madeUpLon.Longitude > lowestLon.Longitude) {
                highestLon = madeUpLon;
            } else {
                lowestLon = madeUpLon;
            }
        }

        /*
         * Extract values.
         */
        double lowestLatLat  = lowestLat.Latitude;
        double highestLatLat = highestLat.Latitude;
        double lowestLonLon  = lowestLon.Longitude;
        double highestLonLon = highestLon.Longitude;

        int lowestLatPixelX  = lowestLat.CenterX;
        int highestLatPixelX = highestLat.CenterX;
        int lowestLonPixelX  = lowestLon.CenterX;
        int highestLonPixelX = highestLon.CenterX;

        int lowestLatPixelY  = lowestLat.CenterY;
        int highestLatPixelY = highestLat.CenterY;
        int lowestLonPixelY  = lowestLon.CenterY;
        int highestLonPixelY = highestLon.CenterY;

        if (verbose) {
            Console.WriteLine (" lowestLat = " + lowestLat.Result  + " = " + lowestLatLat  + " at rotated (" + lowestLatPixelX  + "," + lowestLatPixelY  + ")");
            Console.WriteLine ("highestLat = " + highestLat.Result + " = " + highestLatLat + " at rotated (" + highestLatPixelX + "," + highestLatPixelY + ")");
            Console.WriteLine (" lowestLon = " + lowestLon.Result  + " = " + lowestLonLon  + " at rotated (" + lowestLonPixelX  + "," + lowestLonPixelY  + ")");
            Console.WriteLine ("highestLon = " + highestLon.Result + " = " + highestLonLon + " at rotated (" + highestLonPixelX + "," + highestLonPixelY + ")");
        }

        /*
         * For the rotated image:
         *   lon = tfwA * pixx + tfwC * pixy + tfwE
         *   lat = tfwB * pixx + tfwD * pixy + tfwF
         *
         *   pixx = (tfwD * lon - tfwC * lat - tfwD * tfwE + tfwC * tfwF) / (tfwD * tfwA - tfwC * tfwB)
         *   pixy = (tfwB * lon - tfwA * lat - tfwB * tfwE + tfwA * tfwF) / (tfwB * tfwC - tfwA * tfwD)
         */
        double tfwA = 0, tfwB = 0, tfwC = 0, tfwD = 0, tfwE = 0, tfwF = 0;

        if (highestLonPixelX != lowestLonPixelX) {
            tfwA = (highestLonLon - lowestLonLon) / (highestLonPixelX - lowestLonPixelX);
            tfwE = highestLonLon - tfwA * highestLonPixelX;
        }
        if (highestLonPixelY != lowestLonPixelY) {
            tfwC = (highestLonLon - lowestLonLon) / (highestLonPixelY - lowestLonPixelY);
            tfwE = highestLonLon - tfwC * highestLonPixelY;
        }

        if (highestLatPixelX != lowestLatPixelX) {
            tfwB = (highestLatLat - lowestLatLat) / (highestLatPixelX - lowestLatPixelX);
            tfwF = highestLatLat - tfwB * highestLatPixelX;
        }
        if (highestLatPixelY != lowestLatPixelY) {
            tfwD = (highestLatLat - lowestLatLat) / (highestLatPixelY - lowestLatPixelY);
            tfwF = highestLatLat - tfwD * highestLatPixelY;
        }

        if (verbose) {
            Console.WriteLine ("rotated tfwA=" + tfwA);
            Console.WriteLine ("rotated tfwB=" + tfwB);
            Console.WriteLine ("rotated tfwC=" + tfwC);
            Console.WriteLine ("rotated tfwD=" + tfwD);
            Console.WriteLine ("rotated tfwE=" + tfwE);
            Console.WriteLine ("rotated tfwF=" + tfwF);
        }

        /*
         * Validate by checking that all decoded lat/lon markers can be converted to their pixel number.
         */
        LinkedList<Cluster> latClusters = new LinkedList<Cluster> ();
        LinkedList<Cluster> lonClusters = new LinkedList<Cluster> ();
        foreach (Cluster cluster in clusters) {
            // verified earlier that exactly one of lat/lon is non-zero
            if (cluster.Latitude  != 0) latClusters.AddLast (cluster);
            if (cluster.Longitude != 0) lonClusters.AddLast (cluster);
        }

        bool bad = false;
        foreach (Cluster lonClus in lonClusters) {
            foreach (Cluster latClus in latClusters) {
                double lat = latClus.Latitude;
                double lon = lonClus.Longitude;
                int pixx = latClus.CenterX | lonClus.CenterX;
                int pixy = latClus.CenterY | lonClus.CenterY;
                if (verbose) Console.WriteLine ("verifying latlon " + latClus.Result + "," + lonClus.Result + " = " +
                                   lat + "," + lon + "  =>  rotated pixel " + pixx + "," + pixy);

                int comx = (int)((tfwD * lon - tfwC * lat - tfwD * tfwE + tfwC * tfwF) / (tfwD * tfwA - tfwC * tfwB) + 0.5);
                int comy = (int)((tfwB * lon - tfwA * lat - tfwB * tfwE + tfwA * tfwF) / (tfwB * tfwC - tfwA * tfwD) + 0.5);
                if (pixx == 0) pixx = comx;
                if (pixy == 0) pixy = comy;
                double clat = tfwB * pixx + tfwD * pixy + tfwF;
                double clon = tfwA * pixx + tfwC * pixy + tfwE;
                if (verbose) Console.WriteLine ("  computed rotated pixel " + comx + "," + comy + "  =>  latlon " + clat + "," + clon);

                comx -= pixx;
                comy -= pixy;
                clat -= lat;
                clon -= lon;

                if (comx < -VFYPIXERR || comx > VFYPIXERR) {
                    Console.WriteLine ("horizontal verify error " + comx);
                    bad = true;
                }
                if (comy < -VFYPIXERR || comy > VFYPIXERR) {
                    Console.WriteLine ("vertical verify error " + comy);
                    bad = true;
                }
                if (clat < -VFYDEGERR || clat > VFYDEGERR) {
                    Console.WriteLine ("latitude verify error " + clat);
                    bad = true;
                }
                if (clon < -VFYDEGERR || clon > VFYDEGERR) {
                    Console.WriteLine ("longitude verify error " + clon);
                    bad = true;
                }
            }
        }

        /*
         * Also, lat and lon pixel sizes should be the same.
         */
        double lonperpix = Math.Abs (tfwA + tfwC);  // only one of tfwA,tfwC is non-zero
        double latperpix = Math.Abs (tfwB + tfwD);  // only one of tfwB,tfwD is non-zero
        double latcosine = Math.Cos ((highestLatLat + lowestLatLat) / 360.0 * Math.PI);
        double llperpixratio = lonperpix * latcosine / latperpix;
        double ratioshouldbe;
        if (!notsquare.TryGetValue (lowestLat.Result + "," + lowestLon.Result, out ratioshouldbe)) {
            ratioshouldbe = 1.0;
        }
        if (verbose) {
            Console.WriteLine ("lon per pix=" + lonperpix + ", lat per pix=" + latperpix + ", ratio=" + llperpixratio + ", expecting=" + ratioshouldbe);
        }
        llperpixratio /= ratioshouldbe;
        if (llperpixratio > 1.0) llperpixratio = 1.0 / llperpixratio;
        if (llperpixratio < VFYPIXRAT) {
            Console.WriteLine ("pixels are not square");
            bad = true;
        }

        if (bad) throw new Exception ("latlon <-> rotated pixel verify error");

        /*
         * Now we have:
         *
         *    Mr * TCAr * Rc * TACo * Po = L
         *
         *      Mr = [ tfwA tfwC tfwE ]     the TFW matrix from above calcuations
         *           [ tfwB tfwD tfwF ]     that will transform a rotated pixel
         *           [   0    0    1  ]     into lat/lon
         *
         *      TCAr = [ 1 0  Wr/2 ]        translate center-relative to absolue in the rotated image
         *             [ 0 1  Hr/2 ]
         *             [ 0 0   1   ]
         *
         *      Rc = [  cos th  sin th  0 ] rotates a center-relative pixel in the original image
         *           [ -sin th  cos th  0 ] to corresponding center-relative pixel in rotated image
         *           [    0       0     1 ]
         *
         *      TACo = [ 1 0 -Wo/2 ]        translate absolute to center-relative in the original image
         *             [ 0 1 -Ho/2 ]
         *             [ 0 0   1   ]
         *
         *      Po = [ Xo ]                 an arbitrary pixel in the original image
         *           [ Yo ]
         *           [  1 ]
         *
         *      L  = [ lon ]                the corresponding lat/lon coordinate in the real world
         *           [ lat ]
         *           [  1  ]
         */
        double[,] TACo = new double[3,3] { { 1, 0, -origWidth  / 2.0 }, 
                                           { 0, 1, -origHeight / 2.0 },
                                           { 0, 0,             1     } };

        double denom = Math.Sqrt (latLonSlopeX * latLonSlopeX + latLonSlopeY * latLonSlopeY);
        double costh = latLonSlopeX / denom;
        double sinth = latLonSlopeY / denom;
        double[,] Rc   = new double[3,3] { {  costh, sinth, 0 }, 
                                           { -sinth, costh, 0 },
                                           {    0,     0,   1 } };

        double[,] TCAr = new double[3,3] { { 1, 0, portWidth  / 2.0 },
                                           { 0, 1, portHeight / 2.0 },
                                           { 0, 0,            1     } };

        double[,] Mr   = new double[3,3] { { tfwA, tfwC, tfwE },
                                           { tfwB, tfwD, tfwF },
                                           {   0,    0,    1  } };

        /*
         * And so we can compute:
         *
         *    Mo = Mr * TCAr * Rc * TACo
         *
         * Such that:
         *
         *    Mo * Po = L
         */
        double[,] Mo = MatMul (MatMul (Mr, TCAr), MatMul (Rc, TACo));

        /*
         * Extract TFW values that convert a pixel in the original image
         * directly to the corresponding latitude/longitude.
         */
        tfwA = Mo[0,0];
        tfwB = Mo[1,0];
        tfwC = Mo[0,1];
        tfwD = Mo[1,1];
        tfwE = Mo[0,2];
        tfwF = Mo[1,2];

        if (verbose) {
            Console.WriteLine ("final tfwA=" + tfwA);
            Console.WriteLine ("final tfwB=" + tfwB);
            Console.WriteLine ("final tfwC=" + tfwC);
            Console.WriteLine ("final tfwD=" + tfwD);
            Console.WriteLine ("final tfwE=" + tfwE);
            Console.WriteLine ("final tfwF=" + tfwF);
        }

        /*
         * Calculate inverse TFW values to convert a lat/lon to a pixel in
         * the original image.
         *
         * Compute:
         *    MoInv = 1 / Mo
         *
         * Given from above:
         *    Mo * Po = L
         *
         * Multiply both sides by MoInv:
         *    MoInv * Mo * Po = MoInv * L
         *
         * And we get:
         *    Po = MoInv * L
         *
         *    pixx = wftA * lon + wftC * lat + wftE
         *    pixy = wftB * lon + wftD * lat + wftF
         */
        double[,] MoInv = MatInv (Mo);
        double wftA = MoInv[0,0];
        double wftB = MoInv[1,0];
        double wftC = MoInv[0,1];
        double wftD = MoInv[1,1];
        double wftE = MoInv[0,2];
        double wftF = MoInv[1,2];

        if (verbose) {
            Console.WriteLine ("final wftA=" + wftA);
            Console.WriteLine ("final wftB=" + wftB);
            Console.WriteLine ("final wftC=" + wftC);
            Console.WriteLine ("final wftD=" + wftD);
            Console.WriteLine ("final wftE=" + wftE);
            Console.WriteLine ("final wftF=" + wftF);
        }

        /*
         * Maybe write to .csv file.
         * Lines are of the form:
         *   csvoutid,tfwA,tfwB,tfwC,tfwD,tfwE,tfwF,wftA,wftB,wftC,wftD,wftE,wftF
         */
        if ((csvoutfile != null) && (csvoutid != null)) {
            FileStream fs = null;
            int retry = 3;
            while (fs == null) {
                try {
                    fs = File.Open (csvoutfile, FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
                } catch (IOException) {
                    if (-- retry < 0) throw;
                    System.Threading.Thread.Sleep (345);
                }
            }

            SortedDictionary<string,string> oldcsvlines = new SortedDictionary<string,string> ();
            fs.Seek (0, SeekOrigin.Begin);
            StreamReader sr = new StreamReader (fs);
            string lnrd;
            while ((lnrd = sr.ReadLine ()) != null) {
                int i = lnrd.IndexOf (',');
                oldcsvlines[lnrd.Substring(0,i)] = lnrd;
            }
            StringBuilder sb = new StringBuilder ();
            sb.Append (csvoutid);
            sb.Append (',');
            sb.Append (tfwA);
            sb.Append (',');
            sb.Append (tfwB);
            sb.Append (',');
            sb.Append (tfwC);
            sb.Append (',');
            sb.Append (tfwD);
            sb.Append (',');
            sb.Append (tfwE);
            sb.Append (',');
            sb.Append (tfwF);
            sb.Append (',');
            sb.Append (wftA);
            sb.Append (',');
            sb.Append (wftB);
            sb.Append (',');
            sb.Append (wftC);
            sb.Append (',');
            sb.Append (wftD);
            sb.Append (',');
            sb.Append (wftE);
            sb.Append (',');
            sb.Append (wftF);
            oldcsvlines[csvoutid] = sb.ToString ();
            fs.Seek (0, SeekOrigin.Begin);
            StreamWriter sw = new StreamWriter (fs);
            foreach (string lnwr in oldcsvlines.Values) {
                sw.WriteLine (lnwr);
            }
            sw.Close ();
        }

        /*
         * Maybe create marked-up .png file.
         */
        if (markedpng != null) {

            /*
             * Get original image with white background.
             */
            bmp    = new Bitmap (pngname);
            width  = bmp.Width;
            height = bmp.Height;
            for (int y = 0; y < height; y ++) {
                for (int x = 0; x < width; x ++) {
                    Color c = bmp.GetPixel (x, y);
                    int w = 255 - c.A;
                    if (w != 0) {
                        bmp.SetPixel (x, y, Color.FromArgb (c.R + w, c.G + w, c.B + w));
                    }
                }
            }

            /*
             * Draw red crosses wherever lat/lon lines intersect.
             */
            foreach (Cluster lonClus in lonClusters) {
                foreach (Cluster latClus in latClusters) {
                    double lat = latClus.Latitude;
                    double lon = lonClus.Longitude;
                    int x = (int)(wftA * lon + wftC * lat + wftE + 0.5);
                    int y = (int)(wftB * lon + wftD * lat + wftF + 0.5);
                    DrawCross (x, y, 25, 2, Color.Red);
                }
            }

            bmp.Save (markedpng);
        }
    }

    /**
     * @brief Perform matrix multiplication.
     * @returns A * B
     */
    private static double[,] MatMul (double[,] A, double[,] B)
    {
        int arows = A.GetLength (0);
        int acols = A.GetLength (1);
        int brows = B.GetLength (0);
        int bcols = B.GetLength (1);

        if (acols != brows) throw new Exception ("A cols != B rows");

        double[,] C = new double[arows,bcols];

        for (int crow = 0; crow < arows; crow ++) {
            for (int ccol = 0; ccol < bcols; ccol ++) {
                double sum = 0;
                for (int i = 0; i < acols; i ++) {
                    sum += A[crow,i] * B[i,ccol];
                }
                C[crow,ccol] = sum;
            }
        }

        return C;
    }

    private static double[,] MatInv (double[,] A)
    {
        int arows = A.GetLength (0);
        int acols = A.GetLength (1);
        if (arows != acols) throw new Exception ("A not square");

        double[,] T = new double[arows,2*arows];
        for (int row = 0; row < arows; row ++) {
            for (int col = 0; col < arows; col ++) {
                T[row,col] = A[row,col];
            }
            T[row,arows+row] = 1.0;
        }

        RowReduce (T);

        double[,] I = new double[arows,arows];
        for (int row = 0; row < arows; row ++) {
            for (int col = 0; col < arows; col ++) {
                I[row,col] = T[row,arows+col];
            }
        }

        return I;
    }

    private static void RowReduce (double[,] T)
    {
        int trows = T.GetLength (0);
        int tcols = T.GetLength (1);

        double pivot;
        for (int row = 0; row < trows; row ++) {

            /*
             * Make this row's major diagonal colum one by
             * dividing the whole row by that number.
             * But if the number is zero, swap with some row below.
             */
            pivot = T[row,row];
            if (pivot == 0.0) {
                int swaprow;
                for (swaprow = row; ++ swaprow < trows;) {
                    pivot = T[swaprow,row];
                    if (pivot != 0.0) {
                        for (int col = 0; col < tcols; col ++) {
                            double tmp = T[row,col];
                            T[row,col] = T[swaprow,col];
                            T[swaprow,col] = tmp;
                        }
                        break;
                    }
                }
                if (swaprow >= trows) throw new Exception ("not invertable");
            }
            if (pivot != 1.0) {
                for (int col = row; col < tcols; col ++) {
                    T[row,col] /= pivot;
                }
            }

            /*
             * Subtract this row from all below it such that we zero out
             * this row's major diagonal column in all rows below.
             */
            for (int rr = row; ++ rr < trows;) {
                pivot = T[rr,row];
                if (pivot != 0.0) {
                    for (int cc = row; cc < tcols; cc ++) {
                        T[rr,cc] -= pivot * T[row,cc];
                    }
                }
            }
        }

        for (int row = trows; -- row >= 0;) {
            for (int rr = row; -- rr >= 0;) {
                pivot = T[rr,row];
                if (pivot != 0.0) {
                    for (int cc = row; cc < tcols; cc ++) {
                        T[rr,cc] -= pivot * T[row,cc];
                    }
                }
            }
        }
    }

    /**
     * @brief Generate a printable lat/lon string.
     */
    private static string LLString (double llbin)
    {
        bool neg = (llbin < 0);
        if (neg) llbin = -llbin;
        int deg = (int)llbin;
        llbin  -= deg;
        llbin  *= 60;
        int min = (int)llbin;
        llbin  -= min;
        llbin  *= 100;
        int hun = (int)llbin;
        return (neg ? "-" : "") + deg + "^" + min.ToString ().PadLeft (2, '0') + "." + 
                                              hun.ToString ().PadLeft (2, '0') + "'";
    }

    private static void Monochromaticise (Bitmap bm)
    {
        width  = bm.Width;
        height = bm.Height;
        blacks = new byte[height,width];

        for (int y = 0; y < height; y ++) {
            for (int x = 0; x < width; x ++) {
                Color c = bm.GetPixel (x, y);
                byte bl = ColorIsBlack (c);
                blacks[y,x] = bl;
                bm.SetPixel (x, y, (bl != 0) ? Color.Black : Color.White);
            }
        }
    }

    private static byte ColorIsBlack (Color c)
    {
        return (byte)(((c.A > 200) && (c.R < 100) && (c.G < 100) && (c.B < 100)) ? 1 : 0);
    }

    /**
     * @brief Expand a rectangular perimeter to enclose a blotch of black pixels.
     *        This seems to reliably enclose all characters we care about,
     *        ie, 0-9,N,W,degree,period,apostrophe.
     * @param xlef = current leftmost boundary (inclusive)
     * @param xrit = current rightmost boundary (inclusive)
     * @param ytop = current topmost boundary (inclusive)
     * @param ybot = current bottommost boundary (inclusive)
     * @returns true iff size of box is a nice size, ie, at most MAXWH x MAXWH
     */
    private static bool ExpandPerimeter (ref int xlef, ref int xrit, ref int ytop, ref int ybot)
    {
    top:

        /*
         * If anything left of left edge is black, expand left edge.
         */
        for (int yy = ytop; yy <= ybot; yy ++) {
            if ((blacks[yy,xlef] & (blacks[yy,xlef-1] | blacks[yy-1,xlef-1] | blacks[yy+1,xlef-1])) > 0) {
                xlef --;
                if ((xlef > 0) && (xrit - xlef < MAXWH)) goto top;
                return false;
            }
        }

        /*
         * If anything right of right edge is black, expand right edge.
         */
        for (int yy = ytop; yy <= ybot; yy ++) {
            if ((blacks[yy,xrit] & (blacks[yy,xrit+1] | blacks[yy-1,xrit+1] | blacks[yy+1,xrit+1])) > 0) {
                xrit ++;
                if ((xrit < width - 1) && (xrit - xlef < MAXWH)) goto top;
                return false;
            }
        }

        /*
         * If anything above top edge is black, expand top edge.
         */
        for (int xx = xlef; xx <= xrit; xx ++) {
            if ((blacks[ytop,xx] & (blacks[ytop-1,xx] | blacks[ytop-1,xx-1] | blacks[ytop-1,xx+1])) > 0) {
                ytop --;
                if ((ytop > 0) && (ybot - ytop < MAXWH)) goto top;
                return false;
            }
        }

        /*
         * If anything below bottom edge is black, expand bottom edge.
         */
        for (int xx = xlef; xx <= xrit; xx ++) {
            if ((blacks[ybot,xx] & (blacks[ybot+1,xx] | blacks[ybot+1,xx-1] | blacks[ybot+1,xx+1])) > 0) {
                ybot ++;
                if ((ybot < height - 1) && (ybot - ytop < MAXWH)) goto top;
                return false;
            }
        }

        return true;
    }


    /**
     * @brief Draw box to bitmap.
     */
    private static void DrawBox (int x1, int y1, int x2, int y2, Color c)
    {
        DrawLine (x1, y1, x2, y1, c);
        DrawLine (x2, y1, x2, y2, c);
        DrawLine (x2, y2, x1, y2, c);
        DrawLine (x1, y2, x1, y1, c);
    }

    /**
     * @brief Draw cross on image.
     */
    private static void DrawCross (int x, int y, int size, int thick, Color c)
    {
        for (int ss = -size; ss <= size; ss ++) {
            for (int tt = -thick; tt <= thick; tt ++) {
                SetPixel (x + ss, y + tt, c);
                SetPixel (x + tt, y + ss, c);
            }
        }
    }

    /**
     * @brief Draw line on image.
     */
    public static void DrawLine (int x1, int y1, int x2, int y2, Color c)
    {
        int x, y;

        if (x2 != x1) {
            if (x2 < x1) {
                x = x1;
                y = y1;
                x1 = x2;
                y1 = y2;
                x2 = x;
                y2 = y;
            }
            for (x = x1; x <= x2; x ++) {
                y = (int)((float)(y2 - y1) / (float)(x2 - x1) * (float)(x - x1)) + y1;
                SetPixel (x, y, c);
            }
        }

        if (y2 != y1) {
            if (y2 < y1) {
                y = y1;
                x = x1;
                y1 = y2;
                x1 = x2;
                y2 = y;
                x2 = x;
            }
            for (y = y1; y <= y2; y ++) {
                x = (int)((float)(x2 - x1) / (float)(y2 - y1) * (float)(y - y1)) + x1;
                SetPixel (x, y, c);
            }
        }
    }

    public static void SetPixel (int x, int y, Color c)
    {
        if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
            bmp.SetPixel (x, y, c);
        }
    }
}

/**
 * @brief Contains a string recovered from the pixels in the image pixels.
 */
public class Cluster {
    public const int WIDTH  = 125;  // widest cluster
    public const int HEIGHT =  22;  // highest cluster box
    public const int LINEFUZZ = 50; // distance bewteen string and associated lat/lon line
                                    // big number for BIX 30^24'N
    public const int MINTICKS = 3;  // sometimes numbers are near the main outline
                                    // and the margin appears similar to a tick line
                                    // except it doesn't have many ticks.  so only
                                    // accept tick lines if they have at least this
                                    // many ticks.  see KAFW diagram along the bottom,
                                    // and KTMB along right side.  but KABY only has
                                    // 4 ticks along 31^32.5'N.  and MWC has only 3 ticks
                                    // for 43^06.5'N and SPG has only 3 for 27^46.0'N.
    public const int SEMITILT = 21; // maximum semi-tilt segments we will accept
                                    // 21 for KTUL

    public bool vertical;           // string is vertical (when image portrait oriented)
    public int lox = 999999999;     // upper left corner inclusive
    public int loy = 999999999;
    public int hix = -1;            // lower right corner exclusive
    public int hiy = -1;

    public LinkedList<Deco> decos = new LinkedList<Deco> ();

    private bool latValid;
    private bool lonValid;
    private double lat;
    private double lon;
    private int cenX = -1;
    private int cenY = -1;

    public Cluster () { }

    /**
     * @brief Alternate constructor to create a cluster from a known lat/lon value.
     * @param ll = lat/lon string value
     */
    public Cluster (string ll)
    {
        foreach (char c in ll) {
            Deco deco = new Deco ();
            deco.c = c;
            decos.AddLast (deco);
        }
    }
    public void SetCenter (int cx, int cy)
    {
        cenX = cx;
        cenY = cy;
    }

    /**
     * @before Insert a character in list by ascending X value.
     * @param x = x pixel of upper left corner of character
     * @param y = y pixel of upper left corner of character
     * @param c = character to be added
     */
    public void InsertDeco (Deco deco)
    {
        if (lox > deco.x) lox = deco.x;
        if (loy > deco.y) loy = deco.y;
        if (hix < deco.x + deco.w) hix = deco.x + deco.w;
        if (hiy < deco.y + deco.h) hiy = deco.y + deco.h;

        for (LinkedListNode<Deco> ptr = decos.First; ptr != null; ptr = ptr.Next) {
            if (deco.x < ptr.Value.x) {
                decos.AddBefore (ptr, deco);
                return;
            }
        }
        decos.AddLast (deco);
    }

    /**
     * @brief Get resultant string.
     */
    public string Result {
        get {
            char[] chars = new char[decos.Count];
            int i = 0;
            foreach (Deco deco in decos) {
                if ((deco.c == '\'') && (i > 0) && (chars[i-1] == '\'')) {
                    chars[i-1] = '"';
                } else {
                    chars[i++] = deco.c;
                }
            }
            string r = new String (chars, 0, i);
            string g;
            if (ReadArptDgmPng.badStrings.TryGetValue (r, out g)) {
                if (ReadArptDgmPng.verbose) Console.WriteLine ("replacing bad string " + r + " with " + g);
                r = g;
            }
            return r;
        }
    }

    /**
     * @brief Get decoded character 'i'
     */
    public Deco GetDeco (int i)
    {
        foreach (Deco deco in decos) {
            if (-- i < 0) return deco;
        }
        return null;
    }

    /**
     * @brief Determine if this cluster is a valid lat or lon string.
     *        digits ^ digits [ . digits ] ' [ digits " ] { N | S | E | W }
     */
    public bool IsLatLon {
        get {

            // get the string as we have it
            // if the last char is not decoded, assume it is an N
            // replace all other undecoded chars with 0 as they must be digits
            string r = Result;
            if (r.EndsWith ("?")) {
                r = r.Substring (0, r.Length - 1) + "N";
            }
            r = r.Replace ('?', '0');

            // append null terminator so we don't have to keep checking subscripts
            r += (char)0;

            // must start with a digit
            int i = 0;
            char c = r[i++];
            if ((c < '0') || (c > '9')) return false;

            // skip other digits
            do c = r[i++];
            while ((c >= '0') && (c <= '9'));

            // must have a ^ next
            if (c != '^') return false;

            // must have at least one digit after ^
            // then skip them all
            c = r[i++];
            if ((c < '0') || (c > '9')) return false;
            do c = r[i++];
            while ((c >= '0') && (c <= '9'));

            // can be optional . followed by digits
            if (c == '.') {
                do c = r[i++];
                while ((c >= '0') && (c <= '9'));
            }

            // must have a ' next
            if (c != '\'') return false;

            // ' optionally followed by digits
            c = r[i++];
            if ((c >= '0') && (c <= '9')) {
                do c = r[i++];
                while ((c >= '0') && (c <= '9'));

                // optional digits must be followed by "
                // though we sometimes misread as ' or ^
                while ((c != 'N') && (c != 'S') && (c != 'E') && (c != 'W')) {
                    if ((c != '"') && (c != '\'') && (c != '^')) return false;
                    c = r[i++];
                }
            }

            // finally comes the N,S,E or W
            if ((c != 'N')  && (c != 'S') && (c != 'E') && (c != 'W')) return false;

            // make sure it's the end of the string
            return r[i] == (char)0;
        }
    }

    /**
     * @brief Decode the string to get the longitude.
     * @returns 0: it's a latitude
     *       else: longitude in degrees
     */
    public double Longitude {
        get {
            if (!lonValid) {
                lon = LatLon ('E', 'W');
                lonValid = true;
            }
            return lon;
        }
    }

    /**
     * @brief Decode the string to get the latitude
     * @returns 0: it's a longitude
     *       else: latitude in degrees
     */
    public double Latitude {
        get {
            if (!latValid) {
                lat = LatLon ('N', 'S');
                latValid = true;
            }
            return lat;
        }
    }

    /**
     * @brief Parse the coordinate string into corresponding integer.
     * @param pos = final char for positive values (N or E)
     * @param neg = final char for negative values (S or W)
     * @returns 0: not valid
     *       else: degrees
     */
    private double LatLon (char pos, char neg)
    {
        /*
         * Make sure in correct format for easy decoding.
         */
        if (!IsLatLon) return 0;

        /*
         * Check for matching N,S,E or W on the end.
         */
        string r = Result;
        char end = r[r.Length-1];
        if ((end != 'N') && (end != 'S') && (end != 'E') && (end != 'W')) {
            throw new Exception (r + " doesn't end in N, S, E or W");
        }
        if ((end != pos) && (end != neg)) return 0;

        /*
         * Degrees is everything up to the mandatory ^.
         */
        int i = r.IndexOf ('^');
        int deg = int.Parse (r.Substring (0, i ++));

        /*
         * Minutes is everything up to the mandatory '.
         * Might include a decimal point followed by digits.
         */
        int j = r.IndexOf ('\'');
        string minstr = r.Substring (i, j ++ - i);
        double min = double.Parse (minstr);
        if (minstr.EndsWith (".30")) {
            // KASE 106^51.30'W
            if (ReadArptDgmPng.verbose) Console.WriteLine ("assuming .30 in " + r + " is really .5");
            min += 0.20;
        }

        /*
         * Seconds is optional if digits present following the '.
         */
        char secdig;
        int k = j;
        while (((secdig = r[k]) >= '0') && (secdig <= '9')) k ++;
        int sec = 0;
        if (k > j) {
            sec = int.Parse (r.Substring (j, k - j));
        }

        /*
         * Range check for sanity and return resultant degrees.
         */
        if ((deg >= 180) || (min >= 60) || (sec >= 60)) {
            throw new Exception (r + " degrees, minutes or seconds out of range");
        }

        /*
         * Maybe apply negative sign (S or W).
         */
        double value = (double)deg + min / 60 + (double)sec / 3600;
        if (end == neg) value = -value;
        return value;
    }

    /**
     * @brief Find pixel where the most vertical lines with tick marks are.
     *        If character string is horizontal, look for vertical line
     *          within limits of the string itself.
     *        If character string is vertical, look for vertical line from
     *          up to LINEFUZZ pixels to the left of the string (lox-LINEFUZZ)
     *          and LINEFUZZ pixels to the right limit of the string (hiy+LINEFUZZ).
     * @returns 0: there is no valid vertical line associated with this string
     *       else: X-pixel number of vertical line associated with this string
     */
    public int CenterX {
        get {
            if (cenX < 0) {

                /*
                 * First look for a single long line that contains at
                 * least MINTICKS tick marks on it.
                 */
                int bestX = 0;
                int bestCount = MINTICKS - 1;
                int inix = lox;
                int limx = hix;
                if (vertical) {
                    inix -= LINEFUZZ;
                    limx += LINEFUZZ;
                }
                if (inix < 0) inix = 0;
                if (limx >= ReadArptDgmPng.width) limx = ReadArptDgmPng.width - 1;
                for (int x = inix; x <= limx; x ++) {
                    int llv = ReadArptDgmPng.longlenverts[x];
                    if (llv > bestCount) {
                        bestX = x;
                        bestCount = llv;
                    }
                }
                if (bestX == 0) {

                    /*
                     * Didn't find one, look for a bunch of closely spaced
                     * long lines that have a total of MINTICKS marks on
                     * them.  These are from the chart being just slightly
                     * tilted.
                     */
                    bestCount  = 0;
                    int startX = 0;
                    int stopX  = 0;
                    for (int x = inix; x <= limx; x ++) {
                        int llv = ReadArptDgmPng.longlenverts[x];
                        if (llv > 0) {
                            if (startX == 0) startX = x;
                            stopX = x;
                            bestCount += llv;
                        }
                    }
                    if ((bestCount >= MINTICKS) && (stopX - startX <= SEMITILT)) {
                        bestX = (startX + stopX) / 2;
                    }
                }

                /*
                 * Cache what we found.
                 */
                cenX = bestX;
            }
            return cenX;
        }
    }

    /**
     * @brief Find pixel where the most horizontal boxes are.
     *        If character string is vertical, look for horizontal
     *          line within limits of the string itself.
     *        If character string is horizontal, look for horizontal
     *          line from up to LINEFUZZ pixels above the string
     *          (loy-LINEFUZZ) to LINEFUZZ pixels below the bottom
     *          of the string (hiy+LINEFUZZ).
     * @returns 0: there is no valid horizontal line associated with this string
     *       else: Y-pixel number of horizontal line associated with this string
     */
    public int CenterY {
        get {
            if (cenY < 0) {

                /*
                 * First look for a single long line that contains at
                 * least MINTICKS tick marks on it.
                 */
                int bestY = 0;
                int bestCount = MINTICKS - 1;
                int iniy = loy;
                int limy = hiy;
                if (!vertical) {
                    iniy -= LINEFUZZ;
                    limy += LINEFUZZ;
                }
                if (iniy < 0) iniy = 0;
                if (limy >= ReadArptDgmPng.height) limy = ReadArptDgmPng.height - 1;
                for (int y = iniy; y <= limy; y ++) {
                    int llv = ReadArptDgmPng.longlenhorzs[y];
                    if (llv > bestCount) {
                        bestY = y;
                        bestCount = llv;
                    }
                }
                if (bestY == 0) {

                    /*
                     * Didn't find one, look for a bunch of closely spaced
                     * long lines that have a total of MINTICKS marks on
                     * them.  These are from the chart being just slightly
                     * tilted.
                     */
                    bestCount  = 0;
                    int startY = 0;
                    int stopY  = 0;
                    for (int y = iniy; y <= limy; y ++) {
                        int llv = ReadArptDgmPng.longlenhorzs[y];
                        if (llv > 0) {
                            if (startY == 0) startY = y;
                            stopY = y;
                            bestCount += llv;
                        }
                    }
                    if ((bestCount >= MINTICKS) && (stopY - startY <= SEMITILT)) {
                        bestY = (startY + stopY) / 2;
                    }
                }

                /*
                 * Cache what we found.
                 */
                cenY = bestY;
            }
            return cenY;
        }
    }

    /**
     * @brief The image orientation is now known,
     *        fix any strings that are near intersections
     *        and think they are both on an X and a Y line.
     * @param landscape = false: latitudes only have horizontal lines; longitudes only have vertical lines
     *                     true: latitudes only have vertical lines; longitudes only have horizontal lines
     */
    public void SetOrientation (bool landscape)
    {
        // if portrait orientation and it's a latitude number, it has no vertical line in original image
        if (!landscape && (Latitude  != 0)) cenX = 0;

        // if portrait orientation and it's a longitude number, it has no horizontal line in original image
        if (!landscape && (Longitude != 0)) cenY = 0;

        // if landscape orientation and it's a latitude number, it has no horizontal line in original image
        if (landscape  && (Latitude  != 0)) cenY = 0;

        // if landscape orientation and it's a longitude number, it has no vertical line in original image
        if (landscape  && (Longitude != 0)) cenX = 0;
    }

    public void ZeroCenterX ()
    {
        cenX = 0;
    }
    public void ZeroCenterY ()
    {
        cenY = 0;
    }
}

/**
 * @brief A decoded character from the image and where it was found.
 */
public class Deco {
    public const int MAXWH = 30;

    public int x;       // upper left corner x
    public int y;       // upper left corner y
    public int w;       // number of pixels wide (exclusive)
    public int h;       // number of pixels high (exclusive)
    public char c;      // resultant character
    public byte[,] grays;
}

/**
 * @brief Character string given on the command line and
 *        approximately where it should be found in image.
 */
public class Given {
    public bool found;
    public int x;
    public int y;
    public string s;

    public Given (int xx, int yy, string ss)
    {
        x = xx;
        y = yy;
        s = ss;
    }
}
