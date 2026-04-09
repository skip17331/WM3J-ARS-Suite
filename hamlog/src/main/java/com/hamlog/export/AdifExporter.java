package com.hamlog.export;

import com.hamlog.db.QsoDao;
import com.hamlog.model.QsoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export engine for ADIF and CSV formats.
 */
public class AdifExporter {

    private static final Logger log = LoggerFactory.getLogger(AdifExporter.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    /** Export entire normal log to ADIF file. */
    public static void exportAdif(Path destination) throws Exception {
        List<QsoRecord> qsos = QsoDao.getInstance().fetchAll();
        try (PrintWriter pw = new PrintWriter(new FileWriter(destination.toFile()))) {
            // ADIF header
            pw.println("HamLog ADIF Export");
            pw.println("<ADIF_VER:5>3.1.0");
            pw.println("<PROGRAMID:6>HamLog");
            pw.println("<EOH>");
            pw.println();

            for (QsoRecord q : qsos) {
                adifField(pw, "CALL",      q.getCallsign());
                if (q.getDateTimeUtc() != null) {
                    adifField(pw, "QSO_DATE", q.getDateTimeUtc().format(DATE_FMT));
                    adifField(pw, "TIME_ON",  q.getDateTimeUtc().format(TIME_FMT));
                }
                adifField(pw, "BAND",      q.getBand());
                adifField(pw, "MODE",      q.getMode());
                adifField(pw, "FREQ",      q.getFrequency());
                adifField(pw, "TX_PWR",    String.valueOf(q.getPowerWatts()));
                adifField(pw, "RST_SENT",  q.getRstSent());
                adifField(pw, "RST_RCVD",  q.getRstReceived());
                adifField(pw, "COUNTRY",   q.getCountry());
                adifField(pw, "NAME",      q.getOperatorName());
                adifField(pw, "STATE",     q.getState());
                adifField(pw, "CNTY",      q.getCounty());
                adifField(pw, "COMMENT",   q.getNotes());
                adifField(pw, "QSL_SENT",  q.isQslSent()     ? "Y" : "N");
                adifField(pw, "QSL_RCVD",  q.isQslReceived() ? "Y" : "N");
                pw.println("<EOR>");
                pw.println();
            }
            log.info("Exported {} QSOs to ADIF: {}", qsos.size(), destination);
        }
    }

    /** Export entire normal log to CSV file. */
    public static void exportCsv(Path destination) throws Exception {
        List<QsoRecord> qsos = QsoDao.getInstance().fetchAll();
        try (PrintWriter pw = new PrintWriter(new FileWriter(destination.toFile()))) {
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

    private static void adifField(PrintWriter pw, String tag, String value) {
        if (value == null || value.isBlank()) return;
        pw.print("<" + tag + ":" + value.length() + ">" + value + " ");
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
