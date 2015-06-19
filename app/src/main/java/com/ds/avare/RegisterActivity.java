/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare;

import com.ds.avare.R;
import com.ds.avare.message.Helper;
import com.ds.avare.message.Logger;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.PossibleEmail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
    private Spinner mEmailSpinner;
    private WebView mPrivacy;
    private Preferences mPref;
    
    
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