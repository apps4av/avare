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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

/**
 * This class exposes the remote service to the client.
 * The client will be the Avare Helper, sending data to Avare
 * author zkhan
 */
public class IHelperService extends Service {

    private StorageService mService;
    
    /**
     * We need to bind to storage service to do anything useful 
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            /* 
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder)service;
            mService = binder.getService();
        }    

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate() {       
        mService = null;
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        getApplicationContext().unbindService(mConnection);
        mService = null;
    }

    /**
     * 
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    /**
     * 
     */
    private final IHelper.Stub mBinder = new IHelper.Stub() {
        @Override
        public void sendDataText(String text) {
            
            /*
             * This is where we are all messages
             * All messages are comma separated
             */
            Message msg = mHandler.obtainMessage();
            msg.obj = text;
            mHandler.sendMessage(msg);
        }
    };
    
    /**
     * Posting a location hence do from UI thread
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {            

            String text = (String)msg.obj;
            
            if(text == null || mService == null) {
                return;
            }
            
            String tokens[] = text.split(",");
            if(tokens[0].equals("ownship")) {
                Location l = new Location(LocationManager.GPS_PROVIDER);
                l.setLongitude(Double.parseDouble(tokens[1]));
                l.setLatitude(Double.parseDouble(tokens[2]));
                l.setSpeed((float)Double.parseDouble(tokens[3]));
                l.setBearing((float)Double.parseDouble(tokens[4]));
                l.setAltitude(Double.parseDouble(tokens[5]));
                l.setTime(Long.parseLong(tokens[6]));
                mService.getGps().onLocationChanged(l);
            }
        }
    };
}