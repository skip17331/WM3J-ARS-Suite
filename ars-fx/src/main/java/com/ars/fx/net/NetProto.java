package com.ars.fx.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The messages the stations of a contest network send each other: one JSON object per WebSocket text frame, with a {@code t} saying what it is.
 * {@code challenge} / {@code hello} / {@code welcome} / {@code reject} open a connection; {@code have} lists the QSOs a station holds (so the
 * other can send what is missing); {@code qso} and {@code del} carry a QSO or a deletion; {@code status} says what a station is doing.
 * Points and the dupe flag are never sent: each station works them out from the merged log.
 */
public final class NetProto {
    private NetProto() {}

    public static final int VERSION = 1;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---- building ----------------------------------------------------------------------------------------------------

    public static String challenge(String nonce) { JsonObject o = obj("challenge"); o.addProperty("nonce", nonce); o.addProperty("v", VERSION); return o.toString(); }
    public static String hello(String station, String op, String proof) {
        JsonObject o = obj("hello"); o.addProperty("station", station); o.addProperty("op", op == null ? "" : op); o.addProperty("proof", proof); o.addProperty("v", VERSION); return o.toString();
    }
    public static String welcome(String station, String contest) { JsonObject o = obj("welcome"); o.addProperty("station", station); o.addProperty("contest", contest == null ? "" : contest); return o.toString(); }
    /** Asks the other station to send its {@code have} (used when a contest is opened after the connection was made). */
    public static String sync() { return obj("sync").toString(); }
    public static String reject(String reason) { JsonObject o = obj("reject"); o.addProperty("reason", reason); return o.toString(); }

    public static String have(String contest, List<Meta> items) {
        JsonObject o = obj("have"); o.addProperty("contest", contest);
        JsonArray a = new JsonArray();
        for (Meta m : items) {
            JsonObject i = new JsonObject(); i.addProperty("u", m.uid()); i.addProperty("ts", m.ts()); i.addProperty("o", m.origin() == null ? "" : m.origin());
            if (m.deleted()) i.addProperty("d", true);
            a.add(i);
        }
        o.add("items", a);
        return o.toString();
    }

    public static String qso(QsoRecord q) {
        JsonObject o = obj("qso");
        o.addProperty("uid", q.getUid()); o.addProperty("c", q.getContestId()); o.addProperty("call", q.getCallsign());
        o.addProperty("dt", q.getDateTimeUtc() == null ? null : q.getDateTimeUtc().format(DT));
        o.addProperty("band", q.getBand()); o.addProperty("mode", q.getMode()); o.addProperty("freq", q.getFrequency()); o.addProperty("op", q.getOperator());
        o.addProperty("ss", q.getSerialSent()); o.addProperty("sr", q.getSerialReceived());
        o.addProperty("f1", q.getContestField1()); o.addProperty("f2", q.getContestField2()); o.addProperty("f3", q.getContestField3());
        o.addProperty("f4", q.getContestField4()); o.addProperty("f5", q.getContestField5());
        o.addProperty("rs", q.getRstSent()); o.addProperty("rr", q.getRstReceived()); o.addProperty("notes", q.getNotes());
        o.addProperty("origin", q.getOrigin()); o.addProperty("ts", q.getUpdatedMs());
        return o.toString();
    }

    public static String chat(String station, String text, long ts) {
        JsonObject o = obj("chat"); o.addProperty("station", station); o.addProperty("text", text); o.addProperty("ts", ts); return o.toString();
    }

    public static String del(String uid, String contest, long ts) {
        JsonObject o = obj("del"); o.addProperty("uid", uid); o.addProperty("c", contest); o.addProperty("ts", ts); return o.toString();
    }

