package com.ds.avare.adsb;

import android.graphics.Color;
import android.util.SparseArray;

import com.ds.avare.position.PixelCoordinate;
import com.ds.avare.shapes.DrawingContext;
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

    public static void draw(DrawingContext ctx, SparseArray<Traffic> traffic, double altitude, boolean shouldDraw) {
        /*
         * Get traffic to draw.
         */
        if((!ctx.pref.showAdsbTraffic()) || (null == traffic) || (!shouldDraw)) {
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

            /*
             * Make traffic line and info
             */
            float x = (float)ctx.origin.getOffsetX(t.mLon);
            float y = (float)ctx.origin.getOffsetY(t.mLat);

            /*
             * Find color from altitude
             */
            int color = Traffic.getColorFromAltitude(altitude, t.mAltitude);


            float radius = ctx.dip2pix * 8;
            String text = t.mAltitude + "'";
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
            ctx.service.getShadowedText().draw(ctx.canvas, ctx.textPaint,
                    text, Color.DKGRAY, (float)x, (float)y + radius + ctx.textPaint.getTextSize());

        }


    }
    
}
