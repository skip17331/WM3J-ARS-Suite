package com.hamradio.jhub;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the four upload service implementations and exposes them by id.
 * Used by the {@code /api/uploaders} servlet.
 */
public class LogUploaderRegistry {

    private static final Logger log = LoggerFactory.getLogger(LogUploaderRegistry.class);

    private static final Map<String, LogUploader> UPLOADERS = new LinkedHashMap<>();
    static {
        UPLOADERS.put("eqsl",     new EqslUploader());
        UPLOADERS.put("clublog",  new ClublogUploader());
        UPLOADERS.put("qrz",      new QrzLogbookUploader());
        UPLOADERS.put("hrdlog",   new HrdLogUploader());
    }

    public static LogUploader get(String id)        { return UPLOADERS.get(id); }
    public static java.util.Collection<LogUploader> all() { return UPLOADERS.values(); }

    /** /api/uploaders snapshot — no plaintext credentials. */
    public static List<UploaderStatus> snapshot() {
        int total = UploadStateDao.getInstance().totalQsos();
        List<UploaderStatus> out = new ArrayList<>(UPLOADERS.size());
        for (LogUploader u : UPLOADERS.values()) {
            UploaderStatus s = new UploaderStatus();
            s.serviceId   = u.serviceId();
            s.displayName = u.displayName();
            s.fields      = u.credentialFields();
            s.configured  = u.isConfigured();
            s.totalQsos   = total;
            s.uploaded    = UploadStateDao.getInstance().uploadedFor(u.serviceId());
            s.pending     = total < 0 ? -1 : Math.max(0, total - s.uploaded);
            out.add(s);
        }
        return out;
    }

    public static LogUploader.UploadResult upload(String id) {
        LogUploader u = UPLOADERS.get(id);
        if (u == null) {
            LogUploader.UploadResult r = new LogUploader.UploadResult();
            r.serviceId = id; r.success = false; r.message = "unknown service";
            r.timestamp = Instant.now().toString();
            return r;
        }
        log.info("Uploader {}: starting batch", u.serviceId());
        LogUploader.UploadResult r = u.upload();
        r.serviceId  = u.serviceId();
        r.timestamp  = Instant.now().toString();
        log.info("Uploader {}: {} ({} of {} qsos)",
            u.serviceId(), r.success ? "OK" : "FAILED", r.qsosUploaded, r.qsosAttempted);
        return r;
    }

    public static class UploaderStatus {
        public String   serviceId;
        public String   displayName;
        public String[] fields;
        public boolean  configured;
        public int      totalQsos;
        public int      uploaded;
        public int      pending;
    }

    // -----------------------------------------------------------
    // Shared HTTP helpers used by every uploader
    // -----------------------------------------------------------

