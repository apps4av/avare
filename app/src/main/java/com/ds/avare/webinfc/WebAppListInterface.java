/*
Copyright (c) 2015, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ds.avare.webinfc;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import com.ds.avare.ChecklistActivity;
import com.ds.avare.StorageService;
import com.ds.avare.flight.Checklist;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * 
 * @author zkhan
 * This class feeds the WebView with data
 */
public class WebAppListInterface {
    private StorageService mService; 
    private WebView mWebView;
    private ImportTask mImportTask;
    private GenericCallback mCallback;
    private Preferences mPref;
    private Checklist mList;
    
    private static final int MSG_UPDATE_LIST = 1;
    private static final int MSG_CLEAR_LIST = 2;
    private static final int MSG_ADD_LIST = 3;
    private static final int MSG_CLEAR_LIST_SAVE = 7;
    private static final int MSG_ADD_LIST_SAVE = 8;
    private static final int MSG_NOTBUSY = 9;
    private static final int MSG_BUSY = 10;
    
    private static final int MAX_FILE_LINE_SIZE = 256;
    private static final int MAX_FILE_LINES = 100;

    /** 
     * Instantiate the interface and set the context
     */
    public WebAppListInterface(Context c, WebView ww, GenericCallback cb) {
        mWebView = ww;
        mCallback = cb;
        mPref = new Preferences(c);
        mList = new Checklist("");
    }

    /**
     * When service connects.
     * @param s
     */
    public void connect(StorageService s) { 
        mService = s;
    }

    /**
     * 
     */
    public void clearList() {
    	mHandler.sendEmptyMessage(MSG_CLEAR_LIST);
    }

    /**
     * 
     */
    public void clearListSave() {
    	mHandler.sendEmptyMessage(MSG_CLEAR_LIST_SAVE);
    }

    /**
     * 
     * @param item
     */
    public void addItemToList(String item) {
    	// Add using javascript to show on page, strings require '' around them
    	Message m = mHandler.obtainMessage(MSG_ADD_LIST, (Object)("'" + Helper.formatJsArgs(item) + "'"));
    	mHandler.sendMessage(m);
    }

    /**
     * New saved list when the save list changes.
     */
    public void newSaveList() {
    	clearListSave();
    	
        LinkedList<Checklist> lists = mService.getDBResource().getUserLists();
        if(lists == null) {
            return;
        }

        for (Checklist cl : lists) {
        	Message m = mHandler.obtainMessage(MSG_ADD_LIST_SAVE, (Object)("'" + Helper.formatJsArgs(cl.getName()) + "'"));
        	mHandler.sendMessage(m);
        }
    }

    /**
     * New list when the list changes.
     */
    public void newList() {
        clearList();
        
        String steps[] = mList.getStepsArray();
        if(steps == null) {
            updateList();
            return;
        }
        for(int i = 0; i < steps.length; i++) {
            addItemToList(steps[i]);
        }

        updateList();
    }

    /**
     * Update the passed point on the List page
     */
    public void updateList() {
        mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
    }
    
    /**
     * Move back and forth in the list. Unlike plans which are short, this is a frequent action
     * on long list. Hence do from Android widgets 
     */
    public void moveBack() {
    	mList.moveBack();
		updateList();
    }

    /**
     * Move back and forth in the list. Unlike plans which are short, this is a frequent action 
     * on long list. Hence do from Android widgets 
     */
    public void moveForward() {
    	mList.moveForward();
		updateList();
    }


