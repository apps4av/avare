package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.SatelliteFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class ToolsNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return SatelliteFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new SatelliteFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_TOOLS;
    }

}
