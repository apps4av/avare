//+++2015-10-26
//    Copyright (C) 2015, Mike Rieker, Beverly, MA USA
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
//---2015-10-26

/**
 * Decode lat/lon of an approach plate and generate georef info.
 *
 *  export CLASSPATH=pdfbox-1.8.10.jar:commons-logging-1.2.jar
 *
 *  javac -Xlint:deprecation DecodePlate.java Lib.java
 *
 *  exec java DecodePlate [ BVY 'IAP-RNAV (GPS) RWY 27' ]
 *      [ -csvout bvygps27.csv ]
 *      [ -markedpng bvygps27.png ]
 *      [ -rejects bvygps27.rej ]
 *      [ -verbose ]
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageNode;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.PDFOperator;

public class DecodePlate {
    private final static int pdfDpi =  72;  // PDFs are defined with 72dpi units
    private final static int csvDpi = 300;  // database is set up for 300dpi pixels
    private final static double maxFixDistNM = 75;
    private final static char dmeMark = '[';

    private final static double K_APPCRSSTRANG = Math.toRadians (9.0);
    private final static double K_LITNBOLTEND2END = Math.toRadians (10.0);
    private final static double K_LITNBOLTFOLDMIN = Math.toRadians (140.0);
    private final static double K_LITNBOLTFOLDMAX = Math.toRadians (175.0);
    private final static double K_MAXRWYCLANG = Math.toRadians (3.5);
    private final static double K_MAXRWYCWANG = Math.toRadians (6.5);
    private final static double K_MINRWYCLLEN = csvDpi / 10.0;
    private final static double K_STEPDOWNSLOP = csvDpi / 50.0;
    private final static double K_STRVERTCOMBINE = 0.67;
    private final static double K_TEXTJOINANGLE = Math.toRadians (4.0);
    private final static double K_TILTEDTEXT = Math.toRadians (2.0);
    private final static int K_HVLINEGAP = 2;
    private final static int K_ONEFIXSCALE = 500000;

    private static HashMap<String,String> brokenStrings = GetBrokenStrings ();
    private static HashMap<String,String> brokenWords = GetBrokenWords ();
    private static HashSet<String> ignorePlateFixes = GetIgnorePlateFixes ();

    private static Airport airport;
    private static boolean verbose;
    private static HashMap<String,Airport> allAirports = new HashMap<> ();
    private static HashMap<String,DBFix> nearDBFixes = new HashMap<> ();
    private static LinkedList<DBFix> allDBFixes = new LinkedList<> ();
    private static PrintWriter csvoutfile;
    private static PrintWriter rejectsfile;
    private static Runway runway;
    private static String basedir;
    private static String csvoutname;
    private static String cycles28expdate;
    private static String cycles56expdate;
    private static String faaid;
    private static String markedpngname;
    private static String pdfName;
    private static String plateid;
    private static String rejectsname;

    // strings we don't read correctly
    private static HashMap<String,String> GetBrokenStrings ()
    {
        HashMap<String,String> bs = new HashMap<> ();
        bs.put ("FSM:110.4", "110.4 FSM");              // FSM 'IAP-VOR OR TACAN RWY 25'
        bs.put ("FNL:I -FNL", "I-FNL");                 // FNL 'IAP-ILS OR LOC RWY 33'
        bs.put ("MAJ:316   MA         ", "316 MAJ");    // MAJ 'IAP-NDB RWY 25'
        bs.put ("SCX:108.4LV  ", "108.4 LVT");          // SCX 'IAP-VORDME-A'
        return bs;
    }

    private static HashMap<String,String> GetBrokenWords ()
    {
        HashMap<String,String> bw = new HashMap<> ();
        bw.put ("CCY:417 IYY",        "417 IY");            // CCY 'IAP-NDB RWY 12'
        bw.put ("LAA:IAF LAMAR",      "116.9 LAA");         // LAA 'IAP-VOR RWY 18'
        bw.put ("LWV:108.8 LW",       "108.8 LWV");         // LWV 'IAP-VOR RWY 36'
        bw.put ("MQW:IAF MC RAE MQW", "mc rae 280 MQW");    // MQW 'IAP-NDB RWY 21'
        bw.put ("RZT:IAF YELLOW BUD", "iaf yellow bud");    // RZT 'IAP-VOR RWY 23'
        bw.put ("WYS:TARGY",          "TARGY 415 LO");      // WYS 'IAP-NDB RWY 01'
        return bw;
    }

    // platefixes we don't locate correctly
    private static HashSet<String> GetIgnorePlateFixes ()
    {
        HashSet<String> ipf = new HashSet<String> ();
        ipf.add ("ARR:IAP-VOR RWY 33:DPA");             // DPA needs a fence - not to scale
        ipf.add ("BOI:IAP-ILS OR LOC RWY 10R:SALLA");   // SALLA needs a fence - not to scale
        ipf.add ("DCU:IAP-VOR RWY 18:MASHA");           // MASHA needs a fence - not to scale
        ipf.add ("EWR:IAP-VORDME RWY 22L:PATRN");       // PATRN needs a fence - not to scale
        ipf.add ("GVT:IAP-RNAV (GPS) RWY 35:ROCKK");    // ROCKK needs a fence - not to scale
        ipf.add ("GVT:IAP-RNAV (GPS) RWY 35:SLR");      // SLR needs a fence - not to scale
        ipf.add ("GYI:IAP-NDB RWY 17L:ADM");            // ADM needs a fence - not to scale
        ipf.add ("INW:IAP-VOR OR GPS RWY 11:RW11");     // RW11 pointer/marker confusing
        ipf.add ("LAX:IAP-ILS OR LOC RWY 25R:SEAVU");   // RIIVR fix misread as SEAVU; text sizing problem
        ipf.add ("PHX:IAP-ILS OR LOC RWY 07R:ALLIS");   // ALLIS fenced off crooked
        ipf.add ("MGW:IAP-VOR-A:TEDDS");                // TEDDS missed approach not boxed off
        ipf.add ("MIA:IAP-RNAV (GPS) Z RWY 30:BRBRA");  // BRBRA missed approach box badly drawn
        ipf.add ("OUN:IAP-LOC RWY 03:SOONR");           // SOONR OM/INT eye-shaped marker we don't do
        ipf.add ("PHF:IAP-RNAV (GPS) RWY 25:JAWES");    // JAMES needs a fence - not to scale
        ipf.add ("TOP:IAP-VOR RWY 22:LEBVY");           // LEBVY confusing mark
        ipf.add ("TTN:IAP-RNAV (GPS) RWY 16:SBJ");      // SBJ needs a fence - not to scale
        ipf.add ("TTN:IAP-RNAV (GPS) RWY 16:ZUVIV");    // ZUVIV needs a fence - not to scale
        return ipf;
    }

    public static void main (String[] args)
    {
        try {
            basedir = new File (DecodePlate.class.getProtectionDomain ().getCodeSource ().getLocation ().toURI ()).getParent ().toString ();

            // Decode command line

            for (int i = 0; i < args.length;) {
                String arg = args[i++];
                if (arg.startsWith ("-")) {
                    if (arg.equals ("-csvout") && (i < args.length)) {
                        csvoutname = args[i++];
                        continue;
                    }
                    if (arg.equals ("-markedpng") && (i < args.length)) {
                        markedpngname = args[i++];
                        continue;
                    }
                    if (arg.equals ("-rejects") && (i < args.length)) {
                        rejectsname = args[i++];
                        continue;
                    }
                    if (arg.equals ("-verbose")) {
                        verbose = true;
                        continue;
                    }
                    System.err.println ("unknown option " + arg);
                    System.exit (1);
                }
                if (faaid == null) {
                    faaid = arg;
                    continue;
                }
                if (plateid == null) {
                    plateid = arg;
                    continue;
                }
                System.err.println ("unknown parameter " + arg);
                System.exit (1);
            }

            // Read expiration dates so we know what cycle we are dealing with

            BufferedReader br1 = new BufferedReader (new FileReader (basedir + "/datums/aptplates_expdate.dat"), 256);
            cycles28expdate = br1.readLine ();
            br1.close ();

            BufferedReader br3 = new BufferedReader (new FileReader (basedir + "/datums/aptinfo_expdate.dat"), 256);
            cycles56expdate = br3.readLine ();
            br3.close ();

            // Read in airports to get their lat/lons.
            // KBVY,BVY,107.3,"BEVERLY MUNI",42.5841410277778,-70.9161444166667,16,...

            BufferedReader br4 = new BufferedReader (new FileReader (basedir + "/datums/airports_" + cycles56expdate + ".csv"), 4096);
            String line;
            while ((line = br4.readLine ()) != null) {
                String[] csvs = Lib.QuotedCSVSplit (line);
                Airport apt = new Airport ();
                apt.name    = csvs[0];  // icaoid
                apt.faaid   = csvs[1];
                apt.lat     = Double.parseDouble (csvs[4]);
                apt.lon     = Double.parseDouble (csvs[5]);
                apt.magvar  = Double.parseDouble (csvs[6]);
                allAirports.put (apt.faaid, apt);
            }
            br4.close ();

            // Read in fixes to get their lat/lons.

            BufferedReader br5 = new BufferedReader (new FileReader (basedir + "/datums/fixes_" + cycles56expdate + ".csv"), 4096);
            while ((line = br5.readLine ()) != null) {
                String[] csvs = Lib.QuotedCSVSplit (line);
                DBFix dbfix = new DBFix ();
                dbfix.name  = csvs[0];
                dbfix.type  = csvs[4];
                dbfix.lat   = Double.parseDouble (csvs[1]);
                dbfix.lon   = Double.parseDouble (csvs[2]);
                allDBFixes.addLast (dbfix);
            }
            br5.close ();

            // Read in localizers to get their lat/lons.

            BufferedReader br10 = new BufferedReader (new FileReader (basedir + "/datums/localizers_" + cycles56expdate + ".csv"), 4096);
            while ((line = br10.readLine ()) != null) {
                String[] csvs = Lib.QuotedCSVSplit (line);
                DBFix dbfix = new DBFix ();
                dbfix.name  = csvs[1];  // eg, "I-BVY"
                dbfix.type  = csvs[0];  // eg, "ILS/DME"
                dbfix.lat   = Double.parseDouble (csvs[4]);
                dbfix.lon   = Double.parseDouble (csvs[5]);
                allDBFixes.addLast (dbfix);
            }
            br10.close ();

            // Read in navaids to get their lat/lons.

            BufferedReader br6 = new BufferedReader (new FileReader (basedir + "/datums/navaids_" + cycles56expdate + ".csv"), 4096);
            while ((line = br6.readLine ()) != null) {
                String[] csvs = Lib.QuotedCSVSplit (line);
                DBFix dbfix = new DBFix ();
                dbfix.name  = csvs[1];
                dbfix.type  = csvs[0];
                dbfix.lat   = Double.parseDouble (csvs[4]);
                dbfix.lon   = Double.parseDouble (csvs[5]);
                if (!csvs[6].equals ("")) dbfix.magvar = Double.parseDouble (csvs[6]);
                if (dbfix.name.equals ("OSH") && (dbfix.magvar < 0)) dbfix.magvar = - dbfix.magvar;
                allDBFixes.addLast (dbfix);
            }
            br6.close ();

            // Read in runways to get their lat/lons.

            BufferedReader br8 = new BufferedReader (new FileReader (basedir + "/datums/runways_" + cycles56expdate + ".csv"), 4096);
            while ((line = br8.readLine ()) != null) {
                String[] csvs = Lib.QuotedCSVSplit (line);
                String faaid  = csvs[0];
                Airport apt   = allAirports.get (faaid);
                apt.addRunway ("RW" + csvs[1],  // eg, "RW04L" for plate fix name
                        Double.parseDouble (csvs[4]),
                        Double.parseDouble (csvs[5]),
                        Double.parseDouble (csvs[6]),
                        Double.parseDouble (csvs[7]));
            }
            br8.close ();

            // If plate given on command line, process just that one plate

            if ((faaid != null) && (plateid != null)) {
                File[] stateFiles = new File (basedir + "/datums/aptplates_" + cycles28expdate + "/state").listFiles ();
                for (File stateFile : stateFiles) {
                    String statePath = stateFile.getPath ();
                    if (statePath.endsWith (".csv")) {
                        BufferedReader br2 = new BufferedReader (new FileReader (stateFile), 4096);
                        while ((line = br2.readLine ()) != null) {
                            String[] csvs = Lib.QuotedCSVSplit (line);
                            if (csvs[0].equals (faaid) && csvs[1].equals (plateid)) {
                                br2.close ();
                                airport = allAirports.get (faaid);
                                if (airport == null) throw new Exception ("airport " + faaid + " not found");
                                ProcessPlate (csvs[2]);
                                if (csvoutfile != null) csvoutfile.close ();
                                if (rejectsfile != null) rejectsfile.close ();
                                return;
                            }
                        }
                        br2.close ();
                    }
                }
                throw new Exception ("plate not found");
            }

            // Otherwise, read state file csv lines from stdin and process all those plates

            if ((faaid == null) && (plateid == null)) {

                // see what plates have already been done
                // then set up to append onto the file
                HashMap<String,Boolean> existingDecodes = new HashMap<> ();
                try {
                    BufferedReader br9 = new BufferedReader (new FileReader (csvoutname), 4096);
                    while ((line = br9.readLine ()) != null) {
                        String[] csvs = Lib.QuotedCSVSplit (line);
                        String key = csvs[0] + ":" + csvs[1];
                        existingDecodes.put (key, true);
                    }
                    br9.close ();
                    csvoutfile = new PrintWriter (new FileOutputStream (csvoutname, true));
                } catch (FileNotFoundException fnfe) {
                }
                try {
                    BufferedReader br9 = new BufferedReader (new FileReader (rejectsname), 4096);
                    while ((line = br9.readLine ()) != null) {
                        String[] csvs = Lib.QuotedCSVSplit (line);
                        String key = csvs[0] + ":" + csvs[1];
                        existingDecodes.put (key, true);
                    }
                    br9.close ();
                    rejectsfile = new PrintWriter (new FileOutputStream (rejectsname, true));
                } catch (FileNotFoundException fnfe) {
                }

                // process plates given in stdin that aren't already in the csvoutfile
                BufferedReader br7 = new BufferedReader (new InputStreamReader (System.in), 4096);
                while ((line = br7.readLine ()) != null) {
                    String[] csvs = Lib.QuotedCSVSplit (line);
                    faaid   = csvs[0];
                    plateid = csvs[1];
                    airport = allAirports.get (faaid);
                    if (airport == null) throw new Exception ("airport " + faaid + " not found");
                    String key = airport.name + ":" + plateid;
                    if (!existingDecodes.containsKey (key)) {
                        existingDecodes.put (key, true);
                        if (verbose) System.out.println ("");
                        System.out.print ("---------------- " + faaid + " " + plateid + "\n");
                        long started = System.nanoTime ();
                        ProcessPlate (csvs[2]);
                        long finished = System.nanoTime ();
                        if (verbose) System.out.println ("---------------- " + ((finished - started + 500000) / 1000000) + " ms");
                    }
                }
                br7.close ();
                if (csvoutfile != null) csvoutfile.close ();
                if (rejectsfile != null) rejectsfile.close ();
                return;
            }

            System.err.println ("missing required parameters");
            System.exit (1);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (1);
        }
    }

    public static void ProcessPlate (String gifName) throws Exception
    {
        // Get PDF file name from <state>.csv
        //   datums/aptplates_20150917/state
        //   BVY,"IAP-RNAV (GPS) RWY 27",gif_150/050/39r27.gif

        if (!gifName.startsWith ("gif_") || !gifName.endsWith (".gif")) {
            throw new Exception ("bad gif name " + gifName);
        }
        pdfName = basedir + "/datums/aptplates_" + cycles28expdate + "/pdftemp" +
                gifName.substring (gifName.indexOf ('/'), gifName.length () - 4) + ".pdf";

        // Try to get runway

        runway = null;
        int i = plateid.indexOf (" RWY ");
        if (i >= 0) {
            String rwid = "RW" + plateid.substring (i + 5);
            runway = airport.runways.get (rwid);
        }

        // Process the plate

        ProcessPlateWork ();
    }

    // Input:
    //   airport = airport the approach is for
    //   runway = runway the approach is for (or null if unknown)
    //   plateid = which plate is being processed (for messages and csv/rej file record)
    //   pdfName = name of PDF file as downloaded from FAA
    //   allDBFixes = all fixes from FAA databases (fixes, navaids, etc)
    //   markedpngname = where to write marked-up png to (debugging)
    //   csvoutname = where to write .csv info to (if successful)
    //   rejectsname = where to write .rej info to (if unable to resolve)
    // Output:
    //   writes csvoutname, rejectsname and markedpngname files
    public static void ProcessPlateWork () throws Exception
    {
        // Filter out fixes more than 50nm away from airport, no chart goes that far.
        // This helps us from trying to decode spurious strings as fix names and
        // helps us avoid duplicate name problems.

        nearDBFixes.clear ();
        for (DBFix dbfix : allDBFixes) {
            if (Lib.LatLonDist (dbfix.lat, dbfix.lon, airport.lat, airport.lon) <= maxFixDistNM) {
                dbfix.mentioned = false;
                nearDBFixes.put (dbfix.name, dbfix);
            }
        }

        // Also add in runways as fixes cuz some plates use them for fixes.

        for (Runway rwy : airport.runways.values ()) {
            nearDBFixes.put (rwy.name, rwy);
        }

        // Open PDF and scan it.

        PDDocument pddoc = PDDocument.load (pdfName);
        PDDocumentCatalog doccat = pddoc.getDocumentCatalog ();
        PDPageNode pages = doccat.getPages ();
        List kids = new LinkedList ();
        pages.getAllKids (kids);
        if (kids.size () != 1) throw new Exception ("pdf not a single Page");
        Object kid    = kids.get (0);
        PDPage page   = (PDPage) kid;
        int imgWidth  = (int) (page.getMediaBox ().getWidth  () / pdfDpi * csvDpi + 0.5F);
        int imgHeight = (int) (page.getMediaBox ().getHeight () / pdfDpi * csvDpi + 0.5F);
        BufferedImage bi = new BufferedImage (imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics ();
        PagePanel pagepanel = new PagePanel (page);
        pagepanel.paintComponent (g2d);
        pagepanel.resolveFixes (g2d);
        if (markedpngname != null) {
            if (!ImageIO.write (bi, "png", new File (markedpngname))) {
                throw new IOException ("ImageIO.write(" + markedpngname + ") failed");
            }
        }
        pddoc.close ();
    }

    /**
     * Display the given PDF page in a panel.
     */
    private static class PagePanel {
        private final static String[] marker_types_bez16    = new String[] { "GPS-WP", "MIL-REP-PT", "REP-PT", "RNAV-WP", "WAYPOINT" };
        private final static String[] marker_types_ndbdots  = new String[] { "MIL-REP-PT", "NDB", "NDB/DME", "OM", "REP-PT", "RNAV-WP" };
        private final static String[] marker_types_stepdown = new String[] { "CNF", "MIL-REP-PT", "RADAR", "REP-PT", "RNAV-WP", "dmefix" };

        private final static byte HVLB_UNKN = 0;
        private final static byte HVLB_DRWN = 1;
        private final static byte HVLB_STKD = 2;
        private final static byte HVLB_GOOD = 3;
        private final static byte HVLB_RING = 4;
        private byte[] hvLineBytes;
        private HashSet<Integer> horizLineList = new HashSet<> ();
        private HashSet<Integer> vertLineList  = new HashSet<> ();

        private boolean appCrsFound;
        private boolean isBlack;
        private boolean isGray;
        private double appCrsMag;
        private double appCrsTrue;
        private Graphics2D g2d;
        private HashMap<Integer,SegNode>        segmentNodes     = new HashMap<> ();
        private HashSet<Long>                   bezierSegEdges   = new HashSet<> ();
        private int panelHeight, panelWidth;
        private LineSeg                         rwyCenterline;
        private LinkedList<Bez16>               bigBez16s        = new LinkedList<> ();
        private LinkedList<Bez16>               lilBez16s        = new LinkedList<> ();
        private LinkedList<FencedPlateFix>      fencedPlateFixes = new LinkedList<> ();
        private LinkedList<LineSeg>             allLineSegs      = new LinkedList<> ();
        private LinkedList<LineSeg>             litnBolts;
        private LinkedList<LineSeg>             rwyCrossedSegs   = new LinkedList<> ();
        private LinkedList<LineSeg>             smallBeziers     = new LinkedList<> ();
        private LinkedList<LineSeg>             smallLineSegs    = new LinkedList<> ();
        private LinkedList<LinkedList<SegNode>> closedLoops      = new LinkedList<> ();
        private LinkedList<LinkedList<SegNode>> openLoops        = new LinkedList<> ();
        private LinkedList<Marker>              markers          = new LinkedList<> ();
        private LinkedList<Marker>              ndbCenters       = new LinkedList<> ();
        private LinkedList<PlateFix>            allPlateFixes    = new LinkedList<> ();
        private LinkedList<PlateFix>            foundDMEFixes    = new LinkedList<> ();
        private LinkedList<PlateFix>            foundPlateFixes  = new LinkedList<> ();
        private LinkedList<PlateFix>            livePlateFixes;
        private LinkedList<Point>               ndbDots          = new LinkedList<> ();
        private LinkedList<TextString>          appCrsStrings    = new LinkedList<> ();
        private LinkedList<TextString>          textStrings      = new LinkedList<> ();
        private List<Object>                    tokens;
        private String appCrsStr;

        public PagePanel (PDPage page) throws IOException
        {
            // calculate size of panel to hold the PDF page and tell scrollpanel what that size is
            panelWidth  = (int) (page.getMediaBox ().getWidth  () / pdfDpi * csvDpi + 0.5F);
            panelHeight = (int) (page.getMediaBox ().getHeight () / pdfDpi * csvDpi + 0.5F);
            System.out.println ("panel size " + panelWidth + " x " + panelHeight + " = " + ((float) panelWidth / csvDpi) + "\" x " + ((float) panelHeight / csvDpi) + "\"");

            // set up a bitmap to capture all the large horizontal and vertical lines
            hvLineBytes = new byte[panelHeight*panelWidth];

            // point to token stream in PDF file
            PDStream pdstream = page.getContents ();
            COSStream cosstream = pdstream.getStream ();
            tokens = cosstream.getStreamTokens ();
        }

        public void paintComponent (Graphics g)
        {
            g2d = (Graphics2D) g;

            g2d.setColor (Color.WHITE);
            g2d.fillRect (0, 0, panelWidth, panelHeight);

            allPlateFixes.clear ();
            Stack<COSBase> objstack = new Stack<> ();
            Stack<GraphicsState> gsstack = new Stack<> ();
            GraphicsState gscur = new GraphicsState ();
            gscur.ctm_a =   (float) csvDpi / pdfDpi;
            gscur.ctm_d = - (float) csvDpi / pdfDpi;
            gscur.ctm_f = panelHeight;

            isBlack   = true;
            isGray    = true;
            Path path = null;

            Font originalFont = g2d.getFont ();
            float fontScale = 0.0F;
            float textLeading = 1.0F;
            float textLineMatrix_a = 0;
            float textLineMatrix_b = 0;
            float textLineMatrix_c = 0;
            float textLineMatrix_d = 0;
            float textLineMatrix_e = 0;
            float textLineMatrix_f = 0;
            float textMatrix_a = 0;
            float textMatrix_b = 0;
            float textMatrix_c = 0;
            float textMatrix_d = 0;
            float textMatrix_e = 0;
            float textMatrix_f = 0;
            int textRenderMode = 0;
            PDFont fontInPDF = null;

            // PDF Reference, Third Edition p.701 has a table of the opcodes

            for (Object token : tokens) {
                if (token instanceof PDFOperator) {
                    String code = ((PDFOperator) token).getOperation ();
                    switch (code) {

                        // GRAPHICS STATE

                        case "q": {
                            gsstack.push (gscur.clone ());
                            break;
                        }

                        case "Q": {
                            gscur = gsstack.pop ();
                            break;
                        }

                        case "cm": {
                            float df = GetFloatValue (objstack.pop ());
                            float de = GetFloatValue (objstack.pop ());
                            float dd = GetFloatValue (objstack.pop ());
                            float dc = GetFloatValue (objstack.pop ());
                            float db = GetFloatValue (objstack.pop ());
                            float da = GetFloatValue (objstack.pop ());
                            gscur.concat (da, db, dc, dd, de, df);
                            break;
                        }

                        case "w": {
                            gscur.lineWidth = GetFloatValue (objstack.pop ());
                            break;
                        }

                        case "j": {
                            gscur.lineCapStyle = GetIntegerValue (objstack.pop ());
                            break;
                        }

                        case "J": {
                            gscur.lineJoinStyle = GetIntegerValue (objstack.pop ());
                            break;
                        }

                        case "M": {
                            gscur.miterLimit = GetFloatValue (objstack.pop ());
                            break;
                        }

                        // LINE DRAWING

                        case "m": {
                            float y = GetFloatValue (objstack.pop ());
                            float x = GetFloatValue (objstack.pop ());
                            if (path == null) path = new Path ();
                            if (!isGray) break;
                            PathOpMove po = new PathOpMove ();
                            path.movex = po.x = gscur.devx (x, y);
                            path.movey = po.y = gscur.devy (x, y);
                            path.pathops.addLast (po);
                            break;
                        }

                        case "l": {
                            float y = GetFloatValue (objstack.pop ());
                            float x = GetFloatValue (objstack.pop ());
                            if (!isGray) break;
                            PathOpLine po = new PathOpLine ();
                            po.x = gscur.devx (x, y);
                            po.y = gscur.devy (x, y);
                            path.pathops.addLast (po);
                            break;
                        }

                        case "h": {
                            if (!isGray) break;
                            PathOpLine po = new PathOpLine ();
                            po.x = path.movex;
                            po.y = path.movey;
                            path.pathops.addLast (po);
                            break;
                        }

                        case "n": {
                            path = null;
                            break;
                        }

                        case "re": {
                            float h = GetFloatValue (objstack.pop ());
                            float w = GetFloatValue (objstack.pop ());
                            float y = GetFloatValue (objstack.pop ());
                            float x = GetFloatValue (objstack.pop ());
                            if (!isGray) break;
                            if (path == null) path = new Path ();
                            PathOpRect po = new PathOpRect ();
                            po.x = gscur.devx (x, y);
                            po.y = gscur.devy (x, y);
                            x += w;
                            y += h;
                            po.u = gscur.devx (x, y);
                            po.v = gscur.devy (x, y);
                            path.pathops.addLast (po);
                            break;
                        }

                        case "c": {
                            float y3 = GetFloatValue (objstack.pop ());
                            float x3 = GetFloatValue (objstack.pop ());
                            float y2 = GetFloatValue (objstack.pop ());
                            float x2 = GetFloatValue (objstack.pop ());
                            float y1 = GetFloatValue (objstack.pop ());
                            float x1 = GetFloatValue (objstack.pop ());
                            if (!isGray) break;
                            PathOpBezier po = new PathOpBezier ();
                            po.x1 = gscur.devx (x1, y1);
                            po.y1 = gscur.devy (x1, y1);
                            po.x2 = gscur.devx (x2, y2);
                            po.y2 = gscur.devy (x2, y2);
                            po.x3 = gscur.devx (x3, y3);
                            po.y3 = gscur.devy (x3, y3);
                            path.pathops.addLast (po);
                            break;
                        }
                        case "v": {
                            float y3 = GetFloatValue (objstack.pop ());
                            float x3 = GetFloatValue (objstack.pop ());
                            float y2 = GetFloatValue (objstack.pop ());
                            float x2 = GetFloatValue (objstack.pop ());
                            if (!isGray) break;
                            PathOpBezier po = new PathOpBezier ();
                            po.typev = true;
                            po.x2 = gscur.devx (x2, y2);
                            po.y2 = gscur.devy (x2, y2);
                            po.x3 = gscur.devx (x3, y3);
                            po.y3 = gscur.devy (x3, y3);
                            path.pathops.addLast (po);
                            break;
                        }
                        case "y": {
                            float y3 = GetFloatValue (objstack.pop ());
                            float x3 = GetFloatValue (objstack.pop ());
                            float y1 = GetFloatValue (objstack.pop ());
                            float x1 = GetFloatValue (objstack.pop ());
                            if (!isGray) break;
                            PathOpBezier po = new PathOpBezier ();
                            po.x1 = gscur.devx (x1, y1);
                            po.y1 = gscur.devy (x1, y1);
                            po.x2 = gscur.devx (x3, y3);
                            po.y2 = gscur.devy (x3, y3);
                            po.x3 = gscur.devx (x3, y3);
                            po.y3 = gscur.devy (x3, y3);
                            path.pathops.addLast (po);
                            break;
                        }

                        case "f*":
                        case "S": {
                            if (!isGray) break;
                            path.draw ();
                            path = null;
                            break;
                        }

                        case "s": {
                            if (!isGray) break;
                            PathOpLine po = new PathOpLine ();
                            po.x = path.movex;
                            po.y = path.movey;
                            path.pathops.addLast (po);
                            path.draw ();
                            path = null;
                            break;
                        }

                        // TEXT DRAWING

                        case "BT":
                        case "ET": {
                            textLineMatrix_a = 1;
                            textLineMatrix_b = 0;
                            textLineMatrix_c = 0;
                            textLineMatrix_d = 1;
                            textLineMatrix_e = 0;
                            textLineMatrix_f = 0;
                            textMatrix_a = 1;
                            textMatrix_b = 0;
                            textMatrix_c = 0;
                            textMatrix_d = 1;
                            textMatrix_e = 0;
                            textMatrix_f = 0;
                            break;
                        }

                        case "Tf": {
                            fontScale = GetFloatValue (objstack.pop ());
                            String fontIndex = GetNameValue (objstack.pop ());
                            //fontsInPDF = page.findResources ().getFonts ();
                            //fontInPDF = fontsInPDF.get (fontIndex);
                            break;
                        }

                        case "Tr": {
                            textRenderMode = GetIntegerValue (objstack.pop ());
                            break;
                        }

                        case "Tm": {
                            textMatrix_f = textLineMatrix_f = GetFloatValue (objstack.pop ());
                            textMatrix_e = textLineMatrix_e = GetFloatValue (objstack.pop ());
                            textMatrix_d = textLineMatrix_d = GetFloatValue (objstack.pop ());
                            textMatrix_c = textLineMatrix_c = GetFloatValue (objstack.pop ());
                            textMatrix_b = textLineMatrix_b = GetFloatValue (objstack.pop ());
                            textMatrix_a = textLineMatrix_a = GetFloatValue (objstack.pop ());
                            break;
                        }

                        // [ lma lmb 0 ]   [ ma mb 0 ]   [ 1 0 0 ] [ lma lmb 0 ]
                        // [ lmc lmd 0 ] = [ mc md 0 ] = [ 0 1 0 ] [ lmc lmd 0 ]
                        // [ lme lmf 1 ]   [ me mf 1 ]   [ x y 1 ] [ lme lmf 1 ]

                        case "TD":    // move to start of next line and set leading parameter //TODO what's the difference?
                        case "Td": {  // move to start of next line
                            float y = GetFloatValue (objstack.pop ());
                            float x = GetFloatValue (objstack.pop ());
                            textMatrix_a = textLineMatrix_a;
                            textMatrix_b = textLineMatrix_b;
                            textMatrix_c = textLineMatrix_c;
                            textMatrix_d = textLineMatrix_d;
                            textMatrix_e = textLineMatrix_a * x + textLineMatrix_c * y + textLineMatrix_e;
                            textMatrix_f = textLineMatrix_b * x + textLineMatrix_d * y + textLineMatrix_f;
                            textLineMatrix_a = textMatrix_a;
                            textLineMatrix_b = textMatrix_b;
                            textLineMatrix_c = textMatrix_c;
                            textLineMatrix_d = textMatrix_d;
                            textLineMatrix_e = textMatrix_e;
                            textLineMatrix_f = textMatrix_f;
                            break;
                        }

                        //TODO - move to start of next line
                        case "T*": {
                            break;
                        }

                        case "Tj": {
                            String str = GetStringValue (objstack.pop ());
                            if (!isGray || str.trim ().equals ("")) break;

                            // save text string for processing later
                            TextString ts = new TextString ();
                            ts.rawstr = str;

                            // brain damaged - gets null pointer exception in drawString() in most cases
                            // in cases where it succeeds, nothing appears on image
                            if (false) { //// (fontInPDF != null) {
                                AffineTransform at = gscur.getAffine ();
                                at.concatenate (new AffineTransform (textMatrix_a, textMatrix_b, textMatrix_c, textMatrix_d, textMatrix_e, textMatrix_f));
                                try {
                                    fontInPDF.drawString (str, null, g2d, fontScale, at, 0.0F, 0.0F);
                                    System.err.println ("Tj drew " + str);
                                } catch (Exception e) {
                                    String msg = e.getMessage ();
                                    if (msg == null) msg = e.getClass ().toString ();
                                    System.err.println ("error drawing string: " + msg);
                                    System.err.println ("fontInPDF=" + fontInPDF.getClass ().toString ());
                                    e.printStackTrace ();
                                    fontInPDF = null;
                                }
                            }

                            // this code puts strings in the right place
                            // although they aren't quite the right size

                            // select and scale the font to be used
                            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment ();
                            Font[] allFonts = ge.getAllFonts ();
                            Font newFont = allFonts[0];
                            for (Font f : allFonts) {
                                if (f.getFontName ().equals ("Dialog.plain")) {
                                    newFont = f;
                                    break;
                                }
                            }
                            newFont = newFont.deriveFont (new AffineTransform (fontScale * 0.9F, 0, 0, fontScale * -1.4F, 0, 0));
                            g2d.setFont (newFont);

                            // set up drawing transformation for the text string
                            // doing g2d.drawString() with the newFont does pretty good with this transformation
                            AffineTransform saveat = g2d.getTransform ();
                            g2d.transform (gscur.getAffine ());
                            g2d.transform (new AffineTransform (textMatrix_a, textMatrix_b, textMatrix_c, textMatrix_d, textMatrix_e, textMatrix_f));
                            AffineTransform textat = g2d.getTransform ();

                            // however FontMetrics is brain-dead if the font is rotated, so devise an un-rotated transform
                            double[] textmat = new double[6];
                            textat.getMatrix (textmat);
                            double angle = Math.atan2 (- textmat[2], - textmat[3]);
                                                // =0: horizontal non-inverted
                                                // >0: rotated anti-clockwise, eg, "MSA" in "MSA xxx xxx NM"
                                                // <0: rotated clockwise, eg, "NM" in "MSA xxx xxx NM"

                            textmat[0] = Math.hypot (textmat[0], textmat[1]);
                            textmat[1] = 0.0;
                            textmat[3] = Math.hypot (textmat[3], textmat[2]);
                            textmat[2] = 0.0;
                            g2d.setTransform (new AffineTransform (textmat));

                            FontMetrics fm   = g2d.getFontMetrics ();
                            double strWidth  = Math.abs (textmat[0] * fm.stringWidth (str));
                            double chrHeight = Math.abs (textmat[3] * fm.getAscent ());  // does not work in 1.7, use 1.8.0_25

                            // - lengths along the horizontal and vertical axes of the rotated text
                            //   comments are for small pos angle, eg, "MSA" in "MSA xxx xxx NM"
                            //   ...and relative to bottom left corner of the "M"
                            double horizdx =   strWidth  * Math.cos (angle);  // mostly goes to the right along the bottom
                            double horizdy = - strWidth  * Math.sin (angle);  // slightly goes up along the bottom
                            double vertdx  = - chrHeight * Math.sin (angle);  // slightly goes left along the left edge
                            double vertdy  = - chrHeight * Math.cos (angle);  // mostly goes up along the left adge

                            // - bottom left corner
                            ts.botleftx = (int) (textmat[4] + 0.5);
                            ts.botlefty = (int) (textmat[5] + 0.5);

                            // - bottom right corner
                            ts.botritex = (int) (textmat[4] + horizdx + 0.5);
                            ts.botritey = (int) (textmat[5] + horizdy + 0.5);

                            // - top left corner
                            ts.topleftx = (int) (textmat[4] + vertdx + 0.5);
                            ts.toplefty = (int) (textmat[5] + vertdy + 0.5);

                            // - top right corner
                            ts.topritex = (int) (textmat[4] + horizdx + vertdx + 0.5);
                            ts.topritey = (int) (textmat[5] + horizdy + vertdy + 0.5);

                            // - undo unrotate so we are rotated again
                            g2d.setTransform (textat);

                            // skip the huge morse-code letters
                            if (chrHeight < csvDpi / 3) {

                                // draw text string
                                g2d.setColor (isBlack ? Color.BLACK : Color.GRAY);
                                g2d.drawString (str, 0.0F, 0.0F);

                                // save it
                                textStrings.addLast (ts);
                            }

                            // - pop text transformation
                            g2d.setTransform (saveat);

                            // draw a rotated pink box around the text
                            ////g2d.setColor (Color.PINK);
                            ////ts.drawBox (g2d);
                            break;
                        }

                        // COLOR

                        case "RG":      // set color
                        case "rg": {    // set color
                            float bb = GetFloatValue (objstack.pop ());
                            float gg = GetFloatValue (objstack.pop ());
                            float rr = GetFloatValue (objstack.pop ());
                            isGray   = ((rr == gg) && (gg == bb)) || ((rr >= 0.75) && (gg == 0) && (bb == 0));
                            isBlack  = isGray;
                            break;
                        }

                        // DON'T CARE

                        case "d":       // set line dash pattern
                            objstack.pop ();

                        case "CS":      // set color space
                        case "cs":      // set color space
                        case "gs":      // set parameters from graphics state parameter dictionary
                        case "i":       // set flatness tolerance
                        case "ri":      // set color rendering intent
                            objstack.pop ();

                        case "f":       // fill path
                        case "SCN":     // set color
                        case "scn":     // set color
                        case "W*":      // set clipping path
                            ////System.err.println ("don't care PDF code <" + code + ">");
                            break;

                        // NOT SUPPORTED

                        case "d1":
                            objstack.pop ();
                            objstack.pop ();

                        case "K":
                        case "k":
                            objstack.pop ();
                            objstack.pop ();

                        case "d0":
                            objstack.pop ();

                        case "Do":
                        case "G":
                        case "g":
                        case "sh":
                        case "Tc":
                        case "TJ":
                        case "TL":
                        case "Ts":
                        case "Tw":
                        case "Tz":
                            objstack.pop ();

                        default:
                            System.err.println ("unsupported PDF code <" + code + ">");
                            break;
                    }
                } else if (token instanceof COSBase) {
                    ////printCOSObject ("", "  token -> [" + objstack.size () + "]", (COSBase) token);
                    objstack.push ((COSBase) token);
                } else {
                    System.err.println (spacer + "  unhandled token " + token.getClass ());
                }
            }

            g2d.setFont (originalFont);
            g2d = null;
        }

        /**
         * Resolve positions on the plate of named fixes.
         */
        public void resolveFixes (Graphics2D g)
                throws IOException
        {
            g2d = g;

            // analyze line segments
            analyzeLineSegs ();

            // analyze strings part one
            analyzeStrings1 ();

            // find boxed-off areas and shade them yellow
            findBoxedOffAreas ();

            // find runway centerline
            findRunwayCenterline ();

            // find loops made of line segments
            findSegmentLoops ();

            // maybe there is a FEEDER FACILITIES ring
            // box off everything outside that ring
            findFeederFacilitiesRing ();

            shadeBoxedOffAreas ();

            // analyze strings part two
            analyzeStrings2 ();

            // if there is a pair of little Bez16s near each other,
            // it's a 4-pointed star GPS marker.

            while (true) {

                // find the two little Bez16s that are closest to each other on whole plate
                // but no farther than 1/6 inch away from each other (typically 1/12 inch apart)
                Bez16 besti = null;
                Bez16 bestj = null;
                float bestdistsq = csvDpi * csvDpi / 36.0F;
                for (Bez16 bez16i : lilBez16s) {
                    for (Bez16 bez16j : lilBez16s) {
                        if (bez16j != bez16i) {
                            float dx = bez16j.posx - bez16i.posx;
                            float dy = bez16j.posy - bez16i.posy;
                            float distsq = dx * dx + dy * dy;
                            if (bestdistsq > distsq) {
                                bestdistsq = distsq;
                                besti = bez16i;
                                bestj = bez16j;
                            }
                        }
                    }
                }
                if (besti == null) break;

                // pair of little Bez16s found, remove from list yet to process
                lilBez16s.remove (besti);
                lilBez16s.remove (bestj);

                // get the average as the position of the marker
                Marker marker = new Marker (
                    (int) ((besti.posx + bestj.posx + 1.0F) / 2.0F),
                    (int) ((besti.posy + bestj.posy + 1.0F) / 2.0F));
                marker.types = marker_types_bez16;
                marker.linesegs = besti.linesegs;
                marker.linesegs.addAll (bestj.linesegs);

                // save it as a marker
                markers.addLast (marker);
            }

            // try to assemble VORDME, VORTAC, etc shapes from small line segments
            // to form corresponding markers

            findSmallLineShapes (smallLineSegs, vorShape,       marker_types_vor);
            findSmallLineShapes (smallLineSegs, vorShape2,      marker_types_vor);
            findSmallLineShapes (smallLineSegs, vordmeShape,    marker_types_vordme);
            findSmallLineShapes (smallLineSegs, vordmeShape2,   marker_types_vordme);
            findSmallLineShapes (smallLineSegs, vordmeShape3,   marker_types_vordme);
            findSmallLineShapes (smallLineSegs, vortacShape,    marker_types_vortac);
            findSmallLineShapes (smallLineSegs, triangleShape,  marker_types_triangle);
            findSmallLineShapes (smallLineSegs, localizerShape, marker_types_localizer);

            findSmallLineShapes (smallBeziers,  threshShape,    marker_types_thresh);

            // try to assemble NDB shapes from ndbCenters and ndbDots Beziers to form corresponding markers

            findNDBShapes ();

            // sometimes there is a long line on the runway centerline
            // with small crosswise tick mark lines indicating stepdowns
            // and they often have associated fix names

            findRwyStepDowns ();

            // except for the big box surrounding the whole plate,
            // ignore markers found inside any smaller boxes.
            // this prevents us from trying to use markers in missed approach procedure depictions.
            // likewise for platefix strings and segment loops.

            livePlateFixes = new LinkedList<> (allPlateFixes);
            deleteBoxedMarkers ();

            // - allPlateFixes = all strings found on plate that are in database near airport, even in yellowed-out area
            // - livePlateFixes = subset of allPlateFixes that are not in yellowed-out area

            // look for cartoon bubble loops around VOR and NDB platefix strings
            // move the string position to end of the corresponding pointer line

            findCartoonBubbles ();

            // display cyan box around all live platefix strings

            for (PlateFix platefix : livePlateFixes) {
                TextString ts = platefix.text;
                g2d.setColor (Color.GREEN);
                ts.drawBox (g2d);
                g2d.setColor (Color.CYAN);
                for (LRTBInt bound : ts.bounds) {
                    bound.drawBox (g2d);
                }
                if (verbose) {
                    DBFix dbfix = platefix.dbfix;
                    int gx = (ts.botleftx + ts.botritex) / 2;
                    int gy = (ts.toplefty + ts.botlefty) / 2;
                    System.out.println ("platefix " + dbfix.name + " (" + dbfix.type + ") string at " + gx + "," + gy);
                }
            }

            // display small magenta crosshairs on all markers

            g2d.setColor (Color.MAGENTA);
            for (Marker marker : markers) {
                int x0 = marker.centx;
                int y0 = marker.centy;
                g2d.drawLine (x0 - 30, y0, x0 + 30, y0);
                g2d.drawLine (x0, y0 - 30, x0, y0 + 30);
                if (verbose) System.out.println ("marker " + marker);
            }

            // find lightning bolts

            findLightningBolts ();

            // for each live plate fix, find the closest marker

            matchPlateFixesToMarkers ();

            // - allPlateFixes = unchanged (all strings found on plate that are in database near airport, even in yellowed-out area)
            // - livePlateFixes = all fixes not in yellowed-out areas that aren't matched with a marker
            // - foundPlateFixes = all non-DME fixes not in yellowed-out areas that are matched with a marker
            // - foundDMEFixes = all DME fixes not in yellowed-out areas that are matched with a marker

            for (Iterator<PlateFix> it = foundPlateFixes.iterator (); it.hasNext ();) {
                PlateFix lpf = it.next ();
                if (ignorePlateFixes.contains (faaid + ":" + plateid + ":" + lpf.dbfix.name)) {
                    it.remove ();
                }
            }

            // display a large magenta crosshair on all found fixes

            for (PlateFix platefix : foundPlateFixes) {
                if (verbose) System.out.println ("platefix " + platefix.dbfix.name + " found at " + platefix.posx + "," + platefix.posy + " q=" + platefix.quality);
                g2d.setColor (Color.MAGENTA);
                g2d.drawLine (platefix.posx - 80, platefix.posy, platefix.posx + 80, platefix.posy);
                g2d.drawLine (platefix.posx, platefix.posy - 80, platefix.posx, platefix.posy + 80);
                g2d.setColor (Color.MAGENTA);
                g2d.drawLine (platefix.posx - 80, platefix.posy, platefix.posx + 80, platefix.posy);
                g2d.drawLine (platefix.posx, platefix.posy - 80, platefix.posx, platefix.posy + 80);
            }

            // fences indicate the line from the navaid into the plate is not the correct length
            // ...and therefore the marker is not in the right place
            // hide the fenced-off markers so we can try to solve the plate without those navaids

            hideFencedPlateFixes ();

            // - allPlateFixes    = unchanged
            // - livePlateFixes   = unchanged (plate fix strings from live area that were not matched with a marker)
            // - foundPlateFixes  = plate fix strings from live area that were matched with a marker and are not fenced off
            // - fencedPlateFixes = plate fix strings from live area that were matched with a marker but that are fenced off

            int nFoundPlateFixes = foundPlateFixes.size ();
            PlateFix[] platefixarray = new PlateFix[nFoundPlateFixes];
            foundPlateFixes.toArray (platefixarray);
            PlateFix besti = null;
            PlateFix bestj = null;

            // for plates with many fixes, pick the two that make all others match the closest

            if (nFoundPlateFixes > 3) {
                double bestdists = (nFoundPlateFixes - 3) * csvDpi / 30;

                // i,j = two fixes that we are testing
                for (int i = 0; i < nFoundPlateFixes; i ++) {
                    PlateFix pfi = platefixarray[i];
                    for (int j = i; ++ j < nFoundPlateFixes;) {
                        PlateFix pfj = platefixarray[j];
                        if (pfj.dbfix == pfi.dbfix) continue;

                        // m = one fix to be completely ignored (in case of the common case of missing a fence)
                        for (int m = 0; m < nFoundPlateFixes; m ++) {
                            PlateFix pfm = platefixarray[m];
                            if (pfm.dbfix == pfi.dbfix) continue;
                            if (pfm.dbfix == pfj.dbfix) continue;

                            // k = used to sum all the fixes other than i,j,m
                            // sum up how far off the other fixes are from their marks
                            double dists = 0.0;
                            for (int k = 0; k < nFoundPlateFixes; k ++) {
                                PlateFix pfk = platefixarray[k];
                                if (pfk.dbfix == pfi.dbfix) continue;
                                if (pfk.dbfix == pfj.dbfix) continue;
                                if (pfk.dbfix == pfm.dbfix) continue;
                                int x = latlon2X (pfi, pfj, pfk.dbfix.lat, pfk.dbfix.lon);
                                int y = latlon2Y (pfi, pfj, pfk.dbfix.lat, pfk.dbfix.lon);
                                dists += Math.hypot (x - pfk.posx, y - pfk.posy);
                            }
                            if (verbose) System.out.println ("match " + pfi.dbfix.name + " " + pfj.dbfix.name + ", skip " + pfm.dbfix.name + ", dists=" + dists);
                            if (bestdists > dists) {
                                bestdists = dists;
                                besti = pfi;
                                bestj = pfj;
                            }
                        }
                    }
                }
            }

            // find pair of non-fenced fixes that have the best quality
            // but the course line between the two fixes of x,y on the plate must be within 5deg of the course of lat/lon
            // they must also be at least 1.0nm apart

            if (besti == null) {
                double bestqual = csvDpi * 1.5;
                for (int i = 0; i < nFoundPlateFixes; i ++) {
                    PlateFix platefixi = platefixarray[i];
                    for (int j = i; ++ j < nFoundPlateFixes;) {
                        PlateFix platefixj = platefixarray[j];
                        if (platefixj.dbfix != platefixi.dbfix) {
                            double ad     = CalcAngleDiff (platefixi, platefixj);
                            double qual   = platefixi.quality + platefixj.quality;
                            double distnm = Lib.LatLonDist (platefixi.dbfix.lat, platefixi.dbfix.lon, platefixj.dbfix.lat, platefixj.dbfix.lon);
                            if (verbose) {
                                System.out.println ("match " + platefixi.dbfix.name + " " + platefixj.dbfix.name +
                                        " ad=" + ad + " q=" + qual + " d=" + distnm);
                            }
                            double distpx = distnm * 1852.0 * 1000.0 / 25.4 * csvDpi;
                            if ((ad < 5.0) && (distnm > 1.0) && (bestqual > qual + ad - distpx / 1000000.0)) {
                                bestqual = qual + ad - distpx / 1000000.0;
                                besti    = platefixi;
                                bestj    = platefixj;
                            }
                        }
                    }
                }
            }

            // if no solution found, try using a found fix and a DME fix
            // but the found fix and DME fix must both be on runway centerline
            // so we can calculate lat/lon of DME fix

            if ((besti == null) && (rwyCenterline != null)) {
                ActualDMEFix admef = new ActualDMEFix ();
                besti = admef.besti;
                bestj = admef.bestj;
            }

            // if no solution found, try using a found fix and a fenced fix.

            if (besti == null) {

                // the line coming out of the fenced fix going through the fence must not
                // point directly at the found fix because we don't know the chart's scale.
                // use found vs fenced combo that gives lines closest to being perpendicular,
                double bestangle = FencedPlateFix.minang;
                FencedPlateFix fencedFix = null;
                bestj = null;
                for (FencedPlateFix fpf : fencedPlateFixes) {
                    for (PlateFix pf : foundPlateFixes) {
                        double angle = fpf.solve (pf);
                        if (verbose) System.out.println ("fenced " + fpf.fencedFix.dbfix.name + " vs " + pf.dbfix.name + " angle " + Math.toDegrees (angle));
                        if (bestangle < angle) {
                            bestangle = angle;
                            fencedFix = fpf;
                            bestj = pf;
                        }
                    }
                }

                if (fencedFix != null) {
                    besti = fencedFix.fencedFix;

                    // figure out where to put fenced fix so it works out nice
                    if (verbose) System.out.println ("using fenced fix " + besti.dbfix.name + " with known fix " + bestj.dbfix.name);
                    fencedFix.solve (bestj);

                    besti.posx = (int) Math.round (fencedFix.solvedx);
                    besti.posy = (int) Math.round (fencedFix.solvedy);
                }
            }

            // there may be a long runway centerline that has a fenced vor and a
            // dme fix using that vor.  if so, we can resolve the lat/lon of the
            // dme fix.  BXA IAP-VORDME-A.

            if ((besti == null) && (rwyCenterline != null)) {
                findDMEFixLatLons ();
            }

            // if no solution and no centerline, maybe there is an alternate centerline.

            if ((besti == null) && (rwyCenterline == null)) {
                findAlternateCenterline ();
            }

            // if no solution found and there is a fix on the runway centerline,
            // assume it is a single-fix chart and synthesize a DME fix halfway along the detected runway centerline

            if ((besti == null) && (rwyCenterline != null)) {
                SynthDMEFix sdmef = new SynthDMEFix ();
                besti = sdmef.besti;
                bestj = sdmef.bestj;
            }

            // verify the mapping locates line segments for the runways

            if (besti != null) {
                g2d.setColor (new Color (165, 0, 213));
                Stroke oldStroke = g2d.getStroke ();
                g2d.setStroke (new BasicStroke (5));

                // split runways between list of good and bad depending on whether a matching line is found
                LinkedList<Runway> badRunways  = new LinkedList<> ();
                LinkedList<Runway> goodRunways = new LinkedList<> ();
                for (Runway runway : airport.runways.values ()) {

                    // don't bother with water runways cuz they aren't on charts
                    if (runway.name.endsWith ("W")) continue;

                    // get pixels where we should find the runway
                    int rwybegx = latlon2X (besti, bestj, runway.lat, runway.lon);
                    int rwybegy = latlon2Y (besti, bestj, runway.lat, runway.lon);
                    int rwyendx = latlon2X (besti, bestj, runway.endlat, runway.endlon);
                    int rwyendy = latlon2Y (besti, bestj, runway.endlat, runway.endlon);
                    double rwylen = Math.hypot (rwyendx - rwybegx, rwybegy - rwyendy);

                    // draw it, hopefully overlapping what is on plate
                    g2d.drawLine (rwybegx, rwybegy, rwyendx, rwyendy);

                    // rotate onto Y axis for easy overlap check
                    double rwyhdg    = Math.atan2 (rwyendx - rwybegx, rwybegy - rwyendy);
                    double rwyhdgcos = Math.cos (rwyhdg);
                    double rwyhdgsin = Math.sin (rwyhdg);
                    double rwybegr   = rwybegy * rwyhdgcos - rwybegx * rwyhdgsin;
                    double rwyendr   = rwyendy * rwyhdgcos - rwyendx * rwyhdgsin;
                    if (rwybegr > rwyendr) { double t = rwybegr; rwybegr = rwyendr; rwyendr = t; }

                    // scan for matching line segment
                    boolean found    = false;
                    for (LineSeg ls : allLineSegs) {

                        // must be parallel within a few pixels
                        double distbeg = distToLine (rwybegx, rwybegy, ls.x0, ls.y0, ls.x1, ls.y1);
                        double distend = distToLine (rwyendx, rwyendy, ls.x0, ls.y0, ls.x1, ls.y1);
                        if ((distbeg < 12.0) && (distend < 12.0)) {

                            // must have some overlap
                            double lsr0 = ls.y0 * rwyhdgcos - ls.x0 * rwyhdgsin;
                            double lsr1 = ls.y1 * rwyhdgcos - ls.x1 * rwyhdgsin;
                            if (lsr0 > lsr1) { double t = lsr0; lsr0 = lsr1; lsr1 = t; }
                            double beg = Math.max (lsr0, rwybegr);
                            double end = Math.min (lsr1, rwyendr);
                            double overlap = end - beg;
                            if (overlap > rwylen / 2.0) {
                                found = true;
                                break;
                            }
                        }
                    }

                    // add runway to good (found) or bad (not found) list
                    (found ? goodRunways : badRunways).addLast (runway);
                }
                g2d.setStroke (oldStroke);

                // see if any runway was not found
                if (!badRunways.isEmpty ()) {

                    // find largest angle between runways that were found (and thus verified)
                    double bestsplit = 0.0;
                    for (Runway rwyi : goodRunways) {
                        double hdgi = Lib.LatLonTC_rad (rwyi.lat, rwyi.lon, rwyi.endlat, rwyi.endlon);
                        for (Runway rwyj : goodRunways) {
                            double hdgj = Lib.LatLonTC_rad (rwyj.lat, rwyj.lon, rwyj.endlat, rwyj.endlon);
                            double split = AngleDiff (hdgi, hdgj);
                            if (split > Math.PI / 2.0) split = Math.PI - split;
                            if (bestsplit < split) bestsplit = split;
                        }
                    }

                    // if large split (eg, 45 or 90 deg angle), consider that all is verified
                    // otherwise, print list of unverified runways
                    if (bestsplit < Math.toRadians (15.0)) {
                        for (Runway rwy : badRunways) {
                            System.out.println ("CANNOT VERIFY LOCATION OF " + rwy.name);
                        }
                    }
                }
            }

            // write out solution results

            String quotedplateid = Lib.QuotedString (plateid);
            if (besti == null) {
                if (verbose) System.out.println ("no best pair found");
                if (rejectsname != null) {
                    if (rejectsfile == null) rejectsfile = new PrintWriter (rejectsname);
                    rejectsfile.print (airport.faaid + "," + quotedplateid + "," + pdfName + ",");
                    boolean first = true;
                    for (DBFix dbfix : nearDBFixes.values ()) {
                        if (dbfix.mentioned) {
                            if (!first) rejectsfile.print (" ");
                            rejectsfile.print (dbfix.name);
                            first = false;
                        }
                    }
                    rejectsfile.println ("");
                    rejectsfile.flush ();
                }
            } else {
                if (verbose) {
                    double ad = CalcAngleDiff (besti, bestj);
                    double sf = CalcScaleFactor (besti, bestj);
                    System.out.println ("sf=" + String.format ("%09d", (int) sf) + " " + airport.faaid + " '" + plateid + "' ad=" + ad);
                    System.out.println ("  " + besti.dbfix.name + " at " + besti.posx + "," + besti.posy);
                    System.out.println ("  " + bestj.dbfix.name + " at " + bestj.posx + "," + bestj.posy);
                }

                /*
                 * Write result to georefs/machine_<expdate>.csv
                 */
                if (csvoutname != null) {
                    if (csvoutfile == null) csvoutfile = new PrintWriter (csvoutname);
                    csvoutfile.println (airport.name + "," + quotedplateid + "," + besti.dbfix.name + "," + besti.posx + "," + besti.posy);
                    csvoutfile.println (airport.name + "," + quotedplateid + "," + bestj.dbfix.name + "," + bestj.posx + "," + bestj.posy);
                    csvoutfile.flush ();
                }

                /*
                 * For all fix name strings mentioned, draw a spot on the chart with the name.
                 */
                airport.mentioned = true;
                nearDBFixes.put (airport.name, airport);
                Font oldfont = g2d.getFont ();
                int oldfontheight = g2d.getFontMetrics ().getHeight ();
                int newfontheight = csvDpi / 10;
                Font normfont = oldfont.deriveFont (oldfont.getSize2D () * newfontheight / oldfontheight);
                Font mapdfont = normfont.deriveFont (Font.BOLD);
                g2d.setFont (oldfont.deriveFont (oldfont.getSize2D () * newfontheight / oldfontheight));
                g2d.setColor (Color.RED);
                for (DBFix dbfix : nearDBFixes.values ()) {
                    if (dbfix.mentioned) {
                        g2d.setFont (((dbfix == besti.dbfix) || (dbfix == bestj.dbfix)) ? mapdfont : normfont);
                        int x = latlon2X (besti, bestj, dbfix.lat, dbfix.lon);
                        int y = latlon2Y (besti, bestj, dbfix.lat, dbfix.lon);
                        g2d.fillOval (x - 4, y - 4, 8, 8);
                        g2d.drawString (dbfix.name, x + 6, y - 6);
                    }
                }
                g2d.setFont (oldfont);
            }

            g2d = null;
        }

        /**
         * Strings might be broken down into character-by-character strings.
         * Try to consolidate the broken down ones into whole strings.
         */
        private void analyzeStrings1 ()
        {
            /*
             * Merge strings that are close enough in a line to each other.
             */
            int nTextStrings = textStrings.size ();
            TextString[] textStringArray = new TextString[nTextStrings];
            textStrings.toArray (textStringArray);
            boolean didSomething;
            do {
                didSomething = false;
                for (int i = 0; i < nTextStrings; i ++) {

                    // get first string
                    TextString tsi = textStringArray[i];
                    if (tsi == null) continue;

                    double charwidth = tsi.width () / tsi.rawstr.length ();

                    // look for a string immediately to the right of first string
                    double bestdw = charwidth / 2.0;
                    int bestj = -1;
                    TextString besttc = null;
                    for (int j = 0; j < nTextStrings; j ++) {
                        if (j == i) continue;

                        // get second string
                        TextString tsj = textStringArray[j];
                        if (tsj == null) continue;

                        // they must be at same angle
                        if (AngleDiff (tsi.angle (), tsj.angle ()) > K_TEXTJOINANGLE) continue;

                        // make up a tentative composite text string
                        TextString tsc = new TextString ();
                        tsc.rawstr   = tsi.rawstr + tsj.rawstr;
                        tsc.botleftx = tsi.botleftx;  // left edge is same as first string's left edge
                        tsc.botlefty = tsi.botlefty;
                        tsc.topleftx = tsi.topleftx;
                        tsc.toplefty = tsi.toplefty;
                        tsc.botritex = tsj.botritex;  // right edge is same as second string's right edge
                        tsc.botritey = tsj.botritey;
                        tsc.topritex = tsj.topritex;
                        tsc.topritey = tsj.topritey;

                        // the composite's angle should be the same as the original's
                        double anglediff = AngleDiff (tsi.angle (), tsc.angle ());
                        if (anglediff > Math.toRadians (2.0)) continue;

                        // the composite's width should be no more than an extra character's width
                        // but also allow for 0.90 char space overlap cuz we oversize the width sometimes
                        double widthdiff = tsc.width () - tsi.width () - tsj.width ();
                        if (widthdiff < - 0.90 * charwidth) continue;

                        // see if this combo is the most closely spaced so far
                        if (bestdw <= widthdiff) continue;

                        // for non-tilted text, make sure there is no vertical bar dividing them apart
                        // we specifically don't want the 'APP CRS' and degrees strings merging with anything from next box over
                        if (tsi.angle () < K_TILTEDTEXT) {
                            int leftx = Math.min (tsi.topritex, tsi.botritex) - 5;  // leftmost of where to search
                            int ritex = Math.max (tsj.topleftx, tsj.botleftx) + 5;  // rightmost of where to search
                            int topy  = Math.min (tsi.topritey, tsj.toplefty) - 3;  // topmost of where to search
                            int boty  = Math.max (tsi.botritey, tsj.botlefty) + 3;  // bottommost of where to search

                            int leftStrLen = tsi.rawstr.length ();
                            if (leftStrLen > 0) {
                                do {
                                    if (tsi.rawstr.charAt (leftStrLen - 1) != ' ') break;
                                } while (-- leftStrLen > 0);
                                int spaceWidths = (tsi.topritex - tsi.topleftx) *
                                                      (tsi.rawstr.length () - leftStrLen)
                                                          / leftStrLen;
                                leftx -= spaceWidths;
                            }

                            boolean foundOne = false;
                            for (int x = leftx; ++ x < ritex;) {
                                int nHits = 0;
                                for (int y = topy; ++ y < boty;) {
                                    if (hvLineBytes[y*panelWidth+x] == HVLB_DRWN) nHits ++;
                                }
                                if (nHits * 4 > (boty - topy) * 3) {
                                    foundOne = true;
                                    break;
                                }
                            }
                            if (foundOne) continue;
                        }

                        // it is the best combo so far, save it
                        bestdw = widthdiff;
                        bestj  = j;
                        besttc = tsc;
                    }

                    // if combination detected, have it replace the first string and delete second string
                    if (besttc != null) {
                        textStringArray[i] = tsi = besttc;
                        textStringArray[bestj] = null;
                        didSomething = true;
                    }
                }
            } while (didSomething);

            /*
             * Put back in list form.
             */
            textStrings.clear ();
            for (TextString ts : textStringArray) {
                if (ts != null) {
                    ////System.out.println ("analyzeStrings1*: <" + faaid + ":" + ts.rawstr + ">");
                    String bs = brokenStrings.get (faaid + ":" + ts.rawstr);
                    if (bs != null) ts.rawstr = bs;
                    textStrings.addLast (ts);
                }
            }

            /*
             * Near upper left corner is a box giving the approach course.
             */
            for (TextString tsm : textStrings) {
                String str = tsm.rawstr.trim ();
                if (str.equals ("APCH CRS") || str.equals ("APP CRS")) {
                    int markx = (tsm.botleftx + tsm.topritex) / 2;
                    int marky = (tsm.botlefty + tsm.topritey) / 2;
                    int csize =  tsm.botritey - tsm.toplefty;
                    for (TextString tsv : textStrings) {
                        int x  = (tsv.botleftx + tsv.topritex) / 2;
                        int y  = (tsv.botlefty + tsv.topritey) / 2;
                        int dx = x - markx;
                        int dy = y - marky;
                        if ((Math.abs (dx) < csize * 4) && (dy > csize / 2) && (dy < csize * 4)) {
                            String stripped = tsv.rawstr.replace (" ", "");
                            if (stripped.endsWith ("\u00B0")) {
                                stripped = stripped.substring (0, stripped.length () - 1);
                            } else if (stripped.endsWith ("\u00B0M")) {
                                stripped = stripped.substring (0, stripped.length () - 2);
                            }
                            try {
                                appCrsMag   = Double.parseDouble (stripped);
                                appCrsTrue  = appCrsMag - airport.magvar;
                                appCrsStr   = stripped;
                                appCrsFound = true;
                                if (appCrsTrue <    0.0) appCrsTrue += 360.0;
                                if (appCrsTrue >= 360.0) appCrsTrue -= 360.0;
                                System.out.println ("approach course " + appCrsStr + "\u00B0, " + appCrsTrue + "\u00B0 true");
                                break;
                            } catch (NumberFormatException nfe) {
                            }
                        }
                    }
                    if (appCrsFound) break;
                }
            }
            if (!appCrsFound && (runway != null)) {
                appCrsFound = true;
                appCrsTrue  = Lib.LatLonTC (runway.lat, runway.lon, runway.endlat, runway.endlon);
            }

            /*
             * If we know what the approach course is, look for those strings at an angle throughout the plate.
             * Then get rid of all other angled strings cuz we don't need them for anything else.
             */
            if (appCrsStr != null) {
                String appCrsStrDeg    = appCrsStr + "\u00B0";
                String appCrsStrDegMag = appCrsStr + "\u00B0M";

                // get angle approach course makes with horizontal (radians)
                double appangle = Math.toRadians (90.0 - appCrsTrue);

                // loop through all the text strings
                for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {
                    TextString ts = it.next ();

                    // get text angle from horizontal (radians)
                    double tsangle = ts.angle ();

                    // see if we have same string (with or without ^ or ^M) as shown in upper left corner under APCH CRS or APP CRS
                    String str = ts.rawstr.trim ();
                    if (str.equals (appCrsStr) || str.equals (appCrsStrDeg) || str.equals (appCrsStrDegMag)) {
                        double ad = AngleDiff (tsangle, appangle);
                        if (ad > Math.PI / 2) ad = Math.PI - ad;
                        if (ad < K_APPCRSSTRANG) {
                            if (verbose) System.out.println ("app crs string found at " + ts.centerx () + "," + ts.centery ());
                            appCrsStrings.addLast (ts);
                        }
                    }

                    // if string is at a tilt, remove it from further processing
                    if (Math.abs (tsangle) > K_TILTEDTEXT) it.remove ();
                }
            }
        }

        /**
         * Do some analysis on the composite strings.
         */
        private void analyzeStrings2 ()
        {
            /*
             * Get rid of all strings in blocked-off areas.
             */
            for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {
                TextString ts = it.next ();
                if (!containsAGoodPixel (ts.botleftx, ts.botritex, ts.toplefty, ts.botlefty)) {
                    it.remove ();
                }
            }

            /*
             * Break strings into words, removing redundant spaces.
             * Get rid of strings that don't have any words.
             */
            for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {
                TextString ts = it.next ();
                String[] words = ts.rawstr.split (" ");
                int j = 0;
                for (String word : words) {
                    if (!word.equals ("")) words[j++] = word;
                }
                if (j > 0) {
                    if (j < words.length) words = Arrays.copyOf (words, j);
                    ts.words = words;
                } else {
                    it.remove ();
                }
            }

            /*
             * There are strings "x" or "X" that are markers for misc fixes.
             */
            for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {
                TextString ts = it.next ();
                if ((ts.words.length == 1) && (ts.words[0].equals ("x") || ts.words[0].equals ("X"))) {
                    int centerx = (ts.botleftx + ts.botritex) / 2;
                    int centery = (ts.toplefty  + ts.botlefty)  / 2;
                    Marker marker   = new Marker (centerx, centery);
                    marker.types    = marker_types_stepdown;
                    marker.linesegs = new LinkedList<> ();
                    marker.linesegs.addLast (new LineSeg (ts.botleftx, ts.botlefty, ts.botritex, ts.toplefty));
                    marker.linesegs.addLast (new LineSeg (ts.botleftx, ts.toplefty, ts.botritex, ts.botlefty));
                    markers.addLast (marker);
                    it.remove ();
                }
            }

            /*
             * There are strings like
             *        +------
             *   OLU  | 22.5  )
             *        +------
             * and
             *         TOP
             *        +---
             *        | 7  )
             *        +---
             * so we want to mark the numbers with a [.
             */
            // scan through all the closed loops
            for (LinkedList<SegNode> loop : closedLoops) {

                // there should be a left/top/bottom line segment forming a D symbol
                // left vertical segments about 1/9 inch long
                // horizontal segments at least 1/12 inch long
                int top  = 999999999;
                int bot  = 0;
                int left = 999999999;
                int rite = 0;
                int lx   = 0;
                int ly   = 0;
                for (SegNode node : loop) {
                    int nx = node.index & 0xFFFF;
                    int ny = node.index >> 16;
                    int dx = Math.abs (nx - lx);
                    int dy = Math.abs (ny - ly);
                    if ((nx == lx) && (dy > csvDpi / 12) && (dy < csvDpi / 6)) {
                        if (left > nx) left = nx;
                    }
                    if ((dx < csvDpi / 12) && (dy < csvDpi / 12)) {
                        if (rite < nx) rite = nx;
                    }
                    if ((ny == ly) && (dx > csvDpi / 12) && (dx < csvDpi / 4)) {
                        if (top > ny) top = ny;
                        if (bot < ny) bot = ny;
                    }
                    lx = nx;
                    ly = ny;
                }

                // the box should be about a 1/4 inch wide and 1/9 inch tall
                int width  = rite - left;
                int height = bot  - top;
                if ((width > csvDpi / 8) && (width < csvDpi / 3) && (height > csvDpi / 12) && (height < csvDpi / 6)) {
                    int dmeboxcx = (left + rite) / 2;
                    int dmeboxcy = (top  + bot)  / 2;

                    // mark any enclosed numeric string with a [
                    // make string edges include the D shape
                    double bestdist = csvDpi / 4.0;
                    TextString bestts = null;
                    for (TextString ts : textStrings) {
                        if ((ts.toplefty > top) && (ts.botlefty < bot) && (ts.botleftx < rite) && (ts.botritex > left)) {
                            double dist = Math.hypot (ts.centerx () - dmeboxcx, ts.centery () - dmeboxcy);
                            if (bestdist > dist) {
                                bestdist = dist;
                                bestts   = ts;
                            }
                        }
                    }

                    if (bestts != null) {
                        int nwords = bestts.words.length;
                        if (nwords > 0) {

                            // sometime the fix name is jammed up with the DME value
                            // so we have something like IFI25.5 as on RQO 'IAP-VORDME RWY 35'
                            // and we want to split it into IFI 25.5
                            String lastword = bestts.words[nwords-1];
                            int lwlength = lastword.length ();
                            int i;
                            for (i = 0; i < lwlength; i ++) {
                                char c = lastword.charAt (i);
                                if ((c <= 'A') || (c >= 'Z')) break;
                            }
                            if (i < lwlength) {
                                if (i > 0) {
                                    String[] newwords = new String[nwords+1];
                                    System.arraycopy (bestts.words, 0, newwords, 0, nwords - 1);
                                    newwords[nwords-1] = lastword.substring (0, i);
                                    newwords[nwords++] = lastword.substring (i);
                                    bestts.words = newwords;
                                }
                            }

                            // either way, stick the DME mark before the last word which is now presumably all numeric
                            bestts.words[nwords-1] = dmeMark + bestts.words[nwords-1];
                        }

                        // make the word boundary include the dme symbol box
                        if (bestts.toplefty > top)  bestts.toplefty = top;
                        if (bestts.botlefty < bot)  bestts.botlefty = bot;
                        if (bestts.topritey > top)  bestts.topritey = top;
                        if (bestts.botritey < bot)  bestts.botritey = bot;
                        if (bestts.topleftx > left) bestts.topleftx = left;
                        if (bestts.topritex < rite) bestts.topritex = rite;
                        if (bestts.botleftx > left) bestts.botleftx = left;
                        if (bestts.botritex < rite) bestts.botritex = rite;
                    }
                }
            }

            /*
             * There are elevation strings all over the plates.
             * We can easily get rid of all things that are just a single integer,
             * as we don't ever use them for anything anyway (DME distances excepted
             * which we have marked above).
             * Getting rid of elevation strings helps when finding multiple-line
             * strings as they can make two unrelated blocks seem to go together.
             */
            for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {
                TextString ts = it.next ();
                if (ts.words.length == 1) {
                    String str = ts.words[0];
                    if (str.matches ("^\\d+$")) it.remove ();
                }
            }

            /*
             * DME strings must have a navaid name string to their left.
             * If DME string is bare, combine into navaid name on it's left.
             */
            for (Iterator<TextString> it = textStrings.iterator (); it.hasNext ();) {

                // see if the string is a bare DME string
                TextString tsdme = it.next ();
                if ((tsdme.words.length == 1) && (tsdme.words[0].charAt (0) == dmeMark)) {
                    int cxdme = tsdme.centerx ();
                    int cydme = tsdme.centery ();

                    // find string closest to its left
                    int bestdx = csvDpi / 3;
                    TextString tsfix = null;
                    for (TextString tsj : textStrings) {
                        if (tsj.words.length == 1) {
                            int cxfix = tsj.centerx ();
                            int cyfix = tsj.centery ();
                            int dx = cxdme - cxfix;
                            if ((dx > 0) && (Math.abs (cyfix - cydme) < 5) && (bestdx > dx)) {
                                bestdx = dx;
                                tsfix  = tsj;
                            }
                        }
                    }

                    // if found, remove dme string and merge dme string with fix string
                    if (tsfix != null) {
                        it.remove ();
                        String fix     = tsfix.words[0];
                        String dme     = tsdme.words[0];
                        tsfix.words    = new String[] { fix, dme };
                        tsfix.topritex = tsdme.topritex;
                        tsfix.topritey = tsdme.topritey;
                        tsfix.botritex = tsdme.botritex;
                        tsfix.botritey = tsdme.botritey;
                    }
                }
            }

            /*
             * Sometimes on charts are things like:
             *      (IAF)
             *    RUYON INT
             *   I-LDZ [20.7
             *      RADAR
             * So gather up the strings up into a multi-line string.
             */
            if (textStrings.size () == 0) return;
            double avgstrheight = 0;
            for (TextString ts : textStrings) {
                avgstrheight += ts.botlefty - ts.toplefty;
                ts.initBounds ();
            }
            avgstrheight /= textStrings.size ();
            while (true) {

                // on the whole chart, find two strings most closely stacked
                float      closestDistance  = csvDpi;
                TextString closestStringTop = null;
                TextString closestStringBot = null;

                for (TextString tsa : textStrings) {
                    float tsacx = (tsa.botleftx + tsa.botritex) / 2.0F;

                    for (TextString tsb : textStrings) {
                        if (tsb == tsa) continue;
                        float tsbcx = (tsb.botleftx + tsb.botritex) / 2.0F;

                        // center of text string must match center of plate fix string horizontally
                        if (Math.abs (tsbcx - tsacx) < avgstrheight * 2) {

                            // see if A is just above B
                            int distance = tsb.toplefty - tsa.botlefty;
                            if ((distance > 0) && (distance < avgstrheight * K_STRVERTCOMBINE)) {
                                closestDistance  = distance;
                                closestStringTop = tsa;
                                closestStringBot = tsb;
                            }
                        }
                    }
                }

                // stop if there is no match at all on whole plate
                if (closestStringBot == null) break;

                // merge the strings
                closestStringTop.mergeBot (closestStringBot);

                // remove bottom from list cuz it is merged into the top now
                textStrings.remove (closestStringBot);
            }
 
            /*
             * Sometimes a VOR or NDB frequency is jammed right up with the name,
             * eg, '112.7BOS', so split them apart.  But make sure it is at least
             * 3 digits so we don't split a '2' from '2B2'.
             */
            for (TextString ts : textStrings) {
                String[] words = ts.words;
                int nwords = words.length;
                for (int wi = 0; wi < nwords; wi ++) {
                    String word = ts.words[wi];
                    if ((word.charAt (0) >= '0') && (word.charAt (0) <= '9')) {
                        int wordlen = word.length ();
                        int i;
                        for (i = 0; ++ i < wordlen;) {
                            char c = word.charAt (i);
                            if (((c < '0') || (c > '9')) && (c != '.')) break;
                        }
                        if ((i < wordlen) && (i > 2)) {

                            // [0] = BOSTON
                            // [1] = 112.7BOS  <- wi
                            // [2] = VOR/DME

                            String freq = word.substring (0, i);
                            String name = word.substring (i);

                            String[] newwords = new String[nwords+1];

                            System.arraycopy (words, 0, newwords, 0, wi);
                            // [0] = BOSTON

                            newwords[wi++] = freq;
                            newwords[wi]   = name;
                            // [0] = BOSTON
                            // [1] = 112.7
                            // [2] = BOS       <- wi

                            System.arraycopy (words, wi, newwords, wi + 1, nwords - wi);
                            // [0] = BOSTON
                            // [1] = 112.7
                            // [2] = BOS       <- wi
                            // [3] = VOR/DME

                            nwords ++;
                            ts.words = words = newwords;
                        }
                    }
                }
            }

            /*
             * Handle this situation:
             *    LAMAR MILLS
             *     116.9 LAA  (LAA is in database)
             *      Chan 116
             * ...where LAMAR and/or MILLS are fix names that we don't want.
             * Example is from LAA 'IAP-VORDME RWY 36' and 7F3 'IAP-NDB RWY 36'
             *
             * But we must preserve the fix ILELE in this situation:
             *    ILELE
             *   2 NM to
             *    MABAV
             * (as on SDL 'IAP-RNAV (GPS)-E')
             *
             * ...and preserve MUNSO in this situation:
             *     LOM
             *    MUNSO
             *    385 MR  (MR is not in database)
             * (as on SNS 'IAP-LOCDME RWY 31')
             */
            for (TextString ts : textStrings) {
                String[] words = ts.words;
                int nwords = words.length;

                for (int wi = 0; ++ wi < nwords - 1;) {
                    if (ts.words[wi].matches ("^\\d+(\\.\\d+)?$") &&
                        nearDBFixes.containsKey (ts.words[wi+1])) {
                        while (-- wi >= 0) {
                            words[wi] = words[wi].toLowerCase ();
                        }
                        break;
                    }
                }
            }

            /*
             * Some strings are broken so replace with fixed versions.
             */
            for (TextString ts : textStrings) {
                String[] words = ts.words;
                int nwords = words.length;
                StringBuilder sb = new StringBuilder ();
                sb.append (faaid);
                char sep = ':';
                for (String word : ts.words) {
                    sb.append (sep);
                    sb.append (word);
                    sep = ' ';
                }
                ////System.out.println ("analyzeStrings2*: <" + sb.toString () + "> " + ts);
                String good = brokenWords.get (sb.toString ());
                if (good != null) ts.words = good.split (" ");
            }

            /*
             * Strip some noise words out.
             */
            for (TextString ts : textStrings) {
                String[] words = ts.words;
                int nwords = words.length;
                int j = 0;

                // scan the words looking for name of a nearby fix
                for (int i = 0; i < nwords; i ++) {
                    String word = words[i];

                    // skip some parenthesized keywords that can be a fix name
                    if (word.equals ("(FAF)")) continue;
                    if (word.equals ("(IAF)")) continue;
                    if (word.equals ("(IF)")) continue;

                    words[j++] = word;
                }

                if (j < nwords) {
                    String[] newwords = new String[j];
                    System.arraycopy (words, 0, newwords, 0, j);
                    ts.words = newwords;
                }
            }

            /*
             * See if the consolidated strings are useful at all.
             */
            for (TextString ts : textStrings) {
                String[] words = ts.words;
                int nwords = words.length;

                // scan the words looking for name of a nearby fix
                for (int wi = 0; wi < nwords; wi ++) {
                    String word = words[wi];

                    // sometimes there are parentheses around the fix name
                    // eg, GHM "IAP-VORDME OR GPS RWY 02"
                    while (word.startsWith ("(") && word.endsWith (")")) {
                        word = word.substring (1, word.length () - 1).trim ();
                    }

                    // if the word is a nearby known fix, save it
                    DBFix dbfix = nearDBFixes.get (word);
                    if (dbfix != null) {
                        dbfix.mentioned = true;

                        /*
                         * Don't use it if preceded by "to" as in:
                         *    2NM to
                         *    ATLIS
                         * or as in:
                         *    3000 NoPT
                         *    to MOYER
                         *
                         * Likewise with "at" as in:
                         *    Procedure NA for arrivals at NOLLI via ...
                         *
                         * Also ignore if followed by a DME string as in:
                         *    I-BVY [6
                         */
                        if (!((wi > 0) && ts.words[wi-1].equals ("to")) &&
                            !((wi > 0) && ts.words[wi-1].equals ("at")) &&
                            !((wi < nwords - 1) && (ts.words[wi+1].charAt (0) == dmeMark))) {

                            // add platefix string to list of seen platefixes
                            PlateFix platefix = new PlateFix ();
                            platefix.dbfix = dbfix;
                            platefix.text  = ts;
                            allPlateFixes.addLast (platefix);
                            break;
                        }

                        /*
                         * But we also have strings that are just 'I-BVY [6' near markers
                         * and we don't want the marker associated with anything else,
                         * so make a plate fix for it with a dummy dbfix entry (as we don't
                         * know the lat/lon of the fix).
                         */
                        if ((nwords == 2) && (wi == 0) && (ts.words[1].charAt (0) == dmeMark)) {

                            // add platefix string to list of seen platefixes
                            DMEDBFix dmefix   = new DMEDBFix ();
                            dmefix.name       = ts.words[0] + ts.words[1];
                            dmefix.type       = "dmefix";
                            dmefix.navaid     = nearDBFixes.get (ts.words[0]);
                            String[] parts    = ts.words[1].substring (1).split ("/");
                            try {
                                dmefix.distnm = Float.parseFloat (parts[0]);
                            } catch (NumberFormatException nfe) {
                                dmefix.distnm = -1;
                            }
                            if (parts.length > 1) {
                                try {
                                    double truehdgdeg = Double.parseDouble (parts[1]);
                                    dmefix.lat = Lib.LatHdgDist2Lat (dmefix.navaid.lat, truehdgdeg, dmefix.distnm);
                                    dmefix.lon = Lib.LatLonHdgDist2Lon (dmefix.navaid.lat, dmefix.navaid.lon, truehdgdeg, dmefix.distnm);
                                } catch (NumberFormatException nfe) {
                                }
                            }
                            if (parts.length > 2) {
                                dmefix.name   = ts.words[0] + dmeMark + parts[0] + '/' + parts[1];
                                dmefix.type   = parts[2];
                                dmefix.mentioned = true;
                                nearDBFixes.put (dmefix.name, dmefix);
                            }
                            PlateFix platefix = new PlateFix ();
                            platefix.dbfix    = dmefix;
                            platefix.text     = ts;
                            allPlateFixes.addLast (platefix);
                            break;
                        }
                    }
                }
            }
        }

        /**
         * Analyze all line segments seen scanning the PDF.
         * Input:
         *   allLineSegs = segments seen scanning the PDF
         * Output:
         *   smallLineSegs = small segments that might compose markers
         *   hvLineBytes = horiz & vert lines that might compose boxes that
         *                 box off parts of the chart like the descent profile
         */
        private void analyzeLineSegs ()
        {
            // scan segments
            for (LineSeg lineSeg : allLineSegs) {
                int x0 = lineSeg.x0;
                int y0 = lineSeg.y0;
                int x1 = lineSeg.x1;
                int y1 = lineSeg.y1;

                // save small line segments
                int dx = x1 - x0;
                int dy = y1 - y0;
                int distsq = dx * dx + dy * dy;
                if (distsq < csvDpi * csvDpi / 46) {
                    ////if ((x0 >= 779) && (x0 <= 829) && (y0 >= 1094) && (y0 <= 1132)) {
                    ////    System.out.println ("           new LineSeg (" + lineSeg + ")");
                    ////}
                    smallLineSegs.addLast (lineSeg);
                }

                // save non-small vertical and horizontal line segments.
                // they must be bigger than lines used in VORDMEs.
                // must be small enough to cover all the boxes around missed approach instructions and the like,
                // ...so we won't try to scan missed approach instructions and such for fixes
                // shortest line to date is on MDW 'IAP-RNAV (GPS) Z RWY 22L' MISSED APCH FIX box
                if (distsq > csvDpi * csvDpi / 50) {
                    if (Math.abs (y0 - y1) <= 1) {
                        horizLineList.add (y0);
                        horizLineList.add (y1);
                        int pwm = panelWidth - 1;
                        for (int x = Math.max (x0 - K_HVLINEGAP, 0); x <= Math.min (x1 + K_HVLINEGAP, pwm); x ++) hvLineBytes[y0*panelWidth+x] = HVLB_DRWN;
                        for (int x = Math.max (x1 - K_HVLINEGAP, 0); x <= Math.min (x0 + K_HVLINEGAP, pwm); x ++) hvLineBytes[y0*panelWidth+x] = HVLB_DRWN;
                        for (int x = Math.max (x0 - K_HVLINEGAP, 0); x <= Math.min (x1 + K_HVLINEGAP, pwm); x ++) hvLineBytes[y1*panelWidth+x] = HVLB_DRWN;
                        for (int x = Math.max (x1 - K_HVLINEGAP, 0); x <= Math.min (x0 + K_HVLINEGAP, pwm); x ++) hvLineBytes[y1*panelWidth+x] = HVLB_DRWN;
                    }
                    if (Math.abs (x0 - x1) <= 1) {
                        vertLineList.add (x0);
                        vertLineList.add (x1);
                        int phm = panelHeight - 1;
                        for (int y = Math.max (y0 - K_HVLINEGAP, 0); y <= Math.min (y1 + K_HVLINEGAP, phm); y ++) hvLineBytes[y*panelWidth+x0] = HVLB_DRWN;
                        for (int y = Math.max (y1 - K_HVLINEGAP, 0); y <= Math.min (y0 + K_HVLINEGAP, phm); y ++) hvLineBytes[y*panelWidth+x0] = HVLB_DRWN;
                        for (int y = Math.max (y0 - K_HVLINEGAP, 0); y <= Math.min (y1 + K_HVLINEGAP, phm); y ++) hvLineBytes[y*panelWidth+x1] = HVLB_DRWN;
                        for (int y = Math.max (y1 - K_HVLINEGAP, 0); y <= Math.min (y0 + K_HVLINEGAP, phm); y ++) hvLineBytes[y*panelWidth+x1] = HVLB_DRWN;
                    }
                }
            }
        }

        /**
         * Scan the plate for rectangles.  They are used for missed approach instructions
         * that include markers for the fixes which aren't to scale, so we want ignore
         * anything therein.
         */
        private void findBoxedOffAreas ()
        {
            /*
             * Sort the vertical/horizontal lines found.
             */
            int height = horizLineList.size ();
            int width  = vertLineList.size  ();
            int[] horizLineArray = new int[height];
            int[] vertLineArray  = new int[width];
            int i = 0; for (Integer y : horizLineList) horizLineArray[i++] = y;
            int j = 0; for (Integer x : vertLineList)  vertLineArray[j++]  = x;
            Arrays.sort (horizLineArray);
            Arrays.sort (vertLineArray);

            /*
             * Make a grid of rectangles whose dimensions are wherever a horizontal/vertical line was drawn.
             */
            -- height;
            -- width;
            BoxedArea[][] boxedAreaGrid = new BoxedArea[height][];
            for (int indexy = 0; indexy < height; indexy ++) {
                boxedAreaGrid[indexy] = new BoxedArea[width];
                for (int indexx = 0; indexx < width; indexx ++) {
                    BoxedArea ba  = new BoxedArea ();
                    ba.leftxcoord = vertLineArray[indexx+0];
                    ba.ritexcoord = vertLineArray[indexx+1];
                    ba.topycoord  = horizLineArray[indexy+0];
                    ba.botycoord  = horizLineArray[indexy+1];
                    boxedAreaGrid[indexy][indexx] = ba;
                }
            }

            /*
             * Make links in all four directions from one rectangle to the next.
             */
            for (int indexy = 0; indexy < height; indexy ++) {
                BoxedArea[] boxedAreaRow = boxedAreaGrid[indexy];
                for (int indexx = 0; indexx < width; indexx ++) {
                    BoxedArea ba = boxedAreaRow[indexx];
                    if (indexx > 0)          ba.leftxlink = boxedAreaGrid[indexy][indexx-1];
                    if (indexx < width  - 1) ba.ritexlink = boxedAreaGrid[indexy][indexx+1];
                    if (indexy > 0)          ba.topylink  = boxedAreaGrid[indexy-1][indexx];
                    if (indexy < height - 1) ba.botylink  = boxedAreaGrid[indexy+1][indexx];
                }
            }

            /*
             * Unlink those with drawn borders.
             */
            for (BoxedArea[] boxedAreaRow : boxedAreaGrid) {
                for (BoxedArea ba : boxedAreaRow) {
                    int topyhits = 0;
                    int botyhits = 0;
                    for (int x = ba.leftxcoord; x <= ba.ritexcoord; x ++) {
                        if (hvLineBytes[ba.topycoord*panelWidth+x] == HVLB_DRWN) topyhits ++;
                        if (hvLineBytes[ba.botycoord*panelWidth+x] == HVLB_DRWN) botyhits ++;
                    }
                    if (topyhits >= ba.ritexcoord - ba.leftxcoord) {
                        ba.topylink = null;
                    }
                    if (botyhits >= ba.ritexcoord - ba.leftxcoord) {
                        ba.botylink = null;
                    }

                    int leftxhits = 0;
                    int ritexhits = 0;
                    for (int y = ba.topycoord; y <= ba.botycoord; y ++) {
                        if (hvLineBytes[y*panelWidth+ba.leftxcoord] == HVLB_DRWN) leftxhits ++;
                        if (hvLineBytes[y*panelWidth+ba.ritexcoord] == HVLB_DRWN) ritexhits ++;
                    }
                    if (leftxhits >= ba.botycoord - ba.topycoord) {
                        ba.leftxlink = null;
                    }
                    if (ritexhits >= ba.botycoord - ba.topycoord) {
                        ba.ritexlink = null;
                    }
                }
            }

            /*
             * Find group of linked boxes with greatest total area.
             */
            int bestBoxedAreaGroupArea = 0;
            LinkedList<BoxedArea> bestBoxedAreaGroup = null;
            for (int indexy = 0; indexy < height; indexy ++) {
                BoxedArea[] boxedAreaRow = boxedAreaGrid[indexy];
                for (int indexx = 0; indexx < width; indexx ++) {
                    BoxedArea ba = boxedAreaRow[indexx];
                    if (!ba.counted) {
                        LinkedList<BoxedArea> boxedAreaGroup = new LinkedList<> ();
                        int totalarea = 0;
                        Stack<BoxedArea> stack = new Stack<> ();
                        stack.push (ba);
                        while (!stack.isEmpty ()) {
                            ba = stack.pop ();
                            if (!ba.counted) {
                                boxedAreaGroup.addLast (ba);
                                totalarea += (ba.ritexcoord - ba.leftxcoord) * (ba.botycoord - ba.topycoord);
                                ba.counted = true;
                                if ((ba.leftxlink != null) && !ba.leftxlink.counted) stack.push (ba.leftxlink);
                                if ((ba.ritexlink != null) && !ba.ritexlink.counted) stack.push (ba.ritexlink);
                                if ((ba.topylink  != null) && !ba.topylink.counted)  stack.push (ba.topylink);
                                if ((ba.botylink  != null) && !ba.botylink.counted)  stack.push (ba.botylink);
                            }
                        }
                        if (bestBoxedAreaGroupArea < totalarea) {
                            bestBoxedAreaGroupArea = totalarea;
                            bestBoxedAreaGroup = boxedAreaGroup;
                        }
                    }
                }
            }

            /*
             * Mark the pixels inside the largest area as good.
             */
            for (BoxedArea ba : bestBoxedAreaGroup) {
                for (int y = ba.topycoord; y < ba.botycoord; y ++) {
                    for (int x = ba.leftxcoord; x < ba.ritexcoord; x ++) {
                        hvLineBytes[y*panelWidth+x] = HVLB_GOOD;
                    }
                }
            }

            /*
             * Group of 16 big Beziers is an MSA circle, so block off the interior.
             */
            for (Bez16 bez16 : bigBez16s) {
                int cx  = (int) bez16.posx;
                int cy  = (int) bez16.posy;
                int rad = (int) bez16.radius;
                int rsq = rad * rad;
                for (int dy = - rad; dy <= rad; dy ++) {
                    int y = cy + dy;
                    for (int dx = - rad; dx <= rad; dx ++) {
                        int x = cx + dx;
                        if (dx * dx + dy * dy <= rsq) {
                            hvLineBytes[y*panelWidth+x] = HVLB_DRWN;
                        }
                    }
                }
            }
        }

        /**
         * See if the given pixel is in a 'good' area, ie, not in one of the blocked-off yellowed-out boxes.
         * We can't check just the center pixel itself because the center pixel may be on a horizontal or
         * vertical line such as a stepdown marker crossed line, and we want the line to be valid.
         */
        private boolean hasANearbyGoodPixel (int xc, int yc)
        {
            return containsAGoodPixel (xc - 3, xc + 3, yc - 3, yc + 3);
        }

        private boolean containsAGoodPixel (int left, int rite, int top, int bot)
        {
            for (int y = top; y <= bot; y ++) {
                if ((y >= 0) && (y < panelHeight)) {
                    for (int x = left; x <= rite; x ++) {
                        if ((x >= 0) && (x < panelWidth)) {
                            if (hvLineBytes[y*panelWidth+x] == HVLB_GOOD) return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Try to find the runway approach path line.
         * Input:
         *   allLineSegs = all line segments found on plate
         *   appCrsStrings = all tilted approach course strings (eg, "223^")
         *   hvLineBytes = blocked-off areas
         * Output:
         *   rwyCenterline  = null or centerline
         *   rwyCrossedSegs = shorter segs that are crosswise to runway
         */
        private void findRunwayCenterline ()
        {
            /*
             * Find big text string of the approach course, which shows us where the centerline is.
             */
            TextString rwyCenterBigText = null;
            double bestHeight = 0.0;
            for (TextString ts : appCrsStrings) {
                int x = (ts.botleftx + ts.botritex + ts.topritex + ts.topleftx) / 4;
                int y = (ts.botlefty + ts.botritey + ts.topritey + ts.toplefty) / 4;
                if (hasANearbyGoodPixel (x, y)) {
                    double height = ts.height ();
                    if (bestHeight < height) {
                        bestHeight = height;
                        rwyCenterBigText = ts;
                    }
                }
            }

            if (rwyCenterBigText != null) {
                LinkedList<AlignedSeg> rwyAlignedSegs = new LinkedList<> ();

                // true course of the runway (radians)
                double rwyangle = Math.atan2 (rwyCenterBigText.botritex - rwyCenterBigText.botleftx,
                        rwyCenterBigText.botlefty - rwyCenterBigText.botritey);

                // scan segments for segments bigger than arrowheads along the centerline, approx 1/8th inch
                for (LineSeg lineSeg : allLineSegs) {
                    int x0 = lineSeg.x0;
                    int y0 = lineSeg.y0;
                    int x1 = lineSeg.x1;
                    int y1 = lineSeg.y1;
                    int dx = x1 - x0;
                    int dy = y0 - y1;
                    double dist = Math.sqrt (dx * dx + dy * dy);

                    // maybe it is runway aligned or cross to runway
                    if ((dist > K_MINRWYCLLEN) && hasANearbyGoodPixel (x0, y0) && hasANearbyGoodPixel (x1, y1)) {

                        // true course of the line as plotted on the page (radians)
                        double segtc = Math.atan2 (dx, dy);

                        // differences of the courses (radians, 0..PI/2)
                        double align = AngleDiff (segtc, rwyangle);
                        if (align > Math.PI / 2.0) align = Math.PI - align;

                        // if within 3.1 degrees of runway, it is aligned with the runway
                        // it also must be near the center of the big runway approach course text
                        // GYB 'IAP-VORDME-A' centerline is 2.76 deg off course
                        if (align <= K_MAXRWYCLANG) {
                            float midx = (x0 + x1) / 2.0F;
                            float midy = (y0 + y1) / 2.0F;
                            double distfrombigtexttop = distToLine (midx, midy, rwyCenterBigText.topleftx, rwyCenterBigText.toplefty, rwyCenterBigText.topritex, rwyCenterBigText.topritey);
                            double distfrombigtextbot = distToLine (midx, midy, rwyCenterBigText.botleftx, rwyCenterBigText.botlefty, rwyCenterBigText.botritex, rwyCenterBigText.botritey);
                            if ((distfrombigtexttop < bestHeight * 0.85) && (distfrombigtextbot < bestHeight * 0.85)) {
                                addRwyAlignedSeg (rwyAlignedSegs, x0, y0, x1, y1, dist);
                            }
                        }

                        // if within 6.5 degrees of perpendicular, it is crossed to the runway
                        // MAJ 'IAP-NDB RWY 25' TIVIE is 6^
                        if (align >= Math.PI / 2 - K_MAXRWYCWANG) {
                            addRwyCrossedSeg (x0, y0, x1, y1);
                        }
                    }
                }

                /*
                 * Find line aligned with runway, near the center of the approach path string,
                 * that has the most drawn length (at least 1/6 inch).
                 */
                AlignedSeg bestAlign = null;
                double bestLength = csvDpi / 6;
                bestAlign = null;
                for (AlignedSeg aligned : rwyAlignedSegs) {
                    double drawnLen = aligned.drawn;
                    if (bestLength < drawnLen) {
                        bestLength = drawnLen;
                        bestAlign  = aligned;
                    }
                }

                if (bestAlign != null) {
                    rwyCenterline = bestAlign;
                    if (verbose) System.out.println ("centerline " + rwyCenterline);
                }
            }
        }

        /**
         * Found a line segment with same alignment as runway,
         * add to list of known aligned segments.
         */
        private static void addRwyAlignedSeg (LinkedList<AlignedSeg> rwyAlignedSegs, int x0, int y0, int x1, int y1, double newdrawn)
        {
            double newtotal = Math.hypot (x1 - x0, y0 - y1);
            double newangle = Math.atan2 (x1 - x0, y0 - y1);

            // see if co-linear with another line already in list
            for (Iterator<AlignedSeg> it = rwyAlignedSegs.iterator (); it.hasNext ();) {
                AlignedSeg seg = it.next ();
                double oldtotal = seg.length ();

                // get endpoint distances from the line under test
                // use whichever line is longer as the base and measure shorter segment's endpoints against it
                double dist0, dist1;
                if (newtotal < oldtotal) {
                    dist0 = distToLine (x0, y0, seg.x0, seg.y0, seg.x1, seg.y1);
                    dist1 = distToLine (x1, y1, seg.x0, seg.y0, seg.x1, seg.y1);
                } else {
                    dist0 = distToLine (seg.x0, seg.y0, x0, y0, x1, y1);
                    dist1 = distToLine (seg.x1, seg.y1, x0, y0, x1, y1);
                }

                // if new line outside 1.5 pixels average of the existing line, can't merge it
                if (dist0 + dist1 < 3.0) {

                    // mergeable, remove old segment from list
                    it.remove ();

                    // find combination of endpoints that makes longest segment
                    int[] xs = { x0, x1, seg.x0, seg.x1 };
                    int[] ys = { y0, y1, seg.y0, seg.y1 };
                    int bestd = 0;
                    for (int i = 0; i < 4; i ++) {
                        for (int j = i; ++ j < 4;) {
                            int dx = xs[j] - xs[i];
                            int dy = ys[j] - ys[i];
                            int d  = dx * dx + dy * dy;
                            if (bestd < d) {
                                bestd = d;
                                x0 = xs[i];
                                y0 = ys[i];
                                x1 = xs[j];
                                y1 = ys[j];
                            }
                        }
                    }

                    // add that long segment, maybe combining with others
                    addRwyAlignedSeg (rwyAlignedSegs, x0, y0, x1, y1, seg.drawn + newdrawn);
                    return;
                }
            }

            // not co-linear with existing segment, add it as a completely new segment
            AlignedSeg align = new AlignedSeg (x0, y0, x1, y1, newdrawn);
            rwyAlignedSegs.addLast (align);
        }

        /**
         * Found a line segment with cross-aligned to runway,
         * add to list of known crossed segments.
         */
        private void addRwyCrossedSeg (int x0, int y0, int x1, int y1)
        {
            // make sure segments always go same direction
            if ((x0 > x1) || ((x0 == x1) && (y0 > y1))) {
                int ss;
                ss = x0;
                x0 = x1;
                x1 = ss;
                ss = y0;
                y0 = y1;
                y1 = ss;
            }

            // see if continuous with an existing segment
            for (Iterator<LineSeg> it = rwyCrossedSegs.iterator (); it.hasNext ();) {
                LineSeg seg = it.next ();

                // look for endpoints within 2 pixels (1 x + 1 y) of each other
                // since segments only go one way, compare opposite ends
                int[] xs = new int[] { x0, x1, seg.x0, seg.x1 };
                int[] ys = new int[] { y0, y1, seg.y0, seg.y1 };
                for (int i = 0; i < 2; i ++) {
                    int j = 3 - i;
                    int dx = xs[j] - xs[i];
                    int dy = ys[j] - ys[i];
                    int distsq = dx * dx + dy * dy;
                    if (distsq <= 2) {

                        // found, remove old segment
                        it.remove ();

                        // add segment with non-near endpoints
                        // maybe longer segment merges with another segment
                        addRwyCrossedSeg (xs[i^1], ys[i^1], xs[j^1], ys[j^1]);
                        return;
                    }
                }
            }

            // if not, add it as a new cross segment
            LineSeg cross = new LineSeg (x0, y0, x1, y1);
            rwyCrossedSegs.addLast (cross);
        }

        /**
         * Given two plate fixes defining the map, map a lat/lon to bitmap X/Y
         */
        private static int latlon2X (PlateFix fi, PlateFix fj, double lat, double lon)
        {
            // true course from first given to second given
            double tcfifj = Lib.LatLonTC (fi.dbfix.lat, fi.dbfix.lon, fj.dbfix.lat, fj.dbfix.lon);

            // true course from first given to lat/lon
            double tcfill = Lib.LatLonTC (fi.dbfix.lat, fi.dbfix.lon, lat, lon);

            // naut miles from first given to second given
            double nmfifj = Lib.LatLonDist (fi.dbfix.lat, fi.dbfix.lon, fj.dbfix.lat, fj.dbfix.lon);

            // naut miles from first given to lat/lon
            double nmfill = Lib.LatLonDist (fi.dbfix.lat, fi.dbfix.lon, lat, lon);

            // pixel course from first given to second given
            double pcfifj = Math.toDegrees (Math.atan2 (fj.posx - fi.posx, fi.posy - fj.posy));

            // pixel distance from first given to second given
            double pdfifj = Math.hypot (fi.posx - fj.posx, fi.posy - fj.posy);

            // pixel course from first given to lat/lon
            double pcfill = tcfill - tcfifj + pcfifj;

            // pixel distance from first given to lat/lon
            double pdfill = nmfill / nmfifj * pdfifj;

            double x = fi.posx + pdfill * Math.sin (Math.toRadians (pcfill));
            return (int) (x + 0.5);
        }

        private static int latlon2Y (PlateFix fi, PlateFix fj, double lat, double lon)
        {
            // true course from first given to second given
            double tcfifj = Lib.LatLonTC (fi.dbfix.lat, fi.dbfix.lon, fj.dbfix.lat, fj.dbfix.lon);

            // true course from first given to lat/lon
            double tcfill = Lib.LatLonTC (fi.dbfix.lat, fi.dbfix.lon, lat, lon);

            // naut miles from first given to second given
            double nmfifj = Lib.LatLonDist (fi.dbfix.lat, fi.dbfix.lon, fj.dbfix.lat, fj.dbfix.lon);

            // naut miles from first given to lat/lon
            double nmfill = Lib.LatLonDist (fi.dbfix.lat, fi.dbfix.lon, lat, lon);

            // pixel course from first given to second given
            double pcfifj = Math.toDegrees (Math.atan2 (fj.posx - fi.posx, fi.posy - fj.posy));

            // pixel distance from first given to second given
            double pdfifj = Math.hypot (fi.posx - fj.posx, fi.posy - fj.posy);

            // pixel course from first given to lat/lon
            double pcfill = tcfill - tcfifj + pcfifj;

            // pixel distance from first given to lat/lon
            double pdfill = nmfill / nmfifj * pdfifj;

            double y = fi.posy - pdfill * Math.cos (Math.toRadians (pcfill));
            return (int) (y + 0.5);
        }

        /**
         * Try to solve plate with an actual detected DME fix and its associated navaid.
         */
        private class ActualDMEFix {
            public PlateFix besti, bestj;

            public ActualDMEFix ()
            {
                float bestdist = 0.0F;

                // find a known fix on runway centerline
                for (PlateFix knownPlateFix : foundPlateFixes) {
                    if (distToLine (knownPlateFix.posx, knownPlateFix.posy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1) < 3.0) {
                        DBFix knownDBFix = knownPlateFix.dbfix;

                        // find a dme fix on runway centerline
                        for (PlateFix dmePlateFix : foundDMEFixes) {
                            if (distToLine (dmePlateFix.posx, dmePlateFix.posy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1) < 3.0) {
                                DMEDBFix dmeDBFix = (DMEDBFix) dmePlateFix.dbfix;
                                if (dmeDBFix.navaid == knownDBFix) {

                                    // fill in dme fix with the best result
                                    navaidIsKnown (knownPlateFix, dmePlateFix);
                                    bestdist = dmeDBFix.distnm;
                                    besti    = knownPlateFix;
                                    bestj    = dmePlateFix;
                                }
                            }
                        }
                    }
                }

                // if one found, get it drawn on markedpng image
                if (bestj != null) {
                    DMEDBFix dmeDBFix = (DMEDBFix) bestj.dbfix;
                    dmeDBFix.mentioned = true;
                    dmeDBFix.makeName ();
                    nearDBFixes.put (dmeDBFix.name, dmeDBFix);
                }
            }

            /**
             * Solve when the navaid that the DME fix is based on is the same as the fix that we know the pixel position of.
             * eg, LFK 'IAP-VORDME RWY 15'
             */
            private void navaidIsKnown (PlateFix knownPlateFix, PlateFix dmePlateFix)
            {
                DBFix knownDBFix = knownPlateFix.dbfix;
                DMEDBFix dmeFix = (DMEDBFix) dmePlateFix.dbfix;

                // get on-plate true heading from navaid to dme fix by looking at the pixel locations
                double onplatehdg = Math.atan2 (dmePlateFix.posx - knownPlateFix.posx, knownPlateFix.posy - dmePlateFix.posy);

                // get true heading pilot would follow from navaid to dme fix by looking at the approach course heading
                double pilotshdg;
                if (knownDBFix.magvar != DBFix.MAGVAR_MISSING) {
                    pilotshdg = appCrsMag - knownDBFix.magvar;
                    if (AngleDiff (Math.toRadians (pilotshdg), onplatehdg) >= Math.PI / 2.0) {
                        pilotshdg += 180.0;
                    }
                } else {
                    pilotshdg = Math.toDegrees (onplatehdg);
                }

                // calc lat/lon of dme fix following that heading for the given distance
                // any difference in onplatehdg vs pilotshdg will give a rotation
                // ...such that flying the using pilotshdg will track the on-plate line
                dmeFix.lat = Lib.LatHdgDist2Lat (knownDBFix.lat, pilotshdg, dmeFix.distnm);
                dmeFix.lon = Lib.LatLonHdgDist2Lon (knownDBFix.lat, knownDBFix.lon, pilotshdg, dmeFix.distnm);
            }
        }

        /**
         * If there is a DME fix along the runway centerline with the corresponding also along the runway
         * centerline, we can compute the lat/lon of the DME fix and thus convert it to a found fix.
         */
        private void findDMEFixLatLons ()
        {
            for (Iterator<PlateFix> it = foundDMEFixes.iterator (); it.hasNext ();) {
                PlateFix dmeplatefix = it.next ();

                // make sure dme platefix is on runway centerline
                double distfromline = distToLine (dmeplatefix.posx, dmeplatefix.posy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                if (distfromline > 1.0) continue;

                // find corresponding navaid platefix
                DMEDBFix dmedbfix = (DMEDBFix) dmeplatefix.dbfix;
                DBFix navaiddbfix = dmedbfix.navaid;
                PlateFix navaidplatefix = null;
                for (PlateFix pf : foundPlateFixes) {
                    if (pf.dbfix == navaiddbfix) navaidplatefix = pf;
                }
                for (FencedPlateFix fpf : fencedPlateFixes) {
                    PlateFix pf = fpf.fencedFix;
                    if (pf.dbfix == navaiddbfix) navaidplatefix = pf;
                }
                if (navaidplatefix == null) continue;

                // navaid must be on the centerline
                distfromline = distToLine (navaidplatefix.posx, navaidplatefix.posy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                if (distfromline > 1.0) continue;

                // compute lat/lon at the distance along runway centerline from navaid
                double hdgfromnavaidtodmefix = Math.toDegrees (Math.atan2 (dmeplatefix.posx - navaidplatefix.posx, navaidplatefix.posy - dmeplatefix.posy));
                if (hdgfromnavaidtodmefix < 0.0) hdgfromnavaidtodmefix += 360.0;
                dmedbfix.lat = Lib.LatHdgDist2Lat (navaiddbfix.lat, hdgfromnavaidtodmefix, dmedbfix.distnm);
                dmedbfix.lon = Lib.LatLonHdgDist2Lon (navaiddbfix.lat, navaiddbfix.lon, hdgfromnavaidtodmefix, dmedbfix.distnm);

                // put DME fix on found plate fixes list as it has a lat/lon now
                it.remove ();
                foundPlateFixes.addLast (dmeplatefix);

                dmedbfix.makeName ();
                dmedbfix.mentioned = true;
                nearDBFixes.put (dmedbfix.name, dmedbfix);
            }
        }

        /**
         * Some approaches, such as BML 'IAP-VOR-B', have an approach path that is not
         * the same as the runway centerline so look for the different approach path
         * if we can't resolve the plate otherwise.
         */
        private void findAlternateCenterline ()
        {
            /*
             * Look for the one-and-only platefix (NDB, VOR, etc) with a cartoon bubble.
             */
            PlateFix mainPlateFix = null;
            for (PlateFix pf : foundPlateFixes) {
                if (pf.bubbled) {
                    if (mainPlateFix != null) return;
                    mainPlateFix = pf;
                }
            }
            if (mainPlateFix == null) return;

            /*
             * See what line segments we can find radiating from the platefix.
             */
            LinkedList<AlignedSeg> radialLineSegs = new LinkedList<> ();
            for (LineSeg ls : allLineSegs) {
                if (!hasANearbyGoodPixel (ls.x0, ls.y0)) continue;
                if (!hasANearbyGoodPixel (ls.x1, ls.y1)) continue;

                // get distance of the endpoints from navaid
                double dist0 = Math.hypot (ls.x0 - mainPlateFix.posx, mainPlateFix.posy - ls.y0);
                double dist1 = Math.hypot (ls.x1 - mainPlateFix.posx, mainPlateFix.posy - ls.y1);
                double length = Math.abs (dist1 - dist0);

                // only consider segments at least 1/6th inch long
                if (length > csvDpi / 6.0) {

                    // both ends of the segment should be on same heading from the navaid
                    double theta0 = Math.atan2 (ls.x0 - mainPlateFix.posx, mainPlateFix.posy - ls.y0);
                    double theta1 = Math.atan2 (ls.x1 - mainPlateFix.posx, mainPlateFix.posy - ls.y1);
                    if (AngleDiff (theta0, theta1) < Math.toRadians (1.5)) {

                        // make sure x0,y0 is closer to navaid than x1,y1
                        if (dist1 < dist0) {
                            ls = ls.reverse ();
                            double swap = dist0;
                            dist0 = dist1;
                            dist1 = swap;
                        }

                        // see if we already found a segment on this same heading from the navaid
                        AlignedSeg radialLine = null;
                        double radialDiff = Math.toRadians (1.0);
                        for (AlignedSeg rl : radialLineSegs) {
                            double t = Math.atan2 (rl.x1 - mainPlateFix.posx, mainPlateFix.posy - rl.y1);
                            double d = AngleDiff (t, theta1);
                            if (radialDiff > d) {
                                radialDiff = d;
                                radialLine = rl;
                            }
                        }

                        // if none found, create a new one
                        if (radialLine == null) {
                            radialLine = new AlignedSeg (ls.x0, ls.y0, ls.x1, ls.y1, dist1 - dist0);
                        } else {

                            // found one on same radial, add the new segment to it
                            radialLineSegs.remove (radialLine);

                            double drawn = radialLine.drawn + dist1 - dist0;

                            int rlx0 = radialLine.x0;
                            int rly0 = radialLine.y0;
                            double rldist0 = Math.hypot (rlx0 - mainPlateFix.posx, mainPlateFix.posy - rly0);
                            if (rldist0 > dist0) {
                                rlx0 = ls.x0;
                                rly0 = ls.y0;
                            }

                            int rlx1 = radialLine.x1;
                            int rly1 = radialLine.y1;
                            double rldist1 = Math.hypot (rlx1 - mainPlateFix.posx, mainPlateFix.posy - rly1);
                            if (rldist1 < dist1) {
                                rlx1 = ls.x1;
                                rly1 = ls.y1;
                            }

                            radialLine = new AlignedSeg (rlx0, rly0, rlx1, rly1, drawn);
                        }

                        radialLineSegs.addLast (radialLine);
                    }
                }
            }

            /*
             * Find the longest drawn radial line.
             * Must be at least total of 1/2 inch long.
             */
            double bestDrawn = csvDpi / 2.0;
            AlignedSeg bestRadial = null;
            for (AlignedSeg rl : radialLineSegs) {
                if (bestDrawn < rl.drawn) {
                    bestDrawn  = rl.drawn;
                    bestRadial = rl;
                }
            }

            /*
             * If one found, adpot it as the runway (approach) centerline.
             */
            if (bestRadial != null) {
                rwyCenterline = bestRadial;
                appCrsTrue = Math.toDegrees (Math.atan2 (bestRadial.x1 - mainPlateFix.posx, mainPlateFix.posy - bestRadial.y1));
                appCrsMag  = appCrsTrue + airport.magvar;
                if (verbose) System.out.println ("alternate centerline " + bestRadial + String.format (", %3.1f\u00B0 true", appCrsTrue));
            }
        }

        /**
         * Try to solve plate with a synthetic (made up) DME fix and its associated navaid.
         */
        private class SynthDMEFix {
            public PlateFix besti, bestj;

            private DBFix    mainDBFix;
            private DMEDBFix dmeDBFix;
            private PlateFix dmePlateFix;

            public SynthDMEFix ()
            {
                // look for a fix that is on the runway centerline
                // should only be one if we are this desparate
                for (PlateFix mainPlateFix : foundPlateFixes) {
                    double d = distToLine (mainPlateFix.posx, mainPlateFix.posy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                    if (d < csvDpi / 35) {
                        mainDBFix   = mainPlateFix.dbfix;
                        dmePlateFix = new PlateFix ();
                        dmeDBFix    = new DMEDBFix ();

                        // solve using midpoint on approach course
                        trySolve (mainPlateFix,
                            (rwyCenterline.x0 + rwyCenterline.x1 + 1) / 2,
                            (rwyCenterline.y0 + rwyCenterline.y1 + 1) / 2);

                        // if too close to beacon, try one end or the other,
                        // whichever is farthest from beacon
                        if (dmeDBFix.distnm < 3) {
                            double dfb0 = trySolve (mainPlateFix, rwyCenterline.x0, rwyCenterline.y0);
                            double dfb1 = trySolve (mainPlateFix, rwyCenterline.x1, rwyCenterline.y1);
                            if (dfb0 > dfb1) {
                                trySolve (mainPlateFix, rwyCenterline.x0, rwyCenterline.y0);
                            }
                        }

                        // link the on-plate pixel x,y to the lat/lon
                        dmePlateFix.dbfix = dmeDBFix;

                        // set up these two fixes as the solution
                        besti = mainPlateFix;
                        bestj = dmePlateFix;
                        nearDBFixes.put (dmeDBFix.name, dmeDBFix);

                        if (verbose) System.out.println ("synth dme fix " + dmeDBFix.name + " at " + dmeDBFix.lat + " " + dmeDBFix.lon);
                        break;
                    }
                }
            }

            /**
             * Compute DME fix values for the given x,y pixel based on the given navaid.
             * @param mainPlateFix = navaid pixelx,y and lat/lon
             * @param posx,posy = proposed pixelx,y for DME fix
             * @returns with DME fix pixelx,y and lat/lon filled in
             */
            private double trySolve (PlateFix mainPlateFix, int posx, int posy)
            {
                // get pixel distance between the navaid and the proposed DME fix position
                double pixdist = Math.hypot (posx - mainPlateFix.posx, mainPlateFix.posy - posy);

                // assuming normal scale factor, get corresponding naut mile distance
                // between the navaid and the proposed DME fix position
                double nmdist = pixdist / csvDpi * K_ONEFIXSCALE * 25.4 / 1000.0 / 1852.0;

                // round to nearest tenth so it looks like a DME fix on the plate
                nmdist = Math.round (nmdist * 10.0) / 10.0;

                // get pixel heading from navaid to proposed DME fix position
                double hdgtruerad  = Math.atan2 (posx - mainPlateFix.posx, mainPlateFix.posy - posy);
                double hdgtruedeg  = Math.toDegrees (hdgtruerad);

                // compute lat,lon of the synthetic dme fix
                // save other info needed for a database fix
                dmeDBFix.lat       = Lib.LatHdgDist2Lat (mainDBFix.lat, hdgtruedeg, nmdist);
                dmeDBFix.lon       = Lib.LatLonHdgDist2Lon (mainDBFix.lat, mainDBFix.lon, hdgtruedeg, nmdist);
                dmeDBFix.distnm    = (float) nmdist;
                dmeDBFix.mentioned = true;
                dmeDBFix.navaid    = mainDBFix;
                dmeDBFix.makeName ();
                nearDBFixes.put (dmeDBFix.name, dmeDBFix);

                // recompute pixel based on final distance and heading
                pixdist = nmdist * csvDpi / K_ONEFIXSCALE / 25.4 * 1000.0 * 1852.0;
                dmePlateFix.posx = mainPlateFix.posx + (int) Math.round (pixdist * Math.sin (hdgtruerad));
                dmePlateFix.posy = mainPlateFix.posy - (int) Math.round (pixdist * Math.cos (hdgtruerad));

                // distance between fix and beacon
                return nmdist;
            }
        }

        /**
         * Calculate the difference in angle between the pixel course line and the latlon
         * course line (absolute value degrees, 0..180).
         */
        private static double CalcAngleDiff (PlateFix platefixi, PlateFix platefixj)
        {
            int dx = platefixj.posx - platefixi.posx;
            int dy = platefixi.posy - platefixj.posy;
            if ((dx == 0) && (dy == 0)) return 999.0;

            // calc amount to rotate by (see PlateView.PlateImage.UpdateIAPGeoRefs())
            double avglatcos = Math.cos (Math.toRadians (airport.lat));
            double de    = (platefixj.dbfix.lon - platefixi.dbfix.lon) * avglatcos;
            double dn    = platefixj.dbfix.lat - platefixi.dbfix.lat;
            double tcxy  = Math.atan2 (dx, dy);
            double tcen  = Math.atan2 (de, dn);

            return Math.toDegrees (AngleDiff (tcxy, tcen));
        }

        /**
         * Calculate scale factor of mapping the two fixes.
         */
        private static double CalcScaleFactor (PlateFix platefixi, PlateFix platefixj)
        {
            int distx = platefixj.posx - platefixi.posx;
            int disty = platefixi.posy - platefixj.posy;
            double xynm = Math.hypot (distx, disty);
            if (xynm < 2.0) return 0.0;
            xynm *= 25.4 / csvDpi / 1000.0 / 1852.0;

            double llnm = Lib.LatLonDist (platefixi.dbfix.lat, platefixi.dbfix.lon, platefixj.dbfix.lat, platefixj.dbfix.lon);
            return llnm / xynm;
        }

        /**
         *  PVD VORTAC on EWB ILS 5 plate
         *
         *       G             D
         *      H AAAAAAAAAAAAA E
         *     H                 E
         *    H                   E
         *    IC                 BF
         *      C               B
         *       C             B
         *        C           B
         *         C         B
         *          JJJJJJJJJ
         *                  K
         *          LLLLLLLLL
         */
        private final static String[] marker_types_vortac = { "VOR", "VOR/DME", "VORTAC" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] vortacShape = new LineSeg[] {
            new LineSeg ( 98,708, 112,708),    // A
            new LineSeg (119,720, 112,732),    // B
            new LineSeg ( 91,720,  98,732),    // C
            new LineSeg (112,708, 116,706),    // D
            new LineSeg (116,706, 124,718),    // E
            new LineSeg (124,718, 119,720),    // F
            new LineSeg ( 98,708,  94,706),    // G
            new LineSeg ( 94,706,  86,718),    // H
            new LineSeg ( 86,718,  91,720),    // I
            new LineSeg ( 98,732, 112,732),    // J
            new LineSeg (112,732, 112,738),    // K
            new LineSeg (112,738,  98,738)     // L
        };

        /**
         * VOR/DMEs are a rectangle with an enclosed hexagon.
         * The rectangle is drawn then angle brackes on left and right sides are drawn.
         *
         * The prototype points are taken from MHT VORDME in upper left corner of BVY LOC RWY 16.
         */
        private final static String[] marker_types_vordme = { "VOR", "VOR/DME", "VORTAC" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] vordmeShape = new LineSeg[] {
            new LineSeg (118,638, 156,638),    // bottom horizontal line
            new LineSeg (156,638, 156,607),    // right vertical line
            new LineSeg (156,607, 118,607),    // top horizontal line
            new LineSeg (118,607, 118,638),    // left vertical line
            new LineSeg (127,607, 118,623),    // top-half left angle bracket
            new LineSeg (118,623, 128,638),    // bottom-half left angle bracket
            new LineSeg (147,607, 156,623),    // top-half right angle bracket
            new LineSeg (156,623, 146,638)     // bottom-half right angle bracket
        };
        // 74V VOR A
        private final static LineSeg[] vordmeShape2 = new LineSeg[] {
            new LineSeg (823, 976, 786, 976),
            new LineSeg (786, 976, 786,1008),
            new LineSeg (786,1008, 823,1008),
            new LineSeg (823,1008, 823, 976),
            new LineSeg (814,1008, 823, 992),
            new LineSeg (823, 992, 813, 976),
            new LineSeg (794,1008, 786, 992),
            new LineSeg (786, 992, 796, 976)
        };
        // VCT "IAP-VOR RWY 13L"
        private final static LineSeg[] vordmeShape3 = new LineSeg[] {
            new LineSeg (826,1132, 825,1100),   // right side
            new LineSeg (825,1100, 787,1101),   // top side
            new LineSeg (787,1101, 788,1133),   // left side
            new LineSeg (788,1133, 826,1132),   // bottom side
            new LineSeg (796,1101, 788,1117),   // upper left angle
            new LineSeg (788,1117, 799,1133),   // lower left angle
            new LineSeg (816,1100, 826,1116),   // upper right angle
            new LineSeg (826,1116, 816,1132)    // lower right angle
        };

        /**
         * Triangles used to locate airway fixes on plates.
         * Prototype taken from WITCH on BVY LOC RWY 16.
         */
        private final static String[] marker_types_triangle = { "REP-PT" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] triangleShape = new LineSeg[] {
            new LineSeg (1093,1117, 1069,1117),    // base
            new LineSeg (1069,1117, 1081,1097),    // left side
            new LineSeg (1081,1097, 1093,1117)     // right side
        };

        /**
         * Plain VORs depicted by a simple hexagon.
         * Prototype taken from FDK on GAI VOR 14 plate.
         */
        private final static String[] marker_types_vor = { "VOR", "VOR/DME", "VORTAC" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] vorShape = new LineSeg[] {
            new LineSeg (425,760, 441,760),    // bottom edge
            new LineSeg (441,760, 450,746),    // lower right edge
            new LineSeg (450,746, 442,732),    // upper right edge
            new LineSeg (442,732, 424,732),    // top edge
            new LineSeg (424,732, 416,746),    // upper left edge
            new LineSeg (416,746, 425,760)     // lower left edge
        };
        // VQQ "IAP-VOR RWY 09R" vor VQQ
        private final static LineSeg[] vorShape2 = new LineSeg[] {
            new LineSeg (813,1087, 797,1087),
            new LineSeg (797,1087, 788,1100),
            new LineSeg (788,1100, 796,1115),
            new LineSeg (796,1115, 814,1115),
            new LineSeg (814,1115, 822,1100),
            new LineSeg (822,1100, 813,1087)
        };

        /**
         * Squares with a circle inside.
         * Prototype taked from I-TV on TVL 'IAP-LDADME-2 RWY 18'
         */
        private final static String[] marker_types_localizer = { "ILS", "ILS/DME", "LDA", "LDA/DME",
                "LOC/DME", "LOC/GS", "LOCALIZER", "SDF", "SDF/DME" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] localizerShape = new LineSeg[] {
            new LineSeg (848,1567, 824,1567),
            new LineSeg (824,1567, 824,1591),
            new LineSeg (824,1591, 848,1591),
            new LineSeg (848,1591, 848,1567)
        };
        /* beziers along with it...
            new LineSeg (848,1579 844,1571),
            new LineSeg (844,1571 836,1568),
            new LineSeg (836,1568 828,1571),
            new LineSeg (828,1571 824,1579),
            new LineSeg (824,1579 828,1588),
            new LineSeg (828,1588 836,1591),
            new LineSeg (836,1591 844,1588),
            new LineSeg (844,1588 848,1579)
        */

        /**
         * Threshold marker.
         * Prototype taken from GCD on GPS 09 plate, fix WIXOX.
         */
        private final static String[] marker_types_thresh = { "RNAV-WP", "RUNWAY" };
        //TODO depends on csvDpi=300
        private final static LineSeg[] threshShape = new LineSeg[] {
            new LineSeg (1086,1114, 1092,1113),
            new LineSeg (1097,1109, 1101,1104),
            new LineSeg (1103,1104, 1107,1109),
            new LineSeg (1102,1108, 1097,1110),
            new LineSeg (1096,1114, 1096,1117),
            new LineSeg (1097,1119, 1092,1116),
            new LineSeg (1107,1109, 1112,1113),
            new LineSeg (1118,1114, 1112,1115),
            new LineSeg (1107,1119, 1103,1124),
            new LineSeg (1102,1130, 1101,1124),
            new LineSeg (1097,1119, 1102,1121),
            new LineSeg (1106,1119, 1108,1114),
            new LineSeg (1108,1112, 1106,1110),
            new LineSeg (1113,1103, 1102,1098),
            new LineSeg (1086,1114, 1090,1126),
            new LineSeg (1102,1130, 1113,1126),
            new LineSeg (1113,1126, 1118,1114)
        };

        /**
         * Find all instances of a given shape among all the small line segments.
         * Enter them on the markers list.
         * @param smalls = small line segments to search
         * @param disps = prototype line segments required to make the shape
         * @param marker_types = what kinds of dbfixes it can match
         */
        private void findSmallLineShapes (LinkedList<LineSeg> smalls, LineSeg[] disps, String[] marker_types)
        {
            /*
             * The average of all the displacement points is the center of the shape.
             */
            int minx = 999999999;
            int miny = 999999999;
            int maxx = 0;
            int maxy = 0;
            for (LineSeg disp : disps) {
                if (minx > disp.x0) minx = disp.x0;
                if (miny > disp.y0) miny = disp.y0;
                if (minx > disp.x1) minx = disp.x1;
                if (miny > disp.y1) miny = disp.y1;
                if (maxx < disp.x0) maxx = disp.x0;
                if (maxy < disp.y0) maxy = disp.y0;
                if (maxx < disp.x1) maxx = disp.x1;
                if (maxy < disp.y1) maxy = disp.y1;
            }
            int avgx = (minx + maxx) / 2;
            int avgy = (miny + maxy) / 2;

            int diagsq = (maxx - minx) * (maxx - minx) + (maxy - miny) * (maxy - miny);

            /*
             * Relocate prototype segments to be displacements from that center.
             */
            if ((avgx != 0) || (avgy != 0)) {
                for (int i = disps.length; -- i >= 0;) {
                    LineSeg disp = disps[i];
                    disps[i] = new LineSeg (
                            disp.x0 - avgx,
                            disp.y0 - avgy,
                            disp.x1 - avgx,
                            disp.y1 - avgy);
                }
            }

            /*
             * Get mask of required segments and length of initial segment.
             */
            int done    = (1 << disps.length) - 1;
            int firstdx = disps[0].x1 - disps[0].x0;
            int firstdy = disps[0].y1 - disps[0].y0;

            /*
             * Scan through whole list looking for all instances of the shape.
             */
            for (LineSeg slsi : smalls) {
                int centerx = 0;  // point in this instance mapping to avgx,y
                int centery = 0;
                int prog    = 0;  // don't have any matching segments yet

                /*
                 * See if this segment meets criteria for first segment of the shape.
                 */

                // assuming the segment goes in same direction as disps[0],
                // see how much different it is from disps[0]
                int fwddx = Math.abs (slsi.x1 - slsi.x0 - firstdx);
                int fwddy = Math.abs (slsi.y1 - slsi.y0 - firstdy);

                // assuming the segment goes in opposite direction,
                // see how much different it is from reversed disps[0].
                int revdx = Math.abs (slsi.x0 - slsi.x1 - firstdx);
                int revdy = Math.abs (slsi.y0 - slsi.y1 - firstdy);

                if ((fwddx <= 1) && (fwddy <= 1)) {
                    centerx = slsi.x0 - disps[0].x0;
                    centery = slsi.y0 - disps[0].y0;
                    prog    = 1;
                } else if ((revdx <= 1) && (revdy <= 1)) {
                    centerx = slsi.x1 - disps[0].x0;
                    centery = slsi.y1 - disps[0].y0;
                    prog    = 1;
                }

                /*
                 * If this segment can be the first segment of the shape,
                 * look for all the others relative to centerx,y which
                 * corresponds to avgx,y.
                 */
                if (prog > 0) {
                    LinkedList<LineSeg> foundSegments = new LinkedList<> ();
                    foundSegments.addLast (slsi);

                    for (LineSeg slsj : smalls) {

                        // get segment endpoints relative to shape's center
                        int x0 = slsj.x0 - centerx;
                        int y0 = slsj.y0 - centery;
                        int x1 = slsj.x1 - centerx;
                        int y1 = slsj.y1 - centery;

                        // skip segments that are too far away (optimization)
                        //   diagsq = (diagonal across prototype) ** 2
                        //     and the diagonal is twice the distance from the center to any segment endpoint
                        // so if an endpoint of this segment is outside that, the segment can't possibly match
                        if (x0 * x0 + y0 * y0 > diagsq) continue;

                        // scan through list of required segments relative to shape's center
                        int mask = 1;
                        for (LineSeg disp : disps) {
                            if ((prog & mask) == 0) {

                                // assuming segment goes in same direction as disp,
                                // see how far off the endpoints are
                                int fx0 = Math.abs (disp.x0 - x0);
                                int fy0 = Math.abs (disp.y0 - y0);
                                int fx1 = Math.abs (disp.x1 - x1);
                                int fy1 = Math.abs (disp.y1 - y1);

                                // assuming segment goes in opposite direction,
                                // see how far off the endpoints are
                                int rx0 = Math.abs (disp.x0 - x1);
                                int ry0 = Math.abs (disp.y0 - y1);
                                int rx1 = Math.abs (disp.x1 - x0);
                                int ry1 = Math.abs (disp.y1 - y0);

                                // if there is a match, flag this displacement as matched
                                if (((fx0 <= 1) && (fy0 <= 1) && (fx1 <= 1) && (fy1 <= 1)) ||
                                    ((rx0 <= 1) && (ry0 <= 1) && (rx1 <= 1) && (ry1 <= 1))) {
                                    foundSegments.addLast (slsj);
                                    prog |= mask;
                                    if (prog == done) break;
                                }
                            }
                            mask <<= 1;
                        }
                        if (prog == done) break;
                    }

                    /*
                     * If we found all the segments, make a marker out of the shape.
                     */
                    if (prog == done) {
                        Marker marker = new Marker (centerx, centery);
                        marker.types = marker_types;
                        marker.linesegs = foundSegments;
                        markers.addLast (marker);
                    }
                }
            }
        }

        /**
         * Group ndbCenters and ndbDots into markers.
         */
        private void findNDBShapes ()
        {
            int maxradsq = csvDpi * csvDpi / 64;

            int mindots = 65;
            int maxdots = 75;

            for (Marker center : ndbCenters) {
                int centerx = center.centx;
                int centery = center.centy;

                // scan through all the dots and count those close by
                int nFound = 0;
                for (Point dot : ndbDots) {
                    int dotx = dot.x;
                    int doty = dot.y;

                    int dx = dotx - centerx;
                    int dy = doty - centery;
                    int radsq = dx * dx + dy * dy;

                    if (radsq <= maxradsq) {
                        center.linesegs.addLast (new LineSeg (dotx, doty, dotx, doty));
                        nFound ++;
                    }
                }

                // sometimes the NDB shape is overdrawn
                // eg, DNS 'IAP-NDB RWY 30'
                nFound /= center.instances;

                // if we found enough nearby dots, the center is an NDB
                if ((nFound >= mindots) && (nFound <= maxdots)) {
                    markers.add (center);
                }
            }
        }

        /**
         * Make markers where there are little lines drawn perpendicular to runway centerline.
         */
        private void findRwyStepDowns ()
        {
            /*
             * Check for runway centerline found.
             * If one was found, make markers out of where all the cross-aligned
             * segments intersect the aligned segment.
             */
            if (rwyCenterline != null) {

                /*
                 * Build list of cross-aligned segments that are centered on the centerline.
                 */
                LinkedList<LineSeg> crosses = new LinkedList<> ();
                for (LineSeg cross : rwyCrossedSegs) {
                    if (cross.length () > csvDpi / 6.0) {
                        int midx = (cross.x0 + cross.x1) / 2;
                        int midy = (cross.y0 + cross.y1) / 2;
                        double dist = distToLine (midx, midy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                        if (dist < K_STEPDOWNSLOP) {
                            int x = lineIntersectX (rwyCenterline, cross);
                            int y = lineIntersectY (rwyCenterline, cross);
                            Marker m   = new Marker (x, y);
                            m.linesegs = new LinkedList<> ();
                            m.types    = marker_types_stepdown;
                            m.linesegs.addLast (cross);
                            markers.addLast (m);
                        }
                    }
                }
            }
        }

        /**
         * Map x0,y0=>0,0 and x1,y1=>1,0 using rotation, scaling, translation
         */
        private static double[] fenceCompute (double x0, double y0, double x1, double y1)
        {
            double[][] mat = new double[][] {
                //             m0  m1  m2  m3  m4  m5   =
                new double[] { x0,  0, y0,  0,  1,  0,  0 },  // x0 * m0 + y0 * m2 + m4 = 0
                new double[] {  0, x0,  0, y0,  0,  1,  0 },  // x0 * m1 + y0 * m3 + m5 = 0
                new double[] { x1,  0, y1,  0,  1,  0,  1 },  // x1 * m0 + y1 * m2 + m4 = 1
                new double[] {  0, x1,  0, y1,  0,  1,  0 },  // x1 * m1 + y1 * m3 + m5 = 0
                new double[] {  1,  0,  0, -1,  0,  0,  0 },  // m0 - m3 = 0
                new double[] {  0,  1,  1,  0,  0,  0,  0 }   // m1 + m2 = 0
            };

            Lib.RowReduce (mat);

            for (int i = 0; i < 6; i ++) mat[0][i] = mat[i][6];
            return mat[0];
        }
        private static double fenceXformX (double[] m, double x, double y)
        {
            return m[0] * x + m[2] * y + m[4];
        }
        private static double fenceXformY (double[] m, double x, double y)
        {
            return m[1] * x + m[3] * y + m[5];
        }

        /**
         * Scan the plate for rectangles.  They are used for missed approach instructions
         * that include markers for the fixes which aren't to scale, so we want to delete
         * the enclosed markers.
         */
        private void deleteBoxedMarkers ()
        {
            /*
             * Delete any markers in the blocked off areas.
             */
            for (Iterator<Marker> it = markers.iterator (); it.hasNext ();) {
                Marker m = it.next ();
                if (!hasANearbyGoodPixel (m.centx, m.centy)) {
                    it.remove ();
                }
            }
        }

        /**
         * Scan for lightning bolts.
         * Three line segments with at least one open end in correct shape.
         * Note that they can be grayscaled so can't use openLoops,
         * eg, FDK 'IAP-ILS OR LOC RWY 23'.
         */
        private void findLightningBolts ()
        {
            /*
             * Get line segments at least as long as needed.
             */
            LinkedList<LineSeg> lbsegs = new LinkedList<> ();
            for (LineSeg ls : allLineSegs) {
                if (ls.length () > csvDpi / 20.0) {
                    lbsegs.addLast (ls);
                }
            }

            /*
             * From those, get triplets that make up a lightning bolt.
             */
            litnBolts = new LinkedList<> ();
            LitnBoltScanner lbs = new LitnBoltScanner ();
            while (!lbsegs.isEmpty ()) {
                LineSeg ls = lbsegs.removeFirst ();
                lbs.reset (ls);
                boolean added;
                do {
                    added = false;
                    for (Iterator<LineSeg> it = lbsegs.iterator (); it.hasNext ();) {
                        ls = it.next ();
                        if (lbs.tryToAddSegment (ls)) {
                            it.remove ();
                            added = true;
                        }
                    }
                } while (added);
                LineSeg lb = lbs.getBolt ();
                if (lb != null) litnBolts.addLast (lb);
            }
        }

        private static class LitnBoltScanner {
            private boolean begopen;
            private boolean endopen;
            private int nSegs;      // number of segments so far
            private LineSeg begseg; // x0,y0 = one end of the bolt
            private LineSeg endseg; // x1,y1 = other end of the bolt

            public void reset (LineSeg ls)
            {
                endopen = begopen = true;
                endseg  = begseg  = ls;
                nSegs   = 1;
            }

            /**
             * See if line segment is part of this lightning bolt and add it in if so.
             */
            public boolean tryToAddSegment (LineSeg addseg)
            {
                /*
                 * Maybe swap segment so it matches up with beginning segment.
                 */
                if (distSqBtwnPts (addseg.x0, addseg.y0, begseg.x0, begseg.y0) < 3) {
                    addseg = addseg.reverse ();
                }

                /*
                 * See if it matches up with beginning segment.
                 */
                if (distSqBtwnPts (addseg.x1, addseg.y1, begseg.x0, begseg.y0) < 3) {

                    /*
                     * Adjacent segments have a sharp angle.
                     * Also, there are no more than 3 segments to a lightning bolt.
                     */
                    double ad = anglediff (begseg, addseg);
                    if ((nSegs < 3) && (ad > K_LITNBOLTFOLDMIN) && (ad < K_LITNBOLTFOLDMAX)) {
                        begseg = addseg;
                        nSegs ++;
                        return true;
                    }

                    /*
                     * Matches but is not part of the lightning bolt,
                     * so this end of the lightning bolt is closed.
                     */
                    begopen = false;
                    return false;
                }

                /*
                 * Maybe swap ends so it matches up with end segment.
                 */
                if (distSqBtwnPts (addseg.x1, addseg.y1, endseg.x1, endseg.y1) < 3) {
                    addseg = addseg.reverse ();
                }

                /*
                 * See if it matches up with end segment.
                 */
                if (distSqBtwnPts (addseg.x0, addseg.y0, endseg.x1, endseg.y1) < 3) {

                    /*
                     * Adjacent segments have a sharp angle.
                     * Also, there are no more than 3 segments to a lightning bolt.
                     */
                    double ad = anglediff (endseg, addseg);
                    if ((nSegs < 3) && (ad > K_LITNBOLTFOLDMIN) && (ad < K_LITNBOLTFOLDMAX)) {
                        endseg = addseg;
                        nSegs ++;
                        return true;
                    }

                    /*
                     * Matches but is not part of the lightning bolt,
                     * so this end of the lightning bolt is closed.
                     */
                    endopen = false;
                    return false;
                }

                /*
                 * Doesn't match at all.
                 */
                return false;
            }

            /**
             * Get ends of the resulting lightning bolt.
             */
            public LineSeg getBolt ()
            {
                if ((nSegs < 3) || (!begopen && !endopen)) return null;
                if (anglediff (begseg, endseg) > K_LITNBOLTEND2END) return null;

                // isn't really a lightning bolt if it completely doubles back on itself
                // this can happen in the line-drawn lettering of a fix name and be confusing
                // eg, WAMAK in ECS 'IAP-RNAV (GPS) RWY 32' for the MSA semi-circles
                double s1len = Math.hypot (begseg.x0 - begseg.x1, begseg.y0 - begseg.y1);
                double s2len = Math.hypot (endseg.x0 - begseg.x1, endseg.y0 - begseg.y1);
                double s3len = Math.hypot (endseg.x0 - endseg.x1, endseg.y0 - endseg.y1);
                if (s2len > (s1len + s3len) / 2 * 0.75) return null;

                // ok, return segment going from one end to the other
                return new LineSeg (begseg.x0, begseg.y0, endseg.x1, endseg.y1);
            }

            /**
             * See if the two directed line segments form a sharp angle or not.
             * @param firstseg = one line segment
             * @param secondseg = other line segment
             * @returns number of radians turned when walking from one segment to the other (0..PI)
             */
            private static double anglediff (LineSeg firstseg, LineSeg secondseg)
            {
                // get heading of the segments on the page
                // - a = heading when walking from firstseg.p0 to firstseg.p1
                // - b = heading when walking from secondseg.p0 to secondseg.p1
                double a = Math.atan2 (firstseg.x1  - firstseg.x0,  firstseg.y0  - firstseg.y1);
                double b = Math.atan2 (secondseg.x1 - secondseg.x0, secondseg.y0 - secondseg.y1);

                // get difference of angles
                // a difference of -1^ is the same as a difference of 1^
                double d = Math.abs (a - b);

                // a difference of 359^ is the same as a difference of 1^
                if (d >= Math.PI) d = Math.PI * 2 - d;

                return d;
            }
        }

        /**
         * Match live plate fix strings to corresponding marker.
         */
        private void matchPlateFixesToMarkers ()
        {
            // some markers have a lightning bolt pointing to them
            // if so, we look for the string nearest the other end of the lightning bolt
            // the gap is set at csvDpi/25.0 so MIT IAP-VOR-A works at the SCRAP fix

            for (Marker marker : markers) {
                int lookx = marker.centx;
                int looky = marker.centy;

                double  nearestLitnBoltDist = csvDpi / 25.0;
                LineSeg nearestLitnBoltObj  = null;
                for (LineSeg litnBolt : litnBolts) {
                    for (LineSeg markerLineSeg : marker.linesegs) {
                        double dist0 = distToLineSeg (litnBolt.x0, litnBolt.y0, markerLineSeg.x0, markerLineSeg.y0, markerLineSeg.x1, markerLineSeg.y1);
                        if (nearestLitnBoltDist > dist0) {
                            nearestLitnBoltDist = dist0;
                            nearestLitnBoltObj  = litnBolt;
                            lookx = litnBolt.x1;
                            looky = litnBolt.y1;
                        }
                        double dist1 = distToLineSeg (litnBolt.x1, litnBolt.y1, markerLineSeg.x0, markerLineSeg.y0, markerLineSeg.x1, markerLineSeg.y1);
                        if (nearestLitnBoltDist > dist1) {
                            nearestLitnBoltDist = dist1;
                            nearestLitnBoltObj  = litnBolt;
                            lookx = litnBolt.x0;
                            looky = litnBolt.y0;
                        }
                    }
                }

                // if we found a nearby lightning bolt, set up to look for text at the far end of lightning bolt
                // and remember the length of the gap between the near end of the bolt and the marker
                // (the larger the gap the less the quality of the marker)
                if (nearestLitnBoltObj != null) {
                    litnBolts.remove (nearestLitnBoltObj);
                    marker.quality = nearestLitnBoltDist;
                }

                marker.lookx = lookx;
                marker.looky = looky;
            }

            // if there are any remaining lightning bolts that touch the runway centerline,
            // make a marker at the intersection point

            if (rwyCenterline != null) {
                for (Iterator<LineSeg> it = litnBolts.iterator (); it.hasNext ();) {
                    LineSeg litnBolt = it.next ();

                    // see if end 0 is within 4 pixels of the runway centerline
                    // if so, make a marker with centerpoint at the itersection
                    // and look for a platefix string at the other end
                    double dist0 = distToLineSeg (litnBolt.x0, litnBolt.y0, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                    if (dist0 < 4) {
                        double x = lineIntersectX (litnBolt.x0, litnBolt.y0, litnBolt.x1, litnBolt.y1, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                        double y = lineIntersectY (litnBolt.x0, litnBolt.y0, litnBolt.x1, litnBolt.y1, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                        Marker marker  = new Marker ((int) (x + 0.5), (int) (y + 0.5));
                        marker.lookx   = litnBolt.x1;
                        marker.looky   = litnBolt.y1;
                        marker.types   = marker_types_stepdown;
                        marker.quality = dist0;
                        markers.addLast (marker);
                        it.remove ();
                        if (verbose) System.out.println ("marker " + marker);
                        continue;
                    }

                    // see if end 1 is within 4 pixels of the runway centerline
                    // if so, make a marker with centerpoint at the itersection
                    // and look for a platefix string at the other end
                    double dist1 = distToLineSeg (litnBolt.x1, litnBolt.y1, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                    if (dist1 < 4) {
                        double x = lineIntersectX (litnBolt.x0, litnBolt.y0, litnBolt.x1, litnBolt.y1, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                        double y = lineIntersectY (litnBolt.x0, litnBolt.y0, litnBolt.x1, litnBolt.y1, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                        Marker marker  = new Marker ((int) (x + 0.5), (int) (y + 0.5));
                        marker.lookx   = litnBolt.x0;
                        marker.looky   = litnBolt.y0;
                        marker.types   = marker_types_stepdown;
                        marker.quality = dist1;
                        markers.addLast (marker);
                        it.remove ();
                        if (verbose) System.out.println ("marker " + marker);
                        continue;
                    }
                }
            }

            // if there are any remaining lightning bolts that touch two other lines,
            // make a marker at the intersection point

            for (Iterator<LineSeg> it = litnBolts.iterator (); it.hasNext ();) {
                LineSeg litnBolt = it.next ();

                if (intersectsTwoSegments (litnBolt.x0, litnBolt.y0)) {
                    Marker marker  = new Marker (litnBolt.x0, litnBolt.y0);
                    marker.lookx   = litnBolt.x1;
                    marker.looky   = litnBolt.y1;
                    marker.types   = marker_types_stepdown;
                    markers.addLast (marker);
                    it.remove ();
                    if (verbose) System.out.println ("marker " + marker);
                    continue;
                }

                if (intersectsTwoSegments (litnBolt.x1, litnBolt.y1)) {
                    Marker marker  = new Marker (litnBolt.x1, litnBolt.y1);
                    marker.lookx   = litnBolt.x0;
                    marker.looky   = litnBolt.y0;
                    marker.types   = marker_types_stepdown;
                    markers.addLast (marker);
                    it.remove ();
                    if (verbose) System.out.println ("marker " + marker);
                    continue;
                }
            }

            // sometimes there are single line segments from a platefix string
            // to the runway centerline that function just like a lightning bolt
            // eg, T82 IAP-VORDME-A
            // but be sure not to connect to a cartoon bubble'd platefix string
            // as in SBY 'IAP-VOR RWY 32'
            for (LinkedList<SegNode> loop : openLoops) {
                SegNode a, b;
                a = loop.get (0);
                b = loop.get (1);
                checkSingleLineBolt (a, b);
                int n = loop.size ();
                a = loop.get (n - 1);
                b = loop.get (n - 2);
                checkSingleLineBolt (a, b);
            }

            // now match platefixes to markers

            while (true) {

                // find the fix/marker that are closest to each other on the whole plate
                // but no more than 1/2 inch apart

                double bestdist       = csvDpi / 2.0;
                int bestlbposx        = 0;
                int bestlbposy        = 0;
                Marker bestmarker     = null;
                PlateFix bestplatefix = null;

                for (PlateFix platefix : livePlateFixes) {

                    // markers (such as VOR shape or NDB shape) possibly with lightning bolt attached

                    for (Marker marker : markers) {
                        boolean typeok = false;
                        for (String mt : marker.types) {
                            typeok |= mt.equals (platefix.dbfix.type);
                        }
                        if (!typeok) continue;

                        double dist = platefix.text.distOfPoint (marker.lookx, marker.looky);
                        if (bestdist > dist) {
                            bestdist     = dist;
                            bestmarker   = marker;
                            bestplatefix = platefix;
                        }
                    }
                }

                if (bestmarker == null) break;

                // maybe dump a table of the matching matrix
                // mark the current best choice with <>

                if (false) {
                    System.out.println ("matchPlateFixesToMarkers*: -----------------");
                    System.out.print ("matchPlateFixesToMarkers*: ");
                    System.out.print (PadRight ("", 5));
                    for (Marker marker : markers) {
                        System.out.print (PadLeft (marker.lookx + "," + marker.looky, 12));
                    }
                    System.out.println ("");

                    for (PlateFix platefix : livePlateFixes) {
                        System.out.print ("matchPlateFixesToMarkers*: ");
                        System.out.print (PadRight (platefix.dbfix.name, 5));
                        for (Marker marker : markers) {
                            boolean typeok = false;
                            for (String mt : marker.types) {
                                typeok |= mt.equals (platefix.dbfix.type);
                            }
                            if (typeok) {
                                boolean hit = (platefix == bestplatefix) && (marker == bestmarker);
                                double dist = platefix.text.distOfPoint (marker.lookx, marker.looky);
                                System.out.print (PadLeft ((hit ? "<" : " ") + dist + (hit ? ">" : " "), 12));
                            } else {
                                System.out.print (PadLeft ("", 12));
                            }
                        }
                        System.out.println ("");
                    }
                    System.out.println ("matchPlateFixesToMarkers*: -----------------");
                }

                // assign the fix's position to marker's position
                // and remove them from lists yet to be processed

                // the farther the string is from the marker, the lower the quality

                markers.remove (bestmarker);
                bestplatefix.posx     = bestmarker.centx;
                bestplatefix.posy     = bestmarker.centy;
                bestplatefix.quality += bestdist + bestmarker.quality;
                livePlateFixes.remove (bestplatefix);

                // "dmefix" doesn't really give us lat/lon so don't use them normally
                // but they can be used in limited cases so save in separate list
                if (!bestplatefix.dbfix.type.equals ("dmefix")) {
                    foundPlateFixes.addLast (bestplatefix);
                } else {
                    DMEDBFix dmefix = (DMEDBFix) bestplatefix.dbfix;
                    if ((dmefix.navaid != null) && (dmefix.distnm > 0)) {
                        foundDMEFixes.addLast (bestplatefix);
                    }
                }
            }
        }

        /**
         * See if there are at least two other segments that untersect at the given
         * segment endpoint.
         */
        private boolean intersectsTwoSegments (int px, int py)
        {
            int nIntersects = 0;
            for (LineSeg ls : allLineSegs) {
                double dist = distToLineSeg (px, py, ls.x0, ls.y0, ls.x1, ls.y1);
                if ((dist < 3.0) && (++ nIntersects > 2)) return true;
            }
            return false;
        }

        /**
         * Check if the given segment goes from the runway centerline
         * to a platefix string.  If so, make a marker out of the end
         * of the line nearest the runway centerline that points to
         * the platefix string.
         * @param a = open end allegedly near platefix string
         * @param b = next to end allegedly near runway centerline
         * @returns with marker possibly added to markers
         */
        private void checkSingleLineBolt (SegNode a, SegNode b)
        {
            // end of loop needs to be near a platefix string
            int xa = a.index & 0xFFFF;
            int ya = a.index >> 16;

            // next to end needs to be near centerline
            int xb = b.index & 0xFFFF;
            int yb = b.index >> 16;

            // ignore little stray marks (anything less than 1/6th inch)
            // LQR 'IAP-NDB RWY 17' doesn't work if this is 1/10th
            // SFM 'IAP-RNAV (GPS) RWY 25' doesn't work if this is 1/8th
            if (Math.hypot (xa - xb, ya - yb) < csvDpi / 6.0) return;

            // don't do lines aligned with runway
            // they most likely are either centerlines or arrowheads or the runway itself
            // and the ones we want are angled
            if (appCrsFound) {
                double linerad = Math.atan2 (xa - xb, yb - ya);
                double diffrad = AngleDiff (linerad, Math.toRadians (appCrsTrue));

                // 179^ difference is same as 1^
                if (diffrad >= Math.PI / 2.0) diffrad = Math.PI - diffrad;

                if (diffrad < Math.toRadians (15.0)) return;
            }

            // make sure distance from next to end comes close to runway centerline
            if (rwyCenterline != null) {
                double d2l = distToLine (xb, yb, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                if (d2l > csvDpi / 50.0) return;
            }

            // find closest platefix string to end of line segment (at most 1/20th inch away)
            // be sure not to use cartoon bubble'd strings as in SBY 'IAP-VOR RWY 32'
            double bestdist = csvDpi / 20.0;
            PlateFix bestpf = null;
            for (PlateFix pf : livePlateFixes) {
                if (!pf.bubbled) {
                    double dist = pf.text.distOfPoint (xa, ya);
                    if (bestdist > dist) {
                        bestdist = dist;
                        bestpf   = pf;
                    }
                }
            }
            if (bestpf == null) return;

            // only do this for fixes close to the airport cuz there are stray lines
            // ... used for other things far away from airports,
            // ... eg, AUG 'IAP-VORDME RWY 08' near fix NOLLI
            if (Lib.LatLonDist (bestpf.dbfix.lat, bestpf.dbfix.lon, airport.lat, airport.lon) > 2.0) return;

            /*
             * If we don't know where the runway centerline is,
             * make sure the fix is on the approach course of the
             * navaid.
             * AJO "IAP-VOR OR GPS-A"
             */
            if (rwyCenterline == null) {
                PlateFix navaidpf = null;
                for (PlateFix pf : livePlateFixes) {
                    if (!pf.bubbled) continue;
                    if (navaidpf != null) return;
                    navaidpf = pf;
                }
                if (navaidpf == null) return;

                double navtopt = Lib.LatLonTC_rad (navaidpf.dbfix.lat, navaidpf.dbfix.lon, bestpf.dbfix.lat, bestpf.dbfix.lon);
                double diff = AngleDiff (navtopt, Math.toRadians (appCrsTrue));
                if (diff > Math.toRadians (1.0)) return;
            }

            // make a marker at the runway centerline
            // ... but look for platefix string near platefix end
            int xc     = xb;
            int yc     = yb;
            if (rwyCenterline != null) {
                xc     = (int) (lineIntersectX (xa, ya, xb, yb, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1) + 0.5);
                yc     = (int) (lineIntersectY (xa, ya, xb, yb, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1) + 0.5);
            }
            Marker m   = new Marker (xc, yc);
            m.lookx    = xa;
            m.looky    = ya;
            m.types    = new String[] { bestpf.dbfix.type };
            m.linesegs = new LinkedList<> ();
            m.linesegs.addLast (new LineSeg (xa, ya, xb, yb));
            markers.addLast (m);
            if (verbose) System.out.println ("single-line bolt " + xc + "," + yc + " for " + bestpf.dbfix.name);
        }

        /**
         * Scan the plate for fences and hide the markers hidden behind them.
         */
        private void hideFencedPlateFixes ()
        {
            /*
             * Sort line segments by ascending length.
             */
            int nLineSegs = allLineSegs.size ();
            LineSeg[] lineSegArray = new LineSeg[nLineSegs];
            allLineSegs.toArray (lineSegArray);
            Arrays.sort (lineSegArray, new Comparator<LineSeg> () {
                @Override
                public int compare (LineSeg a, LineSeg b)
                {
                    double la = a.length ();
                    double lb = b.length ();
                    if (la < lb) return -1;
                    if (la > lb) return  1;
                    return 0;
                }
            });

            /*
             * Find search limits based on allowed fence picket length.
             */
            int minIndex, maxIndex;
            for (minIndex = 0; minIndex < nLineSegs; minIndex ++) {
                if (lineSegArray[minIndex].length () >= csvDpi / 20.0) break;
            }
            for (maxIndex = nLineSegs; -- maxIndex >= minIndex;) {
                if (lineSegArray[maxIndex].length () <= csvDpi / 12.0) break;
            }

            /*
             * Search for fence halves consisting of 6 pickets,
             * where adjacent pickets are perpendicular.
             */
            LinkedList<LineSeg> fenceHalves = new LinkedList<> ();
            PicketScanner ps = new PicketScanner ();
            int[] pickets = new int[6];
            for (int i = minIndex; i <= maxIndex; i ++) {

                /*
                 * Get segment that is possibly part of a fence.
                 */
                ps.begseg = lineSegArray[i];
                if (ps.begseg == null) continue;
                lineSegArray[i] = null;
                pickets[0] = i;
                int nPickets = 1;

                boolean debug = false; // (ps.begseg.x0 >= 979) && (ps.begseg.x0 <= 1066) && (ps.begseg.y0 >= 1682) && (ps.begseg.y0 <= 1722);
                if (debug) System.out.println ("fence*: begseg=" + ps.begseg + " " + ps.begseg.length ());

                /*
                 * Search for the remaining 5 pickets.
                 */
                ps.endseg = ps.begseg;
                boolean matched;
                do {
                    matched = false;
                    for (int j = i; ++ j <= maxIndex;) {

                        /*
                         * Get a segment that might be part of the fence.
                         */
                        LineSeg seg = lineSegArray[j];
                        if (seg == null) continue;

                        /*
                         * Maybe it matches one end of the known pickets.
                         */
                        if (ps.scan (seg)) {
                            if (debug) System.out.println ("fence*:   +seg=" + seg + " " + seg.length () + " => " + ps.begseg + " " + ps.endseg);
                            if (nPickets < 6) pickets[nPickets] = j;
                            if (++ nPickets > 6) break;
                            matched = true;
                        }
                    }
                } while (matched && (nPickets <= 6));

                /*
                 * There might be a runt on the beginning and/or the end.
                 * Make one last scan for small segments if needed.
                 * Do this separately from main scan above as including these lengths in main scan takes way too long.
                 */
                if ((nPickets == 4) || (nPickets == 5)) {
                    if (debug) System.out.println ("fence*: before nPickets=" + nPickets + " " + ps.begseg.x0 + "," + ps.begseg.y0 + " " + ps.endseg.x1 + "," + ps.endseg.y1);
                    for (int j = minIndex; -- j >= 0;) {

                        /*
                         * Get a segment that might be a runt on one of the ends.
                         */
                        LineSeg seg = lineSegArray[j];
                        if (seg == null) continue;

                        /*
                         * Maybe it matches one end of the known pickets.
                         */
                        if (ps.scan (seg)) {
                            if (nPickets < 6) pickets[nPickets] = j;
                            nPickets ++;
                        }
                    }
                    if (debug) System.out.println ("fence*:  after nPickets=" + nPickets + " " + ps.begseg.x0 + "," + ps.begseg.y0 + " " + ps.endseg.x1 + "," + ps.endseg.y1);
                }

                /*
                 * If we found exactly 6 pickets, it is a proper fence half.
                 * Make a line segment that spans the half from end to end,
                 * except skip the very ends cuz they might be runts.
                 */
                if (nPickets == 6) {
                    fenceHalves.addLast (new LineSeg (ps.begseg.x1, ps.begseg.y1, ps.endseg.x0, ps.endseg.y0));
                    for (int j : pickets) lineSegArray[j] = null;
                }
            }

            // - make a list of fences where we have both halves making a whole
            int gapsq = csvDpi * csvDpi / 1000;
            int numFenceHalves = fenceHalves.size ();
            LineSeg[] fhArray = new LineSeg[numFenceHalves];
            fenceHalves.toArray (fhArray);
            LinkedList<LineSeg> wholeFences = new LinkedList<> ();

            for (int i = 0; i < numFenceHalves; i ++) {
                LineSeg fhai = fhArray[i];
                if (fhai == null) continue;
                for (int j = i; ++ j < numFenceHalves;) {

                    // - the two sides will have midpoints near each other
                    //   this is the only criteria we check
                    //   check midpoints in case ends are swapped
                    LineSeg fhaj = fhArray[j];
                    if (fhaj == null) continue;
                    int dx = (fhaj.x0 + fhaj.x1 - fhai.x0 - fhai.x1) / 2;
                    int dy = (fhaj.y0 + fhaj.y1 - fhai.y0 - fhai.y1) / 2;
                    if (dx * dx + dy * dy <= gapsq) {
                        wholeFences.addLast (fhai);
                        fhArray[j] = null;
                        break;
                    }
                }
            }

            // - block platefixes fenced off by whole fences
            for (LineSeg fence : wholeFences) {
                if (verbose) System.out.println ("fence at " + fence);

                // heading of line going through fence is 90deg from fence's heading
                // FencePlateFix.solve() assumes the fix is somewhere along this
                // extended line and it's not important to get which 180deg it is
                double truehdg = Math.atan2 (fence.x1 - fence.x0, fence.y0 - fence.y1) + Math.PI / 2.0;
                if (truehdg >= Math.PI) truehdg -= Math.PI * 2.0;

                // compute transform to put post0 at 0,0 and post6 at 1,0
                double[] xfm = fenceCompute (fence.x0, fence.y0, fence.x1, fence.y1);

                // see if the transformed fence puts the plate center up (-y is 'up')
                // a center down fence blocks all markers above it (blocks -y markers)
                // a center up fence blocks all markers below it (blocks +y markers)
                boolean centerup = fenceXformY (xfm, panelWidth / 2, panelHeight / 2) < 0;

                // for plate fixes tied to markers laterally above or below fence,
                // hide those on opposite side as the plate center
                for (Iterator<PlateFix> it = foundPlateFixes.iterator (); it.hasNext ();) {
                    PlateFix plateFix = it.next ();
                    double cx = plateFix.posx;
                    double cy = plateFix.posy;
                    double tx = fenceXformX (xfm, cx, cy);
                    double ty = fenceXformY (xfm, cx, cy);
                    if ((tx > 0.0) && (tx < 1.0) && (centerup ^ (ty < 0))) {

                        // fenced off, remove from foundPlateFixes list
                        it.remove ();

                        // then insert on fencedPlateFixes list
                        FencedPlateFix fpf = new FencedPlateFix ();
                        fpf.fencedFix = plateFix;
                        fpf.truehdg = truehdg;
                        fencedPlateFixes.addLast (fpf);

                        if (verbose) System.out.println ("fenced off " + plateFix.dbfix.name);
                    }
                }
            }
        }

        /**
         * Scans for fence pickets.
         */
        private static class PicketScanner {
            private final static int gapsq = 9;

            public LineSeg begseg;  // x0,y0 is one end of the fence
            public LineSeg endseg;  // x1,y1 is other end of the fence

            /**
             * See if given segment is part of the fence and include it if so.
             */
            public boolean scan (LineSeg seg)
            {
                int dx, dy;

                /*
                 * If perpendicular to and connects to beginning of what we have so far,
                 * make it the new beginning of what we have so far.
                 */
                if (segPerpendicular (seg, begseg)) {
                    dx = seg.x1 - begseg.x0;
                    dy = seg.y1 - begseg.y0;
                    if (dx * dx + dy * dy <= gapsq) {
                        begseg = seg;
                        return true;
                    }
                    dx = seg.x0 - begseg.x0;
                    dy = seg.y0 - begseg.y0;
                    if (dx * dx + dy * dy <= gapsq) {
                        begseg = seg.reverse ();
                        return true;
                    }
                }

                /*
                 * If perpendicular to and connects to end of what we have so far,
                 * make it the new end of what we have so far.
                 */
                if (segPerpendicular (seg, endseg)) {
                    dx = seg.x0 - endseg.x1;
                    dy = seg.y0 - endseg.y1;
                    if (dx * dx + dy * dy <= gapsq) {
                        endseg = seg;
                        return true;
                    }
                    dx = seg.x1 - endseg.x1;
                    dy = seg.y1 - endseg.y1;
                    if (dx * dx + dy * dy <= gapsq) {
                        endseg = seg.reverse ();
                        return true;
                    }
                }

                /*
                 * Segment does not connect to what we have for a fence so far.
                 */
                return false;
            }
        }

        /**
         * See if the two segments are approx perpendicular.
         */
        private static boolean segPerpendicular (LineSeg seg0, LineSeg seg1)
        {
            double len0 = seg0.length ();
            double len1 = seg1.length ();
            double norm0dx = (seg0.x1 - seg0.x0) / len0;
            double norm0dy = (seg0.y1 - seg0.y0) / len0;
            double norm1dx = (seg1.x1 - seg1.x0) / len1;
            double norm1dy = (seg1.y1 - seg1.y0) / len1;
            return Math.abs (norm0dx * norm1dx + norm0dy * norm1dy) < Math.cos (Math.toRadians (75.0));
        }

        /**
         * Find loops of segments.
         * Input:
         *   segmentNodes
         *   hvLineBytes
         * Output:
         *   closedLoops
         *   openLoops
         */
        private void findSegmentLoops ()
        {
            int pass = 0;

            /*
             * Get rid of all segment nodes in boxed-off areas.
             * Saves a lot of processing time.
             */
            for (Iterator<SegNode> it = segmentNodes.values ().iterator (); it.hasNext ();) {
                SegNode node = it.next ();

                // see if there's a good spot very near by
                // have to search nearby spots in case this is a horiz/vert line
                // ... that looks like a boxed off area
                int cx = node.index & 0xFFFF;
                int cy = node.index >> 16;
                if (!hasANearbyGoodPixel (cx, cy)) {

                    // node is in boxed-off area, remove from segmentNodes list
                    it.remove ();

                    // remove edges from this node to any other
                    while (!node.edges.isEmpty ()) {
                        SegNode other = node.edges.values ().iterator ().next ();
                        removeSegNodeEdge (node, other);
                    }
                }
            }

            /*
             * Closed loops go all the way around from one arbitrary point
             * back to itself.  The list includes the start/end point at
             * the beginning and end of the list.
             */
            for (SegNode startingNode : segmentNodes.values ()) {
                if (startingNode.edges.size () > 1) {

                    // see if there is a loop with more than 2 nodes from
                    // this node back to itself
                    LinkedList<SegNode> loop = new LinkedList<> ();
                    if (traceThroughSegmentsClosed (++ pass, loop, startingNode, startingNode)) {
                        loop.addLast (startingNode);

                        // make sure we don't process the edges in this loop again
                        SegNode lastNode  = null;
                        for (SegNode node : loop) {
                            if (lastNode  != null) removeSegNodeEdge (lastNode, node);
                            lastNode = node;
                        }

                        // save it
                        loop = optimizeStraightLines (loop);
                        closedLoops.addLast (loop);
                    }
                }
            }

            /*
             * Open loops start at an endpoint and go to another.
             * The list includes both endpoints.
             */
            for (SegNode startingNode : segmentNodes.values ()) {
                if (startingNode.edges.size () == 1) {
                    LinkedList<SegNode> loop = new LinkedList<> ();
                    loop.addLast (startingNode);
                    SegNode next = startingNode.edges.values ().iterator ().next ();
                    traceThroughSegmentsOpen (loop, next);
                }
            }

            /*
             * Sometimes there are open loops that have a small break in
             * the middle somewhere (eg, DNL "IAP-NDBDME-C"), so we want
             * to join those two open loops.
             *
             * Another example: FSD "IAP-ILS OR LOC RWY 03" ROKKY/FS cartoon bubble
             *             and: GRB "IAP-ILS OR LOC RWY 36" DEPRE/SG cartoon bubble
             */
            OpenLoopCombiner olc = new OpenLoopCombiner ();
            while (olc.run ()) { }

            /*
             * Paint loops for debugging.
             */
            //for (LinkedList<SegNode> loop : closedLoops) paintSegNodeLoop (loop, loop.getLast ().pass);
            //for (LinkedList<SegNode> loop : openLoops)   paintSegNodeLoop (loop, ++ pass);
        }

        /**
         * Trace through edges to find end of closed loop (same point as beginning).
         * @param pass = incremented for each loop search
         * @param loop = previous nodes in this loop
         * @param startingNode = first node in the loop
         * @param node = new node to be added to loop
         * @returns true : closed loop found
         *         false : closed loop not found
         */
        private static boolean traceThroughSegmentsClosed (int pass, LinkedList<SegNode> loop, SegNode startingNode, SegNode node)
        {
            // if looped back around to beginning, and it's at least a triangle, loop is complete
            if ((node == startingNode) && (loop.size () > 2)) return true;

            // if we have looped back somewhere to middle of loop or to a segment that's part of a previous loop, this edge isn't part of loop
            if (node.pass >= pass) return false;

            // mark this node as being seen in this pass and push as part of loop
            node.pass = pass;
            loop.addLast (node);

            // search all edges out from here
            // if one of them leads back to startingNode, then the loop is complete
            for (SegNode next : node.edges.values ()) {
                if (traceThroughSegmentsClosed (pass, loop, startingNode, next)) return true;
            }

            // this node isn't on a path that loops back to startingNode
            loop.removeLast ();
            return false;
        }

        /**
         * Trace through edges to find end of open loop.
         * Remove edges and process any when found.
         * @param loop = previous nodes in this loop
         * @param node = new node to be added to loop
         */
        private void traceThroughSegmentsOpen (LinkedList<SegNode> loop, SegNode node)
        {
            // get rid of edge leading to the new node
            removeSegNodeEdge (loop.getLast (), node);

            // push node as part of loop
            loop.addLast (node);

            // if at an endpoint, we have a complete open loop
            if (node.edges.isEmpty ()) {
                loop = optimizeStraightLines (loop);
                openLoops.addLast (loop);
                return;
            }

            // search all edges out from here
            // the first edge found just continues along the same loop
            // all other edges start their own branch loop
            LinkedList<SegNode> branch = loop;
            while (!node.edges.isEmpty ()) {
                SegNode next = node.edges.values ().iterator ().next ();
                if (branch == null) {
                    branch = new LinkedList<> ();
                    branch.addLast (node);
                }
                traceThroughSegmentsOpen (branch, next);
                branch = null;
            }
        }

        /**
         * Remove edge between two nodes.
         * Note that the edge has a link in both directions.
         */
        private static void removeSegNodeEdge (SegNode n1, SegNode n2)
        {
            n1.edges.remove (n2.index);
            n2.edges.remove (n1.index);
        }

        /**
         * Sometimes there are straight lines broken down into separate segments.
         * Combine those into a single segment.
         */
        private static LinkedList<SegNode> optimizeStraightLines (LinkedList<SegNode> original)
        {
            /*
             * If fewer than 3 points, there is nothing to combine.
             */
            int noriginal = original.size ();
            if (noriginal < 3) return original;

            /*
             * Copy original list to an array for easy access.
             */
            SegNode[] array = new SegNode[noriginal];
            original.toArray (array);

            /*
             * Create a new list for the edited version.
             */
            LinkedList<SegNode> edited = new LinkedList<> ();

            /*
             * Grab the last two points from the original list.
             */
            int i = noriginal;
            SegNode s0 = array[--i];
            SegNode s1 = array[--i];

            do {
                /*
                 * At this point:
                 *   we have line s1 -- s0
                 *   neither point is pushed to the output list
                 */

                /*
                 * Grab the next point so we have:
                 *     s2 -- s1 -- s0
                 */
                SegNode s2 = array[--i];

                /*
                 * See if the three points are all in a straight line,
                 * either extension or doubleback.
                 */
                int x0 = s0.index & 0xFFFF;
                int x1 = s1.index & 0xFFFF;
                int x2 = s2.index & 0xFFFF;
                int y0 = s0.index >> 16;
                int y1 = s1.index >> 16;
                int y2 = s2.index >> 16;
                int dx01 = x0 - x1;
                int dy01 = y0 - y1;
                int dx21 = x2 - x1;
                int dy21 = y2 - y1;
                if (dy01 * dx21 == dy21 * dx01) {

                    /*
                     * If co-linear extension:
                     *   just forget about midpoint (s1)
                     *   so transform  s2 -- s0  =>  s1 -- s0
                     */
                    if (((dx01 ^ dx21) | (dy01 ^ dy21)) < 0) {
                        s1 = s2;
                        continue;
                    }

                    /*
                     * Co-linear doubleback, get length of both segments.
                     */
                    int lnsq01 = dx01 * dx01 + dy01 * dy01;
                    int lnsq21 = dx21 * dx21 + dy21 * dy21;

                    /*
                     * If at very beginning of loop and loop's first segment (s2--s1) isn't longer 
                     * than loop's second segment (s1--s0), discard first segment.
                     */
                    if ((i == 0) && (lnsq21 <= lnsq01)) continue;

                    /*
                     * If at very end of loop and loop's next-to-last segment (s2--s1) is at least
                     * as long as loop's last segment (s1--s0), discard last segment.
                     */
                    if (edited.isEmpty () && (lnsq21 >= lnsq01)) {
                        // s2 -- s1  =>  s1 -- s0
                        s0 = s1;
                        s1 = s2;
                        continue;
                    }
                }

                /*
                 * Not co-linear or some doubleback cases:
                 *   output s0
                 *   and shift  s2 -- s1  =>  s1 -- s0
                 */
                edited.addLast (s0);
                s0 = s1;
                s1 = s2;
            } while (i > 0);

            /*
             * Output the final points.
             */
            edited.addLast (s0);
            edited.addLast (s1);

            return edited;
        }

        private final static Color[] loopColors = { Color.ORANGE, Color.GREEN, Color.MAGENTA, Color.BLUE };
        private void paintSegNodeLoop (LinkedList<SegNode> loop, int pass)
        {
            Color color = loopColors[pass%loopColors.length];
            g2d.setColor (color);

            int lastIndex = -1;
            int nextIndex = -1;
            for (SegNode node : loop) {
                lastIndex = nextIndex;
                nextIndex = node.index;
                if (lastIndex >= 0) {
                    int lx = lastIndex & 0xFFFF;
                    int ly = lastIndex >> 16;
                    int nx = nextIndex & 0xFFFF;
                    int ny = nextIndex >> 16;
                    g2d.drawLine (lx, ly, nx, ny);
                }
            }
        }


        /**
         * See if any open loops can be combined cuz endpoints overlap.
         */
        private class OpenLoopCombiner {
            public boolean run ()
            {
                boolean didSomething = false;

                LinkedList<LinkedList<SegNode>> combinedOpenLoops = new LinkedList<> ();
                while (!openLoops.isEmpty ()) {

                    // get one open loop
                    LinkedList<SegNode> loopi = openLoops.removeFirst ();
                    int isize = loopi.size ();

                    for (Iterator<LinkedList<SegNode>> it = openLoops.iterator (); it.hasNext ();) {

                        // get another open loop
                        LinkedList<SegNode> loopj = it.next ();
                        int jsize = loopj.size ();

                        // see if the beginning points match
                        //     loopi = ibeg0 ... iend0
                        //     loopj = jbeg0 ... jend0
                        //   =>
                        //     jend0 ... jbeg0 ibeg0 ... iend0
                        if (joinable (loopi, 0, 1, loopj, 0, 1)) {
                            while (!loopj.isEmpty ()) loopi.addFirst (loopj.removeFirst ());
                            it.remove ();
                            didSomething = true;
                            isize = loopi.size ();
                            continue;
                        }

                        // see if the beginning of I matches the end of J
                        //     loopi = ibeg0 ... iend0
                        //     loopj = jbeg0 ... jend0
                        //   =>
                        //     jbeg0 ... jend0 ibeg0 ... iend0
                        if (joinable (loopi, 0, 1, loopj, jsize - 1, jsize - 2)) {
                            for (SegNode node : loopi) loopj.addLast (node);
                            loopi = loopj;
                            it.remove ();
                            didSomething = true;
                            isize = loopi.size ();
                            continue;
                        }

                        // see if the end of I matches the beginning of J
                        //     loopi = ibeg0 ... iend0
                        //     loopj = jbeg0 ... jend0
                        //   =>
                        //     ibeg0 ... iend0 jbeg0 ... jend0
                        if (joinable (loopi, isize - 1, isize - 2, loopj, 0, 1)) {
                            for (SegNode node : loopj) loopi.addLast (node);
                            it.remove ();
                            didSomething = true;
                            isize = loopi.size ();
                            continue;
                        }

                        // see if the end points match
                        //     loopi = ibeg0 ... iend0
                        //     loopj = jbeg0 ... jend0
                        //   =>
                        //     ibeg0 ... iend0 jend0 ... jbeg0
                        if (joinable (loopi, isize - 1, isize - 2, loopj, jsize - 1, jsize - 2)) {
                            while (!loopj.isEmpty ()) loopi.addLast (loopj.removeLast ());
                            it.remove ();
                            didSomething = true;
                            isize = loopi.size ();
                            continue;
                        }
                    }

                    // possibly we have created a closed loop
                    SegNode ibeg0n = loopi.getFirst ();
                    int ibeg0x = ibeg0n.index & 0xFFFF;
                    int ibeg0y = ibeg0n.index >> 16;
                    SegNode iend0n = loopi.getLast ();
                    int iend0x = iend0n.index & 0xFFFF;
                    int iend0y = iend0n.index >> 16;
                    if (distSqBtwnPts (ibeg0x, ibeg0y, iend0x, iend0y) <= 2) {
                        if ((ibeg0x != iend0x) || (ibeg0y != iend0y)) {
                            loopi.addLast (ibeg0n);
                        }
                        loopi = optimizeStraightLines (loopi);
                        closedLoops.addLast (loopi);
                    } else {

                        // save the combined open loop
                        loopi = optimizeStraightLines (loopi);
                        combinedOpenLoops.addLast (loopi);
                    }
                }

                openLoops = combinedOpenLoops;
                return didSomething;
            }

            /**
             * See if the two loops are joinable.
             * There may be overlap in the end segments.
             * @param loopi = one loop
             * @param i0 = end index in loopi
             * @param i1 = next-to-end index in loopi
             * @param loopj = other loop
             * @param j0 = end index in loopj
             * @param j1 = next-to-end index in loopj
             */
            private boolean joinable (LinkedList<SegNode> loopi, int i0, int i1, LinkedList<SegNode> loopj, int j0, int j1)
            {
                // get end of both loops
                SegNode i0n = loopi.get (i0);
                SegNode j0n = loopj.get (j0);
                int i0x = i0n.index & 0xFFFF;
                int i0y = i0n.index >> 16;
                int j0x = j0n.index & 0xFFFF;
                int j0y = j0n.index >> 16;

                // if very close, loops are joinable as is
                if (distSqBtwnPts (i0x, i0y, j0x, j0y) <= 2) return true;

                // get next-to-end of both loops
                SegNode i1n = loopi.get (i1);
                SegNode j1n = loopj.get (j1);
                int i1x = i1n.index & 0xFFFF;
                int i1y = i1n.index >> 16;
                int j1x = j1n.index & 0xFFFF;
                int j1y = j1n.index >> 16;

                // get distance of endpoint of loopi to last segment of loopj
                double disti = distToLineSeg (i0x, i0y, j0x, j0y, j1x, j1y);

                // get distance of endpoint of loopj to last segment of loopi
                double distj = distToLineSeg (j0x, j0y, i0x, i0y, i1x, i1y);

                // if the last segments of both loops don't match, not joinable
                if ((disti > 2.0) || (distj > 2.0)) return false;

                // segments overlap, chop one end or the other to match
                if (disti > distj) {
                    loopi.remove (i0);
                } else {
                    loopj.remove (j0);
                }
                return true;
            }
        }

        /**
         * Some charts have a FEEDER FACILITIES ring.
         * Ignore everything outside that ring as it is not to scale.
         * Eg, PQI 'IAP-VORDME RWY 01'
         *     HLN 'IAP-ILS OR LOC Z RWY 27'
         */
        private void findFeederFacilitiesRing ()
        {
            /*
             * Get list of segments that might form a ring.
             * Use openLoops list cuz each segment might really be drawn as more than one segment.
             */
            LinkedList<LineSeg> segments = new LinkedList<> ();
            for (LinkedList<SegNode> openLoop : openLoops) {
                SegNode first = openLoop.getFirst ();
                SegNode last  = openLoop.getLast  ();
                int begx = first.index & 0xFFFF;
                int begy = first.index >> 16;
                int endx = last.index & 0xFFFF;
                int endy = last.index >> 16;
                double len = Math.hypot (begx - endx, begy - endy);
                if ((len > csvDpi / 6.0) && (len < csvDpi / 4.0)) {
                    LineSeg ls = new LineSeg (begx, begy, endx, endy);
                    segments.addLast (ls);
                }
            }

            /*
             * Get list of arcs formed by those segments.
             */
            LinkedList<Ring> validRingList = new LinkedList<> ();
            while (!segments.isEmpty ()) {

                /*
                 * Get a chain of segments that might form a substantial part of a ring.
                 */
                LineSeg begseg = segments.removeFirst ();
                Ring ring = new Ring ();
                ring.segments.addLast (begseg);
                LineSeg endseg = begseg;

                boolean matched;
                do {
                    matched = false;
                    for (Iterator<LineSeg> it = segments.iterator (); it.hasNext ();) {
                        LineSeg seg = it.next ();

                        int mbeg = feederFacilitiesRingMatches (seg, begseg.x0, begseg.y0, begseg.x1, begseg.y1);
                        if (mbeg >= 0) {
                            begseg = (mbeg != 0) ? seg : seg.reverse ();
                            ring.segments.addFirst (begseg);
                            it.remove ();
                            matched = true;
                            continue;
                        }
                        int mend = feederFacilitiesRingMatches (seg, endseg.x1, endseg.y1, endseg.x0, endseg.y0);
                        if (mend >= 0) {
                            endseg = (mend == 0) ? seg : seg.reverse ();
                            ring.segments.addLast (endseg);
                            it.remove ();
                            matched = true;
                            continue;
                        }
                    }
                } while (matched);

                /*
                 * If it is really a ring, add to list of valid ring segments.
                 */
                if (ring.isValid ()) {
                    validRingList.addLast (ring);
                }
            }

            /*
             * Find combination of arcs that converge on a spot.
             */
            int nValidRings = validRingList.size ();
            Ring[] validRingArray = new Ring[nValidRings];
            validRingList.toArray (validRingArray);
            Ring superRing = Ring.findSuperRing (validRingArray, panelWidth / 2);

            /*
             * If we found such a ring, mark all pixels outside the ring as boxed off.
             * There might be fixes right on the ring so cover the ring itself plus a little margin.
             */
            if (superRing != null) {
                double bestrad = superRing.getRadius  ();
                double bestcx  = superRing.getCenterX ();
                double bestcy  = superRing.getCenterY ();
                bestrad -= csvDpi / 15.0;
                if (verbose) System.out.println ("feeder facilities ring " + Math.round (bestcx) + "," +
                                    Math.round (bestcy) + " radius=" + Math.round (bestrad));
                for (int y = 0; y < panelHeight; y ++) {
                    for (int x = 0; x < panelWidth; x ++) {
                        double r = Math.hypot (x - bestcx, y - bestcy);
                        if (r > bestrad) {
                            hvLineBytes[y*panelWidth+x] = HVLB_RING;
                        }
                    }
                }
            }
        }

        /**
         * See if the given segment attaches to the existing segment of a ring.
         * @param seg = segment being tested to see if it is part of a ring
         * @param endx,y = one of the endpoints of the existing ring
         * @param intx,y = point on the interior of the existing ring adjacent to endx,y
         * @returns -1: segment is not attached to ring
         *           0: seg.x0,seg.y0 attaches to ring at endx,y
         *           1: seg.x1,seg.y1 attaches to ring at endx,y
         */
        private static int feederFacilitiesRingMatches (LineSeg seg, int endx, int endy, int intx, int inty)
        {
            boolean debug = (endx == 703) && (endy == 579) && (seg.x0 == 630) && (seg.y0 == 534);

            // get length of segment on end of existing ring and angle
            double existing_segment_length = Math.hypot (endy - inty, endx - intx);
            double existing_segment_angle  = Math.atan2 (endy - inty, endx - intx);

            // get gap between seg.x0,y0 point and end of the existing ring
            double gap_between_seg0_and_existing_end_length = Math.hypot (seg.y0 - endy, seg.x0 - endx);

            // see if the gap is within range (about 1/2 length of segments)
            if ((gap_between_seg0_and_existing_end_length < existing_segment_length) &&
                (gap_between_seg0_and_existing_end_length > existing_segment_length / 4)) {

                // get angle of the gap
                double gap_between_seg0_and_existing_end_angle = Math.atan2 (seg.y0 - endy, seg.x0 - endx);

                // if difference of angle isn't too big, assume we have a match
                double diff = existing_segment_angle - gap_between_seg0_and_existing_end_angle;
                while (diff < -Math.PI) diff += Math.PI * 2;
                while (diff >= Math.PI) diff -= Math.PI * 2;
                if (Math.abs (diff) < Math.toRadians (15.0)) return 0;
            }

            // likewise for seg.x1,y1
            double gap_between_seg1_and_existing_end_length = Math.hypot (seg.x1 - endx, seg.y1 - endy);
            if ((gap_between_seg1_and_existing_end_length < existing_segment_length) &&
                (gap_between_seg1_and_existing_end_length > existing_segment_length / 4)) {
                double gap_between_seg1_and_existing_end_angle = Math.atan2 (seg.y1 - endy, seg.x1 - endx);
                double diff = existing_segment_angle - gap_between_seg1_and_existing_end_angle;
                while (diff < -Math.PI) diff += Math.PI * 2;
                while (diff >= Math.PI) diff -= Math.PI * 2;
                if (Math.abs (diff) < Math.toRadians (15.0)) return 1;
            }

            // neither seg.x0,y0 nor seg.x1,y1 matches
            return -1;
        }

        /**
         * Shade the boxed-off areas.
         */
        private void shadeBoxedOffAreas ()
        {
            Paint oldPaint = g2d.getPaint ();
            g2d.setPaint (new Color (255, 255, 0, 63));
            for (int y = 0; y < panelHeight; y ++) {
                for (int x = 0; x < panelWidth; x ++) {
                    if (hvLineBytes[y*panelWidth+x] != HVLB_GOOD) {
                        int w = 1;
                        while ((x + w < panelWidth) && (hvLineBytes[y*panelWidth+x+w] != HVLB_GOOD)) w ++;
                        g2d.fillRect (x, y, w, 1);
                        x += w - 1;
                    }
                }
            }
            g2d.setPaint (oldPaint);
        }

        /**
         * Look through all the closed loops looking for cartoon bubbles around platefix string.
         * They have an intersecting line segment that points to the fix.
         * Also search open loops in case the loop is split by something like LOM/IAF along the top line.
         */
        private void findCartoonBubbles ()
        {
            // scan through all the closed loops
            for (Iterator<LinkedList<SegNode>> it = closedLoops.iterator (); it.hasNext ();) {
                LinkedList<SegNode> loop = it.next ();

                // there should be a left/right/top/bottom line segment forming a box
                // vertical segments at least 1/16 inch long
                // horizontal segments at least 1/6 inch long
                int top  = 999999999;
                int bot  = 0;
                int left = 999999999;
                int rite = 0;
                int lx   = 0;
                int ly   = 0;
                for (SegNode node : loop) {
                    int nx = node.index & 0xFFFF;
                    int ny = node.index >> 16;
                    if ((nx == lx) && (Math.abs (ny - ly) > csvDpi / 16)) {
                        if (left > nx) left = nx;
                        if (rite < nx) rite = nx;
                    }
                    if ((ny == ly) && (Math.abs (nx - lx) > csvDpi /  6)) {
                        if (top  > ny) top  = ny;
                        if (bot  < ny) bot  = ny;
                    }
                    lx = nx;
                    ly = ny;
                }

                if (foundACartoonBubble (loop, left, rite, top, bot, null)) {
                    it.remove ();
                }
            }

            // scan through all the open loops
            for (Iterator<LinkedList<SegNode>> it = openLoops.iterator (); it.hasNext ();) {
                LinkedList<SegNode> loop = it.next ();

                int firstx = loop.getFirst ().index & 0xFFFF;
                int firsty = loop.getFirst ().index >> 16;
                int lastx  = loop.getLast ().index & 0xFFFF;
                int lasty  = loop.getLast ().index >> 16;

                // there should be a left/right/top/bottom line segment forming a box
                // vertical segments at least 1/16 inch long
                // horizontal segments at least 1/2 inch long
                // the top consists of short line segments broken by text like LOM/IF/IAF etc
                // ... a worst-case example is DNL "IAP-NDBDME-C"
                // but sometimes the top line also has the pointer wedge coming out as in
                // FSD "IAP-ILS OR LOC RWY 03" so we have to check for some minimal amount of horizontal length
                int top  = 999999999;
                int bot  = 0;
                int left = 999999999;
                int rite = 0;
                int lx   = 0;
                int ly   = 0;
                for (SegNode node : loop) {
                    int nx = node.index & 0xFFFF;
                    int ny = node.index >> 16;
                    if ((Math.abs (nx - lx) <= 1) && (Math.abs (ny - ly) > csvDpi / 16)) {
                        if (left > Math.min (nx, lx)) left = Math.min (nx, lx);
                        if (rite < Math.max (nx, lx)) rite = Math.max (nx, lx);
                    }
                    if ((ny == ly) && (Math.abs (nx - lx) > csvDpi / 60)) {
                        if (top > ny) top = ny;
                    }
                    if ((ny == ly) && (Math.abs (nx - lx) > csvDpi /  6)) {
                        if (bot < ny) bot = ny;
                    }
                    lx = nx;
                    ly = ny;
                }

                // assume the pointer is not part of the open loop
                SegNode pointsTo = null;

                // see if endpoints are over 1/10th inch apart
                if (distSqBtwnPts (firstx, firsty, lastx, lasty) > csvDpi * csvDpi / 100) {

                    // see if nexttolastx,y..lastx,y is the pointer
                    // ... firstx,y must intersect the nexttolastx,y..lastx,y segment
                    //     and firstx,y must be closer to nexttolastx,y than lastx,y
                    int nexttolastx = loop.get (loop.size () - 2).index & 0xFFFF;
                    int nexttolasty = loop.get (loop.size () - 2).index >> 16;
                    double enddist = distToLineSeg (firstx, firsty, nexttolastx, nexttolasty, lastx, lasty);
                    if (enddist < 2) {
                        if (distSqBtwnPts (firstx, firsty, lastx, lasty) > distSqBtwnPts (firstx, firsty, nexttolastx, nexttolasty)) {
                            pointsTo = loop.getLast ();
                        }
                    }

                    // see if nexttofirstx,y..firstx,y is the pointer
                    // ... lastx,y must intersect the nexttofirstx,y..firstx,y segment
                    //     and lastx,y must be closer to nexttofirstx,y than firstx,y
                    int nexttofirstx = loop.get (1).index & 0xFFFF;
                    int nexttofirsty = loop.get (1).index >> 16;
                    double begdist = distToLineSeg (lastx, lasty, nexttofirstx, nexttofirsty, firstx, firsty);
                    if (begdist < 2) {
                        int d0 = distSqBtwnPts (lastx, lasty, firstx, firsty);
                        int d1 = distSqBtwnPts (lastx, lasty, nexttofirstx, nexttofirsty);
                        if (d0 > d1) {
                            pointsTo = loop.getFirst ();
                        }
                    }
                }

                if (foundACartoonBubble (loop, left, rite, top, bot, pointsTo)) {
                    it.remove ();
                }
            }

            // we may have a cartoon bubble that consists of two open loops:
            //   one has the pointer line and the other doesn't
            //   example: AUS "IAP-ILS OR LOC RWY 35L" CREEK NDB
            boolean isACartoonBubble;
            do {
                isACartoonBubble = false;
                for (LinkedList<SegNode> loopi : openLoops) {

                    // get beginning and ending point for this loop
                    // we are going to assume below that this is the loop that doesn't have the pointer line
                    int isize = loopi.size ();
                    if (isize < 3) continue;

                    for (LinkedList<SegNode> loopj : openLoops) {
                        if (loopj == loopi) continue;

                        // get beginning two and ending two points for this loop
                        // we are going to assume that this is the loop that has the pointer line
                        int jsize = loopj.size ();
                        if (jsize < 3) continue;

                        // see if everything except the last segment of loopj makes a cartoon bubble (and last segment is the pointer)
                        // see if everything except the first segment of loopj makes a cartoon bubble (and first segment is the pointer)
                        if (checkForSplitOpenCartoonBubble (loopi, 0, isize - 1, loopj, 0, jsize - 2, jsize - 1) ||
                            checkForSplitOpenCartoonBubble (loopi, 0, isize - 1, loopj, 1, jsize - 1, 0)) {
                            openLoops.remove (loopi);
                            openLoops.remove (loopj);
                            isACartoonBubble = true;
                            break;
                        }
                    }
                    if (isACartoonBubble) break;
                }
            } while (isACartoonBubble);
        }

        /**
         * See if the given parts of two loops form a cartoon bubble.
         * @param loopa = one loop
         * @param bega  = beginning index in loopa
         * @param enda  = end index of loopa (inclusive)
         * @param loopb = other loop
         * @param begb  = beginning index in loopb
         * @param endb  = end index of loopb (inclusive)
         * @param ptrb  = index in loopb of pointer line end
         */
        private boolean checkForSplitOpenCartoonBubble (
            LinkedList<SegNode> loopa, int bega, int enda,
            LinkedList<SegNode> loopb, int begb, int endb, int ptrb)
        {
            // see if one end or other of loopa intersects with one end or other of loopb
            // to find out if they are connected or not.  note that the ends may overlap.

            // get two points from beginning and end of each loop
            SegNode bega0 = loopa.get (bega + 0);
            SegNode bega1 = loopa.get (bega + 1);
            SegNode enda0 = loopa.get (enda + 0);
            SegNode enda1 = loopa.get (enda - 1);
            SegNode begb0 = loopb.get (begb + 0);
            SegNode begb1 = loopb.get (begb + 1);
            SegNode endb0 = loopb.get (endb + 0);
            SegNode endb1 = loopb.get (endb - 1);

            // ... and their corresponding x,y
            int bega0x = bega0.index & 0xFFFF;
            int bega0y = bega0.index >> 16;
            int bega1x = bega1.index & 0xFFFF;
            int bega1y = bega1.index >> 16;
            int enda1x = enda1.index & 0xFFFF;
            int enda1y = enda1.index >> 16;
            int enda0x = enda0.index & 0xFFFF;
            int enda0y = enda0.index >> 16;

            int begb0x = begb0.index & 0xFFFF;
            int begb0y = begb0.index >> 16;
            int begb1x = begb1.index & 0xFFFF;
            int begb1y = begb1.index >> 16;
            int endb1x = endb1.index & 0xFFFF;
            int endb1y = endb1.index >> 16;
            int endb0x = endb0.index & 0xFFFF;
            int endb0y = endb0.index >> 16;

            // see if one of the endpoints of one loop intersects one of the end segments of the other loop
            if ((distToLineSeg (begb0x, begb0y, bega0x, bega0y, bega1x, bega1y) > 2.0) &&
                (distToLineSeg (endb0x, endb0y, bega0x, bega0y, bega1x, bega1y) > 2.0) &&
                (distToLineSeg (begb0x, begb0y, enda0x, enda0y, enda1x, enda1y) > 2.0) &&
                (distToLineSeg (endb0x, endb0y, enda0x, enda0y, enda1x, enda1y) > 2.0) &&
                (distToLineSeg (bega0x, bega0y, begb0x, begb0y, begb1x, begb1y) > 2.0) &&
                (distToLineSeg (enda0x, enda0y, begb0x, begb0y, begb1x, begb1y) > 2.0) &&
                (distToLineSeg (bega0x, bega0y, endb0x, endb0y, endb1x, endb1y) > 2.0) &&
                (distToLineSeg (enda0x, enda0y, endb0x, endb0y, endb1x, endb1y) > 2.0)) {

                // no intersection found, they cannot form a cartoon bubble
                return false;
            }

            // there should be a left/right/top/bottom line segment forming a box
            // vertical segments at least 1/16 inch long
            // horizontal segments at least 1/4 inch long
            // the top consists of short line segments broken by text like LOM/IF/IAF etc
            // ... a worst-case example is DNL "IAP-NDBDME-C"

            int top  = 999999999;
            int bot  = 0;
            int left = 999999999;
            int rite = 0;

            // scan segments in A loop
            int lx = 0;
            int ly = 0;
            int i  = 0;
            for (SegNode node : loopa) {
                if ((i >= bega) && (i <= enda)) {
                    int nx = node.index & 0xFFFF;
                    int ny = node.index >> 16;
                    if ((nx == lx) && (Math.abs (ny - ly) > csvDpi / 16)) {
                        if (left > nx) left = nx;
                        if (rite < nx) rite = nx;
                    }
                    if (top > ny) top = ny;
                    if ((ny == ly) && (Math.abs (nx - lx) > csvDpi /  4)) {
                        if (bot < ny) bot = ny;
                    }
                    lx = nx;
                    ly = ny;
                }
                i ++;
            }

            // scan segments in B loop
            lx = 0;
            ly = 0;
            i  = 0;
            SegNode pointsTo = null;
            for (SegNode node : loopb) {
                if (i == ptrb) pointsTo = node;
                if ((i >= begb) && (i <= endb)) {
                    int nx = node.index & 0xFFFF;
                    int ny = node.index >> 16;
                    if ((nx == lx) && (Math.abs (ny - ly) > csvDpi / 16)) {
                        if (left > nx) left = nx;
                        if (rite < nx) rite = nx;
                    }
                    if (top > ny) top = ny;
                    if ((ny == ly) && (Math.abs (nx - lx) > csvDpi /  4)) {
                        if (bot < ny) bot = ny;
                    }
                    lx = nx;
                    ly = ny;
                }
                i ++;
            }

            // make sure the pointer goes somewhere outside the rectangle so we don't have a false end
            int ptrx = pointsTo.index & 0xFFFF;
            int ptry = pointsTo.index >> 16;
            int slop = csvDpi / 15;
            if ((ptrx >= left - slop) && (ptrx <= rite - slop) && (ptry >= top - slop) && (ptry <= bot + slop)) return false;

            // see if they form a cartoon bubble and move enclosed platefix string to the pointed-to spot
            return foundACartoonBubble (null, left, rite, top, bot, pointsTo);
        }

        /**
         * Most likely we have found a cartoon bubble.
         * Make sure it is the correct size and look for a platefix string within the bubble.
         * Then look for a line segment that intersects the perimeter of the bubble and then
         * move the platefix string center to the other end of the line segment.
         */
        private boolean foundACartoonBubble (LinkedList<SegNode> loop, int left, int rite, int top, int bot, SegNode pointsTo)
        {
            boolean rc = false;

            // the box should be at least 1/4 inch tall and 1/2 inch wide
            int width  = rite - left;
            int height = bot  - top;
            ////if ((width > 0) && (height > 0)) {
            ////    System.out.println ("foundACartoonBubble*: " + left + ".." + rite + "," + top + ".." + bot);
            ////}
            if ((width > csvDpi / 2) && (width < csvDpi * 5 / 4) && (height > csvDpi / 5) && (height < csvDpi / 2)) {
                ////System.out.println ("foundACartoonBubble*: good size");

                // see if there are any platefix strings in the bubble
                // there may be more than one as in
                //    PEASE
                //  116.5 PSM
                // ...on 3B4 'IAP-VOR-A'
                // ...but only one will match the marker being pointed to based on type
                LinkedList<PlateFix> plateFixes = new LinkedList<> ();
                for (PlateFix pf : allPlateFixes) {
                    int strx = (pf.text.botleftx + pf.text.botritex) / 2;
                    int stry = (pf.text.toplefty + pf.text.botlefty) / 2;
                    if ((strx > left) && (strx < rite) && (stry > top) && (stry < bot)) {
                        plateFixes.addLast (pf);
                    }
                }
                if (!plateFixes.isEmpty ()) {

                    // find line segment intersecting the bubble
                    // must be at least 1/12th inch long
                    // if one found, move string position to other end of line segment ...
                    // putting the string right at the fix being pointed to by the segment
                    if (pointsTo == null) {
                        int distsq = csvDpi * csvDpi / 144;
                        for (LinkedList<SegNode> link : openLoops) {
                            int linksize = link.size ();
                            if (linksize < 2) continue;
                            for (int index = 0; index + 2 <= linksize; index += linksize - 2) {

                                // get either first or last segment in open loop
                                SegNode s0 = link.get (index + 0);
                                SegNode s1 = link.get (index + 1);

                                // sometimes there are beziers intersecting cartoon bubble
                                // eg, KDUC IAP-LOC RWY 35 at I-DUC
                                int i0 = s0.index;
                                int i1 = s1.index;
                                if (!edgeIsBezier (s0, s1)) {

                                    // save if longest straight line found so far that intersects bubble
                                    // discard lines that are duplicates of the bubble itself by accepting only one end intercepting
                                    int x0 = i0 & 0xFFFF;
                                    int y0 = i0 >> 16;
                                    int x1 = i1 & 0xFFFF;
                                    int y1 = i1 >> 16;
                                    int dsq = distSqBtwnPts (x0, y0, x1, y1);
                                    if (distsq < dsq) {
                                        boolean ints0 = xyIntersectsLoop (loop, x0, y0);
                                        boolean ints1 = xyIntersectsLoop (loop, x1, y1);
                                        if (ints0 ^ ints1) {

                                            // what is being pointed to
                                            SegNode ptr = ints0 ? s1 : s0;
                                            int ptrx = ptr.index & 0xFFFF;
                                            int ptry = ptr.index >> 16;

                                            // see if it is supposed to point to an NDB or VOR marker
                                            boolean saveit = true;
                                            for (PlateFix pf : plateFixes) {
                                                if (pf.dbfix.type.contains ("NDB") || pf.dbfix.type.contains ("VOR")) {
                                                    saveit = false;

                                                    // if so, make sure this segment points to a nearby compatible type of marker
                                                    for (Marker m : markers) {
                                                        for (String t : m.types) {
                                                            if (t.equals (pf.dbfix.type) &&
                                                                (distSqBtwnPts (m.centx, m.centy, ptrx, ptry) < csvDpi * csvDpi / 30)) {
                                                                saveit = true;
                                                                break;
                                                            }
                                                        }
                                                        if (saveit) break;
                                                    }
                                                    if (saveit) break;
                                                }
                                            }

                                            // if it isn't an NDB or VOR (eg, localizer) or it points to
                                            // a compatible marker, save the segment
                                            if (saveit) {
                                                distsq   = dsq;
                                                pointsTo = ptr;
                                            }
                                        }
                                    }
                                }

                                if (linksize == 2) break;
                            }
                        }
                    }

                    // if still no pointed-to spot, look for sharp point on the bubble somewhere
                    if (pointsTo == null) {
                        pointsTo = sharpPointOnCartoonBubble (loop, left, rite, top, bot);
                    }

                    // if we figured out where it points, move the enclosed string to that point,
                    // hopefully putting the string very close to the pointed-to marker
                    if (pointsTo != null) {
                        int ptx = pointsTo.index & 0xFFFF;
                        int pty = pointsTo.index >> 16;

                        for (PlateFix plateFix : plateFixes) {

                            // localizers might have a bent line so see if there is a second segment
                            // what takes us closer to the runway centerline, eg, IAD 'IAP-ILS OR LOC RWY 01R',
                            // but do not include a segment that is the runway itself, eg, MDT 'IAP-ILS OR LOC RWY 31'
                            if (plateFix.dbfix.name.startsWith ("I-") && (rwyCenterline != null)) {
                                int xx = ptx;
                                int yy = pty;
                                double bestdist = distToLine (xx, yy, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                                if (bestdist > 5) {
                                    for (LineSeg ls : allLineSegs) {
                                        int x, y;
                                        if ((ls.x0 == xx) && (ls.y0 == yy)) {
                                            x = ls.x1;
                                            y = ls.y1;
                                        } else if ((ls.x1 == xx) && (ls.y1 == yy)) {
                                            x = ls.x0;
                                            y = ls.y0;
                                        } else {
                                            continue;
                                        }
                                        double dist = distToLine (x, y, rwyCenterline.x0, rwyCenterline.y0, rwyCenterline.x1, rwyCenterline.y1);
                                        if (bestdist > dist) {
                                            bestdist = dist;
                                            ptx = x;
                                            pty = y;
                                        }
                                    }
                                }
                            }

                            LRTBInt lrtb = new LRTBInt (ptx - 2, ptx + 2, pty - 2, pty + 2);
                            plateFix.text.botleftx = ptx - 2;
                            plateFix.text.topleftx = ptx - 2;
                            plateFix.text.botritex = ptx + 2;
                            plateFix.text.topritex = ptx + 2;
                            plateFix.text.toplefty = pty - 2;
                            plateFix.text.topritey = pty - 2;
                            plateFix.text.botlefty = pty + 2;
                            plateFix.text.botritey = pty + 2;
                            plateFix.text.initBounds ();
                            plateFix.bubbled = true;

                            // for localizers, we might have to make a marker at that
                            // point if there isn't one
                            if (plateFix.dbfix.name.startsWith ("I-")) {
                                boolean foundit = false;
                                for (Marker marker : markers) {
                                    double dist = Math.hypot (marker.centx - ptx, marker.centy - pty);
                                    if (dist < csvDpi / 8.0) {
                                        for (String type : marker.types) {
                                            if (plateFix.dbfix.type.equals (type)) {
                                                foundit = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (foundit) break;
                                }
                                if (!foundit) {
                                    Marker marker   = new Marker (ptx, pty);
                                    marker.linesegs = new LinkedList<> ();
                                    marker.types    = new String[] { plateFix.dbfix.type };
                                    markers.addLast (marker);
                                }
                            }
                        }

                        rc = true;
                    }
                }
            }

            return rc;
        }

        private boolean edgeIsBezier (SegNode s0, SegNode s1)
        {
            int i0 = s0.index;
            int i1 = s1.index;
            long bez = (i0 < i1) ? (((long) i0 << 32) + i1) :
                                   (((long) i1 << 32) + i0);
            return bezierSegEdges.contains (bez);
        }

        /**
         * Find sharp point on a cartoon bubble loop.
         * @param loop = cartoon bubble
         * @param left = left edge of bubble's main box
         * @param rite = rite edge of bubble's main box
         * @param top  = top edge of bubble's main box
         * @param bot  = bot edge of bubble's main box
         */
        private SegNode sharpPointOnCartoonBubble (LinkedList<SegNode> loop, int left, int rite, int top, int bot)
        {
            // find nodes that are the leftmost, ritemost, topmost, botmost of the whole cartoon bubble
            SegNode leftmost = null;
            SegNode ritemost = null;
            SegNode topmost  = null;
            SegNode botmost  = null;
            for (SegNode node : loop) {
                if (leftmost == null) {
                    leftmost = ritemost = topmost = botmost = node;
                } else {
                    if ((leftmost.index & 0xFFFF) > (node.index & 0xFFFF)) leftmost = node;
                    if ((ritemost.index & 0xFFFF) < (node.index & 0xFFFF)) ritemost = node;
                    if ((topmost.index >> 16)     > (node.index >> 16))    topmost  = node;
                    if ((botmost.index >> 16)     < (node.index >> 16))    botmost  = node;
                }
            }

            // see how far each one of those sticks outside the bubble's main box
            int leftdiff = left - (leftmost.index & 0xFFFF);
            int ritediff = (ritemost.index & 0xFFFF) - rite;
            int topdiff  = top - (topmost.index >> 16);
            int botdiff  = (botmost.index >> 16) - bot;

            // find which of those sticks out the most, but at least 1/20th inch
            SegNode point = null;
            int diff = csvDpi / 20;
            if (diff < leftdiff) { diff = leftdiff; point = leftmost; }
            if (diff < ritediff) { diff = ritediff; point = ritemost; }
            if (diff < topdiff)  { diff = topdiff;  point = topmost;  }
            if (diff < botdiff)  { diff = botdiff;  point = botmost;  }

            return point;
        }

        /**
         * See if the given point intersects the loop somewhere (or is very close).
         * @param loop = loop to scan
         * @param x0,y0 = point to check for
         * @returns wheter the point is on the loop or not
         */
        private static boolean xyIntersectsLoop (LinkedList<SegNode> loop, int x0, int y0)
        {
            int x1 = 0;
            int y1 = 0;
            for (SegNode node : loop) {
                int x2 = node.index & 0xFFFF;
                int y2 = node.index >> 16;
                if ((x1 != 0) || (y1 != 0)) {
                    double dist = distToLineSeg (x0, y0, x1, y1, x2, y2);
                    if (dist < 5.0) return true;
                }
                x1 = x2;
                y1 = y2;
            }
            return false;
        }

        /**
         * Keep track of a path (series of lines, curves) as defined by PDF token stream.
         */
        public class Path {
            public LinkedList<PathOp> pathops = new LinkedList<> ();

            public int lastx, lasty;
            public int movex, movey;

            private LinkedList<LineSeg> bezlines;
            private int beztotn, beztotx, beztoty;
            private int bezmaxx, bezmaxy, bezminx, bezminy;

            /**
             * Draw the path out to the current display (g2d).
             */
            public void draw ()
            {
                bezminx = bezminy = 999999999;

                for (PathOp pathop : pathops) {
                    pathop.draw (this);
                }

                // if path has 4 beziers, it might be an NDB dot
                int bezsizx = bezmaxx - bezminx;
                int bezsizy = bezmaxy - bezminy;
                if ((beztotn == 8) && (bezsizx <= csvDpi / 60) && (bezsizy <= csvDpi / 60)) {
                    ndbDots.addLast (new Point (beztotx / beztotn, beztoty / beztotn));
                }

                // similarly, 8 beziers might be an NDB center
                if ((beztotn == 16) && (bezsizx <= csvDpi / 18) && (bezsizy <= csvDpi / 18)) {
                    int cx = beztotx / beztotn;
                    int cy = beztoty / beztotn;
                    Marker ndbCenter = null;
                    for (Marker nc : ndbCenters) {
                        if ((nc.centx == cx) && (nc.centy == cy)) {
                            ndbCenter = nc;
                            ndbCenter.instances ++;
                        }
                    }
                    if (ndbCenter == null) {
                        ndbCenter = new Marker (cx, cy);
                        ndbCenter.types = marker_types_ndbdots;
                        ndbCenter.linesegs = bezlines;
                        ndbCenters.addLast (ndbCenter);
                    }
                }

                // if path has 16 or 18 beziers and no bigger than 1/6 inch across, remember it
                // they are half of the 4-pointed star used for GPS fixes
                // they must be at least 1/10 inch across cuz there are tiny ones used for MSA which we don't care about and confuse us
                if (((beztotn == 32) || (beztotn == 36)) &&
                        (bezsizx > csvDpi / 10) && (bezsizy > csvDpi / 10) &&
                        (bezsizx < csvDpi /  6) && (bezsizy < csvDpi /  6)) {
                    float cx = (float) beztotx / (float) beztotn;
                    float cy = (float) beztoty / (float) beztotn;

                    boolean duplicate = false;
                    for (Bez16 b : lilBez16s) {
                        float dx = b.posx - cx;
                        float dy = b.posy - cy;
                        if (dx * dx + dy * dy < 9) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (!duplicate) {
                        Bez16 bez16 = new Bez16 ();
                        bez16.posx  = cx;
                        bez16.posy  = cy;
                        bez16.linesegs = bezlines;
                        lilBez16s.addLast (bez16);
                    }
                }

                // if path has 16 beziers and between 0.8 and 1.0 inch across, remember it
                // they are probably an MSA circle and we should ignore any marker therein
                if ((beztotn == 32) && (bezsizx > csvDpi * 0.8F) && (bezsizx < csvDpi * 1.0F) &&
                                       (bezsizy > csvDpi * 0.8F) && (bezsizy < csvDpi * 1.0F)) {
                    float cx = (float) beztotx / (float) beztotn;
                    float cy = (float) beztoty / (float) beztotn;
                    Bez16 bez16  = new Bez16 ();
                    bez16.posx   = cx;
                    bez16.posy   = cy;
                    bez16.radius = (bezsizx + bezsizy) / 4.0F;
                    bez16.linesegs = bezlines;
                    bigBez16s.addLast (bez16);
                }
            }

            /**
             * Save line segments.
             */
            public void gotLineSeg (int x0, int y0, int x1, int y1)
            {
                if (x0 < 2) x0 = 2;
                if (x1 < 2) x1 = 2;
                if (y0 < 2) y0 = 2;
                if (y1 < 2) y1 = 2;
                if (x0 > panelWidth  - 2) x0 = panelWidth  - 2;
                if (x1 > panelWidth  - 2) x1 = panelWidth  - 2;
                if (y0 > panelHeight - 2) y0 = panelHeight - 2;
                if (y1 > panelHeight - 2) y1 = panelHeight - 2;

                if ((x0 != x1) || (y0 != y1)) {
                    insertSegNode (x0, y0, x1, y1, false);
                    LineSeg ls = new LineSeg (x0, y0, x1, y1);
                    allLineSegs.addLast (ls);
                }
            }

            /**
             * Save Bezier curve start and end points.
             */
            public void gotBezier (int x0, int y0, int x3, int y3)
            {
                if (x0 < 2) x0 = 2;
                if (x3 < 2) x3 = 2;
                if (y0 < 2) y0 = 2;
                if (y3 < 2) y3 = 2;
                if (x0 > panelWidth  - 2) x0 = panelWidth  - 2;
                if (x3 > panelWidth  - 2) x3 = panelWidth  - 2;
                if (y0 > panelHeight - 2) y0 = panelHeight - 2;
                if (y3 > panelHeight - 2) y3 = panelHeight - 2;

                insertSegNode (x0, y0, x3, y3, true);

                beztotx += x0 + x3;
                beztoty += y0 + y3;
                beztotn += 2;

                if (bezlines == null) bezlines = new LinkedList<> ();
                LineSeg ls = new LineSeg (x0, y0, x3, y3);
                bezlines.addLast (ls);

                if (ls.length () < csvDpi / 7) {
                    smallBeziers.addLast (ls);
                }

                if (bezmaxx < x0) bezmaxx = x0;
                if (bezmaxx < x3) bezmaxx = x3;
                if (bezmaxy < y0) bezmaxy = y0;
                if (bezmaxy < y3) bezmaxy = y3;
                if (bezminx > x0) bezminx = x0;
                if (bezminx > x3) bezminx = x3;
                if (bezminy > y0) bezminy = y0;
                if (bezminy > y3) bezminy = y3;
            }

            /**
             * Add given segment to list of segment nodes, linked by segments.
             */
            private void insertSegNode (int x0, int y0, int x1, int y1, boolean isBezier)
            {
                // only black lines matter, skip stuff like watee
                if (isBlack) {
                    int i0 = (y0 << 16) + x0;
                    int i1 = (y1 << 16) + x1;
                    if (i0 != i1) {

                        // add x0,y0 to list of nodes if not there akready
                        SegNode s0 = segmentNodes.get (i0);
                        if (s0 == null) {
                            s0 = new SegNode ();
                            s0.index = i0;
                            segmentNodes.put (i0, s0);
                        }

                        // add x1,y1 to list of nodes if not there akready
                        SegNode s1 = segmentNodes.get (i1);
                        if (s1 == null) {
                            s1 = new SegNode ();
                            s1.index = i1;
                            segmentNodes.put (i1, s1);
                        }

                        // add edge from x0,y0 to x1,y1 if not there already
                        if (!s0.edges.containsKey (i1)) s0.edges.put (i1, s1);

                        // add edge from x1,y1 to x0,y0 if not there already
                        if (!s1.edges.containsKey (i0)) s1.edges.put (i0, s0);

                        // remember if edge is bezier curve or not
                        if (isBezier) {
                            long bez = (i0 < i1) ? (((long) i0 << 32) + i1) :
                                                   (((long) i1 << 32) + i0);
                            bezierSegEdges.add (bez);
                        }
                    }
                }
            }
        }

        private static abstract class PathOp {
            public abstract void draw (PagePanel.Path path);
        }

        private class PathOpLine extends PathOp {
            public int x, y;

            @Override
            public void draw (PagePanel.Path path)
            {
                g2d.setColor (isBlack ? Color.BLACK : Color.GRAY);
                path.gotLineSeg (path.lastx, path.lasty, x, y);
                g2d.drawLine (path.lastx, path.lasty, x, y);
                path.lastx = x;
                path.lasty = y;
            }
        }

        private static class PathOpMove extends PathOp {
            public int x, y;

            @Override
            public void draw (PagePanel.Path path)
            {
                path.lastx = x;
                path.lasty = y;
                path.movex = x;
                path.movey = y;
            }
        }

        private static class PathOpRect extends PathOp {
            public int x, y, u, v;

            @Override
            public void draw (PagePanel.Path path)
            {
                path.lastx = x;
                path.lasty = y;
                path.movex = x;
                path.movey = y;
            }
        }

        private class PathOpBezier extends PathOp {
            public boolean typev;
            public int x1,y1, x2,y2, x3,y3;

            @Override
            public void draw (PagePanel.Path path)
            {
                int x0 = path.lastx;
                int y0 = path.lasty;
                if (typev) {
                    x1 = x0;
                    y1 = y0;
                }

                g2d.setColor (isBlack ? Color.BLACK : Color.GRAY);
                path.gotBezier (x0, y0, x3, y3);
                int xx = x0;
                int yy = y0;
                for (float t = 0; (t += 1.0F/8) <= 1.0F;) {
                    float nt = 1.0F - t;
                    int x = (int) (nt * nt * nt * x0 + 3 * t * nt * nt * x1 + 3 * t * t * nt * x2 + t * t * t * x3 + 0.5F);
                    int y = (int) (nt * nt * nt * y0 + 3 * t * nt * nt * y1 + 3 * t * t * nt * y2 + t * t * t * y3 + 0.5F);
                    g2d.drawLine (xx, yy, x, y);
                    xx = x;
                    yy = y;
                }

                path.lastx = x3;
                path.lasty = y3;
            }
        }
    }

    private static String PadLeft (String s, int w)
    {
        while (s.length () < w) s = " " + s;
        return s;
    }
    private static String PadRight (String s, int w)
    {
        while (s.length () < w) s += " ";
        return s;
    }

    /**
     * Get positive difference between two angles
     * @param a = one angle radians
     * @param b = other angle radians
     * @returns number of radians difference, 0..PI
     */
    private static double AngleDiff (double a, double b)
    {
        // get difference of angles
        // a difference of -1^ is the same as a difference of 1^
        // a difference of -179^ is same as difference of 179^
        double d = Math.abs (a - b);

        // a difference of 361^ is the same as difference of 1^
        while (d >= Math.PI * 2) d -= Math.PI * 2;

        // a difference of 359^ is the same as a difference of 1^
        // a difference of 181^ is same as difference of 179^
        if (d >= Math.PI) d = Math.PI * 2 - d;

        return d;
    }


    /**
     * Find point at intersection of extensions of the given line segments.
     */
    private static int lineIntersectX (LineSeg la, LineSeg lb)
    {
        double x = lineIntersectX (la.x0, la.y0, la.x1, la.y1, lb.x0, lb.y0, lb.x1, lb.y1);
        return (int) (x + 0.5);
    }

    private static int lineIntersectY (LineSeg la, LineSeg lb)
    {
        double y = lineIntersectY (la.x0, la.y0, la.x1, la.y1, lb.x0, lb.y0, lb.x1, lb.y1);
        return (int) (y + 0.5);
    }

    /**
     * Find point at intersection of two lines given two points on each line.
     */
    private static double lineIntersectX (double ax0, double ay0, double ax1, double ay1, double bx0, double by0, double bx1, double by1)
    {
        // (y - y0) / (x - x0) = (y1 - y0) / (x1 - x0)
        // (y - y0) = (y1 - y0) / (x1 - x0) * (x - x0)
        // y = (y1 - y0) / (x1 - x0) * (x - x0) + y0

        // (ay1 - ay0) / (ax1 - ax0) * (x - ax0) + ay0 = (by1 - by0) / (bx1 - bx0) * (x - bx0) + by0
        // (ay1 - ay0) / (ax1 - ax0) * x - (ay1 - ay0) / (ax1 - ax0) * ax0 + ay0 = (by1 - by0) / (bx1 - bx0) * x - (by1 - by0) / (bx1 - bx0) * bx0 + by0
        // [(ay1 - ay0) / (ax1 - ax0) - (by1 - by0) / (bx1 - bx0)] * x = (ay1 - ay0) / (ax1 - ax0) * ax0 - (by1 - by0) / (bx1 - bx0) * bx0 + by0 - ay0
        // [(ay1 - ay0) - (by1 - by0) / (bx1 - bx0) * (ax1 - ax0)] * x = (ay1 - ay0) * ax0 - (by1 - by0) / (bx1 - bx0) * (ax1 - ax0) * bx0 + (by0 - ay0) * (ax1 - ax0)
        // [(ay1 - ay0) * (bx1 - bx0) - (by1 - by0) * (ax1 - ax0)] * x = (ay1 - ay0) * (bx1 - bx0) * ax0 - (by1 - by0) * (ax1 - ax0) * bx0 + (by0 - ay0) * (ax1 - ax0) * (bx1 - bx0)
        // x = [(ay1 - ay0) * (bx1 - bx0) * ax0 - (by1 - by0) * (ax1 - ax0) * bx0 + (by0 - ay0) * (ax1 - ax0) * (bx1 - bx0)] / [(ay1 - ay0) * (bx1 - bx0) - (by1 - by0) * (ax1 - ax0)]

        double ax1_ax0 = ax1 - ax0;
        double ay1_ay0 = ay1 - ay0;
        double bx1_bx0 = bx1 - bx0;
        double by1_by0 = by1 - by0;
        return (ay1_ay0 * bx1_bx0 * ax0 - by1_by0 * ax1_ax0 * bx0 + (by0 - ay0) * ax1_ax0 * bx1_bx0) / (ay1_ay0 * bx1_bx0 - by1_by0 * ax1_ax0);
    }

    private static double lineIntersectY (double ax0, double ay0, double ax1, double ay1, double bx0, double by0, double bx1, double by1)
    {
        double ax1_ax0 = ax1 - ax0;
        double ay1_ay0 = ay1 - ay0;
        double bx1_bx0 = bx1 - bx0;
        double by1_by0 = by1 - by0;
        return (ax1_ax0 * by1_by0 * ay0 - bx1_bx0 * ay1_ay0 * by0 + (bx0 - ax0) * ay1_ay0 * by1_by0) / (ax1_ax0 * by1_by0 - bx1_bx0 * ay1_ay0);
    }

    /**
     * Get distance from given point to given line segment.
     * @param x0,y0 = point to check for
     * @param x1,y1/x2,y2 = line segment
     * @returns how far away from line segment point is
     */
    private static double distToLineSeg (double x0, double y0, double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;

        double r;
        if (Math.abs (dx) > Math.abs (dy)) {

            // primarily horizontal

            double mhor = (double) dy / (double) dx;

            // calculate intersection point

            // (y - y1) / (x - x1) = mhor  =>  y - y1 = (x - x1) * mhor  =>  y = (x - x1) * mhor + y1
            // (x0 - x) / (y - y0) = mhor  =>  y - y0 = (x0 - x) / mhor  =>  y = (x0 - x) / mhor + y0

            // (x - x1) * mhor + y1 = (x0 - x) / mhor + y0
            // (x - x1) * mhor^2 + y1 * mhor = (x0 - x) + y0 * mhor
            // x * mhor^2 - x1 * mhor^2 + y1 * mhor = x0 - x + y0 * mhor
            // x * (mhor^2 + 1) - x1 * mhor^2 + y1 * mhor = x0 + y0 * mhor
            // x * (mhor^2 + 1) = x0 + y0 * mhor + x1 * mhor^2 - y1 * mhor
            // x * (mhor^2 + 1) = x1 * mhor^2 + y0 * mhor - y1 * mhor + x0
            // x * (mhor^2 + 1) = x1 * mhor^2 + (y0 - y1) * mhor + x0

            double x = (x1 * mhor * mhor + (y0 - y1) * mhor + x0) / (mhor * mhor + 1);

            // where intercept is on line relative to P1 and P2
            // r=0.0: right on P1; r=1.0: right on P2

            r = (x - x1) / dx;
        } else {

            // primarily vertical

            double mver = (double) dx / (double) dy;

            // calculate intersection point

            // (x - x1) / (y - y1) = mver  =>  x - x1 = (y - y1) * mver  =>  x = (y - y1) * mver + x1
            // (y0 - y) / (x - x0) = mver  =>  x - x0 = (y0 - y) / mver  =>  x = (y0 - y) / mver + x0

            // (y - y1) * mver + x1 = (y0 - y) / mver + x0
            // (y - y1) * mver^2 + x1 * mver = (y0 - y) + x0 * mver
            // y * mver^2 - y1 * mver^2 + x1 * mver = y0 - y + x0 * mver
            // y * (mver^2 + 1) - y1 * mver^2 + x1 * mver = y0 + x0 * mver
            // y * (mver^2 + 1) = y0 + x0 * mver + y1 * mver^2 - x1 * mver
            // y * (mver^2 + 1) = y1 * mver^2 + x0 * mver - x1 * mver + y0
            // y * (mver^2 + 1) = y1 * mver^2 + (x0 - x1) * mver + y0

            double y = (y1 * mver * mver + (x0 - x1) * mver + y0) / (mver * mver + 1);

            // where intercept is on line relative to P1 and P2
            // r=0.0: right on P1; r=1.0: right on P2

            r = (y - y1) / dy;
        }

        if (r <= 0.0) return Math.hypot (x0 - x1, y0 - y1);
        if (r >= 1.0) return Math.hypot (x0 - x2, y0 - y2);

        // https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        return Math.abs (dy * x0 - dx * y0 + x2 * y1 - y2 * x1) / Math.hypot (dy, dx);
    }

    private static double distToLine (double x0, double y0, double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;

        // https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
        return Math.abs (dy * x0 - dx * y0 + x2 * y1 - y2 * x1) / Math.hypot (dy, dx);
    }

    /**
     * Get the squared distance between two points.
     */
    private static int distSqBtwnPts (int x1, int y1, int x2, int y2)
    {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    /**
     * Text string drawn to plate.
     */
    private static class TextString {
        public int botleftx, botlefty;
        public int botritex, botritey;
        public int topleftx, toplefty;
        public int topritex, topritey;
        public String rawstr;  // string itself
        public String[] words;

        // bounding boxes of the various lines of text
        private LinkedList<LRTBInt> bounds;

        public double height ()
        {
            return Math.hypot (topleftx - botleftx, toplefty - botlefty);
        }

        public double width ()
        {
            return Math.hypot (botritex - botleftx, botritey - botlefty);
        }

        public int centerx ()
        {
            return (botleftx + botritex + topleftx + topritex) / 4;
        }

        public int centery ()
        {
            return (botlefty + botritey + toplefty + topritey) / 4;
        }

        // - angle (radians)
        //       0: horizontal non-inverted
        //      >0: tilted anti-clockwise (eg, the "MSA" in "MSA xxx xxx NM" above an MSA circle)
        //      <0: tilted clockwise (eg, the "NM" in "MSA xxx xxx NM" above an MSA circle)
        //  PI,-PI: horizontal but inverted
        public double angle ()
        {
            return Math.atan2 (botlefty - botritey, botritex - botleftx);
        }

        /**
         * Initialize boundaries to whatever the overall boundaries are.
         * ONLY VALID FOR ANGLE=0 STRINGS.
         */
        public void initBounds ()
        {
            bounds = new LinkedList<> ();
            bounds.addLast (new LRTBInt (botleftx, botritex, toplefty, botlefty));
        }

        /**
         * Merge in a text string that is just below this text string.
         * ONLY VALID FOR ANGLE=0 STRINGS.
         */
        public void mergeBot (TextString bot)
        {
            // merge edges
            topleftx = botleftx = Math.min (Math.min (topleftx, bot.topleftx), Math.min (botleftx, bot.botleftx));
            topritex = botritex = Math.max (Math.max (topritex, bot.topritex), Math.max (botritex, bot.botritex));
            toplefty = topritey = Math.min (Math.min (toplefty, bot.toplefty), Math.min (topritey, bot.topritey));
            botlefty = botritey = Math.max (Math.max (botlefty, bot.botlefty), Math.max (botritey, bot.botritey));

            // merge raw strings
            rawstr += "\n" + bot.rawstr;

            // merge words lists
            String[] topwords = words;
            String[] botwords = bot.words;
            String[] allwords = new String[topwords.length+botwords.length];
            System.arraycopy (topwords, 0, allwords, 0, topwords.length);
            System.arraycopy (botwords, 0, allwords, topwords.length, botwords.length);
            words = allwords;

            // merge boundaries
            bounds.addAll (bot.bounds);
        }

        /**
         * Get distance of a given point to this text string.
         * ONLY VALID FOR ANGLE=0 STRINGS.
         */
        public double distOfPoint (int x, int y)
        {
            double bestdist = 999999999.0;
            for (LRTBInt bound : bounds) {
                double dl = distToLineSeg (x, y, bound.left, bound.top, bound.left, bound.bot);
                double dr = distToLineSeg (x, y, bound.rite, bound.bot, bound.rite, bound.top);
                double dt = distToLineSeg (x, y, bound.rite, bound.top, bound.left, bound.top);
                double db = distToLineSeg (x, y, bound.left, bound.bot, bound.rite, bound.bot);
                double dist = Math.min (Math.min (dl, dr), Math.min (dt, db));
                if (bestdist > dist) bestdist = dist;
            }
            return bestdist;
        }

        // for debugging only
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder ();

            sb.append ('<');
            if (words == null) sb.append (rawstr);
            else if (words.length == 0) { }
            else if (words.length == 1) sb.append (words[0]);
            else {
                sb.append (words[0]);
                for (int i = 0; ++ i < words.length;) {
                    sb.append (' ');
                    sb.append (words[i]);
                }
            }
            sb.append ("> ");
            sb.append (centerx ());
            sb.append (',');
            sb.append (centery ());
            sb.append (" angle=");
            sb.append (Math.round (Math.toDegrees (angle ())));
            return sb.toString ();
        }

        public void drawBox (Graphics2D g2d)
        {
            g2d.drawLine (botleftx, botlefty, botritex, botritey);
            g2d.drawLine (botritex, botritey, topritex, topritey);
            g2d.drawLine (topritex, topritey, topleftx, toplefty);
            g2d.drawLine (topleftx, toplefty, botleftx, botlefty);
        }
    }

    private static class LRTBInt {
        public int left, rite, top, bot;

        public LRTBInt () { }

        public LRTBInt (int l, int r, int t, int b)
        {
            left = l;
            rite = r;
            top  = t;
            bot  = b;
        }

        public void drawBox (Graphics2D g2d)
        {
            g2d.drawLine (left, top, rite, top);
            g2d.drawLine (rite, top, rite, bot);
            g2d.drawLine (rite, bot, left, bot);
            g2d.drawLine (left, bot, left, top);
        }
    }

    /**
     * Some artwork on the plate indicating a fix (incl navaid) position.
     */
    private static class Marker {
        public double quality;                // if there is a lightning bolt pointing to this marker
                                              // ... this is the distance from the near end of the bolt
                                              // ... to the marker (should be just a couple pixels)
                                              // low number (ideally zero) means high quality

        public int centx, centy;              // centerpoint
        public int lookx, looky;              // where to look for corresponding platefix string
        public int instances;                 // number of times this marker has been seen
        public LinkedList<LineSeg> linesegs;  // line segments that make up the marker
        public String[] types;                // types of fixes it can match

        public Marker (int xx, int yy)
        {
            centx = xx;
            centy = yy;
            instances = 1;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder ();
            boolean first = true;
            for (String mt : types) {
                if (!first) sb.append (' ');
                sb.append (mt);
                first = false;
            }
            sb.append (" at ");
            sb.append (centx);
            sb.append (',');
            sb.append (centy);
            if ((lookx != 0) || (looky != 0)) {
                sb.append (" look ");
                sb.append (lookx);
                sb.append (',');
                sb.append (looky);
            }
            return sb.toString ();
        }
    }

    /**
     * Line segments found (there are lots of these).
     */
    private static class LineSeg {
        public final int x0, y0;
        public final int x1, y1;

        public LineSeg (int xx0, int yy0, int xx1, int yy1)
        { x0 = xx0; y0 = yy0; x1 = xx1; y1 = yy1; }

        public double length ()
        {
            return Math.hypot (x0 - x1, y0 - y1);
        }

        public LineSeg reverse ()
        {
            return new LineSeg (x1, y1, x0, y0);
        }

        @Override
        public String toString ()
        {
            return x0 + "," + y0 + " " + x1 + "," + y1;
        }
    }

    /**
     * These are line segments aligned with the runway.
     * We keep track of how much of them is actually drawn
     * as there are usually gaps in the line.
     */
    private static class AlignedSeg extends LineSeg {
        public double drawn;

        public AlignedSeg (int x0, int y0,int x1, int y1, double d)
        {
            super (x0, y0, x1, y1);
            drawn = d;
        }
    }

    /**
     * Network of lines and Beziers.
     */
    private static class SegNode {
        public HashMap<Integer,SegNode> edges = new HashMap<> ();
        public int index;  // index<31:16>=y; <15:00>=x
        public int pass;

        @Override
        public String toString ()
        {
            int x = index & 0xFFFF;
            int y = index >> 16;
            return x + "," + y;
        }
    }

    /**
     * These plate fixes are fenced off.  The only thing wrong with them is their distance
     * along the line broken by the fence.  So as a last resort we can use them to resolve
     * the plate lat/lon <-> pixel mapping.
     */
    private static class FencedPlateFix implements GetDBFix {
        public final static double minang = Math.toRadians (15.0);

        /*
         * Equation to stay on line from marker through fence:
         *   pixx - fencedFix.posx = tan truehdg * (fenceFix.posy - pixy)
         */
        public double truehdg;          // true hdg of line thru fence, one way or the other (radians)
        public double solvedx,solvedy;  // what pixel location the marker really should be at along the line
        public PlateFix fencedFix;      // fix that got fenced off
                                        //   .dbfix.lat/lon = latitude/longitude
                                        //   .posx,y = pixel location of fenced off marker

        /**
         * We can locate the fenced fix if we have a known fix, because the marker
         * is actually located along the line coming out of the fenced fix that goes
         * through the fence (actually back out the other way).
         */
        public double solve (PlateFix knownFix)
        {
            /*
             * Calc heading of line through known fix that the fenced fix can be on
             * and maintain the correct lat/lon ratio for square pixels.
             * Note that +Y is down, eg, if fenced fix is SE of known fix or vice
             * versa, the slope is positive.
             *
             * Equation for this line would then be:
             *   pixx - knownFix.posx = tan llthdg * (knownFix.posy - pixy)
             */
            double disty  =  fencedFix.dbfix.lat - knownFix.dbfix.lat;
            double distx  = (fencedFix.dbfix.lon - knownFix.dbfix.lon) * Math.cos (Math.toRadians (airport.lat));
            double llthdg = Math.atan2 (distx, disty);

            /*
             * If small difference in angle, we won't get a good result.
             */
            double rc = Math.abs (llthdg - truehdg);
            while (rc >= Math.PI * 2) rc -= Math.PI * 2;
            if (rc >= Math.PI) rc = Math.PI * 2 - rc;  // 359^ diff is same as 1^ diff
            if (rc >= Math.PI / 2) rc = Math.PI - rc;  // 179^ diff is same as 1^ diff
            if (rc >= minang) {

                /*
                 * Intersect the fence and the known lines to get the fence point.
                 */
                double fencedFix_otherx = fencedFix.posx + Math.sin (truehdg) * 256.0;
                double fencedFix_othery = fencedFix.posy - Math.cos (truehdg) * 256.0;
                double knownFix_otherx  = knownFix.posx  + Math.sin (llthdg)  * 256.0;
                double knownFix_othery  = knownFix.posy  - Math.cos (llthdg)  * 256.0;

                solvedx = lineIntersectX (fencedFix.posx, fencedFix.posy, fencedFix_otherx, fencedFix_othery,
                                          knownFix.posx,  knownFix.posy,  knownFix_otherx,  knownFix_othery);
                solvedy = lineIntersectY (fencedFix.posx, fencedFix.posy, fencedFix_otherx, fencedFix_othery,
                                          knownFix.posx,  knownFix.posy,  knownFix_otherx,  knownFix_othery);
            }
            return rc;
        }

        @Override  // GetDBFix
        public DBFix getDBFix () { return fencedFix.getDBFix (); }
    }

    /**
     * Utilities to get the value froma COSBase object.
     */
    private static float GetFloatValue (COSBase obj)
    {
        if (obj instanceof COSInteger) return ((COSInteger) obj).intValue ();
        if (obj instanceof COSFloat)   return ((COSFloat)   obj).floatValue ();
        throw new IllegalArgumentException (obj.getClass ().toString ());
    }

    private static int GetIntegerValue (COSBase obj)
    {
        if (obj instanceof COSInteger) return ((COSInteger) obj).intValue ();
        if (obj instanceof COSFloat)   return ((COSFloat)   obj).intValue ();
        throw new IllegalArgumentException (obj.getClass ().toString ());
    }

    private static String GetNameValue (COSBase obj)
    {
        if (obj instanceof COSName) return ((COSName) obj).getName ();
        throw new IllegalArgumentException (obj.getClass ().toString ());
    }

    private static String GetStringValue (COSBase obj)
    {
        if (obj instanceof COSString) return ((COSString) obj).getString ();
        throw new IllegalArgumentException (obj.getClass ().toString ());
    }

    /**
     * Graphics state as required for processing PDF page token stream.
     * We only really care about the transform stuff.
     */
    private static class GraphicsState {
        public float ctm_a, ctm_b, ctm_c, ctm_d, ctm_e, ctm_f;
        public float lineWidth;
        public float miterLimit;
        public int dashPhase;
        public int lineCapStyle;
        public int lineJoinStyle;
        public int[] dashArray;

        public GraphicsState clone ()
        {
            GraphicsState c = new GraphicsState ();
            c.lineWidth     = lineWidth;
            c.lineCapStyle  = lineCapStyle;
            c.lineJoinStyle = lineJoinStyle;
            c.miterLimit    = miterLimit;
            c.dashArray     = dashArray;
            c.dashPhase     = dashPhase;
            c.ctm_a         = ctm_a;
            c.ctm_b         = ctm_b;
            c.ctm_c         = ctm_c;
            c.ctm_d         = ctm_d;
            c.ctm_e         = ctm_e;
            c.ctm_f         = ctm_f;
            return c;
        }

        public AffineTransform getAffine ()
        {
            return new AffineTransform (ctm_a, ctm_b, ctm_c, ctm_d, ctm_e, ctm_f);
        }

        // [ na nb 0 ]   [ da db 0 ] [ ca cb 0 ]
        // [ nc nd 0 ] = [ dc dd 0 ] [ cc cd 0 ]
        // [ ne nf 1 ]   [ de df 1 ] [ ce cf 1 ]

        public void concat (float da, float db, float dc, float dd, float de, float df)
        {
            float na = da * ctm_a + db * ctm_c;
            float nc = dc * ctm_a + dd * ctm_c;
            float ne = de * ctm_a + df * ctm_c + ctm_e;

            float nb = da * ctm_b + db * ctm_d;
            float nd = dc * ctm_b + dd * ctm_d;
            float nf = de * ctm_b + df * ctm_d + ctm_f;

            ctm_a = na;
            ctm_b = nb;
            ctm_c = nc;
            ctm_d = nd;
            ctm_e = ne;
            ctm_f = nf;
        }

        //                           [ ca cb 0 ]
        // [ dx dy 1 ] = [ ux uy 1 ] [ cc cd 0 ]
        //                           [ ce cf 1 ]

        public int devx (float ux, float uy)
        {
            return (int) (ctm_a * ux + ctm_c * uy + ctm_e + 0.5F);
        }

        public int devy (float ux, float uy)
        {
            return (int) (ctm_b * ux + ctm_d * uy + ctm_f + 0.5F);
        }
    }

    /**
     * Airports and runways from database.
     */
    private static class Airport extends DBFix {
        public String faaid;
        public HashMap<String,Runway> runways;

        public Airport ()
        {
            type    = "AIRPORT";
            runways = new HashMap<> ();
        }

        /**
         * Add runway database definition to this airport.
         */
        public void addRunway (String number, double beglat, double beglon, double endlat, double endlon)
        {
            /*
             * Make a DBFix for the runway so it can be used as a GPS fix.
             */
            Runway rwy = new Runway ();
            rwy.name   = number;  // eg, "RW04R"
            rwy.lat    = beglat;
            rwy.lon    = beglon;
            rwy.endlat = endlat;
            rwy.endlon = endlon;
            runways.put (number, rwy);
        }
    }

    /**
     * Single runway direction for an airport.
     */
    private static class Runway extends DBFix {
        public double endlat, endlon;

        public Runway ()
        {
            type = "RUNWAY";
        }
    }

    /**
     * A fix as defined in database (including navaids).
     */
    private interface GetDBFix {
        DBFix getDBFix ();
    }

    private static class DBFix implements GetDBFix {
        public final static double MAGVAR_MISSING = 999999.0;

        public String name;
        public String type;
        public double lat, lon;
        public double magvar = MAGVAR_MISSING;  // magnetic = true + magvar
        public boolean mentioned;

        @Override  // GetDBFix
        public DBFix getDBFix () { return this; }
    }

    private static class DMEDBFix extends DBFix {
        public DBFix navaid;
        public float distnm;

        // make up the dme fix's name given that the lat/lon and navaid are filled in
        public void makeName ()
        {
            // get base navaid
            DBFix nav = navaid;
            while (nav instanceof DMEDBFix) {
                nav = ((DMEDBFix) nav).navaid;
            }

            // get distance and heading from the base navaid to this dme fix
            double dist = Lib.LatLonDist (nav.lat, nav.lon, lat, lon);
            double hdg  = Lib.LatLonTC   (nav.lat, nav.lon, lat, lon);
            if (hdg < 0.0) hdg += 360.0;

            // make our name from the base navaid, distance and heading
            String diststr = String.format ("%3.1f", dist);
            String hdgstr  = String.format ("%3.1f", hdg);
            if (diststr.endsWith (".0")) diststr = diststr.substring (0, diststr.length () - 2);
            if (hdgstr.endsWith  (".0")) hdgstr  = hdgstr.substring  (0, hdgstr.length  () - 2);
            name = nav.name + dmeMark + diststr + "/" + hdgstr;
        }
    }

    /**
     * A particular fix string found on a plate.
     */
    private static class PlateFix implements GetDBFix {
        public boolean bubbled; // has been displaced from original string via cartoon bubble
        public DBFix dbfix;     // fix's name/lat/lon/type from database
        public int posx, posy;  // position on chart of the lat/lon (from marker)
        public TextString text; // where the string is

        public double quality;  // total pixels separating the string from the marker
                                // if cartoon bubble or lightning bolt, does not include
                                // the length of the bubble or lightning bolt, just
                                // includes the gaps

        @Override  // GetDBFix
        public DBFix getDBFix () { return dbfix; }
    }

    /**
     * A path of 16 or 18 Bezier curves (half of the 4-pointed star that marks a GPS waypoint).
     */
    private static class Bez16 {
        public float posx, posy, radius;
        LinkedList<LineSeg> linesegs;
    }

    /**
     * One of the boxes used to find the boxed-off areas.
     */
    private static class BoxedArea {
        public boolean counted;
        public BoxedArea leftxlink, ritexlink, topylink, botylink;
        public int leftxcoord, ritexcoord, topycoord, botycoord;
    }

    /**
     * Part of a ring of dashed line segments that might be part of a FEEDER FACILITIES ring.
     */
    private static class Ring {
        public final static double qualinch = 0.125;  // segments must be within 1/8 in of the radius

        public LinkedList<LineSeg> segments = new LinkedList<> ();

        private double centerx;
        private double centery;
        private double radius = -1;

        /**
         * Determine if it is a valid arc or not.
         */
        public boolean isValid ()
        {
            if (segments.size () < 3) return false;

            /*
             * Get centerpoint and radius assuming it is an arc.
             */
            double xc = getCenterX ();
            double yc = getCenterY ();
            double rad = getRadius ();

            if (rad < csvDpi * 1.5) return false;

            /*
             * See if it is valid in that arc.
             */
            double quality = getQuality (xc, yc, rad);
            return quality / segments.size () < csvDpi * qualinch;
        }

        /**
         * Assuming it is an arc, get its centerpoint.
         */
        public double getCenterX ()
        {
            if (radius < 0) calculate ();
            return centerx;
        }

        /**
         * Assuming it is an arc, get its centerpoint.
         */
        public double getCenterY ()
        {
            if (radius < 0) calculate ();
            return centery;
        }

        /**
         * Assuming it is an arc, get its radius.
         */
        public double getRadius ()
        {
            if (radius < 0) calculate ();
            return radius;
        }

        /**
         * See if a super ring can be formed from sub-rings.
         */
        public static Ring findSuperRing (Ring[] validRingArray, double maxRadius)
        {
            /*
             * It has to have a radius less than maxRadius.
             */
            int    bestCombo   = 0;
            double bestRadius  = maxRadius;
            double bestXCenter = 0;
            double bestYCenter = 0;

            /*
             * Search every combination of valid sub-rings.
             */
            int nSubRings = validRingArray.length;
            if (nSubRings > 15) return null;
            for (int i = 0; ++ i < (1 << nSubRings);) {

                /*
                 * Scan through the sub-rings of this combination.
                 * Get the average center and total number of line segments that make up the combination.
                 */
                int nSegments = 0;
                double superRadius  = 0;
                double superXCenter = 0;
                double superYCenter = 0;
                for (int j = 0; j < nSubRings; j ++) {
                    if (((i >> j) & 1) != 0) {
                        Ring r        = validRingArray[j];
                        int weight    = r.segments.size ();
                        nSegments    += weight;
                        superRadius  += r.getRadius  () * weight;
                        superXCenter += r.getCenterX () * weight;
                        superYCenter += r.getCenterY () * weight;
                    }
                }

                /*
                 * Make sure the combination has at least 10 line segments.
                 */
                if (nSegments >= 10) {
                    superRadius  /= nSegments;
                    superXCenter /= nSegments;
                    superYCenter /= nSegments;

                    /*
                     * If it has a smaller radius than any other combo, see if they really form a ring.
                     * All the line segments should be approx superRadius from (superXCenter,superYCenter).
                     */
                    if (bestRadius > superRadius) {
                        double quality = 0.0;
                        for (int j = 0; j < nSubRings; j ++) {
                            if (((i >> j) & 1) != 0) {
                                Ring r = validRingArray[j];
                                quality += r.getQuality (superXCenter, superYCenter, superRadius);
                            }
                        }

                        /*
                         * If all segments are correct, save it as the smallest ring so far.
                         * We want the smallest (eg, FEEDER FACILITIES), not the largest (eg, ENROUTE FACILITIES).
                         */
                        if (quality / nSegments < csvDpi * qualinch) {
                            bestCombo   = i;
                            bestRadius  = superRadius;
                            bestXCenter = superXCenter;
                            bestYCenter = superYCenter;
                        }
                    }
                }
            }

            /*
             * If we found one, return its center and radius.
             */
            Ring s = null;
            if (bestCombo > 0) {
                s = new Ring ();
                s.centerx = bestXCenter;
                s.centery = bestYCenter;
                s.radius  = bestRadius;
            }
            return s;
        }

        /**
         * Calculate this ring's center x, y and its radius.
         */
        private void calculate ()
        {
            /*
             * Get endpoints and midpoint.
             */
            int nsegs = segments.size ();

            LineSeg begseg = segments.getFirst ();
            LineSeg endseg = segments.getLast  ();

            int x0 = begseg.x0;
            int y0 = begseg.y0;
            int x2 = endseg.x1;
            int y2 = endseg.y1;
            int x1, y1;
            if ((nsegs & 1) != 0) {
                LineSeg midseg = segments.get (nsegs / 2);
                x1 = (midseg.x0 + midseg.x1) / 2;
                y1 = (midseg.y0 + midseg.y1) / 2;
            } else {
                LineSeg lomidseg = segments.get (nsegs / 2 - 1);
                LineSeg himidseg = segments.get (nsegs / 2);
                x1 = (lomidseg.x1 + himidseg.x0) / 2;
                y1 = (lomidseg.y1 + himidseg.y0) / 2;
            }

            /*
             * Use those 3 points to calculate centerpoint and radius of possible ring.
             *
             *  (x0 - xc)^2 + (y0 - yc)^2 = r^2
             *  (x1 - xc)^2 + (y1 - yc)^2 = r^2
             *  (x2 - xc)^2 + (y2 - yc)^2 = r^2
             *
             *  (x0 - xc)^2 + (y0 - yc)^2 = (x1 - xc)^2 + (y1 - yc)^2
             *  (x0 - xc)^2 - (x1 - xc)^2 = (y1 - yc)^2 - (y0 - yc)^2
             *  x0^2 - 2*x0*xc + xc^2 - x1^2 + 2*x1*xc - xc^2 = (y1 - yc)^2 - (y0 - yc)^2
             *  - 2*x0*xc + 2*x1*xc = (y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2
             *  2*x1*xc - 2*x0*xc = (y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2
             *  2*xc*(x1 - x0) = (y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2
             *  xc = ((y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2) / (2*(x1 - x0))
             *
             *  xc = ((y1 - yc)^2 - (y2 - yc)^2 - x2^2 + x1^2) / (2*(x1 - x2))
             *
             *  ((y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2) / (2*(x1 - x0)) = ((y1 - yc)^2 - (y2 - yc)^2 - x2^2 + x1^2) / (2*(x1 - x2))
             *  ((y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2) * 2 * (x1 - x2) = ((y1 - yc)^2 - (y2 - yc)^2 - x2^2 + x1^2) * 2 * (x1 - x0)
             *  ((y1 - yc)^2 - (y0 - yc)^2 - x0^2 + x1^2) * (x1 - x2) = ((y1 - yc)^2 - (y2 - yc)^2 - x2^2 + x1^2) * (x1 - x0)
             *  ((y1^2 - 2*y1*yc + yc^2) - (y0^2 - 2*y0*yc + yc^2) - x0^2 + x1^2) * (x1 - x2) = ((y1^2 - 2*y1*yc + yc^2) - (y2^2 - 2*y2*yc + yc^2) - x2^2 + x1^2) * (x1 - x0)
             *  (y1^2 - 2*y1*yc + yc^2 - y0^2 + 2*y0*yc - yc^2 - x0^2 + x1^2) * (x1 - x2) = (y1^2 - 2*y1*yc + yc^2 - y2^2 + 2*y2*yc - yc^2 - x2^2 + x1^2) * (x1 - x0)
             *  (y1^2 - 2*y1*yc - y0^2 + 2*y0*yc - x0^2 + x1^2) * (x1 - x2) = (y1^2 - 2*y1*yc - y2^2 + 2*y2*yc - x2^2 + x1^2) * (x1 - x0)
             *  (yc*(2*y0 - 2*y1) + y1^2 - y0^2 - x0^2 + x1^2) * (x1 - x2) = (yc*(2*y2 - 2*y1) + y1^2 - y2^2 - x2^2 + x1^2) * (x1 - x0)
             *  (yc*(2*y0 - 2*y1) + (y1^2 - y0^2 - x0^2 + x1^2)) * (x1 - x2) = (yc*(2*y2 - 2*y1) + (y1^2 - y2^2 - x2^2 + x1^2)) * (x1 - x0)
             *  yc * (2*y0 - 2*y1) * (x1 - x2) + (y1^2 - y0^2 - x0^2 + x1^2) * (x1 - x2) = yc * (2*y2 - 2*y1) * (x1 - x0) + (y1^2 - y2^2 - x2^2 + x1^2) * (x1 - x0)
             *  yc * (2*y0 - 2*y1) * (x1 - x2) - yc * (2*y2 - 2*y1) * (x1 - x0) = (y1^2 - y2^2 - x2^2 + x1^2) * (x1 - x0) - (y1^2 - y0^2 - x0^2 + x1^2) * (x1 - x2)
             *  yc * 2 * ((y0 - y1) * (x1 - x2) - (y2 - y1) * (x1 - x0)) = (y1^2 - y2^2 + x1^2 - x2^2) * (x1 - x0) - (y1^2 - y0^2 + x1^2 - x0^2) * (x1 - x2)
             *  yc = ((y1^2 - y2^2 + x1^2 - x2^2) * (x1 - x0) - (y1^2 - y0^2 + x1^2 - x0^2) * (x1 - x2)) / ((y0 - y1) * (x1 - x2) - (y2 - y1) * (x1 - x0)) / 2
             */
            double x0sq = x0 * x0;
            double y0sq = y0 * y0;
            double x1sq = x1 * x1;
            double y1sq = y1 * y1;
            double x2sq = x2 * x2;
            double y2sq = y2 * y2;

            double x1sq_x0sq = x1sq - x0sq;
            double y1sq_y0sq = y1sq - y0sq;
            double x1sq_x2sq = x1sq - x2sq;
            double y1sq_y2sq = y1sq - y2sq;

            int x1_x0 = x1 - x0;
            int y1_y0 = y1 - y0;
            int x1_x2 = x1 - x2;
            int y1_y2 = y1 - y2;

            centerx = ((x1sq_x2sq + y1sq_y2sq) * y1_y0 - (x1sq_x0sq + y1sq_y0sq) * y1_y2) / (x1_x2 * y1_y0 - x1_x0 * y1_y2) / 2.0;
            centery = ((y1sq_y2sq + x1sq_x2sq) * x1_x0 - (y1sq_y0sq + x1sq_x0sq) * x1_x2) / (y1_y2 * x1_x0 - y1_y0 * x1_x2) / 2.0;

            radius = Math.hypot (x1 - centerx, y1 - centery);
        }

        /**
         * Get deviation from given radius of all the segments on the arc.
         */
        private double getQuality (double xc, double yc, double rad)
        {
            double quality = 0.0;
            for (LineSeg ls : segments) {
                double r0 = Math.hypot (ls.x0 - xc, ls.y0 - yc);
                double r1 = Math.hypot (ls.x1 - xc, ls.y1 - yc);
                quality  += (Math.abs (r0 - rad) + Math.abs (r1 - rad)) / 2.0;
            }
            return quality;
        }
    }

    /**
     * Utility to print out contents of a COSBase object.
     */
    private static int numdigs = 5;
    private static String spacer = "        ";  // numdigs+3 spaces ("<ddddd> ")
    private static HashMap<COSBase,String> seenObjects = new HashMap<COSBase,String> ();

    private static void printCOSObject (String prefix, String name, COSBase obj)
    {
        String n;
        if (seenObjects.containsKey (obj)) {
            n = seenObjects.get (obj);
            String val = "";
            if ((obj instanceof COSFloat) || (obj instanceof COSInteger) || (obj instanceof COSName) || (obj instanceof COSString)) {
                val = " " + obj.toString ();
            }
            System.out.println (spacer + prefix + name + ": " + n + val);
            return;
        }
        n = Integer.toString (seenObjects.size () + 1);
        while (n.length () < numdigs) n = "0" + n;
        n = "<" + n + "> ";
        seenObjects.put (obj, n);

        if (obj instanceof COSArray) {
            System.out.println (n + prefix + name + ": COSArray {");
            int i = 0;
            for (COSBase elem : (COSArray) obj) {
                printCOSObject (prefix + "  ", "[" + i + "]", elem);
                i ++;
            }
            System.out.println (n + prefix + "}");
        } else if (obj instanceof COSDictionary) {
            System.out.println (n + prefix + name + ": COSDictionary {");
            Set<Map.Entry<COSName,COSBase>> entries = ((COSDictionary) obj).entrySet ();
            for (Map.Entry<COSName,COSBase> entry : entries) {
                COSName key = entry.getKey ();
                COSBase val = entry.getValue ();
                printCOSObject (prefix + "  ", key.getName (), val);
            }
            System.out.println (n + prefix + "}");
        } else if (obj instanceof COSObject) {
            System.out.println (n + prefix + name + ": " + obj.toString ());
            COSBase encap = ((COSObject) obj).getObject ();
            if (encap != null) {
                printCOSObject (prefix + "  ", "encap", encap);
            }
        } else {
            System.out.println (n + prefix + name + ": " + obj.toString ());
        }
    }
}
