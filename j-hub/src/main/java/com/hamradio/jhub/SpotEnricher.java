package com.hamradio.jhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hamradio.jhub.model.Spot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SpotEnricher — enriches raw DX spots with DXCC data, geographic calculations,
 * and local time at the spotted station's location.
 *
 * Data sources:
 *   • /dxcc/prefixes.json (embedded in JAR, loaded once at startup)
 *
 * Enrichment added to each Spot:
 *   lat, lon, country, continent, dxcc (entity number)
 *   bearing (true bearing from station to spotted)
 *   distanceKm, distanceMi
 *   localTimeAtSpot (HH:mm at the spotted QTH)
 *   workedStatus (always "unknown" unless logger provides log data)
 */
public class SpotEnricher {

    private static final Logger log = LoggerFactory.getLogger(SpotEnricher.class);

    // Singleton
    private static final SpotEnricher INSTANCE = new SpotEnricher();
    public static SpotEnricher getInstance() { return INSTANCE; }
    private SpotEnricher() {}

    // DXCC entry: prefix → entity data
    private static class DxccEntry {
        String prefix;
        String country;
        String continent;
        int    dxcc;
        double lat;
        double lon;
        String timezone; // e.g. "Europe/Berlin"
    }

    // Prefix map sorted longest-first for greedy prefix matching
    private final TreeMap<String, DxccEntry> prefixMap = new TreeMap<>(
        Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder())
    );

    // ---------------------------------------------------------------
    // Initialisation — load prefix table from classpath
    // ---------------------------------------------------------------

    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/dxcc/prefixes.json");
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                DxccEntry entry = new DxccEntry();
                entry.prefix    = obj.get("prefix").getAsString().toUpperCase();
                entry.country   = obj.get("country").getAsString();
                entry.continent = obj.get("continent").getAsString();
                entry.dxcc      = obj.get("dxcc").getAsInt();
                entry.lat       = obj.get("lat").getAsDouble();
                entry.lon       = obj.get("lon").getAsDouble();
                entry.timezone  = obj.has("timezone") ? obj.get("timezone").getAsString() : "UTC";
                prefixMap.put(entry.prefix, entry);
            }
            log.info("DXCC prefix table loaded: {} entries", prefixMap.size());
        } catch (Exception e) {
            log.error("Failed to load DXCC prefix table: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Enrichment entry point
    // ---------------------------------------------------------------

    /**
     * Enrich a spot in-place with DXCC data and geographic calculations.
     */
    public void enrich(Spot spot) {
        DxccEntry entry = lookupCallsign(spot.spotted);
        if (entry == null) {
            log.debug("No DXCC entry for {}", spot.spotted);
            return;
        }

        spot.country   = entry.country;
        spot.continent = entry.continent;
        spot.dxcc      = entry.dxcc;
        spot.lat       = entry.lat;
        spot.lon       = entry.lon;

        // Geographic calculations from our station
        var stationCfg = ConfigManager.getInstance().getStation();
        if (stationCfg != null && stationCfg.lat != 0 && stationCfg.lon != 0) {
            spot.bearing    = bearing(stationCfg.lat, stationCfg.lon, entry.lat, entry.lon);
            spot.distanceKm = distanceKm(stationCfg.lat, stationCfg.lon, entry.lat, entry.lon);
            spot.distanceMi = spot.distanceKm * 0.621371;
        }

        // Local time at the spotted station
        spot.localTimeAtSpot = localTime(entry.timezone);
    }

    // ---------------------------------------------------------------
    // Prefix lookup (greedy longest-prefix match)
    // ---------------------------------------------------------------

    private DxccEntry lookupCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) return null;

        // Strip /P /M /MM suffixes for lookup
        String stripped = callsign.replaceAll("/(P|M|MM|AM|QRP)$", "").toUpperCase();

        // Try progressively shorter prefixes
        for (int len = stripped.length(); len > 0; len--) {
            String prefix = stripped.substring(0, len);
            DxccEntry entry = prefixMap.get(prefix);
            if (entry != null) return entry;
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Bearing (Haversine / forward azimuth)
    // ---------------------------------------------------------------

    private double bearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dl   = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dl) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) -
                   Math.sin(phi1) * Math.cos(phi2) * Math.cos(dl);

        double theta = Math.toDegrees(Math.atan2(y, x));
        return (theta + 360) % 360;
    }

    // ---------------------------------------------------------------
    // Distance (Haversine)
    // ---------------------------------------------------------------

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dphi = Math.toRadians(lat2 - lat1);
        double dl   = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dphi / 2) * Math.sin(dphi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(dl / 2) * Math.sin(dl / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---------------------------------------------------------------
    // Local time at spotted station
    // ---------------------------------------------------------------

    private static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    private String localTime(String timezone) {
        try {
            ZoneId zone = ZoneId.of(timezone);
            return ZonedDateTime.now(zone).format(HHmm);
        } catch (Exception e) {
            return ZonedDateTime.now(ZoneId.of("UTC")).format(HHmm);
        }
    }
}
