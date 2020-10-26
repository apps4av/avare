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

    public static Connection getConnection(String type, Context ctx) {
        switch(type) {
            case CF_BlueToothConnectionOut:
                return BTOutConnection.getInstance(ctx);

            case CF_WifiConnection:
                return WifiConnection.getInstance(ctx);

            case CF_BlueToothConnectionIn:
            case CF_FileConnectionIn:
            case CF_GPSSimulatorConnection:
            case CF_MsfsConnection:
            case CF_USBConnectionIn:
            case CF_XplaneConnection:
                break;
        }
        return null;
    }


    /*
 * Find names of all running connections.
 */
    public static String getActiveConnections(Context ctx) {
        String s = "";
        s += getConnection(CF_WifiConnection, ctx).isConnected() ?  "," + ctx.getString(R.string.WIFI) : "";
        s += getConnection(CF_BlueToothConnectionOut, ctx).isConnected() ?  "," + ctx.getString(R.string.BTOut) : "";
        if(s.startsWith(",")) {
            s = s.substring(1);
        }
        return "(" + s + ")";
    }

}
