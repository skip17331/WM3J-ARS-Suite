package com.hamradio.jhub.callsign;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Callook.info provider — free, no credentials required.
 * US callsigns only (FCC database mirror).
 * API: https://callook.info/{callsign}/json
 */
public class CallookProvider implements CallsignProvider {

    private static final Logger log = LoggerFactory.getLogger(CallookProvider.class);
    private static final String BASE = "https://callook.info/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override public String name() { return "callook"; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public CallsignResult lookup(String callsign) throws Exception {
        String url = BASE + callsign.toUpperCase() + "/json";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Callook HTTP " + resp.statusCode());

        JsonObject root   = JsonParser.parseString(resp.body()).getAsJsonObject();
        String     status = root.has("status") ? root.get("status").getAsString() : "";
        if (!"VALID".equalsIgnoreCase(status))
            return CallsignResult.notFound(callsign, "Not found in Callook");

        CallsignResult r = new CallsignResult();
        r.callsign  = callsign.toUpperCase();
        r.found     = true;
        r.source    = "callook";
        r.fetchedAt = Instant.now().toString();

        if (root.has("current")) {
            JsonObject cur = root.getAsJsonObject("current");
            r.licenseClass = HamDbProvider.classExpand(str(cur, "operClass", ""));
        }

        if (root.has("name")) {
            JsonObject name = root.getAsJsonObject("name");
            r.firstName = str(name, "first", "");
            r.lastName  = str(name, "last",  "");
            r.name      = (r.firstName + " " + r.lastName).trim();
        }

        if (root.has("address")) {
            JsonObject addr = root.getAsJsonObject("address");
            r.addr1 = str(addr, "line1", "");
            r.addr2 = str(addr, "line2", "");
        }

        if (root.has("location")) {
            JsonObject loc = root.getAsJsonObject("location");
            try { r.lat = Double.parseDouble(str(loc, "latitude",  "0")); } catch (Exception ignored) {}
            try { r.lon = Double.parseDouble(str(loc, "longitude", "0")); } catch (Exception ignored) {}
            r.grid = str(loc, "gridsquare", "");
        }

        if (root.has("otherInfo")) {
            r.licenseExpires = str(root.getAsJsonObject("otherInfo"), "expireDate", "");
        }

        return r;
    }

    private static String str(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String v = obj.get(key).getAsString().trim();
        return v.isEmpty() ? fallback : v;
    }
}
