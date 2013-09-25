package com.ds.avare.utils;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditTextPreferenceWithSummary extends EditTextPreference {

	public EditTextPreferenceWithSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextPreferenceWithSummary(Context context) {
		super(context);
	}

	@Override
	public void setText(String value) {
		super.setText(value);
		setSummary(value);
	}

	@Override
	public void setSummary(CharSequence summary) {
		super.setSummary(getText());
	}
}
