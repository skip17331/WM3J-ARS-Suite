package com.wm3j.jmap.service.solar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Fetches solar and space weather data from NOAA SWPC JSON feeds.
 *
 * Endpoints:
 *   products/noaa-planetary-k-index.json   — Kp (array of objects, field "Kp" capital K)
 *   products/solar-wind/plasma-1-day.json  — Solar wind speed, density (array of arrays)
 *   products/solar-wind/mag-1-day.json     — IMF Bz, Bt (array of arrays)
 *   json/goes/primary/xrays-1-day.json     — GOES X-ray flux
 *   json/goes/primary/protons-1-day.json   — GOES proton flux ≥10 MeV
 */
public class NoaaSolarDataProvider extends AbstractDataProvider<SolarData>
        implements SolarDataProvider {

    private static final String BASE     = "https://services.swpc.noaa.gov/json/";
    private static final String PRODUCTS = "https://services.swpc.noaa.gov/products/";

    private static final String KP_URL      = PRODUCTS + "noaa-planetary-k-index.json";
    private static final String PLASMA_URL  = PRODUCTS + "solar-wind/plasma-1-day.json";
    private static final String MAG_URL     = PRODUCTS + "solar-wind/mag-1-day.json";
    private static final String XRAY_URL    = BASE + "goes/primary/xrays-1-day.json";
    private static final String PROTON_URL  = BASE + "goes/primary/protons-1-day.json";
    /** NOAA daily F10.7 (10.7 cm solar flux index). Single-element
     *  array of {flux, time_tag}; refreshed three times per day. */
    private static final String SFI_URL     = PRODUCTS + "summary/10cm-flux.json";
    /** NOAA observed solar-cycle indices — monthly SSN (international
     *  Wolf number from SIDC/SILSO + the SWPC-corrected variant).
     *  Array of monthly objects going back to 1749. */
    private static final String SSN_URL     = BASE + "solar-cycle/observed-solar-cycle-indices.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    protected SolarData doFetch() throws DataProviderException {
        SolarData data = new SolarData();
        SolarData prev = getCached();   // for per-field last-known fallback

        fetchKp(data);
        fetchSolarWindPlasma(data);
        fetchSolarWindMag(data);
        fetchXray(data);
        fetchProtons(data);
        fetchSfi(data);
        fetchSsn(data);

        // Per-field preservation: if a sub-fetch failed (left the
        // field at its primitive default of 0 / 0.0 / null) AND we
        // have a previous successful value, keep showing it instead
        // of regressing to "0". Better stale-but-recognisable than
        // suddenly-zero — and the data we're stand-in-ing for
        // (especially SSN, which is a monthly aggregate) does not
        // change on the timescale of a single failed poll anyway.
        if (prev != null) {
            if (data.getSfi() <= 0 && prev.getSfi() > 0) {
                data.setSfi(prev.getSfi());
                data.setSfiObservedAt(prev.getSfiObservedAt());
                log.warn("SFI sub-fetch produced no value; preserving prior {} sfu", (int)prev.getSfi());
            }
            if (data.getSunspotNumber() == 0 && prev.getSunspotNumber() > 0) {
                data.setSunspotNumber(prev.getSunspotNumber());
                data.setSunspotNumberMonth(prev.getSunspotNumberMonth());
                log.warn("SSN sub-fetch produced no value; preserving prior {} ({})",
                         prev.getSunspotNumber(), prev.getSunspotNumberMonth());
            }
            if (data.getKp() <= 0 && prev.getKp() > 0) {
                data.setKp(prev.getKp());
                data.setAIndex(prev.getAIndex());
            }
            if (data.getSolarWindSpeed() <= 0 && prev.getSolarWindSpeed() > 0) {
                data.setSolarWindSpeed(prev.getSolarWindSpeed());
                data.setSolarWindDensity(prev.getSolarWindDensity());
                // Carry the prior observation time forward so the UI
                // can flag "this value is N minutes old", instead of
                // silently presenting a stale value as fresh.
                data.setSolarWindObservedAt(prev.getSolarWindObservedAt());
                log.warn("Solar wind sub-fetch produced no value; preserving prior {} km/s",
                         (int)prev.getSolarWindSpeed());
            }
            if (data.getImfObservedAt() == null && prev.getImfObservedAt() != null
                    && data.getBzField() == 0 && prev.getBzField() != 0) {
                data.setBzField(prev.getBzField());
                data.setBtField(prev.getBtField());
                data.setImfObservedAt(prev.getImfObservedAt());
                log.warn("IMF sub-fetch produced no value; preserving prior Bz={} nT",
                         prev.getBzField());
            }
            if (data.getXrayClass() == null && prev.getXrayClass() != null) {
                data.setXrayFlux(prev.getXrayFlux());
            }
        }

        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    // ── Kp index ───────────────────────────────────────────────
    // Array of objects with field "Kp" (capital K).  Walk backwards for last valid value.
    private void fetchKp(SolarData data) {
        try {
            JsonNode root = get(KP_URL);
            if (!root.isArray() || root.size() == 0) return;
            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode row = root.get(i);
                JsonNode kpNode = row.path("Kp");
                if (!kpNode.isMissingNode() && !kpNode.isNull()) {
                    double kp = kpNode.asDouble(-1);
                    if (kp >= 0) {
                        data.setKp(kp);
                        data.setAIndex(kpToAIndex(kp));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Kp fetch failed: {}", e.getMessage());
        }
    }

    // ── Solar wind plasma ──────────────────────────────────────
    // Array of arrays. Row 0 is column headers; subsequent rows are
    // 1-minute samples in time order. Columns:
    //   [time_tag, density, speed, temperature]
    //
    // DSCOVR has gaps. We walk from newest backward and take the
    // first row whose speed parses as a positive number, then also
    // grab that row's density and time_tag — so the UI can see how
    // fresh the value really is (gap of 90 minutes looks different
    // from gap of 30 seconds).
    private void fetchSolarWindPlasma(SolarData data) {
        try {
            JsonNode root = get(PLASMA_URL);
            if (!root.isArray() || root.size() < 2) return;
            for (int i = root.size() - 1; i >= 1; i--) {
                JsonNode row = root.get(i);
                double speed = parseCell(row.get(2));
                if (!Double.isNaN(speed) && speed > 0) {
                    data.setSolarWindSpeed(speed);
                    double dens = parseCell(row.get(1));
                    data.setSolarWindDensity(Double.isNaN(dens) ? 0 : dens);
                    data.setSolarWindObservedAt(parseSwpcTimeTag(row.get(0)));
                    return;
                }
            }
            log.warn("Solar wind plasma feed had no valid speed in {} rows — DSCOVR gap?",
                     root.size() - 1);
        } catch (Exception e) {
            log.warn("Solar wind plasma fetch failed: {}", e.getMessage());
        }
    }

    // ── Solar wind mag (IMF) ───────────────────────────────────
    // Array of arrays. Row 0 = headers. Columns:
    //   [time_tag, bx_gsm, by_gsm, bz_gsm, lon_gsm, lat_gsm, bt]
    private void fetchSolarWindMag(SolarData data) {
        try {
            JsonNode root = get(MAG_URL);
            if (!root.isArray() || root.size() < 2) return;
            for (int i = root.size() - 1; i >= 1; i--) {
                JsonNode row = root.get(i);
                double bz = parseCell(row.get(3));
                if (!Double.isNaN(bz)) {
                    data.setBzField(bz);
                    double bt = parseCell(row.get(6));
                    data.setBtField(Double.isNaN(bt) ? 0 : bt);
                    data.setImfObservedAt(parseSwpcTimeTag(row.get(0)));
                    return;
                }
            }
            log.warn("Solar wind mag feed had no valid Bz in {} rows — DSCOVR gap?",
                     root.size() - 1);
        } catch (Exception e) {
            log.warn("Solar wind mag fetch failed: {}", e.getMessage());
        }
    }

    /** Parse SWPC's time-tag format ("YYYY-MM-DD HH:MM:SS.SSS" with a
     *  space separator, no timezone suffix — all NOAA times are UTC).
     *  Returns null on any parse failure so the caller can treat it
     *  as "we don't know how old this is". */
    private static Instant parseSwpcTimeTag(JsonNode cell) {
        if (cell == null || cell.isNull()) return null;
        String s = cell.asText("").trim();
        if (s.isEmpty()) return null;
        try {
            // Normalise to ISO-8601 (replace space with T, append Z).
            String iso = s.replace(' ', 'T') + "Z";
            return Instant.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    // ── GOES X-ray flux ────────────────────────────────────────
    // Array of {time_tag, flux, energy}. Use 0.1-0.8nm (XRS-B) channel.
    // Flux in W/m². Most recent data is at end of array.
    private void fetchXray(SolarData data) {
        try {
            JsonNode root = get(XRAY_URL);
            if (!root.isArray() || root.size() == 0) return;

            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                String energy = entry.path("energy").asText("");
                if ("0.1-0.8nm".equals(energy)) {
                    double flux = entry.path("flux").asDouble(-1);
                    if (flux > 0) {
                        data.setXrayFlux(flux); // also sets xrayClass via setter
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("X-ray fetch failed: {}", e.getMessage());
        }
    }

    // ── GOES proton flux ───────────────────────────────────────
    // Array of {time_tag, flux, energy}. Use >=10 MeV channel.
    // Flux in pfu (particles/cm²/s/sr).
    private void fetchProtons(SolarData data) {
        try {
            JsonNode root = get(PROTON_URL);
            if (!root.isArray() || root.size() == 0) return;

            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                String energy = entry.path("energy").asText("");
                if (">=10 MeV".equals(energy)) {
                    double flux = entry.path("flux").asDouble(-1);
                    if (flux >= 0) {
                        data.setProtonFlux(flux);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Proton fetch failed: {}", e.getMessage());
        }
    }

    // ── F10.7 solar flux index ─────────────────────────────────
    // Endpoint: products/summary/10cm-flux.json
    // Shape:    [{ "flux": 111, "time_tag": "2026-05-12T20:00:00" }]
    // NOAA publishes three observations per day (morning / noon /
    // afternoon UTC).
    private void fetchSfi(SolarData data) {
        try {
            JsonNode root = get(SFI_URL);
            if (!root.isArray() || root.size() == 0) return;
            JsonNode latest = root.get(0);
            double flux = latest.path("flux").asDouble(-1);
            if (flux > 0) {
                data.setSfi(flux);
                String t = latest.path("time_tag").asText("");
                if (!t.isEmpty()) {
                    try { data.setSfiObservedAt(Instant.parse(t + "Z")); }
                    catch (Exception ignored) {
                        // NOAA omits the trailing Z on this feed; tolerate either form
                    }
                }
            }
        } catch (Exception e) {
            log.warn("SFI fetch failed: {}", e.getMessage());
        }
    }

    // ── International sunspot number (Wolf number) ─────────────
    // Endpoint: json/solar-cycle/observed-solar-cycle-indices.json
    // Shape:    array of monthly objects, oldest → newest:
    //   { "time-tag": "2026-04", "ssn": 79.3, "smoothed_ssn": -1.0,
    //     "observed_swpc_ssn": 91.6, "f10.7": 120.01, ... }
    // We use the "ssn" field (canonical SIDC/SILSO Wolf number), walk
    // backwards through the array for the most recent populated value,
    // and remember which month it represents so the UI can show the
    // freshness explicitly ("SSN 79 · 2026-04").
    private void fetchSsn(SolarData data) {
        try {
            JsonNode root = get(SSN_URL);
            if (!root.isArray() || root.size() == 0) return;
            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                double ssn = entry.path("ssn").asDouble(-1);
                if (ssn >= 0) {
                    data.setSunspotNumber((int) Math.round(ssn));
                    data.setSunspotNumberMonth(entry.path("time-tag").asText(""));
                    return;
                }
            }
            log.warn("SSN fetch returned no valid 'ssn' entry in {} rows", root.size());
        } catch (Exception e) {
            log.warn("SSN fetch failed: {}", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private static double parseCell(JsonNode cell) {
        if (cell == null || cell.isNull()) return Double.NaN;
        String s = cell.asText("").trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return Double.NaN;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private JsonNode get(String url) throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "J-Map/1.0 (ham radio ARS Suite)")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("HTTP " + resp.statusCode() + " from " + url,
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }
            return MAPPER.readTree(resp.body());
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Fetch failed: " + url + " — " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private static int kpToAIndex(double kp) {
        int[] table = {0, 3, 7, 15, 27, 48, 80, 132, 208, 400};
        return table[(int) Math.min(9, Math.max(0, kp))];
    }
}
