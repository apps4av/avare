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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.Logger;
import com.ds.avare.utils.Telemetry;
import com.ds.avare.utils.TelemetryParams;
import com.ds.avare.views.MemView;
import com.ds.avare.views.SatelliteView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    private Preferences mPref;

    // Request code for selecting a document.
    private static final int IMPORT = 2;
    private static final int EXPORT = 3;
    private static final int DELETE = 4;

    private static final int IO_BUFFER_SIZE = 4096;

    private ProgressBar mProgressBarExport;
    private ProgressBar mProgressBarImport;
    private ProgressBar mProgressBarDelete;
    private Spinner mSpinnerTypeExport;
    private Spinner mSpinnerTypeDelete;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = container.getContext();
        mPref = new Preferences(mContext);

        View view = inflater.inflate(R.layout.layout_satellite, container, false);
        mSatelliteView = (SatelliteView)view.findViewById(R.id.satellite);

        mGpsText = (TextView)view.findViewById(R.id.satellite_text_gps_details);
        mMemView = (MemView)view.findViewById(R.id.memory);
        mMemText = (TextView)view.findViewById(R.id.satellite_text_mem_details);
        mMapAreaText = (TextView)view.findViewById(R.id.satellite_text_map_details);

        // update periodically
        mRunning = true;
        mHandler.postDelayed(mRunnable, 1000);

        mProgressBarExport = (ProgressBar) view.findViewById(R.id.import_export_progress_bar_export);
        mProgressBarImport = (ProgressBar) view.findViewById(R.id.import_export_progress_bar_import);
        mProgressBarDelete = (ProgressBar) view.findViewById(R.id.import_export_progress_bar_delete);
        mSpinnerTypeExport = (Spinner) view.findViewById(R.id.import_export_spinner_export);
        mSpinnerTypeDelete = (Spinner) view.findViewById(R.id.import_export_spinner_delete);

        Button b = (Button)view.findViewById(R.id.import_export_button_export);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentDate = new SimpleDateFormat("__MMM_dd_yyyy-HH_mm_ss", Locale.getDefault()).format(new java.util.Date());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                String name = getString(R.string.app_name) + currentDate + ".zip";
                intent.putExtra(Intent.EXTRA_TITLE, name);
                getActivity().startActivityForResult(intent, EXPORT);
            }
        });

        b = (Button)view.findViewById(R.id.import_export_button_import);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                getActivity().startActivityForResult(intent, IMPORT);
            }
        });

        b = (Button)view.findViewById(R.id.import_export_button_delete);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(mContext);
                builder.setMessage(getString(R.string.Sure))
                        .setCancelable(true)
                        .setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        //confirm before deleting anything
                        .setPositiveButton(getString(R.string.Delete), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Logger.Logit(getString(R.string.Delete));
                                DeleteTask tsk = new DeleteTask();
                                tsk.execute();
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            }
        });

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





    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Uri uri = null;

        if(resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                uri = resultData.getData();
            }
        }
        if(requestCode == EXPORT && uri != null) {
            Logger.Logit(getString(R.string.Export));
            Telemetry t = new Telemetry(getActivity());
            TelemetryParams p = new TelemetryParams();
            p.add(TelemetryParams.EXPORT_NAME, uri.getPath());
            t.sendEvent(Telemetry.EXPORT, p);

            ExportTask tsk = new ExportTask();
            tsk.execute(uri);
        }
        else if(requestCode == IMPORT && uri != null) {
            Logger.Logit(getString(R.string.Import));

            Telemetry t = new Telemetry(getActivity());
            TelemetryParams p = new TelemetryParams();
            p.add(TelemetryParams.IMPORT_NAME, uri.getPath());
            t.sendEvent(Telemetry.IMPORT, p);
            ImportTask tsk = new ImportTask();
            tsk.execute(uri);
        }
    }

    private void updateProgress(int index, int total, int type) {
        ProgressBar b = mProgressBarExport;
        if(type == IMPORT) {
            b = mProgressBarImport;
        }
        else if (type == DELETE) {
            b = mProgressBarDelete;
        }
        if(index == -1) { // error
            Logger.Logit(getString(R.string.error));
            b.setProgress(0);
        }
        else if (index == total) {
            Logger.Logit(getString(R.string.done));
            b.setProgress(100); // done
        }
        else if (index == 0) { // start
            Logger.Logit(getString(R.string.processing));
        }
        else {
            // move bar %
            int perc = (int)((float)index / (float)total * 100.f);
            b.setProgress(perc);
        }
    }

    // Export data
    private class ExportTask extends AsyncTask<Uri, Integer, String> {

        @Override
        protected String doInBackground(Uri... paths) {
            // find which files to export
            int index = mSpinnerTypeExport.getSelectedItemPosition();
            final String filter = getResources().getStringArray(R.array.ExportDataType)[index];

            try {
                // open a place to write files to in zip
                OutputStream outStream = getActivity().getContentResolver().openOutputStream(paths[0]);
                ZipOutputStream out = new ZipOutputStream(outStream);
                // get list of internal files
                String folder = mPref.getServerDataFolder() + File.separatorChar + filter;
                File dir = new File(folder);
                LinkedList<File> files = Helper.getDirectoryContents(dir, null);

                byte data[] = new byte[IO_BUFFER_SIZE];
                int total = files.size();

                // process each file one by one
                for (int filec = 0; filec < total; filec++) {
                    FileInputStream fi = new FileInputStream(files.get(filec));
                    BufferedInputStream origin = new BufferedInputStream(fi, IO_BUFFER_SIZE);

                    // do not store full path as that's platform dependant
                    String name = files.get(filec).toString().split(mPref.getServerDataFolder() + File.separatorChar)[1];
                    ZipEntry entry = new ZipEntry(name);
                    out.putNextEntry(entry);
                    int count;

                    while ((count = origin.read(data, 0, IO_BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                    // next file
                    origin.close();
                    publishProgress(filec, total);
                }
                out.close();
                outStream.close();
                publishProgress(total, total);
            } catch (Exception e) {
                publishProgress(-1, 0);
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgress(values[0], values[1], EXPORT);
        }
    }

    // Export data
    private class DeleteTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            // find which files to delete
            int index = mSpinnerTypeDelete.getSelectedItemPosition();
            final String filter = getResources().getStringArray(R.array.DeleteDataType)[index];

            try {
                // get list of internal files
                String folder = mPref.getServerDataFolder() + (filter.startsWith("*") ?  "" : (File.separatorChar + filter));
                File dir = new File(folder);
                LinkedList<File> files = Helper.getDirectoryContents(dir, filter);

                int total = files.size();

                // process each file one by one
                for (int filec = 0; filec < total; filec++) {
                    files.get(filec).delete();
                    publishProgress(filec, total);
                }
                publishProgress(total, total);
                mService.getUDWMgr().forceReload();	// Tell the UDWs to reload
                mService.getExternalPlanMgr().forceReload(); // Reload plans too
            } catch (Exception e) {
                publishProgress(-1, 0);
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgress(values[0], values[1], DELETE);
        }
    }

    // Import data
    private class ImportTask extends AsyncTask<Uri, Integer, String> {

        @Override
        protected String doInBackground(Uri... paths) {
            try {
                // read files from file
                InputStream inStream = getActivity().getContentResolver().openInputStream(paths[0]);
                ZipInputStream in = new ZipInputStream(inStream);
                // get place of internal files
                String folder = mPref.getServerDataFolder();

                byte data[] = new byte[IO_BUFFER_SIZE];
                ZipEntry entry;
                int total = inStream.available();

                // process each file one by one
                while ((entry = in.getNextEntry()) != null) {
                    String name = entry.getName();
                    String path = folder + File.separatorChar + name;
                    File f = new File(path);
                    if(f.isDirectory()) { // skip dirs but make them
                        f.mkdirs();
                        continue;
                    }
                    File parent = f.getParentFile();
                    if(!parent.exists()) {
                        parent.mkdirs();
                    }
                    FileOutputStream fout = new FileOutputStream(folder + File.separatorChar + name);
                    int count;

                    while ((count = in.read(data)) != -1) {
                        fout.write(data, 0, count);
                    }

                    fout.close();
                    in.closeEntry();
                    // publish after each file bytes remaining
                    publishProgress(total - inStream.available(), total);
                }
                in.close();
                inStream.close();
                publishProgress(total, total);
                mService.getUDWMgr().forceReload();	// Tell the UDWs to reload
                mService.getExternalPlanMgr().forceReload(); // Reload plans too
            } catch (Exception e) {
                publishProgress(-1, 0);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgress(values[0], values[1], IMPORT);
        }
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
