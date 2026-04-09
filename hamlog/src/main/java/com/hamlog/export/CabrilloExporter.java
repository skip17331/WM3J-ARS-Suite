package com.hamlog.export;

import com.hamlog.db.ContestQsoDao;
import com.hamlog.model.QsoRecord;
import com.hamlog.plugin.ContestPlugin;
import com.hamlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports a contest log to Cabrillo format using the plugin's cabrilloMapping.
 */
public class CabrilloExporter {

    private static final Logger log = LoggerFactory.getLogger(CabrilloExporter.class);
    private static final DateTimeFormatter CAB_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CAB_TIME = DateTimeFormatter.ofPattern("HHmm");

    public static void export(ContestPlugin plugin, Path destination) throws Exception {
        List<QsoRecord> qsos = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
        AppConfig cfg = AppConfig.getInstance();
        Map<String, String> mapping = plugin.getCabrilloMapping();

        try (PrintWriter pw = new PrintWriter(new FileWriter(destination.toFile()))) {
            // Cabrillo header
            pw.println("START-OF-LOG: 3.0");
            pw.println("CREATED-BY: HamLog v1.0.0");
            pw.println("CONTEST: " + plugin.getContestId().replace("_", "-"));
            pw.println("CALLSIGN: " + nullSafe(cfg.getStationCallsign()));
            pw.println("CATEGORY-OPERATOR: SINGLE-OP");
            pw.println("CATEGORY-BAND: ALL");
            pw.println("CATEGORY-POWER: HIGH");
            pw.println("CLAIMED-SCORE: " + ContestQsoDao.getInstance().totalPointsByContest(plugin.getContestId()));
            pw.println("NAME: " + nullSafe(cfg.getOperatorName()));
            pw.println("ADDRESS: " + nullSafe(cfg.getQth()));
            pw.println("X-HAMLOG-PLUGIN: " + plugin.getContestId() + " v" + plugin.getVersion());
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
                String sentEx  = buildExchange(q, mapping, "sent");
                String rcvdEx  = buildExchange(q, mapping, "rcvd");

                pw.printf("QSO: %5s %-2s %s %s %-13s %-20s %-13s %-20s%n",
                    freq, mode, date, time, myCall, sentEx, dxCall, rcvdEx);
            }

            pw.println("END-OF-LOG:");
            log.info("Cabrillo export: {} QSOs to {}", qsos.size(), destination);
        }
    }

    private static String buildExchange(QsoRecord q, Map<String, String> mapping, String direction) {
        if (mapping == null) return "";
        StringBuilder sb = new StringBuilder();
        // Collect fields mapped to this direction (prefix "sent_" or "rcvd_")
        mapping.entrySet().stream()
            .filter(e -> e.getValue().startsWith(direction + "_"))
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                String val = resolveField(q, e.getKey());
                if (val != null && !val.isBlank()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(val);
                }
            });
        return sb.toString();
    }

    private static String resolveField(QsoRecord q, String fieldId) {
        return switch (fieldId) {
            case "callsign"      -> q.getCallsign();
            case "serial_sent"   -> q.getSerialSent();
            case "serial_rcvd"   -> q.getSerialReceived();
            case "field1"        -> q.getContestField1();
            case "field2"        -> q.getContestField2();
            case "field3"        -> q.getContestField3();
            case "field4"        -> q.getContestField4();
            case "field5"        -> q.getContestField5();
            case "rst_sent"      -> q.getRstSent();
            case "rst_rcvd"      -> q.getRstReceived();
            case "exchange"      -> q.getExchange();
            default              -> null;
        };
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
