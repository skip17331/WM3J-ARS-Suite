package com.ars.fx.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Info page's "Active now" tile (punch list 5-6): who is on the air right now that you might want. Parks on the Air activators
 * from POTA's public spot list (fetched at most every five minutes, and only when the Info page loads or is refreshed), and DXpeditions
 * whose dates include today (from the DXpedition calendar the Info page already reads). Each is tagged when your log still needs it: a
 * new DXCC entity, or a park you have not worked.
 *
 * <p>Summits on the Air is deliberately left out: SOTA's API terms (api-db2.sota.org.uk/docs) require each developer to register with the
 * SOTA Management Team and not to connect AI-generated software without their prior approval. It can be added once that approval is had.
 */
public final class ActiveNow {
    private ActiveNow() {}

    public static final String POTA_URL = "https://api.pota.app/spot/activator";
    static final Duration KEEP = Duration.ofMinutes(5);
    /** Spots older than this are not "now". */
    static final Duration MAX_AGE = Duration.ofMinutes(60);

    /** One activator on the air. {@code freqKhz} 0 when unknown; {@code need} "" or why your log wants it. */
    public record Activation(String program, String call, String ref, String refName, double freqKhz, String mode, Instant spotted, String need) {
        public Activation withNeed(String n) { return new Activation(program, call, ref, refName, freqKhz, mode, spotted, n); }
    }

    /** POTA's activator spot list. Unreadable entries are skipped. */
    static List<Activation> parsePota(String json) {
        List<Activation> out = new ArrayList<>();
        for (JsonObject o : objects(json)) {
            String call = str(o, "activator");
            if (call.isEmpty()) continue;
            out.add(new Activation("POTA", call.toUpperCase(Locale.ROOT), str(o, "reference").toUpperCase(Locale.ROOT), str(o, "name"),
                    num(str(o, "frequency")), str(o, "mode").toUpperCase(Locale.ROOT), time(str(o, "spotTime")), ""));
        }
        return out;
    }


    /**
     * The newest spot of each activator, no older than {@link #MAX_AGE}, newest first, tagged against the log: "NEW ONE" for a DXCC
     * entity not worked, else "NEW PARK" for a park not worked.
     */
    static List<Activation> current(List<Activation> all, Instant now, Set<Integer> workedDxcc, Set<String> workedParks,
                                    java.util.function.ToIntFunction<String> dxccOf) {
        java.util.Map<String, Activation> latest = new java.util.LinkedHashMap<>();
        for (Activation a : all) {
            if (a.spotted() != null && Duration.between(a.spotted(), now).compareTo(MAX_AGE) > 0) continue;
            String k = a.program() + "|" + a.call();
            Activation was = latest.get(k);
            if (was == null || (a.spotted() != null && (was.spotted() == null || a.spotted().isAfter(was.spotted())))) latest.put(k, a);
        }
        List<Activation> out = new ArrayList<>();
        for (Activation a : latest.values()) {
            int dx = dxccOf.applyAsInt(a.call());
            String need = dx > 0 && !workedDxcc.contains(dx) ? "NEW ONE"
                    : "POTA".equals(a.program()) && !a.ref().isEmpty() && !workedParks.contains(a.ref()) ? "NEW PARK" : "";
            out.add(a.withNeed(need));
        }
        out.sort(Comparator.comparing(Activation::spotted, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /** DXpeditions on the air today: calendar entries whose dates include it. */
    public static List<UpcomingEvents.Event> dxpeditionsToday(List<UpcomingEvents.Event> all, LocalDate today) {
        List<UpcomingEvents.Event> out = new ArrayList<>();
        for (UpcomingEvents.Event e : all) {
            if (e.kind() != UpcomingEvents.Kind.DXPEDITION || e.start() == null) continue;
            LocalDate end = e.end() == null ? e.start() : e.end();
            if (!today.isBefore(e.start()) && !today.isAfter(end)) out.add(e);
        }
        return out;
    }

    /** Fetch and tag the POTA activators now (cached for five minutes). Blocking: call off the FX thread. */
    public static List<Activation> fetch(boolean force) {
        Instant now = Instant.now();
        List<Activation> all = new ArrayList<>();
        FeedFetcher.Fetched p = FeedFetcher.get("active-pota", POTA_URL, force, now, KEEP);
        if (p.body() != null) all.addAll(parsePota(p.body()));
        Awards.Snapshot snap = Awards.compute();
        Set<Integer> dx = new java.util.HashSet<>();
        for (Awards.Entity e : snap.entities.values()) dx.add(e.number);
        return current(all, now, dx, snap.potaStats.keySet(), call -> {
            DxccInfo.Info i = DxccInfo.of(call);
            return i == null ? 0 : i.dxcc();
        });
    }

    // ---- small JSON helpers ----
    private static List<JsonObject> objects(String json) {
        List<JsonObject> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonElement root = JsonParser.parseString(json);
            JsonArray arr = root.isJsonArray() ? root.getAsJsonArray() : new JsonArray();
            for (JsonElement e : arr) if (e.isJsonObject()) out.add(e.getAsJsonObject());
        } catch (RuntimeException ignored) { /* not JSON: nothing */ }
        return out;
    }
    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString().trim();
    }
    private static double num(String s) { try { return Double.parseDouble(s.replace(",", ".")); } catch (NumberFormatException e) { return 0; } }
    /** "2026-11-18T17:05:23" (UTC, as both lists send it), with or without a zone; null when unreadable. */
    static Instant time(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); } catch (RuntimeException ignored) { }
        try { return LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s).toInstant(ZoneOffset.UTC); } catch (RuntimeException e) { return null; }
    }
}
