/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/
package com.ds.avare.message;

import android.os.Handler;
import android.os.Looper;
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

    public static void clear() {
        Message m = mHandler.obtainMessage();
        m.obj = null;
        mHandler.sendMessage(m);
    }

    /**
     * 
     * @param tv
     */
    public static void setTextView(TextView tv) {
        mTv = tv;
    }
    
    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     */
    private static Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(null != msg && null != mTv) {
                String txt = mTv.getText().toString();
                /*
                 * Limit buffer size
                 */
                if(txt.length() > 1023) {
                    txt = txt.substring(0, 1023);
                }
                if(msg.obj == null) {
                    mTv.setText("");
                }
                else {
                    mTv.setText((String)msg.obj + "\n" + txt);
                }
            }
        }
    };
    
}
