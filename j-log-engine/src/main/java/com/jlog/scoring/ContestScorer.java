package com.jlog.scoring;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.MultiplierLists;
import com.jlog.util.CallsignRegion;
import com.jlog.util.Maidenhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure, UI-free per-QSO contest point scoring — the engine-side extraction of
 * {@code ContestLogController.computeQsoPoints} (scoring refactor, stage 2).
 *
 * <p>All inputs are explicit: the {@link ContestPlugin}, the {@link QsoRecord}
 * (received-exchange values read via {@link RecordFields}, never the UI), and a
 * {@link StationContext} (own call/grid/QTH gathered by the caller). No
 * AppConfig, no DAO, no JavaFX — so it is unit-testable and reusable.
 *
 * <p>Aggregate multiplier/score still lives in the controller (stage 4).
 */
public final class ContestScorer {
    private ContestScorer() {}
    private static final Logger log = LoggerFactory.getLogger(ContestScorer.class);

    /** Points for one QSO. Mirrors the controller dispatch exactly. */
    public static int points(ContestPlugin plugin, QsoRecord q, StationContext ctx) {
        String mode = q.getMode() != null ? q.getMode() : "";
        String band = q.getBand() != null ? q.getBand() : "";
        var rules = plugin.getScoringRules();
        if (rules != null && "state_prov_country".equals(rules.getMultiplierType())) return cq160Points(q, ctx);
        if (rules != null && "all_asian".equals(rules.getMultiplierType()))          return allAsianPoints(q, ctx);
        if (rules != null && "russian_dx".equals(rules.getMultiplierType()))         return russianDxPoints(q, ctx);
        if (rules != null && "sac".equals(rules.getMultiplierType()))                return sacPoints(q, ctx);
        if (rules != null && "ari_dx".equals(rules.getMultiplierType()))             return ariDxPoints(q, ctx);
        if (rules != null && "wag".equals(rules.getMultiplierType()))                return wagPoints(q, ctx);
        if (rules != null && "oceania_dx".equals(rules.getMultiplierType()))         return oceaniaDxPoints(q, ctx);
        if (rules != null && "qso_party".equals(rules.getMultiplierType()))          return qsoPartyPoints(plugin, q, ctx);
        if (rules != null && "wpx_prefix".equals(rules.getMultiplierType()))         return cqWpxPoints(q, ctx);
        if (rules != null && "zone_country_state".equals(rules.getMultiplierType())) return cqWwRttyPoints(q, ctx);
        if (rules != null && "zone_country".equals(rules.getMultiplierType()))       return cqWwPoints(q, ctx);

        if (rules != null && rules.isRookieRoundupScoring()) {
            String yearField = fieldPresent(plugin, "year_rcvd", "chk_rcvd", "check_rcvd");
            String yearStr   = RecordFields.value(plugin, q, yearField);
            if (yearStr != null && yearStr.trim().matches("[0-9]{1,2}")) {
                int yy    = Integer.parseInt(yearStr.trim());
                int curYy = LocalDateTime.now(ZoneOffset.UTC).getYear() % 100;
                int delta = ((curYy - yy) + 100) % 100;   // wraparound-safe
                return delta <= 3 ? 2 : 1;
            }
            return 1;
        }
        if (rules != null && rules.getPointsByRegionPair() != null && !rules.getPointsByRegionPair().isEmpty()) {
            return plugin.pointsFor(regionTag(ctx.ownCallsign()), regionTag(q.getCallsign()), mode);
        }
        if (rules != null && rules.getDistanceScoring() != null) {
            var ds = rules.getDistanceScoring();
            String theirId = ds.getTheirGridField() != null ? ds.getTheirGridField() : "grid_rcvd";
            String theirGrid = RecordFields.value(plugin, q, theirId);
            String ownGrid = ds.getOwnGridField() != null ? RecordFields.value(plugin, q, ds.getOwnGridField()) : null;
            if (ownGrid == null || ownGrid.isBlank()) ownGrid = ctx.ownGrid();
            double km = Maidenhead.distanceKm(ownGrid, theirGrid);
            if (km < 0) return 0;
            if (km < ds.getMinKm()) km = ds.getMinKm();
            String f = ds.getFormula() == null ? "" : ds.getFormula();
            if ("km_x_bandfactor".equals(f)) {
                int bf = 1;
                if (ds.getBandFactor() != null)
                    for (var e : ds.getBandFactor().entrySet())
                        if (e.getKey().equalsIgnoreCase(band)) { bf = e.getValue(); break; }
                return (int) Math.round(km) * bf;
            }
            if ("one_plus_ceil_km_div".equals(f)) {
                double div = ds.getDivisorKm() > 0 ? ds.getDivisorKm() : 500;
                int dist = (int) Math.ceil(km / div);
                if (dist < ds.getMinDistancePoints()) dist = ds.getMinDistancePoints();
                return ds.getBasePoints() + dist;
            }
            if ("one_plus_floor_km_div".equals(f)) {
                double div = ds.getDivisorKm() > 0 ? ds.getDivisorKm() : 3000;
                int dist = (int) Math.floor(km / div);
                if (dist < ds.getMinDistancePoints()) dist = ds.getMinDistancePoints();
                return ds.getBasePoints() + dist;
            }
            warnUnknownDistanceFormula(plugin, f);
            return ds.getBasePoints();
        }
        if (rules != null && ((rules.getPointsByBand() != null && !rules.getPointsByBand().isEmpty())
                || (rules.getPointsByBandClass() != null && !rules.getPointsByBandClass().isEmpty()))) {
            return plugin.pointsForBand(band, mode);
        }
        return plugin.pointsForMode(mode);
    }

