/*
Copyright (c) 2021, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author zkhan
 * An activity that deals with information import / export. Introduced after scoped storage
 */
public class ImportExportActivity extends Activity {
    private Preferences mPref;

    // Request code for selecting a document.
    private static final int IMPORT = 2;
    private static final int EXPORT = 3;

    private static final int IO_BUFFER_SIZE = 4096;

    private ProgressBar mProgressBar;

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mPref = new Preferences(getApplicationContext());

        /*
         * Get views from XML
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.import_export, null);
        setContentView(view);

        mProgressBar = (ProgressBar) view.findViewById(R.id.import_export_progress_bar);

        Button b = (Button)view.findViewById(R.id.import_export_button_export);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentDate = new SimpleDateFormat("__MMM_dd_yyyy-HH_mm_ss", Locale.getDefault()).format(new Date());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                String name = getString(R.string.app_name) + currentDate + ".zip";
                intent.putExtra(Intent.EXTRA_TITLE, name);
                startActivityForResult(intent, EXPORT);
            }
        });

        b = (Button)view.findViewById(R.id.import_export_button_import);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                startActivityForResult(intent, IMPORT);
            }
        });

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
            ExportTask t = new ExportTask();
            t.execute(uri);
        }
        else if(requestCode == IMPORT && uri != null) {
            ImportTask t = new ImportTask();
            t.execute(uri);
        }
    }

    private void updateProgress(int index, int total) {
        if(index == 0) { // start
            Toast.makeText(getApplicationContext(), getString(R.string.copying), Toast.LENGTH_LONG).show();
            mProgressBar.setProgress(0);
        }
        else if (index == total) {
            Toast.makeText(getApplicationContext(), getString(R.string.done), Toast.LENGTH_LONG).show();
            mProgressBar.setProgress(100); // done
        }
        else if (index == -1) { // error
            Toast.makeText(getApplicationContext(), getString(R.string.failed), Toast.LENGTH_LONG).show();
        }
        else {
            // move bar %
            int perc = (int)((float)index / (float)total * 100.f);
            mProgressBar.setProgress(perc);
        }
    }

    // Export data
    private class ExportTask extends AsyncTask<Uri, Integer, String> {

        @Override
        protected String doInBackground(Uri... paths) {
            publishProgress(0, 0);
            try {
                // open a place to write files to in zip
                OutputStream outStream = getContentResolver().openOutputStream(paths[0]);
                ZipOutputStream out = new ZipOutputStream(outStream);
                // get list of internal files
                String folder = mPref.getServerDataFolder();
                File dir = new File(folder);
                LinkedList<File> files = Helper.getDirectoryContents(dir);

                byte data[] = new byte[IO_BUFFER_SIZE];
                int total = files.size();

                // process each file one by one
                for (int filec = 0; filec < total; filec++) {
                    FileInputStream fi = new FileInputStream(files.get(filec));
                    BufferedInputStream origin = new BufferedInputStream(fi, IO_BUFFER_SIZE);

                    // do not store full path as that's platform dependant
                    String name = files.get(filec).toString().split(folder + File.separatorChar)[1];
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
            updateProgress(values[0], values[1]);
        }
    }

    // Import data
    private class ImportTask extends AsyncTask<Uri, Integer, String> {

        @Override
        protected String doInBackground(Uri... paths) {
            publishProgress(0, 0);
            try {
                // read files from file
                InputStream inStream = getContentResolver().openInputStream(paths[0]);
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
            } catch (Exception e) {
                publishProgress(-1, 0);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgress(values[0], values[1]);
        }
    }

}
