/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.fragment;

import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ds.avare.R;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.MemView;
import com.ds.avare.views.SatelliteView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author zkhan
 */
public class SatelliteFragment extends StorageServiceGpsListenerFragment {

    public static final String TAG = "SatelliteFragment";

    /**
     * Shows satellites
     */
    private SatelliteView mSatelliteView;
    private MemView mMemView;
    private TextView mMemText;
    private TextView mMapAreaText;
    private SeekBar mBrightnessBar;
    private TextView mGpsText;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.satellite, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mSatelliteView = (SatelliteView) view.findViewById(R.id.satellite);

        mGpsText = (TextView) view.findViewById(R.id.satellite_text_gps_details);
        mMemView = (MemView) view.findViewById(R.id.memory);
        mMemText = (TextView) view.findViewById(R.id.satellite_text_mem_details);
        mMapAreaText = (TextView) view.findViewById(R.id.satellite_text_map_details);

        /*
         * Set brightness bar
         */
        mBrightnessBar = (SeekBar) view.findViewById(R.id.satellite_slider);
        mBrightnessBar.setMax(255);
        mBrightnessBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                if (Build.VERSION.SDK_INT >= 23) {
                    // Need special permission
                    if (!Settings.System.canWrite(getContext())) {
                        Intent i = new Intent();
                        i.setAction(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        startActivity(i);
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (!Settings.System.canWrite(getContext())) {
                        return;
                    }
                }

                if (fromUser) {
                    /*
                     * Manually set brightness
                     */
                    android.provider.Settings.System.putInt(
                            getContext().getContentResolver(),
                            android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    );
                    android.provider.Settings.System.putInt(
                            getContext().getContentResolver(),
                            android.provider.Settings.System.SCREEN_BRIGHTNESS,
                            progress
                    );
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * Set brightness bar to current value
         */
        try {
            float curBrightnessValue = android.provider.Settings.System.getInt(
                    getContext().getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
            );
            mBrightnessBar.setProgress((int) curBrightnessValue);
        } catch (Exception e) { }

    }

    @Override
    protected void postServiceConnected() {
        updateMem();
        updateMapArea();
    }

    @Override
    protected void onGpsStatus(GpsStatus gpsStatus) {
        mSatelliteView.updateGpsStatus(gpsStatus);
    }

    @Override
    protected void onGpsLocation(Location location) {
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
                    String.format(
                            Locale.getDefault(),
                            "%f,%f\n%s\n%s: %d",
                            latitude,
                            longitude,
                            lastTime,
                            getString(R.string.AltitudeAccuracy),
                            accuracy
                    )
            );
        } else {
            mSatelliteView.updateGpsStatus(null);
            mGpsText.setText("");
        }

        updateMem();
        updateMapArea();
    }

    @Override
    protected void onGpsTimeout(boolean timeout) {
        if (timeout) mSatelliteView.updateGpsStatus(null);
    }

    @Override
    protected void onGpsEnabled(boolean enabled) {
        if (!enabled) mSatelliteView.updateGpsStatus(null);
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

        mMemText.setText(String.format(Locale.getDefault(), "%dMB/%dMB", totalAlloc, max));
        mMemView.updateMemStatus((float)totalAlloc / (float)max);
    }

    private void updateMapArea() {
        /*
         * Map area numbers
         */

        /*
         * Find various metrics for user info
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // Subtract one tile from map width / height
        mMapAreaText.setText(
                String.format(
                        Locale.getDefault(),
                        "%s %dx%dpx\n%s %dx%dpx\n%s %d",
                        getString(R.string.MapSize),
                        (mService.getTiles().getXTilesNum() * BitmapHolder.WIDTH - BitmapHolder.WIDTH),
                        (mService.getTiles().getYTilesNum() * BitmapHolder.HEIGHT - BitmapHolder.HEIGHT),
                        getString(R.string.ScreenSize),
                        width,
                        height,
                        getString(R.string.Tiles),
                        (mService.getTiles().getOverhead() + mService.getTiles().getTilesNum())
                )
        );
    }

}