    // ---- CQ family -----------------------------------------------------
    private static int cqWwPoints(QsoRecord q, StationContext ctx) {
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(ctx.ownCallsign());
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)               return 1;
        if (me.id().equals(them.id()))                return 0;
        if (!me.continent().equals(them.continent())) return 3;
        return "NA".equals(me.continent()) ? 2 : 1;
    }

    private static int cqWwRttyPoints(QsoRecord q, StationContext ctx) {
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(ctx.ownCallsign());
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)               return 1;
        if (me.id().equals(them.id()))                return 1;
        if (!me.continent().equals(them.continent())) return 3;
        return 2;
    }

    private static int cqWpxPoints(QsoRecord q, StationContext ctx) {
        String band = q.getBand() == null ? "" : q.getBand();
        boolean lf = band.equals("40m") || band.equals("80m") || band.equals("160m");
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(ctx.ownCallsign());
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)        return lf ? 2 : 1;
        if (me.id().equals(them.id()))         return 1;
        if (!me.continent().equals(them.continent())) return lf ? 6 : 3;
        if ("NA".equals(me.continent()) && "NA".equals(them.continent())) return lf ? 4 : 2;
        return lf ? 2 : 1;
    }

    private static int cq160Points(QsoRecord q, StationContext ctx) {
        if (DxccResolver.isMaritimeOrAir(q.getCallsign())) return 5;
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(ctx.ownCallsign());
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)               return 5;
        if (me.id().equals(them.id()))                return 2;
        if (!me.continent().equals(them.continent())) return 10;
        return 5;
    }

    // ---- entrant-asymmetric --------------------------------------------
    private static int allAsianPoints(QsoRecord q, StationContext ctx) {
        String band = q.getBand() != null ? q.getBand() : "";
        int aPts  = switch (band) { case "160m" -> 3; case "80m","10m" -> 2; default -> 1; };
        int naPts = switch (band) { case "160m" -> 9; case "80m","10m" -> 6; default -> 3; };
        String myCall = ctx.ownCallsign();
        boolean meAsian   = AsianEntities.isAsian(myCall);
        String  them      = q.getCallsign();
        boolean themMM    = DxccResolver.isMaritimeOrAir(them);
        boolean themAsian = themMM || AsianEntities.isAsian(them);
        if (meAsian) {
            if (!themMM) {
                String me = DxccResolver.getInstance().entityOf(myCall);
                String te = DxccResolver.getInstance().entityOf(them);
                if (me != null && me.equals(te)) return 0;
            }
            return themAsian ? aPts : naPts;
        }
        return themAsian ? aPts : 0;
    }

    private static int russianDxPoints(QsoRecord q, StationContext ctx) {
        String them = q.getCallsign();
        if (DxccResolver.isMaritimeOrAir(them)) return 5;
        String myCall = ctx.ownCallsign();
        DxccResolver R = DxccResolver.getInstance();
        boolean meRu   = RussianDx.isRussian(myCall);
        boolean themRu = RussianDx.isRussian(them);
        DxccResolver.Entity meE   = R.resolve(myCall);
        DxccResolver.Entity themE = R.resolve(them);
        String meCont   = meRu   ? RussianDx.russianContinent(myCall) : (meE   != null ? meE.continent()   : null);
        String themCont = themRu ? RussianDx.russianContinent(them)   : (themE != null ? themE.continent() : null);
        boolean sameCont = meCont != null && meCont.equals(themCont);
        if (meRu) {
            if (themRu) return sameCont ? 2 : 5;
            return sameCont ? 3 : 5;
        }
        if (themRu) return 10;
        if (meE != null && themE != null && meE.id().equals(themE.id())) return 2;
        return sameCont ? 3 : 5;
    }

    private static int sacPoints(QsoRecord q, StationContext ctx) {
        String them   = q.getCallsign();
        String myCall = ctx.ownCallsign();
        boolean meScand   = Scandinavian.isScandinavian(myCall);
        boolean themScand = Scandinavian.isScandinavian(them);
        DxccResolver R = DxccResolver.getInstance();
        if (meScand) {
            if (themScand) return 0;
            DxccResolver.Entity te = R.resolve(them);
            return (te != null && "EU".equals(te.continent())) ? 2 : 3;
        }
        if (!themScand) return 0;
        DxccResolver.Entity me = R.resolve(myCall);
        if (me != null && "EU".equals(me.continent())) return 1;
        String band = q.getBand() == null ? "" : q.getBand();
        return (band.equals("80m") || band.equals("40m")) ? 3 : 1;
    }

    private static int ariDxPoints(QsoRecord q, StationContext ctx) {
        String them = q.getCallsign();
        String myCall = ctx.ownCallsign();
        DxccResolver R = DxccResolver.getInstance();
        DxccResolver.Entity meE   = R.resolve(myCall);
        DxccResolver.Entity themE = R.resolve(them);
        if (meE != null && themE != null && meE.id().equals(themE.id())) return 0;
        if (AriDx.isItalian(them)) return 10;
        if (meE == null || themE == null) return 1;
        return meE.continent().equals(themE.continent()) ? 1 : 3;
    }

    private static int wagPoints(QsoRecord q, StationContext ctx) {
        String them = q.getCallsign();
        String myCall = ctx.ownCallsign();
        boolean meGer   = Wag.isGerman(myCall);
        boolean themGer = Wag.isGerman(them);
        if (!meGer) return themGer ? 3 : 0;
        if (themGer) return 1;
        DxccResolver.Entity themE = DxccResolver.getInstance().resolve(them);
        if (themE == null) return 3;
        return "EU".equals(themE.continent()) ? 3 : 5;
    }

    private static int oceaniaDxPoints(QsoRecord q, StationContext ctx) {
        int bp = OceaniaDx.bandPoints(q.getBand());
        if (bp == 0) return 0;
        String myCall = ctx.ownCallsign();
        if (OceaniaDx.isOceania(myCall)) return bp;
        return OceaniaDx.isOceania(q.getCallsign()) ? bp : 0;
    }

    // ---- qso party -----------------------------------------------------
    private static int qsoPartyPoints(ContestPlugin plugin, QsoRecord q, StationContext ctx) {
        ContestPlugin.QsoPartyConfig c = qpCfg(plugin);
        var rules = plugin.getScoringRules();
        boolean merge = c != null && c.isMergeRttyDigital();
        String mc = QsoParty.modeClass(q.getMode(), merge, c != null && c.isMergeCwDigital());
        int base = rules.getPointsPerQso() > 0 ? rules.getPointsPerQso() : 1;
        if (c != null && c.getPointsByModeClass() != null && c.getPointsByModeClass().containsKey(mc))
            base = c.getPointsByModeClass().get(mc);
        Set<String> counties = qpCounties(plugin, c);
        Set<String> grids = c == null ? Set.of() : qpUpper(c.getInStateGrids());
        Set<String> clubs = c == null ? Set.of() : qpUpper(c.getClubMultCalls());
        boolean meIn = QsoParty.isCounty(ctx.sentQth(), counties,
                c == null ? 0 : c.getCountyCodeLen(), c != null && c.isCountyByExclusion());
        boolean themIn = qpWorkedInState(q, c, counties, grids, clubs);
        boolean allQ = c != null && c.isPointsAllQsos();
        if (!allQ && !meIn && !themIn) return 0;
        if (!meIn && c != null && c.getPointsByModeClassOut() != null && c.getPointsByModeClassOut().containsKey(mc))
            base = c.getPointsByModeClassOut().get(mc);
        if (c != null && (c.getPtsWorkedInState() != 0 || c.getPtsWorkedOutState() != 0))
            base = themIn ? c.getPtsWorkedInState() : c.getPtsWorkedOutState();
        if (c != null && (c.getPtsInToIn() != 0 || c.getPtsInToOut() != 0 || c.getPtsOutToIn() != 0)) {
            base = meIn ? (themIn ? c.getPtsInToIn() : c.getPtsInToOut()) : c.getPtsOutToIn();
        }
        if (c != null && c.getQsoPointCalls() != null) {
            Integer fp = c.getQsoPointCalls().get(QsoParty.baseCall(q.getCallsign()));
            if (fp != null) base = fp;
        }
        if (!meIn && c != null && c.getBonusPointCalls() != null) {
            String call = q.getCallsign() == null ? "" : q.getCallsign().trim().toUpperCase();
            Integer b = c.getBonusPointCalls().get(call);
            if (b != null) base += b;
        }
        if (c != null && c.getRareQsoMultiplier() > 1 && c.getRareCounties() != null) {
            String rc = q.getContestField1() == null ? "" : q.getContestField1().trim().toUpperCase();
            if (qpUpper(c.getRareCounties()).contains(rc)) base *= c.getRareQsoMultiplier();
        }
        return base;
    }

    private static ContestPlugin.QsoPartyConfig qpCfg(ContestPlugin plugin) {
        return plugin.getScoringRules() == null ? null : plugin.getScoringRules().getQsoParty();
    }

    private static Set<String> qpUpper(List<String> in) {
        Set<String> s = new HashSet<>();
        if (in != null) for (String v : in) if (v != null) s.add(v.trim().toUpperCase());
        return s;
    }

    private static Set<String> qpCounties(ContestPlugin plugin, ContestPlugin.QsoPartyConfig c) {
        if (c != null && c.getInStateCounties() != null && !c.getInStateCounties().isEmpty())
            return qpUpper(c.getInStateCounties());
        return qpUpper(MultiplierLists.load(plugin.getMultiplierList()));
    }

    private static boolean qpWorkedInState(QsoRecord q, ContestPlugin.QsoPartyConfig c,
                                           Set<String> counties, Set<String> grids, Set<String> clubs) {
        if (QsoParty.callIn(q.getCallsign(), clubs)) return true;
        String mc = QsoParty.modeClass(q.getMode());
        String r  = q.getContestField1();
        if ("DG".equals(mc) && c != null && c.getFt8GridDivisor() > 0) {
            String g = QsoParty.grid4(r);
            return g != null && grids.contains(g);
        }
        return QsoParty.isCounty(r, counties, c == null ? 0 : c.getCountyCodeLen(),
                c != null && c.isCountyByExclusion());
    }

    // ---- dupe ----------------------------------------------------------
    /**
     * Whether this candidate QSO duplicates a prior one under the plugin's dupe
     * rule. Pure over the candidate + the contest's prior QSOs for the SAME
     * callsign (the caller fetches them, e.g. {@code ContestQsoDao.findByCallsign});
     * priors already flagged dupe are ignored. Mirrors the controller dispatch
     * order: contest-wide → rover-aware → per-band-grid → per-mode → field-day
     * mode-class → qso_party → (default) band+mode.
     */
    public static boolean isDupe(ContestPlugin plugin, QsoRecord cand, List<QsoRecord> priorForCall) {
        if (priorForCall == null || priorForCall.isEmpty()) return false;
        final String band = nz(cand.getBand());
        java.util.List<QsoRecord> prior = new java.util.ArrayList<>();
        for (QsoRecord r : priorForCall) if (!r.isDupe()) prior.add(r);
        if (prior.isEmpty()) return false;

        if (plugin.isContestWideDupe()) return true;                         // any prior with this call

        if (plugin.isRoverAwareDupe() && isRover(cand.getCallsign())) {
            String g = nz(RecordFields.value(plugin, cand, gridFieldCand(plugin)));
            return anyMatch(prior, r -> band.equals(nz(r.getBand()))
                    && g.equals(nz(RecordFields.value(plugin, r, gridFieldPrior(plugin)))));
        }
        if (plugin.isRoverAwareDupe()) {
            return anyMatch(prior, r -> band.equals(nz(r.getBand())));        // non-rover: call+band
        }
        if (plugin.isPerBandGridDupe()) {
            String g = nz(RecordFields.value(plugin, cand, gridFieldCand(plugin)));
            return anyMatch(prior, r -> band.equals(nz(r.getBand()))
                    && g.equals(nz(RecordFields.value(plugin, r, gridFieldPrior(plugin)))));
        }
        if (plugin.isPerModeMultipliers()) {
            String mode = nz(cand.getMode());
            return anyMatch(prior, r -> mode.equals(nz(r.getMode())));        // call+mode, band-agnostic
        }
        if (plugin.isFieldDayModeDupe()) {
            String cls = fdModeClass(cand.getMode());
            return anyMatch(prior, r -> band.equals(nz(r.getBand())) && cls.equals(fdModeClass(r.getMode())));
        }
        var rules = plugin.getScoringRules();
        if (rules != null && "qso_party".equals(rules.getMultiplierType())) {
            var qpc = rules.getQsoParty();
            boolean merge = qpc != null && qpc.isMergeRttyDigital();
            boolean mrgCw = qpc != null && qpc.isMergeCwDigital();
            String cls = QsoParty.modeClass(cand.getMode(), merge, mrgCw);
            String qth = nz(RecordFields.value(plugin, cand, "state_prov_rcvd")).trim().toUpperCase();
            return anyMatch(prior, r -> band.equals(nz(r.getBand()))
                    && cls.equals(QsoParty.modeClass(r.getMode(), merge, mrgCw))
                    && qth.equals(nz(r.getContestField1()).trim().toUpperCase()));
        }
        String mode = nz(cand.getMode());
        return anyMatch(prior, r -> band.equals(nz(r.getBand())) && mode.equals(nz(r.getMode())));
    }

    private static boolean isRover(String call) {
        if (call == null) return false;
        String c = call.trim().toUpperCase();
        return c.endsWith("/R") || c.endsWith("/ROVER");
    }
    private static String fdModeClass(String mode) {
        if (mode == null) return "DG";
        String m = mode.trim().toUpperCase();
        if (m.equals("CW")) return "CW";
        if (m.equals("SSB") || m.equals("USB") || m.equals("LSB") || m.equals("FM")
                || m.equals("AM") || m.equals("PHONE") || m.equals("DV") || m.equals("VOICE")) return "PH";
        return "DG";
    }
    /** Candidate's grid is read from grid_rcvd/gridsquare_rcvd; priors are compared on the
     *  multiplier-model field (the original "multColumn") — identical for real band-grid
     *  contests, which set multiplierModel.field = grid_rcvd. */
    private static String gridFieldCand(ContestPlugin plugin) {
        return fieldPresent(plugin, "grid_rcvd", "gridsquare_rcvd");
    }
    private static String gridFieldPrior(ContestPlugin plugin) {
        return plugin.getMultiplierModel() != null ? plugin.getMultiplierModel().getField()
                                                    : gridFieldCand(plugin);
    }
    private static boolean anyMatch(java.util.List<QsoRecord> rs, java.util.function.Predicate<QsoRecord> p) {
        for (QsoRecord r : rs) if (p.test(r)) return true;
        return false;
    }
    private static String nz(String s) { return s == null ? "" : s; }

    // ---- helpers -------------------------------------------------------
    private static String regionTag(String callsign) {
        return switch (CallsignRegion.classify(callsign)) {
            case US     -> "US";
            case CANADA -> "CA";
            case DX     -> "DX";
        };
    }

    /** First id among {@code ids} that is a declared entry field of the plugin (else null). */
    private static String fieldPresent(ContestPlugin plugin, String... ids) {
        if (plugin == null || plugin.getEntryFields() == null) return null;
        for (String id : ids)
            for (ContestPlugin.FieldDef fd : plugin.getEntryFields())
                if (id.equals(fd.getId())) return id;
        return null;
    }

    private static final Set<String> WARNED_UNKNOWN_FORMULAS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static void warnUnknownDistanceFormula(ContestPlugin plugin, String formula) {
        String id = plugin != null ? plugin.getContestId() : "?";
        if (WARNED_UNKNOWN_FORMULAS.add(id + ":" + (formula == null ? "" : formula))) {
            log.warn("Unknown distanceScoring.formula '{}' in plugin '{}' — using basePoints. "
                   + "Known: km_x_bandfactor, one_plus_ceil_km_div, one_plus_floor_km_div.", formula, id);
        }
    }
}
