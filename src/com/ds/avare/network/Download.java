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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ds.avare.utils.NetworkHelper;


import android.os.AsyncTask;

/**
 * 
 * @author zkhan
 *
 */
public class Download extends Observable {
    
    private DownloadTask   mDt;
    private boolean mStop;
    private String mVersion;
    private String mName;
    private String mRoot;
   
    public static final int FAILED = -2;
    public static final int SUCCESS = -1;
    public static final int NONEED = -3;
    
    private static final int blocksize = 8192;
       
    /**
     * 
     * @param act
     */
    public Download(String root) {
        mStop = false;
        mDt = null;
        mVersion = null;
        mName = null;
        mRoot = root;
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
        if(null != mDt) {
            mDt.cancel(true);
        }
    }

    /**
     * 
     * @param url
     * @param path
     * @param filename
     */
    public void start(String path, String filename) {
        if(mDt != null) {
            if(mDt.getStatus() != AsyncTask.Status.RUNNING) {
                return;
            }
        }
        mDt = new DownloadTask();
        mDt.execute(path, filename);
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {

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

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 
         */
        @Override
        protected Boolean doInBackground(String... sUrl) {

            BufferedInputStream input;
            BufferedOutputStream output;
            int count;
            String path = sUrl[0];
            mName = sUrl[1];
            byte data[] = new byte[blocksize];
            mVersion = NetworkHelper.getVersion(mName, mRoot);
            int fileLength;
                        
            try {
                File file = new File(path + "/" + mName);
                
                
                /*
                 * Path in which to install it
                 */
                File f = new File(path);
                if(!f.exists()) {
                    if(!f.mkdir()) {
                        return false;
                    }
                }
                
                /*
                 * Make sure someone does not index avare's images.
                 */
                String nomedia = path + "/.nomedia";
                f = new File(nomedia);
                if(!f.exists()) {
                    f.createNewFile();
                }
                
                /*
                 * Path with file name on local storage
                 */
                String zipfile = path + "/" + mName + ".zip";
                String netfile = NetworkHelper.getUrl(mName + ".zip", mVersion, mRoot);

                /* 
                 * Download the file
                 */
                URL url = new URL(netfile);
                URLConnection connection = url.openConnection();
                connection.connect();
                input = new BufferedInputStream(url.openStream(), blocksize);
                fileLength = connection.getContentLength();
                output = new BufferedOutputStream(new FileOutputStream(zipfile), blocksize);
    
                long total = 0;
                int lastp = FAILED;
                int newp;
                while(true) {
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
                        publishProgress(newp);
                    }
                    output.write(data, 0, count);
                    if(mStop) {
                        output.flush();
                        output.close();
                        input.close();
                        return false;
                    }
                }
    
                output.flush();
                output.close();
                input.close();
                
                /*
                 * Now unzip
                 */
                try {
                    ZipFile zipFile = new ZipFile(zipfile);
                    int filenum = zipFile.size();
                    int totalnum = 0;

                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while(entries.hasMoreElements()) {
                        if(mStop) {
                            zipFile.close();
                            return false;
                        }

                        ZipEntry entry = (ZipEntry)entries.nextElement();

                        /*
                         * Keep un-zipping and creating folders
                         */
                        String fn = path + "/" + entry.getName();
                        File dir = new File(fn.substring(0, fn.lastIndexOf("/")));
                        dir.mkdirs();
                        
                        File outf = new File(path + "/" + entry.getName());
                        if(outf.exists()) {
                            outf.delete();
                        }
                        
                        copyInputStream(zipFile.getInputStream(entry),
                            new BufferedOutputStream(new FileOutputStream(path + "/" + entry.getName()), blocksize));
                        totalnum++;
                        newp = (int)(50 + totalnum * 50 / filenum);
                        if(lastp != newp) {
                            lastp = newp;
                            publishProgress(newp);
                        }
                    }

                    zipFile.close();
                    
                    /*
                     * Delete the downloaded file to save space
                     */
                    new File(zipfile).delete();

                    /*
                     * Now create a version file
                     */
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file), blocksize);                    
                    bw.write(mVersion);
                    bw.flush();
                    bw.close();
                    
                    return true;
                    
                } catch (IOException ioe) {
                }
            } catch (Exception e) {
            }
            return false;
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            Download.this.setChanged();
            Download.this.notifyObservers(progress[0]); 
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if(result && (!mStop)) {
                Download.this.setChanged();
                Download.this.notifyObservers(SUCCESS);
            }
            else {
                Download.this.setChanged();
                Download.this.notifyObservers(FAILED);
            }
        }
    }
}
