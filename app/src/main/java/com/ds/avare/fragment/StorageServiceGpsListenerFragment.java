package com.ds.avare.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;

import com.ds.avare.BuildConfig;
import com.ds.avare.R;
import com.ds.avare.RegisterActivity;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.storage.Preferences;

/**
 * Created by arabbani on 7/9/16.
 */
public abstract class StorageServiceGpsListenerFragment extends Fragment {

    // Service that keeps state even when activity is dead
    protected StorageService mService;

    // App preferences
    protected Preferences mPref;

    // View container for Snackbars
    private CoordinatorLayout mCoordinatorLayout;

    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (!mPref.isRegistered()) {
                Intent intent = new Intent(getContext(), RegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                // don't require registration when running debug apk
                if (!BuildConfig.DEBUG) startActivity(intent);
            }

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mService = ((StorageService.LocalBinder) service).getService();
            mService.registerGpsListener(mGpsInfc);

            postServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg) { }
    };

    private GpsInterface mGpsInfc = new GpsInterface() {
        @Override
        public void statusCallback(GpsStatus gpsStatus) {
            onGpsStatus(gpsStatus);
        }

        @Override
        public void locationCallback(Location location) {
            onGpsLocation(location);
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            onGpsTimeout(timeout);
        }

        @Override
        public void enabledCallback(boolean enabled) {
            onGpsEnabled(enabled);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mService = null;
        mPref = new Preferences(getContext());
        mCoordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Registering our receiver. Bind now.
        Intent intent = new Intent(getContext(), StorageService.class);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mService != null) mService.unregisterGpsListener(mGpsInfc);
        // Clean up on pause that was started in on resume
        getContext().unbindService(mConnection);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && mService != null) postServiceConnected();
    }

    protected void showSnackbar(String message, int duration) {
        if (isVisible()) {
            // only show snackbar if fragment is visible
            Snackbar.make(mCoordinatorLayout, message, duration).show();
        }
    }

    protected void postServiceConnected() { }
    protected void onGpsStatus(GpsStatus gpsStatus) { }
    protected void onGpsLocation(Location location) { }
    protected void onGpsTimeout(boolean timeout) { }
    protected void onGpsEnabled(boolean enabled) { }

}
