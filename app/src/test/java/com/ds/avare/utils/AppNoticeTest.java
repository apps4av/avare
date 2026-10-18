package com.ds.avare.utils;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Same cases as AvareX test/app_notice_test.dart, plus the dismiss-key store.
 */
public class AppNoticeTest {

    private static final Date NOW = utc(2026, 10, 18);

    private static AppNotice notice(String id, String title, String body, boolean active,
                                    Date updatedAt, Date expiresAt, String url) {
        return new AppNotice(
                id,
                title,
                body,
                active,
                updatedAt == null ? utc(2026, 10, 1) : updatedAt,
                expiresAt,
                url);
    }

    private static AppNotice notice(String id) {
        return notice(id, "Chart cycle", "New charts are available.", true, null, null, null);
    }

    @Test
    public void readsAConsoleDocument() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", " Chart cycle ");
        data.put("body", "Download the new cycle.");
        data.put("active", Boolean.TRUE);
        data.put("updatedAt", utc(2026, 10, 2));
        data.put("expiresAt", utc(2026, 11, 1));
        data.put("url", "https://apps4av.net/cycle");

        AppNotice parsed = AppNotice.tryParse("cycle-2513", data);
        assertEquals("Chart cycle", parsed.title);
        assertEquals("https://apps4av.net/cycle", parsed.url);
        assertTrue(parsed.isCurrent(NOW));
    }

    @Test
    public void dropsADocumentWithNoText() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", " ");
        data.put("body", "hi");
        data.put("active", Boolean.TRUE);
        assertNull(AppNotice.tryParse("x", data));
    }

    @Test
    public void ignoresANonHttpsLink() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Hello");
        data.put("body", "World");
        data.put("active", Boolean.TRUE);
        data.put("url", "http://example.com");
        assertNull(AppNotice.tryParse("x", data).url);
    }

    @Test
    public void hidesInactiveExpiredAndDismissedNotices() {
        AppNotice current = notice("new");
        AppNotice seen = notice("seen");
        List<AppNotice> pending = AppNotice.pendingOf(
                Arrays.asList(
                        current,
                        notice("off", "Chart cycle", "New charts are available.", false, null, null, null),
                        notice("old", "Chart cycle", "New charts are available.", true, null,
                                utc(2026, 10, 1), null),
                        seen),
                NOW,
                Collections.singleton(seen.getDismissKey()));
        assertEquals(1, pending.size());
        assertEquals("new", pending.get(0).id);
    }

    @Test
    public void anEditShowsADismissedNoticeAgain() {
        AppNotice original = notice("cycle", "Chart cycle", "First text", true, null, null, null);
        AppNotice edited = notice("cycle", "Chart cycle", "Updated text", true,
                utc(2026, 10, 3), null, null);
        assertNotEquals(original.getDismissKey(), edited.getDismissKey());
        List<AppNotice> pending = AppNotice.pendingOf(
                Collections.singletonList(edited),
                NOW,
                Collections.singleton(original.getDismissKey()));
        assertEquals(1, pending.size());
    }

    @Test
    public void ordersNewestFirstAndCapsTheList() {
        List<AppNotice> pending = AppNotice.pendingOf(
                Arrays.asList(
                        notice("a", "Chart cycle", "New charts are available.", true,
                                utc(2026, 1, 1), null, null),
                        notice("c", "Chart cycle", "New charts are available.", true,
                                utc(2026, 3, 1), null, null),
                        notice("b", "Chart cycle", "New charts are available.", true,
                                utc(2026, 2, 1), null, null)),
                NOW,
                new HashSet<String>(),
                2);
        assertEquals(Arrays.asList("c", "b"), Arrays.asList(pending.get(0).id, pending.get(1).id));
    }

    @Test
    public void usesCreatedAtThenEpochWhenUpdatedAtIsMissing() {
        Map<String, Object> created = new HashMap<String, Object>();
        created.put("title", "Hello");
        created.put("body", "World");
        created.put("active", Boolean.TRUE);
        created.put("createdAt", Long.valueOf(1_700_000_000_000L));
        assertEquals(1_700_000_000_000L, AppNotice.tryParse("x", created).updatedAt.getTime());

        Map<String, Object> neither = new HashMap<String, Object>();
        neither.put("title", "Hello");
        neither.put("body", "World");
        neither.put("active", Boolean.TRUE);
        assertEquals(0L, AppNotice.tryParse("x", neither).updatedAt.getTime());
    }

    @Test
    public void timeReaderSuppliesFirestoreTimestamps() {
        final Date updated = utc(2026, 10, 2);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", "Hello");
        data.put("body", "World");
        data.put("active", Boolean.TRUE);
        data.put("updatedAt", "timestamp-token");
        AppNotice parsed = AppNotice.tryParse("x", data, new AppNotice.TimeReader() {
            @Override
            public Date read(Object value) {
                return "timestamp-token".equals(value) ? updated : null;
            }
        });
        assertEquals(updated, parsed.updatedAt);
    }

    @Test
    public void clipsLongTextAndDropsALongOrHostlessLink() {
        String longTitle = repeat('T', AppNotice.MAX_TITLE + 5);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", longTitle);
        data.put("body", "World");
        data.put("active", Boolean.TRUE);
        data.put("url", "https://");
        AppNotice parsed = AppNotice.tryParse("x", data);
        assertEquals(AppNotice.MAX_TITLE, parsed.title.length());
        assertNull(parsed.url);

        data.put("url", "https://apps4av.net/" + repeat('a', AppNotice.MAX_URL));
        assertNull(AppNotice.tryParse("x", data).url);
    }

    @Test
    public void expiryAtTheCurrentMomentIsHidden() {
        AppNotice expiring = notice("x", "Chart cycle", "New charts are available.", true, null, NOW, null);
        assertFalse(expiring.isCurrent(NOW));
    }

    @Test
    public void defaultPendingLimitIsThree() {
        List<AppNotice> notices = Arrays.asList(
                notice("a", "Chart cycle", "New charts are available.", true, new Date(1), null, null),
                notice("b", "Chart cycle", "New charts are available.", true, new Date(2), null, null),
                notice("c", "Chart cycle", "New charts are available.", true, new Date(3), null, null),
                notice("d", "Chart cycle", "New charts are available.", true, new Date(4), null, null));
        List<AppNotice> pending = AppNotice.pendingOf(notices, NOW, new HashSet<String>());
        assertEquals(Arrays.asList("d", "c", "b"), Arrays.asList(
                pending.get(0).id, pending.get(1).id, pending.get(2).id));
    }

    @Test
    public void equalTimesSortById() {
        Date when = new Date(5);
        List<AppNotice> pending = AppNotice.pendingOf(
                Arrays.asList(
                        notice("b", "Chart cycle", "New charts are available.", true, when, null, null),
                        notice("a", "Chart cycle", "New charts are available.", true, when, null, null)),
                NOW,
                new HashSet<String>());
        assertEquals("a", pending.get(0).id);
        assertEquals("b", pending.get(1).id);
    }

    @Test
    public void emptyFnv1aMatchesTheOffsetBasis() {
        assertEquals(0x811c9dc5L, AppNotice.fnv1a(""));
    }

    @Test
    public void foregroundCheckWaitsTwentySeconds() {
        assertTrue(AppNotice.shouldCheck(false, true, 50_000L, 0L));
        assertFalse(AppNotice.shouldCheck(true, true, 50_000L, 0L));
        assertFalse(AppNotice.shouldCheck(false, false, 50_000L, 0L));
        assertFalse(AppNotice.shouldCheck(false, true, 30_000L, 20_000L));
        assertTrue(AppNotice.shouldCheck(false, true, 40_000L, 20_000L));
    }

    @Test
    public void dismissedKeysRoundTripAndStayCapped() {
        assertTrue(DismissedAppNotices.decode("").isEmpty());
        assertTrue(DismissedAppNotices.decode(null).isEmpty());

        String key = "cycle@123@456,more";
        String stored = DismissedAppNotices.dismiss("", key);
        assertEquals(Collections.singleton(key), DismissedAppNotices.decode(stored));

        String again = DismissedAppNotices.dismiss(stored, "other");
        again = DismissedAppNotices.dismiss(again, key);
        List<String> order = new java.util.ArrayList<String>(DismissedAppNotices.decode(again));
        assertEquals(Arrays.asList("other", key), order);

        String raw = "";
        for (int i = 0; i < DismissedAppNotices.CAP + 1; i++) {
            raw = DismissedAppNotices.dismiss(raw, "k" + i);
        }
        Set<String> kept = DismissedAppNotices.decode(raw);
        assertEquals(DismissedAppNotices.CAP, kept.size());
        assertFalse(kept.contains("k0"));
        assertTrue(kept.contains("k" + DismissedAppNotices.CAP));

        String unicode = DismissedAppNotices.dismiss("", "café");
        assertEquals(Collections.singleton("café"), DismissedAppNotices.decode(unicode));
        assertEquals("%ZZ", DismissedAppNotices.decodeComponent("%ZZ"));
    }

    private static Date utc(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
