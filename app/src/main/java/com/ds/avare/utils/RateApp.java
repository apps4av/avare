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
import android.content.Intent;
import android.net.Uri;

import com.ds.avare.R;
import com.ds.avare.storage.Preferences;


/**
 * Created by zkhan on 10/6/16.
 *
 * Ask for app rating on every so many launches
 */
public class RateApp {

    private static final int ASK_EVERY = 10;

    /**
     * Dialog to rate
     * @param ctx
     * @param pref
     */
    public static void rateIt(final Context ctx, final Preferences pref) {
        /*
         * Ask for rating every tenth time we come here
         */
        int count = pref.getRateAskCount();
        if(count >= 0) {
            count++;
            pref.setRateAskCount(count);
            // Ask every 10th time we come to download activity
            if(count % ASK_EVERY == 0) {
                DecoratedAlertDialogBuilder alert = new DecoratedAlertDialogBuilder(ctx);
                alert.setTitle(ctx.getString(R.string.RatingTitle));
                alert.setMessage(ctx.getString(R.string.RatingMessage));
                alert.setPositiveButton(ctx.getString(R.string.Rate), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ctx.getPackageName())));
                        pref.setRateAskCount(-1);
                        dialog.dismiss();
                    }
                });
                alert.setNeutralButton(ctx.getString(R.string.NeverAsk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pref.setRateAskCount(-1);
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton(ctx.getString(R.string.MaybeLater), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog d = alert.create();
                d.show();
            }
        }
    }
}
