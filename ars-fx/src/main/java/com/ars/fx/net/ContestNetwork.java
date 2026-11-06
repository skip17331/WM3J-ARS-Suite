package com.ars.fx.net;

import com.ars.fx.data.Bands;
import com.ars.fx.data.ContestState;
import com.ars.fx.data.DupeFlags;
import com.ars.fx.data.HubConfig;
import com.ars.fx.data.RigClient;
import com.jlog.db.ContestQsoDao;
import com.jlog.db.ContestQsoDao.Change;
import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScorer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The app's contest network: reads the settings (J-Hub &#9656; Data &#9656; Contest network), runs the {@link NetNode} for this station, and ties
 * it to the contest database. After QSOs arrive from another station it works the dupe flags and points out again from the merged log, with
 * this station's own position, and refreshes the score. Off until the operator picks Host or Join.
 */
public final class ContestNetwork {
    private ContestNetwork() {}

    public static final String KEY_MODE = "network.mode", KEY_HOST = "network.host", KEY_PORT = "network.port",
            KEY_KEY = "network.key", KEY_STATION = "network.station", KEY_OP = "network.op";
    public static final int DEFAULT_PORT = 12070;

    private static final ScheduledExecutorService WORK = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "contest-net-work"); t.setDaemon(true); return t; });
    private static final AtomicBoolean recomputeQueued = new AtomicBoolean(), applyQueued = new AtomicBoolean();
    private static volatile NetNode node;
    private static volatile String startError = "";
    private static volatile String lastContest;
    private static volatile boolean started;

    /** This station's name on the network: the setting, else the computer's name (upper case, 15 characters at most). */
    public static String station() {
        String s = HubConfig.get(KEY_STATION, "").trim();
        if (s.isEmpty()) { s = System.getenv("COMPUTERNAME"); if (s == null || s.isBlank()) s = System.getenv("HOSTNAME"); if (s == null || s.isBlank()) s = "ARS-SUITE"; }
        s = s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_\\-]", "");
        if (s.isEmpty()) s = "ARS-SUITE";
        return s.length() > 15 ? s.substring(0, 15) : s;
    }
    public static String operator() { String o = HubConfig.get(KEY_OP, "").trim(); return o.isEmpty() ? HubConfig.call() : o.toUpperCase(Locale.ROOT); }
    public static String mode() { String m = HubConfig.get(KEY_MODE, "Off").trim(); return m.equalsIgnoreCase("Host") ? "Host" : m.equalsIgnoreCase("Join") ? "Join" : "Off"; }
    public static NetNode node() { return node; }
    /** Why the network did not start, if it did not. */
    public static String startError() { return startError; }

    /** Call once at start-up. Records this station's name on every QSO it logs, and starts the network if the settings say so. */
    public static synchronized void init() {
        if (started) return;
        started = true;
        ContestQsoDao.getInstance().setStationId(station());
        HubConfig.addChangeListener(key -> { if (key != null && (key.startsWith("network.") || key.equals("*"))) scheduleApply(); });
        ContestState.addListener(() -> {                                        // a different contest opened: share that one's log now
            ContestPlugin p = ContestState.plugin();
            String id = p == null ? null : p.getContestId();
            if (id != null && !id.equals(lastContest)) { lastContest = id; NetNode n = node; if (n != null) WORK.execute(n::resync); }
        });
        apply();
    }

    /** Settings changed: restart the network once the operator has stopped typing. */
    private static void scheduleApply() { if (applyQueued.compareAndSet(false, true)) WORK.schedule(() -> { applyQueued.set(false); apply(); }, 900, TimeUnit.MILLISECONDS); }

    /** (Re)starts the network as the settings say. */
    public static synchronized void apply() {
        ContestQsoDao.getInstance().setStationId(station());
        NetNode old = node;
        if (old != null) { old.stop(); node = null; }
        startError = "";
        String mode = mode();
        if (mode.equals("Off")) return;
        String key = HubConfig.get(KEY_KEY, "");
        if (!NetAuth.keyOk(key)) { startError = "Set a shared key of at least " + NetAuth.MIN_KEY + " characters (the same on every station)."; return; }
        NetNode n = new NetNode(station(), operator(), key, new DbStore(), ContestNetwork::myStatus);
        try {
            int port = port();
            if (mode.equals("Host")) n.startHost(port);
            else {
                String host = HubConfig.get(KEY_HOST, "").trim();
                if (host.isEmpty()) { startError = "Enter the host station's address."; return; }
                n.startJoin(host, port);
            }
            node = n;
            lastContest = ContestState.plugin() == null ? null : ContestState.plugin().getContestId();
        } catch (Exception e) { startError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    }

    private static int port() { try { return Integer.parseInt(HubConfig.get(KEY_PORT, String.valueOf(DEFAULT_PORT)).trim()); } catch (NumberFormatException e) { return DEFAULT_PORT; } }

    /** What this station tells the others: its operator, contest, and the rig's band and mode. */
    public static NetStatus myStatus() {
        ContestPlugin p = ContestState.plugin();
        RigClient.State rig = RigClient.getInstance().last();
        boolean on = rig != null && rig.connected() && rig.freqHz() > 0;
        String band = on ? Bands.of(rig.freqHz()) : "";
        String mode = on && rig.mode() != null ? rig.mode().toUpperCase(Locale.ROOT) : "";
        mode = switch (mode) { case "USB", "LSB", "PKTUSB", "PKTLSB" -> "SSB"; case "CWR" -> "CW"; default -> mode; };
        return new NetStatus(station(), operator(), p == null ? null : p.getContestId(), band, mode, on ? rig.freqHz() : 0, 0, 0, ContestState.isRun());
    }

    // ---- the contest database as this station's log -------------------------------------------------------------------

    static final class DbStore implements NetStore {
        private final ContestQsoDao dao = ContestQsoDao.getInstance();
        @Override public String contest() { ContestPlugin p = ContestState.plugin(); return p == null ? null : p.getContestId(); }
        @Override public List<Meta> inventory(String c) throws Exception { return dao.inventory(c); }
        @Override public QsoRecord find(String uid) throws Exception { return dao.findByUid(uid); }
        @Override public Meta metaOf(String uid) throws Exception { return dao.metaOf(uid); }
        @Override public void apply(QsoRecord q) throws Exception {
            dao.applyReplica(q);
            try { com.ars.fx.data.N1mmBroadcast.forwarded(com.jlog.plugin.PluginLoader.getInstance().getById(q.getContestId()), q); } catch (RuntimeException ignored) { }   // N1MM+'s All Computers: pass it on
            try { com.ars.fx.data.CallHistory.learnAsync(com.jlog.plugin.PluginLoader.getInstance().getById(q.getContestId()), q); } catch (RuntimeException ignored) { }   // what another station logged is history too
        }
        @Override public void applyDelete(String uid, String c, long ts) throws Exception { dao.applyReplicaDelete(uid, c, ts); }
        @Override public void onLocalChange(Consumer<Change> l) { dao.addListener(l); }
        @Override public void remoteChanged(String c) { queueRecompute(); }
    }

    /** After QSOs arrived: once things have settled (a sync can deliver thousands), work the dupes and points out again and refresh the score. */
    private static void queueRecompute() {
        if (!recomputeQueued.compareAndSet(false, true)) return;
        WORK.schedule(() -> {
            recomputeQueued.set(false);
            try {
                ContestPlugin p = ContestState.plugin();
                if (p != null) recomputeDerived(p);
                ContestState.recompute();
            } catch (Exception e) { System.err.println("[net] could not refresh after a sync: " + e.getMessage()); }
        }, 400, TimeUnit.MILLISECONDS);
    }

    /**
     * Dupe flags and points are not copied between stations: they depend on the whole log (which QSO of a station came first) and on this
     * station's own position. So after a change from outside they are worked out again here, from the merged log.
     */
    static void recomputeDerived(ContestPlugin plugin) throws Exception {
        ContestQsoDao dao = ContestQsoDao.getInstance();
        List<QsoRecord> all = dao.fetchByContest(plugin.getContestId());
        Map<Long, Integer> pts = new HashMap<>(); Map<Long, Boolean> dup = new HashMap<>();
        for (QsoRecord q : all) { pts.put(q.getId(), q.getPoints()); dup.put(q.getId(), q.isDupe()); }
        DupeFlags.recompute(plugin, all);
        var ctx = ContestState.ctx();
        for (QsoRecord q : all) {
            int p = ContestScorer.points(plugin, q, ctx);
            if (p != pts.get(q.getId()) || q.isDupe() != dup.get(q.getId())) dao.updateDerived(q.getId(), p, q.isDupe());
        }
    }
}
