package com.hamclock.service.astronomy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * Computes local sunrise and sunset times for a given location and date.
 */
public class SunriseSunsetService {

    private final SolarPositionService solarPositionService;

    // Standard sunrise/set uses 90.833° zenith (includes refraction and solar disk)
    private static final double ZENITH = 90.833;

    public SunriseSunsetService(SolarPositionService solarPositionService) {
        this.solarPositionService = solarPositionService;
    }

    /**
     * Compute sunrise time (UTC) for a given location and date.
     *
     * @param lat Latitude degrees
     * @param lon Longitude degrees
     * @param date Date
     * @return UTC sunrise time, or null if no sunrise (polar day/night)
     */
    public LocalTime sunriseUtc(double lat, double lon, LocalDate date) {
        return computeSunEvent(lat, lon, date, true);
    }

    /**
     * Compute sunset time (UTC) for a given location and date.
     */
    public LocalTime sunsetUtc(double lat, double lon, LocalDate date) {
        return computeSunEvent(lat, lon, date, false);
    }

    private LocalTime computeSunEvent(double lat, double lon, LocalDate date, boolean sunrise) {
        // NOAA simplified sunrise/sunset algorithm
        int dayOfYear = date.getDayOfYear();

        // Longitude hour value
        double lonHour = lon / 15.0;

        // Approximate time (hours UTC)
        double t = sunrise
            ? dayOfYear + (6 - lonHour) / 24.0
            : dayOfYear + (18 - lonHour) / 24.0;

        // Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;
        double Mrad = Math.toRadians(M);

        // Sun's true longitude
        double L = M + (1.916 * Math.sin(Mrad)) + (0.020 * Math.sin(2 * Mrad)) + 282.634;
        L = normalize360(L);

        // Right ascension
        double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))));
        RA = normalize360(RA);

        double Lquad = Math.floor(L / 90) * 90;
        double RAquad = Math.floor(RA / 90) * 90;
        RA = (RA + Lquad - RAquad) / 15.0;

        // Declination
        double sinDec = 0.39782 * Math.sin(Math.toRadians(L));
        double cosDec = Math.cos(Math.asin(sinDec));

        // Sun's local hour angle
        double cosH = (Math.cos(Math.toRadians(ZENITH)) - sinDec * Math.sin(Math.toRadians(lat)))
                    / (cosDec * Math.cos(Math.toRadians(lat)));

        if (cosH > 1) return null;  // Sun never rises
        if (cosH < -1) return null; // Sun never sets

        double H;
        if (sunrise) {
            H = 360 - Math.toDegrees(Math.acos(cosH));
        } else {
            H = Math.toDegrees(Math.acos(cosH));
        }
        H /= 15.0;

        // Local mean time of rise/set
        double T = H + RA - (0.06571 * t) - 6.622;

        // UTC time
        double UT = T - lonHour;
        UT = normalize24(UT);

        int hours = (int) UT;
        int minutes = (int) ((UT - hours) * 60);
        int seconds = (int) (((UT - hours) * 60 - minutes) * 60);

        return LocalTime.of(hours, minutes, seconds);
    }

    private double normalize360(double val) {
        val = val % 360;
        if (val < 0) val += 360;
        return val;
    }

    private double normalize24(double val) {
        val = val % 24;
        if (val < 0) val += 24;
        return val;
    }
}
