package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.ds.avare.R;
import com.ds.avare.fragment.SatelliteFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public abstract class NavigationItemSelectedHandler {

    public void handleItemSelected(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();

        removeFragments(fm, ft);
        hideFragments(fm, ft);

        Fragment existingFragment = fm.findFragmentByTag(getFragmentTag());
        if (existingFragment == null) {
            ft.add(R.id.fragment_container, getNewFragment(), getFragmentTag());
        } else {
            ft.show(existingFragment);
        }

        ft.commit();
    }

    private void hideFragments(FragmentManager fm, FragmentTransaction ft) {
        // hide any existing fragments that are visible (should only ever be one)
        if (fm.getFragments() != null) {
            for (Fragment fragment : fm.getFragments()) {
                if (fragment != null) ft.hide(fragment);
            }
        }
    }

    protected void removeFragments(FragmentManager fm, FragmentTransaction ft) {
        // remove the satellite fragment, no need to keep it around
        removeFragmentIfExists(fm, ft, SatelliteFragment.TAG);
    }

    protected void removeFragmentIfExists(FragmentManager fm, FragmentTransaction ft, String tag) {
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment != null) ft.remove(fragment);
    }

    public abstract int getNavItemIndex();

    protected abstract String getFragmentTag();
    protected abstract Fragment getNewFragment();

}
