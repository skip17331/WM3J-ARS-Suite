package com.hamradio.jhub;

import java.util.List;

/**
 * Common interface for QSO upload services (eQSL, Club Log, QRZ Logbook,
 * HRDLog). Each implementation pulls credentials from CredentialStore
 * under its own service id and translates a list of ADIF QSO records
 * into the service's preferred upload format.
 *
 * <h3>Pending-QSO source</h3>
 * Implementations should fetch pending QSOs through {@link #fetchPendingAdif()}
 * — currently the j-log SQLite database read directly from
 * <code>~/.j-log/j-log.db</code>. A QSO is "pending" for service S when
 * the {@code uploads_<S>} table has no row marking it as confirmed.
 * After a successful upload, {@link #markConfirmed(java.util.List)}
 * inserts confirmation rows so the same QSOs aren't re-sent.
 */
public interface LogUploader {

    /** Stable identifier — also the credentialstore service id. */
    String serviceId();

    /** Human label for the UI ("eQSL.cc", "Club Log", …). */
    String displayName();

    /**
     * Field names this service requires in its credential JSON object
     * (e.g. {@code ["user", "pass"]} or {@code ["apiKey"]}).
     * The UI uses this to render the right input row per service.
     */
    String[] credentialFields();

    /**
     * True when credentials are present and structurally valid.
     * Doesn't actually call the remote service.
     */
    boolean isConfigured();

    /**
     * Push every pending QSO to the remote service. Implementations
     * batch-encode and POST in chunks if the service has a limit.
     * Errors are returned in the {@link UploadResult} — never thrown.
     */
    UploadResult upload();

    /** Result DTO returned by {@link #upload()}. */
    class UploadResult {
        public String  serviceId;
        public boolean success;
        public int     qsosAttempted;
        public int     qsosUploaded;
        public String  message = "";
        public String  timestamp;
    }

    /** Convenience: load the in-memory list of pending ADIF strings. */
    static List<PendingQso> fetchPendingAdif(String serviceId) {
        return UploadStateDao.getInstance().pendingFor(serviceId);
    }

    /** Convenience: mark these QSOs uploaded so they're skipped next time. */
    static void markConfirmed(String serviceId, List<Long> qsoIds) {
        UploadStateDao.getInstance().markConfirmed(serviceId, qsoIds);
    }

    /** Pending QSO row — id is the j-log primary key, adif is the rendered ADIF text. */
    class PendingQso {
        public final long id;
        public final String adif;
        public PendingQso(long id, String adif) { this.id = id; this.adif = adif; }
    }
}
