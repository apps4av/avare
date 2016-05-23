/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ConfigurationInfo;
import android.graphics.PorterDuff;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsInterface;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.shapes.Tile;
import com.ds.avare.storage.Preferences;
import com.ds.avare.threed.AreaMapper;
import com.ds.avare.threed.TerrainRenderer;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.OptionButton;
import com.ds.avare.views.ThreeDSurfaceView;

/**
 * @author zkhan
 */
public class ThreeDActivity extends Activity {

    /**
     * Service that keeps state even when activity is dead
     */
    private StorageService mService;

    /**
     * App preferences
     */
    private Preferences mPref;

    private Toast mToast;

    private Context mContext;

    private AreaMapper mAreaMapper;

    private boolean mReady;

    private int mFrames;

    private Button mCenterButton;
    private TextView mText;
    private TextView mTextError;

    private OptionButton mChartOption;

    /**
     * Hold a reference to our GLSurfaceView
     */
    private ThreeDSurfaceView mGlSurfaceView;
    private TerrainRenderer mRenderer = null;

    private static final int MESSAGE_INIT = 0;
    private static final int MESSAGE_TEXT = 1;
    private static final int MESSAGE_ERROR = 2;

    private void setLocation(Location location) {
        mAreaMapper.setGpsParams(new GpsParams(location));

        /*
         * Elevation tile
         */
        Tile t = new Tile(mContext, mPref, location.getLongitude(), location.getLatitude());
        Tile t2 = new Tile(mContext, mPref, location.getLongitude(), location.getLatitude(), true);
        mAreaMapper.setElevationTile(t);
        mAreaMapper.setMapTile(t2);
    }

    private GpsInterface mGpsInfc = new GpsInterface() {

        @Override
        public void statusCallback(GpsStatus gpsStatus) {
        }

        @Override
        public void locationCallback(Location location) {
            if (location != null && mService != null && mReady) {

                /*
                 * Called by GPS. Update everything driven by GPS.
                 */
                setLocation(location);
            }
        }

        @Override
        public void timeoutCallback(boolean timeout) {
            if(mPref.isSimulationMode()) {
                Message m = mHandler.obtainMessage();
                m.what = MESSAGE_ERROR;
                m.obj = getString(R.string.SimulationMode);
                mHandler.sendMessage(m);
            }
            else if(timeout) {
                Message m = mHandler.obtainMessage();
                m.what = MESSAGE_ERROR;
                m.obj = getString(R.string.GPSLost);
                mHandler.sendMessage(m);
            }
        }

        @Override
        public void enabledCallback(boolean enabled) {
        }

    };

    /**
     *
     */
    private void setCenterButton() {
        // Button colors to be synced across activities
        if (mPref.isFirstPerson()) {
            mCenterButton.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
            mToast.setText(getString(R.string.FirstPerson));
            mRenderer.getCamera().setFirstPerson(true);
            mGlSurfaceView.init();
        }
        else {
            mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
            mToast.setText(getString(R.string.BirdEye));
            mRenderer.getCamera().setFirstPerson(false);
            mGlSurfaceView.init();
        }
        mToast.show();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        ((MainActivity) this.getParent()).showMapTab();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mPref = new Preferences(this);

        mContext = this;

        mReady = false;
        mFrames = 0; // drawn frames

        /*
         * Create toast beforehand so multiple clicks dont throw up a new toast
         */
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.threed, null);
        setContentView(view);

        mGlSurfaceView = (ThreeDSurfaceView) view.findViewById(R.id.threed_surface);

        // Check if the system supports OpenGL ES 2.0.
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager
                .getDeviceConfigurationInfo();
        // Even though the latest emulator supports OpenGL ES 2.0,
        // it has a bug where it doesn't set the reqGlEsVersion so
        // the above check doesn't work. The below will detect if the
        // app is running on an emulator, and assume that it supports
        // OpenGL ES 2.0.
        final boolean supportsEs2 =
                configurationInfo.reqGlEsVersion >= 0x20000
                        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")));

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGlSurfaceView.setEGLContextClientVersion(2);
            //r,g,b,a,depth,stencil
            mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

