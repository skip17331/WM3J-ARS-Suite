package com.ars.fx.data;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The radios, rotators and amplifiers Hamlib supports, read from the installed {@code rigctl -l} / {@code rotctl -l} /
 * {@code ampctl -l} so the list always matches the Hamlib that will run the daemon. Empty when Hamlib is not installed.
 */
public final class HamlibModels {
    private HamlibModels() {}

    /** One supported device: Hamlib model number, manufacturer and model name. */
    public record Model(int number, String mfg, String name) {
        /** "IC-7610 (#3078)" — what the dropdown shows. */
        public String label() { return name + "  (#" + number + ")"; }
    }

    private static final Map<String, List<Model>> CACHE = new ConcurrentHashMap<>();

    /** Models for {@code kind} ("rig", "rotor" or "amp"), sorted by manufacturer then name. Cached after the first call. */
    public static List<Model> list(String kind) {
        return CACHE.computeIfAbsent(kind, k -> {
            String tool = k.startsWith("rotor") ? "rotctl" : k.startsWith("amp") ? "ampctl" : "rigctl";
            String bin = DaemonManager.resolveBinary(tool);
            if (bin == null) return List.of();
            try {
                Process p = new ProcessBuilder(bin, "-l").redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor(10, TimeUnit.SECONDS);
                return parse(out);
            } catch (Exception e) { return List.of(); }
        });
    }

    /** Parse the fixed-width table printed by {@code *ctl -l}; the header line gives the column starts. */
    static List<Model> parse(String out) {
        List<Model> res = new ArrayList<>();
        int mfgAt = -1, modelAt = -1, verAt = -1;
        for (String line : out.split("\\R")) {
            if (mfgAt < 0) {
                int m = line.indexOf("Mfg"), mo = line.indexOf("Model"), v = line.indexOf("Version");
                if (line.contains("#") && m > 0 && mo > m && v > mo) { mfgAt = m; modelAt = mo; verAt = v; }
                continue;
            }
            if (line.length() <= modelAt) continue;
            try {
                int num = Integer.parseInt(line.substring(0, mfgAt).trim());
                String mfg = line.substring(mfgAt, modelAt).trim();
                String name = line.substring(modelAt, Math.min(verAt, line.length())).trim();
                if (!mfg.isEmpty() && !name.isEmpty()) res.add(new Model(num, mfg, name));
            } catch (NumberFormatException ignored) { }
        }
        res.sort(Comparator.comparing((Model m) -> m.mfg().toLowerCase()).thenComparing(m -> m.name().toLowerCase()));
        return List.copyOf(res);
    }

    /** The model with Hamlib number {@code number}, or null. */
    public static Model find(String kind, String number) {
        try {
            int n = Integer.parseInt(number.trim());
            for (Model m : list(kind)) if (m.number() == n) return m;
        } catch (Exception ignored) { }
        return null;
    }
}
