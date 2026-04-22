package com.hamradio.jsat.service.tle;

import com.hamradio.jsat.model.TleSet;
import com.hamradio.jsat.service.AbstractDataProvider;
import com.hamradio.jsat.service.DataProviderException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches amateur satellite TLEs from Celestrak's amateur group.
 * No API key required.
 */
public class CelestrakTleProvider extends AbstractDataProvider<List<TleSet>> {

    private static final String URL_AMATEUR  =
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=TLE";
    private static final String URL_STATIONS =
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=TLE";
    // Fallback: AMSAT bare TLE
    private static final String URL_AMSAT =
        "https://amsat.org/tle/current/nasabare.txt";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    @Override
    protected List<TleSet> doFetch() throws DataProviderException {
        List<TleSet> result = new ArrayList<>();

        // Amateur satellite group
        String amateurBody = fetchUrl(URL_AMATEUR);
        if (amateurBody != null && !amateurBody.isBlank())
            result.addAll(parseTles(amateurBody));

        // Stations group (includes ISS ZARYA, CSS, etc.)
        String stationsBody = fetchUrl(URL_STATIONS);
        if (stationsBody != null && !stationsBody.isBlank())
            result.addAll(parseTles(stationsBody));

        // Fallback to AMSAT if neither Celestrak group returned usable TLEs
        if (result.isEmpty()) {
            String amsatBody = fetchUrl(URL_AMSAT);
            if (amsatBody != null && !amsatBody.isBlank())
                result.addAll(parseTles(amsatBody));
        }

        if (result.isEmpty())
            throw new DataProviderException("No TLE data available from any source",
                DataProviderException.ErrorCode.NETWORK_ERROR);

        log.info("Loaded {} TLEs from Celestrak/AMSAT", result.size());
        return result;
    }

    private String fetchUrl(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "J-Sat/1.0 WM3J-ARS")
                .timeout(Duration.ofSeconds(20))
                .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return resp.body();
            log.warn("TLE fetch HTTP {} from {}", resp.statusCode(), url);
            return null;
        } catch (Exception e) {
            log.warn("TLE fetch failed {}: {}", url, e.getMessage());
            return null;
        }
    }

    public static List<TleSet> parseTles(String body) {
        List<TleSet> tles = new ArrayList<>();
        String[] lines = body.split("\\r?\\n");
        int i = 0;
        while (i + 2 < lines.length) {
            String l0 = lines[i].trim();
            String l1 = lines[i + 1].trim();
            String l2 = lines[i + 2].trim();
            if (l1.startsWith("1 ") && l2.startsWith("2 ") && l1.length() >= 69 && l2.length() >= 69) {
                try {
                    tles.add(new TleSet(l0, l1, l2));
                    i += 3;
                } catch (Exception e) {
                    i++;
                }
            } else {
                i++;
            }
        }
        return tles;
    }
}
