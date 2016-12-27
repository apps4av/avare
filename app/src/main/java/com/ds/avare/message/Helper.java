/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/
package com.ds.avare.message;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;

import com.ds.avare.utils.DecoratedAlertDialogBuilder;

/**
 * 
 * @author zkhan
 *
 */
public class Helper {

    /**
     * 
     * @param context
     * @param title
     * @param message
     */
    public static void showAlert(Context context, String title, String message) {
        DecoratedAlertDialogBuilder alertDialogBuilder = new DecoratedAlertDialogBuilder(
                context);
        alertDialogBuilder
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, close
                    // current activity
                    dialog.dismiss();
                }
              });
        
        AlertDialog alertDialog = alertDialogBuilder.create();
        
        // show it
        alertDialog.show();
    }
    
    /**
     * 
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
