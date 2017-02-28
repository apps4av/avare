package com.ds.avare.place;

import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.storage.DataBaseHelper;
import com.ds.avare.userDefinedWaypoints.UDWMgr;
import com.ds.avare.userDefinedWaypoints.Waypoint;


/**
 * Created by zkhan on 1/18/17.
 */

public class UDWDestination extends Destination {

    private String mCmt;

    public UDWDestination(StorageService service, String name) {
        super(service, name);

        mDbType = UDW;
        mDestType = UDW;

        Waypoint p = mService.getUDWMgr().get(mName);
        if(null != p) {
            mLatd = p.getLat();
            mLond = p.getLon();
            if(!mInited) {
                mLonInit = mLond;
                mLatInit = mLatd;
                mInited = true;
            }
            mCmt  = p.getCmt();
            mParams.put(DataBaseHelper.LONGITUDE, "" + mLond);
            mParams.put(DataBaseHelper.LATITUDE, "" + mLatd);
            mParams.put(DataBaseHelper.FACILITY_NAME, UDWMgr.UDWDESCRIPTION);
            mFound = true;
        }
        else {
            mFound = false;
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
            mWinds = mService.getDBResource().getWindsAloft(mLond, mLatd);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            found();
        }

    }

    @Override
    public String getCmt() {
        return mCmt;
    }

}
