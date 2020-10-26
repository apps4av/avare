/*-
 * SPDX-License-Identifier: BSD-2-Clause
 *
 * Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.ds.avare.utils.BTListPreferenceWithSummary;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author rwalker
 * A Bluetooth Output connection
 */
public class BTOutConnection extends Connection {
    private static final UUID mMyUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BTOutConnection mConnection;
    private static BluetoothSocket mBTSocket;
    private static BluetoothAdapter mBTAdapter;
    private static String mName = BTListPreferenceWithSummary.NONE;
    private static boolean mAutoReconnect;
    private static boolean mConnectInProgress;
    private static OutputStream mOutStream = null;

    // We are a singleton class.
    public static BTOutConnection getInstance(Context ctx) {
        if(null == mConnection) {
            mConnection = new BTOutConnection();
        }
        return mConnection;
    }

    // Private constructor only called from the getInstance() method
    private BTOutConnection() {
        super("Bluetooth Output");
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public List<String> getDevices() {
        return new ArrayList<>();
    }

    @Override
    public String getConnDevice() {
        return mName;
    }

    @Override
    public boolean connect(String to, boolean secure) {
        // If there is no bluetooth, there is nothing we can do
        if(null == mBTAdapter) {
            setState(DEAD); // Set to dead since there is no chance of re-opening
            return false;
        }

        // If the name we are to connect to is null or empty, then there is nothing
        // we can do
        if(null == to || to.length() == 0) {
            setState(DEAD); // Set to dead since there is no chance of re-opening
            return false;
        }

        // If we are already connected, do nothing
        if(isConnected()) {
            return true;
        }

        // Save off the device name to connect with. This will be used
        // to re-open the connection in the future if we have any troubles.
        mName = to;

        // Do the connection and set auto re-connect
        mAutoReconnect = true;
        doReconnect();

        return true;
    }

    @Override
    public void disconnect() {

        if(isConnected()) {

            // Close the stream
            if (null != mOutStream) {
                try {
                    mOutStream.close();
                } catch (Exception ignore) {

                }
                mOutStream = null;
            }

            // Now close the socket
            if (null != mBTSocket) {
                try {
                    mBTSocket.close();
                } catch (Exception ignore) {

                }
                mBTSocket = null;
            }
        }

        // We are fully closed
        setState(DISCONNECTED);

        // We were explicitly told to disconnect, so disable the reconnect logic
        mAutoReconnect = false;
    }

    // reconnect to the bluetooth stream with the device name we saved off
    private void reconnect() {

        // A couple flags to check before we blindly start this process
        if(mConnectInProgress || isConnected() ||
            null == mBTAdapter || null == mName ||
            mName.length() == 0) {
            return;
        }

        // Flag that we are in process
        mConnectInProgress = true;

        // Get the collection of bluetooth devices from the system
        BluetoothDevice btDevice = null;
        Set<BluetoothDevice> setDevices = mBTAdapter.getBondedDevices();

        // Search for a device with the desired name.
        for(BluetoothDevice btd : setDevices) {
            String btName = btd.getName();
            if(null != btName) {
                if (btName.equalsIgnoreCase(mName)) {
                    btDevice = btd;
                }
            }
        }

        // tell bluetooth to stop scanning. it's a power issue
        mBTAdapter.cancelDiscovery();

        if(null == btDevice) {
            // If we did not find it, then time to leave after indicating that we don't want to
            // auto reconnect
            mAutoReconnect = false;
            mConnectInProgress = false;
            setState(DEAD); // Set to dead since there is no chance of re-opening
            return;
        }

        // create a socket to communicate over.
        try { mBTSocket = btDevice.createRfcommSocketToServiceRecord(mMyUUID);
            // we got the socket, now connect it.
            try { mBTSocket.connect();
                // connection OK, now get the output stream
                try { mOutStream = mBTSocket.getOutputStream();
                } catch (Exception ex) {
                    // Failed to get the output stream
                    mBTSocket.close();
                    mConnectInProgress = false;
                    return;
                }
            } catch (Exception ex) {
                // socket failed to connect
                mBTSocket.close();
                mConnectInProgress = false;
                return;
            }
        } catch (Exception ex) {
            // failed to create socket
            mConnectInProgress = false;
            return;
        }

        // Set the normal running flags
        setState(CONNECTED);
        mConnectInProgress = false;
    }

    // Write this chunk of data to the connection. If we have a failure, then start the
    // reconnection sequence.
    @Override
    public void write(byte[] aData) {
        if(isConnected()) {
            // We are connected, attempt to write the data
            try {
                mOutStream.write(aData, 0, aData.length);
            } catch (Exception ex) {
                // The write failed. Clean up the connection and attempt to reopen it
                disconnect();
                mAutoReconnect = true;  // re-enable the auto-reconnect flag
                doReconnect();            }
        } else {
            // We are not connected. Attempt reconnect
            doReconnect();
        }
    }

    // Create a thread to call the reconnect logic
    private void doReconnect() {
        if(mAutoReconnect) {
            new Thread(new Runnable() {
                public void run() {
                    reconnect();
                }
            }).start();
        }
    }
}
