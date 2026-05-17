package com.jlog.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Java representation of a JSON contest plugin file.
 *
 * Schema:
 *   contestId        — unique machine ID  e.g. "ARRL_SS_CW"
 *   contestName      — display name       e.g. "ARRL November Sweepstakes (CW)"
 *   version          — plugin version
 *   entryFields      — list of FieldDef for Row 1 entry bar
 *   exchangeFormat   — human-readable exchange description
 *   scoringRules     — ScoringRules object
 *   multiplierModel  — MultiplierModel object
 *   row2Panes        — list of PaneDef for Row 2 (3 or 4 panes)
 *   statistics       — list of statistic IDs to compute
 *   cabrilloMapping  — map from field name to Cabrillo column
 *   sections         — optional list of valid section values (for SS etc.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContestPlugin {

    private String contestId;
    private String contestName;
    private String version;
    private List<FieldDef> entryFields;
    private String exchangeFormat;
    private ScoringRules scoringRules;
    private MultiplierModel multiplierModel;
    private List<PaneDef> row2Panes;
    private List<String> statistics;
    private Map<String, String> cabrilloMapping;
    private List<String> sections;

    // New (ARRL 10M / cockpit framework):
    private String lockedBand;                       // if set, band field is forced + read-only
    private String lockedMode;                       // if set, mode field is forced + read-only
    private boolean perModeMultipliers;              // multipliers count once per mode
    private String stationClassifier;                // e.g. "callsignRegion" → drives conditionalFields
    private List<ConditionalField> conditionalFields;// show/hide fields based on classifier
    private List<String> usStates;                   // multiplier list for us_state_map pane
    private List<String> canadaProvinces;            // multiplier list for canada_map pane
    // Classpath resource path to a JSON array-of-strings of multiplier values
    // (e.g. "/com/jlog/counties/ca.json"). When set, the worked_mults pane
    // renders the full universe instead of only DB-derived worked entries.
    private String multiplierList;
    // If true, when the worked callsign classifies as DX the controller auto-fills
    // the state/prov-rcvd field with a DXCC prefix proxy (letters before first digit)
    // so different DX entities still count as separate multipliers.
    private boolean autoFillDxccPrefix;
    // If true, auto-fill a "prefix_rcvd" field with the WPX-style prefix
    // (letters + first digit) derived from the worked callsign. CQ WPX.
    private boolean autoFillWpxPrefix;
    // If true, a station is a dupe contest-wide (any band/mode). ARRL Sweepstakes.
    private boolean contestWideDupe;
    // If true, rover callsigns (suffix "/R") can be worked again each time
    // they change grid. Dupe check keys on callsign+band+grid instead of
    // callsign+band. Used by ARRL June VHF and similar VHF+ contests.
    private boolean roverAwareDupe;
    // If true, EVERY callsign is a dupe only per (callsign, band, grid) —
    // a station re-worked from a new grid/location is a new QSO regardless
    // of any /R suffix. ARRL 10 GHz & Up ("once per band from each location").
    private boolean perBandGridDupe;

    // ARRL Field Day (Rule 6.3/6.6/6.7): workable once per band per *mode
    // category* — all voice modes equivalent, all non-CW digital equivalent,
    // CW its own. Collapses the raw mode to CW/PH/DG for the dupe key.
    private boolean fieldDayModeDupe;

    // ---------------------------------------------------------------
    // Inner classes
    // ---------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldDef {
        private String id;          // machine name e.g. "callsign"
        private String label;       // display label
        private String type;        // text | number | combo | checkbox
        private int    width;       // suggested width in pixels
        private boolean required;
        private boolean autoIncrement; // for serial number fields
        private List<String> options;  // for combo fields
        private int entryRow;       // 0 = received row (default), 1 = sent row
        private boolean persistent; // if true, value survives doClear()
        private String  validator;  // optional extra validator: "maidenhead" | "numeric" | null

        public String getId()          { return id; }
        public void   setId(String v)  { this.id = v; }
        public String getLabel()       { return label; }
        public void   setLabel(String v){ this.label = v; }
        public String getType()        { return type; }
        public void   setType(String v){ this.type = v; }
        public int    getWidth()       { return width; }
        public void   setWidth(int v)  { this.width = v; }
        public boolean isRequired()    { return required; }
        public void   setRequired(boolean v){ this.required = v; }
        public boolean isAutoIncrement(){ return autoIncrement; }
        public void   setAutoIncrement(boolean v){ this.autoIncrement = v; }
        public List<String> getOptions(){ return options; }
        public void   setOptions(List<String> v){ this.options = v; }
        public int  getEntryRow()      { return entryRow; }
        public void setEntryRow(int v) { this.entryRow = v; }
        public boolean isPersistent()  { return persistent; }
        public void setPersistent(boolean v){ this.persistent = v; }
        public String  getValidator()  { return validator; }
        public void setValidator(String v){ this.validator = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringRules {
        private int    pointsPerQso;         // default points per QSO
        private Map<String, Integer> modePoints; // override per mode
        // Region-pair points (e.g. ARRL 160M): "US_DX"→5, "US_US"→2. Key format
        // is "{myRegion}_{theirRegion}" where regions are US | CA | DX.
        private Map<String, Integer> pointsByRegionPair;
        // Band-class points (e.g. ARRL Intl Digital): "HF"→1, "VHF"→2. The
        // controller resolves a band name to a class via a small lookup table.
        private Map<String, Integer> pointsByBandClass;
        // Direct per-band points (e.g. ARRL June VHF): "6m"→1, "70cm"→2, "10GHz"→3.
        // Takes precedence over pointsByBandClass when both are declared.
        private Map<String, Integer> pointsByBand;
        // If true, the contest score is just the point total — no multiplier
        // multiplication. Used by Field Day (bonus points are computed off-log).
        private boolean scoreIsPointsOnly;
        // If true, Rookie Roundup scoring applies: 2pts when the received
        // year-first-licensed field indicates a rookie (≤2 years), else 1pt.
        private boolean rookieRoundupScoring;
        // Great-circle distance scoring (ARRL 222 & Up, ARRL Intl Digital).
        // When present, per-QSO points are derived from the distance between
        // the worked and own grid-square centers — see DistanceScoring.
        private DistanceScoring distanceScoring;
        private String multiplierType;       // "sections" | "dxcc" | "states" | "custom" | "qso_party"
        private String scoreFormula;         // e.g. "qsoPoints * multipliers"
        private boolean allowDupes;
        private QsoPartyConfig qsoParty;     // config for multiplierType:"qso_party"

        public int    getPointsPerQso()     { return pointsPerQso; }
        public void   setPointsPerQso(int v){ this.pointsPerQso = v; }
        public Map<String, Integer> getModePoints(){ return modePoints; }
        public void   setModePoints(Map<String, Integer> v){ this.modePoints = v; }
        public Map<String, Integer> getPointsByRegionPair(){ return pointsByRegionPair; }
        public void   setPointsByRegionPair(Map<String, Integer> v){ this.pointsByRegionPair = v; }
        public Map<String, Integer> getPointsByBandClass(){ return pointsByBandClass; }
        public void   setPointsByBandClass(Map<String, Integer> v){ this.pointsByBandClass = v; }
        public Map<String, Integer> getPointsByBand(){ return pointsByBand; }
        public void   setPointsByBand(Map<String, Integer> v){ this.pointsByBand = v; }
        public boolean isScoreIsPointsOnly(){ return scoreIsPointsOnly; }
        public void   setScoreIsPointsOnly(boolean v){ this.scoreIsPointsOnly = v; }
        public boolean isRookieRoundupScoring(){ return rookieRoundupScoring; }
        public void   setRookieRoundupScoring(boolean v){ this.rookieRoundupScoring = v; }
        public String getMultiplierType()   { return multiplierType; }
        public void   setMultiplierType(String v){ this.multiplierType = v; }
        public String getScoreFormula()     { return scoreFormula; }
        public void   setScoreFormula(String v){ this.scoreFormula = v; }
        public DistanceScoring getDistanceScoring(){ return distanceScoring; }
        public void   setDistanceScoring(DistanceScoring v){ this.distanceScoring = v; }
        public boolean isAllowDupes()       { return allowDupes; }
        public void   setAllowDupes(boolean v){ this.allowDupes = v; }
        public QsoPartyConfig getQsoParty() { return qsoParty; }
        public void   setQsoParty(QsoPartyConfig v){ this.qsoParty = v; }
    }

    /**
     * Configuration for the reusable {@code multiplierType:"qso_party"}
     * scoring engine. Captures the structural variation across state/
     * province QSO parties: which county codes mark a station in-state,
     * per-mode-class point table, multiplier scope (per mode / per band /
     * once overall), which multiplier dimensions each side (in-state vs
     * out-of-state entrant) may count, the FT8/WSJT grid rule, and the
     * bonus / club special callsigns. All claimed/running — the sponsor
     * re-adjudicates.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QsoPartyConfig {
        private String stateName;                       // "Vermont"
        private String stateAbbr;                       // "VT" — own state, not a W/VE state-mult
        private List<String> inStateCounties;           // county codes ⇒ station is in-state
        private boolean countyByExclusion;               // county = not "DX" & length != 2
                                                         //   (state/prov are 2-char) — for parties
                                                         //   whose county codes are mixed length
        private Map<String,Integer> qsoPointCalls;       // base-call → fixed per-QSO points
                                                         //   (overrides mode table, all entrants)
        private int countyCodeLen;                       // >0: any non-"DX" QTH of this length
                                                         //     is an in-state county (used when
                                                         //     the official code list isn't bundled)
        private int areaStatePrefixLen;                  // >0: multi-state-area party (7QP). An
                                                         //     in-area entrant working an in-area
                                                         //     station counts the leading N chars
                                                         //     of the received state+county code
                                                         //     as the STATE mult (ORDES → "OR");
                                                         //     a non-area entrant still counts the
                                                         //     whole code as a distinct county.
        private List<String> inStateGrids;              // 4-char grids inside the state (FT8)
        private Map<String,Integer> pointsByModeClass;  // PH|CW|RY|DG → QSO points
        private Map<String,Integer> pointsByModeClassOut;// same, but for an OUT-of-state
                                                         //   entrant (entrant-asymmetric pts,
                                                         //   e.g. DEQP in-DE PH1/CW2 vs
                                                         //   out-DE PH10/CW20); null ⇒ both
                                                         //   sides use pointsByModeClass
        private String multScope;                       // "per_mode" | "per_band" | "once"
        private String multScopeOut;                    // optional OUT-of-state-entrant scope
                                                        //   override (entrant-asymmetric scope —
                                                        //   HQP: HI entrant once, non-HI per band);
                                                        //   null/blank ⇒ both sides use multScope
        private boolean inStateCountsStates;            // in-state entrant: W/VE state/prov as mult
        private boolean inStateCountsDxccEach;          // DX per-country (true) vs single "DX" (false)
        private boolean inStateCountsCounties;          // in-state entrant: own counties as mult
        private boolean inStateOwnStateMult;            // in-state entrant: an in-state (county-
                                                        //   sending) station = ONE state mult
                                                        //   (stateAbbr), not per-county (IDQP)
        private boolean inStateSelfStateMult;           // true ⇒ in-state entrant ALSO gets one
                                                        //   "own-state" state mult (their own state
                                                        //   counts among the 50, even though local
                                                        //   stations send a county) — WVQP
        private boolean inStateNoDxMult;                // true ⇒ DX yields NO multiplier (BCQP/SCQP)
        private boolean clubMemberMult;                 // true ⇒ NON-geographic: the multiplier is
                                                        //   the count of distinct club-member base
                                                        //   callsigns worked (a club member signs
                                                        //   call /### so the received exchange
                                                        //   carries a digit) — once overall, not
                                                        //   per band/mode (ARC QSO Party / SJRA)
        private boolean pointsAllQsos;                  // true ⇒ every QSO scores (no in/out gate, NSARA)
        private boolean mergeRttyDigital;               // true ⇒ RTTY folds into the WSJT "DG" class
        private boolean mergeCwDigital;                 // true ⇒ CW+RTTY+digital = one "CD" class (LAQP)
        private boolean gridDivisorCeil;                // true ⇒ in-state FT8 grid mult rounds UP (MSQP)
        private boolean outStateGridUncapped;           // true ⇒ out-of-state grid mult = ALL distinct
                                                        //   in-state grids (no cap, MSQP)
        private Map<String,Integer> bonusStations;      // base-call → flat post-multiply bonus pts,
                                                        //   credited once per band+mode-class
        private Map<String,Integer> bonusStationsOnce;  // base-call → flat post-multiply bonus pts,
                                                        //   credited ONCE for the whole log (N5LCC/W1AW5)
        private Map<String,Integer> bonusStationsPerMode;// base-call → flat post-multiply bonus pts,
                                                        //   credited once per MODE-class (not per
                                                        //   band) — WA Salmon Run W7DX +500/mode
                                                        //   (Phone+CW, max 1000)
        private List<String> rareCounties;              // counties whose QSO pts are ×rareQsoMultiplier
        private int rareQsoMultiplier;                  // per-QSO points multiplier for a rare county
        private int sweepBonusPoints;                   // post-multiply bonus if the sweep threshold
        private int sweepBonusThreshold;                //   distinct rare counties is reached
        private int sweepBonusPoints2;                  // lower fallback tier (awarded only when the
        private int sweepBonusThreshold2;               //   primary tier is NOT met) — MDC: 25→500
                                                        //   else 13→250
        private int ptsInToIn;                          // relationship points (0 = use modeClass table):
        private int ptsInToOut;                         //   in-state↔in / in↔out / out↔in;
        private int ptsOutToIn;                         //   out↔out is always 0 (gated)
        private int ptsWorkedInState;                   // MEQP-style entrant-symmetric points by
        private int ptsWorkedOutState;                  //   the WORKED station: in-state worked →
                                                        //   ptsWorkedInState, else ptsWorkedOutState
                                                        //   (regardless of the entrant's location)
        private boolean multsAllEntrants;               // true ⇒ ALL entrants use the in-state
                                                        //   multiplier branch (counties+states+
                                                        //   prov+DXCC for everyone — MEQP, where
                                                        //   "multipliers are the same for all")
        private int ft8GridDivisor;                     // in-state DG mult = floor(#grids / divisor); 0=off
        private int outStateGridCap;                    // out-of-state DG mult = min(#in-state grids, cap)
        private int multCap;                            // >0 ⇒ total scored mults capped at this
                                                        //   value (CQP: 58 scored of 63 possible)
        private Map<String,Integer> bonusPointCalls;    // out-of-state +pts for working these calls
        private List<String> clubMultCalls;             // each counts as a multiplier (scope-applied)

        public String getStateName(){ return stateName; }
        public void setStateName(String v){ this.stateName = v; }
        public String getStateAbbr(){ return stateAbbr; }
        public void setStateAbbr(String v){ this.stateAbbr = v; }
        public List<String> getInStateCounties(){ return inStateCounties; }
        public void setInStateCounties(List<String> v){ this.inStateCounties = v; }
        public int getCountyCodeLen(){ return countyCodeLen; }
        public void setCountyCodeLen(int v){ this.countyCodeLen = v; }
        public boolean isCountyByExclusion(){ return countyByExclusion; }
        public void setCountyByExclusion(boolean v){ this.countyByExclusion = v; }
        public int getAreaStatePrefixLen(){ return areaStatePrefixLen; }
        public void setAreaStatePrefixLen(int v){ this.areaStatePrefixLen = v; }
        public Map<String,Integer> getQsoPointCalls(){ return qsoPointCalls; }
        public void setQsoPointCalls(Map<String,Integer> v){ this.qsoPointCalls = v; }
        public List<String> getInStateGrids(){ return inStateGrids; }
        public void setInStateGrids(List<String> v){ this.inStateGrids = v; }
        public Map<String,Integer> getPointsByModeClass(){ return pointsByModeClass; }
        public void setPointsByModeClass(Map<String,Integer> v){ this.pointsByModeClass = v; }
        public Map<String,Integer> getPointsByModeClassOut(){ return pointsByModeClassOut; }
        public void setPointsByModeClassOut(Map<String,Integer> v){ this.pointsByModeClassOut = v; }
        public String getMultScope(){ return multScope; }
        public void setMultScope(String v){ this.multScope = v; }
        public String getMultScopeOut(){ return multScopeOut; }
        public void setMultScopeOut(String v){ this.multScopeOut = v; }
        public boolean isInStateCountsStates(){ return inStateCountsStates; }
        public void setInStateCountsStates(boolean v){ this.inStateCountsStates = v; }
        public boolean isInStateCountsDxccEach(){ return inStateCountsDxccEach; }
        public void setInStateCountsDxccEach(boolean v){ this.inStateCountsDxccEach = v; }
        public boolean isInStateCountsCounties(){ return inStateCountsCounties; }
        public void setInStateCountsCounties(boolean v){ this.inStateCountsCounties = v; }
        public boolean isInStateOwnStateMult(){ return inStateOwnStateMult; }
        public void setInStateOwnStateMult(boolean v){ this.inStateOwnStateMult = v; }
        public int getFt8GridDivisor(){ return ft8GridDivisor; }
        public void setFt8GridDivisor(int v){ this.ft8GridDivisor = v; }
        public int getOutStateGridCap(){ return outStateGridCap; }
        public void setOutStateGridCap(int v){ this.outStateGridCap = v; }
        public int getMultCap(){ return multCap; }
        public void setMultCap(int v){ this.multCap = v; }
        public Map<String,Integer> getBonusPointCalls(){ return bonusPointCalls; }
        public void setBonusPointCalls(Map<String,Integer> v){ this.bonusPointCalls = v; }
        public List<String> getClubMultCalls(){ return clubMultCalls; }
        public void setClubMultCalls(List<String> v){ this.clubMultCalls = v; }
        public boolean isInStateSelfStateMult(){ return inStateSelfStateMult; }
        public void setInStateSelfStateMult(boolean v){ this.inStateSelfStateMult = v; }
        public boolean isInStateNoDxMult(){ return inStateNoDxMult; }
        public void setInStateNoDxMult(boolean v){ this.inStateNoDxMult = v; }
        public boolean isClubMemberMult(){ return clubMemberMult; }
        public void setClubMemberMult(boolean v){ this.clubMemberMult = v; }
        public boolean isPointsAllQsos(){ return pointsAllQsos; }
        public void setPointsAllQsos(boolean v){ this.pointsAllQsos = v; }
        public boolean isMergeRttyDigital(){ return mergeRttyDigital; }
        public void setMergeRttyDigital(boolean v){ this.mergeRttyDigital = v; }
        public boolean isMergeCwDigital(){ return mergeCwDigital; }
        public void setMergeCwDigital(boolean v){ this.mergeCwDigital = v; }
        public boolean isGridDivisorCeil(){ return gridDivisorCeil; }
        public void setGridDivisorCeil(boolean v){ this.gridDivisorCeil = v; }
        public boolean isOutStateGridUncapped(){ return outStateGridUncapped; }
        public void setOutStateGridUncapped(boolean v){ this.outStateGridUncapped = v; }
        public Map<String,Integer> getBonusStations(){ return bonusStations; }
        public void setBonusStations(Map<String,Integer> v){ this.bonusStations = v; }
        public Map<String,Integer> getBonusStationsOnce(){ return bonusStationsOnce; }
        public void setBonusStationsOnce(Map<String,Integer> v){ this.bonusStationsOnce = v; }
        public Map<String,Integer> getBonusStationsPerMode(){ return bonusStationsPerMode; }
        public void setBonusStationsPerMode(Map<String,Integer> v){ this.bonusStationsPerMode = v; }
        public List<String> getRareCounties(){ return rareCounties; }
        public void setRareCounties(List<String> v){ this.rareCounties = v; }
        public int getRareQsoMultiplier(){ return rareQsoMultiplier; }
        public void setRareQsoMultiplier(int v){ this.rareQsoMultiplier = v; }
        public int getSweepBonusPoints(){ return sweepBonusPoints; }
        public void setSweepBonusPoints(int v){ this.sweepBonusPoints = v; }
        public int getSweepBonusThreshold(){ return sweepBonusThreshold; }
        public void setSweepBonusThreshold(int v){ this.sweepBonusThreshold = v; }
        public int getSweepBonusPoints2(){ return sweepBonusPoints2; }
        public void setSweepBonusPoints2(int v){ this.sweepBonusPoints2 = v; }
        public int getSweepBonusThreshold2(){ return sweepBonusThreshold2; }
        public void setSweepBonusThreshold2(int v){ this.sweepBonusThreshold2 = v; }
        public int getPtsInToIn(){ return ptsInToIn; }
        public void setPtsInToIn(int v){ this.ptsInToIn = v; }
        public int getPtsInToOut(){ return ptsInToOut; }
        public void setPtsInToOut(int v){ this.ptsInToOut = v; }
        public int getPtsOutToIn(){ return ptsOutToIn; }
        public void setPtsOutToIn(int v){ this.ptsOutToIn = v; }
        public int getPtsWorkedInState(){ return ptsWorkedInState; }
        public void setPtsWorkedInState(int v){ this.ptsWorkedInState = v; }
        public int getPtsWorkedOutState(){ return ptsWorkedOutState; }
        public void setPtsWorkedOutState(int v){ this.ptsWorkedOutState = v; }
        public boolean isMultsAllEntrants(){ return multsAllEntrants; }
        public void setMultsAllEntrants(boolean v){ this.multsAllEntrants = v; }
    }

    /**
     * Distance-based per-QSO scoring. Two formulas are supported:
     *  - "km_x_bandfactor"     (ARRL 222 & Up): round(distanceKm) × bandFactor[band],
     *                          floored at minKm (same-grid = minKm).
     *  - "one_plus_ceil_km_div" (ARRL Intl Digital): basePoints +
     *                          max(minDistancePoints, ceil(distanceKm / divisorKm)).
     * Distance is great-circle between the centers of theirGridField and
     * ownGridField (ownGridField falls back to the station config grid).
     * The result is a claimed/running score — ARRL re-adjudicates from logs.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DistanceScoring {
        private String theirGridField;            // e.g. "grid_rcvd"
        private String ownGridField;              // e.g. "grid_sent" (fallback: station grid)
        private String formula;                   // "km_x_bandfactor" | "one_plus_ceil_km_div"
        private double minKm;                     // same-grid / floor distance
        private Map<String, Integer> bandFactor;  // km_x_bandfactor
        private double divisorKm;                 // one_plus_ceil_km_div
        private int basePoints;                   // one_plus_ceil_km_div base QSO point
        private int minDistancePoints;            // one_plus_ceil_km_div min distance pts

        public String getTheirGridField(){ return theirGridField; }
        public void   setTheirGridField(String v){ this.theirGridField = v; }
        public String getOwnGridField(){ return ownGridField; }
        public void   setOwnGridField(String v){ this.ownGridField = v; }
        public String getFormula(){ return formula; }
        public void   setFormula(String v){ this.formula = v; }
        public double getMinKm(){ return minKm; }
        public void   setMinKm(double v){ this.minKm = v; }
        public Map<String, Integer> getBandFactor(){ return bandFactor; }
        public void   setBandFactor(Map<String, Integer> v){ this.bandFactor = v; }
        public double getDivisorKm(){ return divisorKm; }
        public void   setDivisorKm(double v){ this.divisorKm = v; }
        public int    getBasePoints(){ return basePoints; }
        public void   setBasePoints(int v){ this.basePoints = v; }
        public int    getMinDistancePoints(){ return minDistancePoints; }
        public void   setMinDistancePoints(int v){ this.minDistancePoints = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MultiplierModel {
        private String field;           // which entry field holds the multiplier value
        private List<String> validValues; // known valid multiplier values
        private boolean perBand;        // multipliers counted per band?

        public String getField()        { return field; }
        public void   setField(String v){ this.field = v; }
        public List<String> getValidValues(){ return validValues; }
        public void   setValidValues(List<String> v){ this.validValues = v; }
        public boolean isPerBand()      { return perBand; }
        public void   setPerBand(boolean v){ this.perBand = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConditionalField {
        private String fieldId;
        private List<String> showForRegions;   // e.g. ["US","CA"]
        private List<String> hideForRegions;

        public String getFieldId()            { return fieldId; }
        public void   setFieldId(String v)    { this.fieldId = v; }
        public List<String> getShowForRegions(){ return showForRegions; }
        public void   setShowForRegions(List<String> v){ this.showForRegions = v; }
        public List<String> getHideForRegions(){ return hideForRegions; }
        public void   setHideForRegions(List<String> v){ this.hideForRegions = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaneDef {
        private int    paneIndex;       // 1-4
        private String paneType;        // "dupe_checker" | "custom" | "section_tracker" | "statistics"
        private String title;
        private Map<String, Object> config; // pane-specific config

        public int    getPaneIndex()    { return paneIndex; }
        public void   setPaneIndex(int v){ this.paneIndex = v; }
        public String getPaneType()     { return paneType; }
        public void   setPaneType(String v){ this.paneType = v; }
        public String getTitle()        { return title; }
        public void   setTitle(String v){ this.title = v; }
        public Map<String, Object> getConfig(){ return config; }
        public void   setConfig(Map<String, Object> v){ this.config = v; }
    }

    // ---------------------------------------------------------------
    // Getters / Setters
    // ---------------------------------------------------------------

    public String getContestId()        { return contestId; }
    public void   setContestId(String v){ this.contestId = v; }

    public String getContestName()      { return contestName; }
    public void   setContestName(String v){ this.contestName = v; }

    public String getVersion()          { return version; }
    public void   setVersion(String v)  { this.version = v; }

    public List<FieldDef> getEntryFields(){ return entryFields; }
    public void   setEntryFields(List<FieldDef> v){ this.entryFields = v; }

    public String getExchangeFormat()   { return exchangeFormat; }
    public void   setExchangeFormat(String v){ this.exchangeFormat = v; }

    public ScoringRules getScoringRules(){ return scoringRules; }
    public void   setScoringRules(ScoringRules v){ this.scoringRules = v; }

    public MultiplierModel getMultiplierModel(){ return multiplierModel; }
    public void   setMultiplierModel(MultiplierModel v){ this.multiplierModel = v; }

    public List<PaneDef> getRow2Panes() { return row2Panes; }
    public void   setRow2Panes(List<PaneDef> v){ this.row2Panes = v; }

    public List<String> getStatistics() { return statistics; }
    public void   setStatistics(List<String> v){ this.statistics = v; }

    public Map<String, String> getCabrilloMapping(){ return cabrilloMapping; }
    public void   setCabrilloMapping(Map<String, String> v){ this.cabrilloMapping = v; }

    public List<String> getSections()   { return sections; }
    public void   setSections(List<String> v){ this.sections = v; }

    public String getLockedBand()             { return lockedBand; }
    public void   setLockedBand(String v)     { this.lockedBand = v; }

    public String getLockedMode()             { return lockedMode; }
    public void   setLockedMode(String v)     { this.lockedMode = v; }

    public boolean isPerModeMultipliers()     { return perModeMultipliers; }
    public void   setPerModeMultipliers(boolean v){ this.perModeMultipliers = v; }

    public boolean isAutoFillDxccPrefix()     { return autoFillDxccPrefix; }
    public void   setAutoFillDxccPrefix(boolean v){ this.autoFillDxccPrefix = v; }

    public boolean isAutoFillWpxPrefix()      { return autoFillWpxPrefix; }
    public void   setAutoFillWpxPrefix(boolean v){ this.autoFillWpxPrefix = v; }

    public boolean isContestWideDupe()        { return contestWideDupe; }
    public void   setContestWideDupe(boolean v){ this.contestWideDupe = v; }

    public boolean isRoverAwareDupe()         { return roverAwareDupe; }
    public void   setRoverAwareDupe(boolean v){ this.roverAwareDupe = v; }

    public boolean isPerBandGridDupe()        { return perBandGridDupe; }
    public void   setPerBandGridDupe(boolean v){ this.perBandGridDupe = v; }
    public boolean isFieldDayModeDupe()       { return fieldDayModeDupe; }
    public void   setFieldDayModeDupe(boolean v){ this.fieldDayModeDupe = v; }

    public String getStationClassifier()      { return stationClassifier; }
    public void   setStationClassifier(String v){ this.stationClassifier = v; }

    public List<ConditionalField> getConditionalFields(){ return conditionalFields; }
    public void   setConditionalFields(List<ConditionalField> v){ this.conditionalFields = v; }

    public List<String> getUsStates()         { return usStates; }
    public void   setUsStates(List<String> v) { this.usStates = v; }

    public List<String> getCanadaProvinces()  { return canadaProvinces; }
    public void   setCanadaProvinces(List<String> v){ this.canadaProvinces = v; }

    public String getMultiplierList()         { return multiplierList; }
    public void   setMultiplierList(String v) { this.multiplierList = v; }

    /** Convenience: find a FieldDef by id. */
    public FieldDef getField(String id) {
        if (entryFields == null) return null;
        return entryFields.stream()
            .filter(f -> id.equals(f.getId()))
            .findFirst().orElse(null);
    }

    /**
     * Returns the DB column name (field1–field5) that holds multiplier values for this plugin.
     * Mirrors ContestLogController.getMultiplierColumn() so callers outside the controller
     * (e.g. j-digi via CONTEST_ACTIVE broadcast) can do accurate mult lookups.
     */
    public String computeMultiplierDbColumn() {
        if (multiplierModel == null || multiplierModel.getField() == null) return "field1";
        String col = fieldSlotColumn(multiplierModel.getField());
        return col != null ? col : "field1";
    }

    /**
     * DB column (field1–field5) that stores the entry field {@code targetId},
     * using the exact positional rule the controller applies on save: the
     * special non-slot ids (callsign, serial_*, rst_*, band, mode and the
     * SS-only sent constants prec/check/sect_sent) consume no slot; every
     * other entry field takes the next field1..5 in declaration order.
     * Returns null if {@code targetId} is one of those special ids, isn't a
     * declared entry field, or would land beyond field5.
     */
    public String fieldSlotColumn(String targetId) {
        if (entryFields == null || targetId == null) return null;
        int slot = 0;
        for (FieldDef fd : entryFields) {
            String id = fd.getId() != null ? fd.getId() : "";
            switch (id) {
                case "callsign", "serial_sent", "serial_rcvd", "band", "mode",
                     "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent" -> {}
                default -> {
                    if (id.equals(targetId)) return slot < 5 ? "field" + (slot + 1) : null;
                    slot++;
                }
            }
        }
        return null;
    }

    /** Points per QSO, honouring mode override if present. */
    public int pointsForMode(String mode) {
        if (scoringRules == null) return 1;
        if (scoringRules.getModePoints() != null
                && scoringRules.getModePoints().containsKey(mode)) {
            return scoringRules.getModePoints().get(mode);
        }
        return scoringRules.getPointsPerQso();
    }

    /** Points honouring region-pair map first, then mode, then default. */
    public int pointsFor(String myRegion, String theirRegion, String mode) {
        if (scoringRules != null
                && scoringRules.getPointsByRegionPair() != null
                && myRegion != null && theirRegion != null) {
            Integer pts = scoringRules.getPointsByRegionPair()
                .get(myRegion + "_" + theirRegion);
            if (pts != null) return pts;
        }
        return pointsForMode(mode);
    }

    /** Points awarded for a QSO on the given band. Resolution order:
     *  (1) pointsByBand (exact band name, e.g. "6m"→1 for June VHF),
     *  (2) pointsByBandClass ("HF"/"VHF"), (3) mode-based, (4) default. */
    public int pointsForBand(String band, String mode) {
        if (scoringRules != null && band != null) {
            Map<String, Integer> byBand = scoringRules.getPointsByBand();
            if (byBand != null) {
                for (Map.Entry<String, Integer> e : byBand.entrySet()) {
                    if (e.getKey().equalsIgnoreCase(band)) return e.getValue();
                }
            }
            Map<String, Integer> byClass = scoringRules.getPointsByBandClass();
            if (byClass != null) {
                String cls = bandClass(band);
                Integer pts = byClass.get(cls);
                if (pts != null) return pts;
            }
        }
        return pointsForMode(mode);
    }

    /** Classify a band label into "HF" (160m–10m) or "VHF" (6m and above). */
    public static String bandClass(String band) {
        if (band == null) return "HF";
        String b = band.trim().toLowerCase();
        return switch (b) {
            case "160m","80m","60m","40m","30m","20m","17m","15m","12m","10m" -> "HF";
            case "6m","4m","2m","1.25m","70cm","33cm","23cm","13cm","9cm","6cm","3cm","1cm" -> "VHF";
            default -> "HF";
        };
    }
}
