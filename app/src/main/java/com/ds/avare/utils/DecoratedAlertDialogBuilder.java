/*
Copyright (c) 2016, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.Button;

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
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                // ensure button sizes match parent
                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                ViewGroup.LayoutParams positiveParams = positiveButton.getLayoutParams();
                positiveParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                positiveButton.setLayoutParams(positiveParams);

                Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                ViewGroup.LayoutParams negativeParams = negativeButton.getLayoutParams();
                negativeParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                negativeButton.setLayoutParams(negativeParams);

                Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                ViewGroup.LayoutParams neutralParams = neutralButton.getLayoutParams();
                neutralParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                neutralButton.setLayoutParams(neutralParams);
            }
        });

        return dialog;
    }

}
