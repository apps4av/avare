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

import android.content.Context;

import com.ds.avare.nmea.Ownship;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author zkhan
 *
 */
public class MsfsConnection extends Connection {

    
    private static MsfsConnection mConnection;
    
    DatagramSocket mSocket;
    
    private int mPort;

    /**
     * 
     */
    private MsfsConnection() {
        super("MSFS Input");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {

                com.ds.avare.nmea.DataBuffer nbuffer =
                        new com.ds.avare.nmea.DataBuffer(16384);
                com.ds.avare.nmea.Decode ndecode =
                        new com.ds.avare.nmea.Decode();
                Ownship nmeaOwnship = new Ownship();


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
                        Logger.Logit("Listener error, re-starting listener");

                        disconnect();
                        connect(Integer.toString(mPort), false);
                        continue;
                    }

                    nbuffer.put(buffer, red);

                    byte[] buf;

                    while (null != (buf = nbuffer.get())) {
                        com.ds.avare.nmea.Message m = ndecode.decode(buf);
                        if (nmeaOwnship.addMessage(m)) {

                            /*
                             * Make a GPS locaiton message from ADSB ownship message.
                             */
                            JSONObject object = new JSONObject();
                            Ownship om = nmeaOwnship;
                            try {
                                object.put("type", "ownship");
                                object.put("longitude", (double) om.mLon);
                                object.put("latitude", (double) om.mLat);
                                object.put("speed", (double) (om.mHorizontalVelocity));
                                object.put("bearing", (double) om.mDirection);
                                object.put("altitude", (double) ((double) om.mAltitude));
                                object.put("time", (long) om.getTime());
                            } catch (JSONException e1) {
                                continue;
                            }

                            sendDataToHelper(object.toString());
                        }
                    }
                }

                return null;
            }
        });
    }

    
    /**
     * 
     * @return
     * @param ctx
     */
    public static MsfsConnection getInstance(Context ctx) {

        if(null == mConnection) {
            mConnection = new MsfsConnection();
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
    public boolean connect(String to, boolean secure) {

        try {
            mPort = Integer.parseInt(to);
        }
        catch (Exception e) {
            return false;
        }
        
        /*
         * Make socket
         */
        Logger.Logit("Making socket to listen");

        try {
            mSocket = new DatagramSocket(mPort);
            mSocket.setReuseAddress(true);
        }
        catch(Exception e) {
            Logger.Logit("Failed! Connecting socket " + e.getMessage());
            return false;
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
            mSocket.close();
        } 
        catch(Exception e2) {
            Logger.Logit("Error stream close");
        }

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

    /**
     * 
     * @return
     */
    private int read(byte[] buffer) {
        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length); 
        try {
            mSocket.receive(pkt);
        } 
        catch(Exception e) {
            return -1;
        }

        saveToFile(pkt.getLength(), buffer);
        return pkt.getLength();
    }

}
