package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ADIF 3.1 export for normal log and contest logs. Produces output accepted
 * by LoTW and major third-party loggers. Skips empty / zero values rather
 * than emitting blank fields, normalizes mode/band/frequency to ADIF-spec
 * values, and includes the fields LoTW needs for signing.
 */
public class AdifExporter {

    private static final Logger log = LoggerFactory.getLogger(AdifExporter.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    // j-log internal mode name → ADIF-standard mode. Leaves unknown values alone.
    private static final Map<String, String> MODE_MAP = Map.ofEntries(
        Map.entry("PHONE", "SSB"),
        Map.entry("USB",   "SSB"),
        Map.entry("LSB",   "SSB"),
        Map.entry("FM",    "FM"),
        Map.entry("AM",    "AM"),
        Map.entry("CW",    "CW"),
        Map.entry("RTTY",  "RTTY"),
        Map.entry("PSK31", "PSK31"),
        Map.entry("PSK63", "PSK63"),
        Map.entry("FT8",   "FT8"),
        Map.entry("FT4",   "FT4"),
        Map.entry("MSK144","MSK144"),
        Map.entry("OLIVIA","OLIVIA"),
        Map.entry("JS8",   "JS8"),
        Map.entry("DIGITAL","DIGITALVOICE")
    );

    // -----------------------------------------------------------------
    // Public entry points
    // -----------------------------------------------------------------

    /** Export entire normal log to ADIF. */
    public static void exportAdif(Path destination) throws Exception {
        List<QsoRecord> qsos = QsoDao.getInstance().fetchAll();
        writeAdif(destination, qsos, false);
        log.info("Exported {} normal-log QSOs to ADIF: {}", qsos.size(), destination);
    }

    /** Export every QSO for a specific contest to ADIF. */
    public static void exportContestAdif(String contestId, Path destination) throws Exception {
        List<QsoRecord> qsos = ContestQsoDao.getInstance().fetchByContest(contestId);
        writeAdif(destination, qsos, true);
        log.info("Exported {} contest QSOs ({}) to ADIF: {}", qsos.size(), contestId, destination);
    }

    /** Export a supplied list (used by LoTW pipeline to control the field set). */
    public static void exportAdif(Path destination, List<QsoRecord> qsos, boolean contest) throws Exception {
        writeAdif(destination, qsos, contest);
        log.info("Exported {} QSOs to ADIF: {}", qsos.size(), destination);
    }

    // -----------------------------------------------------------------
    // Core writer
    // -----------------------------------------------------------------

    private static void writeAdif(Path destination, List<QsoRecord> qsos, boolean contest) throws IOException {
        AppConfig cfg = AppConfig.getInstance();
        String stationCall = cfg.getStationCallsign();
        String stationGrid = cfg.getGridSquare();

        // ContestQsoDao + QsoDao sort datetime_utc DESC for the cockpit UI
        // ("newest at top"). Receiving loggers (LoTW, master log import)
        // expect ascending chronological. Re-sort here; nulls trail.
        qsos = qsos.stream()
            .sorted(Comparator.comparing(
                QsoRecord::getDateTimeUtc,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        // For contest exports, look up the plugin once so we can emit
        // STX_STRING / SRX_STRING using the plugin's declared cabrilloSent /
        // cabrilloRcvd field order. Same exchange string the Cabrillo
        // exporter produces — gives downstream loggers human-readable
        // context for the QSO even when they don't know the contest's
        // semantic field map. Only meaningful when ALL QSOs are in one
        // contest (typical — exportContestAdif filters by contestId).
        ContestPlugin contestPlugin = null;
        if (contest && !qsos.isEmpty()) {
            String firstContestId = qsos.get(0).getContestId();
            boolean uniform = firstContestId != null
                && qsos.stream().allMatch(q -> firstContestId.equals(q.getContestId()));
            if (uniform) {
                contestPlugin = PluginLoader.getInstance().getById(firstContestId);
            }
        }

        // Lock UTF-8 explicitly — the no-arg FileWriter uses the JVM default
        // charset, which is Cp1252 on pre-JDK-18 Windows JREs and could drift
        // if file.encoding isn't set. AdifImporter accepts UTF-8 or cp1252;
        // we always emit UTF-8 for round-trip with modern loggers.
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(destination, StandardCharsets.UTF_8))) {
            pw.println("j-Log ADIF Export");
            adifField(pw, "ADIF_VER",      "3.1.0"); pw.println();
            adifField(pw, "PROGRAMID",     "j-Log"); pw.println();
            adifField(pw, "PROGRAMVERSION","1.0.42"); pw.println();
            pw.println("<EOH>");
            pw.println();

            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;   // never export flagged dupes

                adifField(pw, "CALL",      q.getCallsign());
                if (q.getDateTimeUtc() != null) {
                    adifField(pw, "QSO_DATE", q.getDateTimeUtc().format(DATE_FMT));
                    adifField(pw, "TIME_ON",  q.getDateTimeUtc().format(TIME_FMT));
                }
                adifField(pw, "BAND",      normalizeBand(q.getBand()));
                adifField(pw, "MODE",      normalizeMode(q.getMode()));
                adifField(pw, "FREQ",      normalizeFreqMHz(q.getFrequency()));
                // Default RST from the mode for contest exports — many
                // contest cockpits don't capture RST (it's not in the
                // formal exchange for SS/CQ WW/etc.) but LoTW rejects
                // records without RST_SENT/RST_RCVD. Normal-log exports
                // pass through whatever the operator logged (blank OK).
                String rstSent = q.getRstSent();
                String rstRcvd = q.getRstReceived();
                if (contest) {
                    if (rstSent == null || rstSent.isBlank()) rstSent = defaultRstForMode(q.getMode());
                    if (rstRcvd == null || rstRcvd.isBlank()) rstRcvd = defaultRstForMode(q.getMode());
                }
                adifField(pw, "RST_SENT",  rstSent);
                adifField(pw, "RST_RCVD",  rstRcvd);
                if (q.getPowerWatts() > 0) adifField(pw, "TX_PWR", String.valueOf(q.getPowerWatts()));

                if (!contest) {
                    adifField(pw, "COUNTRY",   q.getCountry());
                    adifField(pw, "NAME",      q.getOperatorName());
                    adifField(pw, "STATE",     q.getState());
                    adifField(pw, "CNTY",      q.getCounty());
                    adifField(pw, "COMMENT",   q.getNotes());
                    adifField(pw, "QSL_SENT",  q.isQslSent()     ? "Y" : "N");
                    adifField(pw, "QSL_RCVD",  q.isQslReceived() ? "Y" : "N");
                    // POTA/SOTA activation tags per ADIF spec.
                    adifField(pw, "SIG",       q.getSig());
                    adifField(pw, "SIG_INFO",  q.getSigInfo());
                } else {
                    // Contest path. STX/SRX preserve numeric serials per ADIF
                    // convention. STX_STRING/SRX_STRING carry the full
                    // exchange in the plugin's declared transmit order
                    // (built via the same helper Cabrillo uses, so the
                    // two file formats agree on what was exchanged). This
                    // makes the LoTW / master-log round-trip useful even
                    // when the receiving logger doesn't know the contest's
                    // semantic field map. A full per-tag mapping (ARRL_SECT,
                    // CHECK, CQZ, GRIDSQUARE, …) is a future enhancement
                    // requiring per-plugin adifField declarations.
                    adifField(pw, "CONTEST_ID",  q.getContestId());
                    adifField(pw, "STX",         q.getSerialSent());
                    adifField(pw, "SRX",         q.getSerialReceived());
                    if (contestPlugin != null) {
                        String stxString = CabrilloExporter.buildOrderedExchange(
                            q, contestPlugin, cfg, contestPlugin.getCabrilloSent());
                        String srxString = CabrilloExporter.buildOrderedExchange(
                            q, contestPlugin, cfg, contestPlugin.getCabrilloRcvd());
                        if (stxString != null && !stxString.isBlank())
                            adifField(pw, "STX_STRING", stxString);
                        if (srxString != null && !srxString.isBlank())
                            adifField(pw, "SRX_STRING", srxString);
                    }
                    adifField(pw, "OPERATOR",    q.getOperator());
                    if (q.getNotes() != null) adifField(pw, "COMMENT", q.getNotes());
                }

                // Station identity — critical for LoTW signature match.
                if (stationCall != null && !stationCall.isBlank())
                    adifField(pw, "STATION_CALLSIGN", stationCall);
                if (stationGrid != null && !stationGrid.isBlank())
                    adifField(pw, "MY_GRIDSQUARE", stationGrid);
                pw.println("<EOR>");
                pw.println();
            }
        }
    }

