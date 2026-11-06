package com.ars.fx.net;

import com.jlog.model.QsoRecord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Who has logged how much: the QSOs, points and last-hour rate of each operator in the merged contest log. Pure. */
public final class OperatorStats {
    private OperatorStats() {}

    public record Row(String operator, int qsos, int points, int lastHour) {}

    /** One row per operator, most QSOs first. Duplicates are left out; a QSO with no operator is counted under "?". {@code nowUtc} fixes the last hour. */
    public static List<Row> compute(List<QsoRecord> qsos, LocalDateTime nowUtc) {
        Map<String, int[]> by = new LinkedHashMap<>();
        LocalDateTime since = nowUtc.minusHours(1);
        for (QsoRecord q : qsos) {
            if (q.isDupe()) continue;
            String op = q.getOperator() == null || q.getOperator().isBlank() ? "?" : q.getOperator().trim().toUpperCase();
            int[] n = by.computeIfAbsent(op, k -> new int[3]);
            n[0]++; n[1] += q.getPoints();
            LocalDateTime t = q.getDateTimeUtc();
            if (t != null && t.isAfter(since) && !t.isAfter(nowUtc)) n[2]++;
        }
        List<Row> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : by.entrySet()) out.add(new Row(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
        out.sort(Comparator.comparingInt(Row::qsos).reversed().thenComparing(Row::operator));
        return out;
    }

    public static List<Row> compute(List<QsoRecord> qsos) { return compute(qsos, LocalDateTime.now(ZoneOffset.UTC)); }

    /** "W1AW 120 (34/h) · N1ABC 80 (20/h)"; empty with fewer than two operators, when the line would say nothing a score does not. */
    public static String line(List<Row> rows) {
        if (rows.size() < 2) return "";
        StringBuilder sb = new StringBuilder();
        for (Row r : rows) sb.append(sb.length() == 0 ? "" : "   ·   ").append(r.operator()).append(' ').append(r.qsos()).append(" (").append(r.lastHour()).append("/h)");
        return sb.toString();
    }
}
