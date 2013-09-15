/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.gdl90;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import com.ds.avare.gps.GpsInterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author zkhan
 *
 */
public class BlueToothConnection {

    private static BluetoothAdapter mBtAdapter = null;
    private static BluetoothSocket mBtSocket = null;
    private static InputStream mStream = null;
    private static boolean mRunning = false;
    private static GpsInterface mListener;
    
    private static BlueToothConnection mConnection;
    
    private static final String mStoreFile = null;
    private static final String mLoadFile = null;
    private static AdsbStatus mAdsbStatus;
    private static final String device = "XGPS170";
    
    /*
     *  Well known SPP UUID
     */
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 
     */
    private BlueToothConnection() {
    }

    
    /**
     * 
     * @return
     */
    public static BlueToothConnection getInstance() {

        if(null == mConnection) {
            mConnection = new BlueToothConnection();
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            mAdsbStatus = new AdsbStatus();
            mAdsbStatus.setState(AdsbStatus.DISCONNECTED);
        }
        return mConnection;
    }

    /**
     * 
     */
    private void sleepit() {
        try {
            Thread.sleep(1000);
        }
        catch(Exception e) {            
        }
    }
    
    /**
     * 
     */
    public void stop() {
        if(mAdsbStatus.getState() != AdsbStatus.CONNECTED) {
            return;
        }
        mRunning = false;
    }

    /**
     * 
     */
    public void registerListener(GpsInterface listener) {
        mListener = listener;
    }
    
    /**
     * 
     */
    public void start() {
        
        if(mAdsbStatus.getState() != AdsbStatus.DISCONNECTED) {
            return;
        }
        
        mRunning = true;
        
        /*
         * Thread that reads BT
         */
        Thread thread = new Thread() {
            @Override
            public void run() {
                
                byte[] buffer = new byte[32768];
                DataBuffer dbuffer = new DataBuffer(32768);
                Decode decode = new Decode();
                File file;
                BufferedOutputStream bos = null;
                BufferedInputStream bis = null;
                
                if(!connect(device)) {
                    return;
                }
                /*
                 * Store file for debug? 
                 */
                if(mStoreFile != null) {
                    try {
                        file = new File(mStoreFile);
                        if(!file.exists()){
                            file.createNewFile();
                        }
                        bos = new BufferedOutputStream(new FileOutputStream(file, true));                    
                    }
                    catch (Exception e) {
                    }
                }               
                if(mLoadFile != null) {
                    try {
                        file = new File(mLoadFile);
                        bis = new BufferedInputStream(new FileInputStream(file));                    
                    }
                    catch (Exception e) {
                    }
                }               
                
                /*
                 * This state machine will keep trying to connect to 
                 * ADBS receiver
                 */
                while(mRunning) {
                    
                    int red = 0;
                    
                    if(mLoadFile == null) {
                        /*
                         * Read.
                         */
                        red = read(buffer);
                        if(red <= 0) {
                            BlueToothConnection.this.stop();
                            continue;
                        }
                    }
                    else {
                        try {
                            red = bis.read(buffer, 0, buffer.length);
                        }
                        catch (Exception e) {
                            sleepit();
                            continue;
                        }
                        if(red <= 0) {
                            sleepit();
                            continue;
                        }
                    }
                    
                    dbuffer.put(buffer, red);
                 
                    /**
                     * Store data to file for debugging if set to debug
                     */
                    if(mStoreFile != null) {
                        try {
                            bos.write(buffer, 0, red);
                            bos.flush();
                        } 
                        catch (Exception e) {
                        }
                    }
                    
                    byte[] buf;
                    while(null != (buf = dbuffer.get())) {

                        /*
                         * Get packets, decode
                         */
                        com.ds.avare.gdl90.Message m = decode.decode(buf);
                        /*
                         * Post on UI thread.
                         */
                        Message msg = mHandler.obtainMessage();
                        msg.obj = m;
                        mHandler.sendMessage(msg);
                    }
                }

                if(mStoreFile != null) {
                    try {
                        bos.close();
                    } 
                    catch (Exception e) {
                    }
                }
                if(mLoadFile != null) {
                    try {
                        bis.close();
                    } 
                    catch (Exception e) {
                    }
                }
                
                /*
                 * Exit
                 */
                try {
                    mStream.close();
                } 
                catch(Exception e2) {
                }
                
                try {
                    mBtSocket.close();
                } 
                catch(Exception e2) {
                }    
                setState(AdsbStatus.DISCONNECTED);
            }
        };
        thread.start();
    }
    
