package com.ars.fx.data;

import com.ars.fx.data.UpcomingEvents.Event;
import com.ars.fx.data.UpcomingEvents.Kind;
import com.ars.fx.data.UpcomingEvents.Loaded;
import com.ars.fx.data.UpcomingEvents.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UpcomingEventsTest {
    static final String DX = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            SUMMARY:Solomon Is (H44MS)
            DTSTART;VALUE=DATE:20261003
            DTEND;VALUE=DATE:20261120
            DESCRIPTION:By DL2GAC\\; 80-10m\\; mainly FT8\\, some SSB\\; QSL via DL2GAC (B
             /d)
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Tanzania (5H3MB)
            DTSTART;VALUE=DATE:20261119
            DTEND;VALUE=DATE:20261120
            DESCRIPTION:By IK2GZU\\; CW
            END:VEVENT
            END:VCALENDAR
            """;
    static final String CONTESTS = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:ARRL Sweepstakes\\, CW
            DTSTART:20261107T210000Z
            DTEND:20261109T030000Z
            URL:https://www.contestcalendar.com/contestdetails.php?ref=1
            END:VEVENT
            END:VCALENDAR
            """;

    @AfterEach
    void reset() { UpcomingEvents.setDirForTest(null); UpcomingEvents.setHttpForTest(null); }

    @Test
    void parsesAllDayEventsWithFoldedLinesAndEscapes() {
        var evs = IcalFeed.parse(DX);
        assertEquals(2, evs.size());
        var h44 = evs.get(0);
        assertEquals("Solomon Is (H44MS)", h44.summary());
        assertEquals(LocalDate.of(2026, 10, 3), h44.start());
        assertEquals(LocalDate.of(2026, 11, 19), h44.end(), "an all-day DTEND is the day after the last day");
        assertEquals("By DL2GAC; 80-10m; mainly FT8, some SSB; QSL via DL2GAC (B/d)", h44.description());
        assertNull(h44.startTime());
        assertEquals(LocalDate.of(2026, 11, 19), evs.get(1).end(), "a one-day event ends the day it starts");
    }

    @Test
    void parsesTimedEvents() {
        var e = IcalFeed.parse(CONTESTS).get(0);
        assertEquals("ARRL Sweepstakes, CW", e.summary());
        assertEquals(LocalDate.of(2026, 11, 7), e.start());
        assertEquals(LocalDate.of(2026, 11, 9), e.end());
        assertEquals(21, e.startTime().getHour());
        assertEquals("https://www.contestcalendar.com/contestdetails.php?ref=1", e.url());
    }

    @Test
    void junkInputGivesNothing() {
        assertTrue(IcalFeed.parse(null).isEmpty());
        assertTrue(IcalFeed.parse("not a calendar").isEmpty());
        assertTrue(IcalFeed.parse("BEGIN:VEVENT\nSUMMARY:no dates\nEND:VEVENT").isEmpty());
    }

    @Test
    void filtersByKindWindowAndText() {
        Source dx = new Source(Kind.DXPEDITION, "dx", "https://x/dx.ics", "DX", "");
        Source ct = new Source(Kind.CONTEST, "contests", "https://x/c.ics", "C", "");
        List<Event> all = new java.util.ArrayList<>(UpcomingEvents.tag(dx, DX));
        all.addAll(UpcomingEvents.tag(ct, CONTESTS));
        LocalDate today = LocalDate.of(2026, 11, 5);

        var next7 = UpcomingEvents.upcoming(all, today, 7, EnumSet.allOf(Kind.class), "");
        assertEquals(List.of("Solomon Is (H44MS)", "ARRL Sweepstakes, CW"), next7.stream().map(Event::title).toList(), "ongoing first, then by start; Tanzania is 14 days off");
        assertEquals(3, UpcomingEvents.upcoming(all, today, 30, EnumSet.allOf(Kind.class), "").size());
        assertEquals(1, UpcomingEvents.upcoming(all, today, 30, EnumSet.of(Kind.CONTEST), "").size());
        assertEquals(1, UpcomingEvents.upcoming(all, today, 30, EnumSet.allOf(Kind.class), "ft8").size(), "text matches the details too");
        assertEquals(0, UpcomingEvents.upcoming(all, LocalDate.of(2027, 1, 1), 30, EnumSet.allOf(Kind.class), "").size(), "finished events are dropped");
        assertEquals("2100Z–0300Z", UpcomingEvents.tag(ct, CONTESTS).get(0).time());
    }

    @Test
    void cacheKeepsRequestsToTheMinimum(@TempDir Path tmp) {
        UpcomingEvents.setDirForTest(tmp);
        AtomicInteger hits = new AtomicInteger();
        UpcomingEvents.setHttpForTest(u -> { hits.incrementAndGet(); return DX; });
        Source dx = new Source(Kind.DXPEDITION, "dx", "https://x/dx.ics", "DX", "");
        Instant t0 = Instant.parse("2026-11-05T12:00:00Z");

        Loaded a = UpcomingEvents.load(dx, false, t0);
        assertEquals(2, a.events().size()); assertFalse(a.fromCache()); assertEquals(1, hits.get());

        Loaded b = UpcomingEvents.load(dx, false, t0.plus(Duration.ofHours(1)));
        assertTrue(b.fromCache()); assertEquals(1, hits.get(), "fresh cache: no request");

        UpcomingEvents.load(dx, true, t0.plus(Duration.ofMinutes(5)));
        assertEquals(1, hits.get(), "a manual refresh within 15 minutes still uses the cache");

        UpcomingEvents.load(dx, true, t0.plus(Duration.ofMinutes(20)));
        assertEquals(2, hits.get(), "a manual refresh after 15 minutes fetches");

        UpcomingEvents.load(dx, false, t0.plus(Duration.ofHours(7)));
        assertEquals(3, hits.get(), "stale cache is refreshed");
    }

    @Test
    void wa7bnmIsFetchedEveryTwelveHours(@TempDir Path tmp) {
        UpcomingEvents.setDirForTest(tmp);
        AtomicInteger hits = new AtomicInteger();
        UpcomingEvents.setHttpForTest(u -> { hits.incrementAndGet(); return CONTESTS; });
        Source wa = new Source(Kind.CONTEST, "wa7bnm", "https://x/wa7bnm.ics", UpcomingEvents.WA7BNM_NAME, UpcomingEvents.WA7BNM_SITE);
        Instant t0 = Instant.parse("2026-11-05T00:00:00Z");
        UpcomingEvents.load(wa, false, t0);
        UpcomingEvents.load(wa, false, t0.plus(Duration.ofHours(11)));
        assertEquals(1, hits.get(), "still fresh after 11 hours (the other feeds go stale after 6)");
        UpcomingEvents.load(wa, false, t0.plus(Duration.ofHours(13)));
        assertEquals(2, hits.get(), "refetched after 12 hours");
    }

    @Test
    void wa7bnmIsOffUntilPermissionAndAnAddressExist() {
        assertFalse(UpcomingEvents.wa7bnmOn(), "off by default");
        for (Source s : UpcomingEvents.sources()) assertNotEquals("wa7bnm", s.key());
    }

    @Test
    void failedFetchFallsBackToTheSavedCopy(@TempDir Path tmp) {
        UpcomingEvents.setDirForTest(tmp);
        Source dx = new Source(Kind.DXPEDITION, "dx", "https://x/dx.ics", "DX", "");
        Instant t0 = Instant.parse("2026-11-05T12:00:00Z");
        UpcomingEvents.setHttpForTest(u -> DX);
        UpcomingEvents.load(dx, false, t0);
        UpcomingEvents.setHttpForTest(u -> { throw new RuntimeException("offline"); });
        Loaded l = UpcomingEvents.load(dx, false, t0.plus(Duration.ofHours(8)));
        assertEquals(2, l.events().size());
        assertTrue(l.fromCache());
        assertTrue(l.error().contains("offline"));

        Source other = new Source(Kind.CONTEST, "contests", "https://x/c.ics", "C", "");
        Loaded none = UpcomingEvents.load(other, false, t0);
        assertTrue(none.events().isEmpty());
        assertNotNull(none.error());
    }

    @Test
    void aChangedFeedAddressIgnoresTheOldCache(@TempDir Path tmp) {
        UpcomingEvents.setDirForTest(tmp);
        AtomicInteger hits = new AtomicInteger();
        UpcomingEvents.setHttpForTest(u -> { hits.incrementAndGet(); return DX; });
        Instant t0 = Instant.parse("2026-11-05T12:00:00Z");
        UpcomingEvents.load(new Source(Kind.CONTEST, "contests", "https://a/c.ics", "C", ""), false, t0);
        UpcomingEvents.load(new Source(Kind.CONTEST, "contests", "https://b/c.ics", "C", ""), false, t0.plusSeconds(60));
        assertEquals(2, hits.get());
    }

    @Test
    void onlyWebAddressesAreFetched(@TempDir Path tmp) {
        UpcomingEvents.setDirForTest(tmp);
        UpcomingEvents.setHttpForTest(u -> { fail("must not fetch"); return ""; });
        assertTrue(UpcomingEvents.validUrl("https://www.example.com/a.ics"));
        assertFalse(UpcomingEvents.validUrl("file:///etc/passwd"));
        assertFalse(UpcomingEvents.validUrl("ftp://x/y"));
        Loaded l = UpcomingEvents.load(new Source(Kind.CONTEST, "contests", "file:///etc/passwd", "C", ""), false, Instant.now());
        assertNotNull(l.error());
        Loaded blank = UpcomingEvents.load(new Source(Kind.HAMFEST, "hamfests", "", "H", ""), false, Instant.now());
        assertNull(blank.error(), "an unset source is simply empty");
    }
}
