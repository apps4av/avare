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
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.RegisterActivity;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.place.Boundaries;
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
                startActivity(intent);
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
        // Clean up anything that was started in onResume
        getContext().unbindService(mConnection);
    }

    @Override
    public final void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            onHidden();
        } else {
            onUnhidden();
        }
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        return false;
    }

    protected void onUnhidden() {
        if (mService != null) {
            postServiceConnected();
        }
    }

    protected void showSnackbar(String message, int duration) {
        if (isVisible() && mCoordinatorLayout != null && mCoordinatorLayout.getContext() != null) {
            // only show snackbar if fragment is visible
            Snackbar.make(mCoordinatorLayout, message, duration).show();
        }
    }

    protected void setupChartSpinner(AppCompatSpinner spinner, int selectedPosition, AdapterView.OnItemSelectedListener selectedListener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, Boundaries.getChartTypes());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(selectedPosition, false);
        spinner.setOnItemSelectedListener(selectedListener);
    }

    protected void closeDrawer() {
        ((MainActivity) getActivity()).getDrawerLayout().closeDrawer(GravityCompat.START);
    }

    protected void postServiceConnected() { }
    protected void onGpsStatus(GpsStatus gpsStatus) { }
    protected void onGpsLocation(Location location) { }
    protected void onGpsTimeout(boolean timeout) { }
    protected void onGpsEnabled(boolean enabled) { }
    protected void onHidden() { }

}
