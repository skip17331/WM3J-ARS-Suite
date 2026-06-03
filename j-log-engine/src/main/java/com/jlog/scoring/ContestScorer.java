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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    // ---- aggregate score / multipliers --------------------------------
    private static final List<String> TRACKED_MODES = List.of("CW", "Phone");

    /**
     * Aggregate score, multiplier count and worked-token collections for the
     * whole contest — the engine-side extraction of
     * {@code ContestLogController.updateStats} (scoring refactor, stage 4).
     *
     * <p>Pure over the inputs: {@code qsos} is the full contest log (dupes
     * included; each branch filters {@code isDupe()} exactly as the SQL did),
     * {@code multColumn} the {@code field1..field5} slot holding the multiplier
     * value, {@code contestBands} the plugin's band list (per-band model), and
     * {@code qtcPoints} the WAE QTC count. QSO points are the already-stored
     * {@link QsoRecord#getPoints()} summed over non-dupes — identical to the old
     * {@code totalPointsByContest} SQL — so callers that want a fresh total must
     * set points via {@link #points} before calling.
     *
     * <p>Dispatch order mirrors the controller exactly: score-is-points-only →
     * per-mode → state_prov_country → all_asian → russian_dx → sac → ari_dx →
     * oceania_dx → qso_party → wag → zone_country[_state] → grid_field → wae →
     * per-band model → (default) flat worked list.
     */
    public static ContestScore score(ContestPlugin plugin, List<QsoRecord> qsos,
                                      StationContext ctx, String multColumn,
                                      List<String> contestBands, int qtcPoints) {
        int count = 0;
        for (QsoRecord q : qsos) if (!q.isDupe()) count++;
        var rules = plugin.getScoringRules();

        // Field Day-style: final score = QSO points, no multiplier (bonuses off-log).
        if (rules != null && rules.isScoreIsPointsOnly()) {
            int total = sumPoints(qsos);
            List<String> worked = distinctField(qsos, multColumn);
            return ContestScore.sectionsOnly(count, total, worked.size(), total, worked);
        }

        if (plugin.isPerModeMultipliers()) {
            // Per-mode mults + per-mode point sums. Score = (P_cw + P_ph) × (M_cw + M_ph).
            Map<String, List<String>> workedByMode = new LinkedHashMap<>();
            int totalMults = 0, totalPoints = 0;
            for (String mode : TRACKED_MODES) {
                List<String> w = distinctFieldByMode(qsos, multColumn, mode);
                workedByMode.put(mode, w);
                totalMults  += w.size();
                totalPoints += sumPointsByMode(qsos, mode);
            }
            return ContestScore.perMode(count, totalPoints, totalMults, totalPoints * totalMults, workedByMode);
        }

        if (rules != null && "state_prov_country".equals(rules.getMultiplierType())) {
            // CQ WW 160m: one combined contest-wide mult set = US state | VE
            // province | DXCC country. W/VE = logged state/prov (field1); DX =
            // DXCC from callsign; MM/AM carry no mult.
            DxccResolver dxr = DxccResolver.getInstance();
            Set<String> mset = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String call = q.getCallsign();
                if (DxccResolver.isMaritimeOrAir(call)) continue;
                DxccResolver.Entity e = dxr.resolve(call);
                if (e == null) continue;
                String key;
                if ("291".equals(e.id()) || "1".equals(e.id())) {
                    String sp = q.getContestField1();
                    if (sp == null || sp.isBlank()) continue;
                    key = e.id() + ":" + sp.trim().toUpperCase();
                } else {
                    key = "DX:" + e.id();
                }
                mset.add(key);
            }
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, mset.size(), total * mset.size());
        }

        if (rules != null && "all_asian".equals(rules.getMultiplierType())) {
            // JARL All Asian DX: per-band mult is asymmetric by entrant. Asian
            // entrant → distinct DXCC per band (same-entity & MM excluded);
            // non-Asian entrant → distinct Asian WPX prefixes per band.
            String aaCall = ctx.ownCallsign();
            boolean meAsian = AsianEntities.isAsian(aaCall);
            String meEnt = DxccResolver.getInstance().entityOf(aaCall);
            Map<String, Set<String>> aaByBand = new LinkedHashMap<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String call = q.getCallsign();
                if (DxccResolver.isMaritimeOrAir(call)) continue;
                String tok;
                if (meAsian) {
                    String e = DxccResolver.getInstance().entityOf(call);
                    if (e == null) continue;
                    if (meEnt != null && meEnt.equals(e)) continue;
                    tok = e;
                } else {
                    if (!AsianEntities.isAsian(call)) continue;
                    tok = CallsignRegion.wpxPrefix(call);
                    if (tok == null || tok.isBlank()) continue;
                }
                aaByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
            }
            int mults = aaByBand.values().stream().mapToInt(Set::size).sum();
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, mults, total * mults);
        }

        if (rules != null && "russian_dx".equals(rules.getMultiplierType())) {
            // Russian DX: dual per-band mult = distinct oblast + distinct
            // DXCC/WAE country. Oblast from logged field1 for Russian stations;
            // UA2F/RI1FJ/RI1AN count as a separate country AND oblast. MM = none.
            Set<String> rdMult = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String call = q.getCallsign();
                if (DxccResolver.isMaritimeOrAir(call)) continue;
                rdMult.add(b + "|C|" + RussianDx.countryToken(call));
                if (RussianDx.isRussian(call)) {
                    String ct = RussianDx.countryToken(call);
                    String ob;
                    if ("RI1FJ".equals(ct) || "RI1AN".equals(ct) || "UA2F".equals(ct))
                        ob = ct;
                    else {
                        String f1 = q.getContestField1();
                        ob = f1 == null ? "" : f1.trim().toUpperCase();
                    }
                    if (!ob.isBlank()) rdMult.add(b + "|O|" + ob);
                }
            }
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, rdMult.size(), total * rdMult.size());
        }

        if (rules != null && "sac".equals(rules.getMultiplierType())) {
            // Scandinavian Activity: per-band mult asymmetric by entrant.
            // Scandinavian entrant → distinct DXCC per band; non-Scandinavian
            // entrant → distinct Scandinavian district tokens per band.
            String scCall = ctx.ownCallsign();
            boolean meScand = Scandinavian.isScandinavian(scCall);
            DxccResolver dxr = DxccResolver.getInstance();
            Map<String, Set<String>> scByBand = new LinkedHashMap<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String call = q.getCallsign();
                String tok;
                if (meScand) {
                    DxccResolver.Entity e = dxr.resolve(call);
                    if (e == null) continue;
                    tok = e.id();
                } else {
                    tok = Scandinavian.multToken(call);
                    if (tok == null) continue;
                }
                scByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
            }
            int mults = scByBand.values().stream().mapToInt(Set::size).sum();
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, mults, total * mults);
        }

        if (rules != null && "ari_dx".equals(rules.getMultiplierType())) {
            // ARI DX: one combined per-band mult set = Italian province (logged
            // field1) + DXCC for every non-Italian station. I (248) and IS0
            // (225) are never a country mult — only their province counts.
            DxccResolver dxr = DxccResolver.getInstance();
            Set<String> arMult = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String call = q.getCallsign();
                if (AriDx.isItalian(call)) {
                    String pr = q.getContestField1();
                    pr = pr == null ? "" : pr.trim().toUpperCase();
                    if (!pr.isBlank()) arMult.add(b + "|P|" + pr);
                } else {
                    String e = dxr.entityOf(call);
                    if (e == null) continue;
                    if (AriDx.ITALY.equals(e) || AriDx.SARDINIA.equals(e)) continue;
                    arMult.add(b + "|C|" + e);
                }
            }
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, arMult.size(), total * arMult.size());
        }

        if (rules != null && "oceania_dx".equals(rules.getMultiplierType())) {
            // Oceania DX: mult = distinct WPX prefixes, once PER BAND. Oceania
            // entrant counts every station's prefix; non-Oceania entrant counts
            // only Oceania stations' prefixes (non-Oc↔non-Oc = none, Rule 4b).
            String ocCall = ctx.ownCallsign();
            boolean meOc = OceaniaDx.isOceania(ocCall);
            Set<String> ocMult = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String call = q.getCallsign();
                if (!meOc && !OceaniaDx.isOceania(call)) continue;
                String pfx = CallsignRegion.wpxPrefix(call);
                if (pfx == null || pfx.isBlank()) continue;
                ocMult.add(b + "|" + pfx);
            }
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, ocMult.size(), total * ocMult.size());
        }

        if (rules != null && "qso_party".equals(rules.getMultiplierType())) {
            // Reusable QSO-party engine. Multiplier scope per_mode / per_band /
            // once. In-state entrant counts own counties + (config) states/prov
            // + DXCC + club calls; out-of-state entrant counts ONLY in-state
            // counties + club calls. Digital w/ grid divisor contributes grid
            // mults. Power multiplier is intentionally not applied.
            ContestPlugin.QsoPartyConfig c = qpCfg(plugin);
            Set<String> counties = qpCounties(plugin, c);
            Set<String> grids = c == null ? Set.of() : qpUpper(c.getInStateGrids());
            Set<String> clubs = c == null ? Set.of() : qpUpper(c.getClubMultCalls());
            String scope = c != null && c.getMultScope() != null ? c.getMultScope() : "once";
            boolean inCounties = c == null || c.isInStateCountsCounties();
            boolean ownState   = c != null && c.isInStateOwnStateMult();
            boolean inStates   = c != null && c.isInStateCountsStates();
            boolean dxEach     = c != null && c.isInStateCountsDxccEach();
            boolean noDx       = c != null && c.isInStateNoDxMult();
            boolean selfState  = c != null && c.isInStateSelfStateMult();
            boolean clubMemMult = c != null && c.isClubMemberMult();
            boolean merge      = c != null && c.isMergeRttyDigital();
            boolean mergeCw    = c != null && c.isMergeCwDigital();
            boolean gridCeil   = c != null && c.isGridDivisorCeil();
            boolean gridUncap  = c != null && c.isOutStateGridUncapped();
            int divisor = c == null ? 0 : c.getFt8GridDivisor();
            int outCap  = c == null ? 0 : c.getOutStateGridCap();
            String stateAbbr = c == null || c.getStateAbbr() == null ? "" : c.getStateAbbr().toUpperCase();
            int countyLen = c == null ? 0 : c.getCountyCodeLen();
            boolean byExcl = c != null && c.isCountyByExclusion();
            int areaPfx = c == null ? 0 : c.getAreaStatePrefixLen();
            Map<String,Integer> bonusMap = c == null || c.getBonusStations() == null
                    ? Map.of() : c.getBonusStations();
            Map<String,Integer> bonusOnce = c == null || c.getBonusStationsOnce() == null
                    ? Map.of() : c.getBonusStationsOnce();
            Map<String,Integer> bonusPerMode = c == null || c.getBonusStationsPerMode() == null
                    ? Map.of() : c.getBonusStationsPerMode();
            boolean multsAll = c != null && c.isMultsAllEntrants();
            boolean meIn = QsoParty.isCounty(ctx.sentQth(), counties, countyLen, byExcl);
            if (!meIn && c != null && c.getMultScopeOut() != null && !c.getMultScopeOut().isBlank())
                scope = c.getMultScopeOut();
            Set<String> mset = new HashSet<>();
            Set<String> allDg = new HashSet<>();
            Set<String> inDg  = new HashSet<>();
            Set<String> bonusSeen = new HashSet<>();
            Set<String> bonusOnceSeen = new HashSet<>();
            Set<String> bonusPerModeSeen = new HashSet<>();
            int bonusPts = 0;
            Set<String> rareSet  = c == null ? Set.of() : qpUpper(c.getRareCounties());
            Set<String> rareSeen = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b  = q.getBand() == null ? "" : q.getBand();
                String mc = QsoParty.modeClass(q.getMode(), merge, mergeCw);
                String call = q.getCallsign() == null ? "" : q.getCallsign().trim().toUpperCase();
                String R  = q.getContestField1() == null ? ""
                        : q.getContestField1().trim().toUpperCase();
                if (clubMemMult) {
                    if (R.chars().anyMatch(Character::isDigit))
                        mset.add("M|" + QsoParty.baseCall(call));
                    continue;
                }
                String sk = "per_mode".equals(scope) ? mc + "|"
                          : "per_band".equals(scope) ? b + "|"
                          : "per_band_mode".equals(scope) ? b + "|" + mc + "|" : "";
                if (!bonusMap.isEmpty()) {
                    String bc = QsoParty.baseCall(call);
                    Integer bp = bonusMap.get(bc);
                    if (bp != null && bonusSeen.add(bc + "|" + b + "|" + mc))
                        bonusPts += bp;
                }
                if (!bonusOnce.isEmpty()) {
                    String bc = QsoParty.baseCall(call);
                    Integer bp = bonusOnce.get(bc);
                    if (bp != null && bonusOnceSeen.add(bc))
                        bonusPts += bp;
                }
                if (!bonusPerMode.isEmpty()) {
                    String bc = QsoParty.baseCall(call);
                    Integer bp = bonusPerMode.get(bc);
                    if (bp != null && bonusPerModeSeen.add(bc + "|" + mc))
                        bonusPts += bp;
                }
                if (rareSet.contains(R)) rareSeen.add(R);
                boolean club = QsoParty.callIn(call, clubs);
                if ("DG".equals(mc) && divisor > 0) {
                    String g = QsoParty.grid4(R);
                    if (g != null) {
                        if (meIn) allDg.add(g);
                        else if (grids.contains(g)) inDg.add(g);
                    }
                    if (club) mset.add(sk + "K|" + call);
                    continue;
                }
                boolean isDx  = "DX".equals(regionTag(call)) || "DX".equals(R);
                boolean isCty = !isDx && QsoParty.isCounty(R, counties, countyLen, byExcl);
                if (meIn || multsAll) {
                    if (isCty) {
                        if (areaPfx > 0 && R.length() >= areaPfx)
                            mset.add(sk + "S|" + R.substring(0, areaPfx));
                        else if (ownState) mset.add(sk + "S|" + stateAbbr);
                        else if (inCounties) mset.add(sk + "C|" + R);
                    } else if (isDx) {
                        if (!noDx) {
                            String e = DxccResolver.getInstance().entityOf(call);
                            mset.add(sk + "X|" + (dxEach ? (e == null ? "DX" : e) : "DX"));
                        }
                    } else if (inStates && !R.isBlank() && !R.equals(stateAbbr)) {
                        mset.add(sk + "S|" + R);
                    }
                    if (selfState) mset.add(sk + "S|" + stateAbbr);
                    if (club) mset.add(sk + "K|" + call);
                } else {
                    if (isCty) mset.add(sk + "C|" + R);
                    if (club) mset.add(sk + "K|" + call);
                }
            }
            if (c != null && c.getSweepBonusThreshold() > 0
                    && rareSeen.size() >= c.getSweepBonusThreshold())
                bonusPts += c.getSweepBonusPoints();
            else if (c != null && c.getSweepBonusThreshold2() > 0
                    && rareSeen.size() >= c.getSweepBonusThreshold2())
                bonusPts += c.getSweepBonusPoints2();
            int qpMults = mset.size();
            if (meIn && divisor > 0)
                qpMults += gridCeil
                    ? (int) Math.ceil(allDg.size() / (double) divisor)
                    : allDg.size() / divisor;
            if (!meIn && (outCap > 0 || gridUncap))
                qpMults += gridUncap ? inDg.size()
                                     : Math.min(inDg.size(), outCap);
            if (c != null && c.getMultCap() > 0)
                qpMults = Math.min(qpMults, c.getMultCap());
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, qpMults, total * qpMults + bonusPts);
        }

        if (rules != null && "wag".equals(rules.getMultiplierType())) {
            // Worked All Germany: mult per band AND per mode-class. German
            // entrant → each DXCC/WAE area (WaeMultiplier token); non-German
            // entrant → German DOK first-letter district (logged field1).
            String wgCall = ctx.ownCallsign();
            boolean meGer = Wag.isGerman(wgCall);
            Set<String> wgMult = new HashSet<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String mc = Wag.modeClass(q.getMode());
                String call = q.getCallsign();
                if (meGer) {
                    String tok = WaeMultiplier.token(call);
                    if (tok != null) wgMult.add(b + "|" + mc + "|" + tok);
                } else {
                    if (!Wag.isGerman(call)) continue;
                    String d = Wag.dokDistrict(q.getContestField1());
                    if (d != null) wgMult.add(b + "|" + mc + "|D|" + d);
                }
            }
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, wgMult.size(), total * wgMult.size());
        }

        if (rules != null && rules.getMultiplierType() != null
                && rules.getMultiplierType().startsWith("zone_country")) {
            // CQ WW dual/triple mult, once PER BAND. Zone = field1; country =
            // DXCC from callsign; CQ WW RTTY adds US states + VE provinces
            // (field3). Paints the CQ zone map from the distinct zones worked.
            boolean withState = "zone_country_state".equals(rules.getMultiplierType());
            DxccResolver dxr = DxccResolver.getInstance();
            Map<String, Set<String>> zonesByBand = new LinkedHashMap<>();
            Map<String, Set<String>> ctrysByBand = new LinkedHashMap<>();
            Map<String, Set<String>> stateByBand = new LinkedHashMap<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String zone = q.getContestField1();
                if (zone != null && !zone.isBlank())
                    zonesByBand.computeIfAbsent(b, k -> new HashSet<>()).add(zone.trim());
                String ent = dxr.entityOf(q.getCallsign());
                if (ent != null)
                    ctrysByBand.computeIfAbsent(b, k -> new HashSet<>()).add(ent);
                if (withState) {
                    String st = q.getContestField3();
                    if (st != null && !st.isBlank())
                        stateByBand.computeIfAbsent(b, k -> new HashSet<>())
                                .add(st.trim().toUpperCase());
                }
            }
            int zoneMults  = zonesByBand.values().stream().mapToInt(Set::size).sum();
            int ctryMults  = ctrysByBand.values().stream().mapToInt(Set::size).sum();
            int stateMults = stateByBand.values().stream().mapToInt(Set::size).sum();
            int total = sumPoints(qsos);
            int mults = zoneMults + ctryMults + stateMults;
            List<String> zonesWorked = zonesByBand.values().stream()
                    .flatMap(Set::stream).distinct().toList();
            return ContestScore.zones(count, total, mults, total * mults, zonesWorked);
        }

        if (rules != null && "grid_field".equals(rules.getMultiplierType())) {
            // WW Digi: mult = distinct 2-char Maidenhead grid FIELD (first 2
            // chars of field1) once PER BAND. Score = Σ distance pts × Σ fields.
            Map<String, Set<String>> gfByBand = new LinkedHashMap<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (b.isBlank()) continue;
                String g = q.getContestField1();
                if (g == null || g.trim().length() < 2) continue;
                gfByBand.computeIfAbsent(b, k -> new HashSet<>())
                        .add(g.trim().substring(0, 2).toUpperCase());
            }
            int mults = gfByBand.values().stream().mapToInt(Set::size).sum();
            int total = sumPoints(qsos);
            return ContestScore.of(count, total, mults, total * mults);
        }

        if (rules != null && "wae".equals(rules.getMultiplierType())) {
            // WAE-DC: per-band distinct WAE token, band-weighted (80×4, 40×3,
            // 20/15/10×2), summed. Score = (Σ QSO pts + Σ QTC pts) × weighted.
            Map<String, Set<String>> tokByBand = new LinkedHashMap<>();
            for (QsoRecord q : qsos) {
                if (q.isDupe()) continue;
                String b = q.getBand() == null ? "" : q.getBand();
                if (WaeMultiplier.bandWeight(b) == 0) continue;
                String tok = WaeMultiplier.token(q.getCallsign());
                if (tok != null)
                    tokByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
            }
            int weighted = 0;
            for (var e : tokByBand.entrySet())
                weighted += e.getValue().size() * WaeMultiplier.bandWeight(e.getKey());
            int qsoPts = sumPoints(qsos);
            return ContestScore.of(count, qsoPts, weighted, (qsoPts + qtcPoints) * weighted);
        }

        if (plugin.getMultiplierModel() != null && plugin.getMultiplierModel().isPerBand()) {
            // Per-band mult accounting (ARRL Intl Digital): total = Σ over bands
            // of distinct values on that band. Feeds WorkedGridsPane.
            Map<String, List<String>> workedByBand = new LinkedHashMap<>();
            int totalMults = 0;
            for (String band : contestBands) {
                List<String> w = distinctFieldByBand(qsos, multColumn, band);
                workedByBand.put(band, w);
                totalMults += w.size();
            }
            int total = sumPoints(qsos);
            return ContestScore.perBand(count, total, totalMults, total * totalMults, workedByBand);
        }

        // Default: flat distinct multiplier list (sections / maps).
        List<String> worked = distinctField(qsos, multColumn);
        int total = sumPoints(qsos);
        return ContestScore.withWorked(count, total, worked.size(), total * worked.size(), worked);
    }

    /** Σ stored QSO points over non-dupes — equivalent to {@code totalPointsByContest}. */
    private static int sumPoints(List<QsoRecord> qsos) {
        int t = 0;
        for (QsoRecord q : qsos) if (!q.isDupe()) t += q.getPoints();
        return t;
    }

    /** Σ stored QSO points over non-dupes with the exact mode — like {@code pointsByMode}. */
    private static int sumPointsByMode(List<QsoRecord> qsos, String mode) {
        int t = 0;
        for (QsoRecord q : qsos) if (!q.isDupe() && mode.equals(q.getMode())) t += q.getPoints();
        return t;
    }

    /** Field-slot column value for one record (field1..field5), else null. */
    private static String fieldCol(QsoRecord q, String col) {
        return switch (col == null ? "" : col) {
            case "field1" -> q.getContestField1();
            case "field2" -> q.getContestField2();
            case "field3" -> q.getContestField3();
            case "field4" -> q.getContestField4();
            case "field5" -> q.getContestField5();
            default -> null;
        };
    }

    /** Distinct non-null slot values over non-dupes (blank "" counts) — {@code distinctFieldByColumn}. */
    private static List<String> distinctField(List<QsoRecord> qsos, String col) {
        if (col == null || !col.matches("field[1-5]")) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        for (QsoRecord q : qsos) {
            if (q.isDupe()) continue;
            String v = fieldCol(q, col);
            if (v != null) seen.add(v);
        }
        return new ArrayList<>(seen);
    }

    /** Distinct non-blank slot values over non-dupes for a mode — {@code distinctFieldByColumnAndMode}. */
    private static List<String> distinctFieldByMode(List<QsoRecord> qsos, String col, String mode) {
        if (col == null || !col.matches("field[1-5]")) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        for (QsoRecord q : qsos) {
            if (q.isDupe() || !mode.equals(q.getMode())) continue;
            String v = fieldCol(q, col);
            if (v != null && !v.isBlank()) seen.add(v);
        }
        return new ArrayList<>(seen);
    }

    /** Distinct non-blank slot values over non-dupes for a band — {@code distinctFieldByColumnAndBand}. */
    private static List<String> distinctFieldByBand(List<QsoRecord> qsos, String col, String band) {
        if (col == null || !col.matches("field[1-5]")) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        for (QsoRecord q : qsos) {
            if (q.isDupe() || !band.equals(q.getBand())) continue;
            String v = fieldCol(q, col);
            if (v != null && !v.isBlank()) seen.add(v);
        }
        return new ArrayList<>(seen);
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
