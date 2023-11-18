/*
Copyright (c) 2012, Apps4Av Inc. (ds.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.ds.avare.connections.Connection;
import com.ds.avare.connections.ConnectionFactory;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.SavedEditText;

import java.io.File;

/**
 * 
 * @author zkhan
 * 
 */
public class USBOutFragment extends IOFragment {

    private Connection mUSB;
    private Context mContext;
    private Button mConnectButton;
    private SavedEditText mParamsText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mContext = container.getContext();

        View view = inflater.inflate(R.layout.layout_usbout, container, false);

        mParamsText = (SavedEditText) view.findViewById(R.id.usbout_params_text);

        mUSB = ConnectionFactory.getConnection(ConnectionFactory.CF_USBConnectionOut, mContext);

        mConnectButton = (Button) view.findViewById(R.id.usbout_button_connect);
        mConnectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                /*
                 * If connected, disconnect
                 */
                if (mUSB.isConnected()) {
                    mUSB.stop();
                    mUSB.disconnect();
                    setStates();
                    return;
                }
                /*
                 * Connect to the given device in list
                 */
                if (!mUSB.isConnected()) {
                    mConnectButton.setText(getString(R.string.Connect));
                    mUSB.connect(mParamsText.getText().toString(), false);
                    Preferences pref = StorageService.getInstance().getPreferences();
                    if (mUSB.isConnected()) {
                        mUSB.start();
                        pref.setLastConnectedUSB(mParamsText.getText().toString()); // save where we connected
                    } else {
                        pref.setLastConnectedUSB(null); // clear
                    }
                    setStates();
                }
            }
        });
        return view;
    }

    /**
     * 
     */
    private void setStates() {
        if (mUSB.isConnected()) {
            mConnectButton.setText(getString(R.string.Disconnect));
        } else {
            mConnectButton.setText(getString(R.string.Connect));
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}