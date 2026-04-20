package com.hamradio.jsat.model;

import java.time.Instant;

/**
 * Parsed two-line element set.
 */
public class TleSet {

    public final String name;
    public final String noradId;
    public final String line1;
    public final String line2;

    // Parsed orbital elements
    public final double inclination;   // degrees
    public final double raan;          // degrees (right ascension of ascending node)
    public final double eccentricity;  // dimensionless
    public final double argPerigee;    // degrees
    public final double meanAnomaly;   // degrees
    public final double meanMotion;    // revolutions/day
    public final double bstar;         // drag term (1/earth_radii)

    public final int    epochYear;     // 2-digit year
    public final double epochDay;      // day of year with fractional day

    public final Instant fetchedAt;

    public TleSet(String name, String line1, String line2) {
        this.name   = name.trim();
        this.line1  = line1.trim();
        this.line2  = line2.trim();
        this.noradId = line1.substring(2, 7).trim();
        this.fetchedAt = Instant.now();

        // Parse epoch from line 1: cols 18-32
        int ey = Integer.parseInt(line1.substring(18, 20).trim());
        this.epochYear = ey < 57 ? 2000 + ey : 1900 + ey;
        this.epochDay  = Double.parseDouble(line1.substring(20, 32).trim());

        // Parse B* drag from line 1: cols 53-61 (decimal assumed 0.)
        this.bstar = parseDecimalAssumed(line1.substring(53, 61).trim());

        // Parse line 2 elements
        this.inclination  = Double.parseDouble(line2.substring(8,  16).trim());
        this.raan         = Double.parseDouble(line2.substring(17, 25).trim());
        this.eccentricity = Double.parseDouble("0." + line2.substring(26, 33).trim());
        this.argPerigee   = Double.parseDouble(line2.substring(34, 42).trim());
        this.meanAnomaly  = Double.parseDouble(line2.substring(43, 51).trim());
        this.meanMotion   = Double.parseDouble(line2.substring(52, 63).trim());
    }

    /** Parse TLE exponential notation (e.g. " 12345-3" → 0.00012345). */
    private static double parseDecimalAssumed(String s) {
        if (s.isBlank()) return 0.0;
        s = s.trim();
        // Find exponent: last +/- that isn't the sign of the mantissa
        int expIdx = Math.max(s.lastIndexOf('+'), s.lastIndexOf('-'));
        if (expIdx > 0) {
            String mantissa = s.substring(0, expIdx);
            int exp = Integer.parseInt(s.substring(expIdx));
            double m = Double.parseDouble("0." + mantissa.replace("-","").replace("+","").replace(" ",""));
            if (mantissa.startsWith("-")) m = -m;
            return m * Math.pow(10, exp);
        }
        return Double.parseDouble(s);
    }

    /** Age of elements in days from now. */
    public double ageInDays() {
        Instant epochInstant = epochToInstant();
        double secAgo = (Instant.now().getEpochSecond() - epochInstant.getEpochSecond());
        return secAgo / 86400.0;
    }

    public Instant epochToInstant() {
        // Year + day-of-year → Unix epoch
        long year = epochYear;
        long daysInYear = (long) epochDay;
        double fracDay = epochDay - daysInYear;
        // Jan 1 of epochYear
        java.time.LocalDate jan1 = java.time.LocalDate.of((int) year, 1, 1);
        java.time.LocalDate epochDate = jan1.plusDays(daysInYear - 1);
        long epochSec = epochDate.toEpochDay() * 86400L + (long)(fracDay * 86400.0);
        return Instant.ofEpochSecond(epochSec);
    }

    /** Freshness indicator: GREEN < 3 days, YELLOW < 7 days, RED >= 7 days. */
    public TleFreshness freshness() {
        double age = ageInDays();
        if (age < 3)  return TleFreshness.GREEN;
        if (age < 7)  return TleFreshness.YELLOW;
        return TleFreshness.RED;
    }

    public enum TleFreshness { GREEN, YELLOW, RED }
}
