package com.ds.avare.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.ds.avare.R;

/**
 * Created by zkhan on 9/1/16.
 */
public class DecoratedAlertDialogBuilder extends AlertDialog.Builder {


    public DecoratedAlertDialogBuilder(Context context) {
        super(context);
    }

    @Override
    public AlertDialog create() {
        final AlertDialog dialog = super.create();
        //2. now setup to change color of the button
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.button_bg);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.button_bg);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(R.drawable.button_bg);
            }
        });

        return dialog;
    }

}
