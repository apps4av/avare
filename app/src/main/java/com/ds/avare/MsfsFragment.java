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
import android.widget.TextView;

import com.ds.avare.connections.Connection;
import com.ds.avare.connections.ConnectionFactory;
import com.ds.avare.storage.Preferences;
import com.ds.avare.storage.SavedEditText;
import com.ds.avare.utils.Util;

/**
 * 
 * @author zkhan
 *
 */
public class MsfsFragment extends Fragment {
    
    private Connection mMsfs;
    private SavedEditText mTextMsfsPort;
    private TextView mTextMsfsIp;
    private CheckBox mMsfsCb;
    private Context mContext;

    @Override  
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {  
        
        mContext = container.getContext();

        View view = inflater.inflate(R.layout.layout_msfs, container, false);
        
        mTextMsfsIp = (TextView)view.findViewById(R.id.main_msfs_ip);
        mTextMsfsPort = (SavedEditText)view.findViewById(R.id.main_msfs_port);
        mMsfsCb = (CheckBox)view.findViewById(R.id.main_button_msfs_connect);
        mTextMsfsIp.setText(mTextMsfsIp.getText() + "(" + Util.getIpAddr(mContext) + ")");
        mMsfsCb.setOnClickListener(new OnClickListener() {
            
            
            @Override
            public void onClick(View v) {
              if (((CheckBox) v).isChecked()) {
                  mMsfs.setHelper(((IOActivity)getActivity()).getService());
                  mMsfs.connect(mTextMsfsPort.getText().toString(), false);
                  mMsfs.start(new Preferences(getActivity()));
              }
              else {
                  mMsfs.stop();
                  mMsfs.disconnect();
              }
            }
        });



        /*
         * Get Connection
         */
        mMsfs = ConnectionFactory.getConnection("MsfsConnection", mContext);
        
        setStates();
        return view;  
        
    }

    @Override  
    public void onDestroyView() {  
        super.onDestroyView();
    }


    /**
     * 
     */
    private void setStates() {
        mMsfsCb.setChecked(mMsfs.isConnected());
    }
    
} 