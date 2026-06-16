package com.ars.fx.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports the J-Log database to standard interchange formats — ADIF (.adi) for
 * LoTW / eQSL / QRZ / Club Log and other loggers, and CSV for spreadsheets.
 * Writes timestamped files into a backup folder so each export is a point-in-time
 * snapshot rather than an overwrite.
 */
public final class LogExport {
    private LogExport() {}

    public record Result(boolean ok, int count, String path, String error) {}

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Build a full ADIF document from the log. */
    public static String toAdif(List<JLogDb.QsoExport> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("ADIF export — ARS Suite J-Log\n");
        field(sb, "ADIF_VER", "3.1.4"); field(sb, "PROGRAMID", "ARS-Suite-JLog");
        sb.append("\n<EOH>\n");
        for (JLogDb.QsoExport q : rows) appendRecord(sb, q);
        return sb.toString();
    }

    /** A single ADIF record ({@code <…>…<EOR>}) for one QSO — used by per-record uploaders (QRZ Logbook). */
    public static String toAdifRecord(JLogDb.QsoExport q) {
        StringBuilder sb = new StringBuilder();
        appendRecord(sb, q);
        return sb.toString();
    }

    private static void appendRecord(StringBuilder sb, JLogDb.QsoExport q) {
        String[] dt = splitDate(q.datetimeUtc());
        field(sb, "CALL", q.call());
        field(sb, "QSO_DATE", dt[0]); field(sb, "TIME_ON", dt[1]);
        field(sb, "BAND", q.band() == null ? null : q.band().toLowerCase());
        String[] m = mode(q.mode()); field(sb, "MODE", m[0]); field(sb, "SUBMODE", m[1]);
        field(sb, "FREQ", q.freq());
        field(sb, "RST_SENT", q.rstSent()); field(sb, "RST_RCVD", q.rstRcvd());
        field(sb, "NAME", q.name()); field(sb, "QTH", q.qth()); field(sb, "GRIDSQUARE", q.grid());
        field(sb, "STATE", q.state()); field(sb, "CNTY", q.county());
        if (q.powerW() > 0) field(sb, "TX_PWR", String.valueOf(q.powerW()));
        field(sb, "COMMENT", q.notes());
        sb.append("<EOR>\n");
    }

    /** Build a CSV document from the log. */
    public static String toCsv(List<JLogDb.QsoExport> rows) {
        StringBuilder sb = new StringBuilder("Call,Date,Time,Band,Mode,Freq,RST_Sent,RST_Rcvd,Name,QTH,Grid,State,County,Power,Comment\n");
        for (JLogDb.QsoExport q : rows) {
            String[] dt = splitDate(q.datetimeUtc());
            sb.append(String.join(",", csv(q.call()), nz(dt[0]), nz(dt[1]), csv(q.band()), csv(q.mode()), csv(q.freq()),
                    csv(q.rstSent()), csv(q.rstRcvd()), csv(q.name()), csv(q.qth()), csv(q.grid()), csv(q.state()),
                    csv(q.county()), q.powerW() > 0 ? String.valueOf(q.powerW()) : "", csv(q.notes()))).append("\n");
        }
        return sb.toString();
    }

    /** Write a timestamped backup ({@code j-log-YYYYMMDD-HHMMSS.<ext>}) to {@code dir}; format = "adi" or "csv". */
    public static Result backup(String dir, String format) {
        try {
            List<JLogDb.QsoExport> rows = JLogDb.allForExport();
            String ext = "csv".equalsIgnoreCase(format) ? "csv" : "adi";
            String body = ext.equals("csv") ? toCsv(rows) : toAdif(rows);
            Path d = resolve(dir); Files.createDirectories(d);
            Path f = d.resolve("j-log-" + LocalDateTime.now().format(STAMP) + "." + ext);
            Files.writeString(f, body, StandardCharsets.UTF_8);
            return new Result(true, rows.size(), f.toString(), null);
        } catch (Exception e) {
            return new Result(false, 0, null, e.getMessage());
        }
    }

    public static Path defaultBackupDir() { return resolve(null); }

    // ---- helpers -----------------------------------------------------------
    private static Path resolve(String dir) {
        String d = (dir == null || dir.isBlank()) ? System.getProperty("user.home") + "/.j-log/backups" : dir.trim();
        if (d.startsWith("~")) d = System.getProperty("user.home") + d.substring(1);
        return Paths.get(d);
    }
    private static void field(StringBuilder sb, String name, String val) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) return;
        sb.append("<").append(name).append(":").append(v.length()).append(">").append(v).append(' ');
    }
    /** "yyyy-MM-dd HH:mm:ss" → {"yyyymmdd","HHMMSS"}. */
    private static String[] splitDate(String dt) {
        if (dt == null || dt.length() < 19) return new String[]{null, null};
        return new String[]{dt.substring(0, 10).replace("-", ""), dt.substring(11, 19).replace(":", "")};
    }
    /** Map a logged mode to ADIF MODE + SUBMODE (USB/LSB → SSB; PSK31 → PSK). */
    private static String[] mode(String m) {
        if (m == null) return new String[]{null, null};
        switch (m.trim().toUpperCase()) {
            case "USB": return new String[]{"SSB", "USB"};
            case "LSB": return new String[]{"SSB", "LSB"};
            case "PSK31": return new String[]{"PSK", "PSK31"};
            case "PSK63": return new String[]{"PSK", "PSK63"};
            default: return new String[]{m.trim().toUpperCase(), null};
        }
    }
    private static String csv(String s) {
        if (s == null) return "";
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
    private static String nz(String s) { return s == null ? "" : s; }
}
