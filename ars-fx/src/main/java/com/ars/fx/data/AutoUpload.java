package com.ars.fx.data;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Setup > Data > LoTW / eQSL auto-upload. After a QSO is logged, each enabled service ({@link UploadConfig}) is sent the QSOs it has not had yet
 * ({@link JLogDb#pendingUpload}) and they are marked once the service accepts them. A failed upload leaves them pending, so the next logged QSO
 * (or {@link #afterLog} call) tries again. Runs on one background thread, never on the UI thread, and bursts of QSOs are folded into one pass.
 */
public final class AutoUpload {
    private AutoUpload() {}

    public static final String KEY = "data.lotwAutoUpload";
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "auto-upload"); t.setDaemon(true); return t; });
    private static final AtomicBoolean queued = new AtomicBoolean(false);
    private static volatile String lastMessage = "";

    /** The outcome of the most recent pass, for display ("" until one has run). */
    public static String lastMessage() { return lastMessage; }

    /** Call after a QSO has been logged. Does nothing when the setting is off. */
    public static void afterLog() {
        if (!HubConfig.getBool(KEY, false)) return;
        if (!queued.compareAndSet(false, true)) return;     // a pass is already waiting: it will pick this QSO up too
        POOL.execute(() -> { queued.set(false); runOnce(); });
    }

    private static void runOnce() {
        StringBuilder msg = new StringBuilder();
        for (String svc : new String[]{"lotw", "eqsl"}) {
            if (!UploadConfig.enabled(svc)) continue;
            List<JLogDb.Pending> pending = JLogDb.pendingUpload(svc);
            if (pending.isEmpty()) continue;
            List<JLogDb.QsoExport> rows = pending.stream().map(JLogDb.Pending::qso).toList();
            LogUpload.Result r;
            try { r = svc.equals("lotw") ? LogUpload.lotw(rows) : LogUpload.eqsl(rows); }
            catch (RuntimeException e) { r = new LogUpload.Result(false, 0, svc + " upload error: " + e.getMessage()); }
            if (r.ok()) JLogDb.markUploaded(svc, pending.stream().map(JLogDb.Pending::id).toList());
            if (msg.length() > 0) msg.append("  ");
            msg.append(svc.equals("lotw") ? "LoTW: " : "eQSL: ").append(r.ok() ? "sent " + rows.size() : r.message());
        }
        if (msg.length() > 0) { lastMessage = msg.toString(); System.err.println("[auto-upload] " + lastMessage); }
    }
}
