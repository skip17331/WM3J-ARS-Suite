package com.jlog.export;

import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Drives LoTW interactions via the {@code tqsl} command-line tool. LoTW itself
 * requires signatures from ARRL's PKI, which only {@code tqsl} knows how to
 * produce — we prepare the ADIF, invoke tqsl, and surface its status.
 *
 * Requires tqsl to be installed (Debian/Ubuntu: {@code apt install trustedqsl}).
 * The station location must already be defined inside tqsl (one-time setup
 * outside this app); the same name is then stored in AppConfig so uploads use
 * it automatically.
 *
 * Three operations:
 *   {@link #signAndUpload(List)} — prepare ADIF, sign, and upload to LoTW.
 *   {@link #signOnly(List, Path)} — produce a {@code .tq8} for manual upload.
 *   {@link #downloadConfirmations(LocalDate)} — fetch new QSL confirmations.
 */
public class LotwService {

    private static final Logger log = LoggerFactory.getLogger(LotwService.class);
    private static final DateTimeFormatter ADIF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static class TqslResult {
        public final int     exitCode;
        public final String  stdout;
        public final String  stderr;
        public TqslResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode; this.stdout = stdout; this.stderr = stderr;
        }
        public boolean ok() { return exitCode == 0; }
    }

    public static class DownloadResult extends TqslResult {
        public int matched;         // confirmations successfully matched to local QSOs
        public int unmatched;       // confirmations with no matching local QSO
        public DownloadResult(TqslResult t) { super(t.exitCode, t.stdout, t.stderr); }
    }

    // -----------------------------------------------------------------
    // Upload pipeline
    // -----------------------------------------------------------------

    /**
     * Prepare an ADIF containing {@code qsos}, invoke tqsl to sign and upload
     * to LoTW in a single step. Uses the station location from AppConfig.
     */
    public static TqslResult signAndUpload(List<QsoRecord> qsos) throws Exception {
        Path tmp = Files.createTempFile("j-log-lotw-", ".adi");
        try {
            AdifExporter.exportAdif(tmp, qsos, false);
            return runTqsl(new String[]{
                "-x", "-d",
                "-l", requireStationLocation(),
                "-u",                 // upload after signing
                tmp.toString()
            });
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    /** Sign only — produces a {@code .tq8} file the operator can upload later. */
    public static TqslResult signOnly(List<QsoRecord> qsos, Path signedDest) throws Exception {
        Path tmp = Files.createTempFile("j-log-lotw-", ".adi");
        try {
            AdifExporter.exportAdif(tmp, qsos, false);
            return runTqsl(new String[]{
                "-x", "-d",
                "-l", requireStationLocation(),
                "-o", signedDest.toString(),
                tmp.toString()
            });
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    // -----------------------------------------------------------------
    // Download confirmations
    // -----------------------------------------------------------------

    /**
     * Pull confirmations from LoTW (optionally limited to QSOs on or after
     * {@code sinceDate}), update matching local QSOs' {@code qsl_rcvd} flag.
     */
    public static DownloadResult downloadConfirmations(LocalDate sinceDate) throws Exception {
        Path tmp = Files.createTempFile("j-log-lotw-dl-", ".adi");
        try {
            List<String> args = new ArrayList<>();
            args.add("-x"); args.add("-d");
            args.add("-l"); args.add(requireStationLocation());
            if (sinceDate != null) {
                args.add("-k"); args.add(sinceDate.format(ADIF_DATE));  // "since" filter
            }
            args.add("-o"); args.add(tmp.toString());
            // tqsl -x mode with no input file and -o triggers confirmation download
            TqslResult r = runTqsl(args.toArray(new String[0]));
            DownloadResult dl = new DownloadResult(r);
            if (r.ok() && Files.exists(tmp) && Files.size(tmp) > 0) {
                dl.matched    = applyConfirmations(tmp, dl);
            }
            return dl;
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    /** Parse the returned ADIF and update qsl_rcvd on matching QSOs. Returns
     *  the count of successfully-matched rows. */
    private static int applyConfirmations(Path adifFile, DownloadResult dl) throws Exception {
        int matched   = 0;
        int unmatched = 0;
        String content = Files.readString(adifFile);
        // Reuse the import parser's field-extraction approach: simple state
        // machine walking <TAG:len>value tokens until <EOR>.
        int cursor = 0;
        int eoh    = indexOfIgnoreCase(content, "<EOH>");
        if (eoh >= 0) cursor = eoh + 5;
        Map<String, String> rec = new HashMap<>();
        while (cursor < content.length()) {
            int lt = content.indexOf('<', cursor);
            if (lt < 0) break;
            int gt = content.indexOf('>', lt);
            if (gt < 0) break;
            String header = content.substring(lt + 1, gt);
            cursor = gt + 1;
            if ("EOR".equalsIgnoreCase(header.trim())) {
                if (recordIsConfirmed(rec)) {
                    boolean ok = updateLocalQslRcvd(rec);
                    if (ok) matched++; else unmatched++;
                }
                rec.clear();
                continue;
            }
            if ("EOH".equalsIgnoreCase(header.trim())) continue;
            String[] parts = header.split(":");
            if (parts.length < 2) continue;
            int len;
            try { len = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException e) { continue; }
            if (cursor + len > content.length()) break;
            rec.put(parts[0].trim().toUpperCase(), content.substring(cursor, cursor + len));
            cursor += len;
        }
        dl.unmatched = unmatched;
        return matched;
    }

    private static boolean recordIsConfirmed(Map<String, String> rec) {
        // LoTW-returned records include QSL_RCVD=Y for confirmations.
        return "Y".equalsIgnoreCase(rec.get("QSL_RCVD"))
            || "Y".equalsIgnoreCase(rec.get("LOTW_QSL_RCVD"));
    }

    private static boolean updateLocalQslRcvd(Map<String, String> rec) {
        try {
            String call = rec.get("CALL");
            String band = rec.get("BAND");
            String mode = rec.get("MODE");
            if (call == null) return false;
            // Look for any local QSO matching (call,band,mode); flip qsl_rcvd.
            // We scan fetchAll rather than add a targeted query — LoTW downloads
            // are typically small and this keeps the DAO surface lean.
            for (QsoRecord q : QsoDao.getInstance().fetchAll()) {
                if (!call.equalsIgnoreCase(q.getCallsign())) continue;
                if (band != null && !band.equalsIgnoreCase(q.getBand())) continue;
                if (mode != null && !mode.equalsIgnoreCase(q.getMode())) continue;
                if (q.isQslReceived()) return true; // already flagged
                q.setQslReceived(true);
                QsoDao.getInstance().update(q);
                return true;
            }
        } catch (Exception e) {
            log.warn("failed to apply confirmation: {}", e.getMessage());
        }
        return false;
    }

    // -----------------------------------------------------------------
    // tqsl subprocess glue
    // -----------------------------------------------------------------

    private static TqslResult runTqsl(String[] args) throws IOException, InterruptedException {
        String exe = AppConfig.getInstance().getTqslPath();
        if (exe == null || exe.isBlank()) exe = "tqsl";
        List<String> cmd = new ArrayList<>();
        cmd.add(exe);
        Collections.addAll(cmd, args);
        log.info("tqsl: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(false);
        Process p = pb.start();
        String stdout = readAll(p.getInputStream());
        String stderr = readAll(p.getErrorStream());
        boolean finished = p.waitFor(120, TimeUnit.SECONDS);
        if (!finished) { p.destroyForcibly(); return new TqslResult(-1, stdout, "tqsl timed out"); }
        int code = p.exitValue();
        log.info("tqsl exit={} stdout.len={} stderr.len={}", code, stdout.length(), stderr.length());
        return new TqslResult(code, stdout, stderr);
    }

    private static String readAll(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = r.readLine()) != null) { sb.append(line).append('\n'); }
        }
        return sb.toString();
    }

    private static String requireStationLocation() {
        String loc = AppConfig.getInstance().getLotwStationLocation();
        if (loc == null || loc.isBlank())
            throw new IllegalStateException(
                "LoTW station location is not configured. Set it via the Setup dialog first " +
                "(the name must match one you've already defined inside tqsl).");
        return loc;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        int n = haystack.length(), m = needle.length();
        for (int i = 0; i <= n - m; i++) {
            if (haystack.regionMatches(true, i, needle, 0, m)) return i;
        }
        return -1;
    }
}
