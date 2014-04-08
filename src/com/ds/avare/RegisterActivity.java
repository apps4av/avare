/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare;

import com.ds.avare.R;
import com.ds.avare.message.Helper;
import com.ds.avare.message.Logger;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.utils.PossibleEmail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
 
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
    private WebView mPrivacy;
    
    
    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        com.ds.avare.utils.Helper.setTheme(this);
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_register);
        
        // Check if Google services
        try {
            GCMRegistrar.checkDevice(this);
        }
        catch (Exception e) {
            Helper.showAlert(RegisterActivity.this,
                    getString(R.string.error),
                    getString(R.string.error_google));
            return;            
        }
        
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
        if(!Helper.isRegistered(this)) {
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
                    
                    // Get GCM registration id
                    if(mRegisterTask != null) {
                        if(mRegisterTask.getStatus() != AsyncTask.Status.FINISHED) {
                            mRegisterTask.cancel(true);
                        }
                    }
                    
                    Logger.Logit(getString(R.string.registering_google));
                    GCMRegistrar.register(RegisterActivity.this, NetworkHelper.getSenderID());
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
                    
                    Logger.Logit(getString(R.string.unregistering_google));
                    GCMRegistrar.unregister(RegisterActivity.this);
                    dialog.dismiss();
                }
            });
        
        mUnregisterDialog = alertDialogBuilder.create();

        /*
         * Click event on Register button
         *
         */
        mButtonRegister = (Button) findViewById(R.id.btn_register);        
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {

                Logger.clear();
                
                if(!GCMRegistrar.getRegistrationId(RegisterActivity.this).equals("")) {
                    Logger.Logit(getString(R.string.already_registered));
                    return;
                }
                
                mPrivacy.loadUrl("file:///android_asset/privacy.html");
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
                
                if(GCMRegistrar.getRegistrationId(RegisterActivity.this).equals("")) {
                    Logger.Logit(getString(R.string.already_unregistered));
                    return;
                }
                                
                // show it
                mUnregisterDialog.show();
           }
        });        
    }
 
    /**
     * Register this account/device pair within the server.
     *
     */
    public static void register(final Context context, final String regId) {

        Logger.Logit(context.getString(R.string.registering_server));

        mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... vals) {
                // Register on our server
                // On server creates a new user
                String serverUrl = NetworkHelper.getServer() + "register.php";
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", "anonoymous");
                params.put("email", PossibleEmail.get(context));
                params.put("regId", regId);
                Random random = new Random();
                long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                // Once GCM returns a registration id, we need to register on our server
                // As the server might be down, we will retry it a couple
                // times.
                for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                    try {
                        NetworkHelper.post(serverUrl, params);
                        GCMRegistrar.setRegisteredOnServer(context, true);
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
                    Logger.clear();
                    Logger.Logit(context.getString(R.string.registered));                    
                }
                else {
                    Logger.Logit(context.getString(R.string.failed_register));                    
                }
             }
        };
        
        mRegisterTask.execute(null, null, null);
    }
 
    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregister(final Context context, final String regId) {

        Logger.Logit(context.getString(R.string.unregistering_server));
        
        mRegisterTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... vals) {
                
                String serverUrl = NetworkHelper.getServer() + "unregister.php";
                Map<String, String> params = new HashMap<String, String>();
                params.put("regId", regId);
                Random random = new Random();
                long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                                
                // Once GCM returns a registration id, we need to register on our server
                // As the server might be down, we will retry it a couple
                // times.
                for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                    try {
                        NetworkHelper.post(serverUrl, params);
                        GCMRegistrar.setRegisteredOnServer(context, false);
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
                    Logger.Logit(context.getString(R.string.unregistered));
                }
                else {
                    Logger.Logit(context.getString(R.string.failed_unregister));                    
                }
            }

        };
        mRegisterTask.execute(null, null, null);
    }
    
}