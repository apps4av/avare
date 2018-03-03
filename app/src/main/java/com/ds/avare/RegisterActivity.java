/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ds.avare.message.Helper;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
 
/**
 * 
 * @author zkhan
 *
 */
public class RegisterActivity extends Activity {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    
    static AsyncTask<Void, Void, Boolean> mRegisterTask = null;

    // Register button
    private Button mButtonRegister;
    private EditText mEmailEditText;
    private WebView mPrivacy;
    private Preferences mPref;

    private void setButtonStates() {
        if(mPref.isRegistered()) {
            mEmailEditText.setEnabled(false);
            mEmailEditText.setText(mPref.getRegisteredEmail());
            mButtonRegister.setText(getString(R.string.unregister));
        }
        else {
            mEmailEditText.setEnabled(true);
            mEmailEditText.setText(mPref.getRegisteredEmail());
            mButtonRegister.setText(getString(R.string.register));
        }
    }


    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        setButtonStates();
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        com.ds.avare.utils.Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);


        // do not compress buttons but pan
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mEmailEditText = (EditText) findViewById(R.id.edittext_register);
        mButtonRegister = (Button) findViewById(R.id.btn_register);
        /*
         * privacy policy load
         */
        mPrivacy = (WebView)findViewById(R.id.privacy_webview);
        mPrivacy.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getApplicationContext(), "privacy"));


        mPref = new Preferences(this);

        // Check if Internet present
        if (!Helper.isNetworkAvailable(this)) {

            DecoratedAlertDialogBuilder alertDialogBuilder = new DecoratedAlertDialogBuilder(RegisterActivity.this);
            alertDialogBuilder
                    .setTitle(getString(R.string.error))
                    .setMessage(getString(R.string.error_internet))
                    .setNeutralButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            if(!isFinishing()) {
                alertDialogBuilder.create().show();
            }

            return;
        }



        /*
         * Click event on Register button
         *
         */
        mButtonRegister.setText(getString(R.string.register));
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {

                if(mPref.isRegistered()) {

                    if(mRegisterTask != null) {
                        if(mRegisterTask.getStatus() != AsyncTask.Status.FINISHED) {
                            mRegisterTask.cancel(true);
                        }
                    }

                    Toast.makeText(RegisterActivity.this, getString(R.string.unregistering_server), Toast.LENGTH_LONG).show();

                    mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... vals) {

                            String serverUrl = NetworkHelper.getServer() + "unregister.php";
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("name", "anonymous");
                            params.put("email", mPref.getRegisteredEmail());
                            params.put("regId", "");
                            Random random = new Random();
                            long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);

                            // Once GCM returns a registration id, we need to register on our server
                            // As the server might be down, we will retry it a couple
                            // times.
                            for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                                try {
                                    NetworkHelper.post(serverUrl, params);
                                    return true;
                                }
                                catch (Exception e) {
                                }
                                // Here we are simplifying and retrying on any error; in a real
                                // application, it should retry only on unrecoverable errors
                                // (like HTTP error code 503).
                                if (i == MAX_ATTEMPTS) {
                                    break;
                                }
                                try {
                                    Thread.sleep(backoff);
                                }
                                catch (InterruptedException e1) {
                                    // Activity finished before we complete - exit.
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                backoff *= 2;
                            }
                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            if(result) {
                                mPref.setRegistered(false);
                                mPref.setRegisteredEmail(null);
                                Toast.makeText(RegisterActivity.this, getString(R.string.unregistered), Toast.LENGTH_LONG).show();
                            }
                            else {
                                Toast.makeText(RegisterActivity.this, getString(R.string.failed_unregister), Toast.LENGTH_LONG).show();
                            }
                            setButtonStates();
                        }

                    };
                    mRegisterTask.execute(null, null, null);
                }
                else {
                    final String email = mEmailEditText.getText().toString();
                    if(!isValidEmail(email)) {
                        Toast.makeText(RegisterActivity.this, getString(R.string.error_email), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(mRegisterTask != null) {
                        if(mRegisterTask.getStatus() != AsyncTask.Status.FINISHED) {
                            mRegisterTask.cancel(true);
                        }
                    }

                    Toast.makeText(RegisterActivity.this, getString(R.string.registering_server), Toast.LENGTH_LONG).show();

                    mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... vals) {
                            // Register on our server
                            // On server creates a new user
                            String serverUrl = NetworkHelper.getServer() + "register.php";
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("name", "anonymous");
                            params.put("email", email);
                            params.put("regId", "");
                            Random random = new Random();
                            long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                            // Once GCM returns a registration id, we need to register on our server
                            // As the server might be down, we will retry it a couple
                            // times.
                            for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                                try {
                                    NetworkHelper.post(serverUrl, params);
                                    return true;
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    // Here we are simplifying and retrying on any error; in a real
                                    // application, it should retry only on unrecoverable errors
                                    // (like HTTP error code 503).
                                    if (i == MAX_ATTEMPTS) {
                                        break;
                                    }
                                    try {
                                        Thread.sleep(backoff);
                                    }
                                    catch (InterruptedException e1) {
                                        // Activity finished before we complete - exit.
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                    // increase backoff exponentially
                                    backoff *= 2;
                                }
                            }
                            return false;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            if(result) {
                                mPref.setRegistered(true);
                                mPref.setRegisteredEmail(email);


                                DecoratedAlertDialogBuilder alertDialogBuilder = new DecoratedAlertDialogBuilder(RegisterActivity.this);
                                alertDialogBuilder
                                        .setTitle(getString(R.string.register))
                                        .setMessage(getString(R.string.registered))
                                        .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        });
                                if(!isFinishing()) {
                                    alertDialogBuilder.create().show();
                                }
                            }
                            else {
                                Toast.makeText(RegisterActivity.this, getString(R.string.failed_register), Toast.LENGTH_LONG).show();
                            }
                            setButtonStates();
                        }
                    };

                    mRegisterTask.execute(null, null, null);
                }

            }
        });

    }


    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}