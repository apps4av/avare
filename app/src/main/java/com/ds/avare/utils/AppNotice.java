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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A message published from Google Cloud (the Firebase console or the Admin
 * SDK) and shown when Avare opens.
 *
 * Collection: {@code appNotices/{id}} — the same collection name AvareX uses.
 *
 *   title      string, required
 *   body       string, required
 *   active     bool, required ({@code true} to show)
 *   createdAt  timestamp
 *   updatedAt  timestamp — set this to now when you publish or edit so a
 *              pilot who already tapped OK sees the new text
 *   expiresAt  timestamp, optional — hidden once this moment has passed
 *   url        string, optional — https only
 *
 * Clients can read these documents and cannot write them. Publish by adding
 * or editing a document in the Firebase console. Set {@code active} to false,
 * or delete the document, to take a message down.
 */
public final class AppNotice {

    public static final String COLLECTION = "appNotices";

    public static final int MAX_TITLE = 120;
    public static final int MAX_BODY = 2000;
    public static final int MAX_URL = 500;

    /** At most one Firestore read every 20 seconds, matching AvareX. */
    public static final long MIN_CHECK_INTERVAL_MS = 20_000L;

    /** How many pending messages to show on one check. */
    public static final int DEFAULT_PENDING_LIMIT = 3;

    /**
     * Converts a backend timestamp (for example a Firestore {@code Timestamp})
     * into a {@link Date}. {@link Date} and epoch milliseconds are accepted
     * without it.
     */
    public interface TimeReader {
        Date read(Object value);
    }

    public final String id;
    public final String title;
    public final String body;
    public final boolean active;
    public final Date updatedAt;
    public final Date expiresAt;
    public final String url;

    public AppNotice(String id, String title, String body, boolean active,
                     Date updatedAt, Date expiresAt, String url) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.active = active;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.url = url;
    }

    /**
     * Token stored after the pilot taps OK. A change to the title, body,
     * link, or {@code updatedAt} produces a new token, so the message is shown again.
     */
    public String getDismissKey() {
        long revision = updatedAt.getTime();
        long content = fnv1a(title + "\n" + body + "\n" + (url == null ? "" : url));
        return id + "@" + revision + "@" + content;
    }

    public boolean isCurrent(Date now) {
        if (!active || title.length() == 0 || body.length() == 0) {
            return false;
        }
        if (expiresAt != null && !expiresAt.after(now)) {
            return false;
        }
        return true;
    }

    /**
     * Newest current notices that this device has not dismissed.
     */
    public static List<AppNotice> pendingOf(List<AppNotice> notices, Date now, Set<String> dismissed) {
        return pendingOf(notices, now, dismissed, DEFAULT_PENDING_LIMIT);
    }

    public static List<AppNotice> pendingOf(List<AppNotice> notices, Date now,
                                            Set<String> dismissed, int limit) {
        List<AppNotice> pending = new ArrayList<AppNotice>();
        for (int i = 0; i < notices.size(); i++) {
            AppNotice notice = notices.get(i);
            if (notice.isCurrent(now) && !dismissed.contains(notice.getDismissKey())) {
                pending.add(notice);
            }
        }
        Collections.sort(pending, new Comparator<AppNotice>() {
            @Override
            public int compare(AppNotice a, AppNotice b) {
                int byTime = b.updatedAt.compareTo(a.updatedAt);
                if (byTime != 0) {
                    return byTime;
                }
                return a.id.compareTo(b.id);
            }
        });
        if (pending.size() <= limit) {
            return pending;
        }
        return new ArrayList<AppNotice>(pending.subList(0, limit));
    }

    /**
     * Skip a check while a dialog is already up, while the screen is gone,
     * and until {@link #MIN_CHECK_INTERVAL_MS} has passed since the last check.
     * {@code lastCheckedMs} of {@code 0} is "never checked".
     */
    public static boolean shouldCheck(boolean presenting, boolean screenActive,
                                      long nowMs, long lastCheckedMs) {
        if (presenting || !screenActive) {
            return false;
        }
        return nowMs - lastCheckedMs >= MIN_CHECK_INTERVAL_MS;
    }

    /**
     * Returns null when the document has no usable title and body.
     */
    public static AppNotice tryParse(String id, Map<String, Object> data) {
        return tryParse(id, data, null);
    }

    public static AppNotice tryParse(String id, Map<String, Object> data, TimeReader readTime) {
        if (id == null || id.length() == 0 || data == null) {
            return null;
        }
        String title = clip(string(data.get("title")), MAX_TITLE);
        String body = clip(string(data.get("body")), MAX_BODY);
        if (title.length() == 0 || body.length() == 0) {
            return null;
        }
        Date updated = time(data.get("updatedAt"), readTime);
        if (updated == null) {
            updated = time(data.get("createdAt"), readTime);
        }
        if (updated == null) {
            updated = new Date(0L);
        }
        return new AppNotice(
                id,
                title,
                body,
                Boolean.TRUE.equals(data.get("active")),
                updated,
                time(data.get("expiresAt"), readTime),
                httpsUrl(data.get("url")));
    }

    static long fnv1a(String value) {
        long hash = 0x811c9dc5L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash = (hash * 0x01000193L) & 0xFFFFFFFFL;
        }
        return hash;
    }

    private static String string(Object value) {
        if (!(value instanceof String)) {
            return "";
        }
        return ((String) value).trim();
    }

    private static String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max).trim();
    }

    private static Date time(Object value, TimeReader readTime) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (readTime == null) {
            return null;
        }
        return readTime.read(value);
    }

    private static String httpsUrl(Object value) {
        String raw = string(value);
        if (raw.length() == 0 || raw.length() > MAX_URL) {
            return null;
        }
        java.net.URI uri;
        try {
            uri = new java.net.URI(raw);
        } catch (java.net.URISyntaxException e) {
            return null;
        }
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getHost().length() == 0) {
            return null;
        }
        return raw;
    }
}
