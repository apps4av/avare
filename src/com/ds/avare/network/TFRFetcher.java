/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.network;

import java.util.LinkedList;

import com.ds.avare.shapes.TFRShape;
import com.ds.avare.utils.Helper;


import android.content.Context;
import android.os.AsyncTask;

/**
 * 
 * @author zkhan
 *
 */
public class TFRFetcher {

    
    private TFRTask mTask;
    private LinkedList<TFRShape> mShapes;
    private Context mContext;
    
    /**
     * 
     */
    public TFRFetcher(Context ctx) {
        mShapes = null;
        mContext = ctx;
    }

    /**
     * Parse TFRs
     */
    public void parse() {
        /*
         * TFR is an expensive operation. Do not do if previous is running
         */
        if(mTask != null) {
            if(mTask.getStatus() == AsyncTask.Status.RUNNING) {
                mTask.cancel(true);
            }
        }
        
        /*
         * Start the task
         */
        mTask = new TFRTask();
        mTask.execute();
    }
    
    /**
     * This will be non null if we have recieved TFR shapes from internet
     * @return
     */
    public LinkedList<TFRShape> getShapes() {
        return mShapes;
    }

    /**
     * @author zkhan
     *
     */
    private class TFRTask extends AsyncTask<Object, Void, Boolean> {


        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Object... vals) {
            Thread.currentThread().setName("TFR");

            mShapes = Helper.getShapesInTFR(mContext);
            return true;
        }
    } 
}