    // -----------------------------------------------------------------
    // CSV (unchanged)
    // -----------------------------------------------------------------

    public static void exportCsv(Path destination) throws Exception {
        List<QsoRecord> qsos = QsoDao.getInstance().fetchAll();
        // Lock UTF-8 explicitly — the no-arg FileWriter uses the JVM default
        // charset, which is Cp1252 on pre-JDK-18 Windows JREs and could drift
        // if file.encoding isn't set. AdifImporter accepts UTF-8 or cp1252;
        // we always emit UTF-8 for round-trip with modern loggers.
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(destination, StandardCharsets.UTF_8))) {
            pw.println("Callsign,Date,Time,Band,Mode,Frequency,Power,RSTSent,RSTRcvd," +
                       "Country,Name,State,County,Notes,QSLSent,QSLRcvd");
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter tmFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (QsoRecord q : qsos) {
                pw.println(String.join(",",
                    csv(q.getCallsign()),
                    q.getDateTimeUtc() != null ? q.getDateTimeUtc().format(dtFmt) : "",
                    q.getDateTimeUtc() != null ? q.getDateTimeUtc().format(tmFmt) : "",
                    csv(q.getBand()),
                    csv(q.getMode()),
                    csv(q.getFrequency()),
                    String.valueOf(q.getPowerWatts()),
                    csv(q.getRstSent()),
                    csv(q.getRstReceived()),
                    csv(q.getCountry()),
                    csv(q.getOperatorName()),
                    csv(q.getState()),
                    csv(q.getCounty()),
                    csv(q.getNotes()),
                    q.isQslSent()     ? "Y" : "N",
                    q.isQslReceived() ? "Y" : "N"
                ));
            }
            log.info("Exported {} QSOs to CSV: {}", qsos.size(), destination);
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Normalize j-log mode string to an ADIF-spec mode. Falls back to input
     *  uppercase if the value isn't in the map. */
    static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return null;
        String up = mode.trim().toUpperCase();
        return MODE_MAP.getOrDefault(up, up);
    }

    /** Normalize band to ADIF format. j-log stores "20m", ADIF expects "20m". */
    static String normalizeBand(String band) {
        if (band == null || band.isBlank()) return null;
        return band.trim().toLowerCase();
    }

    /** Convert whatever frequency string is in the DB to MHz decimal. Returns
     *  null if the value can't be interpreted. Accepts raw MHz ("14.250"),
     *  kHz ("14250"), or kHz-with-decimal ("14250.0"). */
    static String normalizeFreqMHz(String freq) {
        if (freq == null || freq.isBlank()) return null;
        String s = freq.trim().replaceAll("[^0-9.]", "");
        if (s.isEmpty()) return null;
        try {
            double d = Double.parseDouble(s);
            // Heuristic: < 500 assumed MHz, >= 500 assumed kHz.
            if (d >= 500) d = d / 1000.0;
            return String.format(Locale.ROOT, "%.5f", d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void adifField(PrintWriter pw, String tag, String value) {
        if (value == null || value.isBlank()) return;
        pw.print("<" + tag + ":" + value.length() + ">" + value + " ");
    }

    /** Conventional default RST by mode for contest exports where the
     *  operator didn't capture RST (common — SS/CQ WW/etc. don't include
     *  RST in the formal exchange). Phone modes get "59", CW/digital get
     *  "599". Keeps LoTW happy without forcing operators to fill in
     *  fields that aren't part of the contest exchange. */
    private static String defaultRstForMode(String mode) {
        if (mode == null) return "59";
        String m = mode.toUpperCase();
        if (m.contains("SSB") || m.equals("USB") || m.equals("LSB")
                || m.equals("AM") || m.equals("FM") || m.equals("PH")
                || m.equals("PHONE")) {
            return "59";
        }
        return "599";   // CW, RTTY, FT8, PSK31, OLIVIA, MFSK, JT*, etc.
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
