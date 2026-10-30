package com.ars.fx.data;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * ADIF import into the normal J-Log database — the counterpart of {@link LogExport}. Records that are already in the log
 * (same call, minute, band and mode) are skipped, so importing the same file twice, or a file that overlaps what was
 * exported earlier, adds nothing new.
 */
public final class LogImport {
    private LogImport() {}

    public record Result(boolean ok, int imported, int duplicates, int failed, String error) {}

    /** Blocking; run off the FX thread. */
    public static Result importAdif(Path file) {
        String text;
        try { text = read(file); } catch (IOException e) { return new Result(false, 0, 0, 0, "could not read the file: " + e.getMessage()); }
        List<Map<String, String>> records = parse(text);
        if (records.isEmpty()) return new Result(false, 0, 0, 0, "no ADIF records found in that file");
        if (!JLogDb.available()) return new Result(false, 0, 0, 0, "the J-Log database is not available");

        Set<String> have = new HashSet<>();
        for (JLogDb.QsoExport q : JLogDb.allForExport()) have.add(key(q.call(), q.datetimeUtc(), q.band(), q.mode()));

        int added = 0, dupes = 0, bad = 0;
        for (Map<String, String> r : records) {
            String call = r.get("CALL"), date = r.get("QSO_DATE"), time = r.get("TIME_ON");
            if (call == null || call.isBlank() || date == null || date.length() != 8) { bad++; continue; }
            if (time == null || time.length() < 4) time = "0000";
            String dt = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8)
                    + " " + time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + (time.length() >= 6 ? time.substring(4, 6) : "00");
            String freq = r.get("FREQ");
            String band = r.get("BAND") != null ? r.get("BAND").toLowerCase(Locale.ROOT) : Bands.ofMhz(freq);
            String sub = r.get("SUBMODE"), mode = (sub != null && !sub.isBlank() && ("USB".equalsIgnoreCase(sub) || "LSB".equalsIgnoreCase(sub)
                    || r.get("MODE") == null || "PSK".equalsIgnoreCase(r.get("MODE")))) ? sub : r.get("MODE");
            if (!have.add(key(call, dt, band, mode))) { dupes++; continue; }
            int pwr = 0;
            try { if (r.get("TX_PWR") != null) pwr = (int) Math.round(Double.parseDouble(r.get("TX_PWR"))); } catch (NumberFormatException ignored) {}
            boolean ok = JLogDb.insertAt(dt, call, freq, band, mode, r.get("RST_SENT"), r.get("RST_RCVD"), r.get("NAME"), r.get("QTH"),
                    r.get("GRIDSQUARE"), r.get("COMMENT"), r.get("STATE"), r.get("CNTY"), pwr);
            if (ok) added++; else bad++;
        }
        return new Result(true, added, dupes, bad, null);
    }

    private static String key(String call, String dt, String band, String mode) {
        return (call == null ? "" : call.trim().toUpperCase(Locale.ROOT)) + "|" + (dt == null ? "" : dt.substring(0, Math.min(16, dt.length())))
                + "|" + (band == null ? "" : band.trim().toLowerCase(Locale.ROOT)) + "|" + (mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT));
    }

    /** UTF-8 when it decodes cleanly, otherwise Windows-1252 (older loggers). */
    private static String read(Path f) throws IOException {
        byte[] b = Files.readAllBytes(f);
        try { return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(b)).toString(); }
        catch (CharacterCodingException e) { return new String(b, java.nio.charset.Charset.forName("windows-1252")); }
    }

    /** Each record's fields by upper-case name. Lengths are in bytes of the original, so they are counted on the decoded text. */
    static List<Map<String, String>> parse(String t) {
        List<Map<String, String>> out = new ArrayList<>();
        int i = 0, n = t.length();
        String up = t.toUpperCase(Locale.ROOT);
        int eoh = up.indexOf("<EOH>");
        if (eoh >= 0) i = eoh + 5; else if (!t.stripLeading().startsWith("<")) i = 0;
        Map<String, String> cur = new TreeMap<>();
        while (i < n) {
            int lt = t.indexOf('<', i);
            if (lt < 0) break;
            int gt = t.indexOf('>', lt);
            if (gt < 0) break;
            String tag = t.substring(lt + 1, gt);
            i = gt + 1;
            String[] p = tag.split(":");
            String name = p[0].trim().toUpperCase(Locale.ROOT);
            if (name.equals("EOR")) { if (!cur.isEmpty()) out.add(cur); cur = new TreeMap<>(); continue; }
            if (name.equals("EOH") || p.length < 2) continue;
            int len;
            try { len = Integer.parseInt(p[1].trim()); } catch (NumberFormatException e) { continue; }
            int end = Math.min(n, i + len);
            String val = t.substring(i, end).trim();
            if (!val.isEmpty()) cur.put(name, val);
            i = end;
        }
        if (!cur.isEmpty() && cur.containsKey("CALL")) out.add(cur);
        return out;
    }
}
