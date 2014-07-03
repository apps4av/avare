/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare;

import com.ds.avare.R;
import com.ds.avare.message.Logger;
import com.ds.avare.storage.Preferences;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    

    // Register button
    private Button mButtonRegister;
    private Button mButtonUnregister;
    private AlertDialog mRegisterDialog;
    private AlertDialog mUnregisterDialog;
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
                    
                    mPref.setRegistered(true);
                    Logger.clear();
                    Logger.Logit(getString(R.string.registered));                    
                    
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

                    mPref.setRegistered(false);
                    Logger.Logit(getString(R.string.unregistered));
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

                if(mPref.isRegistered()) {
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

                if(!mPref.isRegistered()) {
                    Logger.Logit(getString(R.string.already_unregistered));
                    return;
                }
                
                // show it
                mUnregisterDialog.show();
           }
        });        
    }
 
    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregister(final Context context, final String regId) {

        Logger.Logit(context.getString(R.string.unregistering_server));
        
   }
    
}