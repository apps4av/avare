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
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ds.avare.storage.Preferences;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Reads {@code appNotices} and shows each pending message in a dialog.
 *
 * Failures (offline, rules, Firebase not configured) are ignored. A missing
 * message must not block the map.
 */
public final class AppNoticeService {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "app-notices");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final AppNotice.TimeReader FIRESTORE_TIME = new AppNotice.TimeReader() {
        @Override
        public Date read(Object value) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toDate();
            }
            return null;
        }
    };

    private AppNoticeService() {
    }

    /**
     * Shows pending notices one at a time. Each is remembered only after the
     * pilot taps OK and the screen is still open. {@code onFinished} runs on
     * the main thread when the sequence ends, including when nothing is shown.
     */
    public static void present(final Activity activity, final Preferences preferences, final Runnable onFinished) {
        if (activity == null || preferences == null || !isAlive(activity)) {
            finish(activity, onFinished);
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppNotice> notices;
                try {
                    notices = fetchPending(activity, preferences.dismissedAppNoticeKeys());
                } catch (Throwable ignored) {
                    finish(activity, onFinished);
                    return;
                }
                runOnActivity(activity, new Runnable() {
                    @Override
                    public void run() {
                        if (!isAlive(activity)) {
                            finish(activity, onFinished);
                            return;
                        }
                        showAt(activity, preferences, notices, 0, onFinished);
                    }
                });
            }
        });
    }

    static List<AppNotice> fetchPending(Context context, Set<String> dismissed) throws Exception {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return new ArrayList<AppNotice>();
        }
        QuerySnapshot snap = Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection(AppNotice.COLLECTION)
                        .whereEqualTo("active", true)
                        .limit(20)
                        .get(),
                8,
                TimeUnit.SECONDS);
        List<AppNotice> notices = new ArrayList<AppNotice>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            AppNotice notice = AppNotice.tryParse(doc.getId(), doc.getData(), FIRESTORE_TIME);
            if (notice != null) {
                notices.add(notice);
            }
        }
        return AppNotice.pendingOf(notices, new Date(), dismissed);
    }

    private static void showAt(final Activity activity, final Preferences preferences,
                               final List<AppNotice> notices, final int index, final Runnable onFinished) {
        if (index >= notices.size() || !isAlive(activity)) {
            finish(activity, onFinished);
            return;
        }
        final AppNotice notice = notices.get(index);
        final boolean[] accepted = new boolean[]{false};

        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
        ScrollView scroll = new ScrollView(activity);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(pad, pad, pad, 0);

        TextView body = new TextView(activity);
        body.setText(notice.body);
        column.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (notice.url != null) {
            TextView link = new TextView(activity);
            link.setText(notice.url);
            link.setTextIsSelectable(true);
            link.setPadding(0, pad, 0, 0);
            column.addView(link, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(notice.title);
        builder.setView(scroll);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                accepted[0] = true;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (accepted[0] && isAlive(activity)) {
                    preferences.dismissAppNotice(notice.getDismissKey());
                    showAt(activity, preferences, notices, index + 1, onFinished);
                } else {
                    finish(activity, onFinished);
                }
            }
        });
        try {
            dialog.show();
        } catch (Throwable ignored) {
            finish(activity, onFinished);
        }
    }

    private static boolean isAlive(Activity activity) {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void finish(Activity activity, final Runnable onFinished) {
        if (onFinished == null) {
            return;
        }
        if (activity == null) {
            onFinished.run();
            return;
        }
        runOnActivity(activity, onFinished);
    }

    private static void runOnActivity(Activity activity, Runnable action) {
        try {
            activity.runOnUiThread(action);
        } catch (Throwable ignored) {
            // The screen is gone. Leave the message undisplayed.
        }
    }
}
