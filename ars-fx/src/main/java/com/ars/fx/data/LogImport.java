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
            String mode = modeOf(r.get("MODE"), r.get("SUBMODE"));
            JLogDb.Refs refs = refsOf(r);
            if (!have.add(key(call, dt, band, mode))) {
                JLogDb.mergeRefs(call, dt, band, mode, refs);   // a LoTW / eQSL download confirms what is already logged
                dupes++; continue;
            }
            int pwr = 0;
            try { if (r.get("TX_PWR") != null) pwr = (int) Math.round(Double.parseDouble(r.get("TX_PWR"))); } catch (NumberFormatException ignored) {}
            boolean ok = JLogDb.insertAt(dt, call, freq, band, mode, r.get("RST_SENT"), r.get("RST_RCVD"), r.get("NAME"), r.get("QTH"),
                    r.get("GRIDSQUARE"), r.get("COMMENT"), r.get("STATE"), r.get("CNTY"), pwr, refs);
            if (ok) added++; else bad++;
        }
        try { CallHistory.learnFromAdif(records); } catch (RuntimeException ignored) { /* the call history is a convenience */ }
        return new Result(true, added, dupes, bad, null);
    }

    /**
     * The mode to log for an ADIF MODE / SUBMODE pair: the submode when the mode is only its family (SSB → USB / LSB, PSK → PSK31,
     * MFSK → FT4 / JS8 / Q65 ..., DOMINO → DOMINOEX) or missing, else the mode. WSJT-X writes FT4 as MODE=MFSK SUBMODE=FT4; it used to
     * come in as "MFSK".
     */
    static String modeOf(String mode, String sub) {
        if (sub == null || sub.isBlank()) return mode;
        if (mode == null || mode.isBlank()) return sub.trim();
        String m = mode.trim().toUpperCase(Locale.ROOT);
        return m.equals("SSB") || m.equals("PSK") || m.equals("MFSK") || m.equals("DOMINO") ? sub.trim() : mode;
    }

    /** The award-tracking fields of one ADIF record: IOTA / SOTA / POTA references, zones and confirmation flags. */
    static JLogDb.Refs refsOf(Map<String, String> r) {
        String sig = r.get("SIG") == null ? "" : r.get("SIG").trim().toUpperCase(Locale.ROOT), sigInfo = r.get("SIG_INFO");
        String sota = first(r.get("SOTA_REF")), pota = first(r.get("POTA_REF"));
        if (sota.isEmpty() && "SOTA".equals(sig)) sota = first(sigInfo);      // older loggers keep the reference in SIG / SIG_INFO
        if (pota.isEmpty() && "POTA".equals(sig)) pota = first(sigInfo);
        return new JLogDb.Refs(r.get("IOTA") == null ? "" : r.get("IOTA").trim().toUpperCase(Locale.ROOT), sota, pota,
                intOf(r.get("CQZ")), intOf(r.get("ITUZ")), yes(r.get("QSL_RCVD")), yes(r.get("LOTW_QSL_RCVD")), yes(r.get("EQSL_QSL_RCVD")));
    }
    private static String first(String s) {
        if (s == null || s.isBlank()) return "";
        return s.split("[,;\\s]+")[0].trim().toUpperCase(Locale.ROOT);      // POTA_REF can list several parks
    }
    private static int intOf(String s) { try { return s == null ? 0 : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; } }
    private static boolean yes(String s) { return s != null && s.trim().equalsIgnoreCase("Y"); }

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

    /**
     * Where a field's value ends. ADIF lengths are counted in UTF-8 bytes by most loggers (Log4OM, N1MM+, Club Log) and in
     * characters by some; they differ only when the value has a non-ASCII letter. Counting characters for a byte length ran
     * past the value: "<NAME:5>José<QTH:6>Madrid" gave NAME "José<" and lost QTH (and any later field) of that record.
     * The byte reading is taken when it lands on the next tag, a space or the end, otherwise the character reading.
     */
    static int valueEnd(String t, int start, int len) {
        int n = t.length();
        int charEnd = Math.min(n, start + len);
        int bytes = 0, j = start;
        while (j < n) {
            int cp = t.codePointAt(j);
            int w = cp < 0x80 ? 1 : cp < 0x800 ? 2 : cp < 0x10000 ? 3 : 4;
            if (bytes + w > len) break;
            bytes += w; j += Character.charCount(cp);
        }
        int byteEnd = j;
        if (byteEnd == charEnd) return charEnd;
        boolean byteClean = byteEnd >= n || t.charAt(byteEnd) == '<' || Character.isWhitespace(t.charAt(byteEnd));
        return byteClean ? byteEnd : charEnd;
    }

    /** Each record's fields by upper-case name; see {@link #valueEnd} for how a length is read. */
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
            int end = valueEnd(t, i, len);
            String val = t.substring(i, end).trim();
            if (!val.isEmpty()) cur.put(name, val);
            i = end;
        }
        if (!cur.isEmpty() && cur.containsKey("CALL")) out.add(cur);
        return out;
    }
}
