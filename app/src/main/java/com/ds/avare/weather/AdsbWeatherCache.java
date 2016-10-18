/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.weather;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.SparseArray;

import com.ds.avare.StorageService;
import com.ds.avare.adsb.NexradBitmap;
import com.ds.avare.adsb.NexradImage;
import com.ds.avare.adsb.NexradImageConus;
import com.ds.avare.place.Destination;
import com.ds.avare.position.Origin;
import com.ds.avare.shapes.DrawingContext;
import com.ds.avare.storage.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.RateLimitedBackgroundQueue;
import com.ds.avare.utils.WeatherHelper;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * 
 * This is where all ADSB weather is collected.
 * @author zkhan
 *
 */
public class AdsbWeatherCache {

    private HashMap<String, Taf> mTaf;
    private HashMap<String, Metar> mMetar;
    private HashMap<String, Airep> mAirep;
    private HashMap<String, WindsAloft> mWinds;
    private NexradImage mNexrad;
    private NexradImageConus mNexradConus;
    private Preferences mPref;
    private RateLimitedBackgroundQueue mMetarQueue;


    private static final int MAX_BARBS = 6;
    private static final int BARB_LENGTH = 50;
    private static final int BARB_WIDTH = 12;
    private static final int GUST_X = 13;
    private static final int GUST_Y = 12;
    private static final int BARB_OFFSET = 6;
    /**
     * 
     */
    public AdsbWeatherCache(Context context, StorageService service) {
        mPref = new Preferences(context);
        mTaf = new HashMap<String, Taf>();
        mMetar = new HashMap<String, Metar>();
        mAirep = new HashMap<String, Airep>();
        mWinds = new HashMap<String, WindsAloft>();
        mNexrad = new NexradImage();
        mMetarQueue = new RateLimitedBackgroundQueue(service);
        mNexradConus = new NexradImageConus();
    }

    /**
     * 
     * @return
     */
    public NexradImage getNexrad() {
        return mNexrad;
    }
    
    /**
     * 
     * @return
     */
    public NexradImageConus getNexradConus() {
        return mNexradConus;
    }
    
