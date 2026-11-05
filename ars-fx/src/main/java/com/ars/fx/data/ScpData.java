package com.ars.fx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Super Check Partial call list (the {@code MASTER.SCP} format: one callsign a line, {@code #} comments, a {@code !!Order} header)
 * and the two questions a contester asks it while a call is half-copied: which known calls contain what I have typed, and which known
 * calls are one keystroke away from it (a bust). Pure: no network, no files.
 */
public final class ScpData {
    public static final ScpData EMPTY = new ScpData(new String[0], "");

    private static final Pattern RELEASE = Pattern.compile("^#\\s*Release\\s+(\\S+)", Pattern.MULTILINE);

    private final String[] calls;                 // sorted, upper case, unique
    private final String release;

    private ScpData(String[] calls, String release) { this.calls = calls; this.release = release; }

    /** The calls in {@code text}; comment lines, the order header, blanks and anything that is not a callsign are skipped. */
    public static ScpData parse(String text) {
        if (text == null || text.isEmpty()) return EMPTY;
        TreeSet<String> set = new TreeSet<>();
        for (String line : text.split("\\R")) {
            String c = line.trim().toUpperCase(Locale.ROOT);
            if (c.isEmpty() || c.charAt(0) == '#' || c.charAt(0) == '!') continue;
            if (callLike(c)) set.add(c);
        }
        Matcher m = RELEASE.matcher(text);
        return new ScpData(set.toArray(new String[0]), m.find() ? m.group(1) : "");
    }

    private static boolean callLike(String c) {
        if (c.length() < 3 || c.length() > 14) return false;
        boolean digit = false;
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if (ch >= '0' && ch <= '9') digit = true;
            else if (!(ch >= 'A' && ch <= 'Z') && ch != '/') return false;
        }
        return digit;
    }

    public int size() { return calls.length; }
    /** The release stamp from the file header (like 2026.11.04), or "" if it has none. */
    public String release() { return release; }
    public boolean isEmpty() { return calls.length == 0; }

    public boolean contains(String call) { return call != null && java.util.Arrays.binarySearch(calls, call.trim().toUpperCase(Locale.ROOT)) >= 0; }

    /**
     * What to look up for a typed call: the call itself, without a portable prefix or suffix ({@code W1AW/4}, {@code OH/K1ABC},
     * {@code K1ABC/P}) - the longest part that has a digit. Typed text is upper-cased.
     */
    public static String core(String typed) {
        if (typed == null) return "";
        String t = typed.trim().toUpperCase(Locale.ROOT);
        if (t.indexOf('/') < 0) return t;
        String best = "";
        for (String part : t.split("/")) {
            boolean digit = part.chars().anyMatch(Character::isDigit);
            if (digit && part.length() > best.length()) best = part;
        }
        return best.isEmpty() ? t.replace("/", "") : best;
    }

    /** A page of matches: up to {@code max} calls, out of {@code total} that matched. */
    public record Result(List<String> calls, int total) {}

    /** Known calls containing {@code typed} (at least two characters): those starting with it first, then the rest, each in order. */
    public Result partial(String typed, int max) {
        String t = core(typed);
        if (t.length() < 2) return new Result(List.of(), 0);
        List<String> starts = new ArrayList<>(), inside = new ArrayList<>();
        for (String c : calls) {
            int i = c.indexOf(t);
            if (i == 0) starts.add(c); else if (i > 0) inside.add(c);
        }
        List<String> all = new ArrayList<>(starts); all.addAll(inside);
        int total = all.size();
        return new Result(new ArrayList<>(all.subList(0, Math.min(max, total))), total);
    }

    /** Known calls one keystroke from {@code typed} (a wrong, missing, extra or swapped character): the likely right call for a bust. Not the call itself. */
    public Result near(String typed, int max) {
        String t = core(typed);
        if (t.length() < 3) return new Result(List.of(), 0);
        List<String> out = new ArrayList<>();
        for (String c : calls) if (Math.abs(c.length() - t.length()) <= 1 && oneOff(c, t)) out.add(c);
        return new Result(new ArrayList<>(out.subList(0, Math.min(max, out.size()))), out.size());
    }

    /** True if {@code a} and {@code b} differ by exactly one substitution, insertion, deletion or adjacent swap. */
    static boolean oneOff(String a, String b) {
        if (a.equals(b)) return false;
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        if (la == lb) {
            int i = 0;
            while (i < la && a.charAt(i) == b.charAt(i)) i++;
            if (i == la) return false;
            if (a.regionMatches(i + 1, b, i + 1, la - i - 1)) return true;                       // one substituted
            return i + 1 < la && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i)
                    && a.regionMatches(i + 2, b, i + 2, la - i - 2);                              // two swapped
        }
        String s = la < lb ? a : b, l = la < lb ? b : a;                                          // one inserted or deleted
        int i = 0;
        while (i < s.length() && s.charAt(i) == l.charAt(i)) i++;
        return s.regionMatches(i, l, i + 1, s.length() - i);
    }
}
