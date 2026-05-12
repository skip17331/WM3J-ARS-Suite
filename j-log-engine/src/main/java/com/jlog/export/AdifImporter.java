package com.jlog.export;

import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

/**
 * ADIF 3.x importer. Parses &lt;TAG:len[:TYPE]&gt;value tokens, builds
 * {@link QsoRecord} objects, and inserts them via {@link QsoDao}. Handles
 * common quirks in third-party ADIF output (missing spaces, embedded
 * newlines, mixed case, UTF-8 BOM).
 *
 * Use {@link DupeMode} to control what happens when a callsign+band+mode
 * collision is found in the existing log:
 *   SKIP       — the imported record is silently discarded
 *   OVERWRITE  — existing row is replaced with the imported values
 *   APPEND     — imported row is inserted regardless (a true dupe)
 */
public class AdifImporter {

    private static final Logger log = LoggerFactory.getLogger(AdifImporter.class);

    public enum DupeMode { SKIP, OVERWRITE, APPEND }

    public static class Result {
        public int imported;
        public int skipped;
        public int overwritten;
        public int failed;
        public List<String> failures = new ArrayList<>();

        @Override public String toString() {
            return String.format("imported=%d overwritten=%d skipped=%d failed=%d",
                imported, overwritten, skipped, failed);
        }
    }

    /** Import an ADIF file into the normal-log database. */
    public static Result importAdif(Path source, DupeMode dupeMode) throws IOException {
        Result r = new Result();
        String content = Files.readString(source, StandardCharsets.UTF_8);
        // Strip optional UTF-8 BOM
        if (!content.isEmpty() && content.charAt(0) == '﻿') content = content.substring(1);

        // Skip header: find <EOH> (case-insensitive) and start after it.
        int bodyStart = indexOfIgnoreCase(content, "<EOH>");
        int cursor = bodyStart >= 0 ? bodyStart + 5 : 0;

        parseRecords(content, cursor, rec -> applyRecord(rec, dupeMode, r));
        log.info("ADIF import from {} — {}", source, r);
        return r;
    }

    // -----------------------------------------------------------------
    // Parser
    // -----------------------------------------------------------------

    private static void parseRecords(String content, int startIdx, Consumer<Map<String, String>> onRecord) {
        Map<String, String> fields = new HashMap<>();
        int i = startIdx;
        while (i < content.length()) {
            int lt = content.indexOf('<', i);
            if (lt < 0) break;
            int gt = content.indexOf('>', lt);
            if (gt < 0) break;
            String header = content.substring(lt + 1, gt);
            i = gt + 1;

            // End-of-record marker
            if ("EOR".equalsIgnoreCase(header.trim())) {
                if (!fields.isEmpty()) onRecord.accept(fields);
                fields = new HashMap<>();
                continue;
            }
            // End-of-header marker (shouldn't appear inside body; skip)
            if ("EOH".equalsIgnoreCase(header.trim())) continue;

            // Parse tag:length[:type]
            String[] parts = header.split(":");
            if (parts.length < 2) continue;
            String tag = parts[0].trim().toUpperCase();
            int    len;
            try { len = Integer.parseInt(parts[1].trim()); }
            catch (NumberFormatException e) { continue; }
            if (i + len > content.length()) break;
            String value = content.substring(i, i + len);
            i += len;
            fields.put(tag, value);
        }
        // Some files end records without a final <EOR>; flush if non-empty.
        if (!fields.isEmpty()) onRecord.accept(fields);
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        int n = haystack.length(), m = needle.length();
        for (int i = 0; i <= n - m; i++) {
            if (haystack.regionMatches(true, i, needle, 0, m)) return i;
        }
        return -1;
    }

    // -----------------------------------------------------------------
    // Record → DB
    // -----------------------------------------------------------------

    private static void applyRecord(Map<String, String> fields, DupeMode dupeMode, Result r) {
        try {
            QsoRecord q = toQso(fields);
            if (q.getCallsign() == null || q.getCallsign().isBlank()) {
                r.failed++;
                r.failures.add("Missing CALL in record");
                return;
            }
            String band = q.getBand() == null ? "" : q.getBand();
            String mode = q.getMode() == null ? "" : q.getMode();
            boolean dupe = QsoDao.getInstance().isDuplicate(q.getCallsign(), band, mode);
            if (dupe) {
                if (dupeMode == DupeMode.SKIP) {
                    r.skipped++;
                    return;
                }
                if (dupeMode == DupeMode.OVERWRITE) {
                    QsoDao.getInstance().deleteByKey(q.getCallsign(), band, mode);
                    QsoDao.getInstance().insert(q);
                    r.overwritten++;
                    return;
                }
                // APPEND: fall through to the plain insert below.
            }
            QsoDao.getInstance().insert(q);
            r.imported++;
        } catch (Exception ex) {
            r.failed++;
            r.failures.add(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private static QsoRecord toQso(Map<String, String> f) {
        QsoRecord q = new QsoRecord();
        q.setCallsign(upper(f.get("CALL")));
        q.setBand(lower(f.get("BAND")));
        q.setMode(upper(f.get("MODE")));
        q.setFrequency(f.get("FREQ"));
        q.setRstSent(f.get("RST_SENT"));
        q.setRstReceived(f.get("RST_RCVD"));
        q.setCountry(f.get("COUNTRY"));
        q.setOperatorName(firstNonBlank(f.get("NAME"), f.get("OPERATOR")));
        q.setState(f.get("STATE"));
        q.setCounty(f.get("CNTY"));
        q.setNotes(firstNonBlank(f.get("COMMENT"), f.get("NOTES")));
        q.setQslSent("Y".equalsIgnoreCase(f.get("QSL_SENT")));
        q.setQslReceived("Y".equalsIgnoreCase(f.get("QSL_RCVD")));
        q.setSig     (f.get("SIG"));
        q.setSigInfo (f.get("SIG_INFO"));

        String pwr = f.get("TX_PWR");
        if (pwr != null) {
            try { q.setPowerWatts((int) Math.round(Double.parseDouble(pwr.trim()))); }
            catch (NumberFormatException ignored) {}
        }

        String date = f.get("QSO_DATE");
        String time = firstNonBlank(f.get("TIME_ON"), f.get("TIME_OFF"));
        if (date != null) {
            try {
                String dt = date.trim() + (time == null ? "000000" :
                    (time.trim().length() == 4 ? time.trim() + "00" : time.trim()));
                q.setDateTimeUtc(LocalDateTime.parse(dt,
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            } catch (DateTimeParseException ignored) {}
        }
        return q;
    }

    private static String upper(String s) { return s == null ? null : s.trim().toUpperCase(); }
    private static String lower(String s) { return s == null ? null : s.trim().toLowerCase(); }
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
