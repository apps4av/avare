package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.fragment.PfdFragment;

/**
 * Created by roleary on 12/27/2016.
 */

public class PfdNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return PfdFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new PfdFragment();
    }

}
