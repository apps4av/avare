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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local inbox of Apps4Av messages. One row per notice id, newest first.
 */
public final class AppNoticeInbox {

    public static final int CAP = 40;

    private AppNoticeInbox() {
    }

    /**
     * Saves {@code current} into {@code saved}. A {@code deleted} dismiss key
     * is not put back. A new dismiss key for the same id replaces that row.
     * Messages already saved stay when they are absent from {@code current}.
     */
    public static List<StoredAppNotice> remember(List<StoredAppNotice> saved,
                                                 List<AppNotice> current,
                                                 Set<String> deleted) {
        Map<String, StoredAppNotice> byId = new HashMap<String, StoredAppNotice>();
        for (int i = 0; i < saved.size(); i++) {
            StoredAppNotice item = saved.get(i);
            if (item.getId().length() == 0 || deleted.contains(item.getDismissKey())) {
                continue;
            }
            byId.put(item.getId(), item);
        }
        for (int i = 0; i < current.size(); i++) {
            AppNotice notice = current.get(i);
            if (deleted.contains(notice.getDismissKey())) {
                StoredAppNotice existing = byId.get(notice.getId());
                if (existing != null && existing.getDismissKey().equals(notice.getDismissKey())) {
                    byId.remove(notice.getId());
                }
                continue;
            }
            byId.put(notice.getId(), StoredAppNotice.fromNotice(notice));
        }
        List<StoredAppNotice> list = new ArrayList<StoredAppNotice>(byId.values());
        Collections.sort(list, new Comparator<StoredAppNotice>() {
            @Override
            public int compare(StoredAppNotice a, StoredAppNotice b) {
                int byTime = b.getUpdatedAt().compareTo(a.getUpdatedAt());
                if (byTime != 0) {
                    return byTime;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        if (list.size() <= CAP) {
            return list;
        }
        return new ArrayList<StoredAppNotice>(list.subList(0, CAP));
    }

    public static List<StoredAppNotice> without(List<StoredAppNotice> saved, String dismissKey) {
        List<StoredAppNotice> kept = new ArrayList<StoredAppNotice>();
        for (int i = 0; i < saved.size(); i++) {
            StoredAppNotice item = saved.get(i);
            if (!item.getDismissKey().equals(dismissKey)) {
                kept.add(item);
            }
        }
        return kept;
    }
}
