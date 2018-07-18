/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;


/**
 * 
 * @author zkhan
 *
 */
public class Logger {

    private static TextView mTv;

    public static void Logit(String msg) {
        Message m = mHandler.obtainMessage();
        m.obj = (Object)msg;
        mHandler.sendMessage(m);
    }

    /**
     *
     * @param tv
     */
    public static void setTextView(TextView tv) {
        mTv = tv;
    }
    public static void setContext(Context ctx) {

    }

    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String val = (String) msg.obj;
            if(null != msg && null != mTv) {
                String txt = mTv.getText().toString();
                /*
                 * Limit buffer size
                 */
                if (txt.length() > 1023) {
                    txt = txt.substring(0, 1023);
                }
                mTv.setText(val + "\n" + txt);
            }

        }
    };

}
