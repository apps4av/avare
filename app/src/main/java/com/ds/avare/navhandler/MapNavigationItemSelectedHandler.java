package com.ds.avare.navhandler;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.LocationFragment;
import com.ds.avare.fragment.ThreeDFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class MapNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected void removeFragments(FragmentManager fm, FragmentTransaction ft) {
        super.removeFragments(fm, ft);
        removeFragmentIfExists(fm, ft, ThreeDFragment.TAG);
    }

    @Override
    protected String getFragmentTag() {
        return LocationFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new LocationFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_MAP;
    }

}
