package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.PlanFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class PlanNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return PlanFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new PlanFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_PLAN;
    }

}
