package com.ds.avare.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppNoticeTest {

    private static final Date NOW = new Date(Date.UTC(2026, 9, 18, 0, 0, 0));

    private static AppNotice notice(String id, String title, String body, boolean active,
                                    Date updatedAt, Date expiresAt, String url) {
        return new AppNotice(id, title, body, active,
                updatedAt == null ? new Date(Date.UTC(2026, 9, 1, 0, 0, 0)) : updatedAt,
                expiresAt, url);
    }

    @Test
    public void readsAConsoleDocument() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", " Chart cycle ");
        data.put("body", "Download the new cycle.");
        data.put("active", Boolean.TRUE);
        data.put("updatedAt", new Date(Date.UTC(2026, 9, 2, 0, 0, 0)));
        data.put("expiresAt", new Date(Date.UTC(2026, 10, 1, 0, 0, 0)));
        data.put("url", "https://apps4av.net/cycle");

        AppNotice notice = AppNotice.tryParse("cycle-2513", data, null);
        assertEquals("Chart cycle", notice.getTitle());
        assertEquals("https://apps4av.net/cycle", notice.getUrl());
        assertTrue(notice.isCurrent(NOW));
    }

    @Test
    public void dropsADocumentWithNoText() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", " ");
        data.put("body", "hi");
        data.put("active", Boolean.TRUE);
        assertNull(AppNotice.tryParse("x", data, null));
    }

    @Test
    public void ignoresANonHttpsLink() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Hello");
        data.put("body", "World");
        data.put("active", Boolean.TRUE);
        data.put("url", "http://example.com");
        AppNotice notice = AppNotice.tryParse("x", data, null);
        assertNull(notice.getUrl());
    }

    @Test
    public void stringTrueIsNotActive() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Hello");
        data.put("body", "World");
        data.put("active", "true");
        AppNotice notice = AppNotice.tryParse("x", data, null);
        assertTrue(!notice.isCurrent(NOW));
    }

    @Test
    public void hidesInactiveExpiredAndDismissedNotices() {
        AppNotice current = notice("new", "Chart cycle", "New charts are available.", true, null, null, null);
        AppNotice seen = notice("seen", "Chart cycle", "New charts are available.", true, null, null, null);
        List<AppNotice> pending = AppNotice.pendingOf(
                Arrays.asList(
                        current,
                        notice("off", "Chart cycle", "New charts are available.", false, null, null, null),
                        notice("old", "Chart cycle", "New charts are available.", true, null, new Date(Date.UTC(2026, 9, 1, 0, 0, 0)), null),
                        seen),
                NOW,
                Collections.singleton(seen.getDismissKey()),
                3);
        assertEquals(1, pending.size());
        assertEquals("new", pending.get(0).getId());
    }

    @Test
    public void anEditShowsADismissedNoticeAgain() {
        AppNotice original = notice("cycle", "Chart cycle", "First text", true, null, null, null);
        AppNotice edited = notice("cycle", "Chart cycle", "Updated text", true, new Date(Date.UTC(2026, 9, 3, 0, 0, 0)), null, null);
        assertNotEquals(original.getDismissKey(), edited.getDismissKey());
        List<AppNotice> pending = AppNotice.pendingOf(
                Collections.singletonList(edited),
                NOW,
                Collections.singleton(original.getDismissKey()),
                3);
        assertEquals(1, pending.size());
    }

    @Test
    public void ordersNewestFirstAndCapsTheList() {
        List<AppNotice> pending = AppNotice.pendingOf(
                Arrays.asList(
                        notice("a", "Chart cycle", "New charts are available.", true, new Date(Date.UTC(2026, 0, 1, 0, 0, 0)), null, null),
                        notice("c", "Chart cycle", "New charts are available.", true, new Date(Date.UTC(2026, 2, 1, 0, 0, 0)), null, null),
                        notice("b", "Chart cycle", "New charts are available.", true, new Date(Date.UTC(2026, 1, 1, 0, 0, 0)), null, null)),
                NOW,
                new HashSet<String>(),
                2);
        assertEquals("c", pending.get(0).getId());
        assertEquals("b", pending.get(1).getId());
    }

    @Test
    public void replacesTheSavedRowForTheSameId() {
        AppNotice original = notice("cycle", "Chart cycle", "First text", true, null, null, null);
        AppNotice edited = notice("cycle", "Chart cycle", "Updated text", true, null, null, null);
        List<StoredAppNotice> saved = AppNoticeInbox.remember(
                Collections.singletonList(StoredAppNotice.fromNotice(original)),
                Collections.singletonList(edited),
                new HashSet<String>());
        assertEquals(1, saved.size());
        assertEquals("Updated text", saved.get(0).getBody());
        assertEquals(edited.getDismissKey(), saved.get(0).getDismissKey());
    }

    @Test
    public void aDeletedRevisionIsNotSavedAgain() {
        AppNotice item = notice("cycle", "Chart cycle", "New charts are available.", true, null, null, null);
        List<StoredAppNotice> saved = AppNoticeInbox.remember(
                Collections.<StoredAppNotice>emptyList(),
                Collections.singletonList(item),
                Collections.singleton(item.getDismissKey()));
        assertEquals(0, saved.size());
    }

    @Test
    public void anEditOfADeletedMessageIsSavedAgain() {
        AppNotice original = notice("cycle", "Chart cycle", "First text", true, null, null, null);
        AppNotice edited = notice("cycle", "Chart cycle", "Updated text", true,
                new Date(Date.UTC(2026, 9, 3, 0, 0, 0)), null, null);
        List<StoredAppNotice> saved = AppNoticeInbox.remember(
                Collections.singletonList(StoredAppNotice.fromNotice(original)),
                Collections.singletonList(edited),
                Collections.singleton(original.getDismissKey()));
        assertEquals(1, saved.size());
        assertEquals("Updated text", saved.get(0).getBody());
    }

    @Test
    public void keepsASavedMessageThatIsNoLongerCurrent() {
        AppNotice kept = notice("old", "Chart cycle", "New charts are available.", true,
                new Date(Date.UTC(2026, 0, 1, 0, 0, 0)), null, null);
        AppNotice fresh = notice("new", "Chart cycle", "New charts are available.", true,
                new Date(Date.UTC(2026, 2, 1, 0, 0, 0)), null, null);
        List<StoredAppNotice> saved = AppNoticeInbox.remember(
                Collections.singletonList(StoredAppNotice.fromNotice(kept)),
                Collections.singletonList(fresh),
                new HashSet<String>());
        assertEquals("new", saved.get(0).getId());
        assertEquals("old", saved.get(1).getId());
    }

    @Test
    public void dropsTheOldestPastTheCap() {
        List<AppNotice> current = new ArrayList<AppNotice>();
        for (int index = 0; index < AppNoticeInbox.CAP + 1; index++) {
            current.add(notice("n" + index, "Chart cycle", "New charts are available.", true,
                    new Date(Date.UTC(2026, 0, 1 + index, 0, 0, 0)), null, null));
        }
        List<StoredAppNotice> saved = AppNoticeInbox.remember(
                Collections.<StoredAppNotice>emptyList(),
                current,
                new HashSet<String>());
        assertEquals(AppNoticeInbox.CAP, saved.size());
        assertEquals("n" + AppNoticeInbox.CAP, saved.get(0).getId());
        boolean dropped = true;
        for (int i = 0; i < saved.size(); i++) {
            if ("n0".equals(saved.get(i).getId())) {
                dropped = false;
            }
        }
        assertTrue(dropped);
    }
}
