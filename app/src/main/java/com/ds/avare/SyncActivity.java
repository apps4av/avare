/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 */

package com.ds.avare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.ds.avare.utils.Helper;
import com.ds.avare.utils.ZipFolder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.InputStream;
import java.io.OutputStream;

//author zkhan

/**
 * Store user's data on Google drive
 * Note: Testing indicates that Internet connection is not required for Google Drive load/store.
 * The API seems to be caching and taking care of uploads / sync when Internet connection becomes available.
 */
public class SyncActivity extends Activity implements ConnectionCallbacks,  OnConnectionFailedListener {

    private static final int REQUEST_CODE_OPENER = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;

    private boolean mConnected;

    private TextView mLogText;

    @Override
    /**
     *
     */
    public void onCreate(Bundle savedInstanceState) {
        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.gdrive, null);
        setContentView(view);

        // These buttons save and restore data from Google Drive
        Button saveButton = (Button)view.findViewById(R.id.gdrive_button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveToDrive();
            }
        });
        Button loadButton = (Button)view.findViewById(R.id.gdrive_button_load);
        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loadFromDrive();
            }
        });

        // Logging window
        mLogText = (TextView)view.findViewById(R.id.gdrive_text_log);
        print(getString(R.string.SyncMessage));
    }


    /**
     * Save data to Drive.
     */
    private void saveToDrive() {

        print(getString(R.string.BackingUp));

        new Thread() {

            @Override
            public void run() {

                // Retires for connect
                int retries = 5;
                while((!mConnected) && (retries > 1)) {
                    mHandler.sendEmptyMessage(CODE_CONNECT);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        print(getString(R.string.BackupFailed));
                        print(e.getMessage());
                    }
                    retries--;
                }
                // Still not connected?, return
                if(!mConnected) {
                    print(getString(R.string.BackupFailed));
                    return;
                }

                // Start by creating new content, and setting a callback.
                Drive.DriveApi.newDriveContents(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<DriveContentsResult>() {

                            @Override
                            public void onResult(final DriveContentsResult result) {

                                // If the operation was not successful, we cannot do anythin and must fail.
                                if (!result.getStatus().isSuccess()) {
                                    print(getString(R.string.BackupFailed));
                                    return;
                                }
                                // Otherwise, we can write our data to the new contents.
                                // Get an output stream for the contents from the app folder
                                // Note: to store anything from other maps folder like KML plans, use folder by Preferences.getMaps()
                                print(getString(R.string.Wait));
                                OutputStream outputStream = result.getDriveContents().getOutputStream();
                                if (!ZipFolder.zipFiles(SyncActivity.this.getFilesDir().getParent(), outputStream)) {
                                    print(getString(R.string.Failed));
                                }

                                // Create the initial metadata - MIME type and title.
                                // Note that the user will be able to change the title later.
                                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                        .setMimeType("application/zip").setTitle("avare_data.zip").build();
                                // Create an intent for the file chooser, and start it.
                                IntentSender intentSender = Drive.DriveApi
                                        .newCreateFileActivityBuilder()
                                        .setInitialMetadata(metadataChangeSet)
                                        .setInitialDriveContents(result.getDriveContents())
                                        .build(mGoogleApiClient);
                                try {
                                    startIntentSenderForResult(
                                            intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                                } catch (SendIntentException e) {
                                    print(getString(R.string.Failed));
                                    print(e.getMessage());
                                    return;
                                }
                            }
                        });
            }
        }.start();
    }

    /**
     * Load data from Drive.
     */
    private void loadFromDrive() {

        // Send intent to choose a file, result will be sent in onActivityResult method
        print(getString(R.string.RestoringDrive));
        new Thread() {
            @Override
            public void run() {
                // keep retrying for 5 times
                int retries = 5;
                while ((!mConnected) && (retries > 1)) {
                    mHandler.sendEmptyMessage(CODE_CONNECT);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        print(getString(R.string.RestoreFailed));
                        print(e.getMessage());
                    }
                    retries--;
                }
                if (!mConnected) {
                    // give up
                    print(getString(R.string.RestoreFailed));
                    return;
                }
                IntentSender intentSender = Drive.DriveApi
                        .newOpenFileActivityBuilder()
                        .setMimeType(new String[]{"application/zip"})
                        .build(mGoogleApiClient);
                try {
                    startIntentSenderForResult(
                            intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                } catch (SendIntentException e) {
                    print(getString(R.string.Failed));
                    print(e.getMessage());
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        // Connect when we come in.
        // This is used to get user to choose which account they need to store data with
        super.onResume();
        Helper.setOrientationAndOn(this);

        mHandler.sendEmptyMessage(CODE_CONNECT);
    }

    @Override
    protected void onPause() {
        // Disconnect when leaving activity
        mHandler.sendEmptyMessage(CODE_DISCONNECT);
        super.onPause();
    }

    // This is where all results come in for drive save/load
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                // Creator of file need not do anything because we already wrote to stream
                if (resultCode == RESULT_OK) {
                    print(getString(R.string.Success));
                }
                else {
                    print(getString(R.string.Failed));
                }
                break;
            case REQUEST_CODE_OPENER:
                // Reader of file needs to read file which user picked
                if (resultCode == RESULT_OK) {

                    new Thread() {
                        @Override
                        public void run() {
                            // keep retrying for 5 times
                            int retries = 5;
                            while((!mConnected) && (retries > 1)) {
                                mHandler.sendEmptyMessage(CODE_CONNECT);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    print(getString(R.string.RestoreFailed));
                                    print(e.getMessage());
                                }
                                retries--;
                            }
                            if(!mConnected) {
                                // give up
                                print(getString(R.string.RestoreFailed));
                                return;
                            }

                            // get file ID, read it
                            print(getString(R.string.Wait));
                            DriveId driveId = data.getParcelableExtra(
                                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                            DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, driveId);

                            DriveContentsResult driveContentsResult =
                                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
                            if (!driveContentsResult.getStatus().isSuccess()) {
                                print(getString(R.string.RestoreFailed));
                                return;
                            }

                            // read file and apply to properties in the app folder
                            DriveContents driveContents = driveContentsResult.getDriveContents();
                            InputStream inputStream = driveContents.getInputStream();
                            if(ZipFolder.unzipFiles(SyncActivity.this.getFilesDir().getParent(), inputStream)) {
                                print(getString(R.string.SuccessWithReset));
                            }
                            else {
                                print(getString(R.string.Failed));
                            }
                        }
                    }.start();
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        print(getString(R.string.ConnectionFailed));
        // Called whenever the API client fails to connect.
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        }
        catch (SendIntentException e) {
            print(getString(R.string.Failed));
            print(e.getMessage());
        }
        mConnected = false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mConnected = true;
        print(getString(R.string.ConnectedDrive));
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mConnected = false;
        print(getString(R.string.DisconnectedDrive));
    }


    /*
     * Do logs and connection stuff on handler
     */
    private static final int CODE_LOG = 0;
    private static final int CODE_CONNECT = 1;
    private static final int CODE_DISCONNECT = 2;

    // Print to log text
    private void print(String toprint) {
        Message msg = mHandler.obtainMessage();
        msg.what = CODE_LOG;
        msg.obj = toprint;
        mHandler.sendMessage(msg);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == CODE_LOG) {
                if (msg.obj instanceof String) {
                    mLogText.setText(((String)msg.obj + "\n" + mLogText.getText()));
                }
            }
            else if(msg.what == CODE_CONNECT) {
                if (mGoogleApiClient == null) {
                    // Create the API client and bind it to an instance variable.
                    // We use this instance as the callback for connection and connection
                    // failures.
                    // Since no account name is passed, the user is prompted to choose.
                    mGoogleApiClient = new GoogleApiClient.Builder(SyncActivity.this)
                            .addApi(Drive.API)
                            .addScope(Drive.SCOPE_FILE)
                            .addConnectionCallbacks(SyncActivity.this)
                            .addOnConnectionFailedListener(SyncActivity.this)
                            .build();
                }
                print(getString(R.string.ConnectingDrive));
                // Connect the client. Once connected, the camera is launched.
                mGoogleApiClient.connect();
            }
            else if(msg.what == CODE_DISCONNECT) {
                if (mGoogleApiClient != null) {
                    print(getString(R.string.DisconnectingDrive));
                    mGoogleApiClient.disconnect();
                    mConnected = false;
                }
            }
        }
    };
}
