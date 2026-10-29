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
import org.json.JSONObject;

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
 * Current messages are also copied into local settings until the pilot
 * deletes them. This client does not write Firestore.
 *
 * Failures (offline, rules, App Check) are ignored. A missing message must
 * not block the map. The Firestore read is given 8 seconds, then dropped.
 */
public final class AppNoticeService {

    /** Firestore {@code (default)} database, same as AvareX. */
    public static final String COLLECTION = "appNotices";
    private static final int TOKEN_CAP = 40;
    private static final Object LOCK = new Object();

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
     * Messages kept on this device, newest first.
     */
    public List<StoredAppNotice> saved() {
        synchronized (LOCK) {
            return loadSaved();
        }
    }

    /**
     * Drops one saved message. That revision is not saved or popped up again.
     */
    public void deleteSaved(StoredAppNotice notice) {
        synchronized (LOCK) {
            writeSaved(AppNoticeInbox.without(loadSaved(), notice.getDismissKey()));
            rememberToken(notice.getDismissKey(), true);
            rememberToken(notice.getDismissKey(), false);
        }
    }

    /**
     * Shows pending notices one at a time. Each current message is saved
     * before the popup. OK only suppresses the popup. {@code onDone} runs on
     * the main thread when the check is finished, including when nothing is shown.
     */
    public void present(final Activity activity, final Runnable onDone) {
        if (activity.isFinishing()) {
            finish(activity, onDone);
            return;
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<AppNotice> notices;
                try {
                    notices = fetchPending(new Date());
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

    List<AppNotice> fetchPending(Date now) throws Exception {
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
        List<AppNotice> current = new ArrayList<AppNotice>();
        for (int i = 0; i < notices.size(); i++) {
            AppNotice notice = notices.get(i);
            if (notice.isCurrent(now)) {
                current.add(notice);
            }
        }
        synchronized (LOCK) {
            remember(current);
            return AppNotice.pendingOf(notices, now, dismissedKeys(), AppNotice.DEFAULT_LIMIT);
        }
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

    private static void remember(List<AppNotice> current) {
        Preferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        List<StoredAppNotice> next = AppNoticeInbox.remember(
                loadSaved(),
                current,
                keys(preferences.getDeletedAppNotices()));
        writeSaved(next);
    }

    private static List<StoredAppNotice> loadSaved() {
        List<StoredAppNotice> notices = new ArrayList<StoredAppNotice>();
        Preferences preferences = preferences();
        if (preferences == null) {
            return notices;
        }
        String raw = preferences.getSavedAppNotices();
        if (raw == null || raw.length() == 0) {
            return notices;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                if (!(item instanceof JSONObject)) {
                    continue;
                }
                StoredAppNotice notice = StoredAppNotice.tryParse((JSONObject) item);
                if (notice != null) {
                    notices.add(notice);
                }
            }
        } catch (JSONException ignored) {
        }
        return notices;
    }

    private static void writeSaved(List<StoredAppNotice> notices) {
        Preferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (int i = 0; i < notices.size(); i++) {
            array.put(notices.get(i).toJson());
        }
        preferences.setSavedAppNotices(array.toString());
    }

    private static Set<String> dismissedKeys() {
        Preferences preferences = preferences();
        if (preferences == null) {
            return new HashSet<String>();
        }
        return keys(preferences.getDismissedAppNotices());
    }

    private static Set<String> keys(String raw) {
        Set<String> keys = new HashSet<String>();
        if (raw == null || raw.length() == 0) {
            return keys;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "");
                if (item.length() > 0) {
                    keys.add(item);
                }
            }
        } catch (JSONException ignored) {
        }
        return keys;
    }

    private static void dismiss(String dismissKey) {
        synchronized (LOCK) {
            rememberToken(dismissKey, false);
        }
    }

    /** {@code deleted} stores the key so that revision is not saved again. */
    private static void rememberToken(String token, boolean deleted) {
        Preferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        JSONArray kept = new JSONArray();
        try {
            String raw = deleted ? preferences.getDeletedAppNotices() : preferences.getDismissedAppNotices();
            JSONArray array = raw == null || raw.length() == 0 ? new JSONArray() : new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "");
                if (item.length() > 0 && !item.equals(token)) {
                    kept.put(item);
                }
            }
        } catch (JSONException ignored) {
            kept = new JSONArray();
        }
        kept.put(token);
        if (kept.length() > TOKEN_CAP) {
            JSONArray trimmed = new JSONArray();
            for (int i = kept.length() - TOKEN_CAP; i < kept.length(); i++) {
                trimmed.put(kept.optString(i));
            }
            kept = trimmed;
        }
        if (deleted) {
            preferences.setDeletedAppNotices(kept.toString());
        } else {
            preferences.setDismissedAppNotices(kept.toString());
        }
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
