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

import java.net.URI;
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
 * Collection: {@code appNotices/{id}}
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
 * Clients can read these documents and cannot write them.
 */
public final class AppNotice {

    public static final int MAX_TITLE = 120;
    public static final int MAX_BODY = 2000;
    public static final int MAX_URL = 500;
    public static final int DEFAULT_LIMIT = 3;

    /** Converts a backend timestamp (for example a Firestore Timestamp) into a Date. */
    public interface TimeSource {
        Date read(Object value);
    }

    private final String mId;
    private final String mTitle;
    private final String mBody;
    private final boolean mActive;
    private final Date mUpdatedAt;
    private final Date mExpiresAt;
    private final String mUrl;

    public AppNotice(String id, String title, String body, boolean active,
                     Date updatedAt, Date expiresAt, String url) {
        mId = id;
        mTitle = title;
        mBody = body;
        mActive = active;
        mUpdatedAt = updatedAt;
        mExpiresAt = expiresAt;
        mUrl = url;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getBody() {
        return mBody;
    }

    public String getUrl() {
        return mUrl;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    /**
     * Token stored after the pilot taps OK. A change to the title, body,
     * link, or updatedAt produces a new token, so the message is shown again.
     */
    public String getDismissKey() {
        long revision = mUpdatedAt.getTime();
        long content = fnv1a(mTitle + "\n" + mBody + "\n" + (mUrl == null ? "" : mUrl));
        return mId + "@" + revision + "@" + content;
    }

    public boolean isCurrent(Date now) {
        if (!mActive || mTitle.length() == 0 || mBody.length() == 0) {
            return false;
        }
        if (mExpiresAt != null && !mExpiresAt.after(now)) {
            return false;
        }
        return true;
    }

    /**
     * Newest current notices that this device has not dismissed.
     */
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
                int byTime = b.mUpdatedAt.compareTo(a.mUpdatedAt);
                if (byTime != 0) {
                    return byTime;
                }
                return a.mId.compareTo(b.mId);
            }
        });
        if (pending.size() <= limit) {
            return pending;
        }
        return new ArrayList<AppNotice>(pending.subList(0, limit));
    }

    /**
     * Returns null when the document has no usable title and body.
     */
    public static AppNotice tryParse(String id, Map<String, Object> data, TimeSource timeSource) {
        if (id == null || id.length() == 0 || data == null) {
            return null;
        }
        String title = clip(stringOf(data.get("title")), MAX_TITLE);
        String body = clip(stringOf(data.get("body")), MAX_BODY);
        if (title.length() == 0 || body.length() == 0) {
            return null;
        }
        Date updated = timeOf(data.get("updatedAt"), timeSource);
        if (updated == null) {
            updated = timeOf(data.get("createdAt"), timeSource);
        }
        if (updated == null) {
            updated = new Date(0);
        }
        return new AppNotice(
                id,
                title,
                body,
                Boolean.TRUE.equals(data.get("active")),
                updated,
                timeOf(data.get("expiresAt"), timeSource),
                httpsUrl(data.get("url")));
    }

    private static String stringOf(Object value) {
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

    private static Date timeOf(Object value, TimeSource timeSource) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (timeSource == null) {
            return null;
        }
        return timeSource.read(value);
    }

    private static String httpsUrl(Object value) {
        String raw = stringOf(value);
        if (raw.length() == 0 || raw.length() > MAX_URL) {
            return null;
        }
        try {
            URI uri = new URI(raw);
            if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            if (uri.getHost() == null || uri.getHost().length() == 0) {
                return null;
            }
            return raw;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 32-bit FNV-1a over UTF-16 code units, matching AvareX. */
    private static long fnv1a(String value) {
        long hash = 0x811c9dc5L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash = (hash * 0x01000193L) & 0xFFFFFFFFL;
        }
        return hash;
    }
}
