package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.ContestQtcDao;
import com.jlog.model.QsoRecord;
import com.jlog.model.QtcRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports a contest log to Cabrillo using the plugin's ordered
 * cabrilloSent / cabrilloRcvd exchange spec.
 */
public class CabrilloExporter {

    private static final Logger log = LoggerFactory.getLogger(CabrilloExporter.class);
    private static final DateTimeFormatter CAB_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CAB_TIME = DateTimeFormatter.ofPattern("HHmm");

    public static void export(ContestPlugin plugin, Path destination) throws Exception {
        // fetchByContest sorts datetime_utc DESC for the cockpit UI
        // ("newest at top"). Cabrillo sponsors want the log in chronological
        // order, so re-sort ascending here. Nulls (corrupt rows missing a
        // timestamp) trail at the end rather than throwing.
        List<QsoRecord> qsos = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId())
            .stream()
            .sorted(java.util.Comparator.comparing(
                QsoRecord::getDateTimeUtc,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();
        AppConfig cfg = AppConfig.getInstance();

        // Lock UTF-8 explicitly — see comment in AdifExporter for rationale.
        // Cabrillo 3.0 spec is ASCII-only, but operator notes/comments may
        // contain accented characters and we don't want JVM-default drift.
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(destination, StandardCharsets.UTF_8))) {
            // Cabrillo 3.0 header — categories come from AppConfig (set via the
            // pre-export dialog) rather than being hardcoded. Score computed
            // against plugin rules instead of being a raw point sum.
            pw.println("START-OF-LOG: 3.0");
            pw.println("CREATED-BY: j-Log v1.0.0");
            // Honor the plugin's sponsor-accepted name when set; otherwise
            // fall back to the internal id with underscores dashed. Override
            // is required for split-side plugins (ARRL_DX_SSB_US/DX both
            // emit "ARRL-DX-SSB") and any other case where our internal id
            // doesn't match what the sponsor's robot expects.
            String contestHeader = (plugin.getCabrilloContestName() != null
                                    && !plugin.getCabrilloContestName().isBlank())
                ? plugin.getCabrilloContestName()
                : plugin.getContestId().replace("_", "-");
            pw.println("CONTEST: " + contestHeader);
            pw.println("CALLSIGN: " + nullSafe(cfg.getStationCallsign()));
            pw.println("CATEGORY-OPERATOR: "    + cfg.getCabOperator());
            pw.println("CATEGORY-BAND: "        + cfg.getCabBand());
            pw.println("CATEGORY-MODE: "        + cfg.getCabMode());
            pw.println("CATEGORY-POWER: "       + cfg.getCabPower());
            pw.println("CATEGORY-ASSISTED: "    + cfg.getCabAssisted());
            pw.println("CATEGORY-TRANSMITTER: " + cfg.getCabTransmitter());
            pw.println("CATEGORY-STATION: "     + cfg.getCabStation());
            if (!cfg.getCabTime().isBlank())  pw.println("CATEGORY-TIME: "  + cfg.getCabTime());
            pw.println("CLAIMED-SCORE: "        + computeClaimedScore(plugin));
            pw.println("NAME: "    + nullSafe(cfg.getOperatorName()));
            pw.println("ADDRESS: " + nullSafe(cfg.getQth()));
            if (!cfg.getCabEmail().isBlank()) pw.println("EMAIL: " + cfg.getCabEmail());
            if (!cfg.getCabClub().isBlank())  pw.println("CLUB: "  + cfg.getCabClub());
            if (!nullSafe(cfg.getGridSquare()).isBlank())
                pw.println("GRID-LOCATOR: " + cfg.getGridSquare());
            pw.println("X-JLOG-PLUGIN: " + plugin.getContestId() + " v" + plugin.getVersion());
            pw.println();

            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;

                String freq = q.getFrequency() != null ? q.getFrequency() : "14000";
                String band = cabBand(q.getBand());
                String mode = cabMode(q.getMode());
                String date = q.getDateTimeUtc() != null ? q.getDateTimeUtc().format(CAB_DATE) : "0000-00-00";
                String time = q.getDateTimeUtc() != null ? q.getDateTimeUtc().format(CAB_TIME) : "0000";

                // Build QSO line from plugin mapping
                // Format: QSO: <freq> <mode> <date> <time> <mycall> <sent-exch> <dxcall> <rcvd-exch>
                String myCall  = nullSafe(cfg.getStationCallsign());
                String dxCall  = nullSafe(q.getCallsign());
                // Exchange emitted in the plugin's declared transmit order.
                String sentEx  = buildOrderedExchange(q, plugin, cfg, plugin.getCabrilloSent());
                String rcvdEx  = buildOrderedExchange(q, plugin, cfg, plugin.getCabrilloRcvd());

                pw.printf("QSO: %5s %-2s %s %s %-13s %-20s %-13s %-20s%n",
                    freq, mode, date, time, myCall, sentEx, dxCall, rcvdEx);
            }

