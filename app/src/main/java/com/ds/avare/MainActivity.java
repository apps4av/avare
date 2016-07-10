/*
Copyright (c) 2012, Apps4Av Inc. (apps4av.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


package com.ds.avare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.ds.avare.fragment.LocationFragment;
import com.ds.avare.navhandler.AfdNavigationItemSelectedHandler;
import com.ds.avare.navhandler.FindNavigationItemSelectedHandler;
import com.ds.avare.navhandler.ListNavigationItemSelectedHandler;
import com.ds.avare.navhandler.MapNavigationItemSelectedHandler;
import com.ds.avare.navhandler.NavigationItemSelectedHandler;
import com.ds.avare.navhandler.NearNavigationItemSelectedHandler;
import com.ds.avare.navhandler.PlanNavigationItemSelectedHandler;
import com.ds.avare.navhandler.PlatesNavigationItemSelectedHandler;
import com.ds.avare.navhandler.ThreeDNavigationItemSelectedHandler;
import com.ds.avare.navhandler.ToolsNavigationItemSelectedHandler;
import com.ds.avare.navhandler.TripNavigationItemSelectedHandler;
import com.ds.avare.navhandler.WxbNavigationItemSelectedHandler;
import com.ds.avare.storage.Preferences;
import com.ds.avare.utils.Helper;
import com.ds.avare.utils.NetworkHelper;
import com.ds.avare.utils.ToolbarVisibilityListener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zkhan
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener {

    private static final String SELECTED_NAV_ITEM_IDX_KEY = "selectedNavItemIdx";

    private Preferences mPref;
    private NavigationView mNavigationView;
    private TabLayout mTabLayout;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private Map<Integer, Integer> mTabIndexToNavItemIdMap = new HashMap<>();
    private Map<String, ToolbarVisibilityListener> mToolbarVisibilityListeners = new HashMap<>();

    // Nav items that display in the left slide out drawer. These indexes need to be updated
    // if higher up menu items are added or deleted
    public static final int NAV_ITEM_IDX_MAP       = 6;
    public static final int NAV_ITEM_IDX_PLATES    = 7;
    public static final int NAV_ITEM_IDX_AFD       = 8;
    public static final int NAV_ITEM_IDX_FIND      = 9;
    public static final int NAV_ITEM_IDX_PLAN      = 10;
    public static final int NAV_ITEM_IDX_NEAR      = 11;
    public static final int NAV_ITEM_IDX_THREE_D   = 12;
    public static final int NAV_ITEM_IDX_CHECKLIST = 13;
    public static final int NAV_ITEM_IDX_WXB       = 14;
    public static final int NAV_ITEM_IDX_TRIP      = 15;
    public static final int NAV_ITEM_IDX_TOOLS     = 16;

    private static final Map<Integer, NavigationItemSelectedHandler> NAV_ITEM_HANDLERS = new HashMap<>();

    static {
        NAV_ITEM_HANDLERS.put(R.id.nav_map, new MapNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_plate, new PlatesNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_afd, new AfdNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_find, new FindNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_plan, new PlanNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_near, new NearNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_3d, new ThreeDNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_list, new ListNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_wxb, new WxbNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_trip, new TripNavigationItemSelectedHandler());
        NAV_ITEM_HANDLERS.put(R.id.nav_tools, new ToolsNavigationItemSelectedHandler());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Helper.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mPref = new Preferences(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        // set the back arrow in the toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        setupTabs(mTabLayout);
        mTabLayout.setOnTabSelectedListener(this);

        /*
         * Start service now, bind later. This will be no-op if service is already running
         */
        Intent intent = new Intent(this, StorageService.class);
        startService(intent);

        mTabLayout.setVisibility(mPref.getHideTabBar() ? View.GONE : View.VISIBLE);
        mToolbar.setVisibility(mPref.getHideToolbar() ? View.GONE : View.VISIBLE);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        int selectedNavItemIdx = (savedInstanceState == null)
                ? NAV_ITEM_IDX_MAP
                : savedInstanceState.getInt(SELECTED_NAV_ITEM_IDX_KEY, NAV_ITEM_IDX_MAP);
        onNavigationItemSelected(mNavigationView.getMenu().getItem(selectedNavItemIdx));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.Add, R.string.Add);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        TextView navHeaderText = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.nav_header_text);
        navHeaderText.setText(mPref.getRegisteredEmail());
    }

    private void setupTabs(TabLayout tabLayout) {
        int tabIndex = 0;

        tabLayout.addTab(tabLayout.newTab().setText(R.string.Main), tabIndex, true);
        mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_MAP);

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_PLATES))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plates), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_PLATES);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_AFD))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.AFD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_AFD);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_FIND))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Find), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_FIND);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_PLAN))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plan), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_PLAN);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_NEAR))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Near), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_NEAR);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_THREE_D))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.ThreeD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_THREE_D);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_CHECKLIST))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.List), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_CHECKLIST);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_WXB))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.WXB), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_WXB);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_TRIP))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Trip), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_TRIP);
        }

        if (0 != (mPref.getTabs() & (1 << MainActivity.NAV_ITEM_IDX_TOOLS))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Tools), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, NAV_ITEM_IDX_TOOLS);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (NAV_ITEM_HANDLERS.containsKey(itemId)) { // selected a tab and need to change the view
            NavigationItemSelectedHandler navItemHandler = NAV_ITEM_HANDLERS.get(itemId);

            navItemHandler.handleItemSelected(getSupportFragmentManager());

            // set nav item as selected
            if (mNavigationView != null) item.setChecked(true);

            // set tab item as selected
            if (mTabLayout != null) {
                for (Map.Entry<Integer, Integer> entries : mTabIndexToNavItemIdMap.entrySet()) {
                    if (entries.getValue() == navItemHandler.getNavItemIndex()) {
                        mTabLayout.getTabAt(entries.getKey()).select();
                        break;
                    }
                }
            }
        } else { // selected a non tab item
            if (itemId == R.id.nav_preferences) {
                startPreferencesActivity();
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_downloads) {
                startChartsDownloadActivity();
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_ads) {
                startAdsActivity();
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_help) {
                startHelpActivity();
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_toggle_toolbar) {
                mPref.setHideToolbar(!mPref.getHideToolbar());
                mToolbar.setVisibility(mPref.getHideToolbar() ? View.GONE : View.VISIBLE);
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                for (ToolbarVisibilityListener listener : mToolbarVisibilityListeners.values()) {
                    listener.onToolbarVisibilityChanged(mPref.getHideToolbar());
                }
                return true;
            } else if (itemId == R.id.nav_toggle_tabbar) {
                mPref.setHideTabBar(!mPref.getHideTabBar());
                mTabLayout.setVisibility(mPref.getHideTabBar() ? View.GONE : View.VISIBLE);
                if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        }

        if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    private void startPreferencesActivity() {
        startActivity(new Intent(this, PrefActivity.class));
    }

    private void startChartsDownloadActivity() {
        Intent i = new Intent(this, ChartsDownloadActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    private void startAdsActivity() {
        startActivity(new Intent(this, MessageActivity.class));
    }

    private void startHelpActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra("url", NetworkHelper.getHelpUrl(this));
        startActivity(intent);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int navItemIdx = mTabIndexToNavItemIdMap.get(tab.getPosition());
        onNavigationItemSelected(mNavigationView.getMenu().getItem(navItemIdx));
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { }

    @Override
    public void onBackPressed() {
        if (isMainNavItemSelected()) {
            if (getLocationFragment() != null) getLocationFragment().onBackPressed();
        } else {
            onNavigationItemSelected(mNavigationView.getMenu().getItem(NAV_ITEM_IDX_MAP));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavigationView != null) {
            outState.putInt(SELECTED_NAV_ITEM_IDX_KEY, getSelectedNavItemIdx());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Helper.setOrientationAndOn(this);
    }

    @Override
    public void onDestroy() {
        /*
         * Start service now, bind later. This will be no-op if service is already running
         */
        if (!mPref.shouldLeaveRunning()) {
            if (isFinishing()) {
                /*
                 * Do not kill on orientation change
                 */
                Intent intent = new Intent(this, StorageService.class);
                stopService(intent);
            }
        }
        super.onDestroy();
    }

    /**
     * For switching tab from any tab activity
     */
    private void switchView(int navItemIdx) {
        onNavigationItemSelected(mNavigationView.getMenu().getItem(navItemIdx));

        /*
         * Hide soft keyboard that may be open
         */
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTabLayout.getApplicationWindowToken(), 0);
    }

    /**
     * Display the main/maps tab
     */
    public void showMapView() {
        switchView(NAV_ITEM_IDX_MAP);
    }

    /**
     * Display the Plan tab
     */
    public void showPlanView() {
        switchView(NAV_ITEM_IDX_PLAN);
    }

    /**
     * Show the Plates view
     */
    public void showPlatesView() {
        switchView(NAV_ITEM_IDX_PLATES);
    }

    /**
     * Show the AFD view
     */
    public void showAfdView() {
        switchView(NAV_ITEM_IDX_AFD);
    }

    private LocationFragment getLocationFragment() {
        if (isMainNavItemSelected()) {
            return (LocationFragment) getSupportFragmentManager().findFragmentByTag(LocationFragment.TAG);
        }
        return null;
    }

    private boolean isMainNavItemSelected() {
        return mNavigationView.getMenu().getItem(NAV_ITEM_IDX_MAP).isChecked();
    }

    private int getSelectedNavItemIdx() {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            if (mNavigationView.getMenu().getItem(i).isChecked()) return i;
        }
        return 0;
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    public void addToolbarVisibilityListener(String key, ToolbarVisibilityListener listener) {
        mToolbarVisibilityListeners.put(key, listener);
    }

}