package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.AirportFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class AfdNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return AirportFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new AirportFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_AFD;
    }

}
