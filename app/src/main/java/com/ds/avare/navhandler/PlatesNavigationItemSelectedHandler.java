package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.PlatesFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class PlatesNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return PlatesFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new PlatesFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_PLATES;
    }

}
