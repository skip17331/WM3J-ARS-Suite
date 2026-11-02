package com.ars.fx.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Where the sun is: the sub-solar point behind the gray line, and how it looks from a station. */
public final class SolarPosition {
    private SolarPosition() {}

    /** Sub-solar point {declinationDeg, subSolarLonDeg}. */
    public static double[] subSolar() { return subSolar(Instant.now()); }

    static double[] subSolar(Instant now) {
        double hoursUtc = (now.getEpochSecond() % 86400) / 3600.0;
        double ssLon = ((-15.0 * (hoursUtc - 12.0) + 540) % 360) - 180;
        int doy = LocalDate.ofInstant(now, ZoneOffset.UTC).getDayOfYear();
        double decl = 23.44 * Math.sin(Math.toRadians(360.0 / 365.0 * (doy - 81)));
        return new double[]{decl, ssLon};
    }

    /** Sun's {azimuth, elevation} in degrees as seen from a station. */
    public static double[] azEl(double latDeg, double lonDeg) {
        double[] ss = subSolar();
        double decl = Math.toRadians(ss[0]), lat = Math.toRadians(latDeg), dlon = Math.toRadians(ss[1] - lonDeg);
        double cosC = Math.sin(lat) * Math.sin(decl) + Math.cos(lat) * Math.cos(decl) * Math.cos(dlon);
        double el = 90 - Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosC))));
        double y = Math.sin(dlon) * Math.cos(decl);
        double x = Math.cos(lat) * Math.sin(decl) - Math.sin(lat) * Math.cos(decl) * Math.cos(dlon);
        return new double[]{(Math.toDegrees(Math.atan2(y, x)) + 360) % 360, el};
    }

    /** Sunrise and sunset today as {srUtcHours, ssUtcHours, 0}; NaN times and +1 (always up) or -1 (never up) near the poles. */
    public static double[] riseSet(double latDeg, double lonDeg) {
        double decl = Math.toRadians(subSolar()[0]), lat = Math.toRadians(latDeg);
        double cosH = -Math.tan(lat) * Math.tan(decl);
        if (cosH < -1) return new double[]{Double.NaN, Double.NaN, 1};
        if (cosH > 1)  return new double[]{Double.NaN, Double.NaN, -1};
        double arc = Math.toDegrees(Math.acos(cosH)) / 15.0;         // half the day, in hours
        double noonUtc = 12.0 - lonDeg / 15.0;
        return new double[]{((noonUtc - arc) % 24 + 24) % 24, ((noonUtc + arc) % 24 + 24) % 24, 0};
    }

    /** Hours → "HH:MM". */
    public static String hhmm(double h) {
        int m = (int) Math.round(h * 60) % 1440; if (m < 0) m += 1440;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    /** The sub-solar point as text, e.g. "15.7°S 61.6°W". */
    public static String subSolarText() {
        double[] ss = subSolar();
        return String.format("%.1f°%s %.1f°%s", Math.abs(ss[0]), ss[0] >= 0 ? "N" : "S", Math.abs(ss[1]), ss[1] >= 0 ? "E" : "W");
    }
}
