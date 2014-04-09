/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ds.avare.R;
import com.ds.avare.message.NetworkHelper;

import com.google.android.gcm.GCMBaseIntentService;
 
/**
 * 
 * @author zkhan
 *
 */
public class GCMIntentService extends GCMBaseIntentService {
 
    /**
     * 
     */
    public GCMIntentService() {
        super(NetworkHelper.getSenderID());
    }
 
    /**
     * Method called on device registered
     */
    @Override
    protected void onRegistered(Context context, String registrationId) {
        RegisterActivity.register(context, registrationId);
    }
 
    /**
     * Method called on device un registred
     */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
        RegisterActivity.unregister(context, registrationId);
    }
 
    /**
     * Method called on Receiving a new message
     *
     */
    @Override
    protected void onMessage(Context context, Intent intent) {
        String message = intent.getExtras().getString("message");
        // notifies user
        generateNotification(context, message);
    }
 
    /**
     * Method called on receiving a deleted message
     * 
     */
    @Override
    protected void onDeletedMessages(Context context, int total) {
    }
 
    /**
     * Method called on Error
     *
     */
    @Override
    public void onError(Context context, String errorId) {
    }
 
    @Override
    /**
     * 
     */
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        return super.onRecoverableError(context, errorId);
    }
 
    /**
     * Issues a notification to inform the user that server has sent a message.
     * @param context
     * @param message
     */
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.notification;
        // prepare intent which is triggered if the
        // notification is selected
        
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);

        /*
         * Alert!
         * Make intent to MainActitivty, and send info about this
         */
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra("message", message);
 
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                .setSmallIcon(icon)
                .setContentTitle(context.getString(R.string.app_name))
                .setAutoCancel(true)
                .setContentIntent(pIntent)
                .setContentText(context.getString(R.string.new_message));
            
        Random r = new Random();
        Notification n = builder.build();
        notificationManager.notify(r.nextInt(), n);

    }
 
}