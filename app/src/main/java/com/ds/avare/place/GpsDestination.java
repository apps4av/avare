package com.ds.avare.place;

import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.content.ContentProviderHelper;
import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.utils.Helper;


/**
 * Created by zkhan on 1/18/17.
 */

public class GpsDestination extends Destination {

    public GpsDestination(StorageService service, String name) {
        super(service, name);

        mDbType = GPS;
        mDestType = GPS;

        double coords[] = {0, 0}; // lon, lat
        if(null == Helper.decodeGpsAddress(name, coords)) {
            mName = "";
            mDestType = "";
            mFound = false;
        }
        else {
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
            mParams.put(LocationContentProviderHelper.FACILITY_NAME, GPS);
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

        if(mFound) {
            DataBaseTask locmDataBaseTask = new DataBaseTask();
            locmDataBaseTask.execute();
        }

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
            mWinds = ContentProviderHelper.getWindsAloft(mService.getApplicationContext(), mLond, mLatd);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            found();
        }


    }
}
