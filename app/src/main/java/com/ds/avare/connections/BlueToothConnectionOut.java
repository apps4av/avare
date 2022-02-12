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

import com.ds.avare.nmea.BODPacket;
import com.ds.avare.nmea.GGAPacket;
import com.ds.avare.nmea.RMBPacket;
import com.ds.avare.nmea.RMCPacket;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 
 * @author zkhan
 *
 */
public class BlueToothConnectionOut extends Connection {

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private OutputStream mStream = null;
    private boolean mSecure = true;
    
    private static BlueToothConnectionOut mConnection;
    
    private String mDevName;

    /*
     *  Well known SPP UUID
     */
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 
     */
    private BlueToothConnectionOut() {
        super("Bluetooth Output");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                /*
                 * This state machine will keep trying to connect to
                 * ADBS/GPS receiver
                 */
                while(isRunning()) {

                    /*
                     * Read data from Avare
                     */
                    String recvd = getDataFromHelper();

                    if(null == recvd) {
                        continue;
                    }

                    /*
                     * Send to BT
                     */
                    byte buffer[] = null;
                    byte buffer2[] = null;
                    byte buffer3[] = null;
                    byte buffer4[] = null;
                    try {
                        JSONObject object;
                        object = new JSONObject(recvd);
                        Logger.Logit("sending message to BT " + mDevName + " " +
                                object.toString());
                        String type = object.getString("type");
                        if(type == null) {
                            continue;
                        }
                        if(type.equals("ownship")) {

                            RMCPacket pkt = new RMCPacket(object.getLong("time"),
                                    object.getDouble("latitude"),
                                    object.getDouble("longitude"),
                                    object.getDouble("speed"),
                                    object.getDouble("bearing"),
                                    0); // Ron, please fix this
                            buffer = pkt.getPacket().getBytes();
                            GGAPacket pkt2 = new GGAPacket(object.getLong("time"),
                                    object.getDouble("latitude"),
                                    object.getDouble("longitude"),
                                    object.getDouble("altitude"),
                                    0,
                                    0,0
                            );  // Ron, please fix this
                            buffer2 = pkt2.getPacket().getBytes();
                            RMBPacket pkt3 = new RMBPacket(
                                    object.getDouble("destDistance"),
                                    object.getDouble("destBearing"),
                                    object.getDouble("destLongitude"),
                                    object.getDouble("destLatitude"),
                                    object.getString("destId"),
                                    object.getString("destOriginId"),
                                    object.getDouble("destDeviation"),
                                    object.getDouble("speed"),
                                    true); // Ron, please fix this
                            buffer3 = pkt3.getPacket().getBytes();
                            BODPacket pkt4 = new BODPacket(object.getString("destId"),
                                    object.getString("destOriginId"),
                                    object.getDouble("bearingTrue"),
                                    object.getDouble("bearingMagnetic"));
                            buffer4 = pkt4.getPacket().getBytes();
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    if(null == buffer || null == buffer2) {
                        continue;
                    }

                    /*
                     * Make NMEA messages.
                     */

                    /*
                     * Write.
                     */
                    int wrote = writeToStream(buffer);
                    if(wrote <= 0) {
                        if(isStopped()) {
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

                    wrote = writeToStream(buffer2);
                    if(wrote <= 0) {
                        if(isStopped()) {
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

                    // RMB
                    if(buffer3 == null) {
                        continue;
                    }
                    if(buffer3.length <= 0) {
                        continue;
                    }
                    wrote = writeToStream(buffer3);
                    if(wrote <= 0) {
                        if(isStopped()) {
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

                    // BOD
                    if(buffer4 == null) {
                        continue;
                    }
                    if(buffer4.length <= 0) {
                        continue;
                    }
                    wrote = writeToStream(buffer4);
                    if(wrote <= 0) {
                        if(isStopped()) {
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
    public static BlueToothConnectionOut getInstance(Context ctx) {

        if(null == mConnection) {
            mConnection = new BlueToothConnectionOut();
        }
        return mConnection;
    }

    /**
     * 
     * A device name devNameMatch, will connect to first device whose
     * name matched this string.
     * @return
     */
    @Override
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
                Logger.Logit("Failed! SPP secure socket failed");
    
                setState(Connection.DISCONNECTED);
                return false;
            }
        }
        else {
            try {
                mBtSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } 
            catch(Exception e1) {
                Logger.Logit("Failed! SPP insecure socket failed");
                
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
            mStream = mBtSocket.getOutputStream();
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
    @Override
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
    private int writeToStream(byte[] buffer) {
        int wrote = buffer.length;
        try {
            mStream.write(buffer, 0, buffer.length);
        } 
        catch(Exception e) {
            wrote = -1;
        }
        return wrote;
    }

    /**
     *
     * @return
     */
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
