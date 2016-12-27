package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.fragment.NearestFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class NearNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return NearestFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new NearestFragment();
    }

}
