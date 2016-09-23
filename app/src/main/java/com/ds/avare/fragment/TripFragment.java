/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.ds.avare.R;
import com.ds.avare.utils.DecoratedAlertDialogBuilder;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.OptionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * @author zkhan trip / hotel / car etc. activity
 */
public class TripFragment extends StorageServiceGpsListenerFragment {

    public static final String TAG = "TripFragment";

    /**
     * This view display location on the map.
     */
    private WebView mWebView;
    private Button mFindButton;
    private AlertDialog mAlertDialog;
    private ProgressBar mProgress;
    private ProgressBar mProgressBar;
    private EditText mSearchText;
    private Button mNextButton;
    private Button mLastButton;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.trip, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        mProgress = (ProgressBar) view.findViewById(R.id.trip_load_progress);

        /*
         * Progress bar for search
         */
        mProgressBar = (ProgressBar) (view.findViewById(R.id.trip_progress_bar));

        mWebView = (WebView) view.findViewById(R.id.trip_mainpage);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mProgress.setVisibility(View.VISIBLE);

        mWebView.setWebViewClient(new WebViewClient() {

            /*
             * For hiding loading bar
             */
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgress.setVisibility(View.INVISIBLE);
            }

            /*
             * We let browser open the result page's deep links
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(browserIntent);
                }
                catch (Exception e) {

                }
                return true;
            }
        });


        /*
         * Show zoom
         */
        mWebView.getSettings().setBuiltInZoomControls(true);

        // This is need on some old phones to get focus back to webview.
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                arg0.performClick();
                arg0.requestFocus();
                return false;
            }
        });
        mWebView.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getContext(), "trip"));

        /*
         * This is hotel find button that has all the action from it
         */
        mFindButton = (Button) view.findViewById(R.id.trip_button_find);
        mFindButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mService != null) {
                    /*
                     * Reload
                     */
                    final View findView = getLayoutInflater(savedInstanceState).inflate(R.layout.hotel, null);

                    // arrival and departure date obtain from today and tomorrow
                    final EditText from = (EditText)findView.findViewById(R.id.hotel_datefrom_text);
                    final EditText to = (EditText)findView.findViewById(R.id.hotel_dateto_text);
                    final OptionButton price = (OptionButton)findView.findViewById(R.id.hotel_price);
                    final OptionButton stars = (OptionButton)findView.findViewById(R.id.hotel_stars);
                    final OptionButton distance = (OptionButton)findView.findViewById(R.id.hotel_radius);
                    final OptionButton adults = (OptionButton)findView.findViewById(R.id.hotel_adults);
                    final OptionButton child1 = (OptionButton)findView.findViewById(R.id.hotel_child_1);
                    final OptionButton child2 = (OptionButton)findView.findViewById(R.id.hotel_child_2);
                    final OptionButton child3 = (OptionButton)findView.findViewById(R.id.hotel_child_3);
                    final OptionButton child4 = (OptionButton)findView.findViewById(R.id.hotel_child_4);
                    final OptionButton child5 = (OptionButton)findView.findViewById(R.id.hotel_child_5);
                    /*
                     * Others obtain from preferences
                     */
                    price.setCurrentSelectionIndex(mPref.getHotelMaxPriceIndex());
                    stars.setCurrentSelectionIndex(mPref.getHotelMinStarIndex());
                    distance.setCurrentSelectionIndex(mPref.getHotelMaxDistanceIndex());
                    adults.setCurrentSelectionIndex(mPref.getHotelAdultsIndex());
                    child1.setCurrentSelectionIndex(mPref.getHotelChildIndex("1"));
                    child2.setCurrentSelectionIndex(mPref.getHotelChildIndex("2"));
                    child3.setCurrentSelectionIndex(mPref.getHotelChildIndex("3"));
                    child4.setCurrentSelectionIndex(mPref.getHotelChildIndex("4"));
                    child5.setCurrentSelectionIndex(mPref.getHotelChildIndex("5"));

                    Calendar now = Calendar.getInstance(); // Current time
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    from.setText(sdf.format(now.getTime())); // Get Date String according to date format

                    now.add(Calendar.DAY_OF_MONTH, 1);
                    to.setText(sdf.format(now.getTime()));

                    DecoratedAlertDialogBuilder askd = new DecoratedAlertDialogBuilder(getContext());
                    askd.setView(findView);
                    askd.setCancelable(false);
                    mAlertDialog = askd.create();

                    /*
                     * Cancel last progress
                     */
                    mProgress.setVisibility(View.INVISIBLE);

                    /*
                     * Find button on dialog
                     */
                    mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.Find), new DialogInterface.OnClickListener() {
                        /* (non-Javadoc)
                         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            /*
                             * Make a URL from stuff
                             */
                            String url = makeURL(from, to, price, stars, distance,
                                    adults, child1, child2, child3, child4, child5);
                            if(url != null) {
                                /*
                                 * Show loading progress for page in case internet is slow, it can take time
                                 */
                                mProgress.setVisibility(View.VISIBLE);
                                mWebView.loadUrl(url);
                            }
                            else {
                                mProgress.setVisibility(View.VISIBLE);
                                mWebView.loadUrl(com.ds.avare.utils.Helper.getWebViewFile(getContext(), "trip"));
                            }
                            dialog.dismiss();
                        }
                    });
                    mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    mAlertDialog.show();

                }
            }

        });

        /*
         * For searching, start search on every new key press
         */
        mSearchText = (EditText) view.findViewById(R.id.trip_edit_text);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int after) {

                /*
                 * If text is 0 length or too long, then do not search, show last list
                 */
                if(s.length() < 3) {
                    mWebView.clearMatches();
                    return;
                }

                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mWebView.findAll(s.toString());
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            }
        });

        mNextButton = (Button) view.findViewById(R.id.trip_button_next);
        mNextButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(true);
            }

        });

        mLastButton = (Button) view.findViewById(R.id.trip_button_last);
        mLastButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mWebView.findNext(false);
            }

        });
    }

    private boolean isDateWrong(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        sdf.setLenient(false);

        try {
            //if not valid, it will throw ParseException
            sdf.parse(date);
        }
        catch (Exception e) {
            return true;
        }
        return false;
    }

    /**
     * Make a URL from view
     */
    private String makeURL(EditText from, EditText to, OptionButton price, OptionButton stars, OptionButton distance,
                           OptionButton adults, OptionButton child1, OptionButton child2, OptionButton child3, OptionButton child4, OptionButton child5) {
    	/*
    	 * Get all the user input and make a URL
    	 */
        String fromtext = from.getText().toString();
        String totext = to.getText().toString();

    	/*
    	 * Set Dest from other screens
    	 */
        if(mService == null || mService.getDestination() == null) {
            showSnackbar(getString(R.string.ValidDest), Snackbar.LENGTH_SHORT);
            return null;
        }

    	/*
    	 * Validate date
    	 */
        if(isDateWrong(fromtext) || isDateWrong(totext)) {
            showSnackbar(getString(R.string.IncorrectDateFormat), Snackbar.LENGTH_SHORT);
            return null;
        }

    	/*
    	 * Validate coordinates
    	 */
        if(Helper.isLatitudeSane(mService.getDestination().getLocation().getLatitude()) &&
                Helper.isLongitudeSane(mService.getDestination().getLocation().getLongitude())) {

    		/*
    		 * Children are comma separated
    		 */
            String children = "";
            children +=	child1.getCurrentIndex() == 0 ? "" : child1.getCurrentValue() + ",";
            children += child2.getCurrentIndex() == 0 ? "" : child2.getCurrentValue().toString() + ",";
            children += child3.getCurrentIndex() == 0 ? "" : child3.getCurrentValue().toString() + ",";
            children += child4.getCurrentIndex() == 0 ? "" : child4.getCurrentValue().toString() + ",";
            children += child5.getCurrentIndex() == 0 ? "" : child5.getCurrentValue().toString() + ",";
            children = children.replaceAll(",$", "");
            if(children.length() > 0) {
                children = "&children=" + children;
            }

    		/*
    		 * URL as the PHP needs
    		 */
            String url = "https://apps4av.net/expedia.php?"
                    + "latitude=" + mService.getDestination().getLocation().getLatitude() + "&"
                    + "longitude=" + mService.getDestination().getLocation().getLongitude() + "&"
                    + "radius=" + distance.getCurrentValue() + "&"
                    + "minstar=" + stars.getCurrentValue() + "&"
                    + "maxrate=" + price.getCurrentValue() + "&"
                    + "adults=" + adults.getCurrentValue() + "&"
                    + "arrival=" + fromtext + "&"
                    + "departure=" + totext +
                    children;


    		/*
    		 * Save user preferences
    		 */
            mPref.setHotelMaxPriceIndex(price.getCurrentIndex());
            mPref.setHotelMinStarIndex(stars.getCurrentIndex());
            mPref.setHotelMaxDistanceIndex(distance.getCurrentIndex());
            mPref.setHotelAdultsIndex(adults.getCurrentIndex());
            mPref.setHotelChildIndex("1", child1.getCurrentIndex());
            mPref.setHotelChildIndex("2", child2.getCurrentIndex());
            mPref.setHotelChildIndex("3", child3.getCurrentIndex());
            mPref.setHotelChildIndex("4", child4.getCurrentIndex());
            mPref.setHotelChildIndex("5", child5.getCurrentIndex());
            return url;
        }

        showSnackbar(getString(R.string.ValidDest), Snackbar.LENGTH_SHORT);

        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.requestFocus();
    }

    @Override
    public void onPause() {
        super.onPause();

        try { mAlertDialog.dismiss(); }
        catch(Exception e) { }
    }

}

