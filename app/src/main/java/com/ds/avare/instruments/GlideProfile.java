/**
 * zkhan
 * Glide profile
 */
package com.ds.avare.instruments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Airport;
import com.ds.avare.position.Coordinate;
import com.ds.avare.position.Origin;
import com.ds.avare.position.Projection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.utils.WindTriagle;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.WindsAloft;

public class GlideProfile {

    // Members that get set at object construction
    private StorageService mService;
    private Paint mPaint;
    private float mDipToPix;
    private Preferences mPref;
    double[] mDistanceTotal;
    private String mWind;
    private long mLastTime;

    private static final int HEIGHT_STEPS = 10;
    private static final int DIRECTION_STEPS = 24;
    private static final long UPDATE_TIME = 5000; //ms

    /***
     * Instrument to handle displaying of rings based on glide upon speed, glide ratio, wind speed, and terrain
     *
     * @param service background storage service
     * @param context application context
     * @param textSize size of the text to draw
     */
    public GlideProfile(StorageService service, Context context, float textSize) {
        mService = service;
        mDipToPix = Helper.getDpiToPix(context);
        mPref = new Preferences(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(textSize);
        mPaint.setTypeface(Helper.getTypeFace(context));
        mDistanceTotal = new double[DIRECTION_STEPS];
        mLastTime = System.currentTimeMillis() + UPDATE_TIME;
    }

    public void updateGlide(GpsParams gpsParams) {
        long time = System.currentTimeMillis();
        long diff = time - mLastTime;
        if(diff < UPDATE_TIME) {
            return; // lots of calculations, slow down to 10 sec update.
        }
        mLastTime = time;

        // find area elevation.
        double elevation = mService.getArea().getNearestElevation();

        // units are feet and second
        double currentSpeed = gpsParams.getSpeed() * Preferences.feetConversion / 3600.0; //convert to feet per second
        double lon = gpsParams.getLongitude();
        double lat = gpsParams.getLatitude();
        double altitudeGps = gpsParams.getAltitude();
        double sinkRate = mPref.getBestGlideSinkRate() / 60.0; //feet per minute to feet per second
        double bearing = gpsParams.getBearing();
        double declination = gpsParams.getDeclinition();
        WindsAloft wa;
        double [] metarWinds = null;

        for(int dir = 0; dir < DIRECTION_STEPS; dir++) {
            mDistanceTotal[dir] = 0; // clear
        }

        Metar m = null;
        if(mPref.useAdsbWeather()) {
            wa = mService.getAdsbWeather().getWindsAloft(lon, lat);
            if(null != wa) {
                if(null != wa.station) {
                    m = mService.getAdsbWeather().getMETAR(wa.station);
                }
            }
        }
        else {
            wa = mService.getDBResource().getWindsAloft(lon, lat);
            if(null != wa) {
                if(null != wa.station) {
                    m = mService.getDBResource().getMetar(wa.station);
                }
            }
        }

        if(null == wa) {
            wa = new WindsAloft();
            // no wind calc
        }
        if(null != m) {
            metarWinds = WeatherHelper.getWindFromMetar(m.rawText);
            if (metarWinds != null && metarWinds.length > 0) {
                metarWinds[0] = (metarWinds[0] - declination + 360) % 360; // true winds aloft
            }
            else {
                metarWinds = null;
            }
        }

        int stepSizeDirection =
                (int)(360 / DIRECTION_STEPS);

        // calculate airspeed from ground speed, direction, and wind speed.
        double[] waa = wa.getWindAtAltitude(altitudeGps, metarWinds);
        waa[0] = waa[0] * Preferences.feetConversion / 3600.0;

        double t[] = WindTriagle.getTrueFromGroundAndWind(currentSpeed, bearing, waa[0], waa[1]);
        double as = t[0];

        // Put wind/elevation/airspeed in string. this will be shown on the ring
        mWind = String.format("w%03d@%d/t%d", (int)waa[1],
                (int)(waa[0] / Preferences.feetConversion * 3600),
                (int)((double)as * 3600 / Preferences.feetConversion));

        // calculate winds from current altitude to ground.
        for(int dir = 0; dir < DIRECTION_STEPS; dir++) {
            mDistanceTotal[dir] = findDistanceTo(bearing, dir * stepSizeDirection, sinkRate, altitudeGps, elevation, as, wa, metarWinds);
            // now we know how far we can glide in each direction
            //XXX: Fix ground elevation to include hills which are not easily available from tiles.
        }

        /*
         * Now test which airports in the area are at glide-able distance.
         */
        int n = mService.getArea().getAirportsNumber();
        for(int i = 0; i < n; i++) {
            Airport airport = mService.getArea().getAirport(i);
            double to = airport.getBearing();
            elevation = airport.getElevationNumber();
            double distance = findDistanceTo(bearing, to, sinkRate, altitudeGps, elevation, as, wa, metarWinds);
            airport.setCanGlide(airport.getDistance() < distance);
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

    /**
     * Find distance covered when gliding from bearingAt to bearing arring at elevation from altitudeGps and at give sinkRate and winds aloft, airspeed
     * @param bearing
     * @param bearingAt
     * @param sinkRate
     * @param altitudeGps
     * @param elevation
     * @param as
     * @param wa
     * @return
     */
    public static double findDistanceTo(double bearing, double bearingAt, double sinkRate, double altitudeGps, double elevation, double as, WindsAloft wa, double[] metarWinds) {
        double distance = 0;
        // calculate ground speed from airspeed, direction, and wind speed. This is approx 2 % change in tas per 1000 foot.
        for(int alt = 0; alt < HEIGHT_STEPS; alt++) {
            // correct altitude based on direction as turn loses altitude, assume 1 second per 3 degrees, and shortest dir turn
            double turnAngle = distance(bearing, bearingAt);
            double altLost = turnAngle / 3.0 * sinkRate;
            double altitude = altitudeGps - altLost;
            if (altitude < 0) {
                altitude = 0;
            }
            int stepSizeHeight = (int)((altitude - elevation) / HEIGHT_STEPS);
            double thisAltitude = (double)alt * stepSizeHeight + elevation;
            double wind[] = wa.getWindAtAltitude(thisAltitude, metarWinds);
            wind[0] *= Preferences.feetConversion / 3600.0;
            double tas = as - as * (thisAltitude / 1000 * 2 / 100); // 2% per 1000 foot approx
            double gs = Math.sqrt(tas * tas + wind[0] * wind[0] - 2.0 * tas * wind[0] * Math.cos((bearingAt - wind[1]) * Math.PI / 180.0)); //fps
            double timeInZone = stepSizeHeight / sinkRate; // how much time we spend in each zone, thermals not accounted for.
            distance += gs * timeInZone;
        }
        return  distance / Preferences.feetConversion; // miles;
    }

    /***
     * Render the glide profile to the display canvas
     *
     * @param canvas draw on this canvas
     * @param origin the x/y origin of the upper left of the canvas
     * @param gpsParams current gps location data
     */
    public void draw(Canvas canvas, Origin origin, GpsParams gpsParams) {


        if(mPref.getDistanceRingType() != 3) {
            return;
        }

        /*
         * Set the paint accordingly
         */
        mPaint.setStrokeWidth(4 * mDipToPix);
        mPaint.setShadowLayer(0, 0, 0, 0);
        mPaint.setColor(mPref.getDistanceRingColor());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAlpha(0x7F);

        /*
         * Draw the shape
         */
        int stepSizeDirection = (int)(360 / DIRECTION_STEPS);

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
            canvas.drawLine(xlast, ylast, x, y, mPaint);
            xlast = x;
            ylast = y;
        }

        canvas.drawLine(xlast, ylast, firstX, firstY, mPaint);

        /*
         * Restore some paint settings back to what they were so as not to
         * mess things up
         */
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);

        mService.getShadowedText().draw(canvas, mPaint,
                mWind, Color.BLACK, firstX, firstY - mDipToPix * 32); // move up so it does not overlap the ring

    }

}
