package com.ars.fx.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The Info page's "Reference" tile (punch list 5-6): sunrise and sunset at your station today (UTC) with the greyline windows around
 * them, a reminder when your licence is due for renewal (from J-Vault's licence details), and a J-Learn section as a tip of the day.
 */
public final class DayReference {
    private DayReference() {}

    /** Sunrise and sunset (UTC); null for a polar day or night. */
    public record SunTimes(LocalTime rise, LocalTime set) {}

    /**
     * Sunrise and sunset at {@code lat}/{@code lon} on {@code date}, UTC, to about a minute (the NOAA approximation: the sun's upper limb
     * with standard refraction, 90.833 degrees from the zenith). Null when the sun does not rise or does not set that day.
     */
    public static SunTimes sun(double lat, double lon, LocalDate date) {
        int n = date.getDayOfYear();
        double g = 2 * Math.PI / 365 * (n - 1);                                        // the fractional year, at noon
        double eqTime = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g) - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
        double decl = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g) - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g)
                - 0.002697 * Math.cos(3 * g) + 0.00148 * Math.sin(3 * g);
        double latR = Math.toRadians(lat);
        double cosH = Math.cos(Math.toRadians(90.833)) / (Math.cos(latR) * Math.cos(decl)) - Math.tan(latR) * Math.tan(decl);
        if (cosH > 1 || cosH < -1) return null;
        double ha = Math.toDegrees(Math.acos(cosH));
        double riseMin = 720 - 4 * (lon + ha) - eqTime, setMin = 720 - 4 * (lon - ha) - eqTime;
        return new SunTimes(minutes(riseMin), minutes(setMin));
    }

    private static LocalTime minutes(double m) {
        long s = Math.round(((m % 1440) + 1440) % 1440 * 60);
        return LocalTime.ofSecondOfDay(s % 86_400).truncatedTo(ChronoUnit.MINUTES);
    }

    /** "1105-1205Z" for the hour of greyline centred on {@code t}: the half hour either side, when low-band DX is often best. */
    public static String greyline(LocalTime t) {
        if (t == null) return "";
        return hhmm(t.minusMinutes(30)) + "-" + hhmm(t.plusMinutes(30)) + "Z";
    }

    public static String hhmm(LocalTime t) { return String.format("%02d%02d", t.getHour(), t.getMinute()); }

    /**
     * A reminder when the licence expiry date in J-Vault ({@code yyyy-mm-dd}) is within 180 days or past; "" otherwise or when it is
     * not set or not a date.
     */
    public static String licenceReminder(String expires, LocalDate today) {
        if (expires == null || expires.isBlank()) return "";
        LocalDate d;
        try { d = LocalDate.parse(expires.trim()); } catch (DateTimeParseException e) { return ""; }
        long days = ChronoUnit.DAYS.between(today, d);
        if (days < 0) return "Your licence expired on " + d + ": renew it.";
        if (days <= 180) return "Your licence expires on " + d + " (" + days + " days): renewal opens 90 days before.";
        return "";
    }

    /** A J-Learn section title, the same one all day: {@code index} picks it (the day of the year). Null when there are none. */
    public static String tipOfTheDay(List<String> titles, int index) {
        if (titles == null || titles.isEmpty()) return null;
        return titles.get(Math.floorMod(index, titles.size()));
    }

    /** The section titles under J-Learn's content folder, in file order: each file's front-matter {@code title:}, else its first "# " heading. */
    public static List<String> learnTitles(Path dir) {
        List<String> out = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.walk(dir, 2)) {
            for (Path f : (Iterable<Path>) s.filter(p -> p.getFileName().toString().matches("\\d\\d-\\d\\d.*\\.md")).sorted()::iterator) {
                try (Stream<String> lines = Files.lines(f, StandardCharsets.UTF_8)) {
                    lines.limit(40).filter(l -> l.startsWith("title:") || l.startsWith("# ")).findFirst()
                         .ifPresent(l -> out.add((l.startsWith("title:") ? l.substring(6) : l.substring(2)).trim()));
                } catch (IOException | java.io.UncheckedIOException ignored) { }
            }
        } catch (IOException ignored) { }
        return out;
    }
}
