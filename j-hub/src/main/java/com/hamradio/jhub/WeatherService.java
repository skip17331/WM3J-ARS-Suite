package com.hamradio.jhub;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetches space weather (NOAA SWPC) and local weather (OpenWeather) for the J-Hub web UI.
 * Results are cached and served by WeatherApiServlet via /api/weather.
 * Refreshes every 5 minutes on a daemon thread.
 */
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String NOAA_BASE     = "https://services.swpc.noaa.gov/json/";
    private static final String NOAA_PRODUCTS = "https://services.swpc.noaa.gov/products/";
    private static final String OW_BASE       = "https://api.openweathermap.org/data/2.5/weather";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final AtomicReference<String> cachedJson =
        new AtomicReference<>("{\"spaceWeather\":{},\"localWeather\":null}");
    private volatile Instant lastFetched;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "jhub-weather");
        t.setDaemon(true);
        return t;
    });

    private static final WeatherService INSTANCE = new WeatherService();
    public static WeatherService getInstance() { return INSTANCE; }
    private WeatherService() {}

    public void start() {
        scheduler.scheduleAtFixedRate(this::fetch, 0, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public String getCachedJson() { return cachedJson.get(); }
    public Instant getLastFetched() { return lastFetched; }

    // ── Fetch all data ─────────────────────────────────────────────────────────

    public void fetch() {
        try {
            JsonObject root = new JsonObject();
            root.add("spaceWeather", fetchSpaceWeather());

            String owKey = null;
            double lat = 0, lon = 0;
            try {
                JsonObject jmap = ConfigManager.getInstance().getConfig().jMapSettings;
                if (jmap != null && jmap.has("openWeatherApiKey")) {
                    owKey = jmap.get("openWeatherApiKey").getAsString();
                }
                lat = ConfigManager.getInstance().getConfig().station.lat;
                lon = ConfigManager.getInstance().getConfig().station.lon;
            } catch (Exception ignored) {}

            if (owKey != null && !owKey.isBlank() && (lat != 0 || lon != 0)) {
                JsonElement local = fetchLocalWeather(owKey, lat, lon);
                root.add("localWeather", local);
            } else {
                root.add("localWeather", JsonNull.INSTANCE);
            }

            root.addProperty("fetchedAt", Instant.now().toString());
            cachedJson.set(root.toString());
            lastFetched = Instant.now();
            log.debug("Weather data refreshed");
        } catch (Exception e) {
            log.debug("Weather fetch error: {}", e.getMessage());
        }
    }

    // ── Space weather ──────────────────────────────────────────────────────────

    private JsonObject fetchSpaceWeather() {
        JsonObject sw = new JsonObject();

        // Kp index
        try {
            JsonArray arr = getArray(NOAA_PRODUCTS + "noaa-planetary-k-index.json");
            if (arr != null) {
                for (int i = arr.size() - 1; i >= 0; i--) {
                    JsonObject row = arr.get(i).getAsJsonObject();
                    if (!row.has("Kp") || row.get("Kp").isJsonNull()) continue;
                    double kp = row.get("Kp").getAsDouble();
                    if (kp >= 0) {
                        sw.addProperty("kp", kp);
                        sw.addProperty("kpLabel", String.format("Kp %.2f", kp));
                        sw.addProperty("kpCondition", kpCondition(kp));
                        break;
                    }
                }
            }
        } catch (Exception e) { log.debug("Kp fetch: {}", e.getMessage()); }

        // SFI (10.7cm Solar Flux Index) — key ham-band propagation number
        try {
            JsonArray arr = getArray(NOAA_BASE + "f107_cm_flux.json");
            if (arr != null) {
                for (int i = arr.size() - 1; i >= 0; i--) {
                    JsonObject row = arr.get(i).getAsJsonObject();
                    JsonElement v = row.has("flux") ? row.get("flux")
                                  : row.has("observed_flux") ? row.get("observed_flux") : null;
                    if (v != null && !v.isJsonNull()) {
                        try { sw.addProperty("sfi", v.getAsDouble()); break; }
                        catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) { log.debug("SFI fetch: {}", e.getMessage()); }

        // Sunspot number — from NOAA solar-cycle data
        try {
            JsonArray arr = getArray(NOAA_BASE + "solar-cycle/observed-solar-cycle-indices.json");
            if (arr != null) {
                for (int i = arr.size() - 1; i >= 0; i--) {
                    JsonObject row = arr.get(i).getAsJsonObject();
                    JsonElement v = row.has("ssn") ? row.get("ssn")
                                  : row.has("sunspot_number") ? row.get("sunspot_number") : null;
                    if (v != null && !v.isJsonNull()) {
                        try { sw.addProperty("sunspots", v.getAsDouble()); break; }
                        catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) { log.debug("Sunspot fetch: {}", e.getMessage()); }

        // Solar wind plasma (speed index[2], density index[1], row[0] = header)
        try {
            JsonArray arr = getArray(NOAA_PRODUCTS + "solar-wind/plasma-1-day.json");
            if (arr != null && arr.size() >= 2) {
                for (int i = arr.size() - 1; i >= 1; i--) {
                    JsonArray row = arr.get(i).getAsJsonArray();
                    double speed = parseCell(row.size() > 2 ? row.get(2) : null);
                    if (!Double.isNaN(speed) && speed > 0) {
                        sw.addProperty("solarWindSpeed", speed);
                        double dens = parseCell(row.size() > 1 ? row.get(1) : null);
                        sw.addProperty("solarWindDensity", Double.isNaN(dens) ? 0 : dens);
                        break;
                    }
                }
            }
        } catch (Exception e) { log.debug("Solar wind plasma: {}", e.getMessage()); }

        // Solar wind mag (Bz index[3], Bt index[6])
        try {
            JsonArray arr = getArray(NOAA_PRODUCTS + "solar-wind/mag-1-day.json");
            if (arr != null && arr.size() >= 2) {
                for (int i = arr.size() - 1; i >= 1; i--) {
                    JsonArray row = arr.get(i).getAsJsonArray();
                    double bz = parseCell(row.size() > 3 ? row.get(3) : null);
                    if (!Double.isNaN(bz)) {
                        sw.addProperty("imfBz", bz);
                        double bt = parseCell(row.size() > 6 ? row.get(6) : null);
                        sw.addProperty("imfBt", Double.isNaN(bt) ? 0 : bt);
                        break;
                    }
                }
            }
        } catch (Exception e) { log.debug("Solar wind mag: {}", e.getMessage()); }

        // X-ray flux
        try {
            JsonArray arr = getArray(NOAA_BASE + "goes/primary/xrays-1-day.json");
            if (arr != null) {
                for (int i = arr.size() - 1; i >= 0; i--) {
                    JsonObject row = arr.get(i).getAsJsonObject();
                    if (!"0.1-0.8nm".equals(row.has("energy") ? row.get("energy").getAsString() : "")) continue;
                    double flux = row.has("flux") ? row.get("flux").getAsDouble() : -1;
                    if (flux > 0) {
                        sw.addProperty("xrayFlux", flux);
                        sw.addProperty("xrayClass", fluxToClass(flux));
                        break;
                    }
                }
            }
        } catch (Exception e) { log.debug("X-ray fetch: {}", e.getMessage()); }

        // Proton flux
        try {
            JsonArray arr = getArray(NOAA_BASE + "goes/primary/protons-1-day.json");
            if (arr != null) {
                for (int i = arr.size() - 1; i >= 0; i--) {
                    JsonObject row = arr.get(i).getAsJsonObject();
                    if (!">=10 MeV".equals(row.has("energy") ? row.get("energy").getAsString() : "")) continue;
                    double flux = row.has("flux") ? row.get("flux").getAsDouble() : -1;
                    if (flux >= 0) {
                        sw.addProperty("protonFlux", flux);
                        break;
                    }
                }
            }
        } catch (Exception e) { log.debug("Proton fetch: {}", e.getMessage()); }

        return sw;
    }

    // ── Local weather ──────────────────────────────────────────────────────────

    private JsonElement fetchLocalWeather(String apiKey, double lat, double lon) throws Exception {
        String url = OW_BASE + "?lat=" + lat + "&lon=" + lon
                   + "&appid=" + apiKey + "&units=imperial";
        return getElement(url);
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private String getRaw(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode());
        return resp.body();
    }

    private JsonArray getArray(String url) throws Exception {
        JsonElement el = JsonParser.parseString(getRaw(url));
        return el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    private JsonElement getElement(String url) throws Exception {
        return JsonParser.parseString(getRaw(url));
    }

    // ── Value helpers ──────────────────────────────────────────────────────────

    private static double parseCell(JsonElement cell) {
        if (cell == null || cell.isJsonNull()) return Double.NaN;
        String s = cell.getAsString().trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return Double.NaN;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private static String fluxToClass(double flux) {
        if (flux <= 0) return "---";
        char cls; double base;
        if      (flux >= 1e-4) { cls = 'X'; base = 1e-4; }
        else if (flux >= 1e-5) { cls = 'M'; base = 1e-5; }
        else if (flux >= 1e-6) { cls = 'C'; base = 1e-6; }
        else if (flux >= 1e-7) { cls = 'B'; base = 1e-7; }
        else                   { cls = 'A'; base = 1e-8; }
        return String.format("%c%.1f", cls, flux / base);
    }

    private static String kpCondition(double kp) {
        if (kp < 2)  return "Quiet";
        if (kp < 4)  return "Unsettled";
        if (kp < 5)  return "Active";
        if (kp < 6)  return "Minor Storm";
        if (kp < 7)  return "Moderate Storm";
        if (kp < 8)  return "Strong Storm";
        if (kp < 9)  return "Severe Storm";
        return "Extreme Storm";
    }
}
