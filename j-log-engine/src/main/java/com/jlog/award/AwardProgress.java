package com.jlog.award;

import java.util.*;

/** Result of evaluating an {@link AwardPlugin} against the QSO log. */
public class AwardProgress {

    public final AwardPlugin award;

    /** Upper-cased set of every distinct value pulled from the matchOn field of
     *  QSOs that passed the window / confirmation filters. */
    public final Set<String> workedValues;

    /** Intersection of workedValues with the target set. Only populated for
     *  set-match awards. */
    public final Set<String> matchedTargets;

    /** Intersection with bonus set. */
    public final Set<String> matchedBonus;

    /** Targets not yet worked — useful for "what's missing" views. */
    public final List<AwardPlugin.Target> missingTargets;

    public AwardProgress(AwardPlugin award, Set<String> worked,
                         Set<String> matchedTargets, Set<String> matchedBonus,
                         List<AwardPlugin.Target> missing) {
        this.award           = award;
        this.workedValues    = Collections.unmodifiableSet(worked);
        this.matchedTargets  = Collections.unmodifiableSet(matchedTargets);
        this.matchedBonus    = Collections.unmodifiableSet(matchedBonus);
        this.missingTargets  = Collections.unmodifiableList(missing);
    }

    /** How many "things" are counted toward the award — targets + bonus for
     *  set-match awards, distinct values for count-match. */
    public int count() {
        return award.isSetMatch()
            ? matchedTargets.size() + matchedBonus.size()
            : workedValues.size();
    }

    /** Total required for the top tier (or size of the target set). */
    public int totalRequired() {
        if (!award.isSetMatch()) {
            return award.getTiers() == null || award.getTiers().isEmpty()
                ? 0
                : award.getTiers().get(award.getTiers().size() - 1).getThreshold();
        }
        int n = award.getTargets().size();
        if (award.getBonus() != null) n += award.getBonus().size();
        return n;
    }

    /** Current tier — the highest whose threshold has been met. Null if none. */
    public AwardPlugin.Tier currentTier() {
        if (award.getTiers() == null) return null;
        AwardPlugin.Tier best = null;
        int c = count();
        for (AwardPlugin.Tier t : award.getTiers()) {
            if (c >= t.getThreshold()) best = t;
        }
        return best;
    }

    /** Progress as 0.0 – 1.0 toward the highest tier. */
    public double progressRatio() {
        int total = totalRequired();
        if (total <= 0) return 0.0;
        return Math.min(1.0, (double) count() / total);
    }
}
