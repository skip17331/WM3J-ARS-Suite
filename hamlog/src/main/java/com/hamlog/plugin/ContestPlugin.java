package com.hamlog.plugin;

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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoringRules {
        private int    pointsPerQso;         // default points per QSO
        private Map<String, Integer> modePoints; // override per mode
        private String multiplierType;       // "sections" | "dxcc" | "states" | "custom"
        private String scoreFormula;         // e.g. "qsoPoints * multipliers"
        private boolean allowDupes;

        public int    getPointsPerQso()     { return pointsPerQso; }
        public void   setPointsPerQso(int v){ this.pointsPerQso = v; }
        public Map<String, Integer> getModePoints(){ return modePoints; }
        public void   setModePoints(Map<String, Integer> v){ this.modePoints = v; }
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

    /** Convenience: find a FieldDef by id. */
    public FieldDef getField(String id) {
        if (entryFields == null) return null;
        return entryFields.stream()
            .filter(f -> id.equals(f.getId()))
            .findFirst().orElse(null);
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
}
