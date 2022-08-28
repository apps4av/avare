package com.ds.avare.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ds.avare.touch.LongTouchDestination;
import com.ds.avare.webinfc.WebAppMapInterface;

import java.util.LinkedList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DestinationAlertDialog extends AlertDialog {

    private WebAppMapInterface mInfc;
    private GenericCallback mCb;
    private Context mContext;
    public void setCallback(GenericCallback cb) {
        mCb = cb;
    }



    public void setData(LongTouchDestination data) {
        if(null != mInfc) {
            mInfc.setData(data);
        }
    }

    public DestinationAlertDialog(Context context) {
        super(context);
        setup(context);
    }

    void setup(Context ctx) {
        mContext = ctx;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*
         * Make a dialog to show destination info, when long pressed on it
         */
        WebView wv = new WebView(mContext);
        wv.loadUrl("file:///android_asset/map.html");

        mInfc = new WebAppMapInterface(mContext, wv, new GenericCallback() {
            /*
             * (non-Javadoc)
             * @see com.ds.avare.utils.GenericCallback#callback(java.lang.Object)
             */
            @Override
            public Object callback(Object o, Object o1) {

                if(((String)o).equals("X")) {
                    dismiss();
                }
                else if(null == mCb) {
                } else {
                    mCb.callback(o, o1);
                }
                return null;
            }
        });
        wv.addJavascriptInterface(mInfc, "AndroidMap");

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                return true;
            }
        });

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setBuiltInZoomControls(false);
        // This is need on some old phones to get focus back to webview.
        wv.setFocusable(true);
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                arg0.performClick();
                arg0.requestFocus();
                return false;
            }
        });
        // Do not let selecting text
        wv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        wv.setLongClickable(false);

        setView(wv);

    }

    @Override
    public void show() {
        super.show();
        // make it full size
        getWindow().setLayout(MATCH_PARENT, MATCH_PARENT);
    }
}