    /**
     * Move an entry in the list
     */
    @JavascriptInterface
    public void moveUpItem() {
    	// surround JS each call with busy indication / not busy 
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mList.moveItemUp();
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * Move the pointer to a particular location
     */
    @JavascriptInterface
    public void moveTo(int item) {
    	mList.moveTo(item);
		updateList();
    }

    /**
     * Move an entry in the list
     */
    @JavascriptInterface
    public void moveDownItem() {
    	// surround JS each call with busy indication / not busy 
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mList.moveItemDown();
    	
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    @JavascriptInterface
    public void discardList() {
    	mHandler.sendEmptyMessage(MSG_BUSY);
        mList = new Checklist("");
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     */
    @JavascriptInterface
    public void deleteItem() {
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	// remove current item, change working index to not overflow
    	mList.deleteItem();
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param item
     */	
    @JavascriptInterface
    public void addToList(String item) {
    	/*
    	 * Add from JS add
    	 */
    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mList.addStep(item);
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void saveList(String name) {
    	if(mList.getStepsArray().length < 1) {
    		// Anything less than 1 is not a list
    		return;
    	}

    	mList.changeName(name);

    	mHandler.sendEmptyMessage(MSG_BUSY);

        /*
         * Save to storage on save button
         */
        mService.getDBResource().setUserList(mList);

        /*
         * Make a new working list since last one stored already 
         */
        mList = new Checklist(mList.getName(), mList.getSteps());

        newSaveList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void loadList(String name) {
    	mHandler.sendEmptyMessage(MSG_BUSY);

        mList = mService.getDBResource().getUserList(name);
        if(null == mList) {
            mList = new Checklist("");
        }
    	newList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /**
     * 
     * @param name
     */
    @JavascriptInterface
    public void saveDelete(String name) {

    	mHandler.sendEmptyMessage(MSG_BUSY);

    	mService.getDBResource().deleteUserList(name);

    	newSaveList();
    	mHandler.sendEmptyMessage(MSG_NOTBUSY);
    }

    /** 
     * Get import list data from file async
     */
    @JavascriptInterface
    public void importFromFile(String path) {
        
    	/*
         * If text is 0 length or too long, then do not import
         */
        if(0 == path.length()) {
            return;
        }
        
        if(null != mImportTask) {
            if (!mImportTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                /*
                 * Cancel the last query
                 */
                mImportTask.cancel(true);
            }
        }

    	mHandler.sendEmptyMessage(MSG_BUSY);

        /*
         * New list add from file
         */
        mList = new Checklist("");
        mImportTask = new ImportTask();
        mImportTask.execute(path);
    }

    /**
     * @author zkhan
     *
     */
    private class ImportTask extends AsyncTask<String, String, String> {

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected String doInBackground(String... params) {
            
            Thread.currentThread().setName("Import");

            String txt = params[0];
            BufferedReader buffreader = null;
            InputStream instream = null;
            try {
                instream = new FileInputStream(mPref.getUserDataFolder() + File.separator + txt);
                
                InputStreamReader inputreader = new InputStreamReader(instream);
                buffreader = new BufferedReader(inputreader);

                String line;
                int i = 0;
                do {
                    line = buffreader.readLine();
                    /*
                     * Do not crash if user screws up input
                     */
                    if(i > MAX_FILE_LINES || line.length() > MAX_FILE_LINE_SIZE) {
                        break;
                    }
                    i++;
                    publishProgress(line);
                } while (line != null);
            } 
            catch (Exception e) {
                txt = null;
            }
            
            try {
            	instream.close();
            	buffreader.close();
            }
            catch(Exception e) {
            	
            }

            // dummy
            return txt;
        }

        @Override
        protected void onPostExecute(String result) {
        	
            if(null == result) {
                // show done with result
            }
            /*
             * Set new list in UI
             */
           	newList();
           	
        	mHandler.sendEmptyMessage(MSG_NOTBUSY);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if(values == null) {
                return;
            }
            if(values[0] == null) {
                return;
            }
            
            mList.addStep(values[0]);
            // keep adding lines
        }
    }

    /**
     * This leak warning is not an issue if we do not post delayed messages, which is true here.
     * Must use handler for functions called from JS, but for uniformity, call all JS from this handler
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
        	if(MSG_UPDATE_LIST == msg.what) {
                /*
                 * Now update HTML with latest list stuff, do this every time we start the List screen as
                 * things might have changed.
                 */
        		String[] steps = mList.getStepsArray();
            	for(int num = 0; num < steps.length; num++) {
            		String url = "javascript:set_list_line(" + 
            				num + "," +
            				(mList.isSelected(num) ? 1 : 0) + ",'" + steps[num] + "')";
            		mWebView.loadUrl(url);
            	}
            	
            	if(null != mList.getName()) {
            		mWebView.loadUrl("javascript:list_setname('" + mList.getName() + "')");
            	}
        	}
        	else if(MSG_CLEAR_LIST == msg.what) {
        		mWebView.loadUrl("javascript:list_clear()");
        	}
        	else if(MSG_ADD_LIST == msg.what) {
            	String func = "javascript:list_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_CLEAR_LIST_SAVE == msg.what) {
            	String func = "javascript:save_clear()";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_ADD_LIST_SAVE == msg.what) {
            	String func = "javascript:save_add(" + (String)msg.obj + ")";
            	mWebView.loadUrl(func);
        	}
        	else if(MSG_NOTBUSY == msg.what) {
        		mCallback.callback((Object) ChecklistActivity.UNSHOW_BUSY, null);
        	}
        	else if(MSG_BUSY == msg.what) {
        		mCallback.callback((Object)ChecklistActivity.SHOW_BUSY, null);
        	}
        }
    };
}