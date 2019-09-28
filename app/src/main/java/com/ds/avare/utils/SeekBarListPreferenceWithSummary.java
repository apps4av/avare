package com.ds.avare.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ds.avare.R;

public class SeekBarListPreferenceWithSummary extends ListPreference implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final String androidns = "http://schemas.android.com/apk/res/android";
    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context mContext;
    private String mDialogMessage;
    private String mOriginalSummary;

    public SeekBarListPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        // Get string value for dialogMessage :
        int mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0) {
            mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        }
        else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        mOriginalSummary = super.getSummary().toString();
    }

    @Override
    protected View onCreateDialogView() {

        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        TextView splashText = new TextView(mContext);
        splashText.setPadding(30, 10, 30, 10);
        splashText.setTextColor(Color.WHITE);
        if (mDialogMessage != null) {
            splashText.setText(mDialogMessage);
        }
        layout.addView(splashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(TypedValue.COMPLEX_UNIT_PX, Helper.adjustTextSize(mContext, R.dimen.TextSize));
        mValueText.setTextColor(Color.WHITE);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setProgressBarValue();

        return layout;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // do not call super
    }

    private void setProgressBarValue() {
        String mValue = null;
        if (shouldPersist()) {
            mValue = getValue();
        }

        mSeekBar.setMax(this.getEntries().length - 1);
        mSeekBar.setProgress(this.findIndexOfValue(mValue));
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        setProgressBarValue();
    }

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        mValueText.setText(getEntryFromValue(value));
        mValueText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                Helper.adjustTextSize(mContext, R.dimen.TextSize, getEntryValueFromValue(value).toString()));
    }

    private CharSequence getEntryFromValue(int value) {
        CharSequence[] entries = getEntries();
        return value >= 0 && entries != null ? entries[value] : "";
    }

    private CharSequence getEntryValueFromValue(int value) {
        CharSequence[] entryValues = getEntryValues();
        return value >= 0 && entryValues != null ? entryValues[value] : "";
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void showDialog(Bundle state) {
        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (shouldPersist()) {
            setValueIndex(mSeekBar.getProgress());
        }
        getDialog().dismiss();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(mOriginalSummary + " (" + value + ")");
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(mOriginalSummary + " (" + getEntry() + ")");
    }
}