package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.LocationFragment;
import com.ds.avare.fragment.ThreeDFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class ThreeDNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected void removeFragments(FragmentManager fm, FragmentTransaction ft) {
        super.removeFragments(fm, ft);
        removeFragmentIfExists(fm, ft, LocationFragment.TAG);
    }

    @Override
    protected String getFragmentTag() {
        return ThreeDFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new ThreeDFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_THREE_D;
    }

}
