package com.ds.avare;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ListView;

import com.ds.avare.gps.GpsInterface;
import com.ds.avare.utils.Helper;


public class DebugTabActivity extends Activity {

    private StorageService mService;
    private Context mContext;
    private TextView mTraceLog;
    private ListView mConfigItems;

    private String[] mDebugOptions = {
            "Debug option 1",
            "Debug option 2",
            "Debug option 3",
            "Debug option 4",
            "Debug option 5",
            "Debug option 6",
            "Debug option 7",
            "Debug option 8",
            "Debug option 9",
            "Debug option 10"
    };

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
        }

        @Override
        public void timeoutCallback(boolean timeout) {
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        mContext = this;
        mService = null;

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.debug, null);

        mTraceLog = view.findViewById(R.id.trace_log);
        mTraceLog.setText("This is a test");
        setContentView(view);

        mConfigItems = view.findViewById(R.id.config_items);

        int nDebugOptions = mDebugOptions.length;
        CheckBox[] debugItems = new CheckBox[nDebugOptions];
        for(int x = 0; x < nDebugOptions; x++) {
            debugItems[x] = new CheckBox(mContext);
            debugItems[x].setText(mDebugOptions[x]);
        }
        final ArrayAdapter<CheckBox> configAdapter = new ArrayAdapter<CheckBox>(this, android.R.layout.simple_list_item_1, android.R.id.checkbox, debugItems);
        mConfigItems.setAdapter(configAdapter);
        mConfigItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
            mService.registerGpsListener(mGpsInfc);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        Helper.setOrientationAndOn(this);

        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        getApplicationContext().unbindService(mConnection);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
