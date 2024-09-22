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

import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Logger;

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
public class XplaneConnection extends Connection {

    
    private static XplaneConnection mConnection;

    DatagramSocket mSocket;
    
    private int mPort;
    

    /**
     * 
     */
    private XplaneConnection() {
        super("XPlane Input");
        setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {

                /*
                 * This state machine will keep trying to connect to
                 * ADBS/GPS receiver
                 */
                while(isRunning()) {

                    int red = 0;

                    /*
                     * Read.
                     */
                    red = read(buffer);
                    if(red <= 0) {
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
                        Logger.Logit("Listener error, re-starting listener");

                        disconnect();
                        connect(Integer.toString(mPort), false);
                        continue;
                    }

                    String input = new String(buffer, 0, red);
                    if(input.startsWith("XGPS")) {
                        String tokens[] = input.split(",");
                        if(tokens.length >= 6) {
                            /*
                             * Make a GPS location message from ownship message.
                             */
                            JSONObject object = new JSONObject();
                            try {
                                object.put("type", "ownship");
                                object.put("longitude", Double.parseDouble(tokens[1]));
                                object.put("latitude", Double.parseDouble(tokens[2]));
                                object.put("speed", Double.parseDouble(tokens[5]));
                                object.put("bearing", Double.parseDouble(tokens[4]));
                                object.put("altitude", Double.parseDouble(tokens[3]));
                                object.put("time", System.currentTimeMillis());
                            } catch (Exception e1) {
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
    public static XplaneConnection getInstance(Context ctx) {

        if(null == mConnection) {
            mConnection = new XplaneConnection();
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
        return pkt.getLength();
    }

}
