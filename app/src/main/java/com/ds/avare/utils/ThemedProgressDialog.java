package com.ds.avare.utils;

import android.app.ProgressDialog;
import android.content.Context;

import com.ds.avare.R;

public class ThemedProgressDialog extends ProgressDialog {

    public ThemedProgressDialog(Context context) {
        super(context, R.style.Theme_Dialog);
    }

}