    /**
     * 
     * @param state
     */
    private void setState(int state) {
        if(mListener != null) {
            mAdsbStatus.setState(state);
            Message msg = mHandler.obtainMessage();
            msg.obj = mAdsbStatus;
            mHandler.sendMessage(msg);
        }
    }
    
    /**
     * 
     * A device name devNameMatch, will connect to first device whose
     * name matched this string.
     * @return
     */
    private boolean connect(String devNameMatch) {
        /*
         * Only when not connected, connect
         */
        if(mAdsbStatus.getState() != AdsbStatus.DISCONNECTED) {
            return false;
        }
        setState(AdsbStatus.CONNECTING);
        if(null == mBtAdapter) {
            setState(AdsbStatus.DISCONNECTED);
            return false;
        }
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        /*
         * Find device
         */
        if(null == pairedDevices) {            
            setState(AdsbStatus.DISCONNECTED);
            return false;
        }
        BluetoothDevice device = null;
        for(BluetoothDevice bt : pairedDevices) {
           if(bt.getName().contains(devNameMatch)) {
               device = bt;
           }
        }
   
        /*
         * Stop discovery
         */
        mBtAdapter.cancelDiscovery();
 
        if(null == device) {
            setState(AdsbStatus.DISCONNECTED);
            return false;
        }
        
        /*
         * Make socket
         */
        try {
            mBtSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } 
        catch(Exception e) {
            setState(AdsbStatus.DISCONNECTED);
            return false;
        }
    
        /*
         * Establish the connection.  This will block until it connects.
         */
        try {
            mBtSocket.connect();
        } 
        catch(Exception e) {
            try {
                mBtSocket.close();
            } 
            catch(Exception e2) {
            }
            setState(AdsbStatus.DISCONNECTED);
            return false;
        } 

        try {
            mStream = mBtSocket.getInputStream();
        } 
        catch (Exception e) {
            try {
                mBtSocket.close();
            } 
            catch(Exception e2) {
            }
            setState(AdsbStatus.DISCONNECTED);
        } 

        setState(AdsbStatus.CONNECTED);

        return true;
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
        return red;
    }

    /**
     * 
     * @return
     */
    public boolean isConnected() {
        return mAdsbStatus.getState() == AdsbStatus.CONNECTED;
    }

    /**
     * Send a message to the lone listener.
     */
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {            
            if(mListener != null) {
                
                if(msg.obj instanceof UplinkMessage) {
                    /*
                     * Send an uplink nexrad message
                     */
                    LinkedList<Product> pds = ((UplinkMessage) msg.obj).getFis().getProducts();
                    for(Product p : pds) {
                        if(p instanceof Id6364Product) {
                            Id6364Product pn = (Id6364Product)p;
                            mListener.adbsMessageCallbackNexrad(pn);
                        }
                    }
                }
                else if(msg.obj instanceof OwnshipMessage) {
                    
                    /*
                     * Make a GPS locaiton message from ADSB ownship message.
                     */
                    OwnshipMessage om = (OwnshipMessage)msg.obj;
                    Location l = new Location(device);
                    l.setAltitude(om.mAltitude / 3.28);  // ft to m
                    l.setLatitude(om.mLat);
                    l.setLongitude(om.mLon);
                    l.setSpeed((float)(om.mHorizontalVelocity / 1.944)); // kt to ms/s
                    l.setBearing(om.mDirection);
                    l.setTime(om.getTime());
                    mListener.locationCallback(l);
                }
                else if(msg.obj instanceof AdsbStatus) {
                    mListener.timeoutCallback(false);
                    mListener.adbsStatusCallback(mAdsbStatus);
                }
            }
        }
    };

}
