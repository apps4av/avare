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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ds.avare.MainActivity;
import com.ds.avare.R;
import com.ds.avare.adsb.Traffic;
import com.ds.avare.gps.GpsParams;
import com.ds.avare.place.Obstacle;
import com.ds.avare.shapes.SubTile;
import com.ds.avare.shapes.Tile;
import com.ds.avare.storage.Preferences;
import com.ds.avare.threed.AreaMapper;
import com.ds.avare.threed.TerrainRenderer;
import com.ds.avare.threed.data.Vector4d;
import com.ds.avare.threed.objects.Map;
import com.ds.avare.utils.BitmapHolder;
import com.ds.avare.utils.GenericCallback;
import com.ds.avare.utils.Helper;
import com.ds.avare.views.GlassView;
import com.ds.avare.views.ThreeDSurfaceView;

import java.util.LinkedList;

/**
 * @author zkhan
 */
public class ThreeDFragment extends StorageServiceGpsListenerFragment {

    public static final String TAG = "ThreeDFragment";

    private AreaMapper mAreaMapper;

    private ImageButton mCenterButton;
    private Button mDrawerButton;
    private TextView mText;
    private AppCompatSpinner mChartSpinnerNav;
    private AppCompatSpinner mChartSpinnerBar;

    private GlassView mGlassView;

    private Vector4d mObstacles[];

    /**
     * For performing periodic activities.
     */
    private Location mLocation;
    private long mTime;

    private BitmapHolder mTempBitmap;
    private short[] mVertices;

    /**
     * Hold a reference to our GLSurfaceView
     */
    private ThreeDSurfaceView mGlSurfaceView;
    private TerrainRenderer mRenderer = null;

    // This task loads bitmaps and makes elevation vertices in background
    private AsyncTask<Object, Void, Float> mLoadTask;

    private static final int MESSAGE_INIT = 0;
    private static final int MESSAGE_TEXT = 1;
    private static final int MESSAGE_ERROR = 2;
    private static final int MESSAGE_AGL = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.threed, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mGlSurfaceView = (ThreeDSurfaceView) view.findViewById(R.id.threed_surface);

        mGlassView = (GlassView) view.findViewById(R.id.threed_overlay_view);

