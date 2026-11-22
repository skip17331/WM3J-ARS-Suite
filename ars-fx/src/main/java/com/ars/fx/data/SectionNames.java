package com.ars.fx.data;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The full name of an ARRL / RAC section or a US state / Canadian province from its code (IA -> Iowa, WWA -> Western Washington),
 * for the tooltip on the contest section grid. A code that is both (IA) is the same either way. No JavaFX, so it can be tested.
 */
public final class SectionNames {
    private SectionNames() {}

    private static final Map<String, String> NAMES = new HashMap<>();
    private static void add(String... pairs) { for (int i = 0; i + 1 < pairs.length; i += 2) NAMES.put(pairs[i], pairs[i + 1]); }
    static {
        // US states and Canadian provinces (the codes that are also sections are overwritten below with the section's own name where it differs)
        add("AL", "Alabama", "AK", "Alaska", "AZ", "Arizona", "AR", "Arkansas", "CA", "California", "CO", "Colorado", "CT", "Connecticut",
            "DE", "Delaware", "DC", "District of Columbia", "FL", "Florida", "GA", "Georgia", "HI", "Hawaii", "ID", "Idaho", "IL", "Illinois",
            "IN", "Indiana", "IA", "Iowa", "KS", "Kansas", "KY", "Kentucky", "LA", "Louisiana", "ME", "Maine", "MD", "Maryland",
            "MA", "Massachusetts", "MI", "Michigan", "MN", "Minnesota", "MS", "Mississippi", "MO", "Missouri", "MT", "Montana",
            "NE", "Nebraska", "NV", "Nevada", "NH", "New Hampshire", "NJ", "New Jersey", "NM", "New Mexico", "NY", "New York",
            "NC", "North Carolina", "ND", "North Dakota", "OH", "Ohio", "OK", "Oklahoma", "OR", "Oregon", "PA", "Pennsylvania",
            "RI", "Rhode Island", "SC", "South Carolina", "SD", "South Dakota", "TN", "Tennessee", "TX", "Texas", "UT", "Utah",
            "VT", "Vermont", "VA", "Virginia", "WA", "Washington", "WV", "West Virginia", "WI", "Wisconsin", "WY", "Wyoming",
            "PR", "Puerto Rico", "VI", "US Virgin Islands",
            "AB", "Alberta", "BC", "British Columbia", "MB", "Manitoba", "NB", "New Brunswick", "NL", "Newfoundland and Labrador",
            "NS", "Nova Scotia", "ON", "Ontario", "PE", "Prince Edward Island", "QC", "Quebec", "SK", "Saskatchewan",
            "NT", "Northwest Territories", "NU", "Nunavut", "YT", "Yukon");
        // ARRL and RAC sections
        add("CT", "Connecticut", "EMA", "Eastern Massachusetts", "WMA", "Western Massachusetts", "ENY", "Eastern New York",
            "NLI", "New York City - Long Island", "NNJ", "Northern New Jersey", "SNJ", "Southern New Jersey", "NNY", "Northern New York",
            "WNY", "Western New York", "EPA", "Eastern Pennsylvania", "WPA", "Western Pennsylvania", "MDC", "Maryland - DC",
            "NFL", "Northern Florida", "SFL", "Southern Florida", "WCF", "West Central Florida",
            "NTX", "North Texas", "STX", "South Texas", "WTX", "West Texas",
            "EB", "East Bay", "LAX", "Los Angeles", "ORG", "Orange", "SB", "Santa Barbara", "SCV", "Santa Clara Valley", "SDG", "San Diego",
            "SF", "San Francisco", "SJV", "San Joaquin Valley", "SV", "Sacramento Valley", "PAC", "Pacific",
            "EWA", "Eastern Washington", "WWA", "Western Washington",
            "ONE", "Ontario East", "ONN", "Ontario North", "ONS", "Ontario South", "GH", "Greater Toronto Area", "GTA", "Greater Toronto Area",
            "MAR", "Maritime", "TER", "Territories", "NWT", "Northwest Territories", "DX", "DX (outside the US and Canada)");
    }

    /** The full name for a section / state code, or null when it is not one this knows. */
    public static String nameOf(String code) {
        return code == null ? null : NAMES.get(code.trim().toUpperCase(Locale.ROOT));
    }
}