    static String formEncode(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(),  StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(),
                                         StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    static String httpPostForm(String urlStr, Map<String, String> form) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        byte[] body = formEncode(form).getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(body); }
        int code = conn.getResponseCode();
        try (var in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            String resp = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": " + resp);
            return resp;
        }
    }

    static String httpPostMultipart(String urlStr,
                                     Map<String, String> formFields,
                                     String fileFieldName, String fileName, byte[] fileBytes) throws IOException {
        String boundary = "----jhub" + System.currentTimeMillis();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(120_000);
        try (OutputStream os = conn.getOutputStream()) {
            for (Map.Entry<String, String> e : formFields.entrySet()) {
                os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
                os.write((e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            os.write(("Content-Disposition: form-data; name=\"" + fileFieldName +
                      "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            os.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(fileBytes);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        try (var in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            String resp = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": " + resp);
            return resp;
        }
    }

    // ===========================================================
    // Concrete implementations
    //
    // Each one is small and self-contained — ~40 lines of HTTP
    // glue around the shared helpers above. Endpoints, field
    // names, and "OK" markers documented inline so the next
    // person to touch this file doesn't have to read four
    // separate API specs.
    // ===========================================================

    /**
     * eQSL.cc — uploads ADIF via POST to /ImportADIFForm.cfm with
     * eQSL_user / eQSL_pswd form fields. Response includes
     * "Result: 1 out of N records added" on success.
     */
    static class EqslUploader implements LogUploader {
        @Override public String serviceId()         { return "eqsl"; }
        @Override public String displayName()       { return "eQSL.cc"; }
        @Override public String[] credentialFields(){ return new String[]{"user","pass"}; }
        @Override public boolean isConfigured() {
            JsonObject c = CredentialStore.getInstance().get("eqsl");
            return c.has("user") && c.has("pass")
                && !c.get("user").getAsString().isBlank() && !c.get("pass").getAsString().isBlank();
        }
        @Override public UploadResult upload() {
            UploadResult r = new UploadResult(); r.serviceId = "eqsl";
            if (!isConfigured()) { r.message = "credentials not set"; return r; }
            JsonObject creds = CredentialStore.getInstance().get("eqsl");
            String user = creds.get("user").getAsString();
            String pass = creds.get("pass").getAsString();
            List<PendingQso> pending = LogUploader.fetchPendingAdif("eqsl");
            r.qsosAttempted = pending.size();
            if (pending.isEmpty()) { r.success = true; r.message = "no pending QSOs"; return r; }

            StringBuilder adif = new StringBuilder("<ADIF_VER:5>3.1.0 <PROGRAMID:5>j-hub <EOH>\n");
            for (PendingQso p : pending) adif.append(p.adif);

            try {
                Map<String,String> form = new LinkedHashMap<>();
                form.put("EQSL_USER", user);
                form.put("EQSL_PSWD", pass);
                String resp = httpPostMultipart(
                    "https://www.eqsl.cc/qslcard/ImportADIF.cfm",
                    form, "Filename", "j-hub.adi",
                    adif.toString().getBytes(StandardCharsets.UTF_8));
                // eQSL returns HTML; look for the success marker
                if (resp != null && resp.toLowerCase().contains("result: ")
                    && !resp.toLowerCase().contains("error")) {
                    List<Long> ids = new ArrayList<>();
                    for (PendingQso p : pending) ids.add(p.id);
                    LogUploader.markConfirmed("eqsl", ids);
                    r.qsosUploaded = pending.size();
                    r.success = true;
                    r.message = "OK";
                } else {
                    r.message = "eQSL response did not contain success marker";
                }
            } catch (Exception e) {
                r.message = "upload failed: " + e.getMessage();
            }
            return r;
        }
    }

    /**
     * Club Log — uploads ADIF via POST to https://clublog.org/putlogs.php with
     * email, password, callsign, and the ADIF file as form fields. Response
     * is short text; "OK" on success.
     */
    static class ClublogUploader implements LogUploader {
        @Override public String serviceId()         { return "clublog"; }
        @Override public String displayName()       { return "Club Log"; }
        @Override public String[] credentialFields(){ return new String[]{"email","pass","callsign"}; }
        @Override public boolean isConfigured() {
            JsonObject c = CredentialStore.getInstance().get("clublog");
            return c.has("email") && c.has("pass")
                && !c.get("email").getAsString().isBlank() && !c.get("pass").getAsString().isBlank();
        }
        @Override public UploadResult upload() {
            UploadResult r = new UploadResult(); r.serviceId = "clublog";
            if (!isConfigured()) { r.message = "credentials not set"; return r; }
            JsonObject creds = CredentialStore.getInstance().get("clublog");
            String email = creds.get("email").getAsString();
            String pass  = creds.get("pass").getAsString();
            String call  = creds.has("callsign") ? creds.get("callsign").getAsString()
                : ConfigManager.getInstance().getStation().callsign;
            List<PendingQso> pending = LogUploader.fetchPendingAdif("clublog");
            r.qsosAttempted = pending.size();
            if (pending.isEmpty()) { r.success = true; r.message = "no pending QSOs"; return r; }

            StringBuilder adif = new StringBuilder("<EOH>\n");
            for (PendingQso p : pending) adif.append(p.adif);
            try {
                Map<String,String> form = new LinkedHashMap<>();
                form.put("email",    email);
                form.put("password", pass);
                form.put("callsign", call);
                form.put("api",      "1");
                String resp = httpPostMultipart(
                    "https://clublog.org/putlogs.php", form,
                    "file", "j-hub.adi",
                    adif.toString().getBytes(StandardCharsets.UTF_8));
                if (resp != null && resp.trim().equalsIgnoreCase("OK")) {
                    List<Long> ids = new ArrayList<>();
                    for (PendingQso p : pending) ids.add(p.id);
                    LogUploader.markConfirmed("clublog", ids);
                    r.qsosUploaded = pending.size();
                    r.success = true;
                    r.message = "OK";
                } else {
                    r.message = "Club Log response: " + (resp == null ? "(empty)" : resp.trim());
                }
            } catch (Exception e) {
                r.message = "upload failed: " + e.getMessage();
            }
            return r;
        }
    }

    /**
     * QRZ.com Logbook — POST to https://logbook.qrz.com/api with key + ADIF.
     * Each QSO is uploaded individually because the API expects a single
     * record per request (returns RESULT=OK on success).
     */
    static class QrzLogbookUploader implements LogUploader {
        @Override public String serviceId()         { return "qrz"; }
        @Override public String displayName()       { return "QRZ.com Logbook"; }
        @Override public String[] credentialFields(){ return new String[]{"apiKey"}; }
        @Override public boolean isConfigured() {
            JsonObject c = CredentialStore.getInstance().get("qrz");
            return c.has("apiKey") && !c.get("apiKey").getAsString().isBlank();
        }
        @Override public UploadResult upload() {
            UploadResult r = new UploadResult(); r.serviceId = "qrz";
            if (!isConfigured()) { r.message = "API key not set"; return r; }
            String key = CredentialStore.getInstance().get("qrz").get("apiKey").getAsString();
            List<PendingQso> pending = LogUploader.fetchPendingAdif("qrz");
            r.qsosAttempted = pending.size();
            if (pending.isEmpty()) { r.success = true; r.message = "no pending QSOs"; return r; }

            List<Long> okIds = new ArrayList<>();
            int failures = 0;
            for (PendingQso p : pending) {
                try {
                    Map<String,String> form = new LinkedHashMap<>();
                    form.put("KEY",    key);
                    form.put("ACTION", "INSERT");
                    form.put("ADIF",   p.adif);
                    String resp = httpPostForm("https://logbook.qrz.com/api", form);
                    if (resp != null && resp.contains("RESULT=OK")) {
                        okIds.add(p.id);
                    } else if (resp != null && resp.toLowerCase().contains("duplicate")) {
                        // Already there — count as success and stop pestering it.
                        okIds.add(p.id);
                    } else {
                        failures++;
                        if (failures > 5) { r.message = "stopping after 5 failures: " + resp; break; }
                    }
                } catch (Exception e) {
                    failures++;
                    if (failures > 5) { r.message = "stopping after 5 failures: " + e.getMessage(); break; }
                }
            }
            if (!okIds.isEmpty()) LogUploader.markConfirmed("qrz", okIds);
            r.qsosUploaded = okIds.size();
            r.success = failures == 0 || !okIds.isEmpty();
            if (r.message.isEmpty()) {
                r.message = okIds.size() + " uploaded, " + failures + " failed";
            }
            return r;
        }
    }

    /**
     * HRDLog — POST to https://www.hrdlog.net/NewEntry.aspx with callsign,
     * UploadCode, and ADIF text. Response contains "OK" on success.
     */
    static class HrdLogUploader implements LogUploader {
        @Override public String serviceId()         { return "hrdlog"; }
        @Override public String displayName()       { return "HRDLog.net"; }
        @Override public String[] credentialFields(){ return new String[]{"callsign","uploadCode"}; }
        @Override public boolean isConfigured() {
            JsonObject c = CredentialStore.getInstance().get("hrdlog");
            return c.has("callsign") && c.has("uploadCode")
                && !c.get("callsign").getAsString().isBlank()
                && !c.get("uploadCode").getAsString().isBlank();
        }
        @Override public UploadResult upload() {
            UploadResult r = new UploadResult(); r.serviceId = "hrdlog";
            if (!isConfigured()) { r.message = "credentials not set"; return r; }
            JsonObject creds = CredentialStore.getInstance().get("hrdlog");
            String call = creds.get("callsign").getAsString();
            String code = creds.get("uploadCode").getAsString();
            List<PendingQso> pending = LogUploader.fetchPendingAdif("hrdlog");
            r.qsosAttempted = pending.size();
            if (pending.isEmpty()) { r.success = true; r.message = "no pending QSOs"; return r; }

            StringBuilder adif = new StringBuilder();
            for (PendingQso p : pending) adif.append(p.adif);
            try {
                Map<String,String> form = new LinkedHashMap<>();
                form.put("Callsign",   call);
                form.put("Code",       code);
                form.put("App",        "j-hub");
                form.put("ADIFData",   adif.toString());
                String resp = httpPostForm("https://www.hrdlog.net/NewEntries.aspx", form);
                if (resp != null && resp.toUpperCase().contains("OK")) {
                    List<Long> ids = new ArrayList<>();
                    for (PendingQso p : pending) ids.add(p.id);
                    LogUploader.markConfirmed("hrdlog", ids);
                    r.qsosUploaded = pending.size();
                    r.success = true;
                    r.message = "OK";
                } else {
                    r.message = "HRDLog response: " + (resp == null ? "(empty)" : resp);
                }
            } catch (Exception e) {
                r.message = "upload failed: " + e.getMessage();
            }
            return r;
        }
    }
}
