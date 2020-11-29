/**
 * zkhan
 * Glide profile
 */
package com.ds.avare.instruments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Movement;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.position.Scale;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.weather.WindsAloft;

public class GlideProfile {

    // Members that get set at object construction
    private StorageService mService;
    private Context mContext;
    private Paint mPaint;
    private float mDipToPix;
    private Preferences mPref;
    double[] mDistanceTotal;

    private static final int HEIGHT_STEPS = 10;
    private static final int DIRECTION_STEPS = 24;

    /***
     * Instrument to handle displaying of rings based on glide upon speed, glide ratio, wind speed, and terrain
     *
     * @param service background storage service
     * @param context application context
     * @param textSize size of the text to draw
     */
    public GlideProfile(StorageService service, Context context, float textSize) {
        mService = service;
        mContext = context;
        mDipToPix = Helper.getDpiToPix(context);
        mPref = new Preferences(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(textSize);
        mPaint.setTypeface(Helper.getTypeFace(context));
        mDistanceTotal = new double[DIRECTION_STEPS];
    }

    public void updateGlide(GpsParams gpsParams) {
        // units are feet and second
        double currentSpeed = gpsParams.getSpeed() * Preferences.feetConversion / 3600.0; //convert to feet per second
        double lon = gpsParams.getLongitude();
        double lat = gpsParams.getLatitude();
        double altitudeGps = gpsParams.getAltitude();
        double sinkRate = mPref.getBestGlideSinkRate() / 60.0; //feet per minute to feet per second
        double bearing = gpsParams.getBearing();
        WindsAloft wa;

        if(!mPref.isDrawGlideProfile()) {
            return;
        }

        for(int dir = 0; dir < DIRECTION_STEPS; dir++) {
            mDistanceTotal[dir] = 0; // clear
        }

        if(mPref.useAdsbWeather()) {
            wa = mService.getAdsbWeather().getWindsAloft(lon, lat);
        }
        else {
            wa = mService.getDBResource().getWindsAloft(lon, lat);
        }

        if(null == wa) {
            wa = new WindsAloft();
            // no wind calc
        }

        int stepSizeDirection = (int)(360 / DIRECTION_STEPS);

        // calculate airspeed from ground speed, direction, and wind speed.
        double[] waa = wa.getWindAtAltitude(altitudeGps);
        waa[0] = waa[0] * Preferences.feetConversion / 3600.0;

        // wind triangle solution for airspeed from wind and ground vector.
        Double as = Math.sqrt(waa[0] * waa[0] + currentSpeed * currentSpeed - 2.0 * waa[0] * currentSpeed * Math.cos((bearing - waa[1]) * Math.PI / 180.0));
        if(as.isNaN()) {
            //unsolvable wind triangle
            return;
        }

        // calculate winds from current altitude to ground.
        for(int alt = 0; alt < HEIGHT_STEPS; alt++) {
            // calculate ground speed from airspeed, direction, and wind speed. This is approx 2 % change in tas per 1000 foot.
            for(int dir = 0; dir < DIRECTION_STEPS; dir++) {
                // correct altitude based on direction as turn loses altitude, assume 1 second per 3 degrees, and shortest dir turn
                double turnAngle = distance(bearing, dir * stepSizeDirection);
                double altLost = turnAngle / 3.0 * sinkRate;
                double altitude = altitudeGps - altLost;
                if (altitude < 0) {
                    altitude = 0;
                }
                int stepSizeHeight = (int)(altitude / HEIGHT_STEPS);
                double wind[] = wa.getWindAtAltitude((double)alt * stepSizeHeight);
                wind[0] *= Preferences.feetConversion / 3600.0;
                double tas = as - as * ((altitude - alt * stepSizeHeight) / 1000 * 2 / 100);
                double gs = Math.sqrt(tas * tas + wind[0] * wind[0] - 2.0 * tas * wind[0] * Math.cos((dir * stepSizeDirection - wind[1]) * Math.PI / 180.0)); //fps
                double timeInZone = stepSizeHeight / sinkRate; // how much time we spend in each zone, thermals not accounted for.
                double distance = gs * timeInZone; //feet
                mDistanceTotal[dir] += distance / Preferences.feetConversion; // miles
                // now we know how far we can glide in each direction
                //XXX: Fix ground elevation to include hills which are not easily available from tiles.
            }
        }
    }

    /**
     * Shortest distance (angular) between two angles.
     * It will be in range [0, 180].
     */
    public static double distance(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;       // This is either the distance or 360 - distance
        double distance = phi > 180 ? 360 - phi : phi;
        return distance;
    }

    /***
     * Render the glide profile to the display canvas
     *
     * @param canvas draw on this canvas
     * @param origin the x/y origin of the upper left of the canvas
     * @param gpsParams current gps location data
     */
    public void draw(Canvas canvas, Origin origin, GpsParams gpsParams) {


        if(!mPref.isDrawGlideProfile()) {
            return;
        }

        /*
         * Set the paint accordingly
         */
        mPaint.setStrokeWidth(4 * mDipToPix);
        mPaint.setShadowLayer(0, 0, 0, 0);
        mPaint.setColor(Color.GRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAlpha(0x7F);

        /*
         * Draw the shape
         */
        int stepSizeDirection = (int)(360 / DIRECTION_STEPS);
        Path path = new Path();

        double dist = mDistanceTotal[0];
        double angle = 0;
        Coordinate c = Projection.findStaticPoint(gpsParams.getLongitude(), gpsParams.getLatitude(), angle, dist);
        float firstX = (float)origin.getOffsetX(c.getLongitude());
        float firstY = (float)origin.getOffsetY(c.getLatitude());

        float xlast = firstX;
        float ylast = firstY;
        float x;
        float y;
        for(int dir = 1; dir < DIRECTION_STEPS; dir++) {
            dist = mDistanceTotal[dir];
            angle = dir * stepSizeDirection;
            c = Projection.findStaticPoint(gpsParams.getLongitude(), gpsParams.getLatitude(), angle, dist);
            x = (float)origin.getOffsetX(c.getLongitude());
            y = (float)origin.getOffsetY(c.getLatitude());
            path.moveTo(xlast, ylast);
            path.lineTo(x, y);
            xlast = x;
            ylast = y;
            canvas.drawPath(path, mPaint);
        }

        path.moveTo(xlast, ylast);
        path.lineTo(firstX, firstY);
        canvas.drawPath(path, mPaint);

        /*
         * Restore some paint settings back to what they were so as not to
         * mess things up
         */
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);

    }

}