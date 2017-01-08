package com.ds.avare.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.orientation.OrientationInterface;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.PfdView;
import com.ds.avare.webinfc.WebAppInterface;

/**
 * Created by roleary on 12/27/2016.
 */

public class PfdFragment extends StorageServiceGpsListenerFragment {
    public static final String TAG = "WeatherFragment";

    private PfdView mPfdView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pfd, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mPfdView = (PfdView) view.findViewById(R.id.pfd_view);
    }

    @Override
    public void onGpsLocation(Location location) {
        if(mService == null) {
            return;
        }
        if(mService.getGpsParams() == null || mService.getExtendedGpsParams() == null) {
            return;
        }
        double bearing = 0;
        double cdi = 0;
        double vdi = 0;

        if(mService.getVNAV() != null) {
            vdi = mService.getVNAV().getGlideSlope();
        }
        if(mService.getCDI() != null) {
            cdi = mService.getCDI().getDeviation();
            if (!mService.getCDI().isLeft()) {
                cdi = -cdi;
            }
        }
        if(mService.getDestination() != null) {
            bearing = mService.getDestination().getBearing();
        }
        mPfdView.setParams(mService.getGpsParams(), mService.getExtendedGpsParams(), bearing, cdi, vdi);
        mPfdView.postInvalidate();
    }

    private OrientationInterface mOrientationInfc = new OrientationInterface() {

        @Override
        public void onSensorChanged(double yaw, double pitch, double roll, double acceleration) {
            mPfdView.setPitch(-(float)pitch);
            mPfdView.setRoll(-(float)roll);
            mPfdView.setYaw((float)yaw);
            mPfdView.setAcceleration(acceleration);
            mPfdView.postInvalidate();
        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    /**
     *
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            /*
             * We've bound to LocalService, cast the IBinder and get LocalService instance
             */
            StorageService.LocalBinder binder = (StorageService.LocalBinder) service;
            mService = binder.getService();
            mService.registerOrientationListener(mOrientationInfc);
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        if (null != mService) {
            mService.unregisterOrientationListener(mOrientationInfc);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(this.getActivity());

        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(getContext(), StorageService.class);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}
