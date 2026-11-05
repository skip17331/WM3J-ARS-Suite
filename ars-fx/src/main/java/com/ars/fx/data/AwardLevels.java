package com.ars.fx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The endorsement levels the big awards are issued in, worked out from the same {@link Awards.Snapshot} as the awards
 * screen: DXCC Challenge, 5-Band DXCC, DXCC by mode, 5-Band WAS and VUCC per band. "Worked" counts every logged QSO;
 * "confirmed" only those you have ticked a QSL / LoTW / eQSL on - the sponsors credit confirmed contacts only.
 *
 * <p>The thresholds are the sponsors' published ones as of writing (check the current rules before you apply for an award).
 */
public final class AwardLevels {
    private AwardLevels() {}

    /** One line of the levels screen. */
    public record Level(String group, String name, int worked, int confirmed, int target, String note) {
        public double fraction(boolean confirmedOnly) { return target <= 0 ? 0 : Math.min(1.0, (double) (confirmedOnly ? confirmed : worked) / target); }
        public boolean done() { return target > 0 && confirmed >= target; }
    }

    /** The ten bands the DXCC Challenge counts (160 to 6 m, without 60 m). */
    public static final String[] CHALLENGE_BANDS = {"160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m"};
    /** The five bands of 5-Band DXCC and 5-Band WAS. */
    public static final String[] FIVE_BANDS = {"80m", "40m", "20m", "15m", "10m"};
    /** VUCC: the band and the number of grid squares asked for (satellite VUCC is not counted here). */
    public static final String[][] VUCC = {{"6m", "100"}, {"2m", "100"}, {"1.25m", "50"}, {"70cm", "50"}, {"33cm", "25"}, {"23cm", "25"}};

    private static String norm(String b) { return b == null ? "" : b.trim().toLowerCase(Locale.ROOT); }

    public static List<Level> compute(Awards.Snapshot s) {
        List<Level> out = new ArrayList<>();

        // DXCC Challenge: entity x band slots over the ten bands
        int cw = 0, cc = 0;
        for (Awards.Entity e : s.entities.values()) {
            for (String b : CHALLENGE_BANDS) {
                if (e.bandsWorked.contains(b)) cw++;
                if (e.bandsConfirmed.contains(b)) cc++;
            }
        }
        out.add(new Level("DXCC Challenge", "Entity x band slots, 160 m to 6 m (no 60 m)", cw, cc, 1000,
                "Counts entities as logged; deleted entities are not tracked."));

        // 5-Band DXCC: 100 confirmed entities on each of five bands
        int bandsDone = 0;
        for (String b : FIVE_BANDS) {
            int[] n = s.dxccByBand.getOrDefault(b, new int[]{0, 0});
            if (n[1] >= 100) bandsDone++;
            out.add(new Level("5-Band DXCC", "DXCC on " + b, n[0], n[1], 100, ""));
        }
        out.add(new Level("5-Band DXCC", "Bands completed (of 5)", bandsDone, bandsDone, 5, "5-Band DXCC needs all five."));

        // DXCC by mode
        out.add(new Level("DXCC by mode", "Mixed", s.dxcc.worked(), s.dxcc.confirmed(), 100, ""));
        for (String m : Awards.MODE_GROUPS) {
            int[] n = s.dxccByMode.getOrDefault(m, new int[]{0, 0});
            out.add(new Level("DXCC by mode", m, n[0], n[1], 100, ""));
        }

        // 5-Band WAS
        int wasDone = 0;
        for (String b : FIVE_BANDS) {
            Set<String> w = s.statesByBand.getOrDefault(b, Set.of()), c = s.statesConfirmedByBand.getOrDefault(b, Set.of());
            if (c.size() >= 50) wasDone++;
            out.add(new Level("5-Band WAS", "States on " + b, w.size(), c.size(), 50, ""));
        }
        out.add(new Level("5-Band WAS", "Bands completed (of 5)", wasDone, wasDone, 5, "5-Band WAS needs all five."));

        // VUCC: grid squares per VHF / UHF band
        for (String[] v : VUCC) {
            int w = 0, c = 0;
            for (Awards.RefStat g : s.gridStats.values()) {
                if (g.bands.stream().map(AwardLevels::norm).anyMatch(v[0]::equals)) w++;
                if (g.bandsConfirmed.stream().map(AwardLevels::norm).anyMatch(v[0]::equals)) c++;
            }
            out.add(new Level("VUCC", "Grid squares on " + v[0], w, c, Integer.parseInt(v[1]), ""));
        }
        return out;
    }
}
