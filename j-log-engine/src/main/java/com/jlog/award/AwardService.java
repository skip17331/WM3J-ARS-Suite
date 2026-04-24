package com.jlog.award;

import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.util.CallsignRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Evaluates an {@link AwardPlugin} against the normal-log database and
 * returns an {@link AwardProgress}. The log is already stored locally so
 * this is purely read-only; every call refetches so the dashboard is
 * always in sync with the latest QSO.
 */
public class AwardService {

    private static final Logger log = LoggerFactory.getLogger(AwardService.class);
    private static final AwardService INSTANCE = new AwardService();
    public  static AwardService getInstance() { return INSTANCE; }
    private AwardService() {}

    public AwardProgress compute(AwardPlugin award) {
        List<QsoRecord> qsos;
        try { qsos = QsoDao.getInstance().fetchAll(); }
        catch (Exception e) {
            log.warn("award compute: fetchAll failed: {}", e.getMessage());
            qsos = List.of();
        }

        boolean confirmedOnly = award.getOptions() != null && award.getOptions().isConfirmedOnly();
        boolean baseCall      = award.getOptions() != null && award.getOptions().isMatchBaseCallsign();

        Set<String> worked = new HashSet<>();
        for (QsoRecord q : qsos) {
            if (!award.withinWindow(q.getDateTimeUtc())) continue;
            if (confirmedOnly && !q.isQslReceived()) continue;
            String v = extractValue(q, award.getMatchOn(), baseCall);
            if (v != null && !v.isBlank()) worked.add(v.toUpperCase().trim());
        }

        Set<String> matchedTargets = new LinkedHashSet<>();
        Set<String> matchedBonus   = new LinkedHashSet<>();
        List<AwardPlugin.Target> missing = new ArrayList<>();

        if (award.getTargets() != null) {
            for (AwardPlugin.Target t : award.getTargets()) {
                String id = t.getId() == null ? "" : t.getId().toUpperCase().trim();
                if (worked.contains(id)) matchedTargets.add(id);
                else missing.add(t);
            }
        }
        if (award.getBonus() != null) {
            for (AwardPlugin.Target t : award.getBonus()) {
                String id = t.getId() == null ? "" : t.getId().toUpperCase().trim();
                if (worked.contains(id)) matchedBonus.add(id);
            }
        }

        return new AwardProgress(award, worked, matchedTargets, matchedBonus, missing);
    }

    /** Pull the award's matchOn field from a QSO, applying optional callsign
     *  normalization. Returns null when the field isn't populated. */
    private static String extractValue(QsoRecord q, String matchOn, boolean baseCall) {
        if (matchOn == null) return null;
        switch (matchOn) {
            case "state":   return q.getState();
            case "country": return q.getCountry();
            case "callsign": {
                String c = q.getCallsign();
                if (baseCall && c != null && c.contains("/"))
                    c = CallsignRegion.normalise(c);   // strips portable / compound suffix
                return c;
            }
            case "prefix":       return CallsignRegion.wpxPrefix(q.getCallsign());
            case "dxccPrefix":   return CallsignRegion.dxccPrefix(q.getCallsign());
            case "continent":    return continentOf(q.getCountry());
            case "grid":         return q.getNotes(); // no dedicated grid column yet
            default: return null;
        }
    }

    /** Minimal country→continent mapping — good enough for WAC progress when
     *  the log's {@code country} field is populated by callsign lookup. */
    private static String continentOf(String country) {
        if (country == null) return null;
        String c = country.toLowerCase();
        if (c.contains("united states") || c.contains("canada") || c.contains("mexico")
            || c.contains("cuba") || c.contains("jamaica") || c.contains("bahamas")
            || c.contains("haiti") || c.contains("dominican") || c.contains("puerto rico")
            || c.contains("costa rica") || c.contains("panama") || c.contains("guatemala")
            || c.contains("nicaragua") || c.contains("honduras") || c.contains("salvador")
            || c.contains("belize")) return "NA";
        if (c.contains("argentina") || c.contains("brazil") || c.contains("chile")
            || c.contains("peru") || c.contains("colombia") || c.contains("venezuela")
            || c.contains("ecuador") || c.contains("bolivia") || c.contains("uruguay")
            || c.contains("paraguay") || c.contains("guyana") || c.contains("suriname"))
            return "SA";
        if (c.contains("england") || c.contains("scotland") || c.contains("wales")
            || c.contains("ireland") || c.contains("france") || c.contains("germany")
            || c.contains("italy") || c.contains("spain") || c.contains("portugal")
            || c.contains("netherlands") || c.contains("belgium") || c.contains("switzerland")
            || c.contains("austria") || c.contains("sweden") || c.contains("norway")
            || c.contains("finland") || c.contains("denmark") || c.contains("poland")
            || c.contains("czech") || c.contains("slovak") || c.contains("hungary")
            || c.contains("romania") || c.contains("bulgaria") || c.contains("greece")
            || c.contains("ukraine") || c.contains("belarus") || c.contains("russia")
            || c.contains("serbia") || c.contains("croatia") || c.contains("slovenia")
            || c.contains("estonia") || c.contains("latvia") || c.contains("lithuania")
            || c.contains("iceland") || c.contains("malta") || c.contains("cyprus")
            || c.contains("luxembourg")) return "EU";
        if (c.contains("egypt") || c.contains("morocco") || c.contains("algeria")
            || c.contains("tunisia") || c.contains("libya") || c.contains("south africa")
            || c.contains("kenya") || c.contains("nigeria") || c.contains("ghana")
            || c.contains("ethiopia") || c.contains("sudan") || c.contains("senegal"))
            return "AF";
        if (c.contains("japan") || c.contains("china") || c.contains("korea")
            || c.contains("taiwan") || c.contains("hong kong") || c.contains("vietnam")
            || c.contains("thailand") || c.contains("indonesia") || c.contains("philippines")
            || c.contains("malaysia") || c.contains("singapore") || c.contains("india")
            || c.contains("pakistan") || c.contains("bangladesh") || c.contains("sri lanka")
            || c.contains("iran") || c.contains("iraq") || c.contains("saudi")
            || c.contains("turkey") || c.contains("israel") || c.contains("uae")
            || c.contains("kazakhstan")) return "AS";
        if (c.contains("australia") || c.contains("new zealand") || c.contains("fiji")
            || c.contains("papua") || c.contains("samoa") || c.contains("tonga"))
            return "OC";
        if (c.contains("antarctic")) return "AN";
        return null;
    }
}
