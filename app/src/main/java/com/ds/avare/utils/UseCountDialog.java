/*
Copyright (c) 2026, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.ds.avare.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;

import com.ds.avare.ProActivity;
import com.ds.avare.R;
import com.ds.avare.storage.Preferences;

/**
 * Soft paywall shown at app start to non-subscribers.
 *
 * Behaviour:
 *  - increments the per-install use count,
 *  - asks RevenueCat whether the "Pro" entitlement is active,
 *  - if entitled: does nothing (no dialog),
 *  - otherwise: shows a one-button dialog whose "Continue" button is
 *    disabled for {@code useCount} seconds (i.e. 1 second per app launch,
 *    capped at {@link #MAX_WAIT_SECONDS}). The dialog also offers a
 *    "Subscribe" button that opens the RevenueCat paywall.
 *
 * The whole thing is fail-open: any error path silently dismisses without
 * blocking the user.
 */
public final class UseCountDialog {

    /** Hard cap on the countdown so power users aren't stuck for a minute+. */
    public static final int MAX_WAIT_SECONDS = 30;

    private UseCountDialog() { }

    /**
     * Increment use count, query RevenueCat, and show the gating dialog
     * if the user is not entitled. Safe to call from any activity.
     */
    public static void maybeShow(final Activity activity, final Preferences pref) {
        if (activity == null || pref == null || activity.isFinishing()) {
            return;
        }

        final int useCount = Math.min(pref.getUseCount() + 1, Integer.MAX_VALUE - 1);
        pref.setUseCount(useCount);

        RevenueCatService.isProEntitled(new RevenueCatService.EntitlementCallback() {
            @Override
            public void onResult(boolean entitled) {
                if (entitled) {
                    return;
                }
                if (activity.isFinishing()) {
                    return;
                }
                showCountdownDialog(activity, useCount);
            }
        });
    }

    private static void showCountdownDialog(final Activity activity, int useCount) {
        final int seconds = Math.max(1, Math.min(useCount, MAX_WAIT_SECONDS));

        DecoratedAlertDialogBuilder builder = new DecoratedAlertDialogBuilder(activity);
        builder.setTitle(activity.getString(R.string.ProServicesTitle));
        builder.setMessage(activity.getString(R.string.ProServicesMessage, seconds));
        builder.setCancelable(false);

        builder.setPositiveButton(
                activity.getString(R.string.ProServicesContinueCountdown, seconds),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Optional "Subscribe" path — only meaningful if RevenueCat is wired up.
        if (RevenueCatService.isConfigured()) {
            builder.setNegativeButton(
                    activity.getString(R.string.ProServicesSubscribe),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            try {
                                activity.startActivity(
                                        new Intent(activity, ProActivity.class));
                            } catch (Throwable ignored) {
                                // ignore — Pro screen is optional
                            }
                        }
                    });
        }

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Disable Continue and count down 1s per recorded launch.
        final android.widget.Button positive =
                dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positive == null) {
            return;
        }
        positive.setEnabled(false);

        new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!dialog.isShowing()) {
                    cancel();
                    return;
                }
                int remaining = (int) Math.ceil(millisUntilFinished / 1000.0);
                positive.setText(activity.getString(
                        R.string.ProServicesContinueCountdown, remaining));
            }

            @Override
            public void onFinish() {
                if (!dialog.isShowing()) {
                    return;
                }
                positive.setEnabled(true);
                positive.setText(activity.getString(R.string.ProServicesContinue));
            }
        }.start();
    }
}
