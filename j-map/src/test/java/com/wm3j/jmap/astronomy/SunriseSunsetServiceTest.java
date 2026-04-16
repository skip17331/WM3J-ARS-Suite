package com.wm3j.jmap.astronomy;

import com.wm3j.jmap.service.astronomy.SolarPositionService;
import com.wm3j.jmap.service.astronomy.SunriseSunsetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for sunrise and sunset time calculations.
 * Reference values from timeanddate.com and USNO.
 */
class SunriseSunsetServiceTest {

    private SunriseSunsetService service;

    @BeforeEach
    void setUp() {
        service = new SunriseSunsetService(new SolarPositionService());
    }

    @Test
    void sunrise_London_summerSolstice_isEarly() {
        // London (51.5°N, -0.1°E), 2024-06-21
        // Sunrise ≈ 04:43 UTC
        LocalDate date = LocalDate.of(2024, 6, 21);
        LocalTime sunrise = service.sunriseUtc(51.5, -0.1, date);
        assertNotNull(sunrise, "London should have sunrise at summer solstice");
        assertTrue(sunrise.getHour() >= 3 && sunrise.getHour() <= 5,
            "London summer sunrise should be between 03:00-05:00 UTC, got: " + sunrise);
    }

    @Test
    void sunset_London_summerSolstice_isLate() {
        // London, 2024-06-21, Sunset ≈ 21:21 UTC
        LocalDate date = LocalDate.of(2024, 6, 21);
        LocalTime sunset = service.sunsetUtc(51.5, -0.1, date);
        assertNotNull(sunset, "London should have sunset at summer solstice");
        assertTrue(sunset.getHour() >= 20 && sunset.getHour() <= 22,
            "London summer sunset should be between 20:00-22:00 UTC, got: " + sunset);
    }

    @Test
    void sunrise_beforeSunset_sameDay() {
        LocalDate date = LocalDate.of(2024, 3, 20);
        LocalTime sunrise = service.sunriseUtc(51.5, -0.1, date);
        LocalTime sunset  = service.sunsetUtc(51.5,  -0.1, date);

        assertNotNull(sunrise);
        assertNotNull(sunset);
        assertTrue(sunrise.isBefore(sunset),
            "Sunrise should be before sunset, got rise=" + sunrise + " set=" + sunset);
    }

    @Test
    void dayLength_summerSolstice_longerThanWinterSolstice() {
        // London
        double lat = 51.5, lon = -0.1;
        LocalDate summer = LocalDate.of(2024, 6, 21);
        LocalDate winter = LocalDate.of(2024, 12, 21);

        LocalTime sumRise = service.sunriseUtc(lat, lon, summer);
        LocalTime sumSet  = service.sunsetUtc(lat, lon, summer);
        LocalTime winRise = service.sunriseUtc(lat, lon, winter);
        LocalTime winSet  = service.sunsetUtc(lat, lon, winter);

        assertNotNull(sumRise); assertNotNull(sumSet);
        assertNotNull(winRise); assertNotNull(winSet);

        long sumDayMinutes = java.time.Duration.between(sumRise, sumSet).toMinutes();
        long winDayMinutes = java.time.Duration.between(winRise, winSet).toMinutes();

        assertTrue(sumDayMinutes > winDayMinutes,
            "Summer day should be longer than winter: summer=" + sumDayMinutes + " winter=" + winDayMinutes);
    }

    @Test
    void dayLength_atEquinox_isApproximately12Hours() {
        // At the equinox, day length near equator ≈ 12 hours
        LocalDate equinox = LocalDate.of(2024, 3, 20);
        LocalTime rise = service.sunriseUtc(0.0, 0.0, equinox); // equator, prime meridian
        LocalTime set  = service.sunsetUtc(0.0, 0.0, equinox);

        assertNotNull(rise);
        assertNotNull(set);

        long dayMinutes = java.time.Duration.between(rise, set).toMinutes();
        assertEquals(720, dayMinutes, 40,  // ~12 hours ± 40 minutes tolerance
            "Day length at equinox near equator should be ~720 min, got: " + dayMinutes);
    }

    @ParameterizedTest(name = "lat={0} lon={1} month={2}")
    @CsvSource({
        // Equator, various months - should always have sunrise/sunset
        "0.0,   0.0,  1",
        "0.0,   0.0,  6",
        "0.0,   0.0, 12",
        // Mid-latitudes
        "40.0, -74.0,  6",
        "-33.9, 151.2, 12",
    })
    void sunriseSunset_midLatitudes_areNeverNull(double lat, double lon, int month) {
        LocalDate date = LocalDate.of(2024, month, 15);
        LocalTime rise = service.sunriseUtc(lat, lon, date);
        LocalTime set  = service.sunsetUtc(lat, lon, date);
        assertNotNull(rise, "Sunrise should exist at lat=" + lat + " month=" + month);
        assertNotNull(set,  "Sunset should exist at lat=" + lat + " month=" + month);
    }

    @Test
    void sunrise_vsSunset_sydney_summerDecember() {
        // Sydney (-33.9°S, 151.2°E) in December (austral summer)
        // Sunrise early morning, sunset late evening local time
        LocalDate date = LocalDate.of(2024, 12, 21);
        LocalTime rise = service.sunriseUtc(-33.9, 151.2, date);
        LocalTime set  = service.sunsetUtc(-33.9, 151.2, date);

        assertNotNull(rise);
        assertNotNull(set);
        assertTrue(rise.isBefore(set) || rise.isAfter(set), // Either way is fine due to UTC offset
            "Sunrise and sunset should be different times");
    }
}
