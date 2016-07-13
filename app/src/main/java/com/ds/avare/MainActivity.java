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
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.ds.avare.fragment.LocationFragment;
import com.ds.avare.fragment.StorageServiceGpsListenerFragment;
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

    private static final String SELECTED_NAV_ITEM_ID_KEY = "selectedNavItemId";

    private Preferences mPref;
    private NavigationView mNavigationView;
    private TabLayout mTabLayout;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private AppCompatCheckBox mShowToolbarCheckBox;
    private AppCompatCheckBox mShowTabbarCheckBox;

    private Map<Integer, Integer> mTabIndexToNavItemIdMap = new HashMap<>();
    private Map<String, ToolbarVisibilityListener> mToolbarVisibilityListeners = new HashMap<>();

    // Tab panels that can display at the bottom of the screen. Each one
    // except tabMain is configurable on or off by the user.
    public static final int TAB_MAIN      = 0;
    public static final int TAB_PLATES    = 1;
    public static final int TAB_AFD       = 2;
    public static final int TAB_FIND      = 3;
    public static final int TAB_PLAN      = 4;
    public static final int TAB_NEAR      = 5;
    public static final int TAB_THREE_D   = 6;
    public static final int TAB_CHECKLIST = 7;
    public static final int TAB_WXB       = 8;
    public static final int TAB_TRIP      = 9;
    public static final int TAB_TOOLS     = 10;

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
        int selectedNavItemId = (savedInstanceState == null)
                ? R.id.nav_map
                : savedInstanceState.getInt(SELECTED_NAV_ITEM_ID_KEY, R.id.nav_map);
        onNavigationItemSelected(mNavigationView.getMenu().findItem(selectedNavItemId));

        MenuItem showToolbarItem = mNavigationView.getMenu().findItem(R.id.nav_toggle_toolbar);
        mShowToolbarCheckBox = (AppCompatCheckBox) MenuItemCompat.getActionView(showToolbarItem);
        mShowToolbarCheckBox.setChecked(!mPref.getHideToolbar());
        mShowToolbarCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPref.setHideToolbar(!mPref.getHideToolbar());
                mToolbar.setVisibility(mPref.getHideToolbar() ? View.GONE : View.VISIBLE);
                for (ToolbarVisibilityListener listener : mToolbarVisibilityListeners.values()) {
                    listener.onToolbarVisibilityChanged(mPref.getHideToolbar());
                }
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        MenuItem showTabbarItem = mNavigationView.getMenu().findItem(R.id.nav_toggle_tabbar);
        mShowTabbarCheckBox = (AppCompatCheckBox) MenuItemCompat.getActionView(showTabbarItem);
        mShowTabbarCheckBox.setChecked(!mPref.getHideTabBar());
        mShowTabbarCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPref.setHideTabBar(!mPref.getHideTabBar());
                mTabLayout.setVisibility(mPref.getHideTabBar() ? View.GONE : View.VISIBLE);
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        /*
        MenuItem chartItem = mNavigationView.getMenu().findItem(R.id.nav_action_chart);
        AppCompatSpinner chartSpinner = (AppCompatSpinner) MenuItemCompat.getActionView(chartItem);
        getLocationFragment().setupChartSpinner(chartSpinner);

        ArrayAdapter<String> chartAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Boundaries.getChartTypes());
        chartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartSpinner.setAdapter(chartAdapter);
        chartSpinner.setSelection(Integer.valueOf(mPref.getChartType()), false);

        chartSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mPref.setChartType(String.valueOf(position));
                        getLocationFragment().reloadLocationView();
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                }
        );
        */

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.Add, R.string.Add);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        TextView navHeaderText = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.nav_header_text);
        navHeaderText.setText(mPref.getRegisteredEmail());
    }

    private void setupTabs(TabLayout tabLayout) {
        int tabIndex = 0;

        tabLayout.addTab(tabLayout.newTab().setText(R.string.Main), tabIndex, true);
        mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_map);

        if (0 != (mPref.getTabs() & (1 << TAB_PLATES))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plates), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_plate);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_AFD))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.AFD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_afd);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_FIND))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Find), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_find);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_PLAN))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Plan), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_plan);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_NEAR))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Near), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_near);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_THREE_D))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.ThreeD), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_3d);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_CHECKLIST))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.List), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_list);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_WXB))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.WXB), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_wxb);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_TRIP))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Trip), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_trip);
        }

        if (0 != (mPref.getTabs() & (1 << TAB_TOOLS))) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.Tools), tabIndex, false);
            mTabIndexToNavItemIdMap.put(tabIndex++, R.id.nav_tools);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == getSelectedNavItemId()) return true;

        if (NAV_ITEM_HANDLERS.containsKey(itemId)) { // selected a view change item
            selectNavItem(itemId);

            // set tab item as selected
            if (mTabLayout != null) {
                for (Map.Entry<Integer, Integer> entries : mTabIndexToNavItemIdMap.entrySet()) {
                    if (itemId == entries.getValue()) {
                        if (!mTabLayout.getTabAt(entries.getKey()).isSelected()) {
                            mTabLayout.getTabAt(entries.getKey()).select();
                        }
                        break;
                    }
                }
            }

            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else { // selected a non view change item
            if (itemId == R.id.nav_preferences) {
                startPreferencesActivity();
            } else if (itemId == R.id.nav_downloads) {
                startChartsDownloadActivity();
            } else if (itemId == R.id.nav_ads) {
                startAdsActivity();
            } else if (itemId == R.id.nav_help) {
                startHelpActivity();
            } else if (itemId == R.id.nav_toggle_toolbar) {
                mShowToolbarCheckBox.setChecked(!mShowToolbarCheckBox.isChecked());
            } else if (itemId == R.id.nav_toggle_tabbar) {
                mShowTabbarCheckBox.setChecked(!mShowTabbarCheckBox.isChecked());
            } else if (item.getGroupId() == R.id.nav_menu_map_actions_group
                    || item.getGroupId() == R.id.nav_menu_threed_actions_group) {
                // delegate to fragment onNavigationItemSelected
                StorageServiceGpsListenerFragment fragment = getVisibleFragment();
                if (fragment != null) {
                    return fragment.onNavigationItemSelected(item);
                }
            }

            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int navItemId = mTabIndexToNavItemIdMap.get(tab.getPosition());
        selectNavItem(navItemId);
    }

    private void selectNavItem(int navItemId) {
        MenuItem item = mNavigationView.getMenu().findItem(navItemId);

        if (item.isChecked()) return;

        NavigationItemSelectedHandler navItemHandler = NAV_ITEM_HANDLERS.get(navItemId);
        navItemHandler.handleItemSelected(getSupportFragmentManager());
        item.setChecked(true); // set nav item as selected
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { }

    @Override
    public void onBackPressed() {
        if (isMainNavItemSelected()) {
            if (getLocationFragment() != null) {
                getLocationFragment().onBackPressed();
            }
        } else {
            onNavigationItemSelected(mNavigationView.getMenu().findItem(R.id.nav_map));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavigationView != null) {
            outState.putInt(SELECTED_NAV_ITEM_ID_KEY, getSelectedNavItemId());
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

    /**
     * For switching tab from any tab activity
     */
    private void switchView(int navItemId) {
        onNavigationItemSelected(mNavigationView.getMenu().findItem(navItemId));

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
        switchView(R.id.nav_map);
    }

    /**
     * Display the Plan tab
     */
    public void showPlanView() {
        switchView(R.id.nav_plan);
    }

    /**
     * Show the Plates view
     */
    public void showPlatesView() {
        switchView(R.id.nav_plate);
    }

    /**
     * Show the AFD view
     */
    public void showAfdView() {
        switchView(R.id.nav_afd);
    }

    private LocationFragment getLocationFragment() {
        if (isMainNavItemSelected()) {
            return (LocationFragment) getSupportFragmentManager().findFragmentByTag(LocationFragment.TAG);
        }
        return null;
    }

    private StorageServiceGpsListenerFragment getVisibleFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isVisible()) {
                return (StorageServiceGpsListenerFragment) fragment;
            }
        }
        return null;
    }

    private boolean isMainNavItemSelected() {
        return mNavigationView.getMenu().findItem(R.id.nav_map).isChecked();
    }

    private int getSelectedNavItemId() {
        for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
            if (mNavigationView.getMenu().getItem(i).isChecked()) {
                return mNavigationView.getMenu().getItem(i).getItemId();
            }
        }
        return -1;
    }

    public void addToolbarVisibilityListener(String key, ToolbarVisibilityListener listener) {
        mToolbarVisibilityListeners.put(key, listener);
    }

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    public Menu getNavigationMenu() {
        return mNavigationView.getMenu();
    }

}