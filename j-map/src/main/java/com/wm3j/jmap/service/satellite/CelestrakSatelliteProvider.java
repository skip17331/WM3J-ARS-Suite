package com.wm3j.jmap.service.satellite;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches amateur satellite TLE data from Celestrak and computes positions
 * using a simplified SGP4-like propagator.
 *
 * Full SGP4 implementation requires the full algorithm (Vallado 2013).
 * This implementation fetches TLEs and provides approximate circular-orbit
 * positions sufficient for a map overlay. For precise tracking, integrate
 * a full SGP4 library (e.g., predict4java or orekit).
 */
public class CelestrakSatelliteProvider extends AbstractDataProvider<SatelliteData>
        implements SatelliteProvider {

    private static final Logger log = LoggerFactory.getLogger(CelestrakSatelliteProvider.class);

    // Celestrak amateur satellite group
    private static final String TLE_URL =
        "https://celestrak.org/SOCRATES/query.php?GROUP=amateur&FORMAT=tle";

    private static final String TLE_AMATEUR =
        "https://celestrak.org/SOCRATES/query.php?GROUP=amateur&FORMAT=tle";

    // Use the simpler stations endpoint for AMSAT
    private static final String AMSAT_TLE =
        "https://amsat.org/tle/current/nasabare.txt";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected SatelliteData doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(AMSAT_TLE))
                .header("User-Agent", "J-Map/1.0")
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("Celestrak HTTP " + resp.statusCode(), DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            return parseTleAndPropagate(resp.body());

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Satellite fetch failed: " + e.getMessage(), DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private SatelliteData parseTleAndPropagate(String body) {
        List<SatelliteData.SatPosition> positions = new ArrayList<>();
        String[] lines = body.split("\n");

        int i = 0;
        int count = 0;
        while (i + 2 < lines.length && count < 20) {
            String name = lines[i].trim();
            String tle1 = lines[i + 1].trim();
            String tle2 = lines[i + 2].trim();

            if (tle1.startsWith("1 ") && tle2.startsWith("2 ")) {
                try {
                    SatelliteData.SatPosition pos = propagateSimple(name, tle1, tle2);
                    if (pos != null) {
                        positions.add(pos);
                        count++;
                    }
                } catch (Exception e) {
                    log.debug("Failed to propagate {}: {}", name, e.getMessage());
                }
                i += 3;
            } else {
                i++;
            }
        }

        log.debug("Propagated {} satellites from TLE data", positions.size());
        return new SatelliteData(positions, Instant.now());
    }

    /**
     * Simplified circular orbit propagation from TLE.
     * Parses inclination, RAAN, mean motion; ignores eccentricity and drag.
     */
    private SatelliteData.SatPosition propagateSimple(String name, String tle1, String tle2) {
        // Parse TLE line 2 fields
        // Col  1: satellite number
        //  8-16: inclination (degrees)
        // 17-25: RAAN (degrees)
        // 26-33: eccentricity (decimal point assumed)
        // 34-42: argument of perigee
        // 43-51: mean anomaly
        // 52-63: mean motion (rev/day)
        try {
            double incl       = Double.parseDouble(tle2.substring(8, 16).trim());
            double raan       = Double.parseDouble(tle2.substring(17, 25).trim());
            double meanAnomaly= Double.parseDouble(tle2.substring(43, 51).trim());
            double meanMotion = Double.parseDouble(tle2.substring(52, 63).trim());
            String noradId    = tle1.substring(2, 7).trim();

            // Period in minutes
            double periodMin = 1440.0 / meanMotion;
            // Semi-major axis from Kepler: T^2 = (4π²/GM) a³ → a = (GM/n²)^(1/3)
            double n = meanMotion * 2 * Math.PI / 86400.0; // rad/s
            double GM = 3.986004418e14;
            double a  = Math.pow(GM / (n * n), 1.0 / 3.0);
            double altKm = (a - 6371000) / 1000.0;

            // Current mean anomaly based on epoch + elapsed time
            // (simplified: use meanAnomaly from TLE as starting point,
            //  advance by time since J2000 epoch)
            double nowMin = System.currentTimeMillis() / 60_000.0;
            double currentAnomaly = (meanAnomaly + (nowMin / periodMin) * 360.0) % 360.0;

            // Position in orbital plane
            double anomRad = Math.toRadians(currentAnomaly);
            double inclRad = Math.toRadians(incl);
            double raanRad = Math.toRadians(raan);

            // Unit vector in orbital plane
            double x = Math.cos(anomRad);
            double y = Math.sin(anomRad);

            // Rotate by inclination and RAAN
            double cx = x;
            double cy = y * Math.cos(inclRad);
            double cz = y * Math.sin(inclRad);

            // Rotate by RAAN around Z axis
            double ex = cx * Math.cos(raanRad) - cy * Math.sin(raanRad);
            double ey = cx * Math.sin(raanRad) + cy * Math.cos(raanRad);
            double ez = cz;

            // Sub-satellite lat/lon (ignoring Earth rotation for simplicity)
            double satLat = Math.toDegrees(Math.asin(ez));
            double satLon = Math.toDegrees(Math.atan2(ey, ex));
            // Account for Earth rotation (Earth rotates ~360°/1440min)
            satLon = ((satLon - nowMin * 0.25 + 180) % 360) - 180;

            // Simple ground track: 90 steps of 1 minute
            List<double[]> track = new ArrayList<>();
            for (int t = 0; t < 90; t++) {
                double futAnomaly = (currentAnomaly + t * 360.0 / periodMin) % 360.0;
                double fRad = Math.toRadians(futAnomaly);
                double fx = Math.cos(fRad);
                double fy = Math.sin(fRad);
                double fcx = fx;
                double fcy = fy * Math.cos(inclRad);
                double fcz = fy * Math.sin(inclRad);
                double fex = fcx * Math.cos(raanRad) - fcy * Math.sin(raanRad);
                double fey = fcx * Math.sin(raanRad) + fcy * Math.cos(raanRad);
                double fez = fcz;
                double tLat = Math.toDegrees(Math.asin(fez));
                double tLon = Math.toDegrees(Math.atan2(fey, fex));
                tLon = ((tLon - (nowMin + t) * 0.25 + 180) % 360) - 180;
                track.add(new double[]{tLat, tLon});
            }

            return new SatelliteData.SatPosition(name, noradId, satLat, satLon, altKm, track);

        } catch (Exception e) {
            return null;
        }
    }
}
