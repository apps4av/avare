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
import android.view.Window;
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
public class RegisterActivity extends BaseActivity {
    
    private static final int MAX_ATTEMPTS = 3;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    
    static AsyncTask<Void, Void, Boolean> mRegisterTask = null;

    // Register button
    private Button mButtonRegister;
    private WebView mPrivacy;


    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        super.onBackPressedExit();
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);


        // do not compress buttons but pan
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mButtonRegister = (Button) findViewById(R.id.btn_register);
        /*
         * privacy policy load
         */
        mPrivacy = (WebView)findViewById(R.id.privacy_webview);
        mPrivacy.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getApplicationContext(), "privacy"));

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
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {

                mPref.setRegistered(true);

                Toast.makeText(RegisterActivity.this, getString(R.string.registered), Toast.LENGTH_LONG).show();
            }
        });

    }

}