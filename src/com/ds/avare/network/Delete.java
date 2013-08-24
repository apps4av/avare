/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.network;

import java.io.File;
import java.util.LinkedList;
import java.util.Observable;

import com.ds.avare.storage.DataSource;



import android.os.AsyncTask;

/**
 * 
 * @author zkhan
 *
 */
public class Delete extends Observable {
    
    private DeleteTask   mDt;
    private boolean     mStop;
    private DataSource   mData;
   
    public static final int FAILED = -2;
    public static final int SUCCESS = -1;
    
       
    /**
     * 
     * @param act
     */
    public Delete() {
        mStop = false;
        mDt = null;
    }
    
    /**
     * 
     */
    public void cancel() {
        mStop = true;
        if(null != mDt) {
            mDt.cancel(true);
        }
    }

    /**
     * 
     * @param url
     * @param path
     * @param dataSource 
     * @param filename
     */
    public void start(String path, String name, DataSource dataSource) {
        mData = dataSource;
        if(mDt != null) {
            if(mDt.getStatus() != AsyncTask.Status.RUNNING) {
                return;
            }
        }
        mDt = new DeleteTask();
        mDt.execute(path, name);
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class DeleteTask extends AsyncTask<String, Integer, Boolean> {

        /**
         * 
         */
        @Override
        protected Boolean doInBackground(String... sUrl) {
            String path = sUrl[0];
            String chart = sUrl[1];
            
            if(mData == null || path == null || chart == null) {
                return false;
            }
            LinkedList<String> list = mData.findFilesToDelete(chart);
            
            int fileLength = list.size();
            int total = 0;
            int newp;
            int lastp = FAILED;

            for(String name : list) {
                newp = (int) (total * 50 / fileLength);
                
                String toDelete = path + "/" + name;
                
                try {
                    (new File(toDelete)).delete();
                }
                catch(Exception e) {
                    
                }
                
                if(lastp != newp) {
                    lastp = newp;
                    publishProgress(newp);
                }
            }
            
            return true;
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            Delete.this.setChanged();
            Delete.this.notifyObservers(progress[0]); 
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if(result && (!mStop)) {
                Delete.this.setChanged();
                Delete.this.notifyObservers(SUCCESS);
            }
            else {
                Delete.this.setChanged();
                Delete.this.notifyObservers(FAILED);
            }
        }
    }
}
