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
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author zkhan
 *
 */
public class USBConnectionOut extends Connection {

    private static USBConnectionOut mConnection;
    private static UsbManager mUsbManager;
    private String mParams;
    private static UsbDeviceConnection mDriverConnection;
    private static UsbSerialPort mPort;
    private UsbSerialDriver mDriver = null;
    /**
     *
     */
    private USBConnectionOut() {
        super("USB Output");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {

                /*
                 * This state machine will keep trying to connect to
                 * ADBS/GPS receiver
                 */
                int msgCount = 1;
                while(isRunning()) {

                    /*
                     * Read data from Avare
                     */
                    String r = getDataFromHelper();
                    if (null == r) {
                        continue;
                    }

                    // Build the JSON object from the received string. Contained therein
                    // is a collection of NMEA sentences that drives the autopilot. Send that data
                    // over the connection
                    try {
                        JSONObject object;
                        object = new JSONObject(r);

                        // Read the type of this object. We are only interested in NMEANAV
                        // messages
                        String type = object.getString("type");
                        if (type.equals("nmeanav")) {

                            // Log our intent to the trace window. The data itself was logged in the base object
                            Logger.Logit(msgCount++ + ". Sending sentences to USB out");

                            // Extract the autopilot sentences from the message
                            String apData = object.getString("apsentences");
                            if(0 != apData.length()) {
                                int wrote = writeToStream(apData.getBytes());
                                if (wrote <= 0) {
                                    if (isStopped()) {
                                        break;
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception ignore) {

                                    }

                                    /*
                                     * Try to reconnect
                                     */
                                    Logger.Logit("Disconnected from USB out, retrying to connect");
                                    disconnect();
                                    connect(mParams, false);
                                }
                            }
                        }
                    } catch(Exception ignore) { }
                }
                return null;
            }
        });
    }

    /**
     * 
     * @return
     */
    public static USBConnectionOut getInstance(Context ctx) {
        if(null == mConnection) {
            mConnection = new USBConnectionOut();
            mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
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
    public boolean connect(String params, boolean secure) {
        
        mParams = params;
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        if (availableDrivers.isEmpty()) {
            Logger.Logit("No USB serial device available");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        mDriver = availableDrivers.get(0);
        if(mDriver == null) {
            Logger.Logit("No USB serial device available");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        
        
        /*
         * Only when not connected, connect
         */
        if(getState() != Connection.DISCONNECTED) {
            Logger.Logit("Failed! Already connected?");

            return false;
        }
        setState(Connection.CONNECTING);

        try {

            mDriverConnection = mUsbManager.openDevice(mDriver.getDevice());
            if (mDriverConnection == null) {
                Logger.Logit("Failed!");

                return false;
            }

            mPort = mDriver.getPorts().get(0);
            mPort.open(mDriverConnection);

            String tokens[] = mParams.split(",");
            // 115200, 8, n, 1
            // rate, data, parity, stop
            int rate = Integer.parseInt(tokens[0]);
            int data = Integer.parseInt(tokens[1]);
            int parity;
            if(tokens[2].equals("n")) {
                parity = UsbSerialPort.PARITY_NONE;
            }
            else if (tokens[2].equals("o")) {
                parity = UsbSerialPort.PARITY_ODD;
            }
            else {
                parity = UsbSerialPort.PARITY_EVEN;
            }
            int stop = Integer.parseInt(tokens[3]);
            mPort.setParameters(rate, data, stop, parity);
        } 
        catch (Exception e) {
            setState(Connection.DISCONNECTED);
            Logger.Logit("Failed!");
            return false;
        } 

        return connectConnection();
    }

    @Override
    public void write(byte[] aData) {

    }

    /**
     *
     * @return
     */
    private int writeToStream(byte[] buffer) {
        int wrote = buffer.length;
        try {
            mPort.write(buffer, 0);
        }
        catch(Exception e) {
            wrote = -1;
        }
        return wrote;
    }

    /**
     * 
     */
    @Override
    public void disconnect() {
        
        try {
            mPort.close();
            mDriverConnection.close();
        }
        catch (Exception e) {
            
        }
        mDriver = null;
        /*
         * Exit
         */
        disconnectConnection();
    }

    @Override
    public List<String> getDevices() {
        return new ArrayList<String>();
    }


    @Override
    public String getConnDevice() {
        return "";
    }


}
