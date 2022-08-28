package com.ds.avare.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.NavAid;
import com.ds.avare.position.Projection;
import com.ds.avare.shapes.Layer;
import com.ds.avare.shapes.MetShape;
import com.ds.avare.shapes.TFRShape;
import com.ds.avare.storage.Preferences;
import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.weather.AirSigMet;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;
import com.ds.avare.weather.WindsAloft;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * @author zkhan
 *
 */
public class AirportInfo extends AsyncTask<Object, String, String> {
    private Double lon;
    private Double lat;
    private String tfr = "";
    private String tfra = "";
    private String textMets = "";
    private String sua;
    private String layer;
    private LinkedList<Airep> aireps;
    private LinkedList<String> runways;
    private Taf taf;
    private WindsAloft wa;
    private Metar metar;
    private String elev;
    private Vector<NavAid> navaids;
    private StorageService mService;
    private Context mContext;
    private GpsParams mGpsParams;
    private GenericCallback mCb;
    private Layer mLayer;
    private Preferences mPref;


    private String getTfrTextOnTouch(LinkedList<TFRShape> shapes) {
        String out = "";
        if (null != shapes) {
            for (int shape = 0; shape < shapes.size(); shape++) {
                TFRShape cshape = shapes.get(shape);
                /*
                 * Get TFR text
                 */
                String txt = cshape.getTextIfTouched(lon, lat);
                if (null != txt) {
                    out += txt + "\n--\n";
                }
            }
        }
        return out;
    }

    /* (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected String doInBackground(Object... vals) {
        Thread.currentThread().setName("Closest");

        lon = (Double) vals[0];
        lat = (Double) vals[1];
        String airport = (String) vals[2];
        mContext = (Context) vals[3];
        mService = (StorageService) vals[4];
        mGpsParams = (GpsParams) vals[5];
        mPref = (Preferences) vals[6];
        mLayer = (Layer) vals[7];
        mCb = (GenericCallback) vals[8];

        // if the user is moving instead of doing a long press, give them a chance
        // to cancel us before we start doing anything
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }

        if (isCancelled())
            return "";

        List<AirSigMet> mets = null;
        if (null != mService) {
            if (mPref.useAdsbWeather()) {
                mets = mService.getAdsbWeather().getAirSigMet();
            } else {
                mets = mService.getInternetWeatherCache().getAirSigMet();
            }
        }


        /*
         * Air/sigmets
         */
        if (null != mets) {
            for (int i = 0; i < mets.size(); i++) {
                MetShape cshape = mets.get(i).shape;
                if (null != cshape) {
                    /*
                     * Set MET
                     */
                    textMets += cshape.getHTMLMetOnTouch(mContext, mets.get(i), lon, lat);
                }
            }
        }

        if(null == airport) {
            airport = mService.getDBResource().findClosestAirportID(lon, lat);
        }
        if (isCancelled()) {
            return "";
        }

        if (null == airport) {
            airport = "" + Helper.truncGeo(lat) + "&" + Helper.truncGeo(lon);
        } else {
            taf = mService.getDBResource().getTaf(airport);
            if (isCancelled()) {
                return "";
            }

            metar = mService.getDBResource().getMetar(airport);
            if (isCancelled()) {
                return "";
            }

            runways = mService.getDBResource().findRunways(airport);
            if (isCancelled()) {
                return "";
            }

            elev = mService.getDBResource().findElev(airport);
            if (isCancelled()) {
                return "";
            }

        }

        /*
         * ADSB gets this info from weather cache
         */
        if (!mPref.useAdsbWeather()) {
            aireps = mService.getDBResource().getAireps(lon, lat);
            if (isCancelled()) {
                return "";
            }

            wa = mService.getDBResource().getWindsAloft(lon, lat);
            if (isCancelled()) {
                return "";
            }

            sua = mService.getDBResource().getSua(lon, lat);
            if (isCancelled()) {
                return "";
            }

            if (mLayer != null) {
                layer = mLayer.getDate();
            }
            if (isCancelled()) {
                return "";
            }
        }

        navaids = mService.getDBResource().findNavaidsNearby(lat, lon);

        return airport;
    }

    /* (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String airport) {
        Projection p = new Projection(mGpsParams.getLongitude(), mGpsParams.getLatitude(), lon, lat);
        if (null != mCb && null != airport) {
            LongTouchDestination ltd = new LongTouchDestination();
            ltd.airport = airport;
            ltd.info = Math.round(p.getDistance()) + Preferences.distanceConversionUnit +
                    "(" + p.getGeneralDirectionFrom(mGpsParams.getDeclinition()) + ") " +
                    Helper.correctConvertHeading(Math.round(Helper.getMagneticHeading(p.getBearing(), mGpsParams.getDeclinition()))) + '\u00B0';

            /*
             * Clear old weather
             */
            mService.getAdsbWeather().sweep();
            mService.getAdsbTfrCache().sweep();

            /*
             * Do not background ADSB weather as its a RAM opertation and quick,
             * also avoids concurrent mod exception.
             */

            if (mPref.useAdsbWeather()) {
                taf = mService.getAdsbWeather().getTaf(airport);
                metar = mService.getAdsbWeather().getMETAR(airport);
                aireps = mService.getAdsbWeather().getAireps(lon, lat);
                wa = mService.getAdsbWeather().getWindsAloft(lon, lat);
                layer = mService.getAdsbWeather().getNexrad().getDate();
                sua = mService.getAdsbWeather().getSua();
            } else {
                boolean inWeatherOld = mService.getInternetWeatherCache().isOld(mPref.getExpiryTime());
                if (inWeatherOld) { // expired weather does not show
                    taf = null;
                    metar = null;
                    aireps = null;
                    textMets = null;
                    wa = null;
                }
            }
            if (null != aireps) {
                for (Airep a : aireps) {
                    a.updateTextWithLocation(lon, lat, mGpsParams.getDeclinition());
                }
            }
            if (null != wa) {
                wa.updateStationWithLocation(lon, lat, mGpsParams.getDeclinition());
            }
            tfr = getTfrTextOnTouch(mService.getTFRShapes());
            tfra = getTfrTextOnTouch(mService.getAdsbTFRShapes());
            ltd.tfr = tfr + "\n" + tfra;
            ltd.taf = taf;
            ltd.metar = metar;
            ltd.airep = aireps;
            ltd.mets = textMets;
            ltd.wa = wa;
            ltd.sua = sua;
            ltd.layer = layer;
            //ideally we would pass altitude AGL for navaid reception calculations
            ltd.navaids = new NavAidHelper(mContext, lon, lat, mGpsParams.getAltitude()).toHtmlString(navaids);
            if (metar != null) {
                ltd.performance =
                        WeatherHelper.getMetarTime(metar.rawText) + "\n" +
                                mContext.getString(R.string.DensityAltitude) + " " +
                                WeatherHelper.getDensityAltitude(metar.rawText, elev) + "\n" +
                                mContext.getString(R.string.BestRunway) + " " +
                                WeatherHelper.getBestRunway(metar.rawText, runways);
            }

            mCb.callback(this, ltd);
        }
    }
}

