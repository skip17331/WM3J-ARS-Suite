package com.ars.fx.net;

import com.jlog.db.ContestQsoDao.Change;
import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** A contest log held in memory, standing in for the database so two {@link NetNode}s can be run against each other in one test. */
final class MemStore implements NetStore {
    static final AtomicLong CLOCK = new AtomicLong(System.currentTimeMillis());      // strictly increasing, so no two changes tie

    final String station; volatile String contest;
    final Map<String, QsoRecord> live = new HashMap<>();
    final Map<String, Long> gone = new HashMap<>();
    final List<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();
    final AtomicInteger applied = new AtomicInteger(), remoteNotices = new AtomicInteger();

    MemStore(String station, String contest) { this.station = station; this.contest = contest; }

    // ---- what the app would do at this station ----
    synchronized QsoRecord log(String call, String band) { return log(call, band, LocalDateTime.of(2026, 11, 5, 16, 0)); }
    synchronized QsoRecord log(String call, String band, LocalDateTime when) {
        QsoRecord q = new QsoRecord(); q.setUid(java.util.UUID.randomUUID().toString()); q.setContestId(contest); q.setCallsign(call); q.setBand(band); q.setMode("CW");
        q.setDateTimeUtc(when); q.setOrigin(station); q.setUpdatedMs(CLOCK.incrementAndGet());
        live.put(q.getUid(), q);
        fire(Change.Kind.ADD, q.getUid());
        return q;
    }
    /** A QSO already in the log from before the network was up: stored without telling anyone. */
    synchronized QsoRecord seed(String call, String contestId) {
        QsoRecord q = new QsoRecord(); q.setUid(java.util.UUID.randomUUID().toString()); q.setContestId(contestId); q.setCallsign(call); q.setBand("20m"); q.setMode("CW");
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 5, 15, 0)); q.setOrigin(station); q.setUpdatedMs(CLOCK.incrementAndGet());
        live.put(q.getUid(), q);
        return q;
    }
    synchronized void edit(String uid, String notes) {
        QsoRecord q = live.get(uid); q.setNotes(notes); q.setUpdatedMs(CLOCK.incrementAndGet());
        fire(Change.Kind.EDIT, uid);
    }
    synchronized void erase(String uid) {
        QsoRecord q = live.remove(uid);
        gone.put(uid, CLOCK.incrementAndGet());
        fire(Change.Kind.DELETE, uid);
    }
    private void fire(Change.Kind k, String uid) { for (Consumer<Change> l : listeners) l.accept(new Change(k, uid, contest)); }

    synchronized List<String> calls() { List<String> l = new ArrayList<>(); for (QsoRecord q : live.values()) l.add(q.getCallsign()); java.util.Collections.sort(l); return l; }
    synchronized QsoRecord byCall(String call) { for (QsoRecord q : live.values()) if (q.getCallsign().equals(call)) return q; return null; }

    // ---- NetStore ----
    @Override public String contest() { return contest; }
    @Override public synchronized List<Meta> inventory(String c) {
        List<Meta> out = new ArrayList<>();
        for (QsoRecord q : live.values()) if (c.equals(q.getContestId())) out.add(new Meta(q.getUid(), q.getUpdatedMs(), q.getOrigin(), false));
        for (Map.Entry<String, Long> e : gone.entrySet()) out.add(new Meta(e.getKey(), e.getValue(), "", true));
        return out;
    }
    @Override public synchronized QsoRecord find(String uid) { return live.get(uid); }
    @Override public synchronized Meta metaOf(String uid) {
        QsoRecord q = live.get(uid);
        if (q != null) return new Meta(uid, q.getUpdatedMs(), q.getOrigin(), false);
        Long t = gone.get(uid);
        return t == null ? null : new Meta(uid, t, "", true);
    }
    @Override public synchronized void apply(QsoRecord q) { live.put(q.getUid(), q); gone.remove(q.getUid()); applied.incrementAndGet(); }
    @Override public synchronized void applyDelete(String uid, String c, long ts) { live.remove(uid); gone.put(uid, ts); applied.incrementAndGet(); }
    @Override public void onLocalChange(Consumer<Change> l) { listeners.add(l); }
    @Override public void remoteChanged(String c) { remoteNotices.incrementAndGet(); }
}
