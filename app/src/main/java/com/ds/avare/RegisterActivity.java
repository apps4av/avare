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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.message.Helper;
import com.ds.avare.message.Logger;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.PossibleEmail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Button mButtonUnregister;
    private AlertDialog mRegisterDialog;
    private AlertDialog mUnregisterDialog;
    private Spinner mEmailSpinner;
    private WebView mPrivacy;
    private Preferences mPref;

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    /**
     * Before continuing, ask for permissions
     */
    private void askForPermissions() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsNeeded.add(getString(R.string.RationaleLocation));
        }
        if (!addPermission(permissionsList, Manifest.permission.GET_ACCOUNTS)) {
            permissionsNeeded.add(getString(R.string.RationaleAccount));
        }
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionsNeeded.add(getString(R.string.RationaleExternalStorage));
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = getString(R.string.GrantPermissions);
                for (int i = 0; i < permissionsNeeded.size(); i++) {
                    message = message + "\n(" + (i + 1) + ") " + permissionsNeeded.get(i);
                }
                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.Proceed),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                    }
                                }
                        )
                        .setNegativeButton(getString(R.string.No),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(RegisterActivity.this, getString(R.string.NoRationale), Toast.LENGTH_LONG).show();
                                    }
                                }
                        )
                        .create()
                        .show();
            }
        }
    }

    /**
     *
     * @param permissionsList
     * @param permission
     * @return
     */
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            return false;
        }
        return true;
    }

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.GET_ACCOUNTS, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                }
                else {
                    // Permission Denied
                    Toast.makeText(this, getString(R.string.NoRationale), Toast.LENGTH_LONG)
                            .show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /**
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+
            askForPermissions();
        }
        else {
            // Pre-Marshmallow
            /*
             * Start service now, bind later. This will be no-op if service is already running
             */
        }
    }

    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        com.ds.avare.utils.Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        mPref = new Preferences(this);

        // Check if Internet present
        if (!Helper.isNetworkAvailable(this)) {
            Helper.showAlert(RegisterActivity.this,
                    getString(R.string.error),
                    getString(R.string.error_internet));
            return;
        }

        // Check if email
        if(PossibleEmail.get(this) == null) {
            Helper.showAlert(RegisterActivity.this,
                    getString(R.string.error),
                    getString(R.string.error_email));
            return;            
        }
 
        Logger.setTextView((TextView) findViewById(R.id.log_text));
        if(!mPref.isRegistered()) {
            Logger.Logit(getString(R.string.register_msg));            
        }      

        /*
         * privacy policy load
         */
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPrivacy = (WebView)layoutInflater.inflate(R.layout.privacy, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
            .setTitle(getString(R.string.register))
            .setView(mPrivacy)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    dialog.dismiss();
                }
            })
            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    
                    if(mRegisterTask != null) {
                        if(mRegisterTask.getStatus() != AsyncTask.Status.FINISHED) {
                            mRegisterTask.cancel(true);
                        }
                    }
                    
                    final String email = mEmailSpinner.getSelectedItem().toString();
                    Logger.Logit(getString(R.string.registering_server));

                    mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... vals) {
                            // Register on our server
                            // On server creates a new user
                            String serverUrl = NetworkHelper.getServer() + "register.php";
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("name", "anonoymous");
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
                                Logger.clear();
                                Logger.Logit(getString(R.string.registered));
                                setChoices();
                            }
                            else {
                                Logger.Logit(getString(R.string.failed_register));                    
                            }
                         }
                    };
                    
                    mRegisterTask.execute(null, null, null);
                    
                    dialog.dismiss();
                }
            });
        
        mRegisterDialog = alertDialogBuilder.create();

        alertDialogBuilder = new AlertDialog.Builder(
                RegisterActivity.this);
        alertDialogBuilder
            .setTitle(getString(R.string.unregister))
            .setCancelable(false)
            .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    dialog.dismiss();
                }
            })
            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {

                    
                    // Get GCM registration id
                    if(mRegisterTask != null) {
                        if(mRegisterTask.getStatus() != AsyncTask.Status.FINISHED) {
                            mRegisterTask.cancel(true);
                        }
                    }
                    
                    mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... vals) {
                            
                            String serverUrl = NetworkHelper.getServer() + "unregister.php";
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("name", "anonoymous");
                            params.put("email", PossibleEmail.get(getApplicationContext()));
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
                                Logger.Logit(getString(R.string.unregistered));
                                setChoices();
                            }
                            else {
                                Logger.Logit(getString(R.string.failed_unregister));                    
                            }
                        }

                    };
                    mRegisterTask.execute(null, null, null);
             
                    dialog.dismiss();
                }
            });
        
        mUnregisterDialog = alertDialogBuilder.create();

        /*
         * Email select
         */

        /*
         * Click event on Register button
         *
         */
        mButtonRegister = (Button) findViewById(R.id.btn_register);   
        mButtonRegister.setText(getString(R.string.register));
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {

                Logger.clear();

                if(mPref.isRegistered()) {
                    Logger.Logit(getString(R.string.already_registered));
                    return;
                }
                
                mPrivacy.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getApplicationContext(), "privacy"));
                mPrivacy.setWebViewClient(new WebViewClient() {
                    // wait for page to load
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // show it
                        mRegisterDialog.show();
                    }
                 });
            }
        });
        
        
        mButtonUnregister = (Button) findViewById(R.id.btn_unregister);        
        mButtonUnregister.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {
                Logger.clear();

                if(!mPref.isRegistered()) {
                    Logger.Logit(getString(R.string.already_unregistered));
                    return;
                }
                
                // show it
                mUnregisterDialog.show();
           }
        });        
        

        mEmailSpinner = (Spinner)findViewById(R.id.spinner_register);
        setChoices();
    }

    
    /**
     * Set email choices
     */
    private void setChoices() {
        String emails[];
        if(mPref.isRegistered()) {
        	// If already registered, show the registered email
            emails = new String[1];
            emails[0] = PossibleEmail.get(this);
        }
        else {
        	// If trying to register, show all possible emails
        	emails = PossibleEmail.getAll(this);
        }
        if(emails != null) {
	        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
	        		android.R.layout.simple_expandable_list_item_1, emails);
	        mEmailSpinner.setAdapter(adapter);
        }
    }
    
    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregister(final Context context, final String regId) {

        Logger.Logit(context.getString(R.string.unregistering_server));
        
   }
    
}