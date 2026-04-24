package com.jlog.award;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Declarative specification of a logbook award — standard (WAS, DXCC, WPX) or
 * special-event (13 Colonies, Route 66, etc.). Awards are JSON files loaded
 * by {@link AwardLoader}, evaluated by {@link AwardService} against the
 * normal-log QSO database.
 *
 * Two flavors depending on whether {@code targets} is populated:
 *   Set-match    — targets is a predeclared list; progress = how many targets
 *                  are covered by the log. Example: WAS (50 states).
 *   Count-match  — targets is null/empty; progress = how many distinct values
 *                  appear in the log. Example: WPX (unique prefixes).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwardPlugin {

    private String awardId;
    private String awardName;
    private String description;

    /** Which QSO field to extract and compare against targets. One of:
     *  "state", "country", "callsign", "prefix", "dxccPrefix", "continent". */
    private String matchOn;

    /** Display label for the target axis ("States", "Prefixes", …). */
    private String targetLabel;

    private List<Target> targets;
    private List<Target> bonus;
    private List<Tier>   tiers;
    private Window       window;
    private Options      options;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Target {
        private String id;
        private String label;
        public String getId()           { return id; }
        public void   setId(String v)   { this.id = v; }
        public String getLabel()        { return label; }
        public void   setLabel(String v){ this.label = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tier {
        private int    threshold;
        private String name;
        public int    getThreshold()     { return threshold; }
        public void   setThreshold(int v){ this.threshold = v; }
        public String getName()          { return name; }
        public void   setName(String v)  { this.name = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Window {
        private String startUtc;   // ISO-8601, e.g. "2026-07-01T13:00:00Z"
        private String endUtc;

        public String getStartUtc()       { return startUtc; }
        public void   setStartUtc(String v){ this.startUtc = v; }
        public String getEndUtc()         { return endUtc; }
        public void   setEndUtc(String v) { this.endUtc = v; }

        public LocalDateTime startDateTime() { return parse(startUtc); }
        public LocalDateTime endDateTime()   { return parse(endUtc); }
        private static LocalDateTime parse(String s) {
            if (s == null || s.isBlank()) return null;
            try {
                return LocalDateTime.parse(s.endsWith("Z") ? s.substring(0, s.length() - 1) : s,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) { return null; }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Options {
        private boolean matchBaseCallsign;   // strip /P /M etc when matching
        private boolean confirmedOnly;       // only count QSL-confirmed QSOs
        public boolean isMatchBaseCallsign()       { return matchBaseCallsign; }
        public void   setMatchBaseCallsign(boolean v){ this.matchBaseCallsign = v; }
        public boolean isConfirmedOnly()           { return confirmedOnly; }
        public void   setConfirmedOnly(boolean v)  { this.confirmedOnly = v; }
    }

    public String getAwardId()                  { return awardId; }
    public void   setAwardId(String v)          { this.awardId = v; }
    public String getAwardName()                { return awardName; }
    public void   setAwardName(String v)        { this.awardName = v; }
    public String getDescription()              { return description; }
    public void   setDescription(String v)      { this.description = v; }
    public String getMatchOn()                  { return matchOn; }
    public void   setMatchOn(String v)          { this.matchOn = v; }
    public String getTargetLabel()              { return targetLabel; }
    public void   setTargetLabel(String v)      { this.targetLabel = v; }
    public List<Target> getTargets()            { return targets; }
    public void   setTargets(List<Target> v)    { this.targets = v; }
    public List<Target> getBonus()              { return bonus; }
    public void   setBonus(List<Target> v)      { this.bonus = v; }
    public List<Tier> getTiers()                { return tiers; }
    public void   setTiers(List<Tier> v)        { this.tiers = v; }
    public Window  getWindow()                  { return window; }
    public void   setWindow(Window v)           { this.window = v; }
    public Options getOptions()                 { return options; }
    public void   setOptions(Options v)         { this.options = v; }

    public boolean isSetMatch() { return targets != null && !targets.isEmpty(); }

    /** True if a QSO's UTC datetime falls inside this award's time window.
     *  Awards without a window always return true. */
    public boolean withinWindow(LocalDateTime utc) {
        if (window == null || utc == null) return true;
        LocalDateTime s = window.startDateTime();
        LocalDateTime e = window.endDateTime();
        if (s != null && utc.isBefore(s)) return false;
        if (e != null && utc.isAfter(e))  return false;
        return true;
    }
}