        // Check if the system supports OpenGL ES 2.0.
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
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
            mRenderer = new TerrainRenderer(getContext(), new GenericCallback() {
                @Override
                public Object callback(Object o, Object o1) {

                    /*
                     * This runs in opengl thread context.
                     */
                    if (((String) o1).equals(TerrainRenderer.SURFACE_CREATED)) {
                        // cannot call widgets from opengl thread so handler
                        mHandler.sendEmptyMessage(MESSAGE_INIT);

                        mAreaMapper = new AreaMapper();

                        mTime = System.currentTimeMillis();
                    }
                    else if (((String) o1).equals(TerrainRenderer.DRAW_FRAME)) {

                        // Do heavy load stuff every second
                        if ((System.currentTimeMillis() - 1000) > mTime) {

                            Location location = null;
                            // Simulate destination in sim mode and get altitude from terrain
                            if (mPref.isSimulationMode() && mService != null) {
                                Location l = new Location("");
                                if(mService.getDestination() != null) {
                                    l = mService.getDestination().getLocation();
                                }
                                else if(mService.getGpsParams() != null) {
                                    l.setLatitude(mService.getGpsParams().getLatitude());
                                    l.setLongitude(mService.getGpsParams().getLongitude());
                                    l.setBearing((float) mService.getGpsParams().getBearing());
                                }
                                l.setAltitude(Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_SLOPE / 2.0 +  // give margin for rounding in chart so we dont go underground
                                        getElevation(l.getLongitude(), l.getLatitude()) / Preferences.heightConversion);
                                location = l;
                            }
                            else {
                                synchronized (ThreeDFragment.this) {
                                    if(mLocation != null) {
                                        location = new Location(mLocation);
                                    }
                                }
                            }

                            if(location != null) {
                                mAreaMapper.setGpsParams(new GpsParams(location));
                                double lon = mAreaMapper.getGpsParams().getLongitude();
                                double lat = mAreaMapper.getGpsParams().getLatitude();
                                double alt = mAreaMapper.getGpsParams().getAltitude();

                                /**
                                 * Elevation here
                                 */
                                double elev = getElevation(lon, lat);
                                if (elev > Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT - 1) {
                                    // Write out AGL
                                    Message m = mHandler.obtainMessage();
                                    m.what = MESSAGE_AGL;
                                    m.obj = Math.round(alt - elev) + "ft";
                                    mHandler.sendMessage(m);
                                }

                                SubTile tm;
                                SubTile te;

                                /*
                                 * Set tiles on new location.
                                 * Match so that elevation and map tiles have common level
                                 */
                                int mZoomM = Tile.getMaxZoom(getContext(), mPref.getChartType3D());
                                int mZoomE = Tile.getMaxZoom(getContext(), "6");  // 6 is elevation tile index
                                if (mZoomE > mZoomM) {
                                    tm = new SubTile(getContext(), mPref, lon, lat, 0, mPref.getChartType3D());
                                    te = new SubTile(getContext(), mPref, lon, lat, mZoomE - mZoomM, "6"); // lower res elev tile
                                }
                                else {
                                    tm = new SubTile(getContext(), mPref, lon, lat, mZoomM - mZoomE, mPref.getChartType3D()); // lower res map tile
                                    te = new SubTile(getContext(), mPref, lon, lat, 0, "6");
                                }

                                mAreaMapper.setMapTile(tm);
                                mAreaMapper.setElevationTile(te);

                                if (mAreaMapper.isMapTileNew() || mAreaMapper.isElevationTileNew()) {

                                    if(mLoadTask != null) {
                                        if(mLoadTask.getStatus() == AsyncTask.Status.RUNNING) {
                                            mLoadTask.cancel(false);
                                        }
                                    }

                                    mLoadTask = new AsyncTask<Object, Void, Float>() {

                                        @Override
                                        protected Float doInBackground(Object... params) {
                                            // load tiles for elevation
                                            if(mTempBitmap != null) {
                                                mTempBitmap.recycle();
                                            }
                                            mTempBitmap = new BitmapHolder(SubTile.DIM, SubTile.DIM);
                                            if(!mAreaMapper.getElevationTile().load(mTempBitmap, mPref.mapsFolder())) {
                                                mVertices = null;
                                            }
                                            else {
                                                mVertices = Map.genTerrainFromBitmap(mTempBitmap.getBitmap());
                                            }
                                            // load tiles for map/texture
                                            if(mPref.getChartType3D().equals("6")) {
                                                // Show palette when elevation is chosen for height guidance
                                                mAreaMapper.getMapTile(); // clear flag
                                                mTempBitmap = new BitmapHolder(getContext(), R.drawable.palette);
                                                mRenderer.setAltitude((float)Helper.findPixelFromElevation((float)mAreaMapper.getGpsParams().getAltitude()));
                                            }
                                            else {
                                                if(!mAreaMapper.getMapTile().load(mTempBitmap, mPref.mapsFolder())) {
                                                    mTempBitmap.recycle();
                                                }
                                                mRenderer.setAltitude(256); // this tells shader to skip palette for texture
                                            }
                                            return (Float)mAreaMapper.getTerrainRatio();
                                        }

                                        @Override
                                        protected void onPreExecute () {
                                            // Show we are loading new data
                                            Message m = mHandler.obtainMessage();
                                            m.obj = getContext().getString(R.string.LoadingMaps);
                                            m.what = MESSAGE_TEXT;
                                            mHandler.sendMessage(m);
                                        }

                                        @Override
                                        protected void onPostExecute(Float arg) {
                                            final float ratio = arg;
                                            // Tell GL that new stuff is ready which will be loaded in Runnable
                                            mGlSurfaceView.queueEvent(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mRenderer.setTexture(mTempBitmap);
                                                            mRenderer.setTerrain(mVertices, ratio);
                                                            // show errors or success
                                                            Message m = mHandler.obtainMessage();
                                                            m.what = MESSAGE_TEXT;
                                                            if (!mRenderer.isMapSet()) {
                                                                m.obj = getContext().getString(R.string.MissingElevation);
                                                            } else if (!mRenderer.isTextureSet()) {
                                                                m.obj = getContext().getString(R.string.MissingMaps);
                                                            } else {
                                                                m.obj = getContext().getString(R.string.Ready);
                                                            }
                                                            mHandler.sendMessage(m);
                                                        }
                                                    });
                                        }
                                    };
                                    mLoadTask.execute();
                                }
                            }

                            if(mPref.getChartType3D().equals("6")) {
                                // Show palette when elevation is chosen for height guidance
                                mRenderer.setAltitude((float)Helper.findPixelFromElevation((float)mAreaMapper.getGpsParams().getAltitude()));
                            }

                            // Draw traffic
                            Traffic.draw(mService, mAreaMapper, mRenderer);

                            // Draw obstacles
                            if(mService != null) {
                                LinkedList<Obstacle> obs = mService.getObstacles();
                                if (null != obs) {

                                    Vector4d obstacles[] = new Vector4d[obs.size()];
                                    int count = 0;
                                    for (Obstacle ob : obs) {
                                        obstacles[count++] = mAreaMapper.gpsToAxis(ob.getLongitude(), ob.getLatitude(), ob.getHeight(), 0);
                                    }

                                    if (obstacles != null && obstacles.length != 0) {
                                        mRenderer.setObstacles(obstacles);
                                    }
                                }
                            }

                            // Our position
                            mRenderer.setOwnShip(mAreaMapper.getSelfLocation());

                            // For one second run
                            mTime = System.currentTimeMillis();
                        }
                    }


                    // Set orientation
                    mRenderer.getOrientation().set(mGlSurfaceView);
                    // tell renderer that we have new area
                    mRenderer.getCamera().set(mAreaMapper, mRenderer.getOrientation());

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
            showSnackbar("This device does not support OpenGL ES 2.0.", Snackbar.LENGTH_SHORT);
        }

