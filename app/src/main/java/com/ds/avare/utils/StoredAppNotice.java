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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * A copy of an {@link AppNotice} kept on this device. Firestore is not written.
 */
public final class StoredAppNotice {

    private final String mId;
    private final String mTitle;
    private final String mBody;
    private final String mUrl;
    private final Date mUpdatedAt;
    private final String mDismissKey;

    public StoredAppNotice(String id, String title, String body, String url,
                           Date updatedAt, String dismissKey) {
        mId = id;
        mTitle = title;
        mBody = body;
        mUrl = url;
        mUpdatedAt = updatedAt;
        mDismissKey = dismissKey;
    }

    public static StoredAppNotice fromNotice(AppNotice notice) {
        return new StoredAppNotice(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getUrl(),
                notice.getUpdatedAt(),
                notice.getDismissKey());
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

    public String getDismissKey() {
        return mDismissKey;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", mId);
            object.put("title", mTitle);
            object.put("body", mBody);
            object.put("url", mUrl == null ? JSONObject.NULL : mUrl);
            object.put("updatedAt", mUpdatedAt.getTime());
            object.put("dismissKey", mDismissKey);
        } catch (JSONException ignored) {
        }
        return object;
    }

    public static StoredAppNotice tryParse(JSONObject value) {
        if (value == null) {
            return null;
        }
        String id = value.optString("id", "");
        String title = value.optString("title", "");
        String body = value.optString("body", "");
        String dismissKey = value.optString("dismissKey", "");
        if (id.length() == 0 || title.length() == 0 || body.length() == 0 || dismissKey.length() == 0) {
            return null;
        }
        if (!value.has("updatedAt") || value.isNull("updatedAt")) {
            return null;
        }
        long updatedAt;
        try {
            updatedAt = value.getLong("updatedAt");
        } catch (JSONException ignored) {
            return null;
        }
        String url = value.optString("url", "");
        if (url.length() == 0) {
            url = null;
        }
        return new StoredAppNotice(id, title, body, url, new Date(updatedAt), dismissKey);
    }
}
