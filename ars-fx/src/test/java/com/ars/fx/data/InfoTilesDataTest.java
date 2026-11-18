package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The data behind the Info page's Active now, Your stats and Reference tiles (punch list 5-6). */
class InfoTilesDataTest {

    // two entries in the shape api.pota.app/spot/activator sends (checked against the live list on 2026-11-18)
    static final String POTA = "[{\"spotId\":1,\"activator\":\"ke2hhf\",\"frequency\":\"14074\",\"mode\":\"FT8\",\"reference\":\"US-3057\","
            + "\"parkName\":null,\"spotTime\":\"2026-11-18T17:24:26\",\"name\":\"South Llano River State Park\",\"locationDesc\":\"US-TX\"},"
            + "{\"spotId\":2,\"activator\":\"VE3XYZ\",\"frequency\":\"7032.5\",\"mode\":\"CW\",\"reference\":\"CA-0123\",\"spotTime\":\"2026-11-18T16:00:00\",\"name\":\"Algonquin\"},"
            + "{\"spotId\":3,\"activator\":\"KE2HHF\",\"frequency\":\"7074\",\"mode\":\"FT8\",\"reference\":\"US-3057\",\"spotTime\":\"2026-11-18T17:10:00\",\"name\":\"South Llano River State Park\"}]";
    static final Instant NOW = Instant.parse("2026-11-18T17:30:00Z");

    @Test void potaSpotsAreRead() {
        List<ActiveNow.Activation> l = ActiveNow.parsePota(POTA);
        assertEquals(3, l.size());
        ActiveNow.Activation a = l.get(0);
        assertEquals("KE2HHF", a.call());
        assertEquals(14074, a.freqKhz(), 1e-9);
        assertEquals("US-3057", a.ref());
        assertEquals(Instant.parse("2026-11-18T17:24:26Z"), a.spotted());
        assertEquals(List.of(), ActiveNow.parsePota("not json"));
        assertEquals(List.of(), ActiveNow.parsePota("{\"error\":1}"));
    }

    @Test void currentKeepsTheNewestSpotPerActivatorInTheLastHourAndTagsNeeds() {
        List<ActiveNow.Activation> l = ActiveNow.current(ActiveNow.parsePota(POTA), NOW, Set.of(291), Set.of("US-3057"),
                call -> call.startsWith("VE") ? 1 : 291);
        assertEquals(1, l.size(), "VE3XYZ's spot is 90 minutes old; KE2HHF's two spots are one");
        assertEquals(14074, l.get(0).freqKhz(), 1e-9, "the newest of KE2HHF's spots");
        assertEquals("", l.get(0).need(), "US worked, park worked");
        List<ActiveNow.Activation> l2 = ActiveNow.current(ActiveNow.parsePota(POTA), NOW.minusSeconds(3600), Set.of(291), Set.of(), call -> call.startsWith("VE") ? 1 : 291);
        assertEquals("NEW ONE", l2.stream().filter(a -> a.call().equals("VE3XYZ")).findFirst().orElseThrow().need(), "Canada not worked");
        assertEquals("NEW PARK", l2.stream().filter(a -> a.call().equals("KE2HHF")).findFirst().orElseThrow().need());
    }

    @Test void dxpeditionsOnTheAirToday() {
        LocalDate d = LocalDate.of(2026, 11, 18);
        var on = new UpcomingEvents.Event(UpcomingEvents.Kind.DXPEDITION, "3Y0J Bouvet", d.minusDays(3), d.plusDays(10), "", "", "", "");
        var later = new UpcomingEvents.Event(UpcomingEvents.Kind.DXPEDITION, "VP8 later", d.plusDays(2), d.plusDays(9), "", "", "", "");
        var contest = new UpcomingEvents.Event(UpcomingEvents.Kind.CONTEST, "CQ WW", d, d, "", "", "", "");
        var oneDay = new UpcomingEvents.Event(UpcomingEvents.Kind.DXPEDITION, "one day", d, null, "", "", "", "");
        assertEquals(List.of(on, oneDay), ActiveNow.dxpeditionsToday(List.of(on, later, contest, oneDay), d));
    }

    @Test void stationStats() {
        JLogDb.Refs lotw = new JLogDb.Refs("", "", "", 0, 0, false, true, false), qsl = new JLogDb.Refs("", "", "", 0, 0, true, false, true);
        List<JLogDb.QsoExport> rows = List.of(
                q("2026-11-18 10:00:00", lotw), q("2026-11-02 10:00:00", qsl), q("2026-03-01 10:00:00", null), q("2025-12-31 23:59:00", null));
        Awards.Snapshot snap = Awards.compute(rows);
        StationStats.Stats s = StationStats.compute(rows, snap, LocalDate.of(2026, 11, 18), 3, 1);
        assertEquals(1, s.today()); assertEquals(2, s.thisMonth()); assertEquals(3, s.thisYear()); assertEquals(4, s.total());
        assertEquals(1, s.lotwConfirmed()); assertEquals(1, s.eqslConfirmed()); assertEquals(1, s.qslConfirmed());
        assertEquals(3, s.lotwPending()); assertEquals(1, s.eqslPending());
    }

    private static JLogDb.QsoExport q(String when, JLogDb.Refs r) {
        return new JLogDb.QsoExport("DL1ABC", when, "20m", "CW", "14.025", 0, "599", "599", "", "", "", "", "", "", r == null ? JLogDb.Refs.NONE : r);
    }

    @Test void sunriseAndSunset() {
        // London on the summer solstice: about 03:43 and 20:21 UTC
        DayReference.SunTimes ldn = DayReference.sun(51.5074, -0.1278, LocalDate.of(2026, 6, 21));
        assertTrue(Math.abs(ldn.rise().toSecondOfDay() - LocalTime.of(3, 43).toSecondOfDay()) <= 180, "rise " + ldn.rise());
        assertTrue(Math.abs(ldn.set().toSecondOfDay() - LocalTime.of(20, 21).toSecondOfDay()) <= 180, "set " + ldn.set());
        assertNull(DayReference.sun(69.65, 18.96, LocalDate.of(2026, 6, 21)), "midnight sun at Tromso");
        assertEquals("0313-0413Z", DayReference.greyline(LocalTime.of(3, 43)));
    }

    @Test void licenceReminder() {
        LocalDate d = LocalDate.of(2026, 11, 18);
        assertEquals("", DayReference.licenceReminder("2031-04-12", d));
        assertTrue(DayReference.licenceReminder("2027-02-01", d).contains("75 days"));
        assertTrue(DayReference.licenceReminder("2026-10-01", d).contains("expired"));
        assertEquals("", DayReference.licenceReminder("April 2031", d), "not a date: no reminder");
        assertEquals("", DayReference.licenceReminder("", d));
    }

    @Test void tipOfTheDayFromTheJLearnChapters() {
        List<String> titles = DayReference.learnTitles(Path.of("..", "j-learn-content"));
        assertTrue(titles.size() > 200, "J-Learn's sections: " + titles.size());
        assertTrue(titles.contains("Propagation — Overview"), "front-matter titles: " + titles.subList(0, 3));
        assertEquals(DayReference.tipOfTheDay(titles, 322), DayReference.tipOfTheDay(titles, 322), "the same one all day");
        assertNull(DayReference.tipOfTheDay(List.of(), 5));
    }
}