    /**
     * 
     * @param time
     * @param location
     * @param data
     */
    public void putMetar(long time, String location, String data, String flightCategory) {
        if(!mPref.useAdsbWeather()) {
            return;
        }

        Metar m = new Metar();
        m.rawText = location + " " + data;
        m.stationId = location;
        Date dt = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        m.time = sdf.format(dt) + "Z";
        m.flightCategory = flightCategory;
        m.timestamp = System.currentTimeMillis();
        mMetar.put(location, m);
        mMetarQueue.insertMetarInQueue(m); // This will slowly make a metar map
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


    public static void drawWindBarb(DrawingContext ctx, float x, float y, Metar m)
    {
        // Wind 1-2, no barb
        // Wind 3-7, little barb
        // Wind 8-12, big barb
        int barb[] = new int[MAX_BARBS];
        int yoffset;
        int i, found;
        int wind;
        String gusts;
        int direction;
        String[] metarArray = m.rawText.split("\\s+");
        String windString;
        String windSub;
        float gx, gy;

        // Extract the winds and angle from the METAR string
        found=0;
        direction=0;
        wind=0;
        gusts="";

        for (i=0; i<metarArray.length; i++)
        {
            if ((found==0) && (metarArray[i].endsWith("KT")))
            {
                found=1;
                try {
                    // We found the winds, lets process it
                    windString = metarArray[i];

                    windSub = windString.substring(0, 3);
                    // Handle 'VRB'
                    if (windSub.contains("VRB"))
                        direction = 0;
                    else
                        direction = Integer.parseInt(windSub);
                    wind = Integer.parseInt(windString.substring(3, 5));
                    if (windString.contains("G")) {
                        gusts = windString.substring(5, 8);
                    } else {
                        gusts = "";
                    }
                }
                catch (Exception e) {
                    // An exception was caught, quit and do not display a barb at all
                    return;
                }
            }
        }

        // Zero the wind barb structure

        for (i=0; i<MAX_BARBS; i++)
            barb[i] = 0;

        if ((wind>=3) && (wind <= 7))
        {
            //  __________
            //          \
            //
            barb[1]=2;
        }
        if ((wind >= 8 ) && (wind <= 12))
        {
            //  __________
            //            \
            //             \
            barb[0]=1;
        }
        if ((wind >= 13 ) && (wind <= 17))
        {
            //  __________
            //           \\
            //             \
            barb[0]=1;
            barb[1]=2;
        }
        if ((wind >= 18 ) && (wind <= 22))
        {
            //  __________
            //           \\
            //            \\
            barb[0]=1;
            barb[1]=1;
        }
        if ((wind >= 23 ) && (wind <= 27))
        {
            //  __________
            //          \\\
            //            \\
            barb[0]=1;
            barb[1]=1;
            barb[2]=2;
        }
        if ((wind >= 28 ) && (wind <= 32))
        {
            //  __________
            //          \\\
            //           \\\
            barb[0]=1;
            barb[1]=1;
            barb[2]=1;
        }
        if ((wind >= 33 ) && (wind <= 37))
        {
            //  __________
            //         \\\\
            //           \\\
            barb[0]=1;
            barb[1]=1;
            barb[2]=1;
            barb[3]=2;
        }
        if ((wind >= 38 ) && (wind <= 42))
        {
            //  __________
            //         \\\\
            //          \\\\
            barb[0]=1;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
        }
        if ((wind >= 43 ) && (wind <= 47))
        {
            //  __________
            //        \\\\\
            //          \\\\
            barb[0]=1;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
            barb[4]=2;
        }
        if ((wind >= 48 ) && (wind <= 52))
        {
            //  ____________
            //          \  /
            //           \/
            barb[0]=3;
        }
        if ((wind >= 53 ) && (wind <= 57))
        {
            //  ____________
            //         \\  /
            //           \/
            barb[0]=3;
            barb[1]=2;
        }
        if ((wind >= 58 ) && (wind <= 62))
        {
            //  ____________
            //         \\  /
            //          \\/
            barb[0]=3;
            barb[1]=1;
        }
        if ((wind >= 63 ) && (wind <= 67))
        {
            //  ____________
            //        \\\  /
            //          \\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=2;
        }
        if ((wind >= 68 ) && (wind <= 72))
        {
            //  ____________
            //        \\\  /
            //         \\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
        }
        if ((wind >= 73 ) && (wind <= 77))
        {
            //  ____________
            //       \\\\  /
            //         \\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
            barb[3]=2;
        }
        if ((wind >= 78 ) && (wind <= 82))
        {
            //  ____________
            //       \\\\  /
            //        \\\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
        }
        if ((wind >= 83 ) && (wind <= 87))
        {
            //  ____________
            //      \\\\\  /
            //        \\\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
            barb[4]=2;
        }
        if ((wind >= 88 ) && (wind <= 92))
        {
            //  ____________
            //      \\\\\  /
            //       \\\\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
            barb[4]=1;
        }
        if ((wind >= 93 ) && (wind <= 97))
        {
            //  ____________
            //     \\\\\\  /
            //       \\\\\/
            barb[0]=3;
            barb[1]=1;
            barb[2]=1;
            barb[3]=1;
            barb[4]=1;
            barb[5]=2;
        }
        if (wind >= 98)
        {
            //  ____________
            //      \  /\  /
            //       \/  \/
            barb[0]=3;
            barb[2]=3;
        }

        yoffset=BARB_LENGTH;

        // Set the color to black, and rotate the canvas to the wind angle
        ctx.paint.setColor(Color.BLACK);
        ctx.canvas.save();
        ctx.canvas.rotate(direction-90,x,y);
        // Draw the line if the wind is not 0
        if (wind >= 1)
            ctx.canvas.drawLine(x,y,x+(ctx.dip2pix*BARB_LENGTH),y,ctx.paint);
        for (i=0; i<MAX_BARBS; i++) {
            switch(barb[i]){
                case 1:
                    // A large barb
                    ctx.canvas.drawLine(x + (ctx.dip2pix * yoffset), y, x + (ctx.dip2pix * (yoffset+(BARB_WIDTH/2))), y + (ctx.dip2pix * (BARB_WIDTH)), ctx.paint);
                    break;
                case 2:
                    // A small barb
                    ctx.canvas.drawLine(x + (ctx.dip2pix * yoffset), y, x + (ctx.dip2pix * (yoffset+(BARB_WIDTH/4))), y + (ctx.dip2pix * ( (BARB_WIDTH / 2))), ctx.paint);
                    break;
                case 3:
                    // A filled triangle
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStrokeWidth(1);
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setAntiAlias(true);
                    Path path = new Path();
                    path.moveTo(x + (ctx.dip2pix * yoffset), y);
                    path.lineTo(x + (ctx.dip2pix * (yoffset+(BARB_WIDTH/2))), y + (ctx.dip2pix*BARB_WIDTH));
                    path.lineTo(x + (ctx.dip2pix * (yoffset+BARB_WIDTH)), y );
                    path.lineTo(x + (ctx.dip2pix * yoffset), y);
                    path.close();
                    ctx.canvas.drawPath(path, paint);
                    // Extend the wind barb line as well
                    ctx.canvas.drawLine(x,y,x+(ctx.dip2pix*(BARB_LENGTH+12)),y,ctx.paint);
                    break;
            }

            yoffset -= BARB_OFFSET;
        }
        ctx.canvas.restore();
        // Draw the gust factor right below and to the right
        if (gusts != "") {
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTypeface(Typeface.createFromAsset(ctx.context.getAssets(), "LiberationMono-Bold.ttf"));
            paint.setShadowLayer(0, 0, 0, 0);
            paint.setAlpha(0xff);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            // How big is the text we are about to draw
            Rect mTextSize = new Rect();
            paint.getTextBounds(gusts, 0, gusts.length(), mTextSize);

            gx = x + (ctx.dip2pix*GUST_X);
            gy = y + (ctx.dip2pix*GUST_Y);
            ctx.canvas.drawText(gusts, gx, gy, paint);
            // Calculate the size of the shadow


        }

    }
    /**
     * Draw metar map from ADSB
     * @param ctx
     * @param map
     * @param shouldDraw
     */
    public static void drawMetars(DrawingContext ctx, HashMap<String, Metar> map, boolean shouldDraw) {
        if(0 == ctx.pref.showLayer() || (!shouldDraw) || (!ctx.pref.useAdsbWeather())) {
            // This shows only for metar layer, and when adsb is used
            return;
        }

        Set<String> keys = map.keySet();
        for(String key : keys) {
            Metar m = map.get(key);
            if(!isOnScreen(ctx.origin, m.lat, m.lon)) {
                continue;
            }
            float x = (float)ctx.origin.getOffsetX(m.lon);
            float y = (float)ctx.origin.getOffsetY(m.lat);
            String text = m.flightCategory;
            // Draw the wind barb first, if it is enabled
            if (ctx.pref.isShowWindBarbs())
            {
                drawWindBarb(ctx,x,y,m);
            }
            if (ctx.pref.isShowLabelMETARS())
            {
                // Do not draw unknown metars
                if (WeatherHelper.metarColor(m.flightCategory) != 0xffffffff) {
                    ctx.service.getShadowedText().drawAlpha(ctx.canvas, ctx.textPaint,
                            text, WeatherHelper.metarColor(m.flightCategory), (float) x, (float) y, ctx.pref.showLayer());
                }
            }
            else
            {
                ctx.paint.setColor(Color.BLACK);
                ctx.paint.setAlpha(ctx.pref.showLayer());
                ctx.canvas.drawCircle(x, y, ctx.dip2pix * 9, ctx.paint);
                ctx.paint.setColor(WeatherHelper.metarColor(m.flightCategory));
                ctx.paint.setAlpha(ctx.pref.showLayer());
                ctx.canvas.drawCircle(x, y, ctx.dip2pix * 8, ctx.paint);
            }
            /*
            */
            ctx.paint.setAlpha(255);
        }
    }

    /**
     *
     * @return
     */
    public HashMap<String, Metar> getAllMetars() {
        return mMetar;
    }

    /**
     * 
     * @param time
     * @param location
     * @param data
     */
    public void putTaf(long time, String location, String data) {
        if(!mPref.useAdsbWeather()) {
            return;
        }    
        Taf f = new Taf();
        f.rawText = location + " " + data;
        f.stationId = location;
        Date dt = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        f.time = sdf.format(dt) + "Z";
        f.timestamp = System.currentTimeMillis();
        mTaf.put(location, f);        
    }
    
    /**
     * 
     * @param time
     * @param location
     * @param data
     */
    public void putAirep(long time, String location, String data, DataSource db) {
        if(!mPref.useAdsbWeather()) {
            return;
        }    
        String lonlat = db.findLonLat(location, Destination.BASE);
        if(null == lonlat) {
            return;
        }
        String tokens[] = lonlat.split(",");
        if(tokens.length != 2) {
            return;
        }
        
        Airep a = new Airep();
        a.lon = Float.parseFloat(tokens[0]);
        a.lat = Float.parseFloat(tokens[1]);
        a.rawText = data;
        a.reportType = "PIREP";
        Date dt = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        a.time = sdf.format(dt) + "Z";
        a.timestamp = System.currentTimeMillis();
        
        mAirep.put(location, a);
    }
    
    /**
     * 
     * @param time
     * @param location
     * @param data
     */
    public void putWinds(long time, String location, String data) {
        if(!mPref.useAdsbWeather()) {
            return;
        }    
        WindsAloft w = new WindsAloft();
        w.station = location;
        
        /*
         * Clear garbage spaces etc. Convert to Avare format
         */
        String winds[] = data.split(",");
        if(winds.length < 9) {
            return;
        }
        w.w3k = winds[0];
        w.w6k = winds[1];
        w.w9k = winds[2];
        w.w12k = winds[3];
        w.w18k = winds[4];
        w.w24k = winds[5];
        w.w30k = winds[6];
        w.w34k = winds[7];
        w.w39k = winds[8];
        Date dt = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        w.time = sdf.format(dt) + "Z";
        
        /*
         * Find lon/lat of station
         */
        float coords[] = new float[2];
        if(!Stations.getStationLocation(location, coords)) {
            return;
        }
        
        w.lon = coords[0];
        w.lat = coords[1];
        w.timestamp = System.currentTimeMillis();
        mWinds.put(location, w);
    }
    
    /**
     * 
     * @param time
     * @param block
     * @param empty
     * @param isConus
     * @param data
     * @param cols
     * @param rows
     */
    public void putImg(long time, int block, int empty[], boolean isConus, int data[], int cols, int rows) {
        if(!mPref.useAdsbWeather()) {
            return;
        }
        if(isConus) {
            mNexradConus.putImg(time, block, empty, isConus, data, cols, rows);            
        }
        else {
            mNexrad.putImg(time, block, empty, isConus, data, cols, rows);            
        }
    }

    /**
     * 
     * @param airport
     * @return
     */
    public Taf getTaf(String airport) {
        Taf taf = mTaf.get("K" + airport);
        return taf;
    }

    /**
     * 
     * @param airport
     * @return
     */
    public Metar getMETAR(String airport) {
        return mMetar.get("K" + airport);
    }

    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public LinkedList<Airep> getAireps(double lon, double lat) {
        
        LinkedList<Airep> ret = new LinkedList<Airep>();
        
        /*
         * Find closest aireps
         */
        for(Airep a : mAirep.values()) {
            
            /*
             * Same formula as in database helper
             */
            if(
                    (a.lat > (lat - Airep.RADIUS)) && (a.lat < (lat + Airep.RADIUS)) &&
                    (a.lon > (lon - Airep.RADIUS)) && (a.lon < (lon + Airep.RADIUS))) {
                Airep n = new Airep(a);
                ret.add(n);
            }
            
        }

        
        return ret;
    }

    /**
     * 
     * @param lon
     * @param lat
     * @return
     */
    public WindsAloft getWindsAloft(double lon, double lat) {
        
        WindsAloft toret = null;
        double oldDistance = 1E10;
        
        /*
         * Find closest wind
         */
        for(WindsAloft w : mWinds.values()) {
            float mlon = w.lon;
            float mlat = w.lat;
            /*
             * Distance less? use this one
             */
            double dis = (mlon - lon) * (mlon - lon) + (mlat - lat) * (mlat - lat);
            if(oldDistance > dis) {
                oldDistance = dis;
                toret = w;
            } 
        }

        /*
         * Copy it because we change the title
         */
        if(null == toret) {
            return null;
        }
        WindsAloft w1 = new WindsAloft(toret);
        return w1;

    }

    /*
     * ALL ADSB weather should be kaput after expiry
     */
    public void sweep() {
        long now = System.currentTimeMillis();
        int expiry = mPref.getExpiryTime() * 60 * 1000;

        /*
         * Go at them one by one
         * LinkedList saves against concurrent modification exception
         */
        LinkedList<String> keys;
        
        /*
         * Winds
         */
        keys = new LinkedList<String>();
        for (String key : mWinds.keySet()) {
            WindsAloft w = mWinds.get(key);
            long diff = (now - w.timestamp) - expiry;
            if(diff > 0) {
                keys.add(key);
            }
        }
        for(String key : keys) {
            mWinds.remove(key);
        }
        
        /*
         * Taf
         */
        keys = new LinkedList<String>();
        for (String key : mTaf.keySet()) {
            Taf f = mTaf.get(key);
            long diff = (now - f.timestamp) - expiry;
            if(diff > 0) {
                keys.add(key);
            }
        }
        for(String key : keys) {
            mTaf.remove(key);
        }
        
        /*
         * Metar
         */
        keys = new LinkedList<String>();
        for (String key : mMetar.keySet()) {
            Metar m = mMetar.get(key);
            long diff = (now - m.timestamp) - expiry;
            if(diff > 0) {
                keys.add(key);
            }
        }
        for(String key : keys) {
            mMetar.remove(key);
        }

        /*
         * Airep
         */
        keys = new LinkedList<String>();
        for (String key : mAirep.keySet()) {
            Airep a = mAirep.get(key);
            long diff = (now - a.timestamp) - expiry;
            if(diff > 0) {
                keys.add(key);
            }
        }
        for(String key : keys) {
            mAirep.remove(key);
        }
        
        /*
         * Nexrad
         */
        LinkedList<Integer>keyi = new LinkedList<Integer>();
        SparseArray<NexradBitmap> img = mNexrad.getImages();
        for(int i = 0; i < img.size(); i++) {
            NexradBitmap n = img.valueAt(i);
            long diff = (now - n.timestamp) - expiry;
            if(diff > 0) {
                keyi.add(img.keyAt(i));
            }
        }
        for(Integer key : keyi) {
            img.remove(key);
        }
    }
}
