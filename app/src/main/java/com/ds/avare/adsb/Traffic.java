package com.ds.avare.adsb;

import android.graphics.Color;
import android.graphics.Matrix;
import android.util.SparseArray;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.threed.AreaMapper;
import com.ds.avare.threed.TerrainRenderer;
import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;

import java.util.LinkedList;

public class Traffic {

    public int mIcaoAddress;
    public float mLat;
    public float mLon;
    public int mAltitude;
    public int mHorizVelocity;
    public int mVertVelocity;
    public float mHeading;
    public String mCallSign;
    public boolean mIsAirborne;
    private long mLastUpdate;
    private static Matrix mMatrix = new Matrix();


    public static final double TRAFFIC_ALTITUDE_DIFF_DANGEROUS = 1000; //ft 300m required minimum
    

    
    // ms
    private static final long EXPIRES = 1000 * 60 * 1;

    public Traffic() {
        mIcaoAddress = 0;
        mCallSign = "";
        mLat = 0;
        mLon = 0;
        mAltitude = 0;
    }

    /**
     * 
     * @param callsign
     * @param address
     * @param lat
     * @param lon
     * @param altitude
     * @param heading
     */
    public Traffic(String callsign, int address, boolean isAirborne, float lat, float lon, int altitude,
            float heading, int speed, int vspeed, long time)
    {
        mIcaoAddress = address;
        mCallSign = callsign;
        mIsAirborne = isAirborne;
        mLon = lon;
        mLat = lat;
        mAltitude = altitude;
        mHeading = heading;
        mHorizVelocity = speed;
        mVertVelocity = vspeed;
        mLastUpdate = time;

        /*
         * Limit
         */
        if(mHorizVelocity >= 0xFFF) {
            mHorizVelocity = 0;
        }
    }
    
    /**
     * 
     * @return
     */
    public boolean isOld() {

        long diff = Helper.getMillisGMT();
        diff -= mLastUpdate;

        return diff > EXPIRES;
    }

    public long getLastUpdate() {
        return mLastUpdate;
    }
    
    /**
     * 
     * @return
     */
    public static int getColorFromAltitude(double myAlt, double theirAlt) {
        int color;
        double diff = myAlt - theirAlt;
        if(diff > TRAFFIC_ALTITUDE_DIFF_DANGEROUS) {
            /*
             * Much below us
             */
            color = Color.GREEN;
        }
        else if (diff < TRAFFIC_ALTITUDE_DIFF_DANGEROUS && diff > 0) {
            /*
             * Dangerously below us
             */
            color = Color.RED;
        }
        else if (diff < -TRAFFIC_ALTITUDE_DIFF_DANGEROUS) {
            /*
             * Much above us
             */
            color = Color.BLUE;
        }
        else {
            /*
             * Dangerously above us
             */
            color = Color.MAGENTA;
        }
 
        return color;
    }

