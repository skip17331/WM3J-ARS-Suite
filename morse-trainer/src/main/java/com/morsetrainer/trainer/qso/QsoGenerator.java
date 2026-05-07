package com.morsetrainer.trainer.qso;

import com.morsetrainer.core.AppConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Generates realistic CW QSOs as a sequence of alternating turns between a
 * simulated DX station and the user. The Difficulty enum controls vocabulary
 * and exchange shape.
 *
 * Output is uppercase, with single-space word separators — ready to feed into
 * MorsePlayer (DX turns) or to display as a sending target (user turns).
 *
 * The legacy {@link #generate(Difficulty)} single-string method still works:
 * it concatenates all turns into one string for backward compatibility with
 * the receive-only QSO trainer mode and the existing test suite.
 */
public class QsoGenerator {

    public enum Difficulty { TRAINING, CASUAL, CONTEST }

    public enum Speaker { DX, USER }

    /** A single side of an exchange — either the DX station or the user. */
    public record Turn(Speaker speaker, String text) {}

    /** Identifying fields the user "sends" when it's their turn. */
    public record UserStation(String callsign, String name, String qth) {
        public static UserStation fromConfig(AppConfig cfg) {
            return new UserStation(
                    blankToDefault(cfg.userCallsign,  "WM3J"),
                    blankToDefault(cfg.userName,      "OP"),
                    blankToDefault(cfg.userQth,       "USA"));
        }
        private static String blankToDefault(String v, String d) {
            return v == null || v.isBlank() ? d : v.trim().toUpperCase();
        }
    }

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

    // --- Legacy single-string API ----------------------------------------------

    /** Returns the QSO as one concatenated string (receive-only mode). */
    public String generate(Difficulty difficulty) {
        return generateTurns(difficulty, defaultUser()).stream()
                .map(Turn::text)
                .collect(Collectors.joining(" "));
    }

    /** Default user-station identity when none is supplied (uses AppConfig). */
    private UserStation defaultUser() {
        return UserStation.fromConfig(AppConfig.get());
    }

    // --- Turn-based API for live send-and-receive ------------------------------

    public List<Turn> generateTurns(Difficulty difficulty, UserStation user) {
        return switch (difficulty) {
            case CONTEST -> contestTurns(user);
            case CASUAL  -> casualTurns(user);
            case TRAINING-> trainingTurns(user);
        };
    }

    private List<Turn> trainingTurns(UserStation user) {
        String dx     = randomCallsign();
        String dxName = pick(NAMES);
        String dxQth  = pick(QTHS);
        String you    = user.callsign();

        List<Turn> turns = new ArrayList<>(4);
        turns.add(new Turn(Speaker.DX,
                String.join(" ", "CQ CQ CQ DE", dx, dx, "K")));
        turns.add(new Turn(Speaker.USER,
                String.join(" ", dx, "DE", you, "K")));
        turns.add(new Turn(Speaker.DX, String.join(" ",
                you, "DE", dx, "=",
                "GE OM TNX FER CALL UR RST 599 5NN =",
                "NAME", dxName, "QTH", dxQth, "=",
                "HW CPY?",
                you, "DE", dx, "K")));
        turns.add(new Turn(Speaker.USER, String.join(" ",
                dx, "DE", you, "=",
                "R FB", dxName, "UR RST 599 =",
                "NAME", user.name(), "QTH", user.qth(), "=",
                "TNX FER QSO 73 GL =",
                dx, "DE", you, "SK")));
        return turns;
    }

    private List<Turn> casualTurns(UserStation user) {
        String dx     = randomCallsign();
        String dxName = pick(NAMES);
        String dxQth  = pick(QTHS);
        String dxRig  = pick(RIGS);
        String dxWx   = pick(WEATHER);
        int    dxTemp = random.nextInt(35) - 5;
        String you    = user.callsign();

        List<Turn> turns = new ArrayList<>(4);
        turns.add(new Turn(Speaker.DX,
                String.join(" ", "CQ DE", dx, dx, "PSE K")));
        turns.add(new Turn(Speaker.USER,
                String.join(" ", dx, "DE", you, you, "K")));
        turns.add(new Turn(Speaker.DX, String.join(" ",
                you, "DE", dx, "=",
                "GM DR OM TNX FER NICE CALL UR RST", randomRst(), "=",
                "NAME HR", dxName, "QTH", dxQth, "=",
                "RIG HR", dxRig, "ANT IS DIPOLE 40M =",
                "WX HR", dxWx, "TEMP", String.valueOf(dxTemp), "C =",
                "HW CPY?",
                you, "DE", dx, "KN")));
        turns.add(new Turn(Speaker.USER, String.join(" ",
                dx, "DE", you, "=",
                "R FB", dxName, "TNX FER NICE QSO UR RST 599 =",
                "NAME HR", user.name(), "QTH", user.qth(), "=",
                "RIG IS IC-7300 ANT IS DIPOLE =",
                "73 GL ES TNX AGN =",
                dx, "DE", you, "SK")));
        return turns;
    }

    private List<Turn> contestTurns(UserStation user) {
        String dx        = randomCallsign();
        int    dxSerial  = 1 + random.nextInt(999);
        int    youSerial = 1 + random.nextInt(999);
        String you       = user.callsign();

        List<Turn> turns = new ArrayList<>(5);
        turns.add(new Turn(Speaker.DX,   "TEST " + dx + " TEST"));
        turns.add(new Turn(Speaker.USER, you));
        turns.add(new Turn(Speaker.DX,
                you + " 5NN " + String.format("%03d", dxSerial)));
        turns.add(new Turn(Speaker.USER,
                "R 5NN " + String.format("%03d", youSerial)));
        turns.add(new Turn(Speaker.DX, "TU " + dx));
        return turns;
    }

    // --- helpers ---------------------------------------------------------------

    private String randomCallsign() {
        String prefix = CALL_PREFIXES.get(random.nextInt(CALL_PREFIXES.size()));
        int number = random.nextInt(10);
        int suffixLen = 2 + random.nextInt(2);
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < suffixLen; i++) suffix.append((char) ('A' + random.nextInt(26)));
        return prefix + number + suffix;
    }

    private String pick(List<String> xs) { return xs.get(random.nextInt(xs.size())); }

    private String randomRst() {
        int r = 4 + random.nextInt(2);
        int s = 7 + random.nextInt(3);
        int t = 8 + random.nextInt(2);
        return "" + r + s + t;
    }
}
