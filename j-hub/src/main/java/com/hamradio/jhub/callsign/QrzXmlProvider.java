package com.hamradio.jhub.callsign;

import com.hamradio.jhub.ConfigManager;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;

/**
 * QRZ.com XML provider — requires a QRZ.com XML subscription (~$30/yr).
 * Provides the richest data including photos, biography, LoTW, and eQSL status.
 * API docs: https://www.qrz.com/page/xml_data.html
 *
 * Session keys are obtained once and reused; expired sessions are detected and
 * re-authenticated transparently.
 */
public class QrzXmlProvider implements CallsignProvider {

    private static final Logger log = LoggerFactory.getLogger(QrzXmlProvider.class);
    private static final String QRZ_URL = "https://xmldata.qrz.com/xml/current/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private volatile String sessionKey;

    @Override public String name() { return "qrz"; }

    @Override
    public boolean isAvailable() {
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        return s != null && !s.qrzUsername.isBlank() && !s.qrzPassword.isBlank();
    }

    @Override
    public CallsignResult lookup(String callsign) throws Exception {
        ensureSession();
        CallsignResult r = doLookup(callsign);
        // Transparent re-auth on expired session
        if (!r.found && "session_expired".equals(r.error)) {
            sessionKey = null;
            ensureSession();
            r = doLookup(callsign);
        }
        return r;
    }

    private CallsignResult doLookup(String callsign) throws Exception {
        if (sessionKey == null)
            return CallsignResult.notFound(callsign, "QRZ session unavailable");

        String   url = QRZ_URL + "?s=" + sessionKey + "&callsign=" + callsign.toUpperCase();
        Document doc = fetch(url);

        // Refresh session key if QRZ includes an updated one
        String newKey = text(doc, "Key");
        if (newKey != null && !newKey.isBlank()) sessionKey = newKey;

        String error = text(doc, "Error");
        if (error != null) {
            String low = error.toLowerCase();
            if (low.contains("session") && low.contains("not found"))
                return CallsignResult.notFound(callsign, "session_expired");
            if (low.contains("not found") || low.contains("invalid"))
                return CallsignResult.notFound(callsign, "Not found in QRZ");
            return CallsignResult.notFound(callsign, error);
        }

        if (doc.getElementsByTagName("Callsign").getLength() == 0)
            return CallsignResult.notFound(callsign, "Not found in QRZ");

        CallsignResult r = new CallsignResult();
        r.callsign     = text(doc, "call",    callsign.toUpperCase());
        r.found        = true;
        r.source       = "qrz";
        r.firstName    = text(doc, "fname",   "");
        r.lastName     = text(doc, "name",    "");
        r.name         = (r.firstName + " " + r.lastName).trim();
        r.addr1        = text(doc, "addr1",   "");
        r.addr2        = text(doc, "addr2",   "");
        r.state        = text(doc, "state",   "");
        r.zip          = text(doc, "zip",     "");
        r.country      = text(doc, "country", "");
        r.grid         = text(doc, "grid",    "");
        r.licenseClass = text(doc, "class",   "");
        r.email        = text(doc, "email",   "");
        r.imageUrl     = text(doc, "image",   "");
        r.qslVia       = text(doc, "qslmgr",  "");
        r.fetchedAt    = Instant.now().toString();
        r.lotwActive   = "Y".equalsIgnoreCase(text(doc, "lotw", ""));
        r.eqslActive   = "Y".equalsIgnoreCase(text(doc, "eqsl", ""));

        try { r.lat = Double.parseDouble(text(doc, "lat", "0")); } catch (Exception ignored) {}
        try { r.lon = Double.parseDouble(text(doc, "lon", "0")); } catch (Exception ignored) {}

        // QRZ returns bio length in bytes rather than content; note its presence
        String bioLen = text(doc, "bio", "0");
        if (!bioLen.equals("0")) r.bio = "(Biography available on QRZ.com)";

        return r;
    }

    private void ensureSession() throws Exception {
        if (sessionKey != null) return;
        JHubConfig.CallsignSection s = ConfigManager.getInstance().getConfig().callsignLookup;
        String url = QRZ_URL + "?username=" + s.qrzUsername
                   + "&password=" + s.qrzPassword + "&agent=j-hub";
        Document doc = fetch(url);

        String error = text(doc, "Error");
        if (error != null) throw new RuntimeException("QRZ auth failed: " + error);

        String key = text(doc, "Key");
        if (key == null || key.isBlank()) throw new RuntimeException("QRZ did not return a session key");
        sessionKey = key;
        log.info("QRZ session established");
    }

    // ── XML helpers ────────────────────────────────────────────────────────────

    private Document fetch(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("QRZ HTTP " + resp.statusCode());
        return parseXml(resp.body());
    }

    static Document parseXml(String body) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(null); // suppress SAX console noise
        return db.parse(new ByteArrayInputStream(body.getBytes("UTF-8")));
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