        mAreaMapper = new AreaMapper();

        mCenterButton = (ImageButton) view.findViewById(R.id.threed_button_center);
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

        MenuItem chartMenuItem = ((MainActivity) getActivity()).getNavigationMenu().findItem(R.id.nav_action_threed_chart);
        mChartSpinnerNav = (AppCompatSpinner) MenuItemCompat.getActionView(chartMenuItem);
        setupChartSpinner(
                mChartSpinnerNav,
                Integer.valueOf(mPref.getChartType3D()),
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mPref.setChartType3D(String.valueOf(position));
                        mChartSpinnerBar.setSelection(position, false);
                        closeDrawer();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                }
        );

        mDrawerButton = (Button) view.findViewById(R.id.threed_button_drawer);
        mDrawerButton.getBackground().setAlpha(255);
        mDrawerButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity) getActivity()).getDrawerLayout().openDrawer(Gravity.LEFT);
                    }
                }
        );
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.toolbar_threed_menu, menu);

        MenuItem chartItem = menu.findItem(R.id.action_chart);

        mChartSpinnerBar = (AppCompatSpinner) MenuItemCompat.getActionView(chartItem);
        setupChartSpinner(
                mChartSpinnerBar,
                Integer.valueOf(mPref.getChartType3D()),
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mPref.setChartType3D(String.valueOf(position));
                        mChartSpinnerNav.setSelection(position, false);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        // Clean messages
        mText.setText("");
        mGlassView.setStatus(null);
        mGlassView.setAgl("");

        if (mRenderer != null) {
            mGlSurfaceView.onResume();
        }

        setDrawerButtonVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mRenderer != null) {
            mGlSurfaceView.onPause();
        }
    }

    @Override
    protected void postServiceConnected() {
        mGlassView.setService(mService);
    }

    @Override
    protected void onGpsLocation(Location location) {
        synchronized (ThreeDFragment.this) {
            mLocation = location;
        }
    }

    @Override
    protected void onGpsTimeout(boolean timeout) {
        if (mPref.isSimulationMode()) {
            Message m = mHandler.obtainMessage();
            m.what = MESSAGE_ERROR;
            m.obj = getString(R.string.SimulationMode);
            mHandler.sendMessage(m);
        } else if (timeout) {
            Message m = mHandler.obtainMessage();
            m.what = MESSAGE_ERROR;
            m.obj = getString(R.string.GPSLost);
            mHandler.sendMessage(m);
        }
    }

    public void setDrawerButtonVisibility() {
        mDrawerButton.setVisibility(mPref.getHideToolbar() ? View.VISIBLE : View.INVISIBLE);
    }

    private void setCenterButton() {
        // Button colors to be synced across activities
        if (mPref.isFirstPerson()) {
            mCenterButton.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
            showSnackbar(getString(R.string.FirstPerson), Snackbar.LENGTH_SHORT);
            mRenderer.getCamera().setFirstPerson(true);
            mGlSurfaceView.init();
        } else {
            mCenterButton.getBackground().setColorFilter(0xFF444444, PorterDuff.Mode.MULTIPLY);
            showSnackbar(getString(R.string.BirdEye), Snackbar.LENGTH_SHORT);
            mRenderer.getCamera().setFirstPerson(false);
            mGlSurfaceView.init();
        }
    }

    /**
     * Get elevation at this location
     * @return
     */
    private double getElevation(double lon, double lat) {
        int x = (int)mAreaMapper.getXForLon(lon);
        int y = (int)mAreaMapper.getYForLat(lat);
        // Find from Map
        double elev = mRenderer.getElevationNormalized(y, x, mAreaMapper.getTerrainRatio());
        if(elev <= -1) {
            return Helper.ALTITUDE_FT_ELEVATION_PER_PIXEL_INTERCEPT - 1;
        }

        return Helper.findElevationFromNormalizedElevation(elev);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_INIT) {
                setCenterButton();
            }
            else if (msg.what == MESSAGE_TEXT) {
                mText.setText((String) msg.obj);
            }
            else if (msg.what == MESSAGE_ERROR) {
                mGlassView.setStatus((String) msg.obj);
            }
            else if (msg.what == MESSAGE_AGL) {
                mGlassView.setAgl((String) msg.obj);
            }
        }
    };

}
