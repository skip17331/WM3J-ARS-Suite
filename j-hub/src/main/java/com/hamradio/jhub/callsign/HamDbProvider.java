package com.hamradio.jhub.callsign;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;

/**
 * HamDB.org provider — free, no credentials required.
 * Covers US, Canadian, and some international callsigns.
 * API: https://api.hamdb.org/v1/{callsign}/json/j-hub
 */
public class HamDbProvider implements CallsignProvider {

    private static final Logger log = LoggerFactory.getLogger(HamDbProvider.class);
    private static final String BASE = "https://api.hamdb.org/v1/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override public String name() { return "hamdb"; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public CallsignResult lookup(String callsign) throws Exception {
        String url = BASE + callsign.toUpperCase() + "/json/j-hub";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HamDB HTTP " + resp.statusCode());

        JsonObject root  = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonObject hamdb = root.getAsJsonObject("hamdb");
        if (hamdb == null) throw new RuntimeException("HamDB unexpected response format");

        JsonObject msgs   = hamdb.getAsJsonObject("messages");
        String     status = msgs != null && msgs.has("status") ? msgs.get("status").getAsString() : "";
        if (!"OK".equalsIgnoreCase(status))
            return CallsignResult.notFound(callsign, "Not found in HamDB");

        JsonElement csEl = hamdb.get("callsign");
        if (csEl == null || !csEl.isJsonObject())
            return CallsignResult.notFound(callsign, "Not found in HamDB");

        JsonObject cs = csEl.getAsJsonObject();

        CallsignResult r = new CallsignResult();
        r.callsign     = str(cs, "call", callsign.toUpperCase());
        r.found        = true;
        r.source       = "hamdb";
        r.firstName    = str(cs, "fname", "");
        r.lastName     = str(cs, "name",  "");
        r.name         = (r.firstName + " " + r.lastName).trim();
        r.addr1        = str(cs, "addr1", "");
        r.addr2        = str(cs, "addr2", "");
        r.country      = str(cs, "country", "");
        r.grid         = str(cs, "grid",  "");
        r.licenseClass = classExpand(str(cs, "class", ""));
        r.email        = str(cs, "emails", "");
        r.fetchedAt    = Instant.now().toString();

        try { r.lat = Double.parseDouble(str(cs, "lat", "0")); } catch (Exception ignored) {}
        try { r.lon = Double.parseDouble(str(cs, "lon", "0")); } catch (Exception ignored) {}

        return r;
    }

    private static String str(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String v = obj.get(key).getAsString().trim();
        return v.isEmpty() ? fallback : v;
    }

    static String classExpand(String code) {
        switch (code.toUpperCase()) {
            case "E": return "Amateur Extra";
            case "A": return "Advanced";
            case "G": return "General";
            case "T": return "Technician";
            case "N": return "Novice";
            default:  return code;
        }
    }
}