    public static String status(NetStatus s) {
        JsonObject o = obj("status");
        o.addProperty("station", s.station()); o.addProperty("op", s.op()); o.addProperty("c", s.contest());
        o.addProperty("band", s.band()); o.addProperty("mode", s.mode()); o.addProperty("freq", s.freqHz());
        o.addProperty("clock", System.currentTimeMillis());                // our clock, so the others can see how far off it is
        o.addProperty("run", s.run());
        return o.toString();
    }

    private static JsonObject obj(String type) { JsonObject o = new JsonObject(); o.addProperty("t", type); return o; }

    // ---- reading -----------------------------------------------------------------------------------------------------

    /** The message as an object, or null if it is not JSON or not an object (anything unreadable is ignored, never thrown). */
    public static JsonObject parse(String text) {
        try { JsonElement e = JsonParser.parseString(text); return e.isJsonObject() ? e.getAsJsonObject() : null; }
        catch (RuntimeException e) { return null; }
    }
    public static String type(JsonObject m) { return str(m, "t"); }
    public static String str(JsonObject m, String key) {
        JsonElement e = m.get(key);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }
    static long num(JsonObject m, String key) {
        try { JsonElement e = m.get(key); return e == null || e.isJsonNull() ? 0 : e.getAsLong(); } catch (RuntimeException x) { return 0; }
    }
    private static String nullable(JsonObject m, String key) {
        JsonElement e = m.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    /** The QSO in a {@code qso} message; null if it lacks a uid, a contest, a call or a time. */
    public static QsoRecord qsoFrom(JsonObject m) {
        try {
            QsoRecord q = new QsoRecord();
            q.setUid(str(m, "uid")); q.setContestId(str(m, "c")); q.setCallsign(str(m, "call"));
            String dt = str(m, "dt");
            if (q.getUid().isBlank() || q.getContestId().isBlank() || q.getCallsign().isBlank() || dt.isBlank()) return null;
            q.setDateTimeUtc(LocalDateTime.parse(dt, DT));
            q.setBand(nullable(m, "band")); q.setMode(nullable(m, "mode")); q.setFrequency(nullable(m, "freq")); q.setOperator(nullable(m, "op"));
            q.setSerialSent(nullable(m, "ss")); q.setSerialReceived(nullable(m, "sr"));
            q.setContestField1(nullable(m, "f1")); q.setContestField2(nullable(m, "f2")); q.setContestField3(nullable(m, "f3"));
            q.setContestField4(nullable(m, "f4")); q.setContestField5(nullable(m, "f5"));
            q.setRstSent(nullable(m, "rs")); q.setRstReceived(nullable(m, "rr")); q.setNotes(nullable(m, "notes"));
            q.setOrigin(str(m, "origin")); q.setUpdatedMs(num(m, "ts"));
            return q;
        } catch (RuntimeException e) { return null; }
    }

    /** The versions in a {@code have} message. */
    public static List<Meta> metasFrom(JsonObject m) {
        List<Meta> out = new ArrayList<>();
        JsonElement items = m.get("items");
        if (items == null || !items.isJsonArray()) return out;
        for (JsonElement e : items.getAsJsonArray()) {
            if (!e.isJsonObject()) continue;
            JsonObject i = e.getAsJsonObject();
            String uid = str(i, "u");
            if (uid.isBlank()) continue;
            out.add(new Meta(uid, num(i, "ts"), str(i, "o"), i.has("d") && i.get("d").getAsBoolean()));
        }
        return out;
    }

    /** The station in a {@code status} message, stamped with the time it arrived. */
    public static NetStatus statusFrom(JsonObject m) {
        String st = str(m, "station");
        if (st.isBlank()) return null;
        long now = System.currentTimeMillis(), sent = num(m, "clock");
        return new NetStatus(st, str(m, "op"), str(m, "c").isEmpty() ? null : str(m, "c"), str(m, "band"), str(m, "mode"), num(m, "freq"), now,
                sent > 0 ? sent - now : 0,                              // unknown (an older build) reads as no skew
                m.has("run") && !m.get("run").isJsonNull() && m.get("run").getAsBoolean());
    }
}
