package com.ds.avare.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

/*
 * This class causes the file media to be re-scanned. This lets any new files
 * show up in a browser when connected via USB.
 */
public class SingleMediaScanner implements MediaScannerConnectionClient { 
    private MediaScannerConnection mMs; 
    private String mPath; 
    public SingleMediaScanner(Context context, String fullPathToFile) { 
        mPath = fullPathToFile; 
        mMs = new MediaScannerConnection(context, this); 
        mMs.connect(); 
    } 

    @Override 
    public void onMediaScannerConnected() { 
        mMs.scanFile(mPath, null); 
    } 

	@Override
	public void onScanCompleted(String arg0, Uri arg1) {
		mMs.disconnect();
	} 
}