            // Assign our renderer.
            mRenderer = new TerrainRenderer(this, new GenericCallback() {
                @Override
                public Object callback(Object o, Object o1) {
                    if (((String) o1).equals(TerrainRenderer.SURFACE_CREATED)) {
                        // When ready, do stuff
                        mReady = true;
                        // cannot call widgets from opengl thread so handler
                        mHandler.sendEmptyMessage(MESSAGE_INIT);
                    } else if (((String) o1).equals(TerrainRenderer.DRAW_FRAME)) {

                        if (mAreaMapper.isMapTileNew() || mAreaMapper.isElevationTileNew()) {
                            Message m = mHandler.obtainMessage();
                            m.obj = mContext.getString(R.string.LoadingMaps);
                            m.what = MESSAGE_TEXT;
                            mHandler.sendMessage(m);

                            // load tiles but give feedback as it hangs
                            Tile tout = mAreaMapper.getMapTile();
                            mRenderer.setTexture(new BitmapHolder(mPref.mapsFolder() + "/" + tout.getName()));
                            tout = mAreaMapper.getElevationTile();
                            mRenderer.setTerrain(new BitmapHolder(mPref.mapsFolder() + "/" + tout.getName()));

                            // show errors
                            m = mHandler.obtainMessage();
                            m.what = MESSAGE_TEXT;
                            if (!mRenderer.isMapSet()) {
                                m.obj = mContext.getString(R.string.MissingElevation);
                            } else if (!mRenderer.isTextureSet()) {
                                m.obj = mContext.getString(R.string.MissingMaps);
                            } else {
                                m.obj = mContext.getString(R.string.Ready);
                            }

                            mHandler.sendMessage(m);
                        }

                        // tell renderer that we have new area
                        mRenderer.getCamera().set(mAreaMapper, mRenderer.getOrientation());
                        // Set orientation
                        mRenderer.getOrientation().set(mGlSurfaceView);

                        // Draw traffic every so many frames
                        if (mFrames++ % 60 == 0) {

                            if (mPref.isSimulationMode() && mService != null && mService.getDestination() != null) {
                                setLocation(mService.getDestination().getLocationWithAltitude());
                                // Simulate destination in sim mode
                            }

                            // Draw traffic
                            Traffic.draw(mService, mAreaMapper, mRenderer);
                        }
                    }
                    return null;
                }
            });

            // Set renderer
            mGlSurfaceView.setRenderer(mRenderer);
        } else {
            /*
             * This is where you could create an OpenGL ES 1.x compatible
             * renderer if you wanted to support both ES 1 and ES 2. Since
             * we're not doing anything, the app will crash if the device
             * doesn't support OpenGL ES 2.0. If we publish on the market, we
             * should also add the following to AndroidManifest.xml:
             *
             * <uses-feature android:glEsVersion="0x00020000"
             * android:required="true" />
             *
             * This hides our app from those devices which don't support OpenGL
             * ES 2.0.
             */
            mToast.setText("This device does not support OpenGL ES 2.0.");
            mToast.show();
            return;
        }

        mAreaMapper = new AreaMapper();

        mCenterButton = (Button) view.findViewById(R.id.threed_button_center);
        mCenterButton.getBackground().setAlpha(255);
        mCenterButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    // Orientation init
                    mGlSurfaceView.init();
                }
            }

        });

        mCenterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // long press on mCenterButton button sets first person toggle
                mPref.setFirstPerson(!mPref.isFirstPerson());
                setCenterButton();
                return true;
            }
        });

        mText = (TextView) view.findViewById(R.id.threed_text);
        mTextError = (TextView) view.findViewById(R.id.threed_text_error);

        // Charts different from main view
        mChartOption = (OptionButton) view.findViewById(R.id.threed_spinner_chart);
        mChartOption.setCallback(new GenericCallback() {
            @Override
            public Object callback(Object o, Object o1) {
                mPref.setChartType3D("" + (int) o1);
                return null;
            }
        });
        mChartOption.setCurrentSelectionIndex(Integer.parseInt(mPref.getChartType3D()));

    }


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
            mService.registerGpsListener(mGpsInfc);
            mAreaMapper = new AreaMapper();
        }

        /* (non-Javadoc)
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(this);

        /*
         * Registering our receiver
         * Bind now.
         */
        Intent intent = new Intent(this, StorageService.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (mRenderer != null) {
            mGlSurfaceView.onResume();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (null != mService) {
            mService.unregisterGpsListener(mGpsInfc);
        }

        /*
         * Clean up on pause that was started in on resume
         */
        getApplicationContext().unbindService(mConnection);

        if (mRenderer != null) {
            mGlSurfaceView.onPause();
        }
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MESSAGE_INIT) {
                setCenterButton();
            }
            else if(msg.what == MESSAGE_TEXT) {
                mText.setText((String) msg.obj);
            }
            else if(msg.what == MESSAGE_ERROR) {
                mTextError.setText((String) msg.obj);
            }
        }
    };
}