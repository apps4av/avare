package com.ds.avare.navhandler;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;

import com.ds.avare.R;
import com.ds.avare.fragment.SatelliteFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by arabbani on 7/9/16.
 */
public abstract class NavigationItemSelectedHandler {

    private final List<Integer> navMenuActionGroupIds = Arrays.asList(R.id.nav_menu_map_actions_group, R.id.nav_menu_threed_actions_group);

    public void handleItemSelected(FragmentManager fm, Menu menu) {
        FragmentTransaction ft = fm.beginTransaction();

        Set<String> tagsToRemove = getTagsToRemove();
        Set<String> tagsToHide = getTagsToHide(fm);

        // don't call hide on the fragments that will be removed, can cause issues with fragment lifecycle
        tagsToHide.removeAll(tagsToRemove);

        removeFragments(fm, ft, tagsToRemove);
        hideFragments(fm, ft, tagsToHide);

        Fragment existingFragment = fm.findFragmentByTag(getFragmentTag());
        if (existingFragment == null) {
            ft.add(R.id.fragment_container, getNewFragment(), getFragmentTag());
        } else {
            ft.show(existingFragment);
        }

        ft.commit();

        setupNavigationMenuItems(menu);
    }

    private void removeFragments(FragmentManager fm, FragmentTransaction ft, Set<String> tagsToRemove) {
        for (String tag : tagsToRemove) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment != null) {
                ft.remove(fragment);
            }
        }
    }

    private void hideFragments(FragmentManager fm, FragmentTransaction ft, Set<String> tagsToHide) {
        for (String tag : tagsToHide) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment != null) {
                ft.hide(fragment);
            }
        }
    }

    private void setupNavigationMenuItems(Menu menu) {
        for (Integer groupId : navMenuActionGroupIds) {
            menu.setGroupVisible(groupId, false);
        }
        menu.setGroupVisible(getNavigationMenuGroupId(), true);
    }

    protected int getNavigationMenuGroupId() {
        return Menu.NONE;
    }

    protected Set<String> getTagsToRemove() {
        Set<String> tags = new HashSet<>();
        tags.add(SatelliteFragment.TAG);
        return tags;
    }

    protected Set<String> getTagsToHide(FragmentManager fm) {
        Set<String> tags = new HashSet<>();
        if (fm.getFragments() != null) {
            for (Fragment fragment : fm.getFragments()) {
                if (fragment != null) {
                    tags.add(fragment.getTag());
                }
            }
        }
        return tags;
    }

    protected abstract String getFragmentTag();
    protected abstract Fragment getNewFragment();

}
