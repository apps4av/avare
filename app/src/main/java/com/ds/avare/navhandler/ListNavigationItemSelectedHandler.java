package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;

import com.ds.avare.MainActivity;
import com.ds.avare.fragment.ChecklistFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class ListNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return ChecklistFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new ChecklistFragment();
    }

    @Override
    public int getNavItemIndex() {
        return MainActivity.NAV_ITEM_IDX_CHECKLIST;
    }

}
