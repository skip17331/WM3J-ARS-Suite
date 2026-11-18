package com.ars.fx.data;

import java.time.LocalDate;
import java.util.List;

/**
 * The Info page's "Your stats" tile (punch list 5-6): QSOs today, this month, this year and in all; DXCC worked and confirmed, and
 * entities new this month; confirmations by LoTW, eQSL and paper; and how many QSOs are still to go to LoTW and eQSL.
 */
public final class StationStats {
    private StationStats() {}

    public record Stats(int today, int thisMonth, int thisYear, int total,
                        int dxccWorked, int dxccConfirmed, int newEntitiesThisMonth,
                        int lotwConfirmed, int eqslConfirmed, int qslConfirmed,
                        int lotwPending, int eqslPending) {}

    /** From the whole log, one award pass over it, and the LoTW / eQSL upload queues. Pure. */
    public static Stats compute(List<JLogDb.QsoExport> rows, Awards.Snapshot snap, LocalDate today, int lotwPending, int eqslPending) {
        String day = today.toString(), month = day.substring(0, 7), year = day.substring(0, 4);
        int t = 0, m = 0, y = 0, lotw = 0, eqsl = 0, qsl = 0;
        for (JLogDb.QsoExport q : rows) {
            String d = q.datetimeUtc() == null ? "" : q.datetimeUtc();
            if (d.startsWith(day)) t++;
            if (d.startsWith(month)) m++;
            if (d.startsWith(year)) y++;
            JLogDb.Refs r = q.refs();
            if (r != null) { if (r.lotw()) lotw++; if (r.eqsl()) eqsl++; if (r.qsl()) qsl++; }
        }
        int fresh = 0;
        if (snap != null) for (Awards.Entity e : snap.entities.values()) if (e.firstDate != null && e.firstDate.startsWith(month)) fresh++;
        int worked = snap == null || snap.dxcc == null ? 0 : snap.dxcc.worked(), conf = snap == null || snap.dxcc == null ? 0 : snap.dxcc.confirmed();
        return new Stats(t, m, y, rows.size(), worked, conf, fresh, lotw, eqsl, qsl, Math.max(0, lotwPending), Math.max(0, eqslPending));
    }

    /** The same, read from the log now. Blocking: call off the FX thread. */
    public static Stats now() {
        List<JLogDb.QsoExport> rows = JLogDb.allForExport();
        return compute(rows, Awards.compute(rows), LocalDate.now(java.time.ZoneOffset.UTC),
                JLogDb.pendingUpload("lotw").size(), JLogDb.pendingUpload("eqsl").size());
    }
}