    public static void draw(DrawingContext ctx, LinkedList<Traffic> traffic, double altitude, GpsParams params, boolean shouldDraw,
                            BitmapHolder bRed, BitmapHolder bGreen, BitmapHolder bBlue, BitmapHolder bMagenta) {

        boolean circles = ctx.pref.shouldDrawTrafficCircles();

        /*
         * Get traffic to draw.
         */
        if((null == traffic) || (!shouldDraw)) {
            return;
        }

        ctx.paint.setColor(Color.WHITE);
        for(Traffic t : traffic) {

            if(null == t) {
                continue;
            }
            // Don't draw ground traffic, unless configuration allows it
            if (!(t.mIsAirborne || ctx.pref.showAdsbGroundTraffic())) {
                continue;
            }

            /*
             * Draw all traffic as its not reported for far of places.
             *   if(!isOnScreen(ctx.origin, t.mLat, t.mLon)) {
             *       continue;
             *   }
             */

            /*
             * Make traffic line and info
             */
            float x = (float)ctx.origin.getOffsetX(t.mLon);
            float y = (float)ctx.origin.getOffsetY(t.mLat);

            /*
             * Find color from altitude
             */
            int color = Traffic.getColorFromAltitude(altitude, t.mAltitude);

            int diff;
            String text = "";
            // hide callsign if configured in prefs
            if (ctx.pref.showAdsbCallSign() && !t.mCallSign.equals("")) {
                text = t.mCallSign + ":";
            }

            if(altitude <= StorageService.MIN_ALTITUDE) {
                // display in hundreds of feet
                // This is when we do not have our own altitude set with ownship
                diff = t.mAltitude;
                diff = (int)Math.round(diff / 100.0);
                text += diff + "PrA"; // show that this is pressure altitude
                // do not filter when own PA is not known
            }
            else {
                // Own PA is known, show height difference
                diff = (int)(t.mAltitude - altitude);
                diff = (int)Math.round(diff / 100.0);
                
                if(diff > 0) {
                    text += "+" + (diff < 10 ? "0" : "") + diff;
                } else if(diff < 0) {
                    text += "-" + (diff > -10 ? "0" : "") + Math.abs(diff);
                } else {
                    text += "0" + diff;
                }
            }

            float radius;
            if(circles) {
                radius = ctx.dip2pix * 8;
                /*
                 * Draw outline to show it clearly
                 */
                ctx.paint.setColor((~color) | 0xFF000000);
                ctx.canvas.drawCircle(x, y, radius + 2, ctx.paint);

                ctx.paint.setColor(color);
                ctx.canvas.drawCircle(x, y, radius, ctx.paint);
            }
            else {
                BitmapHolder b = null;
                if (color == Color.RED) {
                    b = bRed;
                }
                else if (color == Color.GREEN) {
                    b = bGreen;
                }
                else if (color == Color.BLUE) {
                    b = bBlue;
                }
                else {
                    b = bMagenta;
                }

                radius =  b.getBitmap().getWidth();
                mMatrix.setRotate(t.mHeading, b.getWidth() / 2, b.getHeight() / 2);
                mMatrix.postTranslate(x - b.getWidth() / 2, y - b.getHeight() / 2);

                ctx.canvas.drawBitmap(b.getBitmap(), mMatrix, ctx.paint);
                ctx.paint.setColor(color);
            }

            /*
             * Show a barb for heading with length based on speed
             * Find distance target will travel in 1 min
             */
            float distance2 = (float)t.mHorizVelocity / 60.f;
            Coordinate c = Projection.findStaticPoint(t.mLon, t.mLat, t.mHeading, distance2);
            float xr = (float)ctx.origin.getOffsetX(c.getLongitude());
            float yr = (float)ctx.origin.getOffsetY(c.getLatitude());

            ctx.canvas.drawLine(x, y, xr, yr, ctx.paint);

            /*
             * If in track-up mode, rotate canvas around screen x/y of
             * where we want to draw
             */
            boolean bRotated = false;
            if (ctx.pref.isTrackUp() && (params != null)) {
                bRotated = true;
                ctx.canvas.save();
                ctx.canvas.rotate((int) params.getBearing(), x, y);
            }


            ctx.service.getShadowedText().draw(ctx.canvas, ctx.textPaint,
                    text, Color.BLACK, (float)x, (float)y + radius + ctx.textPaint.getTextSize());


            if (true == bRotated) {
                ctx.canvas.restore();
            }

        }


    }


    /**
     * Draw for 3D
     * @param service
     * @param mapper
     * @param renderer
     */
    public static void draw(StorageService service, AreaMapper mapper, TerrainRenderer renderer) {
        if (service != null) {
            LinkedList<Traffic> traffic = service.getTrafficCache().getTraffic();
            Vector4d ships[] = new Vector4d[traffic.size()];
            int i = 0;
            for (Traffic t : traffic) {
                ships[i] = mapper.gpsToAxis(t.mLon, t.mLat, t.mAltitude, t.mHeading);
                i++;
            }
            renderer.setShips(ships);
        }
    }


    /*
     * Determine if shape belong to a screen based on Screen longitude and latitude
     * and shape max/min longitude latitude
     */
    public static boolean isOnScreen(Origin origin, double lat, double lon) {

        double maxLatScreen = origin.getLatScreenTop();
        double minLatScreen = origin.getLatScreenBot();
        double minLonScreen = origin.getLonScreenLeft();
        double maxLonScreen = origin.getLonScreenRight();

        boolean isInLat = lat < maxLatScreen && lat > minLatScreen;
        boolean isInLon = lon < maxLonScreen && lon > minLonScreen;
        return isInLat && isInLon;
    }


}
