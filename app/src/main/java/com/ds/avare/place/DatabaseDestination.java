package com.ds.avare.place;

import android.os.AsyncTask;

import com.ds.avare.StorageService;
import com.ds.avare.content.ContentProviderHelper;
import com.ds.avare.content.LocationContentProviderHelper;
import com.ds.avare.content.DataSource;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.StringPreference;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.LinkedList;


/**
 * Created by zkhan on 1/18/17.
 */

public class DatabaseDestination extends Destination {

    private LinkedList<Runway> mRunways;
    private LinkedHashMap<String, String> mFreq;
    private LinkedList<Awos> mAwos;
    private String mAfdFound[];

    /**
     * Cache it for database query from async task
     */
    private DataSource mDataSource;

    public DatabaseDestination(StorageService service, String name, String type) {
        super(service, name);

        mRunways = new LinkedList<Runway>();
        mFreq = new LinkedHashMap<String, String>();
        mAwos = new LinkedList<Awos> ();
        mAfdFound = null;

        mDbType = "";
        mDestType = type;

        mDataSource = mService.getDBResource();
    }

    @Override
    public void find() {
        /*
         * Do in background as database queries are disruptive
         */
        mLooking = true;
        DataBaseTask locmDataBaseTask = new DataBaseTask();
        locmDataBaseTask.execute(false, "");
    }

    @Override
    public void findGuessType() {
        mLooking = true;
        DataBaseTask locmDataBaseTask = new DataBaseTask();
        locmDataBaseTask.execute(true, "");
    }

    @Override
    public void find(String dbType) {
        mLooking = true;
        DataBaseTask locmDataBaseTask = new DataBaseTask();
        locmDataBaseTask.execute(false, dbType);
    }


    /**
     * @author zkhan
     */
    private class DataBaseTask extends AsyncTask<Object, Void, Boolean> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {

            Thread.currentThread().setName("Destination");

            Boolean guess = (Boolean)vals[0];
            String dbType = (String)vals[1];

            /*
             * If we dont know type, find with a guess.
             */
            if(guess) {
                StringPreference s = mService.getDBResource().searchOne(mName);
                if(null == s) {
                    return false;
                }
                mDestType = s.getType();
                mName = s.getId();
            }



            if(null == mDataSource) {
                return false;
            }


	        /*
	         * For all others, find in DB
	         */
            mDataSource.findDestination(mName, mDestType, dbType, mParams, mRunways, mFreq, mAwos);

            if(mDestType.equals(BASE)) {

                /*
                 * Find A/FD
                 */
                mAfdFound = null;
                final LinkedList<String> afdName = mDataSource.findAFD(mName);
                if(afdName.size() > 0) {
                    FilenameFilter filter = new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            boolean match = false;
                            for(final String name : afdName) {
                                match |= fileName.matches(name + Preferences.IMAGE_EXTENSION) ||
                                        fileName.matches(name + "-[0-9]+" + Preferences.IMAGE_EXTENSION);
                            }
                            return match;
                        }
                    };
                    String afd[] = null;
                    afd = new File(mPref.mapsFolder() + "/afd/").list(filter);
                    if(null != afd) {
                        java.util.Arrays.sort(afd);
                        int len1 = afd.length;
                        String tmp1[] = new String[len1];
                        for(int count = 0; count < len1; count++) {
                            /*
                             * Add A/FD
                             */
                            String tokens[] = afd[count].split(Preferences.IMAGE_EXTENSION);
                            tmp1[count] = mPref.mapsFolder() + "/afd/" +
                                    tokens[0];
                        }
                        if(len1 > 0) {
                            mAfdFound = tmp1;
                        }
                    }
                }
            }

            try {
                // Find winds
                mLond = Double.parseDouble(mParams.get(LocationContentProviderHelper.LONGITUDE));
                mLatd = Double.parseDouble(mParams.get(LocationContentProviderHelper.LATITUDE));
                mWinds = ContentProviderHelper.getWindsAloft(mService.getApplicationContext(), mLond, mLatd);
            }
            catch (Exception e) {
                return false;
                // Bad find
            }

            return(!mParams.isEmpty());
        }


        /* (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
        	/*
        	 * This runs on UI
        	 */
            mFound = result;
            if(mFound) {
                mDbType = mParams.get(LocationContentProviderHelper.TYPE);
                try {
                    mLond = Double.parseDouble(mParams.get(LocationContentProviderHelper.LONGITUDE));
                    mLatd = Double.parseDouble(mParams.get(LocationContentProviderHelper.LATITUDE));
                }
                catch(Exception e) {
                    mFound = false;
                }
            }

            found();
        }
    }

    @Override
    public LinkedList<Awos> getAwos() {
        return(mAwos);
    }

    @Override
    public LinkedHashMap<String, String> getFrequencies() {
        return(mFreq);
    }

    @Override
    public LinkedList<Runway> getRunways() {
        return(mRunways);
    }

    @Override
    public String[] getAfd() {
        return(mAfdFound);
    }

}
