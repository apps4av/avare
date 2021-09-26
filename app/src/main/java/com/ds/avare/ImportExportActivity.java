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
import android.widget.Toast;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * @author zkhan
 * An activity that deals with information import / export. Introduced after scoped storage
 */
public class ImportExportActivity extends Activity {
    private Preferences mPref;
    private EditText mEditTextImport;

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

        Button importMaps = (Button)view.findViewById(R.id.import_export_button_import_maps);
        Button importUser = (Button)view.findViewById(R.id.import_export_button_import_userdata);
        importMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile(Uri.fromFile(new File( "/")), IMPORT_FILE_MAPS);
            }
        });
        importUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile(Uri.fromFile(new File( "/")), IMPORT_FILE_USER);
            }
        });

        mEditTextImport = (EditText)view.findViewById(R.id.import_export_edittext_import_path);


    }


    // Request code for selecting a document.
    private static final int IMPORT_FILE_MAPS = 2;
    private static final int IMPORT_FILE_USER = 3;

    /*
     * Pick a file to act on
     */
    private void openFile(Uri pickerInitialUri, int code) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, code);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        String toFile = null;
        Uri uri = null;
        String folder = null;

        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == IMPORT_FILE_USER) {
                folder = mPref.getUserDataFolder() + File.separatorChar + mEditTextImport.getText().toString();
            }
            else if(requestCode == IMPORT_FILE_MAPS) {
                folder = mPref.getServerDataFolder() + File.separatorChar + mEditTextImport.getText().toString();
            }
        }

        if (resultData != null) {
            uri = resultData.getData();
            // keep name same but change path for copy
            toFile = new File(uri.getPath()).getName();
        }

        // The result data contains a URI for the document or directory that
        // the user selected.
        // now copy to file with same name
        if (toFile != null && folder != null && uri != null) {
            ImportTask i = new ImportTask();
            i.execute(uri, folder, toFile);
        }
    }

    private class ImportTask extends AsyncTask<Object, Integer, String> {

        @Override
        protected String doInBackground(Object... objs) {
            publishProgress(0);
            try {
                InputStream inStream = getContentResolver().openInputStream((Uri)objs[0]);
                File f = new File((String)objs[1]);
                f.mkdirs();
                OutputStream out = new FileOutputStream((String)objs[1] + File.separatorChar + (String)objs[2]);
                byte[] buf = new byte[1024];
                int len;
                while((len = inStream.read(buf)) > 0){
                    out.write(buf,0, len);
                }
                out.close();
                inStream.close();
            } catch (Exception e) {
                publishProgress(-1);
                return null;
            }
            publishProgress(100);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int pg = values[0];
            if(pg == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.copying), Toast.LENGTH_LONG).show();
            }
            else if (pg == 100) {
                Toast.makeText(getApplicationContext(), getString(R.string.done), Toast.LENGTH_LONG).show();
            }
            else if (pg == -1) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed), Toast.LENGTH_LONG).show();
            }
        }
    }
}
