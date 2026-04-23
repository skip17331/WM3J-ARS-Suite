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
        private String multiplierType;       // "sections" | "dxcc" | "states" | "custom"
        private String scoreFormula;         // e.g. "qsoPoints * multipliers"
        private boolean allowDupes;

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
        public boolean isAllowDupes()       { return allowDupes; }
        public void   setAllowDupes(boolean v){ this.allowDupes = v; }
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
        String targetId = multiplierModel.getField();
        int slot = 0;
        if (entryFields == null) return "field1";
        for (FieldDef fd : entryFields) {
            switch (fd.getId() != null ? fd.getId() : "") {
                case "callsign", "serial_sent", "serial_rcvd", "band", "mode",
                     "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent" -> {}
                default -> {
                    if (fd.getId().equals(targetId)) return "field" + (slot + 1);
                    slot++;
                }
            }
        }
        return "field1";
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
