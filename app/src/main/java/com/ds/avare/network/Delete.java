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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Observable;

import com.ds.avare.utils.Helper;

import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author zkhan
 *
 */
public class Delete extends Observable {
    
    private DeleteTask   mDt;
    private boolean     mStop;
    private Handler      mHandler;
    private Thread       mThread;
   
    public static final int FAILED = -2;
    public static final int SUCCESS = -1;
    
    static final int blocksize = 4096;

       
    /**
     * 
     * @param act
     */
    public Delete(Handler handler) {
        mStop = false;
        mDt = null;
        mHandler = handler;
    }
    
    /**
     * 
     */
    public void cancel() {
        mStop = true;
    }

    /**
     * 
     * @param url
     * @param path
     * @param dataSource 
     * @param filename
     */
    public void start(String path, String name) {
        mDt = new DeleteTask();
        mDt.path = path;
        mDt.chart = name;
        mThread = new Thread(mDt);
        mThread.start();
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class DeleteTask implements Runnable {

        public String path;
        public String chart;
        
        /**
         * 
         */
        @Override
        public void run() {
            
            Thread.currentThread().setName("Delete");

            if(path == null || chart == null) {
                Message m = mHandler.obtainMessage(Download.FAILED, Delete.this);
                mHandler.sendMessage(m);
            }
            
            /*
             * Get files to delete
             */
            LinkedList<String> list = getFiles(chart, path);
            
            int fileLength = list.size();
            int total = 0;
            int newp;
            int lastp = FAILED;

            for(String name : list) {
                
                if(mStop) {
                    Message m = mHandler.obtainMessage(Download.FAILED, Delete.this);
                    mHandler.sendMessage(m);
                    return;
                }
                newp = (int) (total * 50 / fileLength);
                
                Helper.deleteDir(new File(name));
                
                if(lastp != newp) {
                    lastp = newp;
                    Message m = mHandler.obtainMessage(newp, Delete.this);
                    mHandler.sendMessage(m);
                }
            }

            Message m = mHandler.obtainMessage(Download.SUCCESS, Delete.this);
            mHandler.sendMessage(m);
        }      
    }
    
    /**
     * 
     * @param name
     * @return
     */
    LinkedList<String> getFiles(String name, String path) {
    	LinkedList<String> files2Delete = new LinkedList<String>();
    	/*
    	 * Read file with that name
    	 */
    	String filename = path + "/" + name;
        File file = new File(filename);
        if(file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file), blocksize);
                /*
                 * skip the first line which is date / version
                 */
                String line = br.readLine();
                
                do {
                	/*
                	 * Get list of files in here to delete
                	 */
                	line = br.readLine();
                	if(null != line) {
                		files2Delete.add(path + "/" + line);
                	}
                } while (null != line);
                
                br.close();
            }
            catch (IOException e) {
            }
        }
        
        /*
         * Delete the file and the any partial zip file
         */
        files2Delete.add(filename);
        files2Delete.add(filename + ".zip");
        return files2Delete;
    }
    
}
