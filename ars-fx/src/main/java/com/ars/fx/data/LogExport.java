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
        JLogDb.Refs r = q.refs() == null ? JLogDb.Refs.NONE : q.refs();
        field(sb, "IOTA", r.iota()); field(sb, "SOTA_REF", r.sota()); field(sb, "POTA_REF", r.pota());
        if (r.cqz() > 0) field(sb, "CQZ", String.valueOf(r.cqz()));
        if (r.ituz() > 0) field(sb, "ITUZ", String.valueOf(r.ituz()));
        if (r.qsl()) field(sb, "QSL_RCVD", "Y");
        if (r.lotw()) field(sb, "LOTW_QSL_RCVD", "Y");
        if (r.eqsl()) field(sb, "EQSL_QSL_RCVD", "Y");
        sb.append("<EOR>\n");
    }

    /** Build a CSV document from the log. */
    public static String toCsv(List<JLogDb.QsoExport> rows) {
        StringBuilder sb = new StringBuilder("Call,Date,Time,Band,Mode,Freq,RST_Sent,RST_Rcvd,Name,QTH,Grid,State,County,Power,Comment,IOTA,SOTA,POTA,CQZ,ITUZ,QSL,LoTW,eQSL\n");
        for (JLogDb.QsoExport q : rows) {
            String[] dt = splitDate(q.datetimeUtc());
            JLogDb.Refs r = q.refs() == null ? JLogDb.Refs.NONE : q.refs();
            sb.append(String.join(",", csv(q.call()), nz(dt[0]), nz(dt[1]), csv(q.band()), csv(q.mode()), csv(q.freq()),
                    csv(q.rstSent()), csv(q.rstRcvd()), csv(q.name()), csv(q.qth()), csv(q.grid()), csv(q.state()),
                    csv(q.county()), q.powerW() > 0 ? String.valueOf(q.powerW()) : "", csv(q.notes()),
                    csv(r.iota()), csv(r.sota()), csv(r.pota()), r.cqz() > 0 ? String.valueOf(r.cqz()) : "", r.ituz() > 0 ? String.valueOf(r.ituz()) : "",
                    r.qsl() ? "Y" : "", r.lotw() ? "Y" : "", r.eqsl() ? "Y" : "")).append("\n");
        }
        return sb.toString();
    }

    /** J-Hub ▸ Data ▸ "Cabrillo / CSV charset" is ASCII (UTF-8 otherwise): accents are dropped, anything else becomes "?". */
    public static boolean asciiOnly() { return "ASCII".equalsIgnoreCase(HubConfig.get("data.charset", "UTF-8").trim()); }
    /** The text as the chosen charset needs it (unchanged for UTF-8). */
    public static String forExport(String s) {
        if (s == null || !asciiOnly()) return s;
        String d = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder sb = new StringBuilder(d.length());
        for (int i = 0; i < d.length(); i++) { char c = d.charAt(i); sb.append(c < 128 ? c : '?'); }
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
            Files.writeString(f, forExport(body), StandardCharsets.UTF_8);
            return new Result(true, rows.size(), f.toString(), null);
        } catch (Exception e) {
            return new Result(false, 0, null, e.getMessage());
        }
    }

    public static Path defaultBackupDir() { return resolve(null); }

    // ---- a contest log (the log browser's contest view) ---------------------

    /** ADIF for contest QSOs: the usual fields plus the contest's serials (STX / SRX), the exchange received (SRX_STRING) and the contest id. */
    public static String toAdifContest(List<com.jlog.model.QsoRecord> rows, String cabrilloContestId) {
        StringBuilder sb = new StringBuilder();
        sb.append("ADIF export — ARS Suite contest log\n");
        field(sb, "ADIF_VER", "3.1.4"); field(sb, "PROGRAMID", "ARS-Suite-JLog");
        sb.append("\n<EOH>\n");
        for (com.jlog.model.QsoRecord q : rows) {
            java.time.LocalDateTime t = q.getDateTimeUtc();
            field(sb, "CALL", q.getCallsign());
            if (t != null) { field(sb, "QSO_DATE", t.format(DateTimeFormatter.BASIC_ISO_DATE)); field(sb, "TIME_ON", t.format(DateTimeFormatter.ofPattern("HHmmss"))); }
            field(sb, "BAND", q.getBand() == null ? null : q.getBand().toLowerCase());
            String[] m = mode(q.getMode()); field(sb, "MODE", m[0]); field(sb, "SUBMODE", m[1]);
            field(sb, "FREQ", ContestFreq.toMhz(q.getFrequency()));          // stored in kHz for Cabrillo; ADIF wants MHz
            field(sb, "RST_SENT", q.getRstSent()); field(sb, "RST_RCVD", q.getRstReceived());
            field(sb, "STX", q.getSerialSent()); field(sb, "SRX", q.getSerialReceived());
            field(sb, "SRX_STRING", exchange(q));
            field(sb, "CONTEST_ID", cabrilloContestId);
            field(sb, "COMMENT", q.getNotes());
            sb.append("<EOR>\n");
        }
        return sb.toString();
    }

    /** CSV for contest QSOs (frequency in kHz, as logged). */
    public static String toCsvContest(List<com.jlog.model.QsoRecord> rows) {
        StringBuilder sb = new StringBuilder("Call,Date,Time,Band,Mode,Freq_kHz,RST_Sent,RST_Rcvd,Serial_Sent,Serial_Rcvd,Exchange,Points,Dupe,Notes\n");
        for (com.jlog.model.QsoRecord q : rows) {
            java.time.LocalDateTime t = q.getDateTimeUtc();
            sb.append(String.join(",", csv(q.getCallsign()),
                    t == null ? "" : t.format(DateTimeFormatter.BASIC_ISO_DATE), t == null ? "" : t.format(DateTimeFormatter.ofPattern("HHmmss")),
                    csv(q.getBand()), csv(q.getMode()), csv(q.getFrequency()), csv(q.getRstSent()), csv(q.getRstReceived()),
                    csv(q.getSerialSent()), csv(q.getSerialReceived()), csv(exchange(q)), String.valueOf(q.getPoints()),
                    q.isDupe() ? "Y" : "", csv(q.getNotes()))).append("\n");
        }
        return sb.toString();
    }

    /** The exchange received, as typed in the contest's fields, e.g. "5NN 05". */
    public static String exchange(com.jlog.model.QsoRecord q) {
        StringBuilder b = new StringBuilder();
        for (String f : new String[]{q.getContestField1(), q.getContestField2(), q.getContestField3(), q.getContestField4(), q.getContestField5()})
            if (f != null && !f.isBlank()) { if (b.length() > 0) b.append(' '); b.append(f.trim()); }
        return b.toString();
    }

    /** Write these contest QSOs (the browser's filtered view, in the order shown) to {@code file}: {@code .csv} is CSV, anything else ADIF. */
    public static Result exportContest(Path file, List<com.jlog.model.QsoRecord> rows, String cabrilloContestId) {
        try {
            boolean csv = file.getFileName().toString().toLowerCase().endsWith(".csv");
            String body = csv ? toCsvContest(rows) : toAdifContest(rows, cabrilloContestId);
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, forExport(body), StandardCharsets.UTF_8);
            return new Result(true, rows.size(), file.toString(), null);
        } catch (Exception e) {
            return new Result(false, 0, null, e.getMessage());
        }
    }

    /** A log-browser row as an export row (the browser's QTH / name / grid are the same columns the exporter reads). */
    public static JLogDb.QsoExport fromFull(JLogDb.QsoFull q) {
        return new JLogDb.QsoExport(q.call(), q.datetimeUtc(), q.band(), q.mode(), q.freq(), q.powerW(), q.rstSent(),
                q.rstRcvd(), q.qth(), q.name(), q.state(), q.county(), q.notes(), q.grid(), q.refs());
    }

    /** Write just these QSOs (the browser's filtered view, in the order shown) to {@code file}.
     *  The format follows the extension: {@code .csv} is CSV, anything else is ADIF. */
    public static Result exportFiltered(Path file, List<JLogDb.QsoFull> rows) {
        try {
            List<JLogDb.QsoExport> out = new java.util.ArrayList<>(rows.size());
            for (JLogDb.QsoFull q : rows) out.add(fromFull(q));
            boolean csv = file.getFileName().toString().toLowerCase().endsWith(".csv");
            String body = csv ? toCsv(out) : toAdif(out);
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, forExport(body), StandardCharsets.UTF_8);
            return new Result(true, out.size(), file.toString(), null);
        } catch (Exception e) {
            return new Result(false, 0, null, e.getMessage());
        }
    }

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
