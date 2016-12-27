package com.ds.avare.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.ds.avare.R;
import com.ds.avare.utils.FolderPreferenceDialogFragment;
import com.ds.avare.utils.RegisterActivityPreferenceDialogFragment;
import com.ds.avare.utils.SyncActivityPreferenceDialogFragment;

/**
 * Created by arabbani on 6/23/16.
 */
public class PreferenceFragment extends PreferenceFragmentCompat {

    public static final String TAG = "PreferenceFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        super.onNavigateToScreen(preferenceScreen);
        setPreferenceScreen(preferenceScreen);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey().equals(getString(R.string.RegisterActivityPreferenceKey))) {
            DialogFragment fragment = RegisterActivityPreferenceDialogFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else if (preference.getKey().equals(getString(R.string.SyncActivityPreferenceKey))) {
            DialogFragment fragment = SyncActivityPreferenceDialogFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else if (preference.getKey().equals(getString(R.string.Maps))) {
            DialogFragment fragment = FolderPreferenceDialogFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else if (preference.getKey().equals(getString(R.string.UDWLocation))) {
            DialogFragment fragment = FolderPreferenceDialogFragment.newInstance(preference);
            fragment.setTargetFragment(this, 0);
            fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

}