            // WAE-DC QTC traffic (Rule §7) — emitted after the QSO lines.
            // Syntax: QTC: QRG MODE DATE TIME CALL-RX QTC-GRP CALL-TX
            //              TIME-QSO CALL-QSO NR-QSO
            List<QtcRecord> qtcs = ContestQtcDao.getInstance().fetchByContest(plugin.getContestId());
            String myCall = nullSafe(cfg.getStationCallsign());
            for (QtcRecord r : qtcs) {
                boolean rx = "RX".equalsIgnoreCase(r.getDirection());
                String callRx = rx ? myCall : r.getPartnerCall();
                String callTx = rx ? r.getPartnerCall() : myCall;
                String qDate = r.getQtcDateTimeUtc() != null
                        ? r.getQtcDateTimeUtc().format(CAB_DATE) : "0000-00-00";
                String qTime = r.getQtcDateTimeUtc() != null
                        ? r.getQtcDateTimeUtc().format(CAB_TIME) : "0000";
                pw.printf("QTC: %5s %-2s %s %s %-13s %5s %-13s %4s %-13s %s%n",
                    r.getQtcQrg() != null && !r.getQtcQrg().isBlank() ? r.getQtcQrg() : "14000",
                    cabMode(r.getQtcMode()), qDate, qTime,
                    callRx, r.getSeriesNo() + "/" + r.getSeriesSize(), callTx,
                    nullSafe(r.getQsoTime()), nullSafe(r.getQsoCall()), nullSafe(r.getQsoSerial()));
            }

