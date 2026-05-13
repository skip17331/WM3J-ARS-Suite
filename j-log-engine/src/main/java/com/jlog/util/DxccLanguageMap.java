package com.jlog.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Built-in DXCC → spoken-language mapping. The translator's
 * DXCC-driven window consults this table to decide which target
 * columns to show for a given callsign's home country.
 *
 * <p>Scope is deliberately narrow: only the four languages the
 * translator supports today (English, Spanish, German, Portuguese).
 * A DXCC entity that speaks several of those (Switzerland, Andorra,
 * Belgium, …) maps to multiple language codes and the window shows
 * one column per match. Entities outside the four languages fall
 * back to English so the operator always has something to read.
 *
 * <p>The mapping uses the DXCC entity name as published by the
 * ARRL and j-hub's <code>dxcc/prefixes.json</code> (e.g.
 * "United States", "Spain", "Germany", "Brazil") — passed through
 * {@link #normalise} to absorb common spelling variants
 * ("USA", "U.S.A.", "Deutschland", …) before lookup.
 */
public final class DxccLanguageMap {

    /** ISO 639-1 codes for the four supported languages. */
    public static final String EN = "en";
    public static final String ES = "es";
    public static final String DE = "de";
    public static final String PT = "pt";

    /** All four columns, in display order. */
    public static final List<String> ALL = List.of(EN, ES, DE, PT);

    private static final Map<String, Set<String>> MAP = build();

    private DxccLanguageMap() {}

    /**
     * Return the ordered set of language codes spoken in {@code entity}.
     * <p>Always includes {@link #EN} so the English column never
     * disappears. Returns {@code [EN]} when {@code entity} is null,
     * blank, or has no mapping.
     */
    public static Set<String> languagesFor(String entity) {
        if (entity == null) return Collections.singleton(EN);
        String key = normalise(entity);
        if (key.isEmpty()) return Collections.singleton(EN);
        Set<String> hit = MAP.get(key);
        if (hit == null || hit.isEmpty()) return Collections.singleton(EN);
        // Preserve declared order, but guarantee EN first.
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(EN);
        for (String code : hit) ordered.add(code);
        return Collections.unmodifiableSet(ordered);
    }

    /**
     * Lowercase, strip non-alphanumerics, fold common aliases. Lets
     * the table match "USA" / "U.S.A." / "United States" / "United
     * States of America" all to the same row.
     */
    public static String normalise(String entity) {
        if (entity == null) return "";
        String s = entity.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9 ]", "");
        s = s.replaceAll("\\s+", " ");
        switch (s) {
            case "usa":
            case "us":
            case "united states of america":
                return "united states";
            case "uk":
            case "u k":
            case "great britain":
            case "england":
            case "scotland":
            case "wales":
            case "northern ireland":
                return "united kingdom";
            case "deutschland":
                return "germany";
            case "espana":
            case "españa":
                return "spain";
            case "brasil":
                return "brazil";
            case "republica argentina":
                return "argentina";
            case "republic of korea":
            case "south korea":
                return "korea south";
            default:
                return s;
        }
    }

    // ---------------------------------------------------------------
    // Table
    // ---------------------------------------------------------------

    private static Map<String, Set<String>> build() {
        Map<String, Set<String>> m = new HashMap<>();

        // English-speaking (kept explicit so the rest of the code can
        // distinguish "mapped to English" from "fell off the table")
        add(m, "united states",  EN);
        add(m, "united kingdom", EN);
        add(m, "canada",         EN, "fr"); // English + French primary; fr not yet in DB so EN-only column shows
        add(m, "australia",      EN);
        add(m, "new zealand",    EN);
        add(m, "ireland",        EN);
        add(m, "south africa",   EN);
        add(m, "jamaica",        EN);
        add(m, "kenya",          EN);
        add(m, "nigeria",        EN);
        add(m, "india",          EN);
        add(m, "philippines",    EN);
        add(m, "singapore",      EN);

        // Spanish-speaking
        add(m, "spain",            ES);
        add(m, "mexico",           ES);
        add(m, "argentina",        ES);
        add(m, "chile",            ES);
        add(m, "colombia",         ES);
        add(m, "peru",             ES);
        add(m, "venezuela",        ES);
        add(m, "ecuador",          ES);
        add(m, "uruguay",          ES);
        add(m, "paraguay",         ES);
        add(m, "bolivia",          ES);
        add(m, "cuba",             ES);
        add(m, "dominican republic", ES);
        add(m, "puerto rico",      EN, ES);
        add(m, "guatemala",        ES);
        add(m, "honduras",         ES);
        add(m, "nicaragua",        ES);
        add(m, "costa rica",       ES);
        add(m, "panama",           ES);
        add(m, "el salvador",      ES);

        // German-speaking
        add(m, "germany",      DE);
        add(m, "austria",      DE);
        add(m, "liechtenstein", DE);

        // Portuguese-speaking
        add(m, "portugal",      PT);
        add(m, "brazil",        PT);
        add(m, "azores",        PT);
        add(m, "madeira",       PT);
        add(m, "angola",        PT);
        add(m, "mozambique",    PT);
        add(m, "cape verde",    PT);
        add(m, "sao tome and principe", PT);
        add(m, "guinea bissau", PT);
        add(m, "east timor",    PT);

        // Multi-language entities relevant to our four-language scope
        add(m, "switzerland",   DE);            // also fr/it but those columns don't exist
        add(m, "andorra",       ES);            // also fr/ca but ES is the closest supported
        add(m, "belgium",       DE);            // German-speaking minority; primary fr/nl absent
        add(m, "luxembourg",    DE);            // German + French + Luxembourgish
        add(m, "equatorial guinea", ES, PT);    // Spanish + Portuguese (Annobón) + French
        add(m, "macao",         PT);
        add(m, "gibraltar",     EN, ES);
        add(m, "bonaire",       EN);            // primary nl absent
        add(m, "ceuta and melilla", ES);

        return Collections.unmodifiableMap(m);
    }

    private static void add(Map<String, Set<String>> m, String key, String... codes) {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(codes));
        m.put(key, set);
    }
}
