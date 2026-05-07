package com.morsetrainer.trainer.qso;

import java.util.List;
import java.util.Random;

/**
 * Generates realistic CW QSOs. The Difficulty enum controls vocabulary and
 * speed assumptions for downstream play. The output is uppercase, with
 * single-space word separators — ready to feed into MorsePlayer.
 *
 * Slow training QSOs use simple stock content; contest-style QSOs are short
 * and punchy with serial numbers. Real-world-flavoured fields (callsigns, RST,
 * names, QTHs, rigs, weather) are sampled from curated lists.
 */
public class QsoGenerator {

    public enum Difficulty { TRAINING, CASUAL, CONTEST }

    // --- Sample data (kept short; real users can extend via config later) ---
    private static final List<String> CALL_PREFIXES = List.of(
            "K", "W", "N", "AA", "KB", "KE", "WB", "VE", "G", "DL", "F", "JA", "VK", "ZL", "EA", "OH", "OZ", "SM"
    );
    private static final List<String> NAMES = List.of(
            "JOHN", "MIKE", "DAVE", "SARAH", "ANNA", "TOM", "BILL", "JIM", "ALEX", "CHRIS", "PAT", "LEE", "SAM"
    );
    private static final List<String> QTHS = List.of(
            "DENVER CO", "AUSTIN TX", "SEATTLE WA", "MUNICH", "TOKYO", "MELBOURNE",
            "TORONTO", "BERLIN", "PARIS", "LONDON", "STOCKHOLM", "HELSINKI"
    );
    private static final List<String> RIGS = List.of(
            "FT-991", "K3", "IC-7300", "FT-857", "TS-590", "KX3", "FTDX10", "QCX MINI"
    );
    private static final List<String> WEATHER = List.of(
            "SUNNY", "CLOUDY", "RAINY", "SNOWY", "FOGGY", "WINDY", "CLEAR", "WARM", "COLD"
    );

    private final Random random;

    public QsoGenerator() { this(new Random()); }
    public QsoGenerator(Random random) { this.random = random; }

    public String generate(Difficulty difficulty) {
        return switch (difficulty) {
            case CONTEST -> contest();
            case CASUAL  -> casual();
            case TRAINING-> training();
        };
    }

    private String randomCallsign() {
        String prefix = CALL_PREFIXES.get(random.nextInt(CALL_PREFIXES.size()));
        int number = random.nextInt(10);
        int suffixLen = 2 + random.nextInt(2);
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < suffixLen; i++) suffix.append((char) ('A' + random.nextInt(26)));
        return prefix + number + suffix;
    }

    private String pick(List<String> xs) { return xs.get(random.nextInt(xs.size())); }

    private String training() {
        String me = randomCallsign();
        String you = randomCallsign();
        return String.join(" ",
                "CQ CQ CQ DE", me, me, "K",
                me, "DE", you, "=",
                "GE OM TNX FER CALL UR RST 599 5NN",
                "NAME", pick(NAMES), "QTH", pick(QTHS),
                "HW CPY?",
                me, "DE", you, "K"
        );
    }

    private String casual() {
        String me = randomCallsign();
        String you = randomCallsign();
        return String.join(" ",
                "CQ DE", me, me, "PSE K",
                me, "DE", you, "=",
                "GM DR OM TNX FER NICE QSO UR RST", randomRst(), "=",
                "NAME HR", pick(NAMES), "QTH", pick(QTHS), "=",
                "RIG HR", pick(RIGS), "ANT IS DIPOLE 40M =",
                "WX HR", pick(WEATHER), "TEMP", String.valueOf(random.nextInt(35) - 5), "C =",
                "HW CPY?",
                me, "DE", you, "KN"
        );
    }

    private String contest() {
        String me = randomCallsign();
        String you = randomCallsign();
        int serial = 1 + random.nextInt(999);
        return String.join(" ",
                "TEST", me, "TEST",
                you, "5NN", String.format("%03d", serial),
                "TU", me
        );
    }

    private String randomRst() {
        int r = 4 + random.nextInt(2);
        int s = 7 + random.nextInt(3);
        int t = 8 + random.nextInt(2);
        return "" + r + s + t;
    }
}