            pw.println("END-OF-LOG:");
            log.info("Cabrillo export: {} QSOs, {} QTCs to {}",
                    qsos.size(), qtcs.size(), destination);
        }
    }

    /** Claimed score = (stored QSO points) × (multipliers) using the plugin's
     *  rules. Field-day-style contests and others with {@code scoreIsPointsOnly}
     *  get just the point total. Per-mode and per-band multiplier contests are
     *  unfolded so the score matches what the controller's stats pane shows. */
    private static int computeClaimedScore(ContestPlugin plugin) {
        try {
            ContestQsoDao dao = ContestQsoDao.getInstance();
            String contestId  = plugin.getContestId();
            // QTC traffic counts one point each (WAE Rule §7). No-op for every
            // other contest (no contest_qtc rows → 0).
            int totalPoints   = dao.totalPointsByContest(contestId)
                    + ContestQtcDao.getInstance().totalQtcPointsByContest(contestId);

            ContestPlugin.ScoringRules rules = plugin.getScoringRules();
            if (rules != null && rules.isScoreIsPointsOnly()) return totalPoints;
            // NOTE: for multiplierType "wae" the band-weighted multiplier needs
            // the j-log WaeMultiplier resolver, which the engine layer cannot
            // reference. The header CLAIMED-SCORE multiplier is therefore
            // approximate for WAE; the in-app cockpit shows the correct running
            // score and the WAEDC committee re-adjudicates from QSO/QTC lines.

            String multColumn = plugin.computeMultiplierDbColumn();
            int mults;
            if (plugin.isPerModeMultipliers()) {
                mults = 0;
                for (String mode : new String[]{"CW", "Phone"})
                    mults += dao.distinctFieldByColumnAndMode(contestId, multColumn, mode).size();
            } else if (plugin.getMultiplierModel() != null
                    && plugin.getMultiplierModel().isPerBand()) {
                mults = 0;
                List<String> bands = null;
                for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
                    if ("band".equals(fd.getId()) && fd.getOptions() != null)
                        bands = fd.getOptions();
                }
                if (bands == null) bands = List.of("160m","80m","40m","20m","15m","10m","6m","2m","70cm");
                for (String band : bands)
                    mults += dao.distinctFieldByColumnAndBand(contestId, multColumn, band).size();
            } else {
                mults = dao.distinctFieldByColumn(contestId, multColumn).size();
            }
            return totalPoints * Math.max(1, mults);
        } catch (Exception e) {
            log.warn("claimed-score computation failed: {}", e.getMessage());
            return 0;
        }
    }

    /** Build an exchange string from an explicit ordered list of field-id
     *  tokens (declared sponsor transmit order). Package-private so
     *  {@link AdifExporter} can reuse the same logic when emitting
     *  STX_STRING / SRX_STRING for contest QSOs. */
    static String buildOrderedExchange(QsoRecord q, ContestPlugin plugin,
                                                AppConfig cfg, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String id : tokens) {
            String v = resolveField(q, plugin, cfg, id);
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    /**
     * Resolve a cabrilloSent/cabrilloRcvd token (a plugin entry-field id, a
     * field1..5 slot, or a special name) to its QsoRecord value.
     *
     * Plugins map exchange columns by their semantic field id (e.g. SS
     * "precedence"/"section", CQ WW "cq_zone"/"zone_sent"), not field1..5.
     * Those used to fall through to null and were silently dropped from the
     * QSO line; now they resolve to the field1..5 slot the controller stored
     * them in. The SS sent constants (prec/check/sect_sent) aren't persisted
     * per QSO — they come from station config.
     */
    private static String resolveField(QsoRecord q, ContestPlugin plugin,
                                        AppConfig cfg, String fieldId) {
        // Operator-constant field (RR name/year/section, FD class/section…):
        // not stored per QSO — resolve from station config. Checked first so a
        // plugin that reuses an id the SS legacy cases below also use (e.g. FD
        // "sect_sent") is not hijacked by SS's getSsSection().
        if (plugin != null) {
            ContestPlugin.FieldDef cfd = plugin.getField(fieldId);
            if (cfd != null && cfd.isConstant())
                return cfg.getContestConstant(plugin.getContestId(), fieldId);
        }
        switch (fieldId) {
            case "callsign":    return q.getCallsign();
            case "serial_sent": return q.getSerialSent();
            case "serial_rcvd": return q.getSerialReceived();
            case "rst_sent":    return q.getRstSent();
            case "rst_rcvd":    return q.getRstReceived();
            case "mycall":      return cfg.getStationCallsign();
            case "band":        return q.getBand();
            case "mode":        return q.getMode();
            case "field1":      return q.getContestField1();
            case "field2":      return q.getContestField2();
            case "field3":      return q.getContestField3();
            case "field4":      return q.getContestField4();
            case "field5":      return q.getContestField5();
            // SS own sent exchange — station constants, not stored per QSO.
            case "prec_sent":   return cfg.getSsPrecedence();
            case "check_sent":  return cfg.getSsCheck();
            case "sect_sent":   return cfg.getSsSection();
            default:
                String col = plugin != null ? plugin.fieldSlotColumn(fieldId) : null;
                return switch (col == null ? "" : col) {
                    case "field1" -> q.getContestField1();
                    case "field2" -> q.getContestField2();
                    case "field3" -> q.getContestField3();
                    case "field4" -> q.getContestField4();
                    case "field5" -> q.getContestField5();
                    default       -> null;
                };
        }
    }

    private static String cabBand(String band) {
        if (band == null) return "??";
        return switch (band) {
            case "160m" -> "1800";
            case "80m"  -> "3500";
            case "40m"  -> "7000";
            case "20m"  -> "14000";
            case "15m"  -> "21000";
            case "10m"  -> "28000";
            case "6m"   -> "50";
            case "2m"   -> "144";
            case "70cm" -> "432";
            default     -> band;
        };
    }

    private static String cabMode(String mode) {
        if (mode == null) return "??";
        return switch (mode.toUpperCase()) {
            case "CW"           -> "CW";
            case "USB", "LSB",
                 "AM", "FM"     -> "PH";
            case "RTTY","FSK-R" -> "RY";
            case "FT8","FT4",
                 "PSK31"        -> "DG";
            default             -> "PH";
        };
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
