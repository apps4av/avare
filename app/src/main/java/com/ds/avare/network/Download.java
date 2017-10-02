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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 * @author zkhan
 *
 */
public class Download {
    
    private DownloadTask   mDt;
    private boolean mStop;
    private String mVersion;
    private String mRoot;
    private Handler mHandler;
    private Thread mThread;
    private String mCode;
    private int mCycleAdjust;

    public static final int FAILED = -2;
    public static final int SUCCESS = -1;
    public static final int NONEED = -3;
    
    private static final int blocksize = 8192;
       
    /**
     * 
     */
    public Download(String root, Handler handler, int cycleAdjust) {
        mStop = false;
        mDt = null;
        mVersion = null;
        mRoot = root;
        mCode = "";
        mHandler = handler;
        mCycleAdjust = cycleAdjust;
    }
    
    /**
     * 
     * @return
     */
    public String getVersion() {
        return(mVersion);
    }
    
    /**
     * 
     */
    public void cancel() {
        mStop = true;
        if(mThread != null) {
            mThread.interrupt();
        }
    }

    /**
     * 
     * @param isStatic
     * @param path
     * @param filename
     */
    public void start(String path, String filename, boolean isStatic) {
        mDt = new DownloadTask();
        mDt.path = path;
        mDt.mName = filename;
        mDt.mStatic = isStatic;
        mThread = new Thread(mDt);
        mThread.start();
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class DownloadTask implements Runnable {

        String path;
        String mName;
        boolean mStatic;

        /**
         * 
         * @param in
         * @param out
         * @throws IOException
         */
        public final void copyInputStream(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[blocksize];
            int len;
        
            while((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
        
            in.close();
            out.close();
        }

        /**
         * 
         */
        private void sendFailure() {
            Message m = mHandler.obtainMessage(Download.FAILED, Download.this);
            Bundle b = new Bundle();
            b.putString("code", mName + ", " + mCode);
            m.setData(b);
            mHandler.sendMessage(m);            
        }
        
        /**
         * 
         */
        @Override
        public void run() {

            Thread.currentThread().setName("Download");

            BufferedInputStream input;
            BufferedOutputStream output;
            int count;
            byte data[] = new byte[blocksize];
            long fileLength;
            boolean flags[] = new boolean[1];

            mVersion = NetworkHelper.getVersion(mRoot, mName, flags);

            /*
             * See if we can adjust the version based on number like 1408.
             * If it is time, then ignore the adjust by catching exception
             */
            mVersion = NetworkHelper.findCycleOffset(mVersion, mCycleAdjust);

            try {

            	if(!flags[0]) {
            		mCode = "code unable to connect to server ";
                    sendFailure();
                    return;
            	}

                /*
                 * mCode allows debugging from users
                 */
                mCode = "code invalid path/file name";
                
                /*
                 * Path in which to install it
                 */
                File f = new File(path);
                mCode = "code unable to create folder " + f.getAbsolutePath();
                if(!f.exists()) {
                    if(!f.mkdirs()) {
                        sendFailure();
                        return;
                    }
                }

                /*
                 * Path with file name on local storage
                 */
                mCode = "code unable to get zipped file name";
                String zipfile = path + "/" + mName + ".zip";
                mCode = "code unable to get network file name ";
                String netfile = NetworkHelper.getUrl(mName + ".zip", mVersion, mRoot, mStatic);

                /* 
                 * Download the file
                 */
                mCode = "code unable to get network file URL ";
                URL url = new URL(netfile);
                
                File zfile = new File(zipfile);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if(zfile.exists()) {
                    // if file exists, resume download
                    connection.setRequestProperty("Range", "bytes=" + (zfile.length()) + "-");
                }
                mCode = "code unable to download the file from this cycle ";
                connection.connect();
                int code = connection.getResponseCode();
                if (code != 206 && code != 200) {
                    zfile.delete();
                    sendFailure();
                    return;
                }
                
                
                String connectionField = connection.getHeaderField("content-range");
                long downloadedSize = 0;
                if (connectionField != null) {
                    String[] connectionRanges = connectionField.substring("bytes=".length()).split("-");
                    downloadedSize = Long.valueOf(connectionRanges[0]);
                }
                
                mCode = "code unable to get file from server ";
                fileLength = connection.getContentLength() + downloadedSize;
                
                input = new BufferedInputStream(connection.getInputStream(), blocksize);
                mCode = "code unable to store the zip file ";
                output = new BufferedOutputStream(new FileOutputStream(zipfile, true), blocksize);
    
                long total = downloadedSize;
                int lastp = FAILED;
                int newp;
                while(true) {
                    mCode = "code unable to read zip file from server ";
                    count = input.read(data, 0, blocksize);
                    if(count <= 0) {
                        break;
                    }
                    total += count;
                    newp = (int) (total * 50 / fileLength);
                    /* 
                     * publishing the progress....
                     */
                    if(lastp != newp) {
                        lastp = newp;
                        Message m = mHandler.obtainMessage(newp, Download.this);
                        mHandler.sendMessage(m);
                    }
                    mCode = "code unable to write zip file to flash, disk full";
                    output.write(data, 0, count);
                    if(mStop) {
                        mCode = "code stopped by user during download";
                        output.flush();
                        output.close();
                        sendFailure();
                        input.close();
                        return;
                    }
                }
    
                mCode = "code unable to close retrieved file ";
                output.flush();
                output.close();
                input.close();
                
                mCode = "";
                /*
                 * Now unzip
                 */
                try {
                    mCode = "code unable to start unzip process ";
                    ZipFile zipFile = new ZipFile(zipfile);
                    int filenum = zipFile.size();
                    int totalnum = 0;

                    mCode = "code corrupt zip file ";
                    Enumeration<? extends ZipEntry> ent = zipFile.entries();
                    List list = Collections.list(ent);

                    // sort for it affects cleanup logic
                    Collections.sort(list, new Comparator<ZipEntry>() {
                        public int compare(ZipEntry z1, ZipEntry z2) {
                            return z1.getName().compareTo(z2.getName());
                        }
                    });
                    Enumeration<? extends ZipEntry> entries = Collections.enumeration(list);

                    String lastName = "";
                    while(entries.hasMoreElements()) {
                        mCode = "code unzip file error, disk full";
                        if(mStop) {
                            mCode = "code stopped by user during unzip";
                            zipFile.close();
                            new File(zipfile).delete();
                            sendFailure();
                            return;
                        }

                        mCode = "code stopped by unzip, corrupt file";
                        ZipEntry entry = (ZipEntry)entries.nextElement();

                        /*
                         * Keep un-zipping and creating folders
                         */
                        String entryName = entry.getName();
                        String fn = path + "/" + entryName;
                        String tokens[] = entryName.split("/");
                        String folder = tokens[0];
                        
                        /*
                         * This is a new folder, do something with it.
                         * Mostly needed for delete
                         */
                        mCode = "code invalid overwrite folder";
                        File dir = new File(fn.substring(0, fn.lastIndexOf("/")));

                        if(!lastName.equals(dir.getCanonicalPath())) {
                            /*
                             * Delete older plates
                             */
                            if(folder.equals("plates") && dir.exists()) {
                                mCode = "code unable to delete/replace plates";
                                Helper.deleteDir(dir);
                            }

                            /*
                             * Delete older minimums
                             */
                            else if(folder.equals("minimums") && dir.exists()) {
                                mCode = "code unable to delete/replace minimums";
                                Helper.deleteDir(dir);
                            }

                            /*
                             * Delete older A/FD
                             */
                            else if(folder.equals("afd") && dir.exists()) {
                                mCode = "code unable to delete/replace A/FD";
                                String newRegion = (tokens[1].split("_"))[0];
                                String[] info = dir.list();
                                for(int i = 0; i < info.length; i++) {
                                    if(info[i].startsWith(newRegion)) {
                                        (new File(path + "/afd/" + info[i])).delete();
                                    }
                                }
                            }

                            lastName = dir.getCanonicalPath();
                        }

                        dir.mkdirs();
                        
                        
                        /*
                         * Make sure someone does not index avare's images.
                         */
                        if(dir.isDirectory()) {
                            String nomedia = dir.getAbsolutePath() + "/.nomedia";
                            f = new File(nomedia);
                            mCode = "code unable to create file " + f.getAbsolutePath();
                            if(!f.exists()) {
                                f.createNewFile();
                            } 
                        }


                        mCode = "code unable to delete old file";
                        File outf = new File(path + "/" + entry.getName());
                        
                        // Skip dir creation, its already created above
                        if(outf.isDirectory()) {
                            continue;
                        }
                        
                        if(outf.exists()) {
                            outf.delete();
                        }
                        
                        mCode = "code unable to unzip file, disk full";
                        copyInputStream(zipFile.getInputStream(entry),
                                new BufferedOutputStream(new FileOutputStream(path + "/" + entry.getName()), blocksize));
                        totalnum++;
                        newp = (int)(50 + totalnum * 50 / filenum);
                        if(lastp != newp) {
                            lastp = newp;
                            Message m = mHandler.obtainMessage(newp, Download.this);
                            mHandler.sendMessage(m);
                        }
                    }

                    mCode = "code unable to close zip file";
                    zipFile.close();
                    
                    /*
                     * Delete the downloaded file to save space
                     */
                    mCode = "code unable to delete downloaded zip file";
                    new File(zipfile).delete();

                    mCode = "";
                    Message m = mHandler.obtainMessage(Download.SUCCESS, Download.this);
                    mHandler.sendMessage(m);
                    return;
                    
                } catch (Exception e) {
                    mCode += e.getCause();
                    zfile.delete();
                }
            } catch (Exception e) {
                mCode += e.getCause();
            }
            sendFailure();
            return;
        }        
    }
}
