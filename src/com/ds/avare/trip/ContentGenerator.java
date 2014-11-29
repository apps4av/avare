/*

Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.trip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;

import android.content.Context;
import android.os.AsyncTask;

/**
 * A class that does all the fill in magic
 * @author zkhan
 *
 */
public class ContentGenerator extends Observable {

	private Document mDoc;
	private StorageService mService;
	private Context mContext;
	private Preferences mPref;
	
	/**
	 * 
	 * @param ctx
	 * @param service
	 */
	public ContentGenerator(Context ctx, StorageService service) {
		mContext = ctx;
		mService = service;
		mPref = new Preferences(ctx);
	}
	
	/**
	 * 
	 * @return
	 */
	public void getPage(String page) {

		mDoc = null;
		/**
		 * Do get the page in background
		 */
        AsyncTask<Object, Object, Boolean> task = new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... vals) {
        		try {
        			/*
        			 * Use JSOUP to get the page as we need to fill in data
        			 */
        			mDoc = Jsoup.connect((String)vals[0]).get();
        		} catch (Exception e) {
        			return false;
        		}
        		return true;
            }
            @Override
            protected void onPostExecute(Boolean result) {
            	if(result) {
            		/*
            		 * In foreground
            		 */
            		String dest = "";
            		if(mService.getDestination() != null) {
	            		dest = mService.getDestination().getLocation().getLatitude() + "," +
	            				mService.getDestination().getLocation().getLongitude();
            		}
            		
            		Element elem;

            		elem = mDoc.getElementById("dest");
            		elem.val(dest);

            		elem = mDoc.getElementById("distance");
            		elem.val("10");

            		elem = mDoc.getElementById("adults");
            		elem.val("2");

            		elem = mDoc.getElementById("children");
            		elem.val("2");

            		/*
            		 * Today
            		 */
            		Date date = new Date(System.currentTimeMillis());

            		elem = mDoc.getElementById("startdate");
            		elem.val(new SimpleDateFormat("MM/dd/yyyy").format(date));

            		/*
            		 * Tomorrow
            		 */
            		date = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L);

            		elem = mDoc.getElementById("enddate");
            		elem.val(new SimpleDateFormat("MM/dd/yyyy").format(date));
            		
            		elem = mDoc.getElementById("rooms");
            		elem.val("1");

            		elem = mDoc.getElementById("starrating");
            		elem.val("3");

	    			ContentGenerator.this.setChanged();
	    			ContentGenerator.this.notifyObservers(mDoc.html());
            	}
            }
        };
        task.execute(page);
	}    
}
