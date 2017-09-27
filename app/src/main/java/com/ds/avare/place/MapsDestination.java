package com.ds.avare.place;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.content.ContentProviderHelper;
import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.utils.Helper;

import java.util.List;


/**
 * Created by zkhan on 1/18/17.
 */

public class MapsDestination extends Destination {


    public MapsDestination(StorageService service, String name) {
        super(service, name);

        mDbType = MAPS;
        mDestType = MAPS;

                /*
         * For Google maps address, if we have already geo decoded it using internet,
         * then no need to do again because internet may not be available on flight.
         * It could be coming from storage and not google maps.
         */
        double coords[] = {0, 0}; // lon, lat
        if(null != Helper.decodeGpsAddress(mName, coords)) {
            mName = name;
            mLond = coords[0];
            mLatd = coords[1];
            if(!mInited) {
                mLonInit = mLond;
                mLatInit = mLatd;
                mInited = true;
            }
            mParams.put(LocationContentProviderHelper.LONGITUDE, "" + mLond);
            mParams.put(LocationContentProviderHelper.LATITUDE, "" + mLatd);
            mParams.put(LocationContentProviderHelper.FACILITY_NAME, MAPS);
            mFound = true;
        }
    }

    @Override
    public void findGuessType() {
        find();
    }

    @Override
    public void find(String dbType) {
        find();
    }


    @Override
    public void find() {
        DataBaseTask locmDataBaseTask = new DataBaseTask();
        locmDataBaseTask.execute();
    }


    /**
     * @author zkhan
     */
    private class DataBaseTask extends AsyncTask<Void, Void, Void> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... vals) {
            Thread.currentThread().setName("Destination");

            if (!mFound) {
                /*
                 * We have not decomposed
                 */
                String strAddress = mName;

                Geocoder coder = new Geocoder(mService);
                Address location = null;

                /*
                 * Decompose
                 */
                try {
                    List<Address> address = coder.getFromLocationName(strAddress, 1);
                    if (address != null) {
                        location = address.get(0);
                    }
                } catch (Exception e) {
                    return null;
                }

                if (null == location) {
                    return null;
                }

                /*
                 * Decomposed it
                 *
                 */
                try {
                    mLond = Helper.truncGeo(location.getLongitude());
                    mLatd = Helper.truncGeo(location.getLatitude());
                } catch (Exception e) {

                }
                if ((!Helper.isLatitudeSane(mLatd)) || (!Helper.isLongitudeSane(mLond))) {
                    return null;
                }

                mName += "@" + mLatd + "&" + mLond;

                if (!mInited) {
                    mLonInit = mLond;
                    mLatInit = mLatd;
                    mInited = true;
                }

                mParams.put(LocationContentProviderHelper.LONGITUDE, "" + mLond);
                mParams.put(LocationContentProviderHelper.LATITUDE, "" + mLatd);
                mParams.put(LocationContentProviderHelper.FACILITY_NAME, MAPS);

                mFound = true;
            }

            mWinds = ContentProviderHelper.getWindsAloft(mService.getApplicationContext(), mLond, mLatd);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            found();
        }
    }

}
