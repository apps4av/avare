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
import android.widget.CheckBox;

import com.ds.avare.connections.Connection;
import com.ds.avare.connections.ConnectionFactory;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.SavedEditText;

public class Dump1090Fragment extends IOFragment {
    
    private Connection mDump1090;
    private CheckBox mConState;
    private Context mContext;
    private SavedEditText mIpAddress;

    @Override  
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {  
        
        mContext = container.getContext();

        View view = inflater.inflate(R.layout.layout_dump1090, container, false);

        /*
         * Dump1090 connection
         */
        mIpAddress = (SavedEditText)view.findViewById(R.id.main_ip_address);
        mConState = (CheckBox)view.findViewById(R.id.main_connect1090);
        mConState.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CheckBox) v).isChecked()) {
                    mDump1090.connect(mIpAddress.getText().toString(), false);
                    mDump1090.start();
                } else {
                    mDump1090.stop();
                    mDump1090.disconnect();
                }
            }
        });

        /*
         * Get Connection
         */
        mDump1090 = ConnectionFactory.getConnection(ConnectionFactory.CF_Dump1090Connection, mContext);

        setStates();
        return view;  
        
    }
    
    /**
     * 
     */
    private void setStates() { mConState.setChecked(mDump1090.isConnected()); }

    @Override  
    public void onDestroyView() {  
        super.onDestroyView();
    }    
} 