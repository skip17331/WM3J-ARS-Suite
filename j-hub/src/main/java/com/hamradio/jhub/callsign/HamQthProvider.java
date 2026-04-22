package com.hamradio.jhub.callsign;

import com.hamradio.jhub.ConfigManager;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;

/**
 * HamQTH.com provider — free registration required at hamqth.com.
 * Good international coverage; includes LoTW and eQSL flags.
 * API docs: https://www.hamqth.com/developers.php
 *
 * Session IDs are obtained once per run and reused; expired sessions
 * are detected and re-authenticated transparently.
 */
public class HamQthProvider implements CallsignProvider {

    private static final Logger log = LoggerFactory.getLogger(HamQthProvider.class);
    private static final String HAMQTH_URL = "https://www.hamqth.com/xml.php";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private volatile String sessionId;

    @Override public String name() { return "hamqth"; }

    @Override
    public boolean isAvailable() {
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        return s != null && !s.hamqthUsername.isBlank() && !s.hamqthPassword.isBlank();
    }

    @Override
    public CallsignResult lookup(String callsign) throws Exception {
        ensureSession();
        CallsignResult r = doLookup(callsign);
        if (!r.found && "session_expired".equals(r.error)) {
            sessionId = null;
            ensureSession();
            r = doLookup(callsign);
        }
        return r;
    }

    private CallsignResult doLookup(String callsign) throws Exception {
        if (sessionId == null)
            return CallsignResult.notFound(callsign, "HamQTH session unavailable");

        String   url = HAMQTH_URL + "?id=" + sessionId + "&callsign=" + callsign.toUpperCase() + "&prg=j-hub";
        Document doc = QrzXmlProvider.parseXml(getRaw(url));

        String error = text(doc, "error");
        if (error != null) {
            String low = error.toLowerCase();
            if (low.contains("session") || low.contains("wrong user"))
                return CallsignResult.notFound(callsign, "session_expired");
            if (low.contains("not found") || low.contains("callsign not"))
                return CallsignResult.notFound(callsign, "Not found in HamQTH");
            return CallsignResult.notFound(callsign, error);
        }

        if (doc.getElementsByTagName("search").getLength() == 0)
            return CallsignResult.notFound(callsign, "Not found in HamQTH");

        CallsignResult r = new CallsignResult();
        r.callsign   = callsign.toUpperCase();
        r.found      = true;
        r.source     = "hamqth";
        r.name       = text(doc, "adr_name",    "");
        r.addr1      = text(doc, "adr_street1",  "");
        r.city       = text(doc, "adr_city",     "");
        r.zip        = text(doc, "adr_zip",      "");
        r.country    = text(doc, "adr_country",  "");
        r.state      = text(doc, "us_state",     "");
        r.grid       = text(doc, "grid",         "");
        r.email      = text(doc, "email",        "");
        r.imageUrl   = text(doc, "picture",      "");
        r.qslVia     = text(doc, "qsl_via",      "");
        r.fetchedAt  = Instant.now().toString();
        r.lotwActive = "Y".equalsIgnoreCase(text(doc, "lotw", ""));
        r.eqslActive = "Y".equalsIgnoreCase(text(doc, "eqsl", ""));

        r.addr2 = (r.city + " " + r.state).trim();

        try { r.lat = Double.parseDouble(text(doc, "latitude",  "0")); } catch (Exception ignored) {}
        try { r.lon = Double.parseDouble(text(doc, "longitude", "0")); } catch (Exception ignored) {}

        return r;
    }

    private void ensureSession() throws Exception {
        if (sessionId != null) return;
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        String   url = HAMQTH_URL + "?u=" + s.hamqthUsername + "&p=" + s.hamqthPassword + "&prg=j-hub";
        Document doc = QrzXmlProvider.parseXml(getRaw(url));

        String error = text(doc, "error");
        if (error != null) throw new RuntimeException("HamQTH auth failed: " + error);

        String id = text(doc, "session_id");
        if (id == null || id.isBlank()) throw new RuntimeException("HamQTH did not return a session ID");
        sessionId = id;
        log.info("HamQTH session established");
    }

    private String getRaw(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HamQTH HTTP " + resp.statusCode());
        return resp.body();
    }

    private static String text(Document doc, String tag) {
        NodeList nl = doc.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String v = nl.item(0).getTextContent();
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private static String text(Document doc, String tag, String fallback) {
        String v = text(doc, tag);
        return v != null ? v : fallback;
    }
}
