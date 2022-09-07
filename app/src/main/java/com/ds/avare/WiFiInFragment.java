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


import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.ds.avare.connections.Connection;
import com.ds.avare.connections.WifiConnection;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.SavedEditText;

import java.io.File;

/**
 * 
 * @author zkhan
 *
 */
public class WiFiInFragment extends Fragment {
    
    private Connection mWifi;
    private SavedEditText mTextWifiPort;
    private CheckBox mWifiCb;
    private Context mContext;
    private Button mConnectFileSaveButton;
    private SavedEditText mTextFileSave;

    @Override  
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {  
        
        mContext = container.getContext();

        View view = inflater.inflate(R.layout.layout_wifiin, container, false);
        
        /*
         * WIFI connection
         */

        mWifi = WifiConnection.getInstance(mContext);

        mTextFileSave = (SavedEditText)view.findViewById(R.id.main_file_name_save);
        mWifiCb = (CheckBox)view.findViewById(R.id.main_button_connectwifi);
        mTextWifiPort = (SavedEditText)view.findViewById(R.id.main_wifi_port);
        mWifiCb.setOnClickListener(new OnClickListener() {
            
            
            @Override
            public void onClick(View v) {
                Preferences pref = StorageService.getInstance().getPreferences();

                if (((CheckBox) v).isChecked()) {
                    mWifi.connect(mTextWifiPort.getText().toString(), false);
                    pref.setLastConnectedWifi(mTextWifiPort.getText().toString()); //  save for connect next time
                    mWifi.start();
                }
                else {
                    mWifi.stop();
                    pref.setLastConnectedWifi(null); // clear
                    mWifi.disconnect();
                }
            }
        });

        mConnectFileSaveButton = (Button)view.findViewById(R.id.main_button_connect_file_save);
        mConnectFileSaveButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                /*
                 * If connected, disconnect
                 */
                String val = mTextFileSave.getText().toString();
                if(mConnectFileSaveButton.getText().equals(mContext.getString(R.string.Save))) {
                    mConnectFileSaveButton.setText(mContext.getString(R.string.Saving));
                    String fl = StorageService.getInstance().getPreferences().getUserDataFolder() + File.separatorChar + val;
                    mWifi.setFileSave(fl);
                }
                else {
                    mConnectFileSaveButton.setText(mContext.getString(R.string.Save));
                    mWifi.setFileSave(null);
                }
                setStates();
            }
        });

        setStates();
        return view;  
        
    }
    
    /**
     * 
     */
    private void setStates() {
        mWifiCb.setChecked(mWifi.isConnected());
    }


    @Override  
    public void onDestroyView() {  
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // wifi can be started from other activities so set correct state.
        mWifiCb.setChecked(mWifi.isConnected());
    }
} 