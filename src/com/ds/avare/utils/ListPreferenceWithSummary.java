package com.ds.avare.utils;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListPreferenceWithSummary extends ListPreference {

	public ListPreferenceWithSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListPreferenceWithSummary(Context context) {
		super(context);
	}

	@Override
	public void setValue(String value) {
		super.setValue(value);
		setSummary(value);
	}

	@Override
	public void setSummary(CharSequence summary) {
		super.setSummary(getEntry());
	}
}
