package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.fragment.SearchFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class FindNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return SearchFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new SearchFragment();
    }

}
