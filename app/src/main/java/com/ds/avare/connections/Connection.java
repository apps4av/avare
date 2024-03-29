/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ds.avare.connections;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

import java.io.FileOutputStream;
import java.util.List;

/**
 * 
 * @author zkhan
 *
 */
public abstract class Connection {

    protected static final int CONNECTED = 1;
    protected static final int CONNECTING = 2;
    protected static final int DISCONNECTED = 0;
    protected static final int DEAD = -1;

    private String mName = "";

    protected byte[] buffer = new byte[1024];


    private int mState;

    private boolean mRunning;

    protected StorageService mService;

    private Thread mThread;

    private String mFileSave = null;

    private GenericCallback mCb;

    /**
     *
     */
    public Connection(String name) {
        mState = DISCONNECTED;
        mRunning = false;
        mName = name;
        mService = StorageService.getInstance();
    }

    protected void setCallback(GenericCallback cb) {
        mCb = cb;
    }

    /**
     * @param file
     */
    public void setFileSave(String file) {
        synchronized (this) {
            mFileSave = file;
        }
    }

    /**
     * Save data from connection to file
     *
     * @param red
     * @param buffer
     */
    protected void saveToFile(int red, byte[] buffer) {
        if (red > 0) {
            String file = null;
            synchronized (this) {
                file = mFileSave;
            }
            if (file != null) {
                try {
                    FileOutputStream output = new FileOutputStream(file, true);
                    output.write(buffer, 0, red);
                    output.close();
                } catch (Exception e) {
                }
            }
        }
    }


    /**
     * @return
     */
    protected int getState() {
        return mState;
    }

    /**
     * @param state
     */
    protected void setState(int state) {
        mState = state;
    }

    /**
     * @return
     */
    public boolean isConnected() {
        return getState() == Connection.CONNECTED;
    }
    public boolean isConnecting() {
        return getState() == Connection.CONNECTING;
    }

    public boolean isDead() {
        return getState() == Connection.DEAD;
    }


    /**
     *
     */
    public void stop() {
        Logger.Logit("Stopping " + mName);
        if (getState() != Connection.CONNECTED) {
            Logger.Logit(mName + ": Stop failed because already stopped");
            return;
        }
        mRunning = false;
        if (null != mThread) {
            mThread.interrupt();
        }
        Logger.Logit("Stopped!");
    }


    /**
     *
     */
    public void start() {
        Logger.Logit("Starting " + mName);
        if (getState() != Connection.CONNECTED) {
            Logger.Logit(mName + ": Starting failed because already started");
            return;
        }

        /*
         * Thread that reads Wifi/BT etc.
         */
        mThread = new Thread() {
            @Override
            public void run() {
                mRunning = true;
                mCb.callback((Object) mService.getPreferences(), null);
            }
        };
        mThread.start();
        Logger.Logit("Started!");
    }

    protected boolean isRunning() {
        return mRunning;
    }

    protected boolean isStopped() {
        return !mRunning;
    }

    /**
     * @param s
     */
    protected void sendDataToHelper(String s) {
        Message m = mHandler.obtainMessage();
        m.obj = s;
        mHandler.sendMessage(m);
    }

    /**
     *
     */
    protected String getDataFromHelper() {
        String data = null;
        try {
            data = mService.makeDataForIO();
            Logger.Logit(data);
        } catch (Exception ignored) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored1) {

            }
        }
        return data;
    }

    public void disconnectConnection() {
        setState(Connection.DISCONNECTED);
        Logger.Logit(mName + " :Disconnected");
    }


    public boolean connectConnection() {
        setState(Connection.CONNECTED);
        Logger.Logit(mName + " :Connected");
        return true;
    }


    public abstract List<String> getDevices();

    public abstract String getConnDevice();

    public abstract void disconnect();

    public abstract boolean connect(String param, boolean securely);

    public abstract void write(byte[] aData);

    /**
     * Posting a location hence do from UI thread
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {

            String text = (String) msg.obj;

            if (text == null) {
                return;
            }

            try {
                mService.getDataFromIO(text);
            }
            catch (Exception e) {

            }
        }
    };
}
