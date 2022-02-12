/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;


import android.app.Fragment;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.MemView;
import com.ds.avare.views.SatelliteView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author rasii, zkhan
 *
 */
public class ToolsFragment extends Fragment {


    private Context mContext;
    /**
     * Shows satellites
     */
    private SatelliteView mSatelliteView;
    private MemView mMemView;
    private TextView mMemText;
    private TextView mMapAreaText;
    StorageService mService;

    private TextView mGpsText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = container.getContext();

        View view = inflater.inflate(R.layout.layout_satellite, container, false);
        mSatelliteView = (SatelliteView)view.findViewById(R.id.satellite);

        mGpsText = (TextView)view.findViewById(R.id.satellite_text_gps_details);
        mMemView = (MemView)view.findViewById(R.id.memory);
        mMemText = (TextView)view.findViewById(R.id.satellite_text_mem_details);
        mMapAreaText = (TextView)view.findViewById(R.id.satellite_text_map_details);

        // update periodically
        mRunning = true;
        mHandler.postDelayed(mRunnable, 1000);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRunning = false;
    }

    private void updateMem() {
        /*
         * Memory numbers
         */
        Runtime rt = Runtime.getRuntime();
        long vmAlloc = rt.totalMemory() - rt.freeMemory();
        long nativeAlloc = Debug.getNativeHeapAllocatedSize();
        long totalAlloc = (nativeAlloc + vmAlloc) / (1024 * 1024);

        long max = rt.maxMemory() / (1024 * 1024);

        mMemText.setText(totalAlloc + "MB/" + max + "MB");
        mMemView.updateMemStatus((float)totalAlloc / (float)max);
    }

    public void update() {
        mService = ((IOActivity)getActivity()).getService();
        if(mService == null) {
            return;
        }
        // valid
        updateMem();
        updateMapArea();

        GpsStatus gpsStatus = ((IOActivity)getActivity()).getGpsStatus();
        Location location = ((IOActivity)getActivity()).getLocation();

        if (location != null) {
            double latitude = Helper.truncGeo(location.getLatitude());
            double longitude = Helper.truncGeo(location.getLongitude());
            int accuracy = (int) Math.round(location.getAccuracy() * Preferences.heightConversion);
            Date dt = new Date(System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String lastTime = sdf.format(dt);
            sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
            lastTime += "/" + sdf.format(dt) + "Z";

            mGpsText.setText(
                    latitude + "," + longitude + "\n" +
                            lastTime + "\n" +
                            getString(R.string.AltitudeAccuracy) + ": " + accuracy
            );
            mSatelliteView.updateGpsStatus(gpsStatus);
        } else {
            mSatelliteView.updateGpsStatus(null);
            mGpsText.setText("");
        }
    }

    private void updateMapArea() {
        /*
         * Map area numbers
         */

        /*
         * Find various metrics for user info
         */
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();


        // Subtract one tile from map width / height
        mMapAreaText.setText(
                getString(R.string.MapSize) + " " + (mService.getTiles().getXTilesNum() * BitmapHolder.WIDTH - BitmapHolder.WIDTH)+ "x" + (mService.getTiles().getYTilesNum() * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT) + "px\n" +
                        getString(R.string.ScreenSize) + " " + width + "x" + height + "px" + "\n" + getString(R.string.Tiles) + " " + (mService.getTiles().getOverhead() + mService.getTiles().getTilesNum()));
    }

    final Handler mHandler = new Handler();
    private boolean mRunning = false;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mRunning) {
                update();
                mHandler.postDelayed(this, 1000);
            }
        }
    };

}
