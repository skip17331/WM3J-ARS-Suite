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
 * <p>Phrase fragments are sourced from {@link PhrasePool} (bundled JSON), so
 * each turn template draws from a much larger vocabulary than a handful of
 * fixed strings. With ~200 fragments across categories, hundreds of distinct
 * QSO realisations are possible per difficulty.
 *
 * <p>The legacy {@link #generate(Difficulty)} single-string method still works:
 * it concatenates all turns into one string for backward compatibility with the
 * receive-only QSO trainer mode and the existing test suite.
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

    // --- Hardcoded fallbacks (used only if the JSON pool fails to load) -----
    private static final List<String> CALL_PREFIXES = List.of(
            "K", "W", "N", "AA", "KB", "KE", "WB", "VE", "G", "DL", "F", "JA", "VK", "ZL", "EA", "OH", "OZ", "SM"
    );
    private static final List<String> FALLBACK_NAMES   = List.of("JOHN", "MIKE", "DAVE");
    private static final List<String> FALLBACK_QTHS    = List.of("DENVER CO", "TOKYO", "LONDON");
    private static final List<String> FALLBACK_RIGS    = List.of("FT-991", "K3", "IC-7300");
    private static final List<String> FALLBACK_WEATHER = List.of("SUNNY", "CLOUDY", "RAINY");

    private final Random random;
    private final PhrasePool pool;

    public QsoGenerator() { this(new Random(), PhrasePool.loadDefault()); }
    public QsoGenerator(Random random) { this(random, PhrasePool.loadDefault()); }
    public QsoGenerator(Random random, PhrasePool pool) {
        this.random = random;
        this.pool = pool;
    }

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
        String dxName = pickName();
        String dxQth  = pickQth();
        String you    = user.callsign();

        String cq   = template(pickCq(), dx, you);
        String back = template(pickCallback(), dx, you);

        String dxBlock = joinSegments(
                you + " DE " + dx,
                pickGreeting(),
                resolve(pickSignalReport()),
                resolve(pickNameIntro(), dxName),
                resolve(pickQthIntro(), dxQth),
                pickHwCheck()
        ) + " " + you + " DE " + dx + " K";

        String userBlock = joinSegments(
                dx + " DE " + you,
                "R FB " + dxName + " UR RST 599",
                "NAME " + user.name() + " QTH " + user.qth(),
                pickSignoff() + " " + dx + " DE " + you + " SK"
        );

        List<Turn> turns = new ArrayList<>(4);
        turns.add(new Turn(Speaker.DX,   cq));
        turns.add(new Turn(Speaker.USER, back));
        turns.add(new Turn(Speaker.DX,   dxBlock));
        turns.add(new Turn(Speaker.USER, userBlock));
        return turns;
    }

    private List<Turn> casualTurns(UserStation user) {
        String dx     = randomCallsign();
        String dxName = pickName();
        String dxQth  = pickQth();
        String dxRig  = pickRig();
        String dxAnt  = pickAntenna();
        String dxWx   = pickWeather();
        int    dxTemp = random.nextInt(35) - 5;
        String you    = user.callsign();

        String cq   = template(pickCq(), dx, you);
        String back = template(pickCallback(), dx, you);

        String dxBlock = joinSegments(
                you + " DE " + dx,
                pickGreeting(),
                resolve(pickSignalReport()),
                resolve(pickNameIntro(), dxName),
                resolve(pickQthIntro(), dxQth),
                resolve(pickRigIntro(), dxRig) + " " + dxAnt,
                resolveWx(pickWxIntro(), dxWx) + " " + resolveTemp(pickTempIntro(), dxTemp),
                pickHwCheck()
        ) + " " + you + " DE " + dx + " KN";

        String userBlock = joinSegments(
                dx + " DE " + you,
                "R FB " + dxName + " TNX FER NICE QSO UR RST 599",
                "NAME HR " + user.name() + " QTH " + user.qth(),
                "RIG IS IC-7300 ANT IS DIPOLE",
                pickSignoff() + " " + dx + " DE " + you + " SK"
        );

        List<Turn> turns = new ArrayList<>(4);
        turns.add(new Turn(Speaker.DX,   cq));
        turns.add(new Turn(Speaker.USER, back));
        turns.add(new Turn(Speaker.DX,   dxBlock));
        turns.add(new Turn(Speaker.USER, userBlock));
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

    // --- pool lookups ---------------------------------------------------------

    private String pickCq()        { return pool.pick("cq_openers",     random, "CQ CQ CQ DE {dx} {dx} K"); }
    private String pickCallback()  { return pool.pick("callbacks",      random, "{dx} DE {you} K"); }
    private String pickGreeting()  { return pool.pick("greetings",      random, "GE OM TNX FER CALL"); }
    private String pickSignalReport(){ return pool.pick("signal_reports", random, "UR RST {rst}"); }
    private String pickNameIntro() { return pool.pick("name_intros",    random, "NAME HR {name}"); }
    private String pickQthIntro()  { return pool.pick("qth_intros",     random, "QTH HR {qth}"); }
    private String pickRigIntro()  { return pool.pick("rig_intros",     random, "RIG HR {rig}"); }
    private String pickAntenna()   { return pool.pick("antennas",       random, "ANT IS DIPOLE"); }
    private String pickWxIntro()   { return pool.pick("weather_intros", random, "WX HR {wx}"); }
    private String pickTempIntro() { return pool.pick("temp_intros",    random, "TEMP {temp} C"); }
    private String pickHwCheck()   { return pool.pick("hw_checks",      random, "HW CPY?"); }
    private String pickSignoff()   { return pool.pick("signoffs_73",    random, "73 GL"); }

    private String pickName()      { return pool.has("names")    ? pool.pick("names",   random, "OP")       : pick(FALLBACK_NAMES); }
    private String pickQth()       { return pool.has("qths")     ? pool.pick("qths",    random, "USA")      : pick(FALLBACK_QTHS); }
    private String pickRig()       { return pool.has("rigs")     ? pool.pick("rigs",    random, "K3")       : pick(FALLBACK_RIGS); }
    private String pickWeather()   { return pool.has("weather_adj") ? pool.pick("weather_adj", random, "CLEAR") : pick(FALLBACK_WEATHER); }

    // --- template / token resolution ------------------------------------------

    private String template(String tmpl, String dx, String you) {
        return tmpl.replace("{dx}", dx).replace("{you}", you);
    }

    /** Resolve free tokens like {rst} for templates that need a random value. */
    private String resolve(String tmpl) {
        return tmpl.replace("{rst}", randomRst());
    }

    private String resolve(String tmpl, String value) {
        // Generic single-value replacement — fills whichever token is present.
        return tmpl
                .replace("{name}", value)
                .replace("{qth}",  value)
                .replace("{rig}",  value);
    }

    private String resolveWx(String tmpl, String wx) {
        return tmpl.replace("{wx}", wx);
    }

    private String resolveTemp(String tmpl, int temp) {
        return tmpl.replace("{temp}", String.valueOf(temp));
    }

    /** Joins segments with the CW segment separator "=" (BT prosign in audio). */
    private static String joinSegments(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append(" = ");
            sb.append(segments[i]);
        }
        return sb.toString();
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
