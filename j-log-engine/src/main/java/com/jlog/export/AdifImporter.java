package com.jlog.export;

import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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

    /** What to do when an incoming record collides with an existing one
     *  on the (callsign, band, mode) key. {@code REVIEW} is the
     *  interactive variant — instead of acting silently on every dupe,
     *  the importer collects them into {@link Result#duplicates} and
     *  the UI opens a per-row review dialog after the import settles. */
    public enum DupeMode { SKIP, OVERWRITE, APPEND, REVIEW }

    public static class Result {
        public int imported;
        public int skipped;
        public int overwritten;
        public int failed;
        /** Per-record rejections — operator can review/edit/approve via
         *  the j-log Rejected QSOs dialog. Replaces the prior
         *  {@code List<String>} of bare reason strings so the UI has the
         *  parsed fields available to populate an edit form. */
        public List<RejectedRecord> failures = new ArrayList<>();
        /** Per-record duplicate-collision review queue — populated only
         *  when {@link DupeMode#REVIEW} is used. The UI opens a separate
         *  Review Duplicates dialog after the import settles so the
         *  operator can pick Keep / Overwrite / Skip for each row. */
        public List<DuplicateRecord> duplicates = new ArrayList<>();

        @Override public String toString() {
            return String.format("imported=%d overwritten=%d skipped=%d failed=%d duplicates=%d",
                imported, overwritten, skipped, failed, duplicates.size());
        }
    }

    /** One rejected ADIF record carried back to the UI for review. */
    public static class RejectedRecord {
        /** 1-based record number within the import — useful when the
         *  operator opens the source file to track down a row. */
        public final int recordNumber;
        /** Why we didn't insert it (e.g. "Missing CALL in record",
         *  "UNIQUE constraint failed: …"). */
        public final String reason;
        /** Raw ADIF tag → value map as parsed. Empty if parsing yielded
         *  nothing. Allows the UI to pre-populate an edit form even when
         *  the record was missing required fields. */
        public final Map<String, String> fields;
        /** Best-effort QsoRecord built from {@link #fields}. Whatever
         *  could be extracted is set; missing/invalid values are blank
         *  or null. Operator fills the gap before re-saving. */
        public final QsoRecord partial;

        public RejectedRecord(int recordNumber, String reason,
                              Map<String, String> fields, QsoRecord partial) {
            this.recordNumber = recordNumber;
            this.reason       = reason;
            this.fields       = fields == null ? Map.of() : Map.copyOf(fields);
            this.partial      = partial;
        }

        @Override public String toString() {
            return "#" + recordNumber + ": " + reason;
        }
    }

    /** One incoming record that collided with an existing log row.
     *  Surfaced to the Review Duplicates dialog when DupeMode.REVIEW
     *  is used. */
    public static class DuplicateRecord {
        public final int recordNumber;
        /** Parsed-and-ready QsoRecord for the incoming row. */
        public final QsoRecord incoming;
        /** Existing rows that match the (callsign, band, mode) dupe
         *  key in the DB right now. Usually exactly 1; can be &gt;1 if
         *  earlier APPEND imports left genuine accumulated dupes. */
        public final List<QsoRecord> existingMatches;

        public DuplicateRecord(int recordNumber, QsoRecord incoming,
                               List<QsoRecord> existingMatches) {
            this.recordNumber    = recordNumber;
            this.incoming        = incoming;
            this.existingMatches = existingMatches == null
                ? List.of() : List.copyOf(existingMatches);
        }
    }

    /** Import an ADIF file into the normal-log database. */
    public static Result importAdif(Path source, DupeMode dupeMode) throws IOException {
        Result r = new Result();
        String content = readAdifText(source);
        // Strip optional UTF-8 BOM
        if (!content.isEmpty() && content.charAt(0) == '﻿') content = content.substring(1);

        // Skip header: find <EOH> (case-insensitive) and start after it.
        int bodyStart = indexOfIgnoreCase(content, "<EOH>");
        int cursor = bodyStart >= 0 ? bodyStart + 5 : 0;

        int[] recordCounter = {0};
        parseRecords(content, cursor, rec -> {
            recordCounter[0]++;
            applyRecord(rec, dupeMode, r, recordCounter[0]);
        });
        log.info("ADIF import from {} — {}", source, r);
        return r;
    }

    /**
     * Read an ADIF file as text, tolerating the two encodings the wild
     * actually emits: UTF-8 (everything written by Linux/macOS loggers and
     * the modern ADIF 3.x spec) and Windows-1252 (HRD, Logger32, N1MM, and
     * other Windows-native loggers on US/Western systems). Try strict UTF-8
     * first so we preserve multi-byte characters when possible; fall back to
     * Windows-1252 on MalformedInputException — cp1252 has a glyph for every
     * byte so the fallback decode cannot fail. Replaces a strict
     * {@code Files.readString(path, UTF_8)} that threw
     * {@code MalformedInputException: Input length = N} on any cp1252 file.
     */
    private static String readAdifText(Path source) throws IOException {
        byte[] bytes = Files.readAllBytes(source);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException notUtf8) {
            Charset cp1252 = Charset.forName("windows-1252");
            log.info("ADIF file {} is not valid UTF-8; decoding as {}", source, cp1252);
            return new String(bytes, cp1252);
        }
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

    private static void applyRecord(Map<String, String> fields, DupeMode dupeMode,
                                    Result r, int recordNumber) {
        QsoRecord q = null;
        try {
            q = toQso(fields);
            if (q.getCallsign() == null || q.getCallsign().isBlank()) {
                r.failed++;
                r.failures.add(new RejectedRecord(
                    recordNumber, "Missing CALL in record", fields, q));
                return;
            }
            String band = q.getBand() == null ? "" : q.getBand();
            String mode = q.getMode() == null ? "" : q.getMode();
            // Dupe key includes the QSO date — same call+band+mode on a
            // different UTC day is a DIFFERENT contact. Operator rule.
            java.time.LocalDate qDate = q.getDateTimeUtc() != null
                ? q.getDateTimeUtc().toLocalDate() : null;
            boolean dupe = QsoDao.getInstance().isDuplicate(q.getCallsign(), band, mode, qDate);
            if (dupe) {
                if (dupeMode == DupeMode.SKIP) {
                    r.skipped++;
                    return;
                }
                if (dupeMode == DupeMode.OVERWRITE) {
                    QsoDao.getInstance().deleteByKey(q.getCallsign(), band, mode, qDate);
                    QsoDao.getInstance().insert(q);
                    r.overwritten++;
                    return;
                }
                if (dupeMode == DupeMode.REVIEW) {
                    // Defer the per-row decision to the UI's Review
                    // Duplicates dialog. Don't touch the DB now.
                    r.duplicates.add(new DuplicateRecord(
                        recordNumber, q,
                        QsoDao.getInstance().findByDupeKey(q.getCallsign(), band, mode, qDate)));
                    return;
                }
                // APPEND: fall through to the plain insert below.
            }
            QsoDao.getInstance().insert(q);
            r.imported++;
        } catch (Exception ex) {
            r.failed++;
            r.failures.add(new RejectedRecord(
                recordNumber,
                ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                fields, q));
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
