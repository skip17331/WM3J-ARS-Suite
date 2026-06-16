package com.ars.fx.data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real solar-terrestrial conditions from HamQSL (N0NBH's {@code solarxml.php}) —
 * SFI / A / K / sunspots / aurora / solar wind / geomagnetic field plus the
 * calculated HF band conditions. Fetched off-thread, cached, and refreshed
 * hourly; drives the Propagation, Band-conditions and Solar drawers.
 */
public final class SolarData {
    private static final SolarData INSTANCE = new SolarData();
    public static SolarData getInstance() { return INSTANCE; }
    private SolarData() {}

    public record Band(String group, String day, String night) {}
    public record Snapshot(String sfi, String aIndex, String kIndex, String sunspots, String xray,
                           String aurora, String solarWind, String magField, String updated, List<Band> bands) {}

    private volatile Snapshot snap;
    private volatile boolean started;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public Snapshot snapshot() { return snap; }

    /** Begin background fetch + hourly refresh (idempotent). */
    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(() -> {
            while (true) {
                try { fetch(); } catch (Throwable ignored) { }
                try { Thread.sleep(3_600_000L); } catch (InterruptedException e) { return; }
            }
        }, "solar-data");
        t.setDaemon(true);
        t.start();
    }

    private void fetch() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://www.hamqsl.com/solarxml.php"))
                .timeout(Duration.ofSeconds(12)).header("User-Agent", "ARS-Suite-JHub").GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return;
        String xml = resp.body();
        List<Band> bands = parseBands(xml);
        snap = new Snapshot(tag(xml, "solarflux"), tag(xml, "aindex"), tag(xml, "kindex"),
                tag(xml, "sunspots"), tag(xml, "xray"), tag(xml, "aurora"),
                tag(xml, "solarwind"), tag(xml, "magneticfield"), tag(xml, "updated"), bands);
    }

    private static List<Band> parseBands(String xml) {
        // <band name="80m-40m" time="day">Fair</band>
        java.util.Map<String, String[]> map = new java.util.LinkedHashMap<>();
        Matcher m = Pattern.compile("<band\\s+name=\"([^\"]+)\"\\s+time=\"([^\"]+)\">([^<]+)</band>", Pattern.CASE_INSENSITIVE).matcher(xml);
        while (m.find()) {
            String g = m.group(1), time = m.group(2).toLowerCase(), val = m.group(3).trim();
            String[] dn = map.computeIfAbsent(g, k -> new String[2]);
            if (time.startsWith("day")) dn[0] = val; else dn[1] = val;
        }
        List<Band> out = new ArrayList<>();
        for (var e : map.entrySet()) out.add(new Band(e.getKey(), nz(e.getValue()[0]), nz(e.getValue()[1])));
        return out;
    }

    private static String tag(String xml, String name) {
        Matcher m = Pattern.compile("<" + name + "[^>]*>(.*?)</" + name + ">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
    private static String nz(String s) { return s == null ? "—" : s; }

    /** Geomagnetic descriptor from the K index. */
    public static String geomag(String k) {
        try {
            int v = Integer.parseInt(k.trim());
            return v <= 1 ? "quiet" : v <= 3 ? "unsettled" : v == 4 ? "active" : "storm";
        } catch (Exception e) { return "—"; }
    }
    /** Aurora-activity descriptor from the aurora index. */
    public static String auroraDesc(String a) {
        try {
            int v = Integer.parseInt(a.trim());
            return v <= 2 ? "quiet" : v <= 5 ? "active" : "high";
        } catch (Exception e) { return "—"; }
    }
}
