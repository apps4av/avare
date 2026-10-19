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
import android.widget.TextView;

import com.ds.avare.R;
import com.ds.avare.StorageService;
import com.ds.avare.storage.Preferences;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Reads {@code appNotices} and shows each pending message in a dialog.
 *
 * Failures (offline, rules, App Check) are ignored. A missing message must
 * not block the map. The Firestore read is given 8 seconds, then dropped.
 */
public final class AppNoticeService {

    /** Firestore {@code (default)} database, same as AvareX. */
    public static final String COLLECTION = "appNotices";
    private static final int DISMISSED_CAP = 40;

    private static final AppNoticeService INSTANCE = new AppNoticeService();

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "app-notices");
            thread.setDaemon(true);
            return thread;
        }
    });

    private AppNoticeService() {
    }

    public static AppNoticeService getInstance() {
        return INSTANCE;
    }

    /**
     * Shows pending notices one at a time. Each is remembered only after the
     * pilot taps OK and the screen is still open. {@code onDone} runs on the
     * main thread when the check is finished, including when nothing is shown.
     */
    public void present(final Activity activity, final Runnable onDone) {
        if (activity.isFinishing()) {
            finish(activity, onDone);
            return;
        }
        final Set<String> dismissed = dismissedKeys();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppNotice> notices;
                try {
                    notices = fetchPending(dismissed, new Date());
                } catch (Throwable ignored) {
                    if (ignored instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    finish(activity, onDone);
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        show(activity, notices, 0, onDone);
                    }
                });
            }
        });
    }

    List<AppNotice> fetchPending(Set<String> dismissed, Date now) throws Exception {
        QuerySnapshot snap = Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection(COLLECTION)
                        .whereEqualTo("active", true)
                        .limit(20)
                        .get(),
                8,
                TimeUnit.SECONDS);
        List<AppNotice> notices = new ArrayList<AppNotice>();
        for (QueryDocumentSnapshot doc : snap) {
            AppNotice notice = AppNotice.tryParse(doc.getId(), doc.getData(), new AppNotice.TimeSource() {
                @Override
                public Date read(Object value) {
                    if (value instanceof Timestamp) {
                        return ((Timestamp) value).toDate();
                    }
                    return null;
                }
            });
            if (notice != null) {
                notices.add(notice);
            }
        }
        return AppNotice.pendingOf(notices, now, dismissed, AppNotice.DEFAULT_LIMIT);
    }

    private void show(final Activity activity, final List<AppNotice> notices,
                      final int index, final Runnable onDone) {
        if (index >= notices.size() || activity.isFinishing() || activity.isDestroyed()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        final AppNotice notice = notices.get(index);
        String message = notice.getBody();
        if (notice.getUrl() != null) {
            message = message + "\n\n" + notice.getUrl();
        }
        try {
            AlertDialog dialog = new DecoratedAlertDialogBuilder(activity)
                    .setTitle(notice.getTitle())
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(activity.getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss(notice.getDismissKey());
                            show(activity, notices, index + 1, onDone);
                        }
                    })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextIsSelectable(true);
            }
        } catch (Throwable ignored) {
            if (onDone != null) {
                onDone.run();
            }
        }
    }

    private static Preferences preferences() {
        try {
            return StorageService.getInstance().getPreferences();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Set<String> dismissedKeys() {
        Set<String> keys = new HashSet<String>();
        Preferences preferences = preferences();
        if (preferences == null) {
            return keys;
        }
        String raw = preferences.getDismissedAppNotices();
        if (raw == null || raw.length() == 0) {
            return keys;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                keys.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return keys;
    }

    private static void dismiss(String dismissKey) {
        Preferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        JSONArray kept = new JSONArray();
        try {
            String raw = preferences.getDismissedAppNotices();
            JSONArray array = raw == null || raw.length() == 0 ? new JSONArray() : new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "");
                if (item.length() > 0 && !item.equals(dismissKey)) {
                    kept.put(item);
                }
            }
        } catch (JSONException ignored) {
            kept = new JSONArray();
        }
        kept.put(dismissKey);
        if (kept.length() > DISMISSED_CAP) {
            JSONArray trimmed = new JSONArray();
            for (int i = kept.length() - DISMISSED_CAP; i < kept.length(); i++) {
                trimmed.put(kept.optString(i));
            }
            kept = trimmed;
        }
        preferences.setDismissedAppNotices(kept.toString());
    }

    private static void finish(final Activity activity, final Runnable onDone) {
        if (onDone == null) {
            return;
        }
        try {
            activity.runOnUiThread(onDone);
        } catch (Throwable ignored) {
            onDone.run();
        }
    }
}
