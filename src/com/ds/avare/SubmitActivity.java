/*
Copyright (c) 2014, Apps4Av Inc. (apps4av.com) 
All rights reserved.
*/

package com.ds.avare;

import com.ds.avare.R;
import com.ds.avare.message.Helper;
import com.ds.avare.message.Logger;
import com.ds.avare.message.NetworkHelper;
import com.ds.avare.utils.PossibleEmail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
 
/**
 * 
 * @author zkhan
 *
 */
public class SubmitActivity extends Activity {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    
    // Types of submits
    public static final String SUBMIT = "submit";
    public static final int FUEL = 1;
	public static final int RATINGS = 2;
    public static final String FUEL_AIRPORT = "FUEL_AIRPORT";
	public static final String RATINGS_AIRPORT = "RATINGS_AIRPORT";
    
    static AsyncTask<Void, Void, Boolean> mSubmitTask = null;

    // Submit button
    private Button mSubmitButton;
    View mView;
    
    /**
     * 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        com.ds.avare.utils.Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        // Check if Internet present
        if (!Helper.isNetworkAvailable(this)) {
            Helper.showAlert(SubmitActivity.this,
                    getString(R.string.error),
                    getString(R.string.error_internet));
            return;
        }

        // Check if email
        if(PossibleEmail.get(this) == null) {
            Helper.showAlert(SubmitActivity.this,
                    getString(R.string.error),
                    getString(R.string.error_email));
            return;            
        }

        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Bundle bundle = getIntent().getExtras();
        final int submitCode = bundle.getInt(SUBMIT);
        switch(submitCode) {
        	case FUEL:
        		mView = layoutInflater.inflate(R.layout.fuel_submit, null);
                setContentView(mView);
                mSubmitButton = (Button) findViewById(R.id.fuel_submit_button_submit);
                mSubmitButton.setText(getString(R.string.Report) + "(" + PossibleEmail.get(this) + ")");
                TextView tv = (TextView) findViewById(R.id.fuel_submit_log);
                TextView airporttv = (TextView) findViewById(R.id.fuel_submit_airport);
                airporttv.setText(bundle.getString(FUEL_AIRPORT));
                Logger.setTextView(tv);
        		break;
        	case RATINGS:
        		mView = layoutInflater.inflate(R.layout.comments_submit, null);
                setContentView(mView);
                mSubmitButton = (Button) findViewById(R.id.comments_submit_button_submit);
                mSubmitButton.setText(getString(R.string.Report) + "(" + PossibleEmail.get(this) + ")");
                tv = (TextView) findViewById(R.id.comments_submit_log);
                airporttv = (TextView) findViewById(R.id.comments_submit_airport);
                airporttv.setText(bundle.getString(RATINGS_AIRPORT));
                Logger.setTextView(tv);
        		break;
        	default:
        		return;
        }
        
        /*
         * Click event on submit button
         *
         */
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {
                if(mSubmitTask != null) {
                    if(mSubmitTask.getStatus() != AsyncTask.Status.FINISHED) {
                        mSubmitTask.cancel(true);
                    }
                }
                
                mSubmitTask = new AsyncTask<Void, Void, Boolean>() {

                    String code = "";
                    String serverUrl = "";
                    Map<String, String> params = new HashMap<String, String>();
                    @Override
                    protected Boolean doInBackground(Void... vals) {
                        
                        if(!setup()) {
                        	return false;
                        }
                        Random random = new Random();
                        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                                        
                        // Once GCM returns a registration id, we need to register on our server
                        // As the server might be down, we will retry it a couple
                        // times.
                        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                            try {
                            	publishProgress();
                                code = NetworkHelper.post(serverUrl, params);
                                return true;
                            } 
                            catch (Exception e) {
                            }
                            // Here we are simplifying and retrying on any error; in a real
                            // application, it should retry only on unrecoverable errors
                            // (like HTTP error code 503).
                            if (i == MAX_ATTEMPTS) {
                                break;
                            }
                            try {
                                Thread.sleep(backoff);
                            }
                            catch (InterruptedException e1) {
                                // Activity finished before we complete - exit.
                                Thread.currentThread().interrupt();
                                break;
                            }
                            backoff *= 2;
                        }
                        return false;
                    }

                    
					@Override
                    protected void onProgressUpdate(Void... progress) {     
                    	Logger.Logit(getString(R.string.Trying));
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if(result) {
                            Logger.clear();
                            Logger.Logit(code);
                        }
                        else {
                            Logger.Logit(getString(R.string.Failed));                    
                        }
                    }

                    private boolean setup() {
						switch(submitCode) {
							case FUEL:
								serverUrl = NetworkHelper.getServer() + "fuel.php";
		                        params.put("email", PossibleEmail.get(getApplicationContext()));
		                        params.put("airport", ((TextView)mView.findViewById(R.id.fuel_submit_airport)).getText().toString());
		                        String price = ((EditText)mView.findViewById(R.id.fuel_submit_price)).getText().toString();
		                        try {
		                        	float ptest = Float.parseFloat(price);
			                        if(ptest <= 0) {
			                        	Logger.Logit(getString(R.string.InvalidPrice));
			                        	return false;
			                        }
		                        }
		                        catch (Exception e) {
		                        	Logger.Logit(getString(R.string.InvalidPrice));
		                        	return false;
		                        }
		                        params.put("price", price);
		                        
		                        params.put("fueltype", ((Spinner)mView.findViewById(R.id.fuel_submit_fueltype)).getSelectedItem().toString());
		                        
		                        String fbo = ((EditText)mView.findViewById(R.id.fuel_submit_fbo)).getText().toString();
		                        if(fbo.equals("")) {
		                        	Logger.Logit(getString(R.string.InvalidFBO));
		                        	return false;		                        	
		                        }
		                        params.put("fbo", fbo);
		                        
		                        return true;
							case RATINGS:
								serverUrl = NetworkHelper.getServer() + "ratings.php";
		                        params.put("email", PossibleEmail.get(getApplicationContext()));
		                        params.put("airport", ((TextView)mView.findViewById(R.id.comments_submit_airport)).getText().toString());
		                        String comments = ((EditText)mView.findViewById(R.id.comments_submit_comments)).getText().toString();
		                        if(comments.length() < 30) {
		                        	Logger.Logit(getString(R.string.InvalidComments));
		                        	return false;		                        	
		                        }
		                        params.put("comments", comments);
		                        
		                        int stars = (int)((RatingBar)mView.findViewById(R.id.comments_submit_ratingbar)).getRating();
		                        params.put("stars", stars + "");
		                        
		                        return true;
						}
						return false;
					}

                };
                mSubmitTask.execute(null, null, null);
            }
        });
        

    }
    
}