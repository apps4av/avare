/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.connections;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 
 * @author zkhan
 *
 */
public class BlueToothConnectionIn extends Connection {

    private static BlueToothConnectionIn mConnection;

    private BluetoothSocket mBtSocket = null;
    private InputStream mStream = null;
    private boolean mSecure = true;
    private BluetoothAdapter mBtAdapter = null;
    private String mDevName;

    /*
     *  Well known SPP UUID
     */
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 
     */
    private BlueToothConnectionIn() {
        super("Bluetooth Input");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                BufferProcessor bp = new BufferProcessor();

                byte[] buffer = new byte[8192];

                /*
                 * This state machine will keep trying to connect to
                 * ADBS/GPS receiver
                 */
                while (isRunning()) {

                    int red = 0;

                    /*
                     * Read.
                     */
                    red = read(buffer);
                    if (red <= 0) {
                        if (isStopped()) {
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {

                        }

                        /*
                         * Try to reconnect
                         */
                        Logger.Logit("Disconnected from BT device, retrying to connect");

                        disconnect();
                        connect(mDevName, mSecure);
                        continue;
                    }

                    /*
                     * Put both in Decode and ADBS buffers
                     */
                    bp.put(buffer, red);
                    LinkedList<String> objs = bp.decode((Preferences)o);
                    for (String s : objs) {
                        sendDataToHelper(s);
                    }
                }
                return null;
            }
        });
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 
     * @return
     * @param ctx
     */
    public static BlueToothConnectionIn getInstance(Context ctx) {

        if(null == mConnection) {
            mConnection = new BlueToothConnectionIn();
        }
        return mConnection;
    }


    /**
     * 
     * A device name devNameMatch, will connect to first device whose
     * name matched this string.
     * @return
     */
    public boolean connect(String devNameMatch, boolean secure) {
        
        if(devNameMatch == null) {
            return false;
        }
        
        mDevName = devNameMatch;
        mSecure = secure;
        
        /*
         * Only when not connected, connect
         */
        if(getState() != Connection.DISCONNECTED) {
            Logger.Logit("Failed! Already connected?");

            return false;
        }
        setState(Connection.CONNECTING);
        if(null == mBtAdapter) {
            Logger.Logit("Failed! BT adapter not found");

            setState(Connection.DISCONNECTED);
            return false;
        }
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        /*
         * Find device
         */
        if(null == pairedDevices) {            
            Logger.Logit("Failed! No paired devices");
            setState(Connection.DISCONNECTED);
            return false;
        }
        
        Logger.Logit("BT finding devices");

        BluetoothDevice device = null;
        for(BluetoothDevice bt : pairedDevices) {
           if(bt.getName().equals(devNameMatch)) {
               device = bt;
           }
        }
   
        /*
         * Stop discovery
         */
        mBtAdapter.cancelDiscovery();
 
        if(null == device) {
            Logger.Logit("Failed! No such device");

            setState(Connection.DISCONNECTED);
            return false;
        }
        
        /*
         * Make socket
         */
        Logger.Logit("Finding socket for SPP secure = " + mSecure);

        if(secure) {
            try {
                mBtSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } 
            catch(Exception e) {
                Logger.Logit("Failed! secure SPP socket failed");
                
                setState(Connection.DISCONNECTED);
                return false;
            }
        }
        else {
            try {
                mBtSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } 
            catch(Exception e) {
                Logger.Logit("Failed! insecure SPP socket failed");

                setState(Connection.DISCONNECTED);
                return false;
            }
        }
    
        /*
         * Establish the connection.  This will block until it connects.
         */
        Logger.Logit("Connecting socket");

        try {
            mBtSocket.connect();
        } 
        catch(Exception e) {
            try {
                mBtSocket.close();
            } 
            catch(Exception e2) {
            }
            Logger.Logit("Failed! Socket connection error");

            setState(Connection.DISCONNECTED);
            return false;
        } 

        Logger.Logit("Getting input stream");

        try {
            mStream = mBtSocket.getInputStream();
        } 
        catch (Exception e) {
            try {
                mBtSocket.close();
            } 
            catch(Exception e2) {
            }
            Logger.Logit("Failed! Input stream error");

            setState(Connection.DISCONNECTED);
        }

        return connectConnection();
    }

    @Override
    public void write(byte[] aData) {

    }

    /**
     * 
     */
    public void disconnect() {

        /*
         * Exit
         */
        try {
            mStream.close();
        } 
        catch(Exception e2) {
            Logger.Logit("Error stream close");
        }
        
        try {
            mBtSocket.close();
        } 
        catch(Exception e2) {
            Logger.Logit("Error socket close");
        }

        disconnectConnection();
    }
    
    /**
     * 
     * @return
     */
    private int read(byte[] buffer) {
        int red = -1;
        try {
            red = mStream.read(buffer, 0, buffer.length);
        } 
        catch(Exception e) {
            red = -1;
        }

        saveToFile(red, buffer);
        return red;
    }

    /**
     *
     * @return
     */
    @Override
    public List<String> getDevices() {
        List<String> list = new ArrayList<String>();
        if(null == mBtAdapter) {
            return list;
        }
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        /*
         * Find devices
         */
        if(null == pairedDevices) {
            return list;
        }
        for(BluetoothDevice bt : pairedDevices) {
            list.add((String)bt.getName());
        }

        return list;
    }


    /**
     * 
     * @return
     */
    @Override
    public String getConnDevice() {
        return mDevName;
    }
    
}
