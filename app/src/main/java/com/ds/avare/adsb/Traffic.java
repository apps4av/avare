package com.ds.avare.adsb;

import android.graphics.Color;
import android.util.SparseArray;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Origin;
import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.threed.AreaMapper;
import com.ds.avare.threed.TerrainRenderer;
import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.utils.Helper;

public class Traffic {

    public int mIcaoAddress;
    public float mLat;
    public float mLon;
    public int mAltitude;
    public int mHorizVelocity;
    public float mHeading;
    public String mCallSign;
    private long mLastUpdate;
    

    public static final double TRAFFIC_ALTITUDE_DIFF_DANGEROUS = 1000; //ft 300m required minimum
    

    
    // ms
    private static final long EXPIRES = 1000 * 60 * 1;

    /**
     * 
     * @param callsign
     * @param address
     * @param lat
     * @param lon
     * @param altitude
     * @param heading
     */
    public Traffic(String callsign, int address, float lat, float lon, int altitude, 
            float heading, int speed, long time)
    {
        mIcaoAddress = address;
        mCallSign = callsign;
        mLon = lon;
        mLat = lat;
        mAltitude = altitude;
        mHeading = heading;
        mHorizVelocity = speed;
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
        if(diff > EXPIRES) {
            return true;
        }
        return false;
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

    public static void draw(DrawingContext ctx, SparseArray<Traffic> traffic, double altitude, GpsParams params, int ownIcao, boolean shouldDraw) {

        int filterAltitude = ctx.pref.showAdsbTrafficWithin();

        /*
         * Get traffic to draw.
         */
        if((null == traffic) || (!shouldDraw)) {
            return;
        }

        ctx.paint.setColor(Color.WHITE);
        for(int i = 0; i < traffic.size(); i++) {
            int key = traffic.keyAt(i);
            Traffic t = traffic.get(key);
            if(t.isOld()) {
                traffic.delete(key);
                continue;
            }

            if(t.mIcaoAddress == ownIcao) {
                // Do not draw shadow of own
                continue;
            }

            if(!isOnScreen(ctx.origin, t.mLat, t.mLon)) {
                continue;
            }

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
                // This is when we do not have our own altitude set with ownship
                diff = (int)t.mAltitude;
                text += diff + "PrA'"; // show that this is pressure altitude
                // do not filter when own PA is not known
            }
            else {
                // Own PA is known, show height difference
                diff = (int)(t.mAltitude - altitude);
                text += (diff > 0 ? "+" : "") + diff + "'";
                // filter
                if(Math.abs(diff) > filterAltitude) {
                    continue;
                }
            }


            float radius = ctx.dip2pix * 8;
            /*
             * Draw outline to show it clearly
             */
            ctx.paint.setColor((~color) | 0xFF000000);
            ctx.canvas.drawCircle(x, y, radius + 2, ctx.paint);

            ctx.paint.setColor(color);
            ctx.canvas.drawCircle(x, y, radius, ctx.paint);
            /*
             * Show a barb for heading with length based on speed
             * Vel can be 0 to 4096 knots (practically it can be 0 to 500 knots), so set from length 0 to 100 pixels (1/5)
             */
            float speedLength = radius + (float)t.mHorizVelocity * (float)ctx.dip2pix / 5.f;
            /*
             * Rotation of points to show direction
             */
            double xr = x + PixelCoordinate.rotateX(speedLength, t.mHeading);
            double yr = y + PixelCoordinate.rotateY(speedLength, t.mHeading);
            ctx.canvas.drawLine(x, y, (float)xr, (float)yr, ctx.paint);

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
            SparseArray<Traffic> t = service.getTrafficCache().getTraffic();
            Vector4d ships[] = new Vector4d[t.size()];
            for (int count = 0; count < t.size(); count++) {
                Traffic tr = t.valueAt(count);
                ships[count] = mapper.gpsToAxis(tr.mLon, tr.mLat, tr.mAltitude, tr.mHeading);
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
