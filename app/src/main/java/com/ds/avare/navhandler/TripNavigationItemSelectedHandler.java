package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.fragment.TripFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class TripNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return TripFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new TripFragment();
    }

}
