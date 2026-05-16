package com.jlog.scoring;

/**
 * ARI International DX Contest helpers. An "Italian station" for ARI
 * scoring is one whose DXCC entity is Italy (id 248 — mainland, Sicily
 * IT9, IN3, IV3, IX1, …) or Sardinia (id 225 — IS0). Per the rules I
 * and IS0 are NOT country multipliers (only their provinces are), and a
 * QSO with any Italian station is worth 10 points. Classification reuses
 * the shared {@link DxccResolver}; the resolver's coarse table folds the
 * rare IM0/IW0 (Sardinia) prefixes into mainland Italy (248) — benign
 * here since 248 is still "Italian" for both the 10-point rule and the
 * country-multiplier exclusion. Claimed/running — the ARI committee
 * re-adjudicates.
 */
public final class AriDx {

    private AriDx() {}

    /** DXCC entity id of Italy (mainland + Sicily + northern call areas). */
    public static final String ITALY = "248";
    /** DXCC entity id of Sardinia (IS0). */
    public static final String SARDINIA = "225";

    /** True if the call is an Italian station (I or IS0) — scores 10 pts
     *  and carries a province (not a country) multiplier. */
    public static boolean isItalian(String rawCall) {
        String e = DxccResolver.getInstance().entityOf(rawCall);
        return ITALY.equals(e) || SARDINIA.equals(e);
    }
}
