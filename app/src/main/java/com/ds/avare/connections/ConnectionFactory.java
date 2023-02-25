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

import com.ds.avare.R;


/**
 * Created by zkhan on 9/22/16.
 */
public class ConnectionFactory {
    public static final String CF_BlueToothConnectionIn  = "BlueToothConnectionIn";
    public static final String CF_BlueToothConnectionOut = "BlueToothConnectionOut";
    public static final String CF_WifiConnection         = "WifiConnection";
    public static final String CF_FileConnectionIn       = "FileConnectionIn";
    public static final String CF_GPSSimulatorConnection = "GPSSimulatorConnection";
    public static final String CF_MsfsConnection         = "MsfsConnection";
    public static final String CF_USBConnectionIn        = "USBConnectionIn";
    public static final String CF_XplaneConnection       = "XplaneConnection";
    public static final String CF_Dump1090Connection     = "Dump1090Connection";
    public static final String CF_USBConnectionOut       = "USBConnectionOut";

    public static Connection getConnection(String type, Context ctx) {
        if(type.equals(CF_BlueToothConnectionIn)) {
            return BlueToothConnectionIn.getInstance(ctx);
        }
        if(type.equals(CF_BlueToothConnectionOut)) {
            return BlueToothConnectionOut.getInstance(ctx);
        }
        if(type.equals(CF_FileConnectionIn)) {
            return FileConnectionIn.getInstance(ctx);
        }
        if(type.equals(CF_GPSSimulatorConnection)) {
            return GPSSimulatorConnection.getInstance(ctx);
        }
        if(type.equals(CF_MsfsConnection)) {
            return MsfsConnection.getInstance(ctx);
        }
        if(type.equals(CF_USBConnectionIn)) {
            return USBConnectionIn.getInstance(ctx);
        }
        if(type.equals(CF_WifiConnection)) {
            return WifiConnection.getInstance(ctx);
        }
        if(type.equals(CF_XplaneConnection)) {
            return XplaneConnection.getInstance(ctx);
        }
        if(type.equals(CF_Dump1090Connection)) {
            return Dump1090Connection.getInstance(ctx);
        }
        if(type.equals(CF_USBConnectionOut)) {
            return USBConnectionOut.getInstance(ctx);
        }
        return null;
    }


    /*
 * Find names of all running connections.
 */
    public static String getActiveConnections(Context ctx) {
        String s = "";
        s += getConnection(CF_BlueToothConnectionIn, ctx).isConnected() ? "," + ctx.getString(R.string.Bluetooth) : "";
        s += getConnection(CF_WifiConnection, ctx).isConnected() ?  "," + ctx.getString(R.string.WIFI) : "";
        s += getConnection(CF_XplaneConnection, ctx).isConnected() ? "," + ctx.getString(R.string.XPlane) : "";
        s += getConnection(CF_MsfsConnection, ctx).isConnected() ? "," + ctx.getString(R.string.MSFS) : "";
        s += getConnection(CF_BlueToothConnectionOut, ctx).isConnected() ? "," + ctx.getString(R.string.AP) : "";
        s += getConnection(CF_FileConnectionIn, ctx).isConnected() ? "," + ctx.getString(R.string.Play) : "";
        s += getConnection(CF_GPSSimulatorConnection, ctx).isConnected() ? "," + ctx.getString(R.string.GPSSIM) : "";
        s += getConnection(CF_USBConnectionIn, ctx).isConnected() ? "," + ctx.getString(R.string.USBIN) : "";
        s += getConnection(CF_USBConnectionOut, ctx).isConnected() ? "," + ctx.getString(R.string.APUSB) : "";
        s += getConnection(CF_Dump1090Connection, ctx).isConnected() ? "," + ctx.getString(R.string.DUMP1090) : "";
        if(s.startsWith(",")) {
            s = s.substring(1);
        }
        return "(" + s + ")";
    }

}
